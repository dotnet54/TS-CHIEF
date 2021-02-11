package data.timeseries;

public interface Indexer {

    public int[] getIndex();

    public void setIndex(int[] index);

    public boolean hasDataset();

    public Dataset getDataset();

    public void setDataset(Dataset dataset);

}
