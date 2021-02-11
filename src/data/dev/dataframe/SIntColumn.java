package data.dev.dataframe;

import java.util.ArrayList;
import java.util.List;

public class SIntColumn extends SColumn {

    protected SDataFrame.DTYPE dtype = SDataFrame.DTYPE.integer;
    protected List<Integer> data;

    public SIntColumn(String name){
        super(name);
        this.data = new ArrayList<>();
    }
}
