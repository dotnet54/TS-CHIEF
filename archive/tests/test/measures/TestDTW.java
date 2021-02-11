package test.measures;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.*;

import core.AppContext;
import core.CSVReader;
import datasets.TSDataset;
import datasets.TimeSeries;

public class TestDTW {

	protected final static double DBL_PRE_DELTA = 0.0001;
	protected static int seed = 0;
	protected static Random rand;
	
	protected static TSDataset testData;
	protected static TSDataset trainData;
	protected static String trainFile = "E:/data/ucr/ItalyPowerDemand/ItalyPowerDemand_TRAIN.txt";
	protected static String testFile = "E:/data/ucr/ItalyPowerDemand/ItalyPowerDemand_TRAIN.txt";

	
	@BeforeClass
	public static void setUp() {
		System.out.println("Initializing");
		
		trainData = CSVReader.readCSVToTSDataset(trainFile, AppContext.csv_has_header, 
						AppContext.target_column_is_first, ",", AppContext.verbosity);
		testData = CSVReader.readCSVToTSDataset(testFile, AppContext.csv_has_header, 
						AppContext.target_column_is_first, ",", AppContext.verbosity);
		
		
		rand = new Random(seed);
		
	}
	
	@Test
	public void testDTW() {
		int window = 5;
		
		test.measures.imp.francois.DTW francoisDTW = new test.measures.imp.francois.DTW();
		test.measures.imp.geoff.FasterDTW geoffDTW = new test.measures.imp.geoff.FasterDTW();

		
		TimeSeries series1 = trainData.get_series(rand.nextInt(trainData.size()));
		TimeSeries series2 = trainData.get_series(rand.nextInt(trainData.size()));
		
		double fDistance = francoisDTW.distance(series1.getData(), series2.getData(), Double.POSITIVE_INFINITY, window);
		double gDistance = geoffDTW.distance(series1.getData(), series2.getData(), Double.POSITIVE_INFINITY, window);

		
		
		assertEquals("francois == geoff for: i = " + 0 , fDistance, gDistance, DBL_PRE_DELTA);
	}
	
//	public void double[] find
	
	

}
