package data.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CsvWriter {

    protected String fileName;
    protected boolean append;
    protected BufferedWriter writer;
    protected String separator = ",";
    protected String missingStringValue = "N/A";
    protected String missingNumericValue = String.valueOf(Double.NaN);
    protected boolean allowMissingValues = false;
    protected boolean headerWritten = false;
    protected Row currentRow;
    protected Map<String, Integer> header;  //columnName -> columnID
    protected List<Row> data;

    public CsvWriter(String fileName, boolean append) throws IOException {
        this.fileName = fileName;
        this.append = append;
        this.header = new HashMap<>();
        this.data = new ArrayList<>();
        open();
    }

    public void add(String columnName, String value) throws Exception {
        if(!header.containsKey(columnName)){
            throw new Exception("Column not defined in the csv header");
        }
        if (currentRow.rowData.containsKey(columnName)){
            throw new Exception("Row value has already been set for the column: " + columnName);
        }

        Integer columnID = header.get(columnName);
        currentRow.rowData.put(columnID, value);

    }

    public void addRow(String ... values) {
        for (String value: values) {
            for (Map.Entry<String, Integer> entry : header.entrySet()) {

            }
            this.currentRow.add();
        }
    }

    private class Row {
        protected Map<Integer, String> rowData; // columnID -> columnValue

        public Row(){
            rowData = new HashMap<>();
        }

        public Row(int numColumns){
            rowData = new HashMap<>(numColumns);
        }

        public void add(Integer columnID, String value){
            rowData.put(columnID, value);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < header.size(); i++) {
                sb.append(rowData.get(header.get(i)));
                sb.append(separator);

            }
            return sb.toString();
        }
    }

    public void open() throws IOException {
        writer = new BufferedWriter(new FileWriter(this.fileName, this.append));
    }

    public void close() throws IOException {
        writer.close();
    }

    public int numColumns(){
        if (this.data.isEmpty()){
            return 0;
        }else{
            return this.data.get(0).rowData.keySet().size();
        }
    }

    public int numRows(){
        return this.data.size();
    }

    public void writeHeader() throws IOException {
        if (!headerWritten){
            writer.write(getHeader());
            headerWritten = true;
        }
    }

    public String getHeader(){
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : this.data.get(0).rowData.keySet()) {

        }

        return this.header.toString();
    }

    public String getCurrentRow(){
        return this.currentRow.toString();
    }


    public void addColumns(String columnNames){

        this.header.append(columnNames);
        this.header.append(separator);
        this.numColumns = this.header.toString().split(separator).length;
    }

    public void removeColumn(String columnsName){
        this.header.replace(this.header.indexOf(columnsName), columnsName.length(), "");
    }

    public void addRow(String row){
        this.header.append(columnNames);
        this.header.append(separator);
        this.numColumns = this.header.toString().split(separator).length;
    }

}
