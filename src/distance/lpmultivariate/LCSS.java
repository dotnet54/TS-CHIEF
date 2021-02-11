package distance.lpmultivariate;

import data.timeseries.Dataset;

import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (fixes, and improvements for efficiency) the original classes.
 * 
 */

public class LCSS extends LPMultivarSimMeasure {
	public static final String windowParam = "w";
	public static final String epsilonParam = "e";
	protected int windowSize;
	protected double windowPercentage = -1;
	protected double epsilon;

	public LCSS(boolean dependentDimensions, int[] dimensionsToUse, int windowSize, double epsilon) {
		super(dependentDimensions, dimensionsToUse);
		this.windowSize = windowSize;
		this.epsilon = epsilon;
	}

	public double distance(double[][] series1, double[][] series2, double bsf, int windowSize, double epsilon){
		this.setWindowSize(windowSize);
		this.setEpsilon(epsilon);
		return distance(series1, series2, bsf);
	}

	@Override
	public double distanceIndep(double[] vector1, double[] vector2, double bsf) {
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
	
		matrix[0][0] = isSimilar(vector1[0], vector2[0], epsilon);
		for (i = 1; i < Math.min(length1, 1 + windowSize); i++) {
			matrix[i][0] = (isSimilar(vector1[i], vector2[0], epsilon)==1)?isSimilar(vector1[i], vector2[0], epsilon):matrix[i-1][0];
		}

		for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
			matrix[0][j] = (isSimilar(vector1[0], vector2[j], epsilon)==1?isSimilar(vector1[0], vector2[j], epsilon):matrix[0][j-1]);
		}
		
		if (j < length2)
			matrix[0][j] = Integer.MIN_VALUE;


		for (i = 1; i < length1; i++) {
			int jStart = (i - windowSize < 1) ? 1 : i - windowSize;
			int jStop = (i + windowSize + 1 > length2) ? length2 : i + windowSize + 1;
			
			if (i-windowSize-1>=0)
				matrix[i][i-windowSize-1] = Integer.MIN_VALUE;
			for (j = jStart; j < jStop; j++) {
				if (isSimilar(vector1[i], vector2[j], epsilon) == 1) {
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
		if (dimensionsToUse == null){
			throw new RuntimeException("Set dimensionsToUse before calling this method");
		}
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

	@Override
	public void setRandomParams(Dataset trainData, Random rand){
		setWindowSize(getRandomWindowSize(trainData, rand));
		setEpsilon(getRandomEpsilon(trainData, rand));
	}

	public static int getRandomWindowSize(Dataset dataset, Random rand) {
//		int x = (d.length() +1) / 4;
		return rand.nextInt((dataset.length() +1) / 4); //TODO 25%?
	} 	
	
	public static double getRandomEpsilon(Dataset dataset, Random rand) {
//		double stdTrain = DistanceTools.stdv_p(dataset);
		double stdTrain = dataset.getStdv();
		double stdFloor = stdTrain * 0.2;
		return rand.nextDouble()*(stdTrain-stdFloor)+stdFloor;
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

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	@Override
	public String toString(){
		return "LCSS[,w="+windowSize+",wp="+windowPercentage+",e="+epsilon+","
				+super.toString()+",]";
	}

}
