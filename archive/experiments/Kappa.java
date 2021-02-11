package development.experiments;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class Kappa {

	public static String folder = "E:\\git\\MultiDomainTSF\\output\\dev\\kappa\\";
	public static String pred_file = folder + "Trace1.pred.csv";

	public static void main(String[] args) throws IOException {
		
		int[][] data = read_pred(pred_file);

		System.out.println(data.length);
		System.out.println(data[0].length);

		//get number of classes in data
		int[] c = {1,2,3,4};
		
		System.out.println("ka: " +  kappa_ij(data, c, 5,6));
		
	}
	
	public static double kappa_ij(int[][] data, int[] classes, int tree_i, int tree_j) {
		double[][] M = new double[classes.length][classes.length];
		int kappa = 0;
		
		for (int i = 0; i < classes.length; i++) {
			for (int j = 0; j < classes.length; j++) {
				M[i][j] = getMij(data, classes[i], classes[j], tree_i, tree_j);
			}
		}
		
		
		return kappa;
	}
	
	
	//proportion of label c_i and label c_j by tree_i and tree_j ?? TODO check
	public static double getMij(int[][] data, int ci, int cj, int tree_i, int tree_j) {
		double mij = 0;
		
		
		return mij;
	}
	
	public static int[][] read_pred(String pred_file) throws IOException {
		List<List<Integer>> data = new ArrayList<>();
		int columns;
		int i = 0, j = 0;
		Integer in = 0;

		try (Reader reader = Files.newBufferedReader(Paths.get(pred_file));
				CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);) {
			for (CSVRecord csvRecord : csvParser) {
				
				if (csvRecord.getRecordNumber() == 1) { //header
					columns  = csvRecord.size();
				}else {
					
					List<Integer> row = new ArrayList<>();
					
					for (j = 0; j < csvRecord.size(); j++) {
						String str = csvRecord.get(j);
						
						if (!str.isEmpty()) {
							try {
								in = Integer.parseInt(str);
							}catch(Exception e) {
								System.out.println("cannot parse " + str);
							}	
							row.add( in );
						}
					}					
					data.add(row);
				}

			}
		}
		
		
		int[][] out = new int[data.size()][data.get(0).size()];
		
		
		
		for (int k = 0; k < out.length; k++) {
			for (int k2 = 0; k2 < out[0].length; k2++) {
				out[k][k2] = data.get(k).get(k2);
			}
		}
		
		
		return out;
	}
}
