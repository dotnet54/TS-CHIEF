package development.experiments;

import data.timeseries.MTSDataset;
import development.mts.measures.DTW;

public class kNN {

    public kNN(){

    }

    public void fit(MTSDataset X){

    }

    public int[] predict(MTSDataset X){
        double distance = Double.NEGATIVE_INFINITY;
        double min_distance = Double.NEGATIVE_INFINITY;

        DTW dtw = new DTW();
        int w = 0;

        for (int i = 0; i < X.size(); i++) {
            for (int j = 0; j < X.size(); j++) {
                distance = dtw.distance(X.get_series(i), X.get_series(j), 0 , w);
            }
        }

        return null;
    }
}
