package trees.splitters.old;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import application.AppConfig;
import application.AppConfig.FeatureSelectionMethod;
import data.timeseries.TimeSeries;
import data.timeseries.UTSDataset;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSCheifTree;
import trees.TSCheifTree.Node;
import util.Util;

public class RandomForestSplitter2 implements NodeSplitter{

	private final int NUM_CHILDREN = 2;
	private final int LEFT_BRANCH = 0;
	private final int RIGHT_BRANCH = 1;
	
	private int[] attributes;
	private double [][] thresholds;
	
	private TIntObjectMap<UTSDataset> best_split = null;
	private int best_attribute;
	private double best_threshold;
	
	private int m;//number of instances(features) used to split
	
	public RandomForestSplitter2(TSCheifTree.Node node) throws Exception {
		node.tree.stats.randf_count++;
	}
	

	@Override
	public TIntObjectMap<UTSDataset> train(UTSDataset sample, int[] indices) throws Exception {

		
		if (AppConfig.randf_feature_selection == FeatureSelectionMethod.Sqrt) {
			m = (int) Math.sqrt(sample.length());
		}else if (AppConfig.randf_feature_selection == FeatureSelectionMethod.Loge) {
			m = (int) Math.log(sample.length());
		}else if (AppConfig.randf_feature_selection == FeatureSelectionMethod.Log2) {
			m = (int) (Math.log(sample.length()/ Math.log(2))); 
		}else {
			m = AppConfig.randf_m; //TODO verify
		}		
		
		//select m random attributes
		attributes = Util.non_repeating_rand(m, 0, sample.length(), AppConfig.getRand());	//TODO check seed, this randomness differentiates RandF splitters

		//get column vector
		double[][] subset = get_attrib_vectors(sample, attributes);
		
		//sort the column vectors
		//get list of adjacent means
		thresholds = get_adjacent_means(subset);
		
		int parent_size = sample.size();	//TODO use node size or sample size here?
		
		double weighted_gini = Double.POSITIVE_INFINITY;
		double best_weighted_gini = Double.POSITIVE_INFINITY;
		TIntObjectMap<UTSDataset> splits = null;
		
		for (int i = 0; i < thresholds.length; i++) {
			for (int j = 0; j < thresholds[i].length; j++) {
				
//				System.out.print(i + " < " + thresholds[i][j] + ", ");
				splits = split(sample, attributes[i], thresholds[i][j]);
				weighted_gini = Util.weighted_gini(splits, parent_size);

				if (weighted_gini <  best_weighted_gini) {
					best_weighted_gini = weighted_gini;
					best_split = splits;
					best_attribute = attributes[i];
					best_threshold = thresholds[i][j];
				}
			}
		}
//		System.out.println();		
		
	}
	
	@Override
	public void train_binary(UTSDataset data) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public TIntObjectMap<UTSDataset> split(UTSDataset sample, int attribute, double threshold) throws Exception {
		TIntObjectMap<UTSDataset> splits = new TIntObjectHashMap<UTSDataset>(NUM_CHILDREN);	//0 = left node, 1 = right node
		int size = sample.size();
		TimeSeries series;
		
		splits.put(LEFT_BRANCH, new UTSDataset(sample.size(), sample.length()));	//TODO initial capacity too large, try optimize this
		splits.put(RIGHT_BRANCH, new UTSDataset(sample.size(), sample.length()));	//TODO initial capacity too large, try optimize this		
		
		for (int i = 0; i < size; i++) {
			series = sample.get_series(i);
			
			if (series.getData()[attribute] < threshold) {
				splits.get(LEFT_BRANCH).add(series);
			}else {
				splits.get(RIGHT_BRANCH).add(series);
			}
		}
		return splits;
	}
	


	@Override
	public TIntObjectMap<UTSDataset> split(UTSDataset data, int[] indices) throws Exception {
		return split(data, best_attribute, best_threshold);
	}
	
	@Override
	public int predict(TimeSeries query, int queryIndex) throws Exception {
		if (query.getData()[best_attribute] < best_threshold) {
			return 0;
		}else {
			return 1;
		}
	}	

	private double[][] get_attrib_vectors(UTSDataset sample, int[] attributes) {
		int size = sample.size();
		double[][] subset = new double[attributes.length][size];
		double[] series;

		for (int i = 0; i < size; i++) {
			series = sample.get_series(i).getData();
			for (int j = 0; j < attributes.length; j++) {
				subset[j][i] = series[attributes[j]];
			}
		}
		
		return subset;
	}	
	
	private double[][] get_adjacent_means(double[][] subset) {
		int size = subset[0].length;
		double[][] adjacent_means = new double[subset.length][size -1];
		int len;
		
		for (int i = 0; i < subset.length; i++) {
			Arrays.sort(subset[i]);
			
			len = subset[i].length - 1;
			for (int j = 0; j < len; j++) {
				adjacent_means[i][j] = (subset[i][j] + subset[i][j + 1]) / 2.0;
			}
		}
		
		return adjacent_means;
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
		return "RandF[m=" + m + "]";
	}




	
}
