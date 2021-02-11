package util.math.doubles;

public class VectorsWithFilter {

    public static final double threshold = 1e-10;

    public static double l1Norm(double[] vector, int[] indices) {
        double total = 0;
        double element;
        for (int i : indices) {
            element = vector[i];
            total += (element <= 0.0D) ? 0.0D - element : element;  //Math.abs()
        }
        return total;
    }

    public static double l1Norm(double[][] matrix, int[] dimensions) {
        double total = 0;
        double element;
        for (int i = 0; i < matrix[0].length; i++) {
            for (int dimension : dimensions) {
                element = matrix[dimension][i];
                total += (element <= 0.0D) ? 0.0D - element : element;  //Math.abs()
            }
        }
        return total;
    }

    public static double l2Norm(double[] vector, int[] indices) {
        double total = 0;
        double element;
        for (int i : indices) {
            element = vector[i];
            total += element * element;
        }
        return Math.sqrt(total);
    }

    public static double l2Norm(double[][] matrix, int[] dimensions) {
        double total = 0;
        double element;
        for (int i = 0; i < matrix[0].length; i++) {
            for (int dimension : dimensions) {
                element = matrix[dimension][i];
                total += element * element;
            }
        }
        return Math.sqrt(total);
    }

    public static double l2NormSquared(double[] vector, int[] indices) {
        double total = 0;
        double element;
        for (int i : indices) {
            element = vector[i];
            total += element * element;
        }
        return total;
    }

    public static double l2NormSquared(double[][] matrix, int[] dimensions) {
        double total = 0;
        double element;
        for (int i = 0; i < matrix[0].length; i++) {
            for (int dimension : dimensions) {
                element = matrix[dimension][i];
                total += element * element;
            }
        }
        return total;
    }

    public static double lpNorm(double[] vector, int p, int[] indices) {
        double total = 0;
        double element;
        for (int i : indices) {
            element = vector[i];
            total += Math.pow(element, p);
        }
        return  Math.pow(total, (double) 1/p);
    }

    public static double lpNorm(double[][] matrix, int p, int[] dimensions) {
        double total = 0;
        double element;
        for (int i = 0; i < matrix[0].length; i++) {
            for (int dimension : dimensions) {
                element = matrix[dimension][i];
                total += Math.pow(element, p);
            }
        }
        return  Math.pow(total, (double) 1/p);
    }

    public static double l1Distance(double[] vector1, double[] vector2, int[] indices) {
        double total = 0;
        double diff;
        for (int i = 0; i < indices.length; i++) {
            diff = vector1[indices[i]] - vector2[indices[i]];
            total += (diff <= 0.0D) ? 0.0D - diff : diff;  //Math.abs()
        }
        return total;
    }

    public static double l1Distance(double[][] matrix1, double[][] matrix2, int[] dimensions) {
        double total = 0;
        double diff;
        for (int i = 0; i < matrix1[0].length; i++) {
            for (int dimension : dimensions) {
                diff = matrix1[dimension][i] - matrix2[dimension][i];
                total += (diff <= 0.0D) ? 0.0D - diff : diff;  //Math.abs()
            }
        }
        return total;
    }

    public static double l2Distance(double[] vector1, double[] vector2, int[] indices) {
        double total = 0;
        double diff;
        for (int i = 0; i < indices.length; i++) {
            diff = vector1[indices[i]] - vector2[indices[i]];
            total +=  diff * diff;
        }
        return Math.sqrt(total);
    }

    public static double l2Distance(double[][] matrix1, double[][] matrix2, int[] dimensions) {
        double total = 0;
        double diff;
        for (int i = 0; i < matrix1[0].length; i++) {
            for (int dimension : dimensions) {
                diff = matrix1[dimension][i] - matrix2[dimension][i];
                total +=  diff * diff;
            }
        }
        return Math.sqrt(total);
    }

