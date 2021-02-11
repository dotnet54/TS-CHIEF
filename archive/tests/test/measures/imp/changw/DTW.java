package test.measures.imp.changw;

import datasets.Sequence;
import results.WarpingPathResults;
import util.GenericTools;

public class DTW extends ElasticDistances{
    private final static double[][] matrixD = new double[MAX_SEQ_LENGTH][MAX_SEQ_LENGTH];
    private final static int[][] minDistanceToDiagonal = new int[MAX_SEQ_LENGTH][MAX_SEQ_LENGTH];

    public static WarpingPathResults distanceExt(final Sequence first, final Sequence second, final int windowSize) {
        double minDist = 0.0;
        final int n = first.length;
        final int m = second.length;

        double diff;
        int i, j, indiceRes, absIJ;
        int jStart, jEnd, indexInfyLeft;

        diff = first.value(0) - second.value(0);
        matrixD[0][0] = diff * diff;
        minDistanceToDiagonal[0][0] = 0;
        for (i = 1; i < Math.min(n, 1 + windowSize); i++) {
            diff = first.value(i) - second.value(0);
            matrixD[i][0] = matrixD[i - 1][0] + diff * diff;
            minDistanceToDiagonal[i][0] = i;
        }

        for (j = 1; j < Math.min(m, 1 + windowSize); j++) {
            diff = first.value(0) - second.value(j);
            matrixD[0][j] = matrixD[0][j - 1] + diff * diff;
            minDistanceToDiagonal[0][j] = j;
        }
        if (j < m) matrixD[0][j] = Double.POSITIVE_INFINITY;

        for (i = 1; i < n; i++) {
            jStart = Math.max(1, i - windowSize);
            jEnd = Math.min(m, i + windowSize + 1);
            indexInfyLeft = i - windowSize - 1;
            if (indexInfyLeft >= 0) matrixD[i][indexInfyLeft] = Double.POSITIVE_INFINITY;

            for (j = jStart; j < jEnd; j++) {
                absIJ = Math.abs(i - j);
                indiceRes = GenericTools.argMin3(matrixD[i - 1][j - 1], matrixD[i][j - 1], matrixD[i - 1][j]);
                switch (indiceRes) {
                    case DIAGONAL:
                        minDist = matrixD[i - 1][j - 1];
                        minDistanceToDiagonal[i][j] = Math.max(absIJ, minDistanceToDiagonal[i - 1][j - 1]);
                        break;
                    case LEFT:
                        minDist = matrixD[i][j - 1];
                        minDistanceToDiagonal[i][j] = Math.max(absIJ, minDistanceToDiagonal[i][j - 1]);
                        break;
                    case UP:
                        minDist = matrixD[i - 1][j];
                        minDistanceToDiagonal[i][j] = Math.max(absIJ, minDistanceToDiagonal[i - 1][j]);
                        break;
                }
                diff = first.value(i) - second.value(j);
                matrixD[i][j] = minDist + diff * diff;
            }
            if (j < m) matrixD[i][j] = Double.POSITIVE_INFINITY;
        }

        WarpingPathResults resExt = new WarpingPathResults();
        resExt.distance = matrixD[n - 1][m - 1];
        resExt.distanceFromDiagonal = minDistanceToDiagonal[n - 1][m - 1];
        return resExt;
    }

