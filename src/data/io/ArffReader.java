package data.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import data.timeseries.*;
import org.apache.commons.lang3.time.DurationFormatUtils;

import application.AppConfig;
import core.exceptions.NotSupportedException;
import util.PrintUtilities;

/**
 * DEV
 * 
 * very simple arff loader
 * does not support sparse arff files
 * 
 * @author ashi32
 *
 */

public class ArffReader {

	protected boolean hasHeader = false;
	protected String separator = ",";
	protected int labelColumn = -1;	// only supported {0,-1} == first or last column
	protected String extension = ".csv";
	protected int verbosity = 0;

	//DEV: HACKY: some stats of the last loaded file, this data gets overwritten every time, just for a quick printing if needed
	public ArffFileInfo _lastLoadedFile;
	
	public class ArffFileInfo{
		public String fileFullName;
		public String fileName; //without path
		public String datasetName; //relationName in arff
		public int rows;
		public int columns ;
		// column data types
		
		//simple stats
		public long memBeforeLoad; //in bytes
		public long memAfterLoad; //in bytes
		public long loadTime; //in nanoseconds
		
		public ArffFileInfo() {

		}
		
		
	}
	
	public ArffReader() {
		
	}
	
	public ArffReader(boolean hasHeader, int labelColumn) {
		this.hasHeader = hasHeader;
		this.labelColumn = labelColumn;
	}
	
	//helper function to assist memory allocation
	public ArffFileInfo getArffFileInfo(String fileName) throws IOException {
		FileReader input = new FileReader(fileName);
		LineNumberReader lineNumberReader = new LineNumberReader(input);		
		String line = null;
        String[] line_array = null;		
        ArffFileInfo file_info = new ArffFileInfo();
		boolean isDataSection = false;
        
		try {
			boolean length_check = true;
			
	        while ((line = lineNumberReader.readLine()) != null){
	        	
	        	if (line.startsWith("@data")) {
	        		isDataSection = true;
	        		continue;
	        	}else if (!isDataSection){
	        		continue;
	        	}
	        	
	        	if (length_check) {
	        		length_check = false;
	        		line_array = line.split(separator);
	        	}
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
		
		file_info.columns = line_array.length;  //number of columns;
		
		Path p = Paths.get(fileName);
		file_info.fileFullName = fileName;
		file_info.fileName = p.getFileName().toString();
//		file_info.datasetName = file_info.fileName.substring(0, file_info.fileName.lastIndexOf('.')); //use relation name
		
		return file_info;
	}
	
	
	//NOTE: does not support sparse arff data -- this is a very simple arff loader
	public MTSDataset readFile(String fileName){
		LabelEncoder labelEncoder = new NonContinuousLabelEncoder();
		return readFile(fileName, labelEncoder);
	}

	public MTSDataset readFile(String fileName, LabelEncoder labelEncoder){
        BufferedReader br = null;
        String line = "";
        int i = 0;
        long start, end, elapsed;
        ArffFileInfo file_info;
		MTSDataset dataset = null;
        long used_mem;
        String[] line_array = null;
        double[][] features;
        int num_features;
        Double label;
        File f = new File(fileName);
		boolean isDataSection = false;
		int num_dimensions = 1; //TODO currently supports only 1 dimension from arff files, use .ts file for multivariate datasets
        
        try {
        	if (verbosity > 0) {
        		System.out.print("Reading arff file ["+ f.getName() +"]:");
        	}
         	start  = System.nanoTime();

        	//useful for reading large files;
        	file_info = getArffFileInfo(fileName); 
        	num_features = file_info.columns - 1;  //-1 to exclude target the column

        	dataset = new MTSDataset(file_info.rows);
            br = new BufferedReader(new FileReader(fileName));
            
            while ((line = br.readLine()) != null) {
            	
	        	if (line.startsWith("@data")) {
	        		isDataSection = true;
	        		continue; //skip the tag line starting with @data - start reading actual data from the next line
	        	}else if (line.startsWith("@relation")) {
	        		file_info.datasetName = line.substring(10, line.length()).trim();
	        		continue;
	        	}else if (!isDataSection){
	        		continue; //skip non-data rows which was not read earlier
	        	}
            	
                // use comma as separator
                line_array = line.split(separator);
                
//                System.out.println("Line  " + i + " , Class=[" + line_array[0] + "] length= " + line_array.length);
               
                //allocate new memory every turn
                features = new double[num_dimensions][num_features];
     
                if (labelColumn == 0) {
	                for (int j = 1; j <= num_features; j++){
	                	features[0][j-1] = Double.parseDouble(line_array[j]);
	                }   
	                label = Double.parseDouble(line_array[0]);
                }else if (labelColumn == -1){
                	//assume target is the last column
                	int j;
	                for (j = 0; j < num_features; j++){
	                	features[0][j] = Double.parseDouble(line_array[j]);
	                }  
	                label = Double.parseDouble(line_array[j]);
                }else {
                	throw new NotSupportedException("Only labelColumn = {0, -1} is supported for first or last column");
                }

				TimeSeries series = new MTimeSeries(features, label.intValue());
                dataset.add(series);
                
                i++;
                
            	if (verbosity > 0) {
	                if (i % 1000  == 0) {
	                	if (i % 100000 == 0) {
	                		System.out.print("\n");
	                    	if (i % 1000000 == 0) {
	                    		used_mem = AppConfig.runtime.totalMemory() - AppConfig.runtime.freeMemory();
	                    		System.out.print(i +":" + used_mem/1024/1024 + "mb\n");
	                    	}else {
	                    		
	                    	}
	                	}else {
	                		System.out.print(".");
	                	}
	                }
            	}

                
            }
            end = System.nanoTime();
            elapsed = end - start;
    		String time_duration = DurationFormatUtils.formatDuration((long) (elapsed/1e6), "H:m:s.SSS");
        	if (verbosity > 0) {
        		System.out.println("finished in " + time_duration);
        	}
            
        	_lastLoadedFile = file_info; //hacky, not thread-safe

        } catch (FileNotFoundException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
        } catch (IOException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
        } catch (Exception e) {
//			e.printStackTrace();
		} finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return dataset;
	}	
	
	
}