    public static double l2DistanceSquared(double[] vector1, double[] vector2, int[] indices) {
        double total = 0;
        double diff;
        for (int i = 0; i < indices.length; i++) {
            diff = vector1[indices[i]] - vector2[indices[i]];
            total +=  diff * diff;
        }
        return total;
    }

    public static double l2DistanceSquared(double[][] matrix1, double[][] matrix2, int[] dimensions) {
        double total = 0;
        double diff;
        for (int i = 0; i < matrix1[0].length; i++) {
            for (int dimension : dimensions) {
                diff = matrix1[dimension][i] - matrix2[dimension][i];
                total +=  diff * diff;
            }
        }
        return total;
    }

    public static double lpDistance(double[] vector1, double[] vector2, int p, int[] indices) {
        double total = 0;
        double pow;
        for (int i = 0; i < indices.length; i++) {
            pow =  Math.pow(vector1[indices[i]] - vector2[indices[i]], p);
            total += (pow <= 0.0D) ? 0.0D - pow : pow;  //Math.abs()
        }
        return Math.pow(total,  1.0D /p);
    }

    public static double lpDistance(double[][] matrix1, double[][] matrix2, int p, int[] dimensions) {
        double total = 0;
        double pow;
        for (int i = 0; i < matrix1[0].length; i++) {
            for (int dimension : dimensions) {
                pow = Math.pow(matrix1[dimension][i] - matrix2[dimension][i], p);
                total += (pow <= 0.0D) ? 0.0D - pow : pow;  //Math.abs()
            }
        }
        return Math.pow(total,  1.0D /p);
    }

    public static double dot(double[] vector1, double[] vector2, int[] indices) {
        double total = 0;
        for (int i = 0; i < indices.length; i++) {
            total +=  vector1[indices[i]] * vector2[indices[i]];
        }
        return total;
    }

    public static double dot(double[][] vector1, double[][] vector2, int[] dimensions) {
        double total = 0;
        for (int i = 0; i < vector1[0].length; i++) {
            for (int dimension : dimensions) {
                total +=  vector1[dimension][i] * vector2[dimension][i];
            }
        }
        return total;
    }

    public static double[] add(double[][] vector1, double[][] vector2, int[] dimensions) {
        double total = 0;
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            for (int dimension : dimensions) {
                result[i] = vector1[dimension][i] + vector2[dimension][i];
            }
        }
        return result;
    }

    public static double[] subtract(double[][] vector1, double[][] vector2, int[] dimensions) {
        double total = 0;
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            for (int dimension : dimensions) {
                result[i] = vector1[dimension][i] - vector2[dimension][i];
            }
        }
        return result;
    }

    //TODO CHECK
    public static int l1Similarity(double[] vector1, double[] vector2, double epsilon, int[] indices) {
        double total = 0;
        double diff;
        for (int i = 0; i < indices.length; i++) {
            diff = vector1[indices[i]] - vector2[indices[i]];
            total += (diff <= 0.0D) ? 0.0D - diff : diff;  //Math.abs()
        }
        return total <= indices.length * epsilon ? 1: 0;
    }

    //TODO CHECK
    public static int l2Similarity(double[] vector1, double[] vector2, double epsilon, int[] indices) {
        double total = 0;
        double diff;
        for (int i = 0; i < indices.length; i++) {
            diff = vector1[indices[i]] - vector2[indices[i]];
            total +=  diff * diff;
        }
        // if epsilon < 1?
        return total <= indices.length * epsilon * epsilon ? 1: 0;
    }

    //DEV
    public static int isBetween(double left, double mid, double right) {
        if (left <= mid && mid <= right){
            return 1;
        }else{
            return 0;
        }
    }

//    public static int isBetween(double[][] left, double[][] mid, double[][] right, int[] indices) {
//        double[] leftToRight = subtract(left, right, indices);
//        double midProjection = dot(mid, leftToRight, indices);
//
//
//    }

}
