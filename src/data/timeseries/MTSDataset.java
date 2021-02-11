package data.timeseries;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import application.AppConfig;
import data.io.DataLoader;
import data.io.TSReader;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import util.StandardDeviation;
import util.Statistics;
import util.math.doubles.StatisticsDbl;

public class MTSDataset implements Dataset, Indexable{

	protected String datasetName;
	protected String tags; // comma separated list of tags (e.g {train,test,znorm,deriv1,deriv2})
	protected int minLength;
	protected int maxLength;
	protected int dimensions;

	protected boolean hasTimestamps;
	protected boolean hasMissingValues;
	protected boolean isVariableLength;
	protected boolean isNormalized;

	protected List<TimeSeries> data;
	protected TIntIntMap classDistribution;
	protected LabelEncoder labelEncoder;	//TODO sync with classDistribution when time series are added/removed
	protected List<String> attribNames;

	// extra information if this dataset is a transformed dataset
//	protected Transform transform;

	public static final int DEFAULT_CAPACITY = 2;

	public MTSDataset() {
		this(DEFAULT_CAPACITY);
	}

	public MTSDataset(int expectedSize) {
		this.data = new ArrayList<TimeSeries>(expectedSize);
		this.classDistribution = new TIntIntHashMap();
		this.attribNames = new ArrayList<>();
		this.labelEncoder = new NonContinuousLabelEncoder();
	}

	public int size() {
		return data.size();
	}

	public int length() {
		return maxLength;
	}

	@Override
	public int minLength() {
		return this.minLength;
	}

	@Override
	public int maxLength() {
		return this.maxLength;
	}

	public int dimensions() {
		return this.dimensions;
	}

	public int[] shape() {
		return new int[]{this.size(), this.dimensions(), this.length()};
	}

	public String getName() {
		return this.datasetName;
	}

	public void setName(String datasetName) {
		this.datasetName = datasetName;
	}

	@Override
	public String getTags() {
		return this.tags;
	}

	@Override
	public void setTags(String tags) {
		this.tags = tags;
	}

	public void setDimensions(int dimensions) {
		this.dimensions = dimensions;
	}

	public boolean hasTimestamps() {
		return hasTimestamps;
	}

	public void setHasTimestamps(boolean hasTimestamps) {
		this.hasTimestamps = hasTimestamps;
	}

	public boolean hasMissingValues() {
		return hasMissingValues;
	}

	public void setHasMissing(boolean hasMissingValues) {
		this.hasMissingValues = hasMissingValues;
	}

	public boolean isMultivariate() {
		if (dimensions > 1){
			return true;
		}else{
			return false;
		}
	}

	public boolean isVariableLength() {
		return isVariableLength;
	}

	public void setIsVariableLength(boolean isVariableLength) {
		this.isVariableLength = isVariableLength;
	}

	public boolean isNormalized() {
		return isNormalized;
	}

	public void setIsNormalized(boolean isNormalized) {
		this.isNormalized = isNormalized;
	}

	public Integer[] getLabels() {
		return this.labelEncoder.getUniqueLabels();
	}

	public String[] getLabelsStrings() {
		return this.labelEncoder.getUniqueLabelStrings();
	}

	public synchronized TimeSeries add(TimeSeries series) throws Exception{

		//if first series
		if (this.data.size() == 0 ){
			this.dimensions = series.dimensions();
			this.maxLength = series.length();
		}else{
			if (series.dimensions() != this.dimensions){
				throw new Exception("Cannot add series, dataset dimensions does not match with series dimensions");
			}
			if (this.maxLength < series.length()){
				this.maxLength = series.length();
			}
		}

		Integer label = series.label();
		if (classDistribution.containsKey(label)) {
			classDistribution.put(label, classDistribution.get(label) + 1);
		} else {
			classDistribution.put(label, 1);
		}

		//TODO sync with label encoder, they get out of sync when unknown labels are added

		this.data.add(series);
		return series;
	}

