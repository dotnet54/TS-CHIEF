package application.test.knn;
import com.beust.jcommander.JCommander;
import core.Classifier;
import core.exceptions.NotImplementedException;
import data.io.CsvWriter;
import data.io.DataLoader;
import data.timeseries.Dataset;
import knn.KNN;
import trees.splitters.ee.measures.MEASURE;
import trees.splitters.ee.multivariate.*;
import util.Sampler;
import util.Util;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class KnnLOOCV {

    // BasicMotions,ERing,PenDigits,RacketSports,LSST,Libras,FingerMovements,NATOPS,FaceDetection,ArticularyWordRecognition,
    // Handwriting,Epilepsy,PhonemeSpectra,UWaveGestureLibrary,HandMovementDirection,Heartbeat,AtrialFibrillation,SelfRegulationSCP1,
    // SelfRegulationSCP2,Cricket,EthanolConcentration,StandWalkJump,PEMS-SF,DuckDuckGeese,MotorImagery

    // ArticularyWordRecognition,AtrialFibrillation,BasicMotions,CharacterTrajectories,Cricket,DuckDuckGeese
    // EigenWorms,Epilepsy,ERing,EthanolConcentration,FaceDetection,FingerMovements,HandMovementDirection,
    // Handwriting,Heartbeat,InsectWingbeat,JapaneseVowels,Libras,LSST,MotorImagery,NATOPS,PEMS-SF,PenDigits,
    // PhonemeSpectra,RacketSports,SelfRegulationSCP1,SelfRegulationSCP2,SpokenArabicDigits,StandWalkJump,
    // UWaveGestureLibrary

    //too long or large
    // EigenWorms
    //variable length
    // CharacterTrajectories,InsectWingbeat,JapaneseVowels,SpokenArabicDigits
    //not normalized

    public static AtomicInteger matchCount = new AtomicInteger();
    public static AtomicInteger misMatchCount = new AtomicInteger();

    public static void main(String[] cmdArgs) {
        try {

//            args = ("-data=/data/ -a=Multivariate2018_ts -d=BasicMotions -norm" +
//                    " -seed=6463564 -o=out/knncv.csvSummaryFile -t=0 -loocv -dep=true,false" +
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
//                    " -seed=6463564 -o=out/knncv.csvSummaryFile -t=0 -loocv -dep=false" +
//                    " -dims={{10000}} -randDims=0 -maxDim=1" +
//                    " -m=twe" +
//                    " -w=20 -e=0.2,0.8" +
//                    " -c=0.01,1 -g=0.2,0.8" +
//                    " -randParams=0").trim()
//                    .split(" ");
//            args = ("-data=/data/ -a=Multivariate2018_ts -d=BasicMotions -norm" +
//                    " -seed=6463564 -o=out/loocv.csvSummaryFile -t=0 -loocv -dep=false,true" +
//                    " -dims={{0},{10000}} -randDims=-1 -maxDim=200 -minDim=1" +
//                    " -m=dtwr,lcss,erp,twe" +
//                    " -w=10,20,30,40,50, -e=0.05,0.2,0.5,0.8,1,1.5,2,5,10" +
//                    " -c=0.001,0.2,0.8,1.0,2,5,10, -p=0.001,0.2,0.8,1.0,2,5,10" +
//                    " -randParams=0").trim()
//                    .split(" ");

            cmdArgs = ("-data=/data/ -a=Multivariate2018_ts -d=BasicMotions -norm" +
                    " -seed=6463564 -it=100 -tf=yyyyMMdd- -o=out/e1/ -of=e1-r0.csv -ao -t=0 -loocv -dep=false,true" +
                    " -m=euc,dtwf,dtwr,ddtwf,ddtwr,wdtw,wddtw,lcss,msm,erp,twe" +
                    " -dims=AllDims" +
                    " -params=paramIDList:0,20,40,60,80" +
                    " -w=20,40,60,80,100" +
                    " -e=0,0.2,0.5,0.8,1.0,-1,-1,-1,-1,-1" +
                    " -gWDTW=0,0.01,0.1,1,10,-1,-1,-1,-1,-1" +
                    " -gERP=0,0.2,0.5,0.8,1.0,-1,-1,-1,-1,-1" +
                    " -cMSM=0.01,0.1,1,10,100" +
                    " -lTWE=0,0.25,0.5,0.75,1.0" +
                    " -nTWE=0.0001,0.001,0.01,0.1,1").trim()
                    .split(" ");

            System.err.println(String.join(" ", cmdArgs));
            System.out.println(String.join(" ", cmdArgs));
            Map<String, String> env = System.getenv();
            String jobID = "001";
            for (String envName : env.keySet()) {
                if (envName.toUpperCase().contains("SLURM")) {
                    System.err.format("%s=%s%n", envName, env.get(envName));
                }
                if (envName.toUpperCase().contains("SLURM_JOBID")) {
                    jobID = env.get(envName);
                }
            }

            KnnArgs args = new KnnArgs();
            JCommander.newBuilder().addObject(args).build().parse(cmdArgs);

            if (args.randSeed < 0) {
                args.randSeed = System.nanoTime();
            }
            Random rand = new Random(args.randSeed);
            Sampler.setRand(rand);
            Util.setRand(rand);

            File outDir = new File(args.outputFolder);
            outDir.mkdirs();

            args.csvSummaryFile = new CsvWriter(args.outputFolder + args.outputFile, false);
            args.csvSummaryFile.addColumns("iteration,dataset,trainSize, testSize, classes, dimensions, length, accuracy, name, " +
                    "measure, trainTime, testTime, lpIndep, lpDep, seed, randParams, normalize, noDims, useDependentDims, " +
                    "dimensionsToUse, paramID");
            args.csvSummaryFile.writeHeader();

            for (String datasetName : args.datasets) {
                args.datasetName = datasetName;
                args.expTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(args.timeFormat));
                args.workingDir = new File(args.outputFolder + datasetName + "/");
                args.workingDir.mkdirs();

                Dataset trainData = DataLoader.loadTrainingSet(args.dataPath, args.archive, datasetName,
                        KnnArgs.fileType);
                Dataset testData = DataLoader.loadTestingsetSet(args.dataPath, args.archive, datasetName,
                        KnnArgs.fileType, trainData);

                //TODO calculate derivative versions of train and test data


                if (args.normalize) {
                    System.out.println(datasetName + ": Normalizing...");
                    double meanBeforeNormalize = trainData.getMean();
                    double stdBeforeNormalize = trainData.getStdv();
                    //test mean and std per series per dimension
                    double[][] meansBN = trainData.mean();
                    double[][] stdBN = trainData.stdv();
                    double meanMPDBN = Arrays.stream(meansBN).flatMapToDouble(Arrays::stream).average().getAsDouble();
                    double meanSPDBN = Arrays.stream(stdBN).flatMapToDouble(Arrays::stream).average().getAsDouble();
                    System.out.println("mean of mean per dim per series before normalization: " + meanMPDBN);
                    System.out.println("mean of std per dim per series before normalization: " + meanSPDBN);

                    trainData.zNormalize(true);
                    testData.zNormalize(true);
                    double meanAfterNormalize = trainData.getMean();
                    double stdAfterNormalize = trainData.getStdv();
                    System.out.println("mean: before " + meanBeforeNormalize + " after " + meanAfterNormalize);
                    System.out.println("std: before " + stdBeforeNormalize + " after " + stdAfterNormalize);

                    //test mean and std per series per dimension
                    double[][] meansAN = trainData.mean();
                    double[][] stdAN = trainData.stdv();
                    double meanMPDAN = Arrays.stream(meansAN).flatMapToDouble(Arrays::stream).average().getAsDouble();
                    double meanSPDAN = Arrays.stream(stdAN).flatMapToDouble(Arrays::stream).average().getAsDouble();
                    System.out.println("mean of mean per dim per series (should be close to 0): " + meanMPDAN);
                    System.out.println("mean of std per dim per series (should be close to 1): " + meanSPDAN);
                    trainData.saveToFile(args.outputFolder + datasetName + "_normalized.ts");
                }

                int[][] dimensionSubsets = KnnArgs.parseDimenstionsToUse(args, trainData);
                KnnArgs.parseParams(args, trainData); // TODO should we call this with LOOCV trainData

//                if (args.randParams > 1) {
//                    initializeRandomParams(args, trainData, rand);
//                } else if (args.randParams < 0 && args.loocvParamIds.size() > 0) {
//                    initializeCVParams(args, trainData, rand);
//                }

                HashMap<Integer, Classifier> models;
                SortedMap<Integer, Double> accuracies;
                double accuracy;
                CsvWriter csvPerExpSummary = null;
                CsvWriter csvPerQuery = null;
                int seriesLength = trainData.length();
                double stdTrain = trainData.getStdv();
                double windowAsAPercentage;

//                CsvWriter csvDebug = new CsvWriter(args.workingDir.getPath(),args.expTimestamp+args.datasetName+"-debug.loocv.csv", false);
//                csvDebug.addColumns("iteration,key,value");
//                csvDebug.writeHeader();

                for (MEASURE measureName : args.measures) {
                    System.out.println("--------------------- " + measureName + "--------------------");
//                    String filePrefix = jobID+"-"+args.expTimestamp+measureName+"-"+args.datasetName+"-";
                    String filePrefix = measureName + "-" + args.datasetName + "-";

                    //skip iteration is file exists
                    File outF = new File(filePrefix + "perExp.loocv.csv");
                    if (outF.exists()) {
                        break;
                    }

                    csvPerExpSummary = new CsvWriter(args.workingDir.getPath(), filePrefix + "perExp.loocv.csv", args.appendOut);
                    csvPerExpSummary.addColumns("iteration,dataset,trainSize, testSize, classes, dimensions, length, accuracy, name, " +
                            "measure, trainTime, testTime, lpIndep, lpDep, seed, randParams, normalize, noDims, useDependentDims, " +
                            "dimensionsToUse, paramID");
                    csvPerExpSummary.writeHeader();

                    csvPerQuery = new CsvWriter(args.workingDir.getPath(), filePrefix + "perQuery.loocv.csv", args.appendOut);
//                csvPerQuery.addColumns("timestramp,iteration,queryIndex,queryLabel,nearestIndex,predictedLabel,predClassDistrib,accuracy,w,e,p,c,dimension,measureName");
                    csvPerQuery.addColumns("iteration,queryIndex,nearestIndex,queryLabel,predictedLabel,nearestIndices");
                    csvPerQuery.writeHeader();


                    for (int[] dimensionsToUse : dimensionSubsets) {
                        for (boolean dimensionDependency : args.dimensionDependency) {

                            models = new HashMap<>();
                            accuracies = new TreeMap<>();

                            KNN knn = new KNN(measureName);
                            knn.setNumJobs(args.numThreads);
                            knn.setRand(rand);
                            knn.setUseRandomParams(args.numRandParams > 0);
                            knn.setDimensionsToUse(dimensionsToUse);
                            knn.setDependentDimensions(dimensionDependency);
                            knn.fit(trainData);
                            long experimentSeed = rand.nextLong(); // new Random(rand.nextLong());
                            boolean terminateCV = false;


                            switch (measureName) {
                                case euc:
                                case euclidean:
                                    accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                    accuracies.put(args.iteration, accuracy);
                                    models.put(args.iteration, knn);
                                    writeRow(csvPerExpSummary, args.iteration, datasetName, trainData, testData, accuracy, measureName, knn, args,
                                            dimensionsToUse, dimensionDependency, args.paramIDs.get(0));
                                    break;
                                case dtw:
                                case dtwf:
                                    DTW dtwf = (DTW) knn.getMeasure();
                                    dtwf.setWindowSize(trainData.length());
                                    accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                    accuracies.put(args.iteration, accuracy);
                                    models.put(args.iteration, knn);
                                    writeRow(csvPerExpSummary, args.iteration, datasetName, trainData, testData, accuracy, measureName, knn, args,
                                            dimensionsToUse, dimensionDependency, args.paramIDs.get(0));
                                    break;
                                case dtwcv:
                                case dtwr:
                                    DTW dtwr = (DTW) knn.getMeasure();

                                    if (args.paramIDs.size() > 0 && args.windowSize.size() > 0){
                                        args.windowSize.clear();
                                        for (int i = 0; i < args.paramIDs.size(); i++) {
                                            windowAsAPercentage =  args.paramIDs.get(i) / 100.0;
                                            args.windowSize.add(Math.ceil(seriesLength  * windowAsAPercentage));
                                        }
                                    }

                                    for (int i = 0; i < args.windowSize.size(); i++) {
                                        if (args.windowSize.get(i) > trainData.length()) {
                                            terminateCV = true;
                                        }
                                        if (terminateCV) {
                                            break;
                                        }
                                        dtwr.setWindowSize(args.windowSize.get(i).intValue());
                                        accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                        accuracies.put(args.iteration, accuracy);
                                        models.put(args.iteration, knn);
                                        writeRow(csvPerExpSummary, args.iteration, datasetName, trainData, testData, accuracy, measureName, knn, args,
                                                dimensionsToUse, dimensionDependency, args.paramIDs.get(i)); // assumes that variable i matches the order of paramID in loocvParamIds
                                    }
                                    break;
                                case ddtw:
                                case ddtwf:
                                    DDTW ddtwf = (DDTW) knn.getMeasure();
                                    ddtwf.setWindowSize(trainData.length());
                                    accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                    accuracies.put(args.iteration, accuracy);
                                    models.put(args.iteration, knn);
                                    writeRow(csvPerExpSummary, args.iteration, datasetName, trainData, testData, accuracy, measureName, knn, args,
                                            dimensionsToUse, dimensionDependency, args.paramIDs.get(0));
                                    break;
                                case ddtwcv:
                                case ddtwr:
                                    DDTW ddtwr = (DDTW) knn.getMeasure();

                                    if (args.paramIDs.size() > 0 && args.windowSize.size() > 0){
                                        args.windowSize.clear();
                                        for (int i = 0; i < args.paramIDs.size(); i++) {
                                            windowAsAPercentage =  args.paramIDs.get(i) / 100.0;
                                            args.windowSize.add(Math.ceil(seriesLength  * windowAsAPercentage));
                                        }
                                    }

                                    for (int i = 0; i < args.windowSize.size(); i++) {
                                        if (args.windowSize.get(i) > trainData.length()) {
                                            terminateCV = true;
                                        }
                                        if (terminateCV) {
                                            break;
                                        }
                                        ddtwr.setWindowSize(args.windowSize.get(i).intValue());
                                        accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                        accuracies.put(args.iteration, accuracy);
                                        models.put(args.iteration, knn);
                                        writeRow(csvPerExpSummary, args.iteration, datasetName, trainData, testData, accuracy, measureName, knn, args,
                                                dimensionsToUse, dimensionDependency, args.paramIDs.get(i));
                                    }
                                    break;
                                case wdtw:
                                    WDTW wdtwr = (WDTW) knn.getMeasure();

                                    if (args.paramIDs.size() > 0 && args.gWDTW.size() > 0){
                                        args.gWDTW.clear();
                                        for (int i = 0; i < args.paramIDs.size(); i++) {
                                            args.gWDTW.add(args.paramIDs.get(i) / 100.0);
                                        }
                                    }

                                    for (int i = 0; i < args.gWDTW.size(); i++) {
                                        double g = args.gWDTW.get(i);
                                        if (g == -1) {
                                            wdtwr.setRandomParams(trainData, knn.getRand());
                                        } else {
                                            wdtwr.setG(args.gWDTW.get(i), trainData.length());
                                        }
                                        accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                        accuracies.put(args.iteration, accuracy);
                                        models.put(args.iteration, knn);
                                        writeRow(csvPerExpSummary, args.iteration, datasetName, trainData, testData, accuracy, measureName, knn, args,
                                                dimensionsToUse, dimensionDependency, args.paramIDs.get(i));
                                    }
                                    break;
                                case wddtw:
                                    WDDTW wddtwr = (WDDTW) knn.getMeasure();

                                    if (args.paramIDs.size() > 0 && args.gWDTW.size() > 0){
                                        args.gWDTW.clear();
                                        for (int i = 0; i < args.paramIDs.size(); i++) {
                                            args.gWDTW.add(args.paramIDs.get(i) / 100.0);
                                        }
                                    }

                                    for (int i = 0; i < args.gWDTW.size(); i++) {
                                        double g =  args.gWDTW.get(i);
                                        if (g == -1){
                                            wddtwr.setRandomParams(trainData, knn.getRand());
                                        }else{
                                            wddtwr.setG(args.gWDTW.get(i), trainData.length());
                                        }
                                        accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                        accuracies.put(args.iteration, accuracy);
                                        models.put(args.iteration, knn);
                                        writeRow(csvPerExpSummary, args.iteration,datasetName, trainData, testData, accuracy, measureName, knn, args,
                                                dimensionsToUse, dimensionDependency, args.paramIDs.get(i));
                                    }
                                    break;
                                case lcss:
                                    LCSS lcss = (LCSS) knn.getMeasure();
                                    double[] epsilons = util.Util.linspaceDbl(stdTrain * 0.2, stdTrain, args.paramIDs.size(),true);
                                    double[] deltas = util.Util.linspaceDbl(0, seriesLength/4, args.paramIDs.size(),true);
                                    //TODO use % 10
                                    args.eLCSS = Arrays.stream(epsilons).boxed().collect(Collectors.toList());
                                    args.wLCSS =  Arrays.stream(deltas).boxed().collect(Collectors.toList());

                                    for (int i = 0; i < args.windowSize.size(); i++) {
                                        if (args.windowSize.get(i) > trainData.length()){
                                            terminateCV = true;
                                        }
                                        if (terminateCV){
                                            break;
                                        }
                                        for (int j = 0; j < args.eLCSS.size(); j++) {
                                            double e =  args.eLCSS.get(j);
                                            if (e == -1){
                                                lcss.setRandomParams(trainData, knn.getRand());
                                            }else{
                                                lcss.setEpsilon(args.eLCSS.get(j));
                                            }
                                            lcss.setWindowSize(args.wLCSS.get(i).intValue());
                                            accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                            accuracies.put(args.iteration, accuracy);
                                            models.put(args.iteration, knn);
                                            writeRow(csvPerExpSummary, args.iteration,datasetName, trainData, testData, accuracy, measureName, knn, args,
                                                    dimensionsToUse, dimensionDependency, args.paramIDs.get(i)); // not using i instead of i * j as paramID
                                        }
                                    }
                                    break;
                                case msm:
                                    MSM msm = (MSM) knn.getMeasure();

                                    if (args.paramIDs.size() > 0 && args.cMSM.size() > 0){
                                        args.cMSM.clear();
                                        for (int i = 0; i < args.paramIDs.size(); i++) {
                                            args.cMSM.add(MSM.msmParams[i]);
                                        }
                                    }

                                    for (int i = 0; i < args.cMSM.size(); i++) {
                                        double c = args.cMSM.get(i);
                                        if (c == -1) {
                                            msm.setRandomParams(trainData, knn.getRand());
                                        } else {
                                            msm.setCost(args.cMSM.get(i));
                                        }
                                        msm.setCost(args.cMSM.get(i));
                                        accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                        accuracies.put(args.iteration, accuracy);
                                        models.put(args.iteration, knn);
                                        writeRow(csvPerExpSummary, args.iteration, datasetName, trainData, testData, accuracy, measureName, knn, args,
                                                dimensionsToUse, dimensionDependency, args.paramIDs.get(i));
                                    }
                                    break;
                                case erp:
                                    ERP erp = (ERP) knn.getMeasure();

                                    double stdv = trainData.getStdv();
                                    double[] erpG = util.Util.linspaceDbl(0.2*stdv, stdv, args.paramIDs.size(),true);
                                    double[] erpWindows = util.Util.linspaceDbl(0, seriesLength/4, args.paramIDs.size(),true);

                                    //TODO use % 10
                                    args.gERP = Arrays.stream(erpG).boxed().collect(Collectors.toList());
                                    args.wERP = Arrays.stream(erpWindows).boxed().collect(Collectors.toList());


                                    for (int i = 0; i < args.windowSize.size(); i++) {
                                        if (args.windowSize.get(i) > trainData.length()) {
                                            terminateCV = true;
                                        }
                                        if (terminateCV) {
                                            break;
                                        }
                                        for (int j = 0; j < args.gERP.size(); j++) {
                                            double g = args.gERP.get(j);
                                            if (g == -1) {
                                                erp.setRandomParams(trainData, knn.getRand());
                                            } else {
                                                erp.setG(args.gERP.get(j));
                                            }
                                            erp.setWindowSize(args.windowSize.get(i).intValue());
                                            accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                            accuracies.put(args.iteration, accuracy);
                                            models.put(args.iteration, knn);
                                            writeRow(csvPerExpSummary, args.iteration, datasetName, trainData, testData, accuracy, measureName, knn, args,
                                                    dimensionsToUse, dimensionDependency, args.paramIDs.get(i));
                                        }

                                    }
                                    break;
                                case twe:
                                    TWE twe = (TWE) knn.getMeasure();

                                    if (args.paramIDs.size() > 0 && args.nTWE.size() > 0){
                                        args.nTWE.clear();
                                        for (int i = 0; i < args.paramIDs.size(); i++) {
                                            args.nTWE.add(TWE.twe_nuParams[i/10]);
                                        }
                                    }
                                    if (args.paramIDs.size() > 0 && args.lTWE.size() > 0){
                                        args.lTWE.clear();
                                        for (int i = 0; i < args.paramIDs.size(); i++) {
                                            args.lTWE.add(TWE.twe_lamdaParams[i%10]);
                                        }
                                    }

                                    for (int i = 0; i < args.nTWE.size(); i++) {
                                        for (int j = 0; j < args.lTWE.size(); j++) {
                                            double n = args.nTWE.get(i);
                                            if (n == -1) {
                                                twe.setRandomParams(trainData, knn.getRand());
                                            } else {
                                                twe.setNu(args.nTWE.get(i));
                                            }
                                            double l = args.lTWE.get(j);
                                            if (l == -1) {
                                                twe.setRandomParams(trainData, knn.getRand());
                                            } else {
                                                twe.setLambda(args.lTWE.get(j));
                                            }
                                            accuracy = knn.loocv(++args.iteration, args, csvPerQuery);
                                            accuracies.put(args.iteration, accuracy);
                                            models.put(args.iteration, knn);
                                            writeRow(csvPerExpSummary, args.iteration, datasetName, trainData, testData, accuracy, measureName, knn, args,
                                                    dimensionsToUse, dimensionDependency, args.paramIDs.get(i));
                                        }

                                    }
                                    break;
                                default:
                                    throw new Exception("Unknown similarity measure");
                            } //end case

                            Integer maxKey = accuracies.entrySet().stream()
                                    .max(Comparator.comparing(Map.Entry::getValue)).get().getKey();
                            KNN bestModel = (KNN) models.get(maxKey);
                            args.csvSummaryFile.add(args.iteration, datasetName, trainData.size(), testData.size(), trainData.getNumClasses(),
                                    trainData.dimensions(), trainData.length(), accuracies.get(accuracies.lastKey()),
                                    measureName,
                                    CsvWriter.quoteString(bestModel.getMeasure().toString()),
                                    knn.getResultCollector().trainTime, knn.getResultCollector().testTime,
                                    args.lpDistanceOrderForIndependentDims, args.lpDistanceOrderForDependentDims,
                                    args.randSeed, args.numRandParams,
                                    args.normalize, dimensionsToUse.length, dimensionDependency,
                                    CsvWriter.quoteSet(dimensionsToUse),
                                    matchCount + ", " + misMatchCount);
                            args.csvSummaryFile.flush();


                        } // end dimensionDependency
//                        System.out.println();
//                        csvSummaryFile.writeNewLine();
                    } //end dimensionSubsets
//                    System.out.println("\n");
                } //end measure

                if (csvPerQuery != null) {
                    csvPerQuery.close();
                    csvPerExpSummary.close();
                }

            } //end dataset


//        csvSummaryFile.append();
            args.csvSummaryFile.writeNewLines(1);
            args.csvSummaryFile.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    //NOTE for cv we need grid search support
//    private static void initializeRandomParams(KnnArgs args, Dataset trainData, Random rand) {
//
//        for (int i = 0; i < args.windowSize.size(); i++) {
//            if (args.windowSize.get(i) == -1) {
//                args.windowSize.set(i, (double) DTW.getRandomWindowSize(trainData, rand));
//            }
//        }
//
//        for (int i = 0; i < args.gWDTW.size(); i++) {
//            if (args.gWDTW.get(i) == -1) {
//                args.gWDTW.set(i, WDTW.getRandomG(trainData, rand));
//            }
//        }
//
//        for (int i = 0; i < args.epsilon.size(); i++) {
//            if (args.epsilon.get(i) == -1) {
//                args.epsilon.set(i, LCSS.getRandomEpsilon(trainData, rand));
//            }
//        }
//
//        for (int i = 0; i < args.cMSM.size(); i++) {
//            if (args.cMSM.get(i) == -1) {
//                args.cMSM.set(i, MSM.getRandomCost(trainData, rand));
//            }
//        }
//
//        for (int i = 0; i < args.gERP.size(); i++) {
//            if (args.gERP.get(i) == -1) {
//                args.gERP.set(i, ERP.getRandomG(trainData, rand));
//            }
//        }
//
//        for (int i = 0; i < args.nTWE.size(); i++) {
//            if (args.nTWE.get(i) == -1) {
//                args.nTWE.set(i, TWE.getRandomNu(trainData, rand));
//            }
//        }
//
//        for (int i = 0; i < args.lTWE.size(); i++) {
//            if (args.lTWE.get(i) == -1) {
//                args.lTWE.set(i, TWE.getRandomLambda(trainData, rand));
//            }
//        }
//    }

//    private static int[][] initializeDimensions(KnnArgs args, Dataset trainData){
//        int[][] dimensionSubsets;
//
//        if (args.randDimensions > 0) { // #subsets = randDimensions, each subset has #dimns from minDimns to maxDimns
//            dimensionSubsets = new int[args.randDimensions][];
//            for (int i = 0; i < dimensionSubsets.length; i++) {
//                if (args.maxDimensions == 0) {
//                    dimensionSubsets[i] = Sampler.getIntsFromRange(0, trainData.dimensions(), 1);
//                } else {
//                    int numDimensions = Util.getRandNextInt(args.minDimensions, args.maxDimensions);
//                    dimensionSubsets[i] = Sampler.sampleNRandomIntsFromRange(0, trainData.dimensions(),
//                            numDimensions);
//                }
//            }
//        }else if(args.randDimensions == -1) { // #subsets = min(maxDim - minDim + 1,#dims), each subset has 1 dim from 0 to #subsets, eg. {{0},{1},...,{max}}
//            int numDims = Math.min(args.maxDimensions - args.minDimensions + 1, trainData.dimensions());
//            dimensionSubsets = new int[numDims][];
//            for (int i = 0; i < numDims; i++) {
//                dimensionSubsets[i] = new int[] {i};
//            }
//        }else if(args.randDimensions == -2) { // #subsets = 1 with dimensions from min(maxDim - minDim + 1,#dims) eg. {{0,1,...,max}}
//            int numDims = Math.min(args.maxDimensions - args.minDimensions + 1, trainData.dimensions());
//            dimensionSubsets = new int[1][numDims];
//            for (int i = 0; i < numDims; i++) {
//                dimensionSubsets[0][i] = i;
//            }
//        }else if(args.randDimensions == -3) { // -1 and then -2
//            int numDims = Math.min(args.maxDimensions - args.minDimensions + 1, trainData.dimensions());
//            dimensionSubsets = new int[ numDims + 1][];
//            for (int i = 0; i < numDims; i++) {
//                dimensionSubsets[i] = new int[] {i};
//            }
//            dimensionSubsets[numDims] = new int[numDims];
//            for (int i = 0; i < numDims; i++) {
//                dimensionSubsets[numDims][i] = i;
//            }
//        }else if(args.randDimensions == -4) { // -2 and then -1
//            int numDims = Math.min(args.maxDimensions - args.minDimensions + 1, trainData.dimensions());
//            dimensionSubsets = new int[numDims + 1][];
//            dimensionSubsets[0] = new int[numDims];
//            for (int i = 0; i < numDims; i++) {
//                dimensionSubsets[0][i] = i;
//            }
//            for (int i = 0; i < numDims; i++) {
//                dimensionSubsets[i+1] = new int[] {i};
//            }
//        }else { //predefined subsets
//            String[] subsets = args.dimensionsToUse.split("},");
//            dimensionSubsets = new int[subsets.length][];
//
//            for (int i = 0; i < subsets.length; i++) {
//                String[] set = subsets[i].replaceAll("[{}]", "").split(",");
//
//                for (int j = 0; j < set.length; j++) {
//                    int dim = Integer.parseInt(set[j]);
//
//                    if (dim >= trainData.dimensions()) {
//                        dimensionSubsets[i] = Sampler.getIntsFromRange(0, trainData.dimensions(), 1);
//                    } else if (dim < 0) {
//                        dimensionSubsets[i] = Sampler.sampleNRandomIntsFromRange(0, trainData.dimensions(), dim);
//                    } else {
//                        if (dimensionSubsets[i] == null) {
//                            dimensionSubsets[i] = new int[set.length];
//                        }
//                        dimensionSubsets[i][j] = dim;
//                    }
//                }
//            }
//        }
//
//        return dimensionSubsets;
//    }

    private static void writeRow(CsvWriter writer, int iteration, String datasetName, Dataset trainData, Dataset testData, double accuracy,
                                 MEASURE measureName, KNN knn, KnnArgs jargs, int[] dimensionsToUse,
                                 boolean dimensionDependency, int paramID) throws Exception {
        writer.add(iteration,datasetName, trainData.size(), testData.size(), trainData.getNumClasses(),
                trainData.dimensions(), trainData.length(), accuracy,
                measureName,
                CsvWriter.quoteString(knn.getMeasure().toString()),
                knn.getResultCollector().trainTime / 1e9, knn.getResultCollector().testTime / 1e9,
                jargs.lpDistanceOrderForIndependentDims, jargs.lpDistanceOrderForDependentDims,
                jargs.randSeed, jargs.numRandParams,
                jargs.normalize, dimensionsToUse.length, dimensionDependency,
                CsvWriter.quoteSet(dimensionsToUse), paramID);
        writer.flush();
    }

//    private static void fitAndPredict(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                                      String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        KnnResult result = (KnnResult) knn.getResultCollector();
//
//        for (boolean dimensionDependency : args.dimensionDependency) {
//
//            if (args.loocv){
//                CsvWriter loocOutPutFile = new CsvWriter("out/knncv-"+datasetName+"-"+measureName+".csv", false);
//                loocOutPutFile.addColumns("iterCount,queryIndex,queryLabel,nearestIndex,predictedLabel,accuracy,measure");
//                loocOutPutFile.writeHeader();
//
//                knn.setDependentDimensions(dimensionDependency);
//                knn.fit(trainData);
////                knn.predict(trainData);
////                knn.crossValidate(args, outfile);
//                //knn.looc(0, args, loocOutPutFile);
//                loocOutPutFile.close();
//                throw new NotImplementedException();
//
//
//            }else{
//                matchCount.set(0);
//                misMatchCount.set(0);
//                result.startTime = System.nanoTime();
//                knn.setRand(new Random(experimentSeed));
//                knn.setDependentDimensions(dimensionDependency);
//                knn.fit(trainData);
//                result.trainTime += ((System.nanoTime() - result.startTime) / 1e9);
//
//                knn.predict(testData);
//                result.testTime += ((System.nanoTime() - result.startTime) / 1e9);
//
//                args.csvSummaryFile.add(datasetName, trainData.size(), testData.size(), trainData.getNumClasses(),
//                        trainData.dimensions(), trainData.length(), result.getAccuracyScore(),
//                        measureName,
//                        CsvWriter.quoteString(knn.getMeasure().toString()),
//                        result.trainTime, result.testTime,
//                        args.lpDistanceOrderForIndependentDims, args.lpDistanceOrderForDependentDims,
//                        args.randSeed, args.numRandParams,
//                        args.normalize, dimensionsToUse.length, dimensionDependency,
//                        CsvWriter.quoteSet(dimensionsToUse),
//                        matchCount + ", " + misMatchCount);
//                args.csvSummaryFile.flush();
//                System.out.println(args.csvSummaryFile.getRow(args. csvSummaryFile.getCurrentRowIndex()));
//
//                System.out.println("Match Count " + matchCount);
//                System.out.println("MisMatch Count " + misMatchCount);
//            }
//        }
//    }
//
//    public static void cvEuclidean(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                                   String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//    }
//
//    public static void cvDTWF(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                              String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        DTW measure = (DTW) knn.getMeasure();
//        measure.setWindowSize(trainData.length());
//        fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//    }
//
//    public static void cvDTWR(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                              String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        if (args.numRandParams > 0){
//            for (int i = 0; i < args.numRandParams; i++) {
//                DTW measure = (DTW) knn.getMeasure();
//                measure.setRandomParams(trainData, knn.getRand());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }else{
//            for (int i = 0; i < args.windowSize.size(); i++) {
//                DTW measure = (DTW) knn.getMeasure();
//                measure.setWindowSize(args.windowSize.get(i));
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }
//
//    }
//
//    public static void cvDDTWF(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                               String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        DDTW measure = (DDTW) knn.getMeasure();
//        measure.setWindowSize(trainData.length());
//        measure.setWindowSize(trainData.length());
//        fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//    }
//
//    public static void cvDDTWR(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                               String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//
//        if (args.numRandParams > 0){
//            for (int i = 0; i < args.numRandParams; i++) {
//                DDTW measure = (DDTW) knn.getMeasure();
//                measure.setRandomParams(trainData, knn.getRand());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }else{
//            for (int i = 0; i < args.windowSize.size(); i++) {
//                DDTW measure = (DDTW) knn.getMeasure();
//                measure.setWindowSize(args.windowSize.get(i));
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }
//    }
//
//    public static void cvWDTW(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                              String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        if (args.numRandParams > 0){
//            for (int i = 0; i < args.numRandParams; i++) {
//                WDTW measure = (WDTW) knn.getMeasure();
//                measure.setRandomParams(trainData, knn.getRand());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }else {
//            for (int i = 0; i < args.gWDTW.size(); i++) {
//                WDTW measure = (WDTW) knn.getMeasure();
//                measure.setG(args.gWDTW.get(i), trainData.length());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }
//    }
//
//    public static void cvWDDTW(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                               String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        if (args.numRandParams > 0){
//            for (int i = 0; i < args.numRandParams; i++) {
//                WDDTW measure = (WDDTW) knn.getMeasure();
//                measure.setRandomParams(trainData, knn.getRand());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }else {
//            for (int i = 0; i < args.gWDTW.size(); i++) {
//                WDDTW measure = (WDDTW) knn.getMeasure();
//                measure.setG(args.gWDTW.get(i), trainData.length());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }
//    }
//
//    public static void cvLCSS(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                              String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        if (args.numRandParams > 0){
//            for (int i = 0; i < args.numRandParams; i++) {
//                LCSS measure = (LCSS) knn.getMeasure();
//                measure.setRandomParams(trainData, knn.getRand());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }else {
//            for (int i = 0; i < args.windowSize.size(); i++) {
//                for (int j = 0; j < args.eLCSS.size(); j++) {
//                    LCSS measure = (LCSS) knn.getMeasure();
//                    measure.setWindowSize(args.windowSize.get(i));
//                    measure.setEpsilon(args.eLCSS.get(j));
//                    fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//                }
//            }
//        }
//    }
//
//    public static void cvMSM(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                             String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        if (args.numRandParams > 0){
//            for (int i = 0; i < args.numRandParams; i++) {
//                MSM measure = (MSM) knn.getMeasure();
//                measure.setRandomParams(trainData, knn.getRand());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }else {
//            for (int i = 0; i < args.cMSM.size(); i++) {
//                MSM measure = (MSM) knn.getMeasure();
//                measure.setCost(args.cMSM.get(i));
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }
//    }
//
//    public static void cvWERP(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                              String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        if (args.numRandParams > 0){
//            for (int i = 0; i < args.numRandParams; i++) {
//                ERP measure = (ERP) knn.getMeasure();
//                measure.setRandomParams(trainData, knn.getRand());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }else {
//            for (int i = 0; i < args.windowSize.size(); i++) {
//                for (int j = 0; j < args.gERP.size(); j++) {
//                    ERP measure = (ERP) knn.getMeasure();
//                    measure.setWindowSize(args.windowSize.get(i));
//                    measure.setG(args.gERP.get(j));
//                    fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//                }
//            }
//        }
//    }
//
//    public static void cvTWE(KNN knn, Dataset trainData, Dataset testData, KnnArgs args,
//                             String datasetName, MEASURE measureName, int[] dimensionsToUse, long experimentSeed) throws Exception {
//        if (args.numRandParams > 0){
//            for (int i = 0; i < args.numRandParams; i++) {
//                TWE measure = (TWE) knn.getMeasure();
//                measure.setRandomParams(trainData, knn.getRand());
//                fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//            }
//        }else {
//            for (int i = 0; i < args.lTWE.size(); i++) {
//                for (int j = 0; j < args.nTWE.size(); j++) {
//                    TWE measure = (TWE) knn.getMeasure();
//                    measure.setLambda(args.lTWE.get(i));
//                    measure.setNu(args.nTWE.get(j));
//                    fitAndPredict(knn, trainData, testData, args, datasetName, measureName, dimensionsToUse, experimentSeed);
//                }
//            }
//        }
//    }


}