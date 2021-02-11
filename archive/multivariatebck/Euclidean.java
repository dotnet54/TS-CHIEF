package trees.splitters.ee.multivariate;

public class Euclidean implements MultivariateSimilarityMeasure {

	protected int lpnorm = 2;

	public Euclidean() {
	}

	public synchronized double distance(double[][] series1, double[][] series2, double bsf,
										boolean dependentDimensions, int[] dimensionsToUse){
		if (dependentDimensions){
			return distance_dep(series1, series2, bsf, dimensionsToUse);
		}else if (lpnorm == 1){
			double total = 0;
			for (int dimension : dimensionsToUse) {
				total += distanceIndep(series1[dimension], series2[dimension], bsf);
			}
			return total;
		}else if (lpnorm == 2){
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distanceIndep(series1[dimension], series2[dimension], bsf);
				total += dist * dist;
			}
//			return Math.sqrt(total);
			return total;
		}else{
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distanceIndep(series1[dimension], series2[dimension], bsf);
				total += Math.pow(dist, lpnorm);
			}
//			return Math.pow(total, (double) 1/lpnorm);
			return total;
		}
	}

	//TODO NOTE total <= bsf, if bsf <= 0, < operator will cause problems when early abandoning since total = 0 initially
	public synchronized double distance_indep(double[] s, double[] t, double bsf) {
		double total = 0;
		int minLength = Math.min(s.length, t.length);
		for (int i = 0; i < minLength & total <= bsf; i++){
			total += (s[i] - t[i]) * (s[i] - t[i]);
		}
//		return Math.sqrt(total);
		return total;
	}

	// this version assumes that the input arrays store data as series[numDimensions][length]
	public synchronized double distance_dep(double[][] s, double[][] t, double bsf, int[] dimensionsToUse){
		double total = 0;
		int minLength = Math.min(s[0].length, t[0].length);
		for (int i = 0; i < minLength & total <= bsf; i++){
			for (int dimension : dimensionsToUse) {
				total += (s[dimension][i] - t[dimension][i]) * (s[dimension][i] - t[dimension][i]);
			}
		}
//		return Math.sqrt(total);
		return total;
	}

	// this version assumes that the input arrays store data as series[numDimensions][length]
	public synchronized double distance_dep_transposed(double[][] s, double[][] t, double bsf, int[] dimensionsToUse){
		double total = 0;
		int minLength = Math.min(s[0].length, t[0].length);
		for (int i = 0; i < minLength & total <= bsf; i++){
			for (int dimension : dimensionsToUse) {
				total += (s[i][dimension] - t[i][dimension]) * (s[i][dimension] - t[i][dimension]);
			}
		}
//		return Math.sqrt(total);
		return total;
	}


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
