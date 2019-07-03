package transforms;

import datasets.TSDataset;
import datasets.TimeSeries;

public class ARMA {

	// Max number of AR terms to consider.
	double[] ar;
	public int maxLag;
	public boolean useAIC = true;

	public void setUseAIC(boolean b) {
		useAIC = b;
	}

	public void setMaxLag(int a) {
		maxLag = a;
	}

	public TSDataset transform(TSDataset input) {
		int size = input.size();
		int length = input.length();

		double[] d;
		double[] autos;
		double[][] partials;

		TSDataset output = new TSDataset(size);

		for (int i = 0; i < size; i++) {
			d = input.get_series(i).getData();

			// 2. Fit Autocorrelations
			autos = ACF.transformArray(d, maxLag);
			// 3. Form Partials
			partials = PACF.transformACFArray(autos);
			// 4. Find bet AIC. Could also use BIC?
			int best = maxLag;
			if (useAIC)
				best = findBestAIC(autos, partials, maxLag, d);
			// 5. Find parameters
			double[] pi = new double[maxLag];
			for (int k = 0; k < best; k++)
				pi[k] = partials[k][best - 1];
			TimeSeries ts = new TimeSeries(pi, input.get_class(i));
			output.add(ts);
		}

		return output;
	}
	
	public static double[] transformArray(double data[], double[] acf, double[] pacf, int maxLag, boolean useAIC) {
		int best = maxLag;
		if (useAIC) {
			// 2. Fit Autocorrelations
//			double[] acf = ACF.transformArray(data, maxLag);
			// 3. Form Partials
			double[][] partials = PACF.transformACFArray(acf);
			// 4. Find bet AIC. Could also use BIC?			
			best = findBestAIC(acf, partials, maxLag, data);
		}
		
		double[][] partials = PACF.transformACFArray(acf);

			
		// 5. Find parameters
		double[] pi = new double[maxLag];
		for (int k = 0; k < best; k++)
			pi[k] = partials[k][best - 1];
		return pi;
	}		

//	public static double[] fitAR(double[] d) {
//		// 2. Fit Autocorrelations
//		double[] autos = ACF.transformArray(d, globalMaxLag);
//		// 3. Form Partials
//		double[][] partials = PACF.formPartials(autos);
//		// 4. Find bet AIC. Could also use BIC?
//		int best = findBestAIC(autos, partials, globalMaxLag, d);
//		// 5. Find parameters
//		double[] pi = new double[globalMaxLag];
//		for (int k = 0; k < best; k++)
//			pi[k] = partials[k][best - 1];
//		return pi;
//	}

	public static int findBestAIC(double[] autoCorrelations, double[][] partialCorrelations, int maxLag, double[] d) {
		// need the variance of the series
		double sigma2;
		int n = d.length;
		double var = 0, mean = 0;
		for (int i = 0; i < d.length; i++)
			mean += d[i];
		for (int i = 0; i < d.length; i++)
			var += (d[i] - mean) * (d[i] - mean);
		var /= (d.length - 1);
		double AIC = Double.MAX_VALUE;
		double bestAIC = Double.MAX_VALUE;
		int bestPos = 0;
		int i = 0;
		boolean found = false;
		while (i < maxLag && !found) {
			sigma2 = 1;
			for (int j = 0; j <= i; j++) {
				sigma2 -= autoCorrelations[j] * partialCorrelations[j][i];
				// System.out.println("\tStep ="+j+" incremental sigma ="+sigma2);
			}
			sigma2 *= var;
			AIC = Math.log(sigma2);
			i++;
			AIC += ((double) 2 * (i + 1)) / n;
			// System.out.println("LAG ="+i+" final sigma = "+sigma2+"
			// log(sigma)="+Math.log(sigma2)+" AIC = "+AIC);
			if (AIC == Double.NaN)
				AIC = Double.MAX_VALUE;
			if (AIC < bestAIC) {
				bestAIC = AIC;
				bestPos = i;
			} else
				found = true;
		}
		return bestPos;
	}

}
