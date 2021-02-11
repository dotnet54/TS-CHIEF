package data.containers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import core.ClassifierResult;
import data.timeseries.Dataset;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import application.AppConfig;
import trees.TSCheifForest;
import trees.TSCheifTree;
import trees.TSChiefTreeResult;
import util.Statistics;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ExperimentResultCollector extends ClassifierResult {
	
	private transient TSCheifForest forest;
	public boolean results_collated = false;
	public final String escape_char = "\"";
	public final String replace_search_char = ",";
	public final String replacement_char = ";";
	
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
	

	public int total_num_trees;

	//splitters
	public long boss_transform_time;
	protected long st_transform_time;
	protected long it_transform_time;
	protected long rt_transform_time;
	
	public long mem_before_boss_transform;//TODO
	public long after_before_boss_transform;//TODO
	
	public ForestStatCollector fcollector;
	
	//dev TODO DEV
	public List<String> allShapelets;
	public List<String> winShapelets;
	
	public ExperimentResultCollector(TSCheifForest forest) {
		this.forest = forest;
		this.forest_id = forest.getForestID();
		this.datasetName = AppConfig.getDatasetName();
		
		this.allShapelets = Collections.synchronizedList(new ArrayList<String>());
		this.winShapelets = Collections.synchronizedList(new ArrayList<String>());
	}
	
	public void collateResults() {
		
		if (results_collated) {
			return;
		}		
		fcollector = new ForestStatCollector(forest);
		fcollector.aggregateResults();
		
		train_time_formatted = DurationFormatUtils.formatDuration((long) (elapsedTimeTrain/AppConfig.NANOSECtoMILISEC), "H:m:s.SSS");
		test_time_formatted = DurationFormatUtils.formatDuration((long) (elapsedTimeTest/AppConfig.NANOSECtoMILISEC), "H:m:s.SSS");

		
		results_collated = true;
	}
	
	public void printResults(String datasetName, int experiment_id, String prefix) {
		this.datasetName = datasetName;
//		System.out.println(prefix+ "-----------------Experiment No: " 
//				+ experiment_id + " (" +datasetName+ "), Forest No: " 
//				+ (this.forest_id) +"  -----------------");
		
		if (AppConfig.verbosity > 0) {
			String time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTrain/AppConfig.NANOSECtoMILISEC), "H:m:s.SSS");
	        System.out.format("%sTraining Time: %fms (%s)\n",prefix, elapsedTimeTrain/AppConfig.NANOSECtoMILISEC, time_duration);
			time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTest/AppConfig.NANOSECtoMILISEC), "H:m:s.SSS");		
	        System.out.format("%sPrediction Time: %fms (%s)\n",prefix, elapsedTimeTest/AppConfig.NANOSECtoMILISEC, time_duration);
	
	        
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
		System.out.print(", " + elapsedTimeTrain /AppConfig.NANOSECtoMILISEC + " ms") ;
		System.out.print(", " + elapsedTimeTest /AppConfig.NANOSECtoMILISEC + " ms");
		System.out.print(", " + Statistics.mean(fcollector.depths));
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
//		File pf = fileObj.getParentFile();
//		if (pf != null) {
//			fileObj.getParentFile().mkdirs();
//		}
//			fileObj.createNewFile();
//		}
		
		String timestamp = AppConfig.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT));	
		
