package distance.lpmultivariate;

import data.timeseries.Dataset;

import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (fixes, and improvements for efficiency) the original classes.
 * 
 */

public class DTW extends LPMultivarSimMeasure {
	public static final String windowParam = "w";
	protected int windowSize;
	protected double windowPercentage = -1;

	public DTW(boolean dependentDimensions, int[] dimensionsToUse, int windowSize) {
		super(dependentDimensions, dimensionsToUse);
		this.windowSize = windowSize;
	}

	public double distance(double[][] series1, double[][] series2, double bsf, int windowSize) throws Exception {
		this.setWindowSize(windowSize);
		return distance(series1, series2, bsf);
	}

	//fast DTW implemented by Geoff Webb
	@Override
	public double distanceIndep(double[] vector1, double[] vector2, double bsf) {
		int length1 = vector1.length;
		int length2 = vector2.length;
		int maxLength = Math.max(length1, length2);
		//convert a percentage window size to an integer window size
		if(this.windowPercentage >= 0 && this.windowPercentage <= 1){
			windowSize = (int) Math.ceil(maxLength  * this.windowPercentage);
		}else{
			windowSize = this.windowSize;
		}
		if (windowSize < 0 || windowSize > maxLength) {
			windowSize = maxLength;
		}

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
		double thisSeries1Val = vector1[0];
		double diff;
		
		// initialising the first row - do this in prevRow so as to save swapping rows before next row
		diff = thisSeries1Val - vector2[0];
		prevVal = prevRow[0] = diff * diff;

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			diff = thisSeries1Val - vector2[j];
			prevVal = prevRow[j] = prevVal + diff * diff;
		}

		// the second row is a special case
		if (length1 >= 2){
			thisSeries1Val = vector1[1];
			
			if (windowSize>0){
				diff = thisSeries1Val - vector2[0];
				currentRow[0] = prevRow[0] + diff * diff;
			}
			
			// in this special case, neither matrix[1][0] nor matrix[0][1] can be on the (shortest) minimum path
			diff = thisSeries1Val - vector2[1];
			prevVal = currentRow[1] = prevRow[0] + diff * diff;
			int jStop = (windowSize + 2 > length2) ? length2 : windowSize + 2;
				for (j = 2; j < jStop; j++) {
					// for the second row, matrix[0][j - 1] cannot be on a (shortest) minimum path
					diff = thisSeries1Val - vector2[j];
					prevVal = currentRow[j] = Math.min(prevVal, prevRow[j - 1]) + diff * diff;
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
			
			thisSeries1Val = vector1[i];

			if (i - windowSize < 1) {
				jStart = 1;
				diff = thisSeries1Val -  vector2[0];
				currentRow[0] = prevRow[0] + diff * diff;
			} else {
				jStart = i - windowSize;
			}

			if (windowSize == 0){
				diff = thisSeries1Val -  vector2[jStart];
				prevVal = currentRow[jStart] = prevVal + diff * diff;
			}else if (jStart <= jStop){
				// If jStart is the start of the window, [i][jStart-1] is outside the window.
				// Otherwise jStart-1 must be 0 and the path through [i][0] can never be less than the path directly from [i-1][0]
				diff = thisSeries1Val - vector2[jStart];
				prevVal = currentRow[jStart] = Math.min(prevRow[jStart - 1], prevRow[jStart]) + diff * diff;
				for (j = jStart+1; j < jStop; j++) {
					diff = thisSeries1Val - vector2[j];
					prevVal = currentRow[j] = min(prevVal, prevRow[j], prevRow[j - 1]) + diff * diff;
				}
				
				if (i + windowSize >= length2) {
					// the window overruns the end of the sequence so can have a path through prevRow[jStop]
					diff = thisSeries1Val - vector2[jStop];
					currentRow[jStop] = min(prevVal, prevRow[jStop], prevRow[jStop - 1]) + diff * diff;
				} else {
					diff =  thisSeries1Val - vector2[jStop];
					currentRow[jStop] = Math.min(prevRow[jStop - 1], prevVal) + diff * diff;
				}
			}
		}
		//TODO bug if series 1 length == 1, value is in prevrow instead of current row
//		TODO prevVal
		return currentRow[length2 - 1];
	}

