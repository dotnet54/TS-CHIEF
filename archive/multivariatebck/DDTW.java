package trees.splitters.ee.multivariate;

import core.exceptions.NotImplementedException;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class DDTW extends DTW {
	double[] deriv1, deriv2;
	protected int lpnorm = 2;

	public DDTW() {
	}
	
	public synchronized double distance(double[][] series1, double[][] series2, double bsf, int windowSize,
										boolean dependentDimensions, int[] dimensionsToUse) {
		if (dependentDimensions){
			double[][] s1Deriv = new double[series1.length][];
			double[][] s2Deriv = new double[series2.length][];
			for (int dimension : dimensionsToUse) {
				s1Deriv[dimension] = new double[series1[0].length];
				s2Deriv[dimension] = new double[series2[0].length];

				getDeriv(s1Deriv[dimension], series1[dimension]);
				getDeriv(s2Deriv[dimension], series2[dimension]);
			}
			return super.distance_dep(s1Deriv, s2Deriv, bsf, windowSize, dimensionsToUse);
		}else {
			if (deriv1 == null || deriv1.length != series1[0].length) {
				deriv1 = new double[series1[0].length];
			}
			if (deriv2 == null || deriv2.length != series2[0].length) {
				deriv2 = new double[series2[0].length];
			}

			if (lpnorm == 1){
				double total = 0;
				for (int dimension : dimensionsToUse) {
					getDeriv(deriv1,series1[dimension]);
					getDeriv(deriv2, series2[dimension]);
					total += super.distance_indep(deriv1, deriv2, bsf, windowSize);
				}
				return total;
			}else if (lpnorm == 2){
				double total = 0;
				double dist = 0;
				for (int dimension : dimensionsToUse) {
					getDeriv(deriv1,series1[dimension]);
					getDeriv(deriv2, series2[dimension]);
					dist = super.distance_indep(deriv1, deriv2, bsf, windowSize);
					total += dist * dist;
				}
//			return Math.sqrt(total);
				return total;
			}else{
				double total = 0;
				double dist = 0;
				for (int dimension : dimensionsToUse) {
					getDeriv(deriv1,series1[dimension]);
					getDeriv(deriv2, series2[dimension]);
					dist = super.distance_indep(deriv1, deriv2, bsf, windowSize);
					total += Math.pow(dist, lpnorm);
				}
//			return Math.pow(total, (double) 1/lpnorm);
				return total;
			}
		}
	}
	
	protected static final void getDeriv(double[]d,double[] series) {
		for (int i = 1; i < series.length - 1 ; i++) { 
			d[i] = ((series[i] - series[i - 1]) + ((series[i + 1] - series[i - 1]) / 2.0)) / 2.0;
		}
		d[0] = d[1];
		d[d.length - 1] = d[d.length - 2];
	}
	
}
