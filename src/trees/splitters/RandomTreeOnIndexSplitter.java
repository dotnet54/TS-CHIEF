package trees.splitters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import application.AppConfig.FeatureSelectionMethod;
import core.exceptions.MultivariateDataNotSupportedException;
import core.exceptions.NotSupportedException;
import data.timeseries.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import trees.TSChiefNode;
import util.Util;
import util.storage.pair.DblIntPair;

public class RandomTreeOnIndexSplitter implements NodeSplitter {

    protected final int NUM_CHILDREN = 2;
    protected final int LEFT_BRANCH = 0;
    protected final int RIGHT_BRANCH = 1;

    protected int[] attributes;
    protected int bestAttribute;
    protected double bestThreshold;

    protected int numFeatures;
    protected FeatureSelectionMethod featureSelectionMethod;
    protected TSChiefNode node;
    //if false, selects the first m attributes that can give a valid split
    protected boolean randomizeAttribSelection = true;

    protected boolean debug = false;

    public RandomTreeOnIndexSplitter(TSChiefNode node) throws Exception {
        this.node = node;
    }

    public RandomTreeOnIndexSplitter(TSChiefNode node, int numFeatures) throws Exception {
        this.node = node;
        this.numFeatures = numFeatures;
    }

    public RandomTreeOnIndexSplitter(TSChiefNode node, FeatureSelectionMethod feature_selection_method) throws Exception {
        this.node = node;
        this.featureSelectionMethod = feature_selection_method;
    }

    public int getNumFeatures() {
        return numFeatures;
    }

    public void setNumFeatures(int numFeatures) {
        this.numFeatures = numFeatures;
    }

    public void setNumFeatures(int numFeatures, int length, FeatureSelectionMethod featureSelectionMethod) throws Exception {
        if (featureSelectionMethod == FeatureSelectionMethod.ConstantInt) {
            if (numFeatures <= 0 || numFeatures >= length) {
                setNumFeatures(length); //default for this case
            } else {
                setNumFeatures(numFeatures);
            }
        } else if (featureSelectionMethod == FeatureSelectionMethod.Sqrt) {
            setNumFeatures((int) Math.sqrt(length));
        } else {
            throw new Exception("Unknown feature selection method");
        }
    }

    public int getBestAttribute() {
        return bestAttribute;
    }

    public void setBestAttribute(int best_attribute) {
        this.bestAttribute = best_attribute;
    }

    public double getBestThreshold() {
        return bestThreshold;
    }

    public void setBestThreshold(double best_threshold) {
        this.bestThreshold = best_threshold;
    }

    public boolean getRandomizeAttribSelection() {
        return randomizeAttribSelection;
    }

    public void setRandomizeAttribSelection(boolean randomize) {
        this.randomizeAttribSelection = randomize;
    }

    public String toString() {
        return "GiniSplitter[m=" + numFeatures + ", a=" + bestAttribute + ", t=" + bestThreshold + "]";
    }

    @Override
    public TIntObjectMap<Dataset> fit(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
        throw new NotSupportedException();
    }

    @Override
    public TIntObjectMap<Dataset> split(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
        throw new NotSupportedException();
    }

    public TIntObjectMap<TIntArrayList> fitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
        if (nodeTrainData.isMultivariate()) {
            throw new MultivariateDataNotSupportedException();
        }

        int length = nodeTrainData.length();
        int total_size = nodeTrainData.size();

        boolean can_split = false;
        boolean can_use_attribute = false;
        double weighted_gini = Double.POSITIVE_INFINITY;
        double best_weighted_gini = Double.POSITIVE_INFINITY;
        int left_size = 0, right_size;
        double left_gini, right_gini;
        int class_label;
        int used_attributes = 0;    //if an attribute is not useful we keep trying until used_attributes <= m
        TIntIntMap left_class_dist = new TIntIntHashMap();
        TIntIntMap right_class_dist = new TIntIntHashMap();

