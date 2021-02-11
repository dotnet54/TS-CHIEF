package trees.splitters.boss;

import java.util.ArrayList;
import java.util.List;
import com.carrotsearch.hppc.cursors.IntIntCursor;
import application.AppConfig;
import application.AppConfig.SplitMethod;
import application.AppConfig.TransformLevel;
import core.exceptions.NotSupportedException;
import data.timeseries.*;
import trees.TSChiefNode;
import trees.splitters.boss.BossClassifier.Predictions;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.splitters.NodeSplitter;
import trees.splitters.boss.BOSS.BagOfPattern;
import trees.splitters.boss.SFA.Words;
import trees.splitters.boss.dev.BossDataset;
import trees.splitters.boss.dev.BossTransformContainer;
import trees.splitters.boss.BOSSEnsembleClassifier.BOSSModel;
import trees.splitters.boss.dev.BossDataset.BossParams;
import util.Sampler;
import util.Util;

public class BossSplitter implements NodeSplitter {

	BOSSEnsembleClassifier bossEns;
	TIntObjectHashMap<BOSSEnsembleClassifier> bossEnsPerClass;

	TIntObjectHashMap<BOSSModel> bossModelPerClass;
	TIntObjectHashMap<BOSS> bossPerClass;
	TIntObjectHashMap<BagOfPattern> bopPerClass;
	
	boolean bestNorm;
	int bestWindow;
	int bestF;
	
	TIntIntMap training_class_dist;

	private boolean use_ensemble_per_class = false;

	int windowLength = 8;
	int wordLength = 4;
	int symbols = 4;
	boolean normMean = true;
	boolean lowerBounding = true;
	public static boolean[] NORMALIZATION = new boolean[] { true, false };

	int maxF = 16;
	int minF = 6;
	int maxS = 4;

	int MAX_WINDOW_LENGTH = 250;
	int minWindowLength = 10;

	TSChiefNode node;
//	HashMap<String, SFA> sfa_transforms;
	String best_hist_key;
	TIntObjectMap<Dataset> best_splits = null; //TODO
	
	BossParams forest_transform_params;
	
	public BossSplitter(TSChiefNode node) {
		// TODO Auto-generated constructor stub
		this.node = node;
		this.node.tree.stats.boss_count++;

//		sfa_transforms = ((BossTransformContainer)this.node.tree.getForest().getTransforms().get("boss")).getSFATransforms();
	}

	@Override
	public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();

//		if (use_ensemble_per_class) {
//			train_one_ensemble(data);
//		}else {
//			train_one_ensemble_per_class(data);
//		}

		if (AppConfig.boss_transform_level ==  TransformLevel.Forest) {
			if (AppConfig.boss_split_method == SplitMethod.RandomTreeStyle) {
				train_using_forest_transform_ginisplit(nodeTrainData);
			}else {
				train_using_forest_transform(nodeTrainData, trainIndices);
			}
		}else {
			train_using_tree_transform(nodeTrainData);
		}
		//time is measured separately for split function from train function as it can be called separately -- to prevent double counting 
		this.node.tree.stats.boss_splitter_train_time += (System.nanoTime() - startTime);
		
