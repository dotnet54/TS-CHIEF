package test.measures.imp.changw;

import datasets.Sequence;

public class TWED extends ElasticDistances {
    private final static double[][] D = new double[MAX_SEQ_LENGTH + 1][MAX_SEQ_LENGTH + 1];
    private final static double[] Di1 = new double[MAX_SEQ_LENGTH + 1];
    private final static double[] Dj1 = new double[MAX_SEQ_LENGTH + 1];

    public static double[] twe_nuParams = {
            // <editor-fold defaultstate="collapsed" desc="hidden for space">
            0.00001,
            0.0001,
            0.0005,
            0.001,
            0.005,
            0.01,
            0.05,
            0.1,
            0.5,
            1,// </editor-fold>
    };

    public static double[] twe_lamdaParams = {
            // <editor-fold defaultstate="collapsed" desc="hidden for space">
            0,
            0.011111111,
            0.022222222,
            0.033333333,
            0.044444444,
            0.055555556,
            0.066666667,
            0.077777778,
            0.088888889,
            0.1,// </editor-fold>
    };

    public static double distance(final Sequence first, final Sequence second, final double nu, final double lambda) {
        final int m = first.length;
        final int n = second.length;

        double diff, dist;
        double dmin, htrans;
        int i, j;

        // local costs initializations
        for (j = 1; j <= n; j++) {
            if (j > 1) {
                Dj1[j] = second.value(j - 2) - second.value(j - 1);
                Dj1[j] = Dj1[j] * Dj1[j];
            } else {
                Dj1[j] = second.value(j - 1) * second.value(j - 1);
            }
        }

        for (i = 1; i <= m; i++) {
            if (i > 1) {
                Di1[i] = first.value(i - 2) - first.value(i - 1);
                Di1[i] = Di1[i] * Di1[i];
            } else {
                Di1[i] = first.value(i - 1) * first.value(i - 1);
            }

            for (j = 1; j <= n; j++) {
                D[i][j] = first.value(i - 1) - second.value(j - 1);
                D[i][j] = D[i][j] * D[i][j];
                if (i > 1 && j > 1) {
                    diff = first.value(i - 2) - second.value(j - 2);
                    D[i][j] += diff * diff;
                }
            }
        }

        // border of the cost workday.timeseriesForecast.timeseries.matrix initialization
        D[0][0] = 0;
        for (i = 1; i <= m; i++) {
            D[i][0] = D[i - 1][0] + Di1[i];
        }
        for (j = 1; j <= n; j++) {
            D[0][j] = D[0][j - 1] + Dj1[j];
        }

        for (i = 1; i <= m; i++) {
            for (j = 1; j <= n; j++) {
                htrans = Math.abs(i - j);
                if (j > 1 && i > 1) {
                    htrans *= 2;
                }
                dmin = D[i - 1][j - 1] + nu * htrans + D[i][j];

                dist = Di1[i] + D[i - 1][j] + lambda + nu;
                if (dmin > dist) {
                    dmin = dist;
                }
                dist = Dj1[j] + D[i][j - 1] + lambda + nu;
                if (dmin > dist) {
                    dmin = dist;
                }

                D[i][j] = dmin;
            }
        }

        dist = D[m][n];
        return dist;
    }
}
