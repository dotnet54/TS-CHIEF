// Copyright (c) 2016 - Patrick Schäfer (patrick.schaefer@hu-berlin.de)
// Distributed under the GLP 3.0 (See accompanying file LICENSE)

//modified by shifaz 2018

package trees.splitters.boss;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import data.timeseries.MTimeSeries;
import data.timeseries.TimeSeries;

/**
 * Symbolic Fourier Approximation as published in Schäfer, P., Högqvist, M.:
 * SFA: a symbolic fourier approximation and index for similarity search in high
 * dimensional datasets. In: EDBT, ACM (2012)
 */
public class SFA implements Serializable {
	private static final long serialVersionUID = -3903361341617350743L;

	// distribution of Fourier values
	public transient ArrayList<ValueLabel>[] orderLine;

	public HistogramType histogramType = HistogramType.EQUI_DEPTH;

	public int alphabetSize = 256;
	public byte neededBits = (byte) Words.binlog(this.alphabetSize);
	public int wordLength = 0;
	public boolean initialized = false;
	public boolean lowerBounding = true;

	public int maxWordLength;

	// The Momentary Fourier Transform
	public MFT transformation;

	// use binning / bucketing
	public double[][] bins;

	public enum HistogramType {
		EQUI_FREQUENCY, EQUI_DEPTH, INFORMATION_GAIN
	}

	// for the MFT classifier
	private boolean mftUseMaxOrMin = false;

	static class ValueLabel implements Serializable {
		private static final long serialVersionUID = 4392333771929261697L;

		public double value;
		public double label;

		public ValueLabel() {
		}

		public ValueLabel(double key, Double label) {
			this.value = key;
			this.label = label != null ? label : 0;
		}

		@Override
		public String toString() {
			return "" + this.value + ":" + this.label;
		}
	}

	public static class Words {
		public static int binlog(int bits) {
			int log = 0;
			if ((bits & 0xffff0000) != 0) {
				bits >>>= 16;
				log = 16;
			}
			if (bits >= 256) {
				bits >>>= 8;
				log += 8;
			}
			if (bits >= 16) {
				bits >>>= 4;
				log += 4;
			}
			if (bits >= 4) {
				bits >>>= 2;
				log += 2;
			}
			return log + (bits >>> 1);
		}

		public static long createWord(short[] words, int features, byte usedBits) {
			return fromByteArrayOne(words, features, usedBits);
		}

		/**
		 * Returns a long containing the values in bytes.
		 *
		 * @param bytes
		 * @param to
		 * @param usedBits
		 * @return
		 */
		public static long fromByteArrayOne(short[] bytes, int to, byte usedBits) {
			int shortsPerLong = 60 / usedBits;
			to = Math.min(bytes.length, to);

			long bits = 0;
			int start = 0;
			long shiftOffset = 1;
			for (int i = start, end = Math.min(to, shortsPerLong + start); i < end; i++) {
				for (int j = 0, shift = 1; j < usedBits; j++, shift <<= 1) {
					if ((bytes[i] & shift) != 0) {
						bits |= shiftOffset;
					}
					shiftOffset <<= 1;
				}
			}

			return bits;
		}
	}

	public SFA() {
	}

	public SFA(HistogramType histogramType) {
		this(histogramType, false);
	}

	public SFA(HistogramType histogramType, boolean mftUseMaxOrMin) {
		reset();
		this.histogramType = histogramType;
		this.mftUseMaxOrMin = mftUseMaxOrMin;
	}

	public void reset() {
		this.initialized = false;
		this.orderLine = null;
		this.bins = null;
	}

	@SuppressWarnings("unchecked")
	private void init(int l, int alphabetSize) {
		this.wordLength = l;
		this.maxWordLength = l;
		this.alphabetSize = alphabetSize;
		this.initialized = true;

		// l-dimensional bins
		this.alphabetSize = alphabetSize;
		this.neededBits = (byte) Words.binlog(alphabetSize);

		this.bins = new double[l][alphabetSize - 1];
		for (double[] row : this.bins) {
			Arrays.fill(row, Double.MAX_VALUE);
		}

		this.orderLine = new ArrayList[l];
		for (int i = 0; i < this.orderLine.length; i++) {
			this.orderLine[i] = new ArrayList<>();
		}
	}

	/**
	 * Transforms a single time series to its SFA word
	 *
	 * @param UTimeSeries a sample
	 * @return
	 */
	public short[] transform(TimeSeries UTimeSeries) {
		return transform(UTimeSeries, null);
	}

