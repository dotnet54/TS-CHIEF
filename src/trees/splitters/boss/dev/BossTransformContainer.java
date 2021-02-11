package trees.splitters.boss.dev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import application.AppConfig;
import core.TransformContainer;
import data.timeseries.MTimeSeries;
import data.timeseries.Dataset;
import data.timeseries.TimeSeries;
import trees.splitters.boss.BossSplitter;
import trees.splitters.boss.SFA;
import trees.splitters.boss.BOSS.BagOfPattern;
import trees.splitters.boss.SFA.HistogramType;
import trees.splitters.boss.SFA.Words;
import trees.splitters.boss.dev.BossDataset.BossParams;

public class BossTransformContainer implements TransformContainer {

	int alphabetSize = 4;
	int minWindowLength = 10;
	int maxWindowLength = 0;	//if 0 then assume maxWindowLength == length of series
	int windowLengthStepSize = 1;

	int minWordLength = 6; //TODO put this to appContext
	int maxWordLength = 16;
	int wordLengthStepSize = 2;
	
	boolean use_lower_bound = true; //TODO not implemented?? CHECK
	
	boolean[] NORMALIZATION = new boolean[] { true, false };

	public HashMap<String, SFA> sfa_transforms;	//change String keys to Int keys
	public HashMap<String, BossDataset> boss_datasets;	//change String keys to Int keys
	public List<BossDataset.BossParams> boss_params;
	
	int num_transforms;
	
//	SplittableRandom rand = new SplittableRandom();
	Random rand = AppConfig.getRand();

	public BossTransformContainer(int boss_num_transforms) {
		sfa_transforms = new HashMap<String, SFA>();
		boss_datasets = new HashMap<String, BossDataset>(boss_num_transforms);
		boss_params = new ArrayList<>(boss_num_transforms);	//TODO not necessary
		num_transforms = boss_num_transforms;
		
		
	}
	
	
	
	
	//hacky implementation TODO refactor later
	private ArrayList<Integer> selectRandomParams(Dataset train) {
		
		if (maxWindowLength == 0 || maxWindowLength > train.length()) {
			maxWindowLength = train.length();
		}
		
		List<BossDataset.BossParams> all_params = new ArrayList<>();
		
		int[] wordLengths = getBossWordLengths();
		
		for (boolean normMean : NORMALIZATION) {
			for (int w = minWindowLength; w < maxWindowLength; w+=windowLengthStepSize) {
				for (int l : wordLengths) {
					all_params.add(new BossParams(normMean, w, l, alphabetSize));
					
				}
			}
		}
		
		//pick num_transform items randomly
		
		ArrayList<Integer> indices = new ArrayList<>(all_params.size());
		for (int i = 0; i < all_params.size(); i++) {
			indices.add(i);
		}
		
		Collections.shuffle(indices, AppConfig.getRand());
		
		//TODO if #all possible params is < num_transformation, update max limit
		if (num_transforms > all_params.size()) {
			num_transforms = all_params.size();
			if (AppConfig.verbosity > 1) {
				System.out.println("INFO: boss_num_transformation has been updated to: " + num_transforms + " (#all possible params with given settings ("+all_params.size()+") < the given num_transformations)");
			}
		}
		for (int i = 0; i < num_transforms; i++) {
			//clone
			BossParams temp = all_params.get(indices.get(i));
			
			boss_params.add(new BossParams(temp.normMean, temp.window_len, temp.word_len, temp.alphabet_size));
		};
		
		all_params = null; //let GC free this memory
		
		return indices;
	}
	

	@Override
	public void fit(Dataset train) {
//		System.out.println("computing train transformations: " + num_transforms);
		TimeSeries[] samples = train.toArray();

		selectRandomParams(train);
		
		for (int param = 0; param < boss_params.size(); param++) {
			BossDataset.BossParams temp = boss_params.get(param);
			
			SFA sfa = new SFA(HistogramType.EQUI_DEPTH);
			sfa.fitWindowing(samples, temp.window_len, temp.word_len, temp.alphabet_size, temp.normMean, use_lower_bound);
			
			BossDataset boss_dataset = transform_dataset_using_sfa(train,sfa, temp, samples);
			
			String key =  temp.toString();
			
			sfa_transforms.put(key, sfa);
			boss_datasets.put(key, boss_dataset);
			
		}
	}




