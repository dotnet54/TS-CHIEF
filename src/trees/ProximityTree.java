package trees;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

import com.sun.xml.internal.messaging.saaj.packaging.mime.util.QEncoderStream;

import core.AppContext;
import core.TreeStatCollector;
import core.AppContext.ParamSelection;
import core.AppContext.SplitterType;
import datasets.BossDataset;
import datasets.DataStore;
import datasets.BossDataset.BossParams;
import dev.BossTransformContainer;
import dev.RIFTransformContainer;
import dev.TSFTransformContainer;
import datasets.TSDataset;
import datasets.TimeSeries;
import distance.elastic.DistanceMeasure;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import transforms.SFA;
import transforms.BOSS.BagOfPattern;
import transforms.SFA.HistogramType;
import transforms.SFA.Words;
import trees.splitters.SplitEvaluator;
import trees.ProximityForest.Predictions;
import trees.splitters.BossSplitter;
import trees.splitters.NodeSplitter;
import util.Util;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ProximityTree{
	protected int forest_id;	//TODO remove
	private int tree_id;
	protected Node root;
	protected int node_counter = 0;
	
	protected transient Random rand;
	public TreeStatCollector stats;
	protected transient ProximityForest forest;	//NOTE
	
	public DistanceMeasure tree_distance_measure; //only used if AppContext.random_dm_per_node == false
	
	public double beta = 1e-10; // for boosting computation
	
	//TODO after training deallocate this
	public transient BossDataset boss_train_dataset_tree_level; //FIXME done at root node for all datasets, indices should match with original dataset
	public transient BossDataset boss_test_dataset_tree_level; //FIXME done at root node for all datasets, indices should match with original dataset

	public transient BossDataset.BossParams training_params;	//use this to transform test dataset
	public transient SFA training_sfa;
	
	public transient TSFTransformContainer tsf_transfomer;
	public transient TSDataset tsf_train_data;
	public transient TSDataset tsf_test_data;

	public transient RIFTransformContainer rif_transfomer;
	public transient TSDataset rif_train_data;
	public transient TSDataset rif_test_data;
	
	//TODO temp keeping per tree because if we choose to do boosting this set may differ from the dataset at forest level
	// better to use indices?
	public transient TSDataset train_data;
	
	protected transient DataStore treeDataStore;

	public ProximityTree(int tree_id, ProximityForest forest) {
		this.forest = forest;
		this.forest_id = forest.forest_id;
		this.tree_id = tree_id;
		this.rand = ThreadLocalRandom.current();
		stats = new TreeStatCollector(forest_id, tree_id);
		treeDataStore = new DataStore();
	}
	
	public ProximityForest getForest() {
		return this.forest;
	}

	public Node getRootNode() {
		return this.root;
	}
	
	private BossDataset boss_transform_at_tree_level(TSDataset data, boolean train_sfa) throws Exception {
		//if transformations are enabled perform transformation here
		//select parameters for the transformation from random sampling of CV params obtained before
		//param selection is per tree
		

		//transform the dataset using the randomly picked set of params;
		
		TimeSeries[] samples = data._get_internal_list().toArray(new TimeSeries[] {}); //FIXME
		
		if (train_sfa == true) {
			bossParamSelect(data, AppContext.boss_param_selection);
			System.out.println("boss training params for tree: " + this.tree_id + " " + training_params);
			
			training_sfa = new SFA(HistogramType.EQUI_DEPTH); 
			training_sfa.fitWindowing(samples, training_params.window_len, training_params.word_len, training_params.alphabet_size, training_params.normMean, training_params.lower_bounding);
		}
		
		final int[][] words = new int[samples.length][];
		
        for (int i = 0; i < samples.length; i++) {
              short[][] sfaWords = training_sfa.transformWindowing(samples[i]);
              words[i] = new int[sfaWords.length];
              for (int j = 0; j < sfaWords.length; j++) {
                words[i][j] = (int) Words.createWord(sfaWords[j], training_params.word_len, (byte) Words.binlog(training_params.alphabet_size));
              }
          }

		BagOfPattern[] histograms = BossSplitter.createBagOfPattern(words, samples, training_params.word_len, training_params.alphabet_size);

		
//		System.out.println(boss_dataset.toString());
		
//		TIntObjectMap<BagOfPattern>  class_hist =  boss_dataset.get_class_histograms();
//		System.out.println(class_hist.toString());
		
//		System.out.println(boss_dataset.get_sorted_class_hist(class_hist).toString());

		
		//		bopPerClass.put(key, bag);	
		
		return new BossDataset(data, histograms, training_params);
	}
	
	public void train(TSDataset data, int indices[]) throws Exception {

		if (AppContext.boss_enabled & (AppContext.boss_transform_level == AppContext.TransformLevel.Forest)) {
			//it must be done at forest level
		}else if (AppContext.boss_enabled & (AppContext.boss_transform_level == AppContext.TransformLevel.Tree)) {
			boss_train_dataset_tree_level = boss_transform_at_tree_level(data, true);
		}else if (AppContext.boss_enabled) {
			throw new Exception("ERROR: transformation level not supported for boss");
		}
		
		if (AppContext.tsf_enabled) {
			tsf_transfomer = new TSFTransformContainer(1);
			tsf_transfomer.fit(data);
			tsf_train_data = tsf_transfomer.transform(data);
		}
		
		if (AppContext.rif_enabled) {
			rif_transfomer = new RIFTransformContainer(1);
			rif_transfomer.fit(data);
			rif_train_data = rif_transfomer.transform(data);
		}
		
		this.train_data = data; //keeping a reference to this //TODO used to extract indices at node level, quick fix
		
		this.root = new Node(null, null, ++node_counter, this);
		this.root.class_distribution_str = data.get_class_map().toString();		//TODO debug only - memory leak	
		
		if (AppContext.random_dm_per_node ==  false) {	//DM is selected once per tree
			int r = ThreadLocalRandom.current().nextInt(AppContext.enabled_distance_measures.length);
			tree_distance_measure = new DistanceMeasure(AppContext.enabled_distance_measures[r], this.root);		
			//params selected per node in the splitter class
		}

		this.root.train(data, indices);
	}
	
	
	private BossDataset.BossParams bossParamSelect(TSDataset data, ParamSelection method) throws Exception {
		BossDataset.BossParams boss_params = null;
		int[] word_len = BossTransformContainer.getBossWordLengths();		

		SplittableRandom rand = new SplittableRandom();
		
		if (method == ParamSelection.Random) {
			
			
			//choose random params
//			Random rand = ThreadLocalRandom.current();
			
			boolean norm = rand.nextBoolean();			
			int w = rand.nextInt(10, data.length());
			int l = word_len[rand.nextInt(word_len.length)];
			
			boss_params = new BossParams(norm, w, l, 4);
			
		}else if (method == ParamSelection.PreLoadedSet){
			//choose best from predefined values

			List<BossDataset.BossParams> best_boss_params = AppContext.boss_preloaded_params.get(AppContext.getDatasetName());
			int r2  = rand.nextInt(best_boss_params.size()); //pick a random set of params among the best params for this datatset
			boss_params = best_boss_params.get(r2);
			
		}else {
			throw new Exception("Boss param selection method not supported");
		}
		
		
		this.training_params = boss_params;	//store to use during testing
		return boss_params;
	}
	
	public Predictions predict(TSDataset test) throws Exception {
		Predictions predctions = forest.new Predictions(test.size()); //TODO best memory management?
		
		//TODO do transformation of dataset here
		//if transformations are enabled perform transformation here
		//select parameters for the transformation from random sampling of CV params obtained before
		//param selection is per tree
		if (AppContext.boss_enabled & (AppContext.boss_transform_level == AppContext.TransformLevel.Forest)) {
			//it must be done at forest level
		}else if (AppContext.boss_enabled & (AppContext.boss_transform_level == AppContext.TransformLevel.Tree)) {
			boss_test_dataset_tree_level = boss_transform_at_tree_level(test, false);
		}else if (AppContext.boss_enabled){
			throw new Exception("ERROR: transformation level not supported for boss");
		}

		if (AppContext.tsf_enabled) {
			tsf_test_data = tsf_transfomer.transform(test);
		}
		
		if (AppContext.rif_enabled) {
			rif_test_data = rif_transfomer.transform(test);
		}
		
		for (int i = 0; i < test.size(); i++) {
			TimeSeries query = test.get_series(i);
			Integer predicted_label = predict(query, i);
			
			//TODO assert predicted_label = null;
			if (predicted_label == null) {
				throw new Exception("ERROR: possible bug detected: predicted label is null");
			}
			
			predctions.predicted_labels[i] = predicted_label;
			
			//TODO object equals or == ??
			if (predicted_label.equals(query.getLabel())) {
				predctions.correct.incrementAndGet();	//NOTE accuracy for tree
			}
		}
		
		return predctions;
	}
	
	public double tree_weighted_error(TSDataset data) throws Exception{		
		double weighted_error = 0.0;
		//System.out.println("Data size" + data.size());
		for (int i = 0; i < data.size(); i++) {
			TimeSeries query = data.get_series(i);
			Integer predicted_label = predict(query, i);
			
			//TODO assert predicted_label = null;
			if (predicted_label == null) {
				throw new Exception("ERROR: possible bug detected: predicted label is null");
			}
			
			//TODO object equals or == ??
			if (!predicted_label.equals(query.getLabel())) {
				//System.out.println("predicted_label=" + predicted_label + " -- query=" + query.getLabel());
				weighted_error += query.getWeight();				
			}
		}
		weighted_error /= data.size();
		return weighted_error;
	}
	
	public Integer predict(TimeSeries query, int queryIndex) throws Exception {
		
		
		//transform dataset using the params selected during the training phase.
		
		StringBuilder sb = new StringBuilder();
		sb.append(queryIndex + ":");
		
		
		Node node = this.root;
		
		//debug
		Node prev;
		int d = 0;
		//
		int[] labels = AppContext.getTraining_data().get_unique_classes();
		int lbl = -1;

		while(node != null && !node.is_leaf()) {
			prev = node;	//helps to debug if we store the previous node, TODO remove this later
			lbl = node.splitter.predict(query, queryIndex);
			sb.append(lbl + "-");
//			System.out.println(lbl);
//			if (node.children.get(lbl) == null) {
//				System.out.println("null child, using random choice");
//				//TODO check #class train != test
//				
//				return labels[ThreadLocalRandom.current().nextInt(labels.length)];
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
//			return labels[ThreadLocalRandom.current().nextInt(labels.length)];
//		}else if (node.label() == null) {
//			System.out.println("null label found, returning random label");
//			return labels[ThreadLocalRandom.current().nextInt(labels.length)];
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
		return tree_id;
	}

	
	//************************************** START stats -- development/debug code
	public TreeStatCollector getTreeStatCollection() {
		
		stats.collateResults(this);
		
		return stats;
	}	
	
	public int get_num_nodes() {
		if (node_counter != get_num_nodes(root)) {
			System.out.println("Error: error in node counter!");
			return -1;
		}else {
			return node_counter;
		}
	}	

	public int get_num_nodes(Node n) {
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
	
	public int get_num_leaves(Node n) {
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
	
	public int get_num_internal_nodes(Node n) {
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
	
	public int get_height(Node n) {
		int max_depth = 0;
		
		if (n.children == null) {
			return 0;
		}
		
		for (int key : n.children.keys()) {
			max_depth = Math.max(max_depth, get_height(n.children.get(key)));
		}

		return max_depth+1;
	}
	
	public int get_min_depth(Node n) {
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
	
	
	
	
	
	
	
	public class Node{
	
		protected transient Node parent;	//dont need this, but it helps to debug
		public transient ProximityTree tree;		
		
		protected int node_id;
		protected int node_depth = 0;

		protected boolean is_leaf = false;
		protected Integer label;

//		protected transient Dataset data;	
		//class distribution of data passed to this node during the training phase
		protected TIntIntMap class_distribution; 
		protected String class_distribution_str = ""; //class distribution as a string, just for printing and debugging
		protected TIntObjectMap<Node> children;
		protected SplitEvaluator splitter;
		
		public Node(Node parent, Integer label, int node_id, ProximityTree tree) {
			this.parent = parent;
//			this.data = new ListDataset();
			this.node_id = node_id;
			this.tree = tree;
			
			if (parent != null) {
				node_depth = parent.node_depth + 1;
			}
		}
		
		public boolean is_leaf() {
			return this.is_leaf;
		}
		
		public Integer label() {
			return this.label;
		}	
		
		public TIntObjectMap<Node> get_children() {
			return this.children;
		}		
		
//		public Dataset get_data() {
//			return this.data;
//		}		
		
		public String toString() {
			return "d: " + class_distribution_str;// + this.data.toString();
		}		

		
//		public void train(Dataset data) throws Exception {
//			this.data = data;
//			this.train();
//		}		
		
		public void train(TSDataset data, int indices[]) throws Exception {
//			System.out.println(this.node_depth + ":   " + (this.parent == null ? "r" : this.parent.node_id)  +"->"+ this.node_id +":"+ data.toString());
			
			//Debugging check
			if (data == null) {
//				throw new Exception("possible bug: empty node found");
//				this.label = Util.majority_class(data);
				this.is_leaf = true;
				System.out.println("node data == null, returning");
				return;				
			}
			
			this.class_distribution = data.get_class_map(); //TODO do we need to clone it? nope
			
			if (data.size() == 0) {
				this.is_leaf = true;
				System.out.println("node data.size == 0, returning");
				return;			
			}
			
			if (data.gini() == 0) {
				this.label = data.get_class(0);	//works if pure
				this.is_leaf = true;
				return;
			}
			
			// Minimum leaf size
			if (data.size() <= AppContext.min_leaf_size) {
				this.label = data.get_majority_class();	//-- choose the majority class at the node
				this.is_leaf = true;
				return;
			}

//			this.splitter = SplitterChooser.get_random_splitter(this);
			this.splitter = new SplitEvaluator(this);
				
			TIntObjectMap<TSDataset> best_splits = splitter.train(data, indices);
			
			//TODO refactor
			if (best_splits == null || has_empty_split(best_splits)) {
				//stop training and create a new leaf
//				throw new Exception("Empty leaf found");
				this.label = Util.majority_class(data);
				this.is_leaf = true;
//				System.out.println("Empty split found...returning a leaf: " + this.class_distribution + " leaf_label: " + this.label);
				
				return;
			}
			
			this.children = new TIntObjectHashMap<Node>(best_splits.size());
			
//			System.out.println(Arrays.toString(best_splits.keys()));
			
			for (int key : best_splits.keys()) {
				this.children.put(key, new Node(this, key, ++tree.node_counter, tree));
				this.children.get(key).class_distribution_str = best_splits.get(key).get_class_map().toString(); //TODO debug only mem- leak
			}
			
			for (int key : best_splits.keys()) {
				Node child = this.children.get(key);
				
//				if (best_splits.get(key) == null || best_splits.get(key).size() == 0) {
					//if this split has no data
					//remove this child from the map
//					System.out.println("removing empty or null node:" + key + " " + best_splits.get(key));
//					this.children.remove(key);
					
//				}else {
				
//				double wgini_temp = Util.weighted_gini(best_splits, data.size());
//				
//				if (Math.abs(data.gini() - wgini_temp) < 0.00001) {
//					System.out.println("\nWARN: best_split_wgini == parent gini: " + wgini_temp + " = " + best_splits.toString() + " --- " + data.toString());
//				}
				
					this.children.get(key).train(best_splits.get(key), null);
//				}
			}

		}

	}
	
	public boolean has_empty_split(TIntObjectMap<TSDataset> splits) throws Exception {
		
		for (int key : splits.keys()) {
			if (splits.get(key) == null || splits.get(key).size() == 0) {
				return true;
			}
		}

		return false;
	}
	
}
