package trees;

import core.Classifier;
import core.ClassifierResult;
import data.timeseries.Dataset;

public class TSChiefForestResult extends ClassifierResult {

    protected TSChiefTreeResult baseModelResults[];

    public TSChiefForestResult(TSCheifForest forest){
        super(forest);
        baseModelResults = new TSChiefTreeResult[forest.getSize()];
    }

    public TSChiefTreeResult[] getBaseModelResults() {
        return baseModelResults;
    }

    public void setBaseModelResults(TSChiefTreeResult[] baseModelResults) {
        this.baseModelResults = baseModelResults;
    }

    public void addBaseModelResult(int index, TSChiefTreeResult result) {
        this.baseModelResults[index] = result;
    }

}
