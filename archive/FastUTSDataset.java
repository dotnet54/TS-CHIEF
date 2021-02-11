package data.timeseries;

import java.util.List;
import java.util.Random;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;

/**
 * Immutable dataset
 * 
 */
public class FastUTSDataset implements Dataset{

	//note: keeping public
	public double[][] X;
	public int[] y;
	public int cursor = 0;
	
	public FastUTSDataset(int size, int features) {
		X = new double[size][features];
		y = new int[size];
	}
	
	public FastUTSDataset(double[][] X, int[] y) {
		this.X = X;
		this.y = y;
	}
	

	@Override
	public int size() {
		return y.length;
	}

	@Override
	public int length() {
		return X[0] == null ? 0 : X[0].length;
	}

	@Override
	public void add(Integer label, double[] series) {
		//bounds unchecked
		X[cursor] = series;
		y[cursor] = label;
		cursor++;
	}

	@Override
	public void remove(int i) throws Exception {
		throw new Exception("Data removal not supported");
	}

	@Override
	public double[] get_series(int i) {
		return X[i];
	}

	@Override
	public Integer get_class(int i) {
		return y[i];
	}

	@Override
	public int get_num_classes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int get_class_size(Integer class_label) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public TIntIntMap get_class_map() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] get_unique_classes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TIntObjectMap<Dataset> split_by_class() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double gini() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<double[]> _internal_data_list() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> _internal_class_list() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] _internal_data_array() {
		return X;
	}

	@Override
	public int[] _internal_class_array() {
		return y;
	}

	@Override
	public void shuffle() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shuffle(long seed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Dataset shallow_clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dataset deep_clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dataset sample_n(int n_items, Random rand) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dataset sort_on(int timestamp) {
		// TODO Auto-generated method stub
		return null;
	}
}