		return split(nodeTrainData, trainIndices);
	}


	private int split_freq;
	private int split_word;
	
	private void train_using_forest_transform_ginisplit(Dataset data) {
		throw new NotSupportedException();
	}	
	
	private int predict_gini_split(TimeSeries query, int queryIndex) {
		throw new NotSupportedException();
	}	
	
	private void train_using_tree_transform(Dataset data)throws Exception {
		BossDataset transformed_dataset = this.node.tree.treeLevelBossTrainData;
		BagOfPattern[] transformed_data = transformed_dataset.getTransformed_data();
		
		
		//form the node dataset
		List<BagOfPattern> node_boss_dataset = new ArrayList<BagOfPattern>();
		for (int i = 0; i < transformed_data.length; i++) {
			for (int j = 0; j < data.size(); j++) {
				
				if (transformed_data[i].getSeries() == data.getSeries(j)) {
					node_boss_dataset.add(transformed_data[i]);
				}
				
			}
		}
		
		//split by class
		TIntObjectMap<List<BagOfPattern>> boss_data_per_class = split_by_class(node_boss_dataset);

		//pick one random example per class
		bopPerClass = new TIntObjectHashMap<>();
		for (int key : boss_data_per_class.keys()) {
			int r = AppConfig.getRand().nextInt(boss_data_per_class.get(key).size());
			BagOfPattern example = boss_data_per_class.get(key).get(r);	
			bopPerClass.put(key, example);
		}
		
		
		//for each instance at node, find the closest distance to examplar
		long distance = Long.MAX_VALUE;
		double minWGini = Double.MAX_VALUE;
		
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>();

		for (int j = 0; j < node_boss_dataset.size(); j++) {
			long minDistance = Long.MAX_VALUE;
			int min_key = -1;
			closest_nodes.clear();
			
			for (int key : bopPerClass.keys()) {
				splits.putIfAbsent(key, new MTSDataset());
				
				BagOfPattern bag1_query = node_boss_dataset.get(j);

				BagOfPattern bag2 = bopPerClass.get(key);
				
				if (bag1_query == bag2) {
//					minDistance = 0; //TODO
					min_key = key;
					closest_nodes.clear();
					closest_nodes.add(min_key);
					break;
				}
				
				distance = BossDistance(bag1_query, bag2);
				
//				System.out.println("boss dist - query: " + bag1_query.toString());
//				System.out.println("boss dist - bag2: " + bag2.toString());
//				System.out.println("boss dist: " + distance + ", to class " + key);

			
				if (distance < minDistance) {
					minDistance = distance;
					min_key = key;
					closest_nodes.clear();
					closest_nodes.add(min_key);
				}else if (distance == minDistance) {
	//				if (distance == min_distance) {
	//					System.out.println("min distances are same " + distance + ":" + min_distance);
	//				}
					minDistance = distance;
					closest_nodes.add(key);
				}				
			}


			int r = AppConfig.getRand().nextInt(closest_nodes.size());	//TODO may be use SplitRandom??
			min_key =  closest_nodes.get(r);
			
			//put n to bin with min distance
			splits.get(min_key).add(node_boss_dataset.get(j).getSeries());
//			System.out.println("min distance to key; " + min_key + ", actual class " + node_boss_dataset.get(j).getSeries().getLabel() 
//					+ " splits: " + splits.toString());
//			int asdsa = 0;
//			asdsa++;
		}
		
		double wgini = Util.weighted_gini(splits, data.size());
		
//		System.out.println("wgini:" + wgini + "  " + splits);
		
		//TODO temp
		this.best_splits = splits;

	}
	
	
	public TIntObjectMap<List<BagOfPattern>> split_by_class(List<BagOfPattern> transformed_data) {
		TIntObjectMap<List<BagOfPattern>> split =  new TIntObjectHashMap<List<BagOfPattern>>();
		Integer label;
		List<BagOfPattern> class_set = null;

		for (int i = 0; i < transformed_data.size(); i++) {
			label = transformed_data.get(i).getLabel();
			if (! split.containsKey(label)) {
				class_set = new ArrayList<BagOfPattern>();
				split.put(label, class_set);
			}
			
			split.get(label).add(transformed_data.get(i));
		}
		
		return split;
	}	
	
	
	private void train_using_forest_transform(Dataset data, Indexer trainIndices) throws Exception {
		
		BossTransformContainer transforms = ((BossTransformContainer)this.node.tree.getForest().getTransforms().get("boss"));
		
		
		//pick a random tansform
//		SplittableRandom rand = new SplittableRandom();
		int r = AppConfig.getRand().nextInt(transforms.boss_params.size());
		forest_transform_params = transforms.boss_params.get(r);
		
		BossDataset transformed_dataset = transforms.boss_datasets.get(forest_transform_params.toString());
		BagOfPattern[] transformed_data = transformed_dataset.getTransformed_data();
		
		
		//form the node dataset
		List<BagOfPattern> node_boss_dataset = new ArrayList<BagOfPattern>();
//		for (int i = 0; i < transformed_data.length; i++) {
//			for (int j = 0; j < data.size(); j++) {
//				
//				if (transformed_data[i].getSeries() == data.get_series(j)) {
//					node_boss_dataset.add(transformed_data[i]);
//				}
//				
//			}
//		}
		int[] indices = trainIndices.getIndex();
		for (int j = 0; j < indices.length; j++) {
			node_boss_dataset.add(transformed_data[indices[j]]);
		}
		
		
		//split by class
		TIntObjectMap<List<BagOfPattern>> boss_data_per_class = split_by_class(node_boss_dataset);
		
		
		//extract node data;
		//pick one random example per class
		bopPerClass = new TIntObjectHashMap<>();
		for (int key : boss_data_per_class.keys()) {
			r = AppConfig.getRand().nextInt(boss_data_per_class.get(key).size());
			BagOfPattern example = boss_data_per_class.get(key).get(r);	
			bopPerClass.put(key, example);
		}
		
		
		//for each instance at node, find the closest distance to examplar
		long distance = Long.MAX_VALUE;
		double minWGini = Double.MAX_VALUE;
		
		TIntObjectMap<Dataset> splits = new TIntObjectHashMap<Dataset>();

		for (int j = 0; j < node_boss_dataset.size(); j++) {
			long minDistance = Long.MAX_VALUE;
			int min_key = -1;
			closest_nodes.clear();
			
			for (int key : bopPerClass.keys()) {
				splits.putIfAbsent(key, new MTSDataset());
				
				BagOfPattern bag1_query = node_boss_dataset.get(j);

				BagOfPattern bag2 = bopPerClass.get(key);
				
				if (bag1_query == bag2) {
//					minDistance = 0; //TODO
					min_key = key;
					closest_nodes.clear();
					closest_nodes.add(min_key);
					break;
				}
				
				distance = BossDistance(bag1_query, bag2);
				
//				System.out.println("boss dist - query: " + bag1_query.toString());
//				System.out.println("boss dist - bag2: " + bag2.toString());
//				System.out.println("boss dist: " + distance + ", to class " + key);

			
				if (distance < minDistance) {
					minDistance = distance;
					min_key = key;
					closest_nodes.clear();
					closest_nodes.add(min_key);
				}else if (distance == minDistance) {
	//				if (distance == min_distance) {
	//					System.out.println("min distances are same " + distance + ":" + min_distance);
	//				}
					minDistance = distance;
					closest_nodes.add(key);
				}				
			}


			r = AppConfig.getRand().nextInt(closest_nodes.size());	//TODO may be use SplitRandom??
			min_key =  closest_nodes.get(r);
			
			//put n to bin with min distance
			splits.get(min_key).add(node_boss_dataset.get(j).getSeries());
//			System.out.println("min distance to key; " + min_key + ", actual class " + node_boss_dataset.get(j).getSeries().getLabel() 
//					+ " splits: " + splits.toString());
//			int asdsa = 0;
//			asdsa++;
		}
		
		double wgini = Util.weighted_gini(splits, data.size());
		
//		System.out.println("wgini:" + wgini + "  " + splits);
		
		//TODO temp
		this.best_splits = splits;

	} //end func
	
	
	
	
	
	
	
	
	protected int[][] createWords(final TimeSeries[] samples, SFA sfa, int wordlen, int alphabet) {

		final int[][] words = new int[samples.length][];

		for (int i = 0; i < samples.length; i++) {
			short[][] sfaWords = sfa.transformWindowing(samples[i]);
			words[i] = new int[sfaWords.length];
			for (int j = 0; j < sfaWords.length; j++) {
				words[i][j] = (int) Words.createWord(sfaWords[j], wordlen, (byte) Words.binlog(alphabet));
			}
		}
		return words;
	}
	
	protected int[][] createWords(final TimeSeries[] samples, SFA sfa, int window, int wordlen, int alphabet,
                                  boolean norm, boolean lowerbound) {

		final int[][] words = new int[samples.length][];

		if (sfa == null) {
			// TODO
//	    	sfa = new SFA(HistogramType.EQUI_DEPTH);
//	    	sfa.fitWindowing(samples, window, wordlen, alphabet, norm, true);
		}

		for (int i = 0; i < samples.length; i++) {
			short[][] sfaWords = sfa.transformWindowing(samples[i]);
			words[i] = new int[sfaWords.length];
			for (int j = 0; j < sfaWords.length; j++) {
				words[i][j] = (int) Words.createWord(sfaWords[j], wordlen, (byte) Words.binlog(alphabet));
			}
		}
		return words;
	}
	
	
	public static BagOfPattern createBagOfPattern(final int[] words, final TimeSeries sample, final int wordLength, int symbols) {
		BagOfPattern bagOfPatterns;

		final byte usedBits = (byte) Words.binlog(symbols);
		// FIXME
		// final long mask = (usedBits << wordLength) - 1l;
		final long mask = (1L << (usedBits * wordLength)) - 1L;

		// iterate all samples
		bagOfPatterns = new BagOfPattern(words.length, sample); 
																						

		// create subsequences
		long lastWord = Long.MIN_VALUE;

		for (int offset = 0; offset < words.length; offset++) {
			// use the words of larger queryLength to get words of smaller lengths
			long word = words[offset] & mask;
			if (word != lastWord) { // ignore adjacent samples

				bagOfPatterns.bag.putOrAdd((int) word, (short) 1, (short) 1);
			}
			lastWord = word;
		}

		return bagOfPatterns;
	}
	
	  /**
	   * Create the BOSS boss for a fixed window-queryLength and SFA word queryLength
	   *
	   * @param words      the SFA words of the time series
	   * @param samples    the samples to be transformed
	   * @param wordLength the SFA word queryLength
	   * @return returns a BOSS boss for each time series in samples
	   */
	  public static BagOfPattern[] createBagOfPattern(
	      final int[][] words,
	      final TimeSeries[] samples,
	      final int wordLength,
	      final int symbols) {
	    BagOfPattern[] bagOfPatterns = new BagOfPattern[words.length];

	    final byte usedBits = (byte) Words.binlog(symbols);
	    // FIXME
	    // final long mask = (usedBits << wordLength) - 1l;
	    final long mask = (1L << (usedBits * wordLength)) - 1L;

	    // iterate all samples
	    for (int j = 0; j < words.length; j++) {
//	      bagOfPatterns[j] = new BagOfPattern(words[j].length, samples[j].getLabel());
	      bagOfPatterns[j] = new BagOfPattern(words[j].length, samples[j]); //TODO changed to time series

	      // create subsequences
	      long lastWord = Long.MIN_VALUE;

	      for (int offset = 0; offset < words[j].length; offset++) {
	        // use the words of larger queryLength to get words of smaller lengths
	        long word = words[j][offset] & mask;
	        if (word != lastWord) { // ignore adjacent samples
	        	
	          bagOfPatterns[j].getBag().putOrAdd((int) word, (short) 1, (short) 1); 
	        }
	        lastWord = word;
	      }
	    }

	    return bagOfPatterns;
	  }
	

