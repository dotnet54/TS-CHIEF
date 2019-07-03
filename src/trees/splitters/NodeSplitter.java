package trees.splitters;

import datasets.TSDataset;
import datasets.TimeSeries;
import gnu.trove.map.TIntObjectMap;

public interface NodeSplitter {
	
	public TIntObjectMap<TSDataset> train(TSDataset data, int[] indices) throws Exception;
	public void train_binary(TSDataset data) throws Exception;
	public TIntObjectMap<TSDataset> split(TSDataset data, int[] indices) throws Exception;
	public int predict(TimeSeries query, int queryIndex) throws Exception;

}
