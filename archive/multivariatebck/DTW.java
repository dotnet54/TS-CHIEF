package trees.splitters.ee.multivariate;

import core.exceptions.NotImplementedException;
import data.timeseries.Dataset;

import java.util.Random;

import static java.lang.Math.sqrt;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class DTW {
	protected int lpnorm = 2;

	public DTW() {
		
	}

	public double distance(double[][] series1, double[][] series2,double bsf, int windowSize,
											 boolean dependentDimensions, int[] dimensionsToUse) {
		if (dependentDimensions){
			return distance_dep(series1, series2, bsf, windowSize, dimensionsToUse);
		}else if (lpnorm == 1){
			double total = 0;
			for (int dimension : dimensionsToUse) {
				total += distance_indep(series1[dimension], series2[dimension], bsf, windowSize);
			}
			return total;
		}else if (lpnorm == 2){
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, windowSize);
				total += dist * dist;
			}
//			return Math.sqrt(total);
			return total;
		}else{
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, windowSize);
				total += Math.pow(dist, lpnorm);
			}
//			return Math.pow(total, (double) 1/lpnorm);
			return total;
		}
	}
	
	//fast DTW implemented by Geoff Webb
	public double distance_indep(double[] series1, double[] series2, double bsf, int windowSize) {
		if (windowSize == -1) {
			windowSize = series1.length;
		}

		int length1 = series1.length;
		int length2 = series2.length;

		int maxLength = Math.max(length1, length2);
		
		double[] prevRow = new double[maxLength];
		double[] currentRow = new double[maxLength];
		
		if (prevRow == null || prevRow.length < maxLength) {
			prevRow = new double[maxLength];
		}
		
		if (currentRow == null || currentRow.length < maxLength) {
			currentRow = new double[maxLength];
		}

		int i, j;
		double prevVal;
		double thisSeries1Val = series1[0];
		
		// initialising the first row - do this in prevRow so as to save swapping rows before next row
		prevVal = prevRow[0] = squaredDistanceScalar(thisSeries1Val, series2[0]);

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			prevVal = prevRow[j] = prevVal + squaredDistanceScalar(thisSeries1Val, series2[j]);
		}

		// the second row is a special case
		if (length1 >= 2){
			thisSeries1Val = series1[1];
			
			if (windowSize>0){
				currentRow[0] = prevRow[0]+ squaredDistanceScalar(thisSeries1Val, series2[0]);
			}
			
			// in this special case, neither matrix[1][0] nor matrix[0][1] can be on the (shortest) minimum path
			prevVal = currentRow[1]=prevRow[0]+ squaredDistanceScalar(thisSeries1Val, series2[1]);
			int jStop = (windowSize + 2 > length2) ? length2 : windowSize + 2;

				for (j = 2; j < jStop; j++) {
					// for the second row, matrix[0][j - 1] cannot be on a (shortest) minimum path
					prevVal = currentRow[j] = Math.min(prevVal, prevRow[j - 1]) + squaredDistanceScalar(thisSeries1Val, series2[j]);
				}
		}
		
		// third and subsequent rows
		for (i = 2; i < length1; i++) {
			int jStart;
			int jStop = (i + windowSize >= length2) ? length2-1 : i + windowSize;
			
			// the old currentRow becomes this prevRow and so the currentRow needs to use the old prevRow
			double[] tmp = prevRow;
			prevRow = currentRow;
			currentRow = tmp;
			
			thisSeries1Val = series1[i];

			if (i - windowSize < 1) {
				jStart = 1;
				currentRow[0] = prevRow[0] + squaredDistanceScalar(thisSeries1Val, series2[0]);
			}
			else {
				jStart = i - windowSize;
			}
			
			if (jStart <= jStop){
				// If jStart is the start of the window, [i][jStart-1] is outside the window.
				// Otherwise jStart-1 must be 0 and the path through [i][0] can never be less than the path directly from [i-1][0]
				prevVal = currentRow[jStart] = Math.min(prevRow[jStart - 1], prevRow[jStart])+ squaredDistanceScalar(thisSeries1Val, series2[jStart]);
				for (j = jStart+1; j < jStop; j++) {
					prevVal = currentRow[j] = min(prevVal, prevRow[j], prevRow[j - 1])
									+ squaredDistanceScalar(thisSeries1Val, series2[j]);
				}
				
				if (i + windowSize >= length2) {
					// the window overruns the end of the sequence so can have a path through prevRow[jStop]
					currentRow[jStop] = min(prevVal, prevRow[jStop], prevRow[jStop - 1]) + squaredDistanceScalar(thisSeries1Val, series2[jStop]);
				}
				else {
					currentRow[jStop] = Math.min(prevRow[jStop - 1], prevVal) + squaredDistanceScalar(thisSeries1Val, series2[jStop]);
				}
			}
		}
		//TODO bug if series 1 length == 1, value is in prevrow instead of current row
