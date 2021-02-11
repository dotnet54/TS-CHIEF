package trees.splitters.st;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import application.AppConfig;
import application.AppConfig.ThresholdSelectionMethod;
import core.exceptions.NotSupportedException;
import data.timeseries.*;
import trees.TSChiefNode;
import util.Util;
import util.storage.pair.DblIntPair;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.splitters.NodeSplitter;
import trees.splitters.SplitCriterion;
import trees.splitters.StaticGiniSplitter;

/*
 * 
 * 
 * At each node take a sample (does it needs to have all the classes?)
 * convert the sample into k shapelets
 * at each node select a random exemplar from each class, and find all shapelet candidates for all of them
 * 
 * 
 * 
 */

public class RandomShapeletTransformSplitter implements NodeSplitter{

	private final static int NUM_CHILDREN = 2;
	private final static int LEFT_BRANCH = 0;
	private final static int RIGHT_BRANCH = 1;

	TSChiefNode node;
	Random rand;
	public ShapeletEx best_shapelet;	//stored normalized, if enabled
	public SplitCriterion best_criterion; 
	List<ShapeletEx> shapelets;
	
	public RandomShapeletTransformSplitter(TSChiefNode node) {
		this.node = node;	
		this.rand = AppConfig.getRand();
		this.node.tree.stats.st_count++;
	}
	