//		fileName = AppContext.output_dir + File.separator + AppContext.current_experiment_id + "_" //+ timestamp File.separator + AppContext.current_experiment_id 
//				+ forest_id + "_" 
//				+ repetition_id + "_" + AppContext.getVersionString(true) + File.separator + "json" + File.separator + fileName
//				+ "_eid" + AppContext.current_experiment_id +".json";
		
		if (AppConfig.short_file_names) {
			fileName = AppConfig.output_dir + File.separator 
					+ AppConfig.export_file_prefix
					+ AppConfig.experiment_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT_SHORT))
					+ "-" + forest_id + "-"+ AppConfig.getDatasetName()
					+ ".json";
		}else {
			fileName = AppConfig.output_dir + File.separator + AppConfig.export_file_prefix + AppConfig.application_start_timestamp_formatted 
					+ File.separator + timestamp + "_" + datasetName
					+ File.separator + fileName + "_r" + repeat_id 
					+ "_eid" + AppConfig.current_experiment_id + "_rid" + repetition_id + "_v"+ AppConfig.getVersionString(true)
					+ ".json";			
		}

		
		System.out.println("writing json file: " + fileName);

		File fileObj = new File(fileName);
		File pf = fileObj.getParentFile();
		if (pf != null) {
			fileObj.getParentFile().mkdirs();
		}
		fileObj.createNewFile();		
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){		
		
			Gson gson;
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.serializeSpecialFloatingPointValues();
			gsonBuilder.serializeNulls();
			gsonBuilder.registerTypeAdapter(Class.class, new AppConfig.ClassTypeAdapter());
			gson = gsonBuilder.create();
			
//			SerializableResultSet object = new SerializableResultSet(this.forests);
			
			if (repeat_id == 0) {
				
				StringBuilder buffer = new StringBuilder();
				buffer.append('{');
				
				//write appcontext
				buffer.append("\"settings\": ");
				buffer.append(gson.toJson(AppConfig.class));
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
		
		String timestamp = AppConfig.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT));		