	/**
	 * Transforms a single time series to its SFA word
	 *
	 * @param UTimeSeries    a sample
	 * @param approximation the DFT approximation, if available, else pass 'null'
	 * @return
	 */
	public short[] transform(TimeSeries UTimeSeries, double[] approximation) {
		if (!this.initialized) {
			throw new RuntimeException("Please call fitTransform() first.");
		}
		if (approximation == null) {
			// get approximation of the time series
			approximation = this.transformation.transform(UTimeSeries, this.maxWordLength);
		}

		// use lookup table (bins) to get the word from the approximation
		return quantization(approximation);
	}

	/**
	 * Transforms a set of samples to SFA words
	 *
	 * @param samples a set of samples
	 * @return
	 */
	public short[][] transform(TimeSeries[] samples) {
		if (!this.initialized) {
			throw new RuntimeException("Please call fitTransform() first.");
		}
		short[][] transform = new short[samples.length][];
		for (int i = 0; i < transform.length; i++) {
			transform[i] = transform(samples[i], null);
		}

		return transform;
	}

	/**
	 * Transforms a set of time series to SFA words.
	 *
	 * @param samples       a set of samples
	 * @param approximation the DFT approximations, if available, else pass 'null'
	 * @return
	 */
	public short[][] transform(TimeSeries[] samples, double[][] approximation) {
		if (!this.initialized) {
			throw new RuntimeException("Please call fitTransform() first.");
		}
		short[][] transform = new short[samples.length][];
		for (int i = 0; i < transform.length; i++) {
			transform[i] = transform(samples[i], approximation[i]);
		}

		return transform;
	}

	/**
	 * Quantization of a DFT approximation to its SFA word
	 *
	 * @param approximation the DFT approximation of a time series
	 * @return
	 */
	public short[] quantization(double[] approximation) {
		int i = 0;
		short[] word = new short[approximation.length];
		for (double value : approximation) {
			// lookup character:
			short c = 0;
			for (; c < this.bins[i].length; c++) {
				if (value < this.bins[i][c]) {
					break;
				}
			}
			word[i++] = c;
		}
		return word;
	}

	/**
	 * Quantization of a DFT approximation to its SFA word (using bytes for each
	 * character)
	 *
	 * @param approximation the DFT approximation of a time series
	 * @return
	 */
	public byte[] quantizationByte(double[] approximation) {
		int i = 0;
		byte[] word = new byte[approximation.length];
		for (double value : approximation) {
			// lookup character:
			byte c = 0;
			for (; c < this.bins[i].length; c++) {
				if (value < this.bins[i][c]) {
					break;
				}
			}
			word[i++] = c;
		}
		return word;
	}

	protected void sortOrderLine() {
		for (List<ValueLabel> element : this.orderLine) {
			Collections.sort(element, new Comparator<ValueLabel>() {
				@Override
				public int compare(ValueLabel o1, ValueLabel o2) {
					int comp = Double.compare(o1.value, o2.value);
					if (comp != 0) {
						return comp;
					}
					return Double.compare(o1.label, o2.label);
				}
			});
		}
	}


	/**
	 * Extracts sliding windows from the time series and trains SFA based on the
	 * sliding windows. At the end of this call, the quantization bins are set.
	 *
	 * @param series   A set of samples
	 * @param windowLength The queryLength of each sliding window
	 * @param wordLength   the SFA word-queryLength
	 * @param symbols      the SFA alphabet size
	 * @param normMean     if set, the mean is subtracted from each sliding window
	 */
	public void fitWindowing(TimeSeries[] series, int windowLength, int wordLength, int symbols, boolean normMean,
							 boolean lowerBounding) {
		this.transformation = new MFT(windowLength, normMean, lowerBounding, this.mftUseMaxOrMin);

		ArrayList<TimeSeries> sa = new ArrayList<>(series.length * series[0].length() / windowLength);

		for (TimeSeries t : series) {
			sa.addAll(Arrays.asList(t.getDisjointSequences(windowLength, normMean)));
		}
		fitTransform(sa.toArray(new MTimeSeries[] {}), wordLength, symbols, normMean);
	}

	/**
	 * Extracts sliding windows from a time series and transforms it to its SFA
	 * word.
	 * <p>
	 * Returns the SFA words as short[] (from Fourier transformed windows). Each
	 * short corresponds to one character.
	 *
	 * @param series a sample
	 * @return
	 */
	public short[][] transformWindowing(TimeSeries series) {
		double[][] mft = this.transformation.transformWindowing(series, this.maxWordLength);

		short[][] words = new short[mft.length][];
		for (int i = 0; i < mft.length; i++) {
			words[i] = quantization(mft[i]);
		}

		return words;
	}

