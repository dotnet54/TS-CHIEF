package trees.splitters.ee;

import application.AppConfig;
import core.exceptions.NotImplementedException;
import core.exceptions.NotSupportedException;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import data.timeseries.MTSDataset;
import data.timeseries.TimeSeries;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSChiefNode;
import trees.splitters.NodeSplitter;
import distance.univariate.MEASURE;
import distance.multivariate.*;
import util.Sampler;
import util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

//public class MultivariateElasticDistanceSplitter extends ElasticDistanceSplitter{
public class MultivariateElasticDistanceSplitter implements NodeSplitter {

	protected transient TSChiefNode node;
	protected MultivarSimMeasure similarityMeasure;
	protected TIntObjectMap<double[][]> exemplars;
	protected TIntObjectMap<Dataset> splits;

	//just to reuse this data structure -- used during testing
	private List<Integer> closestNodes = new ArrayList<Integer>();

	public MultivariateElasticDistanceSplitter(TSChiefNode node) throws Exception {
//		super(node);
		this.node = node;
		this.node.tree.stats.ee_count++;
	}
	
	public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		TIntObjectMap<Dataset> dataPerClass = nodeTrainData.splitByClass();	//TODO can be done once at node

		MEASURE measureName;
		boolean useDependentDimensions;
		int[] dimensionsToUse;

		if (AppConfig.useRandomSubsetsOfDimensions){
			if (AppConfig.maxDimensionsPerSubset > nodeTrainData.dimensions() || AppConfig.maxDimensionsPerSubset == 0){
				dimensionsToUse = Sampler.getIntsFromRange(0, nodeTrainData.dimensions(), 1);
			}else{
				int numDimensions = Util.getRandNextInt(AppConfig.minDimensionsPerSubset, AppConfig.maxDimensionsPerSubset);
				dimensionsToUse = Sampler.sampleNRandomIntsFromRange(0, nodeTrainData.dimensions(),
						numDimensions);
			}
		}else{
			int r = Util.getRandNextInt(0, AppConfig.dimensionSubsets.length);
			dimensionsToUse = AppConfig.dimensionSubsets[r];
		}

		if (AppConfig.dimensionDependencyMethod.equals(AppConfig.DimensionDependencyMethod.independent)){
			useDependentDimensions = false;
		}else if (AppConfig.dimensionDependencyMethod.equals(AppConfig.DimensionDependencyMethod.dependent)){
			useDependentDimensions = true;
		}else {
			useDependentDimensions = AppConfig.getRand().nextBoolean();
		}

		if (AppConfig.random_dm_per_node) {
			int r = AppConfig.getRand().nextInt(AppConfig.enabled_distance_measures.length);
			measureName = AppConfig.enabled_distance_measures[r];

			this.similarityMeasure = MultivarSimMeasure.createSimilarityMeasure(
					measureName, useDependentDimensions, dimensionsToUse);
		}else {
			throw new NotImplementedException();
//			distanceMeasure = node.tree.treeDistanceMeasure;
		}

		if (AppConfig.ee_use_random_params){
			if (measureName.equals(MEASURE.ddtw) || measureName.equals(MEASURE.ddtwf)){
				((DDTW) similarityMeasure).setWindowSizeInt(nodeTrainData.length());
			}else if (measureName.equals(MEASURE.dtw) || measureName.equals(MEASURE.dtwf)){
				((DTW) similarityMeasure).setWindowSizeInt(nodeTrainData.length());
			}else{
				similarityMeasure.setRandomParams(nodeTrainData, AppConfig.getRand());
			}
		}else{
			if (similarityMeasure instanceof DDTW){ //test sub class first
				if (measureName.equals(MEASURE.ddtw) || measureName.equals(MEASURE.ddtwf)){
					((DDTW) similarityMeasure).setWindowSizeInt(nodeTrainData.length());
				}else{
					((DDTW) similarityMeasure).setWindowSizeInt(AppConfig.windowSize);
				}
			}else if (similarityMeasure instanceof DTW){
				if (measureName.equals(MEASURE.dtw) || measureName.equals(MEASURE.dtwf)){
					((DTW) similarityMeasure).setWindowSizeInt(nodeTrainData.length());
				}else{
					((DTW) similarityMeasure).setWindowSizeInt(AppConfig.windowSize);
				}
			}if (similarityMeasure instanceof WDDTW){ //test sub class first
				//TODO NOTE - for variable length datasets? -- change nodeTrainData.length()
				((WDDTW) similarityMeasure).setG(AppConfig.penalty, nodeTrainData.length());
			}if (similarityMeasure instanceof WDTW){
				((WDTW) similarityMeasure).setG(AppConfig.penalty, nodeTrainData.length());
			}if (similarityMeasure instanceof LCSS){
				((LCSS) similarityMeasure).setWindowSizeInt(AppConfig.windowSize);
				((LCSS) similarityMeasure).setEpsilon(AppConfig.epsilon);
			}if (similarityMeasure instanceof MSM){
				((MSM) similarityMeasure).setCost(AppConfig.cost);
			}if (similarityMeasure instanceof ERP){
				((ERP) similarityMeasure).setWindowSizeInt(AppConfig.windowSize);
//				((ERP) similarityMeasure).setG(AppConfig.cost);
				throw new RuntimeException("TODO not implemented");
			}if (similarityMeasure instanceof TWE){
				((TWE) similarityMeasure).setNu(AppConfig.penalty);
				((TWE) similarityMeasure).setLambda(AppConfig.cost);
			}
		}

