package data.dev.dataframe;

import java.util.ArrayList;
import java.util.List;

public class STColumn<T>{

    protected List<T> columnData;
    protected T className;
    protected String name;

    public STColumn(String name, T className){
        this.name = name;
        this.className = className;
        this.columnData = new ArrayList<>();
    }

    public void add(T value){
        columnData.add(value);
    }

    public void add(int index, T value){
        columnData.add(index, value);
    }

    public T get(int i){
        return columnData.get(i);
    }

    public int size(){
        return this.columnData.size();
    }

    public String name(){
        return this.name;
    }

    public void addNaN(int i){
        this.columnData.add(i, null);
    }

    public void addNaN(){
        this.columnData.add(null);
    }

}
