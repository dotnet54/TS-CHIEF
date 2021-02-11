package data.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import util.PrintUtilities;

/**
 * 
 * used to read csv dataframes dumped from python, e.g. transfromed datasets. useful for shapelet transform, inception time, etc..
 * 
 * can read csv file as a double[][] instead of into a MTSDataset
 * 
 * @author ashi32
 *
 */

public class CsvToDoubleArrayReader extends CsvReader {

	public CsvToDoubleArrayReader(boolean hasHeader) {
		super();
		this.hasHeader = hasHeader;
	}
	
	
	/**
	 * if hasHeader set during construction
	 * 
	 * @param fileName
	 * @return
	 */
	public double[][] readFileToArray(String fileName){
		return readFileToArray(fileName, this.hasHeader);
	}

	/**
	 * 
	 * if hasHeader needs to be specified
	 * 
	 * @param fileName
	 * @return
	 */
	
	public double[][] readFileToArray(String fileName, boolean hasHeader){
		double[][] data = null;
        BufferedReader br = null;
        String line = "";
        int i = 0;
        long start, end;
        CsvFileInfo file_info;
        String[] line_array = null;
        double[] features;
        
        try {
         	start  = System.nanoTime();
        	file_info = getFileInfo(fileName);	//useful to preallocate memory for large files
        	data = new double[file_info.rows][file_info.columns];
            br = new BufferedReader(new FileReader(fileName));
            
            while ((line = br.readLine()) != null) {
            	if (hasHeader && i == 0) {
            		i++;
            		continue; //skip header row
            	}
            	
                line_array = line.split(separator);
                               
                //allocate new memory every turn
                features = new double[file_info.columns];	
            	int j;
                for (j = 0; j < file_info.columns; j++){
                	features[j] = Double.parseDouble(line_array[j]);
                }  

                data[i] = features;
                
                i++;
            }
            end = System.nanoTime();
            file_info.loadTime = end - start;
 
//        	_lastLoadedFile = file_info; //hacky, not thread-safe
        	
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

		
		return data;
	}
	
}
