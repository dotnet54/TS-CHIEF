package util.math.doubles;

public class Vectors {
    public static final double threshold = 1e-10;

    public static double l1Norm(double[] vector) {
        double total = 0;
        for (double element : vector) {
            total += (element <= 0.0D) ? 0.0D - element : element;  //Math.abs()
        }
        return total;
    }

    public static double l2Norm(double[] vector) {
        double total = 0;
        for (double element : vector) {
            total += element * element;
        }
        return Math.sqrt(total);
    }

    public static double l2NormSquared(double[] vector) {
        double total = 0;
        for (double element : vector) {
            total += element * element;
        }
        return total;
    }

    public static double lpNorm(double[] vector, int p) {
        double total = 0;
        for (double element : vector) {
            total += Math.pow(element, p);
        }
        return  Math.pow(total, (double) 1/p);
    }

    public static double l1Distance(double[] vector1, double[] vector2) {
        double total = 0;
        int minLength = Math.min(vector1.length, vector2.length);
        double diff;
        for (int i = 0; i < minLength; i++) {
            diff = vector1[i] - vector2[i];
            total += (diff <= 0.0D) ? 0.0D - diff : diff;  //Math.abs()
        }
        return total;
    }

    public static double l2Distance(double[] vector1, double[] vector2) {
        double total = 0;
        int minLength = Math.min(vector1.length, vector2.length);
        double diff;
        for (int i = 0; i < minLength; i++) {
            diff = vector1[i] - vector2[i];
            total +=  diff * diff;
        }
        return Math.sqrt(total);
    }

    public static double l2DistanceSquared(double[] vector1, double[] vector2) {
        double total = 0;
        int minLength = Math.min(vector1.length, vector2.length);
        double diff;
        for (int i = 0; i < minLength; i++) {
            diff = vector1[i] - vector1[i];
            total += (diff <= 0.0D) ? 0.0D - diff : diff;  //Math.abs()
        }
        return total;
    }

    public static double lpDistance(double[] vector1, double[] vector2, int p) {
        double total = 0;
        int minLength = Math.min(vector1.length, vector2.length);
        double pow;
        for (int i = 0; i < minLength; i++) {
            pow =  Math.pow(vector1[i] - vector2[i], p);
            total += (pow <= 0.0D) ? 0.0D - pow : pow;  //Math.abs()
        }
        return Math.pow(total,  1.0D /p);
    }

//    //TODO CHECK
//    public static int l1Similarity(double[] vector1, double[] vector2, double epsilon) {
//        double total = 0;
//        int minLength = Math.min(vector1.length, vector2.length);
//        double diff;
//        for (int i = 0; i < minLength; i++) {
//            diff = vector1[i] - vector2[i];
//            total += (diff <= 0.0D) ? 0.0D - diff : diff;  //Math.abs()
//        }
//        return total <= minLength * epsilon ? 1: 0;
//    }

//    //TODO CHECK
//    public static int l2Similarity(double[] vector1, double[] vector2, double epsilon) {
//        double total = 0;
//        int minLength = Math.min(vector1.length, vector2.length);
//        double diff;
//        for (int i = 0; i < minLength; i++) {
//            diff = vector1[i] - vector2[i];
//            total +=  diff * diff;
//        }
//        // if epsilon < 1?
//        return total <= minLength * epsilon * epsilon ? 1: 0;
//    }

//    //TODO CHECK
//    public static int lpSimilarity(double[] vector1, double[] vector2, int p, double epsilon) {
//        double total = 0;
//        int minLength = Math.min(vector1.length, vector2.length);
//        double pow;
//        for (int i = 0; i < minLength; i++) {
//            pow =  Math.pow(vector1[i] - vector2[i], p);
//            total += (pow <= 0.0D) ? 0.0D - pow : pow;  //Math.abs()
//        }
//        return Math.pow(total,  1.0D /p);
//    }

    public static double dot(double[] vector1, double[] vector2) {
        double total = 0;
        int minLength = Math.min(vector1.length, vector2.length);
        for (int i = 0; i < minLength; i++) {
            total +=  vector1[i] * vector2[i];
        }
        return total;
    }

    public static double[] add(double[] vector1, double[] vector2) {
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] =  vector1[i] + vector2[i];
        }
        return result;
    }

    public static double[] subtract(double[] vector1, double[] vector2) {
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] =  vector1[i] - vector2[i];
        }
        return result;
    }

    public static double[] div(double[] vector1, double[] vector2) {
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] =  vector1[i] / vector2[i];
        }
        return result;
    }

    public static double[] mul(double[] vector1, double[] vector2) {
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] =  vector1[i] * vector2[i];
        }
        return result;
    }

    public static double[] add(double[] vector1, double scalar) {
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] =  vector1[i] + scalar;
        }
        return result;
    }

    public static double[] subtract(double[] vector1, double scalar) {
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] =  vector1[i] - scalar;
        }
        return result;
    }

    public static double[] div(double[] vector1, double scalar) {
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] =  vector1[i] / scalar;
        }
        return result;
    }

    public static double[] mul(double[] vector1, double scalar) {
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] =  vector1[i] * scalar;
        }
        return result;
    }

    public static double[] addInPlace(double[] vector1, double[] vector2) {
        for (int i = 0; i < vector1.length; i++) {
            vector1[i] =  vector1[i] + vector2[i];
        }
        return vector1;
    }

    public static double[] subtractInPlace(double[] vector1, double[] vector2) {
        for (int i = 0; i < vector1.length; i++) {
            vector1[i] =  vector1[i] - vector2[i];
        }
        return vector1;
    }

    public static double[] divInPlace(double[] vector1, double[] vector2) {
        for (int i = 0; i < vector1.length; i++) {
            vector1[i] =  vector1[i] / vector2[i];
        }
        return vector1;
    }

    public static double[] mulInPlace(double[] vector1, double[] vector2) {
        for (int i = 0; i < vector1.length; i++) {
            vector1[i] =  vector1[i] * vector2[i];
        }
        return vector1;
    }

    public static double[] addInPlace(double[] vector1, double scalar) {
        for (int i = 0; i < vector1.length; i++) {
            vector1[i] =  vector1[i] + scalar;
        }
        return vector1;
    }

    public static double[] subtractInPlace(double[] vector1, double scalar) {
        for (int i = 0; i < vector1.length; i++) {
            vector1[i] =  vector1[i] - scalar;
        }
        return vector1;
    }

    public static double[] divInPlace(double[] vector1, double scalar) {
        for (int i = 0; i < vector1.length; i++) {
            vector1[i] =  vector1[i] / scalar;
        }
        return vector1;
    }

    public static double[] mulInPlace(double[] vector1, double scalar) {
        for (int i = 0; i < vector1.length; i++) {
            vector1[i] =  vector1[i] * scalar;
        }
        return vector1;
    }
}