	public String toString() {
		return "ST[s=" + 30 + ",cp=" + 5 + "]";
	}
	
//	@Override
//	public TIntObjectMap<TSDataset> train_splitter(TSDataset data, int[] indices) throws Exception {
//		
//		boolean use_subsampling = true;
//		double percent = 0.3;
//		TSDataset sample;
////		int sample_size = (int) (percent * this.data.size());
////		
//		int sample_size = 30;
//		if (use_subsampling) {
////			if (sample_size < 10) {
////				sample = data;
////			}else {
//				sample = data.sample_n(sample_size, rand); //100% sample size;		//TODO stratified??
//				System.out.println("sample_size: " + sample.size());
////			}				
//		}else {
//			sample = data;
//		}
//				
//		sample_size = sample.size(); //update sample size TODO if stratified sampling this may need updating
//		
//		//start
//		
//		int candidates_per_class = 5;
//		int candidate_sample_size;
//		int min_length = 3;
//		int max_length = sample.length();
//		int step = 2;
//		long expected_num_of_shapelets;
//		long num_shapelets_per_series;
//
//		double weighted_gini = Double.POSITIVE_INFINITY;
//		double best_weighted_gini = Double.POSITIVE_INFINITY;
//		double[] series;
//		double[] subsequence;
//		Shapelet shapelet;		
//		TIntObjectMap<Dataset> best_split = null;
//
//		TIntObjectMap<Dataset> data_per_class = sample.split_by_class();
//		ListDataset candidate_samples = Sampler.stratified_sample(data_per_class, candidates_per_class, false, null);
//		candidate_sample_size = candidate_samples.size();
//		num_shapelets_per_series = Shapelet.getNumShapelets(1, sample.length(), min_length, max_length, step);
//		expected_num_of_shapelets = num_shapelets_per_series * candidate_sample_size;
//		System.out.println("Number of expected shapelets: " + expected_num_of_shapelets + "(" + num_shapelets_per_series + ")");
//				
//		List<DblIntPair> distance_vector = new ArrayList<DblIntPair>(sample.size());
//		
//		int i, j, len;
//		int best_i = 0, 
//			best_len = 0, 
//			best_j = 0;
//		double threshold;
//		int label;
//		double bsf;
//
//		
//		for (i = 0; i < candidate_sample_size; i++) {
//			series = candidate_samples.get_series(i);
//			
//			for (len = min_length; len <= max_length; len += step) {
//				for (j = 0; j <= series.length - len; j++) {
//					//z-normalize series here
//					subsequence = Arrays.copyOfRange(series, j, j + len);
//					subsequence = Shapelet.zNormalise (subsequence, false);
//					
//					distance_vector.clear();	//O(n) to init to nulls, use array but implement a sorter
//					bsf = Double.POSITIVE_INFINITY;
//					
//					for (int n = 0; n < sample_size; n++) {
//						//TODO REMOVE CANDIATE SERIES FROM SAMPLE
//						
//						threshold = shapelet_distance(sample.get_series(n), subsequence, bsf);
//						DblIntPair p = new DblIntPair(threshold, sample.get_class(n));
//						distance_vector.add(n, p);
//					}
//					
//					//sort the pairs
//					Collections.sort(distance_vector, (a,b)->{
////						System.out.println("a:" + a.key + " b: " + b.key + " a-b: " + (int) (a.key - b.key));
//						return Double.compare(a.key, b.key);
//					});
//					
//					if (distance_vector.get(0).key != 0) {
////						System.out.println("error: first distance non zero after sorting");
//					}
//					
//					weighted_gini = find_best_split_point(sample, distance_vector);
//
//					if (weighted_gini == 0) {
////						System.out.println("assert: wg==0" + weighted_gini);
//					}
//					
//					if (weighted_gini <=  best_weighted_gini) {
//						best_weighted_gini = weighted_gini;
//						best_i = i;
//						best_len = len;
//						best_j = j;
//						best_threshold = this.temp_best_threshold;
////						System.out.println("  best wgini: " + best_weighted_gini +  " best d: " + best_threshold);
//
//						if (best_weighted_gini < 0) {
//							throw new Exception ("error: -ve wgini");
//						}
//						
//					}						
//				}
//			}
////			System.out.println("series: " + i + " -> shapelets: " + series_shapelet_count 
////					+" :should be: " + num_shapelets_per_series);
//			
//		}
//		
//		//TODO if shapelet normalization enabled, normalize here
//		best_shapelet = new Shapelet(candidate_samples.get_series(best_i), 
//				best_j, best_len, candidate_samples.get_class(best_i), i, true);
//
////		System.gc();
//		best_split = split_data(sample, best_shapelet.shapelet, best_threshold);
//		
//		return best_split;
//	}
	
//	public void check_empty_split(TIntObjectMap<Dataset> splits) throws Exception {
//		
//		for (int key : splits.keys()) {
//			if (splits.get(key).size() == 0) {
//				System.out.println("error: empty split found");
////				throw new Exception("check: empty split found");
//			}
//		}
//
//	}

//	public double find_best_split_point(Dataset sample, List<DblIntPair> distances) throws Exception {
//		
////		TODO //sample.get_num_classes();
////		int num_classes = 3; 
//		int num_classes = sample.get_num_classes(); 
//
//		
//		int total_size = sample.size();
//		TIntIntMap left_class_distributions = new TIntIntHashMap(num_classes);
//		TIntIntMap right_class_distributions = new TIntIntHashMap(num_classes);
//		double left_gini, right_gini;
//		double wgini, bsf = Double.POSITIVE_INFINITY;
//		int left_size = 0, right_size;
//		int class_label;
//		int i;
//		
//		//TODO cloning this map, do this better :)
//		TIntIntMap class_distribution = sample.get_class_map();	
//		for (int key : class_distribution.keys()) {
//			right_class_distributions.put(key, class_distribution.get(key));
//		}
//		
//		left_size++;
//		class_label = distances.get(0).value;
//		//TODO check edge cases, check if val < 0
//		left_class_distributions.put(class_label, left_class_distributions.get(class_label) + 1);
//		right_class_distributions.put(class_label, right_class_distributions.get(class_label) - 1);
//
//		
//		for (i = 1; i < total_size; i++) {
//			right_size = total_size - left_size;	
//			
//			left_gini = gini(left_class_distributions, left_size);
//			right_gini = gini(right_class_distributions, right_size);
//
//			//TODO use wgini function?
//			wgini = ((double)left_size/total_size * left_gini) + 
//					((double)right_size/total_size * right_gini);
//			
//			
//			if (wgini < 0) {
//				System.out.println("error: neg wgini "+ wgini);
//			}else if (wgini == 0) {
////				System.out.println("warn: wgini " + wgini);
//			}
//			
////			System.out.println("\twgini " + wgini + " i: " + i +  " d: " + distances.get(i).key 
////					+ " left:" + printClassDistribution(left_class_distributions, left_size) 
////					+ " right:" + printClassDistribution(right_class_distributions, right_size));
//			
//			left_size++;
//			class_label = distances.get(i).value;
//			
//			if (right_class_distributions.get(class_label) == 0) {
//				System.out.println("r = 0");
//			}
//			
//			left_class_distributions.put(class_label, left_class_distributions.get(class_label) + 1);
//			right_class_distributions.put(class_label, right_class_distributions.get(class_label) - 1);
//						
//			if (wgini <= bsf) {
//				bsf = wgini;
//				this.temp_best_threshold = distances.get(i).key;
//			}
//		}
//				
////		System.out.println("===> best wgini " + bsf + " best threshold: " + temp_best_threshold );
//
////		check_empty_split(split_data(sample,,this.temp_best_threshold));		
//		
//		return bsf;
//	}
	
