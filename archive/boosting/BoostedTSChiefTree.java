package trees.boosting;

import trees.TSCheifForest;
import trees.TSCheifTree;

public class BoostedTSChiefTree extends TSCheifTree {

    public double beta = 1e-10; // for boosting computation

    public BoostedTSChiefTree(int tree_id, TSCheifForest forest){
        super(tree_id, forest);
    }

}
