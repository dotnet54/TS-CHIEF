package dev;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.carrotsearch.hppc.cursors.IntIntCursor;

import core.AppContext;
import core.ParallelFor;
import datasets.TSDataset;
import datasets.TimeSeries;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import transforms.BOSS;
import transforms.SFA;
import transforms.BOSS.BagOfPattern;
import transforms.SFA.HistogramType;
import transforms.SFA.Words;
import trees.ProximityTree.Node;
import trees.splitters.NodeSplitter;
import util.Sampler;
import util.Util;

public class BossSplitterPerNode implements NodeSplitter {

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
	
	Node node;
	HashMap<String, SFA> sfa_transforms;
	String best_hist_key;
	TIntObjectMap<TSDataset> best_splits = null; //TODO
	

	public BossSplitterPerNode(Node node) {
		// TODO Auto-generated constructor stub
		this.node = node;
		this.node.tree.stats.boss_count++;

		sfa_transforms = ((BossTransformContainer)
				this.node.tree.getForest().getTransforms().get("boss")).getSFATransforms();
	}

	@Override
	public void train(TSDataset data, int[] indices) throws Exception {

//		if (use_ensemble_per_class) {
//			train_one_ensemble(data);
//		}else {
//			train_one_ensemble_per_class(data);
//		}

		train_hist_using_forest_transform(data);

	}
	
