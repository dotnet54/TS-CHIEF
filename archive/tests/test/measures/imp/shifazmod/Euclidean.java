package test.measures.imp.shifazmod;

public class Euclidean {
	public Euclidean() {

	}	
	
	public synchronized double distance(double[] s, double[] t){
		
		double bsf = Double.POSITIVE_INFINITY;
		
		return distance(s, t ,bsf);
	}
	
	//default bsf must be DOUBLE.POSITIVE_INFINITY
	public synchronized double distance(double[] s, double[] t, double bsf){
		int i = 0;
		double total = 0;
		double diff = 0;
		int minLen = Math.min(s.length, t.length);

		
		//note: using total <= bsf instead of total < bsf : handles the case when total = 0 (initially always 0) and bsf = 0
		
		for (i = 0; i < minLen & total <= bsf; i++){
			diff = s[i] - t[i];
			total += diff * diff;
		}
		
		return total;		// for optimization:	return Math.sqrt(total);
	}
}
