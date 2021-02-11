package application.test.knn;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;
import data.io.CsvWriter;
import data.timeseries.Dataset;
import distance.univariate.MEASURE;
import util.BuildUtil;
import util.Sampler;
import util.Util;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Parameters(separators = "=")
public class MultivarKNNArgs {

    @Parameter(names = {"-version", "-v"}, description = "Version")
    //  23-8-2020  - znorm fix
    //  0.0.5:2020-9-5:erp and lcss window
    //  0.0.6:2020-9-10:erp and lcss param per dim
    public String version = "0.0.7:2020-11-00:L1 fix-sqrt";

    @Parameter(names = {"-verbosity", "-vb"}, description = "Verbosity")
    public int verbosity = 1;

    @Parameter(names = {"-data"}, description = "Data path")
    public String dataPath = "E:/data/";

    @Parameter(names = {"-archive", "-a"}, description = "Archive name")
    public String archive = "Multivariate2018_ts";

    @Parameter(names = {"-datasets", "-d"}, description = "Comma separated list of datasets",
            required = true, arity = 1)
    public List<String> datasets = new ArrayList<String>();

    @Parameter(names = {"-seed"}, description = "Random seed")
    public long randSeed;

    @Parameter(names = {"-useTLRandom"}, description = "use Thread LocalRandom", arity = 1)
    public boolean useTLRandom = false;

    @Parameter(names = {"-out", "-o"}, description = "Output folder name")
    public File outputDir = new File("out/");

    @Parameter(names = {"-summaryFile", "-sf"}, description = "Output summary file name")
    public String summaryFile = "summary.csv";

    @Parameter(names = {"-fileOutput"}, description = "{skip|append|overwrite}", arity = 1,
            converter = MultivarKNNArgs.WriteModeConverter.class)
    public CsvWriter.WriteMode fileOutput = CsvWriter.WriteMode.append;

    @Parameter(names = {"-exportTrainQueryFile"}, description = "export query/count files", arity = 1)
    public boolean exportTrainQueryFile = true;

    @Parameter(names = {"-exportTestQueryFile"}, description = "export query/count files", arity = 1)
    public boolean exportTestQueryFile = true;

    @Parameter(names = {"-saveTransformedDatasets"}, description = "save normalized and derivative files", arity = 1)
    public boolean saveTransformedDatasets = true;

    @Parameter(names = {"-tf"}, description = "Time format")
//    public String timeFormat = "yyyyMMdd-HHmmssSSS-";
    public String timeFormat = "yyyyMMdd-HH-";

    @Parameter(names = {"-it"}, description = "iteration starts from")
    public int iteration = 0;

    @Parameter(names = {"-threads", "-t"}, description = "Number of threads")
    public int numThreads = 0;

    @Parameter(names = {"-normalize", "-norm"}, description = "Normalize dataset", arity = 1)
    public boolean normalize = false;

    @Parameter(names = {"-useSquaredDiff"}, description = "useSquaredDiff", arity = 1)
    public boolean useSquaredDiff = true;

    @Parameter(names = {"-adjustSquaredDiff"}, description = "adjustSquaredDiff", arity = 1)
    public boolean adjustSquaredDiff = true;

    @Parameter(names = {"-lpIndep"}, description = "lpDistanceOrderForIndependentDims")
    public Double lpDistanceOrderForIndependentDims = 2.0;

    @Parameter(names = {"-lpDep"}, description = "lpDistanceOrderForDependentDims")
    public Double lpDistanceOrderForDependentDims = 2.0;

    @Parameter(names = {"-dimDependency", "-dep"}, description = "Dependent or Independent Dimensions")
    public List<Boolean> dimensionDependency = new ArrayList<>();

    @Parameter(names = {"-measure", "-m"}, description = "Similarity measures to use",
            converter = MultivarKNNArgs.MeasureNameConverter.class)
    public List<MEASURE> measures = new ArrayList<>();

