package distance.lpmultivariate;

import knn.KNNTrainResult;
import core.exceptions.NotImplementedException;
import data.timeseries.Dataset;
import distance.univariate.MEASURE;

import java.util.Random;

public abstract class LPMultivarSimMeasure {

    //NOTE: for thead-safety make sure that class level variables are read only, or initialized once per parallel operation
    // I removed synchronised functions (added for defensive programming in PF and TSCHIEF) from multivariate distances
    // in TSCHIEF threading works because each tree runs on a different thread
    // so multiple threads will not be modifying same MultivariateSimilarityMeasure object at a given time.
    // if these classes are used where fields such as dimensionsToUse of same objects may be modified
    // by multiple threads, then thread-safety is not guaranteed
    // also note: ERP uses class level shared arrays

    //NOTE: do not modify these between threads
    protected boolean dependentDimensions;
    protected int[] dimensionsToUse;
    protected double lpDistanceOrderForIndependentDims = 2.0; //declared as double to experiment with fractional norms
    protected double lpDistanceOrderForDependentDims = 2.0;
    protected boolean useSquaredDiff = true;
    protected boolean adjustSquaredDiff = true;

    //DEV
    public boolean debug = false;
    public boolean allocateLocalMemory = true;
    public boolean useDerivativeData = false; //flag to indicate if we need to cache derivative data

    //DEV DEBUG
    public KNNTrainResult debugResult; //note thread unsafe

    public LPMultivarSimMeasure(boolean dependentDimensions, int[] dimensionsToUse) {
        this.dependentDimensions = dependentDimensions;
        this.dimensionsToUse = dimensionsToUse;
    }

    public double distance(double[][] series1, double[][] series2, double bsf){
        if (dependentDimensions){
            return distanceDep(series1, series2, bsf);
        }else if (lpDistanceOrderForIndependentDims == 1.0){
            double total = 0;
            for (int dimension : dimensionsToUse) {
                total += distanceIndep(series1[dimension], series2[dimension], bsf);
            }
            return total;
        }else if (lpDistanceOrderForIndependentDims == 2.0){
            double total = 0;
            double dist;
            for (int dimension : dimensionsToUse) {
                dist = distanceIndep(series1[dimension], series2[dimension], bsf);
//                total += dist * dist;  // this is unecessary as distances return a squared already
            }
            return Math.sqrt(total);
        }else{
            //TODO NOT FULLY SUPPORTED  -- DEVELOPMENT ONLY
            double total = 0;
            double dist;
            for (int dimension : dimensionsToUse) {
                dist = distanceIndep(series1[dimension], series2[dimension], bsf);
                total += Math.pow(dist, lpDistanceOrderForIndependentDims);
            }
            return Math.pow(total, (double) 1/lpDistanceOrderForIndependentDims);
        }
    }

    public abstract double distanceIndep(double[] vector1, double[] vector2, double bsf);

    public abstract double distanceDep(double[][] series1, double[][] series2, double bsf);

    public double[] sqEucDistanceNormalizationFactors = new double[]{
            20.69331741,  26.10923435,  32.0555738 ,  40.82906597,
            44.19019977,  48.26410584,  51.92572697,  57.96332854,
            60.12477934,  63.27639535};

    public double[] eucDistanceNormalizationFactors = new double[]{
            4.57779967,  5.24349238,  5.69775233,  6.19549599,  6.57517861,
            6.92743669,  7.1976062 ,  7.58274307,  7.82130915,  7.92029086,
            8.32304681,  8.49980377};

    protected final double squaredPoint(double[][] series1, int i) {
        double total = 0;
        double element;
        for (int dimension : dimensionsToUse) {
            element = series1[dimension][i];
            total += element * element;
        }
        if (adjustSquaredDiff){
            return total / (2 * dimensionsToUse.length);
        }else{
            return total;
        }
    }

    protected final double squaredDistancePointToPoint(double[][] series1, int i,
                                                       double[][] series2, int j) {
        double total = 0;
        double diff;
        for (int dimension : dimensionsToUse) {
            diff = series1[dimension][i] - series2[dimension][j];
            total += diff * diff;
        }
        if (adjustSquaredDiff){
            return total / (2 * dimensionsToUse.length);
        }else{
            return total;
        }
    }

    protected final double squaredDistancePointToScalar(double[][] series1, int i, double scalar) {
        double total = 0;
        double diff;

        if(useSquaredDiff) {
            for (int dimension : dimensionsToUse) {
                diff = series1[dimension][i] - scalar;
                total += diff * diff;
            }
        }else{
            for (int dimension : dimensionsToUse) {
                diff = series1[dimension][i] - scalar;
                total += Math.abs(diff);
            }
        }
//        return Math.sqrt(total); //CHECK BUG! fixed on 28.7.2020
        return total;
    }

