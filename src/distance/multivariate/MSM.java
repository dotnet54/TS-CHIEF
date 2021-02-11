package distance.multivariate;

import data.timeseries.Dataset;

import java.util.Random;

/**
 * Some classes in this package may contain borrowed code from the timeseriesweka project (Bagnall, 2017), 
 * we might have modified (fixes, and improvements for efficiency) the original classes.
 * 
 */

public class MSM extends MultivarSimMeasure {
	public static final String costParam = "c";
	protected double cost;

	//DEV
	protected boolean useEnvelop = true;

	public MSM(boolean dependentDimensions, int[] dimensionsToUse, double cost) {
		super(dependentDimensions, dimensionsToUse);
		this.cost = cost;
	}

	public double distance(double[][] series1, double[][] series2, double bsf, double cost){
		this.setCost(cost);
		return distance(series1, series2, bsf);
	}

	@Override
	public double distanceIndep(double[][] series1, double[][] series2, int dimension, double bsf) {
		double[] vector1 = series1[dimension];
		double[] vector2 = series2[dimension];
		int length1 = vector1.length, length2 = vector2.length;
		int maxLength  = (length1 >= length2) ? length1 : length2;
		double[][] costMatrix = MemorySpaceProvider.getInstance(maxLength).getDoubleMatrix();
//		double[][] costMatrix = MemoryManager.getInstance().getDoubleMatrix(0);
		if (costMatrix == null || costMatrix.length < length1 || costMatrix[0].length < length2) {
			costMatrix = new double[length1][length2];
		}
		int i, j;
		// Initialization
		if(useSquaredDiff){
			costMatrix[0][0] = (vector1[0] - vector2[0]) * (vector1[0] - vector2[0]);
			for (i = 1; i < length1; i++) {
				costMatrix[i][0] = costMatrix[i - 1][0] + costForScalers(vector1[i], vector1[i - 1], vector2[0]);
			}
			for (j = 1; j < length2; j++) {
				costMatrix[0][j] = costMatrix[0][j - 1] + costForScalers(vector2[j], vector1[0], vector2[j - 1]);
			}
		}else{
			costMatrix[0][0]= Math.abs((vector1[0] - vector2[0]));
			for (i = 1; i < length1; i++) {
				costMatrix[i][0] = costMatrix[i - 1][0] + costForScalersAbsDiff(vector1[i], vector1[i - 1], vector2[0]);
			}
			for (j = 1; j < length2; j++) {
				costMatrix[0][j] = costMatrix[0][j - 1] + costForScalersAbsDiff(vector2[j], vector1[0], vector2[j - 1]);
			}
		}



		if(useSquaredDiff) {
			// Main Loop
			for (i = 1; i < length1; i++) {
				for (j = 1; j < length2; j++) {
					double diagonal, left, up;
					diagonal = costMatrix[i - 1][j - 1] + (vector1[i] - vector2[j]) * (vector1[i] - vector2[j]);
					left = costMatrix[i - 1][j] + costForScalers(vector1[i], vector1[i - 1], vector2[j]);
					up = costMatrix[i][j - 1] + costForScalers(vector2[j], vector1[i], vector2[j - 1]);
					costMatrix[i][j] = min(left, up, diagonal);
				}
			}
		}else{
			// Main Loop
			for (i = 1; i < length1; i++) {
				for (j = 1; j < length2; j++) {
					double diagonal, left, up;
					diagonal = costMatrix[i - 1][j - 1] + Math.abs(vector1[i] - vector2[j]);
					left = costMatrix[i - 1][j] + costForScalersAbsDiff(vector1[i], vector1[i - 1], vector2[j]);
					up = costMatrix[i][j - 1] + costForScalersAbsDiff(vector2[j], vector1[i], vector2[j - 1]);
					costMatrix[i][j] = min(left, up, diagonal);
				}
			}
		}



		// Output
		double res = costMatrix[length1 - 1][length2 - 1];
		MemorySpaceProvider.getInstance().returnDoubleMatrix(costMatrix);
		return res;
	}

