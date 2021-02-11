package knn;

import application.test.knn.MultivarKNNArgs;
import core.ClassifierResult;
import data.io.CsvWriter;
import data.timeseries.Dataset;
import joinery.DataFrame;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KNNTrainResult extends ClassifierResult {

    // seed??
    public boolean collectTrainStatistics;
    public long trainTime;
    public transient Dataset trainData;
    public int trainSize;
    public String trainKey;

    // check for thread safety
    public int[] nearestIndices;
    public ArrayList<ArrayList<Integer>> nearestTiedIndices;    //comma separated
    public int[] classLabels;

    public CsvWriter trainExpFile;
    public CsvWriter trainQueryFile;
    public CsvWriter trainProbFile;
    public DataFrame<Object> dfTrainProb;

    public MultivarKNNArgs args;

    public KNNTrainResult(KNN knn, Dataset trainData) throws IOException {
        super(knn);

        this.trainData = trainData;
        this.trainSize = trainData.size();
        this.classLabels = trainData.getUniqueClasses();
        this.collectTrainStatistics = true;
        this.args = knn.args;

        //make sure to initialize this array list to its capacity, because set() function is used later
        nearestIndices = new int[trainSize];
        nearestTiedIndices = new ArrayList<>(trainSize);
        for (int i = 0; i < trainSize; i++) {
            nearestTiedIndices.add(null);
        }

    }

    public void initializeOutputFiles() throws IOException {
        // if these file are not initialize before, then initialize.
        // users may set these files to previously initialized files before calling fit() to append the output to an existing file

        if (trainExpFile == null){
            trainExpFile = new CsvWriter(args.currentDir.getPath(), args.trainFilePrefix + ".train.exp.csv", args.fileOutput, 30);
            trainExpFile.addColumns("iteration,iterationKey,timestamp,dataset,trainSize,testSize,classes,dimensions,length,trainAccuracy,name," +
                    "measure,trainTime,lpIndep,lpDep,seed,randParams,normalize,noDims,useDependentDims," +
                    "dimensionsToUse,paramID,version,hostName");
            trainExpFile.writeHeader();
        }

        if (trainQueryFile == null && args.exportTrainQueryFile){
            trainQueryFile = new CsvWriter(args.currentDir.getPath(), args.trainFilePrefix + ".train.query.csv", args.fileOutput, 30 * trainSize);
            trainQueryFile.addColumns("iteration,iterationKey,queryIndex,queryLabel,predictedLabel,nearestIndex,nearestTiedIndices");
            trainQueryFile.writeHeader();
        }

        if (trainProbFile == null && args.exportTrainQueryFile){

            String classColumnNames = IntStream.of(classLabels).mapToObj(String::valueOf).collect(Collectors.joining(","));

            //TODO testing
//            ArrayList<String> columnNames = new ArrayList<>();
//            columnNames.add("testKey");
//            columnNames.add("queryIndex");
//            columnNames.add("queryLabel");
//            for (int className : classLabels) {
//                columnNames.add(className + "");
//            }
//            dfTrainProb = new DataFrame<>(columnNames);
//            dfTrainProb.append(Arrays.asList("a", "b","c", "d","a", "b","c", "d",));
//            dfTrainProb.writeCsv(args.currentDir.getPath() + "/" + args.trainFilePrefix + ".train.prob.csv");

//            System.out.println(dfTrainProb.toString());
            trainProbFile = new CsvWriter(args.currentDir.getPath(), args.trainFilePrefix + ".train.count.csv", args.fileOutput, 30 * trainSize);
            trainProbFile.addColumns("iterationKey,queryIndex,queryLabel,predictedLabel,numNeighbours," + classColumnNames);
            trainProbFile.writeHeader();
        }
    }

    // check thread safety
    public void exportTrainPredictions() throws Exception {
        if (trainProbFile == null){
            return;
        }

        for (int i = 0; i < trainSize; i++) {
            if (nearestTiedIndices.get(i) == null){
                System.out.println("CRITICAL ERROR detected in exportTestQueryFileData");
                System.out.println("testSize " + trainSize);
                System.out.println(i + ": " + nearestTiedIndices.get(i));
                System.out.println(nearestTiedIndices.size());
            }

            String escapedNeighboursList = nearestTiedIndices.get(i).toString();

            trainQueryFile.add(trainSize, i, trainData.getSeries(i).label(),
                    predictedLabels[i], nearestIndices[i], CsvWriter.quoteString(escapedNeighboursList));

            trainProbFile.add();

        }

    }

}
