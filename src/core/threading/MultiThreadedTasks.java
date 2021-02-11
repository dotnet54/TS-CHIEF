package core.threading;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import application.AppConfig;
import core.Classifier;
import core.ClassifierResult;
import data.timeseries.Dataset;
import trees.TSCheifForest;
import trees.TSCheifTree;
import trees.TSChiefForestResult;
import trees.TSChiefTreeResult;
import util.PrintUtilities;

public class MultiThreadedTasks {

	private ExecutorService executor;
	private int pool_size;

	public MultiThreadedTasks() {

		if (AppConfig.num_threads == 0) { // auto
			AppConfig.num_threads = Runtime.getRuntime().availableProcessors();
		}

		setExecutor(Executors.newFixedThreadPool(AppConfig.num_threads));

		// this is important for slurm jobs because
		// Runtime.getRuntime().availableProcessors() does not equal SBATCH argument
		// cpus-per-task
		if (executor instanceof ThreadPoolExecutor) {
			pool_size = ((ThreadPoolExecutor) executor).getMaximumPoolSize();
		}

//		System.out.println("Using " + pool_size + " threads (CPUs=" + Runtime.getRuntime().availableProcessors() + ")");

	}

//	public ExecutorService createExecutor(int num_threads) {
//		return Executors.newFixedThreadPool(num_threads);
//	}

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

	public class TrainingTask implements Callable<Integer> {
		TSCheifTree[] trees;
		Dataset train;
		int start;
		int end;

		public TrainingTask(TSCheifTree[] trees, int start, int end, Dataset train) {
			this.trees = trees;
			this.start = start;
			this.end = end;
			this.train = train;
		}

		@Override
		public Integer call() throws Exception {
			
			int indices[];
			for (int k = start; k < end; k++) {
				trees[k].fit(train);

				if (AppConfig.verbosity > 0) {
					System.out.print(k + ".");
					if (AppConfig.verbosity > 2) {
						PrintUtilities.printMemoryUsage(true);

						// TODO check an atomic integer
//						if ((i+1) % 20 == 0) {
//							System.out.println();
//						}
					}
				}
			}

			return null;
		}

	}
	
//	public class TestingTask implements Callable<Integer> {
//		ProximityForest forest;
//		TSDataset test_data;
//		int start;
//		int end;
//
//		TIntIntMap num_votes;
//		List<Integer> max_voted_classes;
//
//		public TestingTask(ProximityForest forest, int start, int end, TSDataset test) {
//			this.forest = forest;
//			this.start = start;
//			this.end = end;
//			this.test_data = test;
//		}
//
//		@Override
//		public Integer call() throws Exception {
//			int correct = 0;
//
//			num_votes = new TIntIntHashMap(AppContext.num_classes);
//			max_voted_classes = new ArrayList<Integer>();
//
//			int predicted_class;
//			int actual_class;
//			int size = test_data.size();
//
//			for (int i = start; i < end; i++) {
//				actual_class = test_data.get_class(i);
//				predicted_class = predict(test_data.get_series(i));
//				if (actual_class == predicted_class) {
//					correct++;
//				}
//
////				if (AppContext.verbosity > 0) {
////					if (i % AppContext.print_test_progress_for_each_instances == 0) {
//				System.out.print(".");
////					}				
////				}
//			}
//
//			return correct;
//		}
//
//		private Integer predict(TimeSeries query) throws Exception {
//			int label;
//			int max_vote_count = -1;
//			int temp_count = 0;
//
//			num_votes.clear(); // TODO
//			max_voted_classes.clear();
//
//			for (int i = 0; i < forest.getTrees().length; i++) {
//				label = forest.getTree(i).predict(query);
//
//				num_votes.put(label, num_votes.get(label) + 1);
//			}
//
////				System.out.println("vote counting using uni dist");
//
//			for (int key : num_votes.keys()) {
//				temp_count = num_votes.get(key);
//
//				if (temp_count > max_vote_count) {
//					max_vote_count = temp_count;
//					max_voted_classes.clear();
//					max_voted_classes.add(key);
//				} else if (temp_count == max_vote_count) {
//					max_voted_classes.add(key);
//				}
//			}
//
//			int r = AppConfig.getRand().nextInt(max_voted_classes.size());
//
//			// collecting some stats
//			if (max_voted_classes.size() > 1) {
////				this.result.majority_vote_match_count++;
//			}
//
//			return max_voted_classes.get(r);
//		}
//
//	}

	public class TestingPerModelTask implements Callable<Integer> {
		TSCheifForest forest;
		Dataset test_data;
		List<Integer> model_indices;
		TSChiefForestResult result;

		public TestingPerModelTask(TSCheifForest ensemble, List<Integer> base_model_indices, Dataset test,
								   TSChiefForestResult result) {
			this.forest = ensemble;
			this.model_indices = base_model_indices;
			this.test_data = test;
			this.result = result;
		}

		@Override
		public Integer call() throws Exception {
			TSChiefTreeResult result;

			for (int i = 0; i < model_indices.size(); i++) {
				int model_index = model_indices.get(i);
				result = (TSChiefTreeResult) forest.getTree(model_index).predict(test_data);
				this.result.addBaseModelResult(model_index, result);
				if (AppConfig.verbosity > 0) {
					System.out.print(".");
				}
			}

			return null;
		}

//		private Integer predict(TimeSeries query) throws Exception {
//			int label;
//			int max_vote_count = -1;
//			int temp_count = 0;
//			
//			num_votes.clear(); //TODO
//			max_voted_classes.clear();
//
//			for (int i = 0; i < forest.getTrees().length; i++) {
//				label = forest.getTree(i).predict(query);
//				
//				num_votes.put(label, num_votes.get(label) + 1);
//			}
//			
////				System.out.println("vote counting using uni dist");
//			
//			for (int key : num_votes.keys()) {
//				temp_count = num_votes.get(key);
//				
//				if (temp_count > max_vote_count) {
//					max_vote_count = temp_count;
//					max_voted_classes.clear();
//					max_voted_classes.add(key);
//				}else if (temp_count == max_vote_count) {
//					max_voted_classes.add(key);
//				}
//			}
//
//			
//			int r = AppConfig.getRand().nextInt(max_voted_classes.size());
//			
//			//collecting some stats
//			if (max_voted_classes.size() > 1) {
////				this.result.majority_vote_match_count++;
//			}
//			
//			return max_voted_classes.get(r);
//		}

	}

}
