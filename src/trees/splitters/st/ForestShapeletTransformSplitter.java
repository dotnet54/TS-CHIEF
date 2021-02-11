package trees.splitters.st;

import java.util.Random;

import application.AppConfig;
import data.timeseries.*;
import trees.splitters.RandomTreeOnIndexSplitter;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSChiefNode;
import trees.splitters.st.dev.ShapeletTransformDataStore.STInfoRecord;

/*
 * 
 * 
 * At each node take a sample (does it needs to have all the classes?)
 * convert the sample into k shapelets
 * at each node select a random exemplar from each class, and find all shapelet candidates for all of them
 * 
 * 
 * 
 */

public class ForestShapeletTransformSplitter extends RandomTreeOnIndexSplitter {
	
	Random rand;
	public ShapeletEx best_shapelet;	//stored normalized, if enabled
//	public SplitCriterion best_criterion; 
//	List<ShapeletEx> shapelets;
	
	public ForestShapeletTransformSplitter(TSChiefNode node) throws Exception {
		super(node, AppConfig.st_feature_selection);
		this.rand = AppConfig.getRand();
		this.node.tree.stats.st_count++;		
	}
	
	public String toString() {
		return "ST-forest[gsplit=" + super.toString()  +"]";
	}

	@Override
	public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		setNumFeatures(AppConfig.it_m, nodeTrainData.length(), AppConfig.st_feature_selection);
		
		Dataset stTrainingSet = this.node.tree.getForest().stDataStore.getTrainingSet();
//		MTSDataset nodeDataset = new MTSDataset(indices.length, stTrainingSet.length());
//		for (int j = 0; j < indices.length; j++) {
//			nodeDataset.add(stTrainingSet.get_series(indices[j]));
//		}
		
		TIntObjectMap<TIntArrayList> split_indices =  super.fitByIndices(stTrainingSet, trainIndices);
	
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>();
		Dataset fullTrainSet = AppConfig.getTrainingSet();
		for (int k : split_indices.keys()) {
			TIntArrayList ind = split_indices.get(k);
			MTSDataset temp = new MTSDataset(ind.size());
			for (int i = 0; i < ind.size(); i++) {
				temp.add(fullTrainSet.getSeries(ind.get(i)));
			}
			splits.putIfAbsent(k, temp);
		}
				
		//store the shapelet
		//get shapelet record
		STInfoRecord rec = this.node.tree.getForest().stDataStore.stInfoLines.get(getBestAttribute());
		TimeSeries s = fullTrainSet.getSeries(rec.seriesId);
		best_shapelet = new ShapeletEx(s, rec.startPos, rec.startPos + rec.length, true, AppConfig.st_normalize);
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.st_splitter_train_time += (System.nanoTime() - startTime);
		return splits;
	}

	@Override
	public TIntObjectMap<TIntArrayList> fitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		setNumFeatures(AppConfig.it_m, nodeTrainData.length(), AppConfig.it_feature_selection);
		
		Dataset stTrainingSet = this.node.tree.getForest().stDataStore.getTrainingSet();
		trainIndices.setDataset(stTrainingSet); //TODO
		TIntObjectMap<TIntArrayList> splits =  super.fitByIndices(nodeTrainData, trainIndices);
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.st_splitter_train_time += (System.nanoTime() - startTime);
 		return splits;
	}

	@Override
	public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
	
		Dataset stTrainingSet = this.node.tree.getForest().stDataStore.getTrainingSet();
//		MTSDataset nodeDataset = new MTSDataset(indices.length, stTrainingSet.length());
//		for (int j = 0; j < indices.length; j++) {
//			nodeDataset.add(stTrainingSet.get_series(indices[j]));
//		}		
		
		TIntObjectMap<TIntArrayList> split_indices = super.splitByIndices(stTrainingSet, trainIndices);
		
		TIntObjectMap<Dataset> branches = new TIntObjectHashMap<Dataset>();
		Dataset fullTrainSet = AppConfig.getTrainingSet();
		for (int k : split_indices.keys()) {
			TIntArrayList ind = split_indices.get(k);
			Dataset temp = new MTSDataset(ind.size());
			for (int i = 0; i < ind.size(); i++) {
				temp.add(fullTrainSet.getSeries(ind.get(i)));
			}
			branches.putIfAbsent(k, temp);
		}
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.st_splitter_train_time += (System.nanoTime() - startTime);
		return branches;
	}

	@Override
	public TIntObjectMap<TIntArrayList> splitByIndices(Dataset allTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();

		Dataset stTrainingSet = this.node.tree.getForest().stDataStore.getTrainingSet();
		TIntObjectMap<TIntArrayList> branches = super.splitByIndices(stTrainingSet, trainIndices); //TODO  train or split??
		
		//measure time before split function as time is measured separately for split function as it can be called separately --to prevent double counting 
		this.node.tree.stats.st_splitter_train_time += (System.nanoTime() - startTime);
		
		return branches;
	}

	@Override
	public int predict(TimeSeries query, Dataset testData, int queryIndex) throws Exception {
		double dist = best_shapelet.distance(query);

		if (dist < getBestThreshold()) {
			return LEFT_BRANCH;
		}else {
			return RIGHT_BRANCH;
		}		
	}

}

















