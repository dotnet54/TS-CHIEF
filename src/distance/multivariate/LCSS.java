package distance.multivariate;

import data.io.CsvWriter;
import data.timeseries.Dataset;
import util.Util;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (fixes, and improvements for efficiency) the original classes.
 * 
 */

public class LCSS extends MultivarSimMeasure {
	public static final String windowParam = "w";
	public static final String epsilonParam = "e";
	protected int windowSize;
	protected double windowPercentage = -1;
	protected double epsilon;
	protected double[] epsilonPerDim;

	// TODO each instance of distance function reallocates memory for this -- refactor to reuse the existing arrays where possible
	protected double[] windowCache;
	protected double[] epsilonCache;
	protected double[][] epsilonPerDimCache;

	public LCSS(boolean dependentDimensions, int[] dimensionsToUse, int windowSize, double epsilon, double[] epsilonPerDim) {
		super(dependentDimensions, dimensionsToUse);
		this.windowSize = windowSize;
		this.epsilon = epsilon;
		this.epsilonPerDim = epsilonPerDim;
	}

	public double distance(double[][] series1, double[][] series2, double bsf, int windowSize, double epsilon, double[] epsilonPerDim){
		this.setWindowSizeInt(windowSize);
		this.setEpsilon(epsilon);
		this.setEpsilonPerDim(epsilonPerDim);
		return distance(series1, series2, bsf);
	}

	@Override
	public double distanceIndep(double[][] series1, double[][] series2, int dimension, double bsf) {
		double[] vector1 = series1[dimension];
		double[] vector2 = series2[dimension];
		int length1 = vector1.length;
		int length2 = vector2.length;

		int maxLength = Math.max(length1, length2);
		int minLength = Math.min(length1, length2);

		//convert a percentage window size to an integer window size
		if(this.windowPercentage >= 0 && this.windowPercentage <= 1){
			windowSize = (int) Math.ceil(maxLength  * this.windowPercentage);
		}else{
			windowSize = this.windowSize;
		}
		if (windowSize < 0 || windowSize > maxLength) {
			windowSize = maxLength;
		}

		int [][]matrix = MemorySpaceProvider.getInstance(maxLength).getIntMatrix();
//		int[][] matrix = MemoryManager.getInstance().getIntMatrix(0);

		int i, j;
	
		matrix[0][0] = isSimilar(vector1[0], vector2[0], epsilonPerDim[dimension]);
		for (i = 1; i < Math.min(length1, 1 + windowSize); i++) {
			matrix[i][0] = (isSimilar(vector1[i], vector2[0], epsilonPerDim[dimension])==1)?isSimilar(vector1[i], vector2[0], epsilonPerDim[dimension]):matrix[i-1][0];
		}

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			matrix[0][j] = (isSimilar(vector1[0], vector2[j], epsilonPerDim[dimension])==1?isSimilar(vector1[0], vector2[j], epsilonPerDim[dimension]):matrix[0][j-1]);
		}
		
		if (j < length2)
			matrix[0][j] = Integer.MIN_VALUE;


