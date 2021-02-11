package test.measures.imp.changw;

import datasets.Sequence;

public class MSM extends ElasticDistances {
    private final static double[][] matrixD = new double[MAX_SEQ_LENGTH][MAX_SEQ_LENGTH];

    public static double[] msmParams = {
            // <editor-fold defaultstate="collapsed" desc="hidden for space">
            0.01,
            0.01375,
            0.0175,
            0.02125,
            0.025,
            0.02875,
            0.0325,
            0.03625,
            0.04,
            0.04375,
            0.0475,
            0.05125,
            0.055,
            0.05875,
            0.0625,
            0.06625,
            0.07,
            0.07375,
            0.0775,
            0.08125,
            0.085,
            0.08875,
            0.0925,
            0.09625,
            0.1,
            0.136,
            0.172,
            0.208,
            0.244,
            0.28,
            0.316,
            0.352,
            0.388,
            0.424,
            0.46,
            0.496,
            0.532,
            0.568,
            0.604,
            0.64,
            0.676,
            0.712,
            0.748,
            0.784,
            0.82,
            0.856,
            0.892,
            0.928,
            0.964,
            1,
            1.36,
            1.72,
            2.08,
            2.44,
            2.8,
            3.16,
            3.52,
            3.88,
            4.24,
            4.6,
            4.96,
            5.32,
            5.68,
            6.04,
            6.4,
            6.76,
            7.12,
            7.48,
            7.84,
            8.2,
            8.56,
            8.92,
            9.28,
            9.64,
            10,
            13.6,
            17.2,
            20.8,
            24.4,
            28,
            31.6,
            35.2,
            38.8,
            42.4,
            46,
            49.6,
            53.2,
            56.8,
            60.4,
            64,
            67.6,
            71.2,
            74.8,
            78.4,
            82,
            85.6,
            89.2,
            92.8,
            96.4,
            100// </editor-fold>
    };

    public static double distance(final Sequence first, final Sequence second, final double c) {
        final int m = first.length;
        final int n = second.length;
        int i, j;
        double d1, d2, d3;

        // Initialization
        matrixD[0][0] = Math.abs(first.value(0) - second.value(0));
        for (i = 1; i < m; i++) {
            matrixD[i][0] = matrixD[i - 1][0] + editCost(first.value(i), first.value(i - 1), second.value(0), c);
        }
        for (i = 1; i < n; i++) {
            matrixD[0][i] = matrixD[0][i - 1] + editCost(second.value(i), first.value(0), second.value(i - 1), c);
        }

        // Main Loop
        for (i = 1; i < m; i++) {
            for (j = 1; j < n; j++) {
                d1 = matrixD[i - 1][j - 1] + Math.abs(first.value(i) - second.value(j));
                d2 = matrixD[i - 1][j] + editCost(first.value(i), first.value(i - 1), second.value(j), c);
                d3 = matrixD[i][j - 1] + editCost(second.value(j), first.value(i), second.value(j - 1), c);
                matrixD[i][j] = Math.min(d1, Math.min(d2, d3));
            }
        }
        // Output
        return matrixD[m - 1][n - 1];
    }

    public static double distance(final Sequence first, final Sequence second, final double c, final double cutOffValue) {
        final int m = first.length;
        final int n = second.length;
        int i, j;
        double d1, d2, d3;
        double min;

        // Initialization
        matrixD[0][0] = Math.abs(first.value(0) - second.value(0));
        for (i = 1; i < m; i++) {
            matrixD[i][0] = matrixD[i - 1][0] + editCost(first.value(i), first.value(i - 1), second.value(0), c);
        }
        for (i = 1; i < n; i++) {
            matrixD[0][i] = matrixD[0][i - 1] + editCost(second.value(i), first.value(0), second.value(i - 1), c);
        }

        // Main Loop
        for (i = 1; i < m; i++) {
            min = cutOffValue;
            for (j = 1; j < n; j++) {
                d1 = matrixD[i - 1][j - 1] + Math.abs(first.value(i) - second.value(j));
                d2 = matrixD[i - 1][j] + editCost(first.value(i), first.value(i - 1), second.value(j), c);
                d3 = matrixD[i][j - 1] + editCost(second.value(j), first.value(i), second.value(j - 1), c);
                matrixD[i][j] = Math.min(d1, Math.min(d2, d3));

                if (matrixD[i][j] >= cutOffValue) {
                    matrixD[i][j] = Double.MAX_VALUE;
                }

                if (matrixD[i][j] < min) {
                    min = matrixD[i][j];
                }
            }
            if (min >= cutOffValue) {
                return Double.MAX_VALUE;
            }
        }
        // Output
        return matrixD[m - 1][n - 1];
    }

    private static double editCost(final double new_point, final double x, final double y, final double c) {
        if (((x <= new_point) && (new_point <= y)) || ((y <= new_point) && (new_point <= x))) {
            return c;
        } else {
            return c + Math.min(Math.abs(new_point - x), Math.abs(new_point - y));
        }
    }
}