	public synchronized void remove(int i) {
		Integer label = this.data.get(i).label();
		if (classDistribution.containsKey(label)) {
			int count = classDistribution.get(label);
			if (count > 0) {
				classDistribution.put(label, classDistribution.get(label) - 1);
			} else {
				classDistribution.remove(label);
			}
		}

		//TODO sync label encoder
		this.data.remove(i);

		if (this.data.size() == 0 ){
			this.dimensions = 1;
			this.maxLength = 0;
		}
	}

	public void clear(){
		this.dimensions = 1;

	}

	public TimeSeries getSeries(int i) {
		return this.data.get(i);
	}

	@Override
	public double[][] getSeriesData(int i) {
		return getSeries(i).data();
	}

	public Integer getClass(int i) {
		return this.data.get(i).label();
	}

	public int getNumClasses() {
		// TODO if class size falls to 0 when we remove data from the dataset,
		//  make sure this doesnt count 0 count classes
		return this.classDistribution.size();
	}

	public int getClassSize(Integer label) {
		return this.classDistribution.get(label);
	}

	@Override
	public int[] getUniqueClasses() {
		int[] classes = this.classDistribution.keys();
		Arrays.sort(classes);
		return classes;
	}

	public TIntIntMap getClassDistribution() {
		return this.classDistribution;
	}


	public LabelEncoder getLabelEncoder() {
		return labelEncoder;
	}

	public void setLabelEncoder(LabelEncoder labelEncoder) {
		this.labelEncoder = labelEncoder;
	}

	public Integer get_majority_class() {

		List<Integer> label_list = new ArrayList<Integer>();
		label_list.clear();            // Not necessary?

		int[] unique_class = this.getUniqueClasses();
		int maj_size = this.getClassSize(unique_class[0]);
		label_list.add(unique_class[0]);

		for (int i = 1; i < unique_class.length; i++) {
			int current_size = this.getClassSize(unique_class[i]);
			if (current_size > maj_size) {
				maj_size = current_size;
				label_list.clear();
				label_list.add(unique_class[i]);
			} else if (current_size == maj_size) {
				label_list.add(unique_class[i]);
			}
		}
//		int r = ThreadLocalRandom.current().nextInt(label_list.size());
		int r = AppConfig.getRand().nextInt(label_list.size());
		return label_list.get(r);
	}

	public double get_sum_weight() {
		double sum = 0.0;
		int size = this.size();
		for (int i = 0; i < size; i++) {
			sum += this.data.get(i).weight();
		}
		return sum;
	}

	//TODO used only for testing
	public List<TimeSeries> _get_internal_list() {
		return this.data;
	}

	public TIntObjectMap<Dataset> splitByClass() throws Exception {
		TIntObjectMap<Dataset> split = new TIntObjectHashMap<Dataset>(this.getNumClasses());
		Integer label;
		MTSDataset class_set = null;
		int size = this.size();

		for (int i = 0; i < size; i++) {
			label = this.data.get(i).label();
			if (!split.containsKey(label)) {
				class_set = new MTSDataset();
				split.put(label, class_set);
			}

			split.get(label).add(this.data.get(i));
		}

		return split;
	}

	public double gini() {
		double sum = 0.0;
		double p;
		int total_size = this.data.size();

		for (int key : classDistribution.keys()) {
			p = (double) classDistribution.get(key) / total_size;
			sum += p * p;
		}
		return 1 - sum;
	}

	public TimeSeries[] toArray(){
		return data.toArray(new TimeSeries[] {});
	}

	@Override
	public List<double[][]> _data_as_list() {
		return null;
	}

	@Override
	public List<Integer> _class_as_list() {
		return null;
	}

	@Override
	public double[][] _data_as_array() {
		return new double[0][];
	}