	@Override
	public Dataset transform(Dataset test) {
		
		System.out.println("computing test transformations: " + num_transforms);
		TimeSeries[] samples = test.toArray();
		
		for (int param = 0; param < boss_params.size(); param++) {
			BossParams temp = boss_params.get(param);
			SFA sfa = sfa_transforms.get(temp.toString());
			BossDataset boss_dataset = transform_dataset_using_sfa(test,sfa, temp, samples);
			
			String key =  temp.toString();
			key += "_test";
			
			sfa_transforms.put(key, sfa);
			boss_datasets.put(key, boss_dataset);
		}
		return null;
	}
	
	
	//samples array is a quick fix, will remove later
	public BossDataset transform_dataset_using_sfa(Dataset dataset, SFA sfa, BossParams params, TimeSeries[] samples) {
		
		final int[][] words = new int[samples.length][];
		
        for (int i = 0; i < samples.length; i++) {
              short[][] sfaWords = sfa.transformWindowing(samples[i]);
              words[i] = new int[sfaWords.length];
              for (int j = 0; j < sfaWords.length; j++) {
                words[i][j] = (int) Words.createWord(sfaWords[j], sfa.wordLength, (byte) Words.binlog(sfa.alphabetSize));
              }
          }

		BagOfPattern[] histograms = BossSplitter.createBagOfPattern(words, samples, sfa.wordLength, sfa.alphabetSize);

		//TODO change params
		BossDataset boss_dataset = new BossDataset(dataset, histograms, params);
		
		return boss_dataset;
	}
	
	
	public BagOfPattern transform_series_using_sfa(TimeSeries series, SFA sfa) {

		final int[] words;

		short[][] sfaWords = sfa.transformWindowing(series);
		words = new int[sfaWords.length];
		for (int j = 0; j < sfaWords.length; j++) {
			words[j] = (int) Words.createWord(sfaWords[j], sfa.wordLength, (byte) Words.binlog(sfa.alphabetSize));
		}

		BagOfPattern histogram = BossSplitter.createBagOfPattern(words, series, sfa.wordLength,sfa.alphabetSize);

		return histogram;
	}
	
	public static int[] getBossWordLengths() {
		//TODO
//		for (int l = minWordLength; l <= maxWordLength; l += wordLengthStepSize) {
//
//		}
	
		return new int[]{6,8,10,12,14,16};	
	}
	
	
//	protected int[][] createWords(final TimeSeries[] samples, SFA sfa, int window, int wordlen, int alphabet,
//			boolean norm, boolean lowerbound) {
//
//		final int[][] words = new int[samples.length][];
//
//		if (sfa == null) {
//			// TODO
////	    	sfa = new SFA(HistogramType.EQUI_DEPTH);
////	    	sfa.fitWindowing(samples, window, wordlen, alphabet, norm, true);
//		}
//
//		for (int i = 0; i < samples.length; i++) {
//			short[][] sfaWords = sfa.transformWindowing(samples[i]);
//			words[i] = new int[sfaWords.length];
//			for (int j = 0; j < sfaWords.length; j++) {
//				words[i][j] = (int) Words.createWord(sfaWords[j], wordlen, (byte) Words.binlog(alphabet));
//			}
//		}
//		return words;
//	}

//	public HashMap<String, BagOfPattern[]> getHistograms() {
//		return histograms;
//	}
	
//	public HashMap<String, SFA> getSFATransforms() {
//		return sfa_list;
//	}
//	
//	@Override
//	public TSDataset transform() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	// utils
//	protected Integer[] getWindowsBetween(int minWindowLength, int maxWindowLength) {
//		List<Integer> windows = new ArrayList<>();
//		for (int windowLength = maxWindowLength; windowLength >= minWindowLength; windowLength--) {
//			windows.add(windowLength);
//		}
//		return windows.toArray(new Integer[] {});
//	}
//
//	protected boolean compareLabels(Double label1, Double label2) {
//		// compare 1.0000 to 1.0 in String returns false, hence the conversion to double
//		return label1 != null && label2 != null && label1.equals(label2);
//	}
//
//	protected long BossDistance(BagOfPattern b1, BagOfPattern b2) {
//		long minDistance = Integer.MAX_VALUE;
//
//		long distance = 0;
//		for (IntIntCursor key : b1.bag) {
//			long buf;
//
//			if (key.value != 0) {
//				buf = key.value - b2.bag.get(key.key);
//				distance += buf * buf;
//			}
//		}
//
//		return distance;
//	}

}
