package util.math.doubles;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StatisticsDbl {
    public static final double threshold = 1e-10;

    public static double sum(double ... list) {
        double sum = 0;
        for (int i = 0; i < list.length; i++) {
            sum += list[i];
        }
        return sum;
    }

    public static double sum(List<Double> list) {
        double sum = 0;
        int length = list.size();
        for (int i = 0; i < length; i++) {
            sum += list.get(i);
        }
        return sum;
    }

    public static double mean(double ... list) {
        double sum = 0;
        for (int i = 0; i < list.length; i++) {
            sum += list[i];
        }
        return sum/list.length;
    }

    public static double mean(List<Double> list) {
        return sum(list) / list.size();
    }

    public static double varP(double ... list) {
        double mean = mean(list);
        double total = 0;
        for (int i = 0; i < list.length; i++) {
            total += (list[i] - mean) * (list[i] - mean);
        }
        return total/list.length;
    }

    public static double varP(double meanP, double ... list) {
        double total = 0;
        for (int i = 0; i < list.length; i++) {
            total += (list[i] - meanP) * (list[i] - meanP);
        }
        return total/list.length;
    }

    public static double varS(double ... list) {
        double mean = mean(list);
        double total = 0;
        for (int i = 0; i < list.length; i++) {
            total += (list[i] - mean) * (list[i] - mean);
        }
        return total/(list.length-1);
    }

    public static double varS(double meanS, double ... list) {
        double total = 0;
        for (int i = 0; i < list.length; i++) {
            total += (list[i] - meanS) * (list[i] - meanS);
        }
        return total/(list.length-1);
    }

    public static double stdP(double ... list) {
        return Math.sqrt(varP(list));
    }

    public static double stdP(double meanP, double ... list) {
        return Math.sqrt(varP(meanP, list));
    }

    public static double stdS(double ... list) {
        return Math.sqrt(varS(list));
    }

    public static double stdS(double meanS, double ... list) {
        return Math.sqrt(varS(meanS, list));
    }

    public static double median(double ... list) {
        Arrays.sort(list);
        int middle = (list.length / 2);
        if (list.length % 2 == 0) {
            return (list[middle] +  list[middle - 1]) / 2;
        }else{
            return list[middle];
        }
    }

    public static double max(double ... list){
        if (list.length == 0){
            throw new RuntimeException("List is empty");
        }
        double maxValue = list[0];
        for(int i=1;i<list.length;i++){
            if(list[i] > maxValue){
                maxValue = list[i];
            }
        }
        return maxValue;
    }

    public static double min(double ... list){
        if (list.length == 0){
            throw new RuntimeException("List is empty");
        }
        double minValue = list[0];
        for(int i=1;i<list.length;i++){
            if(list[i] < minValue){
                minValue = list[i];
            }
        }
        return minValue;
    }

    public static int argmax(double ... list){
        if (list.length == 0){
            throw new RuntimeException("List is empty");
        }
        double maxValue = list[0];
        int maxIndex = 0;
        for(int i=1;i<list.length;i++){
            if(list[i] > maxValue){
                maxValue = list[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

//    public static int argmax_tie_break(Random rand, double ... list){
//        if (list.length == 0){
//            throw new RuntimeException("List is empty");
//        }
//        double maxValue = list[0];
//        for(int i=1;i<list.length;i++){
//            if(list[i] > maxValue){
//                maxValue = list[i];
//            }
//        }
//        return maxValue;
//    }

    public static int argmin(double ... list){
        if (list.length == 0){
            throw new RuntimeException("List is empty");
        }
        double minValue = list[0];
        int minIndex = 0;
        for(int i=1;i<list.length;i++){
            if(list[i] < minValue){
                minValue = list[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

//    public static int argmin_tie_break(Random rand,double ... list){
//        if (list.length == 0){
//            throw new RuntimeException("List is empty");
//        }
//        double minValue = list[0];
//        for(int i=1;i<list.length;i++){
//            if(list[i] < minValue){
//                minValue = list[i];
//            }
//        }
//        return minValue;
//    }

    /**
     * Welford's online algoirithm for variance
     *
     * Reference: https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
     *
     * @param aggregate array of 5 values for existing values[count, mean, sqDistanceFromMean, varPopulation. varSample]
     * @param newValue new value to add to the aggregates
     * @return array of 3 values for the next call [count, mean, sqDistanceFromMean, varPopulation. varSample]
     */
    public static double[] varOnline(double[] aggregate, double newValue){
        aggregate[0] += 1; //count += 1
        double delta = newValue - aggregate[1]; //delta = newValue - mean
        aggregate[1] += delta / aggregate[0]; //mean += delta / count
        double delta2 = newValue - aggregate[1]; //delta2 = newValue - mean
        aggregate[2] += delta * delta2; //sqDistanceFromMean += delta * delta2

        if (aggregate[0] < 2){
            return new double[]{1,newValue,0,0,0};
        }else{
            aggregate[3] = aggregate[2] / aggregate[0];  //sqDistanceFromMean / count
            aggregate[4] = aggregate[2] / (aggregate[0] - 1); //sqDistanceFromMean / (count - 1)
            return aggregate;
        }
    }

    /**
     * Welford's online algoirithm for standard deviation
     *
     * Reference: https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
     *
     * @param aggregate array of 5 values for existing values[count, mean, sqDistanceFromMean, varPopulation. stdPopulation]
     * @param newValue new value to add to the aggregates
     * @return array of 3 values for the next call [count, mean, sqDistanceFromMean, varPopulation. stdPopulation]
     */
    public static double[] stdvPopulationOnline(double[] aggregate, double newValue){

        if (aggregate == null || aggregate.length != 5){
            aggregate = new double[5];
        }

        aggregate[0] += 1; //count += 1
        double delta = newValue - aggregate[1]; //delta = newValue - mean
        aggregate[1] += delta / aggregate[0]; //mean += delta / count
        double delta2 = newValue - aggregate[1]; //delta2 = newValue - mean
        aggregate[2] += delta * delta2; //sqDistanceFromMean += delta * delta2

        if (aggregate[0] < 2){
            return new double[]{1,newValue,0,0,0};
        }else{
            aggregate[3] = aggregate[2] / aggregate[0];  //sqDistanceFromMean / count
            aggregate[4] = Math.sqrt(aggregate[2] / aggregate[0]); //sqDistanceFromMean / count
            return aggregate;
        }
    }

}
