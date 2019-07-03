package util;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import core.AppContext;
import datasets.TSDataset;
import dev.BossTransformContainer;
import gnu.trove.map.TIntObjectMap;

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
		avail_mem = AppContext.runtime.totalMemory() / AppContext.ONE_MB;
		free_mem = AppContext.runtime.freeMemory() / AppContext.ONE_MB;
		used_mem = avail_mem - free_mem;
		if (minimal) {
			System.out.print("(" + used_mem + "/" + avail_mem + "MB) ");
		}else {
			System.out.println("Using: " + used_mem + " MB, Free: " + free_mem 
					+ " MB, Allocated Pool: " + avail_mem+ " MB, Max Available: " 
					+ AppContext.runtime.maxMemory()/ AppContext.ONE_MB + " MB");				
		}

	}

	public static void printConfiguration() {
		System.out.println("Repeats: " + AppContext.num_repeats + " , Trees: " + AppContext.num_trees + " , Shuffle Data: " + AppContext.shuffle_dataset + ", JVM WarmUp: " + AppContext.warmup_java);
		System.out.println("OutputDir: " + AppContext.output_dir + ", Export: " + AppContext.export_level + ", Verbosity: " + AppContext.verbosity);
		
		//splitter settings
		String prefix;
		System.out.println("Enabled Splitters: " + StringUtils.join(AppContext.enabled_splitters, ","));
		
		//TODO if using probabilities to choose splitters print them here

		if (AppContext.use_probabilities_to_choose_num_splitters_per_node) {
			System.out.println("Splitter Probabilities Per Node(s): " + AppContext.num_splitters_per_node + ", using probabilities: ....");
			//else print exact #splitters to try per splitter			
		}else {
			System.out.println("Splitters Per Node(s): " + AppContext.num_splitters_per_node + " ("
					+ "ee:" + AppContext.ee_splitters_per_node
					+ ",randf:" + AppContext.randf_splitters_per_node
					+ ",rotf:" + AppContext.rotf_splitters_per_node
					+ ",st:" + AppContext.st_splitters_per_node
					+ ",boss:" + AppContext.boss_splitters_per_node
					+ ",tsf:" + AppContext.tsf_splitters_per_node
					+ ",rif:" + AppContext.rif_splitters_per_node
					+ ")");

		}	
				
		prefix = "ED:\t";
		System.out.println(prefix + "Enabled DMs (" + AppContext.enabled_distance_measures.length + "): " +  StringUtils.join(AppContext.enabled_distance_measures, ","));
		System.out.println(prefix + "Random DM per Node: " + AppContext.random_dm_per_node );
		System.out.println(prefix + "ApproxGini: " + AppContext.gini_approx +  ", GiniPercent: " + AppContext.gini_percent + ", GiniMin: " + AppContext.approx_gini_min + ", GiniMinPerClass: " + AppContext.approx_gini_min_per_class);
//		System.out.println("Approximate Gini: " + AppContext.use_approximate_gini);
			
		prefix = "BOSS:\t";
		System.out.println(prefix + "params=" + AppContext.boss_param_selection + ", param_files=" + AppContext.boss_params_files 
				+ ", transform_level: " + AppContext.boss_transform_level
				+ ", split_method: " + AppContext.boss_split_method
				+ ", word_len={" + Arrays.toString(BossTransformContainer.getBossWordLengths()) + "}");

		prefix = "RandF:\t";
		System.out.println(prefix + "m=" + AppContext.randf_m);

		prefix = "RotF:\t";

		prefix = "ST:\t";

		prefix = "TSF:\t";
		if (AppContext.tsf_enabled) {
//			System.out.println(prefix + "tsf_splitters_per_node=" + (int) Math.ceil((float) AppContext.tsf_splitters_per_node / (float)( 3 * AppContext.tsf_num_intervals)) +  " (num of tsf splitters used per node is s_tsf / 3 * num intervals per tsf splitter (1) -- 3 attribs per splitter )");
			System.out.println(prefix + "num_actual_tsf_splitters_needed: " 
					+ AppContext.num_actual_tsf_splitters_needed +", approx_num_gini_per_node: " 
					+ AppContext.num_actual_tsf_splitters_needed * 3 +",m=" + AppContext.tsf_m);
		}

		
		prefix = "RISE:\t";
		if (AppContext.rif_enabled) {
//			System.out.println(prefix + "rif_splitters_per_node per type=" +AppContext.rif_splitters_per_node/4 +  " (total num of rif splitters / 4 i.e. to keep an equal number of splitters for each of the 4 rif splitter types --acf,pacf,arma,ps)");
			System.out.println(prefix + "num_actual_rif_splitters_needed_per_type: " 
					+ AppContext.num_actual_rif_splitters_needed_per_type +", approx_num_gini_per_node: " 
					+ AppContext.rif_m * AppContext.num_actual_rif_splitters_needed_per_type * 4 +",m=" + AppContext.rif_m);
		}


		
		//	
		
		System.out.println("---------------------------------------------------------------------------------------------------------------------");
		 

	}
	
	public static void printDatasetInfo() {
		System.out.println("Dataset: " + AppContext.getDatasetName() 
		+ ", Training Data (size x length, classes): " + AppContext.getTraining_data().size() + "x" + AppContext.getTraining_data().length() 
			+ ", " +AppContext.getTraining_data().get_num_classes() 
		+ " , Testing Data: " + AppContext.getTesting_data().size() + "x" + AppContext.getTesting_data().length() 
			+ ", " +AppContext.getTesting_data().get_num_classes()
			);
	}
	
	
	public static String print_split(TIntObjectMap<TSDataset> splits) {
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat(AppContext.print_decimal_places);

		for (int key : splits.keys()) {
			sb.append(key + "=" +  splits.get(key).get_class_map().toString() + "=" + df.format(splits.get(key).gini())  + ", ");
			
		}
		
		sb.append("wgini = " + df.format(Util.weighted_gini(splits)));

		return sb.toString();
	}

}
