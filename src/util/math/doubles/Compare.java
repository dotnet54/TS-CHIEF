package util.math.doubles;

public class Compare {

    public static final double threshold = 1e-10;

    public static double min(double a, double b){
        return (a <= b) ? a : b;
    }

//    // if equal return the diagonal element (a)
//    public static double min(double a, double b, double c){
//        return (a <= b) ? a : b;
//    }

}
