package trees.splitters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import application.AppConfig;
import application.AppConfig.ThresholdSelectionMethod;
import core.exceptions.MultivariateDataNotSupportedException;
import data.timeseries.*;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSChiefNode;
import trees.splitters.st.RandomShapeletTransformSplitter;
import trees.splitters.st.ShapeletEx;
import util.storage.pair.DblIntPair;

public class  StaticGiniSplitter {
	
	protected static final int NUM_CHILDREN = 2;
	protected static final int LEFT_BRANCH = 0;
	protected static final int RIGHT_BRANCH = 1;
	
	public static boolean debug = false;
	
	
	//returns the threshold for the given attribute
	public static SplitCriterion fit(Dataset trainData, Indexer trainIndices, int attrib) throws Exception {
		if (trainData.isMultivariate()){
			throw new MultivariateDataNotSupportedException();
		}
		int total_size = trainData.size();
		List<DblIntPair> values = new ArrayList<DblIntPair>(total_size);
		double weighted_gini = Double.POSITIVE_INFINITY;
		double best_weighted_gini = Double.POSITIVE_INFINITY;
		int left_size = 0, right_size;
		double left_gini, right_gini;
		int class_label;
		int used_attributes = 0;	//if an attribute is not useful we keep trying until used_attributes <= m
		ClassDistribution left_class_dist = new ClassDistribution();
		ClassDistribution right_class_dist = new ClassDistribution();
		
		//extract the column
		for (int j = 0; j < total_size; j++) {
			//TODO using only dimension 1
			DblIntPair pair = new DblIntPair(trainData.getSeries(j).data(attrib)[0],
					trainData.getClass(j));
			values.add(pair);
		}
		//sort the pairs
		Collections.sort(values, (a,b)->{
//			System.out.println("a:" + a.key + " b: " + b.key + " a-b: " + (int) (a.key - b.key));
			return Double.compare(a.key, b.key);	//TODO use a dbl compare class with a threshold
		});
		
		left_class_dist.clear();
		right_class_dist.clear();
		left_size = 0;
		double cur_threshold = values.get(0).key;		
		right_size = total_size - left_size;

		//clone the class distribution from the dataset
		ClassDistribution class_dist = new ClassDistribution(true);
		class_dist._set_internal_map(trainData.getClassDistribution());//TODO refactor
		
		for (int key : class_dist.keys()) {
			right_class_dist.put(key, class_dist.get(key));
		}	
		
		
		for (int j = 0; j < total_size - 1; j++) {
			class_label = values.get(j).value;
			
			left_size++;
			left_class_dist.inc(class_label);
			right_class_dist.dec(class_label);				
			right_size = total_size - left_size;				
			
			if (values.get(j+1).key > values.get(j).key) {
				
				left_gini = left_class_dist.gini(total_size);
				right_gini = right_class_dist.gini(total_size);

				weighted_gini = ((double)left_size/total_size * left_gini) + 
						((double)right_size/total_size * right_gini);				
				
				
				//Note: equal case not handled explicitly
				if (weighted_gini < best_weighted_gini) {
					best_weighted_gini = weighted_gini;
					cur_threshold = (cur_threshold +  values.get(j+1).key) / 2;
					
					
					if (debug) {
						DblIntPair pair1 = values.get(j);
						DblIntPair pair2 =values.get(j+1);
						
//						System.out.println("a: " + temp_best_attribute + " t: " + temp_best_threshold + " left: "
//						+ left_class_dist.toString() + " right: " + right_class_dist + " wg: " + weighted_gini);
//						System.out.println("p1: " + pair1 + " p2: " + pair2);

//						str = " left: " + left_class_dist.toString() + " right: " + right_class_dist;
												
					}

				} // end best case update	
				
			} // end split point 
		}
	
		
		SplitCriterion bestCriteria = new SplitCriterion(cur_threshold, best_weighted_gini, ThresholdSelectionMethod.BestGini);
		
		return bestCriteria;
	}
	

