package transforms;

import data.timeseries.Dataset;
import data.timeseries.MTimeSeries;
import data.timeseries.TimeSeries;
import org.jtransforms.fft.DoubleFFT_1D;

import data.timeseries.MTSDataset;
import trees.splitters.boss.FFT;

public class PS {
	
    boolean log=false;	//TODO refer to HIVECOTE paper, check if this is used or not, in the source code its set to false

    
//    private transient SimpleFFT fft;
	
	private transient DoubleFFT_1D fast_fft = null;


    public PS() {
//    	fft = new SimpleFFT();
    }
    
	public Dataset transform(Dataset input) throws Exception {
		int size = input.size();
		int length = input.length();

		Dataset output = new MTSDataset(size);
		
		Dataset fftDataset = fft.transform(input);
		
		for (int i = 0; i < size; i++) {
			double[][] data;
			
	        if(log) {
	            double l1;
                TimeSeries tsfft = fftDataset.getSeries(i);
                
                data = new double[1][tsfft.length()];
                
                for(int j=0;j<data.length;j++){
                    l1= tsfft.data(0)[j*2] * tsfft.data(0)[j*2] + tsfft.data(0)[j*2+1] * tsfft.data(0)[j*2+1];
                    data[0][j] = Math.log(l1);
                }
	        }else {
                TimeSeries tsfft = fftDataset.getSeries(i);
                
                data = new double[1][tsfft.length()];

                for(int j=0;j<data.length;j++){
                	data[0][j] = tsfft.data(0)[j*2] * tsfft.data(0)[j*2] + tsfft.data(0)[j*2+1] * tsfft.data(0)[j*2+1];
                }
	        }
					
			TimeSeries ts = new MTimeSeries(data, input.getClass(i));
			output.add(ts);
		}

		return output;
	}
	
	public double[] transformArray(double[] input) {
		double[] output = null;

		output = new double[input.length];
	    System.arraycopy(input, 0, output, 0, input.length);
        this.fast_fft = new DoubleFFT_1D(input.length);
        this.fast_fft.realForward(output);
        output[1] = 0; // DC-coefficient imaginary part
		
//		// transform
//		FFT.Complex[] c = new Complex[input.length];
//		int count = 0;
//		double seriesTotal = 0;
//		for (int j = 0; j < input.length && count < c.length; j++) { // May cut off the trailing values
//			c[count] = new Complex(input[j], 0.0);
//			seriesTotal += input[j];
//			count++;
//		}
//
//		double mean = seriesTotal / count;
//		while (count < c.length)
//			c[count++] = new Complex(mean, 0);
//
//		c = fft.dft(c);
//
//		output = new double[c.length];
//		for (int j = 0; j < c.length / 2; j++) {
//			output[2*j] = c[j].real;
//			output[2*j+1] = c[j].imag;
//		}
		
		
		return output;
	}
	
	
    public static double[] powerSpectrum(double[] d){

//Check power of 2            
        if(((d.length)&(d.length-1))!=0)    //Not a power of 2
            return null;
        FFT.Complex[] c=new FFT.Complex[d.length];
        for(int j=0;j<d.length;j++){
             c[j]=new FFT.Complex(d[j],0.0);
        }
        FFT f=new FFT();
        f.fft(c,c.length);
        double[] ps=new double[c.length];
        for(int i=0;i<c.length;i++)
            ps[i]=c[i].getReal()*c[i].getReal()+c[i].getImag()*c[i].getImag();
        return ps;
    }
    

    
}
