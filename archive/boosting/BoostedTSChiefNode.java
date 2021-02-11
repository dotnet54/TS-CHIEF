package trees.boosting;

import application.AppConfig;
import data.containers.TreeStatCollector;
import data.timeseries.Dataset;
import data.timeseries.Indexer;
import data.timeseries.MTSDataset;
import data.timeseries.TimeSeries;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSCheifTree;
import trees.TSChiefNode;
import trees.splitters.NodeSplitter;
import trees.splitters.boss.BossSplitter;
import trees.splitters.boss.dev.BossBinarySplitter;
import trees.splitters.ee.ElasticDistanceSplitter;
import trees.splitters.ee.measures.DistanceMeasure;
import trees.splitters.it.InceptionTimeSplitter;
import trees.splitters.randf.RandomForestSplitter;
import trees.splitters.rise.RIFSplitter;
import trees.splitters.rotf.RotationForestSplitter;
import trees.splitters.rt.RocketTreeSplitter;
import trees.splitters.st.ForestShapeletTransformSplitter;
import trees.splitters.st.RandomShapeletTransformSplitter;
import trees.splitters.tsf.TSFSplitter;
import util.Sampler;
import util.Util;

import java.util.ArrayList;
import java.util.List;

public class BoostedTSChiefNode extends TSChiefNode{

    protected transient TSChiefNode parent;	//dont need this, but it helps to debug
    public transient TSCheifTree tree;

    protected int nodeID;
    protected int nodeDepth = 0;

    protected boolean isLeaf = false;
    protected Integer label;

    //		protected transient Dataset data;
    //class distribution of data passed to this node during the training phase
    protected TIntIntMap class_distribution;
    protected String class_distribution_str = ""; //class distribution as a string, just for printing and debugging
    protected TIntObjectMap<TSChiefNode> children;

    List<NodeSplitter> splitters;
    List<NodeSplitter> best_splitters;	//store the best ones to pick a random splitter if more than one are equal
    NodeSplitter best_splitter;


    public BoostedTSChiefNode(TSChiefNode parent, Integer label, int node_id, TSCheifTree tree) {
        this.parent = parent;
//			this.data = new ListDataset();
        this.nodeID = node_id;
        this.tree = tree;

        if (parent != null) {
            nodeDepth = parent.nodeDepth + 1;
        }

        splitters = new ArrayList<NodeSplitter>(AppConfig.num_actual_splitters_needed);
        best_splitters = new ArrayList<NodeSplitter>(2);	//initial capacity is set to a small number, its unlikely that splitters would have same gini very often

    }


    public boolean isLeaf() {
        return this.isLeaf;
    }

    public Integer label() {
        return this.label;
    }

