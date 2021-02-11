package util;

public abstract class StandardDeviation {

    public abstract void reset();

    public abstract void add(double value);

    public abstract double count();

    public abstract double sum();

    public abstract double mean();

    public abstract double variance();

    public abstract double sampleVar();

    public abstract double std();

    public abstract double sampleStd();

    /**
     * Welford's online algoirithm for standard deviation as a static function
     *
     * Reference: https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
     *
     * @param aggregate array of 5 values for existing values[count, mean, sqDistanceFromMean, varPopulation. stdPopulation]
     * @param newValue new value to add to the aggregates
     * @return array of 3 values for the next call [count, mean, sqDistanceFromMean, varPopulation. stdPopulation]
     */
    public static double[] stdvOnlineWelford(double[] aggregate, double newValue, boolean updateAggregates){
        if (aggregate == null || aggregate.length != 5){
            aggregate = new double[5];
        }

        if (updateAggregates){
            aggregate[0] += 1; //count += 1
            double delta = newValue - aggregate[1]; //delta = newValue - mean
            aggregate[1] += delta / aggregate[0]; //mean += delta / count
            double delta2 = newValue - aggregate[1]; //delta2 = newValue - mean
            aggregate[2] += delta * delta2; //sqDistanceFromMean += delta * delta2
        }else{
            if (aggregate[0] < 2){
                return new double[]{1,newValue,0,0,0};
            }else{
                aggregate[3] = aggregate[2] / aggregate[0];  //sqDistanceFromMean / count
                aggregate[4] = Math.sqrt(aggregate[2] / aggregate[0]); //sqDistanceFromMean / count
//				if (Double.isNaN(aggregate[3])){
//					throw new RuntimeException("stdvPopulationOnline: sqDistanceFromMean aggregate[3]" + aggregate[3]);
//				}
//				if (Double.isNaN(aggregate[4])){
//					throw new RuntimeException("stdvPopulationOnline: varPopulation aggregate[4]" + aggregate[4]);
//				}
            }
        }
        return aggregate;
    }


    /**
     * Object-oriented implementations of stdv, this is just to maintain state, especially useful with OO welford algorithm
     *
     */

    //https://stackoverflow.com/questions/11978667/online-algorithm-for-calculating-standard-deviation
    //TODO test
    public static class StandDeviationNaive extends StandardDeviation {
        private int n;
        private double sum;
        private double sumsq;

        public StandDeviationNaive(){

        }

        public void reset(){
            this.n = 0;
            this.sum = 0.0;
            this.sumsq = 0.0;
        }

        public void add(double value){
            ++this.n;
            this.sum += value;
            this.sumsq += value*value;
        }

        public double count(){
            return this.n;
        }

        public double sum(){
            return this.sum;
        }

        public double mean(){
            double mean = 0.0;
            if (this.n > 0) {
                mean = this.sum/this.n;
            }
            return mean;
        }

        public double variance(){
            double deviation = std();
            return deviation*deviation;
        }

        public double sampleVar() {
            double deviation = sampleStd();
            return deviation*deviation;
        }

        public double std(){
            double deviation = 0.0;
            if (this.n > 1) {
                deviation = Math.sqrt((this.sumsq - this.sum*this.sum/this.n)/(this.n));
            }
            return deviation;
        }

        public double sampleStd() {
            double deviation = 0.0;
            if (this.n > 1) {
                deviation = Math.sqrt((this.sumsq - this.sum*this.sum/this.n)/(this.n - 1));
            }
            return deviation;
        }

    }

    /**
     * Welford's online algoirithm for standard deviation
     *
     * https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
     *
     */
    //TODO test
    public static class StandDeviationWelford extends StandardDeviation {
        private int count;
        private double mean;
        private double sqDistanceFromMean;

        public StandDeviationWelford(){

        }

        public void reset(){
            this.count = 0;
            this.mean = 0.0;
            this.sqDistanceFromMean = 0.0;
        }

        public void add(double value){
            count += 1; //count += 1
            double delta = value - mean; //delta = newValue - mean
            mean += delta / count; //mean += delta / count
            double delta2 = value - mean; //delta2 = newValue - mean
            sqDistanceFromMean += delta * delta2; //sqDistanceFromMean += delta * delta2
        }

        @Override
        public double count() {
            return count;
        }

        @Override
        public double sum() {
            throw new RuntimeException("TODO");
        }

        public double mean(){
            return mean;
        }

        public double variance(){
            if (count < 2){
                return 0; //return Double.NaN
            }
            return sqDistanceFromMean/count;
        }

        @Override
        public double sampleVar() {
            if (count < 2){
                return 0; //return Double.NaN
            }
            return sqDistanceFromMean/(count-1);
        }

        public double std(){
            if (count < 2){
                return 0; //return Double.NaN
            }
            return Math.sqrt(variance());
        }

        @Override
        public double sampleStd() {
            if (count < 2){
                return 0; //return Double.NaN
            }
            return Math.sqrt(sampleVar());
        }
    }

    /**
     * Wrapper aroung appache common math implementation
     * https://commons.apache.org/proper/commons-math/javadocs/api-3.6/org/apache/commons/math3/stat/descriptive/moment/StandardDeviation.html
     *
     */
    public static class StandDeviationApacheMath extends StandardDeviation {
        private org.apache.commons.math3.stat.descriptive.moment.StandardDeviation stdCalculator;

        public StandDeviationApacheMath(){
            stdCalculator = new org.apache.commons.math3.stat.descriptive.moment.StandardDeviation(false);
        }

        public void reset(){
            stdCalculator.clear();
        }

        public void add(double value){
            stdCalculator.increment(value);
        }

        @Override
        public double count() {
            return stdCalculator.getN();
        }

        @Override
        public double sum() {
            throw new RuntimeException("TODO");
        }

        public double mean(){
            throw new RuntimeException("TODO");
        }

        public double variance(){
            double std = std();
            return std * std;
        }

        @Override
        public double sampleVar() {
            double sampleStd = sampleStd();
            return sampleStd * sampleStd;
        }

        public double std(){
            if (stdCalculator.getN() < 2){
                return 0; //return Double.NaN
            }
            stdCalculator.setBiasCorrected(false);
            return stdCalculator.getResult();
        }

        @Override
        public double sampleStd() {
            if (stdCalculator.getN() < 2){
                return 0; //return Double.NaN
            }
            stdCalculator.setBiasCorrected(true);
            return stdCalculator.getResult();
        }
    }


}
