package trees.splitters.ee.multivariate;

public interface MultivariateSimilarityMeasure {

    public double distance(double[][] series1, double[][] series2,
                           double bsf, boolean dependentDimensions, int[] dimensionsToUse);

    public double distance_indep(double[] s, double[] t, double bsf);

    public double distance_dep(double[][] s, double[][] t, double bsf, int[] dimensionsToUse);

}
