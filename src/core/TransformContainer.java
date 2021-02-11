package core;

import data.timeseries.Dataset;

public interface TransformContainer {

	public void fit(Dataset train) throws Exception;
	public Dataset transform(Dataset train) throws Exception;
//	public Dataset transform(TSDataset train, ParameterBag params);

}
