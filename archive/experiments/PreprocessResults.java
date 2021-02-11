package development.experiments;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import application.MainApplication;

public class PreprocessResults {

	
	public static String[] datasets;
	
	public static String inputfolder = "E:\\git\\experiments\\22dec18\\app1124_cleaned\\all\\";
	public static String outputfolder = "E:\\git\\experiments\\22dec18\\app1124_cleaned\\javaprocessed\\";

	
	public static void main(String[] args) throws IOException {
		

		datasets = MainApplication.getAllDatasets();
			
		
		copy_files();
		
		count_files();
		

	}
	
	
	public static void count_files() {
		File input = new File(outputfolder);

		String[] experiments = input.list(new FilenameFilter() {
		  @Override
		  public boolean accept(File current, String name) {
		    return new File(current, name).isDirectory();
		  }
		});
		System.out.println(Arrays.toString(experiments));
		
		
		
		for (String exp : experiments) {
			
			File[] repeats = new File(outputfolder + "\\" + exp).listFiles(File::isDirectory);
			
			
			for (File rep : repeats) {
				
				String temp = rep.toString() + "\\csv\\";
				File[] files =  new File(temp).listFiles();
				
				if (files != null) {
					
					System.out.println(rep + " -> " + files.length);

					File newname = new File(rep.toString().split("-")[0] + "-" + files.length);
					rep.renameTo(newname);
					System.out.println(rep.getName() + " -> " + newname.getName());
					
				}
				
			}

		}	
		
	}
	
	public static void copy_files() throws IOException {
		File input = new File(inputfolder);
		File output = new File(outputfolder);

//		String[] directories = input.list(new FilenameFilter() {
//		  @Override
//		  public boolean accept(File current, String name) {
//		    return new File(current, name).isDirectory();
//		  }
//		});
		
		//"2se5b100", "2se5r100", "2sb100r100", "e5", "r100","e5b100t1000rif100"
		String[] directories = {"e5"};
		
		System.out.println(Arrays.toString(directories));
		
		
		for (String dir : directories) {
			String expoutfolder = output + "\\" + dir;
			File expout = new File(expoutfolder);
			expout.mkdirs();
			
			String expinfolder = inputfolder + "\\" + dir;
			File expin = new File(expinfolder);
			
			process_exp(expin, expout);
			
		}		
	}
	
	
	
	public static void process_exp(File expin, File expout) throws IOException {
		System.out.println("processing: " + expin);
		
		
		FileUtils.cleanDirectory(expout);  //TODO
		
		Map<String, LinkedList<Map<String, String>>> allFiles = new HashMap<String, LinkedList<Map<String, String>>>(85);
		
		int[] counters = {0};
		
		Files.walk(Paths.get(expin.toURI()))
        .filter(Files::isRegularFile)
        .forEach( (fileName) -> {
        	
        	if (fileName.toString().endsWith(".pred.csv")) {
	        	counters[0]++;
	//        	System.out.println(counters[0] + " : " + fname);
	        	
	        	File file = fileName.toFile();
	        	
	        	String dname = file.getName().split("_")[0].trim();
	        	String ename = fileName.getParent().getParent().toString();
	        	String fname = file.getName();
	        	String fnoext = file.getName().replaceAll("\\..*$", "");		
	        
	    		System.out.println("dataset: " + dname);
	
	    		LinkedList<Map<String, String>> datasetFiles;
	    		
	    		if (allFiles.containsKey(dname)) {
	    			datasetFiles = allFiles.get(dname);
	    		}else {
	    			datasetFiles = new LinkedList<Map<String, String>>();
	    			allFiles.put(dname, datasetFiles);
	    		}
	    		
	    		Map<String, String> expSet = new HashMap<>();
	    		
	    		expSet.put("exp", ename);
	    		expSet.put("name", fnoext);
	
	    		
	    		datasetFiles.add(expSet);
	    		
	//        	allFiles        		
        	}
        	

        });
		
		System.out.println("Found Files: " + counters[0]);
		
		int dataset_counter = 0;
		for (Entry<String, LinkedList<Map<String, String>>>  entry : allFiles.entrySet()) {
			System.out.println(expin.getName() + "  -> copying.." + entry.getKey());
			dataset_counter++;
			
			LinkedList<Map<String, String>> drepeats = entry.getValue();
			
			int repeat_count = 0;
			for (Map<String, String> set : drepeats) {
				repeat_count++;

				File csvSrc = new File(set.get("exp") + "\\csv\\"  + set.get("name") + ".csv");
				File jsonSrc = new File(set.get("exp") + "\\json\\"  + set.get("name") + ".json");
				File predSrc = new File(set.get("exp") + "\\pred\\"  + set.get("name") + ".pred.csv");
				
				
				File csvDest = new File(expout + "\\" + repeat_count + "\\csv\\"  + set.get("name") + ".csv");
				File jsonDest = new File(expout + "\\" + repeat_count + "\\json\\"  + set.get("name") + ".json");
				File predDest = new File(expout + "\\" + repeat_count + "\\pred\\"  + set.get("name") + ".pred.csv");

				
				FileUtils.copyFile(csvSrc, csvDest);
//				FileUtils.copyFile(jsonSrc, jsonDest);
				FileUtils.copyFile(predSrc, predDest);

			}
			
			
			
		}
		
		System.out.println("Found Datasets: " + dataset_counter);

		
	}

}