	@Override
	public int[] _class_as_array() {
		return new int[0];
	}

//	public double weighted_gini() throws Exception {
//		double sum = 0.0;
//		double p;
//		double total_weight = this.get_sum_weight();
//
//		// Charlotte: weigthed (from the instance) Gini for boosting
//		TIntObjectMap<Dataset> data_per_class = this.splitByClass();
//		for (int key : data_per_class.keys()) {
//			p = data_per_class.get(key).get_sum_weight() / total_weight;
//			sum += p * p;
//		}
//		return 1 - sum;
//	}

	public void shuffle() {
//		this.shuffle(System.nanoTime());
		this.shuffle(AppConfig.rand_seed); //TODO
	}

	public void shuffle(long seed) {
		Collections.shuffle(data, new Random(seed));    //TODO use thread local random??
	}

	@Override
	public Dataset shallowClone() {
		return null;
	}

	@Override
	public Dataset deepClone() {
		return null;
	}

	@Override
	public Dataset sortOn(int timestamp) {
		return null;
	}

	/**
	 * Finds the mean of whole dataset
	 *
	 * @return
	 */
	@Override
	public synchronized double getMean() {
		int size = this.data.size();
		int dimensions = this.dimensions;
		double[][] seriesData;
		double sum = 0;
		int count = 0;
//		org.apache.commons.math3.stat.descriptive.moment.Mean meanCalculaor = new Mean();

		for (int i = 0; i < size; i++) {
			seriesData = this.getSeries(i).data();
			for (int d = 0; d < dimensions; d++) {
				for (int j = 0; j < seriesData[d].length; j++) {
//					meanCalculaor.increment(seriesData[d][j]);
					sum += seriesData[d][j];
					count++;
				}
			}
		}
//		assert count == meanCalculaor.getN();
//		assert (sum / count - meanCalculaor.getResult() < 1e-6);
		return sum / count;
	}

	/**
	 * Finds the mean of whole dataset using an online algorithm
	 * TODO CHECK correctness - DEVELOPMENT only - in production, use other functions below that have been tested
	 *
	 * @return
	 */
	public synchronized double getMeanOnline(){
		int size = this.data.size();
		int dimensions = this.dimensions;
		double[][] seriesData;
		double[] aggregates = null;

		for (int i = 0; i < size; i++) {
			seriesData = this.getSeries(i).data();
			for (int d = 0; d < dimensions; d++) {
				for (int j = 0; j < seriesData[d].length; j++) {
					aggregates = StandardDeviation.stdvOnlineWelford(aggregates, seriesData[d][j], true);
				}
			}
		}
		aggregates = StandardDeviation.stdvOnlineWelford(aggregates, 0, false);
//		if (Double.isNaN(aggregates[1])){
//			throw new RuntimeException("getMean: " + aggregates[1]);
//		}
		return aggregates[1]; //just return mean component
	}

	/**
	 * Finds the std of whole dataset using an online algorithm
	 * Not the fastest method
	 * Also not the most precise method in terms of floating point issues (refer to the online method below)
	 * https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
	 *
	 * @return
	 */
	@Override
	public synchronized double getStdv() {
		int size = this.data.size();
		int dimensions = this.dimensions;
		double[][] seriesData;
		double sum = 0;
		double sumSq = 0;
		int count = 0;
		org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
				stdCalculaor = new org.apache.commons.math3.stat.descriptive.moment.StandardDeviation(false);


		for (int i = 0; i < size; i++) {
			seriesData = this.getSeries(i).data();
			for (int d = 0; d < dimensions; d++) {
				for (int j = 0; j < seriesData[d].length; j++) {
					stdCalculaor.increment(seriesData[d][j]);
					sum += seriesData[d][j];
					sumSq += (seriesData[d][j] * seriesData[d][j]);
					count++;
				}
			}
		}

		if (count == 0){
			return 0;
		}else{
			double variance = (sumSq - (sum * sum) / count) / (count); // population var
			assert count == stdCalculaor.getN();
			assert (Math.sqrt(variance) - stdCalculaor.getResult() < 1e-6);
			return Math.sqrt(variance);
		}
	}

