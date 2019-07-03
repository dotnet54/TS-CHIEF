package util.pair;

public class IntDblPair implements Comparable<IntDblPair>{
	public int key;
	public double value;
	
	public IntDblPair(int key, double value) {
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
