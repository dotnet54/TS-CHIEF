package trees.splitters;

import datasets.TSDataset;
import datasets.TimeSeries;
import gnu.trove.map.TIntObjectMap;
import trees.ProximityTree;

public class GenericNearestClassSplitter implements NodeSplitter{

	private ProximityTree.Node node;
	
	//split evaluation method
	
	public GenericNearestClassSplitter(ProximityTree.Node node) throws Exception {
		this.node = node;
	}

	@Override
	public TIntObjectMap<TSDataset> train(TSDataset data, int[] indices) throws Exception {
		return null;		
	}

	@Override
	public void train_binary(TSDataset data) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TIntObjectMap<TSDataset> split(TSDataset data, int[] indices) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int predict(TimeSeries query, int queryIndex) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}	
	
}
