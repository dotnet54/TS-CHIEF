package trees;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import application.AppConfig;
import application.AppConfig.ParamSelection;
import application.AppConfig.TransformLevel;
import core.*;
import core.exceptions.NotImplementedException;
import core.exceptions.NotSupportedException;
import core.threading.MultiThreadedTasks;
import data.containers.ExperimentResultCollector;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import data.timeseries.TimeSeries;
import data.dev.DataCache;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.splitters.boss.dev.BossTransformContainer;
import trees.splitters.it.InceptionTimeDataStore;
import trees.splitters.rt.RocketTreeDataStore;
import trees.splitters.st.dev.ShapeletTransformDataStore;
import util.PrintUtilities;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class TSCheifForest implements Serializable, Classifier, Ensemble {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1183368028217094381L;
	protected transient ExperimentResultCollector resultCollector;
	protected transient TSChiefForestResult result;
	protected int forest_id;
	protected TSCheifTree trees[];

	private transient MultiThreadedTasks parallel_tasks;
	
	//TODO REFACTOR to use BossTransformDataStore
	protected transient HashMap<String,TransformContainer> transforms;
	protected transient DataCache forestDataCache;
	
	//DEV
	public transient ShapeletTransformDataStore stDataStore;
	public transient InceptionTimeDataStore itDataStore;
	public transient RocketTreeDataStore rtDataStore;

	protected boolean isFitted;

	public TSCheifForest(int forest_id, MultiThreadedTasks parallel_tasks) {
		this.forest_id = forest_id;
		this.resultCollector = new ExperimentResultCollector(this);
		this.resultCollector.repetition_id = AppConfig.repeat_guids.get(forest_id);

		this.trees = new TSCheifTree[AppConfig.num_trees];
		
		for (int i = 0; i < AppConfig.num_trees; i++) {
			trees[i] = new TSCheifTree(i, this);
		}

		this.parallel_tasks = parallel_tasks;
		
		transforms = new HashMap<String, TransformContainer>();
		forestDataCache = new DataCache();
	
	}

	@Override
	public ClassifierResult fit(Dataset trainData) throws Exception {
		return this.fit(trainData, null, null);
	}

	@Override
	public ClassifierResult fit(Dataset trainData, Indexer trainIndices, DebugInfo debugInfo) throws Exception {
		result = new TSChiefForestResult(this);
		resultCollector.startTimeTrain = System.nanoTime();

		if (AppConfig.verbosity == 1) {
			System.out.print("Forest Level Transformations (If needed): ...\n");
		}else if (AppConfig.verbosity > 1) {
			System.out.print("Before Transformations: ");
			PrintUtilities.printMemoryUsage();
		}


		//if boss splitter is enabled, precalculate AppContext.boss_num_transforms of transforms at the forest level
		if (AppConfig.boss_enabled) {

			if (AppConfig.boss_param_selection == ParamSelection.PreLoadedParams) {
				AppConfig.boss_preloaded_params = AppConfig.loadBossParams();
			}

			if (AppConfig.boss_transform_level == TransformLevel.Forest) {
				BossTransformContainer boss_transform_container =  new BossTransformContainer(AppConfig.boss_trasformations);
				boss_transform_container.fit(trainData);

				resultCollector.boss_transform_time = System.nanoTime() - resultCollector.startTimeTrain;

				transforms.put("boss", boss_transform_container);
			}else {
				// transformation will be done per tree -> check tree.train method
			}
		}

		if (AppConfig.tsf_enabled) {
//			TSFTransformContainer tsf_transform_container =  new TSFTransformContainer(AppConfig.tsf_trasformations);
//			tsf_transform_container.fit(trainData);
//			transforms.put("tsf", tsf_transform_container);
			throw new NotSupportedException();
		}

//		if (AppContext.rif_enabled) {
//			RIFTransformContainer rif_transform_container =  new RIFTransformContainer(AppContext.rif_trasformations);
//			rif_transform_container.fit(train_data);
//			transforms.put("rif", rif_transform_container);
//		}

		if (AppConfig.st_enabled) {
			stDataStore = new ShapeletTransformDataStore();
			stDataStore.initBeforeTrain(trainData);
		}

		if (AppConfig.it_enabled) {
			itDataStore = new InceptionTimeDataStore();
			itDataStore.initBeforeTrain(trainData);
		}

		if (AppConfig.rt_enabled) {
			System.out.println("TODO.......................");
		}

		if (AppConfig.verbosity > 1) {
			System.out.print("After Transformations: boss trasnform time = " + resultCollector.boss_transform_time /1e9 + "s, ");
			PrintUtilities.printMemoryUsage();
		}


		List<Callable<Integer>> training_tasks = new ArrayList<Callable<Integer>>();
		List<Future<Integer>> training_results;

		int trees_per_thread = (AppConfig.num_trees / AppConfig.num_threads) + 1;

		for (int i = 0; i < AppConfig.num_threads; i++) {
			int end = Math.min(AppConfig.num_trees, (i*trees_per_thread)+trees_per_thread);
			Callable<Integer> training_task = parallel_tasks.new TrainingTask(trees,
					i*trees_per_thread, end, trainData);
			training_tasks.add(training_task);
		}

		training_results = parallel_tasks.getExecutor().invokeAll(training_tasks);

		//important to catch training exceptions
		for (int i = 0; i < training_results.size(); i++) {
			Future<Integer> future_int = training_results.get(i);
			try {
				future_int.get();//TODO return tree index that finished training
			} catch (ExecutionException ex) {
				ex.getCause().printStackTrace();
				throw new Exception("Error During Training...");
			}
		}

		resultCollector.endTimeTrain = System.nanoTime();
		resultCollector.elapsedTimeTrain = resultCollector.endTimeTrain - resultCollector.startTimeTrain;

		if (AppConfig.verbosity > 0) {
			System.out.print("\n");
			//		System.gc();
			if (AppConfig.verbosity > 2) {
				System.out.print("Testing: ");
				PrintUtilities.printMemoryUsage();
			}
		}

		isFitted = true;
		return result;
	}


	@Override
	public ClassifierResult predict(Dataset testData) throws Exception {
		return this.predict(testData,null, null);
	}

	@Override
	public ClassifierResult predict(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception {
		resultCollector.startTimeTest = System.nanoTime();
		result.allocateForPredictionResults(testData);

		//TODO -> multi threaded transforms?
		//if we need to do a forest level transformation do it here

		//end transforms

		//if boss splitter is enabled, precalculate AppContext.boss_num_transforms of transforms at the forest level
//		if (Arrays.asList(AppContext.enabled_splitters).contains(SplitterType.BossSplitter) & AppContext.boss_transform_at_forest_level) {

		//TODO not precomputing test transformations, going to do this on the fly at node level

//			BossTransformContainer boss_transform_container =  (BossTransformContainer) transforms.get("boss");
//			boss_transform_container.transform(test_data);
//		}

		if (AppConfig.it_enabled) {
			itDataStore.initBeforeTest(testData);
		}

		//allocate trees to threads, each tree will handle at least trees/thread. (the remainder is distributed across threads)
		TIntObjectMap<List<Integer>> tree_indices_per_thread = new TIntObjectHashMap<>(AppConfig.num_threads);
		for (int i = 0; i < trees.length; i++) {
			int thread_id = i % AppConfig.num_threads;
			tree_indices_per_thread.putIfAbsent(thread_id, new ArrayList<Integer>());
			tree_indices_per_thread.get(thread_id).add(i);
		}

		//TODO TEST a dd assertion here to check if trees are divided to threads correctly

		//create tasks
		List<Callable<Integer>> testing_tasks = new ArrayList<Callable<Integer>>();
		for (int i = 0; i < AppConfig.num_threads; i++) {
			if (tree_indices_per_thread.get(i) != null) {	//to handle case #trees < #threads
				Callable<Integer> testing_task = parallel_tasks.new TestingPerModelTask(this,
						tree_indices_per_thread.get(i), testData, result);
				testing_tasks.add(testing_task);
			}
		}

//		try {
		List<Future<Integer>> test_results;
		test_results = parallel_tasks.getExecutor().invokeAll(testing_tasks);
		if (AppConfig.verbosity > 2) {
			System.out.println("after  -- parallel_tasks.getExecutor().invokeAll(testing_tasks): ");
		}



//			//TODO exceptions inside invoke all is not handled here
//		} catch (InterruptedException ex) {
//			   ex.getCause().printStackTrace();
//			   throw new Exception("Error During Testing...");
//		} catch (Exception ex) {
//			   ex.getCause().printStackTrace();
//			   throw new Exception("Error During Testing...");
//		}


		//HOTFIX this helps catch exceptions inside invoke all
		for (int i = 0; i < test_results.size(); i++) {
			Future<Integer> future_int = test_results.get(i);
			try {
				future_int.get();
//				result.correct = result.correct + future_int.get().intValue();
			} catch (ExecutionException ex) {
				ex.getCause().printStackTrace();
				throw new Exception("Error During Testing...");
			}
		}

		//evaluate predictions
		evaluate(result, testData);

		resultCollector.endTimeTest = System.nanoTime();
		resultCollector.elapsedTimeTest = resultCollector.endTimeTest - resultCollector.startTimeTest;

		if (AppConfig.verbosity > 0) {
			System.out.println();
			if (AppConfig.verbosity > 2) {
				System.out.println("Testing Completed: ");
			}
		}



		resultCollector.errors = testData.size() - resultCollector.correct;
		resultCollector.accuracy  = ((double) resultCollector.correct) / testData.size();
		resultCollector.error_rate = 1 - resultCollector.accuracy;

		return resultCollector;
	}


	@Override
	public ClassifierResult predictProba(Dataset testData) throws Exception {
		return this.predictProba(testData, null, null);
	}

	@Override
	public ClassifierResult predictProba(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public int predict(TimeSeries query) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public int predict(int queryIndex) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public double predictProba(TimeSeries query) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public double score(Dataset trainData, Dataset testData) throws Exception {
		return this.score(trainData, testData, null);
	}

	@Override
	public double score(Dataset trainData, Dataset testData, DebugInfo debugInfo) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public Options getParams() {
		throw new core.exceptions.NotImplementedException();
	}

	@Override
	public void setParams(Options params) {
		throw new NotImplementedException();
	}

	@Override
	public ClassifierResult getTrainResults() {
		return result;
	}

	@Override
	public ClassifierResult getTestResults() {
		return null;
	}

	public void evaluate(TSChiefForestResult forestResult, Dataset test_data) throws Exception {
		Integer[] labels = new Integer[trees.length];
		TSChiefTreeResult[] treeResult = forestResult.getBaseModelResults();

		TIntIntMap num_votes = new TIntIntHashMap();
//		TIntDoubleMap num_votes_d = new TIntDoubleHashMap();
		TIntList max_voted_classes = new TIntArrayList();
		
		ArrayList<Integer> predicted_labels = new ArrayList<>(test_data.size());	//storing to only to export to a file

		for (int i = 0; i < test_data.size(); i++) {
			for (int j = 0; j < treeResult.length; j++) {
				int[] predictedLabels = treeResult[j].getPredctictedLabels();
				labels[j] = predictedLabels[i];
			}
			
			Integer pred_label;
			pred_label = majority_vote(labels, num_votes, max_voted_classes);
			predicted_labels.add(pred_label);
			
			if (pred_label.equals(test_data.getSeries(i).label())) { //TODO == vs equal
				resultCollector.correct = resultCollector.correct + 1;
			}
			
		}
		
		if (AppConfig.export_level > 0) {
			resultCollector.exportPredictions(AppConfig.currentOutputFilePrefix + "_r" + forest_id
					+ "_eid" + AppConfig.current_experiment_id + "_rid" + resultCollector.repetition_id, treeResult, predicted_labels, test_data);
		}
		
	}
	
	
	private Integer majority_vote(Integer[] labels, TIntIntMap num_votes,  TIntList max_voted_classes) {
		//ASSUMES CLASSES HAVE BEEN REMAPPED, start from 0
		int label;
		int max_vote_count = -1;
		int temp_count = 0;

		num_votes.clear();
		max_voted_classes.clear();

		for (int i = 0; i < labels.length; i++) {
			label = labels[i];
			num_votes.adjustOrPutValue(label, 1, 1);
		}
		
		for (int key : num_votes.keys()) {
			temp_count = num_votes.get(key);
			
			if (temp_count > max_vote_count) {
				max_vote_count = temp_count;
				max_voted_classes.clear();
				max_voted_classes.add(key);
			}else if (temp_count == max_vote_count) {
				max_voted_classes.add(key);
			}
		}

		int r = AppConfig.getRand().nextInt(max_voted_classes.size());
		
		//collecting some stats
		if (max_voted_classes.size() > 1) {
//			this.result.majority_vote_match_count++;
		}
		
		return max_voted_classes.get(r);
	}


	public HashMap<String, TransformContainer> getTransforms() {
		return transforms;
	}

	
	public TSCheifTree[] getTrees() {
		return this.trees;
	}
	
	public TSCheifTree getTree(int i) {
		return this.trees[i];
	}

	public ExperimentResultCollector getResultSet() {
		return resultCollector;
	}

	public ExperimentResultCollector getExpResultCollection() {
				
		return resultCollector;
	}

	public int getForestID() {
		return forest_id;
	}

	public void setForestID(int forest_id) {
		this.forest_id = forest_id;
	}


	@Override
	public int getSize() {
		return this.trees.length;
	}

	@Override
	public Classifier[] getModels() {
		return this.trees;
	}

	@Override
	public Classifier getModel(int i) {
		return this.trees[i];
	}
}
