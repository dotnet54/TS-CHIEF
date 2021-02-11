package trees.splitters.old;

import gnu.trove.map.TIntObjectMap;

public interface NodeSplitter {
	
	public TIntObjectMap<Dataset> train_splitter(Dataset sample) throws Exception;
	public int predict_by_splitter(double[] query) throws Exception;

}
