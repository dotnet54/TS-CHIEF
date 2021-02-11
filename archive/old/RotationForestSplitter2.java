package trees.splitters.old;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import application.AppConfig;
import data.timeseries.TimeSeries;
import data.timeseries.UTSDataset;
import gnu.trove.impl.hash.TIntDoubleHash;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSCheifTree;
import util.weka.RotationForestStump;
import util.weka.WekaDataConverter;
import weka.classifiers.Classifier;
import weka.classifiers.meta.RotationForest;
import weka.core.Instance;
import weka.core.Instances;

public class RotationForestSplitter2 implements NodeSplitter{

	private final int NUM_CHILDREN = 2;
	private final int LEFT_BRANCH = 0;
	private final int RIGHT_BRANCH = 1;
	TIntObjectMap<UTSDataset> best_split = null;	
	TSCheifTree.Node node;
	
	RotationForestStump rotf;
	
	int num_classifiers = 1;
	
	private TIntIntMap train_class_distrib; //to determine number of branches; -> store during training because during split #class in sample for approx gini may be less
	
	private TIntIntMap train_class_distrib_weka;
	
	public RotationForestSplitter2(TSCheifTree.Node node) {
		this.node = node;	
		this.node.tree.stats.rotf_count++;
	}
	
	
	@Override
	public synchronized void train(UTSDataset data, int[] indices) throws Exception {
		// TODO Auto-generated method stub
		
//		new TIntObjectHashMap<Dataset>(NUM_CHILDREN)
		TIntObjectMap<UTSDataset> splits = new TIntObjectHashMap<UTSDataset>(data.get_num_classes());
//		splits.put(LEFT_BRANCH, new ListDataset(sample.size(), sample.length()));	//TODO initial capacity too large, try optimize this
//		splits.put(RIGHT_BRANCH, new ListDataset(sample.size(), sample.length()));	//TODO initial capacity too large, try optimize this
		
		Instances wekaData = WekaDataConverter.convertListDatasetToWekaInstances(data);
		
		rotf = new RotationForestStump();
		//TODO set random seed, default is set to 1
//		rotf.setNumIterations(num_classifiers);
		rotf.setSeed((int) System.nanoTime()); //TODO MAJOR ISSUE HERE
		rotf.setNumIterations(num_classifiers);
		
		if (AppConfig.verbosity > 2) {
			rotf.setDebug(true);
		}
		
		rotf.buildClassifier(wekaData);
		
		train_class_distrib = data.get_class_map();
		
		train_class_distrib_weka = new TIntIntHashMap(train_class_distrib.size());
		for (int i = 0; i < train_class_distrib.size(); i++) {
			train_class_distrib_weka.put(i, Integer.parseInt(wekaData.classAttribute().value(i)));
		}
		
	}
	
	@Override
	public void train_binary(UTSDataset data) throws Exception {
		// TODO Auto-generated method stub
		
	}

//	public TIntObjectMap<TSDataset> split_old(TSDataset data) throws Exception {
//		
//		Instances wekaData = WekaDataConverter.convertListDatasetToWekaInstances(data);
//		
//		TIntIntMap[] predictions = new TIntIntHashMap[wekaData.size()];
////		TIntDoubleMap[] predicted_distributions = new TIntDoubleHashMap[wekaData.size()];
//		
//		
//		TIntObjectMap<TSDataset> splits = new TIntObjectHashMap<TSDataset>(train_class_distrib.size());
//		
//		//one branch per class found during training
//		for (int class_label : train_class_distrib.keys()) {
//			splits.put(class_label, new TSDataset());
//		}
//
//		double[] pred_dist;
//		for (int i = 0; i < wekaData.size(); i++) {
//			pred_dist = rotf.distributionForInstance(wekaData.get(i));
//			
//			int pred_indx = 0;
//	        for(int j=1;j<pred_dist.length;j++){
//	            if(pred_dist[j]>pred_dist[pred_indx])
//	            	pred_indx=j;
//	        }
////	        String class_label = wekaData.classAttribute().value(pred_indx);
//	        int predicted_label = train_class_distrib_weka.get(pred_indx);
//	        
//	        String temp_label = wekaData.get(i).classAttribute().value((int) wekaData.get(i).classValue());
//	        int actual_label = Integer.parseInt(temp_label);
//	        splits.get(actual_label).add(predicted_label, data.get_series(i));
//		}		
//		
//		return splits;
//	}

	@Override
	public TIntObjectMap<UTSDataset> split(UTSDataset data, int[] indices) throws Exception {

		Instances wekaData = WekaDataConverter.convertListDatasetToWekaInstances(data);		
		
		TIntObjectMap<UTSDataset> splits = new TIntObjectHashMap<UTSDataset>(2);	//two branches
		splits.put(0, new UTSDataset());
		splits.put(1, new UTSDataset());

		if (AppConfig.verbosity > 2) {
			System.out.println("attrib: " + rotf.getAttribIndex() + " threshold : " + rotf.getSplitPoint() + " dist : "
					+ Arrays.deepToString(rotf.getUnNormalizedDistribution()));
		}
		
		double[] pred_dist;
		for (int i = 0; i < data.size(); i++) {
			int branch = rotf.getBranch(wekaData.get(i));
			if (AppConfig.verbosity > 2) {
				System.out.println("i: " + i + " class " + data.get_class(i) + " pred: " + branch + " " +  Arrays.deepToString(rotf.getUnNormalizedDistribution()));
			}
			
	        splits.get(branch).add(data.get_series(i));	 //TODO check?? actual class or pred class
		}		
		
		return splits;
	}
	