	//returns the threshold for the given attribute
	public static SplitCriterion train(Dataset trainData, List<DblIntPair> values) throws Exception {
		if (trainData.isMultivariate()){
			throw new MultivariateDataNotSupportedException();
		}
		int total_size = values.size();
		double weighted_gini = Double.POSITIVE_INFINITY;
		double best_weighted_gini = Double.POSITIVE_INFINITY;
		int left_size = 0, right_size;
		double left_gini, right_gini;
		int class_label;
		int used_attributes = 0;	//if an attribute is not useful we keep trying until used_attributes <= m
		ClassDistribution left_class_dist = new ClassDistribution();
		ClassDistribution right_class_dist = new ClassDistribution();
		
		//sort the pairs
		Collections.sort(values, (a,b)->{
//			System.out.println("a:" + a.key + " b: " + b.key + " a-b: " + (int) (a.key - b.key));
			return Double.compare(a.key, b.key);	//TODO use a dbl compare class with a threshold
		});
		
		left_class_dist.clear();
		right_class_dist.clear();
		left_size = 0;
		double cur_threshold = values.get(0).key;		
		right_size = total_size - left_size;
		
		//clone the initial right class distribution from the dataset
		ClassDistribution class_dist = new ClassDistribution(true);
		class_dist._set_internal_map(trainData.getClassDistribution());//TODO refactor
		
		for (int key : class_dist.keys()) {
			right_class_dist.put(key, class_dist.get(key));
		}	
		
		
		for (int j = 0; j < total_size - 1; j++) {
			class_label = values.get(j).value;
			
			left_size++;
			left_class_dist.inc(class_label);
			right_class_dist.dec(class_label);		
			right_size = total_size - left_size;				
			
			if (values.get(j+1).key > values.get(j).key) {
				
				left_gini = left_class_dist.gini(total_size);
				right_gini = right_class_dist.gini(total_size);

				weighted_gini = ((double)left_size/total_size * left_gini) + 
						((double)right_size/total_size * right_gini);				
				
				
				//Note: equal case not handled explicitly
				if (weighted_gini < best_weighted_gini) {
					best_weighted_gini = weighted_gini;
					cur_threshold = (cur_threshold +  values.get(j+1).key) / 2;
					
					
					if (debug) {
						DblIntPair pair1 = values.get(j);
						DblIntPair pair2 =values.get(j+1);
						
//						System.out.println("a: " + temp_best_attribute + " t: " + temp_best_threshold + " left: "
//						+ left_class_dist.toString() + " right: " + right_class_dist + " wg: " + weighted_gini);
//						System.out.println("p1: " + pair1 + " p2: " + pair2);

//						str = " left: " + left_class_dist.toString() + " right: " + right_class_dist;
												
					}

				} // end best case update	
				
			} // end split point 
		}
		
		SplitCriterion bestCriteria = new SplitCriterion(cur_threshold, best_weighted_gini, ThresholdSelectionMethod.BestGini);
		
		return bestCriteria;
	}
 