	@Override
	public double distanceDep(double[][] series1, double[][] series2, double bsf){
		int length1 = series1[dimensionsToUse[0]].length, length2 = series2[dimensionsToUse[0]].length;
		int maxLength  = (length1 >= length2) ? length1 : length2;
		double[][] costMatrix = MemorySpaceProvider.getInstance(maxLength).getDoubleMatrix();
//		double[][] costMatrix = MemoryManager.getInstance().getDoubleMatrix(0);
		if (costMatrix == null || costMatrix.length < length1 || costMatrix[0].length < length2) {
			costMatrix = new double[length1][length2];
		}
		int i, j;
		// Initialization
		costMatrix[0][0] = squaredEuclideanDistanceFromPointToPoint(series1, 0, series2, 0);
		for (i = 1; i < length1; i++) {
			costMatrix[i][0] = costMatrix[i - 1][0] + costForVectorsWithEnvelop(series1, i , i - 1, series2, 0);
		}
		for (j = 1; j < length2; j++) {
			costMatrix[0][j] = costMatrix[0][j - 1] + costForVectorsWithEnvelop(series2, j , j - 1, series1, 0);
		}

		// Main Loop
		for (i = 1; i < length1; i++) {
			for (j = 1; j < length2; j++) {
				double diagonal, left, up;
				diagonal = costMatrix[i - 1][j - 1] + squaredEuclideanDistanceFromPointToPoint(series1, i, series2, j);
				left = costMatrix[i - 1][j] + costForVectorsWithEnvelop(series1, i , i - 1, series2, j);
				up = costMatrix[i][j - 1] + costForVectorsWithEnvelop(series2, j , j - 1, series1, i);
				costMatrix[i][j] = min(left, up, diagonal);
			}
		}
		// Output
		double res = costMatrix[length1 - 1][length2 - 1];
		MemorySpaceProvider.getInstance().returnDoubleMatrix(costMatrix);
		return res;
	}

//	private static final double calcualteCost(double new_point, double x, double y, double c) {
//		double dist = 0;
//		if (((x <= new_point) && (new_point <= y)) || ((y <= new_point) && (new_point <= x))) {
//			dist = c;
//		} else {
//			dist = c + Math.min(Math.abs(new_point - x), Math.abs(new_point - y));
//		}
//		return dist;
//	}

//	private static final double costForScalers(double qi1, double qi0, double cj1, double c) {
//		if (((qi0 <= qi1) && (qi1 <= cj1)) || ((cj1 <= qi1) && (qi1 <= qi0))) {
//			return c;
//		} else {
////			return c + Math.min(Math.abs(qi1 - qi0), Math.abs(qi1 - cj1));
//			double diff1 = qi1 - qi0;
//			double diff2 = qi1 - cj1;
//			return c + Math.min(diff1 * diff1, diff2 * diff2);
//		}
//	}

//	protected double costForVectors(double[][] series1, double[][] series2,
//									int i1, int i0, int j1, double c, int[] dimensionsToUse) {
//		if (TimeSeriesVectorOps.isBetween(series1, series2,i0, i1, j1, dimensionsToUse)
//				||  TimeSeriesVectorOps.isBetween(series1, series2,j1, i1, i0, dimensionsToUse) ) {
//			return c;
//		} else {
////			return c + Math.min(Math.abs(qi1 - qi0), Math.abs(qi1 - cj1));
//			double diff1 = DistanceTools.vecDifference(series1, series2, i1, i0, 2 , true, dimensionsToUse); //no Math.abs() for norm=2
//			double diff2 = DistanceTools.vecDifference(series1, series2, i1, j1, 2 , true, dimensionsToUse);;
//			return c + Math.min(diff1 * diff1, diff2 * diff2);
//		}
//	}


