package development.mts.measures;

import java.util.Random;
import data.containers.TreeStatCollector;
import data.timeseries.UTSDataset;
import trees.TSCheifTree;
 
public class DistanceMeasure {
	
	public final MEASURE distance_measure;

	private transient Euclidean euc;
	private transient DTW dtw;
	private transient DTW dtwcv;	
	private transient DDTW ddtw;
	private transient DDTW ddtwcv;	
	private transient WDTW wdtw;
	private transient WDDTW wddtw;
	private transient LCSS lcss;
	private transient MSM msm;
	private transient ERP erp;
	private transient TWE twe;
	
	public int windowSizeDTW =-1,
			windowSizeDDTW=-1, 
			windowSizeLCSS=-1,
			windowSizeERP=-1;
	public double epsilonLCSS = -1.0,
			gERP=-1.0,
			nuTWE,
			lambdaTWE,
			cMSM,
			weightWDTW,
			weightWDDTW;
	
	private transient TSCheifTree.Node node;

	public DistanceMeasure (MEASURE m, TSCheifTree.Node node) throws Exception{
		this.distance_measure = m;
		initialize(m);
		this.node = node; //TODO mem usage?? need to collect stats
	}
	
	public void initialize (MEASURE m) throws Exception{
		switch (m) {
			case euclidean:
			case shifazEUCLIDEAN:
				euc = new Euclidean();
				break;
			case erp:
			case shifazERP:
				erp = new ERP();
				break;
			case lcss:
			case shifazLCSS:
				lcss = new LCSS();
				break;
			case msm:
			case shifazMSM:
				msm = new MSM();
				break;
			case twe:
			case shifazTWE:
				twe = new TWE();
				break;
			case wdtw:
			case shifazWDTW:
				wdtw = new WDTW();
				break;
			case wddtw:
			case shifazWDDTW:
				wddtw = new WDDTW();
				break;
			case dtw:
			case shifazDTW:
				dtw = new DTW();
				break;
			case dtwcv:
			case shifazDTWCV:
				dtwcv = new DTW();
				break;
			case ddtw:
			case shifazDDTW:
				ddtw  = new DDTW();
				break;
			case ddtwcv:
			case shifazDDTWCV:
				ddtwcv = new DDTW();
				break;
			default:
				throw new Exception("Unknown distance measure");
//				break;
		}
		
	}
	public void select_random_params(UTSDataset d, Random r) {
		switch (this.distance_measure) {
		case euclidean:
		case shifazEUCLIDEAN:

			break;
		case erp:
		case shifazERP:
			this.gERP = erp.get_random_g(d, r);
			this.windowSizeERP =  erp.get_random_window(d, r);
			break;
		case lcss:
		case shifazLCSS:
			this.epsilonLCSS = lcss.get_random_epsilon(d, r);
			this.windowSizeLCSS = lcss.get_random_window(d, r);
			break;
		case msm:
		case shifazMSM:
			this.cMSM = msm.get_random_cost(d, r);
			break;
		case twe:
		case shifazTWE:
			this.lambdaTWE = twe.get_random_lambda(d, r);
			this.nuTWE = twe.get_random_nu(d, r);
			break;
		case wdtw:
		case shifazWDTW:
			this.weightWDTW = wdtw.get_random_g(d, r);
			break;
		case wddtw:
		case shifazWDDTW:
			this.weightWDDTW = wddtw.get_random_g(d, r);
			break;
		case dtw:
		case shifazDTW:
			this.windowSizeDTW = d.length();	
			break;
		case dtwcv:
		case shifazDTWCV:
			this.windowSizeDTW = dtwcv.get_random_window(d, r);
			break;
		case ddtw:
		case shifazDDTW:
			this.windowSizeDDTW = d.length();	
			break;
		case ddtwcv:
		case shifazDDTWCV:
			this.windowSizeDDTW = ddtwcv.get_random_window(d, r);
			break;
		default:
//			throw new Exception("Unknown distance measure");
//			break;
		}
	}

	public double distance(double[] s, double[] t){
		return this.distance(s, t, Double.POSITIVE_INFINITY);
	}
	
