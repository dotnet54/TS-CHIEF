package development.experiments;

import java.io.FileReader;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.RotationForest;
import weka.core.Instances;

public class WekaClassifierTest {
	
	public static long seed = 1;

	public static final String UCR_dataset = "ItalyPowerDemand";
	public static String training_file = "E:/data/ucr/cleaned/" + UCR_dataset + "/" + UCR_dataset + "_TRAIN.arff";
	public static String testing_file = "E:/data/ucr/cleaned/" + UCR_dataset + "/" + UCR_dataset + "_TEST.arff";
	
	public static Instances train_data;
	public static Instances test_data;
	public static Classifier classifier;
	
	public static void main(String[] args) throws Exception {

		
		train_data = new Instances(new FileReader(training_file));

        if (train_data.classIndex() == -1) {
        	train_data.setClassIndex(train_data.numAttributes() - 1);
        }
       
        test_data = new Instances(new FileReader(training_file));
        if (test_data.classIndex() == -1) {
        	test_data.setClassIndex(test_data.numAttributes() - 1);
        }
        
        classifier = new RotationForest();
        RotationForest model = (RotationForest) classifier;
		
		model.buildClassifier(train_data);
		
		Evaluation eval = new Evaluation(train_data);
		eval.evaluateModel(model, test_data);
		
		 System.out.println(eval.toSummaryString("\nResults\n======\n", false));


	}

	
}
