package distance.multivariate;

import data.timeseries.Dataset;

import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (fixes, and improvements for efficiency) the original classes.
 * 
 */

public class TWE extends MultivarSimMeasure {
	public static final String stiffnessParam = "nu";
	public static final String costParam = "lambda";
	protected double nu;
	protected double lambda;

	public TWE(boolean dependentDimensions, int[] dimensionsToUse, double nu, double lambda) {
		super(dependentDimensions, dimensionsToUse);
		this.nu = nu;
		this.lambda = lambda;
	}

	public double distance(double[][] series1, double[][] series2, double bsf, double nu, double lambda) throws Exception {
		this.setNu(nu);
		this.setLambda(lambda);
		return distance(series1, series2, bsf);
	}

	@Override
	public double distanceIndep(double[][] series1, double[][] series2, int dimension, double bsf) {
		double[] vector1 = series1[dimension];
		double[] vector2 = series2[dimension];
		int m = vector1.length;
		int n = vector2.length;
		int maxLength = Math.max(m, n);
		double dist, disti1, distj1;

		int r = vector1.length; // this is just m?!
		int c = vector2.length; // so is this, but surely it should actually
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
				distj1 += (vector2[j - 2] - vector2[j - 1]) * (vector2[j - 2] - vector2[j - 1]);
				// OLD VERSION
				// distj1+=Math.pow(Math.abs(series2[j-2][k]-series2[j-1][k]),degree);
				// in c:
				// distj1+=pow(fabs(series2[j-2][k]-series2[j-1][k]),degree);
			} else {
				distj1 += vector2[j - 1] * vector2[j - 1];
			}
			// OLD distj1+=Math.pow(Math.abs(series2[j-1][k]),degree);
			Dj1[j] = (distj1);
		}

		for (i = 1; i <= r; i++) {
			disti1 = 0;
			if (i > 1) {
				disti1 += (vector1[i - 2] - vector1[i - 1]) * (vector1[i - 2] - vector1[i - 1]);
			} // OLD
			  // disti1+=Math.pow(Math.abs(series1[i-2][k]-series1[i-1][k]),degree);
			else {
				disti1 += (vector1[i - 1]) * (vector1[i - 1]);
			}
			// OLD disti1+=Math.pow(Math.abs(series1[i-1][k]),degree);

			Di1[i] = (disti1);

			for (j = 1; j <= c; j++) {
				dist = 0;
				dist += (vector1[i - 1] - vector2[j - 1]) * (vector1[i - 1] - vector2[j - 1]);
				// dist+=Math.pow(Math.abs(series1[i-1][k]-series2[j-1][k]),degree);
				if (i > 1 && j > 1) {
					dist += (vector1[i - 2] - vector2[j - 2]) * (vector1[i - 2] - vector2[j - 2]);
				}
				// dist+=Math.pow(Math.abs(series1[i-2][k]-series2[j-2][k]),degree);
				D[i][j] = (dist);
			}
		} // for i

		// border of the cost matrix initialization
		D[0][0] = 0;
		for (i = 1; i <= r; i++) {
			D[i][0] = Double.POSITIVE_INFINITY;
		}
		for (j = 1; j <= c; j++) {
			D[0][j] = Double.POSITIVE_INFINITY;
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

	@Override
	public double distanceDep(double[][] series1, double[][] series2, double bsf){
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
				distj1 += adjustedSquaredEuclideanDistanceFromPointToPoint(series2, j-2, series2, j-1);
//				distj1 += (series2[j - 2] - series2[j - 1]) * (series2[j - 2] - series2[j - 1]);
				// OLD VERSION
				// distj1+=Math.pow(Math.abs(series2[j-2][k]-series2[j-1][k]),degree);
				// in c:
				// distj1+=pow(fabs(series2[j-2][k]-series2[j-1][k]),degree);
			} else {
				distj1 += adjustedSquaredEuclideanNormOfPoint(series2, j-1);
//				distj1 += series2[j - 1] * series2[j - 1];
			}
			// OLD distj1+=Math.pow(Math.abs(series2[j-1][k]),degree);
			Dj1[j] = (distj1);
		}

		for (i = 1; i <= r; i++) {
			disti1 = 0;
			if (i > 1) {
				disti1 += adjustedSquaredEuclideanDistanceFromPointToPoint(series1, i-2, series1, i-1);
//				disti1 += (series1[i - 2] - series1[i - 1]) * (series1[i - 2] - series1[i - 1]);
			} // OLD
			// disti1+=Math.pow(Math.abs(series1[i-2][k]-series1[i-1][k]),degree);
			else {
				disti1 += adjustedSquaredEuclideanNormOfPoint(series1, i-1);
//				disti1 += (series1[i - 1]) * (series1[i - 1]);
			}
			// OLD disti1+=Math.pow(Math.abs(series1[i-1][k]),degree);

			Di1[i] = (disti1);

			for (j = 1; j <= c; j++) {
				dist = 0;
				dist += adjustedSquaredEuclideanDistanceFromPointToPoint(series1, i-1, series2, j-1);
//				dist += (series1[i - 1] - series2[j - 1]) * (series1[i - 1] - series2[j - 1]);
				// dist+=Math.pow(Math.abs(series1[i-1][k]-series2[j-1][k]),degree);
				if (i > 1 && j > 1) {
					dist +=  adjustedSquaredEuclideanDistanceFromPointToPoint(series1, i-2, series2, j-2);
//					dist += (series1[i - 2] - series2[j - 2]) * (series1[i - 2] - series2[j - 2]);
				}
				// dist+=Math.pow(Math.abs(series1[i-2][k]-series2[j-2][k]),degree);
				D[i][j] = (dist);
			}
		} // for i

