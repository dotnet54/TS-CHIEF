package core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Options {

	private String name = "";
	private HashMap<String, Object> options;
	
	private ArrayList<String> required = new ArrayList<>();
	
	public Options() {
		this.options = new HashMap<>();
	}
	
	public Options(String name) {
		this.options = new HashMap<>();
		this.name = name;
	}
	
	public Options set(String key, Object obj) {
		options.put(key, obj);
		return this;
	}
	
	public <T> Options set(String key, T obj, Class<T> cls) {
		options.put(key, obj);
		return this;
	}	
	
	public Options set(String key, boolean blnValue) {
		options.put(key, blnValue);
		return this;
	}
	
	public Options set(String key, int intValue) {
		options.put(key, intValue);
		return this;
	}
	
	public Options set(String key, double dblValue) {
		options.put(key, dblValue);
		return this;
	}
	
	public Options set(String key, String str) {
		options.put(key, str);
		return this;
	}
	
	public Object get(String key) {
		return options.get(key);
	}
	
	public <T> T get(String key, Class<T> cls) {
		Object obj = options.get(key);
		
		if (cls.isInstance(obj)) {
			return cls.cast(options.get(key));
		}else {
			throw new ClassCastException();
		}
	}			
	
	public boolean getBoolean(String key) {
		Object obj = options.get(key);

		if (obj instanceof String) {
			return Boolean.parseBoolean((String)obj);
		}else {
			return (boolean) options.get(key);
		}
	}

	public int getInteger(String key) {
		Object obj = options.get(key);

		if (obj instanceof String) {
			return Integer.parseInt((String)obj);
		}else {
			return (int) options.get(key);
		}	
	}
	
	public double getDouble(String key) {
		Object obj = options.get(key);		
		
		if (obj instanceof String) {
			return Double.parseDouble((String)obj);
		}else {
			return (double) options.get(key);
		}	
	}
	
	public String getString(String key) {
		return (String) options.get(key);
	}	
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {
		return this.name + ":" + options.toString();
	}
	
	public int size() {
		return options.size();
	}
	
	public String[] keys() {
		return options.keySet().toArray(new String[options.size()]);
	}
	
	public boolean hasAllRequired() {
		return options.keySet().containsAll(this.required);
	}
	
	public boolean isValid() {
		return hasAllRequired();	//may add more conditions for validity in future, e.g validator callback is satisfied
	}
	
	public static void main(String[] args) {
		
		Options opt1 = new Options("boss");
		Options opt2 = new Options("st");
		
		opt2.set("string-option", "test string");
		
		List<Double> list = new ArrayList<Double>();
		list.add(new Double(5.5));
		opt2.set("list-option", list);
		
		opt1.set("object-option", opt2);
		
		Options opt3 = (Options) opt1.get("object-option");
		
		System.out.println(opt1);
		System.out.println(opt2);
		System.out.println(opt3);
		System.out.println("---");

		//testing casting

		System.out.println(opt3.get("string-option"));
		System.out.println(opt3.get("list-option", ArrayList.class).get(0).getClass());	
		System.out.println(Arrays.toString(opt3.keys()));
		
		//required
		opt1.required.add("st");
		System.out.println(opt1.hasAllRequired());
		opt1.set("st", 78);
		System.out.println(opt1.isValid());

	}
}
