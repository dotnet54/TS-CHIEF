package transforms;

import datasets.TSDataset;
import datasets.TimeSeries;

public class PACF {

	public int maxLag;

	public void setMaxLag(int a) {
		maxLag = a;
	}

	public TSDataset transform(TSDataset input) {
		int size = input.size();
		int length = input.length();

		// TODO can do this better?
		maxLag = length / 4;
		if (maxLag > 100) {
			maxLag = 100;
		} else if (maxLag < 10) {
			maxLag = length;
		}

		TSDataset output = new TSDataset(size);

		for (int i = 0; i < size; i++) {
			// 2. Fit Autocorrelations
			double[] autos = ACF.transformArray(input.get_series(i).getData(), maxLag);
			// 3. Form Partials
			double[][] partials = transformACFArray(autos);
			// 5. Find parameters
			double[] pi = new double[maxLag];
			for (int k = 0; k < maxLag; k++) { // Set NANs to zero
				if (Double.isNaN(partials[k][k]) || Double.isInfinite(partials[k][k])) {
					pi[k] = 0;
				} else
					pi[k] = partials[k][k];
			}

			TimeSeries ts = new TimeSeries(pi, input.get_class(i));
			output.add(ts);
		}

		return output;
	}

	public double[] transformArray(double[] data) {
		// 2. Fit Autocorrelations
		double[] acf = ACF.transformArray(data, maxLag);
		// 3. Form Partials
		double[][] pacf = transformACFArray(acf);
		// 5. Find parameters
		double[] pi = new double[maxLag];
		for (int k = 0; k < maxLag; k++) { // Set NANs to zero
			if (Double.isNaN(pacf[k][k]) || Double.isInfinite(pacf[k][k])) {
				pi[k] = 0;
			} else
				pi[k] = pacf[k][k];
		}

		return pi;
	}	
	
	public static double[] transformArray(double[] acf, int maxLag) {
		// 3. Form Partials
		double[][] pacf = transformACFArray(acf);
		// 5. Find parameters
		double[] pi = new double[maxLag];
		for (int k = 0; k < maxLag; k++) { // Set NANs to zero
			if (Double.isNaN(pacf[k][k]) || Double.isInfinite(pacf[k][k])) {
				pi[k] = 0;
			} else
				pi[k] = pacf[k][k];
		}

		return pi;
	}		
	
	public static double[][] transformACFArray(double[] r) {
		// Using the Durban-Leverson
		int p = r.length;
		double[][] phi = new double[p][p];
		double numerator, denominator;
		phi[0][0] = r[0];

		for (int k = 1; k < p; k++) {
			// Find diagonal k,k
			// Naive implementation, should be able to do with running sums
			numerator = r[k];
			for (int i = 0; i < k; i++)
				numerator -= phi[i][k - 1] * r[k - 1 - i];
			denominator = 1;
			for (int i = 0; i < k; i++)
				denominator -= phi[k - 1 - i][k - 1] * r[k - 1 - i];
			phi[k][k] = numerator / denominator;
			// Find terms 1,k to k-1,k
			for (int i = 0; i < k; i++)
				phi[i][k] = phi[i][k - 1] - phi[k][k] * phi[k - 1 - i][k - 1];
		}
		return phi;
	}
}
