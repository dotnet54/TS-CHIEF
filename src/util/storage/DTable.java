package util.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

//NOTE: temporary table data structure used to export results to csv files 
// TODO improve support for different data types/ use generics,etc..
public class DTable {

	/**
	 * Column class
	 *
	 * @param <T> data type of the column
	 */
	public class Column<T>{
		protected Class<T> dtype;
		protected String name;
		protected List<T> data;
		
		public Column(String name, Class<T> dtype) {
			this.name = name;
			this.dtype = dtype;
			this.data = new ArrayList<T>();
		}
		
		public void add(T item) {
			this.data.add(item);
		}
		
		public T get(int index) {
			return this.data.get(index);
		}
		
		public int rows() {
			return data.size();
		}
		
//		@SuppressWarnings("unchecked")
//		public T[] values() {
//			return (T[]) new Object[data.size()];
//		}
		
		public String toString() {
			return data.toString();
		}
		
	}	
	
	/**
	 * Table class
	 */
	
	protected int rows;
	protected int columns;
	
	protected Map<String, Column> table;
	
	public DTable(int estimated_rows) {
		this.table = new HashMap<String, Column>(estimated_rows);
	}
	
	public <T> void addColumn(String name, Class<T> dtype) {
		table.put(name, new Column(name, dtype));
	}

	
	public void add(int row, String column, String data) {
		
		if (! table.containsKey(column)) {
			addColumn(column, data.getClass());
		}
		
		table.get(column).data.add(row, data);
	}	
	
	public void add(int row, String column, Boolean data) {
		
		if (! table.containsKey(column)) {
			addColumn(column, data.getClass());
		}
		
		table.get(column).data.add(row, data);
	}	
	
	public void add(int row, String column, Integer data) {
		
		if (! table.containsKey(column)) {
			addColumn(column, data.getClass());
		}
		
		table.get(column).data.add(row, data);
	}	
	
	public void add(int row, String column, Double data) {
		
		if (! table.containsKey(column)) {
			addColumn(column, data.getClass());
		}
		
		table.get(column).data.add(row, data);
	}	
	
	
//	public String toString() {
//		
//		for (Entry<String, Column> row : table.entrySet()) {
//			
//		}
//	}

	
	public static void main(String[] args) throws IOException {
//
//		Table table = new Table(5);
//		
//		table.add(0, "A", 100);
//		table.add(1, "A", "apple");

		
		double[] numbers = {1, 2, 3, 4};
		DoubleColumn nc = DoubleColumn.create("nc", numbers);
		System.out.println(nc.print());
		
		
		String[] animals = {"bear", "cat", "giraffe"};
		double[] cuteness = {90.1, 84.3, 99.7};

		Table cuteAnimals =
		    Table.create("Cute Animals")
		        .addColumns(
		            StringColumn.create("Animal types", animals),
		            DoubleColumn.create("rating", cuteness));
		
//		cuteAnimals.column("rating").set(0, 8.5);
		cuteAnimals.doubleColumn("rating").set(0, 8);
		
		System.out.println(cuteAnimals.structure());

		System.out.println(cuteAnimals);
		
		cuteAnimals.write().csv("output/tablesaw.csv");

	}
	
}
