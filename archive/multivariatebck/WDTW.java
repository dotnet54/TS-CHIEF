package trees.splitters.ee.multivariate;

import core.exceptions.NotImplementedException;
import data.timeseries.Dataset;

import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class WDTW {
	
	private static final double WEIGHT_MAX = 1;
	private double g; // "empirical constant that controls the curvature
			  // (slope) of the function
	private double[] weightVector; // initilised on first distance call
	protected int lpnorm = 2;

	public WDTW() {
	}
	
	public void setG(double g, int length){
		if(this.g!=g || this.weightVector==null){
			this.g = g;
			this.initWeights(length);
		}
		
	}

	private void initWeights(int seriesLength) {
		this.weightVector = new double[seriesLength];
		double halfLength = (double) seriesLength / 2;

		for (int i = 0; i < seriesLength; i++) {
			weightVector[i] = WEIGHT_MAX / (1 + Math.exp(-g * (i - halfLength)));
		}
	}

//	public synchronized double distance(double[] first, double[] second, double bsf, double g) {
//		this.setG(g, first.length);
//
//		double[][] distances = MemorySpaceProvider.getInstance(first.length).getDoubleMatrix();
////		double[][] distances = MemoryManager.getInstance().getDoubleMatrix(0);
//
//		// create empty array
//		if (distances == null || distances.length < first.length || distances[0].length < second.length) {
//			distances = new double[first.length][second.length];
//		}
//
//		// first value
//		distances[0][0] = this.weightVector[0] * (first[0] - second[0]) * (first[0] - second[0]);
//
//		// top row
//		for (int i = 1; i < second.length; i++) {
//			distances[0][i] = distances[0][i - 1] + this.weightVector[i] * (first[0] - second[i]) * (first[0] - second[i]); // edited
//																	// by
//																	// Jay
//		}
//
//		// first column
//		for (int i = 1; i < first.length; i++) {
//			distances[i][0] = distances[i - 1][0] + this.weightVector[i] * (first[i] - second[0]) * (first[i] - second[0]); // edited
//																	// by
//																	// Jay
//		}
//
//		// warp rest
//		double minDistance;
//		for (int i = 1; i < first.length; i++) {
//
//			for (int j = 1; j < second.length; j++) {
//				// calculate distances
//				minDistance = DistanceTools.Min3(distances[i][j - 1], distances[i - 1][j], distances[i - 1][j - 1]);
//				distances[i][j] = minDistance + this.weightVector[Math.abs(i - j)] * (first[i] - second[j]) * (first[i] - second[j]); // edited
//																		      // by
//																		      // Jay
//
//				//
//				// if(minDistance > cutOffValue &&
//				// this.isEarlyAbandon){
//				// this.distances[i][j] = Double.MAX_VALUE;
//				// }else{
//				// this.distances[i][j] =
//				// minDistance+this.weightVector[Math.abs(i-j)]
//				// *(first[i]-second[j])*(first[i]-second[j]);
//				// //edited by Jay
//				// overflow = false;
//				// }
//			}
//
//		}
//
//		double res = distances[first.length - 1][second.length - 1];
//		MemorySpaceProvider.getInstance().returnDoubleMatrix(distances);
//		return res;
//	}

	public synchronized double distance(double[][] series1, double[][] series2,double bsf, double g,
										boolean dependentDimensions, int[] dimensionsToUse) {
		if (dependentDimensions){
			return distance_dep(series1, series2, bsf, g, dimensionsToUse);
		}else if (lpnorm == 1){
			double total = 0;
			for (int dimension : dimensionsToUse) {
				total += distance_indep(series1[dimension], series2[dimension], bsf, g);
			}
			return total;
		}else if (lpnorm == 2){
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, g);
				total += dist * dist;
			}
//			return Math.sqrt(total);
			return total;
		}else{
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, g);
				total += Math.pow(dist, lpnorm);
			}
