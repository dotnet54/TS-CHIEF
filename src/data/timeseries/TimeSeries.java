package data.timeseries;

public interface TimeSeries {

    static boolean zNormalize = true;	// NOTE: boss doesnt use z-normalize, so rename this and refactor

    public int dimensions();

    public int length();

    public Integer label();

    public void setLabel(Integer classLabel);

    public double[][] data();

    public double[] data(int dimension);

    public double value(int dimension, int index);

    public double value(int index); //assumes dimension 0

    public void setValue(int dimension, int index, double value);

    public double weight();

    public void setWeight(double weight);

    public double getMean(int dimension);

    public double getStdDev(int dimension);

    public void normalize();

    public void normalize(boolean normMean);

    public void normalize(boolean normMean, double mean, double stdDev);

    public boolean isNormalized();

    public void setIsNormalized(boolean isNormalized);

    public TimeSeries getSubsequence(int offset, int windowSize, boolean zeroMeanNormalize);

    public TimeSeries[] getSubsequences(int windowSize, boolean zeroMeanNormalize);

    public TimeSeries[] getDisjointSequences(int windowSize, boolean normMean);

    public void calcIncrementalMeanStddev(int windowLength, double[] tsData, double[] means, double[] stds);


    // DEV

//    public double[] getMin();

//    public double[] getMax();

//    public double getMin(int dimension);

//    public double getMax(int dimension);

    // z-score normalization
//    public void standardize();

    //min-max normalization
//    public void rescale();

//    public void zNormalize();

}
