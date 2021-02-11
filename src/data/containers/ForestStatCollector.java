package data.containers;

import trees.TSCheifForest;
import trees.TSCheifTree;
import util.Statistics;

public class ForestStatCollector {

//	,mean_node_count,mean_leaf_count,mean_depth,

//	"ee_count,randf_count,rotf_count,st_count,boss_count,tsf_count,rif_count,ee_win,randf_win,rotf_win,st_win,boss_win,tsf_win,rif_win,"+
//	"rif_acf_count,rif_pacf_count,rif_arma_count,rif_ps_count,rif_dft_count,rif_acf_win,rif_pacf_win,rif_arma_win,rif_ps_win,rif_dft_win,"+
//	"euc_count,dtw_count,dtwr_count,ddtw_count,ddtwr_count,wdtw_count,wddtw_count,lcss_count,twe_count,erp_count,msm_count,"+
//	"euc_win,dtw_win,dtwr_win,ddtw_win,ddtwr_win,wdtw_win,wddtw_win,lcss_win,twe_win,erp_win,msm_win,"+
//	"euc_time,dtw_time,dtwr_time,ddtw_time,ddtwr_time,wdtw_time,wddtw_time,lcss_time,twe_time,erp_time,msm_time,"+
//	"boss_transform_time,ee_time,randf_time,rotf_time,st_time,boss_time,tsf_time,rif_time,rif_acf_time,rif_pacf_time,rif_arma_time,rif_ps_time,rif_dft_time,"+
//	"ee_splitter_train_time,boss_splitter_train_time,rise_splitter_train_time,st_splitter_train_time,"+
//	"data_fetch_time,boss_data_fetch_time,rise_data_fetch_time,st_data_fetch_time,split_evaluator_train_time,"+	
	
	protected int[] nodes;
	protected int[] leaves;
	protected double[] depths; 
	protected double[] weighted_depths;
	
	protected int[] ee_count;
	protected int[] randf_count;
	protected int[] rotf_count;
	protected int[] st_count;
	protected int[] boss_count;
	protected int[] tsf_count;
	protected int[] rif_count;
	protected int[] it_count;
	protected int[] rt_count;
	
	protected int[] ee_win;
	protected int[] randf_win;
	protected int[] rotf_win;
	protected int[] st_win;
	protected int[] boss_win;
	protected int[] tsf_win;
	protected int[] rif_win;
	protected int[] it_win;
	protected int[] rt_win;

	protected int[] rif_acf_count;
	protected int[] rif_pacf_count;
	protected int[] rif_arma_count;
	protected int[] rif_ps_count;
	protected int[] rif_dft_count;
	protected int[] rif_acf_win;
	protected int[] rif_pacf_win;
	protected int[] rif_arma_win;
	protected int[] rif_ps_win;
	protected int[] rif_dft_win;
	
	protected int[] euc_count;
	protected int[] dtw_count;
	protected int[] dtwr_count;
	protected int[] ddtw_count;
	protected int[] ddtwr_count;
	protected int[] wdtw_count;
	protected int[] wddtw_count;
	protected int[] lcss_count;
	protected int[] twe_count;
	protected int[] erp_count;
	protected int[] msm_count;
	
	protected int[] euc_win;
	protected int[] dtw_win;
	protected int[] dtwr_win;
	protected int[] ddtw_win;
	protected int[] ddtwr_win;
	protected int[] wdtw_win;
	protected int[] wddtw_win;
	protected int[] lcss_win;
	protected int[] twe_win;
	protected int[] erp_win;
	protected int[] msm_win;
	
	protected long[] euc_time;
	protected long[] dtw_time;
	protected long[] dtwr_time;
	protected long[] ddtw_time;
	protected long[] ddtwr_time;
	protected long[] wdtw_time;
	protected long[] wddtw_time;
	protected long[] lcss_time;
	protected long[] twe_time;
	protected long[] erp_time;
	protected long[] msm_time;
	
	//if collecting transform time per tree -- N/A for forest level transforms
	protected long[] boss_transform_time;
	protected long[] st_transform_time;
	protected long[] it_transform_time;
	protected long[] rt_transform_time;
	
