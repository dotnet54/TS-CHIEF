package trees.splitters.st;

import java.util.Arrays;

public class Shapelet {
    public static final double ROUNDING_ERROR_CORRECTION = 0.000000000000001;

	public int series_index;
	public int label;
	public int start = 0;
	public int length = 0;
	public double[] shapelet;
	public boolean is_normalized;
	
	public Shapelet(double[] series, int start, int length, int label, int index, boolean normalize) {
		this.series_index = index;
		this.label = label;
		this.start = start;
		this.length = length;
		this.is_normalized = normalize;
		
		//TODO throw 
		shapelet = Arrays.copyOfRange(series, start, start + length);
		
		if (normalize) {
			shapelet = Shapelet.zNormalise(shapelet, false);
		}
		
	}
	
	public double[] get_shapelet(double[] series) {
		return Arrays.copyOfRange(series, start, start + length);
	}
	
	public static long getNumShapelets(int n, int m, int min_length,int max_length, int step) {
		long sum = 0;
		int length;
		long temp = 0;
		
		for (length = min_length; length <= max_length; length += step) {
			temp = 0;
			for (int i = 0; i < m - length; i++) {
				temp++;
			}
			temp++;
			sum += temp;
//			System.out.println("length: " + length + " = " + temp);
		}
		return sum * n;
	}
	
    /**
    * Z-Normalise a time series
    *
    * @param input the input time series to be z-normalised
    * @param classValOn specify whether the time series includes a class value
    * (e.g. an full instance might, a candidate shapelet wouldn't)
    * @return a z-normalised version of input
    */
	//@reference
	//code based on/copied from timeseriesweka code, bakeoff paper
   public static double[] zNormalise(double[] input, boolean classValOn)
   {
       double mean;
       double stdv;

       int classValPenalty = classValOn ? 1 : 0;
       int inputLength = input.length - classValPenalty;

       double[] output = new double[input.length];
       double seriesTotal = 0;
       for (int i = 0; i < inputLength; i++)
       {
           seriesTotal += input[i];
       }

       mean = seriesTotal / (double) inputLength;
       stdv = 0;
       double temp;
       for (int i = 0; i < inputLength; i++)
       {
           temp = (input[i] - mean);
           stdv += temp * temp;
       }

       stdv /= (double) inputLength;

       // if the variance is less than the error correction, just set it to 0, else calc stdv.
       stdv = (stdv < ROUNDING_ERROR_CORRECTION) ? 0.0 : Math.sqrt(stdv);
       
       //System.out.println("mean "+ mean);
       //System.out.println("stdv "+stdv);
       
       for (int i = 0; i < inputLength; i++)
       {
           //if the stdv is 0 then set to 0, else normalise.
           output[i] = (stdv == 0.0) ? 0.0 : ((input[i] - mean) / stdv);
       }

       if (classValOn)
       {
           output[output.length - 1] = input[input.length - 1];
       }

       return output;
   }		
}
