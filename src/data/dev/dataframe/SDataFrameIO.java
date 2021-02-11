package data.dev.dataframe;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.*;
import java.util.regex.Pattern;

/**
 * class for csv io operations
 * uses two pass reading: first checks meta data, second loads actual data
 *
 * tried to keep api similar to pandas
 *
 * support json encoded meta data
 * support column dtype encoding using comments
 *
 */
public class SDataFrameIO {

    protected int headerRow; // {NONE, 0 <= int}
    protected int indexCol = SDataFrame.NONE;
    protected String sep = ",";
    protected String secondary_sep = ";";
    protected String quoteChar = "\"";
    protected String escapeChar = "\\";
    protected String comment = "#";
    protected boolean skipMalformedLines = false; // if false throw error
    protected int numRowsToReadToCheckHeader = 10; // we infer column data types based on this number of rows
    protected SDataFrame.DTYPE defaultColumnType = null;  // STable.DTYPE.str;

    BufferedWriter writer;
    SDataFrame dataFrame;
    protected boolean autoSyncWithFile = false;
    protected int autoFlushBatchSize = 0;
    protected int currentWriteIndex = 0;
    protected boolean appendMode = true;
    protected boolean throwIfFileExists = false;
    protected boolean escapeSpecialChars = true;
    protected boolean quoteStrings = false;

    // data type specific - String
    protected String strNaN = "NA"; //String.valueOf(Double.NaN)

    // data type specific - double
    protected double dblNan = Double.NaN;

    // data type specific - int

    // data type specific - boolean

    //TODO auto sync to disk/flush

    public SDataFrameIO(SDataFrame dataFrame){
        this.dataFrame = dataFrame;
    }

    public SDataFrameIO(int headerRow){
        this.headerRow = headerRow;
    }

