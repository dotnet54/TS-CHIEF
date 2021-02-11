package trees.splitters.ee.multivariate;

import core.exceptions.NotImplementedException;
import data.timeseries.Dataset;

import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class ERP {
	protected int lpnorm = 2;
	double[] curr, prev;

	public ERP() {
	}

	public synchronized double distance(double[][] series1, double[][] series2, double bsf, int windowSize, double g,
										boolean dependentDimensions, int[] dimensionsToUse) {
		if (dependentDimensions){
			return distance_dep(series1, series2, bsf, windowSize, g, dimensionsToUse);
		}else if (lpnorm == 1){
			double total = 0;
			for (int dimension : dimensionsToUse) {
				total += distance_indep(series1[dimension], series2[dimension], bsf, windowSize, g);
			}
			return total;
		}else if (lpnorm == 2){
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, windowSize, g);
				total += dist * dist;
			}
//			return Math.sqrt(total);
			return total;
		}else{
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, windowSize, g);
				total += Math.pow(dist, lpnorm);
			}
//			return Math.pow(total, (double) 1/lpnorm);
			return total;
		}
	}

	public synchronized double distance_indep(double[] series1, double[] series2,
											  double bsf, int windowSize, double g) {
		int length1 = series1.length;
//		int length2 = series2.length;

		if (curr == null || curr.length < length1) {
			curr = new double[length1];
			prev = new double[length1];
		} else {
			// FPH: init to 0 just in case, didn't check if
			// important
			//TODO shifaz same cost as allocating new array so check if this is needed
			for (int i = 0; i < curr.length; i++) {
				curr[i] = 0.0;
				prev[i] = 0.0;
			}
		}

		// g parameter for local usage
		for (int i = 0; i < length1; i++) {

			//swap
			double[] temp = prev;
			prev = curr;
			curr = temp;

			int jStart = i - (windowSize + 1);
			if (jStart < 0) {
				jStart = 0;
			}
			int jStop = i + (windowSize + 1);
			if (jStop > (length1 - 1)) {
				jStop = (length1 - 1);
			}

			for (int j = jStart; j <= jStop; j++) {
				if (Math.abs(i - j) <= windowSize) {
					//Math.sqrt() removed
					final double dist1 = (series1[i] - g) * (series1[i] - g);
					final double dist2 = (g - series1[j]) * (g - series1[j]);
					final double dist12 = (series1[i] - series1[j]) * (series1[i] - series1[j]);

					if ((i + j) != 0) {
						if ((i == 0) || ((j != 0) && (((prev[j - 1] + dist12) > (curr[j - 1] + dist2))
										&& ((curr[j - 1] + dist2) < (prev[j] + dist1))))) {
							// del
							curr[j] = curr[j - 1] + dist2;
						} else if ((j == 0) || ((i != 0) && (((prev[j - 1] + dist12) > (prev[j] + dist1))
										&& ((prev[j] + dist1) < (curr[j - 1] + dist2))))) {
							// ins
							curr[j] = prev[j] + dist1;
						} else {
							// match
							curr[j] = prev[j - 1] + dist12;
						}
					} else {
						curr[j] = 0;
					}

				} else {
					curr[j] = Double.POSITIVE_INFINITY; // outside band
				}
			}
		}

//		return Math.sqrt(curr[length1 - 1])
		return curr[length1 - 1];
	}

	public synchronized double distance_dep(double[][] series1, double[][] series2, double bsf, int windowSize,
											double g, int[] dimensionsToUse) {
		int length1 = series1[0].length;

		if (curr == null || curr.length < length1) {
			curr = new double[length1];
			prev = new double[length1];
		} else {
			// FPH: init to 0 just in case, didn't check if
			// important
			//TODO shifaz same cost as allocating new array so check if this is needed
			for (int i = 0; i < curr.length; i++) {
				curr[i] = 0.0;
				prev[i] = 0.0;
			}
		}

		// g parameter for local usage
		for (int i = 0; i < length1; i++) {

			//swap
			double[] temp = prev;
			prev = curr;
			curr = temp;

			int jStart = i - (windowSize + 1);
			if (jStart < 0) {
				jStart = 0;
			}
			int jStop = i + (windowSize + 1);
			if (jStop > (length1 - 1)) {
				jStop = (length1 - 1);
			}

			for (int j = jStart; j <= jStop; j++) {
				if (Math.abs(i - j) <= windowSize) {
					//Math.sqrt() removed
					final double dist1 = DistanceTools.squaredDistanceVector(series1, g, i, dimensionsToUse);
					final double dist2 = DistanceTools.squaredDistanceVector(series2, g, j, dimensionsToUse);
					final double dist12 =  DistanceTools.squaredDistanceVector(series1, series2, i, j, dimensionsToUse);

					if ((i + j) != 0) {
						if ((i == 0) || ((j != 0) && (((prev[j - 1] + dist12) > (curr[j - 1] + dist2))
								&& ((curr[j - 1] + dist2) < (prev[j] + dist1))))) {
							// del
							curr[j] = curr[j - 1] + dist2;
						} else if ((j == 0) || ((i != 0) && (((prev[j - 1] + dist12) > (prev[j] + dist1))
								&& ((prev[j] + dist1) < (curr[j - 1] + dist2))))) {
							// ins
							curr[j] = prev[j] + dist1;
						} else {
							// match
							curr[j] = prev[j - 1] + dist12;
						}
					} else {
						curr[j] = 0;
					}
				} else {
					curr[j] = Double.POSITIVE_INFINITY; // outside band
				}
			}
		}

//		return Math.sqrt(curr[length1 - 1])
		return curr[length1 - 1];
	}


	public int get_random_window(Dataset d, Random r) {
//		int x = (d.length() +1) / 4;
		int w = r.nextInt(d.length()/ 4+1);
		return w;
	} 	
	
	public double get_random_g(Dataset d, Random r) {
		double stdv = DistanceTools.stdv_p(d);
		double g = r.nextDouble()*.8*stdv+0.2*stdv; //[0.2*stdv,stdv]
		return g;
	}

	//shifaz backup 17-3-2020
