package data.containers;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import data.dev.dataframe.SDataFrame;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 *
 * simple flat table data structure to work with csv files in memory
 * tried to make this similar to pandas in a simple way
 *
 */
public class Table {
    public static final int NONE = -1; // for some cases when python uses None
    protected String NaN = "NA"; //String.valueOf(Double.NaN)

    protected String fileName;
    protected TableIO io;
    protected Map<String, Column> dataFrame;
    protected String indexColumn;
    protected int numColumns;
    protected int currentRow = -1;

//  auto cast column type when adding values
//  rules: int -> dbl, dbl -> str, bln - str
    protected boolean autoCastColumn = true;
    protected boolean trimColumnNames = true;
    protected boolean escapeStrings = true;
    protected boolean quoteStrings = false;
    protected boolean useDenseColumns = true;

    public class Column<T>{

        protected String name;
        protected T className;
        protected ArrayList<T> data;

        public Column(String name, T className){
            this.name = name;
            this.className = className;
            this.data = new ArrayList<>();
        }


    }

    public class TableIO{
        protected int headerRow; // {NONE, 0 <= int}
        protected int indexCol = Table.NONE;
        protected String sep = ",";
        protected String secondary_sep = ";";
        protected String quoteChar = "\"";
        protected String escapeChar = "\\";
        protected String comment = "#";
        protected boolean skipMalformedLines = false; // if false throw error
        protected int numRowsToReadToCheckHeader = 10; // we infer column data types based on this number of rows

        BufferedWriter writer;
//        Table dataFrame;
        protected boolean autoSyncWithFile = false;
        protected int autoFlushBatchSize = 0;
        protected int currentWriteIndex = 0;
        protected boolean appendMode = true;
        protected boolean throwIfFileExists = false;
        protected boolean escapeSpecialChars = true;
        protected boolean quoteStrings = false;


        public TableIO(){

        }

        public void toCsv(String fileName, boolean append) throws IOException {
            File file = new File(fileName);
            file.getParentFile().mkdirs();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, append));
            int size = size();

            // -1  == header row
            for (int i = -1; i < size; i++) {
                bw.write(printRow(i));
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
                writer.flush();
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
            writer.write(printRow(SDataFrame.NONE));
            writer.write("\n");
        }

        public void writeln(int i) throws IOException {
            writer.write(printRow(i));
            writer.write("\n");
//        writer.flush();
        }

        public synchronized void writeAll(boolean writeHeader) throws IOException {
            int size = size();
            int i;
            if (writeHeader){
                i = -1;
            }else{
                i  = 0;
            }
            for (; i < size; i++) {
                writeln(i);
            }
            writer.flush();
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



        public Table readCsv(String fileName) throws IOException {
            return createTableStructureFromFile(fileName);
        }


        // https://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
        // https://stackoverflow.com/questions/18893390/splitting-on-comma-outside-quotes/18893443

        private Table createTableStructureFromFile(String fileName) throws IOException {
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
//            super.fileName = fileName;

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
                                    Double.parseDouble(lineElements[i]);
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
                    addColumn(columns[i], columnTypes[i]);
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
                            add(columns[i], lineElements[i]);
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

            return null;
        }


    }

