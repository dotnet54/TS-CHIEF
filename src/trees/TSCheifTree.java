package trees;

import java.util.List;
import java.util.Random;
import application.AppConfig;
import application.AppConfig.ParamSelection;
import core.*;
import core.exceptions.NotImplementedException;
import data.containers.TreeStatCollector;
import data.timeseries.*;
import data.dev.DataCache;
import gnu.trove.map.TIntObjectMap;
import trees.splitters.boss.BossSplitter;
import trees.splitters.boss.SFA;
import trees.splitters.boss.BOSS.BagOfPattern;
import trees.splitters.boss.SFA.HistogramType;
import trees.splitters.boss.SFA.Words;
import trees.splitters.boss.dev.BossDataset;
import trees.splitters.boss.dev.BossTransformContainer;
import trees.splitters.boss.dev.BossDataset.BossParams;
import distance.univariate.UnivarSimMeasure;
import util.Util;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class TSCheifTree implements Classifier {
	protected int forestID;	//TODO remove
	private int treeID;
	protected TSChiefNode root;
	protected int nodeCounter = 0;
	protected boolean isFitted;

	protected transient Random rand;
	public TreeStatCollector stats;
	protected transient TSCheifForest forest;	//NOTE
	
	public UnivarSimMeasure treeDistanceMeasure; //only used if AppContext.random_dm_per_node == false

	//TODO temp keeping per tree because if we choose to do boosting this set may differ from the dataset at forest level
	// better to use indices?
	public transient Dataset treeLevelTrainingData;
	protected transient DataCache treeDataCache;
	protected transient TSChiefTreeResult result;

	//TODO after training deallocate this
	public transient BossDataset treeLevelBossTrainData; //FIXME done at root node for all datasets, indices should match with original dataset
	public transient BossDataset treeLevelBossTestData; //FIXME done at root node for all datasets, indices should match with original dataset
	public transient BossDataset.BossParams treeLevelBossTrainingParams;	//use this to transform test dataset
	public transient SFA teeeLevelBossSFATransform;
	
//	public transient TSFTransformContainer tsf_transfomer;
//	public transient Dataset tsf_train_data;
//	public transient Dataset tsf_test_data;

//	public transient RIFTransformContainer treeLevelRiseTransform;
//	public transient Dataset treeLevelRiseTransformedTrainData;
//	public transient Dataset treeLevelRiseTransformedTestData;

	public TSCheifTree(int tree_id, TSCheifForest forest) {
		this.forest = forest;
		this.forestID = forest.forest_id;
		this.treeID = tree_id;
		this.rand = AppConfig.getRand(); //ThreadLocalRandom.current();
		stats = new TreeStatCollector(this);
		treeDataCache = new DataCache();
	}

	@Override
	public ClassifierResult fit(Dataset trainData) throws Exception {
		result = new TSChiefTreeResult(this);

		ArrayIndex indices = new ArrayIndex(trainData);
		indices.sampleSequentially(trainData.size());
		this.fit(trainData, indices, null);
		isFitted = true;
		return result;
	}

	@Override
	public ClassifierResult fit(Dataset trainData, Indexer trainIndices, DebugInfo debugInfo) throws Exception {
		result = new TSChiefTreeResult(this);

		if (AppConfig.boss_enabled & (AppConfig.boss_transform_level == AppConfig.TransformLevel.Forest)) {
			//it must be done at forest level
		}else if (AppConfig.boss_enabled & (AppConfig.boss_transform_level == AppConfig.TransformLevel.Tree)) {
			treeLevelBossTrainData = boss_transform_at_tree_level(trainData, true);
		}else if (AppConfig.boss_enabled) {
			throw new Exception("ERROR: transformation level not supported for boss");
		}

//		if (AppConfig.tsf_enabled) {
//			tsf_transfomer = new TSFTransformContainer(1);
//			tsf_transfomer.fit(trainData);
//			tsf_train_data = tsf_transfomer.transform(trainData);
//		}

//		if (AppContext.rif_enabled) {
//			rif_transfomer = new RIFTransformContainer(1);
//			rif_transfomer.fit(data);
//			rif_train_data = rif_transfomer.transform(data);
//		}

		this.treeLevelTrainingData = trainData; //keeping a reference to this //TODO used to extract indices at node level, quick fix

		this.root = new TSChiefNode(null, null, ++nodeCounter, this);
		this.root.class_distribution_str = trainData.getClassDistribution().toString();		//TODO debug only - memory leak

		if (AppConfig.random_dm_per_node ==  false) {	//DM is selected once per tree
			int r = AppConfig.getRand().nextInt(AppConfig.enabled_distance_measures.length);
			treeDistanceMeasure = new UnivarSimMeasure(AppConfig.enabled_distance_measures[r], this.root);
			//params selected per node in the splitter class
		}

		this.root.fit(trainData, trainIndices);
		isFitted = true;
		return result;
	}

	@Override
	public ClassifierResult predict(Dataset testData) throws Exception{
		return this.predict(testData, null, null);
	}

	@Override
	public ClassifierResult predict(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception {
		result.allocateForPredictionResults(testData);
		int[] predctions = result.getPredctictedLabels();
		
		//TODO do transformation of dataset here
		//if transformations are enabled perform transformation here
		//select parameters for the transformation from random sampling of CV params obtained before
		//param selection is per tree
		if (AppConfig.boss_enabled & (AppConfig.boss_transform_level == AppConfig.TransformLevel.Forest)) {
			//it must be done at forest level
		}else if (AppConfig.boss_enabled & (AppConfig.boss_transform_level == AppConfig.TransformLevel.Tree)) {
			treeLevelBossTestData = boss_transform_at_tree_level(testData, false);
		}else if (AppConfig.boss_enabled){
			throw new Exception("ERROR: transformation level not supported for boss");
		}

//		if (AppConfig.tsf_enabled) {
//			tsf_test_data = tsf_transfomer.transform(test);
//		}
		
//		if (AppContext.rif_enabled) {
//			rif_test_data = rif_transfomer.transform(test);
//		}
		
		for (int i = 0; i < testData.size(); i++) {
			TimeSeries query = testData.getSeries(i);
			Integer predicted_label = predictSeries(query, i);
			
			//TODO assert predicted_label = null;
			if (predicted_label == null) {
				throw new Exception("ERROR: possible bug detected: predicted label is null");
			}

			predctions[i] = predicted_label;
			
			//TODO object equals or == ??
			if (predicted_label.equals(query.label())) {
				result.correct.incrementAndGet();	//NOTE accuracy for tree
			}
		}
		
		return this.result;
	}


	private int predictSeries(TimeSeries query, int queryIndex) throws Exception {
		//transform dataset using the params selected during the training phase.

		StringBuilder sb = new StringBuilder();
//		sb.append(queryIndex + ":");


		TSChiefNode node = this.root;

		//debug
		TSChiefNode prev;
		int d = 0;
		//
		int[] labels = AppConfig.getTrainingSet().getUniqueClasses();
		int lbl = -1;

		while(node != null && !node.isLeaf()) {
			prev = node;	//helps to debug if we store the previous node, TODO remove this later
			lbl = node.predict(null, query, queryIndex); //TODO CHECK null TEMP
			sb.append(lbl + "-");
//			System.out.println(lbl);
//			if (node.children.get(lbl) == null) {
//				System.out.println("null child, using random choice");
//				//TODO check #class train != test
//
//				return labels[AppConfig.getRand().nextInt(labels.length)];
//			}

			node = node.children.get(lbl);
			if (node == null) {
				System.out.println("null node found: " + lbl);
				return lbl;
			}
			d++;
		}

//		if (node == null) {
//			System.out.println("null node found, returning random label ");
//			return labels[AppConfig.getRand().nextInt(labels.length)];
//		}else if (node.label() == null) {
//			System.out.println("null label found, returning random label");
//			return labels[AppConfig.getRand().nextInt(labels.length)];
//		}

		if (node == null) {
			System.out.println("null node found, returning exemplar label " + lbl);
			return lbl;
		}else if (node.label() == null) {
			System.out.println("null label found, returning exemplar label" + lbl);
			return lbl;
		}

		sb.append(">" + node.label());

//		System.out.println(sb.toString());

		return node.label();
	}


	@Override
	public ClassifierResult predictProba(Dataset testData) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public ClassifierResult predictProba(Dataset testData, Indexer testIndices, DebugInfo debugInfo) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public int predict(TimeSeries query) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public int predict(int queryIndex) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public double predictProba(TimeSeries query) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public double score(Dataset trainData, Dataset testData) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public double score(Dataset trainData, Dataset testData, DebugInfo debugInfo) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public Options getParams() {
		throw new NotImplementedException();
	}

	@Override
	public void setParams(Options params) {
		throw new NotImplementedException();
	}

	@Override
	public ClassifierResult getTrainResults() {
		return result;
	}

	@Override
	public ClassifierResult getTestResults() {
		return null;
	}

	public TSCheifForest getForest() {
		return this.forest;
	}

	public TSChiefNode getRootNode() {
		return this.root;
	}

	//TODO predict distribution
//	public int[] predict_distribution(double[] query) throws Exception {
//		Node node = this.root;
//
//		while(!node.is_leaf()) {
//			node = node.children[node.splitter.predict_by_splitter(query)];
//		}
//
//		return node.label();
//	}
//	
	public int getTreeID() {
		return treeID;
	}





	//************************************** START stats -- development/debug code



	private BossDataset.BossParams bossParamSelect(Dataset data, ParamSelection method) throws Exception {
		BossDataset.BossParams boss_params = null;
		int[] word_len = BossTransformContainer.getBossWordLengths();

//		SplittableRandom rand = new SplittableRandom();
		Random rand = AppConfig.getRand();

		if (method == ParamSelection.Random) {


			//choose random params

			boolean norm = rand.nextBoolean();
			int w = Util.getRandNextInt(10, data.length());
			int l = word_len[rand.nextInt(word_len.length)];

			boss_params = new BossParams(norm, w, l, 4);

		}else if (method == ParamSelection.PreLoadedParams){
			//choose best from predefined values

			List<BossDataset.BossParams> best_boss_params = AppConfig.boss_preloaded_params.get(AppConfig.getDatasetName());
			int r2  = rand.nextInt(best_boss_params.size()); //pick a random set of params among the best params for this datatset
			boss_params = best_boss_params.get(r2);

		}else {
			throw new Exception("Boss param selection method not supported");
		}


		this.treeLevelBossTrainingParams = boss_params;	//store to use during testing
		return boss_params;
	}

	private BossDataset boss_transform_at_tree_level(Dataset data, boolean train_sfa) throws Exception {
		//if transformations are enabled perform transformation here
		//select parameters for the transformation from random sampling of CV params obtained before
		//param selection is per tree


		//transform the dataset using the randomly picked set of params;

		TimeSeries[] samples = data.toArray();

		if (train_sfa == true) {
			bossParamSelect(data, AppConfig.boss_param_selection);
			System.out.println("boss training params for tree: " + this.treeID + " " + treeLevelBossTrainingParams);

			teeeLevelBossSFATransform = new SFA(HistogramType.EQUI_DEPTH);
			teeeLevelBossSFATransform.fitWindowing(samples, treeLevelBossTrainingParams.window_len, treeLevelBossTrainingParams.word_len, treeLevelBossTrainingParams.alphabet_size, treeLevelBossTrainingParams.normMean, treeLevelBossTrainingParams.lower_bounding);
		}

		final int[][] words = new int[samples.length][];

		for (int i = 0; i < samples.length; i++) {
			short[][] sfaWords = teeeLevelBossSFATransform.transformWindowing(samples[i]);
			words[i] = new int[sfaWords.length];
			for (int j = 0; j < sfaWords.length; j++) {
				words[i][j] = (int) Words.createWord(sfaWords[j], treeLevelBossTrainingParams.word_len, (byte) Words.binlog(treeLevelBossTrainingParams.alphabet_size));
			}
		}

		BagOfPattern[] histograms = BossSplitter.createBagOfPattern(words, samples, treeLevelBossTrainingParams.word_len, treeLevelBossTrainingParams.alphabet_size);


//		System.out.println(boss_dataset.toString());

//		TIntObjectMap<BagOfPattern>  class_hist =  boss_dataset.get_class_histograms();
//		System.out.println(class_hist.toString());

//		System.out.println(boss_dataset.get_sorted_class_hist(class_hist).toString());


		//		bopPerClass.put(key, bag);

		return new BossDataset(data, histograms, treeLevelBossTrainingParams);
	}

	public TreeStatCollector getTreeStatCollection() {
		
		stats.aggregateResults();
		
		return stats;
	}	
	
	public int get_num_nodes() {
		if (nodeCounter != get_num_nodes(root)) {
			System.out.println("Error: error in node counter!");
			return -1;
		}else {
			return nodeCounter;
		}
	}	

	public int get_num_nodes(TSChiefNode n) {
		int count = 0 ;
		
		if (n.children == null) {
			return 1;
		}
		
		for (int key : n.children.keys()) {
			count+= get_num_nodes(n.children.get(key));
		}
		
		return count+1;
	}
	
	public int get_num_leaves() {
		return get_num_leaves(root);
	}	
	
	public int get_num_leaves(TSChiefNode n) {
		int count = 0 ;
		
		if (n.children == null) {
			return 1;
		}
		
		for (int key : n.children.keys()) {
			count+= get_num_leaves(n.children.get(key));
		}
		
		return count;
	}
	
	public int get_num_internal_nodes() {
		return get_num_internal_nodes(root);
	}
	
	public int get_num_internal_nodes(TSChiefNode n) {
		int count = 0 ;
		
		if (n.children == null) {
			return 0;
		}
		
		for (int key : n.children.keys()) {
			count+= get_num_internal_nodes(n.children.get(key));
		}
		
		return count+1;
	}
	
	public int get_height() {
		return get_height(root);
	}
	
	public int get_height(TSChiefNode n) {
		int max_depth = 0;
		
		if (n.children == null) {
			return 0;
		}
		
		for (int key : n.children.keys()) {
			max_depth = Math.max(max_depth, get_height(n.children.get(key)));
		}

		return max_depth+1;
	}
	
	public int get_min_depth(TSChiefNode n) {
		int max_depth = 0;
		
		if (n.children == null) {
			return 0;
		}
		
		for (int key : n.children.keys()) {
			max_depth = Math.min(max_depth, get_height(n.children.get(key)));
			
		}
		
		return max_depth+1;
	}
	
//	public double get_weighted_depth() {
//		return printTreeComplexity(root, 0, root.data.size());
//	}
//	
//	// high deep and unbalanced
//	// low is shallow and balanced?
//	public double printTreeComplexity(Node n, int depth, int root_size) {
//		double ratio = 0;
//		
//		if (n.is_leaf) {
//			double r = (double)n.data.size()/root_size * (double)depth;
////			System.out.format("%d: %d/%d*%d/%d + %f + ", n.label, 
////					n.data.size(),root_size, depth, max_depth, r);
//			
//			return r;
//		}
//		
//		for (int i = 0; i < n.children.length; i++) {
//			ratio += printTreeComplexity(n.children[i], depth+1, root_size);
//		}
//		
//		return ratio;
//	}		
	
	
	//**************************** END stats -- development/debug code

	public boolean has_empty_split(TIntObjectMap<Dataset> splits) throws Exception {
		
		for (int key : splits.keys()) {
			if (splits.get(key) == null || splits.get(key).size() == 0) {
				return true;
			}
		}

		return false;
	}
	
}