    public int isSimilar(double a, double b, double epsilon) {
        if(useSquaredDiff) {
            return Math.sqrt((a - b) * (a - b)) <= epsilon ? 1 : 0;
        }else{
            return Math.abs(a - b) <= epsilon ? 1 : 0;
        }
    }

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

    //TODO check thread safety
    protected void getDerivative(double[] outputArray, double[] vector) {
        for (int i = 1; i < vector.length - 1 ; i++) {
            outputArray[i] = ((vector[i] - vector[i - 1]) + ((vector[i + 1] - vector[i - 1]) / 2.0)) / 2.0;
        }
        outputArray[0] = outputArray[1];
        outputArray[outputArray.length - 1] = outputArray[outputArray.length - 2];
    }

    public double distanceNaive(double[] series1, double[] series2){
        throw new NotImplementedException();
    }

    public boolean isDependentDimensions(){
        return dependentDimensions;
    }

    public void setDependentDimensions(boolean dependentDimensions){
        this.dependentDimensions = dependentDimensions;
    }

    public int[] getDimensionsToUse(){
        return dimensionsToUse;
    }

    public void setDimensionsToUse(int[] dimensionsToUse){
        this.dimensionsToUse = dimensionsToUse;
    }

    public double getLpDistanceOrderForIndependentDims() {
        return lpDistanceOrderForIndependentDims;
    }

    public void setLpDistanceOrderForIndependentDims(double lpDistanceOrderForIndependentDims) {
        this.lpDistanceOrderForIndependentDims = lpDistanceOrderForIndependentDims;
    }

    public double getLpDistanceOrderForDependentDims() {
        return lpDistanceOrderForDependentDims;
    }

    public void setLpDistanceOrderForDependentDims(double lpDistanceOrderForDependentDims) {
        this.lpDistanceOrderForDependentDims = lpDistanceOrderForDependentDims;
    }

    public boolean isAdjustSquaredDiff() {
        return adjustSquaredDiff;
    }

    public void setAdjustSquaredDiff(boolean adjustSquaredDiff) {
        this.adjustSquaredDiff = adjustSquaredDiff;
    }

    public boolean isUseSquaredDiff() {
        return useSquaredDiff;
    }

    public void setUseSquaredDiff(boolean useSquaredDiff) {
        this.useSquaredDiff = useSquaredDiff;
    }

    public abstract void setRandomParams(Dataset trainData, Random rand);

    public double getDblParam(String name)  {
        throw new RuntimeException("Invalid parameter");
    }

    public void setDblParam(String name, double value)  {
        throw new RuntimeException("Invalid parameter");

    }

    public int getIntParam(String name)  {
        throw new RuntimeException("Invalid parameter");

    }

    public void setIntParam(String name, int value)  {
        throw new RuntimeException("Invalid parameter");
    }

    public String getName(){
        return this.getClass().getSimpleName();
    }


    public static LPMultivarSimMeasure createSimilarityMeasure(MEASURE measureName,
                                                               boolean dependentDimensions,
                                                               int[] dimensionsToUse){
        LPMultivarSimMeasure measure = null;
        switch (measureName) {
            case euc:
            case euclidean:
                measure = new Euclidean(dependentDimensions, dimensionsToUse);
                break;
            case dtw:
            case dtwf:
                measure = new DTW(dependentDimensions, dimensionsToUse, -1);
                break;
            case dtwcv:
            case dtwr:
                measure = new DTW(dependentDimensions, dimensionsToUse,-1);
                break;
            case ddtw:
            case ddtwf:
                measure = new DDTW(dependentDimensions, dimensionsToUse, -1);
                break;
            case ddtwcv:
            case ddtwr:
                measure = new DDTW(dependentDimensions, dimensionsToUse,-1);
                break;
            case wdtw:
                measure = new WDTW(dependentDimensions, dimensionsToUse, 0);
                break;
            case wddtw:
                measure = new WDDTW(dependentDimensions, dimensionsToUse,0);
                break;
            case lcss:
                measure = new LCSS(dependentDimensions, dimensionsToUse, -1, 0);
                break;
            case msm:
                measure = new MSM(dependentDimensions, dimensionsToUse,1);
                break;
            case erp:
                measure = new ERP(dependentDimensions, dimensionsToUse, -1 , 1);
                break;
            case twe:
                measure = new TWE(dependentDimensions, dimensionsToUse, 0, 1);
                break;
            default:
                throw new RuntimeException("Unknown similarity measure");
        }

        return measure;
    }


