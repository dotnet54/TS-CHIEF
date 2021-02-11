package data.transformers;

import core.DebugInfo;
import core.Options;
import core.Transform;
import core.exceptions.NotSupportedException;
import data.timeseries.Dataset;
import data.timeseries.MTSDataset;
import data.timeseries.MTimeSeries;
import data.timeseries.TimeSeries;

public class DerivativeTransformer implements Transform {
    @Override
    public void fit(Dataset trainData, Options params, DebugInfo debugInfo) throws Exception {

    }

    @Override
    public Dataset fitTransform(Dataset trainData, Options params, DebugInfo debugInfo) throws Exception {
        return transform(trainData, params, debugInfo);
    }

    @Override
    public Dataset transform(Dataset testData, Options params, DebugInfo debugInfo) throws Exception {
        Dataset transformedDataset = new MTSDataset(testData.size());
        transformedDataset.setName(testData.getName());
        transformedDataset.setTags(testData.getTags());
        int numDimensions = testData.dimensions();
        int seriesLength = testData.length();

        for (int i = 0; i < testData.size(); i++) {
            TimeSeries series = testData.getSeries(i);
            TimeSeries transformedSeries;

            double[][] transformedData = new double[numDimensions][];
            for (int dimension = 0; dimension < numDimensions; dimension++) {
                transformedData[dimension] = new double[seriesLength]; // change to length - 1? derivative series has -1 length
                getDerivative(transformedData[dimension], series.data(dimension));
            }
            transformedSeries = new MTimeSeries(transformedData, series.label());
            transformedDataset.add(transformedSeries);
        }
        transformedDataset.setTags(transformedDataset.getTags() + ",deriv1");

        return transformedDataset;
    }

    @Override
    public Dataset inverseTransform(Dataset testData, Options params, DebugInfo debugInfo) throws Exception {
        throw new NotSupportedException();
    }

    @Override
    public Options getParams() {
        return null;
    }

    @Override
    public void setParams(Options params) {

    }

    public void getDerivative(double[] outputArray, double[] vector) {
        for (int i = 1; i < vector.length - 1 ; i++) {
            outputArray[i] = ((vector[i] - vector[i - 1]) + ((vector[i + 1] - vector[i - 1]) / 2.0)) / 2.0;
        }
        outputArray[0] = outputArray[1];
        outputArray[outputArray.length - 1] = outputArray[outputArray.length - 2];
    }

}
