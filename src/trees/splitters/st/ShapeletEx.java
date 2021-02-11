package trees.splitters.st;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import application.AppConfig;
import data.timeseries.TimeSeries;
import data.timeseries.UTimeSeries;
import util.Util;

public class ShapeletEx {
    public static final double ROUNDING_ERROR_CORRECTION = 0.000000000000001;

	protected TimeSeries series;
	protected int start;
	protected int end;
	protected int length;
	protected boolean normalized = false;
	
	
	protected double[] data = null;
	protected int[] sorted_indices;  //speed up technique: Refer to Hills2014 and Rakthanmanon2012 

	public ShapeletEx(TimeSeries series) {
		this.series = series;
		this.start = 0;
		this.length = series.length();
		this.end = this.length + 1;
	}	
	
	public ShapeletEx(TimeSeries series, int start, int end, boolean copy, boolean normalize) {
		this.series = series;
		this.start = start; //start > 0 && start < end ? start: 0;
		this.end = end; //end > start && end <= series.getLength() + 1 ? end: series.getLength();
		
		this.length = end - start; //assert : min length == 1, max length = series.length
		
		if (copy) {
			//then copy data
			this.data = getData();
		}
		
		if (normalize) {	//THEN ALWAYS COPY DATA
			//then normalize the shapelet
			this.data = normalize();
			normalize();
		}
		
		this.normalized = normalize;
	}
	
	public String toString() {
		return this.start + "," + this.end + ","  + this.length;
	}
	
	public double[] copyData() {
		//TODO using only dimension 1
		return Arrays.copyOfRange(this.series.data()[0], this.start, this.end);
	}	
	
	public double[] getData() {
		
		if (this.data == null) {
			return copyData();
		}
		
		return this.data;
	}

	//@reference
	//code based on  from timeseriesweka code -- bakeoff paper
	public double[] normalize() {
		
		if (this.data == null) {
			this.data = getData();
		}

		double mean = 0;
		double stdv = 0;
		double temp = 0;
		
		
		for (int i = 0; i < data.length; i++) {
			temp+= data[i];
		}
		mean = temp / (double) data.length;	//temp = total
		
		temp = 0;
		for (int i = 0; i < data.length; i++) {
			temp = data[i] - mean;
			stdv += temp * temp;
		}
		
		stdv /= data.length;
	    // if the variance is less than the error correction, just set it to 0, else calc stdv.
	    stdv = (stdv < ROUNDING_ERROR_CORRECTION) ? 0.0 : Math.sqrt(stdv);
		
	    for (int i = 0; i < data.length; i++)
	    {
	        //if the stdv is 0 then set to 0, else normalise.
	    	data[i] = (stdv == 0.0) ? 0.0 : ((data[i] - mean) / stdv);
	    }
		
		this.normalized = true;
		return this.data;
	}
	
//	public void initRandomly(Random rand) {
//		
//		this.start = rand.nextInt(this.series.getLength());
//		this.end = rand.nextInt(this.series.getLength());
//		
//		int temp;
//		
//		if (end < start) {
//			temp = start;
//			start = end;
//			end = temp;
//		}
//		
//		this.length = end - start;
//	}
	
	//supports min and max length
	
	protected final int MAX_ITERATIONS = 10000;
	
	public void initRandomly(Random rand) throws Exception {
		int[] interval;
		
		if (AppConfig.st_interval_method.equals("lengthfirst")) {
			
			interval = Util.getRandomIntervalSamplingLengthFirst(rand, AppConfig.st_min_length, AppConfig.st_max_length, this.series.length());
			
		}else if (AppConfig.st_interval_method.equals("swap")) {
			
			interval = Util.getRandomIntervalUsingUniformPoints(rand, AppConfig.st_min_length, AppConfig.st_max_length, this.series.length());
			
		}else {
			throw new Exception("Unsupported random interval selection method");
		}
		
		this.start = interval[0];
		this.end = interval[1];

		this.length = (end - start)+1;
	}
	
//	public void initUsingPreLoadedSet(ShapeletTransformDataLoader stLoader, Random rand) throws Exception {
//		ShapeletTransformDataLoader.STInfoRecord rec = stLoader.getRandomShapelet();
//		
////		this.series = 
//		this.start = rec.startPos;
//		this.end = rec.startPos + rec.length;
//		
//	}

	
	