		this.exemplars = new TIntObjectHashMap<double[][]>(nodeTrainData.getNumClasses());

		int branch = 0;
		for (int key : dataPerClass.keys()) {
			int r = AppConfig.getRand().nextInt(dataPerClass.get(key).size());
			this.exemplars.put(branch, dataPerClass.get(key).getSeries(r).data());
			branch++;
		}

		//measure time before split function as time is measured separately for split function as it can be
		//called separately --to prevent double counting
		this.node.tree.stats.ee_splitter_train_time += (System.nanoTime() - startTime);
		return split(nodeTrainData, trainIndices);
	}

//	@Override
	public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();

		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>(nodeTrainData.getNumClasses());
		int data_size = nodeTrainData.size();
		int closest_branch;

		for (int j = 0; j < data_size; j++) {
			closest_branch = findNearestExemplar(nodeTrainData.getSeries(j).data());

			if (! splits.containsKey(closest_branch)){
				// initial capacity based on class distributions, this may be an over or an underestimate, but better than 0 initial capacity
				splits.put(closest_branch, new MTSDataset(nodeTrainData.getClassDistribution().get(closest_branch)));
			}

			splits.get(closest_branch).add(nodeTrainData.getSeries(j));
		}

		//time is measured separately for split function from train function as it can be called separately -- to prevent double counting
		this.node.tree.stats.ee_splitter_train_time += (System.nanoTime() - startTime);
		return splits;
	}

//	@Override
	public int predict(TimeSeries query, Dataset testData, int queryIndex) throws Exception {
		return findNearestExemplar(query.data());
	}

//	@Override
	public TIntObjectMap<TIntArrayList> fitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		throw new NotSupportedException();
	}

//	@Override
	public TIntObjectMap<TIntArrayList> splitByIndices(Dataset allTrainData, Indexer trainIndices) throws Exception {
		throw new NotSupportedException();
	}

	private int findNearestExemplar(double[][] query) throws Exception{
		closestNodes.clear();
		double dist;
		double min_dist = Double.POSITIVE_INFINITY;

		for (int key : exemplars.keys()) {
			double[][] exemplar = exemplars.get(key);

			if (AppConfig.config_skip_distance_when_exemplar_matches_query && exemplar == query) {
				return key;
			}

			dist = similarityMeasure.distance(query, exemplar, Double.POSITIVE_INFINITY);

			if (dist < min_dist) {
				min_dist = dist;
				closestNodes.clear();
				closestNodes.add(key);
			}else if (dist == min_dist) {
//				if (distance == min_distance) {
//					System.out.println("min distances are same " + distance + ":" + min_distance);
//				}
//				min_dist = dist;
				closestNodes.add(key);
			}
		}

		int r = AppConfig.getRand().nextInt(closestNodes.size());
		return closestNodes.get(r);
	}
	
	public MultivarSimMeasure getSimilarityMeasure() {
		return this.similarityMeasure;
	}
	
	public String toString() {
		if (similarityMeasure == null) {
			return "EE[untrained]";
		}else {
			return "EE[" + similarityMeasure.toString() + "]"; //,e={" + "..." + "}
		}		
	}

}
