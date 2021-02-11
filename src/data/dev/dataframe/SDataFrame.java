package data.dev.dataframe;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.util.*;

/**
 *
 * simple flat table data structure to work with csv files in memory
 * tried to make this similar to pandas in a simple way
 *
 */
public class SDataFrame{
    public static final int NONE = -1; // for some cases when python uses None

    public enum DTYPE{
        obj("object"),
        str("string"),
        date("date"),
        integer("int"),
        dbl("double"),
        bool("boolean"); // internally int
        public final String name;
        DTYPE(String name) {
            this.name = name;
        }
    }

    protected String fileName;
    protected SDataFrameIO io;
    protected Map<String, STColumn> dataFrame;
    protected String indexColumn;
//    protected int numRows; // sync with currentRow + 1
    protected int numColumns; // keep in sync with dataFrame.size()
    protected int currentRow = -1; //current/last row index for reading and writing - used to sync across columns
    protected int numColumnsInCurrentRow;

//    auto cast column type when adding values
    protected boolean autoCastColumn = true; // rules: int -> dbl, dbl -> str, bln - str
    protected boolean trimColumnNames = true;
    protected boolean escapeStrings = true;
    protected boolean quoteStrings = false;
    protected boolean useDenseColumns = true;

    public SDataFrame(){
        io = new SDataFrameIO(this);
        // preserve the iteration order, and use sync map for thread safety
        dataFrame = Collections.synchronizedMap(new LinkedHashMap<>());
    }


    public int size(){
        if (currentRow == 0){
            if (numColumnsInCurrentRow < numColumns){
                return currentRow + 1;
            }else {
                return currentRow;
            }
        }else {
            if (numColumnsInCurrentRow < numColumns){
                return currentRow + 1;
            }else {
                return currentRow;
            }
        }

    }

    public int length(){
        return numColumns; // this.dataFrame.size();
    }

    // synchronized
    public void setIndexColumn(String column){
        if (this.dataFrame.containsKey(column)){
            this.indexColumn = column;

            if (numColumnsInCurrentRow < numColumns){
                this.currentRow = this.dataFrame.get(column).size();
            }else {
                this.currentRow = this.dataFrame.get(column).size() - 1;
            }

        }else {
            throw new RuntimeException("Column not found in the dataframe: " + column);
        }
    }

    public STColumn getIndexColumn(){
        return this.dataFrame.get(this.indexColumn);
    }

