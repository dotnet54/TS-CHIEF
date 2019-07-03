package core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import trees.ProximityForest;
import trees.ProximityTree;
import util.Statistics;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ProximityForestResult {
	
	private transient ProximityForest forest;
	public boolean results_collated = false;
	
	public String datasetName;
	
	//FILLED BY FOREST CLASS
	public int forest_id = -1;
	public String repetition_id;	//just a unique id to store in exported file -- helps to group data in pandas

//	public int majority_vote_match_count = 0;

	public long startTimeTrain;
	public long endTimeTrain;
	public long elapsedTimeTrain;
	public String train_time_formatted;
	
	public long startTimeTest;
	public long endTimeTest;
	public long elapsedTimeTest;	
	public String test_time_formatted;
			
	public int errors, correct;	//NOTE TODO change to atomic ints?
	public double accuracy, error_rate;	
	
	
	//FILLED BY STAT COLLECTOR CLASS

	public int total_num_trees;

	//num nodes
	public double mean_num_nodes;
	public double sd_num_nodes;
//	public int min_num_nodes_per_tree;
//	public int max_num_nodes_per_tree;

	//depth
	public double mean_depth;
	public double sd_depth;
//	public int min_depth_per_tree;
//	public int max_depth_per_tree;	
	

	//weighted depth //TODO comment add formula
	public double mean_weighted;
	public double sd_weighted;
//	public int min_weighted_depth_per_tree;
//	public int max_weighted_depth_per_tree;	
	
	//memory
//	int memory_usage = 0;
	
	//distance timings
//	public long dtw_time;
//	public long dtwcv_time;
//	public long ddtw_time;
//	public long ddtwcv_time;
//	public long wtw_time;
//	public long wddtw_time;	
//	public long euc_time;
//	public long lcss_time;
//	public long msm_time;	
//	public long twe_time;
//	public long erp_time;	
	
	//call counts
//	public long dtw_count;
//	public long dtwcv_count;
//	public long ddtw_count;
//	public long ddtwcv_count;
//	public long wtw_count;
//	public long wddtw_count;	
//	public long euc_count;
//	public long lcss_count;
//	public long msm_count;	
//	public long twe_count;
//	public long erp_count;	
	
	//splitters
	public long boss_transform_time;
	public long mem_before_boss_transform;//TODO
	public long after_before_boss_transform;//TODO
	
	public ProximityForestResult(ProximityForest forest) {
		this.forest = forest;
		this.forest_id = forest.getForestID();
	}
	
	public void collateResults() {
		
		if (results_collated) {
			return;
		}		
		
		ProximityTree[] trees = forest.getTrees();
		ProximityTree tree;
		TreeStatCollector tree_stats;
		
		total_num_trees = trees.length;
		
		int nodes[] = new int[total_num_trees];
		double depths[] = new double[total_num_trees];
		double weighted_depths[] = new double[total_num_trees];
				
		for (int i = 0; i < total_num_trees; i++) {
			tree = trees[i];
			tree_stats = tree.getTreeStatCollection();
			
			nodes[i] = tree_stats.num_nodes;
			depths[i] = tree_stats.depth;
			weighted_depths[i] = tree_stats.weighted_depth;
			
		}
		mean_num_nodes = Statistics.mean(nodes);
		sd_num_nodes = Statistics.standard_deviation_population(nodes);
		
		mean_depth = Statistics.mean(depths);
		sd_depth = Statistics.standard_deviation_population(depths);
		
		mean_weighted = Statistics.mean(weighted_depths);
		sd_weighted = Statistics.standard_deviation_population(weighted_depths);
		
		train_time_formatted = DurationFormatUtils.formatDuration((long) (elapsedTimeTrain/AppContext.NANOSECtoMILISEC), "H:m:s.SSS");
		test_time_formatted = DurationFormatUtils.formatDuration((long) (elapsedTimeTest/AppContext.NANOSECtoMILISEC), "H:m:s.SSS");

		
		results_collated = true;
	}
	
	public void printResults(String datasetName, int experiment_id, String prefix) {
		this.datasetName = datasetName;
//		System.out.println(prefix+ "-----------------Experiment No: " 
//				+ experiment_id + " (" +datasetName+ "), Forest No: " 
//				+ (this.forest_id) +"  -----------------");
		
		if (AppContext.verbosity > 0) {
			String time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTrain/AppContext.NANOSECtoMILISEC), "H:m:s.SSS");
	        System.out.format("%sTraining Time: %fms (%s)\n",prefix, elapsedTimeTrain/AppContext.NANOSECtoMILISEC, time_duration);
			time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTest/AppContext.NANOSECtoMILISEC), "H:m:s.SSS");		
	        System.out.format("%sPrediction Time: %fms (%s)\n",prefix, elapsedTimeTest/AppContext.NANOSECtoMILISEC, time_duration);
	
	        
	        System.out.format("%sCorrect(TP+TN): %d vs Incorrect(FP+FN): %d\n",prefix,  correct, errors);
	        System.out.println(prefix+"Accuracy: " + accuracy);
	        System.out.println(prefix+"Error Rate: "+ error_rate);			
		}

        
        this.collateResults();
        
        //this is just the same info in a single line, used to grep from output and save to a csv, use the #: marker to find the line easily
       
        //the prefix REPEAT# is added to this comma separated line easily use grep from command line to filter outputs to a csv file
        //just a quick method to filter important info while in command line
        
        String pre = "REPEAT#," + (experiment_id+1) +" ,";
		System.out.print(pre + datasetName);        
		System.out.print(", " + accuracy);
		System.out.print(", " + elapsedTimeTrain /AppContext.NANOSECtoMILISEC + " ms") ;
		System.out.print(", " + elapsedTimeTest /AppContext.NANOSECtoMILISEC + " ms");
		System.out.print(", " + mean_depth);
