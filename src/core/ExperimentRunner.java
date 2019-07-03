package core;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import datasets.TSDataset;
import trees.ProximityForest;
import util.PrintUtilities;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ExperimentRunner {
	
	TSDataset train_data;
	TSDataset test_data;
	private static String csvSeparatpr = ",";
	
	private MultiThreadedTasks parallel_tasks;
	
	public ExperimentRunner(){
		
		parallel_tasks = new MultiThreadedTasks();
	}
	
	public void run() throws Exception {
		
		//read data files
		//we assume no header in the csv files, and that class label is in the first column, modify if necessary
		TSDataset train_data_original = 
				CSVReader.readCSVToTSDataset(AppContext.training_file, AppContext.csv_has_header, 
						AppContext.target_column_is_first, csvSeparatpr, AppContext.verbosity);
		TSDataset test_data_original = 
				CSVReader.readCSVToTSDataset(AppContext.testing_file, AppContext.csv_has_header, 
						AppContext.target_column_is_first, csvSeparatpr, AppContext.verbosity);
		
		
		/**
		 * We do some reordering of class labels in this implementation, 
		 * this is not necessary if HashMaps are used in some places in the algorithm,
		 * but since we used an array in cases where we need HashMaps to store class distributions maps, 
		 * I had to to keep class labels contiguous.
		 * 
		 * I intend to change this later, and use a library like Trove, Colt or FastUtil which implements primitive HashMaps
		 * After thats done, we will not be reordering class here.
		 * 
		 * update:1/7/2019 -- we dont do reordering now
		 */
		train_data = train_data_original; //train_data_original.reorder_class_labels(null);
		test_data = test_data_original; //test_data_original.reorder_class_labels(train_data._get_initial_class_labels());
		
		
		AppContext.setTraining_data(train_data);
		AppContext.setTesting_data(test_data);
		AppContext.updateClassDistribution(train_data, test_data);
				
		//allow garbage collector to reclaim this memory, since we have made copies with reordered class labels
		train_data_original = null;
		test_data_original = null;
		System.gc();

		//setup environment
		File training_file = new File(AppContext.training_file);
		String datasetName = training_file.getName().replaceAll("_TRAIN.txt", "");	//this is just some quick fix for UCR datasets
		AppContext.setDatasetName(datasetName);
		
		if (AppContext.verbosity > 0) {
			PrintUtilities.printDatasetInfo();
//			System.out.println();
		}

		//if we need to shuffle
		if (AppContext.shuffle_dataset) {
			System.out.println("Shuffling the training set...");
			train_data.shuffle(AppContext.rand_seed);	//NOTE seed
			test_data.shuffle(AppContext.rand_seed);
		}
		
		String outputFilePrefix = createOutPutFile(datasetName);	//giving same prefix for all repeats so thats easier to group using pandas
		AppContext.currentOutputFilePrefix = outputFilePrefix;
				
		for (int i = 0; i < AppContext.num_repeats; i++) {
			
			if (AppContext.verbosity > 0) {
				System.out.println("======================================== Repetition No: " + (i+1) + " (" +datasetName+ ") ========================================");
				
				if (AppContext.verbosity > 1) {
					System.out.println("Threads: MaxPool=" + parallel_tasks.getThreadPool().getMaximumPoolSize() 
							+ ", Active: " + parallel_tasks.getThreadPool().getActiveCount()
							+ ", Total: " + Thread.activeCount());					
				}


			}else if (AppContext.verbosity == 0 && i == 0){
				System.out.println("#,Repetition, Dataset, Accuracy, TrainTime(ms), TestTime(ms), AvgDepthPerTree");
			}

			//create model
			ProximityForest forest = new ProximityForest(i, parallel_tasks);
			
			//train model
			if (AppContext.boosting) {
				forest.train(train_data);
			} else {
				forest.train_parallel(train_data);
			}
//			Thread.sleep(500);

			//test model
			ProximityForestResult result = forest.test_parallel_thread_per_model(test_data);

			//print and export resultS
			result.printResults(datasetName, i, "");
			
			if (AppContext.verbosity > 1) {
				System.out.println("Threads: MaxPool=" + parallel_tasks.getThreadPool().getMaximumPoolSize() 
						+ ", Active: " + parallel_tasks.getThreadPool().getActiveCount()
						+ ", Total: " + Thread.activeCount());				
			}

			
			//export level is integer because I intend to add few levels in future, each level with a higher verbosity
			boolean last_record = i == (AppContext.num_repeats-1);
			if (AppContext.export_level == 1) {
				result.exportJSON(datasetName, i, outputFilePrefix, last_record);			
			}else if (AppContext.export_level == 2) {
				result.exportCSV(datasetName, i, outputFilePrefix);		
			}else if (AppContext.export_level == 3) {
				//use this -- might remove others in future
				result.exportJSON(datasetName, i, outputFilePrefix, last_record);			
				result.exportCSV(datasetName, i, outputFilePrefix);		
				result.exportSummaryCSV(datasetName, i, outputFilePrefix);		
			}
			
			if (AppContext.garbage_collect_after_each_repetition) {
				System.gc();
			}
			
			if (AppContext.verbosity > 0) {
				System.out.println();
			}
			
		}
		
		if (AppContext.verbosity > 2) {
			System.out.println("\nShutting down the executor service");
		}
		
		parallel_tasks.getExecutor().shutdown();
		parallel_tasks.getExecutor().awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);	//just wait forever
				
	
	}

	private String createOutPutFile(String datasetName) throws IOException {
		AppContext.experiment_timestamp = LocalDateTime.now();
		String timestamp = AppContext.experiment_timestamp.format(DateTimeFormatter.ofPattern(AppContext.FILENAME_TIMESTAMP_FORMAT));			
		
		String fileName = timestamp + "_" + datasetName;
				
		return fileName;
	}

}