	@Override
	public synchronized int predict(TimeSeries query, int queryIndex) throws Exception {


        Instance instance = WekaDataConverter.getInstance(query.getData(), -1);  //TODO why 3?
        
        return rotf.getBranch(instance);
	}

//	@Override
	public synchronized int predict_old(double[] query) throws Exception {

		int[] intlabels = AppConfig.getTrainingSet().get_unique_classes(); //TODO what if #class in train != test - make this more robust
		Arrays.sort(intlabels);		
		
        int pred=0;
        double[] probs;
                
        Instance series = WekaDataConverter.getInstance(query, -1);  //TODO why 3?
        
        probs = rotf.distributionForInstance(series);
        
        for(int i=1;i<probs.length;i++){	//TODO why i=1?
            if(probs[i]>probs[pred])
                pred=i;
        }

        int class_label = intlabels[pred];
        
		return class_label;
	}
	
	public String toString() {
		return "RotF";
//		return "RotF[k=" + num_classifiers + "]";
	}



	
//	public static void main(String[] args) {
//	System.out.println("rotation forest splitter test");
//	
//	
//	ListDataset data = new ListDataset(10,24);
//	
//	
//	data.add(2, new double[] {0.47297,-0.39603,-0.72191,-1.1564,-1.2107,-1.0478,-0.77622,-1.3193,-0.83053,0.25572,0.96179,1.1247,0.5816,0.20141,-0.23309,-0.34172,-0.66759,-0.72191,-0.45034,-0.015841,0.79885,2.2653,1.9394,1.2877});
//	data.add(2, new double[] {-1.0987,-1.1346,-1.6003,-1.7078,-1.6362,-1.457,-1.0271,-0.70462,0.47771,1.2301,1.2659,1.1226,0.76434,0.65685,0.51354,0.62103,0.72851,0.37023,0.40606,0.37023,0.0836,1.0151,0.62103,0.11943});
//	data.add(2, new double[] {-1.0294,-1.2402,-1.3455,-1.5562,-1.4772,-1.3455,-1.4509,-0.76605,0.2612,0.97237,1.1304,1.1831,0.76166,0.89335,0.89335,1.0777,1.1041,0.78799,0.62996,0.36656,0.050484,-0.054874,0.2612,-0.10755});
//	data.add(2, new double[] {0.29127,-0.3239,-0.88314,-1.1068,-1.2746,-0.65944,-0.88314,-0.49167,-1.1628,-0.3239,0.5709,0.62682,0.62682,-0.1002,-0.3239,-0.3239,-0.65944,-0.5476,-0.5476,0.40312,2.3605,2.0249,1.8012,0.90645});
//	data.add(2, new double[] {-0.81461,-1.2711,-1.5666,-1.6203,-1.6471,-1.4054,-1.0294,-0.46549,0.7967,1.4144,1.307,1.3338,1.0921,0.55501,0.68928,0.55501,0.52815,0.2596,0.12532,0.20589,0.23274,0.7967,0.2596,-0.33121});
//
//	data.add(1, new double[] {-1.1461,-1.4167,-1.5833,-1.5833,-1.625,-1.4584,-0.87538,-0.00086758,0.39475,1.0194,0.97775,0.832,0.62379,0.37392,0.43639,0.5405,0.56132,0.64461,1.2901,1.3525,0.72789,0.47803,-0.021689,-0.54223});
//	data.add(1, new double[] {-0.90972,-1.2692,-1.5328,-1.6287,-1.6287,-1.437,-1.2692,-0.33453,0.64809,1.0795,1.0315,1.0555,0.83982,0.36049,0.60016,0.50429,0.43239,0.28859,0.36049,1.415,0.93568,0.69602,0.12083,-0.3585});
//	data.add(1, new double[] {-1.0254,-1.4202,-1.5957,-1.6615,-1.5957,-1.486,-1.0474,-0.10419,0.70738,0.97059,1.0145,0.97059,0.81705,0.57577,0.55384,0.64157,0.64157,0.57577,0.90478,0.97059,0.75124,0.4661,-0.038385,-0.58674});
//	data.add(1, new double[] {-0.90001,-1.2265,-1.5279,-1.6535,-1.6786,-1.5028,-1.0256,-0.44791,0.55675,1.3102,1.1344,1.1847,0.83303,0.53163,0.50652,0.45628,0.53163,0.38093,0.25535,0.95861,1.034,0.60698,0.18,-0.49814});
//	data.add(1, new double[] {-1.0971,-1.4084,-1.5419,-1.6086,-1.5863,-1.4307,-1.0749,-0.34099,0.50407,1.1935,1.2157,1.0378,0.9266,0.50407,0.54855,0.57079,0.48183,0.28169,0.37064,1.2157,0.88212,0.54855,-0.0074128,-0.18532});
//
//	
//	
//	RotationForestSplitter splitter = new RotationForestSplitter(null);
//	
//	
//	TIntObjectMap<Dataset> splits;
//	try {
//		splits = splitter.train_splitter(data);
//		
//		double[] query0 = new double[] {0.47297,-0.39603,-0.72191,-1.1564,-1.2107,-1.0478,-0.77622,-1.3193,-0.83053,0.25572,0.96179,1.1247,0.5816,0.20141,-0.23309,-0.34172,-0.66759,-0.72191,-0.45034,-0.015841,0.79885,2.2653,1.9394,1.2877};
//		double[] query1 = new double[] {-1.1461,-1.4167,-1.5833,-1.5833,-1.625,-1.4584,-0.87538,-0.00086758,0.39475,1.0194,0.97775,0.832,0.62379,0.37392,0.43639,0.5405,0.56132,0.64461,1.2901,1.3525,0.72789,0.47803,-0.021689,-0.54223};
//		
//		
//		int branch  = splitter.predict_by_splitter(query0);
//		
//		System.out.println("prediction: " + branch);
//		
//		
//		
//	} catch (Exception e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}
//
//	}

}
