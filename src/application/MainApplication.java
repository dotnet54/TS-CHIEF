package application;

import java.util.ArrayList;
import java.util.Arrays;

import application.AppConfig.FeatureSelectionMethod;
import application.AppConfig.ParamSelection;
import application.AppConfig.RifFilters;
import application.AppConfig.SplitMethod;
import application.AppConfig.SplitterType;
import application.AppConfig.ThresholdSelectionMethod;
import application.AppConfig.TransformLevel;
import core.threading.ExperimentRunner;
import distance.univariate.MEASURE;
import util.*;
import util.repo.DataArchive;
import util.repo.UCR2015;

/**
 * Main entry point for TS-CHIEF application
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class MainApplication {

	public static final String[] public_release_test_args = new String[]{
			"-train=E:/data/ucr2015/ItalyPowerDemand/ItalyPowerDemand_TRAIN.txt",
			"-test=E:/data/ucr2015/ItalyPowerDemand/ItalyPowerDemand_TEST.txt",
			"-target_column=first",
			"-csv_has_header=false",
			"-repeats=1",
			"-results=out/results.csv",
			"-out=out/ts_chief/dev/",
			"-boss_trasformations=1000",
			"-trees=100",
			"-s=ee:5,boss:100,rif:100,st:2,it:5",
			"-export=3",
			"-threads=0",
			"-seed=0"
	};

	public static final String[] dev_args = new String[]{
			"-machine=pc",
			"-out=out/local/multiv/ucr2018m_e5k100_d1_r0",
			"-results=out/local/multiv/ucr2018m_e5k100_d1_r0.csv",
			"-threads=0",
			"-repeats=1",
			"-export=3",
			"-verbosity=1",
			"-seed=0",  //TODO WARNING doesnt work with BOSS due to HPPC library, also if -threads > 1
			"-trees=100",

			// uncomment for multivariate experiments
			"-archive=Multivariate2018_ts",
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
//			"-datasets=ArticularyWordRecognition,AtrialFibrillation,BasicMotions,Cricket,DuckDuckGeese,Epilepsy,ERing," +
//					"EthanolConcentration,FaceDetection,FingerMovements,HandMovementDirection,Handwriting,Heartbeat," +
//					"Libras,LSST,MotorImagery,NATOPS,PEMS-SF,PenDigits,PhonemeSpectra,RacketSports,SelfRegulationSCP1," +
//					"SelfRegulationSCP2,StandWalkJump,UWaveGestureLibrary",
			"-datasets=BasicMotions",
			"-s=ee:5",
//			"-ee_dm=euc",
//			"-ee_dm=euc,dtw,dtwr,ddtw,ddtwr,wdtw,wddtw,lcss,twe,erp,msm",
			"-ee_use_random_params=true",
			"-ee_window_size=-1", //-1 = full window
			"-ee_epsilon=0.01", // for double comparison
			"-ee_cost=1", 	// cost of matching/missmatch, msm = c, erp = g, twe = lambda
			"-ee_penalty=0",	//penalty for large warping, wdtw = g, twe = nu

			"-dim_dependency=independent",
			"-dimensions={{0},{1},{10000}}",
			"-use_random_dimensions=true",
			"-num_dim_subsets=1",
			"-min_dimensions=1",
			"-max_dimensions=0" // {total_dimensions >= int >= min_dimensions, log, sqrt}

			// uncomment for univariate experiments
//			"-archive=ucr2015",
//			"-datasets=all", // ToeSegmentation2 ItalyPowerDemand, HandOutlines
//			"-s=ee:5",

			// uncomment for ee experiments
//			"-ee_dm=euc",
//			"-ee_dm=euc,dtw,dtwr,ddtw,ddtwr,wdtw,wddtw,lcss,twe,erp,msm",

			// uncomment for st experiments
////			"-st_min_length_percent=0",
////			"-st_max_length_percent=1",
//			"-st_min_length=10",
//			"-st_max_length=0",
//			"-st_interval_method=lengthfirst", //{lengthfirst, swap} method used to sample intervals
////			"-s=st:100",
//			"-st_params=preload_data",  //{random, preload,preload_data} --randomly or load params from files (default location for -st_params_files=settings/st)
//			"-st_threshold_selection=bestgini", //how do we choose split point ? { bestgini, median, random}
//			"-st_num_rand_thresholds=1", // > 1
//			"-st_feature_selection=int",
//			"-st_params_files=settings/st-5356/",

			// uncomment for it experiments
//			"-s=ee:5,boss:100,rif:100,it:1",
//			"-it_m=100",

			// uncomment for rt experiments
//			"-rt_m=100",
	};

	//TODO: CAUTION timing experiments should be done using a single thread,
	// there may be a stats counter which needs to be changed to an AtomicInetger, need to check and verify this.
	// setting a random seed also doesnt work in a multithreaded environment due to limitations
	// of Java's ThreadLocalRandom Class
	// (its also broken in single thread if using BOSS, because of HPPC library's
	// Hashmap's non-deterministic iteration order -- will try to fix this eventually)
	
	public static void main(String[] args) {
		try {	 
			AppConfig.version = "0.31.0";
			AppConfig.buld_number = "2020.8.3.0";
			AppConfig.version_tag = "ts_chief clean up and big fixes";
			AppConfig.release_notes = AppConfig.release_notes + String.join(
					"0.31.0 clean up before public release - moving bug fixes to master, WIP dev branch");
			
			args = public_release_test_args; //COMMENT before compiling jar file
			
			AppConfig.cmd_args = args; //store the original command line args
			System.out.println("ARGS:" + String.join(" ", args));
			
			//parse command line args -- during parsing cmd args, default values in AppConfig may be overwritten
			parseCMD(args);
			
			//initialize app configs after reading cmd args -- this partly initializes AppConfig
			//the values that cannot be initialized properly at this stage are initialized during input validation
			//after reading the input datafiles, because some validation requires information such as dataset size or length
			//input validaiton is done inside ExperimentRunner class
			AppConfig.initializeAppConfig();
			
			//NOTE:
			// --  since parsed command line values may be changed to some default values during input validation,
			// --  actual settings used during execution may be different.
			// --  serializing AppConfig after input validation will give the actual configuration settings used during execution.
			// --  runtime settings can be exported using the -export_level args -- AppConfig is serialized to a json file. (TODO fix this)			

			//If this is an experiment using the UCR archive, then some assumptions are made to keep things easy
			if (AppConfig.archiveName != null && ! AppConfig.archiveName.isEmpty()) {
				//-archive option supports multiple datasets to be specified,
				//loop through them and send each dataset to ExperimentRunner one by one, ExperimentRunner 

				//if using -archive option -train is assumed to be data_dir name instead of full file name
				AppConfig.data_dir = AppConfig.training_file;

				switch (AppConfig.archiveName) {
					case UCR2015.archiveName:
						AppConfig.archive = AppConfig.ucr2015;
						break;
					case DataArchive.UCR2018UTS.archiveName:
						AppConfig.archive = AppConfig.ucr2018uts;
						break;
					case DataArchive.UCR2018MTS.archiveName:
						AppConfig.archive = AppConfig.ucr2018mts;
						break;
				}

				for (int i = 0; i < AppConfig.datasets.length; i++) {
					String current_dataset = AppConfig.datasets[i];
					AppConfig.training_file = AppConfig.archive.getTrainingFileFullPath(current_dataset);
					AppConfig.testing_file = AppConfig.archive.getTestingFileFullPath(current_dataset);
					
					AppConfig.training_file = Util.toOsPath(AppConfig.training_file); // because I work on both windows and mac
					AppConfig.testing_file = Util.toOsPath(AppConfig.testing_file);	// because I work on both windows and mac
					
					System.out.println("--------------------------------    START UCR DATASET: " + i + " (" + current_dataset + ")   -------------------------");
					
					//experiment runner creates forests and train them; it handles repeating the experiments for each dataset
					ExperimentRunner experiment = new ExperimentRunner();
					experiment.run();				
					
					System.gc();
					System.out.println("--------------------------------   END DATASET: "+ current_dataset +"  -------------------------------------");
				}
				
			}else {
				
				//each ExperimentRunner object handles training and testing one dataset
				//configuration is read from AppConfig static class -- this is done to keep all settings in one place
				//to easily serialize AppConfig to a json file so that experiment configurations can be exported for future reference easily
				ExperimentRunner experiment = new ExperimentRunner();
				experiment.run();
			}
			

			
		}catch(Exception e) {			
            PrintUtilities.abort(e);
		}
				
		System.exit(0); //TODO this is a hot fix for not properly shutting down executor service in ParallelFor class
	}

	
	private static void parseCMD(String[] args) throws Exception {
		//some default settings are specified in the AppContext class but here we
		//override the default settings using the provided command line arguments		
		for (int i = 0; i < args.length; i++) {
			String[] options = args[i].trim().split("=");

			//make sure that the command line options are parsed case-insensitively except for the following args
			if ((! options[0].equals("-archive"))
					&& (! options[0].equals("-datasets"))
					&& (! options[0].equals("-train"))
					&& (! options[0].equals("-test"))
					&& (! options[0].equals("-out"))
					&& (! options[0].equals("-results"))) {
				options[1] = options[1].trim().toLowerCase();
			}
			
			switch(options[0]) {
			
			//TODO temporary command to make things easier on clusters --will be removed in public releases
			
			
			case "-machine":	//cluster name,pc name
				
				String machine = options[1];
				AppConfig.debug_machine  = machine;
				
				if (machine.equals("m3")) {
					AppConfig.training_file = "../data/";
				}else if (machine.equals("pc")) {
					AppConfig.training_file = "E:/data/";
				}else if (machine.equals("mac")) {
					AppConfig.training_file = "/data/";

				}else if (machine.equals("m3-sat")) {
					AppConfig.training_file = "../../data/SatelliteFull/";
				}else if (machine.equals("pc-sat")) {
					AppConfig.training_file = "E:/data/SatelliteFull/sk_stratified/";
				}else if (machine.equals("mac-sat")) {
					AppConfig.training_file = "/data/SatelliteFull/sk_stratified/";
				}
				
				AppConfig.data_dir = AppConfig.training_file;
				
			break; //
			case "-version":
				System.out.println("Version: " + AppConfig.version);
				System.out.println("VersionTag: " + AppConfig.version_tag);
				System.out.println("Release Notes: " + AppConfig.release_notes);
				
				//TODO print java version info and slurm environment info here
				
				System.exit(0);
				
			case "-train":
				AppConfig.training_file = options[1];
				break;
			case "-test":
				AppConfig.testing_file = options[1];
				break;
			case "-out":
				AppConfig.output_dir = options[1];
				break;
			case "-results":
				AppConfig.results_file = options[1];
				break;
			case "-export_file_prefix":
				AppConfig.export_file_prefix = options[1];
				break;
			case "-export_file_suffix":
				AppConfig.export_file_suffix = options[1];
				break;
//			case "-export_filename_format":
//				AppContext.export_filename_format = options[1];
//				break;
			case "-repeats":
				AppConfig.num_repeats = Integer.parseInt(options[1]);
				break;
			case "-trees":
				AppConfig.num_trees = Integer.parseInt(options[1]);
				break;
			case "-s": //use either -s or -s_prob, dont use both params together
				//NOTE eg. s=ee:5,boss:100
				AppConfig.use_probabilities_to_choose_num_splitters_per_node = false;
				parseNumSplittersPerNode(options[1], false);
				break;
			case "-s_prob": //use either -s or -s_prob, dont use both params together
				//TODO implementation not finished
				//last probability is ignored, last = 1-sum(e_1..e_{n-1})
				//NOTE eg. s=10;ee:0.5; 
				AppConfig.use_probabilities_to_choose_num_splitters_per_node = true;
				parseNumSplittersPerNode(options[1], true);
				break;
			case "-ee_dm_on_node":
				AppConfig.random_dm_per_node = Boolean.parseBoolean(options[1]);
				break;
			case "-shuffle":
				AppConfig.shuffle_dataset = Boolean.parseBoolean(options[1]);
				break;
//			case "-jvmwarmup":	//DEV 
//				AppContext.warmup_java = Boolean.parseBoolean(options[1]);
//				break;
			case "-csv_has_header":
				AppConfig.csv_has_header = Boolean.parseBoolean(options[1]);
				break;
			case "-csv_label_column":
			case "-target_column":
				if (options[1].trim().equals("first")) {
					AppConfig.csv_label_column = 0;
				}else if (options[1].trim().equals("last")) {
					AppConfig.csv_label_column = -1;
				}else {
					throw new Exception("Invalid Commandline Argument: " + args[i]);
				}
				break;
			case "-threads":
				AppConfig.num_threads = Integer.parseInt(options[1]);
				break;
			case "-export":
				AppConfig.export_level =  Integer.parseInt(options[1]);
				break;
			case "-verbosity":
				AppConfig.verbosity =  Integer.parseInt(options[1]);
				break;		
//			case "-splitters":
//				AppContext.enabled_splitters = parseSplitters(options[1]);
//				break;				
			case "-ee_approx_gini":
				AppConfig.gini_approx = Boolean.parseBoolean(options[1]);
				break;
			case "-ee_approx_gini_percent":
				AppConfig.gini_percent = Double.parseDouble(options[1]);
				break;
			case "-ee_approx_gini_min":
				AppConfig.approx_gini_min = Integer.parseInt(options[1]);
				break;
			case "-ee_approx_gini_min_per_class":
				AppConfig.approx_gini_min_per_class = Boolean.parseBoolean(options[1]);
				break;
			case "-ee_dm":
				AppConfig.enabled_distance_measures = parseDistanceMeasures(options[1]);
				break;
			case "-randf_m":
				
				if (options[1].equals("sqrt")) {
					AppConfig.randf_feature_selection = FeatureSelectionMethod.Sqrt;
				}else if (options[1].equals("loge")) {
					AppConfig.randf_feature_selection = FeatureSelectionMethod.Loge;
				}else if (options[1].equals("log2")) {
					AppConfig.randf_feature_selection = FeatureSelectionMethod.Log2; 
				}else {
					AppConfig.randf_feature_selection = FeatureSelectionMethod.ConstantInt; 
					AppConfig.randf_m = Integer.parseInt(options[1]); //TODO verify
				}	
				
				break;
			case "-boss_params":
				if (options[1].equals("random")) {
					AppConfig.boss_param_selection = ParamSelection.Random;
				}else if (options[1].equals("best")) {
					AppConfig.boss_param_selection = ParamSelection.PreLoadedParams;
				}else {
					throw new Exception("Boss param selection method not suppoted");
				}
				break;
			case "-boss_trasformations":
				AppConfig.boss_trasformations = Integer.parseInt(options[1]);
				break;
			case "-boss_preload_params_path":
				AppConfig.boss_params_files = options[1];
				break;
			case "-boss_transform_level":
				if (options[1].equals("forest")) {
					AppConfig.boss_transform_level = TransformLevel.Forest;
				}else if (options[1].equals("tree")) {
					AppConfig.boss_transform_level = TransformLevel.Forest;
				}else {
					throw new Exception("Boss transform level not suppoted");
				}
				break;
			case "-boss_split_method":
				if (options[1].equals("gini")) {
					AppConfig.boss_split_method = SplitMethod.RandomTreeStyle;
				}else if (options[1].equals("nearest")) {
					AppConfig.boss_split_method = SplitMethod.NearestClass;
				}else {
					throw new Exception("Boss split method not suppoted");
				}
				break;
			case "-tsf_m":
				if (options[1].equals("sqrt")) {
					AppConfig.tsf_feature_selection = FeatureSelectionMethod.Sqrt;
				}else if (options[1].equals("loge")) {
					AppConfig.tsf_feature_selection = FeatureSelectionMethod.Loge;
				}else if (options[1].equals("log2")) {
					AppConfig.tsf_feature_selection = FeatureSelectionMethod.Log2; 
				}else {
					AppConfig.tsf_feature_selection = FeatureSelectionMethod.ConstantInt; 
					AppConfig.tsf_m = Integer.parseInt(options[1]); //TODO verify
				}	
				break;
			case "-tsf_num_intervals":
				AppConfig.tsf_num_intervals = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-tsf_min_interval":
				AppConfig.tsf_min_interval = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-rif_min_interval":
				AppConfig.rif_min_interval = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-rif_components":
				if (options[1].equals("acf")) {
					AppConfig.rif_components = RifFilters.ACF;
				}else if (options[1].equals("pacf")) {
					AppConfig.rif_components = RifFilters.PACF;
				}else if (options[1].equals("arma")) {
					AppConfig.rif_components = RifFilters.ARMA;
				}else if (options[1].equals("ps")) {
					AppConfig.rif_components = RifFilters.PS;
				}else if (options[1].equals("dft")) {
					AppConfig.rif_components = RifFilters.DFT;
				}else if (options[1].equals("acf_pacf_arma")) {
					AppConfig.rif_components = RifFilters.ACF_PACF_ARMA;
				}else if (options[1].equals("acf_pacf_arma_ps_comb")) {
					AppConfig.rif_components = RifFilters.ACF_PACF_ARMA_PS_combined;
				}else if (options[1].equals("acf_pacf_arma_ps_sep")) {
					AppConfig.rif_components = RifFilters.ACF_PACF_ARMA_PS_separately;
				}else if (options[1].equals("acf_pacf_arma_dft")) {
					AppConfig.rif_components = RifFilters.ACF_PACF_ARMA_DFT;
				}else {
					throw new Exception("RISE component not suppoted");
				}
				break;
			case "-rif_m":
				if (options[1].equals("sqrt")) {
					AppConfig.rif_feature_selection = FeatureSelectionMethod.Sqrt;
				}else if (options[1].equals("loge")) {
					AppConfig.rif_feature_selection = FeatureSelectionMethod.Loge;
				}else if (options[1].equals("log2")) {
					AppConfig.rif_feature_selection = FeatureSelectionMethod.Log2; 
				}else {
					AppConfig.rif_feature_selection = FeatureSelectionMethod.ConstantInt; 
					AppConfig.rif_m = Integer.parseInt(options[1]); //TODO verify
				}	
				break;
			case "-rif_same_intv_component":
				AppConfig.rif_same_intv_component =  Boolean.parseBoolean(options[1]);
				break;
			case "-rif_num_intervals":
				AppConfig.rif_num_intervals = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-st_min_length_percent":
				AppConfig.st_min_length_percent = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-st_max_length_percent":
				AppConfig.st_max_length_percent = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-st_min_length":
				AppConfig.st_min_length = Integer.parseInt(options[1]);
				break;
			case "-st_max_length":
				AppConfig.st_max_length = Integer.parseInt(options[1]); 
				break;
			case "-st_interval_method":
				AppConfig.st_interval_method = options[1];
				break;
			case "-st_params_files":
				AppConfig.st_params_files = options[1];
				break;
			case "-st_params":
				if (options[1].equals("random")) {
					AppConfig.st_param_selection = ParamSelection.Random;
				}else if (options[1].equals("preload")) {
					AppConfig.st_param_selection = ParamSelection.PreLoadedParams;
				}else if (options[1].equals("preload_data")) {
					AppConfig.st_param_selection = ParamSelection.PraLoadedDataAndParams;
				}else {
					throw new Exception("ST param selection method not suppoted, invalid arg: -st_params " +  options[1]);
				}
				break;
			case "-st_threshold_selection":
				if (options[1].equals("random")) {
					AppConfig.st_threshold_selection = ThresholdSelectionMethod.Random;
				}else if (options[1].equals("bestgini")) {
					AppConfig.st_threshold_selection = ThresholdSelectionMethod.BestGini;
				}else if (options[1].equals("median")) {
					AppConfig.st_threshold_selection = ThresholdSelectionMethod.Median;
				}else {
					throw new Exception("ST st_threshold_selection method not suppoted, invalid arg: -st_threshold_selection" + options[1]);
				}
				break;
			case "-st_num_rand_thresholds":
				AppConfig.st_num_rand_thresholds = Integer.parseInt(options[1]);
				break;
			case "-st_normalize":
				AppConfig.st_normalize = Boolean.parseBoolean(options[1]);
				break;
			case "-st_feature_selection":
				if (options[1].equals("sqrt")) {
					AppConfig.st_feature_selection = FeatureSelectionMethod.Sqrt;
				}else {
					AppConfig.st_feature_selection = FeatureSelectionMethod.ConstantInt; //st_splitters_per_node are used as m, and only 1 shapelet splitter instance is created refer to ~line315 SplitEvaluator
				}	
				break;
			case "-it_m":
				AppConfig.it_m = Integer.parseInt(options[1]);
				break;
			case "-it_feature_selection":
				if (options[1].equals("sqrt")) {
					AppConfig.it_feature_selection = FeatureSelectionMethod.Sqrt;
				}else if (options[1].equals("loge")) {
					AppConfig.it_feature_selection = FeatureSelectionMethod.Loge;
				}else if (options[1].equals("log2")) {
					AppConfig.it_feature_selection = FeatureSelectionMethod.Log2; 
				}else {
					AppConfig.it_feature_selection = FeatureSelectionMethod.ConstantInt; 
					AppConfig.it_m = Integer.parseInt(options[1]); //TODO verify
				}	
				break;
			case "-it_transform_level":
				if (options[1].equals("forest")) {
					AppConfig.it_transform_level = TransformLevel.Forest;
				}else if (options[1].equals("tree")) {
					AppConfig.it_transform_level = TransformLevel.Forest;
				}else {
					throw new Exception("IT transform level not suppoted");
				}
				break;
			case "-it_cache_dir":
				AppConfig.it_cache_dir = options[1];
				break;
			case "-rt_kernels":
				AppConfig.rt_kernels = Integer.parseInt(options[1]);
				break;
			case "-rt_m":
				AppConfig.rt_m = Integer.parseInt(options[1]);
				break;
			case "-rt_feature_selection":
				if (options[1].equals("sqrt")) {
					AppConfig.rt_feature_selection = FeatureSelectionMethod.Sqrt;
				}else if (options[1].equals("loge")) {
					AppConfig.rt_feature_selection = FeatureSelectionMethod.Loge;
				}else if (options[1].equals("log2")) {
					AppConfig.rt_feature_selection = FeatureSelectionMethod.Log2; 
				}else {
					AppConfig.rt_feature_selection = FeatureSelectionMethod.ConstantInt; 
					AppConfig.rt_m = Integer.parseInt(options[1]); //TODO verify
				}	
				break;
			case "-rt_transform_level":
				if (options[1].equals("forest")) {
					AppConfig.it_transform_level = TransformLevel.Forest;
				}else if (options[1].equals("tree")) {
					AppConfig.it_transform_level = TransformLevel.Forest;
				}else {
					throw new Exception("RT transform level not suppoted");
				}
				break;
			case "-rt_cache_dir":
				AppConfig.rt_cache_dir = options[1];
				break;
			case "-rt_split_method":
				if (options[1].equals("gini")) {
					AppConfig.rt_split_method = SplitMethod.RandomTreeStyle; //TODO
				}else if (options[1].equals("nearest")) {
					AppConfig.rt_split_method = SplitMethod.RidgeClassifier;
				}else {
					throw new Exception("ST split method not suppoted");
				}
				break;
			case "-rt_cross_validate_at_node":
				AppConfig.rt_cross_validate_at_node = Boolean.parseBoolean(options[1]);
				break;
			case "-rt_normalize":
				AppConfig.rt_normalize = Boolean.parseBoolean(options[1]);
				break;
			case "-rt_alphas":
				//TODO
				break;
			case "-archive":
				AppConfig.archiveName = options[1];
				break;
			case "-datasets":
				AppConfig.datasets = parseDatasetsOption(options[1]);
				break;
			case "-binary_split":
				AppConfig.binary_split =  Boolean.parseBoolean(options[1]);
				break;
			case "-gini_split":
				AppConfig.gini_split =  Boolean.parseBoolean(options[1]);
				break;
			case "-boosting":
				AppConfig.boosting =  Boolean.parseBoolean(options[1]);
				break;
			case "-min_leaf_size":
				AppConfig.min_leaf_size =  Integer.parseInt(options[1]);
				break;
			case "-rand_seed":
			case "-seed":
				AppConfig.rand_seed =  Long.parseLong(options[1]);
				break;
			case "-ee_use_random_params":
				AppConfig.ee_use_random_params =  Boolean.parseBoolean(options[1]);
				break;
			case "-ee_window_size":
				AppConfig.windowSize =  Integer.parseInt(options[1]);
				break;
			case "-ee_epsilon":
				AppConfig.epsilon =  Double.parseDouble(options[1]);
				break;
			case "-ee_cost":
				AppConfig.cost =  Double.parseDouble(options[1]);
				break;
			case "-ee_penalty":
				AppConfig.penalty =  Double.parseDouble(options[1]);
				break;
			case "-dim_dependency":
				AppConfig.dimensionDependencyMethod = AppConfig.DimensionDependencyMethod.valueOf(options[1]);
				break;
			case "-dimensions":
				AppConfig.dimensionSubsetsAsString =  options[1];
				break;
			case "-use_random_dimensions":
				AppConfig.useRandomSubsetsOfDimensions =  Boolean.parseBoolean(options[1]);
				break;
			case "-num_dim_subsets":
				AppConfig.numRandomSubsetsOfDimensionsPerNode =  Integer.parseInt(options[1]);
				break;
			case "-min_dimensions":
				AppConfig.minDimensionsPerSubset =  Integer.parseInt(options[1]);
				break;
			case "-max_dimensions":
				AppConfig.maxDimensionsPerSubset =  Integer.parseInt(options[1]);
				break;
			default:
				throw new Exception("Invalid Commandline Argument: " + args[i]);
			}
		}
		
		//NOTE do these after all params have been parsed from cmd line
		
		//extra validations
		if (AppConfig.boss_param_selection == ParamSelection.PreLoadedParams && ! AppConfig.archiveName.equals("ucr2015")) {
			throw new Exception("Cross validated BOSS params are only available for UCR 2015 datasets");
		}
		
		if(AppConfig.tsf_enabled) {
			//TODO tested only for 1 interval per splitter. check for more intervals
//			AppContext.num_splitters_per_node -= AppContext.tsf_splitters_per_node; //override this
//			AppContext.tsf_splitters_per_node = (int) Math.ceil( (float) AppContext.tsf_splitters_per_node / ( 3 * AppContext.tsf_num_intervals));
//			AppContext.tsf_splitters_per_node = (int) ( (float) AppContext.tsf_splitters_per_node / ( 3 * AppContext.tsf_num_intervals));
//			AppContext.tsf_splitters_per_node /= 3; //override this --works only if tsf_num_intervals == 1
//			AppContext.num_splitters_per_node += AppContext.tsf_splitters_per_node; //override this
			
			
//			AppContext.num_actual_tsf_splitters_needed_per_interval = (int) Math.ceil( (float) AppContext.tsf_splitters_per_node / ( 3 * AppContext.tsf_num_intervals));
			AppConfig.num_actual_tsf_splitters_needed = AppConfig.tsf_splitters_per_node / 3;

		}
		
		if(AppConfig.rif_enabled) {
			//TODO tested only for 1 interval per splitter. check for more intervals

			int num_gini_per_type = AppConfig.rif_splitters_per_node / 4;	// eg. if we need 12 gini per type 12 ~= 50/4 
			int extra_gini = AppConfig.rif_splitters_per_node % 4;
			//assume 1 interval per splitter
			// 2 = ceil(12 / 9) if 9 = min interval length
			int min_splitters_needed_per_type = (int) Math.ceil((float)num_gini_per_type / (float)AppConfig.rif_min_interval); 
			int max_attribs_to_use_per_splitter = (int) Math.ceil(num_gini_per_type / min_splitters_needed_per_type);

			AppConfig.num_actual_rif_splitters_needed_per_type = min_splitters_needed_per_type;
			AppConfig.rif_m = max_attribs_to_use_per_splitter;
			int approx_gini_estimated = 4 * max_attribs_to_use_per_splitter * min_splitters_needed_per_type;
//			System.out.println("RISE: approx_gini_estimated: " + approx_gini_estimated); 
			
		}
		
		if (!(AppConfig.rif_components == RifFilters.ACF_PACF_ARMA_PS_combined)) {
			AppConfig.num_actual_splitters_needed = 
					AppConfig.ee_splitters_per_node +
					AppConfig.randf_splitters_per_node +
					AppConfig.rotf_splitters_per_node +
					AppConfig.st_splitters_per_node +
					AppConfig.boss_splitters_per_node +
					AppConfig.num_actual_tsf_splitters_needed +
					AppConfig.num_actual_rif_splitters_needed_per_type;	//TODO works if 
		}else {
			AppConfig.num_actual_splitters_needed = 
					AppConfig.ee_splitters_per_node +
					AppConfig.randf_splitters_per_node +
					AppConfig.rotf_splitters_per_node +
					AppConfig.st_splitters_per_node +
					AppConfig.boss_splitters_per_node +
					AppConfig.num_actual_tsf_splitters_needed +
					AppConfig.rif_splitters_per_node;	//TODO works if 			
		}
		

				
		if(AppConfig.num_splitters_per_node == 0) {
			throw new Exception("Number of candidate splits per node must be greater than 0. " +
					"\nUse -s command line option. \nEg. \"-s=ee:5,boss:100,rif:100\" for default TSCHIEF settings" +
					"\nOr \"-s=ee:5\" for default Proximity Forest settings");
		}
		
	}
	
	
	
	//eg. -s=10  , -s=ee:5,boss:100   , -s_prob=10,ee:0.1   ,-s_prob=10,equal
	private static void parseNumSplittersPerNode(String string, boolean use_probabilities) throws Exception {
		ArrayList<SplitterType> splitters = new ArrayList<>();
		String[] options = string.split(",");
		
		if (use_probabilities) {
			//TODO exception handling
			
			//assume first item is an integer 
			AppConfig.num_splitters_per_node = Integer.parseInt(options[0]);
			
			//parse probabilities
			double total = 0;
			
			for (int i = 1; i < options.length; i++) {	//TODO handle boundary conditions
				String temp[] = options[i].split(":");
				
				//TODO if equal ...				
				if (temp[0].equals("ee")) {
					AppConfig.probability_to_choose_ee = Double.parseDouble(temp[1]);
					total += AppConfig.probability_to_choose_ee;
					if (AppConfig.probability_to_choose_ee > 0) {
						AppConfig.ee_enabled = true;
						splitters.add(SplitterType.ElasticDistanceSplitter);
					}
				}else if (temp[0].equals("randf")) {
					AppConfig.probability_to_choose_randf = Double.parseDouble(temp[1]);
					total += AppConfig.probability_to_choose_randf;
					if (AppConfig.probability_to_choose_randf > 0) {
						AppConfig.randf_enabled = true;
						splitters.add(SplitterType.RandomForestSplitter);
					}
				}else if (temp[0].equals("rotf")) {
					AppConfig.probability_to_choose_rotf = Double.parseDouble(temp[1]);
					total += AppConfig.probability_to_choose_rotf;
					if (AppConfig.probability_to_choose_rotf > 0) {
						AppConfig.rotf_enabled = true;
						splitters.add(SplitterType.RotationForestSplitter);
					}
				}else if (temp[0].equals("st")) {
					AppConfig.probability_to_choose_st = Double.parseDouble(temp[1]);
					total += AppConfig.probability_to_choose_st;
					if (AppConfig.probability_to_choose_st > 0) {
						AppConfig.st_enabled = true;
						splitters.add(SplitterType.ShapeletTransformSplitter);
					}
				}else if (temp[0].equals("boss")) {
					AppConfig.probability_to_choose_boss = Double.parseDouble(temp[1]);
					total += AppConfig.probability_to_choose_boss;
					if (AppConfig.probability_to_choose_boss > 0) {
						AppConfig.boss_enabled = true;
						splitters.add(SplitterType.BossSplitter);
					}
				}else if (temp[0].equals("tsf")) {
					AppConfig.probability_to_choose_tsf = Double.parseDouble(temp[1]);
					total += AppConfig.probability_to_choose_tsf;
					if (AppConfig.probability_to_choose_tsf > 0) {
						AppConfig.tsf_enabled = true;
						splitters.add(SplitterType.TSFSplitter);
					}
				}else if (temp[0].equals("rif")) {
					AppConfig.probability_to_choose_rif = Double.parseDouble(temp[1]);
					total += AppConfig.probability_to_choose_rif;
					if (AppConfig.probability_to_choose_rif > 0) {
						AppConfig.rif_enabled = true;
						splitters.add(SplitterType.RIFSplitter);
					}
				}else {
					throw new Exception("Unknown Splitter Type");
				}
				
			}
			
			//override the last one
			if (total > 1) {
				throw new Exception("Probabilities add up to more than 14");
			}
			
		}else {
			
			int total = 0;
			
			for (int i = 0; i < options.length; i++) {	//TODO handle boundary conditions
				String temp[] = options[i].split(":");
				
				if (temp[0].equals("ee")) {
					AppConfig.ee_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppConfig.ee_splitters_per_node;
					if (AppConfig.ee_splitters_per_node > 0) {
						AppConfig.ee_enabled = true;
						splitters.add(SplitterType.ElasticDistanceSplitter);
					}
				}else if (temp[0].equals("randf")) {
					AppConfig.randf_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppConfig.randf_splitters_per_node;
					if (AppConfig.randf_splitters_per_node > 0) {
						AppConfig.randf_enabled = true;
						splitters.add(SplitterType.RandomForestSplitter);
					}
				}else if (temp[0].equals("rotf")) {
					AppConfig.rotf_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppConfig.probability_to_choose_rotf;
					if (AppConfig.probability_to_choose_rotf > 0) {
						AppConfig.rotf_enabled = true;
						splitters.add(SplitterType.RotationForestSplitter);
					}
				}else if (temp[0].equals("st")) {
					AppConfig.st_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppConfig.st_splitters_per_node;
					if (AppConfig.st_splitters_per_node > 0) {
						AppConfig.st_enabled = true;
						splitters.add(SplitterType.ShapeletTransformSplitter);
					}
				}else if (temp[0].equals("boss")) {
					AppConfig.boss_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppConfig.boss_splitters_per_node;
					if (AppConfig.boss_splitters_per_node > 0) {
						AppConfig.boss_enabled = true;
						splitters.add(SplitterType.BossSplitter);
					}
				}else if (temp[0].equals("tsf")) {
					AppConfig.tsf_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppConfig.tsf_splitters_per_node;
					if (AppConfig.tsf_splitters_per_node > 0) {
						AppConfig.tsf_enabled = true;
						splitters.add(SplitterType.TSFSplitter);
					}
				}else if (temp[0].equals("rif")) {
					AppConfig.rif_splitters_per_node = Integer.parseInt(temp[1]);				
					total += AppConfig.rif_splitters_per_node;
					if (AppConfig.rif_splitters_per_node > 0) {
						AppConfig.rif_enabled = true;
						splitters.add(SplitterType.RIFSplitter);
					}
				}else if (temp[0].equals("it")) {
					AppConfig.it_splitters_per_node = Integer.parseInt(temp[1]);				
					total += AppConfig.it_splitters_per_node;
					if (AppConfig.it_splitters_per_node > 0) {
						AppConfig.it_enabled = true;
						splitters.add(SplitterType.InceptionTimeSplitter);
					}
				}else if (temp[0].equals("rt")) {
					AppConfig.rt_splitters_per_node = Integer.parseInt(temp[1]);				
					total += AppConfig.rt_splitters_per_node;
					if (AppConfig.rt_splitters_per_node > 0) {
						AppConfig.rt_enabled = true;
						splitters.add(SplitterType.RocketTreeSplitter);
					}
				}else {
					throw new Exception("Unknown Splitter Type");
				}
			}
			
			AppConfig.num_splitters_per_node = total;
			AppConfig.enabled_splitters = splitters.toArray(new SplitterType[splitters.size()]);
			
		}
	}

	private static MEASURE[] parseDistanceMeasures(String string) throws Exception {
		String list[] = string.trim().split(",");
		MEASURE[] enabled_measures = new MEASURE[list.length];
		
		for (int i = 0; i < list.length; i++) {
			switch(list[i].trim()) {
				case "euc":
					enabled_measures[i] = MEASURE.euclidean;
				break;
				case "dtw":
					enabled_measures[i] = MEASURE.dtw;
				break;
				case "dtwr":
					enabled_measures[i] = MEASURE.dtwcv;
				break;
				case "ddtw":
					enabled_measures[i] = MEASURE.ddtw;
				break;
				case "ddtwr":
					enabled_measures[i] = MEASURE.ddtwcv;
				break;
				case "wdtw":
					enabled_measures[i] = MEASURE.wdtw;
				break;
				case "wddtw":
					enabled_measures[i] = MEASURE.wddtw;
				break;
				case "lcss":
					enabled_measures[i] = MEASURE.lcss;
				break;
				case "erp":
					enabled_measures[i] = MEASURE.erp;
				break;
				case "twe":
					enabled_measures[i] = MEASURE.twe;
				break;
				case "msm":
					enabled_measures[i] = MEASURE.msm;
				break;
				
				default:
					throw new Exception("Invalid Commandline Argument(Unknown Distance Measure): " + list[i]);
			}
		}
		
		return enabled_measures;
	}

//	private static SplitterType[] parseSplitters(String string) throws Exception {
//		
//		String list[] = string.trim().split(",");
//		SplitterType[] enabled_splitters = new SplitterType[list.length];
//		
//		for (int i = 0; i < list.length; i++) {
//			switch(list[i].trim()) {
//				case "ee":
//					enabled_splitters[i] = SplitterType.ElasticDistanceSplitter;
//					AppContext.ee_enabled = true;
//				break;
//				case "randf":
//					enabled_splitters[i] = SplitterType.RandomForestSplitter;
//					AppContext.randf_enabled = true;
//				break;
//				case "rotf":
//					enabled_splitters[i] = SplitterType.RotationForestSplitter;
//					AppContext.rotf_enabled = true;
//				break;
//				case "boss":
//					enabled_splitters[i] = SplitterType.BossSplitter;
//					AppContext.boss_enabled = true;
//				break;
//				case "st":
//					enabled_splitters[i] = SplitterType.ShapeletTransformSplitter;
//					AppContext.st_enabled = true;
//				break;
//				default:
//					throw new Exception("Invalid Commandline Argument(Unknown Splitter): " + list[i]);
//			}
//		}
//		
//		return enabled_splitters;
//	}
	
	private static String[] parseDatasetsOption(String options) throws Exception {
		String datasets[];
		
		if (options.equals("all")) {
			datasets = AppConfig.getAllDatasets();	//get all 
		}else if ((options.startsWith("random"))){
			int num_datasets;
			options = options.replace("random", "");
			
			if (options.isEmpty()) {
				num_datasets = 1;
			}else {
				num_datasets = Integer.parseInt(options);
			}
			datasets = AppConfig.getNRandomDatasets(num_datasets);
			
		}else if ((options.startsWith("set"))){
			options = options.replace("set", "");

			if (options.isEmpty()) {
				throw new Exception("Invalid set id for predefined collection of datasets");
			}
			
			int set_id = Integer.parseInt(options);

			datasets = AppConfig.getPredefinedSet(set_id);

		}else if ((options.startsWith("range"))){
			options = options.replace("range", "");
			int start = Integer.parseInt(options.substring(0, options.indexOf("to")));
			int end = Integer.parseInt(options.substring(options.indexOf("o")+1));

			datasets = AppConfig.getSubsetOfDatasets(start, end);

		}else { //just assume its a list of comma separated dataset names
			datasets = Arrays.stream(options.split(",")).map(String::trim).toArray(String[]::new);;
		}
		
		return datasets;
	}


	
}

