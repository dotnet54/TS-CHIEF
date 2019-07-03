package trees.splitters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import core.AppContext;
import core.AppContext.FeatureSelectionMethod;
import core.AppContext.RifFilters;
import core.AppContext.TransformLevel;
import datasets.TSDataset;
import datasets.TimeSeries;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import transforms.RIF;
import transforms.TSF;
import trees.ProximityTree;
import trees.ProximityTree.Node;

public class RIFSplitter implements NodeSplitter{

	private int m;//number of instances(features) used to split
	protected FeatureSelectionMethod feature_selection_method;

	GenericGiniSplitter binary_splitter;

	private Node node;
	TSDataset trans;
	
	protected RIF rif_transformer;
	public RifFilters filter_type;
	
	public RIFSplitter(ProximityTree.Node node, RifFilters filter_type) throws Exception {
		this.node = node;
		this.node.tree.stats.rif_count++;
		this.filter_type = filter_type;
		
		if (filter_type.equals(RifFilters.ACF)) {
			this.node.tree.stats.rif_acf_count++;
		}else if (filter_type.equals(RifFilters.PACF)) {
			this.node.tree.stats.rif_pacf_count++;
		}else if (filter_type.equals(RifFilters.ARMA)) {
			this.node.tree.stats.rif_arma_count++;
		}else if (filter_type.equals(RifFilters.PS)) {
			this.node.tree.stats.rif_ps_count++;
		}else if (filter_type.equals(RifFilters.DFT)) {
			this.node.tree.stats.rif_dft_count++;
		}

		m = 0;
		
		if (AppContext.rif_feature_selection == FeatureSelectionMethod.ConstantInt) {
			binary_splitter = new GenericGiniSplitter(node, AppContext.rif_m);	
		}else {
			binary_splitter = new GenericGiniSplitter(node, AppContext.rif_feature_selection);	
		}

		binary_splitter = new GenericGiniSplitter(node, FeatureSelectionMethod.ConstantInt);	

	}
	
	public RIFSplitter(ProximityTree.Node node, RifFilters filter_type, int m) throws Exception {
		this.node = node;
		this.node.tree.stats.rif_count++;
		this.feature_selection_method = FeatureSelectionMethod.ConstantInt;
		this.m = m;
		
		this.filter_type = filter_type;
		
		if (filter_type.equals(RifFilters.ACF)) {
			this.node.tree.stats.rif_acf_count++;
		}else if (filter_type.equals(RifFilters.PACF)) {
			this.node.tree.stats.rif_pacf_count++;
		}else if (filter_type.equals(RifFilters.ARMA)) {
			this.node.tree.stats.rif_arma_count++;
		}else if (filter_type.equals(RifFilters.PS)) {
			this.node.tree.stats.rif_ps_count++;
		}else if (filter_type.equals(RifFilters.DFT)) {
			this.node.tree.stats.rif_dft_count++;
		}
		
		binary_splitter = new GenericGiniSplitter(node, FeatureSelectionMethod.ConstantInt);	
		binary_splitter.m = this.m;
	}
	
	public RIFSplitter(ProximityTree.Node node, FeatureSelectionMethod feature_selection_method) throws Exception {
		this.node = node;
		this.node.tree.stats.rif_count++;

		this.feature_selection_method = feature_selection_method;	
	}

	@Override
	public TIntObjectMap<TSDataset> train(TSDataset sample, int[] indices) throws Exception {
		long startTime = System.nanoTime();
		
		if (AppContext.rif_transform_level == TransformLevel.Node) {
			rif_transformer = new RIF(this.filter_type, AppContext.rif_num_intervals, AppContext.rif_splitters_per_node / 4);
			rif_transformer.fit(sample);
			trans = rif_transformer.transform(sample);
		}else {
			TSDataset tree_data = this.node.tree.rif_train_data;
			
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
				throw new Exception ("RIF Splitter error");
			}			
		}

		
		//TODO temp test
//		binary_splitter.feature_selection_method = FeatureSelectionMethod.Sqrt;
		
		//TODO fix this
		//updating m to use approx same number of ginis
		if (AppContext.rif_feature_selection == FeatureSelectionMethod.ConstantInt) {
			
//			int num_gini_per_splitter = AppContext.rif_splitters_per_node / 4;	// eg. if we need 12 gini per type 12 ~= 50/4 
//			int num_intervals = (int) Math.ceil((float)num_gini_per_splitter / (float)AppContext.rif_min_interval); // 2 = ceil(12 / 9)					
//			int max_attribs_needed_per_rif_type = (int) Math.ceil(num_gini_per_splitter / num_intervals);
//			binary_splitter.m = Math.min(trans.length(), max_attribs_needed_per_rif_type);// 6 = 12 / 2
			
			binary_splitter.m = Math.min(trans.length(), this.m);// 6 = 12 / 2

			if (AppContext.verbosity > 1) {
//				System.out.println("rif m updated to: " + binary_splitter.m);
			}
		}

		binary_splitter.randomize_attrib_selection = true;
		TIntObjectMap<TSDataset> splits = binary_splitter.train(trans, null);
		
		if (splits == null) {
			return null; //cant find a sensible split point for this data.
		}
		
		TIntObjectMap<TSDataset> original_data_splits = new TIntObjectHashMap<>();
		
		//extract original series
		for (int key : splits.keys()) {
			original_data_splits.put(key, to_original_set(splits.get(key)));
		}
		
		
		//time is measured separately for split function from train function as it can be called separately -- to prevent double counting 
		this.node.tree.stats.rise_splitter_train_time += (System.nanoTime() - startTime);
		
		if (SplitEvaluator.has_empty_split(original_data_splits)) {
//			throw new Exception("empty splits found! check for bugs");
		}

		return original_data_splits;		
	}
	
	@Override
	public void train_binary(TSDataset data) throws Exception {
		
	}

	@Override
	public TIntObjectMap<TSDataset> split(TSDataset data, int[] indices) throws Exception {
		long startTime = System.nanoTime();
		
		TIntObjectMap<TSDataset> splits = binary_splitter.split(trans, indices);
		
		TIntObjectMap<TSDataset> original_data_splits = new TIntObjectHashMap<>();
		
		//extract original series
		for (int key : splits.keys()) {
			original_data_splits.put(key, to_original_set(splits.get(key)));
		}
		
		
		//time is measured separately for split function from train function as it can be called separately -- to prevent double counting 
		this.node.tree.stats.rise_splitter_train_time += (System.nanoTime() - startTime);
		
		
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
			transformed_query = this.rif_transformer.transformSeries(query);
		}else {
			transformed_query = this.node.tree.rif_test_data.get_series(queryIndex);
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
		if (this.rif_transformer != null) {
			
			int len = 0;
			if (this.trans !=  null) {
				len = this.trans.length();
			}
			return "RIF[" + this.rif_transformer.toString() +  ", L:" + len + "," + binary_splitter + "]";
		}else {
			return "RIFSplitter[" + this.filter_type + ". untrained]";
		}
		
	}

}