//		fileName = AppContext.output_dir + File.separator + AppContext.current_experiment_id + "_" //+ timestamp File.separator + AppContext.current_experiment_id 
//				+ forest_id + "_"  + repetition_id + "_" + AppContext.getVersionString(true)
//				+ File.separator + "csv" + File.separator + fileName + "_r" + repeat_id 
//				+ "_eid" + AppContext.current_experiment_id + "_rid" + repetition_id + ".trees.csv";
		
		if (AppConfig.short_file_names) {
			fileName = AppConfig.output_dir + File.separator
					+ AppConfig.export_file_prefix 
					+ AppConfig.experiment_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT_SHORT))
					+ "-" + forest_id + "-"+ AppConfig.getDatasetName()
					+ ".trees.csv";
		}else {
			fileName = AppConfig.output_dir + File.separator + AppConfig.export_file_prefix + AppConfig.application_start_timestamp_formatted 
					+ File.separator + timestamp + "_" + datasetName 
					+ File.separator + fileName + "_r" + repeat_id 
					+ "_eid" + AppConfig.current_experiment_id + "_rid" + repetition_id + "_v"+ AppConfig.getVersionString(true)
					+ ".trees.csv";				
		}
		
		System.out.println("writing trees.csv file (result of each tree): " + fileName);

		
		File fileObj = new File(fileName);
		File pf = fileObj.getParentFile();
		if (pf != null) {
			fileObj.getParentFile().mkdirs();
		}
		fileObj.createNewFile();
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){		
			char sep = ',';
//			if (repeat_id == 0) {
				//write header line
				bw.write("timestamp,dataset,train_size,test_size,length,unique_classes,repeat,exp_id,rep_id,version,host_name,threads,"+
						"tree,accuracy,train_time,test_time,train_time_formatted,test_time_formatted,rand_seed," + 
						"C,C_ee,C_boss,C_tsf,C_rise,C_randf,C_rotf,C_st, C_it, C_rt,"+
						"boss_trasformations,rt_kernels,correct,train_classes,test_classes,total_nodes,leaf_nodes,internal_nodes,depth,"+
						"ee_count,randf_count,rotf_count,st_count,boss_count,tsf_count,rif_count,it_count,rt_count,"+
						"ee_win,randf_win,rotf_win,st_win,boss_win,tsf_win,rif_win,it_win,rt_win,"+
						"rif_acf_count,rif_pacf_count,rif_arma_count,rif_ps_count,rif_dft_count,"+
						"rif_acf_win,rif_pacf_win,rif_arma_win,rif_ps_win,rif_dft_win,"+
						"euc_count,dtw_count,dtwr_count,ddtw_count,ddtwr_count,wdtw_count,wddtw_count,lcss_count,twe_count,erp_count,msm_count,"+
						"euc_win,dtw_win,dtwr_win,ddtw_win,ddtwr_win,wdtw_win,wddtw_win,lcss_win,twe_win,erp_win,msm_win,"+
						"euc_time,dtw_time,dtwr_time,ddtw_time,ddtwr_time,wdtw_time,wddtw_time,lcss_time,twe_time,erp_time,msm_time,"+
						"boss_transform_time,st_transform_time,it_transform_time,rt_transform_time,"+
						"ee_time,randf_time,rotf_time,st_time,boss_time,tsf_time,rif_time,it_time,rt_time,"+
						"rif_acf_time,rif_pacf_time,rif_arma_time,rif_ps_time,rif_dft_time,"+
						"ee_splitter_train_time,boss_splitter_train_time,rise_splitter_train_time,st_splitter_train_time,it_splitter_train_time,rt_splitter_train_time,"+
						"data_fetch_time,boss_data_fetch_time,rise_data_fetch_time,st_data_fetch_time,it_data_fetch_time,rt_data_fetch_time,split_evaluator_train_time,"+
						"args\n");				
//			}

			
			StringBuilder row = new StringBuilder();
			
			TSCheifTree[] trees = forest.getTrees();
			TreeStatCollector tree_stats;
			
			total_num_trees = trees.length;
					
			for (int i = 0; i < total_num_trees; i++) {
				row.setLength(0);
				
				tree_stats = trees[i].getTreeStatCollection();
				
				//timestamp
				row.append(AppConfig.experiment_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT)));
				row.append(sep);
				
				//datasetName
				row.append(datasetName);
				row.append(sep);
				
				//train_size
				row.append(AppConfig.getTrainingSet().size());
				row.append(sep);
				
				//test_size
				row.append(AppConfig.getTestingSet().size());
				row.append(sep);
				
				//length
				row.append(AppConfig.getTrainingSet().length());
				row.append(sep);
				
				//unique_classes
				row.append(AppConfig.num_classes); //TODO check that this is unique(#class_train + #class_test)
				row.append(sep);				
				
				//repeat
				row.append(repeat_id);
				row.append(sep);
				
				//exp id
				row.append(AppConfig.current_experiment_id);
				row.append(sep);
				
				//rep id
				row.append(repetition_id);
				row.append(sep);
				
				//app version
				row.append(AppConfig.version);
				row.append(sep);
				
				//hostname
				row.append(AppConfig.host_name);
				row.append(sep);
				
				//-threads flag  -- may be different from available cpus
				row.append(AppConfig.num_threads);
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
				String time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTrain/AppConfig.NANOSECtoMILISEC), "H:m:s.SSS");
				row.append(time_duration);
				row.append(sep);
				
				//test_time_formatted
				time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTest/AppConfig.NANOSECtoMILISEC), "H:m:s.SSS");
				row.append(time_duration);
				row.append(sep);
				
				//random seed
				row.append(AppConfig.rand_seed);
				row.append(sep);
				
				//total number of splitters
				row.append(AppConfig.num_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppConfig.ee_splitters_per_node);
				row.append(sep);				
				
				//number of splitters
				row.append(AppConfig.boss_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppConfig.tsf_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppConfig.rif_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppConfig.randf_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppConfig.rotf_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppConfig.st_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppConfig.it_splitters_per_node);
				row.append(sep);
				
				//number of splitters
				row.append(AppConfig.rt_splitters_per_node);
				row.append(sep);
				
				//boss transformations
				row.append(AppConfig.boss_trasformations);
				row.append(sep);
				
				//rt kernels
				row.append(AppConfig.rt_kernels);
				row.append(sep);
				
				//correct
				row.append(correct);
				row.append(sep);
				
				//train_classes
				row.append(AppConfig.getTrainingSet().getClassDistribution().size());
				row.append(sep);				
				
				//test_classes
				row.append(AppConfig.getTestingSet().getClassDistribution().size());
				row.append(sep);
				
				//total_nodes
				row.append(tree_stats.num_nodes);
				row.append(sep);
				
				//leaf_count
				row.append(tree_stats.num_leaves);
				row.append(sep);
				
				//internal_nodes
				row.append(tree_stats.num_nodes - tree_stats.num_leaves);
				row.append(sep);				
				
				//depth
				row.append(tree_stats.depth);
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
				
				//RISE
				row.append(tree_stats.rif_count);
				row.append(sep);
				
				//InceptionTime
				row.append(tree_stats.it_count);
				row.append(sep);
				
				//Rocket Tree
				row.append(tree_stats.rt_count);
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
				
				//InceptionTime
				row.append(tree_stats.it_win);
				row.append(sep);
				
				//Rocket Tree
				row.append(tree_stats.rt_win);
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
				row.append(this.st_transform_time);
				row.append(sep);
				row.append(this.it_transform_time);
				row.append(sep);
				row.append(this.rt_transform_time);
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
				row.append(tree_stats.it_time);
				row.append(sep);
				row.append(tree_stats.rt_time);
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
				row.append(tree_stats.it_splitter_train_time);
				row.append(sep);
				row.append(tree_stats.rt_splitter_train_time);
				row.append(sep);
				
				row.append(tree_stats.data_fetch_time);
				row.append(sep);
				row.append(tree_stats.boss_data_fetch_time);
				row.append(sep);
				row.append(tree_stats.rise_data_fetch_time);
				row.append(sep);
				row.append(tree_stats.st_data_fetch_time);
				row.append(sep);
				row.append(tree_stats.it_data_fetch_time);
				row.append(sep);
				row.append(tree_stats.rt_data_fetch_time);
				row.append(sep);
				
				row.append(tree_stats.split_evaluator_train_time);
				row.append(sep);
				
				row.append(escape_char+ String.join(" ", AppConfig.cmd_args).replace(replace_search_char, replacement_char) + escape_char);
				
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
				
		String timestamp = AppConfig.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT));		
		
		
		if (AppConfig.short_file_names) {
			fileName = AppConfig.output_dir + File.separator 
					+ AppConfig.export_file_prefix 
					+ AppConfig.experiment_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT_SHORT))
					+ "-" + forest_id + "-"+ AppConfig.getDatasetName()
					+ ".forest.csv";
		}else {
			fileName = AppConfig.output_dir + File.separator + AppConfig.export_file_prefix + AppConfig.application_start_timestamp_formatted 
					+ File.separator + timestamp + "_" + datasetName
					+ File.separator + fileName + "_r" + repeat_id 
					+ "_eid" + AppConfig.current_experiment_id + "_rid" + repetition_id + "_v"+ AppConfig.getVersionString(true)
					+ ".forest.csv";			
		}

		System.out.println("writing forest.csv file (summary of forest): " + fileName);

		
		File fileObj = new File(fileName);
		File pf = fileObj.getParentFile();
		if (pf != null) {
			fileObj.getParentFile().mkdirs();
		}
		fileObj.createNewFile();
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){		
//			if (repeat_id == 0) {
				//write header line
				bw.write("timestamp,dataset,train_size,test_size,length,unique_classes,repeat_no,experiment_id,repeat_id,version,host_name,threads,"+
//						"requested_threads,available_cpus,"+
						"trees,accuracy,train_time(ns),test_time(ns),train_time_formatted(H:m:s.ms),test_time_formatted(H:m:s.ms),rand_seed," + 
						"candidates_per_node(C),C_ee,C_boss,C_tsf,C_rise,C_randf,C_rotf,C_st, C_it, C_rt,"+
						"boss_trasformations,rt_kernels,"+
						"correct_predictions,train_classes,test_classes,mean_node_count,mean_leaf_count,mean_depth,"+
						"ee_count,randf_count,rotf_count,st_count,boss_count,tsf_count,rif_count,"+
						"ee_win,randf_win,rotf_win,st_win,boss_win,tsf_win,rif_win,"+
						"rif_acf_count,rif_pacf_count,rif_arma_count,rif_ps_count,rif_dft_count,"+
						"rif_acf_win,rif_pacf_win,rif_arma_win,rif_ps_win,rif_dft_win,"+
						"euc_count,dtw_count,dtwr_count,ddtw_count,ddtwr_count,wdtw_count,wddtw_count,lcss_count,twe_count,erp_count,msm_count,"+
						"euc_win,dtw_win,dtwr_win,ddtw_win,ddtwr_win,wdtw_win,wddtw_win,lcss_win,twe_win,erp_win,msm_win,"+
						"euc_time,dtw_time,dtwr_time,ddtw_time,ddtwr_time,wdtw_time,wddtw_time,lcss_time,twe_time,erp_time,msm_time,"+
						"boss_transform_time,st_transform_time,it_transform_time,rt_transform_time,"+
						"ee_time,randf_time,rotf_time,st_time,boss_time,tsf_time,rif_time,it_time,rt_time,"+
						"rif_acf_time,rif_pacf_time,rif_arma_time,rif_ps_time,rif_dft_time,"+
						"ee_splitter_train_time,boss_splitter_train_time,rise_splitter_train_time,st_splitter_train_time,it_splitter_train_time,rt_splitter_train_time,"+
						"data_fetch_time,boss_data_fetch_time,rise_data_fetch_time,st_data_fetch_time,it_data_fetch_time,rt_data_fetch_time,split_evaluator_train_time,"+
						"args\n");			
				
//			}

			
			StringBuilder row = makeCSVSummaryLine(repeat_id);
			

			StringBuilder sb = fcollector.getCSVRow();
			
			row.append(sb);
			
			row.append(escape_char+ String.join(" ", AppConfig.cmd_args).replace(replace_search_char, replacement_char) + escape_char);
			
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
	
	private StringBuilder makeCSVSummaryLine(int repeat_id) {
		final char sep = ',';

		StringBuilder row = new StringBuilder();
		
		TSCheifTree[] trees = forest.getTrees();
		TreeStatCollector tree_stats;
		
		total_num_trees = trees.length;

		row.setLength(0);
		
		//timestamp
		row.append(AppConfig.experiment_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT)));
		row.append(sep);
		
		//datasetName
		row.append(datasetName);
		row.append(sep);
		
		//train_size
		row.append(AppConfig.getTrainingSet().size());
		row.append(sep);
		
		//test_size
		row.append(AppConfig.getTestingSet().size());
		row.append(sep);
		
		//length
		row.append(AppConfig.getTrainingSet().length());
		row.append(sep);
		
		//unique_classes
		row.append(AppConfig.num_classes); //TODO check that this is unique(#class_train + #class_test)
		row.append(sep);				
		
		//repeat
		row.append(repeat_id);
		row.append(sep);
		
		//exp id
		row.append(AppConfig.current_experiment_id);
		row.append(sep);
		
		//rep id
		row.append(repetition_id);
		row.append(sep);
		
		//app version
		row.append(AppConfig.version);
		row.append(sep);
		
		//hostname
		row.append(AppConfig.host_name);
		row.append(sep);
		
		//-threads flag  -- may be different from available cpus
		row.append(AppConfig.num_threads);
		row.append(sep);
		
		//tree
		row.append(AppConfig.num_trees);
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
		String time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTrain/AppConfig.NANOSECtoMILISEC), "H:m:s.SSS");
		row.append(time_duration);
		row.append(sep);
		
		//test_time_formatted
		time_duration = DurationFormatUtils.formatDuration((long) (elapsedTimeTest/AppConfig.NANOSECtoMILISEC), "H:m:s.SSS");
		row.append(time_duration);
		row.append(sep);
		
		//random seed
		row.append(AppConfig.rand_seed);
		row.append(sep);
		
		//total number of splitters
		row.append(AppConfig.num_splitters_per_node);
		row.append(sep);
		
		//number of splitters
		row.append(AppConfig.ee_splitters_per_node);
		row.append(sep);				
		
		//number of splitters
		row.append(AppConfig.boss_splitters_per_node);
		row.append(sep);
		
		//number of splitters
		row.append(AppConfig.tsf_splitters_per_node);
		row.append(sep);
		
		//number of splitters
		row.append(AppConfig.rif_splitters_per_node);
		row.append(sep);
		
		//number of splitters
		row.append(AppConfig.randf_splitters_per_node);
		row.append(sep);
		
		//number of splitters
		row.append(AppConfig.rotf_splitters_per_node);
		row.append(sep);
		
		//number of splitters
		row.append(AppConfig.st_splitters_per_node);
		row.append(sep);
		
		//number of splitters
		row.append(AppConfig.it_splitters_per_node);
		row.append(sep);
		
		//number of splitters
		row.append(AppConfig.rt_splitters_per_node);
		row.append(sep);
		
		//boss transformations
		row.append(AppConfig.boss_trasformations);
		row.append(sep);
		
		//rt kernels
		row.append(AppConfig.rt_kernels);
		row.append(sep);
		
		//correct
		row.append(correct);
		row.append(sep);
		
		//train_classes
		row.append(AppConfig.getTrainingSet().getClassDistribution().size());
		row.append(sep);				
		
		//test_classes
		row.append(AppConfig.getTestingSet().getClassDistribution().size());
		row.append(sep);

		return row;
	}
	
	public void exportSimpleSummaryCSV(String datasetName, int i) throws IOException {
		
		String timestamp = AppConfig.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppConfig.TIMESTAMP_FORMAT_LONG));		
		
		System.out.println("writing results file (summary of results_file): " + AppConfig.results_file);

		File fileObj = new File(AppConfig.results_file);
		File pf = fileObj.getParentFile();
		if (pf != null) {
			fileObj.getParentFile().mkdirs();
		}
		fileObj.createNewFile();
		
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(AppConfig.results_file, true))){		
			char sep = ',';
			
			//check if header is absent add a header line
			
			if (fileObj.length() == 0) {
				//write header line
				bw.write("timestamp,dataset,train_size,test_size,length,unique_classes,repeat_no,experiment_id,repeat_id,version,host_name,threads,"+
						"trees,accuracy,train_time(ns),test_time(ns),train_time_formatted(H:m:s.ms),test_time_formatted(H:m:s.ms),rand_seed," + 
						"total_candidates,C_ed,C_boss,C_tsf,C_rise,C_randf,C_rotf,C_st,boss_trasformations,"+
						"correct_predictions,train_classes,test_classes,mean_node_count,mean_leaf_count,mean_depth,"+
						"\n");				
			}

			
			StringBuilder row = makeCSVSummaryLine(i);
			
			//prepend additional data
//			row.insert(0, timestamp + ",");
			
			//end of row
			row.append("\n");
			bw.write(row.toString());

		
		
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
	//		bw.close();
		}
	}
	
	
	public void exportPredictions(String filePrefix, TSChiefTreeResult[] treeResults , List<Integer> predcicted_labels, Dataset test_data) throws IOException {
		
		String timestamp = AppConfig.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT));
		