    public synchronized STColumn resetIndexColumn(String indexName){
        // could have used a treemap with an iterator to make this easy but that slow down access,
        // so I'm just recreatin the list with a new order
        Map newDataFrame = Collections.synchronizedMap(new LinkedHashMap<>());

        // make a new column and add a sequential index, and insert new column as the first column
        STColumn indexColumn = new STColumn<>(indexName, Integer.class);
        for (int i = 0; i <= currentRow; i++) {
            indexColumn.add(i);
        }
        newDataFrame.put(indexName, indexColumn);

        // copy old columns
        for (Map.Entry<String, STColumn> columnEntry : this.dataFrame.entrySet()) {
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

    public synchronized STColumn dropColumn(String column){
        STColumn droppedColumn = this.dataFrame.remove(column);
        this.numColumns = this.dataFrame.size();
        if (column.equals(indexColumn)){
            setIndexColumn(this.dataFrame.keySet().iterator().next());
        }
        return droppedColumn;
    }

    public synchronized <T> STColumn addColumn(String column, T className){
        STColumn<T> newColumn;

        if(trimColumnNames){
            column = column.trim();
        }

        if (this.dataFrame.containsKey(column)){
            throw new RuntimeException("Column already exists the dataframe: " + column);
        }else{
            newColumn = new STColumn<T>(column, className);
            this.dataFrame.put(column, newColumn);
//            this.columnOrder.addLast(column);
            this.numColumns = this.dataFrame.size();
            if (this.indexColumn == null){
                this.setIndexColumn(column);
            }
        }

        return newColumn; // return the new for chaining
    }

    public STColumn addColumn(String column){
        return addColumn(column, String.class);
    }

    public synchronized STColumn addPrimitiveColumn(String column, DTYPE dtype){
        STColumn newColumn = null;

        if(dtype == DTYPE.integer){
//            sColumn = new SIntColumn(column); //TODO
            newColumn = new STColumn<Class<Integer>>(column, Integer.class);
        }else if (dtype == DTYPE.dbl){
//            sColumn = new SDnlColumn(column); //TODO
            newColumn = new STColumn<Class<Double>>(column, Double.class);
        }else{
            throw new RuntimeException("dtype not implemented: " +dtype);
        }

//        columns.put(length(), column);
        this.dataFrame.put(column, newColumn);
//        this.columnOrder.addLast(column);
        this.numColumns = this.dataFrame.size();
        if (this.indexColumn == null){
            this.setIndexColumn(column);
        }

        return newColumn;
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

    public STColumn getColumn(String column){
        if (this.dataFrame.containsKey(column)){
            return this.dataFrame.get(column);
        }else{
            throw new RuntimeException("Column not found in the dataframe: " + column);
        }
    }

    public synchronized <T> void add(String column, T value){

        if(trimColumnNames){
            column = column.trim();
        }

        if (this.dataFrame.containsKey(column)){
            STColumn stColumn = this.dataFrame.get(column);

            // add null rows to align this column with the current row of the index column
            while (stColumn.columnData.size() < this.currentRow){
                stColumn.addNaN();
            }
            stColumn.add(value);

            if (numColumnsInCurrentRow < numColumns){
                numColumnsInCurrentRow++;
//                this.numRows = this.currentRow + 1;
            }else {
                this.numColumnsInCurrentRow = 0;
                this.currentRow++;
//                this.numRows = this.currentRow;
            }

        }
    }

    // synchronized -- skip filled columns, dont fill next row in this call, ignore extra values
    public void add(Object[] values){
        int numColumnsToSkip;
        int i = 0, j = 0;

        if (numColumnsInCurrentRow == numColumns){
            numColumnsToSkip = 0;
        }else {
            numColumnsToSkip = numColumnsInCurrentRow;
        }

        for (Map.Entry<String, STColumn> entry : dataFrame.entrySet()) {

            if (j < numColumnsToSkip){
                j++;
                continue;
            }

            if (i < values.length && i < numColumns){
                add(entry.getKey(), values[i]);
            }else{
                return;
            }

            i++;
        }
    }

    public String[] columns(){
        return this.dataFrame.keySet().toArray(new String[numColumns]);
    }

    public <T> T[] index(){
        if (indexColumn != null){
            return (T[]) this.dataFrame.get(indexColumn).columnData.toArray();
        }else{
            throw new RuntimeException("Index column is not set");
        }
    }

    public <T> T get(int row, String column){
        return (T) this.dataFrame.get(column).get(row);
    }

    public <T> T iget(int i, int column){
        int size = this.dataFrame.get(column).size();

        if (numColumnsInCurrentRow == 0 && currentRow == 0){
            //empty list
            return null;
        }else if (0 < i && i < size){
            // column has data
            return (T) this.dataFrame.get(column).get(i);
        }else{
            // data is Nan for this column
            return (T) io.strNaN;
        }
    }

    // https://stackoverflow.com/questions/10451842/how-to-escape-comma-and-double-quote-at-same-time-for-csv-file

    public String printRow(int i){
        StringBuilder sb = new StringBuilder();
        STColumn column;
        int size;
        String escaped;
        for (Map.Entry<String, STColumn> entry : dataFrame.entrySet()) {
            column = entry.getValue();
            size = column.size();

            //check index 0 vs emoty list
            if (i == NONE){
                // print header
                escaped = StringEscapeUtils.escapeCsv(column.name);
                sb.append(escaped);
                sb.append(io.sep);
            }else if (numColumnsInCurrentRow == 0 && currentRow == 0){
                //empty list
                break;
            }else if (0 <= i && i < size){
                // column has data
                Object value = column.get(i);
                if (value == null){
                    sb.append(io.strNaN);
                }else{
                    escaped = StringEscapeUtils.escapeCsv(value.toString());
                    sb.append(escaped);
                }

                sb.append(io.sep);
            }else{
                // data is Nan for this column
                sb.append(io.strNaN);
                sb.append(io.sep);
            }
        }
        //delete last separator
        sb.deleteCharAt(sb.lastIndexOf(io.sep));
        return sb.toString();
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        // header
        sb.append(printRow(SDataFrame.NONE));
        sb.append("\n");

        for (int i = 0; i <= currentRow; i++) {
            sb.append(printRow(i));
            sb.append("\n");
        }
        return sb.toString();
    }

    public void toCsv(String fileName) throws IOException {
        SDataFrameIO.toCsv(this, fileName, io.appendMode);
    }

    public static SDataFrame readCsv(String fileName) throws IOException {
        SDataFrame df = new SDataFrame();
        df.io.readCsv(fileName);
        return df;
    }


    public static void main(String[] args) throws IOException {
        String path = "E:\\git\\dotnet54\\TS-CHIEF-DEV\\out\\knn\\dev\\train\\BasicMotions//";
        String file = "BasicMotions-erp-d-p_0_100_1.train.exp.csv";

        SDataFrame table = new SDataFrame();

        // add columns
        assert table.length() == 0;
        assert table.size() == 0;
        table.addColumn("dataset", String.class);
        assert table.length() == 1;
        assert table.size() == 0;

        assert table.getIndexColumn().name.equals("dataset");
        table.addPrimitiveColumn("trainSize", DTYPE.integer);
        table.addPrimitiveColumn("testSize", DTYPE.integer);
        table.addPrimitiveColumn("accuracy", DTYPE.dbl);
        table.addColumn("normalized", Boolean.class);
        table.addColumn("comments", DTYPE.str);
        table.addColumns(new String[]{"host, name", "paramID"});
        table.addColumns("args, trainTime, drop this column");
        assert table.length() == 11;

        //add some data
        assert table.size() == 0;
        assert table.currentRow == 0;
        assert table.numColumnsInCurrentRow == 0;
        table.add("trainSize", 10);
        assert table.size() == 1;
        assert table.currentRow == 0;
        assert table.numColumnsInCurrentRow == 1;
        table.add("dataset", "BasicMotions");   //index column
        assert table.size() == 1;
        assert table.currentRow == 0;
        assert table.numColumnsInCurrentRow == 2;
        table.add("testSize", 10);
        table.add("accuracy", 0.8);
        table.add("normalized", false);
        table.add("comments", "\"quote's row 0\"");
        table.add(new Object[] {"-1", -2,-3,-4,-5});
        assert table.size() == 1;
        assert table.currentRow == 0;
        assert table.numColumnsInCurrentRow == 11;

        table.add(new Object[]{"LSST", 20,20,0.5, true});
        assert table.size() == 2;
        table.add("trainSize", 50);
        assert table.size() == 2;
        assert table.getColumn("trainSize").size() == 3;
        table.setIndexColumn("trainSize");
        assert table.size() == 3;

        String d = table.get(0, "dataset");
        assert d.equals("BasicMotions");
        Integer ts = table.get(0, "trainSize");
        assert ts == 10;
        assert ts.getClass() == Integer.class;
        assert table.get(0, "accuracy").getClass() == Double.class;
        assert table.get(1, "normalized").getClass() == Boolean.class;
        System.out.println(table.printRow(SDataFrame.NONE));
        System.out.println(table.printRow(0));
        System.out.println(table.printRow(1));

        table.dropColumn("drop this column");
        assert table.length() == 10;

        table.resetIndexColumn("index");
        assert table.getIndexColumn().name.equals("index");


        table.add("index", table.size() + 1);
        table.add("dataset", "Fish");
        table.add("test", "this column is not in frame");
        table.add("comments", "Escaped\" String r3");
        table.add("trainTime", 12345);
        assert table.size() == 3;
        table.add("trainTime", 12346);
        assert table.size() == 4;
        table.add("comments", "eg, comma");
        table.add("args", "{eg, comma, \"e:1,b:100;s5\", [3,4,5]}");
        assert table.length() == 11;
//        assert table.getColumn("dataset").get(2) == null;

        System.out.println(table);

        table.toCsv("out/dev/tmp.csv");

        table.io.open("out/dev/tmp2.csv", false);
        table.io.writeln(-1);
        table.io.writeln(0);
        table.io.writeAll();
        table.io.close();

        SDataFrame table2 = SDataFrame.readCsv("out/dev/tmp2.csv");

        System.out.println(table2);
    }


}
