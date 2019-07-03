package trees.splitters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import core.AppContext;
import core.AppContext.FeatureSelectionMethod;
import datasets.TSDataset;
import datasets.TimeSeries;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.ProximityTree;
import util.PrintUtilities;
import util.Util;
import util.pair.DblIntPair;

public class GenericGiniSplitter implements NodeSplitter{

	protected final int NUM_CHILDREN = 2;
	protected final int LEFT_BRANCH = 0;
	protected final int RIGHT_BRANCH = 1;
	
	protected int[] attributes;
	
	protected int best_attribute;
	protected double best_threshold;
	
	//m = number of instances(features) used to split, 
	//if m < length a random subset of size m is used similar to random forest
	protected int m;
	protected FeatureSelectionMethod feature_selection_method;
	protected ProximityTree.Node node;
	protected boolean randomize_attrib_selection = true;	//if false, select the first m attributes that can give a valid split
	
	protected boolean debug = false;
		
	public GenericGiniSplitter(ProximityTree.Node node) throws Exception {
		this.node = node;
	}
	
	public GenericGiniSplitter(ProximityTree.Node node, int m) throws Exception {
		this.node = node;
		this.m = m;
	}
	
	public GenericGiniSplitter(ProximityTree.Node node, FeatureSelectionMethod feature_selection_method) throws Exception {
		this.node = node;
		this.feature_selection_method = feature_selection_method;	
	}	
	