	/**
	 * Finds the std of whole dataset using an online algorithm
	 * Refer to Welford's online algorithm
	 * https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
	 * TODO CHECK correctness - DEVELOPMENT only - in production, use other functions below that have been tested
	 *
	 * @return
	 */
	public synchronized double getStdvOnline() {
		int size = this.data.size();
		int dimensions = this.dimensions;
		double[][] seriesData;
		double[] aggregates = null;

		for (int i = 0; i < size; i++) {
			seriesData = this.getSeries(i).data();
			for (int d = 0; d < dimensions; d++) {
				for (int j = 0; j < seriesData[d].length; j++) {
					aggregates = StandardDeviation.stdvOnlineWelford(aggregates, seriesData[d][j], true);
				}
			}
		}
		aggregates = StandardDeviation.stdvOnlineWelford(aggregates, 0, false);
//		if (Double.isNaN(aggregates[4])){
//			throw new RuntimeException("getStdv: " + aggregates[1]);
//		}
		return aggregates[4];
	}

	/***
	 * Finds the mean of each dimension separately across the whole dataset
	 * Not the most efficient implementation
	 *
	 * @return
	 */
	@Override
	public double[] getMeanPerDimension() {
		int size = this.data.size();
		int dimensions = this.dimensions;
		double[][] seriesData;
		double[] meanPerDimension = new double[dimensions];
		int count;

		for (int d = 0; d < dimensions; d++) {
			count = 0;
			for (int i = 0; i < size; i++) {
				seriesData = this.getSeries(i).data();
				for (int j = 0; j < seriesData[d].length; j++) {
					meanPerDimension[d] += seriesData[d][j];
					count++;
				}
			}
			meanPerDimension[d] = meanPerDimension[d] / count;
		}

		return meanPerDimension;
	}

	/***
	 * Finds the std of each dimension separately across the whole dataset
	 *
	 * @return
	 */
	@Override
	public double[] getStdvPerDimension() {
		int size = this.data.size();
		int dimensions = this.dimensions;
		double[][] seriesData;
		double[] stdPerDimension = new double[dimensions];
		StandardDeviation.StandDeviationWelford[] stdCalculators = new StandardDeviation.StandDeviationWelford[dimensions];

		//debug - method two
		double tmp, diff, tmp2;
		StandardDeviation.StandDeviationNaive[] stdCalculators2 = new StandardDeviation.StandDeviationNaive[dimensions];
		//debug - test using apache common maths
		org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
				stdCalculaor = new org.apache.commons.math3.stat.descriptive.moment.StandardDeviation(false);

		for (int d = 0; d < dimensions; d++) {
			stdCalculators[d] = new StandardDeviation.StandDeviationWelford();
			stdCalculators2[d] = new StandardDeviation.StandDeviationNaive();
			stdCalculaor.clear();
			for (int i = 0; i < size; i++) {
				seriesData = this.getSeries(i).data();
				for (int j = 0; j < seriesData[d].length; j++) {
					stdCalculators[d].add(seriesData[d][j]);
					stdCalculators2[d].add(seriesData[d][j]);
					stdCalculaor.increment(seriesData[d][j]);
				}
			}
			stdPerDimension[d] = stdCalculators[d].std();

			//debug
			tmp = stdCalculators2[d].std();
			diff = stdPerDimension[d] - tmp;
			assert diff < 1e-6;
			tmp2 = stdCalculaor.getResult();
			assert (stdPerDimension[d] - tmp2 < 1e-6);
			assert (tmp - tmp2 < 1e-6);
		}

		return stdPerDimension;
	}


// ----------- commented 13/10/2020
//	/***
//	 * Finds the mean of each dimension separately across the whole dataset
//	 * Not the most efficient implementation -- in future, change this to use the online algorithm
//	 *
//	 * @return
//	 */
//	@Override
//	public double[] getMeanPerDimension() {
//		//NOTE mean of a subset of means == mean of the whole set iff size of the subsets are the same
//		double[][] means = getMeanPerSeriesPerDimension();
//		double[] meanPerDimension = new double[this.dimensions()];
//		double[] tempMeans = new double[means.length];
//
//		for (int dimension = 0; dimension < meanPerDimension.length; dimension++) {
//			for (int series = 0; series < means.length; series++) {
//				tempMeans[series] = means[series][dimension];
//			}
//			meanPerDimension[dimension]  = StatisticsDbl.mean(tempMeans);
//		}
//
//		return meanPerDimension;
//	}
//
//	/***
//	 * Finds the std of each dimension separately across the whole dataset
//	 * Not the most efficient implementation -- in future, change this to use the online algorithm
//	 *
//	 * @return
//	 */
//	@Override
//	public double[] getStdvPerDimension() {
//		//NOTE mean of a subset of means == mean of the whole set iff size of the subsets are the same
//		//TODO check if it works here; this is just finding the mean of stds of each dimension
//		double[][] stds = getStdvPerSeriesPerDimension();
//		double[] stdPerDimension = new double[this.dimensions()];
//		double[] tempStds = new double[stds.length];
//
//		for (int dimension = 0; dimension < stdPerDimension.length; dimension++) {
//			for (int series = 0; series < stds.length; series++) {
//				tempStds[series] = stds[series][dimension];
//			}
//			stdPerDimension[dimension] = StatisticsDbl.mean(tempStds);
//		}
//
//		return stdPerDimension;
//	}

