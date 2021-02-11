package trees.splitters.it;

import application.AppConfig;
import data.timeseries.*;
import trees.TSChiefNode;
import trees.splitters.RandomTreeOnIndexSplitter;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class InceptionTimeSplitter extends RandomTreeOnIndexSplitter {

	protected DataStore dataStore;	
	
	public InceptionTimeSplitter(TSChiefNode node) throws Exception {
		super(node, AppConfig.it_feature_selection);
		this.dataStore = node.tree.getForest().itDataStore;
		this.node.tree.stats.it_count++;		
	}
	
	public String toString() {
		return "InceptionTime[gsplit=" + super.toString()  +"]";
	}
	
	@Override
	public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		setNumFeatures(AppConfig.it_m, nodeTrainData.length(), AppConfig.it_feature_selection);
		
		Dataset itTrainingSet = this.node.tree.getForest().itDataStore.getTrainingSet();
//		MTSDataset nodeDataset = new MTSDataset(indices.length, itTrainingSet.length());
//		for (int j = 0; j < indices.length; j++) {
//			nodeDataset.add(itTrainingSet.get_series(indices[j]));
//		}
		
		TIntObjectMap<TIntArrayList> splitIndices =  super.fitByIndices(itTrainingSet, trainIndices);
		if (splitIndices == null) {
			return null; //cant find a sensible split point for this data.
		}

		Dataset fullTrainSet = AppConfig.getTrainingSet();
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>();
		for (int k : splitIndices.keys()) {
			TIntArrayList ind = splitIndices.get(k);
			MTSDataset temp = new MTSDataset(ind.size());
			for (int i = 0; i < ind.size(); i++) {
				temp.add(fullTrainSet.getSeries(ind.get(i)));
			}
			splits.putIfAbsent(k, temp);
		}
				
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.it_splitter_train_time += (System.nanoTime() - startTime);
 		return splits;
	}

	@Override
	public TIntObjectMap<TIntArrayList> fitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		setNumFeatures(AppConfig.it_m, nodeTrainData.length(), AppConfig.it_feature_selection);
		
		Dataset itTrainingSet = this.node.tree.getForest().itDataStore.getTrainingSet();
		trainIndices.setDataset(itTrainingSet); //TODO refactor
 		TIntObjectMap<TIntArrayList> splits =  super.fitByIndices(nodeTrainData, trainIndices);
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.it_splitter_train_time += (System.nanoTime() - startTime);
 		return splits;
	}

	@Override
	public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		
		Dataset itTrainingSet = this.node.tree.getForest().itDataStore.getTrainingSet();
//		MTSDataset nodeDataset = new MTSDataset(indices.length, itTrainingSet.length());
//		for (int j = 0; j < indices.length; j++) {
//			nodeDataset.add(itTrainingSet.get_series(indices[j]));
//		}		
	
		TIntObjectMap<TIntArrayList> splitIndices = super.splitByIndices(itTrainingSet, trainIndices);

		Dataset fullTrainSet = AppConfig.getTrainingSet();
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>();
		for (int k : splitIndices.keys()) {
			TIntArrayList ind = splitIndices.get(k);
			Dataset temp = new MTSDataset(ind.size());
			for (int i = 0; i < ind.size(); i++) {
				temp.add(fullTrainSet.getSeries(ind.get(i)));
			}
			splits.putIfAbsent(k, temp);
		}
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.it_splitter_train_time += (System.nanoTime() - startTime);
		return splits;
	}

	@Override
	public TIntObjectMap<TIntArrayList> splitByIndices(Dataset allTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();

		Dataset itTrainingSet = this.node.tree.getForest().itDataStore.getTrainingSet();
		TIntObjectMap<TIntArrayList> branches = super.splitByIndices(itTrainingSet, trainIndices);
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.it_splitter_train_time += (System.nanoTime() - startTime);
		return branches;
	}

	@Override
	public int predict(TimeSeries query, Dataset testData, int queryIndex) throws Exception {
		
		Dataset itTestingSet = dataStore.getTestingSet();
		TimeSeries transformed_query = itTestingSet.getSeries(queryIndex);
		
		return super.predict(transformed_query, itTestingSet, queryIndex);
	}

}