	public double distance(double[] s, double[] t, double bsf){
		double distance = Double.POSITIVE_INFINITY;
		
		//TODO only if statistics is enabled
		TreeStatCollector stats =  this.node.tree.stats;
		long start = System.nanoTime();
		
		switch (this.distance_measure) {
		case euclidean:
		case shifazEUCLIDEAN:
			stats.euc_count++;
			distance = euc.distance(s, t, bsf);
			stats.euc_time += (System.nanoTime() - start);
			break;
		case erp:
		case shifazERP:
			stats.erp_count++;
			distance = 	erp.distance(s, t, bsf, this.windowSizeERP, this.gERP);
			stats.erp_time += (System.nanoTime() - start);
			break;
		case lcss:
		case shifazLCSS:
			stats.lcss_count++;
			distance = lcss.distance(s, t, bsf, this.windowSizeLCSS, this.epsilonLCSS);
			stats.lcss_time += (System.nanoTime() - start);
			break;
		case msm:
		case shifazMSM:
			stats.msm_count++;
			distance = msm.distance(s, t, bsf, this.cMSM);
			stats.msm_time += (System.nanoTime() - start);
			break;
		case twe:
		case shifazTWE:
			stats.twe_count++;
			distance = twe.distance(s, t, bsf, this.nuTWE, this.lambdaTWE);
			stats.twe_time += (System.nanoTime() - start);
			break;
		case wdtw:
		case shifazWDTW:
			stats.wdtw_count++;
			distance = wdtw.distance(s, t, bsf, this.weightWDTW);
			stats.wdtw_time += (System.nanoTime() - start);
			break;
		case wddtw:
		case shifazWDDTW:
			stats.wddtw_count++;
			distance = wddtw.distance(s, t, bsf, this.weightWDDTW);
			stats.wddtw_time += (System.nanoTime() - start);
			break;
		case dtw:
		case shifazDTW:
			stats.dtw_count++;
			distance = dtw.distance(s, t, bsf, s.length);
			stats.dtw_time += (System.nanoTime() - start);
			break;
		case dtwcv:
		case shifazDTWCV:
			stats.dtwr_count++;
			distance = 	dtwcv.distance(s, t, bsf, this.windowSizeDTW);
			stats.dtwr_time += (System.nanoTime() - start);
			break;
		case ddtw:
		case shifazDDTW:
			stats.ddtw_count++;
			distance = ddtw.distance(s, t, bsf, s.length);
			stats.ddtw_time += (System.nanoTime() - start);
			break;
		case ddtwcv:
		case shifazDDTWCV:
			stats.ddtwr_count++;
			distance = ddtwcv.distance(s, t, bsf, this.windowSizeDDTW);
			stats.ddtwr_time += (System.nanoTime() - start);
			break;
		default:
//			throw new Exception("Unknown distance measure");
//			break;
		}
		if (distance == Double.POSITIVE_INFINITY) {
			System.out.println("error ***********"); //TODO throw exception
		}
		
		return distance;
	}
	
//	public double distance(int q, int c, double bsf, DMResult result){
////		return dm.distance(s, t, bsf, result);
//		return 0.0;
//	}	
	
	public String toString() {
		return this.distance_measure.toString(); //+ " [" + dm.toString() + "]";
	}
	
	//setters and getters
	
//	public void set_param(String key, Object val) {
//		this.dm.set_param(key, val);
//	}
//	
//	public Object get_param(String key) {
//		return this.dm.get_param(key);
//	}
	
	public void setWindowSizeDTW(int w){
		this.windowSizeDTW = w;
	}
	
	public void setWindowSizeDDTW(int w){
		this.windowSizeDDTW = w;
	}
	
	public void setWindowSizeLCSS(int w){
		this.windowSizeLCSS = w;
	}
	
	public void setWindowSizeERP(int w){
		this.windowSizeERP = w;
	}
	
	public void setEpsilonLCSS(double epsilon){
		this.epsilonLCSS = epsilon;
	}
	
	public void setGvalERP(double g){
		this.gERP= g;
	}
	
	public void setNuTWE(double nuTWE){
		this.nuTWE = nuTWE;
	}
	public void setLambdaTWE(double lambdaTWE){
		this.lambdaTWE = lambdaTWE;
	}
	public void setCMSM(double c){
		this.cMSM = c;
	}
	
	public void setWeigthWDTW(double g){
		this.weightWDTW = g;
	}
	
	public void setWeigthWDDTW(double g){
		this.weightWDDTW = g;
	}

}
