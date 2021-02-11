package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import data.timeseries.Dataset;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import trees.splitters.boss.dev.BossDataset.BossParams;
import distance.univariate.MEASURE;
import util.*;
import util.repo.DataArchive;
import util.repo.UCR2015;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class AppConfig {
	
	private static final long serialVersionUID = -502980220452234173L;
	
	public static String version = "0.0.1"; //update this in main application class
	public static String buld_number = "2020.8.3.0"; //update this in main application class
	public static String version_tag = ""; //update this in main application class
	public static String release_notes = String.join("\r\n"
		    , "rotf_dev,boss_dev"
			, "boss param load worm2class bug fixed "
			, "mcb recalculate issue fixed"
			, "forest lvl transform"
			, "best param for forest level - boss_transform_at_forest_level=false"
			, "major clean up - wip"
			, "boss distance replaced by gini split -geoff's idea - may be broken"
			, "tsf and rif - with arma and ps - ps with fast fft"
			, "split evaluator random tie break - fixed"
			, "expot file updates - csv, json wip"
			, "cleaning - wip"
			, "add num repeats to outputfile as prefix- changing to make one output file set per repeat"
			, "v1117 rif and tsf new fixes, export - time, rif components, rif now splits components, cmd args for tsf rif "
			, "v1118 old rif uses too many attribs -  changes rif_same_intv_comp, s*4 rif comp, approx_attribs_test_per_splitter added"
			, "v1119 WIP export file changes, tsf #gini works only for multiples of 3 and and rif for multiples of 4"
			, "v1120 export folder structure fixed"
			, "v1120 new predefined datasets added, host name added to export, csv_target_last fixed, have issues with json export"
			, "v1121 default t=1000, added a quick fix to rise gini issue "
			, "v1122 boss params fix, word length"
			, "v1123 indices reuse per split evaluator added - some performance gain expected"
			, "v1124 boss word length fix now uses 6 to 16, cleaning"
			, "v1125 cleaning, export file changes,fixed time measure bug, fixed distance bugs, rif bug --WIP"
			, "v1126 cleaning, st splitter, summary export",
			"0.26.3 cleaning, st splitter, st export, implementing st len methods",
			"0.26.4 cleaning, st splitter, st preloading params",
			"0.26.5 cleaning, st splitter, st_threshold_selection added, st transfrm per forest",
			"0.27.0 major bug fixes to st splitter - shapelet distance",
			"0.27.1 minor clean up and bug fixes, disabled rif tree and forest transfrms, rif uses Util.randLenFirst now, preload_data added",
			"0.28.0 major clean up, chief resubmission experiments, rand seed",
			"0.29.0 major clean up, inception and st changes, rt wip",
			"0.30.0 clean up WIP, multivariate"
			); //append latest release notes to this in PFApplication class


	public static final int ONE_MB = 1048576;	
	public static final String TIMESTAMP_FORMAT_LONG = "yyyy-MM-dd HH:mm:ss.SSS";	
	public static final String TIMESTAMP_FORMAT_SHORT = "HH:mm:ss.SSS";	
	
	public static final String FILENAME_TIMESTAMP_FORMAT = "yyyy-MM-dd-HHmmss-SSS";	
	public static final String FILENAME_TIMESTAMP_FORMAT_SHORT = "yyyyMMdd-HHmmss-SSS";	

	public static final double NANOSECtoMILISEC = 1e6;	//format utils function take millisec as argument  
	public static final double NANOSECtoSEC = 1e9;	//elapsed times are calculated in nanoseconds

	public static final int MAX_ITERATIONS_RAND_INTERVAL = 10000; 	
	
	public static final String CSV_SEPARATOR = ","; 	

	
	//********************************************************************
	//DEVELOPMENT and TESTING AREA -- 
	public static String current_experiment_id;
	public static LocalDateTime experiment_timestamp; //updated for each dataset
	public static LocalDateTime application_start_timestamp; //updated only once
	public static String application_start_timestamp_formatted;
	public static ArrayList<String> repeat_guids;
	public static String export_file_prefix = "";
	public static String export_file_suffix = "";
//	public static String export_filename_format = "%dt-%r-%D";	//not implemented

	
	public static boolean warmup_java = false;
	public static boolean garbage_collect_after_each_repetition = true;	
	public static int print_test_progress_for_each_instances = 100;

	public static boolean config_majority_vote_tie_break_randomly = true;
	public static boolean config_skip_distance_when_exemplar_matches_query = true;
	public static boolean config_use_random_choice_when_min_distance_is_equal = true;	
	

	public static boolean transofrm_data_per_node = false;	
	
	//not implemented
	public static boolean use_probabilities_to_choose_num_splitters_per_node = false; 
	public static double probability_to_choose_ee;
	public static double probability_to_choose_randf;
	public static double probability_to_choose_rotf;
	public static double probability_to_choose_st;
	public static double probability_to_choose_boss;
	public static double probability_to_choose_tsf;
	public static double probability_to_choose_rif;
	
//	public static String temp_data_path = "E:/data/ucr";	//only used during testing and development for some other main functions eg. see Util.loadData - dont use this in any production part of the code
	
	public static boolean short_file_names = true;
	//********************************************************************
	

	/*
	 * 
	 * DEFAULT SETTINGS, these are either overridden by command line arguments or populated at during execution
	 *
	 *
	 */
	
	public static String[] cmd_args;
	
	//NOTE TODO WARNING -- needs to check if all classes use this random seed, some old classes uses ThreadLocalRandom which does not support seed 
	// currently if using -threads > 1  and boss splitter -- due to hppc library , then random seed is not supported
	public static long rand_seed = 0;	//default is 0, if seed is set to 0 System.nanoTime is used, else the given seed is used
	public static Random rand;			//use this random number generator
	
	public static Runtime runtime = Runtime.getRuntime();
	public static DataArchive archive;
	public static UCR2015 ucr2015;
	public static DataArchive.UCR2018UTS ucr2018uts;
	public static DataArchive.UCR2018MTS ucr2018mts;
	public static String resource_dir = "resources/";
	public static String cache_dir = "cache/";

	public static String host_name;
	public static String debug_machine; //TEMP related to dev command line arg -cluster

	public static int num_threads = 0;	// if 0 all available CPUs are used
	public static int verbosity = 2; //0, 1, 2, 3
	public static int export_level = 3; //0, 1, 2 

	//set these after reading the dataset -- helps some optimizations
	public boolean ucr_archive_experiment_mode = false;
	public boolean multivariate_mode = false;
	public boolean variable_length_mode = false;

	public static String archiveName;	//are we using UCR datasets? if so cmd args can specify dataset names instead of full file names
	public static String[] datasets;
	public static String data_dir = "/data/";
	public static String training_file;
	public static String testing_file;
	public static String output_dir = "out/local/tmp/";
	public static String results_file = "results.csv"; //temp file to append summary of results - easier to use than exported files in -out folder
	public static boolean csv_has_header = false;
	public static int csv_label_column = 0; //{0 = first, -1 == last}

	public static int num_repeats = 1;
	public static int num_trees = 500;
	public static boolean random_dm_per_node = true;
	public static boolean shuffle_dataset = false;
	public static boolean binary_split = false;

	//multivariate datasets
	public String dimension_selection = "random";
	public int min_dimensions = 1;
	public int max_dimensions = 0; // if 0 then use all available dimensions
	
	//tree stopping criteria
	public static int minimum_gini = 0;
	public static int maximum_depth = -1; //-1 = unlimited
	
	public static StoppingCriteria stopping_criterias[] = {
			StoppingCriteria.MaxDepth, 
			StoppingCriteria.NoPurityImprovementOverParent, 
			StoppingCriteria.PurityThreshold
		};

	//TODO move these to DataStore class -- to manage multiple transformations, bagging, boosting, etc..
	private static transient Dataset train_data;
	private static transient Dataset test_data;
	private static String datasetName; 
	public static String currentOutputFilePrefix;
	
	public static int num_classes; //in both training and test data TODO refactor 
	public static int[] unique_classes;//in both training and test data TODO refactor 
	public static TIntIntMap class_distribution; //in both training and test data
	public static String print_decimal_places = "#0.00000";
	public static DecimalFormat df = new DecimalFormat(AppConfig.print_decimal_places);

	
	//@charlotte experiments 
	public static boolean boosting = false;
	public static int min_leaf_size = 1;
	public static boolean gini_split = false;	
	//end @charlotte experiments
	
	
	/*
	 * 
	 * These settings are related to splitters, their default values or values overridden by command line args may be adjusted during
	 * input validations. For example: if #features to use by random forest is greater than #attributes then #features is adjusted
	 * 
	 */

	public static int num_splitters_per_node;
	//if using_probabilities_to_choose_num_splitters_per_node == false, then use exact numbers
	public static int ee_splitters_per_node;
	public static int randf_splitters_per_node;
	public static int rotf_splitters_per_node;
	public static int st_splitters_per_node;
	public static int boss_splitters_per_node;
	public static int tsf_splitters_per_node;
	public static int rif_splitters_per_node;
	public static int it_splitters_per_node;
	public static int rt_splitters_per_node;
	
	public static boolean ee_enabled = false;
	public static boolean randf_enabled = false;
	public static boolean rotf_enabled = false;
	public static boolean st_enabled = false;
	public static boolean boss_enabled = false;
	public static boolean tsf_enabled = false;
	public static boolean rif_enabled = false;
	public static boolean it_enabled = false; //inception time
	public static boolean rt_enabled = false; //rocket
	
	//actual num of splitters needed
	public static int num_actual_splitters_needed;
	public static int num_actual_tsf_splitters_needed;
	public static int num_actual_rif_splitters_needed_per_type;
	
	//ED
	public static boolean gini_approx = false;
	public static double gini_percent = 1;	
	public static int approx_gini_min = 10;	
	public static boolean approx_gini_min_per_class = true;

	public static boolean ee_use_random_params = true;
	public static int windowSize = -1; //-1 = full window
	public static double epsilon = 0.01; // for double comparison
	public static double cost = 1; // cost of matching/missmatch, msm = c, erp = g, twe = lambda
	public static double penalty = 0; //penalty for large warping, wdtw = g, twe = nu

	//BOSS
//	public static String boss_params = "random"; // {random, best, cv_on_sample} 
	public static String boss_params_files = "cache/boss/"; // default path if no path is provided in cmd args. A path is needed if ParamSelection method is PreLoadedSet
	public static transient HashMap<String, List<BossParams>> boss_preloaded_params;
	public static int boss_trasformations = 1000;
//	public static boolean boss_transform_at_forest_level = true;	//transform once per tree, or store many at the forest and at each node use at most num_splitters_per_node params to evaluate
//	public static boolean boss_use_gini_split = false;
	public static boolean boss_use_numerosity_reduction = true;
	public static TransformLevel boss_transform_level = TransformLevel.Forest;
	public static ParamSelection boss_param_selection = ParamSelection.Random;
	public static SplitMethod boss_split_method = SplitMethod.NearestClass;	
	
	//RandF
	public static int randf_m;
	public static FeatureSelectionMethod randf_feature_selection = FeatureSelectionMethod.Sqrt; 

	//TSF
	public static int tsf_trasformations;
	public static TransformLevel tsf_transform_level = TransformLevel.Node;
	public static int tsf_m = 0; // use all attributes, feature bagging is done by selecting random intervals
	public static FeatureSelectionMethod tsf_feature_selection = FeatureSelectionMethod.ConstantInt;
	public static int tsf_min_interval = 3;
	public static int tsf_num_intervals = 1;	//if 0 then sqrt(length) is used
	
	//RISE
	public static int rif_trasformations;
	public static TransformLevel rif_transform_level = TransformLevel.Node;
	public static int rif_m = 0; // use all attributes, feature bagging is done by selecting random intervals
	public static FeatureSelectionMethod rif_feature_selection = FeatureSelectionMethod.ConstantInt;
	public static int rif_min_interval = 16;
	public static RifFilters rif_components = RifFilters.ACF_PACF_ARMA_PS_separately;
	public static boolean rif_same_intv_component = false; //TODO //share same random interval for each set of component at least once per num rif splitters
	public static int rif_num_intervals = 1; //if 0 its determined auto, by using approx_attribs_test_per_splitter

	//ST
	public static boolean st_normalize = true;
	public static int st_min_length_percent = -1; //TODO DEV if negative ignore percentages -- use absolute values based on st_min_length  and st_max_length
	public static int st_max_length_percent = -1; //TODO DEV if negative ignore percentages -- use absolute values
	public static int st_min_length = 1; //if <= 0 or if > series length, this will be adjusted to series length
	public static int st_max_length = 0; //if <= 0 or if > series length, this will be adjusted to series length
	public static String st_interval_method = "lengthfirst"; //{swap, lengthfirst}
	public static String st_params_files = "cache/st-5356/"; // default path if no path is provided in cmd args. --- this is for a specific experiment:  refer to ShapeletTransformDataLoader class
	public static ParamSelection st_param_selection = ParamSelection.Random; // {"random", "preload", "preload_data"} //preload -- loads only params and transforms in memory, preload_data loads params and transformed dataset from disk
	public static ThresholdSelectionMethod st_threshold_selection = ThresholdSelectionMethod.BestGini; // how do we choose split point using the cut value that gives lowest gini? randomly? using median value?
	public static boolean st_per_class = false; // -- TODO DEV need to implement
	public static int st_num_rand_thresholds = 1; //if using random threadhold for -st_threshold_selection, how many random values to test
	public static FeatureSelectionMethod st_feature_selection = FeatureSelectionMethod.ConstantInt; //default is st_num_splitters to make sure same number of gini is calculated
	
	//InceptionTime
	public static int it_m = 0;
	public static FeatureSelectionMethod it_feature_selection = FeatureSelectionMethod.ConstantInt;
	public static TransformLevel it_transform_level = TransformLevel.Forest;
	public static String it_cache_dir = "cache/inception/";

	
	//RocketTree
	public static int rt_kernels;
	public static int rt_m = 0;
	public static FeatureSelectionMethod rt_feature_selection = FeatureSelectionMethod.ConstantInt;
	public static TransformLevel rt_transform_level = TransformLevel.Forest;
	public static String rt_cache_dir = "cache/rocket_tree/";
	public static SplitMethod rt_split_method = SplitMethod.RidgeClassifier;
	public static boolean rt_cross_validate_at_node = true;
	public static boolean rt_normalize = true;
	public static double rt_alphas[] = {1.00000000e-03, 4.64158883e-03, 2.15443469e-02, 1.00000000e-01,
		       4.64158883e-01, 2.15443469e+00, 1.00000000e+01, 4.64158883e+01,
		       2.15443469e+02, 1.00000000e+03}; //np.logspace(-3, 3, 10)
	//distributions for
	//length
	//weights
	//bias
	//dilation
	//padding

	//Multivar
	public static int lpNormForCombiningIndependentDimensions = 2;
	public static DimensionDependencyMethod dimensionDependencyMethod = DimensionDependencyMethod.both;
	public static String dimensionSubsetsAsString;
	public static int[][] dimensionSubsets;
	public static boolean useRandomSubsetsOfDimensions = true;
	public static int numRandomSubsetsOfDimensionsPerNode = 1;
	public static int maxDimensionsPerSubset = 0;
	public static int minDimensionsPerSubset = 1;

	public enum DimensionDependencyMethod{
		independent,
		dependent,
		both;
	}
	
	public enum SplitterType{
		ElasticDistanceSplitter,
		RandomForestSplitter,
		RandomPairwiseShapeletSplitter,
		ShapeletTransformSplitter,
		RotationForestSplitter,
		CrossDomainSplitter,
		WekaSplitter,
		BossSplitter,
		TSFSplitter,
		RIFSplitter,
		InceptionTimeSplitter,
		RocketTreeSplitter
	}
	
	public enum StoppingCriteria{
		MaxDepth,
		NoPurityImprovementOverParent,
		PurityThreshold,
	}
	
	//command line args will fill this array
	public static SplitterType[] enabled_splitters = new SplitterType[] {
//			SplitterType.ElasticDistanceSplitter
//			SplitterType.ShapeletTransformSplitter,
//			SplitterType.RandomForestSplitter,
//			SplitterType.RotationForestSplitter
	};
	
	public enum TransformLevel{
		Forest,
		Tree,
		Node
	}
	
	//different splitter types may use different methods to split the data. some methods may not make sense for some splitters
	//while some splitters may support more than one method. -- use other options to control the behavior 
	public enum SplitMethod{
		//uses classic decision tree split method, which may use one of the SplitEvaluateMethods such as gini 
		//results in binary splits
		//eg. RandomForest splitter uses this method
		RandomTreeStyle,	
		//picks one example per class and calculate distance/similarity to exemplar to split
		//results in n'nary splits
		//eg. ElasticDistance splitter uses this method
		NearestClass,	
		
		//TODO -- work on this
		//picks two examples and finds distance to them, then uses gini to find a good split point... 
		//eg Similarity Forest uses this method -- binary splits implemented by Charlotte uses this method
		BinarySplitNearestNeighbour,
		
		RidgeClassifier,
		LogisticClassifier,
	}
	
	//TODO only gini is implemented
	public enum SplitEvaluateMethods{
		Gini,
		InformationGain,
		GainRatio
	}
	
	public enum ThresholdSelectionMethod{
		BestGini,
		Median,
		Random
	}
	
	public enum FeatureSelectionMethod{
		Sqrt,
		Log2,
		Loge,
		ConstantInt	//if FeatureSelectionMethod == ConstantInt and m < 1 or m > length,then all attributes are used
	}	
	
	public enum RifFilters{
		ACF,
		PACF,
		PS,
		ARMA,
		ACF_PACF_ARMA,
		ACF_PACF_ARMA_PS_separately,	//divides num_rif_splitters / 4, and creates separate rif spliters for each individual type
		ACF_PACF_ARMA_PS_combined,	
		ACF_PACF_ARMA_DFT,
		DFT,
	}	
	
	public enum ParamSelection{
		Random,
		PreLoadedParams,	//precalculated parameter values are loaded from files -- must provide a path to files in the cmd args.
		PraLoadedDataAndParams, //precalculated parameter values and transformed dataset(s) are loaded from files -- must provide a path to files in the cmd args.
		CrossValidation	//not implemented
	}
		
	public static MEASURE[] enabled_distance_measures = new MEASURE[] {
			MEASURE.euclidean,
			MEASURE.dtw,
			MEASURE.dtwcv,
			MEASURE.ddtw,
			MEASURE.ddtwcv,
			MEASURE.wdtw,
			MEASURE.wddtw,
			MEASURE.lcss,
			MEASURE.erp,
			MEASURE.twe,
			MEASURE.msm	
	};	

// no longer using the static initializer --  calling initializeAppConfig() explicitly in Main Application now
//	static {
//		
//	}

	public static void initializeAppConfig() throws Exception {
		
		//-- DO THIS AFTER parsing cmd line
		//initialize random seed 
		
		//TODO WARNING implementing this feature is not complete or it may be broken due to inconsistencies in implementation among different classes
		//NOTE many classes use ThreadLocalRandom which doesnt allow setting a seed
		// research more about this -- SplitRandom is also used by some classes
		if (rand_seed == 0) {	//if seed is 0 then lets use time this is the default
			rand_seed = System.nanoTime(); //overwrite and store the new seed so that it can be exported later
			rand = new Random(rand_seed);
		}else {
			rand = new Random(rand_seed);	
		}
		
		int test1 = rand.nextInt();
		int test2 = rand.nextInt();

		//init some classes that depend on random numbers
//		Sampler.setRand(AppConfig.rand);
//		Util.setRand(rand);

		//detect runtime environment info
		collectRuntimeEnvironmentInfo();
		
		//init some experiment management data -- this is to help organize data exported from experiments
		AppConfig.current_experiment_id = java.util.UUID.randomUUID().toString().substring(0, 8);
		//these unique ids helps to keep the output folder structure clean
		AppConfig.repeat_guids = new ArrayList<>(AppConfig.num_repeats);
		for (int i = 0; i < AppConfig.num_repeats; i++) {
			AppConfig.repeat_guids.add(java.util.UUID.randomUUID().toString().substring(0, 8));
		}
		
		//DEBUG feature -- used only in experiments when we measure very short runtimes. 
		//-- this code helps warm up java virtual machine by loading classes early -- helps measure more accurate time in some experiments
		if (AppConfig.warmup_java) {
			Util.warmUpJavaRuntime();
		}

		AppConfig.ucr2015 = new UCR2015(AppConfig.data_dir + "/" + UCR2015.archiveName + "/");
		AppConfig.ucr2018uts = new DataArchive.UCR2018UTS(AppConfig.data_dir + "/" + DataArchive.UCR2018UTS.archiveName + "/");
		AppConfig.ucr2018mts = new DataArchive.UCR2018MTS(AppConfig.data_dir + "/" + DataArchive.UCR2018MTS.archiveName + "/");

	}
	
	public static void collectRuntimeEnvironmentInfo() throws UnknownHostException {
	
        AppConfig.host_name = InetAddress.getLocalHost().getHostName();
        AppConfig.application_start_timestamp = LocalDateTime.now();
        AppConfig.application_start_timestamp_formatted = AppConfig.application_start_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT));			
        
        
        //check if running on cluster and store some env variables related to cluster 
        //this is also to help organize the experiments
        
        //TODO store slurm variables such as slurm job id
        
	}
	
	public static Random getRand() {
		return rand;
	}

	public static void setRand(Random newRandom) {
		rand = newRandom;
	}
	
	public static Dataset getTrainingSet() {
		return train_data;
	}

	public static void setTrainingSet(Dataset train_data) {
		AppConfig.train_data = train_data;
	}

	public static Dataset getTestingSet() {
		return test_data;
	}

	public static void setTestingSet(Dataset test_data) {
		AppConfig.test_data = test_data;
	}

	public static String getDatasetName() {
		return datasetName;
	}

	public static void setDatasetName(String datasetName) {
		AppConfig.datasetName = datasetName;
	}
	
	public static String getVersionString(boolean pathFriendly) {
		if (pathFriendly) {
			return AppConfig.version.replace(".", "-");
		}else {
			return AppConfig.version;
		}
		
	}
	
	public static void updateClassDistribution(Dataset train_data, Dataset test_data) {
		
		class_distribution = new TIntIntHashMap();
		
		//create a copy //TODO can be done better? 
		for (int key : train_data.getClassDistribution().keys()) {
			if (class_distribution.containsKey(key)) {
				class_distribution.put(key, class_distribution.get(key) + train_data.getClassDistribution().get(key));
			}else {
				class_distribution.put(key, train_data.getClassDistribution().get(key));
			}
		}		
		
		
		//create a copy //TODO can be done better? 
		for (int key : test_data.getClassDistribution().keys()) {
			if (class_distribution.containsKey(key)) {
				class_distribution.put(key, class_distribution.get(key) + test_data.getClassDistribution().get(key));
			}else {
				class_distribution.put(key, test_data.getClassDistribution().get(key));
			}		
		}		
		
		num_classes = class_distribution.keys().length;
		unique_classes = class_distribution.keys();
				
	}
	
	
	public static class ClassTypeAdapter extends TypeAdapter<Class<?>> {
	    @Override
	    public void write(JsonWriter jsonWriter, Class<?> clazz) throws IOException {
	    	
	        try {
	        	StringBuilder buffer = new StringBuilder();
	        	
				if(clazz == null){
				    jsonWriter.nullValue();
				    return;
				}

				Field[] fields = AppConfig.class.getFields();
				jsonWriter.beginObject();
				for (Field field : fields) {
					field.setAccessible(true);
					
					boolean isTransient = Modifier.isTransient(field.getModifiers());
					
					if (isTransient) {
						continue;
					}
					
					Object value = field.get(this);
					if (field.getType().isArray()) {
						buffer.setLength(0);
						buffer.append('[');
					    int length = Array.getLength(value);
						jsonWriter.name(field.getName() + "["+ length + "]");

					    
					    for (int i = 0; i < length; i ++) {
					        Object arrayElement = Array.get(value, i);
					        buffer.append(arrayElement.toString());
					        if (i != length -1) {
					        	buffer.append(',');
					        }
					    }
						buffer.append(']');
						jsonWriter.value(buffer.toString());
					}else {
						jsonWriter.name(field.getName());
						if (value != null) {
							jsonWriter.value(value.toString());
						}else {
							jsonWriter.nullValue();
						}
						
					}

				}
				jsonWriter.endObject();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	        

	    }

	    @Override
	    public Class<?> read(JsonReader jsonReader) throws IOException {
	        if (jsonReader.peek() == JsonToken.NULL) {
	            jsonReader.nextNull();
	            return null;
	        }
	        Class<?> clazz = null;
	        try {
	            clazz = Class.forName(jsonReader.nextString());
	        } catch (ClassNotFoundException exception) {
	            throw new IOException(exception);
	        }
	        return clazz;
	    }
	}
	
	public class ClassTypeAdapterFactory implements TypeAdapterFactory {
	    @Override
	    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
	        if(!Class.class.isAssignableFrom(typeToken.getRawType())) {
	            return null;
	        }
	        return (TypeAdapter<T>) new ClassTypeAdapter();
	    }

	}
	
	
	public static String[] getPredefinedSet(int set_id) {
		final int num_sets = 50;
		String[][] datasets = new String[num_sets][];
		
		//set 1
		datasets[0] = new String[] {
				"ECG5000","TwoLeadECG", "SonyAIBORobotSurface1", "CricketZ", "ToeSegmentation1", "DistalPhalanxTW","Meat","Worms",
				"UWaveGestureLibraryZ", "CBF", "ShapeletSim", "Wine", "MiddlePhalanxOutlineCorrect", "Mallat", "Lightning2", 
				"LargeKitchenAppliances", "DistalPhalanxOutlineAgeGroup", "Ham", "MedicalImages", "SyntheticControl", "PhalangesOutlinesCorrect", 
				"UWaveGestureLibraryX", "FiftyWords",  "Computers",  "FacesUCR", "ShapesAll", "InlineSkate", "CinCECGtorso", "Phoneme", "FordA"
		};
		
		//set 2
		datasets[1] = new String[] {
				"ItalyPowerDemand", "BirdChicken", "ChlorineConcentration", "Symbols", "Haptics", "ScreenType", "Wafer", "ProximalPhalanxOutlineAgeGroup", 
				"SonyAIBORobotSurface2", "Herring", "Adiac", "FaceAll", "DistalPhalanxOutlineCorrect", "RefrigerationDevices", "SmallKitchenAppliances", 
				"Earthquakes", "Coffee", "ArrowHead", "ECG200", "OliveOil", "OSULeaf", "CricketY", "BeetleFly","CricketX",
				"MoteStrain",  "Yoga", "FordB", "MiddlePhalanxOutlineAgeGroup",   "StarlightCurves", "NonInvasiveFetalECGThorax2"
		};
		
		//set 3
		datasets[2] = new String[] {
				"Car", "Plane", "TwoPatterns",  "WormsTwoClass", "DiatomSizeReduction", "Fish", "InsectWingbeatSound", 
				"Strawberry", "ProximalPhalanxOutlineCorrect", "Trace", "ECGFiveDays", "GunPoint", "Lightning7",
				"MiddlePhalanxTW", "ToeSegmentation2", "Beef", "WordSynonyms", "FaceFour",  "ElectricDevices",
				"SwedishLeaf", "ProximalPhalanxTW","NonInvasiveFetalECGThorax1", "UWaveGestureLibraryY", "HandOutlines", "UWaveGestureLibraryAll"
		};
		
		//set4 = 8 slowest datasets
		datasets[3] = new String[] {
				"NonInvasiveFetalECGThorax1","NonInvasiveFetalECGThorax2", "FordA", "FordB",
				"Phoneme","UWaveGestureLibraryAll","StarlightCurves", "HandOutlines"
		};
		
		//set5 = 77 without the 8 slowest
		datasets[4] = new String[] {
				"Adiac","ArrowHead","Beef","BeetleFly","BirdChicken","Car","CBF","ChlorineConcentration","CinCECGtorso","Coffee",
				"Computers","CricketX","CricketY","CricketZ","DiatomSizeReduction","DistalPhalanxOutlineAgeGroup",
				"DistalPhalanxOutlineCorrect","DistalPhalanxTW","Earthquakes","ECG200","ECG5000","ECGFiveDays",
				"ElectricDevices", "FaceAll","FaceFour","FacesUCR","FiftyWords","Fish","GunPoint",
				"Ham","Haptics","Herring","InlineSkate","InsectWingbeatSound","ItalyPowerDemand",
				"LargeKitchenAppliances","Lightning2","Lightning7","Mallat","Meat","MedicalImages","MiddlePhalanxOutlineAgeGroup",
				"MiddlePhalanxOutlineCorrect","MiddlePhalanxTW","MoteStrain",
				"OliveOil","OSULeaf","PhalangesOutlinesCorrect","Plane","ProximalPhalanxOutlineAgeGroup",
				"ProximalPhalanxOutlineCorrect","ProximalPhalanxTW","RefrigerationDevices","ScreenType",
				"ShapeletSim","ShapesAll","SmallKitchenAppliances","SonyAIBORobotSurface1","SonyAIBORobotSurface2",
				"Strawberry","SwedishLeaf","Symbols","SyntheticControl","ToeSegmentation1",
				"ToeSegmentation2","Trace","TwoLeadECG","TwoPatterns","UWaveGestureLibraryX",
				"UWaveGestureLibraryY","UWaveGestureLibraryZ","Wafer","Wine","WordSynonyms","Worms","WormsTwoClass","Yoga"
		};

		//set6 -- set 5 and then set 4 ordered with fastest first
		datasets[5] = new String[] {
				"Adiac","ArrowHead","Beef","BeetleFly","BirdChicken","Car","CBF","ChlorineConcentration","CinCECGtorso","Coffee",
				"Computers","CricketX","CricketY","CricketZ","DiatomSizeReduction","DistalPhalanxOutlineAgeGroup",
				"DistalPhalanxOutlineCorrect","DistalPhalanxTW","Earthquakes","ECG200","ECG5000","ECGFiveDays",
				"ElectricDevices", "FaceAll","FaceFour","FacesUCR","FiftyWords","Fish","GunPoint",
				"Ham","Haptics","Herring","InlineSkate","InsectWingbeatSound","ItalyPowerDemand",
				"LargeKitchenAppliances","Lightning2","Lightning7","Mallat","Meat","MedicalImages","MiddlePhalanxOutlineAgeGroup",
				"MiddlePhalanxOutlineCorrect","MiddlePhalanxTW","MoteStrain",
				"OliveOil","OSULeaf","PhalangesOutlinesCorrect","Plane","ProximalPhalanxOutlineAgeGroup",
				"ProximalPhalanxOutlineCorrect","ProximalPhalanxTW","RefrigerationDevices","ScreenType",
				"ShapeletSim","ShapesAll","SmallKitchenAppliances","SonyAIBORobotSurface1","SonyAIBORobotSurface2",
				"Strawberry","SwedishLeaf","Symbols","SyntheticControl","ToeSegmentation1",
				"ToeSegmentation2","Trace","TwoLeadECG","TwoPatterns","UWaveGestureLibraryX",
				"UWaveGestureLibraryY","UWaveGestureLibraryZ","Wafer","Wine","WordSynonyms","Worms","WormsTwoClass","Yoga",
				"Phoneme","UWaveGestureLibraryAll","StarlightCurves",
				"NonInvasiveFetalECGThorax1","NonInvasiveFetalECGThorax2", "FordA", "FordB",
				"HandOutlines"
		};	
				
		//set7 = small to medium datasets
		datasets[6] = new String[] {
				"Adiac", "ArrowHead", "Beef", "BeetleFly", "BirdChicken", "Car", "CBF", "ChlorineConcentration", "CinCECGtorso", "Coffee",
				"Computers", "CricketX", "CricketY", "CricketZ", "DiatomSizeReduction", "DistalPhalanxOutlineAgeGroup", "DistalPhalanxOutlineCorrect", 
				"DistalPhalanxTW", "Earthquakes", "ECG200", "ECG5000", "ECGFiveDays", "FaceFour", "FacesUCR",
				"FiftyWords", "Fish",  "GunPoint", "Ham",  "InsectWingbeatSound", 
				"ItalyPowerDemand", "LargeKitchenAppliances", "Lightning2", "Lightning7",  "Meat", "MedicalImages", "MiddlePhalanxOutlineAgeGroup", 
				"MiddlePhalanxOutlineCorrect", "MiddlePhalanxTW", "MoteStrain", 
				"OliveOil", "OSULeaf", "PhalangesOutlinesCorrect",  "Plane", "ProximalPhalanxOutlineAgeGroup", "ProximalPhalanxOutlineCorrect",
				"ProximalPhalanxTW", "RefrigerationDevices", "ScreenType", "ShapeletSim", "ShapesAll", "SmallKitchenAppliances", 
				"SonyAIBORobotSurface1", "SonyAIBORobotSurface2",  "Strawberry", "SwedishLeaf", "Symbols", "SyntheticControl", 
				"ToeSegmentation1", "ToeSegmentation2", "Trace", "TwoLeadECG", "TwoPatterns",  "Wafer", "Wine", "WordSynonyms",  "Yoga"
		};		
		
		//set8 -- excluded from set 7
		datasets[7] = new String[] {
				 "ElectricDevices", "FaceAll","FordA", "FordB","HandOutlines", "Haptics", "Herring", "InlineSkate","Mallat",
				 "NonInvasiveFetalECGThorax1", "NonInvasiveFetalECGThorax2", "Phoneme","StarlightCurves","UWaveGestureLibraryAll", 
				 "UWaveGestureLibraryX", "UWaveGestureLibraryY", "UWaveGestureLibraryZ","Worms", "WormsTwoClass",
		};	
		

		//set9 -- set 6 and then set 7 ordered with fastest first
		datasets[8] = new String[] {
				"Adiac", "ArrowHead", "Beef", "BeetleFly", "BirdChicken", "Car", "CBF", "ChlorineConcentration", "CinCECGtorso", "Coffee",
				"Computers", "CricketX", "CricketY", "CricketZ", "DiatomSizeReduction", "DistalPhalanxOutlineAgeGroup", "DistalPhalanxOutlineCorrect", 
				"DistalPhalanxTW", "Earthquakes", "ECG200", "ECG5000", "ECGFiveDays", "FaceFour", "FacesUCR",
				"FiftyWords", "Fish",  "GunPoint", "Ham",  "InsectWingbeatSound", 
				"ItalyPowerDemand", "LargeKitchenAppliances", "Lightning2", "Lightning7",  "Meat", "MedicalImages", "MiddlePhalanxOutlineAgeGroup", 
				"MiddlePhalanxOutlineCorrect", "MiddlePhalanxTW", "MoteStrain", 
				"OliveOil", "OSULeaf", "PhalangesOutlinesCorrect",  "Plane", "ProximalPhalanxOutlineAgeGroup", "ProximalPhalanxOutlineCorrect",
				"ProximalPhalanxTW", "RefrigerationDevices", "ScreenType", "ShapeletSim", "ShapesAll", "SmallKitchenAppliances", 
				"SonyAIBORobotSurface1", "SonyAIBORobotSurface2",  "Strawberry", "SwedishLeaf", "Symbols", "SyntheticControl", 
				"ToeSegmentation1", "ToeSegmentation2", "Trace", "TwoLeadECG", "TwoPatterns",  "Wafer", "Wine", "WordSynonyms",  "Yoga",
				"FaceAll","Haptics", "Herring", "InlineSkate","Mallat",
				"UWaveGestureLibraryX", "UWaveGestureLibraryY", "UWaveGestureLibraryZ","Worms", "WormsTwoClass",
				"ElectricDevices",
				"Phoneme","StarlightCurves","UWaveGestureLibraryAll", 
				"FordA", "FordB",
				"NonInvasiveFetalECGThorax1", "NonInvasiveFetalECGThorax2", 
				"HandOutlines",
		};	
		
		
		//ordered by fastest first, split into 4 sets
		
		//set10
		datasets[9] = new String[] {
				"SonyAIBORobotSurface1", "SonyAIBORobotSurface2", "ItalyPowerDemand", "TwoLeadECG", "MoteStrain", "ECGFiveDays", "CBF", 
				"GunPoint", "Coffee", "DiatomSizeReduction", "ArrowHead", "ECG200", "FaceFour", "BirdChicken", "BeetleFly", 
				"ToeSegmentation1", "ToeSegmentation2", "Symbols", "Wine", "ShapeletSim", "Plane", "SyntheticControl", "OliveOil", 
				"Beef", "Trace", "Meat", "DistalPhalanxTW", "ProximalPhalanxOutlineAgeGroup", "DistalPhalanxOutlineAgeGroup", 
				"MiddlePhalanxOutlineAgeGroup", "ProximalPhalanxTW", "MiddlePhalanxTW", "Lightning7", "FacesUCR", "MedicalImages", 
				"Herring", "Car", "MiddlePhalanxOutlineCorrect", "DistalPhalanxOutlineCorrect", "ProximalPhalanxOutlineCorrect", 
		};
				
		//set11
		datasets[10] = new String[] {
				"Ham", "ECG5000", "Lightning2", "SwedishLeaf", "InsectWingbeatSound", "TwoPatterns", "Wafer", "FaceAll", "Fish", 
				"Mallat", "OSULeaf", "ChlorineConcentration", "Adiac", "Strawberry", "WordSynonyms", "Yoga", "CinCECGtorso", 
				"PhalangesOutlinesCorrect", "CricketX", "CricketY", "CricketZ", "Computers", "Earthquakes"
		};	
				
		//set12
		datasets[11] = new String[] {
				"UWaveGestureLibraryX", "UWaveGestureLibraryZ", "UWaveGestureLibraryY", "WormsTwoClass", "FiftyWords", "Worms", 
				"SmallKitchenAppliances", "Haptics", "LargeKitchenAppliances", "ScreenType", "RefrigerationDevices", "ElectricDevices", 
				"InlineSkate", "ShapesAll"
		};	
		
		//set13
		datasets[12] = new String[] {
				"StarlightCurves", "Phoneme", "UWaveGestureLibraryAll", "FordA", "FordB", 
				"NonInvasiveFetalECGThorax2", "NonInvasiveFetalECGThorax1", "HandOutlines"
		};	
		
		//set14 -> reserve
		
		
		//set15 - > 40 random sorted by train time --     #p.get_sorted_rand_datasets(p.chief_exp['e5b100r100k100'], p.datasets,40, 5)['dataset']
		//ItalyPowerDemand,TwoLeadECG,Car,GunPoint,Coffee,FaceFour,ArrowHead,BeetleFly,Wine,ShapeletSim,Beef,MiddlePhalanxOutlineAgeGroup,DistalPhalanxTW,DistalPhalanxOutlineAgeGroup,Lightning7,MedicalImages,MiddlePhalanxOutlineCorrect,CBF,DistalPhalanxOutlineCorrect,ProximalPhalanxOutlineCorrect,Ham,Lightning2,ECG200,SwedishLeaf,TwoPatterns,Mallat,ChlorineConcentration,Yoga,CinCECGtorso,PhalangesOutlinesCorrect,CricketY,FiftyWords,UWaveGestureLibraryZ,Worms,Haptics,StarlightCurves,Phoneme,UWaveGestureLibraryAll,FordA,NonInvasiveFetalECGThorax1
		datasets[15] =  new String[] {
				"ItalyPowerDemand","TwoLeadECG","Car","GunPoint","Coffee","FaceFour","ArrowHead","BeetleFly"
				,"Wine","ShapeletSim","Beef","MiddlePhalanxOutlineAgeGroup","DistalPhalanxTW","DistalPhalanxOutlineAgeGroup"
				,"Lightning7","MedicalImages","MiddlePhalanxOutlineCorrect","CBF","DistalPhalanxOutlineCorrect"
				,"ProximalPhalanxOutlineCorrect","Ham","Lightning2","ECG200","SwedishLeaf","TwoPatterns","Mallat"
				,"ChlorineConcentration","Yoga","CinCECGtorso","PhalangesOutlinesCorrect","CricketY","FiftyWords","UWaveGestureLibraryZ","Worms"
				,"Haptics","StarlightCurves","Phoneme","UWaveGestureLibraryAll","FordA","NonInvasiveFetalECGThorax1"
		};
		
		/*
		 * util.misc.py
		# default random seed = 7787765 -- edited HandOutlines <-> StarlightCurves
				# sample
				# CBF,Computers,CricketX,CricketZ,DiatomSizeReduction,DistalPhalanxOutlineAgeGroup,DistalPhalanxOutlineCorrect,DistalPhalanxTW,
				# ECG200,ECG5000,Earthquakes,FaceFour,FacesUCR,FordB,GunPoint,HandOutlines,Herring,InsectWingbeatSound,ItalyPowerDemand,
				# LargeKitchenAppliances,Lightning7,Mallat,Meat,MiddlePhalanxOutlineAgeGroup,MiddlePhalanxTW,NonInvasiveFetalECGThorax1,
				# OSULeaf,OliveOil,Plane,RefrigerationDevices,ScreenType,ShapesAll,SmallKitchenAppliances,SwedishLeaf,SyntheticControl,
				# ToeSegmentation1,UWaveGestureLibraryX,UWaveGestureLibraryY,Wafer,Worms
				# not_in_sample
				# Adiac,ArrowHead,Beef,BeetleFly,BirdChicken,Car,ChlorineConcentration,CinCECGtorso,Coffee,CricketY,ECGFiveDays,ElectricDevices,\
				# FaceAll,FiftyWords,Fish,FordA,Ham,Haptics,InlineSkate,Lightning2,MedicalImages,MiddlePhalanxOutlineCorrect,MoteStrain,\
				# NonInvasiveFetalECGThorax2,PhalangesOutlinesCorrect,Phoneme,ProximalPhalanxOutlineAgeGroup,ProximalPhalanxOutlineCorrect,\
				# ProximalPhalanxTW,ShapeletSim,SonyAIBORobotSurface1,SonyAIBORobotSurface2,StarlightCurves,Strawberry,Symbols,ToeSegmentation2,\
				# Trace,TwoLeadECG,TwoPatterns,UWaveGestureLibraryAll,UWaveGestureLibraryZ,Wine,WordSynonyms,WormsTwoClass,Yoga
				*/
				//dev set
		datasets[16] =  new String[] {
				"CBF","Computers","CricketX","CricketZ","DiatomSizeReduction","DistalPhalanxOutlineAgeGroup","DistalPhalanxOutlineCorrect",
				"DistalPhalanxTW","ECG200","ECG5000","Earthquakes","FaceFour","FacesUCR","FordB","GunPoint","StarlightCurves",
				"Herring","InsectWingbeatSound","ItalyPowerDemand","LargeKitchenAppliances","Lightning7","Mallat","Meat",
				"MiddlePhalanxOutlineAgeGroup","MiddlePhalanxTW","NonInvasiveFetalECGThorax1","OSULeaf","OliveOil","Plane",
				"RefrigerationDevices","ScreenType","ShapesAll","SmallKitchenAppliances","SwedishLeaf","SyntheticControl",
				"ToeSegmentation1","UWaveGestureLibraryX","UWaveGestureLibraryY","Wafer","Worms"
		};
				//holdout set
		datasets[17] =  new String[] {
				"Adiac","ArrowHead","Beef","BeetleFly","BirdChicken","Car","ChlorineConcentration","CinCECGtorso","Coffee","CricketY",
				"ECGFiveDays","ElectricDevices","FaceAll","FiftyWords","Fish","FordA","Ham","Haptics","InlineSkate","Lightning2",
				"MedicalImages","MiddlePhalanxOutlineCorrect","MoteStrain","NonInvasiveFetalECGThorax2","PhalangesOutlinesCorrect",
				"Phoneme","ProximalPhalanxOutlineAgeGroup","ProximalPhalanxOutlineCorrect","ProximalPhalanxTW","ShapeletSim",
				"SonyAIBORobotSurface1","SonyAIBORobotSurface2","Strawberry","Symbols","ToeSegmentation2",
				"Trace","TwoLeadECG","TwoPatterns","UWaveGestureLibraryAll","UWaveGestureLibraryZ","Wine","WordSynonyms","WormsTwoClass","Yoga","HandOutlines"
		};
		
		return datasets[set_id-1];
	}

	public static String[] getNRandomDatasets(int num_datasets) {
		String[] datasets = getAllDatasets();
		
		String[] random_set = new String[num_datasets];
		
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < datasets.length; i++) {
			indices.add(i);
		}
		
		Collections.shuffle(indices, getRand());
		
		for (int i = 0; i < random_set.length; i++) {
			random_set[i] = datasets[indices.get(i)];
		}
		
		return random_set;
	}
	

	public static String[] getSubsetOfDatasets(int start, int end) {
		String[] datasets = getAllDatasets();
		String[] subset = new String[end - start];
		
		int j = 0;
		for (int i = start; i < end; i++) {
			subset[j++] = datasets[i];
		}
		
		return subset;
	}

	public static String[] getAllDatasets() {
		String[] datasets = {
				"Adiac", "ArrowHead", "Beef", "BeetleFly", "BirdChicken", "Car", "CBF", "ChlorineConcentration", "CinCECGtorso", "Coffee",
				"Computers", "CricketX", "CricketY", "CricketZ", "DiatomSizeReduction", "DistalPhalanxOutlineAgeGroup", "DistalPhalanxOutlineCorrect", 
				"DistalPhalanxTW", "Earthquakes", "ECG200", "ECG5000", "ECGFiveDays", "ElectricDevices", "FaceAll", "FaceFour", "FacesUCR",
				"FiftyWords", "Fish", "FordA", "FordB", "GunPoint", "Ham", "HandOutlines", "Haptics", "Herring", "InlineSkate", "InsectWingbeatSound", 
				"ItalyPowerDemand", "LargeKitchenAppliances", "Lightning2", "Lightning7", "Mallat", "Meat", "MedicalImages", "MiddlePhalanxOutlineAgeGroup", 
				"MiddlePhalanxOutlineCorrect", "MiddlePhalanxTW", "MoteStrain", "NonInvasiveFetalECGThorax1", "NonInvasiveFetalECGThorax2", 
				"OliveOil", "OSULeaf", "PhalangesOutlinesCorrect", "Phoneme", "Plane", "ProximalPhalanxOutlineAgeGroup", "ProximalPhalanxOutlineCorrect",
				"ProximalPhalanxTW", "RefrigerationDevices", "ScreenType", "ShapeletSim", "ShapesAll", "SmallKitchenAppliances", 
				"SonyAIBORobotSurface1", "SonyAIBORobotSurface2", "StarlightCurves", "Strawberry", "SwedishLeaf", "Symbols", "SyntheticControl", 
				"ToeSegmentation1", "ToeSegmentation2", "Trace", "TwoLeadECG", "TwoPatterns", "UWaveGestureLibraryAll", "UWaveGestureLibraryX", 
				"UWaveGestureLibraryY", "UWaveGestureLibraryZ", "Wafer", "Wine", "WordSynonyms", "Worms", "WormsTwoClass", "Yoga"
			};
		
		return datasets;
	}

	//NOTE this experiment is set up for UCR datasets -- it will work for any dataset if params are stored in the same format
	public static HashMap<String, List<BossParams>> loadBossParams() throws Exception{
		HashMap<String, List<BossParams>> boss_params = new HashMap<>();
		
		System.out.println("Loading precalculated boss params from: " + AppConfig.boss_params_files);

		File folder = new File(AppConfig.boss_params_files);
		File[] listOfFiles = folder.listFiles();
		
		String[] datasets = getAllDatasets();

		for (File file : listOfFiles) {
		    if (file.isFile()) {
		        String dataset = findMatch(file.getName(), datasets);
		        
		        if (dataset != null) {
		        	List<BossParams> param_list = loadBossParamFile(file);

		        	boss_params.put(dataset, param_list);

		        }else {
		        	throw new Exception("Failed to load boss params from the file: " + file.getName());
		        }
		    }
		}
		
		
		return boss_params;
	}
	
	public static List<BossParams> loadBossParamFile(File file) throws FileNotFoundException{
		List<BossParams> param_list = new ArrayList<>();
		
		Scanner s = new Scanner(file);
		while (s.hasNext()){
			String line = s.next();
			String[] words = line.split(",");
			
			boolean norm = Boolean.parseBoolean(words[1]);
			int w = Integer.parseInt(words[2]);
			int l = Integer.parseInt(words[3]);
			
			BossParams params = new BossParams(norm, w, l, 4);
			
			param_list.add(params);
		}
		s.close();
		
		return param_list;
	}
	
	public static String findMatch(String fileName, String[] datasets) {
		for (String string : datasets) {
			
			//HOTFIX
			
			String temp = fileName.toLowerCase();
			temp = temp.substring(0, temp.indexOf("-"));
			
			if (temp.equals(string.toLowerCase())) { 
				return string;
			}			
		}
		return null;
	}
	
}