        List<DblIntPair> values = new ArrayList<DblIntPair>(total_size);
        double best_threshold_per_attrib = 0;
        int best_attribute_per_attrib = 0;

        int _best_split_pos;    //debug -> index/position of best threshold

        //if no data -- usually this case is handled much earlier -- duplicated here to make this class work independently also
        if (nodeTrainData.size() == 0) {
            return null;
        }

        //if data.size == 1 or data.gini() == 0 -> these cases are handled at node.train();

        if (featureSelectionMethod == FeatureSelectionMethod.Sqrt) {
            numFeatures = (int) Math.sqrt(length);
        } else if (featureSelectionMethod == FeatureSelectionMethod.Loge) {
            numFeatures = (int) Math.log(length);
        } else if (featureSelectionMethod == FeatureSelectionMethod.Log2) {
            numFeatures = (int) (Math.log(length / Math.log(2)));
        } else {
            //assume m is set via constructor
            //			m = Integer.parseInt(AppContext.randf_m); //TODO verify
        }

        //		set m, for random forest, set correct m using constructor or the setter function
        if (numFeatures == 0 || numFeatures > length) {
            numFeatures = length;
        }

        double[] _best_gini_per_attrib = null;
        if (debug) {
            _best_gini_per_attrib = new double[numFeatures];
        }

        attributes = new int[length];
        for (int i = 0; i < length; i++) {
            attributes[i] = i;
        }

        if (randomizeAttribSelection) {
            attributes = Util.shuffleArray(attributes);    //note not an in place shuffle, change this later
        }

        //TODO check used_attributes <= m or used_attributes < m
        for (int i = 0; i < attributes.length && used_attributes <= numFeatures; i++) {
            can_use_attribute = false;
            values.clear(); // O(n) complexity
            left_class_dist.clear();
            right_class_dist.clear();
            left_size = 0;

            //clone the class distribution from the dataset
            TIntIntMap class_dist = nodeTrainData.getClassDistribution();
            for (int key : class_dist.keys()) {
                right_class_dist.put(key, class_dist.get(key));
            }
            //extract the column
            for (int j = 0; j < total_size; j++) {
                //todo using only dimension 1
                DblIntPair pair = new DblIntPair(nodeTrainData.getSeries(j).value(0, attributes[i]),
                        nodeTrainData.getClass(j));
                values.add(pair);
            }
            //sort the pairs
            Collections.sort(values, (a, b) -> {
                //				System.out.println("a:" + a.key + " b: " + b.key + " a-b: " + (int) (a.key - b.key));
                return Double.compare(a.key, b.key);    //TODO use a dbl compare class with a threshold
            });

            double cur_threshold = values.get(0).key;            //TODO check first item or 0? or doesnt matter?
            right_size = total_size - left_size;


            for (int j = 0; j < total_size - 1; j++) {
                class_label = values.get(j).value;

                left_size++;
                left_class_dist.adjustOrPutValue(class_label, 1, 1);
                right_class_dist.adjustOrPutValue(class_label, -1, 0);
                right_size = total_size - left_size;

                if (values.get(j + 1).key > values.get(j).key) {
                    can_split = true;    //there is at least two distinct values in at least one attribute
                    can_use_attribute = true;

                    left_gini = Util.gini(left_class_dist, left_size);
                    right_gini = Util.gini(right_class_dist, right_size);

                    weighted_gini = ((double) left_size / total_size * left_gini) +
                            ((double) right_size / total_size * right_gini);


                    //Note: equal case not handled explicitly
                    if (weighted_gini < best_weighted_gini) {
                        best_weighted_gini = weighted_gini;
                        best_attribute_per_attrib = attributes[i];
                        cur_threshold = (cur_threshold + values.get(j + 1).key) / 2;
                        best_threshold_per_attrib = cur_threshold;

                        _best_split_pos = j;

                        if (debug) {
                            DblIntPair pair1 = values.get(j);
                            DblIntPair pair2 = values.get(j + 1);


                            //						System.out.println("a: " + temp_best_attribute + " t: " + temp_best_threshold + " left: "
                            //						+ left_class_dist.toString() + " right: " + right_class_dist + " wg: " + weighted_gini);
                            //						System.out.println("p1: " + pair1 + " p2: " + pair2);

                            //						str = " left: " + left_class_dist.toString() + " right: " + right_class_dist;

                            if (bestThreshold == 0) {
                                //							System.out.println("1.b = 0");
                            }
                        }

                    } // end best case update

                } // end split point

                if (debug) {
                    //					System.out.println("a: " + best_attribute + " t: " + best_threshold + " left: " + left_class_dist.toString() + " right: " + right_class_dist);
                }

            } //end each attribute

            //			if (best_threshold == 0) {
            //				System.out.println("2.b = 0 //skip this attrib: same val " + same_value);
            //			}
            //			System.out.println("a: " + best_attribute + " t: " + best_threshold + " left: " + left_class_dist.toString() + " right: " + right_class_dist + "wg: " + weighted_gini);

            if (can_use_attribute) {
                bestAttribute = best_attribute_per_attrib;
                bestThreshold = best_threshold_per_attrib;
                if (debug) {
                    _best_gini_per_attrib[used_attributes] = best_weighted_gini;
                }
                used_attributes++;
            } else {

                if (debug) {
                    //no gain from this attribute, skip this attrib
                    System.out.println("cant place a split point for attrib: " + i + " temp_best_attribute: " + best_attribute_per_attrib
                            + " temp_best_threshold: " + best_threshold_per_attrib);
                }

            }

        }// end attrib loop

