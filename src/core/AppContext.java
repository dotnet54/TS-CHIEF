package core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import datasets.TSDataset;
import datasets.BossDataset.BossParams;
import distance.elastic.MEASURE;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class AppContext {
	
	private static final long serialVersionUID = -502980220452234173L;
	public static String version = "1.0.0"; //TODO update
	public static String version_tag = ""; //TODO update
	public static String release_notes = String.join("\r\n"
			); //append latest release notes to this in PFApplication class
	
	
	public static String[] cmd_args;
	
	public static String host_name;
	
	public static final int ONE_MB = 1048576;	
	public static final String TIMESTAMP_FORMAT_LONG = "yyyy-MM-dd HH:mm:ss.SSS";	
	public static final String TIMESTAMP_FORMAT_SHORT = "HH:mm:ss.SSS";	
	
	public static final String FILENAME_TIMESTAMP_FORMAT = "yyyy-MM-dd-HHmmss-SSS";	
	
	public static final double NANOSECtoMILISEC = 1e6;	//format utils function take millisec as argument -NOTE
	public static final double NANOSECtoSEC = 1e9;	//elapsed times are calculated in nanoseconds


	//********************************************************************
	//DEVELOPMENT and TESTING AREA -- options and command lines related to these may not work 
	
	public static String debug_machine; //TEMP related to dev command line arg -cluster
	
	public static String current_experiment_id;
	public static LocalDateTime experiment_timestamp; //updated for each dataset
	public static LocalDateTime application_start_timestamp; //updated only once
	public static String application_start_timestamp_formatted;
	public static ArrayList<String> repeat_guids;

	
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
	
	public static String temp_data_path = "E:/data/ucr";	//only used during testing and development for some other main functions eg. see Util.loadData - dont use this in any production part of the code
	
	//END DEV SECTION ********************************************************************
	
	
	
	//DEFAULT SETTINGS, these are overridden by command line arguments
	public static long rand_seed = 0;	//TODO set seed to reproduce results -- not implemented
	public static Random rand;
	
	public static int num_threads = 0;	// if 0 all available CPUs are used
	public static int verbosity = 2; //0, 1, 2, 3
	public static int export_level = 3; //0, 1, 2 

	public static String training_file;
	public static String testing_file;
	public static String output_dir = "output/dev/";
	public static boolean csv_has_header = false;
	public static boolean target_column_is_first = true;

	public static int num_repeats = 1;
	public static int num_trees = 100;
	public static boolean random_dm_per_node = true;
	public static boolean shuffle_dataset = false;
	public static boolean binary_split = false;	
	
	//@charlotte experiments 
	public static boolean boosting = false;
	public static int min_leaf_size = 1;
	public static boolean gini_split = false;	
	//end @charlotte experiments
	
	
	//splitter settings
	public static int num_splitters_per_node;
	//if using_probabilities_to_choose_num_splitters_per_node == false, then use exact numbers
	public static int ee_splitters_per_node;
	public static int randf_splitters_per_node;
	public static int rotf_splitters_per_node;
	public static int st_splitters_per_node;
	public static int boss_splitters_per_node;
	public static int tsf_splitters_per_node;
	public static int rif_splitters_per_node;
	
	public static boolean ee_enabled = false;
	public static boolean randf_enabled = false;
	public static boolean rotf_enabled = false;
	public static boolean st_enabled = false;
	public static boolean boss_enabled = false;
	public static boolean tsf_enabled = false;
	public static boolean rif_enabled = false;
	
	//actual num of splitters needed
	public static int num_actual_splitters_needed;
	public static int num_actual_tsf_splitters_needed;
	public static int num_actual_rif_splitters_needed_per_type;
	
	//ED
	public static boolean gini_approx = false;
	public static double gini_percent = 1;	
	public static int approx_gini_min = 10;	
	public static boolean approx_gini_min_per_class = true;	
	
	
	//BOSS
//	public static String boss_params = "random"; // {random, best, cv_on_sample} 
	public static String boss_params_files = "settings/boss/"; // default path if no path is provided in cmd args. A path is needed if ParamSelection method is PreLoadedSet
	public static transient HashMap<String, List<BossParams>> boss_preloaded_params;
	public static int boss_trasformations = 1000;
//	public static boolean boss_transform_at_forest_level = true;	//transform once per tree, or store many at the forest and at each node use at most num_splitters_per_node params to evaluate
//	public static boolean boss_use_gini_split = false;
	public static boolean boss_use_numerosity_reduction = true;
	public static TransformLevel boss_transform_level = TransformLevel.Forest;
	public static ParamSelection boss_param_selection = ParamSelection.Random;
	public static SplitMethod boss_split_method = SplitMethod.Nary_NearestClass;	
	
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
		Binary_Gini,	
		//picks one example per class and calculate distance/similarity to exemplar to split
		//results in n'nary splits
		//eg. ElasticDistance splitter uses this method
		Nary_NearestClass,	
		
		//TODO -- work on this
		//picks two examples and finds distance to them, then uses gini to find a good split point... 
		//eg Similarity Forest uses this method -- binary splits implemented by Charlotte uses this method
		Binary_NearestExample,
	}
	
	//TODO only gini is implemented
	public enum SplitEvaluateMethods{
		Gini,
		InformationGain,
		GainRatio
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
		PreLoadedSet,	//precalculated values are loaded from files -- must provide a path to files in the cmd args.
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

	public static Runtime runtime = Runtime.getRuntime();	
	
	//TODO move these to DataStore class -- to manage multiple transformations, bagging, boosting, etc..
	private static transient TSDataset train_data;
	private static transient TSDataset test_data;
	private static String datasetName; 
	public static String currentOutputFilePrefix;
	
	
	public static int num_classes; //in both training and test data TODO refactor 
	public static int[] unique_classes;//in both training and test data TODO refactor 
	public static TIntIntMap class_distribution; //in both training and test data
	public static String print_decimal_places = "#0.00000";
	public static DecimalFormat df = new DecimalFormat(AppContext.print_decimal_places);

	static {
		//TODO WARNING implementing this feature is not complete or its broken due to inconsistencies in implementation among different classes
		//NOTE many classes use ThreadLocalRandom which doesnt allow setting a seed
		// research more about this -- SplitRandom is also used by some classes
		//
		if (rand_seed == 0) {
			rand_seed = System.nanoTime();
		}
		
		rand_seed = System.nanoTime();
		rand = new Random(rand_seed);	
		
	}

	public static Random getRand() {
		return rand;
	}

	public static TSDataset getTraining_data() {
		return train_data;
	}

	public static void setTraining_data(TSDataset train_data) {
		AppContext.train_data = train_data;
	}

	public static TSDataset getTesting_data() {
		return test_data;
	}

	public static void setTesting_data(TSDataset test_data) {
		AppContext.test_data = test_data;
	}

	public static String getDatasetName() {
		return datasetName;
	}

	public static void setDatasetName(String datasetName) {
		AppContext.datasetName = datasetName;
	}
	
	public static String getVersionString(boolean pathFriendly) {
		if (pathFriendly) {
			return AppContext.version.replace(".", "-");
		}else {
			return AppContext.version;
		}
		
	}
	
	public static void updateClassDistribution(TSDataset train_data, TSDataset test_data) {
		
		class_distribution = new TIntIntHashMap();
		
		//create a copy //TODO can be done better? 
		for (int key : train_data.get_class_map().keys()) {
			if (class_distribution.containsKey(key)) {
				class_distribution.put(key, class_distribution.get(key) + train_data.get_class_map().get(key));
			}else {
				class_distribution.put(key, train_data.get_class_map().get(key));
			}
		}		
		
		
		//create a copy //TODO can be done better? 
		for (int key : test_data.get_class_map().keys()) {
			if (class_distribution.containsKey(key)) {
				class_distribution.put(key, class_distribution.get(key) + test_data.get_class_map().get(key));
			}else {
				class_distribution.put(key, test_data.get_class_map().get(key));
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

				Field[] fields = AppContext.class.getFields();
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
		final int num_sets = 9;
		String[][] datasets = new String[num_sets][];
		
		//set 1
		datasets[0] = new String[] {
				"SonyAIBORobotSurface1","TwoLeadECG","ECG5000",  "ToeSegmentation1", "DistalPhalanxTW","Meat", "Lightning2","Worms",
				 "CBF", "ShapeletSim", "Wine", "MiddlePhalanxOutlineCorrect", "Mallat","CricketZ", "SyntheticControl", 
				"LargeKitchenAppliances", "DistalPhalanxOutlineAgeGroup", "Ham", "MedicalImages", "PhalangesOutlinesCorrect", 
				 "FiftyWords",  "Computers",  "FacesUCR", "ShapesAll", "InlineSkate", "CinCECGtorso","UWaveGestureLibraryX","UWaveGestureLibraryZ", "Phoneme", "FordA"
		};
		
		//set 2
		datasets[1] = new String[] {
				"ItalyPowerDemand", "BirdChicken", "Coffee",  "BeetleFly", "Symbols", "Haptics", "ScreenType",  "ProximalPhalanxOutlineAgeGroup", 
				"SonyAIBORobotSurface2", "ECG200",   "FaceAll", "DistalPhalanxOutlineCorrect", "SmallKitchenAppliances", "Wafer",
				"Earthquakes", "ArrowHead", "OliveOil", "OSULeaf","Adiac", "CricketY","RefrigerationDevices", "ChlorineConcentration","CricketX",
				"MoteStrain",  "Yoga", "MiddlePhalanxOutlineAgeGroup","Herring",   "StarlightCurves", "NonInvasiveFetalECGThorax2", "FordB"
		};
		
		//set 3
		datasets[2] = new String[] {
				"Car", "Plane", "TwoPatterns",  "WormsTwoClass", "DiatomSizeReduction", "Fish", "InsectWingbeatSound", 
				"Strawberry", "ProximalPhalanxOutlineCorrect", "Trace", "ECGFiveDays", "GunPoint", "Lightning7","SwedishLeaf",
				"MiddlePhalanxTW", "ToeSegmentation2", "Beef", "WordSynonyms", "FaceFour", 
				 "ProximalPhalanxTW", "ElectricDevices","NonInvasiveFetalECGThorax1", "UWaveGestureLibraryY", "UWaveGestureLibraryAll", "HandOutlines"
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
		return datasets[set_id-1];
	}

	public static String[] getNRandomDatasets(int num_datasets) {
		String[] datasets = getAllDatasets();
		
		String[] random_set = new String[num_datasets];
		
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < datasets.length; i++) {
			indices.add(i);
		}
		
		Collections.shuffle(indices);
		
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
		
		System.out.println("Loading precalculated boss params from: " + AppContext.boss_params_files);

		File folder = new File(AppContext.boss_params_files);
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
