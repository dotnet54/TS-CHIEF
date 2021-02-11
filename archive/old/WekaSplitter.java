package trees.splitters.old;

import gnu.trove.map.TIntObjectMap;
import trees.TSCheifTree;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;

public class WekaSplitter implements NodeSplitter {
	private TSCheifTree.Node node;

	
	Classifier c;
	
	public WekaSplitter(TSCheifTree.Node node) {
		c = new RandomForest();
        ((RandomForest)c).setNumTrees(500);
	}
	
	@Override
	public TIntObjectMap<Dataset> train_splitter(Dataset sample) throws Exception {
		// TODO Auto-generated method stub
		
		
//        c.buildClassifier(data[0]);

		
		
		return null;
	}

	@Override
	public int predict_by_splitter(double[] query) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

}
