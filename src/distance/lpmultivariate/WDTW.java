package distance.lpmultivariate;

import data.timeseries.Dataset;

import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (fixes, and improvements for efficiency) the original classes.
 * 
 */

public class WDTW extends LPMultivarSimMeasure {

	public static final String gParam = "g";
	protected static final double WEIGHT_MAX = 1;
	protected double g; // "empirical constant that controls the curvature (slope) of the function
	protected double[] weightVector; // initialised on first distance call

	public WDTW(boolean dependentDimensions, int[] dimensionsToUse, double g) {
		super(dependentDimensions, dimensionsToUse);
		this.g = g;
	}

	public double distance(double[][] series1, double[][] series2, double bsf, double g){
		this.setG(g, series1[0].length);
		return distance(series1, series2, bsf);
	}

	//fast WDTW implemented by Geoff Webb
	@Override
	public double distanceIndep(double[] vector1, double[] vector2, double bsf) {

		double[] prevRow = new double[vector2.length];
		double[] curRow = new double[vector2.length];
		double second0 = vector2[0];
		double diff;
		double prevVal = 0.0;
		this.setG(g, curRow.length);

		
		// put the series1 row into prevRow to save swapping before moving to the series2 row
		
		{	double first0 = vector1[0];
		
			// series1 value
			diff = first0 - second0;
			prevVal = prevRow[0] = this.weightVector[0] * diff * diff;
	
			// top row
			for (int j = 1; j < vector2.length; j++) {
				diff = first0 - vector2[j];
				prevVal = prevRow[j] = prevVal + this.weightVector[j] * diff * diff;
			}
		}
		
		double minDistance;
		double firsti = vector1[1];
		
		// series2 row is a special case because path can't go through prevRow[j]
		diff = firsti - second0;
		prevVal = curRow[0] = prevRow[0] + this.weightVector[1] * diff * diff;
		
		for (int j = 1; j < vector2.length; j++) {
			// calculate distances
			minDistance = Math.min(prevVal, prevRow[j - 1]);
			diff = firsti - vector2[j];
			prevVal = curRow[j] = minDistance + this.weightVector[j-1] * diff * diff;
		}

		// warp rest
		for (int i = 2; i < vector1.length; i++) {
			// make the old current row into the current previous row and set current row to use the old prev row
			double [] tmp = curRow;
			curRow = prevRow;
			prevRow = tmp;
			firsti = vector1[i];
			
			diff = firsti - second0;
			prevVal = curRow[0] = prevRow[0] + this.weightVector[i] * diff * diff;

			for (int j = 1; j < vector2.length; j++) {
				// calculate distances
				minDistance = min(prevVal, prevRow[j], prevRow[j - 1]);
				diff = firsti - vector2[j];
				prevVal = curRow[j] = minDistance + this.weightVector[Math.abs(i - j)] * diff * diff;
			}

		}

		return prevVal;  //check sqrt?
	}

	@Override
	public double distanceDep(double[][] series1, double[][] series2, double bsf){
		if (dimensionsToUse == null){
			throw new RuntimeException("Set dimensionsToUse before calling this method");
		}
		int length1 = series1[dimensionsToUse[0]].length;
		int length2 = series2[dimensionsToUse[0]].length;

		double[] prevRow = new double[length2];
		double[] curRow = new double[length2];
		double prevVal;
		double minDistance;

		this.setG(g, length2);

		// put the series1 row into prevRow to save swapping before moving to the series2 row
		prevVal = prevRow[0] = this.weightVector[0] * squaredDistancePointToPoint(series1, 0, series2, 0);

		// top row
		for (int j = 1; j < length2; j++) {
			prevVal = prevRow[j] = prevVal + this.weightVector[j]
					* squaredDistancePointToPoint(series1, 0, series2, j);
		}

		// series2 row is a special case because path can't go through prevRow[j]
		prevVal = curRow[0] = prevRow[0] + this.weightVector[1]
				* squaredDistancePointToPoint(series1, 1, series2, 0);

		for (int j = 1; j < length2; j++) {
			// calculate distances
			minDistance = Math.min(prevVal, prevRow[j - 1]);
			prevVal = curRow[j] = minDistance + this.weightVector[j-1]
					* squaredDistancePointToPoint(series1, 1, series2, j);
		}

		// warp rest
		for (int i = 2; i < length1; i++) {
			// make the old current row into the current previous row and set current row to use the old prev row
			double [] tmp = curRow;
			curRow = prevRow;
			prevRow = tmp;

			prevVal = curRow[0] = prevRow[0] + this.weightVector[i]
					* squaredDistancePointToPoint(series1, i, series2, 0);

			for (int j = 1; j < length2; j++) {
				// calculate distances
				minDistance = min(prevVal, prevRow[j], prevRow[j - 1]);
				prevVal = curRow[j] = minDistance + this.weightVector[Math.abs(i - j)]
						* squaredDistancePointToPoint(series1, i, series2, j);
			}

		}

//		double res = prevVal;
		return prevVal;
	}

	private void initWeights(int seriesLength) {
		this.weightVector = new double[seriesLength];
		double halfLength = (double) seriesLength / 2;

		for (int i = 0; i < seriesLength; i++) {
			weightVector[i] = WEIGHT_MAX / (1 + Math.exp(-g * (i - halfLength)));
		}
	}

	@Override
	public void setRandomParams(Dataset trainData, Random rand) {
		setG(getRandomG(trainData, rand), trainData.length());
	}

	public static double getRandomG(Dataset dataset, Random rand) {
		return rand.nextDouble();
	}

	public void setG(double g, int seriesLength){
		if(this.g!=g || this.weightVector==null){
			this.g = g;
			this.initWeights(seriesLength);
		}
	}

	@Override
	public String toString(){
		return "WDTW[,g="+g+","
				+super.toString()+",]";
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

}
