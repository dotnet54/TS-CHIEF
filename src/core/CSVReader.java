package core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import org.apache.commons.lang3.time.DurationFormatUtils;
import datasets.TSDataset;
import util.PrintUtilities;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class CSVReader {
	
	//helper function to assist memory allocation
	public static int[] getFileInformation(String fileName, boolean hasHeader, String separator) throws IOException {
		FileReader input = new FileReader(fileName);
		LineNumberReader lineNumberReader = new LineNumberReader(input);		
		String line = null;
        String[] line_array = null;		
		int [] file_info = new int[2];
        
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
			file_info[0] = lineNumberReader.getLineNumber() == 0 ? lineNumberReader.getLineNumber(): lineNumberReader.getLineNumber() - 1; 
		}else {
			//number of rows;
			file_info[0] =lineNumberReader.getLineNumber();  
		}
		
		file_info[1] = line_array.length;  //number of columns;

		return file_info;
	}
	
	//helper function to assist memory allocation
	public static int[] getARFFFileInformation(String fileName, boolean hasHeader, String separator) throws IOException {
		FileReader input = new FileReader(fileName);
		LineNumberReader lineNumberReader = new LineNumberReader(input);		
		String line = null;
        String[] line_array = null;		
		int [] file_info = new int[2];
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
			file_info[0] = lineNumberReader.getLineNumber() == 0 ? lineNumberReader.getLineNumber(): lineNumberReader.getLineNumber() - 1; 
		}else {
			//number of rows;
			file_info[0] =lineNumberReader.getLineNumber();  
		}
		
		file_info[1] = line_array.length;  //number of columns;

		return file_info;
	}

	
	//with defaults provided if not specified
	public static TSDataset readCSVToTSDataset(String fileName) {
		return readCSVToTSDataset(fileName, false, true, ",", AppContext.verbosity);
	}
	
	public static TSDataset readCSVToTSDataset(String fileName, boolean hasHeader, 
			boolean targetColumnIsFirst, String separator, int verbosity){
        BufferedReader br = null;
        String line = "";
        int i = 0;
        long start, end, elapsed;
        int[] file_info;
        TSDataset dataset = null;
        long used_mem;
        String[] line_array = null;
        double[] tmp;
        Double label;
        File f = new File(fileName);
        
        if (fileName.endsWith(".arff")) {
        	return readARFFToTSDataset(fileName, hasHeader, targetColumnIsFirst, separator, verbosity);
        }
        
        try {
        	if (verbosity > 0) {
        		System.out.print("Reading csv file ["+ f.getName() +"]:");
        	}
         	start  = System.nanoTime();

        	//useful for reading large files;
        	file_info = getFileInformation(fileName,hasHeader, separator); //0=> no. of rows 1=> no. columns
        	int expected_size = file_info[0];	
        	int data_length = file_info[1] - 1;  //-1 to exclude target the column

        	dataset = new TSDataset(expected_size, data_length);
            br = new BufferedReader(new FileReader(fileName));
            
            while ((line = br.readLine()) != null) {
                // use comma as separator
                line_array = line.split(separator);
                
//                System.out.println("Line  " + i + " , Class=[" + line_array[0] + "] length= " + line_array.length);
               
                //allocate new memory every turn
                tmp = new double[data_length];	
     
                if (targetColumnIsFirst) {
	                for (int j = 1; j <= data_length; j++){
	                	tmp[j-1] = Double.parseDouble(line_array[j]);
	                }   
	                label = Double.parseDouble(line_array[0]);
                }else {
                	//assume target is the last column
                	int j;
	                for (j = 0; j < data_length; j++){
	                	tmp[j] = Double.parseDouble(line_array[j]);
	                }  
	                label = Double.parseDouble(line_array[j]);
                }

                dataset.add(tmp, label.intValue());
                
                i++;
                
            	if (verbosity > 0) {
	                if (i % 1000  == 0) {
	                	if (i % 100000 == 0) {
	                		System.out.print("\n");
	                    	if (i % 1000000 == 0) {
	                    		used_mem = AppContext.runtime.totalMemory() - AppContext.runtime.freeMemory();
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
            

        } catch (FileNotFoundException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
        } catch (IOException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
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
	
	
	
	public static TSDataset readARFFToTSDataset(String fileName, boolean hasHeader, 
			boolean targetColumnIsFirst, String separator, int verbosity){
        BufferedReader br = null;
        String line = "";
        int i = 0;
        long start, end, elapsed;
        int[] file_info;
        TSDataset dataset = null;
        long used_mem;
        String[] line_array = null;
        double[] tmp;
        Double label;
        File f = new File(fileName);
		boolean isDataSection = false;
        
        try {
        	if (verbosity > 0) {
        		System.out.print("Reading arff file ["+ f.getName() +"]:");
        	}
         	start  = System.nanoTime();

        	//useful for reading large files;
        	file_info = getARFFFileInformation(fileName,hasHeader, separator); //0=> no. of rows 1=> no. columns
        	int expected_size = file_info[0];	
        	int data_length = file_info[1] - 1;  //-1 to exclude target the column

        	dataset = new TSDataset(expected_size, data_length);
            br = new BufferedReader(new FileReader(fileName));
            
            while ((line = br.readLine()) != null) {
            	
	        	if (line.startsWith("@data")) {
	        		isDataSection = true;
	        		continue;
	        	}else if (!isDataSection){
	        		continue;
	        	}
            	
                // use comma as separator
                line_array = line.split(separator);
                
//                System.out.println("Line  " + i + " , Class=[" + line_array[0] + "] length= " + line_array.length);
               
                //allocate new memory every turn
                tmp = new double[data_length];	
     
                if (targetColumnIsFirst) {
	                for (int j = 1; j <= data_length; j++){
	                	tmp[j-1] = Double.parseDouble(line_array[j]);
	                }   
	                label = Double.parseDouble(line_array[0]);
                }else {
                	//assume target is the last column
                	int j;
	                for (j = 0; j < data_length; j++){
	                	tmp[j] = Double.parseDouble(line_array[j]);
	                }  
	                label = Double.parseDouble(line_array[j]);
                }

                dataset.add(tmp, label.intValue());
                
                i++;
                
            	if (verbosity > 0) {
	                if (i % 1000  == 0) {
	                	if (i % 100000 == 0) {
	                		System.out.print("\n");
	                    	if (i % 1000000 == 0) {
	                    		used_mem = AppContext.runtime.totalMemory() - AppContext.runtime.freeMemory();
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
            

        } catch (FileNotFoundException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
        } catch (IOException e) {
//            e.printStackTrace();
        	PrintUtilities.abort(e);
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
