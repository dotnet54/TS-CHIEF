package knn;

import core.Classifier;
import core.ClassifierResult;
import core.DebugInfo;
import core.Options;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import data.timeseries.TimeSeries;
import trees.splitters.ee.measures.DistanceMeasure;
import trees.splitters.ee.measures.MEASURE;

import java.util.Random;

public class uniKNN implements Classifier {

    protected int k;
    protected DistanceMeasure measure;
    protected MEASURE measureName;
    protected Dataset trainData;
    protected int[] dimensionsToUse;
    protected int independentDimensionCombineUsingLpNorm = 2;
    protected int numJobs;
    protected int lpNorm = 2;
    protected ParallelTasks parallelTasks;
    protected long randSeed = 0;
    protected Random rand;

    //sim measure params
    protected boolean useRandomParams = false;
    public int windowSize = 0;
    public double wdtwG = 0;
    public double epsilon = 0;
    public double msmCost = 0;
    public double erpG = 0;
    public double tweNu = 0;
    public double tweLambda = 0;

    /***
     * TODO
     * k > 1
     * random tie break
     * params
     */

//    public KNN(){
//        this(1, MEASURE.euclidean, false, 1, 0);
//    }
//
//    public KNN(MEASURE measureName, boolean dependentDimensions, int maxDimensionsToUse){
//        this(1, measureName, dependentDimensions, maxDimensionsToUse, 0);
//    }

//    public uniKNN(int k, MEASURE measureName, boolean dependentDimensions, int maxDimensionsToUse, int numJobs){
//        this.k = k;
//        this.measureName = measureName;
//        this.dependentDimensions = dependentDimensions;
//        this.maxDimensionsToUse = maxDimensionsToUse;
//
//        if (numJobs <= 0) { // auto
//            numJobs = Runtime.getRuntime().availableProcessors();
//        }
//        this.numJobs = numJobs;
//        this.parallelTasks = new ParallelTasks(numJobs);
//    }

    public uniKNN(int k, MEASURE measureName, int[] dimensionsToUse, int numJobs, int randSeed){
        this.k = k;
        this.measureName = measureName;
        this.dimensionsToUse = dimensionsToUse;
        this.randSeed = randSeed;
        this.rand = new Random(randSeed);

        if (numJobs <= 0) { // auto
            numJobs = Runtime.getRuntime().availableProcessors();
        }
        this.numJobs = numJobs;
        this.parallelTasks = new ParallelTasks(numJobs);
    }


    @Override
    public void fit(Dataset trainData) throws Exception {
        this.trainData = trainData;
        this.measure = new DistanceMeasure(measureName, null);

        if (useRandomParams){
            measure.select_random_params(trainData, rand);
        }else{
            measure.setWindowSizeDTW(windowSize);
            measure.setWindowSizeDDTW(windowSize);
            measure.setWindowSizeLCSS(windowSize);
            measure.setWindowSizeERP(windowSize);

            measure.setWeigthWDTW(wdtwG);
            measure.setWeigthWDDTW(wdtwG);

            measure.setEpsilonLCSS(epsilon);
            measure.setGvalERP(erpG);
            measure.setCMSM(msmCost);

            measure.setNuTWE(tweNu);
            measure.setLambdaTWE(tweLambda);
        }

    }

    @Override
    public void fit(Dataset trainData, Indexer trainIndices, DebugInfo debugInfo) throws Exception {

    }

    @Override
    public ClassifierResult predict(Dataset testData) throws Exception {

        if (this.numJobs > 1){
            return this.predictInParallel(testData);
        }

        int trainSize = trainData.size();
        int testSize = testData.size();
        double distance;
        double min_distance;
        int predictedLabel = trainData.getSeries(0).label();


        ClassifierResult result = new ClassifierResult();
        result.allocateForPredictionResults(testData);
        ClassifierResult.Predictions predictions = result.getPredctictedLabels();

        for (int i = 0; i < testSize; i++) {
            TimeSeries q = testData.getSeries(i);
            min_distance = Double.POSITIVE_INFINITY;
            for (int j = 0; j < trainSize; j++) {
                TimeSeries s1 = trainData.getSeries(j);

                if (s1 == q){ // check -- for case testData == trainData -- use .equals()
                    continue;
                }

                distance = 0;
                for (int dimension : dimensionsToUse) {
                    distance += measure.distance(s1.data()[dimension], q.data()[dimension], Double.POSITIVE_INFINITY);
                }

                if (distance < min_distance){
                    min_distance = distance;
                    predictedLabel = s1.label();
                }
            }
            predictions.predictedLabels[i] = predictedLabel;
            if (q.label().equals(predictedLabel)){
                predictions.correct.addAndGet(1);
            }
        }
        return result;
    }

    private ClassifierResult predictInParallel(Dataset testData) throws Exception {
        int testSize = testData.size();
        ClassifierResult result = new ClassifierResult();
        result.allocateForPredictionResults(testData);
        ClassifierResult.Predictions predictions = result.getPredctictedLabels();

        predictions.predictedLabels = this.parallelTasks.runPredictionTasks(this, testData);

        for (int i = 0; i < testSize; i++) {
            TimeSeries q = testData.getSeries(i);
            if (q.label().equals(predictions.predictedLabels[i])){
                predictions.correct.addAndGet(1);
            }
        }

        this.parallelTasks.getExecutor().shutdown();
        return result;
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
        int trainSize = trainData.size();
        double distance;
        double min_distance = Double.POSITIVE_INFINITY;
        int predictedLabel = trainData.getClass(0);

        for (int j = 0; j < trainSize; j++) {
            TimeSeries series = trainData.getSeries(j);

            if (series == query){ // check -- for case testData == trainData -- use .equals()
                continue;
            }

            distance = 0;
            for (int dimension : dimensionsToUse) {
                distance += measure.distance(series.data()[dimension], query.data()[dimension], Double.POSITIVE_INFINITY);
            }

            if (distance < min_distance){
                min_distance = distance;
                predictedLabel = series.label();
            }
        }

        return predictedLabel;
    }

    @Override
    public double predictProba(TimeSeries query) throws Exception {
        return -1;
    }

    @Override
    public double score(Dataset trainData, Dataset testData) throws Exception {
        return -1;
    }

    @Override
    public double score(Dataset trainData, Dataset testData, DebugInfo debugInfo) throws Exception {
        return -1;
    }

    @Override
    public Options getParams() {
        return null;
    }

    @Override
    public void setParams(Options params) {

    }

    public DistanceMeasure getMeasure() {
        return measure;
    }

    public String getParamString(){
        StringBuilder sb = new StringBuilder();
        sb.append("\"[w:");
        sb.append(windowSize);
        sb.append(",e:");
        sb.append(epsilon);
        sb.append(",wg:");
        sb.append(wdtwG);
        sb.append(",eg:");
        sb.append(erpG);
        sb.append(",c:");
        sb.append(msmCost);
        sb.append(",n:");
        sb.append(tweNu);
        sb.append(",l:");
        sb.append(tweLambda);
        sb.append("]\"");
        return sb.toString();
    }

    public void setDimensionsToUse(int[] dimensionsToUse) {
        this.dimensionsToUse = dimensionsToUse;
    }

    public void setIndependentDimensionCombineUsingLpNorm(int independentDimensionCombineUsingLpNorm) {
        this.independentDimensionCombineUsingLpNorm = independentDimensionCombineUsingLpNorm;
    }

    public void setNumJobs(int numJobs) {
        this.numJobs = numJobs;
    }
}