	protected long[] ee_time;
	protected long[] randf_time;
	protected long[] rotf_time;
	protected long[] st_time;
	protected long[] boss_time;
	protected long[] tsf_time;
	protected long[] rif_time;
	protected long[] it_time;
	protected long[] rt_time;
	
	protected long[] rif_acf_time;
	protected long[] rif_pacf_time;
	protected long[] rif_arma_time;
	protected long[] rif_ps_time;
	protected long[] rif_dft_time;
	
	protected long[] ee_splitter_train_time;
	protected long[] boss_splitter_train_time;
	protected long[] rise_splitter_train_time;
	protected long[] st_splitter_train_time;
	protected long[] randf_splitter_train_time;
	protected long[] it_splitter_train_time;
	protected long[] rt_splitter_train_time;
	
	protected long[] data_fetch_time;
	protected long[] boss_data_fetch_time;
	protected long[] rise_data_fetch_time;
	protected long[] st_data_fetch_time;
	protected long[] it_data_fetch_time;
	protected long[] rt_data_fetch_time;
	protected long[] split_evaluator_train_time;

	protected boolean results_aggregated = false;
	protected int forest_id;
	protected transient ExperimentResultCollector results;	
	protected transient TSCheifTree[] trees;
	protected int k;
	
	public ForestStatCollector(TSCheifForest forest){		
		this.forest_id = forest.getForestID();
		this.results = forest.getResultSet();
		this.trees = forest.getTrees();
		this.k = trees.length;
		
		init();

	}	
	
	private void init() {
		
		nodes = new int[k];
		leaves = new int[k];
		depths = new double[k];
		
		ee_count = new int[k];
		randf_count = new int[k];
		rotf_count = new int[k];
		st_count = new int[k];
		boss_count = new int[k];
		tsf_count = new int[k];
		rif_count = new int[k];
		
		ee_win = new int[k];
		randf_win = new int[k];
		rotf_win = new int[k];
		st_win = new int[k];
		boss_win = new int[k];
		tsf_win = new int[k];
		rif_win = new int[k];
		
		rif_acf_count = new int[k];
		rif_pacf_count = new int[k];
		rif_arma_count = new int[k];
		rif_ps_count = new int[k];
		rif_dft_count = new int[k];
		rif_acf_win = new int[k];
		rif_pacf_win = new int[k];
		rif_arma_win = new int[k];
		rif_ps_win = new int[k];
		rif_dft_win = new int[k];
		
		euc_count = new int[k];
		dtw_count = new int[k];
		dtwr_count = new int[k];
		ddtw_count = new int[k];
		ddtwr_count = new int[k];
		wdtw_count = new int[k];
		wddtw_count = new int[k];
		lcss_count = new int[k];
		twe_count = new int[k];
		erp_count = new int[k];
		msm_count = new int[k];
		
		euc_win = new int[k];
		dtw_win = new int[k];
		dtwr_win = new int[k];
		ddtw_win = new int[k];
		ddtwr_win = new int[k];
		wdtw_win = new int[k];
		wddtw_win = new int[k];
		lcss_win = new int[k];
		twe_win = new int[k];
		erp_win = new int[k];
		msm_win = new int[k];
		
		euc_time = new long[k];
		dtw_time = new long[k];
		dtwr_time = new long[k];
		ddtw_time = new long[k];
		ddtwr_time = new long[k];
		wdtw_time = new long[k];
		wddtw_time = new long[k];
		lcss_time = new long[k];
		twe_time = new long[k];
		erp_time = new long[k];
		msm_time = new long[k];
		
		boss_transform_time = new long[k];
		ee_time = new long[k];
		randf_time = new long[k];
		rotf_time = new long[k];
		st_time = new long[k];
		boss_time = new long[k];
		tsf_time = new long[k];
		rif_time = new long[k];
		
		rif_acf_time = new long[k];
		rif_pacf_time = new long[k];
		rif_arma_time = new long[k];
		rif_ps_time = new long[k];
		rif_dft_time = new long[k];
		
		ee_splitter_train_time = new long[k];
		boss_splitter_train_time = new long[k];
		rise_splitter_train_time = new long[k];
		st_splitter_train_time = new long[k];
		data_fetch_time = new long[k];
		boss_data_fetch_time = new long[k];
		rise_data_fetch_time = new long[k];
		st_data_fetch_time = new long[k];
		split_evaluator_train_time = new long[k];
	}
	
