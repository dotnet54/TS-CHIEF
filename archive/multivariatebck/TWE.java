package trees.splitters.ee.multivariate;

import core.exceptions.NotImplementedException;
import data.timeseries.Dataset;

import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class TWE {
	protected int lpnorm = 2;

	public TWE() {
		
	}

	public synchronized double distance(double[][] series1, double[][] series2, double bsf, double nu, double lambda,
										boolean dependentDimensions, int[] dimensionsToUse) {
		if (dependentDimensions){
			return distance_dep(series1, series2, bsf, nu, lambda, dimensionsToUse);
		}else if (lpnorm == 1){
			double total = 0;
			for (int dimension : dimensionsToUse) {
				total += distance_indep(series1[dimension], series2[dimension], bsf, nu, lambda);
			}
			return total;
		}else if (lpnorm == 2){
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, nu, lambda);
				total += dist * dist;
			}
//			return Math.sqrt(total);
			return total;
		}else{
			double total = 0;
			double dist = 0;
			for (int dimension : dimensionsToUse) {
				dist = distance_indep(series1[dimension], series2[dimension], bsf, nu, lambda);
				total += Math.pow(dist, lpnorm);
			}
//			return Math.pow(total, (double) 1/lpnorm);
			return total;
		}
	}

	public synchronized double distance_indep(double[] series1, double[] series2, double bsf, double nu, double lambda) {

		int m = series1.length;
		int n = series2.length;
		int maxLength = Math.max(m, n);
		
		double dist, disti1, distj1;

		int r = series1.length; // this is just m?!
		int c = series2.length; // so is this, but surely it should actually
				   // be n anyway
		
		int i, j;
		/*
		 * allocations in c double **D = (double **)calloc(r+1,
		 * sizeof(double*)); double *Di1 = (double *)calloc(r+1,
		 * sizeof(double)); double *Dj1 = (double *)calloc(c+1,
		 * sizeof(double)); for(i=0; i<=r; i++) { D[i]=(double
		 * *)calloc(c+1, sizeof(double)); }
		 */

		double[][]D = MemorySpaceProvider.getInstance(maxLength+1).getDoubleMatrix();
		double[]Di1 = MemorySpaceProvider.getInstance(maxLength+1).getDoubleArray();
		double[]Dj1 = MemorySpaceProvider.getInstance(maxLength+1).getDoubleArray();
//		double[][] D = MemoryManager.getInstance().getDoubleMatrix(0);
//		double[] Di1 = MemoryManager.getInstance().getDoubleArray(0);
//		double[] Dj1 = MemoryManager.getInstance().getDoubleArray(1);

		// FPH adding initialisation given that using matrices as fields
		Di1[0] = 0.0;
		Dj1[0] = 0.0;
		// local costs initializations
		for (j = 1; j <= c; j++) {
			distj1 = 0;
			if (j > 1) {
				// CHANGE AJB 8/1/16: Only use power of
				// 2 for speed
				distj1 += (series2[j - 2] - series2[j - 1]) * (series2[j - 2] - series2[j - 1]);
				// OLD VERSION
				// distj1+=Math.pow(Math.abs(series2[j-2][k]-series2[j-1][k]),degree);
				// in c:
				// distj1+=pow(fabs(series2[j-2][k]-series2[j-1][k]),degree);
			} else {
				distj1 += series2[j - 1] * series2[j - 1];
			}
			// OLD distj1+=Math.pow(Math.abs(series2[j-1][k]),degree);
			Dj1[j] = (distj1);
		}

		for (i = 1; i <= r; i++) {
			disti1 = 0;
			if (i > 1) {
				disti1 += (series1[i - 2] - series1[i - 1]) * (series1[i - 2] - series1[i - 1]);
			} // OLD
			  // disti1+=Math.pow(Math.abs(series1[i-2][k]-series1[i-1][k]),degree);
			else {
				disti1 += (series1[i - 1]) * (series1[i - 1]);
			}
			// OLD disti1+=Math.pow(Math.abs(series1[i-1][k]),degree);

			Di1[i] = (disti1);

			for (j = 1; j <= c; j++) {
				dist = 0;
				dist += (series1[i - 1] - series2[j - 1]) * (series1[i - 1] - series2[j - 1]);
				// dist+=Math.pow(Math.abs(series1[i-1][k]-series2[j-1][k]),degree);
				if (i > 1 && j > 1) {
					dist += (series1[i - 2] - series2[j - 2]) * (series1[i - 2] - series2[j - 2]);
				}
				// dist+=Math.pow(Math.abs(series1[i-2][k]-series2[j-2][k]),degree);
				D[i][j] = (dist);
			}
		} // for i

		// border of the cost matrix initialization
		D[0][0] = 0;
		for (i = 1; i <= r; i++) {
			D[i][0] = D[i - 1][0] + Di1[i];
		}
		for (j = 1; j <= c; j++) {
			D[0][j] = D[0][j - 1] + Dj1[j];
		}

		double dmin, htrans, dist0;

		for (i = 1; i <= r; i++) {
			for (j = 1; j <= c; j++) {
				htrans = Math.abs(i- j);
				if (j > 1 && i > 1) {
					htrans += Math.abs((i-1) - (j-1));
				}
				dist0 = D[i - 1][j - 1] + nu * htrans + D[i][j];
				dmin = dist0;
				if (i > 1) {
					htrans = 1;
				} else {
					htrans = i;
				}
				dist = Di1[i] + D[i - 1][j] + lambda + nu * htrans;
				if (dmin > dist) {
					dmin = dist;
				}
				if (j > 1) {
					htrans = 1;
				} else {
					htrans = j;
				}
				dist = Dj1[j] + D[i][j - 1] + lambda + nu * htrans;
				if (dmin > dist) {
					dmin = dist;
				}
				D[i][j] = dmin;
			}
		}

		dist = D[r][c];
		MemorySpaceProvider.getInstance().returnDoubleMatrix(D);
		MemorySpaceProvider.getInstance().returnDoubleArray(Di1);
		MemorySpaceProvider.getInstance().returnDoubleArray(Dj1);
		return dist;
	}

	public synchronized double distance_dep(double[][] series1, double[][] series2, double bsf, double nu, double lambda, int[] dimensionsToUse) {

		int m = series1[0].length;
		int n = series2[0].length;
		int maxLength = Math.max(m, n);

		double dist, disti1, distj1;

		int r = series1[0].length; // this is just m?!
		int c = series2[0].length; // so is this, but surely it should actually
		// be n anyway

		int i, j;
		/*
		 * allocations in c double **D = (double **)calloc(r+1,
		 * sizeof(double*)); double *Di1 = (double *)calloc(r+1,
		 * sizeof(double)); double *Dj1 = (double *)calloc(c+1,
		 * sizeof(double)); for(i=0; i<=r; i++) { D[i]=(double
		 * *)calloc(c+1, sizeof(double)); }
		 */

		double[][]D = MemorySpaceProvider.getInstance(maxLength+1).getDoubleMatrix();
		double[]Di1 = MemorySpaceProvider.getInstance(maxLength+1).getDoubleArray();
		double[]Dj1 = MemorySpaceProvider.getInstance(maxLength+1).getDoubleArray();
//		double[][] D = MemoryManager.getInstance().getDoubleMatrix(0);
//		double[] Di1 = MemoryManager.getInstance().getDoubleArray(0);
//		double[] Dj1 = MemoryManager.getInstance().getDoubleArray(1);

		// FPH adding initialisation given that using matrices as fields
		Di1[0] = 0.0;
		Dj1[0] = 0.0;
		// local costs initializations
		for (j = 1; j <= c; j++) {
			distj1 = 0;
			if (j > 1) {
				// CHANGE AJB 8/1/16: Only use power of
				// 2 for speed
				distj1 += DistanceTools.vecDifference(series2, series2, j-2, j-1, 2, true, dimensionsToUse);
//				distj1 += (series2[j - 2] - series2[j - 1]) * (series2[j - 2] - series2[j - 1]);
				// OLD VERSION
				// distj1+=Math.pow(Math.abs(series2[j-2][k]-series2[j-1][k]),degree);
				// in c:
				// distj1+=pow(fabs(series2[j-2][k]-series2[j-1][k]),degree);
			} else {
				distj1 += DistanceTools.vecDifference(series2, series2, j-1, j-1, 2, true, dimensionsToUse);
//				distj1 += series2[j - 1] * series2[j - 1];
			}
			// OLD distj1+=Math.pow(Math.abs(series2[j-1][k]),degree);
			Dj1[j] = (distj1);
		}

		for (i = 1; i <= r; i++) {
			disti1 = 0;
			if (i > 1) {
				disti1 += DistanceTools.vecDifference(series1, series1, i-2, i-1, 2, true, dimensionsToUse);
//				disti1 += (series1[i - 2] - series1[i - 1]) * (series1[i - 2] - series1[i - 1]);
			} // OLD
			// disti1+=Math.pow(Math.abs(series1[i-2][k]-series1[i-1][k]),degree);
			else {
				disti1 += DistanceTools.vecDifference(series1, series1, i-1, i-1, 2, true, dimensionsToUse);
//				disti1 += (series1[i - 1]) * (series1[i - 1]);
			}
			// OLD disti1+=Math.pow(Math.abs(series1[i-1][k]),degree);

			Di1[i] = (disti1);

			for (j = 1; j <= c; j++) {
				dist = 0;
				dist += DistanceTools.vecDifference(series1, series2, i-1, j-1, 2, true, dimensionsToUse);
//				dist += (series1[i - 1] - series2[j - 1]) * (series1[i - 1] - series2[j - 1]);
				// dist+=Math.pow(Math.abs(series1[i-1][k]-series2[j-1][k]),degree);
				if (i > 1 && j > 1) {
					dist +=  DistanceTools.vecDifference(series1, series1, i-2, j-2, 2, true, dimensionsToUse);
//					dist += (series1[i - 2] - series2[j - 2]) * (series1[i - 2] - series2[j - 2]);
				}
				// dist+=Math.pow(Math.abs(series1[i-2][k]-series2[j-2][k]),degree);
				D[i][j] = (dist);
			}
		} // for i

		// border of the cost matrix initialization
		D[0][0] = 0;
		for (i = 1; i <= r; i++) {
			D[i][0] = D[i - 1][0] + Di1[i];
		}
		for (j = 1; j <= c; j++) {
			D[0][j] = D[0][j - 1] + Dj1[j];
		}

		double dmin, htrans, dist0;

		for (i = 1; i <= r; i++) {
			for (j = 1; j <= c; j++) {
				htrans = Math.abs(i- j);
				if (j > 1 && i > 1) {
					htrans += Math.abs((i-1) - (j-1));
				}
				dist0 = D[i - 1][j - 1] + nu * htrans + D[i][j];
				dmin = dist0;
				if (i > 1) {
					htrans = 1;
				} else {
					htrans = i;
				}
				dist = Di1[i] + D[i - 1][j] + lambda + nu * htrans;
				if (dmin > dist) {
					dmin = dist;
				}
				if (j > 1) {
					htrans = 1;
				} else {
					htrans = j;
				}
				dist = Dj1[j] + D[i][j - 1] + lambda + nu * htrans;
				if (dmin > dist) {
					dmin = dist;
				}
				D[i][j] = dmin;
			}
		}

		dist = D[r][c];
		MemorySpaceProvider.getInstance().returnDoubleMatrix(D);
		MemorySpaceProvider.getInstance().returnDoubleArray(Di1);
		MemorySpaceProvider.getInstance().returnDoubleArray(Dj1);
		return dist;
	}


	public double get_random_nu(Dataset d, Random r) {
		double nu = twe_nuParams[r.nextInt(twe_nuParams.length)];
		return nu;
	} 	
	
	public double get_random_lambda(Dataset d, Random r) {
		double lambda = twe_lamdaParams[r.nextInt(twe_lamdaParams.length)];
		return lambda;
	} 		
	
	protected static final double[] twe_nuParams = { 0.00001, 0.0001, 0.0005, 0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1 };

	protected static final double[] twe_lamdaParams = { 0, 0.011111111, 0.022222222, 0.033333333, 0.044444444, 0.055555556, 0.066666667,
			0.077777778, 0.088888889, 0.1 };


