package trees.splitters.ee.multivariate;

import core.exceptions.NotImplementedException;
import data.timeseries.Dataset;
import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class LCSS {
	protected int lpnorm = 2;

	public LCSS() {

	}

	public synchronized double distance(double[][] series1, double[][] series2, double bsf, int windowSize, double epsilon,
										boolean dependentDimensions, int[] dimensionsToUse) {
		if (dependentDimensions){
			return distance_dep(series1, series2, bsf, windowSize, epsilon, dimensionsToUse);
		}else if (lpnorm == 1){
			double total = 0;
			for (int dimension : dimensionsToUse) {
				total += distance_indep(series1[dimension], series2[dimension], bsf, windowSize, epsilon);
			}
			return total;
		}else if (lpnorm == 2){
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, windowSize, epsilon);
				total += dist * dist;
			}
//			return Math.sqrt(total);
			return total;
		}else{
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, windowSize, epsilon);
				total += Math.pow(dist, lpnorm);
			}
//			return Math.pow(total, (double) 1/lpnorm);
			return total;
		}
	}

	public synchronized double distance_indep(double[] series1, double[] series2, double bsf, int windowSize, double epsilon) {
		if (windowSize == -1) {
			windowSize = series1.length;
		}

		int length1 = series1.length;
		int length2 = series2.length;

		int maxLength = Math.max(length1, length2);
		int minLength = Math.min(length1, length2);

		int [][]matrix = MemorySpaceProvider.getInstance(maxLength).getIntMatrix();
//		int[][] matrix = MemoryManager.getInstance().getIntMatrix(0);

		int i, j;
	
		matrix[0][0] = isSimilar(series1[0], series2[0], epsilon);
		for (i = 1; i < Math.min(length1, 1 + windowSize); i++) {
			matrix[i][0] = (isSimilar(series1[i], series2[0], epsilon)==1)?isSimilar(series1[i], series2[0], epsilon):matrix[i-1][0];
		}

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			matrix[0][j] = (isSimilar(series1[0], series2[j], epsilon)==1?isSimilar(series1[0], series2[j], epsilon):matrix[0][j-1]);
		}
		
		if (j < length2)
			matrix[0][j] = Integer.MIN_VALUE;


		for (i = 1; i < length1; i++) {
			int jStart = (i - windowSize < 1) ? 1 : i - windowSize;
			int jStop = (i + windowSize + 1 > length2) ? length2 : i + windowSize + 1;
			
			if (i-windowSize-1>=0)
				matrix[i][i-windowSize-1] = Integer.MIN_VALUE;
			for (j = jStart; j < jStop; j++) {
				if (isSimilar(series1[i], series2[j], epsilon) == 1) {
					matrix[i][j] = matrix[i - 1][j - 1] + 1;
				} else {
					matrix[i][j] = max(matrix[i][j - 1], matrix[i - 1][j], matrix[i - 1][j - 1]);
				}
			}
			if (jStop < length2)
				matrix[i][jStop] = Integer.MIN_VALUE;
		}
		
		double res = 1.0 - 1.0 * matrix[length1 - 1][length2 - 1] / minLength;
		MemorySpaceProvider.getInstance().returnIntMatrix(matrix);
		return res;

	}

	public synchronized double distance_dep(double[][] series1, double[][] series2, double bsf, int windowSize, double epsilon, int[] dimensionsToUse) {
		if (windowSize == -1) {
			windowSize = series1[0].length;
		}

		int length1 = series1[0].length;
		int length2 = series2[0].length;

		int maxLength = Math.max(length1, length2);
		int minLength = Math.min(length1, length2);

		int [][]matrix = MemorySpaceProvider.getInstance(maxLength).getIntMatrix();
//		int[][] matrix = MemoryManager.getInstance().getIntMatrix(0);

		int i, j, similarity;
		double epsilonSquared = epsilon * epsilon;

		matrix[0][0] = isSimilar(series1, series2,0, 0, epsilonSquared, dimensionsToUse);
		for (i = 1; i < Math.min(length1, 1 + windowSize); i++) {
			similarity = isSimilar(series1, series2, i, 0, epsilonSquared, dimensionsToUse);
			matrix[i][0] = similarity == 1 ? similarity : matrix[i-1][0];
		}

		for (j = 1; j < Math.min(length2, 1 + windowSize); j ++) {
			similarity = isSimilar(series1, series2, 0, j, epsilonSquared, dimensionsToUse);
			matrix[0][j] = similarity == 1 ? similarity : matrix[0][j-1];
		}

		if (j < length2)
			matrix[0][j] = Integer.MIN_VALUE;

		for (i = 1; i < length1; i++) {
			int jStart = (i - windowSize < 1) ? 1 : i - windowSize;
			int jStop = (i + windowSize + 1 > length2) ? length2 : i + windowSize + 1;

			if (i-windowSize-1>=0)
				matrix[i][i-windowSize-1] = Integer.MIN_VALUE;
			for (j = jStart; j < jStop; j++) {
				if (isSimilar(series1, series2, i, j, epsilonSquared, dimensionsToUse) == 1) {
					matrix[i][j] = matrix[i - 1][j - 1] + 1;
				} else {
					matrix[i][j] = max(matrix[i][j - 1], matrix[i - 1][j], matrix[i - 1][j - 1]);
				}
			}
			if (jStop < length2)
				matrix[i][jStop] = Integer.MIN_VALUE;
		}

		double res = 1.0 - 1.0 * matrix[length1 - 1][length2 - 1] / minLength;
		MemorySpaceProvider.getInstance().returnIntMatrix(matrix);
		return res;

	}


	public int isSimilar(double a, double b, double epsilon) {
		return (Math.abs(a - b) <= epsilon) ? 1 : 0;
	}

	public int isSimilar(double[][] A, double[][] B, int i, int j, double epsilonSquared, int[] dimensionsToUse) {
		double total = 0;
		for (int dimension : dimensionsToUse) {
			total += ((A[dimension][i] - B[dimension][j]) * (A[dimension][i] - B[dimension][j]));
		}
		// TODO error -- will most likely be larger than epsilon always, divide by noDims??
//		total =  Math.sqrt(total); //
//		total /= dimensionsToUse.length; //TODO unsure
//		return (total <= epsilon) ? 1 : 0;
		return total <= dimensionsToUse.length * epsilonSquared ? 1: 0;
	}

	public final int max(int left, int up, int diagonal) {
		if (left > up) {
			if (left > diagonal) {
				// left > up and left > diagonal
				return left;
			} else {
				// diagonal >= left > up
				return diagonal;
			}
		} else {
			if (up > diagonal) {
				// up > left and up > diagonal
				return up;
			} else {
				// diagonal >= up > left
				return diagonal;
			}
		}
	}
	
	public int get_random_window(Dataset d, Random r) {
//		int x = (d.length() +1) / 4;
		return r.nextInt((d.length() +1) / 4); //TODO
	} 	
	
	public double get_random_epsilon(Dataset d, Random r) {
		double stdTrain = DistanceTools.stdv_p(d);
		double stdFloor = stdTrain * 0.2;
		double e = r.nextDouble()*(stdTrain-stdFloor)+stdFloor;
		return e;
	} 
	
}