//	public synchronized double distance_indep(double[] first, double[] second, double bsf, int windowSize, double gValue) {
//		// base case - we're assuming class val is last. If this is
//		// true, this method is fine,
//		// if not, we'll default to the DTW class
//
//		int m = first.length;
//		int n = second.length;
//
//		if (curr == null || curr.length < m) {
//			curr = new double[m];
//			prev = new double[m];
//		} else {
//			// FPH: init to 0 just in case, didn't check if
//			// important
//			for (int i = 0; i < curr.length; i++) {
//				curr[i] = 0.0;
//				prev[i] = 0.0;
//			}
//		}
//
//		// size of edit distance band
//		// bandsize is the maximum allowed distance to the diagonal
//		// int band = (int) Math.ceil(v2.getDimensionality() *
//		// bandSize);
////		int band = (int) Math.ceil(m * bandSize);
//		int band = windowSize;
//
//		// g parameter for local usage
//		for (int i = 0; i < m; i++) {
//			// Swap current and prev arrays. We'll just overwrite
//			// the new curr.
//			{
//				double[] temp = prev;
//				prev = curr;
//				curr = temp;
//			}
//			int l = i - (band + 1);
//			if (l < 0) {
//				l = 0;
//			}
//			int r = i + (band + 1);
//			if (r > (m - 1)) {
//				r = (m - 1);
//			}
//
//			for (int j = l; j <= r; j++) {
//				if (Math.abs(i - j) <= band) {
//					// compute squared distance of feature
//					// vectors
//					double val1 = first[i];
//					double val2 = gValue;
//					double diff = (val1 - val2);
////					final double d1 = Math.sqrt(diff * diff);
//					final double d1 = diff;//FPH simplificaiton
//
//					val1 = gValue;
//					val2 = second[j];
//					diff = (val1 - val2);
////					final double d2 = Math.sqrt(diff * diff);
//					final double d2 = diff;
//
//					val1 = first[i];
//					val2 = second[j];
//					diff = (val1 - val2);
////					final double d12 = Math.sqrt(diff * diff);
//					final double d12 = diff;
//
//					final double dist1 = d1 * d1;
//					final double dist2 = d2 * d2;
//					final double dist12 = d12 * d12;
//
//					final double cost;
//
//					if ((i + j) != 0) {
//						if ((i == 0) || ((j != 0) && (((prev[j - 1] + dist12) > (curr[j - 1] + dist2))
//								&& ((curr[j - 1] + dist2) < (prev[j] + dist1))))) {
//							// del
//							cost = curr[j - 1] + dist2;
//						} else if ((j == 0) || ((i != 0) && (((prev[j - 1] + dist12) > (prev[j] + dist1))
//								&& ((prev[j] + dist1) < (curr[j - 1] + dist2))))) {
//							// ins
//							cost = prev[j] + dist1;
//						} else {
//							// match
//							cost = prev[j - 1] + dist12;
//						}
//					} else {
//						cost = 0;
//					}
//
//					curr[j] = cost;
//					// steps[i][j] = step;
//				} else {
//					curr[j] = Double.POSITIVE_INFINITY; // outside
//					// band
//				}
//			}
//		}
//
//		return Math.sqrt(curr[m - 1]);	//TODO do we need sqrt here
//	}

}
