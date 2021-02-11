package trees.splitters.st.dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import application.AppConfig;
import application.AppConfig.ParamSelection;
import data.io.DataLoader;
import data.timeseries.*;
import trees.splitters.st.ShapeletEx;
import util.weka.WekaDataConverter;

public class ShapeletTransformDataStore extends DataStore{

	public ArrayList<STInfoRecord> stInfoLines;
	public ArrayList<double[]> shapelets;
	public String fileName;
	public String datasetName;
	public Random rand; // TODO check ignore this and use threadLocalRandom.. sacrificing
	// reproducibility
	String cvsSplitBy = ",";

	public class STInfoRecord {
		// informationGain, seriesId,startPos,length,classVal,numChannels,dimension
		public double informationGain = -1;
		public int seriesId = -1;
		public int startPos = -1;
		public int length = -1;
		public double classVal = -1;
		public int numChannels = -1;
		public int dimension = -1;
		public double[] data = null;
	
		public String toString() {
			return "" + informationGain + "," + seriesId + "," + startPos + "," + length + "," + classVal + ","
					+ numChannels + "," + dimension;
		}
	}

	public ShapeletTransformDataStore() {
		super();		
		this.stInfoLines = new ArrayList<STInfoRecord>();
		this.datasetName = AppConfig.getDatasetName();
		this.rand = AppConfig.getRand();	
		this.fileName = AppConfig.ucr2015.to2015Name(this.datasetName);
		
		this.trainingSetKey = "st_train";
		this.testingSetKey = "st_test";		
	}
	
	public ShapeletTransformDataStore(String shapeletTransformExportFile, String datasetName, Random r) {
		super();
	
		this.stInfoLines = new ArrayList<STInfoRecord>();
		this.fileName = shapeletTransformExportFile;
		this.datasetName = datasetName;
		this.rand = r;
		
		this.trainingSetKey = "st_train";
		this.testingSetKey = "st_test";		
	}	

	
	public void initBeforeTrain(Dataset train) throws Exception {
//		double[][][] transformedData = loadTransformedData(AppConfig.getDatasetName(), "train");
//		setTrainingSet(toMTSDataset(train, transformedData[0]));
		
		
		String st_param_file =  ShapeletTransformDataStore
				.getFileNameForDataset(AppConfig.st_params_files, AppConfig.ucr2015.to2015Name(AppConfig.getDatasetName()));
		System.out.println(AppConfig.getDatasetName() +":"+ AppConfig.ucr2015.to2015Name(AppConfig.getDatasetName()) + " - Shapelet Transform Param File: " + st_param_file);
		
		this.fileName = st_param_file;
		readFile(2);	// 2 = read both shapelet info and shapelet data
		
		System.out.print("trainsforming st data...");
		
		if (AppConfig.st_param_selection == ParamSelection.PraLoadedDataAndParams) {
			//load transformed arff files saved after running tsml project's shapelet transform funcitons
			//TODO implement
			String stTransFormedDataFileName = AppConfig.st_params_files 
					+AppConfig.ucr2015.to2015Name(AppConfig.getDatasetName())+ "-Transforms-5356.arff";
			setTrainingSet(WekaDataConverter.convertWekaInstancesToTSDataset(WekaDataConverter.loadArff(stTransFormedDataFileName))); 
		}else if (AppConfig.st_param_selection == ParamSelection.PreLoadedParams){
			setTrainingSet(ShapeletTransformDataStore.transformDataset(train, stInfoLines));
		}else {
			// transforming at node level using random params
		}
		
		System.out.println("finished");
		
	}	


	public static Dataset transformDataset(Dataset input, ArrayList<STInfoRecord> stInfoLines) throws Exception {
		Dataset output = new MTSDataset(input.size());
		output.setName(input.getName() + "_st");
		boolean normalize = true;
		
		//TODO  --------------- DEV verify st data with rec data
		
		//TODO supports univariate data only
		double matrix[][][] = new double[input.size()][1][stInfoLines.size()];
		
		for (int i = 0; i < stInfoLines.size(); i++) {
			STInfoRecord rec = stInfoLines.get(i);
			
			TimeSeries s = input.getSeries(rec.seriesId);
			
			ShapeletEx st = new ShapeletEx(s, rec.startPos, rec.startPos + rec.length, true, normalize);

			for (int j = 0; j < input.size(); j++) {
				matrix[j][0][i] = st.distance(input.getSeries(j));	//TODO using only dimension 1
			}

		}
		
		for (int i = 0; i < matrix.length; i++) {
			TimeSeries transformedSeries = new MTimeSeries(matrix[i], input.getClass(i));
			output.add(transformedSeries);			
		}
		
		return output;
	}
	