	/**
	 * Extracts sliding windows from a time series and applies the Fourier
	 * Transform.
	 * <p>
	 * Returns the Fourier transformed windows.
	 *
	 * @param series a sample
	 * @return
	 */
	public double[][] transformWindowingDouble(TimeSeries series) {
		return this.transformation.transformWindowing(series, this.maxWordLength);
	}

	/**
	 * Extracts sliding windows from a time series and transforms it to its SFA
	 * word.
	 * <p>
	 * Returns the SFA words as a single int (compacts the characters into one int).
	 *
	 * @param ts
	 * @param wordLength
	 * @return
	 */
	public int[] transformWindowingInt(TimeSeries ts, int wordLength) {
		short[][] words = transformWindowing(ts);
		int[] intWords = new int[words.length];
		for (int i = 0; i < words.length; i++) {
			intWords[i] = (int) Words.createWord(words[i], wordLength, this.neededBits);
		}
		return intWords;
	}

	/**
	 * Trains the SFA boss based on a set of samples. At the end of this call, the
	 * quantization bins are set.
	 *
	 * @param samples    the samples to use for training.
	 * @param wordLength Length of the resulting SFA words. Each character of a word
	 *                   corresponds to one Fourier value. Even characters (starting
	 *                   with 0) are real values and uneven characters are imaginary
	 *                   values. A shorter word queryLength corresponds to a
	 *                   stronger low-pass filtering of the time series.
	 * @param symbols    the alphabet size, i.e. number of quantization bins to use
	 * @param normMean   true: sets mean to 0 for each time series.
	 * @return the Fourier transformation of the time series.
	 */
	public double[][] fitTransformDouble(TimeSeries[] samples, int wordLength, int symbols, boolean normMean) {
		if (!this.initialized) {
			init(wordLength, symbols);

			if (this.transformation == null) {
				this.transformation = new MFT(samples[0].length(), normMean, this.lowerBounding,
						this.mftUseMaxOrMin);
			}
		}

		double[][] transformedSamples = fillOrderline(samples, wordLength);

		if (this.histogramType == HistogramType.EQUI_DEPTH) {
			divideEquiDepthHistogram();
		} else if (this.histogramType == HistogramType.EQUI_FREQUENCY) {
			divideEquiWidthHistogram();
		} else if (this.histogramType == HistogramType.INFORMATION_GAIN) {
//			divideHistogramInformationGain();
		}

		this.orderLine = null;

		return transformedSamples;
	}

	/**
	 * Same as fitTransformDouble but returns the SFA words instead of the Fourier
	 * transformed time series.
	 */
	public short[][] fitTransform(TimeSeries[] samples, int wordLength, int symbols, boolean normMean) {
		return transform(samples, fitTransformDouble(samples, wordLength, symbols, normMean));
	}

	/**
	 * Fills data in the orderline
	 *
	 * @param samples A set of samples
	 */
	protected double[][] fillOrderline(TimeSeries[] samples, int l) {
		double[][] transformedSamples = new double[samples.length][];

		for (int i = 0; i < samples.length; i++) {
			// z-normalization
			samples[i].normalize();

			// approximation
			transformedSamples[i] = this.transformation.transform(samples[i], l);

			for (int j = 0; j < transformedSamples[i].length; j++) {
				// round to 2 decimal places to reduce noise
				double value = Math.round(transformedSamples[i][j] * 100.0) / 100.0;
				this.orderLine[j].add(new ValueLabel(value, samples[i].label().doubleValue()));	//TODO int to dbl convt
			}
		}

		// Sort ascending by value
		sortOrderLine();

		return transformedSamples;
	}

	/**
	 * Use equi-width binning to divide the orderline
	 */
	protected void divideEquiWidthHistogram() {
		int i = 0;
		for (List<ValueLabel> elements : this.orderLine) {
			if (!elements.isEmpty()) {
				// apply the split
				double first = elements.get(0).value;
				double last = elements.get(elements.size() - 1).value;
				double intervalWidth = (last - first) / (this.alphabetSize);

				for (int c = 0; c < this.alphabetSize - 1; c++) {
					this.bins[i][c] = intervalWidth * (c + 1) + first;
				}
			}
			i++;
		}
	}