		for (i = 1; i < length1; i++) {
			int jStart = (i - windowSize < 1) ? 1 : i - windowSize;
			int jStop = (i + windowSize + 1 > length2) ? length2 : i + windowSize + 1;
			
			if (i-windowSize-1>=0)
				matrix[i][i-windowSize-1] = Integer.MIN_VALUE;
			for (j = jStart; j < jStop; j++) {
				if (isSimilar(vector1[i], vector2[j], epsilonPerDim[dimension]) == 1) {
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

	@Override
	public double distanceDep(double[][] series1, double[][] series2, double bsf){
		int length1 = series1[0].length;
		int length2 = series2[0].length;

		int maxLength = Math.max(length1, length2);
		int minLength = Math.min(length1, length2);

		//convert a percentage window size to an integer window size
		if(this.windowPercentage >= 0 && this.windowPercentage <= 1){
			windowSize = (int) Math.ceil(maxLength  * this.windowPercentage);
		}else{
			windowSize = this.windowSize;
		}
		if (windowSize < 0 || windowSize > maxLength) {
			windowSize = maxLength;
		}

		int [][]matrix = MemorySpaceProvider.getInstance(maxLength).getIntMatrix();
//		int[][] matrix = MemoryManager.getInstance().getIntMatrix(0);

		int i, j, similarity;


		matrix[0][0] = isSimilar(series1, series2,0, 0, epsilon);
		for (i = 1; i < Math.min(length1, 1 + windowSize); i++) {
			similarity = isSimilar(series1, series2, i, 0, epsilon);
			matrix[i][0] = similarity == 1 ? similarity : matrix[i-1][0];
		}

		for (j = 1; j < Math.min(length2, 1 + windowSize); j ++) {
			similarity = isSimilar(series1, series2, 0, j, epsilon);
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
				if (isSimilar(series1, series2, i, j, epsilon) == 1) {
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


	// Used by LCSS_I
	public int isSimilar(double a, double b, double epsilon) {
		if(useSquaredDiff) {
			return Math.sqrt((a - b) * (a - b)) <= epsilon ? 1 : 0;
		}else{
			return Math.abs(a - b) <= epsilon ? 1 : 0;
		}
	}

	// Used by LCSS_D
	public int isSimilar(double[][] A, double[][] B, int i, int j, double epsilon) {
		double total = 0;
		if(useSquaredDiff) {
			for (int dimension : dimensionsToUse) {
				total += ((A[dimension][i] - B[dimension][j]) * (A[dimension][i] - B[dimension][j]));
			}
		}else{
			for (int dimension : dimensionsToUse) {
				total += Math.abs((A[dimension][i] - B[dimension][j]));
			}
		}
		if (adjustSquaredDiff){
			total = total / (2 * dimensionsToUse.length);
		}
		if (total < epsilon){
			return 1;
		}else {
			return 0;
		}
	}

	@Override
	public void setRandomParams(Dataset trainData, Random rand){
		setWindowSizeInt(getRandomWindowSize(trainData, rand));
		setEpsilon(getRandomEpsilon(trainData, rand));
		setEpsilonPerDim(getRandomEpsilonPerDim(trainData, rand));
	}

	@Override
	public void initParamsByID(Dataset trainData, Random rand) {
		double stdTrain = trainData.getStdv();
		double[] stdPerDim = trainData.getStdvPerDimension(); // TODO check getStdvPerDimension

		windowCache = Util.linspaceDbl(0, 0.25, 10,true);
		epsilonCache = Util.linspaceDbl(stdTrain * 0.2, stdTrain, 10,true);

		epsilonPerDimCache = new double[stdPerDim.length][];
		for (int dim = 0; dim < epsilonPerDimCache.length; dim++) {
			epsilonPerDimCache[dim] = Util.linspaceDbl(stdPerDim[dim] * 0.2, stdPerDim[dim], 10,true);
		}

	}

	@Override
	public void setParamsByID(int paramID, int seriesLength, Random rand) {
		setWindowSizeDbl(windowCache[paramID / 10] , seriesLength);
		setEpsilon(epsilonCache[paramID % 10]);
		epsilonPerDim = new double[epsilonPerDimCache.length];

		for (int dim = 0; dim < epsilonPerDimCache.length; dim++) {
			epsilonPerDim[dim] = epsilonPerDimCache[dim][paramID % 10] ;
		}
	}

	public static int getRandomWindowSize(Dataset dataset, Random rand) {
		return rand.nextInt(dataset.length() / 4); // w ~ U[0, L/4]
	} 	
	
	public static double getRandomEpsilon(Dataset dataset, Random rand) {
		double stdTrain = dataset.getStdv();
		double stdFloor = stdTrain * 0.2;
		return rand.nextDouble()*(stdTrain-stdFloor)+stdFloor; // e ~ U(std/5, std)
	}

	public static double[] getRandomEpsilonPerDim(Dataset dataset, Random rand) {
		double[] stdPerDim = dataset.getStdvPerDimension();
		double[] e = new double[stdPerDim.length];
		for (int dim = 0; dim < stdPerDim.length; dim++) {
			double stdFloor = stdPerDim[dim] * 0.2;
			e[dim] = rand.nextDouble()*(stdPerDim[dim]-stdFloor)+stdFloor; // e ~ U(std/5, std)
		}
		return e;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public double getWindowSizeDbl() {
		return windowPercentage;
	}

	public void setWindowSizeDbl(double windowPercentage, int seriesLength){
		int window;
		//convert a percentage window size to an integer window size
		// if windowPercentage == 1.0 we can interpret in two ways, either use 100% (w=length) or convert to int and use w=1
		if(0 <= windowPercentage && windowPercentage <= 1){
			this.windowPercentage = windowPercentage;
			window  = (int) Math.ceil(seriesLength  * windowPercentage);
		}else {
			// a window -1 can be used to set a fixed window size = 1, because 1.0 is converted to 100%
			// or use the setWindowSizeInt function
			window = Math.abs(seriesLength);
		}
		setWindowSizeInt(window, seriesLength);
	}

	public void setWindowSizeInt(int windowSize, int seriesLength){
		if (windowSize < 0 || windowSize > seriesLength) {
			windowSize = seriesLength;
		}
		this.windowSize = windowSize;
	}

	public void setWindowSizeInt(int windowSize){
		this.windowSize = windowSize;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public double[] getEpsilonPerDim() {
		return epsilonPerDim;
	}

	public void setEpsilonPerDim(double[] epsilonPerDim) {
		this.epsilonPerDim = epsilonPerDim;
	}

	@Override
	public String toString(){
		String epd = Arrays.stream(epsilonPerDim).boxed().map(String::valueOf).collect(Collectors.joining(","));
		return "LCSS[,w="+windowSize+",wp="+windowPercentage+",e="+epsilon+",ed="+epd+","
				+super.toString()+",]";
	}

}
