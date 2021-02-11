package knn;

import application.test.knn.MultivarKNNArgs;
import core.Classifier;
import core.DebugInfo;
import core.Options;
import data.io.CsvWriter;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import data.timeseries.TimeSeries;
import distance.univariate.MEASURE;
import distance.multivariate.*;
import util.Sampler;
import util.math.doubles.StatisticsDbl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class KNN implements Classifier {

    protected int k;
    protected MultivarSimMeasure measure;
    protected MEASURE measureName;
    protected Dataset trainData;
    protected boolean dependentDimensions;
    protected int maxDimensionsToUse;
    protected int[] dimensionsToUse;
    protected double lpDistanceOrderForIndependentDims;
    protected double lpDistanceOrderForDependentDims;
    protected ParallelTasks parallelTasks;
    protected long randSeed = 0;
    protected Random rand;

    protected boolean isFitted;
    protected KNNTrainResult trainResult;
    protected KNNTestResult testResult;

    //sim measure params
    protected boolean useRandomParams = false;
    public int windowSize = 0; //DTW(w), DDTW(w), LCSS(w), ERP(w)
    public double epsilon = 0; // LCSS(c)
    public double penalty = 0; // WDTW(g), TWE(nu)
    public double cost = 0; // MSM(c), ERP(g), TWE(lambda)

    // TODO temp solution --  in future use MultivarKNNArgs for mEE args, then use Options class for knn
    public MultivarKNNArgs args;
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



    public KNN(int k, MEASURE measureName, boolean dependentDimensions, int maxDimensionsToUse,
               boolean useRandomParams, int numJobs, int randSeed) throws Exception {
        this.k = k;
        this.measureName = measureName;
        this.dependentDimensions = dependentDimensions;
        this.maxDimensionsToUse = maxDimensionsToUse;
        this.randSeed = randSeed;
        this.rand = new Random(randSeed);
        this.useRandomParams = useRandomParams;

        if (numJobs <= 0) { // auto
            numJobs = Runtime.getRuntime().availableProcessors();
        }
        this.parallelTasks = new ParallelTasks(numJobs);

        if (trainData.isMultivariate()){
            this.measure = MultivarSimMeasure.createSimilarityMeasure(measureName,
                    dependentDimensions, dimensionsToUse);
        }else{
//            this.dm = new DistanceMeasure(measure, null);
        }
    }

    public KNN(int k, MEASURE measureName, boolean dependentDimensions, int[] dimensionsToUse,
               boolean useRandomParams, int numJobs, int randSeed) throws Exception {
        this.k = k;
        this.measureName = measureName;
        this.dependentDimensions = dependentDimensions;
        this.dimensionsToUse = dimensionsToUse;
        this.randSeed = randSeed;
        this.rand = new Random(randSeed);
        this.useRandomParams = useRandomParams;

        if (numJobs <= 0) { // auto
            numJobs = Runtime.getRuntime().availableProcessors();
        }
        this.parallelTasks = new ParallelTasks(numJobs);

//        if (trainData.isMultivariate()){
            this.measure = MultivarSimMeasure.createSimilarityMeasure(measureName,
                    dependentDimensions, dimensionsToUse);
//        }else{
////            this.dm = new DistanceMeasure(measure, null);
//        }
    }

    public KNN(MEASURE measureName, int numJobs) throws Exception {
        this.k = 1;
        this.measureName = measureName;
        this.dependentDimensions = false;
        this.dimensionsToUse = null;
        this.randSeed = System.nanoTime();
        this.rand = new Random(randSeed);
        this.useRandomParams = false;
        this.parallelTasks = new ParallelTasks(numJobs);

//        if (trainData.isMultivariate()){
            this.measure = MultivarSimMeasure.createSimilarityMeasure(measureName,
                    dependentDimensions, dimensionsToUse);
//        }else{
////            this.dm = new DistanceMeasure(measure, null);
//        }
    }

    @Override
    public KNNTrainResult fit(Dataset trainData) throws Exception {
        trainResult = new KNNTrainResult(this, trainData);

        this.trainData = trainData;
        if (maxDimensionsToUse == 0){
            maxDimensionsToUse = trainData.dimensions();
        }
        if (dimensionsToUse == null){
            this.dimensionsToUse = Sampler.sampleNRandomIntsFromRange(0, trainData.dimensions(), maxDimensionsToUse);
        }
        this.measure.setDimensionsToUse(this.dimensionsToUse);
        this.measure.setDependentDimensions(this.dependentDimensions);
        this.measure.setpForIndependentDims(this.lpDistanceOrderForIndependentDims);
        this.measure.setpForDependentDims(this.lpDistanceOrderForDependentDims);

        if (useRandomParams){
            measure.setRandomParams(trainData, rand);
        }else{
//            if (measure instanceof DDTW){ //test sub class first
//                ((DDTW) measure).setWindowSize(windowSize);
//            }else if (measure instanceof DTW){
//                ((DTW) measure).setWindowSize(windowSize);
//            }if (measure instanceof WDDTW){ //test sub class first
//                ((WDDTW) measure).setG(wdtwG, trainData.length());
//            }if (measure instanceof WDTW){
//                ((WDTW) measure).setG(wdtwG, trainData.length());
//            }if (measure instanceof LCSS){
//                ((LCSS) measure).setWindowSize(windowSize);
//                ((LCSS) measure).setEpsilon(epsilon);
//            }if (measure instanceof MSM){
//                ((MSM) measure).setCost(msmCost);
//            }if (measure instanceof ERP){
//                ((ERP) measure).setWindowSize(windowSize);
//                ((ERP) measure).setG(erpG);
//            }if (measure instanceof TWE){
//                ((TWE) measure).setNu(tweNu);
//                ((TWE) measure).setLambda(tweLambda);
//            }
        }
        measure.debugResult = trainResult;
        isFitted = true;
        return trainResult;
    }

    @Override
    public KNNTrainResult fit(Dataset trainData, Indexer trainIndices, DebugInfo debugInfo) throws Exception {
        return null;
    }

    @Override
    public KNNTestResult predict(Dataset testData) throws Exception {
        testResult = new KNNTestResult(this, testData);
        testResult.testData = testData;

        if (this.parallelTasks.numThreads != 1){
            return this.predictInParallel(testData);
        }else{
            long startTime = System.nanoTime();
            int testSize = testData.size();
            int[] predictedLabels = testResult.getPredctictedLabels();

            for (int i = 0; i < testSize; i++) {
                predictedLabels[i] = this.predict(i);
                TimeSeries q = testData.getSeries(i);
                if (q.label().equals(predictedLabels[i])){
                    testResult.correct.addAndGet(1);
                }
            }

            testResult.testTime = (System.nanoTime() - startTime);
            return testResult;
        }
    }

    private KNNTestResult predictInParallel(Dataset testData) throws Exception {
        long startTime = System.nanoTime();
        int testSize = testData.size();
        int[] predictedLabels = this.parallelTasks.runPredictionTasks(this, testData);;
        testResult.setPredictedLabels(predictedLabels);

        for (int i = 0; i < testSize; i++) {
            TimeSeries q = testData.getSeries(i);
            if (q.label().equals(predictedLabels[i])){
                testResult.correct.addAndGet(1);
            }
        }

        this.parallelTasks.getExecutor().shutdown();
        testResult.testTime = (System.nanoTime() - startTime);
        return testResult;
    }

    public double loocv(int itertion, String iterationKey, MultivarKNNArgs args, int paramID) throws Exception {
        long startTime = System.nanoTime();
        int trainSize = trainData.size();
        this.args = args;

        int[] predictedLabels = new int[trainSize];
        int[] nearestNeighbours = new int[trainSize];
        ArrayList<Integer> nearestTiedNeighboursList = new ArrayList<>(trainSize);
//        ArrayList<Integer> predictedLabelsList = new ArrayList<>(trainSize);
        double[][] distanceCache = new double[trainSize][trainSize];
        int correct = 0;
        double accuracy = 0;
        double[] accuracies = new double[trainSize];
        double distance;
        double minDistance;
        int q, s;
        int[][] classLabelCounts = new int[trainSize][trainResult.classLabels.length];
        double[] classProbabilities = new double[trainResult.classLabels.length];


        for (int r = 0; r < trainSize; r++) {
            int row = r; //to make r effectively final
            TimeSeries query = trainData.getSeries(row);

            if (args.numThreads == 1){
                IntStream.range(0, trainSize).forEach( column -> {
                    TimeSeries series = trainData.getSeries(column);
                    if (column < row){
                        distanceCache[row][column] = distanceCache[column][row];
                    } else if (row == column){
                        distanceCache[row][column] = 0;
                    }else{
                        if (args.verbosity >= 2 && (column % 1000 == 0)){
                            System.out.print(column + ".");
                        }
                        //TODO refactor
                        if (measure.useDerivativeData){
                            distanceCache[row][column] = measure.distance(row, column);
                        }else{
                            distanceCache[row][column] = measure.distance(series.data(), query.data(), Double.POSITIVE_INFINITY);
                        }

                    }
                });
            }else{
                IntStream.range(0, trainSize).parallel().forEach( column -> {
                    TimeSeries series = trainData.getSeries(column);
                    if (column < row){
                        distanceCache[row][column] = distanceCache[column][row];
                    } else if (row == column){
                        distanceCache[row][column] = 0;
                    }else{
                        if (args.verbosity >= 2 && (column % 1000 == 0)){
                            System.out.print(column + ".");
                        }
                        //TODO refactor
                        if (measure.useDerivativeData){
                            distanceCache[row][column] = measure.distance(row, column);
                        }else{
                            distanceCache[row][column] = measure.distance(series.data(), query.data(), Double.POSITIVE_INFINITY);
                        }

                    }
                });
            }

        }

//        Kryo kryo = new Kryo();
//        String distDir = args.outputDir + "/train/.dist";
//        File fDistDir = new File(distDir);
//        fDistDir.mkdirs();
//        String fileName = distDir + "/" + args.datasetName + "-" + measureName
//                + "-" + measure.isDependentDimensions() + "-" + paramID + ".kryo.bin";
//        Output output = new Output(new FileOutputStream(fileName));
//        kryo.writeObject(output, distanceCache);
//        output.close();


        for (q = 0; q < trainSize; q++) {
            minDistance = Double.POSITIVE_INFINITY;

            for (s = 0; s < trainSize; s++) {
                if (q == s){
                    continue;
                }

                if (distanceCache[q][s] < minDistance){
                    minDistance = distanceCache[q][s];
                    nearestTiedNeighboursList.clear();
//                    predictedLabelsList.clear();
                    nearestTiedNeighboursList.add(s);
//                    predictedLabelsList.add(trainData.getSeries(s).label());
                }else if (distanceCache[q][s] == minDistance){
                    nearestTiedNeighboursList.add(s);
//                    predictedLabelsList.add(trainData.getSeries(s).label());
                }
            }

            int r = rand.nextInt(nearestTiedNeighboursList.size());
            nearestNeighbours[q] = nearestTiedNeighboursList.get(r);
            predictedLabels[q] = trainData.getSeries(nearestNeighbours[q]).label();

            if (predictedLabels[q] == trainData.getSeries(q).label()){
                accuracies[q] = 1.0d;
            }

            String escapedTiedNeighboursList = nearestTiedNeighboursList.toString();
            if (trainResult.trainQueryFile != null) {
                trainResult.trainQueryFile.add(itertion, iterationKey, q, trainData.getSeries(q).label(),
                        predictedLabels[q], nearestNeighbours[q], CsvWriter.quoteString(escapedTiedNeighboursList));
            }

            if (trainResult.trainProbFile != null) {

                for (int i = 0; i < nearestTiedNeighboursList.size(); i++) {
                    int nnIndex = nearestTiedNeighboursList.get(i);
                    int nnClass = trainData.getSeries(nnIndex).label();
                    classLabelCounts[q][nnClass] += 1;
                }

//                //  check
//                int tieCount = 0;
//                for (int i = 0; i < classLabelCounts.length; i++) {
//                    tieCount = tieCount + classLabelCounts[q][i];
//                }
//                if (tieCount != nearestTiedNeighboursList.size()){
//                    throw new RuntimeException(" issue detected tieCount != nearestTiedNeighboursList.size()");
//                }

                // does not tie break, selects the class of the first max
                for (int i = 0; i < classLabelCounts[q].length; i++) {
                    classProbabilities[i] = (double) classLabelCounts[q][i] / nearestTiedNeighboursList.size();
                }
                int predictedLabelByProb = StatisticsDbl.argmax(classProbabilities);

                trainResult.trainProbFile.add(iterationKey, q, trainData.getSeries(q).label(), predictedLabelByProb,
                        nearestTiedNeighboursList.size());
                Integer[] boxedArray = Arrays.stream(classLabelCounts[q]).boxed().toArray(Integer[]::new);
                trainResult.trainProbFile.add(boxedArray);
            }

        }

        if (trainResult.trainQueryFile != null) {
            trainResult.trainQueryFile.flush();
        }

        if (trainResult.trainProbFile != null) {
            trainResult.trainProbFile.flush();
        }

        accuracy = StatisticsDbl.mean(accuracies);
        trainResult.trainTime = (System.nanoTime() - startTime);

        // export results
        trainResult.trainExpFile.add(args.iteration, iterationKey, args.batchTimestamp, trainData.getName(),
                trainData.size(), args.testData.size(), trainData.getNumClasses(),
                trainData.dimensions(), trainData.length(), accuracy,
                measureName,
                CsvWriter.quoteString(measure.toString()),
                trainResult.trainTime / 1e9,
                args.lpDistanceOrderForIndependentDims, args.lpDistanceOrderForDependentDims,
                args.randSeed, -1,
                args.normalize, dimensionsToUse.length, measure.isDependentDimensions(),
                CsvWriter.quoteSet(dimensionsToUse), paramID, args.version, args.hostName);

        trainResult.trainExpFile.flush();

        return accuracy;
    }

    // this function must be thread-safe
    public int predict(int testQueryIndex) throws Exception {
        if (!isFitted){
            throw new Exception("Model must be fitted before calling this function");
        }

        if (testResult.testData == null){
            throw new Exception("Use overloaded predict functions that provide a testDataset");
        }

        double[][] testQueryData = testResult.testData.getSeries(testQueryIndex).data();

        int trainSize = trainData.size();
        double distance = 0;
        double minDistance = Double.POSITIVE_INFINITY;
        int predictedLabel = trainData.getClass(0);
        ArrayList<Integer> nearestTiedNeighboursList = new ArrayList<>(trainSize);
        int[] classLabelCounts = new int[testResult.classLabels.length]; // consider when #class in train != test
        double[] classProbabilities = new double[trainResult.classLabels.length];


        for (int trainIndex = 0; trainIndex < trainSize; trainIndex++) {
            TimeSeries trainSeries = trainData.getSeries(trainIndex);

//            if (trainSeries == testQuery){ // check -- for case testData == trainData -- use .equals()
//                continue;
//            }

            //TODO refactor
            if (measure.useDerivativeData){
                distance = measure.distance(trainIndex, testQueryIndex);
            }else{
                distance = measure.distance(trainSeries.data(), testQueryData, Double.POSITIVE_INFINITY);
            }

            if (Double.isNaN(distance)){
                throw new RuntimeException("distance == Nan in method predict");
            }

            if (distance < minDistance){
                minDistance = distance;
                nearestTiedNeighboursList.clear();
                nearestTiedNeighboursList.add(trainIndex);
            }else if (distance == minDistance){
                nearestTiedNeighboursList.add(trainIndex);
            }else{
                //
            }
        }

        if (nearestTiedNeighboursList == null){
            System.out.println("CRITICAL ERROR detected in predict nearestNeighbourIndices == null");
            System.out.println("testQueryIndex " + testQueryIndex);
            throw new RuntimeException("nearestNeighbourIndices == null");
        }

        if (nearestTiedNeighboursList.size() == 0){
            System.out.println("CRITICAL ERROR detected in predict nearestNeighbourIndices.size == 0");
            System.out.println("testQueryIndex " + testQueryIndex);
            throw new RuntimeException("nearestNeighbourIndices.size == 0");
        }


//        int randomIndex = rand.nextInt(nearestNeighbourIndices.size()); // CHECK NOT thread safe
        int randomIndex = ThreadLocalRandom.current().nextInt(nearestTiedNeighboursList.size());

        int nearestNeighbourIndex =  nearestTiedNeighboursList.get(randomIndex);
        testResult.nearestIndices[testQueryIndex] = nearestNeighbourIndex;

//        if (testQueryIndex == 264){
//            System.out.println("CRITICAL ERROR test in predict");
//            System.out.println("testQueryIndex " + testQueryIndex);
//            System.out.println(nearestNeighbourIndices);
//        }
//        if (testQueryIndex == 263){
//            System.out.println("CRITICAL ERROR test in predict");
//            System.out.println("testQueryIndex " + testQueryIndex);
//            System.out.println(nearestNeighbourIndices);
//        }
        testResult.nearestTiedIndices.set(testQueryIndex, nearestTiedNeighboursList);

        TimeSeries nearestSeries = trainData.getSeries(nearestNeighbourIndex);
        predictedLabel = nearestSeries.label();



        if (trainResult.trainProbFile != null) {

            for (int i = 0; i < nearestTiedNeighboursList.size(); i++) {
                int nnIndex = nearestTiedNeighboursList.get(i);
                int nnClass = trainData.getSeries(nnIndex).label();
                classLabelCounts[nnClass] += 1;
            }

//                //  check
//                int tieCount = 0;
//                for (int i = 0; i < classLabelCounts.length; i++) {
//                    tieCount = tieCount + classLabelCounts[q][i];
//                }
//                if (tieCount != nearestTiedNeighboursList.size()){
//                    throw new RuntimeException(" issue detected tieCount != nearestTiedNeighboursList.size()");
//                }

            // does not tie break, selects the class of the first max
            for (int i = 0; i < classLabelCounts.length; i++) {
                classProbabilities[i] = (double) classLabelCounts[i] / nearestTiedNeighboursList.size();
            }
            int predictedLabelByProb = StatisticsDbl.argmax(classProbabilities);

            trainResult.trainProbFile.add(testResult.testKey, testQueryIndex, args.testData.getSeries(testQueryIndex).label(),
                    predictedLabelByProb, nearestTiedNeighboursList.size());
            Integer[] boxedArray = Arrays.stream(classLabelCounts).boxed().toArray(Integer[]::new);
            trainResult.trainProbFile.add(boxedArray);
        }


        return predictedLabel;
    }


        @Override
    public KNNTestResult predict(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception {
        return null;
    }

    @Override
    public KNNTestResult predictProba(Dataset testData) throws Exception {
        return null;
    }

    @Override
    public KNNTestResult predictProba(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception {
        return null;
    }

    @Override
    public int predict(TimeSeries query) throws Exception {
//        if (!result.isFitted){
//            throw new Exception("Model must be fitted before calling this function");
//        }
//
//        int trainSize = trainData.size();
//        double distance;
//        double min_distance = Double.POSITIVE_INFINITY;
//        int predictedLabel = trainData.getClass(0);
//
//        for (int j = 0; j < trainSize; j++) {
//            TimeSeries series = trainData.getSeries(j);
//
//            if (series == query){ // check -- for case testData == trainData -- use .equals()
//                continue;
//            }
//
//            distance = measure.distance(series.data(), query.data(), Double.POSITIVE_INFINITY);
//            if (distance < min_distance){
//                min_distance = distance;
//                predictedLabel = series.label();
//            }
//        }
//
//        return predictedLabel;
        throw new Exception("Use the predict(int queryIndex) method");
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

    @Override
    public KNNTrainResult getTrainResults() {
        return trainResult;
    }

    @Override
    public KNNTestResult getTestResults() {
        return testResult;
    }

    public MultivarSimMeasure getMeasure() {
        return measure;
    }

    public String getParamString(){
        StringBuilder sb = new StringBuilder();
        sb.append("\"[w:");
        sb.append(windowSize);
        sb.append(",e:");
        sb.append(epsilon);
        sb.append(",wg:");
        sb.append(penalty);
        sb.append(",eg:");
        sb.append(penalty);
        sb.append(",c:");
        sb.append(cost);
        sb.append(",n:");
        sb.append(penalty);
        sb.append(",l:");
        sb.append(cost);
        sb.append("]\"");
        return sb.toString();
    }

    public void setDimensionsToUse(int[] dimensionsToUse) {
        this.dimensionsToUse = dimensionsToUse;
    }

    public void setLpOderForIndependent(double lpDistanceOrderForIndependentDims) {
        this.lpDistanceOrderForIndependentDims = lpDistanceOrderForIndependentDims;
    }

    public void setLpOrderForDependent(double lpDistanceOrderForDependentDims) {
        this.lpDistanceOrderForDependentDims = lpDistanceOrderForDependentDims;
    }

    public double getLpOrderForDependent(){
        return this.lpDistanceOrderForIndependentDims;
    }

    public double getLpOrderForIndependent(){
        return this.lpDistanceOrderForDependentDims;
    }

    public boolean isDependentDimensions() {
        return dependentDimensions;
    }

    public void setDependentDimensions(boolean dependentDimensions) {
        this.dependentDimensions = dependentDimensions;
    }

    public Random getRand() {
        return rand;
    }

    public void setRand(Random rand) {
        this.rand = rand;
    }

    public boolean isUseRandomParams() {
        return useRandomParams;
    }

    public void setUseRandomParams(boolean useRandomParams) {
        this.useRandomParams = useRandomParams;
    }
}
