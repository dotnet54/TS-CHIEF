package data.io;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.exceptions.NotSupportedException;
import data.timeseries.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import util.PrintUtilities;

/**
 * 
 * DEV
 * 
 * add support for skip top n rows, exclude columns, 
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class TSReader {
	protected boolean hasHeader = false;
	protected int labelColumn = -1;	// only supported {0,-1} == first or last column
	protected String valSeparator = ",";
	protected String dimSeparator = ":";
	protected String commentStartChar = "#";
	protected String sectionStartChar = "@";
	protected String extension = ".ts";
	
	//DEV: HACKY: some stats of the last loaded file, this data gets overwritten every time, just for a quick printing if needed
	public TSFileInfo _lastLoadedFile;
	protected int verbosity = 0;

	public class TSFileInfo{
		public String fileFullName;
		public String fileName; //without path
		public String datasetName;  //without extension
		List<String> tags = new ArrayList<>();

		public int rows;
		public int columns ;
		// column data types
		
		public String problemName;
		public boolean timeStamps;
		public boolean missing;
		public boolean univariate;
		public int dimensions;
		public boolean equalLength;
		public int seriesLength;
		public boolean classLabel;
		public String[] classLabels;

		//extra -- for file verification
		public boolean isMultiLabel;
		public int minNumLabels; //per series
		public int maxNumLabels;
		public int minLength;
		public int maxLength;
		public int minDimensions; //to verify consistency
		public int maxDimensions;
		public NonContinuousLabelEncoder labelEncoder;
		public ClassDistribution classDistribution;


		//simple stats
		public long memBeforeLoad; //in bytes
		public long memAfterLoad; //in bytes
		public long loadTime; //in nanoseconds

		public TSFileInfo() {
			labelEncoder = new NonContinuousLabelEncoder();
			classDistribution = new ClassDistribution();
		}

	}
	
	public TSReader() {}
	
	public TSReader(boolean hasHeader, int labelColumn) {
		this.hasHeader = hasHeader;
		this.labelColumn = labelColumn;
	}

	public TSFileInfo verifyFile(String fileName,
								 boolean allowMultivariate, boolean allowVariableLength,
								 boolean allowMultipleLabels, boolean checkIfNormalized) throws IOException, RuntimeException{

		FileReader fileReader = new FileReader(fileName);
		LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
		TSReader.TSFileInfo fileInfo = new TSFileInfo();
		String line = null;
		String[] lineDimensions = null;
		String[] lineValues = null;
		boolean isDataSection = false;

		try {

			while ((line = lineNumberReader.readLine()) != null){

				if(line.startsWith(commentStartChar)){
					//skip
				} else if (line.startsWith("@problemName")) {
					fileInfo.problemName = line.split(" ")[1].trim();
				}else if (line.startsWith("@timeStamps")) {
					fileInfo.timeStamps = Boolean.parseBoolean(line.split(" ")[1].trim());
				}else if (line.startsWith("@missing")) {
					fileInfo.missing = Boolean.parseBoolean(line.split(" ")[1].trim());
				}else if (line.startsWith("@univariate")) {
					fileInfo.univariate = Boolean.parseBoolean(line.split(" ")[1].trim());
				}else if (line.startsWith("@dimensions")) {
					fileInfo.dimensions = Integer.parseInt(line.split(" ")[1].trim());
				}else if (line.startsWith("@equalLength")) {
					fileInfo.equalLength = Boolean.parseBoolean(line.split(" ")[1].trim());
				}else if (line.startsWith("@seriesLength")) {
					fileInfo.seriesLength = Integer.parseInt(line.split(" ")[1].trim());
				}else if (line.startsWith("@classLabel")) {
					String [] tmp = line.split(" ");
					fileInfo.classLabel = Boolean.parseBoolean(tmp[1].trim());
					line = line.substring("@classLabel true ".length(),line.length());  // remove @classLabel from String
					fileInfo.classLabels = line.split(" ");
					fileInfo.classLabels = Arrays.stream(fileInfo.classLabels).map(String::trim).toArray(String[]::new);
				}else if (line.startsWith("@data")) {
					isDataSection = true;
					continue; // from next line is assume that it is data
				}else if (!isDataSection){
					continue; //skip or throw error...
				}else if(isDataSection){
					//if inside the data section do extra verification
					lineDimensions = line.split(dimSeparator);
					for (int dimension = 0; dimension < lineDimensions.length; dimension++) {
						if (dimension < fileInfo.dimensions){
							//data
							lineValues = lineDimensions[dimension].split(valSeparator);

							if (fileInfo.minLength == 0 || fileInfo.minLength < lineValues.length){
								fileInfo.minLength = lineValues.length;
							}
							if (fileInfo.maxLength == 0 || fileInfo.maxLength > lineValues.length){
								fileInfo.maxLength = lineValues.length;
							}

							assert fileInfo.minLength <= fileInfo.maxLength;

							//TODO verify data values, check for missing data

							//TODO verify normalization

						}else if (dimension == fileInfo.dimensions){
							//last element should be class the label,
							//if this element is not a label,
							//then there is a variable number of dimensions -- throw file inconsistency error
							lineValues = lineDimensions[dimension].split(valSeparator);

							if (lineValues.length > 1){
								//then there are comma separated values in the label,
								// throw an error or treat them as multiple labels?
								throw new NotSupportedException("Number of dimensions in the header does not match " +
										"the number of dimensions found in the data section." +
										" If this is a multilabel dataset, they are not supported currently");
							}

							String label = lineValues[0].trim();
							//TODO update label encoder and class distribution

						}else{
							throw new NotSupportedException("Inconsistencies found in the TS file. Number of dimensions" +
									"found in the data section does not match header information.");
						}
					}

				}else{
					continue; //skip
				}

			}

		}finally{
			if (fileReader != null) fileReader.close();
		}

		//this output array contains file information
		if (hasHeader) {
			//number of rows;
			fileInfo.rows = lineNumberReader.getLineNumber() == 0 ? lineNumberReader.getLineNumber(): lineNumberReader.getLineNumber() - 1;
		}else {
			//number of rows;
			fileInfo.rows =lineNumberReader.getLineNumber();
		}

//		fileInfo.columns = lineDimensions.length;  //number of columns;

		if (!allowMultivariate && fileInfo.dimensions > 1 ){
			throw new NotSupportedException("Support for multivariate datasets is not enabled");
		}

		if (!allowVariableLength && (fileInfo.minLength != fileInfo.maxLength ||
				fileInfo.minLength != fileInfo.seriesLength || fileInfo.maxLength != fileInfo.seriesLength)){
			throw new NotSupportedException("Support for variable length datasets is not enabled");
		}

		if (!allowMultipleLabels && fileInfo.minNumLabels != fileInfo.maxNumLabels ){
			throw new NotSupportedException("Dataset contains multiple labels");
		}

		//record file name and infer dataset name
		Path p = Paths.get(fileName);
		fileInfo.fileFullName = fileName;
		fileInfo.fileName = p.getFileName().toString();
		fileInfo.datasetName = fileInfo.problemName;
		//fileInfo.fileName.substring(0, fileInfo.fileName.indexOf('.'));

		if (fileInfo.fileName.contains("_TRAIN")){
			fileInfo.tags.add("train");
		}

		if (fileInfo.fileName.contains("_TEST")){
			fileInfo.tags.add("test");
		}

		return fileInfo;

	}

	public TSFileInfo getFileInfo(String fileName) throws IOException {
		FileReader input = new FileReader(fileName);
		LineNumberReader lineNumberReader = new LineNumberReader(input);		
		String line = null;
        String[] line_array = null;		
        TSReader.TSFileInfo file_info = new TSFileInfo();
		boolean isDataSection = false;

		try {
			boolean length_check = true;
			
	        while ((line = lineNumberReader.readLine()) != null){
	        	
	        	if (line.startsWith("@problemName")) {
	        		file_info.problemName = line.split(" ")[1].trim();
	        	}else if (line.startsWith("@timeStamps")) {
	        		file_info.timeStamps = Boolean.parseBoolean(line.split(" ")[1].trim());
	        	}else if (line.startsWith("@missing")) {
	        		file_info.missing = Boolean.parseBoolean(line.split(" ")[1].trim());
	        	}else if (line.startsWith("@univariate")) {
	        		file_info.univariate = Boolean.parseBoolean(line.split(" ")[1].trim());
	        	}else if (line.startsWith("@dimensions")) {
	        		file_info.dimensions = Integer.parseInt(line.split(" ")[1].trim());
	        	}else if (line.startsWith("@equalLength")) {
	        		file_info.equalLength = Boolean.parseBoolean(line.split(" ")[1].trim());
	        	}else if (line.startsWith("@seriesLength")) {
	        		file_info.seriesLength = Integer.parseInt(line.split(" ")[1].trim());
	        	}else if (line.startsWith("@classLabel")) {
	        		String [] tmp = line.split(" ");
	        		file_info.classLabel = Boolean.parseBoolean(tmp[1].trim());
	        		line = line.substring("@classLabel true ".length(),line.length());  // remove @classLabel from String
					file_info.classLabels = line.split(" ");
					file_info.classLabels = Arrays.stream(file_info.classLabels).map(String::trim).toArray(String[]::new);
	        	}else if (line.startsWith("@data")) {
	        		isDataSection = true;
	        		continue;
	        	}else if (!isDataSection){
	        		continue;
	        	}
	        	
//	        	if (length_check) {
//	        		length_check = false;
//	        		line_array = line.split(valSeparator);
//	        	}
//	            System.out.println("Line " + lineNumberReader.getLineNumber() + ": " + line);
	        }			
	        
		}finally{
			if (input != null) input.close();
		}
		
		//this output array contains file information
		if (hasHeader) {
			//number of rows;
			file_info.rows = lineNumberReader.getLineNumber() == 0 ? lineNumberReader.getLineNumber(): lineNumberReader.getLineNumber() - 1; 
		}else {
			//number of rows;
			file_info.rows =lineNumberReader.getLineNumber();  
		}
		
//		file_info.columns = line_array.length;  //number of columns;

		
		//record file name and infer dataset name
		Path p = Paths.get(fileName);
		file_info.fileFullName = fileName;
		file_info.fileName = p.getFileName().toString();
		file_info.datasetName = file_info.fileName.substring(0, file_info.fileName.indexOf('.'));

		
		return file_info;
	}

	public MTSDataset readFile(String fileName){
		LabelEncoder labelEncoder = new NonContinuousLabelEncoder();
		return readFile(fileName, labelEncoder);
	}

	public MTSDataset readFile(String fileName, LabelEncoder labelEncoder){
        BufferedReader br = null;
        String line = "";
        int i = 0;
        long start, end, elapsed;
        TSFileInfo file_info;
        MTSDataset dataset = null;
        long used_mem;
        String[] line_array = null;
        double[][] features;
        String label = null;
        Integer encodedLabel = null;
        File f = new File(fileName);
		boolean isDataSection = false;
//		NonContinuousLabelEncoder labelEncoder = new NonContinuousLabelEncoder();
 
        try {
        	if (verbosity > 0) {
        		System.out.print("Reading csv file ["+ f.getName() +"]:");
        	}
         	start  = System.nanoTime();

        	file_info = verifyFile(fileName, true, false,
					false, false);
        	dataset = new MTSDataset(file_info.rows);
			dataset.setIsVariableLength(! file_info.equalLength);

            br = new BufferedReader(new FileReader(fileName));

            //set up label encoder
			//NOTE refit should not clear the existing list
			//TODO handle labels not in training data but present in test data
//			if (! labelEncoder.isFitted()){
				labelEncoder.fit(file_info.classLabels); 	//assumes that the header has correct info, does not read all labels line by line
//			}
			dataset.setLabelEncoder(labelEncoder);

            while ((line = br.readLine()) != null) {
            	
	        	if (line.startsWith("@data")) {
	        		isDataSection = true;
	        		continue; //skip the tag line starting with @data - start reading actual data from the next line
	        	}else if (!isDataSection){
	        		continue; //skip non-data rows which was not read earlier
	        	}
            	
	        	//TODO check need?
            	if (hasHeader && i == 0) {
            		i++;
            		continue; //skip header row
            	}
            	
                // use comma as separator
                String[] dimensions = line.split(dimSeparator);
                features = new double [file_info.dimensions][];
                boolean variableLengthFound = false;
                int previousLength = 0;
                int currentLength;

				for (int j = 0; j < dimensions.length; j++) {
					line_array = dimensions[j].split(valSeparator);
					currentLength = line_array.length;

					if (j==0){
						previousLength = currentLength;
					}

					if (j != dimensions.length-1 && currentLength != previousLength){		//skip label
						previousLength = currentLength;
						variableLengthFound = true;
					}

					if (j == dimensions.length - 1){		//last dimension is the class label
						label = line_array[0].trim();
					}else{
						features[j] = new double[line_array.length];
						for (int k = 0; k < line_array.length; k++){
							features[j][k] = Double.parseDouble(line_array[k]);
						}
					}
				}

				TimeSeries series = new MTimeSeries(features, dataset.getLabelEncoder().getLabel(label));
				dataset.setIsVariableLength(variableLengthFound);
                dataset.add(series);
                
                i++;
            }
            
            
            end = System.nanoTime();
            elapsed = end - start;
    		String time_duration = DurationFormatUtils.formatDuration((long) (elapsed/1e6), "H:m:s.SSS");
        	if (verbosity > 0) {
        		System.out.println("finished in " + time_duration);
        	}

        	dataset.setName(file_info.datasetName);
        	dataset.setTags(StringUtils.join(file_info.tags, ','));
        	_lastLoadedFile = file_info; //hacky, not thread-safe
        	
        } catch (FileNotFoundException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
        } catch (IOException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
        }catch (Exception e) {
            e.printStackTrace();
		} finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        return dataset;
	}

	public static void writeFile(Dataset dataset, String fileName, LabelEncoder labelEncoder, boolean overwrite){
		BufferedWriter bw = null;
		try {
			File file = new File(fileName);

			if (overwrite == false && file.exists()){
				return;
			}

			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			//write header
			bw.write("# This .ts file was saved from TSReader.java class in TS-CHIEF package. @dotnet54 \n");
			bw.write( "# classLabel contaisn encodedLabels, not actual labels" + dataset.getName() + ": " +dataset.getTags() + "\n");

			bw.write("\n");
			bw.write("@problemName " + dataset.getName() + "\n");
			bw.write("@timeStamps false\n");
			bw.write("@missing false\n");
			bw.write("@univariate " + (dataset.dimensions() == 1 ? "true\n": "false\n"));
			bw.write("@dimensions " + dataset.dimensions() + "\n");
			bw.write("@equalLength " + (dataset.isVariableLength() ? "false\n": "true\n"));
			bw.write("@seriesLength " + dataset.length() + "\n");
			// TODO fix classList -> getUniqueClasses
			String classList = String.join(" ",
					Arrays.stream(dataset.getUniqueClasses()).mapToObj(String::valueOf).toArray(String[]::new));
			bw.write("@classLabel true " + classList + "\n");

			bw.write("\n@data\n");

			StringBuilder sb = new StringBuilder();
			int datasetSize = dataset.size();
			for (int i = 0; i < datasetSize; i++) {
				TimeSeries series = dataset.getSeries(i);
				double[][] data = series.data();

				sb.setLength(0);
				for (int j = 0; j < data.length; j++) {
					for (int k = 0; k < data[j].length - 1; k++) { // -1 to prevent adding final "," before ":"
						sb.append(data[j][k]);
						sb.append(",");
					}
					// add last item
					if (data[j].length > 0){
						sb.append(data[j][data[j].length-1]);
					}
					sb.append(":");
				}
				sb.append(series.label());
				sb.append("\n");

				bw.write(sb.toString());
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally{
			try{
				if(bw!=null)
					bw.close();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		try {

			System.out.println("Testing MTSDataset");
			String datasetName = "BasicMotions";
			String archiveName = "Multivariate2018_ts";
			String trainFile = "E:/data/" + archiveName + "/" + datasetName + "/" + datasetName + "_TRAIN.ts";
			String testFile = "E:/data/" + archiveName + "/" + datasetName + "/" + datasetName + "_TEST.ts";

			TSReader reader = new TSReader();
			MTSDataset train = reader.readFile(trainFile);

		} catch (Exception e) {
			System.out.println(e);
		}
	}


}
