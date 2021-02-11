package data.timeseries;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import application.AppConfig;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class UTSDataset{
	protected String datasetName;
	protected List<TimeSeries> data;
	protected TIntIntMap class_distribution;
	protected List<String> attrib_names;
	
	//TODO norm series?? boolean flag , add to constructor? add method
	
	//TODO store stats such as min, max
	
	protected int length;
	
	public UTSDataset() {
		this.data = new ArrayList<TimeSeries>();
		this.class_distribution = new TIntIntHashMap();
		this.attrib_names = new ArrayList<>(0);
	}
	
	public UTSDataset(String datasetName) {
		this.data = new ArrayList<TimeSeries>();
		this.class_distribution = new TIntIntHashMap();
		this.attrib_names = new ArrayList<>(0);
		this.datasetName = datasetName;
	}
	
	public UTSDataset(int expected_size) {
		this.data = new ArrayList<TimeSeries>(expected_size);
		this.class_distribution = new TIntIntHashMap();
		this.attrib_names = new ArrayList<>(0);
	}
	
	public UTSDataset(int expected_size, String datasetName) {
		this.data = new ArrayList<TimeSeries>(expected_size);
		this.class_distribution = new TIntIntHashMap();
		this.attrib_names = new ArrayList<>(0);
		this.datasetName = datasetName;
	}

	public UTSDataset(int expected_size, int length) {
		this.length = length;
		this.data = new ArrayList<TimeSeries>(expected_size);
		this.class_distribution = new TIntIntHashMap();
		this.attrib_names = new ArrayList<>(0);
	}
	
	public UTSDataset(int expected_size, int length, String datasetName) {
		this.length = length;
		this.data = new ArrayList<TimeSeries>(expected_size);
		this.class_distribution = new TIntIntHashMap();
		this.attrib_names = new ArrayList<>(0);
		this.datasetName = datasetName;
	}

	public int size() {
		return data.size();
	}

	public int length() {
		return this.data.isEmpty() ? length : this.data.get(0).getLength();
	}
	
	public String getDatasetName() {
		return this.datasetName;
	}

	public void setDatasetName(String datasetName) {
		this.datasetName = datasetName;
	}
	
	public void add(TimeSeries series) {
		this.data.add(series);	//rafactor
		
		Integer label = series.getLabel();		
		
		if (class_distribution.containsKey(label)) {
			class_distribution.put(label, class_distribution.get(label) + 1);
		}else {
			class_distribution.put(label, 1);
		}
		
		if (length == 0) {
			length = series.data.length;
		}
	}	
	
	public void add(TimeSeries series, double weight) {
		series.setWeight(weight);
		this.data.add(series);
		
		Integer label = series.getLabel();		
		
		if (class_distribution.containsKey(label)) {
			class_distribution.put(label, class_distribution.get(label) + 1);
		}else {
			class_distribution.put(label, 1);
		}
	}	
	
	public void add(double[] series, Integer label) {
		this.data.add(new TimeSeries(series, label));	//refactor
		
		if (class_distribution.containsKey(label)) {
			class_distribution.put(label, class_distribution.get(label) + 1);
		}else {
			class_distribution.put(label, 1);
		}
	}

	public void remove(int i) {
		Integer label = this.data.get(i).getLabel();
		
		if (class_distribution.containsKey(label)) {
			int count = class_distribution.get(label);
			if (count > 0) {
				class_distribution.put(label, class_distribution.get(label) - 1);
			}else {
				class_distribution.remove(label);
			}
		}
		
		this.data.remove(i);
	}

	public TimeSeries get_series(int i) {
		return this.data.get(i);
	}

	public Integer get_class(int i) {
		return this.data.get(i).label;
	}

	public int get_num_classes() {
		return this.class_distribution.size();	//TODO what if histogram have a 0 count for a class?? happens if we remove data from the list; but this case never occurs in current PF implementation, this just keep to this in mind for future
	}

	public int get_class_size(Integer label) {
		return this.class_distribution.get(label);
	}

	public TIntIntMap get_class_map() {
		return this.class_distribution;
	}

	public int[] get_unique_classes() {
		return this.class_distribution.keys();
	}
	
	public Integer get_majority_class() {

		List<Integer> label_list = new ArrayList<Integer>();
		label_list.clear();			// Not necessary?
		
		int[] unique_class = this.get_unique_classes();
		int maj_size = this.get_class_size(unique_class[0]);
		label_list.add(unique_class[0]);
		
		for (int i=1; i<unique_class.length; i++) {
			int current_size = this.get_class_size(unique_class[i]);
			if (current_size>maj_size) {
				maj_size = current_size;
				label_list.clear();
				label_list.add(unique_class[i]);
			}else if (current_size == maj_size) {
				label_list.add(unique_class[i]);
			}					
		}
//		int r = ThreadLocalRandom.current().nextInt(label_list.size());
		int r = AppConfig.getRand().nextInt(label_list.size());
		return label_list.get(r);
	}
	
	public double get_sum_weight() {
		double sum = 0.0;
		int size = this.size();
		for (int i = 0; i < size; i++) {
			sum += this.data.get(i).getWeight();
		}
		return sum;
	}
	
	//TODO used only for testing
	public List<TimeSeries> _get_internal_list() {
		return this.data;
	}
	
	public TIntObjectMap<UTSDataset> split_by_class() {
		TIntObjectMap<UTSDataset> split =  new TIntObjectHashMap<UTSDataset>(this.get_num_classes());
		Integer label;
		UTSDataset class_set = null;
		int size = this.size();

		for (int i = 0; i < size; i++) {
			label = this.data.get(i).getLabel();
			if (! split.containsKey(label)) {
				class_set = new UTSDataset(this.class_distribution.get(label));
				split.put(label, class_set);
			}
			
			split.get(label).add(this.data.get(i));
		}
		
		return split;
	}

	public double gini() {
		double sum = 0.0;
		double p;
		int total_size = this.data.size();
		
		for (int key: class_distribution.keys()) {
			p = (double)class_distribution.get(key) / total_size;
			sum += p * p;
		}
		return 1 - sum;
	}

	public double weighted_gini() {
		double sum = 0.0;
		double p;
		double total_weight = this.get_sum_weight();
		
		// Charlotte: weigthed (from the instance) Gini for boosting 
		TIntObjectMap<UTSDataset> data_per_class = this.split_by_class();
		for (int key : data_per_class.keys()) {
			p = data_per_class.get(key).get_sum_weight()/total_weight; 
			sum += p * p;
		}
		return 1 - sum;
	}

	public void shuffle() {
//		this.shuffle(System.nanoTime());
		this.shuffle(AppConfig.rand_seed); //TODO
	}

	public void shuffle(long seed) {
		Collections.shuffle(data, new Random(seed));	//TODO use thread local random??
	}

//	public ListDataset shallow_clone() {
//		// TODO Auto-generated method stub
//		return null;
//	}

//	public ListDataset deep_clone() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		DecimalFormat df = new DecimalFormat(AppConfig.print_decimal_places);

		sb.append(this.class_distribution.toString() + "=" + df.format(this.gini()) 
		+ " size = "+ size() + " length = " + length + " Name = " + this.datasetName);
		sb.append("\n");

		int max = 10;
		int i;
		for (i = 0; i < Math.min(data.size(), max); i++) {
			sb.append(data.get(i).toString() + "\n");
		}
		
		if ( i == max) {
			sb.append("...");
		}
		
		return sb.toString();
	}

	public UTSDataset bootstrap() throws Exception {
		
		UTSDataset sample = new UTSDataset(this.size(), this.length());
		for (int i=0; i<this.size(); i++) {
//			int r = ThreadLocalRandom.current().nextInt(this.size());
			int r = AppConfig.getRand().nextInt(this.size());
			sample.add(this.get_series(r));
		}
		return sample;
	}

	public UTSDataset bootstrap(double weight) throws Exception {
			
			UTSDataset sample = new UTSDataset(this.size(), this.length());
			for (int i=0; i<this.size(); i++) {
//				int r = ThreadLocalRandom.current().nextInt(this.size());
				int r = AppConfig.getRand().nextInt(this.size());
				sample.add(this.get_series(r),weight);
			}
			return sample;
		}		
	
	public UTSDataset sample_n(int sample_size, Random rand) throws Exception {
		// TODO Auto-generated method stub
		throw new Exception("method not implemented");
		
//		return null;
	}

	public List<String> getAttribNames() {
		return attrib_names;
	}

	public void setAttribNames(List<String> attrib_names) {
		this.attrib_names = attrib_names;
	}	
}
