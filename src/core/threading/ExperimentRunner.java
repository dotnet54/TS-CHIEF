package core.threading;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import application.AppConfig;
import data.io.DataLoader;
import data.containers.ExperimentResultCollector;
import data.timeseries.Dataset;
import trees.TSCheifForest;
import util.PrintUtilities;
import util.Sampler;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ExperimentRunner {
	
	public Dataset trainData;
	public Dataset testData;
	
	private MultiThreadedTasks parallelTasks;
	private String datasetName;
	private String outputFilePrefix;
	
	public ExperimentRunner(){
		
		parallelTasks = new MultiThreadedTasks();
	}
	
	public void run() throws Exception {
		
		if (AppConfig.verbosity > 0) {
			System.out.println("Version: " + AppConfig.version + " (" + AppConfig.version_tag + "); Current Time: " 
					+ AppConfig.application_start_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.TIMESTAMP_FORMAT_LONG)));
		}	
		
		//read all input files
		//we assume no header in the csv files, and that class label is in the first column, modify if necessary
		trainData =  DataLoader.loadTrainingSet(AppConfig.training_file);
		//its important to pass training data reference to the test data loader so that it will use same label encoder
		testData =  DataLoader.loadTestingSet(AppConfig.testing_file, trainData);
		
		validateAndInitializeRunTimeConfiguration();
		
		if (AppConfig.verbosity > 0) {			
			PrintUtilities.printConfiguration();
			PrintUtilities.printDatasetInfo();
		}
		

		//if we need to shuffle
		if (AppConfig.shuffle_dataset) {
			System.out.println("Shuffling the training set...");
			trainData.shuffle(AppConfig.rand_seed);	//NOTE seed
			testData.shuffle(AppConfig.rand_seed);
		}
		
		
		for (int i = 0; i < AppConfig.num_repeats; i++) {
			
			if (AppConfig.verbosity > 0) {
				System.out.println("======================================== Repetition No: " + (i+1) + " (" +datasetName+ ") ========================================");
				
				if (AppConfig.verbosity > 1) {
					System.out.println("Threads: MaxPool=" + parallelTasks.getThreadPool().getMaximumPoolSize() 
							+ ", Active: " + parallelTasks.getThreadPool().getActiveCount()
							+ ", Total: " + Thread.activeCount());					
				}


			}else if (AppConfig.verbosity == 0 && i == 0){
				System.out.println("#,Repetition, Dataset, Accuracy, TrainTime(ms), TestTime(ms), AvgDepthPerTree");
			}

			//create model
			TSCheifForest forest;

			
			//train model
			if (AppConfig.boosting) {
				forest = null; //force termination -- DEV only
//				forest = new BoostedTSChiefForest(i, parallelTasks);
//				forest.fit(trainData);
			} else {
				forest = new TSCheifForest(i, parallelTasks);
				forest.fit(trainData);
			}
//			Thread.sleep(500);

			//test model
			ExperimentResultCollector result = (ExperimentResultCollector) forest.predict(testData);

			//print and export resultS
			result.printResults(datasetName, i, "");
			
			if (AppConfig.verbosity > 1) {
				System.out.println("Threads: MaxPool=" + parallelTasks.getThreadPool().getMaximumPoolSize() 
						+ ", Active: " + parallelTasks.getThreadPool().getActiveCount()
						+ ", Total: " + Thread.activeCount());				
			}

			
			//export level is integer because I intend to add few levels in future, each level with a higher verbosity
			boolean last_record = i == (AppConfig.num_repeats-1);
			if (AppConfig.export_level == 1) {
				result.exportJSON(datasetName, i, outputFilePrefix, last_record);			
			}else if (AppConfig.export_level == 2) {
				result.exportCSV(datasetName, i, outputFilePrefix);		
			}else if (AppConfig.export_level == 3) {
				//use this -- might remove others in future
//				result.exportJSON(datasetName, i, outputFilePrefix, last_record);			
				result.exportCSV(datasetName, i, outputFilePrefix);		
				result.exportSummaryCSV(datasetName, i, outputFilePrefix);		
			}
			result.exportSimpleSummaryCSV(datasetName, i);
			result.exportShapelets(datasetName, i, outputFilePrefix);

			if (AppConfig.garbage_collect_after_each_repetition) {
				System.gc();
			}
			
			if (AppConfig.verbosity > 0) {
				System.out.println();
			}
			
		}
		
		if (AppConfig.verbosity > 2) {
			System.out.println("\nShutting down the executor service");
		}
		
		parallelTasks.getExecutor().shutdown();
		parallelTasks.getExecutor().awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);	//just wait forever
				
	
	}
	
	/**
	 * 
	 * Once input files have been read. validate command line arguments and adjust runtime configurations as necessary
	 * 
	 * Call this after reading input files such as training and test files
	 * @throws Exception 
	 * 
	 */

	private void validateAndInitializeRunTimeConfiguration() throws Exception {

		//TODO FIXME setup environment
		File training_file = new File(AppConfig.training_file);
		datasetName = training_file.getName().replaceAll("_TRAIN.txt", "");	//this is just some quick fix for UCR datasets
		AppConfig.setDatasetName(datasetName);
		
		String outputFilePrefix = createOutPutFile(datasetName);	//giving same prefix for all repeats so thats easier to group using pandas
		AppConfig.currentOutputFilePrefix = outputFilePrefix;
				
		
		//TODO refactor
		//do initializations or param validations that can only be done after data have been loaded, these include setting params that 
		//depends on the length of series, etc...
		
		if (AppConfig.st_min_length <= 0  || AppConfig.st_min_length > trainData.length()) {
			AppConfig.st_min_length = trainData.length();
		}		
		
		if (AppConfig.st_max_length <= 0 ||  AppConfig.st_max_length > trainData.length()) {
			AppConfig.st_max_length =  trainData.length();
		}
		
		if (AppConfig.st_min_length > AppConfig.st_max_length) {
			AppConfig.st_min_length = AppConfig.st_max_length;
		}

		AppConfig.setTrainingSet(trainData);
		AppConfig.setTestingSet(testData);
		AppConfig.updateClassDistribution(trainData, testData);


		if (AppConfig.dimensionSubsetsAsString != null){
			String[] subsets = AppConfig.dimensionSubsetsAsString.split("},");
			AppConfig.dimensionSubsets = new int[subsets.length][];
			for (int i = 0; i < subsets.length; i++) {
				String[] set = subsets[i].replaceAll("[{}]", "").split(",");

				for (int j = 0; j < set.length; j++) {
					int dim = Integer.parseInt(set[j]);

					if (dim > trainData.dimensions()) {
						AppConfig.dimensionSubsets[i] = Sampler.getIntsFromRange(0, trainData.dimensions(), 1);
					} else if (dim < 0) {
						AppConfig.dimensionSubsets[i] = Sampler.sampleNRandomIntsFromRange(0, trainData.dimensions(), -dim);
					} else {
						if (AppConfig.dimensionSubsets[i] == null) {
							AppConfig.dimensionSubsets[i] = new int[set.length];
						}
						AppConfig.dimensionSubsets[i][j] = dim;
					}
				}
			}
		}

	}

	private String createOutPutFile(String datasetName) throws IOException {
		AppConfig.experiment_timestamp = LocalDateTime.now();
		String timestamp = AppConfig.experiment_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT));			
		
		String fileName = timestamp + "_" + datasetName;
				
		return fileName;
	}

}
