package test.measures.imp.changw;

import datasets.Sequence;
import datasets.Sequences;
import results.WarpingPathResults;

public class DDTW extends ElasticDistances {
    public static Sequences getDerivative(final Sequences data) {
        final Sequences output = new Sequences(data.size(), data.length());

        for (int i = 0; i < data.size(); i++) { // for each data
            final Sequence sequence = data.getInstance(i);
            final double[] rawData = sequence.data;
            final double[] derivative = getDerivative(rawData); // class value has now been removed - be careful!
            final Sequence toAdd = new Sequence(sequence.label, derivative);
            output.add(toAdd);
        }
        return output;
    }

    public static Sequence getDerivative(final Sequence input) {
        final double[] derivative = new double[input.length];

        for (int i = 1; i < input.length - 1; i++) {
            derivative[i] = ((input.value(i) - input.value(i - 1)) + ((input.value(i + 1) - input.value(i - 1)) / 2)) / 2;
        }

        derivative[0] = derivative[1];
        derivative[derivative.length - 1] = derivative[derivative.length - 2];

        return new Sequence(input.label, derivative);
    }

    public static double[] getDerivative(final double[] input) {
        final double[] derivative = new double[input.length];

        for (int i = 1; i < input.length - 1; i++) {
            derivative[i] = ((input[i] - input[i - 1]) + ((input[i + 1] - input[i - 1]) / 2)) / 2;
        }

        derivative[0] = derivative[1];
        derivative[derivative.length - 1] = derivative[derivative.length - 2];

        return derivative;
    }

    public static WarpingPathResults distanceExt(final Sequence first, final Sequence second, final int windowSize, final boolean transformed) {
        if (transformed) return DTW.distanceExt(first, second, windowSize);
        return DTW.distanceExt(getDerivative(first), getDerivative(second), windowSize);
    }

    public static WarpingPathResults distanceExt(final Sequence first, final Sequence second, final int windowSize, final double cutOffValue, final boolean transformed) {
        if (transformed) return DTW.distanceExt(first, second, windowSize, cutOffValue);
        return DTW.distanceExt(getDerivative(first), getDerivative(second), windowSize, cutOffValue);
    }

    public static double distance(final Sequence first, final Sequence second, final boolean transformed) {
        if (transformed) return DTW.distance(first, second);
        return DTW.distance(getDerivative(first), getDerivative(second));
    }

    public static double distance(final Sequence first, final Sequence second, final int windowSize, final boolean transformed) {
        if (transformed) return DTW.distance(first, second, windowSize);
        return DTW.distance(getDerivative(first), getDerivative(second), windowSize);
    }

    public static double distance(final Sequence first, final Sequence second, final double cutOffValue, final boolean transformed) {
        if (transformed) return DTW.distance(first, second, cutOffValue);
        return DTW.distance(getDerivative(first), getDerivative(second), cutOffValue);
    }

    public static double distance(final Sequence first, final Sequence second, final int windowSize, final double cutOffValue, final boolean transformed) {
        if (transformed) return DTW.distance(first, second, windowSize, cutOffValue);
        return DTW.distance(getDerivative(first), getDerivative(second), windowSize, cutOffValue);
    }

    public static int getWindowSize(final int n, final double r) {
        return DTW.getWindowSize(n, r);
    }
}