//		System.out.print(", " + mean_weighted_depth_per_tree);
		System.out.println();
	}
	
	public String exportJSON(String datasetName, int repeat_id, String fileName, boolean last_record) throws Exception {
		
		//String shortFileName;
//		if (fileName == null) {
//			String timestamp = LocalDateTime.now()
//				       .format(DateTimeFormatter.ofPattern(AppContext.FILENAME_TIMESTAMP_FORMAT));		
//			
//			fileName = AppContext.output_dir + File.separator + datasetName  + "_"+ timestamp + "_r" + repeat_id;
//			
//			File fileObj = new File(fileName);
//			
//			fileObj.getParentFile().mkdirs();
//			fileObj.createNewFile();
//		}
		
		String timestamp = AppContext.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppContext.FILENAME_TIMESTAMP_FORMAT));	
		
//		fileName = AppContext.output_dir + File.separator + AppContext.current_experiment_id + "_" //+ timestamp File.separator + AppContext.current_experiment_id 
//				+ forest_id + "_" 
//				+ repetition_id + "_" + AppContext.getVersionString(true) + File.separator + "json" + File.separator + fileName
//				+ "_eid" + AppContext.current_experiment_id +".json";
		
		fileName = AppContext.output_dir + File.separator + AppContext.application_start_timestamp_formatted 
				+ File.separator + timestamp + "_" + datasetName
				+ File.separator + fileName + "_r" + repeat_id 
				+ "_eid" + AppContext.current_experiment_id + "_rid" + repetition_id + "_v"+ AppContext.getVersionString(true)
				+ ".json";			
		
		System.out.println("writing json file: " + fileName);

		File fileObj = new File(fileName);
		fileObj.getParentFile().mkdirs();
		fileObj.createNewFile();		
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){		
		
			Gson gson;
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.serializeSpecialFloatingPointValues();
			gsonBuilder.serializeNulls();
			gsonBuilder.registerTypeAdapter(Class.class, new AppContext.ClassTypeAdapter());
			gson = gsonBuilder.create();
			
//			SerializableResultSet object = new SerializableResultSet(this.forests);
			
			if (repeat_id == 0) {
				
				StringBuilder buffer = new StringBuilder();
				buffer.append('{');
				
				//write appcontext
				buffer.append("\"settings\": ");
				buffer.append(gson.toJson(AppContext.class));
				buffer.append(',');
		
				buffer.append("\"repeats\" : [");
			
				bw.write(buffer.toString());
			}
			bw.write(gson.toJson(this));
			
			if (last_record) {
				bw.write("]}");
			}else {
				bw.write(",");
			}
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
//			bw.close();
		}
					
		return fileName;
	}

	
	public String exportCSV(String datasetName, int repeat_id, String fileName) throws Exception {
		
//		if (fileName == null) {
//			String timestamp = LocalDateTime.now()
//				       .format(DateTimeFormatter.ofPattern(AppContext.FILENAME_TIMESTAMP_FORMAT));		
//			
//			fileName = AppContext.output_dir + File.separator + datasetName  + "_"+ timestamp + "_r" + repeat_id;
//			
//			File fileObj = new File(fileName);
//			
//			fileObj.getParentFile().mkdirs();
//			fileObj.createNewFile();
//		}
		
		String timestamp = AppContext.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppContext.FILENAME_TIMESTAMP_FORMAT));		

