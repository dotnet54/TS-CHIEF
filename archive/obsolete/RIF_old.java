package development.obsolete;


import org.jtransforms.fft.DoubleFFT_1D;

import application.AppConfig;
import application.AppConfig.RifFilters;
import data.timeseries.TimeSeries;
import data.timeseries.UTSDataset;
import util.PrintUtilities;
import util.Util;

public class RIF_old implements Transform{

	protected int[][] intervals;	//rif transformer uses only one interval unlike the tsf
	
	protected int minIntervalLength;;
	
//	protected ACF acf_filter;
//	protected PACF pacf_filter;
//	protected ARMA arma_filter;
//	protected PS ps_filter;	
	protected final int MAX_ITERATIONS = 1000;
	protected int maxLag;

	protected RifFilters filter_type;
	protected int num_intervals = 1;
	
	protected transient DoubleFFT_1D fft = null;	
	
	protected int num_attribs;
	
	public RIF_old(RifFilters filter_type, int num_intervals, int num_attribs) {
		
		minIntervalLength = AppConfig.rif_min_interval;
		this.num_intervals = num_intervals;
		this.num_attribs = num_attribs;	// should be in fit func?? TODO
		
		this.filter_type = filter_type;
		
//        acf_filter = new ACF();
//        pacf_filter = new PACF();
//        arma_filter = new ARMA();
//        ps_filter = new PS();
        
	}
	
	public void fit(UTSDataset train) throws Exception {
		int fullLength = train.length();
		
		//find approx num of intervals needed to get min num of needed attributes
		if (AppConfig.tsf_num_intervals == 0) {
			if (filter_type.equals(RifFilters.ACF) || filter_type.equals(RifFilters.PACF) || filter_type.equals(RifFilters.ARMA)) {
//				#attribs = 0.25 * maxLag[interval_i]
				num_intervals = 1;
				
			}else if (filter_type.equals(RifFilters.PS)) {
//				#attribs = ith interval length

				num_intervals = 1;
			}else if (filter_type.equals(RifFilters.DFT)) {
				num_intervals = 1;
			}
			
			//TODO note updating this
			
		}


		intervals = new int[num_intervals][2];// 2 = start and end pos
		
		
//		//generate interval - old method
//        int intervalLength = AppConfig.getRand().nextInt(minIntervalLength, datasetLength);
//        interval[0]=AppConfig.getRand().nextInt(0, datasetLength - intervalLength);	//start pos
//        interval[1]=interval[0]+intervalLength;	//end pos

		for (int i = 0; i < intervals.length; i++) {
			
			int j = 0;
	        do {
	        	intervals[i][0]=AppConfig.getRand().nextInt(fullLength);	//start pos
	        	intervals[i][1]=AppConfig.getRand().nextInt(fullLength);	//end pos
		        j++;
	        } while(Math.abs(intervals[i][0]  - intervals[i][1]) < minIntervalLength && j < MAX_ITERATIONS);
	        
	        if (j>= MAX_ITERATIONS) {
	        	throw new Exception("Failed to find a good interval for RISE after MAX_ITERATIONS. Please change min_intevral length");
	        }
	        
	        int temp;
	        if (intervals[i][1] < intervals[i][0]) { //swap
	        	temp = intervals[i][1];
	        	intervals[i][1] = intervals[i][0];
	        	intervals[i][0] = temp;
	        }			
		}
		

        
	}
	
	public UTSDataset transform(UTSDataset train) {
		//TODO using only first interval
		int i = 0;
		
		int datsetSize = train.size();
        int interval_length = intervals[i][1] - intervals[i][0];		
		
//		this.num_attribs_to_keep = Math.min(interval_length, num_attribs_to_keep);
//		this.num_attribs_per_component = this.num_attribs_to_keep / 4;
		
		UTSDataset output = new UTSDataset(datsetSize);
		
		this.fft =  new DoubleFFT_1D(interval_length);

		
		//maxLag = min(max(0.25 * len, 10), 100) 
		//, where len = random interval length, which is expected to be on average 1/3 * l, where l = length of full series.
		//this expectation is due to the way we generate random intervals (two uniform_rand(0,L) integers and swap them if start < end)
		//Expected mean = 1/3L comes from the distribution of absolute difference of two numbers from uniform distribution
		maxLag=(interval_length)/4;
		if(maxLag>100)
		    maxLag=100;
		if(maxLag<10)
		    maxLag=(interval_length);	
		
		for (int s = 0; s < datsetSize; s++) {
            double[] series = train.get_series(s).getData();
            
            //shifaz - check if this copy is necessary? can modify convert function to work on a given range
            double[] tmp = new double[interval_length];
            System.arraycopy(series, intervals[i][0], tmp, 0, interval_length);	
  
            double[] newseries = convert(tmp, filter_type);     
            
            TimeSeries ts = new TimeSeries(newseries, train.get_class(s));
            ts.original_series = train.get_series(s);
            ts.transformed_series = true;
            output.add(ts);
		}
		
		return output;
	}
	