	protected double costForScalersAbsDiff(double xi, double xi_prev, double yi) {
		if ((xi_prev <= xi && xi <= yi) || (xi_prev >= xi && xi >= yi)) {
			return cost;
		} else {
			double diff1 = Math.abs(xi - xi_prev);
			double diff2 = Math.abs(xi - yi);
			return cost + (diff1 <= diff2 ? diff1 : diff2);
//			return c + Math.min(diff1, diff2);
		}
	}

	protected double costForScalers(double xi, double xi_prev, double yi) {
		if ((xi_prev <= xi && xi <= yi) || (xi_prev >= xi && xi >= yi)) {
			return cost;
		} else {
			double diff1 = xi - xi_prev;
			diff1 =  diff1 * diff1;
			double diff2 = xi - yi;
			diff2 = diff2 * diff2;
			return cost + (diff1 <= diff2 ? diff1 : diff2);
//			return c + Math.min(diff1 * diff1, diff2 * diff2);
		}
	}

	protected double costForVectorsWithProjection(double[][] seriesX, int xi, int xi_prev, double[][] seriesY, int yi) {

		double[] s = new double[seriesX.length];
		for (int dimension : dimensionsToUse) {
			s[dimension] = seriesY[dimension][yi] - seriesX[dimension][xi_prev];
		}

		double[] v = new double[seriesX.length];
		for (int dimension : dimensionsToUse) {
			v[dimension] = seriesX[dimension][xi] - seriesX[dimension][xi_prev];
		}

		//TODO     cp = s * np.dot(v,s)/np.dot(s,s) -- check order of ops
		double[] tmp1 = new double[seriesX.length];
		for (int dimension : dimensionsToUse) {
			tmp1[dimension] = v[dimension] * s[dimension];
		}
		double[] tmp2 = new double[seriesX.length];
		for (int dimension : dimensionsToUse) {
			tmp2[dimension] = s[dimension] * s[dimension];
		}
		double[] cp = new double[seriesX.length];
		for (int dimension : dimensionsToUse) {
			cp[dimension] = s[dimension] * tmp1[dimension];
		}
		for (int dimension : dimensionsToUse) {
			cp[dimension] = cp[dimension] * tmp2[dimension];
		}

		double[] x_prime = new double[seriesX.length];
		for (int dimension : dimensionsToUse) {
			x_prime[dimension] = cp[dimension] * seriesX[dimension][xi_prev];
		}

		double diff;
		double dist1 = 0;
		for (int dimension : dimensionsToUse) {
			diff = x_prime[dimension] - seriesX[dimension][xi_prev];
			dist1 += diff * diff;
		}
		dist1 = Math.sqrt(dist1);

		double dist2 = 0;
		for (int dimension : dimensionsToUse) {
			diff = x_prime[dimension] - seriesX[dimension][yi];
			dist2 += diff * diff;
		}
		dist2 = Math.sqrt(dist2);

		double dist3 = 0;
		for (int dimension : dimensionsToUse) {
			diff = seriesX[dimension][xi_prev] - seriesY[dimension][yi];
			dist3 += diff * diff;
		}
		dist3 = Math.sqrt(dist3);

		if (dist1 + dist2 <=  dist3){
			return cost;
		}else{
			if (dist1 <  dist2){
				return cost + dist1;
			}else{
				return cost + dist2;
			}
		}

	}