	private double gini(TIntIntMap class_distribution, int total_size) {
		double sum = 0;
		double p;
		
		for (int key : class_distribution.keys()) {
			p = (double) class_distribution.get(key) / total_size;
			sum += p * p;					
		}
		
		return 1 - sum;
	}
	
	public static double weighted_gini(TIntObjectMap<Dataset> splits, int parent_node_size) {
		double wgini = 0.0;
		
		//TODO handle case when a split contains null object
		//TODO empty splits are favoured? check n think abt it
		for (int key : splits.keys()) {
			wgini = wgini + ((double) splits.get(key).size() / parent_node_size) * splits.get(key).gini();
		}

		return wgini;
	}	
	
//	public TIntObjectMap<Dataset> split_data(Dataset sample, double[] shapelet, double threshold) throws Exception {
//		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>(NUM_CHILDREN);
//		splits.put(LEFT_BRANCH, new ListDataset(sample.size() /2, sample.length())); //TODO init size
//		splits.put(RIGHT_BRANCH, new ListDataset(sample.size() /2, sample.length())); //TODO init size
//		double d = Double.POSITIVE_INFINITY;
//		double[] s;
//		
//		for (int i = 0; i < sample.size(); i++) {
//			s = sample.get_series(i);
//			d = shapelet_distance(s, shapelet, d);
//			
////			System.out.println("splitting i:" + i + " d: " + d + " best: " + best_threshold);
//
//			if (d < threshold) {
//				splits.get(LEFT_BRANCH).add(sample.get_class(i), s);
//			}else {
//				splits.get(RIGHT_BRANCH).add(sample.get_class(i), s);
//			}
//		}
//		
//		check_empty_split(splits);
//		
//		return splits;
//	}
		

//	@Override
//	public int predict_by_splitter(double[] query) throws Exception {
//		//TODO check shapelet should be normalized
//		double d = shapelet_distance(query, this.best_shapelet.shapelet, Double.POSITIVE_INFINITY);
//		
////		System.out.println("testing d: " + d + " vs best threshold: " + best_threshold);
//
//		if (d < this.best_threshold) {
//			return 0;
//		}else {
//			return 1;
//		}
//	}

	
	
	
	//@reference
	//modified version based on timeseriesweka code, bakeoff paper
	private double shapelet_distance (double[] series, double[] shapelet, double bsf) {
		double min_sum = Double.POSITIVE_INFINITY;
        double sum;
        double temp;
        double stdv, mean;
//      double [] subseries;
        
        //TODO use bsf
        
        for (int i = 0; i <= series.length - shapelet.length; i++){
            sum = 0;
            
            //normalize completely
//          subseries = Arrays.copyOfRange(series, i, i + shapelet.length);
//          subseries = zNormalise(subseries, false);
            
            //or normalize subseries on the fly
            mean = subsequnce_mean(series, i, i + shapelet.length);
            stdv = subsequnce_stdv(series, i, i + shapelet.length, mean);
            temp = (stdv == 0.0) ? 0.0 : ((series[i] - mean) / stdv);
            
            for (int j = 0; j < shapelet.length; j++){
                temp = (temp - shapelet[j]);
                sum = sum + (temp * temp);
            }
            
            if (sum < min_sum){
            	min_sum = sum;
            }
        }

        return (min_sum == 0.0) ? 0.0 : (1.0 / shapelet.length * min_sum);
	}	
	