	public TimeSeries transformSeries(TimeSeries series) {
		//TODO using only one interval
		int i = 0;
		
        int len = intervals[i][1] - intervals[i][0];
        
        //maxLag = min(max(0.25 * len, 10), 100) 
        //, where len = random interval length, which is expected to be on average 1/3 * l, where l = length of full series.
        //this expectation is due to the way we generate random intervals (two uniform_rand(0,L) integers and swap them if start < end)
        //Expected mean = 1/3L comes from the distribution of absolute difference of two numbers from uniform distribution
        maxLag=(len)/4;
        if(maxLag>100)
            maxLag=100;
        if(maxLag<10)
            maxLag=(len);	
        
        double[] tmp = new double[len];
        System.arraycopy(series.getData(), intervals[i][0], tmp, 0, len);

        double[] newData = convert(tmp, filter_type);             
        
        TimeSeries ts = new TimeSeries(newData, series.getLabel());
        ts.original_series = series;
        ts.transformed_series = true;

		return ts;
	}
	
	private double[] convert(double[] data, RifFilters filter) {
		double[] out = null;
        
		if (filter == RifFilters.ACF) {

//          acf_filter.setMaxLag(maxLag);
//          acf_filter.setNormalized(false);
//          pacf_filter.setMaxLag(maxLag);
//          arma_filter.setMaxLag(maxLag);
//          arma_filter.setUseAIC(false);
//			double[] acf = acf_filter.transformArray(data);
//			double[] pacf = pacf_filter.transformArray(data);
//			double[] arma = arma_filter.transformArray(data);
	
			out = ACF.transformArray(data, maxLag);

			for (int i = 0; i < out.length; i++) {
				if (out[i] == Double.NaN) {
					out[i] = 0;
				}
			}
			
		}else if (filter == RifFilters.PACF) {

//          acf_filter.setMaxLag(maxLag);
//          acf_filter.setNormalized(false);
//          pacf_filter.setMaxLag(maxLag);
//          arma_filter.setMaxLag(maxLag);
//          arma_filter.setUseAIC(false);
//			double[] acf = acf_filter.transformArray(data);
//			double[] pacf = pacf_filter.transformArray(data);
//			double[] arma = arma_filter.transformArray(data);

			out = PACF.transformArray(data, maxLag);

			for (int i = 0; i < out.length; i++) {
				if (out[i] == Double.NaN) {
					out[i] = 0;
				}
			}
			
		}else if (filter == RifFilters.ARMA) {

//          acf_filter.setMaxLag(maxLag);
//          acf_filter.setNormalized(false);
//          pacf_filter.setMaxLag(maxLag);
//          arma_filter.setMaxLag(maxLag);
//          arma_filter.setUseAIC(false);
//			double[] acf = acf_filter.transformArray(data);
//			double[] pacf = pacf_filter.transformArray(data);
//			double[] arma = arma_filter.transformArray(data);
	
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

//            acf_filter.setMaxLag(maxLag);
//            acf_filter.setNormalized(false);
//            pacf_filter.setMaxLag(maxLag);
//            arma_filter.setMaxLag(maxLag);
//            arma_filter.setUseAIC(false);
//			double[] acf = acf_filter.transformArray(data);
//			double[] pacf = pacf_filter.transformArray(data);
//			double[] arma = arma_filter.transformArray(data);
	
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
			
			out = new double[num_attribs_per_component * 4];
			
			//new changes in v1.1.17 adding num_attribs_per_component 
			System.arraycopy(acf, 0, out, 0, Math.min(num_attribs_per_component, acf.length));
			System.arraycopy(pacf, 0, out, acf.length, Math.min(num_attribs_per_component,pacf.length));
			System.arraycopy(arma, 0, out, acf.length + pacf.length, Math.min(num_attribs_per_component,arma.length));
			System.arraycopy(spectral, 0, out, acf.length + pacf.length + arma.length, Math.min(num_attribs_per_component,spectral.length));

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
	
	public String toString() {
		if (intervals != null) {
			return "RIF_transform["+num_intervals +",mLag=" + this.maxLag +","+ filter_type + "]";
		}
		return "untrained";
	}
	
//	public static void main(String[] args) {
//		try {
//			
//			TSDataset train = Util.loadTrainSet("DistalPhalanxOutlineCorrect");
//			
//			RIF transformer = new RIF(RifFilters.ACF_PACF_ARMA_PS_combined, 50);
//			
//			transformer.fit(train);
//			TSDataset transformed =  transformer.transform(train);
//			
//			System.out.println("rif: " + transformed.size());
//			System.out.println("rif:\n" + transformed.get_series(0));
//
//    
//		}catch(Exception e) {			
//            PrintUtilities.abort(e);
//		}
//	}
}
