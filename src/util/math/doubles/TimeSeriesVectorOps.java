package util.math.doubles;

public class TimeSeriesVectorOps {

    public static final double threshold = 1e-10;

    public static double l1Norm(double[][] series, int[] dimensions) {
        double total = 0;
        double element;
        for (int i = 0; i < series[0].length; i++) {
            for (int dimension : dimensions) {
                element = series[dimension][i];
                total += (element <= 0.0D) ? 0.0D - element : element;  //Math.abs()
            }
        }
        return total;
    }

    public static double l2Norm(double[][] series, int[] dimensions) {
        double total = 0;
        double element;
        for (int i = 0; i < series[0].length; i++) {
            for (int dimension : dimensions) {
                element = series[dimension][i];
                total += element * element;
            }
        }
        return Math.sqrt(total);
    }

    public static double l2NormSquared(double[][] series, int[] dimensions) {
        double total = 0;
        double element;
        for (int i = 0; i < series[0].length; i++) {
            for (int dimension : dimensions) {
                element = series[dimension][i];
                total += element * element;
            }
        }
        return total;
    }

    public static double lpNorm(double[][] series, int p, int[] dimensions) {
        double total = 0;
        double element;
        for (int i = 0; i < series[0].length; i++) {
            for (int dimension : dimensions) {
                element = series[dimension][i];
                total += Math.pow(element, p);
            }
        }
        return  Math.pow(total, (double) 1/p);
    }

    public static double l1Distance(double[][] series1, double[][] series2, int[] dimensions) {
        double total = 0;
        double diff;
        for (int i = 0; i < series1[0].length; i++) {
            for (int dimension : dimensions) {
                diff = series1[dimension][i] - series2[dimension][i];
                total += (diff <= 0.0D) ? 0.0D - diff : diff;  //Math.abs()
            }
        }
        return total;
    }

    public static double l2Distance(double[][] series, double[][] series2, int[] dimensions) {
        double total = 0;
        double diff;
        for (int i = 0; i < series[0].length; i++) {
            for (int dimension : dimensions) {
                diff = series[dimension][i] - series2[dimension][i];
                total +=  diff * diff;
            }
        }
        return Math.sqrt(total);
    }

    public static double l2DistanceSquared(double[][] series1, double[][] series2, int[] dimensions) {
        double total = 0;
        double diff;
        for (int i = 0; i < series1[0].length; i++) {
            for (int dimension : dimensions) {
                diff = series1[dimension][i] - series2[dimension][i];
                total +=  diff * diff;
            }
        }
        return total;
    }

    public static double lpDistance(double[][] series1, double[][] series2, int p, int[] dimensions) {
        double total = 0;
        double pow;
        for (int i = 0; i < series1[0].length; i++) {
            for (int dimension : dimensions) {
                pow = Math.pow(series1[dimension][i] - series2[dimension][i], p);
                total += (pow <= 0.0D) ? 0.0D - pow : pow;  //Math.abs()
            }
        }
        return Math.pow(total,  1.0D /p);
    }

    //TODO CHECK
    public static int l1Similarity(double[][] series1, double[][] series2, double epsilon, int[] dimensions) {
        double total = 0;
        double diff;
        for (int i = 0; i < series1.length; i++) {
            for (int dimension : dimensions) {
                diff = series1[dimension][i] - series2[dimension][i];
                total += (diff <= 0.0D) ? 0.0D - diff : diff;  //Math.abs()
            }
        }
        return total <= dimensions.length * epsilon ? 1: 0;
    }

    //TODO CHECK
    public static int l2Similarity(double[][] series1, double[][] series2, double epsilon, int[] dimensions) {
        double total = 0;
        double diff;
        for (int i = 0; i < series1.length; i++) {
            for (int dimension : dimensions) {
                diff = series1[dimension][i] - series2[dimension][i];
                total += diff * diff;
            }
        }
        // if epsilon < 1?
        return total <= dimensions.length * epsilon * epsilon ? 1: 0;
    }

    public static double[] add(double[][] series1, int i, double[][] series2, int j, int[] dimensions) {
        double[] result = new double[series1.length];
        for (int dimension : dimensions) {
            result[i] = series1[dimension][i] + series2[dimension][i];
        }
        return result;
    }

    public static double[] subtract(double[][] series1, int i, double[][] series2, int j, int[] dimensions) {
        double[] result = new double[series1.length];
        for (int dimension : dimensions) {
            result[dimension] = series1[dimension][i] - series2[dimension][j];
        }
        return result;
    }

    public static double dot(double[][] series1, double[][] series2, int[] dimensions) {
        double total = 0;
        for (int i = 0; i < series1[0].length; i++) {
            for (int dimension : dimensions) {
                total +=  series1[dimension][i] * series2[dimension][i];
            }
        }
        return total;
    }


    public static double project(double[][] series1, int point, double[] direction, int[] dimensions) {
        double total = 0;
        for (int dimension : dimensions) {
            total +=  series1[dimension][point] * direction[dimension];
        }
        return total;
    }

    //TODO CHECK
    public static boolean isBetween(double[][] series1, double[][] series2, int left, int mid, int right, int[] dimensions) {
        double[] directionVector = subtract(series1, left, series2, right, dimensions);
        double[] unitVector = Vectors.div(directionVector, Vectors.l2Norm(directionVector));
        double midOnLeftProjection = project(series1, mid, unitVector, dimensions);
        double midOnRightProjection = project(series2, mid, unitVector, dimensions);
        if (midOnLeftProjection > 0 && midOnRightProjection < 0){
            return true;
        }else{
            return false;
        }

    }

}
