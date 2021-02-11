package data.dev.dataframe;

import java.util.ArrayList;
import java.util.List;

public abstract class SColumn {

    protected String name;
    protected SDataFrame.DTYPE dtype = SDataFrame.DTYPE.str;

    protected List<Object> data;

    public SColumn(String name){
        this.name = name;
    }

    public int size(){
        return this.data.size();
    }



}
