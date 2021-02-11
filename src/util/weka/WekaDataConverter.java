package util.weka;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import data.io.CsvReader;
import data.timeseries.Dataset;
import data.timeseries.MTSDataset;
import data.timeseries.MTimeSeries;
import data.timeseries.TimeSeries;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WekaDataConverter {

	
	public WekaDataConverter() {
		
	}
	
	public static Dataset convertWekaInstancesToTSDataset(Instances wekaData) throws Exception {
		int size = wekaData.size(), length = wekaData.numAttributes() - 1;

		Dataset data = new MTSDataset(size);
		
		int class_index = wekaData.classIndex();
		double[][] tempData;

		for (int i = 0; i < size; i++) {
			tempData = new double[0][length]; //TODO support multivariate data
			double class_val = wekaData.get(i).classValue();
			int label = (int) class_val;
			
			//really slow copy, TODO refactor later
			for (int j = 0; j < length; j++) {
				tempData[0][j] =  wekaData.get(i).value(j);
			}

			TimeSeries series = new MTimeSeries(tempData, label);
			data.add(series);
		}
		
		data.setName(wekaData.relationName());
		return data;
	}
	
	public static Instances convertTSDatasetToWekaInstances(Dataset data) {
		
		int size = data.size(), length = data.length();
		
		ArrayList<Attribute> attribs = new ArrayList<Attribute>(length + 1);
		
		for (int i = 0; i < length; i++) {
			attribs.add(new Attribute("att" + i));
		}
		
		int[] intlabels = data.getUniqueClasses();
		Arrays.sort(intlabels);
		List<String> labels = new ArrayList<String>(intlabels.length);
		
		for (int i = 0; i < intlabels.length; i++) {
			labels.add("" + intlabels[i]);
		}
		
		
		attribs.add(new Attribute("target", labels));

		Instances wekaData= new Instances("unnamed", attribs, size);
		wekaData.setClassIndex(wekaData.numAttributes()-1);

		double[] temp;
		
		for (int i = 0; i < size; i++) {
			temp = new double[length + 1];
			//TODO add support for multivariate data
			System.arraycopy(data.getSeries(i).data()[0], 0, temp, 0, length);
			Integer label = data.getClass(i);
			Instance series  = new DenseInstance(1, temp);
			
			//TODO optimize
			Attribute class_attrib = wekaData.classAttribute();
			
//			class_attrib.value(valIndex)
			temp[length] = class_attrib.indexOfValue(label.toString());

			wekaData.add(series);
			
//			if (label == 2)
//				System.out.println(wekaData.get(i).toString());
		}
		
		if (data.getName() != null) {
			wekaData.setRelationName(data.getName());
		}
		
		return wekaData;
		
	}
	
	public static Instance getInstance(double[] data, Integer label) {
		Instance series;
		double[] temp = new double[data.length + 1];
		System.arraycopy(data, 0, temp, 0, data.length);
		temp[data.length] = label.doubleValue();
		series = new DenseInstance(1, temp);
				
		return series;
	}
	
	public static double[] getInstanceData(Instance series) {
		double[] temp = new double[series.numAttributes() - 1];
		
		//really slow copy, TODO refactor later
		for (int j = 0; j < temp.length; j++) {
			temp[j] =  series.value(j);
		}
		return temp;
	}
	
	public static double[][] convertListDatasetToArray() {
		return null;
		
	}
	
	public static Dataset convertArrayToListDatasetTo() {
		return null;
		
	}
	
	/**
	 * Loads an arff file, assumes that the class label is last attribute
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException 
	 */
	public static Instances loadArff(String fileName) throws IOException {
        FileReader reader = new FileReader(fileName);
        Instances data = new Instances(reader);
        data.setClassIndex(data.numAttributes() - 1);
        reader.close();
        return data;
	}
	
	// tester 
	
	public static void main (String[] args) throws Exception {
		System.out.println("---- Testing Weka Utilities ----");
		
		String datasetPath = "E:/data/ucr/";
		String datasetName = "ToeSegmentation2"; //ToeSegmentation2 ItalyPowerDemand
		
		System.out.println("--loading arff file");
		
		Instances train_arff = loadArff(datasetPath+datasetName+"/"+ datasetName+"_TRAIN.arff");
		Instances test_arff = loadArff(datasetPath+datasetName+"/"+ datasetName+"_TEST.arff");

//		System.out.println(train.size());
//		System.out.println(test.get(0).toString());

		System.out.println("--loading csv file");

		CsvReader reader = new CsvReader();
		Dataset train_csv = reader.readFile(datasetPath+datasetName+"/"+ datasetName+"_TRAIN.txt");
		Dataset test_csv = reader.readFile(datasetPath+datasetName+"/"+ datasetName+"_TEST.txt");
		
		System.out.println("--converting arff to tsdata and vice versa");

		Dataset train_ts = WekaDataConverter.convertWekaInstancesToTSDataset(train_arff);
		Instances train_w = WekaDataConverter.convertTSDatasetToWekaInstances(train_csv);

		System.out.println(train_ts.size() == train_w.size());
		
		
//		int class_index = wekaData.classIndex();
//		for (int i = 0; i < wekaData.size(); i++) {
//			System.out.println("instance: " + i + " w: " + wekaData.get(i).value(class_index) + " L: " + train.get_class(i));
//		}
//		
//		
//		
//
//		ListDataset listData = (ListDataset) WekaDataConverter.convertWekaInstancesToListDataset(wekaData);
//		
//		for (int i = 0; i < wekaData.size(); i++) {
//			System.out.println("instance: " + i + " w: " + wekaData.get(i).value(class_index) + " L: " + listData.get_class(i));
//		}
//		
	}
	
}