	public TIntObjectMap<TSDataset> train(TSDataset data, int[] indices) throws Exception {
		int length = data.length();
		int total_size = data.size();	
		
		boolean can_split = false;
		boolean can_use_attribute = false;
		double weighted_gini = Double.POSITIVE_INFINITY;
		double best_weighted_gini = Double.POSITIVE_INFINITY;
		int left_size = 0, right_size;
		double left_gini, right_gini;
		int class_label;
		int used_attributes = 0;	//if an attribute is not useful we keep trying until used_attributes <= m
		TIntIntMap left_class_dist = new TIntIntHashMap();
		TIntIntMap right_class_dist = new TIntIntHashMap();
		
		List<DblIntPair> values = new ArrayList<DblIntPair>(total_size);
		double best_threshold_per_attrib = 0;
		int best_attribute_per_attrib = 0;
		
		int _best_split_pos;	//debug -> index/position of best threshold
		
		//if no data -- usually this case is handled much earlier -- duplicated here to make this class work independently also
		if (data.size() == 0) {
			return null;
		}
		
		//if data.size == 1 or data.gini() == 0 -> these cases are handled at node.train();
		
		if (feature_selection_method == FeatureSelectionMethod.Sqrt) {
			m = (int) Math.sqrt(length);
		}else if (feature_selection_method == FeatureSelectionMethod.Loge) {
			m = (int) Math.log(length);
		}else if (feature_selection_method == FeatureSelectionMethod.Log2) {
			m = (int) (Math.log(length/ Math.log(2))); 
		}else {
			//assume m is set via constructor
//			m = Integer.parseInt(AppContext.randf_m); //TODO verify
		}			
		
//		set m, for random forest, set correct m using constructor or the setter function
		if (m == 0 || m > length) {
			m = length;
		}			

		double[] _best_gini_per_attrib = null;
		if (debug) {
			_best_gini_per_attrib = new double[m];
		}
		
		attributes =  new int[length];
		for (int i = 0; i < length; i++) {
			attributes[i] = i;
		}
		
		if (randomize_attrib_selection) {
			attributes = Util.shuffleArray(attributes);	//note not an in place shuffle, change this later 
		}
		
		//TODO check used_attributes <= m or used_attributes < m
		for (int i = 0; i < attributes.length && used_attributes <= m; i++) {
			can_use_attribute = false;
			values.clear(); // O(n) complexity
			left_class_dist.clear();
			right_class_dist.clear();
			left_size = 0;
		
			//clone the class distribution from the dataset
			TIntIntMap class_dist = data.get_class_map();
			for (int key : class_dist.keys()) {
				right_class_dist.put(key, class_dist.get(key));
			}			
			//extract the column
			for (int j = 0; j < total_size; j++) {
				DblIntPair pair = new DblIntPair(data.get_series(j).getData(attributes[i]), 
						data.get_class(j));
				values.add(pair);
			}
			//sort the pairs
			Collections.sort(values, (a,b)->{
//				System.out.println("a:" + a.key + " b: " + b.key + " a-b: " + (int) (a.key - b.key));
				return Double.compare(a.key, b.key);	//TODO use a dbl compare class with a threshold
			});
			
			double cur_threshold = values.get(0).key;			//TODO check first item or 0? or doesnt matter?
			right_size = total_size - left_size;

	
			for (int j = 0; j < total_size - 1; j++) {
				class_label = values.get(j).value;
				
				left_size++;
				left_class_dist.adjustOrPutValue(class_label, 1, 1);
				right_class_dist.adjustOrPutValue(class_label, -1, 0);				
				right_size = total_size - left_size;				
				
				if (values.get(j+1).key > values.get(j).key) {
					can_split = true;	//there is at least two distinct values in at least one attribute
					can_use_attribute = true;
					
					left_gini = Util.gini(left_class_dist, left_size);
					right_gini = Util.gini(right_class_dist, right_size);
	
					weighted_gini = ((double)left_size/total_size * left_gini) + 
							((double)right_size/total_size * right_gini);				
					
					
					//Note: equal case not handled explicitly
					if (weighted_gini < best_weighted_gini) {
						best_weighted_gini = weighted_gini;
						best_attribute_per_attrib = attributes[i];
						cur_threshold = (cur_threshold +  values.get(j+1).key) / 2;
						best_threshold_per_attrib = cur_threshold;
						
						_best_split_pos = j;
						
						if (debug) {
							DblIntPair pair1 = values.get(j);
							DblIntPair pair2 =values.get(j+1);
							
							
	//						System.out.println("a: " + temp_best_attribute + " t: " + temp_best_threshold + " left: "
	//						+ left_class_dist.toString() + " right: " + right_class_dist + " wg: " + weighted_gini);
	//						System.out.println("p1: " + pair1 + " p2: " + pair2);
	
	//						str = " left: " + left_class_dist.toString() + " right: " + right_class_dist;
							
							if (best_threshold == 0) {
	//							System.out.println("1.b = 0");
							}							
						}

					} // end best case update	
					
				} // end split point 
				
				if (debug) {
//					System.out.println("a: " + best_attribute + " t: " + best_threshold + " left: " + left_class_dist.toString() + " right: " + right_class_dist);
				}
				
			} //end each attribute
			
//			if (best_threshold == 0) {
//				System.out.println("2.b = 0 //skip this attrib: same val " + same_value);
//			}
//			System.out.println("a: " + best_attribute + " t: " + best_threshold + " left: " + left_class_dist.toString() + " right: " + right_class_dist + "wg: " + weighted_gini);

			if (can_use_attribute) {
				best_attribute = best_attribute_per_attrib;
				best_threshold = best_threshold_per_attrib;
				if (debug) {
					_best_gini_per_attrib[used_attributes] = best_weighted_gini;	
				}
				used_attributes++;
			}else {
				
				if (debug) {
					//no gain from this attribute, skip this attrib
					System.out.println("cant place a split point for attrib: " + i + " temp_best_attribute: " + best_attribute_per_attrib
							+ " temp_best_threshold: " + best_threshold_per_attrib);					
				}

			}			

		}// end attrib loop
	
		if (debug) {
//			System.out.println("final a: " + best_attribute + " t: " + best_threshold + " wg:" + best_weighted_gini + " sp: " + str);
		}
				
		//if we can make a sensible split using any attribute, split the data,
		//else return null -> assign max gini to this during evaluation, if there 
		//are no splitter  better than that, use class distribution at node for prediction;
		if (can_split) {
			return split(data, null);
		}else {
			//if no attribute can give us a valid split point eg. if all values are same
//			System.out.println("cannot split this data in a sensible way...");
			return null;
		}
	
	}

	
	public TIntObjectMap<TSDataset> split(TSDataset data, int[] indices) throws Exception {
		TIntObjectMap<TSDataset> splits = new TIntObjectHashMap<TSDataset>(NUM_CHILDREN);	//0 = left node, 1 = right node
		int size = data.size();
		TimeSeries series;
		
		splits.put(LEFT_BRANCH, new TSDataset(size, data.length()));	//TODO initial capacity too large, try optimize this
		splits.put(RIGHT_BRANCH, new TSDataset(size, data.length()));	//TODO initial capacity too large, try optimize this		
		for (int i = 0; i < size; i++) {
			series = data.get_series(i);
			
			if (debug) {
				double temp = series.getData()[best_attribute]; //print
			}
			
			if (series.getData()[best_attribute] < best_threshold) { 
				splits.get(LEFT_BRANCH).add(series);
			}else {
				splits.get(RIGHT_BRANCH).add(series);
			}
		}
		
		
		if (debug) {
//			System.out.println("splits: " + PrintUtilities.print_split(splits));
			
			if (SplitEvaluator.has_empty_split(splits)) {
				throw new Exception("empty splits found! check for bugs: best_attribute " + best_attribute + " best_threshold " + best_threshold);
			}			
		}

		return splits;
	}
	
	public int predict(TimeSeries query, int queryIndex) throws Exception {
		if (query.getData()[best_attribute] < best_threshold) {
			return LEFT_BRANCH;
		}else {
			return RIGHT_BRANCH;
		}
	}
	
	public int getM() {
		return m;
	}
	
	public void setM(int newM) {
		this.m = newM;
	}	
	
	public int getBestAttribute() {
		return best_attribute;
	}

	public void setBestAttribute(int best_attribute) {
		this.best_attribute = best_attribute;
	}

	public double getBestThreshold() {
		return best_threshold;
	}

	public void setBestThreshold(double best_threshold) {
		this.best_threshold = best_threshold;
	}
	
	public boolean getRandomizeAttribSelection() {
		return randomize_attrib_selection;
	}

	public void setRandomizeAttribSelection(boolean randomize) {
		this.randomize_attrib_selection = randomize;
	}

	public String toString() {
		return "GiniSplitter[m=" + m + ", a=" + best_attribute + ", t="+ best_threshold+"]";
	}

	@Override
	public void train_binary(TSDataset data) throws Exception {
		// TODO Auto-generated method stub
		
	}		
	
}
