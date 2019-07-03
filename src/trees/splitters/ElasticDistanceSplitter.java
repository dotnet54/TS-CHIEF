package trees.splitters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import peersim.util.WeightedRandPerm;

import core.AppContext;
import datasets.TSDataset;
import datasets.TimeSeries;
import distance.elastic.DistanceMeasure;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.ProximityTree;
import trees.ProximityTree.Node;
import util.Sampler;
import util.Util;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ElasticDistanceSplitter implements NodeSplitter{
	
	protected DistanceMeasure distance_measure;
	protected TIntObjectMap<double[]> exemplars;
	protected TIntObjectMap<TSDataset> splits;	

	protected ProximityTree.Node node;
	
	private double best_threshold=Double.POSITIVE_INFINITY;
	
	public ElasticDistanceSplitter(ProximityTree.Node node) throws Exception {
		this.node = node;
		this.node.tree.stats.ee_count++;
	}
	
	public TIntObjectMap<TSDataset> train(TSDataset data, int[] indices) throws Exception {
		long startTime = System.nanoTime();

		TIntObjectMap<TSDataset> data_per_class = data.split_by_class();	//TODO can be done once at node
		
		if (AppContext.random_dm_per_node) {
			int r = ThreadLocalRandom.current().nextInt(AppContext.enabled_distance_measures.length);
			distance_measure = new DistanceMeasure(AppContext.enabled_distance_measures[r], node);		
		}else {
			distance_measure = node.tree.tree_distance_measure;
		}		
		
		distance_measure.select_random_params(data, AppContext.getRand());
		exemplars = new TIntObjectHashMap<double[]>(data.get_num_classes());
//		splits = new TIntObjectHashMap<Dataset>(data.get_num_classes());

		
		int branch = 0;
		for (int key : data_per_class.keys()) {
			int r = ThreadLocalRandom.current().nextInt(data_per_class.get(key).size());
			
			exemplars.put(branch, data_per_class.get(key).get_series(r).getData());
			branch++;
		}
		
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.ee_splitter_train_time += (System.nanoTime() - startTime);
		
		return split(data, indices);
	}
	
	public void train_binary(TSDataset data) throws Exception {
		
		throw new Exception("Not implemented -- experimental");
	}
	
	public TIntObjectMap<TSDataset> split(TSDataset data, int[] indices) throws Exception {
		long startTime = System.nanoTime();

		if (best_threshold!=Double.POSITIVE_INFINITY)
		{	
			return split(data,best_threshold);
		}
		TIntObjectMap<TSDataset> splits = new TIntObjectHashMap<TSDataset>(data.get_num_classes());
		int data_size = data.size();
		int closest_branch;
		
		for (int j = 0; j < data_size; j++) {
			closest_branch = find_closest_exemplar(data.get_series(j).getData(), distance_measure, exemplars);
			
			if (! splits.containsKey(closest_branch)){
				// initial capacity based on class distributions, this may be an over or an underestimate, but better than 0 initial capacity
				splits.put(closest_branch, new TSDataset(data.get_class_map().get(closest_branch), data.length())); 
			}
			
			splits.get(closest_branch).add(data.get_series(j));
		}

		//time is measured separately for split function from train function as it can be called separately -- to prevent double counting 
		this.node.tree.stats.ee_splitter_train_time += (System.nanoTime() - startTime);
		
		return splits;
	}
	
	public TIntObjectMap<TSDataset> split(TSDataset data, double threshold) throws Exception {
		int nb_splits = 2;
		TIntObjectMap<TSDataset> splits = new TIntObjectHashMap<TSDataset>(nb_splits);
		int data_size = data.size();
		
		int[] key_val = new int[nb_splits];
		int cmpt = 0;
		for (int key : exemplars.keys()) {
			key_val[cmpt] = key;
			cmpt++;
		}

		for (int j = 0; j < data_size; j++) {
			double[] query = data.get_series(j).getData();
			double dist_diff = 0;
			
			cmpt = 0;
			for (int key : exemplars.keys()) {
				double[] exemplar = exemplars.get(key);
	
				if (AppContext.config_skip_distance_when_exemplar_matches_query && exemplar == query) {
					if (cmpt==0) {dist_diff = 0;};
				}
				if (cmpt==0) {
					dist_diff = distance_measure.distance(query, exemplar);
				}
				else {
					dist_diff = dist_diff-distance_measure.distance(query, exemplar);
				}
				cmpt++;
			}
			if (dist_diff<threshold) {
				if (! splits.containsKey(key_val[0])){
					// initial capacity based on class distributions, this may be an over or an underestimate, but better than 0 initial capacity
					splits.put(key_val[0], new TSDataset(data.get_class_map().get(key_val[0]), data.length()));
				}
				splits.get(key_val[0]).add(data.get_series(j));
			} else {
				if (! splits.containsKey(key_val[1])){
					// initial capacity based on class distributions, this may be an over or an underestimate, but better than 0 initial capacity
					splits.put(key_val[1], new TSDataset(data.get_class_map().get(key_val[1]), data.length()));
				}
				splits.get(key_val[1]).add(data.get_series(j));
			}			
		}		
		return splits;
	}
	
	

	//just to reuse this data structure
	List<Integer> closest_nodes = new ArrayList<Integer>();
	
	private synchronized int find_closest_exemplar(
			double[] query, DistanceMeasure distance_measure,
			TIntObjectMap<double[]> exemplars) throws Exception{
		closest_nodes.clear();
		double dist = Double.POSITIVE_INFINITY;
		double bsf = Double.POSITIVE_INFINITY;		

		for (int key : exemplars.keys()) {
			double[] exemplar = exemplars.get(key);

			if (AppContext.config_skip_distance_when_exemplar_matches_query && exemplar == query) {
				return key;
			}
							
			dist = distance_measure.distance(query, exemplar);
			
			if (dist < bsf) {
				bsf = dist;
				closest_nodes.clear();
				closest_nodes.add(key);
			}else if (dist == bsf) {
//				if (distance == min_distance) {
//					System.out.println("min distances are same " + distance + ":" + min_distance);
//				}
				bsf = dist;
				closest_nodes.add(key);
			}			
		}
		
		int r = ThreadLocalRandom.current().nextInt(closest_nodes.size());
		return closest_nodes.get(r);
	}
	
	
	public int predict(TimeSeries query, int queryIndex) throws Exception{
		return find_closest_exemplar(query.getData(), distance_measure, exemplars);
	}		
	
	public DistanceMeasure get_dm() {
		return this.distance_measure;
	}
	
	public String toString() {
		if (distance_measure == null) {
			return "EE[untrained]";
		}else {
			return "EE[" + distance_measure.toString() + "]"; //,e={" + "..." + "}
		}		
	}
}
