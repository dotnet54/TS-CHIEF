package application.test.knn;

import core.Classifier;

import java.util.HashMap;
import java.util.List;

public class GridSearchCV {

    //TODO make this work with any accuracy score, currently only supports accuracy score

    protected Classifier estimator;
    protected HashMap<String, List<Object>> paramGrid;

    protected int numTrainings;
    protected List searchScores;
    protected List bestParams;

    public GridSearchCV(Classifier estimator,HashMap<String, List<Object>> paramGrid){
        this.estimator = estimator;
        this.paramGrid = paramGrid;
    }

    public void generatePermutations(){

    }

    public void fit(){

        for (String param : paramGrid.keySet()) {

        }
    }

    public void predict(){

    }

}
