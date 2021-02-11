 package trees.splitters.old;

import java.util.concurrent.ThreadLocalRandom;

import application.AppConfig;
import application.AppConfig.SplitterType;
import trees.TSCheifTree;
import trees.TSCheifTree.Node;

public class SplitterChooser {


	
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
			case CrossDomainSplitter:
				splitter = new CrossDomainSplitter(node, AppConfig.num_splitters_per_node);
				break;
			case WekaSplitter:
				splitter = new WekaSplitter(node);
				break;
			default:
				throw new Exception("Splitter type not supported");
		}
		
		return splitter;
	}
}
