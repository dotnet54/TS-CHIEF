package util;

import java.text.DecimalFormat;
import java.util.Arrays;

import data.timeseries.Dataset;
import org.apache.commons.lang3.StringUtils;

import application.AppConfig;
import data.timeseries.MTSDataset;
import gnu.trove.map.TIntObjectMap;
import trees.splitters.boss.dev.BossTransformContainer;

public class PrintUtilities {

	public static void abort(Exception e) {
//        System.out.println("\nFatal Error::" + e.getMessage() + "\n\n");
        System.err.println("\nFatal Error:: " + e.getMessage() + "\n");
        e.printStackTrace();
        System.exit(-1);
	}
	
	public static void printMemoryUsage() {
		PrintUtilities.printMemoryUsage(false);
	}	
	
	public static void printMemoryUsage(boolean minimal) {
		long avail_mem, free_mem, used_mem;
		avail_mem = AppConfig.runtime.totalMemory() / AppConfig.ONE_MB;
		free_mem = AppConfig.runtime.freeMemory() / AppConfig.ONE_MB;
		used_mem = avail_mem - free_mem;
		if (minimal) {
			System.out.print("(" + used_mem + "/" + avail_mem + "MB) ");
		}else {
			System.out.println("Using: " + used_mem + " MB, Free: " + free_mem 
					+ " MB, Allocated Pool: " + avail_mem+ " MB, Max Available: " 
					+ AppConfig.runtime.maxMemory()/ AppConfig.ONE_MB + " MB");				
		}

	}

	public static void printConfiguration() {
		System.out.println("Repeats: " + AppConfig.num_repeats + " , Trees: " + AppConfig.num_trees + " , Shuffle Data: " + AppConfig.shuffle_dataset + ", JVM WarmUp: " + AppConfig.warmup_java);
		System.out.println("OutputDir: " + AppConfig.output_dir + ", Export: " + AppConfig.export_level + ", Verbosity: " + AppConfig.verbosity);
		
		//splitter settings
		String prefix;
		System.out.println("Enabled Splitters: " + StringUtils.join(AppConfig.enabled_splitters, ","));
		
		//TODO if using probabilities to choose splitters print them here

		if (AppConfig.use_probabilities_to_choose_num_splitters_per_node) {
			System.out.println("Splitter Probabilities Per Node(s): " + AppConfig.num_splitters_per_node + ", using probabilities: ....");
			//else print exact #splitters to try per splitter			
		}else {
			System.out.println("Splitters Per Node(s): " + AppConfig.num_splitters_per_node + " ("
					+ "ee:" + AppConfig.ee_splitters_per_node
					+ ",randf:" + AppConfig.randf_splitters_per_node
					+ ",rotf:" + AppConfig.rotf_splitters_per_node
					+ ",st:" + AppConfig.st_splitters_per_node
					+ ",boss:" + AppConfig.boss_splitters_per_node
					+ ",tsf:" + AppConfig.tsf_splitters_per_node
					+ ",rise:" + AppConfig.rif_splitters_per_node
					+ ",it:" + AppConfig.it_splitters_per_node
					+ ",rt:" + AppConfig.rt_splitters_per_node
					+ ")");

		}	
				
		prefix = "ED:\t";
		if (AppConfig.ee_enabled) {
			System.out.println(prefix + "Enabled DMs (" + AppConfig.enabled_distance_measures.length + "): " +  StringUtils.join(AppConfig.enabled_distance_measures, ","));
			System.out.println(prefix + "Random DM per Node: " + AppConfig.random_dm_per_node );
			System.out.println(prefix + "ApproxGini: " + AppConfig.gini_approx +  ", GiniPercent: " + AppConfig.gini_percent + ", GiniMin: " + AppConfig.approx_gini_min + ", GiniMinPerClass: " + AppConfig.approx_gini_min_per_class);
	//		System.out.println("Approximate Gini: " + AppContext.use_approximate_gini);
		}

			
		prefix = "BOSS:\t";
		if (AppConfig.boss_enabled) {
			System.out.println(prefix + "params=" + AppConfig.boss_param_selection + ", param_files=" + AppConfig.boss_params_files 
					+ ", transform_level: " + AppConfig.boss_transform_level
					+ ", split_method: " + AppConfig.boss_split_method
					+ ", word_len={" + Arrays.toString(BossTransformContainer.getBossWordLengths()) + "}");
		}


		prefix = "RandF:\t";
		if (AppConfig.randf_enabled) {
			System.out.println(prefix + "m=" + AppConfig.randf_m);
		}
		
		prefix = "RotF:\t";
		if (AppConfig.rotf_enabled) {

		}
		
		prefix = "ST:\t";
		if (AppConfig.st_enabled) {

		}

		prefix = "TSF:\t";
		if (AppConfig.tsf_enabled) {
//			System.out.println(prefix + "tsf_splitters_per_node=" + (int) Math.ceil((float) AppContext.tsf_splitters_per_node / (float)( 3 * AppContext.tsf_num_intervals)) +  " (num of tsf splitters used per node is s_tsf / 3 * num intervals per tsf splitter (1) -- 3 attribs per splitter )");
			System.out.println(prefix + "num_actual_tsf_splitters_needed: " 
					+ AppConfig.num_actual_tsf_splitters_needed +", approx_num_gini_per_node: " 
					+ AppConfig.num_actual_tsf_splitters_needed * 3 +",m=" + AppConfig.tsf_m);
		}

		
		prefix = "RISE:\t";
		if (AppConfig.rif_enabled) {
//			System.out.println(prefix + "rif_splitters_per_node per type=" +AppContext.rif_splitters_per_node/4 +  " (total num of rif splitters / 4 i.e. to keep an equal number of splitters for each of the 4 rif splitter types --acf,pacf,arma,ps)");
			System.out.println(prefix + "num_actual_rif_splitters_needed_per_type: " 
					+ AppConfig.num_actual_rif_splitters_needed_per_type +", approx_num_gini_per_node: " 
					+ AppConfig.rif_m * AppConfig.num_actual_rif_splitters_needed_per_type * 4 +",m=" + AppConfig.rif_m);
		}

		prefix = "Inception:\t";
		if (AppConfig.it_enabled) {
			System.out.println(prefix +"m=" + AppConfig.it_m);
		}
		
		prefix = "Rocket:\t";
		if (AppConfig.rt_enabled) {
			System.out.println(prefix +"m=" + AppConfig.rt_m);
		}
		
		//	
		
		System.out.println("---------------------------------------------------------------------------------------------------------------------");
		 

	}
	
	public static void printDatasetInfo() {
		System.out.println("Dataset: " + AppConfig.getDatasetName() 
		+ ", Training Data (size x length, classes): " + AppConfig.getTrainingSet().size() + "x" + AppConfig.getTrainingSet().length() 
			+ ", " +AppConfig.getTrainingSet().getNumClasses()
		+ " , Testing Data: " + AppConfig.getTestingSet().size() + "x" + AppConfig.getTestingSet().length() 
			+ ", " +AppConfig.getTestingSet().getNumClasses()
			);
	}
	
	
	public static String print_split(TIntObjectMap<Dataset> splits) {
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat(AppConfig.print_decimal_places);

		for (int key : splits.keys()) {
			sb.append(key + "=" +  splits.get(key).getClassDistribution().toString() + "=" + df.format(splits.get(key).gini())  + ", ");
			
		}
		
		sb.append("wgini = " + df.format(Util.weighted_gini(splits)));

		return sb.toString();
	}

}
