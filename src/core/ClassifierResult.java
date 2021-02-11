package core;

import data.timeseries.Dataset;

import java.util.concurrent.atomic.AtomicInteger;

public class ClassifierResult {

    protected transient Classifier model;
    protected int[] predictedLabels; // supports a matrix of predicted labels (for ensembles)
    public AtomicInteger correct;
    public long trainTime;

    public ClassifierResult(){
        this.correct = new AtomicInteger();
    }

    public ClassifierResult(Classifier model){
        this.correct = new AtomicInteger();
        this.model = model;
    }

    public ClassifierResult(Classifier model, Dataset testData){
        this.correct = new AtomicInteger();
        this.model = model;
        allocateForPredictionResults(testData);
    }

    // for delayed memory allocations
    // remove in future -- temporary method
    public void initialize(Dataset dataset){
        //pass

        predictedLabels = new int[dataset.size()];
    }

    //TODO move to initialize?
    public int[] allocateForPredictionResults(Dataset testData){
        predictedLabels = new int[testData.size()];
        return predictedLabels;
    }

    public int[] getPredctictedLabels(){
        return this.predictedLabels;
    }


    public void setPredictedLabels(int[] predictedLabels) {
        this.predictedLabels = predictedLabels;
    }

    public int getNumCorrectPredictions(){
        return this.correct.get();
    }

    public double getAccuracyScore(){
        return ((double) getNumCorrectPredictions()) / this.predictedLabels.length;
    }

}