    //  usage:  { {{0,...,D},...{0,...,D}}; oneDimPerSubset[:n,min,max]; AllDims[:n,min,max];
    //          oneDimPerSubsetThenAllDims[:n,min,max]; AllDimsThenOneDimPerSubset[:n,min,max];
    //          nRandomSubsets[:n,min,max] }
    @Parameter(names = {"-dimensions", "-dims"}, description = "Dimensions to use (explicit if not using random) - ")
    public String dimensionsToUse;

    @Parameter(names = {"-numRandomSubsets"}, description = "numRandomSubsets")
    public int numRandomSubsets = 0;

    @Parameter(names = {"-maxDimensions", "-maxDim"}, description = "maxDimensions")
    public int maxDimensions = 10000;

    @Parameter(names = {"-minDimensions", "-minDim"}, description = "minDimensions")
    public int minDimensions = 1;

    //  usage:  [list:0,1,2,...,99; range:start,end,step; random:n,id]
    @Parameter(names = {"-parameters", "-params"}, description = "Parameters to use for similarity measures")
    public String params = "";

    // nRandomSubsets == paramIDs.size(),
    // paramIDs = -1 == default random distribution,
    // paramIDs = -2 == uniform distribution U(0,1)
    // paramIDs = -3 == normal distribution N(0,1)
    // paramIDs = 0-99 == paramID as in EE
    // paramIDs >= 100 use custom values
    @Parameter(names = {"-paramIDs"}, description = "list of values from 0 to 100 (exclusive) which identifies paramID for LOOCV -- similar to how paramIDs were used in EE implementation")
    public List<Integer> paramIDs = new ArrayList<>();

//    @Parameter(names = {"-windowSize", "-w"}, description = "warping window size as integers")
//    public List<Integer> windowSize = new ArrayList<>();
//
//    @Parameter(names = {"-windowPercentage", "-wp"}, description = "warping window size as a percentage")
//    public List<Double> windowPercentage = new ArrayList<>();

    @Parameter(names = {"-windowSize", "-w"}, description = "warping window size: as 1 <= integer < length, or as 0 <= percentage < 1")
    public List<Double> windowSize = new ArrayList<>();

    @Parameter(names = {"-gWDTW"}, description = "penalty applied to warpings of larger size - WDTW(g)")
    public List<Double> gWDTW = new ArrayList<>();

    @Parameter(names = {"-eLCSS"}, description = "epsilon to compare double values - LCSS(e); sampling: -1 uniform, -2 norm , -3 exp, preset")
    public List<Double> eLCSS = new ArrayList<>();
    public double[][] epsilonsPerDim;

    @Parameter(names = {"-wLCSS"}, description = "warping window size LCSS(w): as 0 < integer < length, or as 0 <= percentage < 1")
    public List<Double> wLCSS = new ArrayList<>();

    @Parameter(names = {"-cMSM"}, description = "cost of matching/not matching - MSM(c)")
    public List<Double> cMSM = new ArrayList<>();

    @Parameter(names = {"-gERP"}, description = "cost of gap - ERP(g)")
    public List<Double> gERP = new ArrayList<>();
    public double[][] erpGPerDim;

    @Parameter(names = {"-wERP"}, description = "warping window size ERP(w): as 0 < integer < length, or as 0 <= percentage < 1")
    public List<Double> wERP = new ArrayList<>();

    @Parameter(names = {"-lTWE"}, description = " TWE(lambda)")
    public List<Double> lTWE = new ArrayList<>();

    @Parameter(names = {"-nTWE"}, description = " TWE(nu)")
    public List<Double> nTWE = new ArrayList<>();

    // Training only
    @Parameter(names = {"-runLOOCV"}, description = "do leave one out cross validation", arity = 1)
    public boolean runLOOCV = false;

    // Testing
    //
    // -- testing takes best params from a file. This helps to search different paramID in different servers in parallel

    @Parameter(names = {"-runTesting"}, description = "runs the testing procedure using file with best params", arity = 1)
    public boolean runTesting = false;

    @Parameter(names = {"-testDir"}, description = "this is the dir where test results will be saved and best parameter files (*.train.best.csv) are read from")
    public String testDir;

