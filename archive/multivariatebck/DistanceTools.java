package trees.splitters.ee.multivariate;

import data.timeseries.Dataset;

import java.util.List;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class DistanceTools {
	public static int[] getInclusive10(int min, int max){
	        int[] output = new int[10];

	        double diff = (double)(max-min)/9;
	        double[] doubleOut = new double[10];
	        doubleOut[0] = min;
	        output[0] = min;
	        for(int i = 1; i < 9; i++){
	            doubleOut[i] = doubleOut[i-1]+diff;
	            output[i] = (int)Math.round(doubleOut[i]);
	        }
	        output[9] = max; // to make sure max isn't omitted due to double imprecision
	        return output;
	    }

	    public static double[] getInclusive10(double min, double max){
	        double[] output = new double[10];
	        double diff = (max-min)/9;
	        output[0] = min;
	        for(int i = 1; i < 9; i++){
	            output[i] = output[i-1]+diff;
	        }
	        output[9] = max;

	        return output;
	    }
	    public static int sim(double a, double b, double epsilon) {
			return (Math.abs(a - b) <= epsilon) ? 1 : 0;
		}

		public static double stdv_p(Dataset train) {

			double sumx = 0;
			double sumx2 = 0;
			double[] ins2array;
			for (int i = 0; i < train.size(); i++) {
				ins2array = train.getSeries(i).data()[0]; //TODO using dimension 0 only
				for (int j = 0; j < ins2array.length; j++) {
										// avoid
										// classVal
					sumx += ins2array[j];
					sumx2 += ins2array[j] * ins2array[j];
				}
			}
			int n = train.size() * (train.length());
			double mean = sumx / n;
			return Math.sqrt(sumx2 / (n) - mean * mean);  //TODO check this?? possible issue here 

		}	    
	    
		public static double stdv_p(List<double[]> train) {

			double sumx = 0;
			double sumx2 = 0;
			double[] ins2array;
			for (int i = 0; i < train.size(); i++) {
				ins2array = train.get(i);
				for (int j = 0; j < ins2array.length; j++) {
										// avoid
										// classVal
					sumx += ins2array[j];
					sumx2 += ins2array[j] * ins2array[j];
				}
			}
			int n = train.size() * (train.get(0).length);
			double mean = sumx / n;
			return Math.sqrt(sumx2 / (n) - mean * mean); //TODO check this?? possible issue here 

		}
		
//		public static <C,D> double stdv_p(ArrayDataset<C,D> train) {
//
//			double sumx = 0;
//			double sumx2 = 0;
//			double[] ins2array;
//			for (int i = 0; i < train.size(); i++) {
//				ins2array = train.getPrimitiveDoubleArray(i);
//				for (int j = 0; j < ins2array.length - 1; j++) {// -1 to
//										// avoid
//										// classVal
//					sumx += ins2array[j];
//					sumx2 += ins2array[j] * ins2array[j];
//				}
//			}
//			int n = train.size() * (train.get(0).size());
//			double mean = sumx / n;
//			return Math.sqrt(sumx2 / (n) - mean * mean);
//
//		}		
		
		public final static double Min3(final double a, final double b, final double c) {
			return (a <= b) ? ((a <= c) ? a : c) : (b <= c) ? b : c;
		}

		public static int ArgMin3(final double a, final double b, final double c) {
			return (a <= b) ? ((a <= c) ? 0 : 2) : (b <= c) ? 1 : 2;
		}
		
		public static double sum(final double... tab) {
			double res = 0.0;
			for (double d : tab)
				res += d;
			return res;
		}
		
		public static double max(final double... tab) {
			double max = Double.NEGATIVE_INFINITY;
			for (double d : tab){
				if(max<d){
					max = d;
				}
			}
			return max;
		}
		
		public static double min(final double... tab) {
			double min = Double.POSITIVE_INFINITY;
			for (double d : tab){
				if(d<min){
					min = d;
				}
			}
			return min;
		}

	public static double[] getTimePoint(double[][] series, int index){
		double[] timePoint = new double[series.length];
		for (int dimension = 0; dimension < series.length; dimension++) {
			timePoint[dimension] = series[dimension][index];
		}
		return timePoint;
	}

	public static double[] getTimePoint(double[][] series, int index, double[] allocatedTimePoint){
		for (int dimension = 0; dimension < series.length; dimension++) {
			allocatedTimePoint[dimension] = series[dimension][index];
		}
		return allocatedTimePoint;
	}

	public static final double squaredDistanceScalar(double A, double B) {
		return (A - B) * (A - B);
	}

	public static final double squaredDistanceVector(double[] A, double[] B, int[] dimensionsToUse) {
		double total = 0;
		for (int dimension : dimensionsToUse) {
			total += ((A[dimension] - B[dimension]) * (A[dimension] - B[dimension]));
		}
		return total;
	}

	public static final double squaredDistanceVector(double[] A, double[][] B, int j, int[] dimensionsToUse) {
		double total = 0;
		for (int dimension : dimensionsToUse) {
			total += ((A[dimension] - B[dimension][j]) * (A[dimension] - B[dimension][j]));
		}
		return total;
	}

	public static final double squaredDistanceVector(double[][] A, double[][] B, int i, int j, int[] dimensionsToUse) {
		double total = 0;
		for (int dimension : dimensionsToUse) {
			total += ((A[dimension][i] - B[dimension][j]) * (A[dimension][i] - B[dimension][j]));
		}
		return total;
	}

	public static final double squaredDistanceVector(double[][] A, double scalar, int i, int[] dimensionsToUse) {
		double total = 0;
		for (int dimension : dimensionsToUse) {
			total += ((A[dimension][i] - scalar) * (A[dimension][i] - scalar));
		}
		return total;
	}

	public static double lpNorm(double[] vector, int norm, boolean getSumOnly){
		if (norm == 1){
			double total = 0;
			for (double element : vector) {
				// assumes all that elements are positive, strictly this is
				// total += Math.abs(element)
				total += element;
			}
			return total;
		}else if (norm == 2){
			double total = 0;
			for (double element : vector) {
				total += element * element;
			}
			if (getSumOnly){
				return total;
			}else{
				return Math.sqrt(total);
			}
		}else{
			double total = 0;
			for (double element : vector) {
				// assumes all that elements are positive, strictly this is
				// total += Math.abs(Math.pow(element, norm))
				total += Math.pow(element, norm);
			}
			if (getSumOnly) {
				return total;
			}else{
				return Math.pow(total, (double) 1/norm);
			}
		}
	}

	public static double lpNorm(double[] vector, int norm, boolean getSumOnly, int[] dimensionsToUse){
		if (norm == 1){
			double total = 0;
			for (int dimension : dimensionsToUse) {
				// assumes all that elements are positive, strictly this is
				// total += Math.abs(element)
				total += vector[dimension];
			}
			return total;
		}else if (norm == 2){
			double total = 0;
			for (int dimension : dimensionsToUse) {
				total += vector[dimension] * vector[dimension];
			}
			if (getSumOnly){
				return total;
			}else{
				return Math.sqrt(total);
			}
		}else{
			double total = 0;
			for (int dimension : dimensionsToUse) {
				// assumes all that elements are positive, strictly this is
				// total += Math.abs(Math.pow(element, norm))
				total += Math.pow(vector[dimension], norm);
			}
			if (getSumOnly) {
				return total;
			}else{
				return Math.pow(total, (double) 1/norm);
			}
		}
	}

	public static double vecDifference(double[] vectorA, double[] vectorB, int norm, boolean getSumOnly){
		int minLength = Math.min(vectorA.length, vectorB.length);
		if (norm == 1){
			double total = 0;
			for (int i = 0; i < minLength; i++) {
				total += Math.abs(vectorA[i] - vectorB[i]);
			}
			return total;
		}else if (norm == 2){
			double total = 0;
			double diff;
			for (int i = 0; i < minLength; i++) {
				diff = vectorA[i] - vectorB[i];
				total += diff * diff;
			}
			if (getSumOnly){
				return total;
			}else{
				return Math.sqrt(total);
			}
		}else{
			double total = 0;
			for (int i = 0; i < minLength; i++) {
				total += Math.pow(vectorA[i] - vectorB[i], norm);
			}
			if (getSumOnly) {
				return total;
			}else{
				return Math.pow(total, (double) 1/norm);
			}
		}
	}

	public static double vecDifference(double[][] series1, double[][] series2, int norm,
									   boolean getSumOnly, int[] dimensionsToUse){
		int minLength = Math.min(series1[0].length, series2[0].length);

		if (norm == 1){
			double total = 0;
			for (int i = 0; i < minLength; i++) {
				for (int dimension : dimensionsToUse) {
					total += Math.abs(series1[dimension][i] - series2[dimension][i]);
				}
			}
			return total;
		}else if (norm == 2){
			double total = 0;
			double diff;
			for (int i = 0; i < minLength; i++) {
				for (int dimension : dimensionsToUse) {
					diff = series1[dimension][i] - series2[dimension][i];
					total += diff * diff;
				}
			}
			if (getSumOnly){
				return total;
			}else{
				return Math.sqrt(total);
			}
		}else{
			double total = 0;
			for (int i = 0; i < minLength; i++) {
				for (int dimension : dimensionsToUse) {
					total += Math.pow(series1[dimension][i] - series2[dimension][i], norm);
				}
			}
			if (getSumOnly) {
				return total;
			}else{
				return Math.pow(total, (double) 1/norm);
			}
		}
	}


	public static double vecDifference(double[][] series1, double[][] series2,
									   int i, int j,
									   int norm, boolean getSumOnly, int[] dimensionsToUse){
		int minLength = Math.min(series1[0].length, series2[0].length);

		if (norm == 1){
			double total = 0;
			for (int dimension : dimensionsToUse) {
				total += Math.abs(series1[dimension][i] - series2[dimension][j]);
			}
			return total;
		}else if (norm == 2){
			double total = 0;
			double diff;
			for (int dimension : dimensionsToUse) {
				diff = series1[dimension][i] - series2[dimension][j];
				total += diff * diff;
			}
			if (getSumOnly){
				return total;
			}else{
				return Math.sqrt(total);
			}
		}else{
			double total = 0;
			for (int dimension : dimensionsToUse) {
				total += Math.pow(series1[dimension][i] - series2[dimension][j], norm);
			}
			if (getSumOnly) {
				return total;
			}else{
				return Math.pow(total, (double) 1/norm);
			}
		}
	}

}
