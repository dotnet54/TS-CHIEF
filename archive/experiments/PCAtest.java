package development.experiments;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

import data.io.CsvReader;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import util.weka.RotationForestStump;
import util.weka.WekaDataConverter;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.PrincipalComponents;

public class PCAtest {
	public static long seed = 1;
	public static Random random = new Random(seed);

	public static final String UCR_dataset = "Fish";//"DiatomSizeReduction";
	public static String training_file = "E:/data/ucr/" + UCR_dataset + "/" + UCR_dataset + "_TRAIN";
	public static String testing_file = "E:/data/ucr/" + UCR_dataset + "/" + UCR_dataset + "_TEST";
	
	public static Instances train;
	public static Instances test;
	public static Dataset train_csv;
	public static Dataset test_csv;
	public static Classifier classifier;
	
	public static void main(String[] args) throws Exception {

		
		train = new Instances(new FileReader(training_file + ".arff"));

        if (train.classIndex() == -1) {
        	train.setClassIndex(train.numAttributes() - 1);
        }
       
        test = new Instances(new FileReader(testing_file + ".arff"));
        if (test.classIndex() == -1) {
        	test.setClassIndex(test.numAttributes() - 1);
        }
        
        
        train_csv = CsvReader.readCSVToListDataset(training_file + ".txt");
        test_csv = CsvReader.readCSVToListDataset(testing_file + ".txt");

        Instances train_cvt = WekaDataConverter.convertListDatasetToWekaInstances(train_csv);
        Instances test_cvt = WekaDataConverter.convertListDatasetToWekaInstances(test_csv);

        
        PrincipalComponents pca = new PrincipalComponents();
        
        classifier = new RotationForestStump();
        RotationForestStump model = (RotationForestStump) classifier;
		
        int m_RemovedPercentage = 0;
        
//        train.randomize(random);
//        // Remove a percentage of the instances
//		Instances originalDataSubSet = train;
//		train.randomize(random);
//	        RemovePercentage rp = new RemovePercentage();
//	        rp.setPercentage( m_RemovedPercentage );
//	        rp.setInputFormat( train );
//	        train = Filter.useFilter( train, rp );
//		if( train.numInstances() < 2 ) {
//			train = originalDataSubSet;
//		}     
		
		System.out.println(train.size());
		
//        Capabilities cap = model.getCapabilities();

//        model.setDebug(true);
		model.setSeed(0);
//		model.setSeed((int) System.nanoTime()); //TODO MAJOR ISSUE HERE

        model.setNumIterations(1);
        model.setDebug(true);
//        model.setClassifier(newClassifier);
		model.buildClassifier(train2);
		
		split(train, model);
		
		Evaluation eval = new Evaluation(train2);
		eval.evaluateModel(model, test);
		
		 System.out.println(eval.toSummaryString("\nResults\n======\n", false));
		 
		 double[] pred = model.distributionForInstance(test.get(0));
		 String result = Arrays.stream(pred)
			        .mapToObj(String::valueOf)
			        .collect(Collectors.joining(", "));
		 System.out.println("pred: " + result + ": actual: " + test.get(0).classValue());

	}
	
	public static TIntObjectMap<ArrayList<Instance>> split(Instances data, Classifier model) throws Exception {
		
		
		TIntIntMap[] predictions = new TIntIntHashMap[data.size()];
		
		
		TIntObjectMap<ArrayList<Instance>> splits = new TIntObjectHashMap<ArrayList<Instance>>(data.numClasses());
		
		
		TIntIntMap train_class_distrib_weka = new TIntIntHashMap(data.numClasses());
		for (int i = 0; i < data.numClasses(); i++) {
			train_class_distrib_weka.put(i, Integer.parseInt(data.classAttribute().value(i)));
		}
		
		//one branch per class found during training
		for (int class_label : train_class_distrib_weka.values()) {
			splits.put(class_label, new ArrayList<Instance>());
		}

		double[] pred_dist;
		for (int i = 0; i < data.size(); i++) {
			pred_dist = model.distributionForInstance(data.get(i));
			
			int pred_indx = 0;
	        for(int j=1;j<pred_dist.length;j++){
	            if(pred_dist[j]>pred_dist[pred_indx])
	            	pred_indx=j;
	        }
//	        String class_label = wekaData.classAttribute().value(pred_indx);
	        int predicted_label = train_class_distrib_weka.get(pred_indx);
	        
	        String temp_label = data.get(i).classAttribute().value((int) data.get(i).classValue());
	        int actual_label = Integer.parseInt(temp_label);
//	        splits.get(actual_label).add(predicted_label, data.get_series(i));
//	        System.out.println("i: " + i +  " actual " + actual_label + " pred: " + predicted_label);
	        splits.get(predicted_label).add(data.get(i)); //TODO MAJOR BUG note:  add to pred label
	        if (predicted_label != actual_label) {
		        System.out.println("miss classification i: " + i +  " actual " + actual_label + " pred: " + predicted_label);
	        }else{
		        System.out.println("correct i: " + i +  " actual " + actual_label + " pred: " + predicted_label);
	        }
		}		
		
		return splits;
	}
}
