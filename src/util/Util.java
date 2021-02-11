package util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import application.AppConfig;
import data.timeseries.Dataset;
import org.apache.commons.lang3.time.DurationFormatUtils;

import data.timeseries.MTSDataset;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;

public class Util {

	private static Random rand = new Random(System.nanoTime());
	public static void setRand(Random rand){
		Util.rand = rand;
	}
	public static void setRandSeed(long randSeed){
		Util.rand = new Random(randSeed);
	}

	public static String getCurrentTimeStamp(String format) {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
	}
	
	public static String formatTime(long duration, String format) {
		return DurationFormatUtils.formatDuration((long) duration, "H:m:s.SSS");
	}
	
	public static void warmUpJavaRuntime() {
		//TODO
		System.out.println("TODO doing some extra work to warm up jvm..."
				+ "this helps to measure time more accurately for short experiments");
	}
	
	
	
	//TODO test this 
	public static int[] non_repeating_rand(int n, int min, int max, Random rand) {
		int[] tmp = new int[max - min];
		int[] tmp2 = new int[n];		
		
//		for (int i = min; i < max; i++) {
//			tmp[i] = i;
//		}
//		
//		Collections.shuffle(Arrays.asList(tmp), AppConfig.getRand());
//		
//		for (int i = 0; i < tmp2.length; i++) {
//			tmp2[i] = tmp[i];
//		}

		List<Integer> randomNumbers = rand.ints(min, max).distinct().limit(n).boxed().collect(Collectors.toList());
		int [] ints = randomNumbers.stream().mapToInt(Integer::intValue).toArray();
		
		return ints;
	}
	
	
	//TODO convert this to Yates Fisher algorithm
	public static int[] shuffleArray(int[] array) {
		List<Integer> list = new ArrayList<>();
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
		Collections.shuffle(list, rand);
		return list.stream().mapToInt(i->i).toArray();
	}

	
	//FIXME
//	public static <T> T[] shuffleArray(T[] array) {
//		List<T> list = new ArrayList<>();
//		for (int i = 0; i < array.length; i++) {
//			list.add(array[i]);
//		}
//		Collections.shuffle(list, AppConfig.getRand());
//		return list.toArray(new T[array.length]);
////		return list.stream().mapToInt(i->i).toArray();
//	}
	
	
	public static double weighted_gini(TIntObjectMap<Dataset> splits, int parent_node_size) {
		double wgini = 0.0;
		
		//TODO handle case when a split contains null object
		for (int key : splits.keys()) {
			wgini = wgini + ((double) splits.get(key).size() / parent_node_size) * splits.get(key).gini();
		}

		return wgini;
	}

	
	public static double weighted_gini(Dataset[] splits, int parent_node_size) {
		double wgini = 0.0;
		
		//TODO handle case when a split contains null object
		for (int i = 0; i < splits.length; i++) {
			wgini = wgini + ((double) splits[i].size() / parent_node_size) * splits[i].gini();
		}

		return wgini;
	}
	
	public static double weighted_gini(TIntObjectMap<Dataset> splits) {
		int total_size = 0;
		
		//TODO handle case when a split contains null object
		for (int key : splits.keys()) {
			TIntIntMap class_dist = splits.get(key).getClassDistribution();
			for (int class_label: class_dist.keys()) {
				total_size += class_dist.get(class_label);
			}
		}

		return weighted_gini(splits, total_size);
	}

	
	public static double weighted_gini(Dataset[] splits) {
		int total_size = 0;
		
		//TODO handle case when a split contains null object
		for (int i = 0; i < splits.length; i++) {
			TIntIntMap class_dist = splits[i].getClassDistribution();
			for (int class_label: class_dist.keys()) {
				total_size += class_dist.get(class_label);
			}
		}

		return weighted_gini(splits, total_size);
	}
	
	public static double gini(TIntIntMap class_distribution, int total_size) {
		double sum = 0.0;
		double p;
		
		for (int key: class_distribution.keys()) {
			p = (double)class_distribution.get(key) / total_size;
			sum += p * p;
		}
		return 1 - sum;
	}	
	
	public static double gini(TIntIntMap class_distribution) {
		int total_size = 0;
		
		for (int key: class_distribution.keys()) {
			total_size += class_distribution.get(key);
		}
		
		
		return gini(class_distribution, total_size);
	}	

	//TODO test
	public static Integer majority_class(Dataset data) {
		TIntIntMap map = data.getClassDistribution();
		List<Integer> majority_class = new ArrayList<Integer>();
		int bsf_count = Integer.MIN_VALUE;
		
		for (int key : map.keys()) {
			if (bsf_count < map.get(key)) {
				bsf_count = map.get(key);
				majority_class.clear();
				majority_class.add(key);
			}else if (bsf_count == map.get(key)) {
				majority_class.add(key);
			}
		}		
		
		int r = rand.nextInt(majority_class.size());
		return majority_class.get(r);
	}	
	
	  public static String getOperatingSystemType() {
		      String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		      if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
		        return "mac";
		      } else if (OS.indexOf("win") >= 0) {
		    	  return "win";
		      } else if (OS.indexOf("nux") >= 0) {
		    	  return "linux";
		      } else {
		    	 return "other";
		      }
		  }
	
	// these are temp functions to remove drive letter from path, -- makes life easier when working on both mac and win
	public static String convertToLinuxPath(String path) {
		// just remove E: drive letter from path
		return path.replaceFirst("E:", "");
	}
	
	public static String convertToWindowsPath(String path) {
		//just add E: drive to path
		return "E:" + path;
	}
	
	public static String toOsPath(String path) {
//		if (getOperatingSystemType().equals("win")) {
//			if (path.startsWith("E:")) {
//				return path;
//			}else {
//				return convertToWindowsPath(path);
//			}
//		}else {
//			if (path.startsWith("E:")) {
//				return convertToLinuxPath(path);
//			}else {
//				return path;
//			}
//		}
		return path; //temp disabled
	}
	
	//TODO 28/11/2019 delete -- moved to data.io package DataLoader class
