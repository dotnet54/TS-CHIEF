package distance.multivariate;

import data.timeseries.Dataset;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (fixes, and improvements for efficiency) the original classes.
 * 
 */

public class DDTW extends DTW {
//	protected double[] deriv1, deriv2; //not thread safe

	public DDTW(boolean dependentDimensions, int[] dimensionsToUse, int windowSize) {
		super(dependentDimensions, dimensionsToUse, windowSize);
		this.useDerivativeData = true;
	}

//	@Override
//	public double distance(double[][] series1, double[][] series2, double bsf){
//		if (dependentDimensions){
//			double[][] s1Deriv = new double[series1.length][];
//			double[][] s2Deriv = new double[series2.length][];
//			for (int dimension : dimensionsToUse) {
//				s1Deriv[dimension] = new double[series1[0].length];
//				s2Deriv[dimension] = new double[series2[0].length];
//
//				getDerivative(s1Deriv[dimension], series1[dimension]);
//				getDerivative(s2Deriv[dimension], series2[dimension]);
//			}
//			return distanceDep(s1Deriv, s2Deriv, bsf);
//		}else {
//			double[] deriv1 = new double[series1[0].length];
//			double[] deriv2 = new double[series1[0].length];
//
////			if (deriv1 == null || deriv1.length != series1[0].length) {
////				deriv1 = new double[series1[0].length];
////			}
////			if (deriv2 == null || deriv2.length != series2[0].length) {
////				deriv2 = new double[series2[0].length];
////			}
//
//			if (lpDistanceOrderForIndependentDims == 1) {
//				double total = 0;
//				for (int dimension : dimensionsToUse) {
//					getDerivative(deriv1, series1[dimension]);
//					getDerivative(deriv2, series2[dimension]);
//					total += distanceIndep(deriv1, deriv2, bsf);
//				}
//				return total;
//			} else if (lpDistanceOrderForIndependentDims == 2) {
//				double total = 0;
//				double dist = 0;
//				for (int dimension : dimensionsToUse) {
//					getDerivative(deriv1, series1[dimension]);
//					getDerivative(deriv2, series2[dimension]);
//					dist = distanceIndep(deriv1, deriv2, bsf);
//					total += dist * dist;
//				}
//				return Math.sqrt(total);
////				return total;
//			} else {
//				double total = 0;
//				double dist = 0;
//				for (int dimension : dimensionsToUse) {
//					getDerivative(deriv1, series1[dimension]);
//					getDerivative(deriv2, series2[dimension]);
//					dist = distanceIndep(deriv1, deriv2, bsf);
//					total += Math.pow(dist, lpDistanceOrderForIndependentDims);
//				}
//				return Math.pow(total, (double) 1 / lpDistanceOrderForIndependentDims);
////				return total;
//			}
//		}
//	}

	@Override
	public String toString(){
		return "DDTW[,w="+windowSize+","
				+super.toString()+",]";
	}
}