	private double subsequnce_mean(double sequence[], int start, int end) {
	       double mean;
	       double sum = 0;

	       for (int i = start; i < end; i++){
	    	   sum += sequence[i];
	       }

	       mean = sum / (double)  (end - start);
	       return mean;
	}
	
	private double subsequnce_stdv(double sequence[], int start, int end, double mean) {
		double stdv = 0;
	    double temp;

       for (int i = start; i < end; i++){
           temp = (sequence[i] - mean);
           stdv += temp * temp;
       }
	   return stdv = (stdv < Shapelet.ROUNDING_ERROR_CORRECTION) ? 0.0 : Math.sqrt(stdv);
	}
	
	
	
//	public static void main(String[] args) {
//		System.out.println("shapelet transform splitter test");
//		
//		
//		ListDataset data = new ListDataset(10,8);
//		
//		
//		data.add(0, new double[] {1.0,1.3,1.45,1.0,1.0,1.0,1.0,1.0});
//		data.add(0, new double[] {1.0,1.25,1.5,1.25,1.0,1.0,1.0,1.0});
//		data.add(0, new double[] {1.0,1.0,1.3,1.0,1.0,1.0,1.0,1.0});
//		data.add(0, new double[] {1.0,1.25,1.5,1.25,1.0,1.0,1.0,1.0});
//		data.add(0, new double[] {1.0,1.25,1.4,1.3,1.0,1.0,1.0,1.0});
//
//		data.add(1, new double[] {1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0});
//		data.add(1, new double[] {1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0});
//		data.add(1, new double[] {1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0});
//		data.add(1, new double[] {1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0});
//		data.add(1, new double[] {1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0});
//
//		
//		
//		
//		ShapeletTransformSplitter splitter = new ShapeletTransformSplitter(null);
//		
//		
//		Dataset[] splits;
//		try {
//			splits = splitter.find_best_split(data);
//			
//			System.out.println(splits.length);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		
//		
//	}
//	
	
	private String printClassDistribution(TIntIntMap class_distribution, int total_size) {
		StringBuilder str = new StringBuilder();
		
		str.append('{');
		
		for (int key : class_distribution.keys()) {
			str.append(key);
			str.append(':');
			str.append(class_distribution.get(key));
			str.append(',');			
		}

		str.deleteCharAt(str.length()-1);
		
		str.append('}');
		
		str.append('=');
		str.append(gini(class_distribution, total_size));
				
		return str.toString();
	}

	@Override
	public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		int size = nodeTrainData.size();
		int MAX_SHAPELETS = AppConfig.st_splitters_per_node;
		
		List<SplitCriterion> splitCriteria = new ArrayList<SplitCriterion>(MAX_SHAPELETS);
		shapelets = new ArrayList<ShapeletEx>(MAX_SHAPELETS);
		
		ShapeletEx shapelet;