//	//quick helper functions to load UCR dataset
//	public static MTSDataset loadTrainSet(String name) {
//		return Util.loadDataset(toOsPath(AppConfig.temp_data_path), name, "TRAIN");
//	}
//	
//	public static MTSDataset loadTestSet(String name) {
//		return Util.loadDataset(toOsPath(AppConfig.temp_data_path), name, "TEST");
//	}
//	
//	public static MTSDataset loadDataset(String path, String name, String trainOrTest) {
//		
//		String fullName = path + "/" + name + "/" + name + "_" + trainOrTest.toUpperCase() + ".txt";
//		
//		return CsvReader.readCSVToTSDataset(fullName, AppConfig.csv_has_header, 
//				AppConfig.csv_label_column, ",", AppConfig.verbosity);
//	}
	
	
	public static int getRandomNumberInRange(int min, int max) {

		if (min >= max) {
			throw new IllegalArgumentException("getRandomNumberInRange - max must be greater than min");
		}

		return rand.nextInt((max - min) + 1) + min;
	}
	
	
	public static int getRandNextInt(int min, int max) {

		if (min > max) {
			throw new IllegalArgumentException("getRandomNumberInRange - max must be greater than min");
		}

		return rand.nextInt((max - min) + 1) + min;
	}
	
	public static int[] getRandomIntervalUsingUniformPoints(Random rand, int min, int max, int seriesLength) throws Exception {
		int j = 0;
		int[] interval = new int[3]; //[start_index,end_index,length] -- length is not necessary, but just storing it to avoid recalculating again
		int temp;

		if (max > seriesLength || max <= 0) {	//if max == 0, assume max possible length
			max = seriesLength;
		}
		if (min < 1) {
			min = 1;
		}
		
        do {
        	interval[0] = rand.nextInt(seriesLength - 1);
        	interval[1] = rand.nextInt(seriesLength - 1);
	        j++;
	        temp = Math.abs(interval[0]  - interval[1]) + 1;
        } while((temp < min || temp > max) && j <= AppConfig.MAX_ITERATIONS_RAND_INTERVAL);
        
        if (j>= AppConfig.MAX_ITERATIONS_RAND_INTERVAL) {
        	throw new Exception("Error in function initRandomIntervalUsingUniformPoints: Failed to find a random interval with min "
        			+ min + " and max " +max+ " after MAX_ITERATIONS: " + AppConfig.MAX_ITERATIONS_RAND_INTERVAL);
        }			
			

        interval[2] = temp; //store the length also in the output array
        
		if (interval[1] < interval[0]) {
			temp = interval[0];
			interval[0] = interval[1];
			interval[1] = temp;
		}
		
		return interval;
	}
	
	
	// max length = series length
	// min length = 1
	// output = [start_index,end_index,length] -- length is not necessary, but just storing it to avoid recalculating again
	public static int[] getRandomIntervalSamplingLengthFirst(Random rand, int min, int max, int seriesLength) throws Exception {
		int[] interval = new int[3]; //[start_index,end_index,length]
		
		if (max > seriesLength || max <= 0) {//if max == 0, assume max possible length
			max = seriesLength;
		}
		if (min < 1) {
			min = 1;
		}

		interval[2] = rand.nextInt((max - min) + 1) + min; //interval[2] is length of the interval
		
    	interval[0] = rand.nextInt((seriesLength - interval[2]) + 1);
    	//min and and max are in terms of length which starts at 1, but interval[0] and [1] are in terms of index positions which start at 0
    	interval[1] = interval[0] + interval[2] - 1;	 

		return interval;
	}

	/**
	 * Return evenly spaced numbers over a specified interval.
	 * Returns num evenly spaced samples, calculated over the interval [start, stop].
	 *
	 * //https://numpy.org/doc/stable/reference/generated/numpy.linspace.html
	 *
	 * @param start
	 * @param stop
	 * @param num
	 * @param endpoint if True, result includes stop
	 * @return
	 */
	public static int[] linspaceInt(int start,int stop,int num,boolean endpoint){
		double[] tmp = linspaceDbl(start,stop, num, endpoint);
		int[] output = new int[tmp.length];
		for (int i = 0; i < tmp.length; i++) {
			output[i] = (int) Math.round(tmp[i]);
		}
		return output;
	}

	/**
	 * Return evenly spaced numbers over a specified interval.
	 * Returns num evenly spaced samples, calculated over the interval [start, stop].
	 *
	 * //https://numpy.org/doc/stable/reference/generated/numpy.linspace.html
	 *
	 * @param start
	 * @param stop
	 * @param num
	 * @param endpoint
	 * @return
	 */
	public static double[] linspaceDbl(double start,double stop,int num,boolean  endpoint){
		double[] output = new double[num];
		int div;
		if (endpoint){
			div = num - 1;
		}else{
			div = num;
		}
		double step = (stop-start)/div;
		output[0] = start;
		for(int i = 1; i < div; i++){
			output[i] = output[i-1]+step;
		}
		if (endpoint){
			output[div] = stop;
		}
		return output;
	}

}