//			return Math.pow(total, (double) 1/lpnorm);
			return total;
		}
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

	public final double squaredDistanceVector(double[][] A, double[][] B, int i, int j,int[] dimensionsToUse) {
		double total = Double.NaN;
		for (int dimension : dimensionsToUse) {
			total += ((A[dimension][i] - B[dimension][j])
					* (A[dimension][i] - B[dimension][j]));
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

	//fast WDTW implemented by Geoff Webb
	public synchronized double distance_indep(double[] series1, double[] series2, double bsf, double g) {
		this.setG(g, Math.max(series1.length,series2.length));

		double[] prevRow = new double[series2.length];
		double[] curRow = new double[series2.length];
		double second0 = series2[0];
		double thisDiff;
		double prevVal = 0.0;
		
		// put the series1 row into prevRow to save swapping before moving to the series2 row
		
		{	double first0 = series1[0];
		
			// series1 value
			thisDiff = first0 - second0;
			prevVal = prevRow[0] = this.weightVector[0] * thisDiff * thisDiff;
	
			// top row
			for (int j = 1; j < series2.length; j++) {
				thisDiff = first0 - series2[j];
				prevVal = prevRow[j] = prevVal + this.weightVector[j] * thisDiff * thisDiff;
			}
		}
		
		double minDistance;
		double firsti = series1[1];
		
		// series2 row is a special case because path can't go through prevRow[j]
		thisDiff = firsti - second0;
		prevVal = curRow[0] = prevRow[0] + this.weightVector[1] * thisDiff * thisDiff;
		
		for (int j = 1; j < series2.length; j++) {
			// calculate distances
			minDistance = Math.min(prevVal, prevRow[j - 1]);
			thisDiff = firsti - series2[j];
			prevVal = curRow[j] = minDistance + this.weightVector[j-1] * thisDiff * thisDiff;
		}

		// warp rest
		for (int i = 2; i < series1.length; i++) {
			// make the old current row into the current previous row and set current row to use the old prev row
			double [] tmp = curRow;
			curRow = prevRow;
			prevRow = tmp;
			firsti = series1[i];
			
			thisDiff = firsti - second0;
			prevVal = curRow[0] = prevRow[0] + this.weightVector[i] * thisDiff * thisDiff;
			
			for (int j = 1; j < series2.length; j++) {
				// calculate distances
				minDistance = min(prevVal, prevRow[j], prevRow[j - 1]);
				thisDiff = firsti - series2[j];
				prevVal = curRow[j] = minDistance + this.weightVector[Math.abs(i - j)] * thisDiff * thisDiff;
			}

		}

		double res = prevVal;
		return res;
	}


	public synchronized double distance_dep(double[][] series1, double[][] series2, double bsf, double g, int[] dimensionsToUse) {
		this.setG(g, Math.max(series1.length,series2.length));

		double[] prevRow = new double[series2[dimensionsToUse[0]].length];
		double[] curRow = new double[series2[dimensionsToUse[0]].length];
		double prevVal;

		// put the series1 row into prevRow to save swapping before moving to the series2 row
		double[] first0 = getTimePoint(series1, 0, dimensionsToUse);

		// series1 value
		prevVal = prevRow[0] = this.weightVector[0] * squaredDistanceVector(first0, series2, 0, dimensionsToUse);

		// top row
		for (int j = 1; j < series2.length; j++) {
			prevVal = prevRow[j] = prevVal + this.weightVector[j] * squaredDistanceVector(first0, series2, j, dimensionsToUse);
		}

		double minDistance;
		double[] firsti = getTimePoint(series1, 1, dimensionsToUse);

		// series2 row is a special case because path can't go through prevRow[j]
		prevVal = curRow[0] = prevRow[0] + this.weightVector[1] * squaredDistanceVector(firsti, series2, 0, dimensionsToUse);

		for (int j = 1; j < series2.length; j++) {
			// calculate distances
			minDistance = Math.min(prevVal, prevRow[j - 1]);
			prevVal = curRow[j] = minDistance + this.weightVector[j-1] * squaredDistanceVector(firsti, series2, j, dimensionsToUse);
		}

		// warp rest
		for (int i = 2; i < series1.length; i++) {
			// make the old current row into the current previous row and set current row to use the old prev row
			double [] tmp = curRow;
			curRow = prevRow;
			prevRow = tmp;
			firsti = getTimePoint(series1, i, dimensionsToUse);

			prevVal = curRow[0] = prevRow[0] + this.weightVector[i] * squaredDistanceVector(firsti, series2, 0, dimensionsToUse);

			for (int j = 1; j < series2.length; j++) {
				// calculate distances
				minDistance = min(prevVal, prevRow[j], prevRow[j - 1]);
				prevVal = curRow[j] = minDistance + this.weightVector[Math.abs(i - j)] * squaredDistanceVector(firsti, series2, j, dimensionsToUse);
			}

		}

		double res = prevVal;
		return res;
	}

//	public synchronized double distance_dep(double[][] series1, double[][] series2, double bsf, double g, int[] dimensionsToUse) {
//		int length1 = series1[0].length;
//		int length2 = series2[0].length;
//		this.setG(g, Math.max(length1, length2));
//
//		double[] prevRow = new double[length2];
//		double[] currRow = new double[length2];
//		double prevVal;
//
//		//value at matrix(0,0) -- top left value
//		prevVal = prevRow[0] = this.weightVector[0] * squaredDistanceVector(series1, series2,
//				0, 0, dimensionsToUse);
//
//		// values at matrix(0, j) -- top row
//		for (int j = 1; j < series2.length; j++) {
//			prevVal = prevRow[j] = prevVal + this.weightVector[j] * squaredDistanceVector(series1, series2,
//					0, j, dimensionsToUse);
//		}
//
//
//		double minDistance;
//		double series1i = series1[1];
//
//		thisDiff = firsti - second0;
//		prevVal = curRow[0] = prevRow[0] + this.weightVector[1] + squaredDistanceVector(series1, series2,
//				0, j, dimensionsToUse);
//
//		for (int j = 1; j < series2.length; j++) {
//			// calculate distances
//			minDistance = Math.min(prevVal, prevRow[j - 1]);
//			prevVal = currRow[j] = minDistance + this.weightVector[j-1] * squaredDistanceVector(series1, series2,
//					0, j, dimensionsToUse);
//		}
//
//		// warp rest
//		for (int i = 2; i < series1.length; i++) {
//			// make the old current row into the current previous row and set current row to use the old prev row
//			double [] tmp = currRow;
//			currRow = prevRow;
//			prevRow = tmp;
//			series1i = series1[i];
//
//			thisDiff = series1i - second0;
//			prevVal = currRow[0] = prevRow[0] + this.weightVector[i] * thisDiff * thisDiff;
//
//			for (int j = 1; j < series2.length; j++) {
//				// calculate distances
//				minDistance = min(prevVal, prevRow[j], prevRow[j - 1]);
//				thisDiff = series1i - series2[j];
//				prevVal = currRow[j] = minDistance + this.weightVector[Math.abs(i - j)] * thisDiff * thisDiff;
//			}
//
//		}
//
//		double res = prevVal;
//		return res;
//	}


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
	
	public double get_random_g(Dataset d, Random r) {
		return r.nextDouble();
	}
	
}
