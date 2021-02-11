package knn;

import core.*;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import data.timeseries.TimeSeries;

public class ElasticEnsemble implements Classifier, Ensemble {
    @Override
    public ClassifierResult fit(Dataset trainData) throws Exception {
        return null;
    }

    @Override
    public ClassifierResult fit(Dataset trainData, Indexer trainIndices, DebugInfo debugInfo) throws Exception {
        return null;
    }

    @Override
    public ClassifierResult predict(Dataset testData) throws Exception {
        return null;
    }

    @Override
    public ClassifierResult predict(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception {
        return null;
    }

    @Override
    public ClassifierResult predictProba(Dataset testData) throws Exception {
        return null;
    }

    @Override
    public ClassifierResult predictProba(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception {
        return null;
    }

    @Override
    public int predict(TimeSeries query) throws Exception {
        return 0;
    }

    @Override
    public int predict(int queryIndex) throws Exception {
        return 0;
    }

    @Override
    public double predictProba(TimeSeries query) throws Exception {
        return 0;
    }

    @Override
    public double score(Dataset trainData, Dataset testData) throws Exception {
        return 0;
    }

    @Override
    public double score(Dataset trainData, Dataset testData, DebugInfo debugInfo) throws Exception {
        return 0;
    }

    @Override
    public Options getParams() {
        return null;
    }

    @Override
    public void setParams(Options params) {

    }

    @Override
    public ClassifierResult getTrainResults() {
        return null;
    }

    @Override
    public ClassifierResult getTestResults() {
        return null;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public Classifier[] getModels() {
        return new Classifier[0];
    }

    @Override
    public Classifier getModel(int i) {
        return null;
    }
}
