package datasets;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class TransformedDataset extends TSDataset{

	
	public TIntArrayList indices;
	
	public TransformedDataset() {
		super();
	}
	
	public TransformedDataset(int expected_size) {
		super(expected_size);
	}

	public TransformedDataset(int expected_size, int length) {
		super(expected_size, length);
	}
	
	
	public void add(TimeSeries transformed_series, int original_series_index) {
		this.data.add(transformed_series);	//rafactor
		
		Integer label = transformed_series.getLabel();		
		
		if (class_distribution.containsKey(label)) {
			class_distribution.put(label, class_distribution.get(label) + 1);
		}else {
			class_distribution.put(label, 1);
		}
		
		indices.add(original_series_index);
		
	}	

	//TODO remove method
	
}
