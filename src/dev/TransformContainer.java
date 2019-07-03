package dev;

import datasets.TSDataset;

public interface TransformContainer {

	public void fit(TSDataset train);
	public TSDataset transform(TSDataset train);
//	public TSDataset transform(TSDataset train, ParameterBag params);

}
