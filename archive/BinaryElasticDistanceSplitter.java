package trees.splitters.ee;

import application.AppConfig;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import data.timeseries.MTSDataset;
import data.timeseries.TimeSeries;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import peersim.util.WeightedRandPerm;
import trees.TSChiefNode;
import trees.splitters.NodeSplitter;
import trees.splitters.ee.measures.DistanceMeasure;
import util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class BinaryElasticDistanceSplitter implements NodeSplitter{

	protected TSChiefNode node;
	protected DistanceMeasure distanceMeasure;
	protected TIntObjectMap<double[][]> exemplars;
	protected TIntObjectMap<Dataset> splits;

	public BinaryElasticDistanceSplitter(TSChiefNode node) throws Exception {
		this.node = node;
		this.node.tree.stats.ee_count++;
	}
	
	public TIntObjectMap<Dataset> fit(Dataset trainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		TIntObjectMap<Dataset> dataPerClass = trainData.splitByClass();	//TODO can be done once at node

		if (AppConfig.random_dm_per_node) {
			int r = AppConfig.getRand().nextInt(AppConfig.enabled_distance_measures.length);
			distanceMeasure = new DistanceMeasure(AppConfig.enabled_distance_measures[r], node);
		}else {
			distanceMeasure = node.tree.treeDistanceMeasure;
		}

		distanceMeasure.select_random_params(trainData, AppConfig.getRand());
		exemplars = new TIntObjectHashMap<double[][]>(trainData.getNumClasses());

		int branch = 0;
		for (int key : dataPerClass.keys()) {
			int r = AppConfig.getRand().nextInt(dataPerClass.get(key).size());
			exemplars.put(branch, dataPerClass.get(key).getSeries(r).data());
			branch++;
		}

		//measure time before split function as time is measured separately for split function as it can be
		//called separately --to prevent double counting
		this.node.tree.stats.ee_splitter_train_time += (System.nanoTime() - startTime);
		return split(trainData, trainIndices);
	}

	private TIntObjectMap<Dataset> fitUsingNearestClass(Dataset trainData, Indexer trainIndices) throws Exception{

	}

	private TIntObjectMap<Dataset> fitUsingBinarySplit(Dataset trainData, Indexer trainIndices) throws Exception{

		int nb_splits = 2;
		int data_size = trainData.size();

		TIntObjectMap<Dataset> data_per_class = trainData.splitByClass();	//TODO can be done once at node

		if (AppConfig.random_dm_per_node) {
			int r = AppConfig.getRand().nextInt(AppConfig.enabled_distance_measures.length);
			distanceMeasure = new DistanceMeasure(AppConfig.enabled_distance_measures[r], node);
		}else {
			distanceMeasure = node.tree.treeDistanceMeasure;
		}

		distanceMeasure.select_random_params(trainData, AppConfig.getRand());
		exemplars = new TIntObjectHashMap<double[]>(nb_splits);
//		splits = new TIntObjectHashMap<Dataset>(data.get_num_classes());
		int[] key_val = new int[data_per_class.size()];
		int n = 0;
		for (int key : data_per_class.keys()) {
			key_val[n] = key;
			n++;
		}

		int[] l = new int[nb_splits];
		if (trainData.getNumClasses()<=nb_splits) {
			for (int i=0; i<trainData.getNumClasses(); i++) {
				l[i] = key_val[i];
			}
		}
		else {
			double[] weights = new double[trainData.getNumClasses()];
			for (int i=0; i<trainData.getNumClasses(); i++) {
				weights[i] = (double)data_per_class.get(key_val[i]).size()/(double)data_size;
			}
			WeightedRandPerm weight_rand_perm = new WeightedRandPerm(AppConfig.getRand(), weights);
			weight_rand_perm.reset(weights.length);
			for (int i=0; i<nb_splits; i++) {
				l[i] = key_val[weight_rand_perm.next()];
			}
		}

		// Select the exemplars
		for (int branch = 0; branch<nb_splits;  branch++) {
			int r = AppConfig.getRand().nextInt(data_per_class.get(l[branch]).size());
			exemplars.put(branch, data_per_class.get(l[branch]).getSeries(r).data());
		}


		// Define the best threshold
		if (AppConfig.gini_split) {

			//-- 1. compute all the distance
			double[] dist_diff = new double[data_size];
			for (int j = 0; j < data_size; j++) {
				double[] query = trainData.getSeries(j).data();

				dist_diff[j] = 0;
				int cmpt = 0;
				for (int key : exemplars.keys()) {
					double[] exemplar = exemplars.get(key);

					if (AppConfig.config_skip_distance_when_exemplar_matches_query && exemplar == query) {
						if (cmpt==0) {dist_diff[j] = 0;};
					}
					if (cmpt==0) {
						dist_diff[j] = distanceMeasure.distance(query, exemplar);
					}
					else {
						dist_diff[j] = dist_diff[j]- distanceMeasure.distance(query, exemplar);
					}
					cmpt++;
				}
			}

			//-- 2. find the best threshold values
			TIntObjectMap<MTSDataset> splits = null;
			double threshold;
			double weighted_gini = Double.POSITIVE_INFINITY;
			double best_weighted_gini = Double.POSITIVE_INFINITY;
			for (int j = 0; j < data_size-1; j++) {
				threshold = (dist_diff[j]+dist_diff[j+1])/2.0;
				splits = split(trainData, threshold);
				weighted_gini = Util.weighted_gini(splits, data_size);

				if (weighted_gini <  best_weighted_gini) {
					best_weighted_gini = weighted_gini;
					binarySplitBestThreshold = threshold;
				}

			}
		}
	}

	
	public TIntObjectMap<Dataset> split(Dataset data, int[] indices) throws Exception {
		long startTime = System.nanoTime();

		if (binarySplitBestThreshold !=Double.POSITIVE_INFINITY)
		{	
			return split(data, binarySplitBestThreshold);
		}
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>(data.getNumClasses());
		int data_size = data.size();
		int closest_branch;
		
		for (int j = 0; j < data_size; j++) {
			closest_branch = find_closest_exemplar(data.getSeries(j).data(), distanceMeasure, exemplars);
			
			if (! splits.containsKey(closest_branch)){
				// initial capacity based on class distributions, this may be an over or an underestimate, but better than 0 initial capacity
				splits.put(closest_branch, new MTSDataset(data.getClassDistribution().get(closest_branch), data.length()));
			}
			
			splits.get(closest_branch).add(data.getSeries(j));
		}

		//time is measured separately for split function from train function as it can be called separately -- to prevent double counting 
		this.node.tree.stats.ee_splitter_train_time += (System.nanoTime() - startTime);
		
		return splits;
	}
	
	public TIntObjectMap<Dataset> split(Dataset data, double threshold) throws Exception {
		int nb_splits = 2;
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>(nb_splits);
		int data_size = data.size();
		
		int[] key_val = new int[nb_splits];
		int cmpt = 0;
		for (int key : exemplars.keys()) {
			key_val[cmpt] = key;
			cmpt++;
		}

		for (int j = 0; j < data_size; j++) {
			double[] query = data.getSeries(j).data();
			double dist_diff = 0;
			
			cmpt = 0;
			for (int key : exemplars.keys()) {
				double[] exemplar = exemplars.get(key);
	
				if (AppConfig.config_skip_distance_when_exemplar_matches_query && exemplar == query) {
					if (cmpt==0) {dist_diff = 0;};
				}
				if (cmpt==0) {
					dist_diff = distanceMeasure.distance(query, exemplar);
				}
				else {
					dist_diff = dist_diff- distanceMeasure.distance(query, exemplar);
				}
				cmpt++;
			}
			if (dist_diff<threshold) {
				if (! splits.containsKey(key_val[0])){
					// initial capacity based on class distributions, this may be an over or an underestimate, but better than 0 initial capacity
					splits.put(key_val[0], new MTSDataset(data.getClassDistribution().get(key_val[0])));
				}
				splits.get(key_val[0]).add(data.getSeries(j));
			} else {
				if (! splits.containsKey(key_val[1])){
					// initial capacity based on class distributions, this may be an over or an underestimate, but better than 0 initial capacity
					splits.put(key_val[1], new MTSDataset(data.getClassDistribution().get(key_val[1])));
				}
				splits.get(key_val[1]).add(data.getSeries(j));
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

			if (AppConfig.config_skip_distance_when_exemplar_matches_query && exemplar == query) {
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
		
		int r = AppConfig.getRand().nextInt(closest_nodes.size());
		return closest_nodes.get(r);
	}
	
	
	public int predict(TimeSeries query, int queryIndex) throws Exception{
		return find_closest_exemplar(query.data(), distanceMeasure, exemplars);
	}		
	
	public DistanceMeasure get_dm() {
		return this.distanceMeasure;
	}
	
	public String toString() {
		if (distanceMeasure == null) {
			return "EE[untrained]";
		}else {
			return "EE[" + distanceMeasure.toString() + "]"; //,e={" + "..." + "}
		}		
	}

}
