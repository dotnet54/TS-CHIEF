package data.dev.dataframe;

import java.util.ArrayList;
import java.util.List;

public class SDnlColumn extends SColumn{

    protected SDataFrame.DTYPE dtype = SDataFrame.DTYPE.dbl;
    protected List<Integer> data;

    public SDnlColumn(String name){
        super(name);
        this.data = new ArrayList<>();
    }
}
