package test.measures.imp.changw;

import datasets.Sequence;
import results.WarpingPathResults;

public class LCSS extends ElasticDistances {
    public static double distance(final Sequence first, final Sequence second, final double epsilon, final int delta) {
        final int m = first.length;
        final int n = second.length;
        int i, j;
        final int[][] matrixD = new int[m + 1][n + 1];

        for (i = 0; i < m; i++) {
            for (j = i - delta; j <= i + delta; j++) {
                if (j < 0) {
                    j = -1;
                } else if (j >= n) {
                    j = i + delta;
                } else if (second.value(j) + epsilon >= first.value(i) &&
                        second.value(j) - epsilon <= first.value(i)) {
                    matrixD[i + 1][j + 1] = matrixD[i][j] + 1;
                } else if (delta == 0) {
                    matrixD[i + 1][j + 1] = matrixD[i][j];
                } else if (matrixD[i][j + 1] > matrixD[i + 1][j]) {
                    matrixD[i + 1][j + 1] = matrixD[i][j + 1];
                } else {
                    matrixD[i + 1][j + 1] = matrixD[i + 1][j];
                }
            }
        }

        int max = -1;
        for (i = 1; i < m + 1; i++) {
            if (matrixD[m][i] > max) {
                max = matrixD[m][i];
            }
        }
        return 1.0 - 1.0 * matrixD[m][n] / m;
    }

    public static WarpingPathResults distanceExt(final Sequence first, final Sequence second, final double epsilon, final int delta) {
        final int m = first.length;
        final int n = second.length;
        int i, j, absIJ;
        final int[][] matrixD = new int[m + 1][n + 1];
        final int[][] minDelta = new int[m + 1][n + 1];

        for (i = 0; i < m; i++) {
            for (j = i - delta; j <= i + delta; j++) {
                if (j < 0) {
                    j = -1;
                } else if (j >= n) {
                    j = i + delta;
                } else if (second.value(j) + epsilon >= first.value(i) &&
                        second.value(j) - epsilon <= first.value(i)) {
                    absIJ = Math.abs(i - j);
                    matrixD[i + 1][j + 1] = matrixD[i][j] + 1;
                    minDelta[i + 1][j + 1] = Math.max(absIJ, minDelta[i][j]);
                } else if (delta == 0) {
                    matrixD[i + 1][j + 1] = matrixD[i][j];
                    minDelta[i + 1][j + 1] = 0;
                } else if (matrixD[i][j + 1] > matrixD[i + 1][j]) {
                    matrixD[i + 1][j + 1] = matrixD[i][j + 1];
                    minDelta[i + 1][j + 1] = minDelta[i][j + 1];
                } else {
                    matrixD[i + 1][j + 1] = matrixD[i + 1][j];
                    minDelta[i + 1][j + 1] = minDelta[i + 1][j];
                }
            }
        }

        int max = -1, maxR = -1;
        for (i = 1; i < m + 1; i++) {
            if (matrixD[m][i] > max) {
                max = matrixD[m][i];
                maxR = minDelta[m][i];
            }
        }
        WarpingPathResults resExt = new WarpingPathResults();
        resExt.distance = 1.0 - 1.0 * matrixD[m][n] / m;
        resExt.distanceFromDiagonal = maxR;
        return resExt;
    }
}
