package trees.splitters.it;

import application.AppConfig;
import data.io.DataLoader;
import data.timeseries.DataStore;
import data.timeseries.Dataset;
import data.timeseries.MTSDataset;
import data.timeseries.MTimeSeries;

public class InceptionTimeDataStore extends DataStore {
	
	public InceptionTimeDataStore() {
		super();
		this.trainingSetKey = "it_train";
		this.testingSetKey = "it_test";
	}
	
	@Override
	public void initBeforeTrain(Dataset train) throws Exception {
		double[][][] transformedData = loadTransformedData(AppConfig.getDatasetName(), "train");
		setTrainingSet(toDataset(train, transformedData[0]));
	}

	@Override
	public void initBeforeTest(Dataset test) throws Exception {
		double[][][] transformedData = loadTransformedData(AppConfig.getDatasetName(), "test");
		setTestingSet(toDataset(test, transformedData[1]));
	}
	
	private double[][][] loadTransformedData(String datasetName, String load) {
		double[][][] transformedData = new double[2][][];
		String fileName;
		
		if (load.equals("train") || load.equals("both") ) {
			fileName = AppConfig.it_cache_dir + "/" + AppConfig.ucr2015.to2015Name(datasetName) + "/x_train_vec.csv";
			transformedData[0] = DataLoader.loadToArray(fileName, false);
		}

		if (load.equals("test")  || load.equals("both") ) {
			fileName = AppConfig.it_cache_dir + "/" + AppConfig.ucr2015.to2015Name(datasetName) + "/x_test_vec.csv";
			transformedData[1] = DataLoader.loadToArray(fileName, false);
		}

		return transformedData;
	}
	
	private Dataset toDataset(Dataset originalData, double[][] transformedData) throws Exception {
		Dataset transformedDataset = new MTSDataset(originalData.size());
		
		for (int i = 0; i < originalData.size(); i++) {
			double[][] data = new double[1][];
			data[0] = transformedData[i];
			MTimeSeries series = new MTimeSeries(data, originalData.getClass(i));
			transformedDataset.add(series);
		}
		
		return transformedDataset;
	}

	
}
