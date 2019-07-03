package trees.splitters;

public class SplitCriterion {
	
	public int attribute = -1; //-1 = unknown attribute, may store index of an item from a list (eg. index of a shapelet), or index of column from 2d array
	public double threshold;
	public double wgini;
	
	public SplitCriterion(double t, double wg) {
		this.threshold = t;
		this.wgini = wg;
	}
	
	public String toString() {
		return this.attribute + ":" + this.threshold + " = " + this.wgini;
	}
}