//		String fileName = AppContext.output_dir + File.separator + AppContext.current_experiment_id + "_"//+ timestamp + "_" //+ File.separator + AppContext.current_experiment_id 
//				+ forest_id + "_" + result.repetition_id + "_" + AppContext.getVersionString(true) + File.separator + "pred" + File.separator + filePrefix + ".pred.csv";
		
		String fileName;
		if (AppConfig.short_file_names) {
			fileName = AppConfig.output_dir + File.separator 
					+ AppConfig.export_file_prefix 
					+ AppConfig.experiment_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT_SHORT))
					+ "-" + forest_id + "-"+ AppConfig.getDatasetName()
					+ ".pred.csv";
		}else {
			fileName = AppConfig.output_dir + File.separator + AppConfig.export_file_prefix + AppConfig.application_start_timestamp_formatted 
					+ File.separator + timestamp + "_" + AppConfig.getDatasetName()
					+ File.separator + AppConfig.currentOutputFilePrefix + "_r" + forest_id 
					+ "_eid" + AppConfig.current_experiment_id + "_rid" + repetition_id + "_v"+ AppConfig.getVersionString(true)
					+ ".pred.csv";			
		}

		
		int size = test_data.size();
		
		if (AppConfig.verbosity > 1) {
			System.out.println("\nwriting pred.csv file (detailed predictions): " + fileName);
		}
		
		
		File fileObj = new File(fileName);
		fileObj.getParentFile().mkdirs();
		fileObj.createNewFile();
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){		
		
		
			StringBuilder row = new StringBuilder();
			
			row.append("id");
			row.append(",");
			
			row.append("actual_label");
			row.append(",");
			
			row.append("predicted_label");
			row.append(",");
			
			row.append("correct");
			row.append(",");
			
			//predictions for each tree
			for (int k = 0; k < treeResults.length; k++) {
				row.append("tree_" + k);
				row.append(",");
			}
			
			row.append("\n");
			bw.write(row.toString());
			row.setLength(0);
			
			for (int i = 0; i < size; i++) {
				row.append(i);
				row.append(",");
				
				row.append(test_data.getClass(i));
				row.append(",");
				
				row.append(predcicted_labels.get(i));
				row.append(",");
				
				if (predcicted_labels.get(i).equals(test_data.getSeries(i).label())) { //TODO == vs equal
					row.append(1);
				}else {
					row.append(0);
				}
				row.append(",");
				
				for (int k = 0; k < treeResults.length; k++) {
					row.append(treeResults[k].getPredctictedLabels()[i]);	//for kth tree, ith instance
					row.append(",");
				}
				row.append("\n");
				bw.write(row.toString());	
				row.setLength(0);
			}
		

			
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
	//		bw.close();
		}
		
	}
	
	
	public String generateOutPutFileName(String filePrefix, String extension, int repeat_id) {
		String timestamp = AppConfig.experiment_timestamp
			       .format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT));	
		
		String fileName = null;
		
		if (AppConfig.short_file_names) {
			fileName = AppConfig.output_dir + File.separator 
					+ AppConfig.export_file_prefix
					+ AppConfig.experiment_timestamp.format(DateTimeFormatter.ofPattern(AppConfig.FILENAME_TIMESTAMP_FORMAT_SHORT))
					+ "-" + forest_id + "-"+ AppConfig.getDatasetName()
					+ extension;
		}else {
			fileName = AppConfig.output_dir + File.separator + AppConfig.export_file_prefix + AppConfig.application_start_timestamp_formatted 
					+ File.separator + timestamp + "_" + datasetName
					+ File.separator + filePrefix + "_r" + repeat_id 
					+ "_eid" + AppConfig.current_experiment_id + "_rid" + repetition_id + "_v"+ AppConfig.getVersionString(true)
					+ extension;			
		}
		return fileName;
	}
	
	public void exportShapelets(String datasetName, int repeat_id, String filePrefix) throws IOException {
		
		String fileName = this.generateOutPutFileName(filePrefix, ".st.csv", repeat_id);
		
		System.out.println("writing shapelets file : " + fileName);

		File fileObj = new File(fileName);
		File pf = fileObj.getParentFile();
		if (pf != null) {
			fileObj.getParentFile().mkdirs();
		}
		fileObj.createNewFile();
		
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))){		
			char sep = ',';
			
			//check if header is absent add a header line
			
			bw.write("start,end,length,selected,method\n");
			String currentItem;
			
			int method = 0;
			if (AppConfig.st_interval_method.equals("lengthfirst")) {
				method = 0;
			}else if (AppConfig.st_interval_method.equals("swap")) {
				method = 1;
			}else {
				method = -1;
			}
			
			
			for (int i = 0; i < allShapelets.size(); i++) {
				if (allShapelets.get(i) != null) {
		    		bw.write(allShapelets.get(i));
		    		bw.write(",0,"+method+"\n"); //0 for first list -- includes everything					
				}
			}
			for (int i = 0; i < winShapelets.size(); i++) {
				if (winShapelets.get(i) != null) {
		    		bw.write(winShapelets.get(i));
		    		bw.write(",1,"+method+"\n"); //0 for first list -- includes everything					
				}
			}
			
			
//			//synchronized blocks because arraylist used here is threadsafe
//			synchronized(allShapelets)
//			{
//			    Iterator<String> iterator = allShapelets.iterator();
//			    while (iterator.hasNext())
//			    {
////			        System.out.println(iterator.next());
//			    	currentItem = iterator.next();
//			    	
//			    	if (currentItem != null) { //quick fix
//			    		bw.write(currentItem);
//			    		bw.write(",0\n"); //0 for first list -- includes everything
//			    	}
//
//			    }
//			}
//			
//			synchronized(winShapelets)
//			{
//			    Iterator<String> iterator = winShapelets.iterator();
//			    while (iterator.hasNext())
//			    {
////			        System.out.println(iterator.next());
//			    	currentItem = iterator.next();
//			    	
//			    	if (currentItem != null) { //quick fix
//			    		bw.write(currentItem);
//			    		//1 for selected list so that they both can be saved in the same file. 
//			    		//filter selected shapelets using this flag in pandas
//			    		bw.write(",1\n");	
//			    	}
//			    }
//			}
		
			
			bw.flush();
			bw.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
	//		bw.close();
		}
	}
	
	
}




















