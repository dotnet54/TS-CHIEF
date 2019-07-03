package trees.splitters;

import core.AppContext;
import core.AppContext.FeatureSelectionMethod;
import datasets.TSDataset;
import datasets.TimeSeries;
import gnu.trove.map.TIntObjectMap;
import trees.ProximityTree;

public class RandomForestSplitter extends GenericGiniSplitter{

	public RandomForestSplitter(ProximityTree.Node node) throws Exception {
		super(node);
		node.tree.stats.randf_count++;
	}
	
	public RandomForestSplitter(ProximityTree.Node node, int m) throws Exception {
		super(node, m);
		node.tree.stats.randf_count++;		
	}
	
	public RandomForestSplitter(ProximityTree.Node node, FeatureSelectionMethod feature_selection_method) throws Exception {
		super(node, feature_selection_method);
		node.tree.stats.randf_count++;
	}

	@Override
	public TIntObjectMap<TSDataset> train(TSDataset data, int[] indices) throws Exception {

		
		if (AppContext.randf_feature_selection == FeatureSelectionMethod.Sqrt) {
			m = (int) Math.sqrt(data.length());
		}else if (AppContext.randf_feature_selection == FeatureSelectionMethod.Loge) {
			m = (int) Math.log(data.length());
		}else if (AppContext.randf_feature_selection == FeatureSelectionMethod.Log2) {
			m = (int) (Math.log(data.length()/ Math.log(2))); 
		}else {
			m = AppContext.randf_m; //TODO verify
		}		

		return super.train(data, indices);
		
	}
	
	@Override
	public void train_binary(TSDataset data) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TIntObjectMap<TSDataset> split(TSDataset data, int[] indices) throws Exception {
		return super.split(data, indices);
	}
	
	@Override
	public int predict(TimeSeries query, int queryIndex) throws Exception {
		return super.predict(query, queryIndex);
	}	

	public String toString() {
		return "RandF[m=" + m + "]";
	}




	
}
