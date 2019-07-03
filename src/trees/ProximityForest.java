package trees;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.org.apache.bcel.internal.generic.CPInstruction;

import core.AppContext;
import core.AppContext.SplitterType;
import core.AppContext.TransformLevel;
import core.MultiThreadedTasks;
import core.ProximityForestResult;
import datasets.BossDataset;
import datasets.DataStore;
import datasets.TSDataset;
import datasets.TimeSeries;
import dev.BossTransformContainer;
import dev.RIFTransformContainer;
import dev.TSFTransformContainer;
import dev.TransformContainer;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.ProximityForest.Predictions;
import util.PrintUtilities;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ProximityForest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1183368028217094381L;
	protected transient ProximityForestResult result;
	protected int forest_id;
	protected ProximityTree trees[];
	public String prefix;
	
//	int[] num_votes;
//	List<Integer> max_voted_classes;
	
	private transient MultiThreadedTasks parallel_tasks;
	protected transient HashMap<String,TransformContainer> transforms;
	protected transient DataStore forestDataStore;

	public ProximityForest(int forest_id, MultiThreadedTasks parallel_tasks) {
		this.forest_id = forest_id;
		this.result = new ProximityForestResult(this);
		this.result.repetition_id = AppContext.repeat_guids.get(forest_id);

		this.trees = new ProximityTree[AppContext.num_trees];
		
		for (int i = 0; i < AppContext.num_trees; i++) {
			trees[i] = new ProximityTree(i, this);
		}

		this.parallel_tasks = parallel_tasks;
		
		transforms = new HashMap<String, TransformContainer>();
		forestDataStore = new DataStore();
	
	}
	
	//stores predictions for a single model
	public class Predictions{
//		Model model;
//		public int[] correct;	//TODO atomic integer?
		public AtomicInteger correct;
		public int[] predicted_labels;
//		public double [][] prdicted_distribution;	//TODO class->prob //int class?
		
		public Predictions(int test_size) {
			correct = new AtomicInteger();
			predicted_labels = new int[test_size];
		}
		
		public String toString() {
			return correct.toString();
		}
	}
	
	public void train(TSDataset train_data) throws Exception {
		result.startTimeTrain = System.nanoTime();

		System.out.println("Boosting: " + AppContext.boosting);
		if (!AppContext.boosting ){ 				//Charlotte: original code
			for (int i = 0; i < AppContext.num_trees; i++) {
				trees[i].train(train_data);
				
				if (AppContext.verbosity > 0) {
					System.out.print(i+".");
					if (AppContext.verbosity > 2) {
						PrintUtilities.printMemoryUsage(true);	
						if ((i+1) % 20 == 0) {
							System.out.println();
						}
					}		
				}

			}	
		}
		else {										//Charlotte: With boosting
			//experimental 
		}
		
		result.endTimeTrain = System.nanoTime();
		result.elapsedTimeTrain = result.endTimeTrain - result.startTimeTrain;
		
		if (AppContext.verbosity > 0) {
			System.out.print("\n");				
		}
		
//		System.gc();
		if (AppContext.verbosity > 0) {
			PrintUtilities.printMemoryUsage();	
		}
	
	}
	
	public void train_parallel(TSDataset train_data) throws Exception {
		result.startTimeTrain = System.nanoTime();
		
		if (AppContext.verbosity == 1) {
			System.out.print("Forest Level Transformations (If needed): ...\n");
		}else if (AppContext.verbosity > 1) {
			System.out.print("Before Transformations: ");
			PrintUtilities.printMemoryUsage();
		}


		//if boss splitter is enabled, precalculate AppContext.boss_num_transforms of transforms at the forest level 
		if (AppContext.boss_enabled) {
			if (AppContext.boss_transform_level == TransformLevel.Forest) {
				BossTransformContainer boss_transform_container =  new BossTransformContainer(AppContext.boss_trasformations);
				boss_transform_container.fit(train_data);
				
				result.boss_transform_time = System.nanoTime() - result.startTimeTrain;
				
				transforms.put("boss", boss_transform_container);				
			}else {
				// transformation will be done per tree -> check tree.train method
			}
		}else if (AppContext.tsf_enabled) {
			TSFTransformContainer tsf_transform_container =  new TSFTransformContainer(AppContext.tsf_trasformations);
			tsf_transform_container.fit(train_data);
			transforms.put("tsf", tsf_transform_container);	
		}else if (AppContext.rif_enabled) {
			RIFTransformContainer rif_transform_container =  new RIFTransformContainer(AppContext.rif_trasformations);
			rif_transform_container.fit(train_data);
			transforms.put("rif", rif_transform_container);	
		}
		
		if (AppContext.verbosity > 1) {
			System.out.print("After Transformations: boss trasnform time = " + result.boss_transform_time /1e9 + "s, ");
			PrintUtilities.printMemoryUsage();
		}

		
		List<Callable<Integer>> training_tasks = new ArrayList<Callable<Integer>>();
		List<Future<Integer>> training_results;

		int trees_per_thread = (AppContext.num_trees / AppContext.num_threads) + 1;
		
		for (int i = 0; i < AppContext.num_threads; i++) {
			int end = Math.min(AppContext.num_trees, (i*trees_per_thread)+trees_per_thread);
			Callable<Integer> training_task = parallel_tasks.new TrainingTask(trees, 
					i*trees_per_thread, end, train_data);
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
	
		result.endTimeTrain = System.nanoTime();
		result.elapsedTimeTrain = result.endTimeTrain - result.startTimeTrain;
		
		if (AppContext.verbosity > 0) {
			System.out.print("\n");		
	//		System.gc();
			if (AppContext.verbosity > 2) {
				System.out.print("Testing: ");
				PrintUtilities.printMemoryUsage();	
			}			
		}


	
	}
	
	
	public ProximityForestResult test_parallel_thread_per_model(TSDataset test_data) throws Exception {
		result.startTimeTest = System.nanoTime();
		
		//TODO -> multi threaded transforms?
		//if we need to do a forest level transformation do it here
		
		//end transforms
		
		//if boss splitter is enabled, precalculate AppContext.boss_num_transforms of transforms at the forest level 
//		if (Arrays.asList(AppContext.enabled_splitters).contains(SplitterType.BossSplitter) & AppContext.boss_transform_at_forest_level) {
			
			//TODO not precomputing test transformations, going to do this on the fly at node level
			
//			BossTransformContainer boss_transform_container =  (BossTransformContainer) transforms.get("boss");
//			boss_transform_container.transform(test_data);
//		}		

		Predictions[] predictions = new Predictions[trees.length];
		
		//allocate trees to threads, each tree will handle at least trees/thread. (the remainder is distributed across threads)
		TIntObjectMap<List<Integer>> tree_indices_per_thread = new TIntObjectHashMap<>(AppContext.num_threads);	
		for (int i = 0; i < trees.length; i++) {
			int thread_id = i % AppContext.num_threads;
			tree_indices_per_thread.putIfAbsent(thread_id, new ArrayList<Integer>());
			tree_indices_per_thread.get(thread_id).add(i);
		}
		
		//TODO TEST a dd assertion here to check if trees are divided to threads correctly
		
		//create tasks
		List<Callable<Integer>> testing_tasks = new ArrayList<Callable<Integer>>();
		for (int i = 0; i < AppContext.num_threads; i++) {
			if (tree_indices_per_thread.get(i) != null) {	//to handle case #trees < #threads
				Callable<Integer> testing_task = parallel_tasks.new TestingPerModelTask(this, tree_indices_per_thread.get(i), test_data, predictions);
				testing_tasks.add(testing_task);				
			}
		}
		
//		try {
			List<Future<Integer>> test_results;
			test_results = parallel_tasks.getExecutor().invokeAll(testing_tasks);
			if (AppContext.verbosity > 2) {
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
		evaluate(predictions, test_data);
		
		result.endTimeTest = System.nanoTime();
		result.elapsedTimeTest = result.endTimeTest - result.startTimeTest;
		
		if (AppContext.verbosity > 0) {
			System.out.println();
			if (AppContext.verbosity > 2) {
				System.out.println("Testing Completed: ");
			}
		}
		
		
		
		result.errors = test_data.size() - result.correct;		
		result.accuracy  = ((double) result.correct) / test_data.size();
		result.error_rate = 1 - result.accuracy;

        return result;
	}
	
	public void evaluate(Predictions predictions[], TSDataset test_data) throws Exception {
		Integer[] labels = new Integer[trees.length];
		
		TIntIntMap num_votes = new TIntIntHashMap();
		TIntDoubleMap num_votes_d = new TIntDoubleHashMap();

		TIntList max_voted_classes = new TIntArrayList();
		
		ArrayList<Integer> predicted_labels = new ArrayList<>(test_data.size());	//storing to only to export to a file

		for (int i = 0; i < test_data.size(); i++) {
			for (int j = 0; j < predictions.length; j++) {
				labels[j] = predictions[j].predicted_labels[i];
			}
			
			Integer pred_label;
			if (!AppContext.boosting) {				//Charlotte: no boosting => majority voting
				pred_label = majority_vote(labels, num_votes, max_voted_classes);
			}else {									//Charlotte: boosting => weighted vote using beta_t
//				pred_label = weighted_boosting_vote(labels, num_votes_d, max_voted_classes);
				pred_label = null;	//to disable boosting //TODO temp remove this
			}
			
			predicted_labels.add(pred_label);
			
			if (pred_label.equals(test_data.get_series(i).getLabel())) { //TODO == vs equal
				result.correct = result.correct + 1;
			}
			
		}
		
		if (AppContext.export_level > 0) {
			exportPredictions(AppContext.currentOutputFilePrefix + "_r" + forest_id 
					+ "_eid" + AppContext.current_experiment_id + "_rid" + result.repetition_id, predictions, predicted_labels, test_data);
		}
		
	}
	
	public void exportPredictions(String filePrefix, Predictions predictions[], List<Integer> predcicted_labels, TSDataset test_data) throws IOException {
		
		String timestamp = AppContext.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppContext.FILENAME_TIMESTAMP_FORMAT));
		
//		String fileName = AppContext.output_dir + File.separator + AppContext.current_experiment_id + "_"//+ timestamp + "_" //+ File.separator + AppContext.current_experiment_id 
//				+ forest_id + "_" + result.repetition_id + "_" + AppContext.getVersionString(true) + File.separator + "pred" + File.separator + filePrefix + ".pred.csv";
		
		String fileName = AppContext.output_dir + File.separator + AppContext.application_start_timestamp_formatted 
				+ File.separator + timestamp + "_" + AppContext.getDatasetName()
				+ File.separator + AppContext.currentOutputFilePrefix + "_r" + result.forest_id 
				+ "_eid" + AppContext.current_experiment_id + "_rid" + result.repetition_id + "_v"+ AppContext.getVersionString(true)
				+ ".pred.csv";		
		
		int size = test_data.size();
		
		if (AppContext.verbosity > 1) {
			System.out.println("\nwriting pred.csv file (detailed predictions): " + fileName);
		}
		
		
		File fileObj = new File(fileName);
		fileObj.getParentFile().mkdirs();
		fileObj.createNewFile();
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){		
		
		
			StringBuilder row = new StringBuilder();
			
			row.append("id");
			row.append(",");
			
			row.append("actual_label");
			row.append(",");
			
			row.append("predicted_label");
			row.append(",");
			
			row.append("correct");
			row.append(",");
			
			//predictions for each tree
			for (int k = 0; k < predictions.length; k++) {
				row.append("tree_" + k);
				row.append(",");
			}
			
			row.append("\n");
			bw.write(row.toString());
			row.setLength(0);
			
			for (int i = 0; i < size; i++) {
				row.append(i);
				row.append(",");
				
				row.append(test_data.get_class(i));
				row.append(",");
				
				row.append(predcicted_labels.get(i));
				row.append(",");
				
				if (predcicted_labels.get(i).equals(test_data.get_series(i).getLabel())) { //TODO == vs equal
					row.append(1);
				}else {
					row.append(0);
				}
				row.append(",");
				
				for (int k = 0; k < predictions.length; k++) {
					row.append(predictions[k].predicted_labels[i]);	//for kth tree, ith instance
					row.append(",");
				}
				row.append("\n");
				bw.write(row.toString());	
				row.setLength(0);
			}
		

			
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
	//		bw.close();
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

		int r = ThreadLocalRandom.current().nextInt(max_voted_classes.size());
		
		//collecting some stats
		if (max_voted_classes.size() > 1) {
//			this.result.majority_vote_match_count++;
		}
		
		return max_voted_classes.get(r);
	}

	public HashMap<String, TransformContainer> getTransforms() {
		return transforms;
	}

	
	public ProximityTree[] getTrees() {
		return this.trees;
	}
	
	public ProximityTree getTree(int i) {
		return this.trees[i];
	}

	public ProximityForestResult getResultSet() {
		return result;
	}

	public ProximityForestResult getForestStatCollection() {
		
		result.collateResults();
		
		return result;
	}

	public int getForestID() {
		return forest_id;
	}

	public void setForestID(int forest_id) {
		this.forest_id = forest_id;
	}




	
}
