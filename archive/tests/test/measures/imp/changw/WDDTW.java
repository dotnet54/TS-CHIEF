package test.measures.imp.changw;

import datasets.Sequence;

public class WDDTW extends ElasticDistances{
    public static double distance(final Sequence first, final Sequence second, final double[] weightVector, final boolean transformed) {
        if (transformed) return WDTW.distance(first, second, weightVector);
        return WDTW.distance(DDTW.getDerivative(first), DDTW.getDerivative(second), weightVector);
    }

    public static double distance(final Sequence first, final Sequence second, final double[] weightVector, final double cutOffValue, final boolean transformed) {
        if (transformed) return WDTW.distance(first, second, weightVector, cutOffValue);
        return WDTW.distance(DDTW.getDerivative(first), DDTW.getDerivative(second), weightVector, cutOffValue);
    }
}
