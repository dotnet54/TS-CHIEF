package datasets;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public interface Dataset {
	
	public int size();
	
	public int length();
		
//	public void add(int label, double[] series);
	
	public void add(Integer label, double[] series);	
	
	public void remove(int i);		
	
//	public double[] get_index(int i);	
	
	public double[] get_series(int i);
	
	public Integer get_class(int i);	
	
//	public int get_class(int i);	
	
	public int get_num_classes();	
	
	public int get_class_size(Integer class_label);	
	
//	public int get_class_size(int class_label);	

	public TIntIntMap get_class_map();
		
	public int[] get_unique_classes();	
	
//	public Integer[] get_unique_classes();
	
//	public Set<Integer> get_unique_classes_as_set();
	
	public TIntObjectMap<Dataset> split_by_class();	
	
//	public Map<Integer, ? extends Dataset> split_classes();	
	
	public double gini();
	
	public List<double[]> _internal_data_list();
	
	public List<Integer> _internal_class_list();
	
	public double[][] _internal_data_array();
	
	public int[] _internal_class_array();
	
	//key = new labels, value = old labels
//	public Dataset reorder_class_labels(Map<Integer, Integer> new_order);	
	
//	public Map<Integer, Integer> _get_initial_class_labels();	
	
	public void shuffle();	
	
	public void shuffle(long seed);
	
	public Dataset shallow_clone();
	
	public Dataset deep_clone();	
	
	public Dataset sample_n(int n_items, Random rand);
	
	public Dataset sort_on(int timestamp);

}
