package trees.splitters.old;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import application.AppConfig;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSCheifTree;
import trees.TSCheifTree.Node;
import util.Util;

public class RandomForestSplitter implements NodeSplitter{

	private final int NUM_CHILDREN = 2;
	private final int LEFT_BRANCH = 0;
	private final int RIGHT_BRANCH = 1;
	
	private int[] attributes;
	private double [][] thresholds;
	
	private TIntObjectMap<Dataset> best_split = null;
	private int best_attribute;
	private double best_threshold;
	
	private int m;// = 20; //number of instances(features) used to split
	
	public RandomForestSplitter(TSCheifTree.Node node) {
		//create criterias
		m = (int) Math.sqrt(AppConfig.getTrainingSet().length());
	}
	

	@Override
	public TIntObjectMap<Dataset> train_splitter(Dataset sample) throws Exception {

		//select m random attributes
		attributes = Util.non_repeating_rand(m, 0, sample.length(), AppConfig.getRand());

		//get column vector
		double[][] subset = get_attrib_vectors(sample, attributes);
		
		//sort the column vectors
		//get list of adjacent means
		thresholds = get_adjacent_means(subset);
		
		int parent_size = sample.size();	//TODO use node size or sample size here?
		
		double weighted_gini = Double.POSITIVE_INFINITY;
		double best_weighted_gini = Double.POSITIVE_INFINITY;
		TIntObjectMap<Dataset> splits = null;
		
		for (int i = 0; i < thresholds.length; i++) {
			for (int j = 0; j < thresholds[i].length; j++) {
				
//				System.out.print(i + " < " + thresholds[i][j] + ", ");
				splits = split_data(sample, attributes[i], thresholds[i][j]);
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
		
		return best_split;
	}

	@Override
	public int predict_by_splitter(double[] query) throws Exception {
		if (query[best_attribute] < best_threshold) {
			return 0;
		}else {
			return 1;
		}
	}	

	private double[][] get_attrib_vectors(Dataset sample, int[] attributes) {
		int size = sample.size();
		double[][] subset = new double[attributes.length][size];
		double[] series;

		for (int i = 0; i < size; i++) {
			series = sample.get_series(i);
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
	
	public TIntObjectMap<Dataset> split_data(Dataset sample, int attribute, double threshold) throws Exception {
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>(NUM_CHILDREN);	//0 = left node, 1 = right node
		int size = sample.size();
		double[] series;
		
		splits.put(LEFT_BRANCH, new ListDataset(sample.size(), sample.length()));	//TODO initial capacity too large, try optimize this
		splits.put(RIGHT_BRANCH, new ListDataset(sample.size(), sample.length()));	//TODO initial capacity too large, try optimize this		
		
		for (int i = 0; i < size; i++) {
			series = sample.get_series(i);
			
			if (series[attribute] < threshold) {
				splits.get(LEFT_BRANCH).add(sample.get_class(i), series);
			}else {
				splits.get(RIGHT_BRANCH).add(sample.get_class(i), series);
			}
		}
		return splits;
	}			
	
//	private ListDataset[] get_attrib_vectors_as_list(Dataset sample, int[] attributes) {
//		int size = sample.size();
//		ListDataset[] subset = new ListDataset[attributes.length];
//		double[] series;
//		Integer label;
//		
//		for (int i = 0; i < attributes.length; i++) {
//			subset[i] = new ListDataset(sample.size(), 1);
//		}
//		
//		for (int i = 0; i < size; i++) {
//			label = sample.get_class(i);
//			series = sample.get_series(i);
//			for (int j = 0; j < attributes.length; j++) {
//				double[] x = new double[1];
//				x[0] = series[j];
//				subset[j].add(label, x);
//			}
//		}
//		
//		return subset;
//	}
	


	
//	private int get_best_split_point_list(int[] labels) {
//		double best_gini = 2;
//		double gini = 1;
//		int total = labels.length;
//		int split_at = 0;
//		int left_count = 0;
//		int right_count = total;
//		
//		
//		for (int i = 0; i < total; i++) {
//			
//			gini = 1 - (left_count/total) - (right_count/total);
//			
//			if (gini < best_gini) {
//				best_gini = gini;
//				split_at = i;
//			}
//			
//			
//		}
//		
//		return split_at;
//	}
	
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
