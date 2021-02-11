package trees.splitters.ee;

import java.util.ArrayList;
import java.util.List;

import application.AppConfig;
import core.exceptions.NotSupportedException;
import data.timeseries.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSChiefNode;
import trees.splitters.NodeSplitter;
import distance.univariate.UnivarSimMeasure;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ElasticDistanceSplitter implements NodeSplitter{

	protected transient TSChiefNode node;
	protected UnivarSimMeasure distanceMeasure;
	protected TIntObjectMap<double[][]> exemplars;
	protected TIntObjectMap<Dataset> splits;

	//just to reuse this data structure -- used during testing
	private List<Integer> closestNodes = new ArrayList<Integer>();

	public ElasticDistanceSplitter(TSChiefNode node) throws Exception {
		this.node = node;
		this.node.tree.stats.ee_count++;
	}
	
	public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		TIntObjectMap<Dataset> dataPerClass = nodeTrainData.splitByClass();	//TODO can be done once at node

		if (AppConfig.random_dm_per_node) {
			int r = AppConfig.getRand().nextInt(AppConfig.enabled_distance_measures.length);
			distanceMeasure = new UnivarSimMeasure(AppConfig.enabled_distance_measures[r], node);
		}else {
			distanceMeasure = node.tree.treeDistanceMeasure;
		}

		distanceMeasure.select_random_params(nodeTrainData, AppConfig.getRand());
		exemplars = new TIntObjectHashMap<double[][]>(nodeTrainData.getNumClasses());

		int branch = 0;
		for (int key : dataPerClass.keys()) {
			int r = AppConfig.getRand().nextInt(dataPerClass.get(key).size());
			exemplars.put(branch, dataPerClass.get(key).getSeries(r).data());
			branch++;
		}

		//measure time before split function as time is measured separately for split function as it can be
		//called separately --to prevent double counting
		this.node.tree.stats.ee_splitter_train_time += (System.nanoTime() - startTime);
		return split(nodeTrainData, trainIndices);
	}

	@Override
	public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();

		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>(nodeTrainData.getNumClasses());
		int data_size = nodeTrainData.size();
		int closest_branch;

		for (int j = 0; j < data_size; j++) {
			closest_branch = findNearestExemplar(nodeTrainData.getSeries(j).data(), distanceMeasure, exemplars);

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

	@Override
	public int predict(TimeSeries query, Dataset testData, int queryIndex) throws Exception {
		return findNearestExemplar(query.data(), distanceMeasure, exemplars);
	}

	@Override
	public TIntObjectMap<TIntArrayList> fitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		throw new NotSupportedException();
	}

	@Override
	public TIntObjectMap<TIntArrayList> splitByIndices(Dataset allTrainData, Indexer trainIndices) throws Exception {
		throw new NotSupportedException();
	}

	private synchronized int findNearestExemplar(
			double[][] query, UnivarSimMeasure distance_measure,
			TIntObjectMap<double[][]> exemplars) throws Exception{

		closestNodes.clear();
		double dist = Double.POSITIVE_INFINITY;
		double bsf = Double.POSITIVE_INFINITY;

		for (int key : exemplars.keys()) {
			double[][] exemplar = exemplars.get(key);

			if (AppConfig.config_skip_distance_when_exemplar_matches_query && exemplar == query) {
				return key;
			}

			dist = distance_measure.distance(query[0], exemplar[0]); //TODO using only dimension 0

			if (dist < bsf) {
				bsf = dist;
				closestNodes.clear();
				closestNodes.add(key);
			}else if (dist == bsf) {
//				if (distance == min_distance) {
//					System.out.println("min distances are same " + distance + ":" + min_distance);
//				}
				bsf = dist;
				closestNodes.add(key);
			}
		}

		int r = AppConfig.getRand().nextInt(closestNodes.size());
		return closestNodes.get(r);
	}
	
	public UnivarSimMeasure getSimilarityMeasure() {
		return this.distanceMeasure;
	}
	
	public String toString() {
		if (distanceMeasure == null) {
			return "EE[untrained]";
		}else {
			return "EE[" + distanceMeasure.toString() + "]"; //,e={" + "..." + "}
		}		
	}

}