	@Override
	public double distanceDep(double[][] series1, double[][] series2, double bsf){
		if (dimensionsToUse == null){
			throw new RuntimeException("Set dimensionsToUse before calling this method");
		}
		int length1 = series1[dimensionsToUse[0]].length;
		int length2 = series2[dimensionsToUse[0]].length;
		int maxLength = Math.max(length1, length2);

		//convert a percentage window size to an integer window size
		if(this.windowSize > 0 && this.windowSize < 1){
			windowSize = (int) Math.ceil(maxLength  * this.windowSize);
		}else{
			windowSize = (int) this.windowSize;
		}
		if (windowSize < 0 || windowSize > maxLength) {
			windowSize = maxLength;
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

		// initialising the first row - do this in prevRow so as to save swapping rows before next row
		prevValue = prevRow[0] = squaredDistancePointToPoint(series1, 0, series2, 0);

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			prevValue = prevRow[j] = prevValue + squaredDistancePointToPoint(series1, 0, series2, j);
		}

		// the second row is a special case
		if (length1 >= 2){
			if (windowSize>0){
				currRow[0] = prevRow[0] + squaredDistancePointToPoint(series1, 1, series2, 0);
			}

			// in this special case, neither matrix[1][0] nor matrix[0][1] can be on the (shortest) minimum path
			prevValue = currRow[1] = prevRow[0] + squaredDistancePointToPoint(series1, 1, series2, 1);
			int jStop = (windowSize + 2 > length2) ? length2 : windowSize + 2;

			for (j = 2; j < jStop; j++) {
				// for the second row, matrix[0][j - 1] cannot be on a (shortest) minimum path
				prevValue = currRow[j] = Math.min(prevValue, prevRow[j - 1])
						+ squaredDistancePointToPoint(series1, 1, series2, j);;
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

			if (i - windowSize < 1) {
				jStart = 1;
				currRow[0] = prevRow[0] + squaredDistancePointToPoint(series1, i, series2, 0);
			}
			else {
				jStart = i - windowSize;
			}

			if (windowSize == 0){
				prevValue = currRow[jStart] = prevValue + squaredDistancePointToPoint(series1, i, series2, jStart);
			}else if (jStart <= jStop){
				// If jStart is the start of the window, [i][jStart-1] is outside the window.
				// Otherwise jStart-1 must be 0 and the path through [i][0] can never be less than the path directly from [i-1][0]
				prevValue = currRow[jStart] = Math.min(prevRow[jStart - 1], prevRow[jStart])
						+ squaredDistancePointToPoint(series1, i, series2, jStart);
				for (j = jStart+1; j < jStop; j++) {
					prevValue = currRow[j] = min(prevValue, prevRow[j], prevRow[j - 1])
							+ squaredDistancePointToPoint(series1, i, series2, j);
				}

				if (i + windowSize >= length2) {
					// the window overruns the end of the sequence so can have a path through prevRow[jStop]
					currRow[jStop] = min(prevValue, prevRow[jStop], prevRow[jStop - 1])
							+ squaredDistancePointToPoint(series1, i, series2, jStop);
				}
				else {
					currRow[jStop] = Math.min(prevRow[jStop - 1], prevValue)
							+ squaredDistancePointToPoint(series1, i, series2, jStop);
				}
			}
		}

		return currRow[length2 - 1];
	}

	@Override
	public double distanceNaive(double[] series1, double[] series2){
		int length1 = series1.length;
		int length2 = series2.length;
		int window;
		//convert a percentage window size to an integer window size
		if(this.windowSize > 0 && this.windowSize < 1){
			window = (int) Math.ceil(length1  * this.windowSize);
		}else{
			window = (int) this.windowSize;
		}
		if (window < 0 || window > length1) {
			window = length1;
		}

		int i, j, start, stop;
		double[][] matrix = new double[length1][length1];
		matrix[0][0] =  (series1[0] - series2[0]) * (series1[0] - series2[0]); 	// matrix[0,0]

		stop = Math.min(window, length2);
		for (j = 1; j < stop; j++) {
			matrix[0][j] =  (series1[0] - series2[j]) * (series1[0] - series2[j]); // first row
		}
		stop = Math.min(window, length1);
		for (i = 1; i < stop; j++) {
			matrix[i][0] =  (series1[i] - series2[0]) * (series1[i] - series2[0]); //first column
		}

		for (i = 1; i < length1; i++) {
			if (i - window < 1) {
				start = 0;
			} else {
				start = i - window;
			}
			stop = (i + window >= length2) ? length2-1 : i + window;

			matrix[i][start] = Math.min(matrix[i-1][j], matrix[i-1][j-1]) +
					((series1[i] - series2[j]) * (series1[i] - series2[j])); //left edge of the window

			for (j = start + 1; j < stop; j++) {
				matrix[i][j] = min(matrix[i][j-1], matrix[i-1][j], matrix[i][j-1]) +
						((series1[i] - series2[j]) * (series1[i] - series2[j])); //inside the window
			}

			if (i + window >= length2) {
				matrix[i][stop] = min(matrix[i][j-1], matrix[i-1][j], matrix[i][j-1]) +
						((series1[i] - series2[j]) * (series1[i] - series2[j]));   //last column
			}else{
				matrix[i][stop] = Math.min(matrix[i][j-1], matrix[i-1][j-1]) +
						((series1[i] - series2[j]) * (series1[i] - series2[j]));//right edge of the window
			}
		}

		return Math.sqrt(matrix[length1 - 1][length1 - 1]);
//		return matrix[length1 - 1][length1 - 1];
	}

	@Override
	public void setRandomParams(Dataset trainData, Random rand) {
		setWindowSize(getRandomWindowSize(trainData, rand));
	}

	public static int getRandomWindowSize(Dataset dataset, Random rand) {
		return rand.nextInt((dataset.length() +1) / 4);
	}

	public double getWindowSize(){
		return this.windowSize;
	}

	public void setWindowSize(double windowPercentage, int seriesLength){
		int window;
		//convert a percentage window size to an integer window size
		if(windowPercentage >= 0 && windowPercentage < 1){
			window  = (int) Math.ceil(seriesLength  * windowPercentage);
		}else {
			window = seriesLength;
		}
		setWindowSize(window, seriesLength);
	}


	public void setWindowSize(int windowSize, int seriesLength){
		if (windowSize < 0 || windowSize > seriesLength) {
			windowSize = seriesLength;
		}
		this.windowSize = windowSize;
	}

	public void setWindowPercentage(double windowPercentage){
		this.windowPercentage = windowPercentage;
	}

	public void setWindowSize(int windowSize){
		this.windowSize = windowSize;
	}

	@Override
	public String toString(){
		return "DTW[,w="+windowSize+",wp="+windowPercentage+","
				+super.toString()+",]";
	}

}