//	private void train_hist_per_class(TSDataset data) throws Exception {
//		TIntObjectMap<TSDataset> data_per_class = data.split_by_class(); // TODO can be done once at node
//		training_class_dist = data.get_class_map();
//
//		int maxWindowLength = Math.max(data.length(), MAX_WINDOW_LENGTH);
//
//		Integer[] windows = getWindowsBetween(minWindowLength, maxWindowLength);
//
//		long distance = Long.MAX_VALUE;
//		double minWGini = Double.MAX_VALUE;
//		
//
//
//		//TODO mcb should be on whole dataset
//		//move to forest
//		
//		//TODO we may split using more data than train amount which is why its split again in another func
//		
//		
//
//		bossModelPerClass = new TIntObjectHashMap<>();
//		bossPerClass = new TIntObjectHashMap<>();
//		bopPerClass = new TIntObjectHashMap<>();
//		
//		for (boolean normMean : NORMALIZATION) {
//			for (int i = 0; i < windows.length; i++) {
//				for (int f = minF; f <= maxF; f += 2) {
//					for (int key : data_per_class.keys()) {
//						
//						BOSSModel currentBossModel = new BOSSModel(normMean, windows[i]);
//						bossModelPerClass.put(key, currentBossModel);
//
//						TimeSeries[] samples1 = data_per_class.get(key)._get_internal_list().toArray(new TimeSeries[] {});
//						BOSS boss = new BOSS(maxF, maxS, windows[i], normMean);
//						bossPerClass.put(key, boss);
//
//						int[][] words = boss.createWords(samples1);
//						BagOfPattern[] b1 = boss.createBagOfPattern(words, samples1, f);
//						bopPerClass.put(key, b1[0]);
//					}
//					
//					TIntObjectMap<TSDataset> splits = new TIntObjectHashMap<TSDataset>();
//					for (int j = 0; j < data.size(); j++) {
//						long minDistance = Long.MAX_VALUE;
//						int min_key = -1;
//						
//						for (int key : data_per_class.keys()) {
//							splits.putIfAbsent(key, new TSDataset());
//							
//							BOSS boss = new BOSS(maxF, maxS, windows[i], normMean);
//							
//							TimeSeries[] samples2 = new TimeSeries[1];
//							samples2[0] = data.get_series(j);
//							
//							int[][] words = boss.createWords(samples2);
//							BagOfPattern[] b2 = boss.createBagOfPattern(words, samples2, f);
//
//							distance = BossDistance(bopPerClass.get(key), b2[0]);
//							
//							//TODO if dist == random select
//							if (distance < minDistance) {
//								minDistance = distance;
//								min_key = key;
//							}
//						}
//						
//						//put n to bin with min distance
//						splits.get(min_key).add(data.get_series(j));
//					}
//					
//					double wgini = Util.weighted_gini(splits, data.size());
//					
//					System.out.println("wgini: " + wgini + splits);
//
//					if (wgini < minWGini) {
//						minWGini =  wgini;
//						bestF = f;
//						bestWindow = windows[i];
//						bestNorm = normMean;
//					}
//
//				} //word len
//			}
//		}
//		
//	}
	
	
	@Override
	public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		long startTime = System.nanoTime();
		
