package trees.splitters.old;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.carrotsearch.hppc.cursors.IntIntCursor;

import core.Classifier.Predictions;
import core.threading.ParallelFor;
import data.timeseries.TimeSeries;
import data.timeseries.UTSDataset;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSCheifTree.Node;
import trees.splitters.boss.BOSS;
import trees.splitters.boss.SFA;
import trees.splitters.boss.BOSS.BagOfPattern;
import trees.splitters.boss.SFA.HistogramType;
import trees.splitters.boss.dev.BOSSEnsembleClassifier;
import trees.splitters.boss.dev.BOSSEnsembleClassifier.BOSSModel;
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
	
	BossSplitter() {

	}

	public BossSplitter(Node node) {
		// TODO Auto-generated constructor stub

	}

	@Override
	public void train(UTSDataset data) throws Exception {

//		if (use_ensemble_per_class) {
//			train_one_ensemble(data);
//		}else {
//			train_one_ensemble_per_class(data);
//		}

		train_hist_per_class(data);

	}

	private void train_hist_per_class(UTSDataset data) throws Exception {
		TIntObjectMap<UTSDataset> data_per_class = data.split_by_class(); // TODO can be done once at node
		training_class_dist = data.get_class_map();

		int maxWindowLength = Math.max(data.length(), MAX_WINDOW_LENGTH);

		Integer[] windows = getWindowsBetween(minWindowLength, maxWindowLength);

		long distance = Long.MAX_VALUE;
		double minWGini = Double.MAX_VALUE;
		


		//TODO mcb should be on whole dataset
		//move to forest
		
		//TODO we may split using more data than train amount which is why its split again in another func
		
		

		bossModelPerClass = new TIntObjectHashMap<>();
		bossPerClass = new TIntObjectHashMap<>();
		bopPerClass = new TIntObjectHashMap<>();
		
		for (boolean normMean : NORMALIZATION) {
			for (int i = 0; i < windows.length; i++) {
				for (int f = minF; f <= maxF; f += 2) {
					for (int key : data_per_class.keys()) {
						
						BOSSModel currentBossModel = new BOSSModel(normMean, windows[i]);
						bossModelPerClass.put(key, currentBossModel);

						TimeSeries[] samples1 = data_per_class.get(key)._get_internal_list().toArray(new TimeSeries[] {});
						BOSS boss = new BOSS(maxF, maxS, windows[i], normMean);
						bossPerClass.put(key, boss);

						int[][] words = boss.createWords(samples1);
						BagOfPattern[] b1 = boss.createBagOfPattern(words, samples1, f);
						bopPerClass.put(key, b1[0]);
					}
					
					TIntObjectMap<UTSDataset> splits = new TIntObjectHashMap<UTSDataset>();
					for (int j = 0; j < data.size(); j++) {
						long minDistance = Long.MAX_VALUE;
						int min_key = -1;
						
						for (int key : data_per_class.keys()) {
							splits.putIfAbsent(key, new UTSDataset());
							
							BOSS boss = new BOSS(maxF, maxS, windows[i], normMean);
							
							TimeSeries[] samples2 = new TimeSeries[1];
							samples2[0] = data.get_series(j);
							
							int[][] words = boss.createWords(samples2);
							BagOfPattern[] b2 = boss.createBagOfPattern(words, samples2, f);

							distance = BossDistance(bopPerClass.get(key), b2[0]);
							
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
					
					System.out.println("wgini: " + wgini + splits);

					if (wgini < minWGini) {
						minWGini =  wgini;
						bestF = f;
						bestWindow = windows[i];
						bestNorm = normMean;
					}

				} //word len
			}
		}
		
	}
	
	
	
	@Override
	public TIntObjectMap<UTSDataset> split(UTSDataset data) throws Exception {
		TIntObjectMap<UTSDataset> splits = new TIntObjectHashMap<UTSDataset>();
		long distance = Integer.MAX_VALUE;

		for (int j = 0; j < data.size(); j++) {
			long minDistance = Integer.MAX_VALUE;
			int min_key = -1;
			
			
			//TODO training_class_dist?? if train data != split data ??
			for (int key : data.get_class_map().keys()) {
				splits.put(key, new UTSDataset());
				
				BOSS boss = new BOSS(bestF, maxS, bestWindow, normMean);
				
				TimeSeries[] samples2 = new TimeSeries[1];
				samples2[0] = data.get_series(j);
				
				int[][] words = boss.createWords(samples2);
				BagOfPattern[] b2 = boss.createBagOfPattern(words, samples2, bestF);

				distance = BossDistance(bopPerClass.get(key), b2[0]);
				
				if (distance < minDistance) {
					minDistance = distance;
					min_key = key;
				}
			}
			
			//put n to bin with min distance
			splits.get(min_key).add(data.get_series(j));
		}
		

		return splits;
	}

	@Override
	public int predict(TimeSeries query) throws Exception {
		long minDistance = Integer.MAX_VALUE;
		long distance = Integer.MAX_VALUE;
		int min_key = 0; //TODO

		//TODO training_class_dist?? if train data != split data ??

		for (int key : training_class_dist.keys()) {

			TimeSeries[] samples2 = new TimeSeries[1];
			samples2[0] = query;
			
			BOSS boss = new BOSS(bestF, maxS, bestWindow, normMean);
			int[][] words = boss.createWords(samples2);
			BagOfPattern[] b2 = boss.createBagOfPattern(words, samples2, bestF);

			distance = BossDistance(bopPerClass.get(key), b2[0]);
			
			if (distance < minDistance) {
				minDistance = distance;
				min_key = key;
			}
		}

		return min_key;
	}
	
	
	
	
	
	
	
	
	
	

	private void train_one_ensemble(UTSDataset data) throws Exception {
		bossEns = new BOSSEnsembleClassifier();

		// NOTE sample_size = const or sample_size = const * #class_at_root
		int sample_size = 5;

		UTSDataset sample = Sampler.uniform_sample(data, sample_size);

		TimeSeries[] sample_array = sample._get_internal_list().toArray(new TimeSeries[] {});

		bossEns.fit(sample_array);

		// TODO remove evaluate
		Double[] labels = bossEns.predict(sample_array);

	}

	private void train_one_ensemble_per_class(UTSDataset data) throws Exception {
		TIntObjectMap<UTSDataset> data_per_class = data.split_by_class(); // TODO can be done once at node

		int no_samples_per_class = 2;
		TIntObjectMap<UTSDataset> samples_per_class = new TIntObjectHashMap<UTSDataset>();

		for (int key : data_per_class.keys()) {
			BOSSEnsembleClassifier currentBossEns = new BOSSEnsembleClassifier();

			bossEnsPerClass.put(key, currentBossEns);
			samples_per_class.put(key, new UTSDataset());

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

}
