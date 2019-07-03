package util;

public class Compare {

	//floating point comparison threshold;
	public static double FP_THRESHOLD = 1e-6;
	
	public static boolean eq(double a, double b) {
		if (Math.abs(a-b) < FP_THRESHOLD) {
			return true;
		}else {
			return false;
		}
	}
	
//	public static boolean lt(double a, double b) {
//		if ((a-b) < FP_THRESHOLD) {
//			return true;
//		}else {
//			return false;
//		}
//	}
//	
//	public static boolean gt(double a, double b) {
//		if ((a-b) < FP_THRESHOLD) {
//			return true;
//		}else {
//			return false;
//		}
//	}	

//	public static boolean lte(double a, double b) {
//		if (lt(a,b) || eq(a,b)) {
//			return true;
//		}else {
//			return false;
//		}
//	}
//	
//	public static boolean gte(double a, double b) {
//		if (gt(a,b) || eq(a,b)) {
//			return true;
//		}else {
//			return false;
//		}
//	}	
	
	public static void main(String[] args) {
		//tests;
		
		
		
	}
}
