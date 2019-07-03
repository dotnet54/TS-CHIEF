package core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import datasets.TSDataset;
import datasets.TimeSeries;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import trees.ProximityForest;
import trees.ProximityForest.Predictions;
import trees.ProximityTree;
import util.PrintUtilities;

public class MultiThreadedTasks {

	private ExecutorService executor;
	private int pool_size;

	public MultiThreadedTasks() {

		if (AppContext.num_threads == 0) { // auto
			AppContext.num_threads = Runtime.getRuntime().availableProcessors();
		}

		setExecutor(Executors.newFixedThreadPool(AppContext.num_threads));

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
		ProximityTree[] trees;
		TSDataset train;
		int start;
		int end;

		public TrainingTask(ProximityTree[] trees, int start, int end, TSDataset train) {
			this.trees = trees;
			this.start = start;
			this.end = end;
			this.train = train;
		}

		@Override
		public Integer call() throws Exception {
			
			int indices[];
			for (int k = start; k < end; k++) {
				
				indices = sample_indices(train); // if bagging is enabled this function will sample indices with replacement, bagging not implemented yet
				
				trees[k].train(train, indices);

				if (AppContext.verbosity > 0) {
					System.out.print(k + ".");
					if (AppContext.verbosity > 2) {
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

	//TODO bagging not implemented yet
	public int[] sample_indices(TSDataset train) {
		int[] sample = new int[train.size()];
		
		for (int i = 0; i < sample.length; i++) {
			sample[i] = i; // just add everything
		}
		return sample;
	}

	public class TestingPerModelTask implements Callable<Integer> {
		ProximityForest forest;
		TSDataset test_data;
		List<Integer> model_indices;
		Predictions[] predictions; // must be initialized by the caller, this array is shared by multiple threads,
									// size must equal to no. of models

		public TestingPerModelTask(ProximityForest ensemble, List<Integer> base_model_indices, TSDataset test,
				Predictions[] predictions) {
			this.forest = ensemble;
			this.model_indices = base_model_indices;
			this.test_data = test;
			this.predictions = predictions;
		}

		@Override
		public Integer call() throws Exception {

			for (int i = 0; i < model_indices.size(); i++) {
				int index = model_indices.get(i);
				predictions[index] = forest.getTree(index).predict(test_data);
				if (AppContext.verbosity > 0) {
					System.out.print(".");
				}

			}

			return null;
		}

	}

}