	protected double costForVectorsWithEnvelop(double[][] seriesX, int xi, int xi_prev, double[][] seriesY, int yi) {

//		if (debug){
//			double xp = seriesX[dimensionsToUse[0]][xi_prev];
//			double x = seriesX[dimensionsToUse[0]][xi];
//			double y = seriesY[dimensionsToUse[0]][yi];
////			System.out.print("\n" + xp +", "+ x + ", " + y);
//			return costForScalers(x, xp, y);
//		}

		double diff;
		double radius = 0;
		for (int dimension : dimensionsToUse) {
			diff = seriesX[dimension][xi_prev] - seriesY[dimension][yi];
			radius += diff * diff;
		}
//		radius = Math.sqrt(radius);  //CHECK
		radius = radius  / 2;

		double[] mid_point = new double[seriesX.length];
		for (int dimension : dimensionsToUse) {
			mid_point[dimension] = (seriesX[dimension][xi_prev] + seriesY[dimension][yi]) / 2;
		}

		double dist_to_mid = 0;
		for (int dimension : dimensionsToUse) {
			diff = mid_point[dimension] - seriesX[dimension][xi];
			dist_to_mid += diff * diff;
		}
//		dist_to_mid = Math.sqrt(dist_to_mid); //CHECK

		if (dist_to_mid <= radius){
			return cost;
		}else{
			double dist_to_x_prev = 0;
			for (int dimension : dimensionsToUse) {
				diff = seriesX[dimension][xi_prev] - seriesX[dimension][xi];
				dist_to_x_prev += diff * diff;
			}
			dist_to_x_prev = Math.sqrt(dist_to_x_prev); //CHECK

			double dist_to_y = 0;
			for (int dimension : dimensionsToUse) {
				diff = seriesY[dimension][yi] - seriesX[dimension][xi];
				dist_to_y += diff * diff;
			}
			dist_to_y = Math.sqrt(dist_to_y);  //CHECK

			if (dist_to_x_prev < dist_to_y){
				return cost + dist_to_x_prev;
			}else{
				return cost + dist_to_y;
			}
		}
	}

//	protected double costForVectors(double[][] seriesX, int xi, int xi_prev, double[][] seriesY, int yi) {
////		double debug_a = seriesX[0][xi_prev];
////		double debug_b = seriesX[0][xi];
////		double debug_c = seriesY[0][yi];
////		System.out.print("\n" + debug_a +", "+ debug_b+ ", " + debug_c);
////		return costForScalers(debug_b, debug_a, debug_c, c);
//
//		if (isBetween(seriesX, xi, xi_prev, seriesY, yi)) {
////			System.out.println(" a <= b <= c");
//			return cost;
//		} else {
//			double diff1 = squaredDistanceVectorToVector(seriesX, xi, seriesX, xi_prev);
//			double diff2 = squaredDistanceVectorToVector(seriesX, xi, seriesY, yi);
//			return cost + (diff1 <= diff2 ? diff1 : diff2);
//		}
//	}

//	//CHECK - to test if b is between a and c, the idea is to test the projection of b onto c is less than the length of c
//	// and greater than the projection of a onto c
//	public boolean isBetween(double[][] seriesX, int xi, int xi_prev, double[][] seriesY, int yi) {
//		double length_of_yi = 0;
//		for (int dimension : dimensionsToUse) {
//			length_of_yi += seriesY[dimension][yi] * seriesY[dimension][yi];
//		}
//		length_of_yi = Math.sqrt(length_of_yi);
//		if (length_of_yi == 0){
//			length_of_yi = 1; //CHECK does this makes sense? this is done to prevent division by 0,
//		}
//		double[] unit_direction_vector = new double[seriesY.length];
//		for (int dimension : dimensionsToUse) {
//			unit_direction_vector[dimension] = seriesY[dimension][yi] / length_of_yi;
//		}
//
//		double projection_xi_on_yi = 0;
//		for (int dimension : dimensionsToUse) {
//			projection_xi_on_yi +=  seriesX[dimension][xi] * unit_direction_vector[dimension];
//		}
//
//		double projection_xi_prev_on_yi = 0;
//		for (int dimension : dimensionsToUse) {
//			projection_xi_prev_on_yi +=  seriesX[dimension][xi_prev] * unit_direction_vector[dimension];
//		}
//
////		return (projection_xi_prev_on_yi <= projection_xi_on_yi && projection_xi_on_yi <= length_of_yi);
//		return (projection_xi_prev_on_yi <= projection_xi_on_yi && projection_xi_on_yi <= length_of_yi) ||
//				(length_of_yi >= projection_xi_on_yi && projection_xi_on_yi >= projection_xi_on_yi);
//
//	}






//	public static boolean isBetween(double[][] series1, double[][] series2, int xi, int xi_prev, int yi, int[] dimensions) {
//		double[] xi_prev_to_y_direction = TimeSeriesVectorOps.subtract(series1, xi_prev, series2, yi, dimensions);
//		double[] unit_direction_vector = Vectors.div(xi_prev_to_y_direction, Vectors.l2Norm(xi_prev_to_y_direction));
//		double projection = TimeSeriesVectorOps.project(series1, xi, unit_direction_vector, dimensions);
//		if (projection > 0 && projection < 0){
//			return true;
//		}else{
//			return false;
//		}
//	}
//	public static boolean isBetween(double[][] series1, double[][] series2, int xi, int xi_prev, int yi, int[] dimensions) {
//		double[] xi_prev_to_y_direction = TimeSeriesVectorOps.subtract(series1, xi_prev, series2, yi, dimensions);
//		double[] unit_direction_vector = Vectors.div(xi_prev_to_y_direction, Vectors.l2Norm(xi_prev_to_y_direction));
//		double projection = TimeSeriesVectorOps.project(series1, xi, unit_direction_vector, dimensions);
//		if (projection > 0 && projection < 0){
//			return true;
//		}else{
//			return false;
//		}
//
//	}

//	protected double costForVectors(double[][] series1, double[][] series2,
//									int xi, int xi_prev, int yi, double c, int[] dimensionsToUse) {
//		double debug_a = series1[0][xi_prev];
//		double debug_b = series1[0][xi];
//		double debug_c = series2[0][yi];
//
//		System.out.print("\n" + debug_a +", "+ debug_b+ ", " + debug_c);
//
//		if (isBetween(series1, series2, xi, xi_prev, yi, dimensionsToUse)
//				||  isBetween(series1, series2, yi, xi_prev, xi, dimensionsToUse) ) {
//			System.out.println(" a <= b <= c");
//			return c;
//		} else {
//			double diff1 = squaredDistanceVectorToVector(series1, xi, series1, xi_prev);
//			double diff2 = squaredDistanceVectorToVector(series1, xi, series2, yi);
//			return c + (diff1 <= diff2 ? diff1 : diff2);
//		}
//	}