	public static String getFileNameForDataset(String folder, String datasetName) {
		String fullName = null;

		File dir = new File(folder);
		// gets you the list of files at this folder
		File[] listOfFiles = dir.listFiles();
		// loop through each of the files looking for filenames that match
		for (int i = 0; i < listOfFiles.length; i++) {
			String filename = listOfFiles[i].getName();
			String tmp = filename.toLowerCase().split("-")[0];
			if (tmp.equals(datasetName.toLowerCase())
					&& listOfFiles[i].getName().endsWith(".shtransform.tsml.csv")) {
				fullName = folder + "/" + filename;
			}
		}

		return fullName;
	}

	public static void verifyParamFile(MTSDataset dataset, ArrayList<STInfoRecord> params) throws Exception {
		
		System.out.println("verifying file: " + dataset.size());
		
		for (int i = 0; i < params.size(); i++) {
			STInfoRecord rec = params.get(i);

			if (rec.seriesId > dataset.size()) {
				throw new Exception("Indices larger than dataset found " + rec.seriesId);

			}
		}

	}

	public STInfoRecord getRandomShapelet() {
		int r = this.rand.nextInt(stInfoLines.size());

		return stInfoLines.get(r);
	}

	// parseLines == 0 -> only info lines, parseLines == 1 only shapelet data, parseLines == 2 both
	public void readFile(int parseLines) throws IOException {
		String line = "";
		int lineNumber = 0;
	
		try (BufferedReader br = new BufferedReader(new FileReader(this.fileName))) {
			STInfoRecord rec = null;
			
			while ((line = br.readLine()) != null) {
				
				if (lineNumber == 0) {
					// header
				} else if (lineNumber % 2 == 0  && (parseLines == 1 || parseLines == 2)) { // even
					// actual shapelet data
					String[] values = line.split(cvsSplitBy);
					double[] sdata = new double[values.length];
							
					for (int i = 0; i < sdata.length; i++) {
						sdata[i] = Double.parseDouble(values[i].trim());
					}
					
					rec.data = sdata;
					
				} else if (lineNumber % 2 == 1 && (parseLines == 0 || parseLines == 2)) { // odd
					rec = new STInfoRecord();
					
					// use comma as separator
					String[] values = line.split(cvsSplitBy);
	
					rec.informationGain = Double.parseDouble(values[0].trim());
					rec.seriesId = Integer.parseInt(values[1].trim());
					rec.startPos = Integer.parseInt(values[2].trim());
					rec.length = Integer.parseInt(values[3].trim());
					rec.classVal = Double.parseDouble(values[4].trim());
					rec.numChannels = Integer.parseInt(values[5].trim());
					rec.dimension = Integer.parseInt(values[6].trim());
	
					// System.out.println("Shapelet [seriesId= " + values[1] + " , startPos=" +
					// values[2] + "]" + lineNumber);
	
					stInfoLines.add(rec);
				}else {
					
				}
	
				lineNumber++;
			}
	
			System.out.println("Total Shapelets Loaded: " + stInfoLines.size());
	
		} catch (IOException e) {
			throw e;
		}
	}


	/***
		 * 
		 * This class loads saved shapelet information from a file, after running the
		 * shapelet transform algorithm implemented by tony's team. The idea is to save
		 * those shapelets, and load the information about k-best shapelets selected by
		 * ST. This information is used to get a "best-case" scenario for the random
		 * shapelet splitter, using the distribution of start positions and lengths
		 * selected by ST
		 * 
		 * file format header:
		 * informationGain,seriesId,startPos,length,classVal,numChannels,dimension odd
		 * lines:
		 * informationGain,seriesId,startPos,length,classVal,numChannels,dimension even
		 * lines: shapelet values
		 * 
		 * @param args
		 * @throws Exception
		 */
		public static void main(String[] args) throws Exception {
	
			String folder = "settings/st-5356/";
//			String folder = "settings/st/";
			String datasetName = "SonyAIBORobotSurface1";
			
			AppConfig.initializeAppConfig();

			String settingsFile = getFileNameForDataset(folder, AppConfig.ucr2015.to2015Name(datasetName));
			System.out.println("File: " + settingsFile);
	
			long seed =  System.nanoTime();
			
			 ShapeletTransformDataStore st = new ShapeletTransformDataStore(settingsFile,datasetName, new Random(seed));
			 st.readFile(2);
			
			 STInfoRecord randST = st.getRandomShapelet();
			
			 System.out.println("Rand ST " + randST.toString());
			
//			 MTSDataset train = DataLoader.loadUCRDataset(datasetName)[0];
//			 train.setName(datasetName);
//
//	//		 verifyParamFile(train, st.stInfoLines);
//
//			 MTSDataset stDataset = transformDataset(train, st.stInfoLines);
	
		}

}