        if (debug) {
            //			System.out.println("final a: " + best_attribute + " t: " + best_threshold + " wg:" + best_weighted_gini + " sp: " + str);
        }

        //if we can make a sensible split using any attribute, split the data,
        //else return null -> assign max gini to this during evaluation, if there
        //are no splitter  better than that, use class distribution at node for prediction;
        if (can_split) {
            return splitByIndices(nodeTrainData, trainIndices);
        } else {
            //if no attribute can give us a valid split point eg. if all values are same
            //			System.out.println("cannot split this data in a sensible way...");
            return null;
        }

    }

    /***
     * data is the full dataset, indices are indices of data that only reached the node
     *
     */
    public TIntObjectMap<TIntArrayList> splitByIndices(Dataset nodeTrainData, Indexer trainIndices) throws Exception {
        if (nodeTrainData.isMultivariate()) {
            throw new MultivariateDataNotSupportedException();
        }

        TIntObjectHashMap<TIntArrayList> splits = new TIntObjectHashMap<TIntArrayList>();
        TimeSeries series;

        splits = new TIntObjectHashMap<>(NUM_CHILDREN);
        splits.put(LEFT_BRANCH, new TIntArrayList());
        splits.put(RIGHT_BRANCH, new TIntArrayList());

        int[] indices = trainIndices.getIndex();
        //TODO verify
        //assert nodeTrainData.size() == indices.length
        //assert order of data in nodeTrainData and order of indices to original data match
        for (int i = 0; i < indices.length; i++) {
            //NOTE: not using indices[i] here because nodeTrainDataset is a subset and it may be transformed data
            series = nodeTrainData.getSeries(i);
            //TODO using only dimension 1
            if (series.data()[0][bestAttribute] < bestThreshold) {
                splits.get(LEFT_BRANCH).add(indices[i]);
            } else {
                splits.get(RIGHT_BRANCH).add(indices[i]);
            }
        }

        return splits;
    }


    @Override
    public int predict(TimeSeries query, Dataset testData, int queryIndex) throws Exception {
        //TODO using only dimension 1
        if (query.data()[0][bestAttribute] < bestThreshold) {
            return LEFT_BRANCH;
        } else {
            return RIGHT_BRANCH;
        }
    }

}
