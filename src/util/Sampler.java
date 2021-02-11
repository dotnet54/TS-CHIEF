package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import application.AppConfig;
import data.timeseries.Dataset;
import data.timeseries.MTSDataset;
import gnu.trove.map.TIntObjectMap;

public class Sampler {
	
	private static Random rand = new Random(System.nanoTime());

//	static {
//		rand = AppConfig.getRand();
//	}

//	public Sampler(Random rand) {
//		this.rand = rand;
//	}
//
//	public Sampler(long randSeed) {
//		this.rand = new Random(randSeed);
//	}

	public static void setRand(Random rand){
		Sampler.rand = rand;
	}

	public static void setRandSeed(long randSeed){
		Sampler.rand = new Random(randSeed);
	}
	
	
// Reference
// https://stackoverflow.com/questions/4702036/take-n-random-elements-from-a-liste?
// utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
	public static <E> List<E> pickNRandomElements(List<E> list, int n) {
	    int length = list.size();

	    if (length < n) return null;

	    //We don't need to shuffle the whole list
	    for (int i = length - 1; i >= length - n; --i)
	    {
	        Collections.swap(list, i , rand.nextInt(i + 1));
	    }
	    return list.subList(length - n, length);
	}

	
	/**
	 * An improved version (Durstenfeld) of the Fisher-Yates algorithm with O(n) time complexity
	 * Permutes the given array
	 * @param array array to be shuffled
	 * reference
	 * @url http://www.programming-algorithms.net/article/43676/Fisher-Yates-shuffle
	 */
	public static void fisherYatesKnuthShuffle(int[] array) {
	    for (int i = array.length - 1; i > 0; i--) {
	        int index = rand.nextInt(i);
	        //swap
	        int tmp = array[index];
	        array[index] = array[i];
	        array[i] = tmp;
	    }
	}

	public static int[] sampleNRandomInts(int[] population, int n) {
		fisherYatesKnuthShuffle(population);
		int[] sample = new int[n];
		for (int i = 0; i < n; i++) {
			sample[i] = population[i];
		}
		return sample;
	}

	public static int[] sampleNRandomIntsFromRange(int min, int max, int n) {
		int[] population = new int[max-min];
		for (int i = min; i < max; i++) {
			population[i] = i;
		}
		fisherYatesKnuthShuffle(population);
		if (n > population.length){
			n = population.length;
		}
		int[] sample = new int[n];
		for (int i = 0; i < n; i++) {
			sample[i] = population[i];
		}
		return sample;
	}

	public static int[] getIntsFromRange(int min, int max, int step) {
		int[] range = new int[max-min];
		for (int i = min; i < max; i += step) {
			range[i] = i;
		}
		return range;
	}

	public static Dataset uniform_sample(Dataset dataset, int n) throws Exception {
		
		n = n > dataset.size() ? dataset.size() : n;

		Dataset sample = new MTSDataset(n);
		
		int[] indices = new int[n];
		for (int i = 0; i < n; i++) {
			indices[i] = i;
		}
		Sampler.fisherYatesKnuthShuffle(indices);
	
		for (int i = 0; i < n; i++) {
			sample.add(dataset.getSeries(i));
		}
		
		return sample;
	}
	
	//TODO naive implementation, quick fix, check only dimension 0 to exclude
	public static Dataset uniform_sample(Dataset dataset, int n, double[][] exclude) throws Exception {
		Dataset sample = Sampler.uniform_sample(dataset, n);
		int size = sample.size();
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < exclude.length; j++) {
				if (sample.getSeries(i).data()[0] == exclude[j]) {
					sample.remove(i);
				}
			}
		}
		
		return sample;
	}
	
	public static Dataset stratified_sample(TIntObjectMap<Dataset> data_per_class,
			int n_per_class, boolean shuffle, double[][] exclude) throws Exception {
		Dataset sample = new MTSDataset(data_per_class.size() * n_per_class);
		Dataset class_sample;
		int class_sample_size;
		
		for (int key : data_per_class.keys()) {
			
			if (exclude == null) {
				class_sample = Sampler.uniform_sample(data_per_class.get(key), n_per_class);
			}else {
				class_sample = Sampler.uniform_sample(data_per_class.get(key), n_per_class, exclude);
			}
			class_sample_size = class_sample.size();

			for (int i = 0; i < class_sample_size; i++) {
				sample.add(class_sample.getSeries(i));
			}
		}
		
		if (shuffle) {
			sample.shuffle(); //NOTE seed
		}		
		
		return sample;
	}
	
	public static Map<Integer, Dataset> stratified_sample_per_class(
			TIntObjectMap<Dataset> data_per_class, int n_per_class,
			boolean shuffle_each_class, double[][] exclude) throws Exception {
		Map<Integer, Dataset> sample = new HashMap<Integer, Dataset> ();
		Dataset class_sample;
		
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