//		fileName = AppContext.output_dir + File.separator + AppContext.current_experiment_id + "_" //+ timestamp File.separator + AppContext.current_experiment_id 
//				+ forest_id + "_"  + repetition_id + "_" + AppContext.getVersionString(true)
//				+ File.separator + "csv" + File.separator + fileName + "_r" + repeat_id 
//				+ "_eid" + AppContext.current_experiment_id + "_rid" + repetition_id + ".trees.csv";
		
		fileName = AppContext.output_dir + File.separator + AppContext.application_start_timestamp_formatted 
				+ File.separator + timestamp + "_" + datasetName 
				+ File.separator + fileName + "_r" + repeat_id 
				+ "_eid" + AppContext.current_experiment_id + "_rid" + repetition_id + "_v"+ AppContext.getVersionString(true)
				+ ".trees.csv";		
		
		System.out.println("writing trees.csv file (result of each tree): " + fileName);

		
		File fileObj = new File(fileName);
		fileObj.getParentFile().mkdirs();
		fileObj.createNewFile();
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){		
			char sep = ',';
//			if (repeat_id == 0) {
				//write header line
				bw.write("dataset,train_size,test_size,length,unique_classes,repeat,exp_id,rep_id,version,host_name,"+
						"tree,accuracy,train_time,test_time,train_time_formatted,test_time_formatted," + 
						"s,s_ed,s_boss,s_tsf,s_rise,s_randf,s_rotf,s_st,boss_trasformations,correct,train_classes,test_classes,node_count,leaf_count,depth,weighted_depth,"+
						"ee_count,randf_count,rotf_count,st_count,boss_count,tsf_count,rif_count,ee_win,randf_win,rotf_win,st_win,boss_win,tsf_win,rif_win,"+
						"rif_acf_count,rif_pacf_count,rif_arma_count,rif_ps_count,rif_dft_count,rif_acf_win,rif_pacf_win,rif_arma_win,rif_ps_win,rif_dft_win,"+
						"euc_count,dtw_count,dtwr_count,ddtw_count,ddtwr_count,wdtw_count,wddtw_count,lcss_count,twe_count,erp_count,msm_count,"+
						"euc_win,dtw_win,dtwr_win,ddtw_win,ddtwr_win,wdtw_win,wddtw_win,lcss_win,twe_win,erp_win,msm_win,"+
						"euc_time,dtw_time,dtwr_time,ddtw_time,ddtwr_time,wdtw_time,wddtw_time,lcss_time,twe_time,erp_time,msm_time,"+
						"boss_transform_time,ee_time,randf_time,rotf_time,st_time,boss_time,tsf_time,rif_time,rif_acf_time,rif_pacf_time,rif_arma_time,rif_ps_time,rif_dft_time,"+
						"ee_splitter_train_time,boss_splitter_train_time,rise_splitter_train_time,st_splitter_train_time,"+
						"data_fetch_time,boss_data_fetch_time,rise_data_fetch_time,st_data_fetch_time,split_evaluator_train_time,"+
						"\n");				
//			}

			
			StringBuilder row = new StringBuilder();
			
			ProximityTree[] trees = forest.getTrees();
			TreeStatCollector tree_stats;
			
			total_num_trees = trees.length;
			
			int nodes[] = new int[total_num_trees];
			double depths[] = new double[total_num_trees];
			double weighted_depths[] = new double[total_num_trees];
					
			for (int i = 0; i < total_num_trees; i++) {
				row.setLength(0);
				
				tree_stats = trees[i].getTreeStatCollection();
				
				nodes[i] = tree_stats.num_nodes;
				depths[i] = tree_stats.depth;
				weighted_depths[i] = tree_stats.weighted_depth;
				
				//datasetName
				row.append(datasetName);
				row.append(sep);
				
				//train_size
				row.append(AppContext.getTraining_data().size());
				row.append(sep);
				
				//test_size
				row.append(AppContext.getTesting_data().size());
				row.append(sep);
				
				//length
				row.append(AppContext.getTraining_data().length());
				row.append(sep);
				
				//unique_classes
				row.append(AppContext.num_classes); //TODO check that this is unique(#class_train + #class_test)
				row.append(sep);				
				
				//repeat
				row.append(repeat_id);
				row.append(sep);
				
				//exp id
				row.append(AppContext.current_experiment_id);
				row.append(sep);
				
				//rep id
				row.append(repetition_id);
				row.append(sep);
				
				//app version
				row.append(AppContext.version);
				row.append(sep);
				
				//hostname
				row.append(AppContext.host_name);
				row.append(sep);
				
				//tree
				row.append(i);
				row.append(sep);
				
				//accuracy
				row.append(accuracy);
				row.append(sep);
				
				//train_time
				row.append(elapsedTimeTrain);
				row.append(sep);
				
				//test_time
				row.append(elapsedTimeTest);
				row.append(sep);
				
				//train_time_formatted
				String time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTrain/AppContext.NANOSECtoMILISEC), "H:m:s.SSS");
				row.append(time_duration);
				row.append(sep);
				
				//test_time_formatted
				time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTest/AppContext.NANOSECtoMILISEC), "H:m:s.SSS");
				row.append(time_duration);
				row.append(sep);
				
				//total number of splitters
				row.append(AppContext.num_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppContext.ee_splitters_per_node);
				row.append(sep);				
				
				//number of splitters
				row.append(AppContext.boss_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppContext.tsf_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppContext.rif_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppContext.randf_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppContext.rotf_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppContext.st_splitters_per_node);
				row.append(sep);
				
				//boss transformations
				row.append(AppContext.boss_trasformations);
				row.append(sep);
				
				//correct
				row.append(correct);
				row.append(sep);
				
				//train_classes
				row.append(AppContext.getTraining_data().get_class_map().size());
				row.append(sep);				
				
				//test_classes
				row.append(AppContext.getTesting_data().get_class_map().size());
				row.append(sep);
				
				//node_count
				row.append(tree_stats.num_nodes);
				row.append(sep);
				
				//leaf_count
				row.append(tree_stats.num_leaves);
				row.append(sep);
				
				//depth
				row.append(tree_stats.depth);
				row.append(sep);
				
				//weighted depth
				row.append(tree_stats.weighted_depth);
				row.append(sep);				
				
				//SPLITTER SELECTION COUNT //TODO make sure there are no multithreading issues here
				//EE
				row.append(tree_stats.ee_count);
				row.append(sep);
				
				//RandF
				row.append(tree_stats.randf_count);
				row.append(sep);
				
				//RotF
				row.append(tree_stats.rotf_count);
				row.append(sep);
				
				//ST
				row.append(tree_stats.st_count);
				row.append(sep);
				
				//BOSS
				row.append(tree_stats.boss_count);
				row.append(sep);
				
				//ST
				row.append(tree_stats.tsf_count);
				row.append(sep);
				
				//BOSS
				row.append(tree_stats.rif_count);
				row.append(sep);
				
				//EE
				row.append(tree_stats.ee_win);
				row.append(sep);
				
				//RandF
				row.append(tree_stats.randf_win);
				row.append(sep);
				
				//RotF
				row.append(tree_stats.rotf_win);
				row.append(sep);
				
				//ST
				row.append(tree_stats.st_win);
				row.append(sep);
				
				//BOSS
				row.append(tree_stats.boss_win);
				row.append(sep);

				//ST
				row.append(tree_stats.tsf_win);
				row.append(sep);
				
				//BOSS
				row.append(tree_stats.rif_win);
				row.append(sep);
				
				//rif details
				row.append(tree_stats.rif_acf_count);
				row.append(sep);
				row.append(tree_stats.rif_pacf_count);
				row.append(sep);
				row.append(tree_stats.rif_arma_count);
				row.append(sep);
				row.append(tree_stats.rif_ps_count);
				row.append(sep);
				row.append(tree_stats.rif_dft_count);
				row.append(sep);
				row.append(tree_stats.rif_acf_win);
				row.append(sep);
				row.append(tree_stats.rif_pacf_win);
				row.append(sep);
				row.append(tree_stats.rif_arma_win);
				row.append(sep);
				row.append(tree_stats.rif_ps_win);
				row.append(sep);
				row.append(tree_stats.rif_dft_win);
				row.append(sep);
				
				//dm counts
				row.append(tree_stats.euc_count);
				row.append(sep);
				row.append(tree_stats.dtw_count);
				row.append(sep);
				row.append(tree_stats.dtwr_count);
				row.append(sep);
				row.append(tree_stats.ddtw_count);
				row.append(sep);
				row.append(tree_stats.ddtwr_count);
				row.append(sep);
				row.append(tree_stats.wdtw_count);
				row.append(sep);
				row.append(tree_stats.wddtw_count);
				row.append(sep);
				row.append(tree_stats.lcss_count);
				row.append(sep);
				row.append(tree_stats.twe_count);
				row.append(sep);
				row.append(tree_stats.erp_count);
				row.append(sep);
				row.append(tree_stats.msm_count);
				row.append(sep);
				
				//dm wins
				row.append(tree_stats.euc_win);
				row.append(sep);
				row.append(tree_stats.dtw_win);
				row.append(sep);
				row.append(tree_stats.dtwr_win);
				row.append(sep);
				row.append(tree_stats.ddtw_win);
				row.append(sep);
				row.append(tree_stats.ddtwr_win);
				row.append(sep);
				row.append(tree_stats.wdtw_win);
				row.append(sep);
				row.append(tree_stats.wddtw_win);
				row.append(sep);
				row.append(tree_stats.lcss_win);
				row.append(sep);
				row.append(tree_stats.twe_win);
				row.append(sep);
				row.append(tree_stats.erp_win);
				row.append(sep);
				row.append(tree_stats.msm_win);
				row.append(sep);
				
				//dm time
				row.append(tree_stats.euc_time);
				row.append(sep);
				row.append(tree_stats.dtw_time);
				row.append(sep);
				row.append(tree_stats.dtwr_time);
				row.append(sep);
				row.append(tree_stats.ddtw_time);
				row.append(sep);
				row.append(tree_stats.ddtwr_time);
				row.append(sep);
				row.append(tree_stats.wdtw_time);
				row.append(sep);
				row.append(tree_stats.wddtw_time);
				row.append(sep);
				row.append(tree_stats.lcss_time);
				row.append(sep);
				row.append(tree_stats.twe_time);
				row.append(sep);
				row.append(tree_stats.erp_time);
				row.append(sep);
				row.append(tree_stats.msm_time);
				row.append(sep);

				
				//splitters
				
				row.append(this.boss_transform_time);
				row.append(sep);
				
				
				//splitter timings
				row.append(tree_stats.ee_time);
				row.append(sep);
				row.append(tree_stats.randf_time);
				row.append(sep);
				row.append(tree_stats.rotf_time);
				row.append(sep);
				row.append(tree_stats.st_time);
				row.append(sep);
				row.append(tree_stats.boss_time);
				row.append(sep);
				row.append(tree_stats.tsf_time);
				row.append(sep);
				row.append(tree_stats.rif_time);
				row.append(sep);
				row.append(tree_stats.rif_acf_time);
				row.append(sep);
				row.append(tree_stats.rif_pacf_time);
				row.append(sep);
				row.append(tree_stats.rif_arma_time);
				row.append(sep);
				row.append(tree_stats.rif_ps_time);
				row.append(sep);			
				row.append(tree_stats.rif_dft_time);
				row.append(sep);
				
				//splitter train function times
				row.append(tree_stats.ee_splitter_train_time);
				row.append(sep);
				row.append(tree_stats.boss_splitter_train_time);
				row.append(sep);
				row.append(tree_stats.rise_splitter_train_time);
				row.append(sep);
				row.append(tree_stats.st_splitter_train_time);
				row.append(sep);

				row.append(tree_stats.data_fetch_time);
				row.append(sep);
				row.append(tree_stats.boss_data_fetch_time);
				row.append(sep);
				row.append(tree_stats.rise_data_fetch_time);
				row.append(sep);
				row.append(tree_stats.st_data_fetch_time);
				row.append(sep);
				
				row.append(tree_stats.split_evaluator_train_time);
				row.append(sep);
				
				
				//end of row
				row.append("\n");
				bw.write(row.toString());
				
			}

			
			
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
//			bw.close();
		}
		
		
		return fileName;
	}

	public String exportSummaryCSV(String datasetName, int repeat_id, String fileName) throws Exception {
				
		String timestamp = AppContext.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppContext.FILENAME_TIMESTAMP_FORMAT));		
		
		fileName = AppContext.output_dir + File.separator + AppContext.application_start_timestamp_formatted 
				+ File.separator + timestamp + "_" + datasetName
				+ File.separator + fileName + "_r" + repeat_id 
				+ "_eid" + AppContext.current_experiment_id + "_rid" + repetition_id + "_v"+ AppContext.getVersionString(true)
				+ ".forest.csv";	
		
		System.out.println("writing forest.csv file (summary of forest): " + fileName);

		
		File fileObj = new File(fileName);
		fileObj.getParentFile().mkdirs();
		fileObj.createNewFile();
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){		
			char sep = ',';
//			if (repeat_id == 0) {
				//write header line
				bw.write("dataset,train_size,test_size,length,unique_classes,repeat_no,experiment_id,repeat_id,version,host_name,"+
						"trees,accuracy,train_time(ns),test_time(ns),train_time_formatted(H:m:s.ms),test_time_formatted(H:m:s.ms)," + 
						"total_candidates,C_ed,C_boss,C_tsf,C_rise,C_randf,C_rotf,C_st,boss_trasformations,"+
						"correct_predictions,train_classes,test_classes,mean_node_count,mean_leaf_count,mean_depth,"+
						"\n");				
//			}

			
			StringBuilder row = new StringBuilder();
			
			ProximityTree[] trees = forest.getTrees();
			TreeStatCollector tree_stats;
			
			total_num_trees = trees.length;
			
			int nodes[] = new int[total_num_trees];
			int leaves[] = new int[total_num_trees];
			double depths[] = new double[total_num_trees];
			double weighted_depths[] = new double[total_num_trees];
			
			double mean_nodes;
			double mean_leaves;
			double mean_depth;
			double mean_weighted_depth;

					
			for (int i = 0; i < total_num_trees; i++) {
				tree_stats = trees[i].getTreeStatCollection();
				
				nodes[i] = tree_stats.num_nodes;
				leaves[i] = tree_stats.num_leaves;
				depths[i] = tree_stats.depth;
				weighted_depths[i] = tree_stats.weighted_depth;
			}
			
			mean_nodes = Statistics.mean(nodes);
			mean_leaves = Statistics.mean(leaves);
			mean_depth = Statistics.mean(depths);
			mean_weighted_depth = Statistics.mean(weighted_depths);
			
			
			row.setLength(0);
			
			//datasetName
			row.append(datasetName);
			row.append(sep);
			
			//train_size
			row.append(AppContext.getTraining_data().size());
			row.append(sep);
			
			//test_size
			row.append(AppContext.getTesting_data().size());
			row.append(sep);
			
			//length
			row.append(AppContext.getTraining_data().length());
			row.append(sep);
			
			//unique_classes
			row.append(AppContext.num_classes); //TODO check that this is unique(#class_train + #class_test)
			row.append(sep);				
			
			//repeat
			row.append(repeat_id);
			row.append(sep);
			
			//exp id
			row.append(AppContext.current_experiment_id);
			row.append(sep);
			
			//rep id
			row.append(repetition_id);
			row.append(sep);
			
			//app version
			row.append(AppContext.version);
			row.append(sep);
			
			//hostname
			row.append(AppContext.host_name);
			row.append(sep);
			
			//tree
			row.append(AppContext.num_trees);
			row.append(sep);
			
			//accuracy
			row.append(accuracy);
			row.append(sep);
			
			//train_time
			row.append(elapsedTimeTrain);
			row.append(sep);
			
			//test_time
			row.append(elapsedTimeTest);
			row.append(sep);
			
			//train_time_formatted
			String time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTrain/AppContext.NANOSECtoMILISEC), "H:m:s.SSS");
			row.append(time_duration);
			row.append(sep);
			
			//test_time_formatted
			time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTest/AppContext.NANOSECtoMILISEC), "H:m:s.SSS");
			row.append(time_duration);
			row.append(sep);
			
			//total number of splitters
			row.append(AppContext.num_splitters_per_node);
			row.append(sep);
			
			//number of splitters
			row.append(AppContext.ee_splitters_per_node);
			row.append(sep);				
			
			//number of splitters
			row.append(AppContext.boss_splitters_per_node);
			row.append(sep);
			
			//number of splitters
			row.append(AppContext.tsf_splitters_per_node);
			row.append(sep);
			
			//number of splitters
			row.append(AppContext.rif_splitters_per_node);
			row.append(sep);
			
			//number of splitters
			row.append(AppContext.randf_splitters_per_node);
			row.append(sep);
			
			//number of splitters
			row.append(AppContext.rotf_splitters_per_node);
			row.append(sep);
			
			//number of splitters
			row.append(AppContext.st_splitters_per_node);
			row.append(sep);
			
			//boss transformations
			row.append(AppContext.boss_trasformations);
			row.append(sep);
			
			//correct
			row.append(correct);
			row.append(sep);
			
			//train_classes
			row.append(AppContext.getTraining_data().get_class_map().size());
			row.append(sep);				
			
			//test_classes
			row.append(AppContext.getTesting_data().get_class_map().size());
			row.append(sep);
			
			//NEED TO FIND MEAN 
			//node_count
			row.append(mean_nodes);
			row.append(sep);
			
			//leaf_count
			row.append(mean_leaves);
			row.append(sep);
			
			//depth
			row.append(mean_depth);
			row.append(sep);
						
							
			//end of row
			row.append("\n");
			bw.write(row.toString());

			
			
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
//			bw.close();
		}
		
		
		return fileName;		
	}
	
}




















