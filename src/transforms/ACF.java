package transforms;

import datasets.TSDataset;
import datasets.TimeSeries;

public class ACF {

	private boolean normalized = false; // Assumes zero mean and unit variance
	int endTerms = 4;
	int maxLag = 300;
	int seriesLength;

	public void setMaxLag(int n) {
		maxLag = n;
	}

	public void setNormalized(boolean flag) {
		normalized = flag;
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
		
		// TODO check this again
		if (maxLag > input.length() - endTerms) // weka implementation use len + 1 - endTerms
			maxLag = input.length() - endTerms;
		if (maxLag < 0)
			maxLag = input.length();		

		TSDataset output = new TSDataset(size);

		for (int i = 0; i < size; i++) {
			double[] acf = fitAutoCorrelations(input.get_series(i).getData());

			for (int j = 0; j < acf.length; j++) {
				if (acf[j] < -1.0 || acf[j] > 1 || Double.isNaN(acf[j]) || Double.isInfinite(acf[j])) {
					acf[j] = 0;
				}
			}

			TimeSeries ts = new TimeSeries(acf, input.get_class(i));
			output.add(ts);
		}

		return output;
	}
	
	
	public double[] transformArray(double[] data) {
		double[] acf = fitAutoCorrelations(data);

		for (int j = 0; j < acf.length; j++) {
			if (acf[j] < -1.0 || acf[j] > 1 || Double.isNaN(acf[j]) || Double.isInfinite(acf[j])) {
				acf[j] = 0;
			}
		}

		return acf;
	}

	// reference: timeseriesweka source code
	public double[] fitAutoCorrelations(double[] data) {
		double[] a = new double[maxLag];

		if (!normalized) {
			for (int i = 1; i <= maxLag; i++) {
				double s1, s2, ss1, ss2, v1, v2;
				a[i - 1] = 0;
				s1 = s2 = ss1 = ss2 = 0;
				for (int j = 0; j < data.length - i; j++) {
					s1 += data[j];
					ss1 += data[j] * data[j];
					s2 += data[j + i];
					ss2 += data[j + i] * data[j + i];
				}
				s1 /= data.length - i;
				s2 /= data.length - i;
				for (int j = 0; j < data.length - i; j++)
					a[i - 1] += (data[j] - s1) * (data[j + i] - s2);
				a[i - 1] /= (data.length - i);

				v1 = ss1 / (data.length - i) - s1 * s1;
				v2 = ss2 / (data.length - i) - s2 * s2;

				a[i - 1] /= Math.sqrt(v1) * Math.sqrt(v2);
			}
		} else {
			for (int i = 1; i <= maxLag; i++) {
				a[i - 1] = 0;
				for (int j = 0; j < data.length - i; j++)
					a[i - 1] += data[j] * data[j + i];
				a[i - 1] /= data.length;
			}
		}
		return a;
	}

	public static double[] transformArray(double[] data, int mLag) {
		double[] a = new double[mLag];

		double s1, s2, ss1, ss2, v1, v2;
		for (int i = 1; i <= mLag; i++) {
			a[i - 1] = 0;
			s1 = s2 = ss1 = ss2 = 0;
			for (int j = 0; j < data.length - i; j++) {
				s1 += data[j];
				ss1 += data[j] * data[j];
				s2 += data[j + i];
				ss2 += data[j + i] * data[j + i];
			}
			s1 /= data.length - i;
			s2 /= data.length - i;

			for (int j = 0; j < data.length - i; j++)
				a[i - 1] += (data[j] - s1) * (data[j + i] - s2);
			a[i - 1] /= (data.length - i);
			v1 = ss1 / (data.length - i) - s1 * s1;
			v2 = ss2 / (data.length - i) - s2 * s2;
			a[i - 1] /= Math.sqrt(v1) * Math.sqrt(v2);
		}
		
		for (int j = 0; j < a.length; j++) {
			if (a[j] < -1.0 || a[j] > 1 || Double.isNaN(a[j]) || Double.isInfinite(a[j])) {
				a[j] = 0;
			}
		}
		
		return a;
	}

	
	public String toString() {
		return "ACF[max_lag=" + maxLag + "]";
	}
}
