package util.storage.pair;

public class IntIntPair implements Comparable<IntDblPair>{
	public int key;
	public int value;
	
	public IntIntPair(int key, int value) {
		this.key = key;
		this.value = value;
	}

	public String toString() {
		return key + " => " + value;
	}

	@Override
	public int compareTo(IntDblPair otherPair) {
		if (key < otherPair.key) {
			return -1;
		}else if (key > otherPair.key) {
			return 1;
		}else {
			return 0;
		}
	} 

}
