package trees.splitters.rise;

import java.util.ArrayList;
import java.util.List;

import core.exceptions.MultivariateDataNotSupportedException;
import data.timeseries.*;
import org.jtransforms.fft.DoubleFFT_1D;

import application.AppConfig;
import application.AppConfig.RifFilters;
import util.Util;

public class RISETransform {

	protected final int MAX_ITERATIONS = 10000;
	
//	protected int[][] intervals;	//rif transformer uses only one interval unlike the tsf
//	protected int maxLag;	//max lag for each interval is stored in the [][4] 4rd dim of intervals array
	
	protected final int MAXLAG_INDEX = 3;
	protected final int LENGTH_INDEX = 2;
	
	protected List<int[]> intervals;
	protected int minIntervalLength;;

	protected RifFilters filter_type;
	protected int num_intervals = 1;
	
	protected transient DoubleFFT_1D fft = null;	
	
	protected int num_attribs;
	
	public RISETransform(RifFilters filter_type, int num_intervals, int num_attribs) {
		
		minIntervalLength = AppConfig.rif_min_interval;
		
//		if (minIntervalLength > 1/3 * L) {
//			minIntervalLength = 1/3 * L;
//		}
		
		this.num_intervals = num_intervals;
		this.num_attribs = num_attribs;	// should be in fit func?? TODO
		this.filter_type = filter_type;
		
		intervals= new ArrayList<int[]>();
	}
	
	public void fit(Dataset train) throws Exception {
		int fullLength = train.length();
		
//		//find approx num of intervals needed to get min num of needed attributes
//		if (AppContext.tsf_num_intervals == 0) {
//			if (filter_type.equals(RifFilters.ACF) || filter_type.equals(RifFilters.PACF) || filter_type.equals(RifFilters.ARMA)) {
////				#attribs = 0.25 * maxLag[interval_i]
//				num_intervals = 1;
//				
//			}else if (filter_type.equals(RifFilters.PS)) {
////				#attribs = ith interval length
//
//				num_intervals = 1;
//			}else if (filter_type.equals(RifFilters.DFT)) {
//				num_intervals = 1;
//			}
//			
//			//TODO note updating this
//			
//		}
		
		num_attribs = 0;
		
		while(num_attribs < AppConfig.rif_splitters_per_node) {
			int[] intervalInfo = new int[4];	//[start_index,end_index,length,maxLag]
			intervals.add(intervalInfo);
			
//			generateRandInterval(interval, fullLength);
//	        int interval_length = interval[1] - interval[0];
			
			int[] interval = Util.getRandomIntervalSamplingLengthFirst(AppConfig.getRand(), AppConfig.rif_min_interval, fullLength, fullLength);

			int maxLag=(interval[LENGTH_INDEX])/4;
			if(maxLag>100)
			    maxLag=100;
			if(maxLag<AppConfig.rif_min_interval)
			    maxLag=(interval[LENGTH_INDEX]);	
			
			//for acf,pacf and arma, maxLag gives length of the transformed vector
			//if length is less than min num attribs we need, we are going to generate more random intervals
			//TODO refactor --- clean up this code 
			intervalInfo[0] = interval[0]; //start
			intervalInfo[1] = interval[1]; //end
			intervalInfo[LENGTH_INDEX] = interval[LENGTH_INDEX];
			intervalInfo[MAXLAG_INDEX] = maxLag;	
			num_attribs+= maxLag;
		}

	}
	
