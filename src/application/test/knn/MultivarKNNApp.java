package application.test.knn;
import com.beust.jcommander.JCommander;
import data.io.CsvWriter;
import data.io.DataLoader;
import data.timeseries.Dataset;
import data.timeseries.MTSDataset;
import data.transformers.DerivativeTransformer;
import knn.KNN;
import distance.univariate.MEASURE;
import distance.multivariate.*;
import knn.KNNTestResult;
import knn.KNNTrainResult;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import util.Sampler;
import util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultivarKNNApp {

    // by alphabetical order
    // ArticularyWordRecognition,AtrialFibrillation,BasicMotions,CharacterTrajectories,Cricket,DuckDuckGeese
    // EigenWorms,Epilepsy,ERing,EthanolConcentration,FaceDetection,FingerMovements,HandMovementDirection,
    // Handwriting,Heartbeat,InsectWingbeat,JapaneseVowels,Libras,LSST,MotorImagery,NATOPS,PEMS-SF,PenDigits,
    // PhonemeSpectra,RacketSports,SelfRegulationSCP1,SelfRegulationSCP2,SpokenArabicDigits,StandWalkJump,
    // UWaveGestureLibrary

    // by variable length datasets
    // CharacterTrajectories,InsectWingbeat,JapaneseVowels,SpokenArabicDigits

    // by timing rank
    // less than 1 hr: ERing,BasicMotions,Libras,RacketSports,AtrialFibrillation,NATOPS,Handwriting,Epilepsy,UWaveGestureLibrary,FingerMovements,ArticularyWordRecognition,
    // less than 6 hrs: HandMovementDirection,LSST,
    // more than 24 hrs: Heartbeat
    // incpmplete: StandWalkJump,SelfRegulationSCP1,EthanolConcentration,DuckDuckGeese
    // unranked: Cricket,SelfRegulationSCP2,MotorImagery,PEMS-SF,PhonemeSpectra,FaceDetection,PenDigits,EigenWorms

    // training time rank on 5 params
    // less than x hr: ERing,BasicMotions,Libras,RacketSports,AtrialFibrillation,NATOPS,Handwriting,Epilepsy,UWaveGestureLibrary,FingerMovements,ArticularyWordRecognition
    // less than y hrs: HandMovementDirection,LSST,StandWalkJump,Cricket,SelfRegulationSCP1,SelfRegulationSCP2,EthanolConcentration
    // more than z hrs: Heartbeat,DuckDuckGeese,PenDigits,PhonemeSpectra,PEMS-SF,MotorImagery
    // unranked: FaceDetection,EigenWorms

    public static long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024/1024;
    public static long presumableFreeMemory = (Runtime.getRuntime().maxMemory() - allocatedMemory)/1024/1024;
    public static double timeElapsed;

    public static String[] prodArgs = ("-data=/data/ -a=Multivariate2018_ts -vb=1 -threads=0 -seed=6463564 -useTLRandom=false" +
            " -o=E:/git/experiments/knn/20-10-2020/sorted/i2d2-norm/train/ -fileOutput=overwrite -exportTrainQueryFile=true -exportTestQueryFile=true -saveTransformedDatasets=false" +
            " -runTesting=true -testDir=E:/git/experiments/knn/20-10-2020/sorted/i2d2-norm/test/ -testParamFileSuffixes=i,d,id,b -generateBestParamFiles=false -python=venv/Scripts/python" +
            " -runLOOCV=false -norm=false -dep=false,true -dims=AllDims -lpIndep=2 -lpDep=2 -adjustSquaredDiff=true -useSquaredDiff=true" +
//            " -d=ERing,BasicMotions,Libras,RacketSports,AtrialFibrillation,NATOPS,Handwriting,Epilepsy,UWaveGestureLibrary,FingerMovements,ArticularyWordRecognition,HandMovementDirection,StandWalkJump,Cricket" +
            " -d=Heartbeat" +
            " -m=euc,dtwf,dtwr,ddtwf,ddtwr,wdtw,wddtw,lcss,msm,erp,twe" +
            " -params=range:0,100,1" +
            "").trim().split(" ");

    public static void main(String[] cmdArgs) {
        try {
// -i1d2+norm -i1d2-norm -i2d2-norm -i2d2+norm
            String[] devArgs = ("-data=/data/ -a=Multivariate2018_ts -vb=1 -threads=0 -seed=6463564 -useTLRandom=false" +
                    " -o=out/knn/dev/ -fileOutput=overwrite -exportTrainQueryFile=true -exportTestQueryFile=true -saveTransformedDatasets=false" +
                    " -runTesting=false -testDir=out/knn/lpbug/test/ -testParamFileSuffixes=i,d -generateBestParamFiles=false -python=venv/Scripts/python" +
                    " -runLOOCV=true -norm=false -dep=false,true -dims=AllDims -lpIndep=2 -lpDep=2 -adjustSquaredDiff=true -useSquaredDiff=true" +
//                    " -d=SelfRegulationSCP1,SelfRegulationSCP2,EthanolConcentration,Heartbeat,DuckDuckGeese" +
                    " -d=ERing" +
//                    " -m=euc,dtwf,dtwr,ddtwf,ddtwr,wdtw,wddtw,lcss,msm,erp,twe" +
                    " -m=euc,dtwf" +
                    " -params=range:0,100,1" +
                    " -w=0.2,0.4,0.6,0.8,0.99" +
                    " -eLCSS=0,0.2,0.5,0.8,1.0,2,5,10,20" +
                    " -wLCSS=0.2,0.4,0.6,0.8,0.99" +
                    " -gWDTW=0,0.01,0.1,1,10,-1,-1,-1,-1,-1" +
                    " -gERP=0,0.2,0.5,0.8,1.0,-1,-1,-1,-1,-1" +
                    " -wERP=0.2,0.4,0.6,0.8,0.99" +
                    " -cMSM=0.01,0.1,1,10,100" +
                    " -lTWE=0,0.25,0.5,0.75,1.0" +
                    " -nTWE=0.0001,0.001,0.01,0.1,1" +
                    "").trim().split(" ");

//            0.0.7:2020-11-1:L1 fix
            cmdArgs = devArgs; // ------ COMMENT BEFORE COMPILING JAR FILE

            System.err.println(String.join(" ", cmdArgs));
            logSlurmInformation(cmdArgs);

            MultivarKNNArgs args = new MultivarKNNArgs();
            JCommander.newBuilder().addObject(args).build().parse(cmdArgs);

            if (args.randSeed < 0) {
                args.randSeed = System.nanoTime();
            }
            Random rand = new Random(args.randSeed);
            Sampler.setRand(rand);
            Util.setRand(rand);

            //dev auto name for output folder
            if(args.normalize){
                args.outputDir = new File(args.outputDir.getAbsolutePath()
                        + "/i" + args.lpDistanceOrderForIndependentDims.intValue()
                        + "d"+ args.lpDistanceOrderForDependentDims.intValue()
                        + "+norm/train/"
                );
            }else{
                args.outputDir = new File(args.outputDir.getAbsolutePath()
                        + "/i" + args.lpDistanceOrderForIndependentDims.intValue()
                        + "d"+ args.lpDistanceOrderForDependentDims.intValue()
                        + "-norm/train/"
                );
            }
            args.outputDir.mkdirs();

            for (String datasetName : args.datasets) {
                args.datasetName = datasetName;
                args.expTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                System.out.println("Dataset: " + datasetName + " started at: " + args.expTimestamp);

                MTSDataset[] datasets = DataLoader.loadTrainAndTestSet(args.dataPath, args.archive, datasetName, MultivarKNNArgs.fileType);
                args.trainData = datasets[0];
                args.testData = datasets[1];

                if (args.runLOOCV){
                    runLOOCVForDataset(args, rand);
                }

                if (args.runTesting){
                    runTestingForDataset(args, rand);
                }

                args.expTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                System.out.println("Dataset: " + datasetName + " finished at: " + args.expTimestamp);
                System.out.println();

            } //end dataset

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runLOOCVForDataset(MultivarKNNArgs args, Random rand) throws Exception {

        args.currentDir = new File(args.outputDir + "/" + args.datasetName + "/");
        args.currentDir.mkdirs();

        if (args.normalize) {
            normalizeDataset(args, args.datasetName, args.trainData, true, false,  "train");
            normalizeDataset(args, args.datasetName, args.testData, true, false,  "test");
        }

        DerivativeTransformer dt = new DerivativeTransformer();
        args.derivativeTrainData = dt.transform(args.trainData, null, null);
        args.derivativeTestData = dt.transform(args.testData, null, null);

        File tmpPath = new File(args.outputDir + "/.data/");
        tmpPath.mkdirs();
        if (args.saveTransformedDatasets & args.normalize) {
            args.trainData.saveToFile(tmpPath + "/" + args.datasetName + "-znorm-train.training"+".ts", true);
            args.testData.saveToFile(tmpPath + "/" + args.datasetName + "-znorm-test.training"+".ts", true);
        }
        if (args.saveTransformedDatasets & (args.measures.contains(MEASURE.ddtwf) || args.measures.contains(MEASURE.ddtwr))){
            args.derivativeTrainData.saveToFile(tmpPath + "/" + args.datasetName + "-deriv1-train.training.ts", true);
            args.derivativeTestData.saveToFile(tmpPath + "/" + args.datasetName + "-deriv1-test.training.ts", true);
        }

        Dataset trainData = args.trainData;
        int seriesLength = trainData.length();

        // initializations that can be done after the datasets has been loaded
        int[][] dimensionSubsets = args.parseDimenstionsToUse(args.trainData);
        args.parseParams(args.trainData);

        //init params based on ParamIDs
        initParamIDs(args, trainData, seriesLength);

        for (MEASURE measureName : args.measures) {
            System.out.println(trainData.getName() + ": --------------------- " + measureName + " --------------------: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(args.timeFormat)));

            for (int[] dimensionsToUse : dimensionSubsets) {
                for (boolean dimensionDependency : args.dimensionDependency) {
                    long startTime = System.nanoTime();

                    // NOTE: breaking change  - dimensionsToUse will overwrite the file unless the name is unique or append = true
                    if (dimensionDependency){
                        args.trainFilePrefix = "/"+ args.datasetName +"-"+ measureName + "-d" + args.fileSuffixForParamsID;
                    }else {
                        args.trainFilePrefix = "/"+ args.datasetName +"-"+ measureName + "-i" + args.fileSuffixForParamsID;
                    }

                    //skip iteration if file exists
                    File outF = new File( args.currentDir.getPath() + "/" + args.trainFilePrefix + ".train.exp.csv");
                    if (outF.exists() && args.fileOutput == CsvWriter.WriteMode.skip) {
                        System.out.println("WARN: SKIPPING iteration, result file exists:  " + outF);
                        break;
                    }

                    KNN knn = new KNN(measureName, args.numThreads);
                    knn.args = args; // refactor - quick fix
                    knn.setRand(rand);
//                            knn.setUseRandomParams(args.numRandParams > 0);
                    knn.setDimensionsToUse(dimensionsToUse);
                    knn.setDependentDimensions(dimensionDependency);
                    knn.setLpOderForIndependent(args.lpDistanceOrderForIndependentDims);
                    knn.setLpOrderForDependent(args.lpDistanceOrderForDependentDims);

                    KNNTrainResult trainResult = knn.fit(trainData);
                    trainResult.initializeOutputFiles();

                    long experimentSeed = rand.nextLong(); // new Random(rand.nextLong());
                    boolean terminateCV = false;


                    switch (measureName) {
                        case euc:
                        case euclidean:
                            runLOOCVPerParamID(args,
                                    knn, 0, startTime);
                            break;
                        case dtw:
                        case dtwf:
                            DTW dtwf = (DTW) knn.getMeasure();
                            dtwf.setWindowSizeInt(trainData.length());
                            runLOOCVPerParamID(args,
                                    knn, 100, startTime);
                            break;
                        case dtwcv:
                        case dtwr:
                            DTW dtwr = (DTW) knn.getMeasure();
                            if (args.paramIDs.size() > 0){
                                for (Integer paramID : args.paramIDs) {
                                    dtwr.setWindowSizeDbl(args.windowSize.get(paramID), seriesLength);
                                    runLOOCVPerParamID(args,
                                            knn, paramID, startTime);
                                }
                            }else{
                                for (int i = 0; i < args.windowSize.size(); i++) {
                                    if (args.windowSize.get(i) > trainData.length()) {
                                        terminateCV = true;
                                    }
                                    if (terminateCV) {
                                        break;
                                    }
                                    dtwr.setWindowSizeDbl(args.windowSize.get(i), seriesLength);
                                    runLOOCVPerParamID(args,
                                            knn, i, startTime);
                                }
                            }
                            break;
                        case ddtw:
                        case ddtwf:
                            DDTW ddtwf = (DDTW) knn.getMeasure();
                            ddtwf.setWindowSizeInt(trainData.length());
                            runLOOCVPerParamID(args,
                                    knn, 100, startTime);
                            break;
                        case ddtwcv:
                        case ddtwr:
                            DDTW ddtwr = (DDTW) knn.getMeasure();

                            if (args.paramIDs.size() > 0){
                                for (Integer paramID : args.paramIDs) {
                                    ddtwr.setWindowSizeDbl(args.windowSize.get(paramID), seriesLength);
                                    runLOOCVPerParamID(args,
                                            knn, paramID, startTime);
                                }
                            }else{
                                for (int i = 0; i < args.windowSize.size(); i++) {
                                    if (args.windowSize.get(i) > trainData.length()) {
                                        terminateCV = true;
                                    }
                                    if (terminateCV) {
                                        break;
                                    }
                                    ddtwr.setWindowSizeDbl(args.windowSize.get(i), seriesLength);
                                    runLOOCVPerParamID(args,
                                            knn, i, startTime);
                                }
                            }
                            break;
                        case wdtw:
                            WDTW wdtwr = (WDTW) knn.getMeasure();

                            if (args.paramIDs.size() > 0){
                                for (Integer paramID : args.paramIDs) {
                                    wdtwr.setG(args.gWDTW.get(paramID), trainData.length());
                                    runLOOCVPerParamID(args,
                                            knn, paramID, startTime);
                                }
                            }else{
                                for (int i = 0; i < args.gWDTW.size(); i++) {
                                    double g = args.gWDTW.get(i);
                                    if (g == -1) {
                                        wdtwr.setRandomParams(trainData, knn.getRand());
                                    } else {
                                        wdtwr.setG(args.gWDTW.get(i), trainData.length());
                                    }
                                    runLOOCVPerParamID(args,
                                            knn, i, startTime);
                                }
                            }
                            break;
                        case wddtw:
                            WDDTW wddtwr = (WDDTW) knn.getMeasure();

                            if (args.paramIDs.size() > 0){
                                for (Integer paramID : args.paramIDs) {
                                    wddtwr.setG(args.gWDTW.get(paramID), trainData.length());
                                    runLOOCVPerParamID(args,
                                            knn, paramID, startTime);
                                }
                            }else{
                                for (int i = 0; i < args.gWDTW.size(); i++) {
                                    double g =  args.gWDTW.get(i);
                                    if (g == -1){
                                        wddtwr.setRandomParams(trainData, knn.getRand());
                                    }else{
                                        wddtwr.setG(args.gWDTW.get(i), trainData.length());
                                    }
                                    runLOOCVPerParamID(args,
                                            knn, i, startTime);
                                }
                            }
                            break;
                        case lcss:
                            LCSS lcss = (LCSS) knn.getMeasure();
                            lcss.initParamsByID(trainData, rand);

                            if (args.paramIDs.size() > 0){
                                for (Integer paramID : args.paramIDs) {
                                    lcss.setParamsByID(paramID, trainData.length(), rand);
                                    runLOOCVPerParamID(args,
                                            knn, paramID, startTime);
                                }
                            }else{
                                for (int i = 0; i < args.wLCSS.size(); i++) {
                                    if (args.wLCSS.get(i) > trainData.length()){
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
                                        lcss.setWindowSizeDbl(args.wLCSS.get(i), seriesLength);
                                        runLOOCVPerParamID(args,
                                                knn, i, startTime);
                                    }
                                }
                            }

                            break;
                        case msm:
                            MSM msm = (MSM) knn.getMeasure();

                            if (args.paramIDs.size() > 0){
                                for (Integer paramID : args.paramIDs) {
                                    msm.setCost(args.cMSM.get(paramID));
                                    runLOOCVPerParamID(args,
                                            knn, paramID, startTime);
                                }
                            }else{
                                for (int i = 0; i < args.cMSM.size(); i++) {
                                    double c = args.cMSM.get(i);
                                    if (c == -1) {
                                        msm.setRandomParams(trainData, knn.getRand());
                                    } else {
                                        msm.setCost(args.cMSM.get(i));
                                    }
                                    msm.setCost(args.cMSM.get(i));
                                    runLOOCVPerParamID(args,
                                            knn, i, startTime);
                                }
                            }
                            break;
                        case erp:
                            ERP erp = (ERP) knn.getMeasure();
                            erp.initParamsByID(trainData, rand);

                            if (args.paramIDs.size() > 0){
                                for (Integer paramID : args.paramIDs) {
                                    erp.setParamsByID(paramID, trainData.length(), rand);
                                    runLOOCVPerParamID(args,
                                            knn, paramID, startTime);
                                }
                            }else {
                                for (int i = 0; i < args.wERP.size(); i++) {
                                    if (args.wERP.get(i) > trainData.length()) {
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
                                            double[] gpd = new double[args.erpGPerDim.length];
                                            for (int dim = 0; dim < args.erpGPerDim.length; dim++) {
                                                gpd[dim] = g;
                                            }
                                            erp.setGPerDim(gpd);
                                        }
                                        erp.setWindowSizeDbl(args.wERP.get(i), seriesLength);
                                        runLOOCVPerParamID(args,
                                                knn, i, startTime);
                                    }
                                }
                            }
                            break;
                        case twe:
                            TWE twe = (TWE) knn.getMeasure();

                            if (args.paramIDs.size() > 0){
                                for (Integer paramID : args.paramIDs) {
                                    twe.setNu(args.nTWE.get(paramID / 10));
                                    twe.setLambda(args.lTWE.get(paramID % 10));
                                    runLOOCVPerParamID(args,
                                            knn, paramID, startTime);
                                }
                            }else {
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
                                        runLOOCVPerParamID(args,
                                                knn, i, startTime);
                                    }

                                }
                            }
                            break;
                        default:
                            throw new Exception("Unknown similarity measure");
                    } //end case


                    if (knn.getTrainResults().trainExpFile != null){
                        knn.getTrainResults().trainExpFile.close();
                    }
                    if (knn.getTrainResults().trainQueryFile != null){
                        knn.getTrainResults().trainQueryFile.close();
                    }

                    if (knn.getTrainResults().trainProbFile != null){
                        knn.getTrainResults().trainProbFile.close();
                    }

                    System.out.println();
                } // end dimensionDependency
            } //end dimensionSubsets
//            System.out.println();
        } //end measure

//        System.out.println();

    }

    public static double runLOOCVPerParamID(MultivarKNNArgs args, KNN knn, int paramID, long startTime) throws Exception {

        // Training Phase -- LOOCV
        MultivarSimMeasure measure = knn.getMeasure();
        //TODO refactor
        if (measure.useDerivativeData){
            measure.setTrainDataset(args.derivativeTrainData);   //set both to train and test dataset as during training phase
            measure.setTestDataset(args.derivativeTrainData);    //set both to train and test dataset as during training phase
        }
        measure.setUseSquaredDiff(args.useSquaredDiff);
        measure.setAdjustSquaredDiff(args.adjustSquaredDiff);
        String iterationKey = java.util.UUID.randomUUID().toString().substring(0, 8);


        if (paramID % 5 == 0){
            allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024/1024;
            presumableFreeMemory = (Runtime.getRuntime().maxMemory() - allocatedMemory)/1024/1024;
        }

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        System.out.print("\r paramID: " + paramID + ", time: " + currentTime + ", memory(MB): " + allocatedMemory + "/" + presumableFreeMemory + ", elapsedTime(s): " + timeElapsed);
        System.out.flush();

        double trainAccuracy = knn.loocv(++args.iteration, iterationKey, args, paramID);
        timeElapsed = (System.nanoTime() - startTime) / 1e9;

        return trainAccuracy;
    }

    public static void runTestingForDataset(MultivarKNNArgs args, Random rand) throws Exception {

        args.expTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(args.timeFormat));
        args.currentDir = new File(args.testDir + "/" + args.datasetName + "/");
        args.currentDir.mkdirs();
        System.out.println("Testing Dataset: " + args.datasetName + " started at: " + args.expTimestamp + " cdir: " + args.currentDir);

        for (int i = 0; i < args.testParamFileSuffixes.size(); i++) {

            args.testFilePrefix = args.datasetName +"-" + args.testParamFileSuffixes.get(i);
            System.out.println("Testing: " + args.testFilePrefix);

            // search for *.best.csv file and load it
            String bestParamFileName = args.currentDir.getPath() + "/" + args.datasetName + "-" + args.testParamFileSuffixes.get(i) + ".train.best.exp.csv";
            File bestParamFile = new File(bestParamFileName);

            if (args.generateBestParamFiles){
                generateBestParamFile(args, args.datasetName, bestParamFile);
            }

            if (!bestParamFile.exists()) {
                System.out.println("WARN: FileNotFound: " + bestParamFile.getAbsolutePath());
                continue;
            }

            CsvReadOptions.Builder builder = CsvReadOptions.builder(bestParamFileName)
                            .maxCharsPerColumn(100000).header(true);
            CsvReadOptions options = builder.build();
            Table paramTable = Table.read().usingOptions(options);

//            SDataFrame pTable = SDataFrame.readCsv(bestParamFileName);
            paramTable.write().toFile("out/tablesaw.csv");

            // using the first rowof data  in csv, override some of the cmd arguments
            Row firstRow = paramTable.row(0);
            args.normalize = firstRow.getBoolean("normalize");
            args.lpDistanceOrderForIndependentDims = (double) firstRow.getInt("lpIndep"); //TODO change to getDouble, fix issue with casting in tablesaw
            args.lpDistanceOrderForDependentDims = (double) firstRow.getInt("lpDep");

            if (args.normalize) {
                normalizeDataset(args, args.datasetName, args.trainData, true, false,  "train");
                normalizeDataset(args, args.datasetName, args.testData, true, false,  "test");
            }

            DerivativeTransformer dt = new DerivativeTransformer();
            args.derivativeTrainData = dt.transform(args.trainData, null, null);
            args.derivativeTestData = dt.transform(args.testData, null, null);

            Dataset trainData = args.trainData;
            Dataset testData = args.testData;


            // initializations that can be done after the datasets has been loaded
            int[][] dimensionSubsets = args.parseDimenstionsToUse(args.trainData);
            args.parseParams(args.trainData);

            // TOOD refactor init params based on ParamIDs
            initParamIDs(args, trainData, trainData.length());

            //TODO
            CsvWriter csvTestExp = new CsvWriter(args.currentDir.getPath(), trainData.getName() + "-" + args.testParamFileSuffixes.get(i) + ".test.exp.csv", args.fileOutput, 25);
            csvTestExp.addColumns("testKey,timestamp,dataset,trainSize,testSize,classes,dimensions,length," +
                    "classifier,trainAccuracy,testAccuracy,name,dependency,groupBy,measure,trainMeasure," +
                    "trainTime,testTime,seed,normalize,noDims,useDependentDims,dimensionsToUse,paramID,iterationKey," +
                    "bestFile,expFile"
            );
            csvTestExp.writeHeader();

            CsvWriter csvTestQuery = null;
            if (args.exportTestQueryFile){
                csvTestQuery = new CsvWriter(args.currentDir.getPath(), trainData.getName() +"-" + args.testParamFileSuffixes.get(i) + ".test.query.csv", args.fileOutput, 25 * testData.size());
                csvTestQuery.addColumns("testKey,queryIndex,queryLabel,predictedLabel,nearestIndex,nearestTiedIndices");
                csvTestQuery.writeHeader();
            }

            CsvWriter csvTestProb = null;
            if (args.exportTestQueryFile){
                csvTestProb = new CsvWriter(args.currentDir.getPath(), trainData.getName() +"-" + args.testParamFileSuffixes.get(i) + ".test.count.csv", args.fileOutput, 25 * testData.size());
                int[] classLabels = testData.getUniqueClasses();
                String classColumnNames = IntStream.of(classLabels).mapToObj(String::valueOf).collect(Collectors.joining(","));
                csvTestProb.addColumns("testKey,queryIndex,queryLabel,predictedLabel,numNeighbours," + classColumnNames);
                csvTestProb.writeHeader();
            }

            for (Row row : paramTable) {
                MEASURE measureName = MEASURE.valueOf(row.getString("name"));
                boolean dimensionDependency = row.getBoolean("useDependentDims");
                String dimensionsToUseAsString = row.getString("dimensionsToUse");
                int[] dimensionsToUse = Arrays.stream(dimensionsToUseAsString.replaceAll("[{} ]", "").split(",")).mapToInt( s -> Integer.parseInt(s)).toArray();

                KNN knn = new KNN(measureName, args.numThreads);
                knn.args = args; // refactor - quick fix
                knn.setRand(rand); // not using the seed in the file for flexibility i.e. new Random(row.getInt("seed"))
                knn.setDimensionsToUse(dimensionsToUse);
                knn.setDependentDimensions(dimensionDependency);
                knn.setLpOderForIndependent(args.lpDistanceOrderForIndependentDims); // use value loaded from csv
                knn.setLpOrderForDependent(args.lpDistanceOrderForDependentDims); // use value loaded from csv
                knn.fit(trainData);

                // TODO supports paramIDs only
                MultivarSimMeasure measure = knn.getMeasure();
                measure.setUseSquaredDiff(args.useSquaredDiff); //TODO read from csv
                measure.setAdjustSquaredDiff(args.adjustSquaredDiff); //TODO read from csv
                //TODO refactor
                if (measure.useDerivativeData){
                    measure.setTrainDataset(args.derivativeTrainData);
                    measure.setTestDataset(args.derivativeTestData);
                }

                int paramID = row.getInt("paramID");
                switch (measureName) {
                    case euc:
                    case euclidean:
                        // no parms
                        break;
                    case dtw:
                    case dtwf:
                        DTW dtwf = (DTW) knn.getMeasure();
                        dtwf.setWindowSizeInt(trainData.length());
                        break;
                    case dtwcv:
                    case dtwr:
                        DTW dtwr = (DTW) knn.getMeasure();
                        dtwr.setWindowSizeDbl(args.windowSize.get(paramID), testData.length());
                        break;
                    case ddtw:
                    case ddtwf:
                        DDTW ddtwf = (DDTW) knn.getMeasure();
                        ddtwf.setWindowSizeInt(trainData.length());
                        break;
                    case ddtwcv:
                    case ddtwr:
                        DDTW ddtwr = (DDTW) knn.getMeasure();
                        ddtwr.setWindowSizeDbl(args.windowSize.get(paramID), testData.length());
                        break;
                    case wdtw:
                        WDTW wdtwr = (WDTW) knn.getMeasure();
                        wdtwr.setG(args.gWDTW.get(paramID), trainData.length());
                        break;
                    case wddtw:
                        WDDTW wddtwr = (WDDTW) knn.getMeasure();
                        wddtwr.setG(args.gWDTW.get(paramID), trainData.length());
                        break;
                    case lcss:
                        LCSS lcss = (LCSS) knn.getMeasure();
                        lcss.initParamsByID(trainData, rand);
                        lcss.setParamsByID(paramID, trainData.length(), rand);
                        break;
                    case msm:
                        MSM msm = (MSM) knn.getMeasure();
                        msm.setCost(args.cMSM.get(paramID));
                        break;
                    case erp:
                        ERP erp = (ERP) knn.getMeasure();
                        erp.initParamsByID(trainData, rand);
                        erp.setParamsByID(paramID, trainData.length(), rand);
                        break;
                    case twe:
                        TWE twe = (TWE) knn.getMeasure();
                        twe.setNu(args.nTWE.get(paramID / 10));
                        twe.setLambda(args.lTWE.get(paramID % 10));
                        break;
                    default:
                        throw new Exception("Unknown similarity measure");
                } //end case

                // END

                KNNTestResult knnTestResults = knn.predict(testData); //TODO change this to KnnResult
                double testAccuracy = knnTestResults.getAccuracyScore();
                System.out.print( measureName + ": " + testAccuracy + ", ");

//            knnResult.initTestData(testData);
                knnTestResults.testQueryFile = csvTestQuery;
                knnTestResults.testProbFile = csvTestProb;
                knnTestResults.exportTestPredictions();
                csvTestQuery.flush();
                csvTestProb.flush();


                String dependencyColumn;
                if (measure.isDependentDimensions()){
                    dependencyColumn = "d";
                }else{
                    dependencyColumn = "i";
                }
                String groupBy = args.testParamFileSuffixes.get(i);
                String classifierColumn= measureName + "_" + dependencyColumn + "_" + groupBy;
                csvTestExp.add(knnTestResults.testKey, args.expTimestamp, trainData.getName(),
                        trainData.size(), testData.size(), trainData.getNumClasses(),trainData.dimensions(), trainData.length(),
                        classifierColumn, row.getDouble("trainAccuracy"),testAccuracy,measureName, dependencyColumn, groupBy,
                        CsvWriter.quoteString(knn.getMeasure().toString()),  CsvWriter.quoteString(row.getString("measure")),
                        row.getDouble("trainTime"), knnTestResults.testTime / 1e9,
                        args.randSeed, args.normalize, dimensionsToUse.length, dimensionDependency,CsvWriter.quoteSet(dimensionsToUse),
                        row.getInt("paramID"), row.getString("iterationKey"), bestParamFile,
                        row.getString("expFile")
                        );
                csvTestExp.flush();
            }

            csvTestExp.close();
            if (csvTestQuery != null){
                csvTestQuery.close();
            }
            if (csvTestProb != null){
                csvTestProb.close();
            }
            System.out.println();

        } // end suffix

    }

    public static void generateBestParamFile(MultivarKNNArgs args, String datasetName, File bestParamFile) throws URISyntaxException, IOException, InterruptedException {
        ArrayList<String> pythonArgs = new ArrayList<>();
        URL resScript =  MultivarKNNApp.class.getClassLoader().getResource("python/multivariate/knn_results.py");
        File scriptFile = Paths.get(resScript.toURI()).toFile();
        pythonArgs.add(args.pythonPath);
        pythonArgs.add(scriptFile.getAbsolutePath());
        pythonArgs.add(args.randSeed + "");
        pythonArgs.add(bestParamFile.getAbsolutePath());
        pythonArgs.add(datasetName);
        ProcessBuilder processBuilder = new ProcessBuilder(pythonArgs);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        InputStreamReader isReader = new InputStreamReader(process.getInputStream());
        //Creating a BufferedReader object
        BufferedReader reader = new BufferedReader(isReader);
        StringBuffer sb = new StringBuffer();
        String str;
        while((str = reader.readLine())!= null){
            sb.append(str);
            sb.append("\n");
        }
        System.out.println(sb.toString());
        int exitCode = process.waitFor();
    }

    //TODO move this function to knn.fit()
    public static void initParamIDs(MultivarKNNArgs args, Dataset trainData, int seriesLength) {
        double stdTrain = trainData.getStdv();
        double[] stdPerDim = trainData.getStdvPerDimension();

        if (args.paramIDs.size() > 0){
            args.windowSize.clear();
            IntStream.range(0, MultivarSimMeasure.MAX_PARAMID).forEach(i -> args.windowSize.add(i / 100.0));

            args.gWDTW.clear();
            IntStream.range(0, MultivarSimMeasure.MAX_PARAMID).forEach(i -> args.gWDTW.add(i / 100.0));

            double[] epsilons = Util.linspaceDbl(stdTrain * 0.2, stdTrain, 10,true);
            double[] deltas = Util.linspaceDbl(0, 0.25, 10,true);
            args.eLCSS = Arrays.stream(epsilons).boxed().collect(Collectors.toList());
            args.wLCSS =  Arrays.stream(deltas).boxed().collect(Collectors.toList());

            // DEV - NOTE  args.eLCSS is used dependent LCSS, and args.epsilonsPerDim is use dby independent LCSS
            args.epsilonsPerDim = new double[stdPerDim.length][];
            for (int dim = 0; dim < args.epsilonsPerDim.length; dim++) {
                args.epsilonsPerDim[dim] = Util.linspaceDbl(stdPerDim[dim] * 0.2, stdPerDim[dim], 10,true);
            }

            double[] erpG = Util.linspaceDbl(stdTrain * 0.2, stdTrain, 10,true);
            double[] erpWindows = Util.linspaceDbl(0, 0.25, 10,true);
            args.gERP = Arrays.stream(erpG).boxed().collect(Collectors.toList());
            args.wERP = Arrays.stream(erpWindows).boxed().collect(Collectors.toList());

            //DEV
            args.erpGPerDim = new double[stdPerDim.length][];
            for (int dim = 0; dim < args.epsilonsPerDim.length; dim++) {
                args.erpGPerDim[dim] = Util.linspaceDbl(stdPerDim[dim] * 0.2, stdPerDim[dim], 10,true);
            }

            args.cMSM.clear();
            IntStream.range(0, MultivarSimMeasure.MAX_PARAMID).forEach(i -> args.cMSM.add(MSM.msmParams[i]));

            args.nTWE.clear();
            IntStream.range(0, TWE.twe_nuParams.length).forEach(i -> args.nTWE.add(TWE.twe_nuParams[i]));

            args.lTWE.clear();
            IntStream.range(0, TWE.twe_lamdaParams.length).forEach(i -> args.lTWE.add(TWE.twe_lamdaParams[i]));

        }
    }

    public static void normalizeDataset(MultivarKNNArgs args, String datasetName, Dataset dataset, boolean perSeries,
                                        boolean verbose, String suffix){
        System.out.println("zNormalizing: " + datasetName + "_" + suffix);
        double meanBeforeNormalize = dataset.getMean();
        double stdBeforeNormalize = dataset.getStdv();
        //test mean and std per series per dimension
        double[][] meansBeforeNorm = dataset.getMeanPerSeriesPerDimension();
        double[][] stdBeforeNorm = dataset.getStdvPerSeriesPerDimension();
        double meanMPDBN = Arrays.stream(meansBeforeNorm).flatMapToDouble(Arrays::stream).average().getAsDouble();
        double meanSPDBN = Arrays.stream(stdBeforeNorm).flatMapToDouble(Arrays::stream).average().getAsDouble();
        if (Double.isNaN(meanMPDBN)){
            throw new RuntimeException("WARNING: terminating for safety: meanMPDBN = " + meanMPDBN);
        }
        if (Double.isNaN(meanMPDBN)){
            throw new RuntimeException("WARNING: terminating for safety: meanSPDBN = " + meanMPDBN);
        }
        if(verbose) {
            System.out.println("mean of mean per dim per series before normalization: " + meanMPDBN);
            System.out.println("mean of std per dim per series before normalization: " + meanSPDBN);
        }

        dataset.zNormalize(true);
        double meanAfterNormalize = dataset.getMean();
        double stdAfterNormalize = dataset.getStdv();
//        if (Double.isNaN(meanAfterNormalize)){
//            throw new RuntimeException("WARNING: terminating for safety: meanAfterNormalize = " + meanAfterNormalize);
//        }
//        if (Double.isNaN(stdAfterNormalize)){
//            throw new RuntimeException("WARNING: terminating for safety: stdAfterNormalize = " + stdAfterNormalize);
//        }
        if(verbose){
            System.out.println("mean: before " + meanBeforeNormalize + " after " + meanAfterNormalize);
            System.out.println("std: before " + stdBeforeNormalize + " after " + stdAfterNormalize);
        }

        //test mean and std per series per dimension
        double[][] meansAfterNorm = dataset.getMeanPerSeriesPerDimension();
        double[][] stdAfterNorm = dataset.getStdvPerSeriesPerDimension();
        double meanMPDAN = Arrays.stream(meansAfterNorm).flatMapToDouble(Arrays::stream).average().getAsDouble();
        double meanSPDAN = Arrays.stream(stdAfterNorm).flatMapToDouble(Arrays::stream).average().getAsDouble();
        if (Double.isNaN(meanMPDAN)){
            throw new RuntimeException("WARNING: terminating for safety: meanMPDAN = " + meanMPDAN);
        }
        if (Double.isNaN(meanSPDAN)){
            throw new RuntimeException("WARNING: terminating for safety: meanSPDAN = " + meanSPDAN);
        }
        if(verbose){
            System.out.println("mean of mean per dim per series (should be close to 0): " + meanMPDAN);
            System.out.println("mean of std per dim per series (should be close to 1): " + meanSPDAN);
        }
    }


    public static void logSlurmInformation(String[] cmdArgs) {
        Map<String, String> env = System.getenv();
        String jobID = "001";
        for (String envName : env.keySet()) {
            boolean argsPrintedToStdErr = false;
            if (envName.toUpperCase().contains("SLURM")) {
                if (!argsPrintedToStdErr){
                    System.out.println(String.join(" ", cmdArgs));
                    argsPrintedToStdErr = true;
                }

                System.err.format("%s=%s%n", envName, env.get(envName));
            }
            if (envName.toUpperCase().contains("SLURM_JOBID")) {
                jobID = env.get(envName);
            }
        }
    }


}