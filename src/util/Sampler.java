package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import core.AppContext;
import datasets.TSDataset;
import gnu.trove.map.TIntObjectMap;

public class Sampler {
	
	private static Random rand = new Random();
	
	public Sampler(Random rand) {
	
//		this.rand = rand;
	}
	
	
//reference
//https://stackoverflow.com/questions/4702036/take-n-random-elements-from-a-liste?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
	
	public static <E> List<E> pickNRandomElements(List<E> list, int n, Random r) {
	    int length = list.size();

	    if (length < n) return null;

	    //We don't need to shuffle the whole list
	    for (int i = length - 1; i >= length - n; --i)
	    {
	        Collections.swap(list, i , r.nextInt(i + 1));
	    }
	    return list.subList(length - n, length);
	}

	public static <E> List<E> pickNRandomElements(List<E> list, int n) {
	    return pickNRandomElements(list, n, ThreadLocalRandom.current());
	}	
	
	
	/**
	 * An improved version (Durstenfeld) of the Fisher-Yates algorithm with O(n) time complexity
	 * Permutes the given array
	 * @param array array to be shuffled
	 * reference
	 * @url http://www.programming-algorithms.net/article/43676/Fisher-Yates-shuffle
	 */
	public static void fisherYatesKnuthShuffle(int[] array) {
//	    Random r = new Random();
	    for (int i = array.length - 1; i > 0; i--) {
	        int index = rand.nextInt(i);
	        //swap
	        int tmp = array[index];
	        array[index] = array[i];
	        array[i] = tmp;
	    }
	} 	
	
	public static TSDataset uniform_sample(TSDataset dataset, int n) {
		
		n = n > dataset.size() ? dataset.size() : n;
		
		TSDataset sample = new TSDataset(n, dataset.length());
		
		int[] indices = new int[n];
		for (int i = 0; i < n; i++) {
			indices[i] = i;
		}
		Sampler.fisherYatesKnuthShuffle(indices);
	
		for (int i = 0; i < n; i++) {
			sample.add(dataset.get_series(i));
		}
		
		return sample;
	}
	
	//TODO naive implementation, quick fix
	public static TSDataset uniform_sample(TSDataset dataset, int n, double[][] exclude) {
		TSDataset sample = Sampler.uniform_sample(dataset, n);
		int size = sample.size();
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < exclude.length; j++) {
				if (sample.get_series(i).getData() == exclude[j]) {
					sample.remove(i);
				}
			}
		}
		
		return sample;
	}
	
	public static TSDataset stratified_sample(TIntObjectMap<TSDataset> data_per_class, 
			int n_per_class, boolean shuffle, double[][] exclude) {
		TSDataset sample = new TSDataset(data_per_class.size() * n_per_class);
		TSDataset class_sample;
		int class_sample_size;
		
		for (int key : data_per_class.keys()) {
			
			if (exclude == null) {
				class_sample = Sampler.uniform_sample(data_per_class.get(key), n_per_class);
			}else {
				class_sample = Sampler.uniform_sample(data_per_class.get(key), n_per_class, exclude);
			}
			class_sample_size = class_sample.size();

			for (int i = 0; i < class_sample_size; i++) {
				sample.add(class_sample.get_series(i));
			}
		}
		
		if (shuffle) {
			sample.shuffle(); //NOTE seed
		}		
		
		return sample;
	}
	
	public static Map<Integer, TSDataset> stratified_sample_per_class(
			TIntObjectMap<TSDataset> data_per_class, int n_per_class, 
			boolean shuffle_each_class, double[][] exclude) {
		Map<Integer, TSDataset> sample = new HashMap<Integer, TSDataset> ();
		TSDataset class_sample;
		
		for (int key: data_per_class.keys()) {
			if (exclude == null) {
				class_sample = Sampler.uniform_sample(data_per_class.get(key), n_per_class);
			}else {
				class_sample = Sampler.uniform_sample(data_per_class.get(key), n_per_class, exclude);
			}			
			
			if (shuffle_each_class) {
				class_sample.shuffle();
			}	
			
			sample.put(key, class_sample);
		}
		
		return sample;
	}		
}
