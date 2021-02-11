package core;

import data.timeseries.Dataset;

public interface Transform {

    public void fit(Dataset trainData, Options params, DebugInfo debugInfo) throws Exception;

    public Dataset transform(Dataset testData, Options params, DebugInfo debugInfo) throws Exception;

    public Dataset fitTransform(Dataset trainData, Options params, DebugInfo debugInfo) throws Exception;

    public Dataset inverseTransform(Dataset testData, Options params, DebugInfo debugInfo) throws Exception;

    public Options getParams();

    public void setParams(Options params);
}
