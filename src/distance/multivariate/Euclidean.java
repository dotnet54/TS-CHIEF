package distance.multivariate;

import data.timeseries.Dataset;

import java.util.Random;

public class Euclidean extends MultivarSimMeasure {

	protected boolean useSquaredEuclidean;

	public Euclidean(boolean dependentDimensions, int[] dimensionsToUse) {
		super(dependentDimensions, dimensionsToUse);
		this.useSquaredEuclidean = true;
	}

	public Euclidean(boolean dependentDimensions, int[] dimensionsToUse, boolean useSquaredEuclidean) {
		super(dependentDimensions, dimensionsToUse);
		this.useSquaredEuclidean = useSquaredEuclidean;
	}

	//TODO NOTE total <= bsf, if bsf <= 0, < operator will cause problems when early abandoning since total = 0 initially
	@Override
	public double distanceIndep(double[][] series1, double[][] series2, int dimension, double bsf) {
		double[] vector1 = series1[dimension];
		double[] vector2 = series2[dimension];
		double total = 0;
		int minLength = Math.min(vector1.length, vector2.length);
		for (int i = 0; i < minLength & total <= bsf; i++){
			// L2 inside distance, as in dtw, we use parameter pForIndependentDims only to combine dimensions and not inside the measure
			total += (vector1[i] - vector2[i]) * (vector1[i] - vector2[i]);
			// for any Lp-Norm -- DEV ONLY, we always use l2 inside the distance
//			total += Math.pow(Math.abs(vector1[i] - vector2[i]), pForIndependentDims);
		}
		if (useSquaredEuclidean){
			return total;
		}else{
			return Math.sqrt(total);
		}
	}

	// this version assumes that the input arrays store data as series[numDimensions][length]
	@Override
	public double distanceDep(double[][] series1, double[][] series2, double bsf){
		double total = 0;
		int minLength = Math.min(series1[0].length, series2[0].length);
		for (int i = 0; i < minLength & total <= bsf; i++){
			for (int dimension : dimensionsToUse) {
				// L2 inside distance, as in dtw, we use parameter pForDependentDims only to combine dimensions and not inside the measure
				total += (series1[dimension][i] - series2[dimension][i]) * (series1[dimension][i] - series2[dimension][i]);
				// for any Lp-Norm -- DEV ONLY, we always use l2 inside the distance
//				total += Math.pow(Math.abs(series1[dimension][i] - series2[dimension][i]), pForDependentDims);
			}
			// the sqrt of total at the end of summing each dimension cancels out because each point needs to be squared
			// skipped here: sqrt(total)^2
		}
		if (useSquaredEuclidean){
			return total;
		}else{
			return Math.sqrt(total);
		}
	}

	@Override
	public double distanceNaive(double[] series1, double[] series2){
		double total = 0;
		int minLength = Math.min(series1.length, series2.length);
		for (int i = 0; i < minLength; i++){
			total += (series1[i] - series2[i]) * (series1[i] - series2[i]);
		}
		return Math.sqrt(total);
	}

	@Override
	public void setRandomParams(Dataset trainData, Random rand){
		//pass
	}

	@Override
	public void initParamsByID(Dataset trainData, Random rand) {
		//pass
	}

	@Override
	public void setParamsByID(int paramID, int seriesLength, Random rand) {
		//pass
	}

	@Override
	public String toString(){
		return "Euclidean[sqEuc="+useSquaredEuclidean+","+super.toString()+",]";
	}

//	// this version assumes that the input arrays store data as series[numDimensions][length]
//	public double distance_dep_transposed(double[][] s, double[][] t, double bsf, int[] dimensionsToUse){
//		double total = 0;
//		int minLength = Math.min(s[0].length, t[0].length);
//		for (int i = 0; i < minLength & total <= bsf; i++){
//			for (int dimension : dimensionsToUse) {
//				total += (s[i][dimension] - t[i][dimension]) * (s[i][dimension] - t[i][dimension]);
//			}
//		}
////		return Math.sqrt(total);
//		return total;
//	}


	//REMOVE
	// this version assumes that the input arrays store data as series[length][numDimensions]
//	public synchronized double distance_indep_transposed(double[][] s, double[][] t, double bsf, int[] dimensionsToUse){
//		double total = 0;
//		int minLength = Math.min(s.length, t.length);
//		for (int dimension : dimensionsToUse) {
//			for (int i = 0; i < minLength & total <= bsf; i++){
//				total += (s[i][dimension] - t[i][dimension]) * (s[i][dimension] - t[i][dimension]);
//			}
//		}
////		return Math.sqrt(total);
//		return total;
//	}

}