	@Override
	public void train_binary(TSDataset data) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@SuppressWarnings("unchecked")
	private void train_hist_using_forest_transform(TSDataset data) throws Exception {

//		HashMap<String, BagOfPattern[]> histograms = ((BossTransformContainer)
//				this.node.tree.getForest().getTransforms().get("boss")).getHistograms();
		

		
		TIntObjectMap<TSDataset> data_per_class = data.split_by_class(); // TODO can be done once at node
		training_class_dist = data.get_class_map();

		int maxWindowLength = Math.min(data.length(), MAX_WINDOW_LENGTH);

		Integer[] windows = getWindowsBetween(minWindowLength, maxWindowLength);

		long distance = Long.MAX_VALUE;
		double minWGini = Double.MAX_VALUE;
		String hist_key = null;


		//TODO mcb should be on whole dataset
		//move to forest
		
		//TODO we may split using more data than train amount which is why its split again in another func
		

		bossModelPerClass = new TIntObjectHashMap<>();
		bossPerClass = new TIntObjectHashMap<>();
		bopPerClass = new TIntObjectHashMap<>();
		
		TIntObjectHashMap<TimeSeries> examples = new TIntObjectHashMap<TimeSeries>();
		
		
		for (int key : data_per_class.keys()) {
			int r = AppContext.getRand().nextInt(data_per_class.get(key).size());
			TimeSeries example = data_per_class.get(key).get_series(r);	
			examples.put(key, example);
		}

		
		for (boolean normMean : NORMALIZATION) {
			for (int i = 0; i < windows.length; i++) {
				for (int f = minF; f <= maxF; f += 2) {
					
					hist_key = "n:" + normMean + "-" + windows[i] + "-" + f;
					SFA sfa = sfa_transforms.get(hist_key);
					
					if (sfa == null) {
						System.out.println("sfa not pre trained");
					}
					
					for (int key : examples.keys()) {

						short[][] sfaWords = sfa.transformWindowing(examples.get(key));
						int[] words = new int[sfaWords.length];
						
						for (int j = 0; j < sfaWords.length; j++) {
							words[j] = (int) Words.createWord(sfaWords[j], windows[i], (byte) Words.binlog(maxS));
						}
						BagOfPattern bag = createBagOfPattern(words, examples.get(key), f, symbols);
						bopPerClass.put(key, bag);
					}
					
					
					TIntObjectMap<TSDataset> splits = new TIntObjectHashMap<TSDataset>();
					
					for (int j = 0; j < data.size(); j++) {
						long minDistance = Long.MAX_VALUE;
						int min_key = -1;
						
						for (int key : examples.keys()) {
							splits.putIfAbsent(key, new TSDataset());
							
							TimeSeries series = data.get_series(j);
							
							short[][] sfaWords = sfa.transformWindowing(series);
							int[] words = new int[sfaWords.length];
							
							for (int k = 0; k < sfaWords.length; k++) {
								words[k] = (int) Words.createWord(sfaWords[k], windows[i], (byte) Words.binlog(maxS));
							}
							BagOfPattern bag2 = createBagOfPattern(words, series, f, symbols);
							
							distance = BossDistance(bag2, bopPerClass.get(key));
							
							//TODO if dist == random select
							if (distance < minDistance) {
								minDistance = distance;
								min_key = key;
							}
						}
						
						//put n to bin with min distance
						splits.get(min_key).add(data.get_series(j));
					}
					
					double wgini = Util.weighted_gini(splits, data.size());
					
					System.out.println("wgini: " + wgini + splits + " " + hist_key);

					if (wgini < minWGini) {
						minWGini =  wgini;
						bestF = f;
						bestWindow = windows[i];
						bestNorm = normMean;
						this.best_hist_key = hist_key;
						best_splits = splits;
					}

				} //word len
			} // windows
		} //norm
		
		System.out.println("best wgini: " + minWGini + best_splits + " " + best_hist_key);
		
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
		bagOfPatterns = new BagOfPattern(words.length, sample.getLabel().doubleValue()); // TODO int to double
																									// label

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
	      bagOfPatterns[j] = new BagOfPattern(words[j].length, samples[j].getLabel().doubleValue()); //TODO int to double label

	      // create subsequences
	      long lastWord = Long.MIN_VALUE;

	      for (int offset = 0; offset < words[j].length; offset++) {
	        // use the words of larger queryLength to get words of smaller lengths
	        long word = words[j][offset] & mask;
	        if (word != lastWord) { // ignore adjacent samples
	        	
	          bagOfPatterns[j].bag.putOrAdd((int) word, (short) 1, (short) 1); 
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
	
	
	//TODO NOTE this method is different to train because data subset may be used to train
	@Override
	public TIntObjectMap<TSDataset> split(TSDataset data, int[] indices) throws Exception {
		TIntObjectMap<TSDataset> data_per_class = data.split_by_class(); // TODO can be done once at node
		TIntObjectMap<TSDataset> splits = new TIntObjectHashMap<TSDataset>();
		long distance = Integer.MAX_VALUE;
		
		SFA sfa = sfa_transforms.get(best_hist_key);

		for (int j = 0; j < data.size(); j++) {
			long minDistance = Long.MAX_VALUE;
			int min_key = -1;
			
			for (int key : data_per_class.keys()) {
				splits.putIfAbsent(key, new TSDataset());
				
				TimeSeries series = data.get_series(j);
				
				
				short[][] sfaWords = sfa.transformWindowing(series);
				int[] words = new int[sfaWords.length];
				
				for (int k = 0; k < sfaWords.length; k++) {
					words[k] = (int) Words.createWord(sfaWords[k], bestWindow, (byte) Words.binlog(maxS));
				}
				BagOfPattern bag2 = createBagOfPattern(words, series, bestF, symbols);
				
				distance = BossDistance(bag2, bopPerClass.get(key));
				
				//TODO if dist == random select
				//TODO if using two functions and if using randomness when = make sure this split
				//is equivalent/comparable to best split we got before?? 
				if (distance < minDistance) {
					minDistance = distance;
					min_key = key;
				}
			}
			
			//put n to bin with min distance
			splits.get(min_key).add(data.get_series(j));
		}
		
		
//		return splits;
		return best_splits;
	}

	@Override
	public int predict(TimeSeries query, int queryIndex) throws Exception {
		long minDistance = Integer.MAX_VALUE;
		long distance = Integer.MAX_VALUE;
		int min_key = 0; //TODO
		SFA sfa = sfa_transforms.get(best_hist_key);

		//TODO training_class_dist?? if train data != split data ??

		short[][] sfaWords = sfa.transformWindowing(query);
		int[] words = new int[sfaWords.length];
		
		for (int k = 0; k < sfaWords.length; k++) {
			words[k] = (int) Words.createWord(sfaWords[k], bestWindow, (byte) Words.binlog(maxS));
		}
		BagOfPattern bag2 = createBagOfPattern(words, query, bestF, symbols);


		
		for (int key : bopPerClass.keys()) {
			BagOfPattern bag1 = bopPerClass.get(key);
			
		
//	        // Distance if there is no matching word
//	        double noMatchDistance = 0.0;
//	        for (IntIntCursor key : bagOfPatternsTestSamples[i].bag) {
//	          noMatchDistance += key.value * key.value;
//	        }			
			
			distance = BossDistance(bag2, bag1);
			
			//TODO if dist == random select
			if (distance < minDistance) {
				minDistance = distance;
				min_key = key;
			}			
		}


		return min_key;
	}
	
	
	
	
	
	
	
	
	
	

	private void train_one_ensemble(TSDataset data) throws Exception {
		bossEns = new BOSSEnsembleClassifier();

		// NOTE sample_size = const or sample_size = const * #class_at_root
		int sample_size = 5;

		TSDataset sample = Sampler.uniform_sample(data, sample_size);

		TimeSeries[] sample_array = sample._get_internal_list().toArray(new TimeSeries[] {});

		bossEns.fit(sample_array);

		// TODO remove evaluate
		Double[] labels = bossEns.predict(sample_array);

	}

	private void train_one_ensemble_per_class(TSDataset data) throws Exception {
		TIntObjectMap<TSDataset> data_per_class = data.split_by_class(); // TODO can be done once at node

		int no_samples_per_class = 2;
		TIntObjectMap<TSDataset> samples_per_class = new TIntObjectHashMap<TSDataset>();

		for (int key : data_per_class.keys()) {
			BOSSEnsembleClassifier currentBossEns = new BOSSEnsembleClassifier();

			bossEnsPerClass.put(key, currentBossEns);
			samples_per_class.put(key, new TSDataset());

			int size = Math.min(no_samples_per_class, data_per_class.get(key).size());

			for (int i = 0; i < size; i++) {
				samples_per_class.get(key).add(data_per_class.get(key).get_series(i));
			}

			TimeSeries[] sample_array = samples_per_class.get(key)._get_internal_list().toArray(new TimeSeries[] {});

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
	
	protected long BossDistance(BagOfPattern b1, BagOfPattern b2) {
		long minDistance = Integer.MAX_VALUE;

		long distance = 0;
		for (IntIntCursor key : b1.bag) {
			long buf;
			
			if (key.value != 0) {
				buf = key.value - b2.bag.get(key.key);
				distance += buf * buf;				
			}
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
						p.labels[i] = bagOfPatternsTrainSamples[j].label;
					}
				}
			}

			// check if the prediction is correct
			if (compareLabels(bagOfPatternsTestSamples[i].label, p.labels[i])) {
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

	
}
