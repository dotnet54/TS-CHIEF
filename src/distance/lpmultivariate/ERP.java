package distance.lpmultivariate;

import data.timeseries.Dataset;

import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (fixes, and improvements for efficiency) the original classes.
 * 
 */

public class ERP extends LPMultivarSimMeasure {
	public static final String windowParam = "w";
	public static final String gapParam = "g";
//	protected double[] currentRow, prevRow; //not thread safe
	protected int windowSize;
	protected double windowPercentage = -1;
	protected double g;

	public ERP(boolean dependentDimensions, int[] dimensionsToUse, int windowSize, double g) {
		super(dependentDimensions, dimensionsToUse);
		this.windowSize = windowSize;
		this.g = g;
	}

	public double distance(double[][] series1, double[][] series2, double bsf, int windowSize, double g) throws Exception {
		this.setWindowSize(windowSize);
		this.setG(g);
		return distance(series1, series2, bsf);
	}

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

		//thread safe version
		double[] currentRow = new double[maxLength];
		double[] prevRow = new double[maxLength];
//		if (prevRow == null || prevRow.length < maxLength) {
//			prevRow = new double[maxLength];
//		}
//		if (currentRow == null || currentRow.length < maxLength) {
//			currentRow = new double[maxLength];
//		}

		int i, j, jStart, jStop;
		double tmp1g, tmp2g, tmp12; //temp storage
		double prevValue; //prev computed value
		double vector1CurrentVal = vector1[0]; //store to reduce look ups for commonly referred values

		// initialising the first row - do this in prevRow so as to save swapping rows before next row -- Geoff
		tmp1g = vector1CurrentVal - g;
		tmp2g = vector2[0] - g;
		tmp12 = vector1CurrentVal - vector2[0];
		prevValue = currentRow[0] = min(tmp1g * tmp1g, tmp2g * tmp2g, tmp12 * tmp12); //fill matrix[0,0]

		jStop = Math.min(length2, 1 + windowSize);
		for (j = 1; j < jStop; j++) {
			tmp2g = (vector2[j] - g);
			prevValue = currentRow[j] = prevValue + (tmp2g * tmp2g); //fill row 0
		}