    public static WarpingPathResults distanceExt(final Sequence first, final Sequence second, final int windowSize, final double cutOffValue) {
        boolean tooBig;
        double minDist = 0.0;
        final int n = first.length;
        final int m = second.length;

        double diff;
        int i, j, indiceRes, absIJ;
        int jStart, jEnd, indexInfyLeft;

        diff = first.value(0) - second.value(0);
        matrixD[0][0] = diff * diff;
        minDistanceToDiagonal[0][0] = 0;
        for (i = 1; i < Math.min(n, 1 + windowSize); i++) {
            diff = first.value(i) - second.value(0);
            matrixD[i][0] = matrixD[i - 1][0] + diff * diff;
            minDistanceToDiagonal[i][0] = i;
        }

        for (j = 1; j < Math.min(m, 1 + windowSize); j++) {
            diff = first.value(0) - second.value(j);
            matrixD[0][j] = matrixD[0][j - 1] + diff * diff;
            minDistanceToDiagonal[0][j] = j;
        }
        if (j < m) matrixD[0][j] = Double.POSITIVE_INFINITY;

        for (i = 1; i < n; i++) {
            tooBig = true;
            jStart = Math.max(1, i - windowSize);
            jEnd = Math.min(m, i + windowSize + 1);
            indexInfyLeft = i - windowSize - 1;
            if (indexInfyLeft >= 0) matrixD[i][indexInfyLeft] = Double.POSITIVE_INFINITY;

            for (j = jStart; j < jEnd; j++) {
                absIJ = Math.abs(i - j);
                indiceRes = GenericTools.argMin3(matrixD[i - 1][j - 1], matrixD[i][j - 1], matrixD[i - 1][j]);
                switch (indiceRes) {
                    case DIAGONAL:
                        minDist = matrixD[i - 1][j - 1];
                        minDistanceToDiagonal[i][j] = Math.max(absIJ, minDistanceToDiagonal[i - 1][j - 1]);
                        break;
                    case LEFT:
                        minDist = matrixD[i][j - 1];
                        minDistanceToDiagonal[i][j] = Math.max(absIJ, minDistanceToDiagonal[i][j - 1]);
                        break;
                    case UP:
                        minDist = matrixD[i - 1][j];
                        minDistanceToDiagonal[i][j] = Math.max(absIJ, minDistanceToDiagonal[i - 1][j]);
                        break;
                }
                diff = first.value(i) - second.value(j);
                matrixD[i][j] = minDist + diff * diff;
                if (tooBig && matrixD[i][j] < cutOffValue) tooBig = false;
            }
            //Early abandon
            if (tooBig) return new WarpingPathResults(Double.POSITIVE_INFINITY, windowSize);

            if (j < m) matrixD[i][j] = Double.POSITIVE_INFINITY;
        }

        WarpingPathResults resExt = new WarpingPathResults();
        resExt.distance = matrixD[n - 1][m - 1];
        resExt.distanceFromDiagonal = minDistanceToDiagonal[n - 1][m - 1];
        return resExt;
    }

    public static double distance(final Sequence first, final Sequence second) {
        final int n = first.length;
        final int m = second.length;

        double diff;
        int i, j;

        diff = first.value(0) - second.value(0);
        matrixD[0][0] = diff * diff;
        for (i = 1; i < n; i++) {
            diff = first.value(i) - second.value(0);
            matrixD[i][0] = matrixD[i - 1][0] + diff * diff;
        }

        for (j = 1; j < m; j++) {
            diff = first.value(0) - second.value(j);
            matrixD[0][j] = matrixD[0][j - 1] + diff * diff;
        }

        for (i = 1; i < n; i++) {
            for (j = 1; j < m; j++) {
                diff = first.value(i) - second.value(j);
                matrixD[i][j] = GenericTools.min3(matrixD[i - 1][j - 1], matrixD[i][j - 1], matrixD[i - 1][j]) + diff * diff;
            }
        }

        return matrixD[n - 1][m - 1];
    }

    public static double distance(final Sequence first, final Sequence second, final int windowSize) {
        final int n = first.length;
        final int m = second.length;

        final int winPlus1 = windowSize + 1;
        double diff;
        int i, j, jStart, jEnd, indexInfyLeft;

        diff = first.value(0) - second.value(0);
        matrixD[0][0] = diff * diff;
        for (i = 1; i < Math.min(n, winPlus1); i++) {
            diff = first.value(i) - second.value(0);
            matrixD[i][0] = matrixD[i - 1][0] + diff * diff;
        }

        for (j = 1; j < Math.min(m, winPlus1); j++) {
            diff = first.value(0) - second.value(j);
            matrixD[0][j] = matrixD[0][j - 1] + diff * diff;
        }
        if (j < m)
            matrixD[0][j] = Double.POSITIVE_INFINITY;

        for (i = 1; i < n; i++) {
            jStart = Math.max(1, i - windowSize);
            jEnd = Math.min(m, i + winPlus1);
            indexInfyLeft = i - windowSize - 1;
            if (indexInfyLeft >= 0)
                matrixD[i][indexInfyLeft] = Double.POSITIVE_INFINITY;

            for (j = jStart; j < jEnd; j++) {
                diff = first.value(i) - second.value(j);
                matrixD[i][j] = GenericTools.min3(matrixD[i - 1][j - 1], matrixD[i][j - 1], matrixD[i - 1][j]) + diff * diff;
            }
            if (j < m)
                matrixD[i][j] = Double.POSITIVE_INFINITY;
        }

        return matrixD[n - 1][m - 1];
    }

