package development.experiments;

import java.util.Random;

import core.threading.ParallelFor;
import data.io.CsvReader;
import data.timeseries.UTimeSeries;
import data.timeseries.UTSDataset;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.splitters.boss.dev.BOSSEnsembleClassifier;

public class SFAtest {
	public static long seed = 1;
	public static Random random = new Random(seed);

	public static final String UCR_dataset = "Fish";// "DiatomSizeReduction";
	public static String training_file = "E:/data/ucr/" + UCR_dataset + "/" + UCR_dataset + "_TRAIN.txt";
	public static String testing_file = "E:/data/ucr/" + UCR_dataset + "/" + UCR_dataset + "_TEST.txt";

	public static UTSDataset train_dataset;
	public static UTSDataset test_dataset;

	public static void main(String[] args) throws Exception {

		train_dataset = CsvReader.readCSVToTSDataset(training_file);
		test_dataset = CsvReader.readCSVToTSDataset(testing_file);

		UTimeSeries[] train = train_dataset._get_internal_list().toArray(new UTimeSeries[] {});
		UTimeSeries[] test = test_dataset._get_internal_list().toArray(new UTimeSeries[] {});

		int windowLength = 8;
		int wordLength = 4;
		int symbols = 4;
		boolean normMean = true;
		boolean lowerBounding = true;		
		
		//SFA TESTS
//		SFA sfa = new SFA(HistogramType.EQUI_DEPTH);
//		sfa.fitTransform(train, wordLength, symbols, normMean);

//	    sfa.printBins();
//		short[] wordQuery = sfa.transform(test[0]);
		
//		System.out.println(0 + "-th transformed time series SFA word " + "\t" + toSfaWord(wordQuery, symbols));

		
		//BOSS TESTS
		TIntObjectMap<UTSDataset> data_per_class = train_dataset.split_by_class();	//TODO can be done once at node
		UTimeSeries[] exemplars = new UTimeSeries[data_per_class.size() * 1];
		
		int i = 0;
		for (int key : data_per_class.keys()) {
			exemplars[i++] = data_per_class.get(key).get_series(0);
//			exemplars[i++] = data_per_class.get(key).get_series(1);
		}
		
		int c = 1;
		UTimeSeries[] sample = new UTimeSeries[20 * c];
		
		for (int j = 0; j < sample.length; j++) {
			sample[j] = train_dataset.get_series(j);
		}

		
		BOSSEnsembleClassifier bossEns = new BOSSEnsembleClassifier();
		bossEns.fit(sample);
		Double[] labels = bossEns.predict(train);

		TIntObjectMap<UTSDataset> splits = new TIntObjectHashMap<UTSDataset>();
		
		for (int key : data_per_class.keys()) {
			UTSDataset split = new UTSDataset();
			splits.put(key, split);
		}
		
		for (int j = 0; j < labels.length; j++) {
			UTSDataset split = splits.get(labels[j].intValue());
			split.add(train[j]);
		}
		
		System.out.println(splits.toString());
		System.out.println("wgini: " + weighted_gini(splits, train.length));

			
		ParallelFor.shutdownNow();
		System.exit(0); //TODO hot fix
	}

	public static String toSfaWord(short[] word, int symbols) {
		StringBuilder sfaWord = new StringBuilder();

		for (short c : word) {
			sfaWord.append((char) (Character.valueOf('a') + c));
		}

		return sfaWord.toString();
	}

	public static double weighted_gini(TIntObjectMap<UTSDataset> splits, int parent_node_size) {
		double wgini = 0.0;
		
		//TODO handle case when a split contains null object
		for (int key : splits.keys()) {
			wgini = wgini + ((double) splits.get(key).size() / parent_node_size) * splits.get(key).gini();
		}

		return wgini;
	}
	
	
}