    @Parameter(names = {"-testParamFileSuffixes"}, description = "searches for datasetName-testParamFileSuffix.best.csv")
    public List<String> testParamFileSuffixes = new ArrayList<>();;

    @Parameter(names = {"-generateBestParamFiles"}, description = "generateBestParamFile using the python script", arity = 1)
    public boolean generateBestParamFiles = false;

    @Parameter(names = {"-python"}, description = "pythonPath inside the venv")
    public String pythonPath = "venv/Scripts/python";

    @Parameter(names = {"-ensembleVotingScheme"}, description = "{majority|weighted}")
    public String ensembleVotingScheme = "majority";

    public static String fileType = ".ts";

    //extra params
    public File currentDir;
    public String datasetName;
    public String expTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(timeFormat));
    public int k = 1;
    public String fileSuffixForParamsID = "";
    public String trainFilePrefix = "";
    public String testFilePrefix = "";
    public boolean takeFinalRootForIndep = false;
    public boolean saveCachedDistances;
    public boolean useCachedDistances;


    // GLOBAL SHARED INFORMATION
    public String batchTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss-SSS"));
    public String hostName;

    public transient Dataset trainData;
    public transient Dataset testData;

    public transient Dataset derivativeTrainData;
    public transient Dataset derivativeTestData;

    public MultivarKNNArgs() throws UnknownHostException {
        this.randSeed = System.nanoTime();
        this.hostName = InetAddress.getLocalHost().getHostName();
        this.version += ":build_at=" + BuildUtil.getBuildDate();
    }

    public class MeasureNameConverter implements IStringConverter<MEASURE> {
        @Override
        public MEASURE convert(String smName) {
            MEASURE dm = MEASURE.valueOf(smName);
            return dm;
        }
    }

    public class WriteModeConverter implements IStringConverter<CsvWriter.WriteMode> {

        @Override
        public CsvWriter.WriteMode convert(String value) {
            CsvWriter.WriteMode convertedValue = CsvWriter.WriteMode.fromString(value);

            if(convertedValue == null) {
                throw new ParameterException("Value " + value + "can not be converted to WriteMode.");
            }
            return convertedValue;
        }
    }

    public static class SemiColonSplitter implements IParameterSplitter {
        public List<String> split(String value) {
            return Arrays.asList(value.split(";"));
        }
    }

    //  usage:  { {{0,...,D},...{0,...,D}}; oneDimPerSubset[:n,min,max]; AllDims[:n,min,max];
    //          oneDimPerSubsetThenAllDims[:n,min,max]; AllDimsThenOneDimPerSubset[:n,min,max];
    //          nRandomSubsets[:n,min,max] }
    public int[][] parseDimenstionsToUse(Dataset trainData){
        int[][] dimensionSubsets;
        String[] paramArgs;
        if (this.dimensionsToUse.contains(":")){
            paramArgs = this.dimensionsToUse.split(":")[1].split(",");
            if (paramArgs.length > 0){
                this.numRandomSubsets = Integer.parseInt(paramArgs[1]);
                this.minDimensions = Integer.parseInt(paramArgs[2]);
                this.maxDimensions = Integer.parseInt(paramArgs[3]);
            }
        }

        if (this.dimensionsToUse.startsWith("nRandomSubsets")) { // #subsets = randDimensions, each subset has #dimns from minDimns to maxDimns
            dimensionSubsets = new int[this.numRandomSubsets][];
            for (int i = 0; i < dimensionSubsets.length; i++) {
                if (this.maxDimensions == 0) {
                    dimensionSubsets[i] = Sampler.getIntsFromRange(0, trainData.dimensions(), 1);
                } else {
                    int numDimensions = Util.getRandNextInt(this.minDimensions, this.maxDimensions);
                    dimensionSubsets[i] = Sampler.sampleNRandomIntsFromRange(0, trainData.dimensions(),
                            numDimensions);
                }
            }
        }else if(this.dimensionsToUse.startsWith("oneDimPerSubset")) { // #subsets = min(maxDim - minDim + 1,#dims), each subset has 1 dim from 0 to #subsets, eg. {{0},{1},...,{max}}
            int numDims = Math.min(this.maxDimensions - this.minDimensions + 1, trainData.dimensions());
            dimensionSubsets = new int[numDims][];
            for (int i = 0; i < numDims; i++) {
                dimensionSubsets[i] = new int[] {i};
            }
        }else if(this.dimensionsToUse.startsWith("AllDims")) { // #subsets = 1 with dimensions from min(maxDim - minDim + 1,#dims) eg. {{0,1,...,max}}
            int numDims = Math.min(this.maxDimensions - this.minDimensions + 1, trainData.dimensions());
            dimensionSubsets = new int[1][numDims];
            for (int i = 0; i < numDims; i++) {
                dimensionSubsets[0][i] = i;
            }
        }else if(this.dimensionsToUse.startsWith("oneDimPerSubsetThenAllDims")) { // -1 and then -2
            int numDims = Math.min(this.maxDimensions - this.minDimensions + 1, trainData.dimensions());
            dimensionSubsets = new int[ numDims + 1][];
            for (int i = 0; i < numDims; i++) {
                dimensionSubsets[i] = new int[] {i};
            }
            dimensionSubsets[numDims] = new int[numDims];
            for (int i = 0; i < numDims; i++) {
                dimensionSubsets[numDims][i] = i;
            }
        }else if(this.dimensionsToUse.startsWith("AllDimsThenOneDimPerSubset")) { // -2 and then -1
            int numDims = Math.min(this.maxDimensions - this.minDimensions + 1, trainData.dimensions());
            dimensionSubsets = new int[numDims + 1][];
            dimensionSubsets[0] = new int[numDims];
            for (int i = 0; i < numDims; i++) {
                dimensionSubsets[0][i] = i;
            }
            for (int i = 0; i < numDims; i++) {
                dimensionSubsets[i+1] = new int[] {i};
            }
        }else { //predefined subsets
            String[] subsets = this.dimensionsToUse.split("},");
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

        return dimensionSubsets;
    }


    // params: usage:  [list:0,1,2,...,99; range:start,end,step; random:n,id,explicit]
    //
    // paramID = -1 == default random distribution,
    // paramID = -2 == uniform distribution U(0,1)
    // paramID = -3 == normal distribution N(0,1)
    // paramID = 0-99 == paramID as in EE
    // paramID >= 100 ignored, use custom values
    public void parseParams(Dataset trainData){
        String[] paramArgs;

        if (this.params.startsWith("list")){
            this.paramIDs.clear();
            paramArgs = this.params.split(":")[1].split(",");
            for (String paramArg : paramArgs) {
                this.paramIDs.add(Integer.parseInt(paramArg));
            }
            int min = Collections.min(this.paramIDs);
            int max = Collections.max(this.paramIDs);
            fileSuffixForParamsID = "-p_"+min + "_"+max;
        }else if (this.params.startsWith("range")){
            this.paramIDs.clear();
            paramArgs = this.params.split(":")[1].split(",");
            int start = Integer.parseInt(paramArgs[0]);
            int end = Integer.parseInt(paramArgs[1]);
            int step = Integer.parseInt(paramArgs[2]);
            for (int i = start; i < end; i += step) {
                this.paramIDs.add(i);
            }
            this.fileSuffixForParamsID = "-p_"+start + "_"+end+"_"+step;
        }else if (this.params.startsWith("random")){
            this.paramIDs.clear();
            paramArgs = this.params.split(":")[1].split(",");
            int n  = Integer.parseInt(paramArgs[0]);
            int id = Integer.parseInt(paramArgs[1]);
            for (int i = 0; i < n; i++) {
                this.paramIDs.add(id);
            }
            int min = Collections.min(this.paramIDs);
            int max = Collections.max(this.paramIDs);
            fileSuffixForParamsID = "-pr_"+min + "_"+max;
        }else{ //explicit
            this.paramIDs.clear();
        }
    }
}
