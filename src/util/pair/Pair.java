package util.pair;

import java.io.Serializable;

public class Pair<K, V> implements Serializable//,Comparable<Pair<K,V>>
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5518988385774113204L;
	protected K key;
	protected V value;
	
	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public void setKey(K key) {
		this.key = key;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}
	
	public String toString() {
		return key.toString() + ":" + value.toString();
	}

//	@Override
//	public int compareTo(Pair otherPair) {
//		
//		if (key < otherPair.getKey()) {
//			return -1;
//		}
//		
//		return 0;
//	}
	
}
