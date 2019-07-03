package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.AppContext;
import core.CSVReader;
import datasets.TSDataset;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WekaDataConverter {

	
	public WekaDataConverter() {
		
	}
	
	public static TSDataset convertWekaInstancesToListDataset(Instances wekaData) {
		int size = wekaData.size(), length = wekaData.numAttributes() - 1;

		TSDataset listData = new TSDataset(size, length);
		
		int class_index = wekaData.classIndex();
		double[] temp;

		for (int i = 0; i < size; i++) {
			temp = new double[length];
			double class_val = wekaData.get(i).classValue();
			int label = (int) class_val;
			
			//really slow copy, TODO refactor later
			for (int j = 0; j < length; j++) {
				temp[j] =  wekaData.get(i).value(j);
			}

			listData.add(temp, label);
		}
		
		return listData;
	}
	
	public static Instances convertListDatasetToWekaInstances(TSDataset data) {
		
		int size = data.size(), length = data.length();
		
		ArrayList<Attribute> attribs = new ArrayList<Attribute>(length + 1);
		
		for (int i = 0; i < length; i++) {
			attribs.add(new Attribute("att" + i));
		}
		
		int[] intlabels = data.get_unique_classes();
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
			System.arraycopy(data.get_series(i).getData(), 0, temp, 0, length);
			Integer label = data.get_class(i);
			Instance series  = new DenseInstance(1, temp);
			
			//TODO optimize
			Attribute class_attrib = wekaData.classAttribute();
			
//			class_attrib.value(valIndex)
			temp[length] = class_attrib.indexOfValue(label.toString());

			wekaData.add(series);
			
//			if (label == 2)
//				System.out.println(wekaData.get(i).toString());
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
	
	public static TSDataset convertArrayToListDatasetTo() {
		return null;
		
	}
	
	
//	public static void main (String[] args) {
//		System.out.println("testing weka data converter");
//		
//		TSDataset train = CSVReader.readCSVToListDataset(AppContext.training_file);
//		
//		System.out.println("list data set to weka");
//
//		Instances wekaData = WekaDataConverter.convertListDatasetToWekaInstances(train);
//		
//		int class_index = wekaData.classIndex();
//		for (int i = 0; i < wekaData.size(); i++) {
//			System.out.println("instance: " + i + " w: " + wekaData.get(i).value(class_index) + " L: " + train.get_class(i));
//		}
//		
//		
//		System.out.println("weka to list dataset");
//
//		ListDataset listData = (ListDataset) WekaDataConverter.convertWekaInstancesToListDataset(wekaData);
//		
//		for (int i = 0; i < wekaData.size(); i++) {
//			System.out.println("instance: " + i + " w: " + wekaData.get(i).value(class_index) + " L: " + listData.get_class(i));
//		}
//		
//	}
	
}
