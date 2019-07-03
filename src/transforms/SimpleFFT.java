package transforms;

import datasets.TSDataset;
import datasets.TimeSeries;
import transforms.FFT.Complex;

public class SimpleFFT {
	private static final double TWOPI = (Math.PI * 2);

	// TODO only dft is implemented
	public enum AlgorithmType {
		DFT, FFT
	} // If set to DFT, this will only perform a FFT if the series is length power of
		// 2, otherwise resorts to DFT

	public TSDataset transform(TSDataset input) {
		int size = input.size();
		int length = input.length();

		TSDataset output = new TSDataset(size);

		for (int i = 0; i < size; i++) {
			double[] data = null;

			// transform
			FFT.Complex[] c = new Complex[length];
			int count = 0;
			double seriesTotal = 0;
			for (int j = 0; j < length && count < c.length; j++) { // May cut off the trailing values
				c[count] = new Complex(input.get_series(i).getData(j), 0.0);
				seriesTotal += input.get_series(i).getData(j);
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

			TimeSeries ts = new TimeSeries(data, input.get_class(i));
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
