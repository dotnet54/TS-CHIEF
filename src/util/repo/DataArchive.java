package util.repo;

import application.AppConfig;

import java.io.IOException;

public abstract class DataArchive {
    public String train_file_suffix;
    public String test_file_suffix;
    public String archivePath;
    public String infoFile;

    public String getTrainingFileFullPath(String datasetName){
        return archivePath + "/" + datasetName + "/" + datasetName + train_file_suffix;
    }

    public String getTestingFileFullPath(String datasetName){
        return archivePath + "/" + datasetName + "/" + datasetName + test_file_suffix;
    }

    public static class UCR2018UTS extends DataArchive {

        public static final String archiveName = "Univariate2018_ts";

        public UCR2018UTS(String archivePath)  throws IOException {
            this.archivePath = archivePath;
            this.infoFile = AppConfig.resource_dir + "/" + archiveName + ".csv";
            train_file_suffix = "_TRAIN.ts";
            test_file_suffix = "_TEST.ts";
        }
    }

    public static class UCR2018MTS extends DataArchive {

        public static final String archiveName = "Multivariate2018_ts";

        public UCR2018MTS(String archivePath)  throws IOException {
            this.archivePath = archivePath;
            this.infoFile = AppConfig.resource_dir + "/" + archiveName + ".csv";
            train_file_suffix = "_TRAIN.ts";
            test_file_suffix = "_TEST.ts";
        }

    }

}
