package knn;

import application.test.knn.MultivarKNNArgs;
import com.univocity.parsers.csv.Csv;
import core.ClassifierResult;
import data.io.CsvWriter;
import data.timeseries.Dataset;
import joinery.DataFrame;
import util.math.doubles.StatisticsDbl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KNNTestResult extends ClassifierResult {

    // seed??
    public boolean collectTestStatistics;
    public long testTime;
    public transient Dataset testData;
    public int testSize;
    public String testKey;

    public transient Dataset trainData;
    public int trainSize;

    // check for thread safety
    public int[] nearestIndices;
    public ArrayList<ArrayList<Integer>> nearestTiedIndices;    //comma separated
    public int[] classLabels;

    public CsvWriter testExpFile;
    public CsvWriter testQueryFile;
    public CsvWriter testProbFile;

    public MultivarKNNArgs args;

    public KNNTestResult(KNN knn, Dataset testData) throws IOException {
        super(knn);

        this.testData = testData;
        this.testSize = testData.size();
        this.trainData = knn.trainData;
        this.trainSize = this.trainData.size();
        this.classLabels = testData.getUniqueClasses();
        this.collectTestStatistics = true;
        this.args = knn.args;
        this.testKey = java.util.UUID.randomUUID().toString().substring(0, 8);

        //make sure to initialize this array list to its capacity, because set() function is used later
        nearestIndices = new int[testSize];
        nearestTiedIndices = new ArrayList<>(testSize);
        for (int i = 0; i < testSize; i++) {
            nearestTiedIndices.add(null);
        }

    }

    public void initializeOutputFiles() throws IOException {
        //        if (testExpFile == null){
//            testExpFile = new CsvWriter(args.currentDir.getPath(), args.testFilePrefix + ".test.exp.csv", args.fileOutput, 30);
//            testExpFile.addColumns("")
//            testExpFile.writeHeader();
//        }
//
//        if (testQueryFile == null && args.exportTestQueryFile){
//            testQueryFile = new CsvWriter(args.currentDir.getPath(), args.testFilePrefix + ".test.query.csv", args.fileOutput, 30 * testSize);
//            testQueryFile.writeHeader();
//        }

//        if (testProbFile == null && args.exportTestQueryFile){
//            String classColumnNames = IntStream.of(classLabels).mapToObj(String::valueOf).collect(Collectors.joining(","));
//            testProbFile = new CsvWriter(args.currentDir.getPath(), args.testFilePrefix + ".test.count.csv", args.fileOutput, 30 * testSize);
//            testProbFile.writeHeader();
//        }
    }

    //to make exporting thread safe - collect partial results in arrays then output to the csv file
    // check thread safety
    public void exportTestPredictions() throws Exception {
        int[][] classLabelCounts = new int[testSize][classLabels.length];
        double[] classProbabilities = new double[classLabels.length];

        for (int testIndex = 0; testIndex < testSize; testIndex++) {
            if (nearestTiedIndices.get(testIndex) == null){
                System.out.println("CRITICAL ERROR detected in exportTestQueryFileData");
                System.out.println("testSize " + testSize);
                System.out.println(testIndex + ": " + nearestTiedIndices.get(testIndex));
                System.out.println(nearestTiedIndices.size());
            }

            String escapedNeighboursList = nearestTiedIndices.get(testIndex).toString();

            testQueryFile.add(testKey, testIndex, testData.getSeries(testIndex).label(), predictedLabels[testIndex],
                    nearestIndices[testIndex], CsvWriter.quoteString(escapedNeighboursList));

            if (testProbFile != null) {

                for (int trainIndex = 0; trainIndex < nearestTiedIndices.get(testIndex).size(); trainIndex++) {
                    int nnTrainIndex = nearestTiedIndices.get(testIndex).get(trainIndex);
                    int nnClass = trainData.getSeries(nnTrainIndex).label();
                    classLabelCounts[testIndex][nnClass] += 1;
                }

//                //  check
//                int tieCount = 0;
//                for (int testIndex = 0; testIndex < classLabelCounts.length; testIndex++) {
//                    tieCount = tieCount + classLabelCounts[q][testIndex];
//                }
//                if (tieCount != nearestTiedNeighboursList.size()){
//                    throw new RuntimeException(" issue detected tieCount != nearestTiedNeighboursList.size()");
//                }

                // does not tie break, selects the class of the first max
                for (int k = 0; k < classLabelCounts[testIndex].length; k++) {
                    classProbabilities[k] = (double) classLabelCounts[testIndex][k] / nearestTiedIndices.size();
                }
                int predictedLabelByProb = StatisticsDbl.argmax(classProbabilities);

                testProbFile.add(testKey, testIndex, testData.getSeries(testIndex).label(),predictedLabelByProb, nearestTiedIndices.get(testIndex).size());
                Integer[] boxedArray = Arrays.stream(classLabelCounts[testIndex]).boxed().toArray(Integer[]::new);
                testProbFile.add(boxedArray);
            }
        }

        testProbFile.flush();

    }

}
