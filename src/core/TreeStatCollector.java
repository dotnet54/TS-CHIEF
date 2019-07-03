package core;

import trees.ProximityTree;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class TreeStatCollector {

	private transient ProximityTree tree;
	
	public int forest_id;
	public int tree_id;
	
	//tree stats
	public int num_nodes;
	public int num_leaves;
	public int depth;
	public double weighted_depth;
	
	public int ee_count;
	public int randf_count;
	public int rotf_count;
	public int st_count;
	public int boss_count;
	public int tsf_count;
	public int rif_count;
	public int rif_acf_count;
	public int rif_pacf_count;
	public int rif_arma_count;
	public int rif_ps_count;
	public int rif_dft_count;

	public int ee_win;
	public int randf_win;
	public int rotf_win;
	public int st_win;
	public int boss_win;
	public int tsf_win;
	public int rif_win;
	public int rif_acf_win;
	public int rif_pacf_win;
	public int rif_arma_win;
	public int rif_ps_win;	
	public int rif_dft_win;

//	"euc_count, dtw_count, dtwr_count, ddtw_count, ddtwr_count, wdtw_count, wddtw_count, lcss_count, twe_count,erp_count,msm_count,"+
//	"euc_time, dtw_time, dtwr_time, ddtw_time, ddtwr_time, wdtw_time, wddtw_time, lcss_time, twe_time,erp_time,msm_time,"+
	
	public int euc_count;
	public int dtw_count;
	public int dtwr_count;
	public int ddtw_count;
	public int ddtwr_count;
	public int wdtw_count;
	public int wddtw_count;
	public int lcss_count;
	public int twe_count;
	public int erp_count;
	public int msm_count;

	public int euc_win;
	public int dtw_win;
	public int dtwr_win;
	public int ddtw_win;
	public int ddtwr_win;
	public int wdtw_win;
	public int wddtw_win;
	public int lcss_win;
	public int twe_win;
	public int erp_win;
	public int msm_win;
	
	public long euc_time;
	public long dtw_time;
	public long dtwr_time;
	public long ddtw_time;
	public long ddtwr_time;
	public long wdtw_time;
	public long wddtw_time;
	public long lcss_time;
	public long twe_time;
	public long erp_time;
	public long msm_time;
	
	//time for training
	public long ee_time;
	public long randf_time;
	public long rotf_time;
	public long st_time;
	public long boss_time;
	public long tsf_time;
	public long rif_time;
	public long rif_acf_time;
	public long rif_pacf_time;
	public long rif_arma_time;
	public long rif_ps_time;	
	public long rif_dft_time;
	
	//tree stats for last testing
	public double tree_accuracy;
	public long tree_train_time;
	public long tree_test_time;
		
	public long ee_splitter_train_time;
	public long boss_splitter_train_time;
	public long rise_splitter_train_time;
	public long st_splitter_train_time;

	public long data_fetch_time;
	public long boss_data_fetch_time;
	public long rise_data_fetch_time;
	public long st_data_fetch_time;

	public long split_evaluator_train_time;	
	
	public TreeStatCollector(int forest_id, int tree_id){		
		this.forest_id = forest_id;
		this.tree_id = tree_id;
	}
	
	public void collateResults(ProximityTree tree) {
		this.tree = tree;
		
		num_nodes = tree.get_num_nodes();
		num_leaves = tree.get_num_leaves();
		depth = tree.get_height();
		weighted_depth = -1; //TODO not implemented yet tree.get_weighted_depth();
	}
	
}
