package util.pair;

public class DblIntPair{

	public double key;
	public int value;
	
	public DblIntPair(double key, int value) {
		this.key = key;
		this.value = value;
	}

//	@Override
//	public int compareTo(K o) {
//		return key.compareTo(o);
//	}

	public String toString() {
		return key + " => " + value;
	}
}