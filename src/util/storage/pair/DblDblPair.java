package util.storage.pair;

public class DblDblPair// implements Comparable<IntDblPair>
{
	public final double EPSILON = 1e9;
	public double key;
	public double value;
	
	
	public DblDblPair(double key, double value) {
		this.key = key;
		this.value = value;
	}

	public String toString() {
		return key + " => " + value;
	}

//	//TODO use a threshold for double comparison
//	@Override
//	public int compareTo(IntDblPair otherPair) {
//		if (key < otherPair.key) {
//			return -1;
//		}else if (key > otherPair.key) {
//			return 1;
//		}else {
//			return 0;
//		}
//	}
}
