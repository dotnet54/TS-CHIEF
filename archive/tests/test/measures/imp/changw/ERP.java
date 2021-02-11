package test.measures.imp.changw;

import datasets.Sequence;
import results.WarpingPathResults;

public class ERP extends ElasticDistances {
    private static double[] prev = new double[MAX_SEQ_LENGTH];
    private static double[] curr = new double[MAX_SEQ_LENGTH];
    private final static int[][] minDistanceToDiagonal = new int[MAX_SEQ_LENGTH][MAX_SEQ_LENGTH];

    public static WarpingPathResults distanceExt(Sequence first, Sequence second, final double g, final double bandSize) {
        final int m = first.length;
        final int n = second.length;
        final int band = getBandSize(bandSize, m);
        double diff, d1, d2, d12, cost;
        int i, j, left, right, absIJ;

        Sequence tmp;
        if (n < m) {
            tmp = first;
            first = second;
            second = tmp;
        }

        minDistanceToDiagonal[0][0] = 0;
        for (i = 0; i < m; i++) {
            // Swap current and prev arrays. We'll just overwrite the new curr.
            double[] temp = prev;
            prev = curr;
            curr = temp;

            left = i - (band + 1);
            if (left < 0) {
                left = 0;
            }
            right = i + (band + 1);
            if (right > (m - 1)) {
                right = (m - 1);
            }

            for (j = left; j <= right; j++) {
                absIJ = Math.abs(i - j);
                if (absIJ <= band) {
                    diff = first.value(i) - g;
                    d1 = (diff * diff);

                    diff = g - second.value(j);
                    d2 = (diff * diff);

                    diff = first.value(i) - second.value(j);
                    d12 = (diff * diff);

                    if ((i + j) != 0) {
                        if ((i == 0) || ((j != 0) &&
                                (((prev[j - 1] + d12) >= (curr[j - 1] + d2)) &&
                                        ((curr[j - 1] + d2) <= (prev[j] + d1))))) {
                            // del
                            cost = curr[j - 1] + d2;
                            minDistanceToDiagonal[i][j] = Math.max(absIJ, minDistanceToDiagonal[i][j - 1]);
                        } else if (j == 0 || prev[j - 1] + d12 >= prev[j] + d1 && prev[j] + d1 <= curr[j - 1] + d2) {
                            // ins
                            cost = prev[j] + d1;
                            minDistanceToDiagonal[i][j] = Math.max(absIJ, minDistanceToDiagonal[i - 1][j]);
                        } else {
                            // match
                            cost = prev[j - 1] + d12;
                            minDistanceToDiagonal[i][j] = Math.max(absIJ, minDistanceToDiagonal[i - 1][j - 1]);
                        }
                    } else {
                        cost = 0;
                        minDistanceToDiagonal[i][j] = 0;
                    }

                    curr[j] = cost;
                    // steps[i][j] = step;
                } else {
                    curr[j] = Double.POSITIVE_INFINITY; // outside band
                }
            }
        }

        WarpingPathResults resExt = new WarpingPathResults();
        resExt.distance = curr[m - 1];
        resExt.distanceFromDiagonal = minDistanceToDiagonal[n - 1][m - 1];
        return resExt;
    }

    public static double distance(Sequence first, Sequence second, final double g, final double bandSize) {
        final int m = first.length;
        final int n = second.length;
        final int band = getBandSize(bandSize, m);
        double diff, d1, d2, d12, cost;
        int i, j, left, right, absIJ;

        Sequence tmp;
        if (n < m) {
            tmp = first;
            first = second;
            second = tmp;
        }

        for (i = 0; i < m; i++) {
            double[] temp = prev;
            prev = curr;
            curr = temp;

            left = i - (band + 1);
            if (left < 0) {
                left = 0;
            }
            right = i + (band + 1);
            if (right > (m - 1)) {
                right = (m - 1);
            }

            for (j = left; j <= right; j++) {
                absIJ = Math.abs(i - j);
                if (absIJ <= band) {
                    diff = first.value(i) - g;
                    d1 = (diff * diff);

                    diff = g - second.value(j);
                    d2 = (diff * diff);

                    diff = first.value(i) - second.value(j);
                    d12 = (diff * diff);

                    if ((i + j) != 0) {
                        if ((i == 0) || ((j != 0) &&
                                (((prev[j - 1] + d12) >= (curr[j - 1] + d2)) &&
                                        ((curr[j - 1] + d2) <= (prev[j] + d1))))) {
                            cost = curr[j - 1] + d2;
                        } else if (j == 0 || prev[j - 1] + d12 >= prev[j] + d1 && prev[j] + d1 <= curr[j - 1] + d2) {
                            cost = prev[j] + d1;
                        } else {
                            cost = prev[j - 1] + d12;
                        }
                    } else {
                        cost = 0;
                    }

                    curr[j] = cost;
                } else {
                    curr[j] = Double.POSITIVE_INFINITY; // outside band
                }
            }
        }

        return (curr[m - 1]);
    }

    public static int getBandSize(double bandSize, int n) {
        return (int) Math.ceil(bandSize * n);
    }
}
