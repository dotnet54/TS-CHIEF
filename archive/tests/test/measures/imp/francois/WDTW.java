package test.measures.imp.francois;

import java.util.Random;
import datasets.TSDataset;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (bug fixes, and improvements for efficiency) the original classes.
 * 
 */

public class WDTW {
	
	private static final double WEIGHT_MAX = 1;
	private double g; // "empirical constant that controls the curvature
			  // (slope) of the function
	private double[] weightVector; // initilised on first distance call
	
	public WDTW() {
		
	}
	
	public void setG(double g,int length){
		if(this.g!=g || this.weightVector==null){
			this.g= g;
			this.initWeights(length);
		}
		
	}	
	
	public synchronized double distance(double[] first, double[] second, double bsf, double g) {
		this.setG(g, first.length);

		double[][] distances = MemorySpaceProvider.getInstance(first.length).getDoubleMatrix();
//		double[][] distances = MemoryManager.getInstance().getDoubleMatrix(0);

		// create empty array
		if (distances == null || distances.length < first.length || distances[0].length < second.length) {
			distances = new double[first.length][second.length];
		}

		// first value
		distances[0][0] = this.weightVector[0] * (first[0] - second[0]) * (first[0] - second[0]);

		// top row
		for (int i = 1; i < second.length; i++) {
			distances[0][i] = distances[0][i - 1] + this.weightVector[i] * (first[0] - second[i]) * (first[0] - second[i]); // edited
																	// by
																	// Jay
		}

		// first column
		for (int i = 1; i < first.length; i++) {
			distances[i][0] = distances[i - 1][0] + this.weightVector[i] * (first[i] - second[0]) * (first[i] - second[0]); // edited
																	// by
																	// Jay
		}

		// warp rest
		double minDistance;
		for (int i = 1; i < first.length; i++) {

			for (int j = 1; j < second.length; j++) {
				// calculate distances
				minDistance = DistanceTools.Min3(distances[i][j - 1], distances[i - 1][j], distances[i - 1][j - 1]);
				distances[i][j] = minDistance + this.weightVector[Math.abs(i - j)] * (first[i] - second[j]) * (first[i] - second[j]); // edited
																		      // by
																		      // Jay

				//
				// if(minDistance > cutOffValue &&
				// this.isEarlyAbandon){
				// this.distances[i][j] = Double.MAX_VALUE;
				// }else{
				// this.distances[i][j] =
				// minDistance+this.weightVector[Math.abs(i-j)]
				// *(first[i]-second[j])*(first[i]-second[j]);
				// //edited by Jay
				// overflow = false;
				// }
			}

		}

		double res = distances[first.length - 1][second.length - 1];
		MemorySpaceProvider.getInstance().returnDoubleMatrix(distances);
		return res;
	}
	
	
	private void initWeights(int seriesLength) {
		this.weightVector = new double[seriesLength];
		double halfLength = (double) seriesLength / 2;

		for (int i = 0; i < seriesLength; i++) {
			weightVector[i] = WEIGHT_MAX / (1 + Math.exp(-g * (i - halfLength)));
		}
	}
	
	public double get_random_g(TSDataset d, Random r) {
		return r.nextDouble();
	}
	
}