	//returns the shapelet distance of this shapelet to the given series
	public double distance(TimeSeries series) {
		double total = 0;
		double min_distance = Double.POSITIVE_INFINITY;
		double[] seriesData = series.data()[0]; //TODO using only dimension 1

		if (data == null) { //assume normalized == false
			for (int i = 0; i < seriesData.length - this.length-1; i++) {
				total = 0; 
				for (int j = start; j < end ; j++){
					//TODO using only dimension 1
					total += ((seriesData[i] - series.data(j)[0]) * (seriesData[i] - series.data(j)[0]));
				}	
				if (total < min_distance) {
					min_distance = total;
				}
			}	
		}else {
			
			if (normalized) {
				//TODO optimize this point--> memory allocations - can we reuse the arrays?
				double copy[] = new double [data.length];
				
				for (int i = 0; i < seriesData.length - this.length +1; i++) {
					total = 0; 
					
					//copy array
					//System.arraycopy(seriesData, i, copy, 0, data.length);
					copy = normalizeArray(seriesData, i, i+data.length-1, true);		
					
					for (int j = 0; j < data.length; j++){
						total += ((copy[j] - data[j]) * (copy[j] - data[j]));
					}	
//					total = Math.sqrt(total);
					if (total < min_distance) {
						min_distance = total;
					}
				}
			}else {
				
				for (int i = 0; i < seriesData.length - this.length-1; i++) {
					total = 0; 
					for (int j = 0; j < data.length; j++){
						total += ((seriesData[i+j] - data[j]) * (seriesData[i+j] - data[j]));
					}	
					if (total < min_distance) {
						min_distance = total;
					}
				}				
			}
			

		}
		
		//TODO DEBUG CHECK 
		if (min_distance == 0) {
//			System.out.println("check min_distance == 0");
			
			double copy[] = new double [data.length];
			
			for (int i = 0; i < seriesData.length - this.length +1; i++) {
				total = 0; 
				
				//copy array
				//System.arraycopy(seriesData, i, copy, 0, data.length);
				copy = normalizeArray(seriesData, i, i+data.length-1, true);		
				
				for (int j = 0; j < data.length; j++){
					total += ((copy[j] - data[j]) * (copy[j] - data[j]));
				}	
//				total = Math.sqrt(total);
				if (total < min_distance) {
					min_distance = total;
				}
			}
			
		}
		
		//TODO refer to ImprovedOnlineSubSeqDistance class in bakeoff implementation
		min_distance = (min_distance == 0.0) ? 0.0 : (1.0 / data.length * min_distance);
		//double check above statement

		return min_distance;
	}
	