//		TIntObjectMap<TSDataset> data_per_class = data.split_by_class(); // TODO can be done once at node
//		TIntObjectMap<TSDataset> splits = new TIntObjectHashMap<TSDataset>();
//		long distance = Integer.MAX_VALUE;
//		
//		SFA sfa = sfa_transforms.get(best_hist_key);
//
//		for (int j = 0; j < data.size(); j++) {
//			long minDistance = Long.MAX_VALUE;
//			int min_key = -1;
//			
//			for (int key : data_per_class.keys()) {
//				splits.putIfAbsent(key, new TSDataset());
//				
//				TimeSeries series = data.get_series(j);
//				
//				
//				short[][] sfaWords = sfa.transformWindowing(series);
//				int[] words = new int[sfaWords.length];
//				
//				for (int k = 0; k < sfaWords.length; k++) {
//					words[k] = (int) Words.createWord(sfaWords[k], bestWindow, (byte) Words.binlog(maxS));
//				}
//				BagOfPattern bag2 = createBagOfPattern(words, series, bestF, symbols);
//				
//				distance = BossDistance(bag2, bopPerClass.get(key));
//				
//				//TODO if dist == random select
//				//TODO if using two functions and if using randomness when = make sure this split
//				//is equivalent/comparable to best split we got before?? 
//				if (distance < minDistance) {
//					minDistance = distance;
//					min_key = key;
//				}
//			}
//			
//			//put n to bin with min distance
//			splits.get(min_key).add(data.get_series(j));
//		}


		//time is measured separately for split function from train function as it can be called separately -- to prevent double counting 
		this.node.tree.stats.boss_splitter_train_time += (System.nanoTime() - startTime);
		
