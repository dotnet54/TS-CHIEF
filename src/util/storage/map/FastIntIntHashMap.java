package util.storage.map;

import java.util.Arrays;

//these maps are fast but size must be predefined since they are implemented using two primitive arrays.
//resizing is expensive as new arrays must be created and old data needs to be coppied - so use this in cases where we know size doesnt change

public class FastIntIntHashMap {
	
	private int keys[];
	private int values[];
	private int indices[];
	
	public FastIntIntHashMap(int size) throws Exception {
		
		resize(size);
	}

	public void resize(int size) throws Exception {
		
		if (this.keys == null) {
			this.keys = new int[size];
			this.values = new int[size];	
			this.indices = new int[size];
		}else {
			// copy existing data -- if new size is smaller raise an exception, else copy data
			
			if (size < this.keys.length) {
				throw new Exception("Cannot resize Hashmap as new size is smaller than existing size");
			}
			
			this.keys = Arrays.copyOfRange(keys, 0, size);	//TODO check if its ok to modify coppying array
			this.values = Arrays.copyOfRange(values, 0, size);
		}
	}
	
	public int size() {
		return this.keys.length;
	}
	
	public int[] keys() {
		return this.keys;
	}
	
	public int[] values() {
		return this.values;
	}
	
	public FastIntIntHashMap clone() throws CloneNotSupportedException{
		
		FastIntIntHashMap c;
		try {
			c = new FastIntIntHashMap(this.keys.length);
		} catch (Exception e) {
			throw new CloneNotSupportedException();
		}
		
		for (int i = 0; i < keys.length; i++) {
			int[] v = c.values();
			v[i] = this.values[i];
			int[] k = c.keys();
			k[i] = this.keys[i];
		}
		return c;
	}
	
	public void put(int k, int v) {
		
	}
	
//	public int get(int k) {
//		return this.keys
//	}
	
	
}
