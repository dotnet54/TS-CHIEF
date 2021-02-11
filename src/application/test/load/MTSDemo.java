package application.test.load;

import data.io.TSReader;
import data.timeseries.MTSDataset;

public class MTSDemo {

	public static void main(String[] args) {
		try {
			
			testMTSDatasetReading();
			
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void testMTSDatasetReading() {
		TSReader reader = new TSReader();
		MTSDataset train = reader.readFile("E:/data/Multivariate2018_ts/AtrialFibrillation/AtrialFibrillation_TRAIN.ts", null);

		System.out.println(train.size());		
	}

}
