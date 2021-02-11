package application.test.knn;

import com.beust.jcommander.JCommander;
import core.ClassifierResult;
import data.io.CsvWriter;
import data.io.DataLoader;
import data.timeseries.Dataset;
import knn.KNN;
import trees.splitters.ee.measures.MEASURE;
import trees.splitters.ee.multivariate.*;
import util.Sampler;
import util.Util;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class kNNCV {

    public static AtomicInteger matchCount = new AtomicInteger();
    public static AtomicInteger misMatchCount = new AtomicInteger();
    public static long startTime;
    public static double trainTime, testTime;
    public static CsvWriter csv;

    public static void main(String[] args) {
        try {


//        args = ("-data=E:/data/ -a=Multivariate2018_ts -d=ArticularyWordRecognition,BasicMotions" +
//                " -seed=-1 -o=out/tmp.csv -t=0 -dep=true,false -dims={{1},{0}} -randDim -maxDim=0" +
//                " -m=dtwr -randParams").trim()
//                .split(" ");

//        args = ("-data=/data/ -a=Multivariate2018_ts -d=BasicMotions -norm" +
//                " -seed=6463564 -o=out/tmp.csv -t=0 -dep=true,false" +
//                " -dims={{0},{1},{2},{0,1},{0,1,2},{1000}}" +
//                " -m=lcss" +
//                " -w=20 -e=0.5 -c=1 -g=1").trim()
//                .split(" ");

            //0,0.2,0.4,0.6,0.8,1,2,3,4,5,6,8,10,60

//            args = ("-data=/data/ -a=Multivariate2018_ts -d=BasicMotions -norm" +
//                    " -seed=6463564 -o=out/knncv.csv -t=0 -loocv -dep=true,false" +
////                    " -dims={{0},{1},{2},{3},{4},{5}} -randDims=0 -maxDim=1" +
////                    " -dims={{0,1,2,3,4,5}} -randDims=0 -maxDim=1" +
//                    " -dims={{0},{1},{2},{0,1,2}} -randDims=0 -maxDim=1" +
//                    " -m=twe" +
//                    " -w=20 -e=0,0.05,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0" +
////                    " -g=0.00001,0.0005,0.005,0.05,0.1,0.5,1 -c=0,0.011111111,0.033333333,0.055555556,0.088888889,0.1" +
////                    " -g=0.00001 -c=0.1,0.2,0.5,1,2,5,10,20,50,60,80,100,200" +
//                    " -c=0.011111111,0.1,0.5,1,50 -g=0.00001" +
//                    " -randParams=0").trim()
//                    .split(" ");

//            args = ("-data=/data/ -a=Multivariate2018_ts -d=BasicMotions -norm" +
//                    " -seed=6463564 -o=out/knncv.csv -t=0 -loocv -dep=false" +
//                    " -dims={{10000}} -randDims=0 -maxDim=1" +
//                    " -m=twe" +
//                    " -w=20 -e=0.2,0.8" +
//                    " -c=0.01,1 -g=0.2,0.8" +
//                    " -randParams=0").trim()
//                    .split(" ");
            args = ("-data=/data/ -a=Multivariate2018_ts -d=BasicMotions -norm" +
                    " -seed=6463564 -o=out/knncv.csv -t=0 -loocv -dep=false,true" +
                    " -dims={{0},{10000}} -randDims=0 -maxDim=1" +
                    " -m=dtwr" +
                    " -w=20,30 -e=0.2" +
                    " -c=0.00001 -g=0.2" +
                    " -randParams=0").trim()
                    .split(" ");


            KnnArgs jargs = new KnnArgs();
            JCommander.newBuilder().addObject(jargs).build().parse(args);

            if (jargs.randSeed < 0){
                jargs.randSeed = System.nanoTime();
            }
            Random rand = new Random(jargs.randSeed);
            Sampler.setRand(rand);
            Util.setRand(rand);

            csv = new CsvWriter(jargs.outputFolder, true);
            csv.addColumns("dataset,train_size, test_size, classes, dimensions, length, accuracy, name, " +
                    "measure, trainTime, testTime, lpIndep, lpDep, seed, randParams, normalize, noDims, useDependentDims, " +
                    "dimensionsToUse, extra");
//            csv.writeHeader();



            for (String datasetName : jargs.datasets) {

                Dataset trainData = DataLoader.loadTrainingSet(jargs.dataPath, jargs.archive, datasetName,
                        KnnArgs.fileType);
                Dataset testData = DataLoader.loadTestingsetSet(jargs.dataPath, jargs.archive, datasetName,
                        KnnArgs.fileType, trainData);

                if (jargs.normalize){
                    System.out.println("Normalizing the datasets");
                    trainData.zNormalize(true);
                    testData.zNormalize(true);
                }

                int[][] dimensionSubsets;
                if (jargs.numRandomSubsets > 0) {
                    dimensionSubsets = new int[jargs.numRandomSubsets][];
                    for (int i = 0; i < jargs.numRandomSubsets; i++) {
                        if (jargs.maxDimensions == 0) {
                            dimensionSubsets[i] = Sampler.getIntsFromRange(0, trainData.dimensions(), 1);
                        } else {
                            int numDimensions = Util.getRandNextInt(jargs.minDimensions, jargs.maxDimensions);
                            dimensionSubsets[i] = Sampler.sampleNRandomIntsFromRange(0, trainData.dimensions(),
                                    numDimensions);
                        }
                    }
                } else {
                    String[] subsets = jargs.dimensionsToUse.split("},");
                    dimensionSubsets = new int[subsets.length][];

                    for (int i = 0; i < subsets.length; i++) {
                        String[] set = subsets[i].replaceAll("[{}]", "").split(",");

                        for (int j = 0; j < set.length; j++) {
                            int dim = Integer.parseInt(set[j]);

                            if (dim >= trainData.dimensions()) {
                                dimensionSubsets[i] = Sampler.getIntsFromRange(0, trainData.dimensions(), 1);
                            } else if (dim < 0) {
                                dimensionSubsets[i] = Sampler.sampleNRandomIntsFromRange(0, trainData.dimensions(), dim);
                            } else {
                                if (dimensionSubsets[i] == null) {
                                    dimensionSubsets[i] = new int[set.length];
                                }
                                dimensionSubsets[i][j] = dim;
                            }
                        }
                    }
                }

                for (MEASURE measure : jargs.measures) {
                    System.out.println("---------- " + measure + "----------");
                    KNN knn = new KNN(measure);
                    knn.setNumJobs(jargs.numThreads);
                    knn.setRand(rand);
                    knn.setUseRandomParams(jargs.numRandParams > 0);

                    for (int[] dimensionsToUse : dimensionSubsets) {
                        knn.setDimensionsToUse(dimensionsToUse);
                        long experimentSeed = rand.nextLong(); // new Random(rand.nextLong());

                        switch (measure) {
                            case euc:
                            case euclidean:
                                cvEuclidean(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case dtw:
                            case dtwf:
                                cvDTWF(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case dtwcv:
                            case dtwr:
                                cvDTWR(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case ddtw:
                            case ddtwf:
                                cvDDTWF(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case ddtwcv:
                            case ddtwr:
                                cvDDTWR(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case wdtw:
                                cvWDTW(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case wddtw:
                                cvWDDTW(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case lcss:
                                cvLCSS(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case msm:
                                cvMSM(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case erp:
                                cvWERP(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            case twe:
                                cvTWE(knn, trainData, testData, jargs, datasetName, measure, dimensionsToUse, experimentSeed);
                                break;
                            default:
                                throw new Exception("Unknown similarity measure");
                        }
                        System.out.println();
//                        csv.writeNewLine();
                    }
                    System.out.println("\n");
                }
            }

//        csv.append();
            csv.writeNewLines(1);
            csv.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    public static void loocv(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                             String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed, boolean dimensionDependency) throws Exception {
//
//        csv = new CsvWriter("out/knncv-datasetName-dtw.csv", true);
//        csv.addColumns("queryID, predictedLabel");
//
//        int trainSize = trainData.size();
//        TimeSeries query;
//        int[] predictedLabels = new int[trainSize];
//        for (int q = 0; q < trainSize; q++) {
//            query = trainData.getSeries(q);
//            trainData.remove(q);
//
//            startTime = System.nanoTime();
//            knn.setRand(new Random(experimentSeed));
//            knn.setDependentDimensions(dimensionDependency);
//            knn.fit(trainData);
//            trainTime += ((System.nanoTime() - startTime) / 1e9);
//
//            predictedLabels[q] = knn.predict(query);
//            testTime += ((System.nanoTime() - startTime) / 1e9);
//
//            csv.add(datasetName, trainData.size(), testData.size(), trainData.getNumClasses(),
//                    trainData.dimensions(), trainData.length(), result.getAccuracyScore(),
//                    measureName,
//                    CsvWriter.quoteString(knn.getMeasure().toString()),
//                    trainTime, testTime,
//                    args.lpDistanceOrderForIndependentDims, args.lpDistanceOrderForDependentDims,
//                    args.randSeed, args.randParams,
//                    args.normalize, dimensionsToUse.length, dimensionDependency,
//                    CsvWriter.quoteSet(dimensionsToUse),
//                    matchCount + ", " + misMatchCount);
//
//            trainData.add(query);
//        }
//        csv.flush();
//        System.out.println(csv.getRow(csv.getCurrentRowIndex()));
//    }

    private static void fitAndPredict(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                                  String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        for (boolean dimensionDependency : args.dimensionDependency) {

            if (args.loocv){
                CsvWriter loocOutPutFile = new CsvWriter("out/knncv-"+datasetName+"-"+measureName+".csv", false);
                loocOutPutFile.addColumns("iterCount,queryIndex,queryLabel,nearestIndex,predictedLabel,accuracy,measure");
                loocOutPutFile.writeHeader();

                knn.setDependentDimensions(dimensionDependency);
                knn.fit(trainData);
//                knn.predict(trainData);
//                knn.crossValidate(args, outfile);
                throw new RuntimeException("not implemented");

            }else{
                matchCount.set(0);
                misMatchCount.set(0);
                startTime = System.nanoTime();
                knn.setRand(new Random(experimentSeed));
                knn.setDependentDimensions(dimensionDependency);
                knn.fit(trainData);
                trainTime += ((System.nanoTime() - startTime) / 1e9);

                ClassifierResult result = knn.predict(testData);
                testTime += ((System.nanoTime() - startTime) / 1e9);

                csv.add(datasetName, trainData.size(), testData.size(), trainData.getNumClasses(),
                        trainData.dimensions(), trainData.length(), result.getAccuracyScore(),
                        measureName,
                        CsvWriter.quoteString(knn.getMeasure().toString()),
                        trainTime, testTime,
                        args.lpDistanceOrderForIndependentDims, args.lpDistanceOrderForDependentDims,
                        args.randSeed, args.numRandParams,
                        args.normalize, dimensionsToUse.length, dimensionDependency,
                        CsvWriter.quoteSet(dimensionsToUse),
                        matchCount + ", " + misMatchCount);
                csv.flush();
                System.out.println(csv.getRow(csv.getCurrentRowIndex()));

                System.out.println("Match Count " + matchCount);
                System.out.println("MisMatch Count " + misMatchCount);
            }
        }
    }

    public static void cvEuclidean(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                               String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
    }

    public static void cvDTWF(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                          String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        DTW measure = (DTW) knn.getMeasure();
        measure.setWindowSize(trainData.length());
        fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
    }

    public static void cvDTWR(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                          String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        if (args.numRandParams > 0){
            for (int i = 0; i < args.numRandParams; i++) {
                DTW measure = (DTW) knn.getMeasure();
                measure.setRandomParams(trainData, knn.getRand());
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }else{
            for (int i = 0; i < args.windowSize.size(); i++) {
                DTW measure = (DTW) knn.getMeasure();
                measure.setWindowSize(args.windowSize.get(i));
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }

    }

    public static void cvDDTWF(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                           String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        DDTW measure = (DDTW) knn.getMeasure();
        measure.setWindowSize(trainData.length());
        measure.setWindowSize(trainData.length());
        fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
    }

    public static void cvDDTWR(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                           String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {

        if (args.numRandParams > 0){
            for (int i = 0; i < args.numRandParams; i++) {
                DDTW measure = (DDTW) knn.getMeasure();
                measure.setRandomParams(trainData, knn.getRand());
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }else{
            for (int i = 0; i < args.windowSize.size(); i++) {
                DDTW measure = (DDTW) knn.getMeasure();
                measure.setWindowSize(args.windowSize.get(i));
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }
    }

    public static void cvWDTW(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                          String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        if (args.numRandParams > 0){
            for (int i = 0; i < args.numRandParams; i++) {
                WDTW measure = (WDTW) knn.getMeasure();
                measure.setRandomParams(trainData, knn.getRand());
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }else {
            for (int i = 0; i < args.gWDTW.size(); i++) {
                WDTW measure = (WDTW) knn.getMeasure();
                measure.setG(args.gWDTW.get(i), trainData.length());
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }
    }

    public static void cvWDDTW(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                           String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        if (args.numRandParams > 0){
            for (int i = 0; i < args.numRandParams; i++) {
                WDDTW measure = (WDDTW) knn.getMeasure();
                measure.setRandomParams(trainData, knn.getRand());
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }else {
            for (int i = 0; i < args.gWDTW.size(); i++) {
            WDDTW measure = (WDDTW) knn.getMeasure();
            measure.setG(args.gWDTW.get(i), trainData.length());
            fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }
    }

    public static void cvLCSS(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                          String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        if (args.numRandParams > 0){
            for (int i = 0; i < args.numRandParams; i++) {
                LCSS measure = (LCSS) knn.getMeasure();
                measure.setRandomParams(trainData, knn.getRand());
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }else {
            for (int i = 0; i < args.windowSize.size(); i++) {
                for (int j = 0; j < args.eLCSS.size(); j++) {
                    LCSS measure = (LCSS) knn.getMeasure();
                    measure.setWindowSize(args.windowSize.get(i));
                    measure.setEpsilon(args.eLCSS.get(j));
                    fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
                }
            }
        }
    }

    public static void cvMSM(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                         String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        if (args.numRandParams > 0){
            for (int i = 0; i < args.numRandParams; i++) {
                MSM measure = (MSM) knn.getMeasure();
                measure.setRandomParams(trainData, knn.getRand());
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }else {
            for (int i = 0; i < args.cMSM.size(); i++) {
                MSM measure = (MSM) knn.getMeasure();
                measure.setCost(args.cMSM.get(i));
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }
    }

    public static void cvWERP(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                          String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        if (args.numRandParams > 0){
            for (int i = 0; i < args.numRandParams; i++) {
                ERP measure = (ERP) knn.getMeasure();
                measure.setRandomParams(trainData, knn.getRand());
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }else {
            for (int i = 0; i < args.windowSize.size(); i++) {
                for (int j = 0; j < args.gERP.size(); j++) {
                    ERP measure = (ERP) knn.getMeasure();
                    measure.setWindowSize(args.windowSize.get(i));
                    measure.setG(args.gERP.get(j));
                    fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
                }
            }
        }
    }

    public static void cvTWE(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
                                         String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
        if (args.numRandParams > 0){
            for (int i = 0; i < args.numRandParams; i++) {
                TWE measure = (TWE) knn.getMeasure();
                measure.setRandomParams(trainData, knn.getRand());
                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
            }
        }else {
            for (int i = 0; i < args.lTWE.size(); i++) {
                for (int j = 0; j < args.nTWE.size(); j++) {
                    TWE measure = (TWE) knn.getMeasure();
                    measure.setLambda(args.lTWE.get(i));
                    measure.setNu(args.nTWE.get(j));
                    fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
                }
            }
        }
    }


}