    public static void toCsv(SDataFrame df, String fileName, boolean append) throws IOException {
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, append));
        int size = df.size();

        bw.write(df.printRow(SDataFrame.NONE));
        bw.write("\n");
        for (int i = 0; i < size; i++) {
            bw.write(df.printRow(i));
            bw.write("\n");
        }

        bw.close();
    }

    public BufferedWriter open(String fileName, boolean append) throws IOException {
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        writer = new BufferedWriter(new FileWriter(file, append));;
        return writer;
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void close() throws IOException {
        if (writer != null){
            writer.close();
        }
    }

    public int getCurrentWriteIndex(){
        return currentWriteIndex;
    }

    public void setCurrentWriteIndex(int i){
        this.currentWriteIndex = i;
    }

    public void writeHeader() throws IOException {
        writer.write(dataFrame.printRow(SDataFrame.NONE));
        writer.write("\n");
    }

    public void writeln(int i) throws IOException {
        writer.write(dataFrame.printRow(i));
        writer.write("\n");
//        writer.flush();
    }

    public synchronized void writeAll() throws IOException {
        int size = dataFrame.size();
        for (int i = 0; i < size; i++) {
            writeln(i);
        }
    }

    // uses an internal head
    public synchronized void writeln() throws IOException {
        writeln(currentWriteIndex);
        currentWriteIndex++;
    }

    public void writeBatch(int numRows) throws IOException {
        int size = dataFrame.size();
        for (int i = currentWriteIndex; i < numRows && i < size; i++) {
            writeln(i);
            currentWriteIndex++;
        }
        writer.flush();
    }



    public SDataFrame readCsv(String fileName) throws IOException {
        return createTableStructureFromFile(fileName);
    }


    // https://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
    // https://stackoverflow.com/questions/18893390/splitting-on-comma-outside-quotes/18893443

    private SDataFrame createTableStructureFromFile(String fileName) throws IOException {
        LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(fileName));
        BufferedReader br = null;
        int numLinesRead = 0;       //excludes comment lines
        String line = null;
        String unescaped = null;
        String[] lineElements;
        String[] columns = null;
        Object[] columnTypes = null;
        boolean headerInitialized = false;

//        SDataFrame table = new SDataFrame();
//        dataFrame.clear();
        dataFrame.fileName = fileName;

        try{
            // read structure
            while ((line = lineNumberReader.readLine()) != null) {
                if (line.startsWith(comment)) {
                    //skip
                    // read meta data in the comment section
                } else if (headerRow == numLinesRead){
                    //read header names

                    //TODO
//                    unescaped = StringEscapeUtils.unescapeCsv(line);
//                    columns = unescaped.split(sep);
                    columns = Iterables.toArray(
                            Splitter.on(
                                    Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                                    .trimResults().split(line),
                            String.class);
                 }else if (columns != null){ //if we have not read header, keep skipping lines
                    if (numLinesRead >= numRowsToReadToCheckHeader){
                        continue;
                    }

//                    unescaped = StringEscapeUtils.unescapeCsv(line);
//                    lineElements = unescaped.split(sep);
                    lineElements = Iterables.toArray(
                            Splitter.on(
                                    Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                                    .trimResults().split(line),
                            String.class);
                    if (columnTypes == null){
                        columnTypes = new Object[lineElements.length];
                    }

                    // currently supports dbl and str while reading  -- THIS is to be defensive and simple
                    // after reading the file change column type if needed.

                    //column data type is updated if necessary after reading every row
                    //dont make a data type of column smaller than last inferred value to prevent data loss
                    // eg. dont go from double to int or str to double, but try to go from int to double, double to str
                    for (int i = 0; i < lineElements.length; i++) {

//                        // if possible convert to a boolean column
//                        if (((columns[i] != null) && columns[i].equalsIgnoreCase("true"))){
//                            inferredDType = STable.DTYPE.bool;
//                        }else if(((columns[i] != null) && columns[i].equalsIgnoreCase("false"))){
//                            inferredDType = STable.DTYPE.bool;
//                        }

//                        // try if we can keep the column as an integer column
//                        try{
//                            Integer.parseInt(columns[i]);
//                            inferredDType = STable.DTYPE.integer;
//                        }catch (NumberFormatException e){
//                            //cant parse
//                        }

                        assert columnTypes.length == lineElements.length;
                        // change data type only if its not set, or if we need to expand the size
                        if (columnTypes[i] == null ||  ( columnTypes[i] != null && columnTypes[i].equals(SDataFrame.DTYPE.dbl))){
                            try{
                                Double.parseDouble(columns[i]);
//                                columnTypes[i] = SDataFrame.DTYPE.dbl;
                                columnTypes[i] = Double.class;

                            }catch (NumberFormatException e){
                                //cant parse as a double
                                columnTypes[i] = String.class;
                            }
                        }

                    }

                }else {
                    //skip
                }
                numLinesRead += 1;
            }

            //create table and add header
            assert columns.length == columnTypes.length;
            for (int i = 0; i < columns.length; i++) {
                dataFrame.addColumn(columns[i], columnTypes[i]);
            }

            // read data
            br = new BufferedReader(new FileReader(fileName));
            numLinesRead = 0;
            while ((line = br.readLine()) != null) {

                if (line.startsWith(comment)) {
                    //skip
                    // read meta data in the comment section
                } else if (numLinesRead == headerRow){
                    numLinesRead++;
                    continue;
                }else {
                    lineElements = Iterables.toArray(
                            Splitter.on(
                                    Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                                    .trimResults().split(line),
                            String.class);

                    for (int i = 0; i < lineElements.length; i++) {
                        dataFrame.add(columns[i], lineElements[i]);
                    }
                }
                numLinesRead++;
            }

        }finally{
            lineNumberReader.close();
            if (br != null){
                br.close();
            }
        }

        return dataFrame;
    }

//    private boolean initializeHeader(SDataFrame table, String[] columnNames, int numColumns){
//        table.shape[1] = numColumns;
//
//        if (columnNames == null){
//            for (int i = 0; i < numColumns; i++) {
////                table.columns[i] = String.valueOf(i);
//                table.columnDTypes[i] = defaultColumnType;
//            }
//        }else{
////            table.columns = columnNames;
//            for (int i = 0; i < columnNames.length; i++) {
//                table.columnDTypes[i] = defaultColumnType;
//            }
//        }
//        return true;
//    }

}