	public void aggregateResults() {
		for (int i = 0; i < trees.length; i++) {
			
			trees[i].stats.aggregateResults();	//call this first: aggregate any tree results, such as counting nodes of the tree
			
			nodes[i] = trees[i].stats.num_nodes;
			leaves[i] = trees[i].stats.num_leaves;
			depths[i] = trees[i].stats.depth;
			
			ee_count[i] = trees[i].stats.ee_count;
			randf_count[i] = trees[i].stats.randf_count;
			rotf_count[i] = trees[i].stats.rotf_count;
			st_count[i] = trees[i].stats.st_count;
			boss_count[i] = trees[i].stats.boss_count;
			tsf_count[i] = trees[i].stats.tsf_count;
			rif_count[i] = trees[i].stats.rif_count;
			
			ee_win[i] = trees[i].stats.ee_win;
			randf_win[i] = trees[i].stats.randf_win;
			rotf_win[i] = trees[i].stats.rotf_win;
			st_win[i] = trees[i].stats.st_win;
			boss_win[i] = trees[i].stats.boss_win;
			tsf_win[i] = trees[i].stats.tsf_win;
			rif_win[i] = trees[i].stats.rif_win;
			
			rif_acf_count[i] = trees[i].stats.rif_acf_count;
			rif_pacf_count[i] = trees[i].stats.rif_pacf_count;
			rif_arma_count[i] = trees[i].stats.rif_arma_count;
			rif_ps_count[i] = trees[i].stats.rif_ps_count;
			rif_dft_count[i] = trees[i].stats.rif_dft_count;
			rif_acf_win[i] = trees[i].stats.rif_acf_count;
			rif_pacf_win[i] = trees[i].stats.rif_pacf_win;
			rif_arma_win[i] = trees[i].stats.rif_arma_win;
			rif_ps_win[i] = trees[i].stats.rif_ps_win;
			rif_dft_win[i] = trees[i].stats.rif_dft_win;
			
			euc_count[i] = trees[i].stats.euc_count;
			dtw_count[i] = trees[i].stats.dtw_count;
			dtwr_count[i] = trees[i].stats.dtwr_count;
			ddtw_count[i] = trees[i].stats.ddtw_count;
			ddtwr_count[i] = trees[i].stats.ddtwr_count;
			wdtw_count[i] = trees[i].stats.wddtw_count;
			wddtw_count[i] = trees[i].stats.wddtw_count;
			lcss_count[i] = trees[i].stats.lcss_count;
			twe_count[i] = trees[i].stats.twe_count;
			erp_count[i] = trees[i].stats.erp_count;
			msm_count[i] = trees[i].stats.msm_count;
			
			euc_win[i] = trees[i].stats.euc_win;
			dtw_win[i] = trees[i].stats.dtw_win;
			dtwr_win[i] = trees[i].stats.dtwr_count;
			ddtw_win[i] = trees[i].stats.ddtw_win;
			ddtwr_win[i] = trees[i].stats.ddtwr_win;
			wdtw_win[i] = trees[i].stats.wdtw_win;
			wddtw_win[i] = trees[i].stats.wddtw_win;
			lcss_win[i] = trees[i].stats.lcss_win;
			twe_win[i] = trees[i].stats.twe_win;
			erp_win[i] = trees[i].stats.erp_win;
			msm_win[i] = trees[i].stats.msm_win;
			
			euc_time[i] = trees[i].stats.euc_time;
			dtw_time[i] = trees[i].stats.dtw_time;
			dtwr_time[i] = trees[i].stats.dtwr_time;
			ddtw_time[i] = trees[i].stats.ddtw_time;
			ddtwr_time[i] = trees[i].stats.ddtwr_time;
			wdtw_time[i] = trees[i].stats.wdtw_time;
			wddtw_time[i] = trees[i].stats.wddtw_time;
			lcss_time[i] = trees[i].stats.lcss_time;
			twe_time[i] = trees[i].stats.twe_time;
			erp_time[i] = trees[i].stats.erp_time;
			msm_time[i] = trees[i].stats.msm_time;
			
			boss_transform_time[i] = results.boss_transform_time;
			ee_time[i] = trees[i].stats.ee_time;
			randf_time[i] = trees[i].stats.randf_time;
			rotf_time[i] = trees[i].stats.rotf_time;
			st_time[i] = trees[i].stats.st_time;
			boss_time[i] = trees[i].stats.boss_time;
			tsf_time[i] = trees[i].stats.tsf_time;
			rif_time[i] = trees[i].stats.rif_time;
			
			rif_acf_time[i] = trees[i].stats.rif_acf_time;
			rif_pacf_time[i] = trees[i].stats.rif_pacf_time;
			rif_arma_time[i] = trees[i].stats.rif_arma_time;
			rif_ps_time[i] = trees[i].stats.rif_ps_time;
			rif_dft_time[i] = trees[i].stats.rif_dft_time;
			
			//dev/debug stats
			ee_splitter_train_time[i] = trees[i].stats.ee_splitter_train_time;
			boss_splitter_train_time[i] = trees[i].stats.boss_splitter_train_time;
			rise_splitter_train_time[i] = trees[i].stats.rise_splitter_train_time;
			st_splitter_train_time[i] = trees[i].stats.st_splitter_train_time;
			data_fetch_time[i] = trees[i].stats.data_fetch_time;
			boss_data_fetch_time[i] = trees[i].stats.boss_data_fetch_time;
			rise_data_fetch_time[i] = trees[i].stats.rise_data_fetch_time;
			st_data_fetch_time[i] = trees[i].stats.st_data_fetch_time;
			split_evaluator_train_time[i] = trees[i].stats.split_evaluator_train_time;
		}
		
		results_aggregated = true;
	}
	
