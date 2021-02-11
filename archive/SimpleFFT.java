package trees.splitters.boss;

import data.timeseries.UTimeSeries;
import data.timeseries.MTSDataset;

public class SimpleFFT {
	private static final double TWOPI = (Math.PI * 2);

	// TODO only dft is implemented
	public enum AlgorithmType {
		DFT, FFT
	} // If set to DFT, this will only perform a FFT if the series is length power of
		// 2, otherwise resorts to DFT

	public MTSDataset transform(MTSDataset input) {
		int size = input.size();
		int length = input.length();

		MTSDataset output = new MTSDataset(size);

		for (int i = 0; i < size; i++) {
			double[] data = null;

			// transform
			FFT.Complex[] c = new Complex[length];
			int count = 0;
			double seriesTotal = 0;
			for (int j = 0; j < length && count < c.length; j++) { // May cut off the trailing values
				c[count] = new Complex(input.getSeries(i).data(j), 0.0)[0];  //TODO using dimension 0 only
				seriesTotal += input.getSeries(i).data(j)[0]; //using dim 0 only
				count++;
			}

			double mean = seriesTotal / count;
			while (count < c.length)
				c[count++] = new Complex(mean, 0);

			c = dft(c);

			data = new double[c.length / 2];
			for (int j = 0; j < c.length / 2; j++) {
				data[2*j] = c[j].real;
				data[2*j+1] = c[j].imag;
			}

			UTimeSeries ts = new UTimeSeries(data, input.getClass(i));
			output.add(ts);
		}

		return output;
	}

	public Complex[] dft(double[] series) {
		int n = series.length;
		Complex[] dft = new Complex[n];
		for (int k = 0; k < n; k++) { // For each output element
			float sumreal = 0;
			float sumimag = 0;
			for (int t = 0; t < series.length; t++) { // For each input element
				sumreal += series[t] * Math.cos(2 * Math.PI * t * k / n);
				sumimag += -series[t] * Math.sin(2 * Math.PI * t * k / n);
			}
			dft[k] = new Complex(sumreal, sumimag);
		}
		return dft;

	}
	
    public Complex[] dft(Complex[] complex) {
        int n=complex.length;
        Complex[] dft=new Complex[n];
        for (int k = 0; k < n; k++) {  // For each output element
            float sumreal = 0;
            float sumimag = 0;
            for (int t = 0; t < complex.length; t++) {  // For each input element
                    sumreal +=  complex[t].real*Math.cos(2*Math.PI * t * k / n) + complex[t].imag*Math.sin(2*Math.PI * t * k / n);
                    sumimag += -complex[t].real*Math.sin(2*Math.PI * t * k / n) + complex[t].imag*Math.cos(2*Math.PI * t * k / n);
            }
            dft[k]=new Complex(sumreal,sumimag);
        }
        return dft;
        
     }

	public double[] transformArray(double[] data) {
		return data;
	}
}
