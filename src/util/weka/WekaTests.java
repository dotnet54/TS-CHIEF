package util.weka;

import java.io.FileReader;
import java.io.IOException;

import weka.core.Instances;

public class WekaTests {

	public static void main(String[] args) {
		String datapath = "E:/data/ucr/";
		String datasetName = "SonyAIBORobotSurface1";

//		Instances train = ClassifierTools.loadData(datapath + datasetName + "/" + datasetName + "_TRAIN");
		
	}
	
	public static Instances loadArff(String fileName) throws IOException {
        FileReader reader = new FileReader(fileName);
        Instances data = new Instances(reader);
        data.setClassIndex(data.numAttributes() - 1);
        reader.close();
        return data;
	}

}
