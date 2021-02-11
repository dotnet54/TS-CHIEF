package test.measures.imp.changw;

import datasets.Sequence;

public class WDTW extends ElasticDistances{
    private final static double[][] matrixD = new double[MAX_SEQ_LENGTH][MAX_SEQ_LENGTH];

    public static double distance(final Sequence first, final Sequence second, final double[] weightVector) {
        final int m = first.length;
        final int n = second.length;
        double diff;
        double minDistance;
        int i, j;

        //first value
        diff = first.value(0) - second.value(0);
        matrixD[0][0] = weightVector[0] * diff * diff;

        //first column
        for (i = 1; i < m; i++) {
            diff = first.value(i) - second.value(0);
            matrixD[i][0] = matrixD[i - 1][0] + weightVector[i] * diff * diff;
        }

        //top row
        for (j = 1; j < n; j++) {
            diff = first.value(0) - second.value(j);
            matrixD[0][j] = matrixD[0][j - 1] + weightVector[j] * diff * diff;
        }

        //warp rest
        for (i = 1; i < m; i++) {
            for (j = 1; j < n; j++) {
                //calculate distances
                minDistance = Math.min(matrixD[i][j - 1], Math.min(matrixD[i - 1][j], matrixD[i - 1][j - 1]));
                diff = first.value(i) - second.value(j);
                matrixD[i][j] = minDistance + weightVector[Math.abs(i - j)] * diff * diff;
            }
        }
        return matrixD[m - 1][n - 1];
    }

    public static double distance(final Sequence first, final Sequence second, final double[] weightVector, final double cutOffValue) {
        boolean tooBig;
        int m = first.length;
        int n = second.length;
        double diff;
        double minDistance;

        //first value
        diff = first.value(0) - second.value(0);
        matrixD[0][0] = weightVector[0] * diff * diff;
        if (matrixD[0][0] > cutOffValue) {
            return Double.MAX_VALUE;
        }

        //first column
        for (int i = 1; i < m; i++) {
            diff = first.value(i) - second.value(0);
            matrixD[i][0] = matrixD[i - 1][0] + weightVector[i] * diff * diff;
        }

        //top row
        for (int j = 1; j < n; j++) {
            diff = first.value(0) - second.value(j);
            matrixD[0][j] = matrixD[0][j - 1] + weightVector[j] * diff * diff;
        }

        //warp rest
        for (int i = 1; i < m; i++) {
            tooBig = true;
            for (int j = 1; j < n; j++) {
                //calculate distances
                minDistance = Math.min(matrixD[i][j - 1], Math.min(matrixD[i - 1][j], matrixD[i - 1][j - 1]));
                diff = first.value(i) - second.value(j);
                matrixD[i][j] = minDistance + weightVector[Math.abs(i - j)] * diff * diff;
                if (tooBig && matrixD[i][j] < cutOffValue) {
                    tooBig = false;
                }
            }
            //Early abandon
            if (tooBig) {
                return Double.MAX_VALUE;
            }
        }
        return matrixD[m - 1][n - 1];
    }

    public static double distance(final double[] first, final double[] second, final double[] weightVector, final double cutOffValue) {
        boolean tooBig;
        int m = first.length;
        int n = second.length;
        double diff;
        double minDistance;

        //first value
        diff = first[0] - second[0];
        matrixD[0][0] = weightVector[0] * diff * diff;
        if (matrixD[0][0] > cutOffValue) {
            return Double.MAX_VALUE;
        }

        //first column
        for (int i = 1; i < m; i++) {
            diff = first[i] - second[0];
            matrixD[i][0] = matrixD[i - 1][0] + weightVector[i] * diff * diff;
        }

        //top row
        for (int j = 1; j < n; j++) {
            diff = first[0] - second[j];
            matrixD[0][j] = matrixD[0][j - 1] + weightVector[j] * diff * diff;
        }

        //warp rest
        for (int i = 1; i < m; i++) {
            tooBig = true;
            for (int j = 1; j < n; j++) {
                //calculate distances
                minDistance = Math.min(matrixD[i][j - 1], Math.min(matrixD[i - 1][j], matrixD[i - 1][j - 1]));
                diff = first[i] - second[j];
                matrixD[i][j] = minDistance + weightVector[Math.abs(i - j)] * diff * diff;
                if (tooBig && matrixD[i][j] < cutOffValue) {
                    tooBig = false;
                }
            }
            //Early abandon
            if (tooBig) {
                return Double.MAX_VALUE;
            }
        }
        return matrixD[m - 1][n - 1];
    }

    public static double[] initWeights(final int seriesLength, final double g, final double maxWeight) {
        final double[] weightVector = new double[seriesLength];
        double halfLength = (double) seriesLength / 2;

        for (int i = 0; i < seriesLength; i++) {
            weightVector[i] = maxWeight / (1 + Math.exp(-g * (i - halfLength)));
        }
        return weightVector;
    }
}
