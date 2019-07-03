package dev;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import core.AppContext.RifFilters;
import datasets.BossDataset;
import datasets.TSDataset;
import datasets.TimeSeries;
import dev.TSFTransformContainer.FeatureSet;
import transforms.ACF;
import transforms.ARMA;
import transforms.PACF;
import transforms.PS;
import util.PrintUtilities;
import util.Util;

public class RIFTransformContainer implements TransformContainer {

	
	int[][][] intervals;
	int num_transforms;
	int m;
	Random rand = new Random(); //TODO use seed in AppContext
	boolean fitted = false;
	int min_interval_length = 3;
	boolean debug = true;
	private int dataset_length;
	
	protected ACF acf_filter;
	protected PACF pacf_filter;
	protected ARMA arma_filter;
	protected PS ps_filter;

	public HashMap<String, TSDataset> transformed_datasets;	//change String keys to Int keys

	public RIFTransformContainer(int num_transforms) {
		this.num_transforms = num_transforms;
        intervals =new int[num_transforms][][];
        
        acf_filter = new ACF();
        pacf_filter = new PACF();
        arma_filter = new ARMA();
        ps_filter = new PS();
        
        transformed_datasets = new HashMap<>();
	}
	
	private void generateRandomInterval(int i, int j) {
        int interval_length = ThreadLocalRandom.current().nextInt(min_interval_length, dataset_length);
        intervals[i][j][0]=ThreadLocalRandom.current().nextInt(0, dataset_length - interval_length);	//start pos
        intervals[i][j][1]=intervals[i][j][0]+interval_length;	//end pos
	}
	
	
	@Override
	public void fit(TSDataset train) {
		int dataset_size = train.size();
		dataset_length = train.length();
		
		m = 1 ;//

        for(int i=0;i<num_transforms;i++){
        	intervals[i]=new int[m][2];  //Start and end pos //TODO note m

	        for(int j=0;j<m;j++){

	        	generateRandomInterval(i, j);
	        	
	        	//split using this interval
	        	
	        	//
	        	
	           if (debug) {
//	        	   System.out.println(j + " interval: " + intervals[i][j][0] + "-" + intervals[i][j][1]);
	           }
	        }        	
        }
	}
	
	
	
	@Override
	public TSDataset transform(TSDataset train) {
		int dataset_size = train.size();
		
		
        for(int i=0;i<num_transforms;i++){      	
        	TSDataset newDataset = new TSDataset(dataset_size);
        	transformed_datasets.put(i + "", newDataset);
        	
            for(int n=0;n<dataset_size;n++){
                //extract the interval
                double[] series= train.get_series(n).getData();
                
                //TODO 0?
                int len = intervals[i][0][1] - intervals[i][0][1];
                double[] tmp = new double[len];
                System.arraycopy(series, intervals[i][0][0], tmp, 0, len);
      
                double[] newseries = convert(series, RifFilters.ACF_PACF_ARMA);
                
                TimeSeries ts = new TimeSeries(newseries, train.get_class(n));
                ts.original_series = train.get_series(n);
                ts.transformed_series = true;
                newDataset.add(ts);
            }     
                

        	
        }
		
		
		//TODO FIXME
		return null;	
	}

	private double[] convert(double[] data, RifFilters filter) {
		double[] out = null;
		
		if (filter == RifFilters.ACF_PACF_ARMA) {
			
            int maxLag=(data.length)/4;
            if(maxLag>100)
                maxLag=100;
            if(maxLag<10)
                maxLag=(data.length);			
			
            acf_filter.setMaxLag(maxLag);
            acf_filter.setNormalized(false);
            pacf_filter.setMaxLag(maxLag);
            arma_filter.setMaxLag(maxLag);
            arma_filter.setUseAIC(false);
			double[] acf = acf_filter.transformArray(data);
			double[] pacf = pacf_filter.transformArray(data);
//			double[] arma = arma_filter.transformArray(data);
	
			int len = acf.length + pacf.length;// + arma.length;
			
			out = new double[len];
			
			System.arraycopy(acf, 0, out, 0, acf.length);
			System.arraycopy(pacf, 0, out, acf.length, pacf.length);
//			System.arraycopy(arma, 0, out, acf.length + pacf.length, arma.length);

		}
		
		return out;
	}
	
	public static void main(String[] args) {
		try {
			
			TSDataset train = Util.loadTrainSet("ItalyPowerDemand");
			
			RIFTransformContainer tran = new RIFTransformContainer(1);
			
			tran.fit(train);
			tran.transform(train);
			TSDataset transformed = tran.transformed_datasets.get("0");
			
			System.out.println("rif: " + transformed.size() + ":" + transformed.length());
    
			System.out.println("rif:\n " + transformed.get_series(0));
			
			
		}catch(Exception e) {			
            PrintUtilities.abort(e);
		}
	}

	
}