	public Dataset transform(Dataset train) throws Exception {

		if (train.isMultivariate()){
			throw new MultivariateDataNotSupportedException();
		}

		//TODO using only first interval
		int datsetSize = train.size();
		Dataset output = new MTSDataset(datsetSize);
		
//		for (int i = 0; i < intervals.size(); i++) {
//			int interval_length = intervals.get(i)[1] - intervals.get(i)[0];
//			this.fft =  new DoubleFFT_1D(interval_length);
//			
//		}
		int i = 0;
		this.fft =  new DoubleFFT_1D(intervals.get(i)[LENGTH_INDEX]);


		for (int s = 0; s < datsetSize; s++) {
			//TODO using only dimension 1
            double[] series = train.getSeries(s).data()[0];
            
            //shifaz - check if this copy is necessary? can modify convert function to work on a given range
            double[] tmp = new double[intervals.get(i)[LENGTH_INDEX]];
            System.arraycopy(series, intervals.get(i)[0], tmp, 0, intervals.get(i)[LENGTH_INDEX]);	
  
            double[] newseries = convert(tmp, filter_type, intervals.get(i)[MAXLAG_INDEX]);     
            
            TimeSeries ts = new MTimeSeries(newseries, train.getClass(s));
//            ts.original_series = train.getSeries(s);
//            ts.transformed_series = true;
            output.add(ts);
		}
		
		return output;
	}
	
	public TimeSeries transformSeries(TimeSeries series) {
		//TODO using only one interval
		int i = 0;
		        
        double[] tmp = new double[intervals.get(i)[LENGTH_INDEX]];
        //TODO using only dimension 1
        System.arraycopy(series.data()[0], intervals.get(i)[0], tmp, 0, intervals.get(i)[LENGTH_INDEX]);

        double[] newData = convert(tmp, filter_type, intervals.get(i)[MAXLAG_INDEX]);             
        
        TimeSeries ts = new MTimeSeries(newData, series.label());
//        ts.original_series = series;
//        ts.transformed_series = true;

		return ts;
	}
	
