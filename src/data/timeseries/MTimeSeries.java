// Copyright (c) 2016 - Patrick Schäfer (patrick.schaefer@zib.de)
// Distributed under the GLP 3.0 (See accompanying file LICENSE)

//modified by @a.shifaz 2018

package data.timeseries;

import java.io.Serializable;
import java.util.Arrays;

public class MTimeSeries implements TimeSeries, Serializable {
    private static final long serialVersionUID = 6340030797230203868L;


    protected double[][] data;
    protected Integer label = null;
    protected double weight = 1.0;

//    private boolean refreshAggregates = true;
    protected double mean = Double.NaN;
    protected double stdDev = Double.NaN;
    protected boolean isNormalized = false; // is it already normalized?

    public MTimeSeries(double[][] data, Integer label, double weight) {
        this.data = data;
        this.label = label;
        this.weight = weight;
    }

    public MTimeSeries(double[][] data, Integer label) {
        this(data, label, 1);
    }

    //NOTE: useful constructor but be careful using it, it assumes univariate data
    public MTimeSeries(double[] data, Integer label, double weight) {
        this.data = new double[1][];    //make a 1 dimensional time series
        this.data[0] = data;
        this.label = label;
        this.weight = weight;
    }

    public MTimeSeries(double[] data, Integer label) {
        this(data, label, 1);
    }

    public int dimensions() {
        return this.data.length;
    }

    //assumes all dimensions will have the same length
    public int length() {
        return this.data == null ? 0 : this.data[0].length;
    }

    public Integer label() {
        return this.label;
    }

    public void setLabel(Integer label) {
        this.label = label;
    }

    public double[][] data() {
        return this.data;
    }

    public double[] data(int dim) {
        return this.data[dim];
    }

    public double value(int dim, int i) {
        return this.data[dim][i];
    }

    public double value(int i) {
        return this.data[0][i];
    }

    public void setValue(int dimension, int index, double value) {
        this.data[dimension][index] = value;
    }

    public double weight() {
        return this.weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isNormalized() {
        return this.isNormalized;
    }

    public void setIsNormalized(boolean isNormalized) {
        this.isNormalized = true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.label.toString());
        sb.append("::");

        int max = 5;
        int i, d;
        for (d = 0; d < this.data.length; d++) {
            sb.append(d + ":");
            for (i = 0; i < Math.min(data[0].length, max); i++) {
                sb.append(data[d][i]);
                sb.append(",");
            }

            if (i == max) {
                sb.append("...");
            }
            sb.append(" :");
        }


        return sb.toString();
    }

	public void normalize() {
    	this.normalize(true);
	}

    /**
     * After zero-mean-normalization the following holds: mean = 0 stddev = 1
     *
     * @param normMean defines, if the mean should be subtracted from the time
     *                 series
     */
    public void normalize(boolean normMean) {
        this.mean = getMean(0);    //TODO only using dim 0
        this.stdDev = getStdDev(0);

        if (!isNormalized()) {
            normalize(normMean, this.mean, this.stdDev);
        }
    }

    /**
     * Used for zero-mean normalization.
     *
     * @param normMean defines, if the mean should be subtracted from the time
     *                 series
     * @param mean     the mean to set (usually set to 0)
     * @param stddev   the stddev to set (usually set to 1)
     */
    public void normalize(boolean normMean, double mean, double stddev) {
        this.mean = mean;
        this.stdDev = stddev;
        int dimension = 0; // TODO support only 1 dimension

        if (zNormalize && !isNormalized()) {
            double inverseStddev = (this.stdDev != 0) ? 1.0 / this.stdDev : 1.0;

            if (normMean) {
                for (int i = 0; i < this.data.length; i++) {
                    this.data[dimension][i] = (this.data[dimension][i] - this.mean) * inverseStddev;
                }
                this.mean = 0.0;
            } else if (inverseStddev != 1.0) {
                for (int i = 0; i < this.data.length; i++) {
                    this.data[dimension][i] *= inverseStddev;
                }
            }

            // this.mean = 0.0;
            // this.stddev = 1.0;
            this.isNormalized = true;
        }
    }

    public double getMean(int dimension) {
        this.mean = 0.0;

        // get mean values
        for (double value : data(0)) {
            this.mean += value;
        }
        this.mean /= (double) this.data.length;

        return this.mean;
    }

