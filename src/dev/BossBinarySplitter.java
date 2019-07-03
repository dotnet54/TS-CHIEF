package dev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;

import core.AppContext;
import core.ParallelFor;
import core.AppContext.SplitterType;
import core.AppContext.TransformLevel;
import datasets.BossDataset;
import datasets.BossDataset.BossParams;
import datasets.TSDataset;
import datasets.TimeSeries;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import transforms.BOSS;
import transforms.SFA;
import transforms.BOSS.BagOfPattern;
import transforms.SFA.HistogramType;
import transforms.SFA.Words;
import trees.ProximityTree.Node;
import trees.splitters.GenericGiniSplitter;
import trees.splitters.NodeSplitter;
import trees.splitters.SplitEvaluator;
import util.Sampler;
import util.Util;

public class BossBinarySplitter implements NodeSplitter {

	
	Node node;
	String best_hist_key;
	TIntObjectMap<TSDataset> best_splits = null; //TODO
	
	BossParams forest_transform_params;

	GenericGiniSplitter binary_splitter;
	
	TSDataset trans; //TODO temp
	
	public BossBinarySplitter(Node node) {
		this.node = node;
	}

	@Override
	public TIntObjectMap<TSDataset> train(TSDataset data, int[] indices) throws Exception {

		BossTransformContainer transforms = ((BossTransformContainer)this.node.tree.getForest().getTransforms().get("boss"));
		//pick a random tansform
		SplittableRandom rand = new SplittableRandom();
		int r = rand.nextInt(transforms.boss_params.size());
		forest_transform_params = transforms.boss_params.get(r);
		
		BossDataset transformed_dataset = transforms.boss_datasets.get(forest_transform_params.toString());
		BagOfPattern[] transformed_data = transformed_dataset.getTransformed_data();
		
		
		//form the node dataset
		List<BagOfPattern> node_boss_dataset = new ArrayList<BagOfPattern>();
		for (int i = 0; i < transformed_data.length; i++) {
			for (int j = 0; j < data.size(); j++) {
				
				if (transformed_data[i].getSeries() == data.get_series(j)) {
					node_boss_dataset.add(transformed_data[i]);
				}
				
			}
		}
		
		if (node_boss_dataset.size() == 0) {
			throw new Exception ("no data to split -check for bugs");
		}
		
		//generate new dataset
		trans = new TSDataset();
		TIntIntHashMap dictionary = new TIntIntHashMap();
		int[] all_words = extract_words(node_boss_dataset);		
		
		for (int i = 0; i < node_boss_dataset.size(); i++) {
			double[] tmp_data = new double[all_words.length];
			IntIntHashMap bag = node_boss_dataset.get(i).bag;
			
			for (int j = 0; j < all_words.length; j++) {
				if (bag.containsKey(all_words[j])) {
					tmp_data[j] =  bag.get(all_words[j]);
				}
			}
			
			TimeSeries ts= new TimeSeries(tmp_data, node_boss_dataset.get(i).getLabel());
			ts.transformed_series = true;
			ts.original_series = node_boss_dataset.get(i).getSeries();
			trans.add(ts);
		}
		
		int m = trans.length();
		
		
		binary_splitter = new GenericGiniSplitter(node, m);
		
		
		
//		binary_splitter.choose_random_attributes = true;
		
		binary_splitter.train(trans, indices);
		
		
		return split(data, indices);
	}


	private int[] extract_words(List<BagOfPattern> node_boss_dataset) {
		TIntIntHashMap words = new TIntIntHashMap();

		for (int i = 0; i < node_boss_dataset.size(); i++) {
			int[] keys = node_boss_dataset.get(i).bag.keys;
			
			for (int j = 0; j < keys.length; j++) {
				int word = node_boss_dataset.get(i).bag.get(keys[j]); 
				
				if (word != 0) {
					words.put(keys[j], word);
				}else{
//					System.out.println("0 word : check .... this " );
				}
				
				
			}
		}
		
		return words.keys();
	}

	@Override
	public void train_binary(TSDataset data) throws Exception {
		// TODO Auto-generated method stub
		
	}	
	
	
	@Override
	public TIntObjectMap<TSDataset> split(TSDataset data, int[] indices) throws Exception {
		
		TIntObjectMap<TSDataset> splits = binary_splitter.split(trans, indices);

		
		TIntObjectMap<TSDataset> original_data_splits = new TIntObjectHashMap<>();
		
		//extract original series
		for (int key : splits.keys()) {
			original_data_splits.put(key, to_original_set(splits.get(key)));
		}
		
		
		if (SplitEvaluator.has_empty_split(original_data_splits)) {
			throw new Exception("empty splits found! check for bugs");
		}
		
		return original_data_splits;
	}
	
	private TSDataset to_original_set(TSDataset transformed_set) {
		TSDataset split = new TSDataset();
		
		for (int i = 0; i < transformed_set.size(); i++) {
			split.add(transformed_set.get_series(i).original_series);
		}
		
		return split;
	}
	

	List<Integer> closest_nodes = new ArrayList<Integer>();

	@Override
	public int predict(TimeSeries query, int queryIndex) throws Exception {
		
//		return binary_splitter.split(data);
		
		long minDistance = Integer.MAX_VALUE;
		long distance = Integer.MAX_VALUE;
		int min_key = 0; //TODO

		BagOfPattern query_hist;

		if (AppContext.boss_transform_level == TransformLevel.Forest) {
			BossTransformContainer transforms = ((BossTransformContainer)this.node.tree.getForest().getTransforms().get("boss"));
			query_hist = transforms.transform_series_using_sfa(query, transforms.sfa_transforms.get(forest_transform_params.toString()));
		}else {
			query_hist = this.node.tree.boss_test_dataset_tree_level.getTransformed_data()[queryIndex];
		}
	
		closest_nodes.clear();
		
		int word_frquency;
		if (query_hist.bag.containsKey(binary_splitter.getBestAttribute())) {
			word_frquency = query_hist.bag.get(0);
		}else {
			word_frquency = 0;
		}
		
		//TODO equal case
		if (word_frquency < binary_splitter.getBestThreshold()) {
			return 0; //left branch
		}else {
			return 1; //right branch
		}


//		int r = ThreadLocalRandom.current().nextInt(closest_nodes.size());	//TODO may be use SplitRandom??
//		return closest_nodes.get(r);
	}
	

	
	


	
}
