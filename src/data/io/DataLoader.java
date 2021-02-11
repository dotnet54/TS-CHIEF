package data.io;

import application.AppConfig;
import application.test.knn.MultivarKNNArgs;
import data.timeseries.Dataset;
import data.timeseries.LabelEncoder;
import data.timeseries.MTSDataset;
import data.timeseries.NonContinuousLabelEncoder;

public class DataLoader {

	/**
	 * Main loader class for package data.io
	 * 
	 * DEV development plan for the package
	 * 
	 * detect file type {csv, arff} and call appropriate loaders
	 * load predefined dataset names from UCR or other common archives, append/remove suffixes and prefixes to names as necessary
	 * load univariate, multivariate data
	 * load fixed length/variable length datasets
	 * add support for loggers, remove system prints, log stats for loading e.g. time, memory before and after
	 * 
	 */
	
	public DataLoader() {
		
	}

	public static MTSDataset[] loadTrainAndTestSet(String dataPath, String archiveName, String datasetName,
											 String extension) {
		MTSDataset[] datasets = new MTSDataset[2];
		datasets[0] = DataLoader.loadTrainingSet(dataPath, archiveName, datasetName, MultivarKNNArgs.fileType);
		// pass training set to the loader so that it can use same label encoder
		datasets[1] = DataLoader.loadTestingSet(dataPath, archiveName, datasetName, MultivarKNNArgs.fileType, datasets[0]);
		return datasets;
	}

	public static MTSDataset loadTrainingSet(String dataPath, String archiveName, String datasetName,
											 String extension) {
		String trainingFileName =  dataPath  + archiveName + "/" + datasetName + "/" + datasetName
				+ "_TRAIN" + extension;
		return loadMTSDataset(trainingFileName, AppConfig.csv_has_header, AppConfig.csv_label_column, null);
	}

	public static MTSDataset loadTestingSet(String dataPath, String archiveName, String datasetName,
											String extension, Dataset trainData) {
		String testingFileName =  dataPath  + archiveName + "/" + datasetName + "/" + datasetName
				+ "_TEST" + extension;
		return loadMTSDataset(testingFileName, AppConfig.csv_has_header, AppConfig.csv_label_column,
				trainData.getLabelEncoder());
	}

	public static MTSDataset loadTrainingSet(String trainingFileName) {
		return loadMTSDataset(trainingFileName, AppConfig.csv_has_header, AppConfig.csv_label_column, null);
	}

	public static MTSDataset loadTestingSet(String testingFileName, Dataset trainData) {
		return loadMTSDataset(testingFileName, AppConfig.csv_has_header, AppConfig.csv_label_column,
				trainData.getLabelEncoder());
	}

	/**
	 *
	 *
	 * @param fileName
	 * @param hasHeader
	 * @param labelColumn {0,-1} first or last
	 * @return
	 */
	public static MTSDataset loadMTSDataset(String fileName, boolean hasHeader, int labelColumn,
											 LabelEncoder labelEncoder) {
		MTSDataset data = null;
		if (labelEncoder == null){
			labelEncoder = new NonContinuousLabelEncoder();
		}

		if (fileName.endsWith(".arff")) {
			ArffReader reader = new ArffReader(hasHeader, labelColumn);
			data = reader.readFile(fileName.trim(), labelEncoder);
		}else if(fileName.endsWith(".ts")){
			TSReader reader = new TSReader(hasHeader, labelColumn);
			data = reader.readFile(fileName.trim(), labelEncoder);
		}else {
			//assume its a csv file
			CsvReader reader = new CsvReader(hasHeader, labelColumn);
			data = reader.readFile(fileName.trim(), labelEncoder);
		}

		return data;
	}


	/**
	 * 
	 * loads a univariate time series dataset (fixed-length)
	 * 
	 * @param Filename
	 * @return
	 */
//	public static MTSDataset loadUTSDataset(String fileName) {
//		return loadUTSDataset(fileName, AppConfig.csv_has_header, AppConfig.csv_label_column);
//	}
	
	/**
	 * 
	 * 
	 * @param fileName
	 * @param hasHeader
	 * @param labelColumn {0,-1} first or last
	 * @return
	 */
//	public static UTSDataset loadUTSDataset(String fileName, boolean hasHeader, int labelColumn) {
//		UTSDataset data = null;
//
//		if (fileName.endsWith(".arff")) {
//			ArffReader reader = new ArffReader(hasHeader, labelColumn);
//			data = reader.readFile(fileName);
//		}else {
//			//assume its a csv file
//			CsvReader reader = new CsvReader(hasHeader, labelColumn);
//			data = reader.readFile(fileName);
//		}
//
//		return data;
//	}

	
	/**
	 * Loading csv files to double[][] arrays
	 * 
	 */
//	public static double[][] loadToArray(String fileName) {
//		return loadToArray(fileName, false); //default: no header
//	}
	
	public static double[][] loadToArray(String fileName, boolean hasHeader) {
		double[][] data = null;
		
		CsvToDoubleArrayReader reader = new CsvToDoubleArrayReader(hasHeader);
		data = reader.readFileToArray(fileName);
		
		return data;
	}
	
//	/**
//	 * DEV
//	 * loads a multivariate time series dataset  (fixed-length)
//	 * 
//	 * @param Filename
//	 * @return
//	 */
//	public static MTSDataset loadMTSDataset(String Filename) {
//		
//	}
	
	
	public static void main(String[] args) {

		MTSDataset csv = DataLoader.loadTrainingSet("E:/data/ucr/Beef/Beef_TRAIN.txt");
		System.out.println(csv);

		MTSDataset csvHeader = DataLoader.loadMTSDataset("E:/data/ucr/Beef/Beef_TRAIN.txt", true, 0, null); //should skip top row
		System.out.println(csvHeader);

		MTSDataset arff = DataLoader.loadMTSDataset("E:/data/ucr/Beef/Beef_TRAIN.arff", false, -1, null);
		System.out.println(arff);

		MTSDataset sits = DataLoader.loadMTSDataset("E:\\data\\SatelliteFull\\sk_stratified\\SatelliteFull_TRAIN_c59.arff", false, -1, null);
		System.out.println(sits);

		MTSDataset sits2 = DataLoader.loadMTSDataset("E:\\data\\SatelliteFull\\sk_stratified\\SatelliteFull_TRAIN_c59.arff", false, -1, null);
		System.out.println(sits2);
		
//		System.out.println("ucr");
//		MTSDataset[] ucr = DataLoader.loadMTSDataset("Beef");
//		System.out.println(ucr[0]);
		
		System.out.println("transformed");
		double[][] data = DataLoader.loadToArray("cache/inception/Beef/x_train_vec.csv", false);
		System.out.println(data[29][639]);
		
	}
	
}
