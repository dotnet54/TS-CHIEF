package trees.splitters.rise.dev;

import java.util.HashMap;
import java.util.Random;
import application.AppConfig;
import application.AppConfig.RifFilters;
import core.TransformContainer;
import core.exceptions.NotSupportedException;
import data.timeseries.*;
import transforms.PS;
import trees.splitters.rise.ACF;
import trees.splitters.rise.ARMA;
import trees.splitters.rise.PACF;
import util.PrintUtilities;
import util.Util;

public class RIFTransformContainer implements TransformContainer {

	
	int[][][] intervals;
	int num_transforms;
	int m;
	Random rand = AppConfig.getRand();
	boolean fitted = false;
	int min_interval_length = AppConfig.rif_min_interval;
	boolean debug = true;
	private int dataset_length;
	
	protected ACF acf_filter;
	protected PACF pacf_filter;
	protected ARMA arma_filter;
	protected PS ps_filter;

	public HashMap<String, MTSDataset> transformed_datasets;	//change String keys to Int keys

	public RIFTransformContainer(int num_transforms) {
		this.num_transforms = num_transforms;
        intervals =new int[num_transforms][][];
        
        acf_filter = new ACF();
        pacf_filter = new PACF();
        arma_filter = new ARMA();
        ps_filter = new PS();
        
        transformed_datasets = new HashMap<>();
	}
	
//	private void generateRandomInterval(int i, int j) throws Exception {
////        int interval_length = AppConfig.getRand().nextInt(min_interval_length, dataset_length);
////        intervals[i][j][0]=AppConfig.getRand().nextInt(0, dataset_length - interval_length);	//start pos
////        intervals[i][j][1]=intervals[i][j][0]+interval_length;	//end pos
//		intervals[i][j] = Util.getRandomIntervalSamplingLengthFirst(rand, AppContext.rif_min_interval, dataset_length, dataset_length);
//	}
	
	
	@Override
	public void fit(Dataset train) throws Exception {
		int dataset_size = train.size();
		dataset_length = train.length();
		
		m = 1 ;//

        for(int i=0;i<num_transforms;i++){
        	intervals[i]=new int[m][2];  //Start and end pos //TODO note m

	        for(int j=0;j<m;j++){

//	        	generateRandomInterval(i, j);
	        	intervals[i][j] = Util.getRandomIntervalSamplingLengthFirst(rand, AppConfig.rif_min_interval, dataset_length, dataset_length);
	        	
	        	//split using this interval
	        	
	        	//
	        	
	           if (debug) {
//	        	   System.out.println(j + " interval: " + intervals[i][j][0] + "-" + intervals[i][j][1]);
	           }
	        }        	
        }
        
        throw new Exception("Temporarily Disabled Feature.....");
	}
	
	
	
	@Override
	public Dataset transform(Dataset train) throws Exception{
		int dataset_size = train.size();

		if (train.isMultivariate()) {
			throw new NotSupportedException();
		}
		
        for(int i=0;i<num_transforms;i++){      	
        	MTSDataset newDataset = new MTSDataset(dataset_size);
        	transformed_datasets.put(i + "", newDataset);
        	
            for(int n=0;n<dataset_size;n++){
                //extract the interval
				//TODO using only dimension 1
                double[] series= train.getSeries(n).data()[0];
                
                //TODO 0?
                int len = intervals[i][0][1] - intervals[i][0][1];
                double[][] tmp = new double[1][len];
                System.arraycopy(series, intervals[i][0][0], tmp, 0, len);
      
                double[] newseries = convert(series, RifFilters.ACF_PACF_ARMA);
                
                TimeSeries ts = new MTimeSeries(newseries, train.getClass(n));
//                ts.original_series = train.getSeries(n);
//                ts.transformed_series = true;
                newDataset.add(ts);
            }     
                

        	
        }
		
		
		//TODO FIXME
		return null;	//NOTE: tree level transformations are currently disabled -- this is a double check to make sure that the code fails
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
	
//	public static void main(String[] args) {
//		try {
//
//			MTSDataset train = Util.loadTrainSet("ItalyPowerDemand");
//
//			RIFTransformContainer tran = new RIFTransformContainer(1);
//
//			tran.fit(train);
//			tran.transform(train);
//			MTSDataset transformed = tran.transformed_datasets.get("0");
//
//			System.out.println("rif: " + transformed.size() + ":" + transformed.length());
//
//			System.out.println("rif:\n " + transformed.get_series(0));
//
//
//		}catch(Exception e) {
//            PrintUtilities.abort(e);
//		}
//	}

	
}
