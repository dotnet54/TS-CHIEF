package trees.splitters.st;

import java.util.List;
import java.util.Random;

import application.AppConfig;
import data.timeseries.UTimeSeries;
import data.timeseries.MTSDataset;
import gnu.trove.map.TIntObjectMap;
import trees.TSCheifTree;
import trees.splitters.NodeSplitter;
import trees.splitters.SplitCriterion;

public class ShapeletSplitter2 implements NodeSplitter{

	
	private final int NUM_CHILDREN = 2;
	private final int LEFT_BRANCH = 0;
	private final int RIGHT_BRANCH = 1;
	
	TSCheifTree.Node node;
	Random rand;
	public ShapeletEx best_shapelet;	//stored normalized, if enabled
	public SplitCriterion best_criterion; 
	List<ShapeletEx> shapelets;
	
	public ShapeletSplitter2(TSCheifTree.Node node) {
		this.node = node;	
		this.rand = AppConfig.getRand();
		this.node.tree.stats.st_count++;
	}
	
	public String toString() {
		return "ST[s=" + 30 + ",cp=" + 5 + "]";
	}

	@Override
	public TIntObjectMap<MTSDataset> train(MTSDataset data, int[] indices) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void train_binary(MTSDataset data) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TIntObjectMap<MTSDataset> split(MTSDataset data, int[] indices) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int predict(UTimeSeries query, int queryIndex) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}	
	
}