//		// border of the cost matrix initialization
//		D[0][0] = 0;
//		for (i = 1; i <= r; i++) {
//			D[i][0] = D[i - 1][0] + Di1[i];
//		}
//		for (j = 1; j <= c; j++) {
//			D[0][j] = D[0][j - 1] + Dj1[j];
//		}
		//note fix by Mat
//		https://github.com/uea-machine-learning/tsml/commit/bd0b2af1bbfdc3bd9ff078b468882ea493270185#diff-c7dc635e73d5e2013a87938ba4eb5a22
		D[0][0] = 0;
		for (i = 1; i <= r; i++) {
			D[i][0] = Double.POSITIVE_INFINITY;
		}
		for (j = 1; j <= c; j++) {
			D[0][j] = Double.POSITIVE_INFINITY;
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

	// Unused - TWE_D now uses the adjusted method
	protected final double squaredEuclideanNormOfPoint(double[][] series1, int i) {
		double total = 0;
		double element;
		for (int dimension : dimensionsToUse) {
			element = series1[dimension][i];
			total += element * element;
		}
		return total;
	}

	// Used by TWE_D
	protected final double adjustedSquaredEuclideanNormOfPoint(double[][] series1, int i) {
		double total = 0;
		double element;
		for (int dimension : dimensionsToUse) {
			element = series1[dimension][i];
			total += element * element;
		}
		return total / (2 * dimensionsToUse.length);
	}


	// Used by TWE_D
	protected final double adjustedSquaredEuclideanDistanceFromPointToPoint(double[][] series1, int i,
																			double[][] series2, int j) {
		double total = 0;
		double diff;
		for (int dimension : dimensionsToUse) {
			diff = series1[dimension][i] - series2[dimension][j];
			total += diff * diff;
		}
		return total / (2 * dimensionsToUse.length);
	}

	@Override
	public void setRandomParams(Dataset trainData, Random rand){
		setNu(getRandomNu(trainData, rand));
		setLambda(getRandomLambda(trainData, rand));
	}

	@Override
	public void initParamsByID(Dataset trainData, Random rand) {
		//pass
	}

	@Override
	public void setParamsByID(int paramID, int seriesLength, Random rand) {
		setNu(twe_nuParams[paramID / 10]);
		setLambda(twe_lamdaParams[paramID % 10]);
	}

	public static double getRandomNu(Dataset dataset, Random rand) {
		return twe_nuParams[rand.nextInt(twe_nuParams.length)];
	} 	
	
	public static double getRandomLambda(Dataset dataset, Random rand) {
		return twe_lamdaParams[rand.nextInt(twe_lamdaParams.length)];
	}

	public double getNu() {
		return nu;
	}

	public void setNu(double nu) {
		this.nu = nu;
	}

	public double getLambda() {
		return lambda;
	}

	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

	@Override
	public String toString(){
		return "TWE[,n="+nu+",l="+lambda+","
				+super.toString()+",]";
	}

	public static final double[] twe_nuParams = { 0.00001, 0.0001, 0.0005, 0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1 };

	public static final double[] twe_lamdaParams = { 0, 0.011111111, 0.022222222, 0.033333333, 0.044444444, 0.055555556, 0.066666667,
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