	public String getCSVHeader() {
		return null;
	}
	
	public StringBuilder getCSVRow() {
		StringBuilder sb = new StringBuilder();
		final char sep = ',';

		sb.append(Statistics.sum(this.nodes));
		sb.append(sep);
		sb.append(Statistics.sum(this.leaves));
		sb.append(sep);
		sb.append(Statistics.sum(this.depths));
		sb.append(sep);
		
		sb.append(Statistics.sum(this.ee_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.randf_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.rotf_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.st_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.boss_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.tsf_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.ee_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.randf_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.rotf_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.st_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.boss_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.tsf_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_win));
		sb.append(sep);
		
		sb.append(Statistics.sum(this.rif_acf_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_pacf_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_arma_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_ps_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_dft_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_acf_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_pacf_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_arma_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_ps_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_dft_win));
		sb.append(sep);
		
		sb.append(Statistics.sum(this.euc_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.dtw_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.dtwr_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.ddtw_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.ddtwr_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.wdtw_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.wddtw_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.lcss_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.twe_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.erp_count));
		sb.append(sep);
		sb.append(Statistics.sum(this.msm_count));
		sb.append(sep);
		
		sb.append(Statistics.sum(this.euc_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.dtw_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.dtwr_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.ddtw_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.ddtwr_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.wdtw_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.wddtw_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.lcss_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.twe_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.erp_win));
		sb.append(sep);
		sb.append(Statistics.sum(this.msm_win));
		sb.append(sep);
		
		sb.append(Statistics.sum(this.euc_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.dtw_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.dtwr_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.ddtw_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.ddtwr_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.wdtw_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.wddtw_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.lcss_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.twe_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.erp_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.msm_time));
		sb.append(sep);
		
		sb.append(Statistics.sum(this.boss_transform_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.ee_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.randf_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.rotf_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.st_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.boss_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.tsf_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_acf_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_pacf_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_arma_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_ps_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.rif_dft_time));
		sb.append(sep);
		
		sb.append(Statistics.sum(this.ee_splitter_train_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.boss_splitter_train_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.rise_splitter_train_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.st_splitter_train_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.data_fetch_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.boss_data_fetch_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.rise_data_fetch_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.st_data_fetch_time));
		sb.append(sep);
		sb.append(Statistics.sum(this.split_evaluator_train_time));
		sb.append(sep);
		
		
		return sb;
	}
	
}




