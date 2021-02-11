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
 * 
 * DEV
 * 
 * add support for skip top n rows, exclude columns, 
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class CsvReader {

	protected boolean hasHeader = false;
	protected String separator = ",";
	protected int labelColumn = 0;	// only supported {0,-1} == first or last column
	protected String extension = ".csv";
	protected int verbosity = 0;
	
	//DEV: HACKY: some stats of the last loaded file, this data gets overwritten every time, just for a quick printing if needed
	public CsvFileInfo _lastLoadedFile;
	
	public class CsvFileInfo{
		public String fileFullName;
		public String fileName; //without path
		public String datasetName;  //without extension
		public int rows;
		public int columns ;
		// column data types
		
		//simple stats
		public long memBeforeLoad; //in bytes
		public long memAfterLoad; //in bytes
		public long loadTime; //in nanoseconds

		public CsvFileInfo() {

		}
		
		
	}
	
	public CsvReader() {
		
	}
	
	public CsvReader(boolean hasHeader, int labelColumn) {
		this.hasHeader = hasHeader;
		this.labelColumn = labelColumn;
	}
	
	public CsvFileInfo getFileInfo(String fileName) throws IOException {
		FileReader input = new FileReader(fileName);
		LineNumberReader lineNumberReader = new LineNumberReader(input);		
		String line = null;
        String[] line_array = null;		
        CsvReader.CsvFileInfo file_info = new CsvFileInfo();
        
		try {
			boolean length_check = true;
			
	        while ((line = lineNumberReader.readLine()) != null){
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
        CsvFileInfo file_info;
		MTSDataset dataset = null;
        long used_mem;
        String[] line_array = null;
        double[][] features;
        int num_features;
        Double label;
        File f = new File(fileName);
        int num_dimensions = 1; //TODO currently supports univariate files, use .ts reader for multivariate data
        
        try {
        	if (verbosity > 0) {
        		System.out.print("Reading csv file ["+ f.getName() +"]:");
        	}
         	start  = System.nanoTime();

        	file_info = getFileInfo(fileName);	//useful to preallocate memory for large files
        	num_features = file_info.columns - 1; //-1 to exclude target the column
        	dataset = new MTSDataset(file_info.rows);
            br = new BufferedReader(new FileReader(fileName));
            
            while ((line = br.readLine()) != null) {
            	
            	if (hasHeader && i == 0) {
            		i++;
            		continue; //skip header row
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

        	dataset.setName(file_info.datasetName);
        	_lastLoadedFile = file_info; //hacky, not thread-safe
        	
        } catch (FileNotFoundException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
        } catch (IOException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
        } catch (Exception e) {
			e.printStackTrace();
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
