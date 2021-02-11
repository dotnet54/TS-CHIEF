package core;

import data.timeseries.Dataset;
import data.timeseries.Indexer;
import data.timeseries.TimeSeries;

public interface Classifier {

    public ClassifierResult fit(Dataset trainData) throws Exception;

    public ClassifierResult fit(Dataset trainData, Indexer trainIndices, DebugInfo debugInfo) throws Exception;

    public ClassifierResult predict(Dataset testData) throws Exception;

    public ClassifierResult predict(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception;

    public ClassifierResult predictProba(Dataset testData) throws Exception;

    public ClassifierResult predictProba(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception;

    public int predict(TimeSeries query) throws Exception;

    public int predict(int testQueryIndex) throws Exception;

    public double predictProba(TimeSeries query) throws Exception;

    public double score(Dataset trainData, Dataset testData) throws Exception;

    public double score(Dataset trainData, Dataset testData, DebugInfo debugInfo) throws Exception;

    public Options getParams();

    public void setParams(Options params);

    public ClassifierResult getTrainResults();

    public ClassifierResult getTestResults();

    // DEV
//    public void fit(Indexer trainIndices) throws Exception;

//    public ClassifierResult predict(Indexer testIndices) throws Exception;

//    public void fit(Indexer trainIndices, DebugInfo debugInfo) throws Exception;

//    public ClassifierResult predict(Indexer testIndices, DebugInfo debugInfo) throws Exception;

}