		for (i = 1; i < length1; i++) {
			// the old currentRow becomes this prevRow and so the currentRow needs to use the old prevRow
			double[] tmp = prevRow;
			prevRow = currentRow;
			currentRow = tmp;

			vector1CurrentVal = vector1[i];
			jStop = (i + windowSize >= length2) ? length2-1 : i + windowSize;

			if (i - windowSize < 1) {
				tmp1g = vector1CurrentVal - g;
				currentRow[0] = prevRow[0] + (tmp1g * tmp1g);	//fill column 0
				jStart = 1;
			}else {
				jStart = i - windowSize;
			}

			if (windowSize == 0){
				tmp12 = vector1CurrentVal -  vector2[jStart];
				prevValue = currentRow[jStart] = prevValue + tmp12 * tmp12;
			}else if (jStart <= jStop){
				// If jStart is the start of the window, [i][jStart-1] is outside the window.
				// Otherwise jStart-1 must be 0 and the path through [i][0] can never be less than the path directly from [i-1][0]

				tmp2g = vector2[jStart] - g;
				tmp12 = vector1CurrentVal - vector2[jStart];
				//left edge of the window
				prevValue = currentRow[jStart] = Math.min(prevRow[jStart] + tmp2g * tmp2g, prevRow[jStart - 1] + tmp12 * tmp12);

				//inside the window
				tmp1g = vector1CurrentVal - g;
				tmp1g = tmp1g * tmp1g;
				for (j = jStart+1; j < jStop; j++) {
					tmp2g = vector2[j] - g;
					tmp12 = vector1CurrentVal - vector2[j];
					prevValue = currentRow[j] = min(prevValue + tmp1g, prevRow[j] + tmp2g * tmp2g, prevRow[j - 1] + tmp12 * tmp12);
				}

				//right edge of the window
				tmp2g = vector2[jStop] - g;
				tmp12 = vector1CurrentVal - vector2[jStop];
				if (i + windowSize >= length2) {
					// the window overruns the end of the sequence so can have a path through prevRow[jStop]
					currentRow[jStop] = min(prevValue + tmp1g, prevRow[j] + tmp2g * tmp2g, prevRow[j - 1] + tmp12 * tmp12);
				}else {
					currentRow[jStop] = Math.min(prevValue + tmp1g, prevRow[j - 1] + tmp12 * tmp12);
				}
			}
		}
		return currentRow[length1 - 1];
	}

	@Override
	public double distanceDep(double[][] series1, double[][] series2, double bsf){
		int length1 = series1[dimensionsToUse[0]].length;
		int length2 = series2[dimensionsToUse[0]].length;
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

		//thread safe version
		double[] currentRow = new double[maxLength];
		double[] prevRow = new double[maxLength];
//		if (prevRow == null || prevRow.length < maxLength) {
//			prevRow = new double[maxLength];
//		}
//		if (currentRow == null || currentRow.length < maxLength) {
//			currentRow = new double[maxLength];
//		}

		int i, j, jStart, jStop;
		double tmp1g, tmp2g, tmp12; //temp storage
		double prevValue; //prev computed value
//		double vector1CurrentVal = vector1[0]; //store to reduce look ups for commonly referred values

		// initialising the first row - do this in prevRow so as to save swapping rows before next row -- Geoff
		prevValue = currentRow[0] = min(
				squaredDistancePointToScalar(series1, 0, g),
				squaredDistancePointToScalar(series2, 0, g),
				squaredDistancePointToPoint(series1,0, series2, 0)
		); //fill matrix[0,0]

		jStop = Math.min(length2, 1 + windowSize);
		for (j = 1; j < jStop; j++) {
			tmp2g = squaredDistancePointToScalar(series2, j, g);
			prevValue = currentRow[j] = prevValue + tmp2g; //fill row 0
		}

		for (i = 1; i < length1; i++) {
			// the old currentRow becomes this prevRow and so the currentRow needs to use the old prevRow
			double[] tmp = prevRow;
			prevRow = currentRow;
			currentRow = tmp;

			jStop = (i + windowSize >= length2) ? length2-1 : i + windowSize;
			if (i - windowSize < 1) {
				currentRow[0] = prevRow[0] + squaredDistancePointToScalar(series1, i, g);	//fill column 0
				jStart = 1;
			}else {
				jStart = i - windowSize;
			}

			if (windowSize == 0){
				prevValue = currentRow[jStart] = prevValue + squaredDistancePointToPoint(series1, i, series2, jStart);
			}else if (jStart <= jStop){
				// If jStart is the start of the window, [i][jStart-1] is outside the window.
				// Otherwise jStart-1 must be 0 and the path through [i][0] can never be less than the path directly from [i-1][0]

				tmp2g = squaredDistancePointToScalar(series2, jStart, g);
				tmp12 = squaredDistancePointToPoint(series1, i, series2, jStart);
				//left edge of the window
				prevValue = currentRow[jStart] = Math.min(prevRow[jStart] + tmp2g, prevRow[jStart - 1] + tmp12);

				//inside the window
				tmp1g = squaredDistancePointToScalar(series1, i, g);
				for (j = jStart+1; j < jStop; j++) {
					tmp2g = squaredDistancePointToScalar(series2, j, g);
					tmp12 = squaredDistancePointToPoint(series1, i, series2, j);
					prevValue = currentRow[j] = min(prevValue + tmp1g, prevRow[j] + tmp2g, prevRow[j - 1] + tmp12);
				}

				//right edge of the window
				tmp2g = squaredDistancePointToScalar(series2, jStop, g);
				tmp12 = squaredDistancePointToPoint(series1, i, series2, jStop);
				if (i + windowSize >= length2) {
					// the window overruns the end of the sequence so can have a path through prevRow[jStop]
					currentRow[jStop] = min(prevValue + tmp1g, prevRow[j] + tmp2g, prevRow[j - 1] + tmp12);
				}else {
					currentRow[jStop] = Math.min(prevValue + tmp1g, prevRow[j - 1] + tmp12);
				}
			}
		}
		return currentRow[length1 - 1];
	}

	@Override
	public void setRandomParams(Dataset trainData, Random rand){
		setWindowSize(getRandomWindowSize(trainData, rand));
		setG(getRandomG(trainData, rand));
	}

	public static int getRandomWindowSize(Dataset dataset, Random rand) {
//		int x = (d.length() +1) / 4;
		return rand.nextInt((dataset.length()) / 4+1); //CHECK  20%?
	}
	
	public static double getRandomG(Dataset dataset, Random rand) {
//		double stdv = DistanceTools.stdv_p(dataset);
		double stdv = dataset.getStdv();
		return rand.nextDouble()*.8*stdv+0.2*stdv; //[0.2*stdv,stdv]
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(double windowPercentage, int seriesLength){
		int window;
		//convert a percentage window size to an integer window size
		if(windowPercentage >= 0 && windowPercentage <= 1){
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

	public double getG() {
		return g;
	}

	public void setG(double g) {
		this.g = g;
	}

	@Override
	public String toString(){
		return "ERP[,w="+windowSize+",wp="+windowPercentage+",g="+g+","
				+super.toString()+",]";
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
//		return Math.sqrt(curr[m - 1]);
//	}

}
