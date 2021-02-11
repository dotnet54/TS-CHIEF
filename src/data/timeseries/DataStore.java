package data.timeseries;

import java.util.HashMap;
import java.util.Map;
import core.Options;


/***
 * 
 * A DataStore may be used to store loaded datasets or transformed datasets for each splitter
 * one splitter may store a number of datasets which was transformed using different parameters.
 * 
 * @author shifaz
 *
 */
public class DataStore {
	
	protected transient Map<String, Dataset> datasets;
	
	//default keys
	protected String trainingSetKey = "train";
	protected String testingSetKey = "test";
	
	public DataStore() {
		datasets = new HashMap<String, Dataset>();
	}
	
	public DataStore(Options options) {
		this();
	}	
	
	/***
	 * 
	 * Use this method for initializations which can only done once we have loaded the training set
	 * eg. transforming dataset, initializations which depend on length of dataset
	 * 
	 * @param train
	 * @param options
	 * @throws Exception
	 */
	
	public void initBeforeTrain(Dataset train, Options options) throws Exception{
		
	}
	
	public void initBeforeTrain(Dataset train) throws Exception{
		initBeforeTrain(train, null);
	}	
	
	/***
	 * 
	 * Use this method for initializations which can only done once we have loaded the testing set
	 * eg. transforming dataset, initializations which depend on length of dataset
	 *  this is kept separate so that memory footprint can be reduced by not loading testing set before testing starts
	 * 
	 * @param test
	 * @param options
	 * @throws Exception
	 */	
	public void initBeforeTest(Dataset test, Options options) throws Exception{
		
	}
	
	public void initBeforeTest(Dataset test) throws Exception{
		initBeforeTest(test, null);
	}	
	
	/***
	 * 
	 * A datastore can store multiple datasets which is accessed or set using a key.
	 * 
	 * e.g. keys
	 * default keys = { "train" = forest level training set, "test" = forest level testing set}
	 * 
	 * transformed datasets:
	 * 
	 * boss keys = {"boss_train_*",...}, where * is a string made using parameters used to transform
	 * shapelet keys = {"st_train","st_test"}, 
	 * inception keys = {"it_train","it_test"},
	 * rocket tree keys = {"rt_train","rt_test"},
	 * 
	 * @param key
	 * @return
	 */
	
	public Dataset getDataset(String key) {
		return datasets.get(key);
	}
	public void setDataset(String key, Dataset dataset) {
		datasets.put(key, dataset);
	}
	
	public Dataset getTrainingSet() {
		return this.getDataset(trainingSetKey);
	}
	public Dataset getTestingSet() {
		return this.getDataset(testingSetKey);
	}

	public void setTrainingSet(Dataset train) {
		datasets.put(this.trainingSetKey, train);
	}
	public void setTestingSet(Dataset test) {
		datasets.put(this.testingSetKey, test);
	}

	//TODO
//	public int[] getDataIndices(String key);
//	public void setDataIndices(String key, int indices);
	
}