	//start and end are indices of the array
	public double[] normalizeArray(double[] input, int start, int end, boolean copy) {

		double mean = 0;
		double stdv = 0;
		double temp = 0;
		double[] output;
		
		//TODO change this
		if (copy || start != 0 || end != input.length) {
			output = new double[end - start + 1];  //Arrays.copyOfRange(input, start, end);
		}else {
			output = input;
		}
		
		
		for (int i = 0; i < output.length; i++) {
			temp+= input[start+i];
		}
		mean = temp / (double) output.length;	//temp = total
		
		temp = 0;
		for (int i = 0; i < output.length; i++) {
			temp = input[start+i] - mean;
			stdv += temp * temp;
		}
		
		stdv /= output.length;
	    // if the variance is less than the error correction, just set it to 0, else calc stdv.
	    stdv = (stdv < ROUNDING_ERROR_CORRECTION) ? 0.0 : Math.sqrt(stdv);
		
	    for (int i = 0; i < output.length; i++)
	    {
	        //if the stdv is 0 then set to 0, else normalise.
	    	output[i] = (stdv == 0.0) ? 0.0 : ((input[start+i] - mean) / stdv);
	    }
		
		return output;
	}
	
	
	//returns the shapelet distance of this shapelet to the given series
	public double online_distance(UTimeSeries series) {
		double[] T = series.getData();
		double[] S = this.data;
		double b = 0;
		double d = 0;
		double x = 0;
		double s = 0;
		
		if (data == null) { //assume normalized == false
			//TODO find SD without normalization
		}else {
			//algorithm in Hills2014
			
			//assume S is normalized already
			
			//sort indices
			int[] A = sort_indices(S, true);
			double[] F = normalizeArray(T, 0, S.length, true);
			double p = 0;	//running rum	
			double q = T.length;	//running sum of squares
			b = dist_n(S, F, Double.POSITIVE_INFINITY, 1);
			
			for (int i = 0; i < T.length - S.length; i++) {
				p = p - T[i];
				q = q - (T[i] * T[i]);
				
				p = p - T[i+S.length];
				q = q - (T[i+S.length] * T[i+S.length]);
				
				x = p/S.length;
				s = q/S.length - (x*x);
				d = 0;
				
				int j = 0;
				while( j < S.length & d < b) { //TODO note d < b
					double temp = S[A[j]] - ( (T[i + A[j]] - x)/s );
					d = d + temp * temp;
					j++;
				}
				
				if (j == S.length & d <  b) {
					b = d;
				}
				
			}

		}
	
		return b;
	}
	
	

	//S = shapelet/subseqies. T = full series;  assumes normalized S and T
	public double dist_n(double[] S, double[] T, double bsf, int step) {
		double min_dist = 0;
		double total = 0;
		int num_windows_evaluated = 0; //stats

		for (int i = 0; i < T.length; i = i + step) {
			total = 0; 			
			num_windows_evaluated++;
			for (int j = 0; j < S.length; j++) {
				total += ((T[j] - S[j]) * (T[j] - S[j]));
				if (min_dist < total) {
					min_dist = total;
				}
				if (total > bsf) {
					min_dist = total;
					break;
				}
			}
		}
		
		
		return min_dist;	// note; not returning sqr root
	}
	
	
//	//S = shapelet/subseqies. T = full series
//	public double dist(double[] S, double[] T) {
//		return dist(S, T, Double.POSITIVE_INFINITY, 1);
//	}
//	
//	//S = shapelet/subseqies. T = full series
//	public double dist(double[] S, double[] T, double bsf, int step_size) {
//		double min_dist = 0;
//		double total = 0;
//		int num_windows_evaluated = 0; //stats
//
//		for (int i = 0; i < T.length - S.length; i = i + step_size) {
//			total = 0; 
//			num_windows_evaluated++;
//			for (int j = 0; j < S.length; j++) {
//				total += ((T[i+j] - S[j]) * (T[i+j] - S[j]));
//				if (min_dist < total) {
//					min_dist = total;
//				}
//				if (total > bsf) {
//					min_dist = total;
//					break;
//				}
//			}
//		}
//		
//		
//		return min_dist;
//	}
	
	
	//returns sorted indices of array items -- only ascending implemented -- inefficient due to int and Integer conversions
	public int[] sort_indices(double[] array, boolean ascending) {
		
		final Integer[] idx = new Integer[array.length];

		for (int i = 0; i < array.length; i++) {
			idx[i] = i;
		}
		
		Arrays.sort(idx, new Comparator<Integer>() {
		    @Override public int compare(final Integer o1, final Integer o2) {
		        return - Double.compare(array[o1], array[o2]); //reverse
		    }
		});
		
		return Arrays.stream(idx).mapToInt(Integer::intValue).toArray();
	}
	
}
