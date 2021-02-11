package trees.splitters.rise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import application.AppConfig;
import application.AppConfig.FeatureSelectionMethod;
import application.AppConfig.RifFilters;
import application.AppConfig.TransformLevel;
import core.exceptions.NotImplementedException;
import data.timeseries.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSChiefNode;
import trees.splitters.RandomTreeOnIndexSplitter;
import trees.splitters.NodeSplitter;

public class RISESplitter implements NodeSplitter{

	protected int numFeatures;//number of instances(features) used to split
	protected FeatureSelectionMethod featureSelectionMethod;
	protected TSChiefNode node;
	RandomTreeOnIndexSplitter binarySplitter;
	protected RISETransform riseTransform;
	public RifFilters filterType;
	
	public RISESplitter(TSChiefNode node, RifFilters filter_type) throws Exception {
		this.node = node;
		this.node.tree.stats.rif_count++;
		this.filterType = filter_type;
		
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

		numFeatures = 0;
		
		if (AppConfig.rif_feature_selection == FeatureSelectionMethod.ConstantInt) {
			binarySplitter = new RandomTreeOnIndexSplitter(node, AppConfig.rif_m);
		}else {
			binarySplitter = new RandomTreeOnIndexSplitter(node, AppConfig.rif_feature_selection);
		}

		binarySplitter = new RandomTreeOnIndexSplitter(node, FeatureSelectionMethod.ConstantInt);

	}
	
	public RISESplitter(TSChiefNode node, RifFilters filter_type, int numFeatures) throws Exception {
		this.node = node;
		this.node.tree.stats.rif_count++;
		this.featureSelectionMethod = FeatureSelectionMethod.ConstantInt;
		this.numFeatures = numFeatures;
		
		this.filterType = filter_type;
		
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
		
		binarySplitter = new RandomTreeOnIndexSplitter(node, FeatureSelectionMethod.ConstantInt);
		binarySplitter.setNumFeatures(this.numFeatures);
	}
	
	public RISESplitter(TSChiefNode node, FeatureSelectionMethod featureSelectionMethod) throws Exception {
		this.node = node;
		this.node.tree.stats.rif_count++;

		this.featureSelectionMethod = featureSelectionMethod;
	}

	@Override
	public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();

		riseTransform = new RISETransform(this.filterType, AppConfig.rif_num_intervals, AppConfig.rif_splitters_per_node / 4);
		riseTransform.fit(nodeTrainData);
		Dataset transformedNodeTrainData = riseTransform.transform(nodeTrainData);

		//TODO CHECK THIS -- assumes that its already set in the constructor
		if (AppConfig.rif_feature_selection == FeatureSelectionMethod.ConstantInt) {
			
//			int num_intervals = (int) Math.ceil((float)num_gini_per_splitter / (float)AppContext.rif_min_interval); // 2 = ceil(12 / 9)
//			int max_attribs_needed_per_rif_type = (int) Math.ceil(num_gini_per_splitter / num_intervals);
//			binary_splitter.m = Math.min(trans.length(), max_attribs_needed_per_rif_type);// 6 = 12 / 2
			
			binarySplitter.setNumFeatures(Math.min(transformedNodeTrainData.length(), this.numFeatures));// 6 = 12 / 2

			if (AppConfig.verbosity > 1) {
//				System.out.println("rif m updated to: " + binary_splitter.m);
			}
		}

		Dataset fullTrainSet = AppConfig.getTrainingSet();
		trainIndices.setDataset(fullTrainSet); //TODO already set
		TIntObjectMap<TIntArrayList> splitIndices = binarySplitter.fitByIndices(transformedNodeTrainData, trainIndices);
		if (splitIndices == null) {
			return null; //cant find a sensible split point for this data.
		}

		//TODO slow -- optimize this
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>();
		for (int k : splitIndices.keys()) {
			TIntArrayList ind = splitIndices.get(k);
			Dataset temp = new MTSDataset(ind.size());
			for (int i = 0; i < ind.size(); i++) {
				temp.add(fullTrainSet.getSeries(ind.get(i)));
			}
			splits.putIfAbsent(k, temp);
		}

		//time is measured separately for split function from train function as it can be called separately -- to prevent double counting
		this.node.tree.stats.rise_splitter_train_time += (System.nanoTime() - startTime);
		
		if (TSChiefNode.has_empty_split(splits)) {
//			throw new Exception("empty splits found! check for bugs");
		}

		return splits;
	}

	@Override
	public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		Dataset transformedTrainData = riseTransform.transform(nodeTrainData);

		TIntObjectMap<TIntArrayList> splitIndices = binarySplitter.splitByIndices(transformedTrainData, trainIndices);

		Dataset fullTrainSet = AppConfig.getTrainingSet();
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>();
		for (int k : splitIndices.keys()) {
			TIntArrayList ind = splitIndices.get(k);
			Dataset temp = new MTSDataset(ind.size());
			for (int i = 0; i < ind.size(); i++) {
				temp.add(fullTrainSet.getSeries(ind.get(i)));
			}
			splits.putIfAbsent(k, temp);
		}
		
		//time is measured separately for split function from train function as it can be called separately -- to prevent double counting 
		this.node.tree.stats.rise_splitter_train_time += (System.nanoTime() - startTime);

		return splits;
	}
	
	@Override
	public int predict(TimeSeries query, Dataset testData, int queryIndex) throws Exception {
		TimeSeries transformed_query;

		if(AppConfig.tsf_transform_level == TransformLevel.Node) {
			transformed_query = this.riseTransform.transformSeries(query);
		}else {
//			transformed_query = this.node.tree.treeLevelRiseTransformedTestData.getSeries(queryIndex);
			throw new NotImplementedException();
		}		
				
		return binarySplitter.predict(transformed_query, testData, queryIndex);
	}

	@Override
	public TIntObjectMap<TIntArrayList> fitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public TIntObjectMap<TIntArrayList> splitByIndices(Dataset allTrainData, Indexer trainIndices) throws Exception {
		throw new NotImplementedException();
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
		if (this.riseTransform != null) {

			return "RISE[" + this.riseTransform.toString() +  ", M:" + numFeatures + "," + binarySplitter + "]";
		}else {
			return "RISESplitter[" + this.filterType + ". untrained]";
		}
		
	}

}