    public static LPMultivarSimMeasure createSimilarityMeasureWithRandomParams(MEASURE measureName,
                                                                               boolean dependentDimensions,
                                                                               int[] dimensionsToUse,
                                                                               Dataset dataset,
                                                                               Random rand) throws Exception {
        LPMultivarSimMeasure measure = null;

        switch (measureName) {
            case euc:
            case euclidean:
                measure = new Euclidean(dependentDimensions, dimensionsToUse);
                break;
            case dtw:
            case dtwf:
                measure = new DTW(dependentDimensions, dimensionsToUse, dataset.length());
                break;
            case dtwcv:
            case dtwr:
                measure = new DTW(dependentDimensions, dimensionsToUse,
                        DTW.getRandomWindowSize(dataset, rand));
                break;
            case ddtw:
            case ddtwf:
                measure = new DDTW(dependentDimensions, dimensionsToUse, dataset.length());
                break;
            case ddtwcv:
            case ddtwr:
                measure = new DDTW(dependentDimensions, dimensionsToUse,
                        DTW.getRandomWindowSize(dataset, rand));
                break;
            case wdtw:
                measure = new WDTW(dependentDimensions, dimensionsToUse,
                        WDTW.getRandomG(dataset, rand));
                break;
            case wddtw:
                measure = new WDDTW(dependentDimensions, dimensionsToUse,
                        WDTW.getRandomG(dataset, rand));
                break;
            case lcss:
                measure = new LCSS(dependentDimensions, dimensionsToUse,
                        LCSS.getRandomWindowSize(dataset, rand), LCSS.getRandomEpsilon(dataset, rand));
                break;
            case msm:
                measure = new MSM(dependentDimensions, dimensionsToUse,
                        MSM.getRandomCost(dataset, rand));
                break;
            case erp:
                measure = new ERP(dependentDimensions, dimensionsToUse,
                        ERP.getRandomWindowSize(dataset, rand), ERP.getRandomG(dataset, rand));
                break;
            case twe:
                measure = new TWE(dependentDimensions, dimensionsToUse,
                        TWE.getRandomNu(dataset, rand), TWE.getRandomLambda(dataset,rand));
                break;
            default:
                throw new Exception("Unknown similarity measure");
        }

        return measure;
    }

    @Override
    public String toString(){
        return "dep="+dependentDimensions
                +",|d|="+(dimensionsToUse!=null?dimensionsToUse.length:0)
                +",lpI="+lpDistanceOrderForIndependentDims
                +",lpD="+lpDistanceOrderForDependentDims
                +",adj="+adjustSquaredDiff;
    }


    //support functions

    //if equal choose the diagonal
    public final int max(int left, int up, int diagonal) {
        if (left > up) {
            if (left > diagonal) {
                // left > up and left > diagonal
                return left;
            } else {
                // diagonal >= left > up
                return diagonal;
            }
        } else {
            if (up > diagonal) {
                // up > left and up > diagonal
                return up;
            } else {
                // diagonal >= up > left
                return diagonal;
            }
        }
    }

    //if equal choose the diagonal
    public final double min(double left, double up, double diagonal) {
        if (left < up) {
            if (left < diagonal) {
                // left < up and left < diagonal
                return left;
            } else {
                // diagonal <= left < up
                return diagonal;
            }
        } else {
            if (up < diagonal) {
                // up < left and up < diagonal
                return up;
            } else {
                // diagonal <= up < left
                return diagonal;
            }
        }
    }

//    public double[] getTimePoint(double[][] series, int index, int[] dimensionsToUse){
//        double[] timePoint = new double[series.length];
//        for (int dimension : dimensionsToUse) {
//            timePoint[dimension] = series[dimension][index];
//        }
//        return timePoint;
//    }

//    public final double squaredDistanceScalar(double A, double B) {
//        return (A - B) * (A - B);
//    }

//    public final double squaredDistanceVector(double[] A, double[] B, int[] dimensionsToUse) {
//        double total = 0;
//        for (int dimension : dimensionsToUse) {
//            total += ((A[dimension] - B[dimension]) * (A[dimension] - B[dimension]));
//        }
//        return total;
//    }

//    public final double squaredDistanceVector(double[] A, double[][] B, int j, int[] dimensionsToUse) {
//        double total = 0;
//        for (int dimension : dimensionsToUse) {
//            total += ((A[dimension] - B[dimension][j]) * (A[dimension] - B[dimension][j]));
//        }
//        return total;
//    }


