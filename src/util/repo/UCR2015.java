package util.repo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import application.AppConfig;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

/**
 * Utility methods specific to UCR2015 archive
 * 
 * @author ashi32
 *
 */
public class UCR2015 extends DataArchive {

	public static final String archiveName = "ucr2015";
	public HashMap<String, String> ucr2015NameMap; //key = dataset name, value = bakeoff dataset name (relation name in arff files)

	public UCR2015(String archivePath) throws IOException {
		this.archivePath = archivePath;
		this.infoFile = AppConfig.resource_dir + "/" + archiveName + ".csv";
		train_file_suffix = "_TRAIN.txt";
		test_file_suffix = "_TEST.txt";
		ucr2015NameMap = getBakeoffDatasetNamesFromCsvFile(infoFile);
	}

	public String to2015Name(String datasetName) {
		return ucr2015NameMap.get(datasetName);
	}

	public HashMap<String, String> getBakeoffDatasetNamesFromCsvFile(String fileName) throws IOException {
		HashMap<String, String> map = new HashMap<String,String>();

		Table ucr2015 = Table.read().csv(fileName);
		StringColumn datasetColumn = ucr2015.stringColumn("dataset");
		List<String> datasetNames =  datasetColumn.asList();
		List<String> bakeOffPaperNames =  ucr2015.stringColumn("arff_name").asList();

		for (Row row : ucr2015) {
//			System.out.println("On " + row.getString("dataset") + ": " + row.getString("bakeoff_name"));
			map.put(row.getString("dataset"),row.getString("arff_name"));
		}

		return map;
	}



	/**
	 * 
	 * builds a hashmap to map UCR dataset names between www.timeseriesclassification.com names 
	 * and old repository names (stored in relationName of .arff files)
	 * 
	 * eg. 50words <--> FiftyWords --> this is important when working with data obtained from different projects
	 * //key = dataset name, value = bakeoff dataset name (relation name in arff file)
	 * 
	 * @param folder
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, String> getBakeoffDatasetNamesFromFolder(String folder) throws IOException {
		String line = "";
		String fullName = null;
		
		//key = dataset name, value = bakeoff dataset name (relation name in arff file)
		HashMap<String, String> map = new HashMap<String,String>();

		File dir = new File(folder);
		// gets you the list of files at this folder
		File[] listOfFiles = dir.listFiles();
		// loop through each of the files looking for filenames that match
		for (int i = 0; i < listOfFiles.length; i++) {
			String filename = listOfFiles[i].getName();
			
			if (filename.startsWith(".") || filename.equals("SatelliteFull")) {
				continue;
			}
			
//			System.out.println("File: " + filename);

			try (BufferedReader br = new BufferedReader(new 
					FileReader(folder + "/" + filename + "/"+ filename + "_TRAIN.arff"))) {
				int lineNumber = 0;

				while ((line = br.readLine()) != null) {

					if (line.startsWith("@relation")) {
						// header
												
						map.put(filename, line.substring(10, line.length()).trim());
						
//						System.out.println(line);
//						System.out.println( filename+ ","+map.get(filename));

						break;
					} else {
						
					}

					lineNumber++;
				}

			} catch (IOException e) {
				throw e;
			}
			
		}
		
		return map;
	}
}
