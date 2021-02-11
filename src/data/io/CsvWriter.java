package data.io;

import application.AppConfig;
import data.timeseries.Dataset;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CsvWriter {

    protected static final String separator = ",";
    protected static final String missingStringValue = "N/A";
    protected static final String quoteChar = "\"";
    protected static final String newline = "\n";

    protected String fileName;
    protected File file;
    protected BufferedWriter writer;
    protected String missingNumericValue = String.valueOf(Double.NaN);
    protected boolean allowMissingValues = false;
    protected boolean headerWritten = false; //assume header is present if there is data
    protected LinkedHashMap <String, List<String>> data;
    protected ArrayList<String> columnList;
    protected int numRows = 0;
    protected int numColumns = 0;
    protected int currentColumnIndex = 0;
    protected int currentRowIndex = 0;
    protected boolean fileExists = false;


//    protected int currentReadRow = 0;
//    protected int currentReadColumn = 0;
    protected int currentWriteRow = 0;
//    protected int currentWriteColumn = 0;

    protected WriteMode writeMode;
    public enum WriteMode{
        overwrite, append, skip;

        // converter that will be used later
        public static WriteMode fromString(String code) {

            for(WriteMode output : WriteMode.values()) {
                if(output.toString().equalsIgnoreCase(code)) {
                    return output;
                }
                if(output.toString().startsWith(code)) {
                    return output;
                }
            }

            return null;
        }
    }

    public CsvWriter(File file, WriteMode writeMode, int estimatedSize) throws IOException {
        this.file = file;
        this.fileName = file.getName();
        this.writeMode = writeMode;
        this.data = new LinkedHashMap <>();
        this.columnList = new ArrayList<>();
        if (file.exists()){
            fileExists = true;
        }
        open();
    }

    public CsvWriter(String folderName, String fileName, WriteMode writeMode, int estimatedSize) throws IOException {
        this.fileName = folderName + "/" + fileName;
        this.writeMode = writeMode;
        this.data = new LinkedHashMap <>(estimatedSize);
        this.columnList = new ArrayList<>();
        file = new File(this.fileName);
        if (file.exists()){
            fileExists = true;
        }
        open();
    }

    public CsvWriter(String fileName, WriteMode writeMode, int estimatedSize) throws IOException {
        this.fileName = fileName;
        this.writeMode = writeMode;
        this.data = new LinkedHashMap <>(estimatedSize);
        this.columnList = new ArrayList<>();
        file = new File(this.fileName);
        if (file.exists()){
            fileExists = true;
        }
        open();
    }

    public void addColumns(String ... columnNames) throws RuntimeException {
        for (String columnNamesList : columnNames) {
            String[] columnNamesArray = columnNamesList.split(separator);
            for (String colName : columnNamesArray) {
                if (this.data.containsKey(colName)){
                    throw new RuntimeException("Column already defined: "+ colName);
                }
                this.data.put(colName, new ArrayList<>(numRows));
            }
        }
        this.columnList = new ArrayList<>(this.data.keySet());
        numColumns = this.data.keySet().size();
    }

    public void removeColumn(String columnsName){
        this.data.remove(columnsName);
        this.columnList.remove(columnsName);
    }

    public void add(String columnName, int rowIndex, String value) throws RuntimeException {
        if(! data.containsKey(columnName)){
            throw new RuntimeException("Column not defined in the csv header");
        }
        data.get(columnName).add(rowIndex, value);
    }

    // note: check thread-safety
    public synchronized void add(Object ... values) throws Exception {
        for (Object value : values) {
            if (currentColumnIndex >= numColumns) {
                currentColumnIndex = 0;
                currentRowIndex++;
            }
            String currentColumnName = columnList.get(currentColumnIndex);
            add(currentColumnName, currentRowIndex, String.valueOf(value));
            currentColumnIndex++;
        }

    }

    public void addColumnValue(String columnName, String value) throws RuntimeException {
        this.add(columnName, currentRowIndex, value);
    }

    public void addRowAsString(String row) throws Exception {
        this.add(row.split(separator));
    }

    public synchronized boolean open() throws IOException {
        if (writeMode == WriteMode.append){
            writer = new BufferedWriter(new FileWriter(this.fileName, true));
        }else if (writeMode == WriteMode.overwrite){
            writer = new BufferedWriter(new FileWriter(this.fileName, false));
        }else if (this.file.exists() && writeMode == WriteMode.skip){
            System.out.println(" WARN: skipping file writing because a file with this name exists: " + this.fileName);
            return false;
        }else {
            writer = new BufferedWriter(new FileWriter(this.fileName, true));
        }
        return true;
    }

    public void close() throws IOException {
        if (writer != null){
            writer.close();
        }
    }

    public int numColumns(){
        return this.data.keySet().size();
    }

    public int numRows(){
        return this.currentRowIndex + 1;
    }

    public synchronized void writeHeader() throws IOException {
        if (file.length() == 0){
            writer.write(getHeader());
            writer.write(newline);
            headerWritten = true;
        }
    }

    public void appendWithHeader() throws Exception {
        this.writeAll(true);
    }

    public void appendWithoutHeader() throws Exception {
        this.writeAll(false);
    }

    public void appendWithoutHeaderIfExists() throws Exception {
        this.writeAll(! fileExists);
    }

    public void append() throws Exception {
        this.writeAll(false);
    }

    public void writeAll(boolean addHeader) throws Exception {
        if(addHeader){
            writeHeader();
        }

        for (int i = 0; i <= currentRowIndex; i++) {
            writer.write(this.getRow(i));
            writer.write(newline);
        }
    }

    public void resetHead(){
        currentWriteRow = 0;
    }

    public synchronized void flush() throws Exception {
        for (int i = currentWriteRow; i <= currentRowIndex; i++) {
            writer.write(this.getRow(i));
            writer.write(newline);
            currentWriteRow++;
        }
        writer.flush();
    }

    public void writeNewLines(int n) throws IOException {
        for (int i = 0; i < n; i++) {
            writer.write(newline);
        }
    }

    public void writeNewLine() throws IOException {
        writer.write(newline);
    }

    public String getHeader(){
        StringBuilder header = new StringBuilder();
        int j = 0;
        for (String columnName : this.data.keySet()) {
            header.append(columnName);
            if (j != numColumns-1){
                header.append(separator);
            }
            j++;
        }
        return header.toString();
    }

    public String getRow(int i) throws Exception {
        if (i > currentRowIndex){
            throw new Exception("Requested row index " + i +" is greater than the number of rows " + currentRowIndex+1);
        }
        StringBuilder row = new StringBuilder();
        int j = 0;
        for (String columnName : columnList) {
            List<String> series = this.data.get(columnName);
            if (series.size() > i) {
                row.append(series.get(i));
                if (j != numColumns - 1) {
                    row.append(separator);
                }
                j++;
            } else if (allowMissingValues) {
                row.append(missingStringValue);
                if (j != numColumns - 1) {
                    row.append(separator);
                }
                j++;
            } else {
                throw new Exception( "Requested row index " + i + " is larger than the size of column " + columnName + " ("+ series.size() +")");
            }
        }
        for (Map.Entry<String, List<String>> entry : data.entrySet()) {

        }
        return row.toString();
    }

    public int getCurrentColumnIndex() {
        return currentColumnIndex;
    }

    public int getCurrentRowIndex() {
        return currentRowIndex;
    }

    public static String quoteString(String value){
        return quoteChar + value + quoteChar;
    }

    public static String quoteSet(int[] values){
        StringBuilder sb = new StringBuilder();
        sb.append(quoteChar);
        sb.append("{");
        for (int i = 0; i < values.length-1; i++) {
            sb.append(values[i]);
            sb.append(",");
        }
        sb.append(values[values.length-1]);
        sb.append("}");
        sb.append(quoteChar);
        return sb.toString();
    }

    public static String quoteSet(double[] values){
        StringBuilder sb = new StringBuilder();
        sb.append(quoteChar);
        sb.append("{");
        for (int i = 0; i < values.length-1; i++) {
            sb.append(values[i]);
            sb.append(",");
        }
        sb.append(values[values.length-1]);
        sb.append("}");
        sb.append(quoteChar);
        return sb.toString();
    }

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
                CsvWriter file = new CsvWriter("out/test.csv", WriteMode.append, 10);
                file.addColumns("dataset,accuracy,traintime");
                file.addColumns("testtime", "trees");
                file.add("Beef", "0.90", "1544", "657");
                file.addColumnValue("trees","Coffee");
                file.addRowAsString("Fish,0.8,2,5,10");
                file.addRowAsString("Italy,0.8,2,5,10");

                //TODO add flush function

                file.append();
                file.close();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