//		return splits;
		return best_splits;	//TODO
	}

//	@Override
//	public int predict(TimeSeries query, int queryIndex) throws Exception {
//		long minDistance = Integer.MAX_VALUE;
//		long distance = Integer.MAX_VALUE;
//		int min_key = 0; //TODO
//
//		BagOfPattern query_hist = this.node.tree.boss_dataset.getTransformed_data()[queryIndex];
//		
//		
//		for (int key : bopPerClass.keys()) {
//			BagOfPattern example_hist = bopPerClass.get(key);
//
//			distance = BossDistance(query_hist, example_hist);
//			
//			//TODO if dist == random select
//			if (distance < minDistance) {
//				minDistance = distance;
//				min_key = key;
//			}			
//		}
//
//
//		return min_key;
//	}
//	
	
	List<Integer> closest_nodes = new ArrayList<Integer>();

	@Override
	public int predict(TimeSeries query, Dataset testData, int queryIndex) throws Exception {
		long minDistance = Integer.MAX_VALUE;
		long distance = Integer.MAX_VALUE;
		int min_key = 0; //TODO
		
		if (AppConfig.boss_split_method == SplitMethod.RandomTreeStyle) {
			return predict_gini_split(query, queryIndex);
		}

		BagOfPattern query_hist;

		if (AppConfig.boss_transform_level == TransformLevel.Forest) {
			BossTransformContainer transforms = ((BossTransformContainer)this.node.tree.getForest().getTransforms().get("boss"));
			query_hist = transforms.transform_series_using_sfa(query, transforms.sfa_transforms.get(forest_transform_params.toString()));
		}else {
			query_hist = this.node.tree.treeLevelBossTestData.getTransformed_data()[queryIndex];
		}
		
		closest_nodes.clear();

		
		for (int key : bopPerClass.keys()) {
			BagOfPattern example_hist = bopPerClass.get(key);

			distance = BossDistance(query_hist, example_hist);
			
			if (distance < minDistance) {
				minDistance = distance;
				min_key = key;
				closest_nodes.clear();
				closest_nodes.add(min_key);
			}else if (distance == minDistance) {
//				if (distance == min_distance) {
//					System.out.println("min distances are same " + distance + ":" + min_distance);
//				}
				minDistance = distance;
				closest_nodes.add(key);
			}
		}


		int r = AppConfig.getRand().nextInt(closest_nodes.size());	//TODO may be use SplitRandom??
		return closest_nodes.get(r);
	}

	@Override
	public TIntObjectMap<TIntArrayList> fitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
		throw new NotSupportedException();
	}

	@Override
	public TIntObjectMap<TIntArrayList> splitByIndices(Dataset allTrainData, Indexer trainIndices) throws Exception {
		throw new NotSupportedException();
	}


	private void train_one_ensemble(MTSDataset data) throws Exception {
		bossEns = new BOSSEnsembleClassifier();

		// NOTE sample_size = const or sample_size = const * #class_at_root
		int sample_size = 5;

		Dataset sample = Sampler.uniform_sample(data, sample_size);

		TimeSeries[] sample_array = sample.toArray();

		bossEns.fit(sample_array);

		// TODO remove evaluate
		Double[] labels = bossEns.predict(sample_array);

	}

	private void train_one_ensemble_per_class(Dataset data) throws Exception {
		TIntObjectMap<Dataset> data_per_class = data.splitByClass(); // TODO can be done once at node

		int no_samples_per_class = 2;
		TIntObjectMap<MTSDataset> samples_per_class = new TIntObjectHashMap<MTSDataset>();

		for (int key : data_per_class.keys()) {
			BOSSEnsembleClassifier currentBossEns = new BOSSEnsembleClassifier();

			bossEnsPerClass.put(key, currentBossEns);
			samples_per_class.put(key, new MTSDataset());

			int size = Math.min(no_samples_per_class, data_per_class.get(key).size());

			for (int i = 0; i < size; i++) {
				samples_per_class.get(key).add(data_per_class.get(key).getSeries(i));
			}

			TimeSeries[] sample_array = samples_per_class.get(key)._get_internal_list().toArray(new MTimeSeries[] {});

			currentBossEns.fit(sample_array);

			// TODO remove evaluate
			Double[] labels = currentBossEns.predict(sample_array);
		}

	}



	// utils
	protected Integer[] getWindowsBetween(int minWindowLength, int maxWindowLength) {
		List<Integer> windows = new ArrayList<>();
		for (int windowLength = maxWindowLength; windowLength >= minWindowLength; windowLength--) {
			windows.add(windowLength);
		}
		return windows.toArray(new Integer[] {});
	}

	protected boolean compareLabels(Double label1, Double label2) {
		// compare 1.0000 to 1.0 in String returns false, hence the conversion to double
		return label1 != null && label2 != null && label1.equals(label2);
	}
	
	protected long BossDistance(BagOfPattern query, BagOfPattern b2) {

		long distance = 0;
		for (IntIntCursor key : query.getBag()) {
			long buf = key.value - b2.getBag().get(key.key);
			distance += buf * buf;				
		}

		return distance;
	}
	
	
