package test.measures.imp.changw;

import datasets.Sequence;

public class Euclidean {
    public static double distance(final Sequence first, final Sequence second) {
        final int n = first.length;
        final int m = second.length;
        final int minLen = Math.min(n, m);

        double dist = 0;
        for (int i = 0; i < minLen; i++) {
            final double diff = first.value(i) - second.value(i);
            dist += diff * diff;
        }
        return dist;
    }

    public static double distance(final Sequence first, final Sequence second, final double cutOffValue) {
        final int n = first.length;
        final int m = second.length;
        final int minLen = Math.min(n, m);

        double dist = 0;
        for (int i = 0; i < minLen; i++) {
            final double diff = first.value(i) - second.value(i);
            dist += diff * diff;
            if (dist > cutOffValue)
                return Double.POSITIVE_INFINITY;
        }
        return dist;
    }

    public static double distance(final double[] first, final double[] second) {
        final int n = first.length;
        final int m = second.length;
        final int minLen = Math.min(n, m);

        double dist = 0;
        for (int i = 0; i < minLen; i++) {
            final double diff = first[i] - second[i];
            dist += diff * diff;
        }
        return dist;
    }

    public static double distance(final double[] first, final double[] second, final double cutOffValue) {
        final int n = first.length;
        final int m = second.length;
        final int minLen = Math.min(n, m);

        double dist = 0;
        for (int i = 0; i < minLen; i++) {
            final double diff = first[i] - second[i];
            dist += diff * diff;
            if (dist > cutOffValue)
                return Double.POSITIVE_INFINITY;
        }
        return dist;
    }
}
