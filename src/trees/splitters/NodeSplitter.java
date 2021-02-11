package trees.splitters;

		import data.timeseries.Dataset;
		import data.timeseries.Indexer;
		import data.timeseries.TimeSeries;
		import gnu.trove.list.array.TIntArrayList;
		import gnu.trove.map.TIntObjectMap;

public interface NodeSplitter {

	public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception;

	public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception;

//	public int predict(TimeSeries query) throws Exception;

	public int predict(TimeSeries query, Dataset testData, int queryIndex) throws Exception;

	//DEV --- need these as they have different return types

	public TIntObjectMap<TIntArrayList> fitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception;

	public TIntObjectMap<TIntArrayList> splitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception;

}
