package util.math.integers;

import java.util.Arrays;
import java.util.List;

public class StatisticsInt {

    public static int sum(int ... list) {
        int sum = 0;
        for (int i = 0; i < list.length; i++) {
            sum += list[i];
        }
        return sum;
    }

    public static int sum(List<Integer> list) {
        int sum = 0;
        int length = list.size();
        for (int i = 0; i < length; i++) {
            sum += list.get(i);
        }
        return sum;
    }

    public static double mean(int ... list) {
        double sum = 0;
        for (int i = 0; i < list.length; i++) {
            sum += list[i];
        }
        return sum/list.length;
    }

    public static double mean(List<Integer> list) {
        return sum(list) / list.size();
    }

    public static double varP(int ... list) {
        double mean = mean(list);
        double total = 0;
        for (int i = 0; i < list.length; i++) {
            total += (list[i] - mean) * (list[i] - mean);
        }
        return total/list.length;
    }

    public static double varP(double meanP, int ... list) {
        double total = 0;
        for (int i = 0; i < list.length; i++) {
            total += (list[i] - meanP) * (list[i] - meanP);
        }
        return total/list.length;
    }

    public static double varS(int ... list) {
        double mean = mean(list);
        double total = 0;
        for (int i = 0; i < list.length; i++) {
            total += (list[i] - mean) * (list[i] - mean);
        }
        return total/(list.length-1);
    }

    public static double varS(double meanS, int ... list) {
        double total = 0;
        for (int i = 0; i < list.length; i++) {
            total += (list[i] - meanS) * (list[i] - meanS);
        }
        return total/(list.length-1);
    }

    public static double stdP(int ... list) {
        return Math.sqrt(varP(list));
    }

    public static double stdP(double meanP, int ... list) {
        return Math.sqrt(varP(meanP, list));
    }

    public static double stdS(int ... list) {
        return Math.sqrt(varS(list));
    }

    public static double stdS(double meanS, int ... list) {
        return Math.sqrt(varS(meanS, list));
    }

    public static double median(int ... list) {
        Arrays.sort(list);
        int middle = (list.length / 2);
        if (list.length % 2 == 0) {
            return (double) (list[middle] +  list[middle - 1]) / 2;
        }else{
            return list[middle];
        }
    }

    public static int max(int ... list){
        if (list.length == 0){
            throw new RuntimeException("List is empty");
        }
        int maxValue = list[0];
        for(int i=1;i<list.length;i++){
            if(list[i] > maxValue){
                maxValue = list[i];
            }
        }
        return maxValue;
    }

    public static int argmax(int ... list){
        if (list.length == 0){
            throw new RuntimeException("List is empty");
        }
        int maxValue = list[0];
        int maxIndex = 0;
        for(int i=1;i<list.length;i++){
            if(list[i] > maxValue){
                maxValue = list[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static double min(int ... list){
        if (list.length == 0){
            throw new RuntimeException("List is empty");
        }
        int minValue = list[0];
        for(int i=1;i<list.length;i++){
            if(list[i] < minValue){
                minValue = list[i];
            }
        }
        return minValue;
    }

    public static int argmin(int ... list){
        if (list.length == 0){
            throw new RuntimeException("List is empty");
        }
        int minValue = list[0];
        int minIndex = 0;
        for(int i=1;i<list.length;i++){
            if(list[i] < minValue){
                minValue = list[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

}
