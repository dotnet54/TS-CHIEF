package trees.splitters.tsf;

import java.util.Random;

import application.AppConfig;
import core.TransformContainer;
import data.timeseries.MTSDataset;
import data.timeseries.MTimeSeries;
import data.timeseries.Dataset;
import data.timeseries.TimeSeries;
import util.PrintUtilities;
import util.Util;

public class TSFTransformContainer implements TransformContainer {

	int[][][] intervals;
	int num_transforms;
	int m;
	Random rand = AppConfig.getRand();
	boolean fitted = false;
	int min_interval_length = 3;
	boolean debug = true;
	private int dataset_length;
	
	public TSFTransformContainer(int num_transforms) {
		this.num_transforms = num_transforms;
        intervals =new int[num_transforms][][];
	}
	
	@Override
	public void fit(Dataset train) {
		int dataset_size = train.size();
		dataset_length = train.length();
		
		m = (int)Math.sqrt(train.length()); //TODO try log2

		//generate random intervals
		//if interval is not useful, discard and get a new interval 
		//not useful if it produces a an all zero column
		
		//rare case when there is enough data
		boolean column_has_same_value = false;	//eg. 0 std or slope for the interval -> cant use this feature to split on, so generate a random new interval
		
        for(int i=0;i<num_transforms;i++){
        	intervals[i]=new int[m][2];  //Start and end pos

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
	
	private void generateRandomInterval(int i, int j) {
        int interval_length = AppConfig.getRand().nextInt(min_interval_length, dataset_length);
        intervals[i][j][0]=AppConfig.getRand().nextInt(dataset_length - interval_length);	//start pos
        intervals[i][j][1]=intervals[i][j][0]+interval_length;	//end pos
	}

	@Override
	public Dataset transform(Dataset train) {
		int dataset_size = train.size();
		
//		m = (int)Math.sqrt(train.length());

		Dataset transformed_dataset = new MTSDataset(dataset_size);
		boolean column_has_same_value = true;
		
        for(int i=0;i<num_transforms;i++){
//        	intervals[i]=new int[m][2];  //Start and end
//
//	        for(int j=0;j<m;j++){
//	           intervals[i][j][0]=rand.nextInt(train.length() - 1);       //Start point
//	           int length=rand.nextInt(train.length()-1-intervals[i][j][0]); //Min length 3
//	           intervals[i][j][1]=intervals[i][j][0]+length;
//	        }        	
	        
            for(int n=0;n<dataset_size;n++){
                //extract the interval
                double[] series= train.getSeries(n).getData();
                double[] newseries= new double[3 * m];
                
				for(int j=0;j<m;j++){
	                FeatureSet f= new FeatureSet();
	                f.setFeatures(series, intervals[i][j][0], intervals[i][j][1]);		
//	                System.out.println(j + " interval: " + intervals[i][j][0] + "-" + intervals[i][j][1]);
	                newseries[j*3] = f.mean;
	                newseries[j*3+1] = f.stDev;
	                newseries[j*3+2] = f.slope;
	                
//	                if (n > 0 && (train.get_series(n-1).getData()[j*3+1] != f.stDev 
//	                		|| train.get_series(n-1).getData()[j*3+2] != f.stDev)) {
//	                	column_has_same_value = false;
//	                	//if this interval is bad, we can try again
//	                	//how many times to keep trying?? max num times? 
//// 	                	System.out.println("WARN: check frequency of this: std or slope == 0 -- can introduce bad attribs to split");
//	                }
				}              
                
                TimeSeries ts = new MTimeSeries(newseries, train.getClass(n));
                ts.original_series = train.get_series(n);
                ts.transformed_series = true;
                transformed_dataset.add(ts);
            }     
                

        	
        }
		
		
		//TODO FIXME BUG for which i
		return transformed_dataset;	
	}

	//reference: www.timeseriesclassification.com timeseries weka source code
    public static class FeatureSet{
        public double mean;
        public double stDev;
        public double slope;
        public void setFeatures(double[] data, int start, int end){
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
    
    
    
    
	public static void main(String[] args) {
		try {
			
			MTSDataset train = Util.loadTrainSet("DistalPhalanxOutlineCorrect");
			
			TSFTransformContainer tran = new TSFTransformContainer(1);
			
			tran.fit(train);
			MTSDataset transformed =  tran.transform(train);
			
			System.out.println("done" + transformed.size());
    
		}catch(Exception e) {			
            PrintUtilities.abort(e);
		}
	}

    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
