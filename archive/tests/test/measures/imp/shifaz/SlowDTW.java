package test.measures.imp.shifaz;

import static java.lang.Math.sqrt;

import java.util.Random;

import datasets.TSDataset;

public class SlowDTW {
	
//	this is a naive implementation of DTW to use in unit testing, to compare against other implementations for correctness
	
	public SlowDTW() {
		
	}
	
	public synchronized double distance(double[] series1, double[] series2, double bsf, int windowSize) {
		if (windowSize == -1) {
			windowSize = series1.length;
		}

		int length1 = series1.length;
		int length2 = series2.length;
		int maxLength = Math.max(length1, length2);
		double[][] matrix = new double[maxLength][maxLength];
		double diff = 0;
		double total = 0;
		int i, j;
		
		diff = series1[0] - series2[0];
		matrix[0][0] = diff * diff;
		
		
		for (i = 1; i < Math.min(length1, 1 + windowSize); i++) {
			matrix[i][0] = matrix[i - 1][0] + squaredDistance(series1[i], series2[0]);
		}

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			matrix[0][j] = matrix[0][j - 1] + squaredDistance(series1[0], series2[j]);
		}
		
		for (i = 1; i < length1; i++) {
			int jStart = (i - windowSize < 1) ? 1 : i - windowSize;
			int jStop = (i + windowSize + 1 > length2) ? length2 : i + windowSize + 1;

			matrix[i][jStart - 1] = Double.POSITIVE_INFINITY;
			for (j = jStart; j < jStop; j++) {
				matrix[i][j] = min(matrix[i - 1][j - 1], matrix[i][j - 1], matrix[i - 1][j])
								+ squaredDistance(series1[i], series2[j]);
			}
			if (jStop < length2)
				matrix[i][jStop] = Double.POSITIVE_INFINITY;
		}
		
//		double res = sqrt(matrix[length1 - 1][length2 - 1]);
		return matrix[length1 - 1][length2 - 1];
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


	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