    public TIntObjectMap<TSChiefNode> getChildren() {
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

    public void fit(Dataset trainData, Indexer trainIndices) throws Exception {
//			System.out.println(this.node_depth + ":   " + (this.parent == null ? "r" : this.parent.node_id)  +"->"+ this.node_id +":"+ data.toString());

        //Debugging check
        if (trainData == null) {
//				throw new Exception("possible bug: empty node found");
//				this.label = Util.majority_class(data);
            this.isLeaf = true;
            System.out.println("node data == null, returning");
            return;
        }

        this.class_distribution = trainData.getClassDistribution(); //TODO do we need to clone it? nope

        if (trainData.size() == 0) {
            this.isLeaf = true;
            System.out.println("node data.size == 0, returning");
            return;
        }

        if (trainData.gini() == 0) {
            this.label = trainData.getClass(0);	//works if pure
            this.isLeaf = true;
            return;
        }

        // Minimum leaf size
        if (trainData.size() <= AppConfig.min_leaf_size) {
            this.label = Util.majority_class(trainData);	//-- choose the majority class at the node
            this.isLeaf = true;
            return;
        }

        TIntObjectMap<Dataset> best_splits = fitSplitters(trainData, trainIndices);

        //TODO refactor
        if (best_splits == null || this.tree.has_empty_split(best_splits)) {
            //stop training and create a new leaf
//				throw new Exception("Empty leaf found");
            this.label = Util.majority_class(trainData);
            this.isLeaf = true;
//				System.out.println("Empty split found...returning a leaf: " + this.class_distribution + " leaf_label: " + this.label);

            return;
        }

        this.children = new TIntObjectHashMap<TSChiefNode>(best_splits.size());

//			System.out.println(Arrays.toString(best_splits.keys()));

        for (int key : best_splits.keys()) {
            this.children.put(key, new TSChiefNode(this, key, ++tree.nodeCounter, tree));
            this.children.get(key).class_distribution_str = best_splits.get(key).getClassDistribution().toString(); //TODO debug only mem- leak
        }

        for (int key : best_splits.keys()) {
            TSChiefNode child = this.children.get(key);

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

            this.children.get(key).fit(best_splits.get(key), null);
//				}
        }

    }

    private TIntObjectMap<MTSDataset> fitSplitters(Dataset trainData, Indexer trainIndices) throws Exception {
        long seStartTime = System.nanoTime();

        double weighted_gini = Double.POSITIVE_INFINITY;
        double best_weighted_gini = Double.POSITIVE_INFINITY;

        TIntObjectMap<MTSDataset> splits = null;
        TIntObjectMap<MTSDataset> best_split = null;

        double parent_size;
        if (!AppConfig.boosting) {
            parent_size = (double) trainData.size();
        }
        else {
            parent_size = trainData.get_sum_weight();
        }

        chooseSplitters();

        //shuffle splitters here, if not using random tie break	--NOTE not necessary if tie breaking randomly
//		Collections.shuffle(splitters, AppConfig.getRand());

        //find the correct indices
        int node_size = trainData.size();
        int[] node_indices = new int[node_size];

        Dataset all_train_data = this.tree.treeTrainData;
        int total_train_size = all_train_data.size();

        //TODO HIGH PRIORITY -- increases runtime a lot, TEST can be avoided if using indices at all places
        long st2 = System.nanoTime();
        for (int i = 0; i < total_train_size; i++) {
            for (int j = 0; j < node_size; j++) {
                if (all_train_data.getSeries(i) == trainData.getSeries(j)) {
                    node_indices[j] = i;
                }
            }
        }
        this.tree.stats.data_fetch_time += (System.nanoTime() - st2);

        int n = splitters.size();
        TreeStatCollector stats = this.tree.stats;
        RIFSplitter temp_rif;

        for (int i = 0; i < n; i++) {


            MTSDataset sample;
            //n' = sample(2,per_class,1.0)

            if (AppConfig.gini_approx) {
                sample = sample(data, AppConfig.approx_gini_min, AppConfig.approx_gini_min_per_class);
            }else {
                sample = data; //save time by not copying all the data, if we do gini on 100% data
            }

            //collect stats

            NodeSplitter currentSplitter = splitters.get(i);

            long startTime = System.nanoTime();

            if (currentSplitter instanceof ElasticDistanceSplitter) {
                //splitter.train(n)
                if (AppConfig.binary_split) {
                    currentSplitter.train_binary(data);
                    splits = currentSplitter.split(sample, node_indices);
                }else {
                    splits = currentSplitter.train(data, node_indices);
                }

                stats.ee_time += (System.nanoTime() - startTime);
            }else if (currentSplitter instanceof RandomForestSplitter) {
                //splitter.train(n)
                splits = currentSplitter.train(data, node_indices);
                //splitter.test(n')
//				splits = splitters[i].split(data, null);
                stats.randf_time += (System.nanoTime() - startTime);
            }else if (currentSplitter instanceof RotationForestSplitter) {
                //splitter.train(n)
                splits = currentSplitter.train(data, node_indices);
                //splitter.test(n')
//				splits = splitters[i].split(data, null);
                stats.rotf_time += (System.nanoTime() - startTime);
            }else if (currentSplitter instanceof RandomShapeletTransformSplitter
                    || currentSplitter instanceof ForestShapeletTransformSplitter) {

                //TODO
                splits = currentSplitter.train(data, node_indices);

                stats.st_time += (System.nanoTime() - startTime);
            }else if (currentSplitter instanceof BossSplitter) {
                //splitter.train(n)
                splits = currentSplitter.train(data, node_indices);
                //splitter.test(n')
//				splits = splitters[i].split(data, null);
                stats.boss_time += (System.nanoTime() - startTime);
            }else if (currentSplitter instanceof TSFSplitter) {
                //splitter.train(n)
                splits = currentSplitter.train(data, node_indices);
                //splitter.test(n')
//				splits = splitters[i].split(data, null);
                stats.tsf_time += (System.nanoTime() - startTime);
            }else if (currentSplitter instanceof RIFSplitter) {
                //splitter.train(n)
                splits = currentSplitter.train(data, node_indices);
                //splitter.test(n')
//				splits = splitters[i].split(data, null);

                long delta = System.nanoTime() - startTime;
                stats.rif_time += (delta);

                temp_rif = (RIFSplitter) currentSplitter;
                if (temp_rif.filter_type.equals(AppConfig.RifFilters.ACF)) {
                    stats.rif_acf_time+=delta;
                }else if (temp_rif.filter_type.equals(AppConfig.RifFilters.PACF)) {
                    stats.rif_pacf_time+=delta;
                }else if (temp_rif.filter_type.equals(AppConfig.RifFilters.ARMA)) {
                    stats.rif_arma_time+=delta;
                }else if (temp_rif.filter_type.equals(AppConfig.RifFilters.PS)) {
                    stats.rif_ps_time+=delta;
                }else if (temp_rif.filter_type.equals(AppConfig.RifFilters.DFT)) {
                    stats.rif_dft_time+=delta;
                }

            }else {

                //splitter.train(n)
                splits = currentSplitter.train(data, node_indices);
                //splitter.test(n')
//				splits = splitters[i].split(data, null);

                //TODO stat update in constructor
//				throw new Exception("Unsupported Splitter Type");
            }



            weighted_gini = weighted_gini(parent_size, splits);

            if (AppConfig.verbosity > 4) {
                System.out.print("  S" + i + ": " + currentSplitter.toString() + ", wgini=" + AppConfig.df.format(weighted_gini) +", parent=" + sample);
                System.out.print( "   -->splits =");
                for (int key : splits.keys()) {
                    System.out.print(" " + splits.get(key).toString());
                }
                System.out.println();
            }

            //TODO if equal take a random choice? or is it fine without it
            if (weighted_gini <  best_weighted_gini) {
                best_weighted_gini = weighted_gini;
                best_split = splits;
                best_splitter = currentSplitter;
                best_splitters.clear();
                best_splitters.add(currentSplitter);
            }else if (weighted_gini ==  best_weighted_gini) {	//NOTE if we enable this then we need update best_split again -expensive
                best_splitters.add(splitters.get(i));
            }
        }


        //failed to find any valid split point
        if (best_splitters.size() == 0) {
            return null;
        }else if (best_splitters.size() == 1) {
            best_splitter = best_splitters.get(0);	//then use stored best split
        }else { //if we have more than one splitter with equal best gini
            int r =  AppConfig.getRand().nextInt(best_splitters.size());
            best_splitter = best_splitters.get(r);
            // then we need to find the best split again, can't reuse best split we stored before.
            best_split = best_splitter.split(data, node_indices);

            if (AppConfig.verbosity > 4) {
                System.out.println("best_splitters.size() == " + best_splitters.size());
            }
        }

        //split the whole dataset using the (approximately) best splitter
//		best_split =  best_splitter.split(data, null); //TODO check

        //allow gc to deallocate unneeded memory
//		for (int j = 0; j < splitters.length; j++) {
//			if (splitters[j] != best_splitter) {
//				splitters[j] = null;
//			}
//		}
        splitters = null;
        best_splitters.clear();//clear the memory

        if (AppConfig.verbosity > 4) {
            System.out.print(" BEST:" + best_splitter.toString() + ", wgini:" + AppConfig.df.format(best_weighted_gini) + ", splits:");
            for (int key : best_split.keys()) {
                System.out.print(" " + best_split.get(key).toString());
            }
            System.out.println();
        }


        storeStatsForBestSplitter();

//		if (has_empty_split(best_split)) {
//			throw new Exception("empty splits found! check for bugs");//
//		}

        this.tree.stats.split_evaluator_train_time += (System.nanoTime() - seStartTime);
        return best_split;
    }


    private void chooseSplitters() throws Exception {

        int n = AppConfig.num_splitters_per_node;

        if (AppConfig.use_probabilities_to_choose_num_splitters_per_node) {
            //TODO only equal prob used here

            for (int i = 0; i < n; i++) {
                splitters.add(get_random_splitter(node));
            }

        }else {
            int total_added = 0;

            if (AppConfig.ee_enabled) {
                for (int i = 0; i < AppConfig.ee_splitters_per_node; i++) {
                    splitters.add(new ElasticDistanceSplitter(node));
                    total_added++;
                }
            }

            if (AppConfig.randf_enabled) {
                for (int i = 0; i < AppConfig.randf_splitters_per_node; i++) {
//					splitters[total_added++] = new RandomForestSplitter(node);
                    if (AppConfig.randf_feature_selection == AppConfig.FeatureSelectionMethod.ConstantInt) {
                        splitters.add(new RandomForestSplitter(node, AppConfig.randf_m));
                    }else {
                        splitters.add(new RandomForestSplitter(node, AppConfig.randf_feature_selection));
                    }

                    total_added++;
                }
            }

            if (AppConfig.rotf_enabled) {
                for (int i = 0; i < AppConfig.rotf_splitters_per_node; i++) {
                    splitters.add(new RotationForestSplitter(node));
                    total_added++;
                }
            }

            if (AppConfig.st_enabled) {
                for (int i = 0; i < AppConfig.st_splitters_per_node; i++) {

                    if (AppConfig.st_param_selection == AppConfig.ParamSelection.Random) {

                        splitters.add(new RandomShapeletTransformSplitter(node));

                    }else if (AppConfig.st_param_selection == AppConfig.ParamSelection.PreLoadedParams ||
                            AppConfig.st_param_selection == AppConfig.ParamSelection.PraLoadedDataAndParams) {

                        splitters.add(new ForestShapeletTransformSplitter(node));
                        total_added++;

                        break; //NOTE: if using ForestShapeletTransformSplitter -- FeatureSelectionMethod.ConstantInt is used to set m of the gini splitter to get the correct #gini required
                    }else {
                        throw new Exception("Unsupported parameter selection method for shapelet splitter -- invalid arg: -st_params ");
                    }

                    total_added++;
                }
            }

            if (AppConfig.boss_enabled) {
                for (int i = 0; i < AppConfig.boss_splitters_per_node; i++) {

                    if (AppConfig.boss_split_method == AppConfig.SplitMethod.RandomTreeStyle) {
                        splitters.add(new BossBinarySplitter(node));
                    }else {
                        splitters.add(new BossSplitter(node));
                    }
                    total_added++;
                }
            }

            if (AppConfig.tsf_enabled) {
                for (int i = 0; i < AppConfig.num_actual_tsf_splitters_needed; i++) {
                    splitters.add(new TSFSplitter(node));
                    total_added++;
                }
            }

            if (AppConfig.rif_enabled) {

                //TODO if only FeatureSelectionMethod.ConstantInt
//				if (AppContext.rif_feature_selection == FeatureSelectionMethod.ConstantInt) {
//
////					int num_gini_per_splitter = AppContext.rif_splitters_per_node / 4;	// eg. if we need 12 gini per type 12 ~= 50/4
////					int num_intervals = (int) Math.ceil((float)num_gini_per_splitter / (float)AppContext.rif_min_interval); // 2 = ceil(12 / 9)
////					int max_attribs_needed_per_rif_type = (int) Math.ceil(num_gini_per_splitter / num_intervals);
////					binary_splitter.m = Math.min(trans.length(), max_attribs_needed_per_rif_type);// 6 = 12 / 2
//
//					binary_splitter.m = Math.min(trans.length(), AppContext.rif_m);// 6 = 12 / 2
//
//					if (AppContext.verbosity > 1) {
////						System.out.println("rif m updated to: " + binary_splitter.m);
//					}
//				}

//				int approx_total_gini = 0;
//				int min_gini_per_group = AppContext.rif_min_interval * 4;
//				int min_splitters_per_type = (int) Math.ceil((float)AppContext.rif_splitters_per_node / (float)(min_gini_per_group));
//				int estimated_max_gini = min_splitters_per_type * min_gini_per_group;
//				int remaining_gini = estimated_max_gini;
//
//				int num_gini_per_type = AppContext.rif_splitters_per_node / 4;
//				int extra_gini = AppContext.rif_splitters_per_node % 4;



//				int min_splitters_needed_per_type = (int) Math.ceil((float)num_gini_per_type / (float)AppContext.rif_min_interval);
//				int max_attribs_to_use_per_splitter = (int) Math.ceil(num_gini_per_type / min_splitters_needed_per_type);

//				AppContext.num_actual_rif_splitters_needed_per_type = min_splitters_needed_per_type;
//				AppContext.rif_m = max_attribs_to_use_per_splitter;
//				int approx_gini_estimated = 4 * max_attribs_to_use_per_splitter * min_splitters_needed_per_type;
//				System.out.println("RISE: approx_gini_estimated: " + approx_gini_estimated);
//

                int rif_total = AppConfig.num_actual_rif_splitters_needed_per_type * 4;
                int reminder_gini = AppConfig.rif_splitters_per_node - (AppConfig.rif_m * rif_total);
                int m;

                for (int i = 0; i < rif_total; i++) {

                    if (AppConfig.rif_components == AppConfig.RifFilters.ACF_PACF_ARMA_PS_separately) {
                        RIFSplitter splitter;

                        //TODO quick fix to make sure that rif_splitters_per_node of ginis are actually calculated
                        if (reminder_gini > 0) {
                            m = AppConfig.rif_m + 1;
                            reminder_gini--;
                        }else {
                            m = AppConfig.rif_m;
                        }

                        //divide appox equally (best if divisible by 4)
                        if (i % 4 == 0) {
                            RIFSplitter sp = new RIFSplitter(node, AppConfig.RifFilters.ACF, m);
                            splitters.add(sp);
                        }else if (i % 4 == 1) {
                            RIFSplitter sp = new RIFSplitter(node, AppConfig.RifFilters.PACF, m);
                            splitters.add(sp);
                        }else if (i % 4 == 2) {
                            RIFSplitter sp = new RIFSplitter(node, AppConfig.RifFilters.ARMA, m);
                            splitters.add(sp);
                        }else if (i % 4 == 3) {
                            RIFSplitter sp = new RIFSplitter(node, AppConfig.RifFilters.PS, m);
                            splitters.add(sp);
                        }

                    }else {
                        splitters.add(new RIFSplitter(node, AppConfig.rif_components));
                    }

                    total_added++;
                }
            }	//rise

            if (AppConfig.it_enabled) {
                for (int i = 0; i < AppConfig.it_splitters_per_node; i++) {
                    splitters.add(new InceptionTimeSplitter(node));
                    total_added++;
                }
            }

//			if (AppConfig.rt_enabled) {
//				for (int i = 0; i < AppConfig.rt_splitters_per_node; i++) {
//					splitters.add(new RocketTreeSplitter(node));
//					total_added++;
//				}
//			}

            //ASSERT total_added == AppContext.num_actual_splitters_needed
        }



    }



    private static NodeSplitter get_random_splitter(TSChiefNode node) throws Exception {

        NodeSplitter splitter = null;

        int r = AppConfig.getRand().nextInt(AppConfig.enabled_splitters.length);
//		int r = AppContext.getRand().nextInt(AppContext.enabled_splitters.length);
        AppConfig.SplitterType selection = AppConfig.enabled_splitters[r];

        //use split constraints to set up limits to the splitting condition

        switch(selection) {
            case ElasticDistanceSplitter:
                splitter = new ElasticDistanceSplitter(node);
                break;
            case RandomForestSplitter:
//				splitter = new RandomForestSplitter(node);
                break;
            case ShapeletTransformSplitter:
                splitter = new RandomShapeletTransformSplitter(node);
                break;
            case RotationForestSplitter:
                splitter = new RotationForestSplitter(node);
                break;
            case BossSplitter:
//				System.out.println("choosing BossSplitterPerTree");
                splitter = new BossSplitter(node);

//				splitter = new BossSplitterPerNode(node);
                break;
            case TSFSplitter:
                splitter = new TSFSplitter(node);
                break;
            case RIFSplitter:
                splitter = new RIFSplitter(node, AppConfig.rif_components);
                break;
            default:
                throw new Exception("Splitter type not supported");
        }

        return splitter;
    }

    private void storeStatsForBestSplitter() {
        if (best_splitter instanceof ElasticDistanceSplitter) {
            this.tree.stats.ee_win++;
            DistanceMeasure dm = ((ElasticDistanceSplitter)best_splitter).get_dm();
            switch (dm.distance_measure) {
                case euclidean:
                    this.tree.stats.euc_win++;
                    break;
                case dtw:
                    this.tree.stats.dtw_win++;
                    break;
                case dtwcv:
                    this.tree.stats.dtwr_win++;
                    break;
                case ddtw:
                    this.tree.stats.ddtw_win++;
                    break;
                case ddtwcv:
                    this.tree.stats.ddtwr_win++;
                    break;
                case wdtw:
                    this.tree.stats.wdtw_win++;
                    break;
                case wddtw:
                    this.tree.stats.wddtw_win++;
                    break;
                case lcss:
                    this.tree.stats.lcss_win++;
                    break;
                case twe:
                    this.tree.stats.twe_win++;
                    break;
                case erp:
                    this.tree.stats.erp_win++;
                    break;
                case msm:
                    this.tree.stats.msm_win++;
                    break;

                default:
                    break;
            }

        }else if (best_splitter instanceof RandomForestSplitter) {
            this.tree.stats.randf_win++;
        }else if (best_splitter instanceof RotationForestSplitter) {
            this.tree.stats.rotf_win++;
        }else if (best_splitter instanceof RandomShapeletTransformSplitter) {
            this.tree.stats.st_win++;

            //store shapelet that won

            RandomShapeletTransformSplitter temp = (RandomShapeletTransformSplitter) best_splitter;
            this.tree.getForest().getExpResultCollection().winShapelets.add(temp.best_shapelet.toString());

        }else if (best_splitter instanceof ForestShapeletTransformSplitter) {
            this.tree.stats.st_win++;

            //store shapelet that won
            ForestShapeletTransformSplitter temp = (ForestShapeletTransformSplitter) best_splitter;
            this.tree.getForest().getExpResultCollection().winShapelets.add(temp.best_shapelet.toString());

        }else if (best_splitter instanceof BossSplitter) {
            this.tree.stats.boss_win++;
        }else if (best_splitter instanceof TSFSplitter) {
            this.tree.stats.tsf_win++;
        }else if (best_splitter instanceof RIFSplitter) {
            this.tree.stats.rif_win++;

            RIFSplitter rif = (RIFSplitter) best_splitter;
            if (rif.filter_type.equals(AppConfig.RifFilters.ACF)) {
                this.tree.stats.rif_acf_win++;
            }else if (rif.filter_type.equals(AppConfig.RifFilters.PACF)) {
                this.tree.stats.rif_pacf_win++;
            }else if (rif.filter_type.equals(AppConfig.RifFilters.ARMA)) {
                this.tree.stats.rif_arma_win++;
            }else if (rif.filter_type.equals(AppConfig.RifFilters.PS)) {
                this.tree.stats.rif_ps_win++;
            }else if (rif.filter_type.equals(AppConfig.RifFilters.DFT)) {
                this.tree.stats.rif_dft_win++;
            }


        }else if (best_splitter instanceof InceptionTimeSplitter) {
            this.tree.stats.it_win++;
        }else if (best_splitter instanceof RocketTreeSplitter) {
            this.tree.stats.rt_win++;
        }
    }


    public static boolean has_empty_split(TIntObjectMap<MTSDataset> splits) throws Exception {

        for (int key : splits.keys()) {
            if (splits.get(key) == null || splits.get(key).size() == 0) {
                return true;
            }
        }

        return false;
    }

    //takes the min(gini_min * #class_root, n)
    private MTSDataset sample(MTSDataset data, int approx_gini_min, boolean approx_gini_min_per_class) {
        MTSDataset sample;
        int sample_size;

        //use number of classes in the root
        if (approx_gini_min_per_class) {
            sample_size = Math.min(approx_gini_min * AppConfig.getTrainingSet().getNumClasses(), data.size());
        }else {
            sample_size = Math.min(approx_gini_min, data.size());
        }

        sample = Sampler.uniform_sample(data, sample_size);

        return sample;
    }

    public int predict(TimeSeries query, int queryIndex) throws Exception {
        return best_splitter.predict(query, queryIndex);
    }





    public double weighted_gini(double parent_size, TIntObjectMap<MTSDataset> splits) throws Exception {
        double wgini = 0.0;
        double gini;
        double split_size = 0;

        if (splits == null) {
            return Double.POSITIVE_INFINITY;
        }

        if (!AppConfig.boosting) {
            for (int key : splits.keys()) {
                if (splits.get(key) == null) {	//NOTE
                    gini = 1;
                    split_size = 0;
                }else {
                    gini = splits.get(key).gini();
                    split_size = (double) splits.get(key).size();
                }
                wgini = wgini + (split_size / parent_size) * gini;
            }
        } else {
            for (int key : splits.keys()) {
                if (splits.get(key) == null) {	//NOTE
                    gini = 1;
                    split_size = 0;
                }else {
                    gini = splits.get(key).weighted_gini();
                    split_size = splits.get(key).get_sum_weight();
                }
                wgini = wgini + (split_size / parent_size) * gini;
            }
        }

        return wgini;
    }





}