	@Override
	public double[] getMinPerDimension() {
		throw new RuntimeException("TODO: Not Implemented");
	}

	@Override
	public double[] getMaxPerDimension() {
		throw new RuntimeException("TODO: Not Implemented");
	}

	@Override
	public double[][] getMeanPerSeriesPerDimension() {
		double[][] means = new double[this.size()][this.dimensions()];
		for (int series = 0; series < means.length; series++) {
			for (int dimension = 0; dimension < means[series].length; dimension++) {
				means[series][dimension] = StatisticsDbl.mean(this.data.get(series).data(dimension));
				if (Double.isNaN(means[series][dimension])){
					throw new RuntimeException("WARNING: terminating for debugging: stdvPerSeriesPerDimension = " + means[series][dimension]);
				}
			}
		}
		return means;
	}

	@Override
	public double[][] getStdvPerSeriesPerDimension() {
		double[][] std = new double[this.size()][this.dimensions()];
		for (int series = 0; series < std.length; series++) {
			for (int dimension = 0; dimension < std[series].length; dimension++) {
				std[series][dimension] = StatisticsDbl.stdP(this.data.get(series).data(dimension));

				if (std[series][dimension] == 0){
					std[series][dimension] = 1;
				}
				if (Double.isNaN(std[series][dimension])){
					throw new RuntimeException("WARNING: terminating for debugging: stdvPerSeriesPerDimension = " + std[series][dimension]);
				}
			}
		}
		return std;
	}

	@Override
	public double[][] getMinPerSeriesPerDimension() {
		double[][] min = new double[this.size()][this.dimensions()];
		for (int series = 0; series < min.length; series++) {
			for (int dimension = 0; dimension < min[series].length; dimension++) {
				min[series][dimension] = StatisticsDbl.min(this.data.get(series).data(dimension));
			}
		}
		return min;
	}

