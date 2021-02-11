package application.test;

import application.AppConfig;
import data.io.CsvWriter;
import data.io.DataLoader;
import data.timeseries.Dataset;

public class Test {


    public static void main(String[] args) {
        try {
            String dataPath = "E:/data/";
            String archive = "Multivariate2018_ts";
            String fileType = ".ts";
            String[] datsets = "Heartbeat"
                    .split(",");

            AppConfig.initializeAppConfig();

            for (String datasetName:datsets) {
                Dataset trainData =  DataLoader.loadTrainingSet(dataPath + archive + "/" + datasetName + "/"
                        + datasetName + "_TRAIN" + fileType);
                Dataset testData =  DataLoader.loadTestingSet(dataPath + archive + "/" + datasetName + "/"
                        + datasetName + "_TEST" + fileType, trainData);


                // --- for temporary tests
                CsvWriter file = new CsvWriter("out/test.csv", CsvWriter.WriteMode.append, 5);
                file.addColumns("dataset,accuracy,traintime");
                file.addColumns("testtime", "trees");
                file.add("Beef", "0.90", "1544", "657");
                file.addColumnValue("trees","Coffee");
                file.addRowAsString("Fish,0.8,2,5,10");
                file.addRowAsString("Italy,0.8,2,5,10");

                file.appendWithoutHeaderIfExists();

                file.close();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
