package application;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import core.AppContext;
import core.ExperimentRunner;
import distance.elastic.MEASURE;
import core.AppContext.FeatureSelectionMethod;
import core.AppContext.ParamSelection;
import core.AppContext.RifFilters;
import core.AppContext.SplitMethod;
import core.AppContext.SplitterType;
import core.AppContext.TransformLevel;
import util.Util;
import util.PrintUtilities;

/**
 * Main entry point for the Proximity Forest application
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class MainApplication {
	public static String[] ucr_datasets;
	public static boolean use_ucr = false;
	
	public static final String[] dev_args = new String[]{
			"-machine=pc",
			"-ucr=ItalyPowerDemand",
			"-out=output/dev/t1/",
			"-trees=10",
			"-s=ee:5,boss:100,rise:100",
			"-threads=0",
			"-repeats=1",
			"-export=0"
	};
	
	//NOTE: for examples of how to use command line pls refer to AppSettings.java file

	public static void main(String[] args) {
		try {	 
			AppContext.version = "1.0.0.beta";
			AppContext.version_tag = "v010000.beta first release - code cleaning in progress";
			AppContext.release_notes = AppContext.release_notes + String.join(
					"v010000.beta first release - code cleaning in progress; hence beta",
					"build_at=3/7/2019");
			
			args = dev_args;

			AppContext.cmd_args = args; //store 
			System.out.print("ARGS:");
            for (String arg : args) {
                  System.out.print(arg + " ");
            }
            System.out.println();

            //sysinfo
            AppContext.host_name = InetAddress.getLocalHost().getHostName();
            AppContext.application_start_timestamp = LocalDateTime.now();
            AppContext.application_start_timestamp_formatted = AppContext.application_start_timestamp.format(DateTimeFormatter.ofPattern(AppContext.FILENAME_TIMESTAMP_FORMAT));			
            
			parseCMD(args);

    		AppContext.current_experiment_id = java.util.UUID.randomUUID().toString().substring(0, 8);
    		//these unique ids helps to keep the output folder structure clean
    		AppContext.repeat_guids = new ArrayList<>(AppContext.num_repeats);
    		for (int i = 0; i < AppContext.num_repeats; i++) {
    			AppContext.repeat_guids.add(java.util.UUID.randomUUID().toString().substring(0, 8));
    		}
    		
			//experiment
			if (AppContext.warmup_java) {
				Util.warmUpJavaRuntime();
			}
			
			//experiment
			if (AppContext.boss_param_selection == ParamSelection.PreLoadedSet) {
				AppContext.boss_preloaded_params = AppContext.loadBossParams();
			}
			
			if (AppContext.verbosity > 0) {
				System.out.println("Version: " + AppContext.version + " (" + AppContext.version_tag + ")");
				PrintUtilities.printConfiguration();
//				System.out.println();				
			}
			
			if (use_ucr) { // if only -ucr arg is present --> making this easier for UCR experiments
				
				String tmp_training_dir = AppContext.training_file;
				String tmp_testing_dir = AppContext.testing_file == null ? AppContext.training_file : AppContext.testing_file;	
						
				for (int i = 0; i < ucr_datasets.length; i++) {
					String current_dataset = ucr_datasets[i];
					AppContext.training_file = tmp_training_dir + "/" + current_dataset + "/" + current_dataset + "_TRAIN.txt";
					AppContext.testing_file = tmp_testing_dir + "/"  + current_dataset + "/" + current_dataset + "_TEST.txt";
					
					AppContext.training_file = Util.adaptPathToOS(AppContext.training_file);
					AppContext.testing_file = Util.adaptPathToOS(AppContext.testing_file);
					
					System.out.println("--------------------------------    START UCR DATASET: " + i + " (" + current_dataset + ")   -------------------------");
					
					ExperimentRunner experiment = new ExperimentRunner();
					experiment.run();				
					
					System.gc();
					System.out.println("--------------------------------   END DATASET: "+ current_dataset +"  -------------------------------------");
				}
				
			}else {
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
			
			if ((! options[0].equals("-ucr")) && (! options[0].equals("-train")) && (! options[0].equals("-test"))  && (! options[0].equals("-out"))) {
				options[1] = options[1].trim().toLowerCase();
			}
			
			switch(options[0]) {
			
			//TODO dev command line argument to make things easier -- not for public release
			case "-machine":	//cluster or pc or laptop
				
				String machine = options[1];
				AppContext.debug_machine  = machine;
				
				if (machine.equals("m3")) {
					AppContext.training_file = "../data/ucr/";
				}else if (machine.equals("pc")) {
					AppContext.training_file = "E:/data/ucr/";

				}else if (machine.equals("mac")) {
					AppContext.training_file = "/data/ucr/";

				}else if (machine.equals("m3-sat")) {
					AppContext.training_file = "../../data/SatelliteFull/";
				}else if (machine.equals("pc-sat")) {
					AppContext.training_file = "E:/data/SatelliteFull/sk_stratified/";
				}else if (machine.equals("mac-sat")) {
					AppContext.training_file = "/data/SatelliteFull/sk_stratified/";
				}
				
			
			break;
			case "-v":
			case "-version":
				System.out.println("Version: " + AppContext.version);
				System.out.println("VersionTag: " + AppContext.version_tag);
				System.out.println("Release Notes: " + AppContext.release_notes);
				
				//TODO print java version info and slurm environment info here
				
				System.exit(0);
			case "-h":
			case "-help":
				System.out.println("Version: " + AppContext.version);
				//TODO
				System.out.println("TODO print help message -- and example of command line args: " + AppContext.version);

				System.exit(0);
			case "-train":
				AppContext.training_file = options[1];
				break;
			case "-test":
				AppContext.testing_file = options[1];
				break;
			case "-out":
				AppContext.output_dir = options[1];
				break;
			case "-repeats":
				AppContext.num_repeats = Integer.parseInt(options[1]);
				break;
			case "-trees":
				AppContext.num_trees = Integer.parseInt(options[1]);
				break;
			case "-s": //use either -s or -s_prob, dont use both params together
				//NOTE eg. s=ee:5,boss:100
				AppContext.use_probabilities_to_choose_num_splitters_per_node = false;
				parseNumSplittersPerNode(options[1], false);
				break;
//			case "-s_prob": //use either -s or -s_prob, dont use both params together
//				//TODO this is experimental --> dont use it
//				//last probability is ignored, last = 1-sum(e_1..e_{n-1})
//				//NOTE eg. s=10;ee:0.5; 
//				AppContext.use_probabilities_to_choose_num_splitters_per_node = true;
//				parseNumSplittersPerNode(options[1], true);
//				break;
			case "-ee_dm_on_node":
				AppContext.random_dm_per_node = Boolean.parseBoolean(options[1]);
				break;
			case "-shuffle":
				AppContext.shuffle_dataset = Boolean.parseBoolean(options[1]);
				break;
//			case "-jvmwarmup":	//TODO 
//				AppContext.warmup_java = Boolean.parseBoolean(options[1]);
//				break;
			case "-csv_has_header":
				AppContext.csv_has_header = Boolean.parseBoolean(options[1]);
				break;
			case "-target_column":
				if (options[1].trim().equals("first")) {
					AppContext.target_column_is_first = true;
				}else if (options[1].trim().equals("last")) {
					AppContext.target_column_is_first = false;
				}else {
					throw new Exception("Invalid Commandline Argument: " + args[i]);
				}
				break;
			case "-threads":
				AppContext.num_threads = Integer.parseInt(options[1]);
				break;
			case "-export":
				AppContext.export_level =  Integer.parseInt(options[1]);
				break;
			case "-verbosity":
				AppContext.verbosity =  Integer.parseInt(options[1]);
				break;		
//			case "-splitters":
//				AppContext.enabled_splitters = parseSplitters(options[1]);
//				break;				
			case "-ee_approx_gini":
				AppContext.gini_approx = Boolean.parseBoolean(options[1]);
				break;
			case "-ee_approx_gini_percent":
				AppContext.gini_percent = Double.parseDouble(options[1]);
				break;
			case "-ee_approx_gini_min":
				AppContext.approx_gini_min = Integer.parseInt(options[1]);
				break;
			case "-ee_approx_gini_min_per_class":
				AppContext.approx_gini_min_per_class = Boolean.parseBoolean(options[1]);
				break;
			case "-ee_dm":
				AppContext.enabled_distance_measures = parseDistanceMeasures(options[1]);
				break;
			case "-randf_m":
				
				if (options[1].equals("sqrt")) {
					AppContext.randf_feature_selection = FeatureSelectionMethod.Sqrt;
				}else if (options[1].equals("loge")) {
					AppContext.randf_feature_selection = FeatureSelectionMethod.Loge;
				}else if (options[1].equals("log2")) {
					AppContext.randf_feature_selection = FeatureSelectionMethod.Log2; 
				}else {
					AppContext.randf_feature_selection = FeatureSelectionMethod.ConstantInt; 
					AppContext.randf_m = Integer.parseInt(options[1]); //TODO verify
				}	
				
				break;
			case "-boss_params":
				if (options[1].equals("random")) {
					AppContext.boss_param_selection = ParamSelection.Random;
				}else if (options[1].equals("best")) {
					AppContext.boss_param_selection = ParamSelection.PreLoadedSet;
				}else {
					throw new Exception("Boss param selection method not suppoted");
				}
				break;
			case "-boss_trasformations":
				AppContext.boss_trasformations = Integer.parseInt(options[1]);
				break;
			case "-boss_preload_params_path":
				AppContext.boss_params_files = options[1];
				break;
			case "-boss_transform_level":
				if (options[1].equals("forest")) {
					AppContext.boss_transform_level = TransformLevel.Forest;
				}else if (options[1].equals("tree")) {
					AppContext.boss_transform_level = TransformLevel.Forest;
				}else {
					throw new Exception("Boss transform level not suppoted");
				}
				break;
			case "-boss_split_method":
				if (options[1].equals("gini")) {
					AppContext.boss_split_method = SplitMethod.Binary_Gini;
				}else if (options[1].equals("nearest")) {
					AppContext.boss_split_method = SplitMethod.Nary_NearestClass;
				}else {
					throw new Exception("Boss split method not suppoted");
				}
				break;
			case "-tsf_m":
				if (options[1].equals("sqrt")) {
					AppContext.tsf_feature_selection = FeatureSelectionMethod.Sqrt;
				}else if (options[1].equals("loge")) {
					AppContext.tsf_feature_selection = FeatureSelectionMethod.Loge;
				}else if (options[1].equals("log2")) {
					AppContext.tsf_feature_selection = FeatureSelectionMethod.Log2; 
				}else {
					AppContext.tsf_feature_selection = FeatureSelectionMethod.ConstantInt; 
					AppContext.tsf_m = Integer.parseInt(options[1]); //TODO verify
				}	
				break;
			case "-tsf_num_intervals":
				AppContext.tsf_num_intervals = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-tsf_min_interval":
				AppContext.tsf_min_interval = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-rif_min_interval":
				AppContext.rif_min_interval = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-rif_components":
				if (options[1].equals("acf")) {
					AppContext.rif_components = RifFilters.ACF;
				}else if (options[1].equals("pacf")) {
					AppContext.rif_components = RifFilters.PACF;
				}else if (options[1].equals("arma")) {
					AppContext.rif_components = RifFilters.ARMA;
				}else if (options[1].equals("ps")) {
					AppContext.rif_components = RifFilters.PS;
				}else if (options[1].equals("dft")) {
					AppContext.rif_components = RifFilters.DFT;
				}else if (options[1].equals("acf_pacf_arma")) {
					AppContext.rif_components = RifFilters.ACF_PACF_ARMA;
				}else if (options[1].equals("acf_pacf_arma_ps_comb")) {
					AppContext.rif_components = RifFilters.ACF_PACF_ARMA_PS_combined;
				}else if (options[1].equals("acf_pacf_arma_ps_sep")) {
					AppContext.rif_components = RifFilters.ACF_PACF_ARMA_PS_separately;
				}else if (options[1].equals("acf_pacf_arma_dft")) {
					AppContext.rif_components = RifFilters.ACF_PACF_ARMA_DFT;
				}else {
					throw new Exception("RISE component not suppoted");
				}
				break;
			case "-rif_m":
				if (options[1].equals("sqrt")) {
					AppContext.rif_feature_selection = FeatureSelectionMethod.Sqrt;
				}else if (options[1].equals("loge")) {
					AppContext.rif_feature_selection = FeatureSelectionMethod.Loge;
				}else if (options[1].equals("log2")) {
					AppContext.rif_feature_selection = FeatureSelectionMethod.Log2; 
				}else {
					AppContext.rif_feature_selection = FeatureSelectionMethod.ConstantInt; 
					AppContext.rif_m = Integer.parseInt(options[1]); //TODO verify
				}	
				break;
			case "-rif_same_intv_component":
				AppContext.rif_same_intv_component =  Boolean.parseBoolean(options[1]);
				break;
			case "-rif_num_intervals":
				AppContext.rif_num_intervals = Integer.parseInt(options[1]); //TODO validate
				break;
			case "-ucr":
				use_ucr = true;
				ucr_datasets = parseUCROption(options[1]);
				break;
			case "-binary_split":
				AppContext.binary_split =  Boolean.parseBoolean(options[1]);
				break;
			case "-gini_split":
				AppContext.gini_split =  Boolean.parseBoolean(options[1]);
				break;
//			case "-boosting":
//				AppContext.boosting =  Boolean.parseBoolean(options[1]);
//				break;
//			case "-min_leaf_size":
//				AppContext.min_leaf_size =  Integer.parseInt(options[1]);
//				break;
			default:
				throw new Exception("Invalid Commandline Argument: " + args[i]);
			}
		}
		
		//NOTE do these after all params have been parsed from cmd line
		
		//extra validations
		if (AppContext.boss_param_selection == ParamSelection.PreLoadedSet && ! use_ucr) {
			throw new Exception("Cross validated BOSS params are only available for UCR datasets");
		}
		
		if(AppContext.tsf_enabled) {
			//TODO tested only for 1 interval per splitter. check for more intervals
//			AppContext.num_splitters_per_node -= AppContext.tsf_splitters_per_node; //override this
//			AppContext.tsf_splitters_per_node = (int) Math.ceil( (float) AppContext.tsf_splitters_per_node / ( 3 * AppContext.tsf_num_intervals));
//			AppContext.tsf_splitters_per_node = (int) ( (float) AppContext.tsf_splitters_per_node / ( 3 * AppContext.tsf_num_intervals));
//			AppContext.tsf_splitters_per_node /= 3; //override this --works only if tsf_num_intervals == 1
//			AppContext.num_splitters_per_node += AppContext.tsf_splitters_per_node; //override this
			
			
//			AppContext.num_actual_tsf_splitters_needed_per_interval = (int) Math.ceil( (float) AppContext.tsf_splitters_per_node / ( 3 * AppContext.tsf_num_intervals));
			AppContext.num_actual_tsf_splitters_needed = AppContext.tsf_splitters_per_node / 3;

		}
		
		if(AppContext.rif_enabled) {
			//TODO tested only for 1 interval per splitter. check for more intervals

			int num_gini_per_type = AppContext.rif_splitters_per_node / 4;	// eg. if we need 12 gini per type 12 ~= 50/4 
			int extra_gini = AppContext.rif_splitters_per_node % 4;
			//assume 1 interval per splitter
			// 2 = ceil(12 / 9) if 9 = min interval length
			int min_splitters_needed_per_type = (int) Math.ceil((float)num_gini_per_type / (float)AppContext.rif_min_interval); 
			int max_attribs_to_use_per_splitter = (int) Math.ceil(num_gini_per_type / min_splitters_needed_per_type);

			AppContext.num_actual_rif_splitters_needed_per_type = min_splitters_needed_per_type;
			AppContext.rif_m = max_attribs_to_use_per_splitter;
			int approx_gini_estimated = 4 * max_attribs_to_use_per_splitter * min_splitters_needed_per_type;
//			System.out.println("RISE: approx_gini_estimated: " + approx_gini_estimated); 
			
		}
		
		if (!(AppContext.rif_components == RifFilters.ACF_PACF_ARMA_PS_combined)) {
			AppContext.num_actual_splitters_needed = 
					AppContext.ee_splitters_per_node +
					AppContext.randf_splitters_per_node +
					AppContext.rotf_splitters_per_node +
					AppContext.st_splitters_per_node +
					AppContext.boss_splitters_per_node +
					AppContext.num_actual_tsf_splitters_needed +
					AppContext.num_actual_rif_splitters_needed_per_type;	//TODO works if 
		}else {
			AppContext.num_actual_splitters_needed = 
					AppContext.ee_splitters_per_node +
					AppContext.randf_splitters_per_node +
					AppContext.rotf_splitters_per_node +
					AppContext.st_splitters_per_node +
					AppContext.boss_splitters_per_node +
					AppContext.num_actual_tsf_splitters_needed +
					AppContext.rif_splitters_per_node;	//TODO works if 			
		}
		

				
		if(AppContext.num_splitters_per_node == 0) {
			throw new Exception("Number of candidate splits per node must be greater than 0. use -s option");
		}
		
	}
	
	
	
	//eg. -s=10  , -s=ee:5,boss:100   , -s_prob=10,ee:0.1   ,-s_prob=10,equal
	private static void parseNumSplittersPerNode(String string, boolean use_probabilities) throws Exception {
		ArrayList<SplitterType> splitters = new ArrayList<>();
		String[] options = string.split(",");
		
		if (use_probabilities) {	//TODO does not work -experimental
			//TODO exception handling
			
			//assume first item is an integer 
			AppContext.num_splitters_per_node = Integer.parseInt(options[0]);
			
			//parse probabilities
			double total = 0;
			
			for (int i = 1; i < options.length; i++) {	//TODO handle boundary conditions
				String temp[] = options[i].split(":");
				
				//TODO if equal ...				
				if (temp[0].equals("ee")) {
					AppContext.probability_to_choose_ee = Double.parseDouble(temp[1]);
					total += AppContext.probability_to_choose_ee;
					if (AppContext.probability_to_choose_ee > 0) {
						AppContext.ee_enabled = true;
						splitters.add(SplitterType.ElasticDistanceSplitter);
					}
				}else if (temp[0].equals("randf")) {
					AppContext.probability_to_choose_randf = Double.parseDouble(temp[1]);
					total += AppContext.probability_to_choose_randf;
					if (AppContext.probability_to_choose_randf > 0) {
						AppContext.randf_enabled = true;
						splitters.add(SplitterType.RandomForestSplitter);
					}
				}else if (temp[0].equals("rotf")) {
					AppContext.probability_to_choose_rotf = Double.parseDouble(temp[1]);
					total += AppContext.probability_to_choose_rotf;
					if (AppContext.probability_to_choose_rotf > 0) {
						AppContext.rotf_enabled = true;
						splitters.add(SplitterType.RotationForestSplitter);
					}
				}else if (temp[0].equals("st")) {
					AppContext.probability_to_choose_st = Double.parseDouble(temp[1]);
					total += AppContext.probability_to_choose_st;
					if (AppContext.probability_to_choose_st > 0) {
						AppContext.st_enabled = true;
						splitters.add(SplitterType.ShapeletTransformSplitter);
					}
				}else if (temp[0].equals("boss")) {
					AppContext.probability_to_choose_boss = Double.parseDouble(temp[1]);
					total += AppContext.probability_to_choose_boss;
					if (AppContext.probability_to_choose_boss > 0) {
						AppContext.boss_enabled = true;
						splitters.add(SplitterType.BossSplitter);
					}
				}else if (temp[0].equals("tsf")) {
					AppContext.probability_to_choose_tsf = Double.parseDouble(temp[1]);
					total += AppContext.probability_to_choose_tsf;
					if (AppContext.probability_to_choose_tsf > 0) {
						AppContext.tsf_enabled = true;
						splitters.add(SplitterType.TSFSplitter);
					}
				}else if (temp[0].equals("rif")) {
					AppContext.probability_to_choose_rif = Double.parseDouble(temp[1]);
					total += AppContext.probability_to_choose_rif;
					if (AppContext.probability_to_choose_rif > 0) {
						AppContext.rif_enabled = true;
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
					AppContext.ee_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppContext.ee_splitters_per_node;
					if (AppContext.ee_splitters_per_node > 0) {
						AppContext.ee_enabled = true;
						splitters.add(SplitterType.ElasticDistanceSplitter);
					}
				}else if (temp[0].equals("randf")) {
					AppContext.randf_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppContext.randf_splitters_per_node;
					if (AppContext.randf_splitters_per_node > 0) {
						AppContext.randf_enabled = true;
						splitters.add(SplitterType.RandomForestSplitter);
					}
				}else if (temp[0].equals("rotf")) {
					AppContext.rotf_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppContext.probability_to_choose_rotf;
					if (AppContext.probability_to_choose_rotf > 0) {
						AppContext.rotf_enabled = true;
						splitters.add(SplitterType.RotationForestSplitter);
					}
				}else if (temp[0].equals("st")) {
					AppContext.st_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppContext.st_splitters_per_node;
					if (AppContext.st_splitters_per_node > 0) {
						AppContext.st_enabled = true;
						splitters.add(SplitterType.ShapeletTransformSplitter);
					}
				}else if (temp[0].equals("boss")) {
					AppContext.boss_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppContext.boss_splitters_per_node;
					if (AppContext.boss_splitters_per_node > 0) {
						AppContext.boss_enabled = true;
						splitters.add(SplitterType.BossSplitter);
					}
				}else if (temp[0].equals("tsf")) {
					AppContext.tsf_splitters_per_node = Integer.parseInt(temp[1]);
					total += AppContext.tsf_splitters_per_node;
					if (AppContext.tsf_splitters_per_node > 0) {
						AppContext.tsf_enabled = true;
						splitters.add(SplitterType.TSFSplitter);
					}
				}else if (temp[0].equals("rif") || temp[0].equals("rise")) {
					AppContext.rif_splitters_per_node = Integer.parseInt(temp[1]);				
					total += AppContext.rif_splitters_per_node;
					if (AppContext.rif_splitters_per_node > 0) {
						AppContext.rif_enabled = true;
						splitters.add(SplitterType.RIFSplitter);
					}
				}else {
					throw new Exception("Unknown Splitter Type");
				}
			}
			
			AppContext.num_splitters_per_node = total;
			AppContext.enabled_splitters = splitters.toArray(new SplitterType[splitters.size()]);
			
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

	
	private static String[] parseUCROption(String options) throws Exception {
		String datasets[];
		
		if (options.equals("all")) {
			datasets = AppContext.getAllDatasets();	//get all 
		}else if ((options.startsWith("random"))){
			int num_datasets;
			options = options.replace("random", "");
			
			if (options.isEmpty()) {
				num_datasets = 1;
			}else {
				num_datasets = Integer.parseInt(options);
			}
			datasets = AppContext.getNRandomDatasets(num_datasets);
			
		}else if ((options.startsWith("set"))){
			options = options.replace("set", "");

			if (options.isEmpty()) {
				throw new Exception("Invalid set id for predefined collection of datasets");
			}
			
			int set_id = Integer.parseInt(options);

			datasets = AppContext.getPredefinedSet(set_id);

		}else if ((options.startsWith("range"))){
			options = options.replace("range", "");
			int start = Integer.parseInt(options.substring(0, options.indexOf("to")));
			int end = Integer.parseInt(options.substring(options.indexOf("o")+1));

			datasets = AppContext.getSubsetOfDatasets(start, end);

		}else { //just assume its a list of comma separated dataset names
			datasets = Arrays.stream(options.split(",")).map(String::trim).toArray(String[]::new);;
		}
		
		return datasets;
	}


	
}