	private double[] convert(double[] data, RifFilters filter, int maxLag) {
		double[] out = null;
        
		if (filter == RifFilters.ACF) {
			out = ACF.transformArray(data, maxLag);

			for (int i = 0; i < out.length; i++) {
				if (out[i] == Double.NaN) {
					out[i] = 0;
				}
			}
			
		}else if (filter == RifFilters.PACF) {

			out = PACF.transformArray(data, maxLag);

			for (int i = 0; i < out.length; i++) {
				if (out[i] == Double.NaN) {
					out[i] = 0;
				}
			}
			
		}else if (filter == RifFilters.ARMA) {

			double[] acf = ACF.transformArray(data, maxLag);
			double[] pacf = PACF.transformArray(acf, maxLag);
			double[] arma = ARMA.transformArray(data, acf, pacf, maxLag, false);
			
			int len =  arma.length;
			
			out = new double[len];
			
			System.arraycopy(arma, 0, out, 0, arma.length);

			for (int i = 0; i < out.length; i++) {
				if (out[i] == Double.NaN) {
					out[i] = 0;
				}
			}
			
		}else if (filter == RifFilters.ACF_PACF_ARMA) {

			double[] acf = ACF.transformArray(data, maxLag);
			double[] pacf = PACF.transformArray(acf, maxLag);
			double[] arma = ARMA.transformArray(data, acf, pacf, maxLag, false);
			
			int len = acf.length + pacf.length + arma.length;
			
			out = new double[len];
			
			System.arraycopy(acf, 0, out, 0, acf.length);
			System.arraycopy(pacf, 0, out, acf.length, pacf.length);
			System.arraycopy(arma, 0, out, acf.length + pacf.length, arma.length);

			for (int i = 0; i < out.length; i++) {
				if (out[i] == Double.NaN) {
					out[i] = 0;
				}
			}
			
		}else if (filter == RifFilters.DFT) {

	        out = transformSpectral(data, RifFilters.DFT);

		}else if (filter == RifFilters.PS) {

	        out = transformSpectral(data, RifFilters.PS);

		}else if (filter == RifFilters.ACF_PACF_ARMA_PS_combined) {

			double[] acf = ACF.transformArray(data, maxLag);
			double[] pacf = PACF.transformArray(acf, maxLag);
			double[] arma = ARMA.transformArray(data, acf, pacf, maxLag, false);
			
	        double[] spectral = transformSpectral(data, RifFilters.PS);
			
			int len = acf.length + pacf.length + arma.length + spectral.length;
			
			out = new double[num_attribs * 4];	//TODO check
			
			//new changes in v1.1.17 adding num_attribs_per_component 
			System.arraycopy(acf, 0, out, 0, Math.min(num_attribs, acf.length));
			System.arraycopy(pacf, 0, out, acf.length, Math.min(num_attribs,pacf.length));
			System.arraycopy(arma, 0, out, acf.length + pacf.length, Math.min(num_attribs,arma.length));
			System.arraycopy(spectral, 0, out, acf.length + pacf.length + arma.length, Math.min(num_attribs,spectral.length));

			for (int i = 0; i < out.length; i++) {
				if (out[i] == Double.NaN) {
					out[i] = 0;
				}
			}
		}else if (filter == RifFilters.ACF_PACF_ARMA_DFT) {

			double[] acf = ACF.transformArray(data, maxLag);
			double[] pacf = PACF.transformArray(acf, maxLag);
			double[] arma = ARMA.transformArray(data, acf, pacf, maxLag, false);
			
	        double[] spectral = transformSpectral(data, RifFilters.DFT);
			
			int len = acf.length + pacf.length + arma.length + spectral.length;
			
			out = new double[len];
			
			System.arraycopy(acf, 0, out, 0, acf.length);
			System.arraycopy(pacf, 0, out, acf.length, pacf.length);
			System.arraycopy(arma, 0, out, acf.length + pacf.length, arma.length);
			System.arraycopy(spectral, 0, out, acf.length + pacf.length + arma.length, spectral.length);

			for (int i = 0; i < out.length; i++) {
				if (out[i] == Double.NaN) {
					out[i] = 0;
				}
			}
		}
		
		
		
		return out;
	}	
	
	
	//just a temp function -- move this code to a filter
	private double[] transformSpectral(double[] input, RifFilters subFilter) {
		//extract this code to a clean PS filter in future, for now, just keeping here to prevent unnecessarily creating lots of objects
		//
		
		if (subFilter == RifFilters.DFT) {
			double[] dft = new double[2 * input.length];
		    System.arraycopy(input, 0, dft, 0, input.length);
	//        this.fft.realForward(dft);
	        this.fft.complexForward(dft);
	//        ps[1] = 0; // DC-coefficient imaginary part

	        return dft;
	        
		}else{ // use PS
			double[] dft = new double[2 * input.length];
		    System.arraycopy(input, 0, dft, 0, input.length);
	//        this.fft.realForward(dft);
	        this.fft.complexForward(dft);
	//        ps[1] = 0; // DC-coefficient imaginary part
	        
	        //TODO using PS only because HiveCOTE uses it, we could just use DFT here, right?
	        
	        double[] ps = new double[input.length];
	
	        for(int j=0;j<input.length;j++){
	        	ps[j] = dft[j*2] * dft[j*2] + dft[j*2+1] * dft[j*2+1];
	        }
	        
	        return ps;
		}
		
	}
	
	
	//NOTE: not using now, use Util.* functions
	
//	//assumes intervals_array is allocated already, this makes things faster
//	//intervals_array[x], x must be at least 2
//	private void generateRandInterval(int[] intervals_array, int fullLength) throws Exception {
//		int j = 0;
//        do {
//        	intervals_array[0]=AppConfig.getRand().nextInt(0, fullLength);	//start pos
//        	intervals_array[1]=AppConfig.getRand().nextInt(0, fullLength);	//end pos
//	        j++;
//        } while(Math.abs(intervals_array[0]  - intervals_array[1]) < minIntervalLength && j < MAX_ITERATIONS);
//        
//        if (j>= MAX_ITERATIONS) {
//        	throw new Exception("Error: Failed to find a good interval for RISE after MAX_ITERATIONS. Please decrease min_interval length parameter");
//        }
//        
//        int temp;
//        if (intervals_array[1] < intervals_array[0]) { //swap
//        	temp = intervals_array[1];
//        	intervals_array[1] = intervals_array[0];
//        	intervals_array[0] = temp;
//        }
//        
//	}
	
	public String toString() {
		if (intervals != null) {
			return "RIF_transform["+num_intervals+ ", filter_type" + "]";
		}
		return filter_type+ ",untrained";
	}
	
}