    public Table(){
        io = new TableIO();
        // preserve the iteration order, and use sync map for thread safety
        dataFrame = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public int size(){
        if (indexColumn == null) {
            return 0;
        }else if (this.dataFrame.get(indexColumn).data.size() == 0){
            return 0;
        }else {
            return currentRow + 1;
        }
    }

    public int length(){
        return numColumns; // assert numColumns == this.dataFrame.size();
    }

    public Column getIndexColumn(){
        return this.dataFrame.get(this.indexColumn);
    }

    // synchronized
    public void setIndexColumn(String column){
        if (this.dataFrame.containsKey(column)){
            this.indexColumn = column;
            if (this.dataFrame.get(indexColumn).data.size() == 0){
                this.currentRow = -1;
            }else{
                this.currentRow = this.dataFrame.get(indexColumn).data.size() - 1;
            }
        }else {
            throw new RuntimeException("Column not found in the dataframe: " + column);
        }
    }

    public synchronized Column resetIndexColumn(String indexName){
        // could have used a treemap with an iterator to make this easy but that slow down access,
        // so I'm just recreatin the list with a new order
        Map newDataFrame = Collections.synchronizedMap(new LinkedHashMap<>());

        // make a new column and add a sequential index, and insert new column as the first column
        Column indexColumn = new Column<>(indexName, Integer.class);
        for (int i = 0; i <= currentRow; i++) {
            indexColumn.data.add(i);
        }
        newDataFrame.put(indexName, indexColumn);

        // copy old columns
        for (Map.Entry<String, Column> columnEntry : this.dataFrame.entrySet()) {
            newDataFrame.put(columnEntry.getKey(), columnEntry.getValue());
        }

        this.numColumns = newDataFrame.size();
        this.dataFrame = newDataFrame;
        setIndexColumn(indexName);
        return indexColumn;
    }

    public synchronized void reOrderColumns(String[] newOrder){
        //TODO
    }

    public synchronized Column dropColumn(String column){
        Column droppedColumn = this.dataFrame.remove(column);
        this.numColumns = this.dataFrame.size();
        if (column.equals(indexColumn)){
            setIndexColumn(this.dataFrame.keySet().iterator().next());
        }
        return droppedColumn;
    }

    public synchronized <T> Column addColumn(String column, T className){
        Column<T> newColumn;
        if(trimColumnNames){
            column = column.trim();
        }
        if (this.dataFrame.containsKey(column)){
            throw new RuntimeException("Column already exists the dataframe: " + column);
        }else{
            newColumn = new Column<T>(column, className);
            this.dataFrame.put(column, newColumn);
            this.numColumns = this.dataFrame.size();
            if (this.indexColumn == null){
                this.setIndexColumn(column);
            }
        }
        return newColumn; // return the new for chaining
    }

    public Column addColumn(String column){
        return addColumn(column, String.class);
    }

    public void addColumns(String[] columns){
        for (int i = 0; i < columns.length; i++) {
            addColumn(columns[i]);
        }
    }

    public void addColumns(String columns){
        String[] columnNames = columns.split(io.sep);;
        if (trimColumnNames){
            Arrays.stream(columns.split(io.sep)).map(String::trim).toArray(x -> columnNames);
        }
        for (String columnName : columnNames) {
            addColumn(columnName);
        }
    }

    public Column getColumn(String column){
        if (this.dataFrame.containsKey(column)){
            return this.dataFrame.get(column);
        }else{
            throw new RuntimeException("Column not found in the dataframe: " + column);
        }
    }

    public String[] columns(){
        return this.dataFrame.keySet().toArray(new String[numColumns]);
    }

    public <T> T[] index(){
        if (indexColumn != null){
            return (T[]) this.dataFrame.get(indexColumn).data.toArray();
        }else{
            throw new RuntimeException("Index column is not set");
        }
    }


    public synchronized <T> void add(int row, String column, T value){
        Column col = this.dataFrame.get(column);
        if (col != null){
            col.data.ensureCapacity(row);
            col.data.add(row, value);
            if (column.equals(indexColumn)){
                this.currentRow = row;
            }
        }
    }

    public synchronized <T> void add(String column, T value){
        Column col = this.dataFrame.get(column);
        if (col != null){

            //fill preceding rows to NaN
            while (col.data.size() < currentRow){
                col.data.add(NaN);
            }

            col.data.add(value);
            if (column.equals(indexColumn)){
                this.currentRow++;
            }
        }
    }

    // synchronized
    public void add(String fromColumn, Object[] values){
        int i = 0;
        boolean skipColumns = true;
        for (Map.Entry<String, Column> entry : dataFrame.entrySet()) {
            // start from the fromColumn
            if (skipColumns){
                if (entry.getKey().equals(fromColumn)){
                    skipColumns = false;
                }else{
                    continue;
                }
            }

            if (i < values.length && i < numColumns){
                add(entry.getKey(), values[i]);
            }else{
                // ignore extra values in the array
                return;
            }
            i++;
        }
    }

//    // gets data by mapping logical index to physical index
//    //TODO
//    public <T> T get(int row, String column){
//        Column col = this.dataFrame.get(column);
//
//        if (0 <= row && row < col.data.size()){
//            // column has data
//            return (T) col.data.get(row);
//        }else{
//            //outside a valid range, so return NaN
//            return (T) io.NaN;
//        }
//    }

    // gets data based on physical index
    public <T> T loc(int row, String column){
        Column col = this.dataFrame.get(column);

        if (0 <= row && row < col.data.size() && row <= currentRow){
            // column has data
            return (T) col.data.get(row);
        }else{
            //outside a valid range, so return NaN
            return (T) NaN;
        }
    }

//    public <T> T iloc(int i, int column){
//        String colName = //get col name from index;
//
//        Column col = this.dataFrame.get(column);
//
//        if (0 <= i && i < col.data.size()){
//            // column has data
//            return (T) col.data.get(i);
//        }else{
//            //outside a valid range, so return NaN
//            return (T) io.NaN;
//        }
//    }

    // https://stackoverflow.com/questions/10451842/how-to-escape-comma-and-double-quote-at-same-time-for-csv-file

    public String printRow(int i){
        StringBuilder sb = new StringBuilder();
        Column column;
        int indexSize, columnSize;
        String escaped;

        if (i == NONE){
            return getHeader();
        }else{
            if (numColumns == 0){
                return "";
            }else if (indexColumn == null){
                throw new RuntimeException("Set an index column");
            }else{
                indexSize = dataFrame.get(indexColumn).data.size();
                if (indexSize == 0){
                    return "";
                }else{
                    for (Map.Entry<String, Column> entry : dataFrame.entrySet()) {
                        column = entry.getValue();
                        columnSize = column.data.size();

                        if (0 <= i && i < columnSize && i <= currentRow){
                            // column has data
                            Object value = column.data.get(i);
                            if (value == null){
                                sb.append(NaN);
                            }else{
                                escaped = StringEscapeUtils.escapeCsv(value.toString());
                                sb.append(escaped);
                            }

                            sb.append(io.sep);
                        }else{
                            // data is NA for this column
                            sb.append(NaN);
                            sb.append(io.sep);
                        }
                    }
                }


            }
        }

        //delete last separator
        int lastSeparator = sb.lastIndexOf(io.sep);
        if (lastSeparator >= 0){
            sb.deleteCharAt(lastSeparator);
        }
        return sb.toString();
    }

    public String getHeader(){
        StringBuilder sb = new StringBuilder();
        Column column;
        String escaped;

        if (numColumns == 0){
            return "";
        }else if (indexColumn == null){
            throw new RuntimeException("Set an index column");
        }else {
            for (Map.Entry<String, Column> entry : dataFrame.entrySet()) {
                column = entry.getValue();
                escaped = StringEscapeUtils.escapeCsv(column.name);
                sb.append(escaped);
                sb.append(io.sep);
            }
        }
        int lastSeparator = sb.lastIndexOf(io.sep);
        if (lastSeparator >= 0){
            sb.deleteCharAt(lastSeparator);
        }
        return sb.toString();
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        String row;

        // -1 == header row
        for (int i = -1; i <= currentRow; i++) {
            row = printRow(i);
            if (!row.isEmpty()){
                sb.append(row);
                sb.append("\n");
            }

        }

        int lastSeparator = sb.lastIndexOf("\n");
        if (lastSeparator >= 0){
            sb.deleteCharAt(lastSeparator);
        }
        return sb.toString();
    }

    // for debugging
    protected void printInternalStructure(){
        StringBuilder sb = new StringBuilder();
        Column column;
        System.out.println(printRow(-1));
        for (Map.Entry<String, Column> entry : dataFrame.entrySet()) {
            column = entry.getValue();
            sb.append(column.className);
            sb.append(":");
            sb.append(column.data.size());
            sb.append(io.sep);
        }
        System.out.println(sb.toString());
    }

    public void toCsv(String fileName, boolean append) throws IOException {
        io.toCsv(fileName, append);
    }

    public static Table readCsv(String fileName) throws IOException {
        Table df = new Table();
        df.io.readCsv(fileName);
        return df;
    }


    public static void main(String[] args) throws IOException {

        Table table = new Table();

        // add columns
        assert table.length() == 0;
        assert table.size() == 0;
        assert table.currentRow == -1;
        System.out.println(table);
        table.addColumn("dataset");
        assert table.length() == 1;
        assert table.size() == 0;
        assert table.currentRow == -1;
        assert table.getIndexColumn().name.equals("dataset");
        System.out.println(table);

        table.addColumn("trainSize", Integer.class);
        table.addColumn("testSize", Integer.class);
        table.addColumn("accuracy", Double.class);
        table.addColumn("normalized", Boolean.class);
        table.addColumn("comments", String.class);
        table.addColumns(new String[]{"host, name", "paramID"});
        table.addColumns("args, test, space \" quote");
        assert table.length() == 11;

        //add some data to row 0
        assert table.size() == 0;
        assert table.currentRow == -1;
        table.add("trainSize", 01); // non index column
        assert table.size() == 0;
        assert table.currentRow == -1;
        System.out.println(table);
        table.add("dataset", "BasicMotions");   //index column
        assert table.size() == 1;
        assert table.currentRow == 0;
        System.out.println(table);

        table.add("testSize", 02);
        table.add("accuracy", 0.8);
        table.add("normalized", false);
        table.add("comments", "\"06: comment with quote's\"");
        table.add("host, name", new Object[] {"07", 8,9,10,11});
        assert table.size() == 1;
        assert table.currentRow == 0;
        System.out.println(table);

        // add to row 1
        table.add("dataset", new Object[]{"LSST", 11,12,0.5, true});
        assert table.size() == 2;
        assert table.currentRow == 1;
        table.add("trainSize", 21);
        assert table.size() == 2;
        assert table.currentRow == 1;
        assert table.getColumn("trainSize").data.size() == 3;

        // change index to new column
        table.setIndexColumn("trainSize");
        assert table.size() == 3;
        assert table.currentRow == 2;
        table.add("trainSize", 31);
        assert table.size() == 4;
        assert table.currentRow == 3;
        assert table.getColumn("trainSize").data.size() == 4;
        System.out.println(table);
        table.printInternalStructure();

        // change index back the original column
        table.setIndexColumn("dataset");
        assert table.size() == 2;
        assert table.currentRow == 1;

        System.out.println(table);
        table.printInternalStructure();

        // data types
        String d = table.loc(0, "dataset");
        assert d.equals("BasicMotions");
        Integer ts = table.loc(0, "trainSize");
        assert ts == 1;
        assert ts.getClass() == Integer.class;
        assert table.loc(0, "accuracy").getClass() == Double.class;
        assert table.loc(1, "normalized").getClass() == Boolean.class;

        // printing manually
        assert table.currentRow == 1;
        System.out.println(table.printRow(Table.NONE));
        System.out.println(table.printRow(0));
        System.out.println(table.printRow(1));
        System.out.println(table.printRow(2));
        System.out.println(table.printRow(3));
        System.out.println(table.printRow(4));
        assert table.loc(2, "trainSize").equals(table.NaN);

        // drop column
        table.dropColumn("test");
        assert table.length() == 10;

        // reset index  - auto index
        table.resetIndexColumn("index");
        assert table.getIndexColumn().name.equals("index");
        System.out.println(table);

        // modify data alignment manually
        table.printInternalStructure();
        table.add("index", table.size());
        assert table.size() == 3;
        table.add("dataset", "Fish");
        table.add("test", "this column is not in frame");
        System.out.println(table);

        // add to columns with smaller internal size than the index column
        // make sure that the printing of rows are aligned using NA
        table.add("comments", "26: internal row 1");
        assert table.loc(2, "comments").equals("26: internal row 1");

        // keep adding data to same column without adding to index column
        table.add("trainTime", 12345);
        table.add("trainTime", 12346);
        assert table.size() == 3;
        table.add("comments", "eg, comma");
        table.add("args", "{eg, comma, \"e:1,b:100;s5\", [3,4,5]}");
        assert table.length() == 11;
//        assert table.getColumn("dataset").get(2) == null;

        System.out.println(table);

        table.toCsv("out/dev/tmp.csv", false);

        table.io.open("out/dev/tmp2.csv", false);
        table.io.writeAll(true);
        table.io.close();

        Table table2 = Table.readCsv("out/dev/tmp2.csv");
        System.out.println(table2);
        table2.printInternalStructure();

        // test with an actual result file
        String path = "E:\\git\\dotnet54\\TS-CHIEF-DEV\\out\\knn\\dev\\train\\BasicMotions//";
        String file = "BasicMotions-erp-d-p_0_100_1.train.exp.csv";


    }


}
