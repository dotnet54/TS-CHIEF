package data.timeseries;

import java.util.List;

import data.io.ArffReader;
import data.io.CsvReader;
import data.io.TSReader;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public interface Dataset {

	public String getName();

	public void setName(String name);

	public String getTags();

	public void setTags(String tags);

	public boolean isMultivariate();

	public boolean isVariableLength();

	public boolean isNormalized();

	public boolean hasMissingValues();

	public boolean hasTimestamps();

	public int size();
	
	public int length();

	public int minLength();

	public int maxLength();

	public int dimensions();

	public TimeSeries add(TimeSeries series) throws Exception;
	
	public void remove(int i) throws Exception;

	public void clear();

	public TimeSeries getSeries(int i);

	public double[][] getSeriesData(int i);

	public Integer getClass(int i);

	public TIntIntMap getClassDistribution();

	public int getNumClasses();
	
	public int getClassSize(Integer classLabel);

	public int[] getUniqueClasses();

	public LabelEncoder getLabelEncoder();

	public TIntObjectMap<Dataset> splitByClass() throws Exception;

	public double gini();

//	TODO TEMPORARY FIXES

	public TimeSeries[] toArray();

	public List<double[][]> _data_as_list();
	
	public List<Integer> _class_as_list();
	
	public double[][] _data_as_array();
	
	public int[] _class_as_array();
	
	public void shuffle();	
	
	public void shuffle(long seed);
	
	public Dataset shallowClone();
	
	public Dataset deepClone();

	public Dataset sortOn(int timestamp);

	public double getMean();

	public double getStdv();

	public double[] getMeanPerDimension();

	public double[] getStdvPerDimension();

	public double[] getMinPerDimension();

	public double[] getMaxPerDimension();

	public double[][] getMeanPerSeriesPerDimension();

	public double[][] getStdvPerSeriesPerDimension();

	public double[][] getMinPerSeriesPerDimension();

	public double[][] getMaxPerSeriesPerDimension();

	public void zNormalize(boolean perSeries);

	public void meanNormalize(boolean perSeries);

	public void featureScale(boolean perSeries);

	public void saveToFile(String fileType, boolean overwrite);

	public static Dataset readFromFile(String fileName, boolean hasHeader, int labelColumn,
								   LabelEncoder labelEncoder) {
		Dataset data = null;
		if (labelEncoder == null){
			labelEncoder = new NonContinuousLabelEncoder();
		}

		if (fileName.endsWith(".arff")) {
			ArffReader reader = new ArffReader(hasHeader, labelColumn);
			data = reader.readFile(fileName.trim(), labelEncoder);
		}else if(fileName.endsWith(".ts")){
			TSReader reader = new TSReader(hasHeader, labelColumn);
			data = reader.readFile(fileName.trim(), labelEncoder);
		}else {
			//assume its a csv file
			CsvReader reader = new CsvReader(hasHeader, labelColumn);
			data = reader.readFile(fileName.trim(), labelEncoder);
		}

		return data;
	}

}
