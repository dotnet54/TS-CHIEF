package trees;

import core.Classifier;
import core.ClassifierResult;
import data.timeseries.Dataset;

public class TSChiefTreeResult extends ClassifierResult {

    public TSChiefTreeResult(TSCheifTree model){
        super(model);
    }

    public TSChiefTreeResult(TSCheifTree model, Dataset testData){
        super(model, testData);
    }

}
