package datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.IntIntCursor;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import transforms.BOSS.BagOfPattern;

public class BossDataset {

	
	//indices should match??
//	private boolean out_of_sync;
//	private TSDataset original_dataset;
	private BagOfPattern[] transformed_data;
	
	private BossParams params;
	
	//TODO
//	make a histogram for the whole dataset
	
	public static class BossParams {
		public boolean normMean;
		public int window_len;
		public int word_len;
		public int alphabet_size;
		public boolean lower_bounding = true;	//TODO
		
		public BossParams(boolean norm, int w, int l, int a) {
			this.normMean = norm;
			this.window_len = w;
			this.word_len = l;
			this.alphabet_size = a;
		}
		
		public String toString() {
			return "" + "," + normMean + ","+ window_len + ","+ word_len + ","+ alphabet_size;
		}
	}
	
	
	public BossDataset(TSDataset original_dataset) {
//		this.original_dataset = original_dataset;
	}
	
	
	public BossDataset(TSDataset original_dataset,BagOfPattern[] transformed_data, BossParams params) {
//		this.original_dataset = original_dataset;
		this.transformed_data = transformed_data;
		this.params = params;
	}
	
	public TIntObjectMap<List<BagOfPattern>> split_by_class() {
		TIntObjectMap<List<BagOfPattern>> split =  new TIntObjectHashMap<List<BagOfPattern>>();
		Integer label;
		List<BagOfPattern> class_set = null;

		for (int i = 0; i < this.transformed_data.length; i++) {
			label = this.transformed_data[i].getLabel();
			if (! split.containsKey(label)) {
				class_set = new ArrayList<BagOfPattern>();
				split.put(label, class_set);
			}
			
			split.get(label).add(this.transformed_data[i]);
		}
		
		return split;
	}	
	
	public TIntObjectMap<BagOfPattern> get_class_histograms() {
		TIntObjectMap<BagOfPattern> split =  new TIntObjectHashMap<BagOfPattern>();
		Integer label;
		BagOfPattern class_hist = null;

		for (int i = 0; i < this.transformed_data.length; i++) {
			label = this.transformed_data[i].getLabel(); //TODO lbl int
			if (! split.containsKey(label)) {
				class_hist = new BagOfPattern();
				class_hist.setLabel(this.transformed_data[i].getLabel());	//TODO check
				split.put(label, class_hist);
			}
			
			for (IntIntCursor key : transformed_data[i].bag) {
				int word = key.key;
				split.get(label).bag.putOrAdd((int) word, (short) 1, (short) key.value);
			}

		}
		

		

		
		return split;
	}	
	
	public TreeMap<Integer, TreeMap<Integer, Integer>> get_sorted_class_hist(TIntObjectMap<BagOfPattern> split){
		//sort
		TreeMap<Integer, TreeMap<Integer, Integer>> tmap = new TreeMap<Integer, TreeMap<Integer, Integer>>();
		for (int key : split.keys()) {
			tmap.put(key, new TreeMap<Integer, Integer>());
			for (IntIntCursor key2 : split.get(key).bag) {
				int word = key2.key;
				tmap.get(key).put(key2.key, split.get(key).bag.get(key2.key));
			}			
		}		
		
		return tmap;
	}
	
	
	public BagOfPattern[] getTransformed_data() {
		return transformed_data;
	}


	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("all: ");
		sb.append(Arrays.toString( this.transformed_data));
		sb.append("\n");
		
		TIntObjectMap<List<BagOfPattern>> split = split_by_class();
		
		for (int key : split.keys()) {
			sb.append("class " + key +  ":");
			
			BagOfPattern[] array = split.get(key).toArray(new BagOfPattern[split.get(key).size()]);
			
			sb.append(Arrays.toString(array));
			sb.append("\n");
		}
				
		return sb.toString();
	}

}