	/**
	 * Use equi-depth binning to divide the orderline
	 */
	protected void divideEquiDepthHistogram() {
		// For each real and imaginary part
		for (int i = 0; i < this.bins.length; i++) {
			// Divide into equi-depth intervals
			double depth = this.orderLine[i].size() / (double) (this.alphabetSize);

			int pos = 0;
			long count = 0;
			for (ValueLabel value : this.orderLine[i]) {
				if (++count > Math.ceil(depth * (pos + 1)) && (pos == 0 || this.bins[i][pos - 1] != value.value)) {
					this.bins[i][pos++] = value.value;
				}
			}
		}
	}

//	/**
//	 * Use information-gain to divide the orderline
//	 */
//	protected void divideHistogramInformationGain() {
//		// for each Fourier coefficient: split using maximal information gain
//		for (int i = 0; i < this.orderLine.length; i++) {
//			List<ValueLabel> element = this.orderLine[i];
//			if (!element.isEmpty()) {
//				ArrayList<Integer> splitPoints = new ArrayList<>();
//				findBestSplit(element, 0, element.size(), this.alphabetSize, splitPoints);
//
//				Collections.sort(splitPoints);
//
//				// apply the split
//				for (int j = 0; j < splitPoints.size(); j++) {
//					double value = element.get(splitPoints.get(j) + 1).value;
//					// double value = (element.get(splitPoints.get(j)).value +
//					// element.get(splitPoints.get(j)+1).value)/2.0;
//					this.bins[i][j] = value;
//				}
//			}
//		}
//	}
//
//	protected static double entropy(TObjectIntHashMap<Double> frequency, double total) {
//		double entropy = 0;
//		double log2 = 1.0 / Math.log(2.0);
//		for (int element : frequency.values()) {
//			double p = element / total;
//			if (p > 0) {
//				entropy -= p * Math.log(p) * log2;
//			}
//		}
//		return entropy;
//	}
//
//	protected static double calculateInformationGain(TObjectIntHashMap<Double> cIn, TObjectIntHashMap<Double> cOut,
//			double class_entropy, double total_c_in, double total) {
//		double total_c_out = (total - total_c_in);
//		return class_entropy - total_c_in / total * entropy(cIn, total_c_in)
//				- total_c_out / total * entropy(cOut, total_c_out);
//	}
//
//	protected void findBestSplit(List<ValueLabel> element, int start, int end, int remainingSymbols,
//			List<Integer> splitPoints) {
//
//		double bestGain = -1;
//		int bestPos = -1;
//
//		// class entropy
//		int total = end - start;
//		TObjectIntHashMap<Double> cIn = new TObjectIntHashMap<>();
//		TObjectIntHashMap<Double> cOut = new TObjectIntHashMap<>();
//		for (int pos = start; pos < end; pos++) {
//			cOut.putOrAdd(element.get(pos).label, 1, 1);
//		}
//		double class_entropy = entropy(cOut, total);
//
//		int i = start;
//		Double lastLabel = element.get(i).label;
//		i += moveElement(element, cIn, cOut, start);
//
//		for (int split = start + 1; split < end - 1; split++) {
//			Double label = element.get(i).label;
//			i += moveElement(element, cIn, cOut, split);
//
//			// only inspect changes of the label
//			if (!label.equals(lastLabel)) {
//				double gain = calculateInformationGain(cIn, cOut, class_entropy, i, total);
//
//				if (gain >= bestGain) {
//					bestPos = split;
//					bestGain = gain;
//				}
//			}
//			lastLabel = label;
//		}
//
//		if (bestPos > -1) {
//			splitPoints.add(bestPos);
//
//			// recursive split
//			remainingSymbols = remainingSymbols / 2;
//			if (remainingSymbols > 1) {
//				if (bestPos - start > 2 && end - bestPos > 2) { // enough data points?
//					findBestSplit(element, start, bestPos, remainingSymbols, splitPoints);
//					findBestSplit(element, bestPos, end, remainingSymbols, splitPoints);
//				} else if (end - bestPos > 4) { // enough data points?
//					findBestSplit(element, bestPos, (end - bestPos) / 2, remainingSymbols, splitPoints);
//					findBestSplit(element, (end - bestPos) / 2, end, remainingSymbols, splitPoints);
//				} else if (bestPos - start > 4) { // enough data points?
//					findBestSplit(element, start, (bestPos - start) / 2, remainingSymbols, splitPoints);
//					findBestSplit(element, (bestPos - start) / 2, end, remainingSymbols, splitPoints);
//				}
//			}
//		}
//	}
//
//	protected int moveElement(List<ValueLabel> element, TObjectIntHashMap<Double> cIn, TObjectIntHashMap<Double> cOut,
//			int pos) {
//		cIn.putOrAdd(element.get(pos).label, 1, 1);
//		cOut.putOrAdd(element.get(pos).label, -1, -1);
//		return 1;
//	}

	public void printBins() {
		System.out.print("[");
		for (double[] element : this.bins) {
			System.out.print("-Inf\t");
			for (double element2 : element) {
				String e = element2 != Double.MAX_VALUE ? ("" + element2) : "Inf";
				System.out.print("," + e + "\t");
			}
			System.out.println(";");
		}
		System.out.println("]");
	}

}