	@Override
	public void setRandomParams(Dataset trainData, Random rand){
		setCost(getRandomCost(trainData, rand));
	}

	@Override
	public void initParamsByID(Dataset trainData, Random rand) {
		//pass
	}

	@Override
	public void setParamsByID(int paramID, int seriesLength, Random rand) {
		setCost(msmParams[paramID]);
	}

	public double getCost() {
		return this.cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public static double getRandomCost(Dataset dataset, Random rand) {
		return msmParams[rand.nextInt(msmParams.length)];
	}

	@Override
	public String toString(){
		return "MSM[,c="+cost+","
				+super.toString()+",]";
	}

	public static final double[] msmParams = { 0.01, 0.01375, 0.0175, 0.02125, 0.025, 0.02875, 0.0325, 0.03625, 0.04, 0.04375, 0.0475, 0.05125,
			0.055, 0.05875, 0.0625, 0.06625, 0.07, 0.07375, 0.0775, 0.08125, 0.085, 0.08875, 0.0925, 0.09625, 0.1, 0.136, 0.172, 0.208,
			0.244, 0.28, 0.316, 0.352, 0.388, 0.424, 0.46, 0.496, 0.532, 0.568, 0.604, 0.64, 0.676, 0.712, 0.748, 0.784, 0.82, 0.856,
			0.892, 0.928, 0.964, 1, 1.36, 1.72, 2.08, 2.44, 2.8, 3.16, 3.52, 3.88, 4.24, 4.6, 4.96, 5.32, 5.68, 6.04, 6.4, 6.76, 7.12,
			7.48, 7.84, 8.2, 8.56, 8.92, 9.28, 9.64, 10, 13.6, 17.2, 20.8, 24.4, 28, 31.6, 35.2, 38.8, 42.4, 46, 49.6, 53.2, 56.8, 60.4,
			64, 67.6, 71.2, 74.8, 78.4, 82, 85.6, 89.2, 92.8, 96.4, 100 };
}
