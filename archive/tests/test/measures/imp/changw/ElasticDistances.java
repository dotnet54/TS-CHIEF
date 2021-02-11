package test.measures.imp.changw;

import java.text.DecimalFormat;

public abstract class ElasticDistances {
    public enum Measures {
        Euclidean,
        FullDTW,
        DTW,
        FullDDTW,
        DDTW,
        WDTW,
        WDDTW,
        LCSS,
        TWE,
        ERP,
        MSM
    }

    final static int MAX_SEQ_LENGTH = 4000;         // maximum sequence length possible
    final static int DIAGONAL = 0;                  // value for diagonal
    final static int LEFT = 1;                      // value for left
    final static int UP = 2;                        // value for up

    public static final DecimalFormat df4 = new DecimalFormat("#0.####");
}
