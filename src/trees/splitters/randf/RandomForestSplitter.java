package trees.splitters.randf;

import application.AppConfig;
import application.AppConfig.FeatureSelectionMethod;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import gnu.trove.map.TIntObjectMap;
import trees.TSChiefNode;
import trees.splitters.RandomTreeSplitter;

public class RandomForestSplitter extends RandomTreeSplitter {

	public RandomForestSplitter(TSChiefNode node) throws Exception {
		super(node);
		node.tree.stats.randf_count++;
	}
	
	public RandomForestSplitter(TSChiefNode node, int m) throws Exception {
		super(node, m);
		node.tree.stats.randf_count++;		
	}
	
	public RandomForestSplitter(TSChiefNode node, FeatureSelectionMethod feature_selection_method) throws Exception {
		super(node, feature_selection_method);
		node.tree.stats.randf_count++;
	}

	@Override
	public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();

		if (AppConfig.randf_feature_selection == FeatureSelectionMethod.Sqrt) {
			numFeatures = (int) Math.sqrt(nodeTrainData.length());
		}else if (AppConfig.randf_feature_selection == FeatureSelectionMethod.Loge) {
			numFeatures = (int) Math.log(nodeTrainData.length());
		}else if (AppConfig.randf_feature_selection == FeatureSelectionMethod.Log2) {
			numFeatures = (int) (Math.log(nodeTrainData.length()/ Math.log(2)));
		}else {
			numFeatures = AppConfig.randf_m; //TODO verify
		}

		TIntObjectMap<Dataset> splits =  super.fit(nodeTrainData, trainIndices);
		this.node.tree.stats.randf_splitter_train_time += (System.nanoTime() - startTime);
		return splits;
	}

	@Override
	public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();

		TIntObjectMap<Dataset> splits = super.split(nodeTrainData, trainIndices);

		this.node.tree.stats.randf_splitter_train_time += (System.nanoTime() - startTime);
		return splits;
	}

	public String toString() {
		return "RandF[m=" + numFeatures + "]";
	}

	
}
