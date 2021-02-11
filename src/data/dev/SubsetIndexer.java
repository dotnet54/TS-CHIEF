package data.dev;

import data.timeseries.Dataset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubsetIndexer {

    protected Dataset dataset;  //values of index always reference with respect to this dataset
    protected Dataset subset;   //subset of the dataset, it may or may not be transformed data
    protected int[] index;      //size of index is <= subset.size()

    public SubsetIndexer(Dataset dataset){
        this.dataset =  dataset;
    }

    public SubsetIndexer(int size){
        this.index = new int[size];
        this.dataset =  null;
    }

    public SubsetIndexer(int[] index){
        this.index = index;
        this.dataset =  null;
    }

    public SubsetIndexer(int size, Dataset dataset){
        this.index = new int[size];
        this.dataset =  dataset;
    }

    public SubsetIndexer(int[] index, Dataset dataset){
        this.index = index;
        this.dataset =  dataset;
    }

    public int[] getIndex() {
        return index;
    }

    public void setIndex(int[] index) {
        this.index = index;
    }

    public boolean hasDataset(){
        return dataset != null;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Dataset getSubset() {
        return subset;
    }

    public void setSubset(Dataset subset) {
        this.subset = subset;
    }

    public void getSeries(int i){
        this.dataset.getSeries(this.index[i]); //TODO subset
    }

    public void sampleSequentially(int sampleSize){
        if (sampleSize > dataset.size()){
            sampleSize = dataset.size();
        }
        this.index = new int[sampleSize];
        for (int i = 0; i < index.length; i++) {
            index[i] = i; // just add everything
        }
    }

    public void sampleRandomly(int sampleSize){
        if (sampleSize > dataset.size()){
            sampleSize = dataset.size();
        }
        // NOTE: this is a slow way to shuffle an array, faster way would be to use Yates-Fisher algorithm
        // TODO refactor to use Yates-Fisher implementation in util class
        List<Integer> sample = new ArrayList<Integer>(sampleSize);
        for (int i = 0; i < sampleSize; i++) {
            sample.add(i);
        }
        // TODO support seeding, test Random vs ThreadLocalRandom -- called in tree classes when bagging, which may have its own thread
        Collections.shuffle(sample);
        this.index =  sample.stream().mapToInt(i->i).toArray();
    }

    public void sync(Dataset subset) throws Exception {
        if (hasDataset()){
            int datasetSize = this.dataset.size();
            int subsetSize = subset.size();

            if (index == null || subsetSize != index.length){
                this.index = new int[subsetSize];
            }

            for (int i = 0; i < datasetSize; i++) {
                for (int j = 0; j < subsetSize; j++) {
                    //TODO implement equals method in TimeSeries instead of using ==
                    if (this.dataset.getSeries(i) == subset.getSeries(j)) {
                        index[j] = i;
                    }
                }
            }
        }else{
            throw new Exception("Attach a dataset to the indexer before calling sync");
        }
    }
}