		for (int i = 0; i < MAX_SHAPELETS; i++) {

//			if (AppContext.st_param_selection == ParamSelection.Random) {
				
				int r = rand.nextInt(nodeTrainData.size());
				TimeSeries s = nodeTrainData.getSeries(r);
				
				shapelet = new ShapeletEx(s);				
				
				shapelet.initRandomly(rand);	
				
//			}else if (AppContext.st_param_selection == ParamSelection.PreLoadedSet) {
//				
//				//choose random shapelet from one of the preloaded settings
//				ShapeletTransformDataLoader.STInfoRecord rec = this.node.tree.getForest().stLoader.getRandomShapelet();
//
//				//pick a random series. --> note: rec.seriesId is not useful here because that series might not be present at this node.
//				//just need to use start and end data
//				
//				int r = rand.nextInt(data.size());	//TODO NOTE ---------> FIX THIS -- select a random instance from the same class
//				TimeSeries s = data.get_series(r);
//				
//				shapelet = new ShapeletEx(s, rec.startPos, rec.startPos+rec.length, true, AppContext.st_normalize);	
//								
//			}else {
//				throw new Exception("Unsupported parameter selection method for shapelet splitter -- invalid arg: -st_params ");
//			}
			

			if (AppConfig.st_normalize) {
				shapelet.normalize();
			}
			
			this.node.tree.getForest().getExpResultCollection().allShapelets.add(shapelet.toString());
			shapelets.add(shapelet);
			
			SplitCriterion sc = trainSingleShapelet(nodeTrainData, trainIndices, shapelet, size);
			
			sc.attribute = i; //add index of shapelet in the list so that we can later find the shapelet that produced this criteria
			
			splitCriteria.add(sc);
		}
		
		best_criterion = findBestCriterion(splitCriteria); //find criteria with min wgini
		best_shapelet = shapelets.get(best_criterion.attribute);
		
//		double delta =  split(data, indices);
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.st_splitter_train_time += (System.nanoTime() - startTime);
		
		return split(nodeTrainData, trainIndices);
	}
	
	
	public SplitCriterion trainSingleShapelet(Dataset data, Indexer trainIndices, ShapeletEx shapelet , int size) throws Exception {

		
		List<DblIntPair> distances = new ArrayList<DblIntPair>(size);
		
		List<DblIntPair> distances_slow = new ArrayList<DblIntPair>(size);

		
		for (int i = 0; i < size; i++) {
			distances_slow.add(new DblIntPair(shapelet.distance(data.getSeries(i)), data.getClass(i)));	//TODO just to debug
			
//			distances.add(new DblIntPair(shapelet.online_distance(data.get_series(i)), data.get_class(i)));
		}
		
		distances = distances_slow; //TODO temp
		
		SplitCriterion sc;
		
		//TODO DEV DEBUG check
		if (distances.get(0).key == 0) {
//			System.out.println("check why dist == 0");
		}
		
		if (AppConfig.st_threshold_selection == ThresholdSelectionMethod.BestGini) {
			sc = StaticGiniSplitter.train(data, distances);
		}else if (AppConfig.st_threshold_selection == ThresholdSelectionMethod.Median) {
			sc = selectMedianValueThreshold(data, distances, shapelet);
		}else if (AppConfig.st_threshold_selection == ThresholdSelectionMethod.Random) {
			sc = StaticGiniSplitter.fitRandomThreshold(data, distances, shapelet);
		}else {
			throw new Exception("ThresholdSelectionMethod not supported -- make sure -st_threshold_selection argument is valid");
		}
				
		return sc;
	}
	
	
	public static SplitCriterion selectMedianValueThreshold(Dataset data, List<DblIntPair> values, ShapeletEx shapelet) throws Exception {
		double weighted_gini = Double.POSITIVE_INFINITY;
		int median = 0;
		
		//sort the pairs
		Collections.sort(values, (a,b)->{
//			System.out.println("a:" + a.key + " b: " + b.key + " a-b: " + (int) (a.key - b.key));
			return Double.compare(a.key, b.key);	//TODO use a dbl compare class with a threshold
		});	    
	    
		median = values.size() / 2;
		median = median > 0 && median % 2 == 0 ? median - 1 : median;
		
		TIntObjectMap<Dataset> splits = split(data, null, shapelet, median);
		
		weighted_gini = weighted_gini(splits, data.size());
		
		SplitCriterion medianCriteria = new SplitCriterion(median, weighted_gini, ThresholdSelectionMethod.Median);
		
		return medianCriteria;
	}
	
	//find a min max range, and then find random numbers
	//this uses a min max
	public static SplitCriterion selectRandomThreshold(Dataset data, List<DblIntPair> values, ShapeletEx shapelet) throws Exception {
		double weighted_gini = Double.POSITIVE_INFINITY;
		double best_weighted_gini = Double.POSITIVE_INFINITY;
		int min = 0;
		int max = 1;
		int r = 0; 
		int best_threshold = 0;
		int num_tries = application.AppConfig.st_num_rand_thresholds;
		
		//TODO quick hack -- optimize using one loop
		max = values .stream()
			      .mapToInt(v -> v.value)
			      .max().orElseThrow(NoSuchElementException::new);
		
		min = values .stream()
			      .mapToInt(v -> v.value)
			      .min().orElseThrow(NoSuchElementException::new);
		
		for (int i = 0; i < num_tries; i++) {
			r = Util.getRandNextInt(min, max);
			
			//split
			TIntObjectMap<Dataset> splits = split(data, null, shapelet, r);
			weighted_gini = weighted_gini(splits, data.size());

			
			if (weighted_gini < best_weighted_gini) {
				best_weighted_gini = weighted_gini;
				best_threshold = r;
			}
				
		}
		
		
		SplitCriterion bestRandomCriteria = new SplitCriterion(best_threshold, best_weighted_gini, ThresholdSelectionMethod.Random);
		return bestRandomCriteria;
	}
	
	
	private SplitCriterion findBestCriterion(List<SplitCriterion> splitCriteria) {
		int i = 0;
		double min =  Double.POSITIVE_INFINITY;
		int min_index = 0;
				
		for (i = 0; i < splitCriteria.size(); i++) {
			if (splitCriteria.get(i).wgini < min) {
				min_index = i;
				min = splitCriteria.get(i).wgini;
			}
		}
		
		return splitCriteria.get(min_index);
				
	}