    public double getStdDev(int dimension) {
        this.stdDev = 0;

        // stddev
        double var = 0;
        for (double value : data(dimension)) {
            var += value * value;
        }

        double norm = 1.0 / ((double) this.data.length);
        double buf = norm * var - this.mean * this.mean;
        if (buf > 0) {
            this.stdDev = Math.sqrt(buf);
        }

        return this.stdDev;
    }


    public TimeSeries getSubsequence(int offset, int windowSize, boolean zeroMeanNormalize) {
        double[][] subsequenceData = new double[data.length][];
        for (int dimension = 0; dimension < data.length; dimension++) {
            subsequenceData[dimension] = Arrays.copyOfRange(data[dimension], offset, Math.min(data[dimension].length, offset + windowSize));
        }
        TimeSeries sequence = new MTimeSeries(subsequenceData, label);
        sequence.normalize(zeroMeanNormalize);
        return sequence;
    }


    public TimeSeries[] getSubsequences(int windowSize, boolean zeroMeanNormalize) {
        // windowSize should not be larger than the data size
        int ws = Math.min(windowSize, this.data.length);

        // extract subsequences
        int size = (this.data[0].length - ws) + 1;
        TimeSeries[] subsequences = new MTimeSeries[size];

        for (int i = 0; i < subsequences.length; i++) {
            double subsequenceData[][] = new double[data.length][windowSize];
            for (int dimension = 0; dimension < data.length; dimension++) {
                double[] means = new double[size];
                double[] stddevs = new double[size];

                //NOTE normalization per dimension
                calcIncrementalMeanStddev(windowSize, data[dimension], means, stddevs);
                System.arraycopy(this.data[dimension], i, subsequenceData[dimension], 0, ws);

                // The newly created time series have queryLength windowSize and offset i
                subsequences[i] = new MTimeSeries(subsequenceData, label);
                subsequences[i].normalize(zeroMeanNormalize, means[i], stddevs[i]);
            }
        }

        return subsequences;
    }

    public TimeSeries[] getDisjointSequences(int windowSize, boolean zeroMeanNormalize) {

        // extract subsequences
        int numSequences = length() / windowSize;
        TimeSeries[] subsequences = new MTimeSeries[numSequences];

        for (int i = 0; i < numSequences; i++) {
            double subsequenceData[][] = new double[data.length][windowSize];
            for (int dimension = 0; dimension < data.length; dimension++) {
                System.arraycopy(this.data[dimension], i * windowSize, subsequenceData[dimension], 0, windowSize);
            }
            subsequences[i] = new MTimeSeries(subsequenceData, label);
            subsequences[i].normalize(zeroMeanNormalize);
        }

        return subsequences;
    }

    public void calcIncrementalMeanStddev(int windowLength, double[] tsData, double[] means, double[] stds) {
    	double sum = 0;
        double squareSum = 0;

        // it is faster to multiply than to divide
        double rWindowLength = 1.0 / (double) windowLength;

        for (int ww = 0; ww < Math.min(tsData.length, windowLength); ww++) {
            sum += tsData[ww];
            squareSum += tsData[ww] * tsData[ww];
        }

        // first window
        means[0] = sum * rWindowLength;
        double buf = squareSum * rWindowLength - means[0] * means[0];
        stds[0] = buf > 0 ? Math.sqrt(buf) : 0;

        // remaining windows
        for (int w = 1, end = tsData.length - windowLength + 1; w < end; w++) {
            sum += tsData[w + windowLength - 1] - tsData[w - 1];
            means[w] = sum * rWindowLength;

            squareSum += tsData[w + windowLength - 1] * tsData[w + windowLength - 1] - tsData[w - 1] * tsData[w - 1];
            buf = squareSum * rWindowLength - means[w] * means[w];
            stds[w] = buf > 0 ? Math.sqrt(buf) : 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TimeSeries)) {
            throw new RuntimeException("Both objects should be time series: " + (o.toString()));
        }

        TimeSeries ts = (TimeSeries) o;
        if (ts.dimensions() != this.data.length) {
            return false;
        }

        if (ts.length() != this.length()) {
            return false;
        }

        if (!this.label.equals(ts.label())) {
            return false;
        }

        // Only compare normed time series
        if (ts.isNormalized() != this.isNormalized()) {
            throw new RuntimeException("Please normalize both time series before checking equality");
        }

        // NOTE not checking equality of weights

        // NOTE computationaly expensive when equal
        for (int dimension = 0; dimension < this.data.length; dimension++) {
            for (int i = 0; i < this.data[dimension].length; i++) {
                if (this.data[dimension][i] != ts.value(dimension, i)) {
                    return false;
                }
            }
        }

        return true;
    }

}
