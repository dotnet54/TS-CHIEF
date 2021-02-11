package trees.splitters.old;

import java.util.concurrent.ThreadLocalRandom;

import application.AppConfig;
import application.AppConfig.SplitterType;
import gnu.trove.map.TIntObjectMap;
import trees.TSCheifTree;

public class CrossDomainSplitter implements NodeSplitter{

	NodeSplitter[] splitters; 
	TSCheifTree.Node node;
	NodeSplitter best_splitter;
	int num_splits; 

	public CrossDomainSplitter(TSCheifTree.Node node, int num_splits) throws Exception {
		this.node = node;	

		splitters = new NodeSplitter[num_splits];

		for (int i = 0; i < splitters.length; i++) {
			splitters[i] =  get_random_splitter(node);
		}
		
	}
	
	public static synchronized NodeSplitter get_random_splitter(TSCheifTree.Node node) throws Exception {
		
		NodeSplitter splitter = null;
		
		int r = AppConfig.getRand().nextInt(AppConfig.enabled_splitters.length);
		SplitterType selection = AppConfig.enabled_splitters[r];
		
		//use split constraints to set up limits to the splitting condition 
		
		switch(selection) {
			case ElasticDistanceSplitter:
				splitter = new ElasticDistanceSplitter(node, AppConfig.num_candidates_per_ee_split);
				break;
			case RandomForestSplitter:
				splitter = new RandomForestSplitter(node);
				break;
			case ShapeletTransformSplitter:
				splitter = new ShapeletTransformSplitter(node);
				break;
			case RotationForestSplitter:
				splitter = new RotationForestSplitter(node);
				break;
			case WekaSplitter:
				splitter = new WekaSplitter(node);
				break;
			default:
				throw new Exception("Splitter type not supported");
		}
		
		return splitter;
	}
	
	
	@Override
	public TIntObjectMap<Dataset> train_splitter(Dataset sample) throws Exception {

		double weighted_gini = Double.POSITIVE_INFINITY;
		double best_weighted_gini = Double.POSITIVE_INFINITY;
		TIntObjectMap<Dataset> splits = null;
		TIntObjectMap<Dataset> best_split = null;
		int parent_size = sample.size();
		
		for (int i = 0; i < splitters.length; i++) {
			splits = splitters[i].train_splitter(sample);
			weighted_gini = weighted_gini(parent_size, splits);
			
			
//			System.out.println("Splitter" + i + ": " + splitters[i].toString() 
//					+ " WGINI " + weighted_gini + " node= " + sample);
//			for (int j = 0; j < splits.length; j++) {
//				System.out.print("\tsplits: " + splits[j]);
//			}
//			System.out.println();
// 
			if (weighted_gini <  best_weighted_gini) {
				best_weighted_gini = weighted_gini;
				best_split = splits;
				best_splitter = splitters[i];
			}
		}
		
		//allow gc to deallocate unneeded memory
		for (int i = 0; i < splitters.length; i++) {
			if (splitters[i] != best_splitter) {
				splitters[i] = null;
			}
		}

		return best_split;
	}

	@Override
	public int predict_by_splitter(double[] query) throws Exception {
		return best_splitter.predict_by_splitter(query);
	}


	
	
	
	
	
	
	
	
	
	
	
	public double weighted_gini(int parent_size, TIntObjectMap<Dataset> splits) {
		double wgini = 0.0;
		double gini;
		double split_size = 0;
		
		for (int key : splits.keys()) {
			if (splits.get(key) == null) {	//TODO
				gini = 1;
				split_size = 0;
			}else {
				gini = splits.get(key).gini();
				split_size = (double) splits.get(key).size();
			}
			wgini = wgini + (split_size / parent_size) * gini;
		}

		return wgini;
	}	

}