//	shifaz old code backup 18/3/2020
//	public synchronized double distance_indep(double[] ta, double[] tb, double bsf, double nu, double lambda) {
//
//		int m = ta.length;
//		int n = tb.length;
//		int maxLength = Math.max(m, n);
//
//		double dist, disti1, distj1;
//
//		int r = ta.length; // this is just m?!
//		int c = tb.length; // so is this, but surely it should actually
//		// be n anyway
//
//		int i, j;
//		/*
//		 * allocations in c double **D = (double **)calloc(r+1,
//		 * sizeof(double*)); double *Di1 = (double *)calloc(r+1,
//		 * sizeof(double)); double *Dj1 = (double *)calloc(c+1,
//		 * sizeof(double)); for(i=0; i<=r; i++) { D[i]=(double
//		 * *)calloc(c+1, sizeof(double)); }
//		 */
//
//		double[][]D = MemorySpaceProvider.getInstance(maxLength+1).getDoubleMatrix();
//		double[]Di1 = MemorySpaceProvider.getInstance(maxLength+1).getDoubleArray();
//		double[]Dj1 = MemorySpaceProvider.getInstance(maxLength+1).getDoubleArray();
////		double[][] D = MemoryManager.getInstance().getDoubleMatrix(0);
////		double[] Di1 = MemoryManager.getInstance().getDoubleArray(0);
////		double[] Dj1 = MemoryManager.getInstance().getDoubleArray(1);
//
//		// FPH adding initialisation given that using matrices as fields
//		Di1[0] = 0.0;
//		Dj1[0] = 0.0;
//		// local costs initializations
//		for (j = 1; j <= c; j++) {
//			distj1 = 0;
//			if (j > 1) {
//				// CHANGE AJB 8/1/16: Only use power of
//				// 2 for speed
//				distj1 += (tb[j - 2] - tb[j - 1]) * (tb[j - 2] - tb[j - 1]);
//				// OLD VERSION
//				// distj1+=Math.pow(Math.abs(tb[j-2][k]-tb[j-1][k]),degree);
//				// in c:
//				// distj1+=pow(fabs(tb[j-2][k]-tb[j-1][k]),degree);
//			} else {
//				distj1 += tb[j - 1] * tb[j - 1];
//			}
//			// OLD distj1+=Math.pow(Math.abs(tb[j-1][k]),degree);
//			Dj1[j] = (distj1);
//		}
//
//		for (i = 1; i <= r; i++) {
//			disti1 = 0;
//			if (i > 1) {
//				disti1 += (ta[i - 2] - ta[i - 1]) * (ta[i - 2] - ta[i - 1]);
//			} // OLD
//			// disti1+=Math.pow(Math.abs(ta[i-2][k]-ta[i-1][k]),degree);
//			else {
//				disti1 += (ta[i - 1]) * (ta[i - 1]);
//			}
//			// OLD disti1+=Math.pow(Math.abs(ta[i-1][k]),degree);
//
//			Di1[i] = (disti1);
//
//			for (j = 1; j <= c; j++) {
//				dist = 0;
//				dist += (ta[i - 1] - tb[j - 1]) * (ta[i - 1] - tb[j - 1]);
//				// dist+=Math.pow(Math.abs(ta[i-1][k]-tb[j-1][k]),degree);
//				if (i > 1 && j > 1) {
//					dist += (ta[i - 2] - tb[j - 2]) * (ta[i - 2] - tb[j - 2]);
//				}
//				// dist+=Math.pow(Math.abs(ta[i-2][k]-tb[j-2][k]),degree);
//				D[i][j] = (dist);
//			}
//		} // for i
//
//		// border of the cost matrix initialization
//		D[0][0] = 0;
//		for (i = 1; i <= r; i++) {
//			D[i][0] = D[i - 1][0] + Di1[i];
//		}
//		for (j = 1; j <= c; j++) {
//			D[0][j] = D[0][j - 1] + Dj1[j];
//		}
//
//		double dmin, htrans, dist0;
//
//		for (i = 1; i <= r; i++) {
//			for (j = 1; j <= c; j++) {
//				htrans = Math.abs(i- j);
//				if (j > 1 && i > 1) {
//					htrans += Math.abs((i-1) - (j-1));
//				}
//				dist0 = D[i - 1][j - 1] + nu * htrans + D[i][j];
//				dmin = dist0;
//				if (i > 1) {
//					htrans = 1;
//				} else {
//					htrans = i;
//				}
//				dist = Di1[i] + D[i - 1][j] + lambda + nu * htrans;
//				if (dmin > dist) {
//					dmin = dist;
//				}
//				if (j > 1) {
//					htrans = 1;
//				} else {
//					htrans = j;
//				}
//				dist = Dj1[j] + D[i][j - 1] + lambda + nu * htrans;
//				if (dmin > dist) {
//					dmin = dist;
//				}
//				D[i][j] = dmin;
//			}
//		}
//
//		dist = D[r][c];
//		MemorySpaceProvider.getInstance().returnDoubleMatrix(D);
//		MemorySpaceProvider.getInstance().returnDoubleArray(Di1);
//		MemorySpaceProvider.getInstance().returnDoubleArray(Dj1);
//		return dist;
//	}

	
}
