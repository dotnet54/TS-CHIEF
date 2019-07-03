package transforms;

import java.util.concurrent.ThreadLocalRandom;

import core.AppContext;
import datasets.TSDataset;
import datasets.TimeSeries;
import util.PrintUtilities;
import util.Util;

public class TSF implements Transform{

	protected int numIntervals;
	protected int[][] intervals;
	
	protected final int MAX_ITERATIONS = 1000;
	
	protected int minIntervalLength = AppContext.tsf_min_interval;
	
	public TSF(int numIntervals) {
		this.numIntervals = numIntervals;
	}
	
	
	public void fit(TSDataset train) throws Exception {
		int fullLength = train.length();

		intervals = new int[numIntervals][2];
		
		for (int i = 0; i < numIntervals; i++) {
			
			//generate interval - old method
//	        int intervalLength = ThreadLocalRandom.current().nextInt(minIntervalLength, datasetLength);
//	        intervals[i][0]=ThreadLocalRandom.current().nextInt(0, datasetLength - intervalLength);	//start pos
//	        intervals[i][1]=intervals[i][0]+intervalLength;	//end pos
	        
			//generate interval -- this method gives a better distribution
			int j = 0;
	        do {
		        intervals[i][0]=ThreadLocalRandom.current().nextInt(0, fullLength);	//start pos
		        intervals[i][1]=ThreadLocalRandom.current().nextInt(0, fullLength);	//end pos
		        j++;
	        } while(Math.abs(intervals[i][0]  - intervals[i][1]) < minIntervalLength && j < MAX_ITERATIONS);
	        
	        if (j>= MAX_ITERATIONS) {
	        	throw new Exception("Failed to find a good interval for TSF after MAX_ITERATIONS. Please change min_intevral length");
	        }
	        
	        int temp;
	        if (intervals[i][1] < intervals[i][0]) { //swap
	        	temp = intervals[i][1];
	        	intervals[i][1] = intervals[i][0];
	        	intervals[i][0] = temp;
	        }
	        
		}
		
	}
	
	public TSDataset transform(TSDataset train) {
		int datsetSize = train.size();
		
		TSDataset output = new TSDataset(datsetSize, 3 * numIntervals);

		for (int i = 0; i < datsetSize; i++) {
            double[] series = train.get_series(i).getData();
            double[] newseries = new double[3 * numIntervals];
            
			for(int j = 0;j < numIntervals;j++){
                FeatureSet f= new FeatureSet();
                f.setFeatures(series, intervals[j][0], intervals[j][1]);		
//                System.out.println(j + " interval: " + intervals[i][j][0] + "-" + intervals[i][j][1]);
                newseries[j*3] = f.mean;
                newseries[j*3+1] = f.stDev;
                newseries[j*3+2] = f.slope;
                
			}              
            
            TimeSeries ts = new TimeSeries(newseries, train.get_class(i));
            ts.original_series = train.get_series(i);
            ts.transformed_series = true;
            output.add(ts);
		}
		
		return output;
	}
	
	public TimeSeries transformSeries(TimeSeries series) {
		
        double[] newData = new double[3 * numIntervals];
        
		for(int j = 0;j < numIntervals;j++){
            FeatureSet f = new FeatureSet(); //TODO optimize, make static
            f.setFeatures(series.getData(), intervals[j][0], intervals[j][1]);		
//                System.out.println(j + " interval: " + intervals[i][j][0] + "-" + intervals[i][j][1]);
            newData[j*3] = f.mean;
            newData[j*3+1] = f.stDev;
            newData[j*3+2] = f.slope;
            
		}              
        
        TimeSeries ts = new TimeSeries(newData, series.getLabel());
        ts.original_series = series;
        ts.transformed_series = true;

		return ts;
	}
	
    public static class FeatureSet{
        public double mean;
        public double stDev;
        public double slope;
        public synchronized void setFeatures(double[] data, int start, int end){
            double sumX=0,sumYY=0;
            double sumY=0,sumXY=0,sumXX=0;
            int length=end-start+1;
            for(int i=start;i<=end;i++){
                sumY+=data[i];
                sumYY+=data[i]*data[i];
                sumX+=(i-start);
                sumXX+=(i-start)*(i-start);
                sumXY+=data[i]*(i-start);
            }
            mean=sumY/length;
            stDev=sumYY-(sumY*sumY)/length;
            slope=(sumXY-(sumX*sumY)/length);
            if(sumXX-(sumX*sumX)/length!=0)
                slope/=sumXX-(sumX*sumX)/length;
            else
                slope=0;
            stDev/=length;
            if(stDev==0)    //Flat line
                slope=0;
//            else
//                stDev=Math.sqrt(stDev);
            if(slope==0)
                stDev=0;
        }
        public void setFeatures(double[] data){
            setFeatures(data,0,data.length-1);
        }
        @Override
        public String toString(){
            return "mean="+mean+" stdev = "+stDev+" slope ="+slope;
        }
    } 
	
	public String toString() {
		return "TSF_transform[num_intervals="+numIntervals+"]";
	}
	
	public static void main(String[] args) {
		try {
			
			TSDataset train = Util.loadTrainSet("DistalPhalanxOutlineCorrect");
			
			TSF transformer = new TSF(train.length());
			
			transformer.fit(train);
			TSDataset transformed =  transformer.transform(train);
			
			System.out.println("tsf: " + transformed.size());
			System.out.println("tsd:\n" + transformed.get_series(0));

    
		}catch(Exception e) {			
            PrintUtilities.abort(e);
		}
	}
}