	@Override
	public double[][] getMaxPerSeriesPerDimension() {
		double[][] max = new double[this.size()][this.dimensions()];
		for (int series = 0; series < max.length; series++) {
			for (int dimension = 0; dimension < max[series].length; dimension++) {
				max[series][dimension] = StatisticsDbl.max(this.data.get(series).data(dimension));
			}
		}
		return max;
	}

//	//TODO check
//	private double[] meanPerDimension(double[][] means){
//		int numDimensions = this.dimensions();
//		double[] meanPerDim = new double[numDimensions];
//		double[] tempMeans = new double[means.length];
//
//		for (int dimension = 0; dimension < numDimensions; dimension++) {
//			for (int series = 0; series < means.length; series++) {
//				tempMeans[series] = means[series][dimension];
//			}
//			meanPerDim[dimension] = StatisticsDbl.mean(tempMeans);
//		}
//		return meanPerDim;
//	}

//	//TODO fix
//	private double[] stdPerDimension(double[][] stds){
//		int numDimensions = this.dimensions();
//		double[] stdPerDim = new double[numDimensions];
//		double[] tempStd = new double[stds.length];
//
//		for (int i = 0; i < numDimensions; i++) {
//			for (int j = 0; j < stds.length; j++) {
//				tempStd[j] = stds[j][i];
//			}
//			stdPerDim[i] = StatisticsDbl.stdP(tempStd);
//		}
//		return stdPerDim;
//	}

	@Override
	public void zNormalize(boolean perSeries) {
		int size = this.size();
		double[][] data;
		double[][] mean = getMeanPerSeriesPerDimension();
		double[][] std = getStdvPerSeriesPerDimension();

		if (perSeries){
			for (int series = 0; series < size; series++) {
				data =  this.getSeries(series).data();
				for (int dimension = 0; dimension < data.length; dimension++) {
					for (int timepoint = 0; timepoint < data[dimension].length; timepoint++) {
						data[dimension][timepoint] = (data[dimension][timepoint] - mean[series][dimension])
								/ std[series][dimension];

						if (Double.isNaN(data[dimension][timepoint])){
							throw new RuntimeException("WARNING: terminating for safety: zNormalize = " + data[dimension][timepoint]);
						}

					}
				}
			}
		}else {
			double [] meanPerDimension = getMeanPerDimension();
			double [] stdPerDimension = getStdvPerDimension();

			for (int series = 0; series < size; series++) {
				data =  this.getSeries(series).data();
				for (int dimension = 0; dimension < data.length; dimension++) {
					for (int timepoint = 0; timepoint < data[dimension].length; timepoint++) {
						data[dimension][timepoint] = (data[dimension][timepoint] - meanPerDimension[dimension])
								/ stdPerDimension[dimension];
					}
				}
			}
		}

		this.setTags(getTags() + ",znorm");
	}

	@Override
	public void meanNormalize(boolean perSeries) {
		int size = this.size();
		double[][] data;
		double[][] mean = getMeanPerSeriesPerDimension();

		if (perSeries){
			for (int series = 0; series < size; series++) {
				data =  this.getSeries(series).data();
				for (int dimension = 0; dimension < data.length; dimension++) {
					for (int timepoint = 0; timepoint < data[dimension].length; timepoint++) {
						data[dimension][timepoint] = (data[dimension][timepoint] - mean[series][dimension]);
					}
				}
			}
		}else{
			double [] meanPerDimension = getMeanPerDimension();
			for (int series = 0; series < size; series++) {
				data =  this.getSeries(series).data();
				for (int dimension = 0; dimension < data.length; dimension++) {
					for (int timepoint = 0; timepoint < data[dimension].length; timepoint++) {
						data[dimension][timepoint] = (data[dimension][timepoint] - meanPerDimension[dimension]);
					}
				}
			}
		}
	}

	@Override
	public void featureScale(boolean perSeries) {
		// verify
		int size = this.size();
		double scale;
		double[][] data;
		double[][] min = getMinPerSeriesPerDimension();
		double[][] max = getMaxPerSeriesPerDimension();
		for (int i = 0; i < size; i++) {
			data =  this.getSeries(i).data();
			for (int j = 0; j < data.length; j++) {
				scale = max[i][j] - min[i][j];
				for (int k = 0; k < data[j].length; k++) {
					data[j][k] = (data[j][k] - min[i][j])/scale;
				}
			}
		}
	}

