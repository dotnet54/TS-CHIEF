package knn;

import core.Classifier;
import core.ClassifierResult;
import core.DebugInfo;
import core.Ensemble;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import util.PrintUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelTasks {

	private ExecutorService executor;
	protected int numThreads;
	protected int numRequestedThreads;
	protected int numAvailableProcessors;
	//dev
	private int maxPoolSize;

	public ParallelTasks(int numThreads) {
		int numAvailableProcessors = Runtime.getRuntime().availableProcessors();
		this.numRequestedThreads = numThreads;
		this.numThreads = numRequestedThreads;
		if (numThreads == 0 || numThreads > numAvailableProcessors) {
			this.numThreads = Runtime.getRuntime().availableProcessors();
		}

		setExecutor(Executors.newFixedThreadPool(this.numThreads));

		// this is important for slurm jobs because
		// Runtime.getRuntime().availableProcessors() does not equal SBATCH argument
		// cpus-per-task
		if (executor instanceof ThreadPoolExecutor) {
			maxPoolSize = ((ThreadPoolExecutor) executor).getMaximumPoolSize();
		}

//		System.out.println("ParallelTasks: threads: " + this.numThreads +
//				", requested: " + numRequestedThreads + ", available: " + numAvailableProcessors +
//				", executor max pool: " + maxPoolSize);

	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public ThreadPoolExecutor getThreadPool() {
		if (executor instanceof ThreadPoolExecutor) {
			return ((ThreadPoolExecutor) executor);
		} else {
			return null;
		}
	}

	public class AsyncTaskResult{
		protected AtomicInteger taskID;

		public AsyncTaskResult(int taskID){
			this.taskID = new AtomicInteger(taskID);
		}

		public AtomicInteger getTaskID() {
			return taskID;
		}

	}

	public class AsyncTask implements Callable<AsyncTaskResult>{
		protected AsyncTaskResult result;

		public AsyncTask(int taskID){
			this.result = new AsyncTaskResult(taskID);
		}

		public AsyncTaskResult getTaskID() {
			return result;
		}

		@Override
		public AsyncTaskResult call() throws Exception {
			return this.result;
		}
	}

	public void runTasks(Callable<Integer>[] tasks) throws InterruptedException, ExecutionException {
		List<Callable<Integer>> taskList = Arrays.asList(tasks);
		List<Future<Integer>> resultList;

		resultList = this.executor.invokeAll(taskList);

		for (int i = 0; i < resultList.size(); i++) {
			Future<Integer> result = resultList.get(i);
			result.get(); //dev
		}
	}

	public void runEnsembleTrainingTask(ParallelTasks executor, Ensemble ensemble, Dataset trainData)
			throws InterruptedException, ExecutionException {
		int k = ensemble.getSize();
		List<TrainingTask> tasks = new ArrayList<TrainingTask>(k);
		List<Future<Integer>> resultList;

		for (int i = 0; i < k; i++) {
			tasks.add(executor.new TrainingTask(ensemble.getModel(i), trainData, i));
		}

		resultList = executor.executor.invokeAll(tasks);

		for (Future<Integer> result : resultList) {
			result.get(); //dev
		}
	}

	public void runEnsembleTestingTasks(ParallelTasks executor, Ensemble ensemble, Dataset testData)
			throws InterruptedException, ExecutionException {
		int k = ensemble.getSize();
		List<TestingTask> tasks = new ArrayList<TestingTask>(k);
		List<Future<Integer>> resultList;

		for (int i = 0; i < k; i++) {
			tasks.add(executor.new TestingTask(ensemble.getModel(i), testData, i));
		}

		resultList = executor.executor.invokeAll(tasks);

		for (Future<Integer> result : resultList) {
			result.get(); //dev
		}
	}

	public int[] runPredictionTasks(Classifier model, Dataset testData)
			throws InterruptedException, ExecutionException {
		int testDataSize = testData.size();
		List<PredictClassPerQueryRangeTask> tasks = new ArrayList<PredictClassPerQueryRangeTask>(this.numThreads);
		List<Future<Integer>> resultList;
		int[] predictedClasses = new int[testDataSize];
		int i, start, end = 0;
		int partitionSize = testDataSize / this.numThreads;
		int reminder = testDataSize % this.numThreads;

		if (partitionSize > 0){
			// <= adds an task to handle the reminder
			for (i = 0; end < testDataSize; i++) {
				start = i * partitionSize;
				end = i * partitionSize + partitionSize;
				if (end >= testDataSize){
					end = testDataSize;
				}
				tasks.add(new PredictClassPerQueryRangeTask(model, testData, i, start, end, predictedClasses));
			}
		}else{
			// for case when #threads > testSize
			tasks.add(new PredictClassPerQueryRangeTask(model, testData, 0, 0, reminder, predictedClasses));
		}

		// extra verification -- for debugging
		// START
		ArrayList<Integer> assignedItems =  new ArrayList<>(testDataSize);
		for (int j = 0; j < tasks.size(); j++) {
			PredictClassPerQueryRangeTask task = tasks.get(j);
			for (int k = task.start; k < task.end; k++) {
				assignedItems.add(k);
			}
		}
		if (assignedItems.size() != testDataSize){
			System.out.println(assignedItems.size());
			System.out.println(tasks.size());
			System.out.println(tasks.get(tasks.size()-1));
			throw new RuntimeException("CRITICAL ERROR in runPredictionTasks: bug with allocating test queries to threads");
		}
		// END


		resultList = this.executor.invokeAll(tasks);

		for (Future<Integer> result : resultList) {
			result.get(); //dev
		}

		return predictedClasses;
	}

	public class TrainingTask implements Callable<Integer> {
		protected Classifier model;
		protected Dataset trainData;
		protected Indexer trainIndices;
		protected DebugInfo debugInfo;
		protected AtomicInteger taskID;

		//dev
		public int verbosity;

		public TrainingTask(Classifier model, Dataset trainData, int taskID) {
			this.model = model;
			this.trainData = trainData;
			this.taskID = new AtomicInteger(taskID);
		}

		public TrainingTask(Classifier model, Dataset trainData, Indexer trainIndices, DebugInfo debugInfo, int taskID) {
			this.model = model;
			this.trainData = trainData;
			this.trainIndices = trainIndices;
			this.debugInfo = debugInfo;
			this.taskID = new AtomicInteger(taskID);
		}

		@Override
		public Integer call() throws Exception {

			if (trainIndices == null){
				model.fit(trainData);
			}else{
				model.fit(trainData, trainIndices, debugInfo);
			}


			if (verbosity > 0) {
				System.out.print(taskID.get() + ".");
				if (verbosity > 2) {
					PrintUtilities.printMemoryUsage(true);
					// TODO check an atomic integer
//						if ((i+1) % 20 == 0) {
//							System.out.println();
//						}
				}
			}

			return taskID.get();
		}

	}

	public class TestingTask implements Callable<Integer> {
		protected Classifier model;
		protected Dataset testData;
		protected Indexer testIndices;
		protected DebugInfo debugInfo;
		protected AtomicInteger taskID;
		protected boolean getProbability;
		protected ClassifierResult result;

		//dev
		public int verbosity;

		public TestingTask(Classifier model, Dataset testData, int taskID) {
			this.model = model;
			this.testData = testData;
			this.taskID = new AtomicInteger(taskID);
			this.getProbability = false;
		}

		public TestingTask(Classifier model, Dataset testData, Indexer testIndices, DebugInfo debugInfo,
				boolean getProbability, int taskID) {
			this.model = model;
			this.testData = testData;
			this.testIndices = testIndices;
			this.debugInfo = debugInfo;
			this.taskID = new AtomicInteger(taskID);
			this.getProbability = getProbability;
		}

		@Override
		public Integer call() throws Exception {

			if (testIndices == null){
				if (getProbability){
					result = model.predictProba(testData);
				}else{
					result = model.predict(testData);
				}
			}else{
				if (getProbability){
					result = model.predictProba(testData, testIndices, debugInfo);
				}else{
					result = model.predict(testData, testIndices, debugInfo);
				}
			}


			if (verbosity > 0) {
				System.out.print(taskID.get() + ".");
				if (verbosity > 2) {
					PrintUtilities.printMemoryUsage(true);
					// TODO check an atomic integer
//						if ((i+1) % 20 == 0) {
//							System.out.println();
//						}
				}
			}

			return taskID.get();
		}

	}

	public class PredictClassPerQueryRangeTask implements Callable<Integer> {
		protected Classifier model;
		protected Dataset testData;
		protected Indexer testIndices;
		protected DebugInfo debugInfo;
		protected AtomicInteger taskID;
		protected int[] predictedClasses;
		protected int start;
		protected int end;

		//dev
		public int verbosity;

		public PredictClassPerQueryRangeTask(Classifier model, Dataset testData, int taskID, int start, int end,
									  int[] predictedClasses) {
			this.model = model;
			this.testData = testData;
			this.taskID = new AtomicInteger(taskID);
			this.predictedClasses = predictedClasses;
			this.start = start;
			this.end = end;
		}

		public PredictClassPerQueryRangeTask(Classifier model, Dataset testData, Indexer testIndices, DebugInfo debugInfo,
						    int taskID, int start, int end, int[] predictedClasses) {
			this.model = model;
			this.testData = testData;
			this.testIndices = testIndices;
			this.debugInfo = debugInfo;
			this.taskID = new AtomicInteger(taskID);
			this.predictedClasses = predictedClasses;
			this.start = start;
			this.end = end;
		}

		public String toString(){
			return "Task "+taskID.get()+": start: " + start + ", end: " + end;
		}

		@Override
		public Integer call() throws Exception {

			if (end > predictedClasses.length){
				end = predictedClasses.length;
			}

			if (start >= end){
				throw new Exception("starting index should be less than end");
			}

			if (testIndices == null){
				for (int i = start; i < end; i++) {
//					predictedClasses[i] = model.predict(testData.getSeries(i));
					predictedClasses[i] = model.predict(i);
				}
			}else{
				for (int i = start; i < end; i++) {
//					predictedClasses[i] = model.predict(testData.getSeries(testIndices.getIndex()[i]));
					predictedClasses[i] = model.predict(i);
				}
			}

			if (verbosity > 0) {
				System.out.print(taskID.get() + ".");
				if (verbosity > 2) {
					PrintUtilities.printMemoryUsage(true);
					// TODO check an atomic integer
//						if ((i+1) % 20 == 0) {
//							System.out.println();
//						}
				}
			}

			return taskID.get();
		}

	}

}
