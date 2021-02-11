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

public class AsyncTasks<T> {

	private ExecutorService executor;
	protected int numThreads;

	public AsyncTasks(int numThreads) {
		if (numThreads == 0) { // auto
			numThreads = Runtime.getRuntime().availableProcessors();
		}
		this.numThreads = numThreads;
		setExecutor(Executors.newFixedThreadPool(numThreads));
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

//	public class AsyncTaskResult{
//		protected AtomicInteger taskID;
//
//		public AsyncTaskResult(int taskID){
//			this.taskID = new AtomicInteger(taskID);
//		}
//
//		public AtomicInteger getTaskID() {
//			return taskID;
//		}
//	}
//
//	public class AsyncTask implements Callable<T>{
//		protected int taskID;
//		protected T result;
//
//		public AsyncTask(int taskID){
//			this.taskID = taskID;
//		}
//
//		@Override
//		public T call() throws Exception {
//			return result;
//		}
//	}

	public List<T> run(Callable<T>[] tasks) throws InterruptedException, ExecutionException {
		List<Callable<T>> taskList = Arrays.asList(tasks);
		List<Future<T>> futures;
		List<T> results = new ArrayList<>(tasks.length);
		futures = this.executor.invokeAll(taskList);

		for (int i = 0; i < futures.size(); i++) {
			Future<T> result = futures.get(i);
			results.add(result.get()); //dev
		}
		return results;
	}

}