//	protected long BossDistance(BagOfPattern b1, BagOfPattern b2){
//		long minDistance = Integer.MAX_VALUE;
//
//		// Distance if there is no matching word
//		double noMatchDistance = 0.0;
//		for (IntIntCursor key : bagOfPatternsTestSamples[i].bag) {
//			noMatchDistance += key.value * key.value;
//		}
//		
//		nnSearch: for (int j = 0; j < bagOfPatternsTrainSamples.length; j++) {
//			if (bagOfPatternsTestSamples[i] != bagOfPatternsTrainSamples[j]) {
//				// determine distance
//				long distance = 0;
//				for (IntIntCursor key : bagOfPatternsTestSamples[i].bag) {
//					long buf = key.value - bagOfPatternsTrainSamples[j].bag.get(key.key);
//					distance += buf * buf;
//
//					if (distance >= minDistance) {
//						continue nnSearch;
//					}
//				}
//
//				// update nearest neighbor
//				if (distance != noMatchDistance && distance < minDistance) {
//					minDistance = distance;
//					p.labels[i] = bagOfPatternsTrainSamples[j].label;
//				}
//			}
//		}
//		
//		
//	}

	protected Predictions predict(final BagOfPattern[] bagOfPatternsTestSamples,
			final BagOfPattern[] bagOfPatternsTrainSamples) {

		Predictions p = new Predictions(new Double[bagOfPatternsTestSamples.length], 0);

		// iterate each sample to classify
		for (int i = 0; i < bagOfPatternsTestSamples.length; i++) {
			long minDistance = Integer.MAX_VALUE;

			// Distance if there is no matching word
			double noMatchDistance = 0.0;
			for (IntIntCursor key : bagOfPatternsTestSamples[i].bag) {
				noMatchDistance += key.value * key.value;
			}

			nnSearch: for (int j = 0; j < bagOfPatternsTrainSamples.length; j++) {
				if (bagOfPatternsTestSamples[i] != bagOfPatternsTrainSamples[j]) {
					// determine distance
					long distance = 0;
					for (IntIntCursor key : bagOfPatternsTestSamples[i].bag) {
						long buf = key.value - bagOfPatternsTrainSamples[j].bag.get(key.key);
						distance += buf * buf;

						if (distance >= minDistance) {
							continue nnSearch;
						}
					}

					// update nearest neighbor
					if (distance != noMatchDistance && distance < minDistance) {
						minDistance = distance;
						p.labels[i] = bagOfPatternsTrainSamples[j].getLabel().doubleValue();
					}
				}
			}

			// check if the prediction is correct
			if (compareLabels(bagOfPatternsTestSamples[i].getLabel().doubleValue(), p.labels[i])) {
				p.correct.incrementAndGet();
			}
		}

		return p;
	}

	public static String toSfaWord(short[] word, int symbols) {
		StringBuilder sfaWord = new StringBuilder();

		for (short c : word) {
			sfaWord.append((char) (Character.valueOf('a') + c));
		}

		return sfaWord.toString();
	}
	
	public String toString() {
		return "BossSplitter[ForestParam:" + forest_transform_params+ "]";
	}
	
}
