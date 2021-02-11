package util;

import java.util.Arrays;
import java.util.List;

public class Statistics {

	public static double sum(double[] list) {
		double sum = 0;
		for (int i = 0; i < list.length; i++) {
			sum += list[i];
		}
		return sum;
	}	
	
	public static long sum(long[] list) {
		long sum = 0;
		for (int i = 0; i < list.length; i++) {
			sum += list[i];
		}
		return sum;
	}	
	
	public static int sum(int[] list) {
		int sum = 0;
		for (int i = 0; i < list.length; i++) {
			sum += list[i];
		}
		return sum;
	}		
	
	public static int sumIntList(List<Integer> list) {
		int sum = 0;
		int length = list.size();
		for (int i = 0; i < length; i++) {
			sum +=list.get(i);
		}
		return sum;
	}

	public static double sumDoubleList(List<Double> list) {
		double sum = 0;
		int length = list.size();
		for (int i = 0; i < length; i++) {
			sum += list.get(i);
		}
		return sum;
	}
	
	public static double meanIntList(List<Integer> list) {
		return Statistics.sumIntList(list) / list.size();
	}	
	
	public static double meanDoubleList(List<Double> list) {
		return Statistics.sumDoubleList(list) / list.size();
	}	
	
	public static double mean(double[] list) {
		double sum = 0;
		for (int i = 0; i < list.length; i++) {
			sum += list[i];
		}
		return sum/list.length;
	}
	
	public static double mean(int[] list) {
		int sum = 0;
		for (int i = 0; i < list.length; i++) {
			sum += list[i];
		}
		return sum/list.length;
	}
	
	public static double mean(long[] list) {
		long sum = 0;
		for (int i = 0; i < list.length; i++) {
			sum += list[i];
		}
		return sum/list.length;
	}
	
	public static double standard_deviation_population(double[] list) {
		double mean = Statistics.mean(list);
		double sq_sum = 0;
		
		for (int i = 0; i < list.length; i++) {
			sq_sum += (list[i] - mean) * (list[i] - mean);
		}
		
		return Math.sqrt(sq_sum/list.length);
	}
	
	public static double standard_deviation_population(long[] list) {
		double mean = Statistics.mean(list);
		long sq_sum = 0;
		
		for (int i = 0; i < list.length; i++) {
			sq_sum += (list[i] - mean) * (list[i] - mean);
		}
		
		return Math.sqrt((double)sq_sum/list.length);
	}

	public static double standard_deviation_population(int[] list) {
		double mean = Statistics.mean(list);
		long sq_sum = 0;
		
		for (int i = 0; i < list.length; i++) {
			sq_sum += (list[i] - mean) * (list[i] - mean);
		}
		
		return Math.sqrt((double)sq_sum/list.length);
	}
	
	public static double median(double[] numArray) {
		Arrays.sort(numArray);
		double median;
		if (numArray.length % 2 == 0)
		    median = ((double)numArray[numArray.length/2] + (double)numArray[numArray.length/2 - 1])/2;
		else
		    median = (double) numArray[numArray.length/2];		
		return median;
	}

//	/**
//	 *  commenting this method -- USE new implementation in class StandardDeviation 13/10/2020
//	 *
//	 * Welford's online algoirithm for standard deviation
//	 *
//	 * Reference: https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
//	 *
//	 * @param aggregate array of 5 values for existing values[count, mean, sqDistanceFromMean, varPopulation. stdPopulation]
//	 * @param newValue new value to add to the aggregates
//	 * @return array of 3 values for the next call [count, mean, sqDistanceFromMean, varPopulation. stdPopulation]
//	 */
//	public static double[] stdvPopulationOnline(double[] aggregate, double newValue, boolean updateAggregates){
//		if (aggregate == null || aggregate.length != 5){
//			aggregate = new double[5];
//		}
//
//		if (updateAggregates){
//			aggregate[0] += 1; //count += 1
//			double delta = newValue - aggregate[1]; //delta = newValue - mean
//			aggregate[1] += delta / aggregate[0]; //mean += delta / count
//			double delta2 = newValue - aggregate[1]; //delta2 = newValue - mean
//			aggregate[2] += delta * delta2; //sqDistanceFromMean += delta * delta2
//		}else{
//			if (aggregate[0] < 2){
//				return new double[]{1,newValue,0,0,0};
//			}else{
//				aggregate[3] = aggregate[2] / aggregate[0];  //sqDistanceFromMean / count
//				aggregate[4] = Math.sqrt(aggregate[2] / aggregate[0]); //sqDistanceFromMean / count
////				if (Double.isNaN(aggregate[3])){
////					throw new RuntimeException("stdvPopulationOnline: sqDistanceFromMean aggregate[3]" + aggregate[3]);
////				}
////				if (Double.isNaN(aggregate[4])){
////					throw new RuntimeException("stdvPopulationOnline: varPopulation aggregate[4]" + aggregate[4]);
////				}
//			}
//		}
//		return aggregate;
//	}
//
}