	/**
	 * Simple experiment that selects a random cut point instead of selecting cutpoint with minimum
	 * gini... use this when running experiments to test for overfitting
	 *
	 */
	public static SplitCriterion fitRandomThreshold(Dataset trainData, List<DblIntPair> values, ShapeletEx shapelet) throws Exception {
		if (trainData.isMultivariate()){
			throw new MultivariateDataNotSupportedException();
		}
		int total_size = values.size();
		double weighted_gini = Double.POSITIVE_INFINITY;
		double best_weighted_gini = Double.POSITIVE_INFINITY;
		int left_size = 0, right_size;
		double left_gini, right_gini;
		int class_label;
		int used_attributes = 0;	//if an attribute is not useful we keep trying until used_attributes <= m
		ClassDistribution left_class_dist = new ClassDistribution();
		ClassDistribution right_class_dist = new ClassDistribution();
		
		//sort the pairs
		Collections.sort(values, (a,b)->{
//			System.out.println("a:" + a.key + " b: " + b.key + " a-b: " + (int) (a.key - b.key));
			return Double.compare(a.key, b.key);	//TODO use a dbl compare class with a threshold
		});
		
		left_class_dist.clear();
		right_class_dist.clear();
		left_size = 0;
		double cur_threshold = values.get(0).key;		
		right_size = total_size - left_size;
		
		//clone the initial right class distribution from the dataset
		ClassDistribution class_dist = new ClassDistribution(true);
		class_dist._set_internal_map(trainData.getClassDistribution());//TODO refactor
		
		for (int key : class_dist.keys()) {
			right_class_dist.put(key, class_dist.get(key));
		}	
		
		
		TDoubleArrayList cutpoints = new TDoubleArrayList(total_size);
		
		for (int j = 0; j < total_size - 1; j++) {
			class_label = values.get(j).value;
			
			left_size++;
			left_class_dist.inc(class_label);
			right_class_dist.dec(class_label);		
			right_size = total_size - left_size;				
			
			if (values.get(j+1).key > values.get(j).key) {
				
				cur_threshold = (cur_threshold +  values.get(j+1).key) / 2;
				
				cutpoints.add(cur_threshold);
				
				
				left_gini = left_class_dist.gini(total_size);
				right_gini = right_class_dist.gini(total_size);

				weighted_gini = ((double)left_size/total_size * left_gini) + 
						((double)right_size/total_size * right_gini);				
				
				
				//Note: equal case not handled explicitly
				if (weighted_gini < best_weighted_gini) {
					best_weighted_gini = weighted_gini;
					cur_threshold = (cur_threshold +  values.get(j+1).key) / 2;
					
					
					if (debug) {
						DblIntPair pair1 = values.get(j);
						DblIntPair pair2 =values.get(j+1);
						
//						System.out.println("a: " + temp_best_attribute + " t: " + temp_best_threshold + " left: "
//						+ left_class_dist.toString() + " right: " + right_class_dist + " wg: " + weighted_gini);
//						System.out.println("p1: " + pair1 + " p2: " + pair2);

//						str = " left: " + left_class_dist.toString() + " right: " + right_class_dist;
												
					}

				} // end best case update	
				
			} // end split point 
		}
		
		int num_tries = application.AppConfig.st_num_rand_thresholds;
		double best_threshold = cur_threshold;
		
		for (int i = 0; i < num_tries; i++) {
			if (cutpoints.size() > 0) {
				int r = AppConfig.getRand().nextInt(cutpoints.size());
				cur_threshold = cutpoints.get(r);				
			}else {
				//cur_threshold
				System.out.println("using cur_threshold: " + cur_threshold);
			}

			
			//split //TODO refactor -- remove hacky dependency
			TIntObjectMap<Dataset> splits = RandomShapeletTransformSplitter.split(trainData, null, shapelet, cur_threshold);
			weighted_gini = RandomShapeletTransformSplitter.weighted_gini(splits, trainData.size());

			
			if (weighted_gini < best_weighted_gini) {
				best_weighted_gini = weighted_gini;
				best_threshold = cur_threshold;
			}
				
		}
		
		SplitCriterion bestCriteria = new SplitCriterion(cur_threshold, best_weighted_gini, ThresholdSelectionMethod.BestGini);
		
		return bestCriteria;
	}
	
	
	public static TIntObjectMap<Dataset> split(Dataset trainData, double threshold, int attribute) throws Exception {
		if (trainData.isMultivariate()){
			throw new MultivariateDataNotSupportedException();
		}
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>(NUM_CHILDREN);	//0 = left node, 1 = right node
		int size = trainData.size();
		TimeSeries series;
		
		splits.put(LEFT_BRANCH, new MTSDataset(size));	//TODO initial capacity too large, try optimize this
		splits.put(RIGHT_BRANCH, new MTSDataset(size));	//TODO initial capacity too large, try optimize this
		for (int i = 0; i < size; i++) {
			series = trainData.getSeries(i);
			
			if (debug) {
				double temp = series.data()[0][attribute]; //print
			}

			//TODO using only dimension 1
			if (series.data()[0][attribute] < threshold) {
				splits.get(LEFT_BRANCH).add(series);
			}else {
				splits.get(RIGHT_BRANCH).add(series);
			}
		}
		
		
		if (debug) {
//			System.out.println("splits: " + PrintUtilities.print_split(splits));
			
			if (TSChiefNode.has_empty_split(splits)) {
				throw new Exception("empty splits found! check for bugs: best_attribute " + attribute + " best_threshold " + threshold);
			}			
		}

		return splits;
	}

	
}
