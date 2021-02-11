package trees.splitters;

import java.util.ArrayList;
import java.util.List;
import application.AppConfig;
import application.AppConfig.FeatureSelectionMethod;
import application.AppConfig.ParamSelection;
import application.AppConfig.RifFilters;
import application.AppConfig.SplitMethod;
import application.AppConfig.SplitterType;
import data.containers.TreeStatCollector;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import data.timeseries.MTSDataset;
import data.timeseries.TimeSeries;
import gnu.trove.map.TIntObjectMap;
import trees.TSCheifTree;
import trees.splitters.boss.BossSplitter;
import trees.splitters.boss.dev.BossBinarySplitter;
import trees.splitters.ee.ElasticDistanceSplitter;
import trees.splitters.ee.measures.DistanceMeasure;
import trees.splitters.it.InceptionTimeSplitter;
import trees.splitters.randf.RandomForestSplitter;
import trees.splitters.rise.RIFSplitter;
import trees.splitters.rotf.RotationForestSplitter;
import trees.splitters.rt.RocketTreeSplitter;
import trees.splitters.st.ForestShapeletTransformSplitter;
import trees.splitters.st.RandomShapeletTransformSplitter;
import trees.splitters.tsf.TSFSplitter;
import util.Sampler;

public class SplitEvaluator{

	List<NodeSplitter> splitters;
	TSCheifTree.Node node;
	NodeSplitter best_splitter;
	
	List<NodeSplitter> best_splitters;	//store the best ones to pick a random splitter if more than one are equal
	
	public SplitEvaluator(TSCheifTree.Node node) throws Exception {
		this.node = node;	
		
		splitters = new ArrayList<NodeSplitter>(AppConfig.num_actual_splitters_needed);
		best_splitters = new ArrayList<NodeSplitter>(3);	//initial capacity is set to a small number, its unlikely that splitters would have same gini very often
		
	}


	public String toString() {
		if (best_splitter == null){
			return "untrained";
		}else {
			return best_splitter.toString();
		}
	}
	

	
//	//takes the max(gini_min * #class_root, gini_approx * n)
//	private Dataset sample(Dataset data, int approx_gini_min, boolean approx_gini_min_per_class, double approx_gini_percent) {
//		Dataset sample;
//		int sample_size = (int) (approx_gini_percent * data.size());
//		int min_sample_size;
//
//		//use number of classes in the root
//		if (approx_gini_min_per_class) {
//			min_sample_size = approx_gini_min * AppContext.getTraining_data().get_num_classes();
//			if (sample_size  < min_sample_size) {
//				sample_size = min_sample_size;
//			}
//		}else {
//			if (sample_size < approx_gini_min) {
//				sample_size = approx_gini_min;
//			}		
//		}
//		
//		sample = Sampler.uniform_sample(data, sample_size);
//		
//		return sample;
//	}


}
