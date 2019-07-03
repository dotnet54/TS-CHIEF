package datasets;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DataStore {

	//key format:
	//transform:train:params,params,..hash
	
	protected static int datasetID;	//TODO atomic int??
	protected Map<String, TSDataset> dataStore;
	protected AtomicInteger datasetHash;
	
	public DataStore() {
		dataStore = new HashMap<String, TSDataset>();
	}

	public Map<String, TSDataset> getDataStore() {
		return dataStore;
	}
	

	
	
}
