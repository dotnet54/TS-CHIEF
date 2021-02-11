package test.measures.imp.shifaz;

import static java.lang.Math.sqrt;

import java.util.Random;

import datasets.TSDataset;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class DTW {
	
	public DTW() {
		
	}
	
	public synchronized double distance(double[] series1, double[] series2,double bsf, int windowSize) {
		if (windowSize == -1) {
			windowSize = series1.length;
		}

		int length1 = series1.length;
		int length2 = series2.length;

		int maxLength = Math.max(length1, length2);
		
		double[][] matrix = MemorySpaceProvider.getInstance(maxLength).getDoubleMatrix();
//		double[][] matrix = MemoryManager.getInstance().getDoubleMatrix(0);
//		for (int i = 0; i < matrix.length; i++) {
//			for (int j = 0; j < matrix.length; j++) {
//				matrix[i][j] = Double.POSITIVE_INFINITY;
//			}
//		}
		
		if (matrix == null || matrix.length < maxLength) {
			matrix = new double[maxLength][maxLength];
		}

		int i, j;
		matrix[0][0] = squaredDistance(series1[0], series2[0]);
		for (i = 1; i < Math.min(length1, 1 + windowSize); i++) {
		    matrix[i][0] = matrix[i - 1][0] + squaredDistance(series1[i], series2[0]);
		}
		if (i < length1)
		    matrix[i][0] = Double.POSITIVE_INFINITY;

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			matrix[0][j] = matrix[0][j - 1] + squaredDistance(series1[0], series2[j]);
		}
		if (j < length2)
			matrix[0][j] = Double.POSITIVE_INFINITY;

		for (i = 1; i < length1; i++) {
			int jStart = (i - windowSize < 1) ? 1 : i - windowSize;
			int jStop = (i + windowSize + 1 > length2) ? length2 : i + windowSize + 1;

			for (j = jStart; j < jStop; j++) {
				matrix[i][j] = min(matrix[i - 1][j - 1], matrix[i][j - 1], matrix[i - 1][j])
								+ squaredDistance(series1[i], series2[j]);
			}
		    if (jStop < length2)
		        matrix[i][jStop] = Double.POSITIVE_INFINITY;
		    if (i < length1-1)
		        matrix[i+1][jStart] = Double.POSITIVE_INFINITY;
		}
		
		double res = sqrt(matrix[length1 - 1][length2 - 1]);
		MemorySpaceProvider.getInstance().returnDoubleMatrix(matrix);
		return res;
	}

	public static final double min(double A, double B, double C) {
		if (A < B) {
			if (A < C) {
				// A < B and A < C
				return A;
			} else {
				// C < A < B
				return C;
			}
		} else {
			if (B < C) {
				// B < A and B < C
				return B;
			} else {
				// C < B < A
				return C;
			}
		}
	}
	
	public static final double squaredDistance(double A, double B) {
		double x = A - B;
		return x * x;
	}	

	public int get_random_window(TSDataset d, Random r) {
		return r.nextInt((d.length() +1) / 4);
	}

}
