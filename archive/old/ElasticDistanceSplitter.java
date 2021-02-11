package trees.splitters.old;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import application.AppConfig;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSCheifTree;
import trees.TSCheifTree.Node;
import trees.splitters.ee.measures.DistanceMeasure;
import util.Sampler;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ElasticDistanceSplitter implements NodeSplitter{
	
	protected int num_children; //may be move to splitter?
	protected DistanceMeasure distance_measure;
	protected TIntObjectMap<double[]> exemplars;
	
	protected DistanceMeasure temp_distance_measure;
	protected TIntObjectMap<double[]> temp_exemplars;	
	
	TIntObjectMap<Dataset> best_split = null;	
	TSCheifTree.Node node;
	protected int num_splits;
	
	double percentage_of_sample_to_use = 1;
	int approx_gini_threshold = 10;
	int samples_for_gini;
	
	public ElasticDistanceSplitter(TSCheifTree.Node node, int num_splits) throws Exception {
		this.node = node;
		this.num_splits = num_splits;
	}
	
	public TIntObjectMap<Dataset> split_data(Dataset data, TIntObjectMap<Dataset> data_per_class, int samples_for_gini) throws Exception {

		//TODO
		Dataset sample = Sampler.uniform_sample(data, samples_for_gini);
//		System.out.println("approx gini sample size: " + sample.size());		
		
		//		num_children = sample.get_num_classes();
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>(sample.get_num_classes());
		temp_exemplars = new TIntObjectHashMap<double[]>(sample.get_num_classes());

		int branch = 0;
		for (int key : data_per_class.keys()) {
			int r = AppConfig.getRand().nextInt(data_per_class.get(key).size());
			
			splits.put(branch, new ListDataset(sample.size(), sample.length()));
			temp_exemplars.put(branch, data_per_class.get(key).get_series(r));
			branch++;
		}
		
		int sample_size = sample.size();
		int closest_branch = -1;
		for (int j = 0; j < sample_size; j++) {
			closest_branch = this.find_closest_branch(sample.get_series(j), 
					temp_distance_measure, temp_exemplars);
			if (closest_branch == -1) {
				assert false;
			}
			
			splits.get(closest_branch).add(sample.get_class(j), sample.get_series(j));
		}

		return splits;
	}	

	public int find_closest_branch(double[] query, DistanceMeasure dm, TIntObjectMap<double[]> e) throws Exception{
		return dm.find_closest_node(query, e, true);
	}	
	
	public int predict_by_splitter(double[] query) throws Exception{
		return this.distance_measure.find_closest_node(query, exemplars, true);
	}		
	
	public TIntObjectMap<Dataset> getBestSplits() {
		return this.best_split;
	}
	
	public TIntObjectMap<Dataset> train_splitter(Dataset data) throws Exception {
				
		TIntObjectMap<Dataset> data_per_class = data.split_by_class();
		
		double weighted_gini = Double.POSITIVE_INFINITY;
		double best_weighted_gini = Double.POSITIVE_INFINITY;
		TIntObjectMap<Dataset> splits = null;
		int parent_size = data.size();
	
		for (int i = 0; i < num_splits; i++) {

			if (AppConfig.random_dm_per_node) {
				int r = AppConfig.getRand().nextInt(AppConfig.enabled_distance_measures.length);
				temp_distance_measure = new DistanceMeasure(AppConfig.enabled_distance_measures[r]);		
			}else {
				//NOTE: num_candidates_per_split has no effect if random_dm_per_node == false (if DM is selected once per tree)
				//after experiments we found that DM selection per node is better since it diversifies the ensemble
				temp_distance_measure = node.tree.tree_distance_measure;
			}
			
			temp_distance_measure.select_random_params(data, AppConfig.getRand());
			
			int num_instances = (int) Math.max(approx_gini_threshold, percentage_of_sample_to_use * data.size());
			
			splits = split_data(data, data_per_class, num_instances);
			weighted_gini = weighted_gini(parent_size, splits);

			if (weighted_gini <  best_weighted_gini) {
				best_weighted_gini = weighted_gini;
				best_split = splits;
				distance_measure = temp_distance_measure;
				exemplars = temp_exemplars;
			}
		}

		this.num_children = best_split.size();
		
		return this.best_split;
	}
	
	public double weighted_gini(int parent_size, TIntObjectMap<Dataset> splits) {
		double wgini = 0.0;
		
		for (int key : splits.keys()) {
			wgini = wgini + ((double) splits.get(key).size() / parent_size) * splits.get(key).gini();
		}

		return wgini;
	}	
	
	public String toString() {
		return "EE[r=" + num_splits + "]";
	}
}