//		TODO prevVal
		double res = sqrt(currentRow[length2 - 1]);
		return res;
	}

	public double[] getTimePoint(double[][] series, int index, int[] dimensionsToUse){
		double[] timePoint = new double[series.length];
		for (int dimension : dimensionsToUse) {
			timePoint[dimension] = series[dimension][index];
		}
		return timePoint;
	}

//	public double[] getTimePoint(double[][] series, int index, double[] allocatedTimePoint){
//		for (int dimension = 0; dimension < series.length; dimension++) {
//			allocatedTimePoint[dimension] = series[dimension][index];
//		}
//		return allocatedTimePoint;
//	}


	public double distance_dep(double[][] series1, double[][] series2, double bsf, int windowSize,
											int[] dimensionsToUse) {
		int length1 = series1[dimensionsToUse[0]].length;
		int length2 = series2[dimensionsToUse[0]].length;
		int maxLength = Math.max(length1, length2);

		if (windowSize == -1) {
			windowSize = length1;
		}

		double[] prevRow = new double[maxLength];
		double[] currRow = new double[maxLength];

		if (prevRow == null || prevRow.length < maxLength) {
			prevRow = new double[maxLength];
		}

		if (currRow == null || currRow.length < maxLength) {
			currRow = new double[maxLength];
		}

		int i, j;
		double prevValue;
		double[] curRowPoint0 = getTimePoint(series1, 0, dimensionsToUse);

		// initialising the first row - do this in prevRow so as to save swapping rows before next row
		prevValue = prevRow[0] = squaredDistanceVector(curRowPoint0, series2, 0, dimensionsToUse);

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			prevValue = prevRow[j] = prevValue + squaredDistanceVector(curRowPoint0, series2, j,
					dimensionsToUse);
		}

		// the second row is a special case
		if (length1 >= 2){
			curRowPoint0 = getTimePoint(series1, 1, dimensionsToUse);

			if (windowSize>0){
				currRow[0] = prevRow[0] + squaredDistanceVector(curRowPoint0, series2, 0, dimensionsToUse);
			}

			// in this special case, neither matrix[1][0] nor matrix[0][1] can be on the (shortest) minimum path
			prevValue = currRow[1] = prevRow[0] + squaredDistanceVector(curRowPoint0, series2, 1,
					dimensionsToUse);
			int jStop = (windowSize + 2 > length2) ? length2 : windowSize + 2;

			for (j = 2; j < jStop; j++) {
				// for the second row, matrix[0][j - 1] cannot be on a (shortest) minimum path
				prevValue = currRow[j] = Math.min(prevValue, prevRow[j - 1])
						+ squaredDistanceVector(curRowPoint0, series2, j, dimensionsToUse);
			}
		}

		// third and subsequent rows
		for (i = 2; i < length1; i++) {
			int jStart;
			int jStop = (i + windowSize >= length2) ? length2-1 : i + windowSize;

			// the old currRow becomes this prevRow and so the currRow needs to use the old prevRow
			double[] tmp = prevRow;
			prevRow = currRow;
			currRow = tmp;

			curRowPoint0 = getTimePoint(series1, i, dimensionsToUse);

			if (i - windowSize < 1) {
				jStart = 1;
				currRow[0] = prevRow[0] + squaredDistanceVector(curRowPoint0, series2, 0, dimensionsToUse);
			}
			else {
				jStart = i - windowSize;
			}

			if (jStart <= jStop){
				// If jStart is the start of the window, [i][jStart-1] is outside the window.
				// Otherwise jStart-1 must be 0 and the path through [i][0] can never be less than the path directly from [i-1][0]
				prevValue = currRow[jStart] = Math.min(prevRow[jStart - 1], prevRow[jStart])
						+ squaredDistanceVector(curRowPoint0, series2, jStart, dimensionsToUse);
				for (j = jStart+1; j < jStop; j++) {
					prevValue = currRow[j] = min(prevValue, prevRow[j], prevRow[j - 1])
							+ squaredDistanceVector(curRowPoint0, series2, j, dimensionsToUse);
				}

				if (i + windowSize >= length2) {
					// the window overruns the end of the sequence so can have a path through prevRow[jStop]
					currRow[jStop] = min(prevValue, prevRow[jStop], prevRow[jStop - 1])
							+ squaredDistanceVector(curRowPoint0, series2, jStop, dimensionsToUse);
				}
				else {
					currRow[jStop] = Math.min(prevRow[jStop - 1], prevValue)
							+ squaredDistanceVector(curRowPoint0, series2, jStop, dimensionsToUse);
				}
			}
		}

		double res = sqrt(currRow[length2 - 1]);
		return res;
	}


	public double distance_dep_transposed(double[][] series1, double[][] series2, double bsf, int windowSize,
											int[] dimensionsToUse) {
		int length1 = series1.length;
		int length2 = series2.length;
		int maxLength = Math.max(length1, length2);

		if (windowSize == -1) {
			windowSize = length1;
		}

		double[] prevRow = new double[maxLength];
		double[] currRow = new double[maxLength];

		if (prevRow == null || prevRow.length < maxLength) {
			prevRow = new double[maxLength];
		}

		if (currRow == null || currRow.length < maxLength) {
			currRow = new double[maxLength];
		}

		int i, j;
		double prevValue;
		double[] curRowPoint0 = getTimePoint(series1, 0, dimensionsToUse);

		// initialising the first row - do this in prevRow so as to save swapping rows before next row
		prevValue = prevRow[0] = squaredDistanceVector(curRowPoint0, series2, 0, dimensionsToUse);

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			prevValue = prevRow[j] = prevValue + squaredDistanceVector(curRowPoint0, series2, j,
					dimensionsToUse);
		}

		// the second row is a special case
		if (length1 >= 2){
			curRowPoint0 = getTimePoint(series1, 1, dimensionsToUse);

			if (windowSize>0){
				currRow[0] = prevRow[0] + squaredDistanceVector(curRowPoint0, series2, 0, dimensionsToUse);
			}

			// in this special case, neither matrix[1][0] nor matrix[0][1] can be on the (shortest) minimum path
			prevValue = currRow[1] = prevRow[0] + squaredDistanceVector(curRowPoint0, series2, 1,
					dimensionsToUse);
			int jStop = (windowSize + 2 > length2) ? length2 : windowSize + 2;

			for (j = 2; j < jStop; j++) {
				// for the second row, matrix[0][j - 1] cannot be on a (shortest) minimum path
				prevValue = currRow[j] = Math.min(prevValue, prevRow[j - 1])
						+ squaredDistanceVector(curRowPoint0, series2, j, dimensionsToUse);
			}
		}

		// third and subsequent rows
		for (i = 2; i < length1; i++) {
			int jStart;
			int jStop = (i + windowSize >= length2) ? length2-1 : i + windowSize;

			// the old currRow becomes this prevRow and so the currRow needs to use the old prevRow
			double[] tmp = prevRow;
			prevRow = currRow;
			currRow = tmp;

			curRowPoint0 = getTimePoint(series1, i, dimensionsToUse);

			if (i - windowSize < 1) {
				jStart = 1;
				currRow[0] = prevRow[0] + squaredDistanceVector(curRowPoint0, series2, 0, dimensionsToUse);
			}
			else {
				jStart = i - windowSize;
			}

			if (jStart <= jStop){
				// If jStart is the start of the window, [i][jStart-1] is outside the window.
				// Otherwise jStart-1 must be 0 and the path through [i][0] can never be less than the path directly from [i-1][0]
				prevValue = currRow[jStart] = Math.min(prevRow[jStart - 1], prevRow[jStart])
						+ squaredDistanceVector(curRowPoint0, series2, jStart, dimensionsToUse);
				for (j = jStart+1; j < jStop; j++) {
					prevValue = currRow[j] = min(prevValue, prevRow[j], prevRow[j - 1])
							+ squaredDistanceVector(curRowPoint0, series2, j, dimensionsToUse);
				}

				if (i + windowSize >= length2) {
					// the window overruns the end of the sequence so can have a path through prevRow[jStop]
					currRow[jStop] = min(prevValue, prevRow[jStop], prevRow[jStop - 1])
							+ squaredDistanceVector(curRowPoint0, series2, jStop, dimensionsToUse);
				}
				else {
					currRow[jStop] = Math.min(prevRow[jStop - 1], prevValue)
							+ squaredDistanceVector(curRowPoint0, series2, jStop, dimensionsToUse);
				}
			}
		}

		double res = sqrt(currRow[length2 - 1]);
		return res;
	}

	//if equal choose the diagonal
	public final double min(double left, double up, double diagonal) {
		if (left < up) {
			if (left < diagonal) {
				// left < up and left < diagonal
				return left;
			} else {
				// diagonal <= left < up
				return diagonal;
			}
		} else {
			if (up < diagonal) {
				// up < left and up < diagonal
				return up;
			} else {
				// diagonal <= up < left
				return diagonal;
			}
		}
	}

	public final double squaredDistanceScalar(double A, double B) {
		return (A - B) * (A - B);
	}

	public final double squaredDistanceVector(double[] A, double[] B, int[] dimensionsToUse) {
		double total = 0;
		for (int dimension : dimensionsToUse) {
			total += ((A[dimension] - B[dimension]) * (A[dimension] - B[dimension]));
		}
		return total;
	}

	public final double squaredDistanceVector(double[] A, double[][] B, int j, int[] dimensionsToUse) {
		double total = 0;
		for (int dimension : dimensionsToUse) {
			total += ((A[dimension] - B[dimension][j]) * (A[dimension] - B[dimension][j]));
		}
		return total;
	}

	public final double squaredDistanceVector(double[][] A, double[][] B, int i, int j, int[] dimensionsToUse) {
		double total = 0;
		for (int dimension : dimensionsToUse) {
			total += ((A[dimension][i] - B[dimension][j]) * (A[dimension][i] - B[dimension][j]));
		}
		return total;
	}

	public int get_random_window(Dataset d, Random r) {
		return r.nextInt((d.length() +1) / 4);
	}


	public double dtw_naive(double[] series1, double[] series2, int windowSize) {
		int length1 = series1.length;
		int length2 = series2.length;

		if (windowSize < 0 || windowSize > length1) {
			windowSize = length1;
		}

		int i, j, start, stop;
		double[][] matrix = new double[length1][length1];
		matrix[0][0] =  (series1[0] - series2[0]) * (series1[0] - series2[0]);

		stop = Math.min(windowSize, length2);
		for (j = 1; j < stop; j++) {
			matrix[0][j] =  (series1[0] - series2[j]) * (series1[0] - series2[j]);
		}
		stop = Math.min(windowSize, length1);
		for (i = 1; i < stop; j++) {
			matrix[i][0] =  (series1[i] - series2[0]) * (series1[i] - series2[0]);
		}

		for (i = 1; i < length1; i++) {
			if (i - windowSize < 1) {
				start = 0;
			} else {
				start = i - windowSize;
			}
			stop = (i + windowSize >= length2) ? length2-1 : i + windowSize;

			matrix[i][start] = Math.min(matrix[i-1][j], matrix[i-1][j-1]) +
					((series1[i] - series2[j]) * (series1[i] - series2[j]));

			for (j = start + 1; j < stop; j++) {
				matrix[i][j] = min(matrix[i][j-1], matrix[i-1][j], matrix[i][j-1]) +
						((series1[i] - series2[j]) * (series1[i] - series2[j]));
			}

			if (i + windowSize >= length2) {
				matrix[i][stop] = min(matrix[i][j-1], matrix[i-1][j], matrix[i][j-1]) +
						((series1[i] - series2[j]) * (series1[i] - series2[j]));
			}else{
				matrix[i][stop] = Math.min(matrix[i][j-1], matrix[i-1][j-1]) +
						((series1[i] - series2[j]) * (series1[i] - series2[j]));
			}
		}

//		return Math.sqrt(matrix[length1 - 1][length1 - 1]);
		return matrix[length1 - 1][length1 - 1];
	}


}