	@Override
	public void saveToFile(String fileName, boolean overwrite) {
		TSReader.writeFile(this, fileName, this.labelEncoder, overwrite);
	}

//	public ListDataset shallow_clone() {
//		// TODO Auto-generated method stub
//		return null;
//	}

//	public ListDataset deep_clone() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public String getSummaryInfo(){
		StringBuilder sb = new StringBuilder();
		sb.append(datasetName);
		sb.append("(");
		sb.append("tags=");
		sb.append(tags);
		sb.append(",size=");
		sb.append(size());
		sb.append(",length=");
		sb.append(length());
		sb.append(",dims=");
		sb.append(dimensions());
		sb.append(",classes=");
		sb.append(getNumClasses());
		sb.append(")");
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		DecimalFormat df = new DecimalFormat(AppConfig.print_decimal_places);

		sb.append(this.classDistribution.toString() + "=" + df.format(this.gini())
				+ " size = " + size() + " length = " + maxLength + " Name = " + this.datasetName);
		sb.append("\n");

		int max = 10;
		int i;
		for (i = 0; i < Math.min(data.size(), max); i++) {
			sb.append(data.get(i).toString() + "\n");
		}

		if (i == max) {
			sb.append("...");
		}

		return sb.toString();
	}

	// TODO DEVELOPMENT ONLY

//	public MTSDataset bootstrap() throws Exception {
//
//		MTSDataset sample = new MTSDataset(this.size());
//		for (int i = 0; i < this.size(); i++) {
////			int r = ThreadLocalRandom.current().nextInt(this.size());
//			int r = AppConfig.getRand().nextInt(this.size());
//			sample.add(this.getSeries(r));
//		}
//		return sample;
//	}
//
//	public MTSDataset bootstrap(double weight) throws Exception {
//
//		MTSDataset sample = new MTSDataset(this.size());
//		for (int i = 0; i < this.size(); i++) {
////				int r = ThreadLocalRandom.current().nextInt(this.size());
//			int r = AppConfig.getRand().nextInt(this.size());
//			sample.add(this.getSeries(r).setWeight(weight));
//		}
//		return sample;
//	}


	/**
	 * For testing
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		String archive = "E:/data/Multivariate2018_ts/";
		String datasetName = "BasicMotions";
		MTSDataset train = DataLoader.loadTrainingSet(archive + datasetName + "/" + datasetName + "_TRAIN.ts");
		MTSDataset test = DataLoader.loadTestingSet(archive + datasetName + "/" + datasetName + "_TEST.ts", train);
		System.out.println(train.getSummaryInfo());
		System.out.println(test.getSummaryInfo());

		MTSDataset dataset = train;
		double mean = dataset.getMean();
		double meanOnline = dataset.getMeanOnline();
		double stdv = dataset.getStdv();
		double stdvOnline = dataset.getStdvOnline();

		double[] meanPerDimension = dataset.getMeanPerDimension();
		double[] stdvPerDimension = dataset.getStdvPerDimension();

		double[][] meanPerSeriesPerDimension = dataset.getMeanPerSeriesPerDimension();
		double[][] stdvPerSeriesPerDimension = dataset.getStdvPerSeriesPerDimension();

		dataset.zNormalize(true);

		double mean2 = dataset.getMean();
		double meanOnline2 = dataset.getMeanOnline();
		double stdv2 = dataset.getStdv();
		double stdvOnline2 = dataset.getStdvOnline();

		double[] meanPerDimension2 = dataset.getMeanPerDimension();
		double[] stdvPerDimension2 = dataset.getStdvPerDimension();

		double[][] meanPerSeriesPerDimension2 = dataset.getMeanPerSeriesPerDimension();
		double[][] stdvPerSeriesPerDimension2 = dataset.getStdvPerSeriesPerDimension();

	}

}
