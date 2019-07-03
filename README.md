

# TS-CHIEF 
An effective and scalable distance-based classifier for time series classification. This repostitory contains the source code for the time series classification algorithm TS-CHIEF, published in the paper [TS-CHIEF](https://arxiv.org/abs/1906.10329)

NOTE - some refactoring is in progress to cleap up the code, so this read me will be updated soon

## Abstract 
Time Series Classification (TSC) has seen enormous progress over the last two decades. HIVE-COTE (Hierarchical Vote Collective of Transformation-based Ensembles) is the current state of the art in terms of classification accuracy. HIVE-COTE recognizes that time series are a specific data type for which the traditional attribute-value representation, used predominantly in machine learning, fails to provide a relevant representation. HIVE-COTE combines multiple types of classifiers: each extracting information about a specific aspect of a time series, be it in the time domain, frequency domain or summarization of intervals within the series. However, HIVE-COTE (and its predecessor, FLAT-COTE) is often infeasible to run on even modest amounts of data. For instance, training HIVE-COTE on a dataset with only 1,500 time series can require 8 days of CPU time. It has polynomial runtime w.r.t training set size, so this problem compounds as data quantity increases. We propose a novel TSC algorithm, TS-CHIEF, which is highly competitive to HIVE-COTE in accuracy, but requires only a fraction of the runtime. TS-CHIEF constructs an ensemble classifier that integrates the most effective embeddings of time series that research has developed in the last decade. It uses tree-structured classifiers to do so efficiently. We assess TS-CHIEF on 85 datasets of the UCR archive, where it achieves state-of-the-art accuracy with scalability and efficiency. We demonstrate that TS-CHIEF can be trained on 130k time series in 2 days, a data quantity that is beyond the reach of any TSC algorithm with comparable accuracy.

When using this repository, please cite:
```
https://arxiv.org/abs/1906.10329
```

## Usage and prerequisites

The project requires Java 8.0, and two open source libraries [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/) 3.7, and [Google Gson](https://github.com/google/gson) 2.8.2. These two libraries are included in the lib folder.

The project was developed using Eclipse 4.8 IDE. If you require the project to be moved to another IDE, just create a new project and import src and lib directories to the new IDE.

###  Creating a jar file

Open the project in Eclipse and use File->Export->Java->JAR File. to export a `.jar` file of the project

### Input data format
This implementation currently supports CSV files with the following format for testing and training data. 

 - A matrix of comma separated double values
 - Class label is either the first or last column, refer to the command line options below
 - If header row is included, refer to the command line options below
 - All time series are expected to have the same length 
 - Data z-normalised per series works best  - TODO

### Running in command line 
```
java -jar -Xmx1g ProximityForest.jar 
-train=E:/data/ucr/ItalyPowerDemand/ItalyPowerDemand_TRAIN.csv 
-test=E:/data/ucr/ItalyPowerDemand/ItalyPowerDemand_TEST.csv 
-out=output -repeats=1 -trees=100 -s=ee:5,boss:100,rise:100 -on_tree=true -export=1 -verbosity=0
```
The `Xmx1g` sets the Java Virtual Machine memory limit to 1GB - increase this value as required by your dataset. Use the `-verbosity` option to print memory usage.

Separate each command line option with an `=`. Available options are 
- `-train`=path to training file (only CSV files are supported)
- `-test`=path to training file (only CSV files are supported)
- `-out`=output folder, folder is created if it does not exist
- `-repeats`=number of times to repeat the experiment, default is 1, but since this is a randomised ensemble it is recommended to repeat the experiment at last 10 times. Especially if the number of trees is low
- `-trees`=number of trees in the ensemble, the default is 1 to run an initial test quickly, but you can start testing with 10, 20, 50, 100, etc. TS_chief paper uses 500 trees.
- `-s`= number of candidate splits to evaluate per node per splitter type, recommended settings in paper is 5 similarity-based (EE-based), 100 dictionary-based (BOSS-based) and 100 interval-based (RISE-based) candidates per node.
- `-on_tree`= if `true` distance measure is selected per node, if `false` it is selected once per tree. , if not specified the default is `true` 
- `-shuffle`= if `true` shuffles the training dataset, if not specified the default is `false` 
<!---
- `-jvmwarmup`= if `true` some extra calculation is done before the experiment is started to "warmup" java virtual machine, this helps measure more accurate elapsed time for short duration.
-->
- `-export`= set to 1 to export results in json format to the specified output directory, if 0 results are not exported.
- `-verbosity`= if 0 minimal printing to stdout, if 1 progress is printed per tree, if 2 memory usage is printed per tree, the default is 0
- `-csv_has_header`= set to `true` if input csv files contain a header row, default assumes `false`
- `-target_column`= set `first` if first column contains target label in the input files, or set to `last` if the last column is the target label. The default is `first`, and only the values `first` and `last` is supported.  

If an option is not specified in the command line, a default setting is used, which can be found in the `AppContext.java`class.

# Support


The authors would like to thank Prof. Eamonn Keogh and all the people who have contributed to the UCR time series classification archive. We also would like to acknowledge the use of source code freely available at http://www.timeseriesclassification.com and thank Prof. Anthony Bagnall and
other contributors of the project. We also acknowledge the use of source code freely provided by the original author of BOSS algorithm, Dr. Patrick Sch¨afer. Finally, we acknowledge the use of two Java libraries (Trove and HPPC), which was used to optimize the implementation of our source code.


YourKit is supporting Proximity Forest open source project with its full-featured Java Profiler.
YourKit is the creator of innovative and intelligent tools for profiling Java and .NET applications. http://www.yourkit.com 