//	
	@Override
	public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();

		TIntObjectMap<Dataset> branches = split(nodeTrainData, trainIndices, best_shapelet, best_criterion.threshold);
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.st_splitter_train_time += (System.nanoTime() - startTime);
		
		return branches;
	}
	

	//this function does not measure time separately -- usually used during training the splitter
	public static TIntObjectMap<Dataset> split(Dataset trainData, Indexer trainIndices, ShapeletEx shapelet, double threshold) throws Exception {

		int size = trainData.size();

		TIntObjectMap<Dataset> branches = new TIntObjectHashMap<Dataset>(NUM_CHILDREN);
				
		branches.putIfAbsent(LEFT_BRANCH, new MTSDataset(size)); //TODO size is an overestimate
		branches.putIfAbsent(RIGHT_BRANCH, new MTSDataset(size)); //TODO size is an overestimate
		
		for (int i = 0; i < size; i++) {
			
			if (shapelet.distance(trainData.getSeries(i)) < threshold){
				branches.get(LEFT_BRANCH).add(trainData.getSeries(i));
			}else {
				branches.get(RIGHT_BRANCH).add(trainData.getSeries(i));
			}

		}
		
		return branches;
	}


	@Override
	public int predict(TimeSeries query, Dataset testData, int queryIndex) throws Exception {
		
		if (best_shapelet.distance(query) < best_criterion.threshold){
			return LEFT_BRANCH;
		}else {
			return RIGHT_BRANCH;
		}
		
	}

	@Override
	public TIntObjectMap<TIntArrayList> fitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		throw new NotSupportedException();
	}

	@Override
	public TIntObjectMap<TIntArrayList> splitByIndices(Dataset allTrainData, Indexer trainIndices) throws Exception {
		throw new NotSupportedException();
	}

}

