    public static double distance(final Sequence first, final Sequence second, final double cutOffValue) {
        boolean tooBig;
        final int n = first.length;
        final int m = second.length;

        double diff;
        int i, j;

        diff = first.value(0) - second.value(0);
        matrixD[0][0] = diff * diff;
        for (i = 1; i < n; i++) {
            diff = first.value(i) - second.value(0);
            matrixD[i][0] = matrixD[i - 1][0] + diff * diff;
        }

        for (j = 1; j < m; j++) {
            diff = first.value(0) - second.value(j);
            matrixD[0][j] = matrixD[0][j - 1] + diff * diff;
        }

        for (i = 1; i < n; i++) {
            tooBig = true;

            for (j = 1; j < m; j++) {
                diff = first.value(i) - second.value(j);
                matrixD[i][j] = GenericTools.min3(matrixD[i - 1][j - 1], matrixD[i][j - 1], matrixD[i - 1][j]) + diff * diff;
                if (tooBig && matrixD[i][j] < cutOffValue)
                    tooBig = false;
            }
            //Early abandon
            if (tooBig)
                return Double.POSITIVE_INFINITY;
        }

        return matrixD[n - 1][m - 1];
    }

    public static double distance(final double[] first, final double[] second, final double cutOffValue) {
        boolean tooBig;
        final int n = first.length;
        final int m = second.length;

        double diff;
        int i, j;

        diff = first[0] - second[0];
        matrixD[0][0] = diff * diff;
        for (i = 1; i < n; i++) {
            diff = first[i] - second[0];
            matrixD[i][0] = matrixD[i - 1][0] + diff * diff;
        }

        for (j = 1; j < m; j++) {
            diff = first[0] - second[j];
            matrixD[0][j] = matrixD[0][j - 1] + diff * diff;
        }

        for (i = 1; i < n; i++) {
            tooBig = true;

            for (j = 1; j < m; j++) {
                diff = first[i] - second[j];
                matrixD[i][j] = GenericTools.min3(matrixD[i - 1][j - 1], matrixD[i][j - 1], matrixD[i - 1][j]) + diff * diff;
                if (tooBig && matrixD[i][j] < cutOffValue)
                    tooBig = false;
            }
            //Early abandon
            if (tooBig)
                return Double.POSITIVE_INFINITY;
        }

        return matrixD[n - 1][m - 1];
    }

    public static double distance(final Sequence first, final Sequence second, final int windowSize, final double cutOffValue) {
        boolean tooBig;
        final int n = first.length;
        final int m = second.length;

        double diff;
        int i, j, jStart, jEnd, indexInfyLeft;

        diff = first.value(0) - second.value(0);
        matrixD[0][0] = diff * diff;
        for (i = 1; i < Math.min(n, 1 + windowSize); i++) {
            diff = first.value(i) - second.value(0);
            matrixD[i][0] = matrixD[i - 1][0] + diff * diff;
        }

        for (j = 1; j < Math.min(m, 1 + windowSize); j++) {
            diff = first.value(0) - second.value(j);
            matrixD[0][j] = matrixD[0][j - 1] + diff * diff;
        }
        if (j < m)
            matrixD[0][j] = Double.POSITIVE_INFINITY;

        for (i = 1; i < n; i++) {
            tooBig = true;
            jStart = Math.max(1, i - windowSize);
            jEnd = Math.min(m, i + windowSize + 1);
            indexInfyLeft = i - windowSize - 1;
            if (indexInfyLeft >= 0)
                matrixD[i][indexInfyLeft] = Double.POSITIVE_INFINITY;

            for (j = jStart; j < jEnd; j++) {
                diff = first.value(i) - second.value(j);
                matrixD[i][j] = GenericTools.min3(matrixD[i - 1][j - 1], matrixD[i][j - 1], matrixD[i - 1][j]) + diff * diff;
                if (tooBig && matrixD[i][j] < cutOffValue)
                    tooBig = false;
            }
            //Early abandon
            if (tooBig)
                return Double.POSITIVE_INFINITY;

            if (j < m)
                matrixD[i][j] = Double.POSITIVE_INFINITY;
        }

        return matrixD[n - 1][m - 1];
    }

    public static int getWindowSize(final int n, final double r) {
        return (int) (r * n);
    }
}