    //TODO --- DEV

//    //generic implementation -- skeleton
//    public double distanceElastic(double[] series1Dimension, double[] series2Dimension,
//                                  double bsf, int windowSize) {
//        int length1 = series1Dimension.length;
//        int length2 = series2Dimension.length;
//        int maxLength = Math.max(length1, length2);
//
//        if (windowSize < 0 || windowSize > maxLength) {
//            windowSize = maxLength;
//        }
//
//        double[] prevRow = new double[maxLength];
//        double[] currentRow = new double[maxLength];
//
//        if (prevRow == null || prevRow.length < maxLength) {
//            prevRow = new double[maxLength];
//        }
//
//        if (currentRow == null || currentRow.length < maxLength) {
//            currentRow = new double[maxLength];
//        }
//
//        int i, j;
//        double prevVal;
//        double thisSeries1Val = series1Dimension[0];
//
//        // initialising the first row - do this in prevRow so as to save swapping rows before next row
//        prevVal = prevRow[0] = squaredDistanceScalar(thisSeries1Val, series2Dimension[0]);
//
//        for (j = 1; j < Math.min(length2, 1 + windowSize); j++) {
//            prevVal = prevRow[j] = prevVal + squaredDistanceScalar(thisSeries1Val, series2Dimension[j]);
//        }
//
//        // the second row is a special case
//        if (length1 >= 2){
//            thisSeries1Val = series1Dimension[1];
//
//            if (windowSize>0){
//                currentRow[0] = prevRow[0]+ squaredDistanceScalar(thisSeries1Val, series2Dimension[0]);
//            }
//
//            // in this special case, neither matrix[1][0] nor matrix[0][1] can be on the (shortest) minimum path
//            prevVal = currentRow[1]=prevRow[0]+ squaredDistanceScalar(thisSeries1Val, series2Dimension[1]);
//            int jStop = (windowSize + 2 > length2) ? length2 : windowSize + 2;
//
//            for (j = 2; j < jStop; j++) {
//                // for the second row, matrix[0][j - 1] cannot be on a (shortest) minimum path
//                prevVal = currentRow[j] = Math.min(prevVal, prevRow[j - 1]) + squaredDistanceScalar(thisSeries1Val, series2Dimension[j]);
//            }
//        }
//
//        // third and subsequent rows
//        for (i = 2; i < length1; i++) {
//            int jStart;
//            int jStop = (i + windowSize >= length2) ? length2-1 : i + windowSize;
//
//            // the old currentRow becomes this prevRow and so the currentRow needs to use the old prevRow
//            double[] tmp = prevRow;
//            prevRow = currentRow;
//            currentRow = tmp;
//
//            thisSeries1Val = series1Dimension[i];
//
//            if (i - windowSize < 1) {
//                jStart = 1;
//                currentRow[0] = prevRow[0] + squaredDistanceScalar(thisSeries1Val, series2Dimension[0]);
//            }
//            else {
//                jStart = i - windowSize;
//            }
//
//            if (jStart <= jStop){
//                // If jStart is the start of the window, [i][jStart-1] is outside the window.
//                // Otherwise jStart-1 must be 0 and the path through [i][0] can never be less than the path directly from [i-1][0]
//                prevVal = currentRow[jStart] = Math.min(prevRow[jStart - 1], prevRow[jStart])+ squaredDistanceScalar(thisSeries1Val, series2Dimension[jStart]);
//                for (j = jStart+1; j < jStop; j++) {
//                    prevVal = currentRow[j] = min(prevVal, prevRow[j], prevRow[j - 1])
//                            + squaredDistanceScalar(thisSeries1Val, series2Dimension[j]);
//                }
//
//                if (i + windowSize >= length2) {
//                    // the window overruns the end of the sequence so can have a path through prevRow[jStop]
//                    currentRow[jStop] = min(prevVal, prevRow[jStop], prevRow[jStop - 1]) + squaredDistanceScalar(thisSeries1Val, series2Dimension[jStop]);
//                }
//                else {
//                    currentRow[jStop] = Math.min(prevRow[jStop - 1], prevVal) + squaredDistanceScalar(thisSeries1Val, series2Dimension[jStop]);
//                }
//            }
//        }
//        //TODO bug if series 1 length == 1, value is in prevrow instead of current row
////		TODO prevVal
//        return Math.sqrt(currentRow[length2 - 1]);
//    }


}
