package application;

public class AppSetting {

	
	
	//java -Xmx40000m -jar app1118_tsf_rif.jar -train=../../data/ucr/ -boss_params=random -boss_trasformations=500 -export=3 -ucr=all -out=out/fg_boss100t500_r5_1118 -s=boss:100
//	-train=E:/data/resampled/InlineSkate/InlineSkate_TRAIN_32.arff -test=E:/data/resampled/InlineSkate/InlineSkate_TEST_32.arff -target_column=last -boss_trasformations=100 -export=3 -repeats=1 -threads=1 -trees=100 -out=out/resample/InlineSkate/temp/  -s=ee:5,boss:100,tsf:100,rif:100
//	-train=E:/data/Phonemes/CSV/weka_stratified512/all512_train_b32.arff -test=E:/data/Phonemes/CSV/weka_stratified512/all512_test.arff -target_column=last -boss_trasformations=100 -export=3 -repeats=1 -threads=1 -trees=100 -out=out/phoneme/temp/  -s=tsf:20			
	
//java -Xmx200000m -jar app1123_tsf_rif.jar  -train=../../data/SatelliteFull/SatelliteFull_TRAIN_c$1.arff -test=../../data/SatelliteFull/SatelliteFull_TEST_1000.arff -target_column=last -boss_trasformations=1000 -export=3 -repeats=1 -threads=1 -trees=100 -out=output/app1123/$1/  -s=ee:5,boss:100,rif:100  
//java -Xmx200000m -jar cote_experiments_v3.jar  hivecote false predefined ../../data/SatelliteFull/ SatelliteFull SatelliteFull_TRAIN_c$1 SatelliteFull_TEST_1000 output/hcote/$1 .arff 1

	
	
	
	//TODO test support file paths with a space?
	public static final String[] test_args = new String[]{
//			-ucr option is optional, if not used -train and -test should give full paths to training and test files
//			"-ucr=range0to10", //supports a range from start to end (end is exclusive)  eg. range0to84 
//			"-ucr=random{n}" //support selecting n random datasets eg. -ucr=random10 selects 10 random datasets, default n = 1
//			"-ucr=set4" //supports selecting a predefined set
//			"-ucr=all" //runs all 85 ucr datasets == to range0to84
//			"-ucr=ItalyPowerDemand", 	//runs a single ucr dataset
//			"-ucr=ItalyPowerDemand,Beef,Wine,DistalPhalanxOutlineAgeGroup,GunPoint" //runs a list of datasets, separated by comma
			"-ucr=Coffee", 
//			"-ucr=Adiac,ArrowHead,Beef,BeetleFly,BirdChicken,Car,CBF,ChlorineConcentration,Coffee,Computers,CricketX,CricketY,CricketZ,DiatomSizeReduction,DistalPhalanxOutlineAgeGroup,DistalPhalanxOutlineCorrect,DistalPhalanxTW,Earthquakes,ECG200,ECG5000,ECGFiveDays",
//			"-ucr=FaceAll,FaceFour,FacesUCR,FiftyWords,Fish,GunPoint,Ham,InsectWingbeatSound,LargeKitchenAppliances,Lightning2,Lightning7,Meat,MedicalImages,MiddlePhalanxOutlineAgeGroup,MiddlePhalanxOutlineCorrect,MiddlePhalanxTW,MoteStrain,OliveOil,OSULeaf,PhalangesOutlinesCorrect,Plane,ProximalPhalanxOutlineAgeGroup,ProximalPhalanxOutlineCorrect,ProximalPhalanxTW,RefrigerationDevices,ScreenType,ShapeletSim,ShapesAll,SmallKitchenAppliances,SonyAIBORobotSurface1,SonyAIBORobotSurface2,Strawberry,SwedishLeaf,Symbols,SyntheticControl,ToeSegmentation1,ToeSegmentation2,Trace,TwoLeadECG,TwoPatterns,UWaveGestureLibraryX,UWaveGestureLibraryY,UWaveGestureLibraryZ,Wafer,Wine,WordSynonyms,Worms,WormsTwoClass,Yoga",
			"-train=E:/data/ucr/",
			"-test=E:/data/ucr/",	//optional arg, if not specified, same folder as training path is searched
//			"-train=E:/data/satellite/sample100000_TRAIN.txt", 	//use full path to the file if not using -ucr option
//			"-test=E:/data/satellite/sample100000_TEST.txt", 	//use full path to the file if not using -ucr option
			"-out=output/dev/temp",
//			"-out=E:/git/experiments/02oct18/s10ag10m1_eerandf",
			"-repeats=1",
			"-trees=100",   
			"-shuffle=false",	//keep this false unless you want to shuffle training or test sets. not needed for ucr train/test splits we use
//			"-jvmwarmup=true",	//disabled -- not implemented in this version
			"-export=0",	//if 0 no export, if 1 only json, if 2 only csv, if 3 both csv and json
			"-verbosity=2",	//if 0 min printing, 1 is ideal, if 2 or more lots of debug printing
			"-csv_has_header=false", 
			"-target_column=first",	//{first,last} which column is the label column in your csv file?
			"-threads=1",	//0 = one thread per available core (including virtual cores)
			
			//"-binary_split=false",  //experimental
			"-gini_split=false",
			//"-boosting=false",  //experimental
			"-min_leaf_size=1",	// set to 1 to get pure node
			
			//TODO difficult to implement this because ThreadLocalRandom does not support setting seed, now looking at SplittableRandom class
			//requires lots of refactoring and some research to make this work with multithreading. -- WIP (work in progress)
//			"-rand_seed=0" 	//TODO if 0 use System.nanoTime(), else use this number
			
			//#candidate splits to try at each node
//			"-s=ee:5,boss:100",
			"-s=st:1",
//			"-s_prob=10:equal",	//TODO implementation not finished
//			"-splitters=boss, ee", //dont need this setting now, enable splitters implicitly using the -s option			

			"-ee_approx_gini=false",
			"-ee_approx_gini_min=10",
			"-ee_approx_gini_min_per_class=true",
			"-ee_approx_gini_percent=1",
			"-ee_dm=euc,dtw,dtwr,ddtw,ddtwr,wdtw,wddtw,lcss,twe,erp,msm",
			"-ee_dm_on_node=true", //dm selection per tree or per node
			
			"-randf_m=sqrt",	// {sqrt, loge, log2, 0<integer<#features} 
			
			"-boss_params=random",	//// {random, best, cv_on_sample}  //TODO only random and best is implemented, best needs param files in a folder in project dir
			"-boss_trasformations=100",	//number of transformations to precompute at the transform level - only forest level supports this, tree level uses a single transform per tree-- will require large amount of memory for a big dataset
			"-boss_preload_params_path=settings/boss/",	//if -boss_params=best, params are loaded from this path
			"-boss_transform_level=forest", // {forest,tree,node}, node is not implemented. 
			"-boss_split_method=nearest", // {gini, nearest}, if nearest, will find similarity of each instance at node to a random example from each class, similar to Proximity Forest, if gini, will split similar to a classic decision tree using gini //NOTE: gini doesnt work well -- need to check why
           
			//TODO

//			"tsf_trasformations=0",	//not valid, when transform level is node
			"-tsf_m=0",	// use all attributes to train splitter, feature bagging is handled by the random intervals
			"-tsf_min_interval=3", //TODO NOTE min x was used in HiveCOTE paper, this doesnt match code
//			"-tsf_transform_level=node", //only node is implemented
			"-tsf_num_intervals=1", //detect auto
//			"tsf_feature_selection=int"
			
//			"-rif_trasformations=0", 	//not valid, when transform level is node
			"-rif_m=0",
			"-rif_min_interval=16",	//TODO NOTE min 16 was used in HiveCOTE paper, this doesnt match code, if min interval is close to m ...
//			"-rif_transform_level=node", //only node is implemented
			"-rif_components=acf_pacf_arma_ps_sep",
			"-rif_same_intv_component=false",	//use same random interval for each set of component
			"-tsf_num_intervals=1", //detect auto 
			

	};
	
	public static final String[] prod_args = new String[]{
			"-machine=pc",
			"-ucr=Coffee",
			"-out=output/dev/temp-time2",
			"-trees=10",
			"-s=ee:5,rif:100,boss:100"
	};

	public static final String[] boss_exp = new String[]{
			"-train=E:/data/ucr/",
			"-repeats=2",
			"-ucr=set5",
			"-out=output/1124/e5b50t1000rif50/",
			"-boss_trasformations=1000",
			"-s=ee:5,boss:50,rif:50"
	};	
	
	public static final String[] resample_exp = new String[]{
			"-train=E:/data/resampled/InlineSkate/InlineSkate_TRAIN_2048.csv",
			"-test=E:/data/resampled/InlineSkate/InlineSkate_TEST_2048.csv",
			"-target_column=last",
			"-repeats=1",
			"-out=output/1124/resample/temp/",
			"-boss_trasformations=1000",
			"-s=ee:5,boss:100,rif:100",
			"-threads=0"
	};	
}
