package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;

import core.AppContext;
import core.CSVReader;
import datasets.TSDataset;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;

public class Util {

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
//		Collections.shuffle(Arrays.asList(tmp));
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
		Collections.shuffle(list);
		return list.stream().mapToInt(i->i).toArray();
	}

	
	//FIXME
//	public static <T> T[] shuffleArray(T[] array) {
//		List<T> list = new ArrayList<>();
//		for (int i = 0; i < array.length; i++) {
//			list.add(array[i]);
//		}
//		Collections.shuffle(list);
//		return list.toArray(new T[array.length]);
////		return list.stream().mapToInt(i->i).toArray();
//	}
	
	
	public static double weighted_gini(TIntObjectMap<TSDataset> splits, int parent_node_size) {
		double wgini = 0.0;
		
		//TODO handle case when a split contains null object
		for (int key : splits.keys()) {
			wgini = wgini + ((double) splits.get(key).size() / parent_node_size) * splits.get(key).gini();
		}

		return wgini;
	}

	
	public static double weighted_gini(TSDataset[] splits, int parent_node_size) {
		double wgini = 0.0;
		
		//TODO handle case when a split contains null object
		for (int i = 0; i < splits.length; i++) {
			wgini = wgini + ((double) splits[i].size() / parent_node_size) * splits[i].gini();
		}

		return wgini;
	}
	
	public static double weighted_gini(TIntObjectMap<TSDataset> splits) {
		int total_size = 0;
		
		//TODO handle case when a split contains null object
		for (int key : splits.keys()) {
			TIntIntMap class_dist = splits.get(key).get_class_map();
			for (int class_label: class_dist.keys()) {
				total_size += class_dist.get(class_label);
			}
		}

		return weighted_gini(splits, total_size);
	}

	
	public static double weighted_gini(TSDataset[] splits) {
		int total_size = 0;
		
		//TODO handle case when a split contains null object
		for (int i = 0; i < splits.length; i++) {
			TIntIntMap class_dist = splits[i].get_class_map();
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
	public static Integer majority_class(TSDataset data) {
		TIntIntMap map = data.get_class_map();
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
		
		int r = ThreadLocalRandom.current().nextInt(majority_class.size());
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
	
	public static String adaptPathToOS(String path) {
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
	
	//quick helper functions to load UCR dataset
	public static TSDataset loadTrainSet(String name) {
		return Util.loadDataset(adaptPathToOS(AppContext.temp_data_path), name, "TRAIN");
	}
	
	public static TSDataset loadTestSet(String name) {
		return Util.loadDataset(adaptPathToOS(AppContext.temp_data_path), name, "TEST");
	}
	
	public static TSDataset loadDataset(String path, String name, String trainOrTest) {
		
		String fullName = path + "/" + name + "/" + name + "_" + trainOrTest.toUpperCase() + ".txt";
		
		return CSVReader.readCSVToTSDataset(fullName, AppContext.csv_has_header, 
				AppContext.target_column_is_first, ",", AppContext.verbosity);
	}
	
	
	
}










