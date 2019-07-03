package trees.splitters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import core.AppContext;
import core.AppContext.FeatureSelectionMethod;
import core.AppContext.TransformLevel;
import datasets.TSDataset;
import datasets.TimeSeries;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import transforms.BOSS.BagOfPattern;
import transforms.TSF;
import trees.ProximityTree;
import trees.ProximityTree.Node;
import util.Util;

public class TSFSplitter implements NodeSplitter{

	private int m;//number of instances(features) used to split
	protected FeatureSelectionMethod feature_selection_method;

	GenericGiniSplitter binary_splitter;

	private Node node;
	TSDataset trans;
	
	protected TSF tsf_transformer;
	
	public TSFSplitter(ProximityTree.Node node) throws Exception {
		this.node = node;
		this.node.tree.stats.tsf_count++;

		m = 0;
		if (AppContext.tsf_feature_selection == FeatureSelectionMethod.ConstantInt) {
			binary_splitter = new GenericGiniSplitter(node, AppContext.tsf_m);	
		}else {
			binary_splitter = new GenericGiniSplitter(node, AppContext.tsf_feature_selection);	
		}

		binary_splitter = new GenericGiniSplitter(node, FeatureSelectionMethod.ConstantInt);	
			
	}
	
	public TSFSplitter(ProximityTree.Node node, int m) throws Exception {
		this.node = node;
		this.node.tree.stats.tsf_count++;

		this.m = m;
	}
	
	public TSFSplitter(ProximityTree.Node node, FeatureSelectionMethod feature_selection_method) throws Exception {
		this.node = node;
		this.node.tree.stats.tsf_count++;

		this.feature_selection_method = feature_selection_method;	
	}

	@Override
	public TIntObjectMap<TSDataset> train(TSDataset sample, int[] indices) throws Exception {
		

		
		if (AppContext.tsf_transform_level == TransformLevel.Node) {
			int num_intervals;
			
			if (AppContext.tsf_num_intervals <= 0) {
				num_intervals = (int) Math.sqrt(sample.length());
			}else {
				num_intervals = AppContext.tsf_num_intervals;
			}

			tsf_transformer = new TSF(num_intervals);
			tsf_transformer.fit(sample);
			trans = tsf_transformer.transform(sample);
		}else {
			TSDataset tree_data = this.node.tree.tsf_train_data;
			
			trans = new TSDataset();
			
//			for (int i = 0; i < tree_data.size(); i++) {
//				for (int j = 0; j < sample.size(); j++) {
//					if (sample.get_series(j) == tree_data.get_series(i).original_series) {
//						trans.add(tree_data.get_series(i));
//					}
//				}
//			}
			for (int j = 0; j < indices.length; j++) {
				trans.add(tree_data.get_series(indices[j]));
			}
			
			//assert sizes ==
			if (sample.size() != trans.size()) {
				throw new Exception ("TSF Splitter error");
			}			
		}
		
		
		if (AppContext.tsf_feature_selection == FeatureSelectionMethod.ConstantInt) {
			binary_splitter.m = trans.length(); //#attribs == 3 * num_intervals
 			if (AppContext.verbosity > 1) {
//				System.out.println("tsf m updated to: " + binary_splitter.m);
			}
		}
		
//		binary_splitter.randomize_attrib_selection = true;
		TIntObjectMap<TSDataset> splits = binary_splitter.train(trans, null);
		
		if (splits == null) {
			return null; //cant find a sensible split point for this data.
		}
		
		TIntObjectMap<TSDataset> original_data_splits = new TIntObjectHashMap<>();
		
		//extract original series
		for (int key : splits.keys()) {
			original_data_splits.put(key, to_original_set(splits.get(key)));
		}
		
		
		if (SplitEvaluator.has_empty_split(original_data_splits)) {
//			throw new Exception("empty splits found! check for bugs");
		}
		
		return original_data_splits;		
	}
	
	@Override
	public void train_binary(TSDataset data) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TIntObjectMap<TSDataset> split(TSDataset data, int[] indices) throws Exception {
		
		TIntObjectMap<TSDataset> splits = binary_splitter.split(trans, indices);
		
		TIntObjectMap<TSDataset> original_data_splits = new TIntObjectHashMap<>();
		
		//extract original series
		for (int key : splits.keys()) {
			original_data_splits.put(key, to_original_set(splits.get(key)));
		}
		
		
		if (SplitEvaluator.has_empty_split(original_data_splits)) {
//			throw new Exception("empty splits found! check for bugs");
		}
		
		return original_data_splits;
	}
	
	private TSDataset to_original_set(TSDataset transformed_set) {
		TSDataset split = new TSDataset();
		
		for (int i = 0; i < transformed_set.size(); i++) {
			split.add(transformed_set.get_series(i).original_series);
		}
		
		return split;
	}
	
	@Override
	public int predict(TimeSeries query, int queryIndex) throws Exception {
		TimeSeries transformed_query;
		
		if(AppContext.tsf_transform_level == TransformLevel.Node) {
			transformed_query = this.tsf_transformer.transformSeries(query);
		}else {
			transformed_query = this.node.tree.tsf_test_data.get_series(queryIndex);
		}
		
		
		return binary_splitter.predict(transformed_query, queryIndex);
	}	

	public double gini(List<Integer> labels) {

		Map<Integer, Integer> class_map = new HashMap<Integer, Integer>();
		Integer key;
		
		for (int i = 0; i < labels.size(); i++) {
			key = labels.get(i);
			
			if (class_map.containsKey(key)) {
				class_map.put(key, class_map.get(key) + 1);
			}else {
				class_map.put(key, 1);
			}
		}
		
		double sum = 0;
		double p;
		int total_size = labels.size();
		
		for (Entry<Integer, Integer> entry: class_map.entrySet()) {
			p = (double)entry.getValue() / total_size;
			sum += p * p;
		}
		
		return 1 - sum;
	}
	
	public String toString() {
		return "TSF[]";
	}




	
}
