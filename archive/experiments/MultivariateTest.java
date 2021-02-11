package development.experiments;

import data.io.TSReader;
import data.timeseries.MTSDataset;

public class MultivariateTest {

    public static void main(String[] args) {
        try {

            System.out.println("Testing MultivariateTest");

            String datasetName = "BasicMotions";
            String archiveName = "Multivariate2018_ts";
            String trainFile = "E:/data/" + archiveName + "/" + datasetName + "/" + datasetName + "_TRAIN.ts";
            String testFile = "E:/data/" + archiveName + "/" + datasetName + "/" + datasetName + "_TEST.ts";

            TSReader reader = new TSReader();
            MTSDataset train = reader.readFile(trainFile);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
