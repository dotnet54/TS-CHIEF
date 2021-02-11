package core;

import java.util.Random;

public class EnsembleVoter {

    public enum EnsembleVotingScheme{
        MajorityVoting,
        WeightedTrainingAccuracyVoting
    }

    public int ensembleSize;
    public int[] maxPredictedLabels;
//    public double[] maxPredictedProbabilities;

    public EnsembleVoter(int ensembleSize){
        this.ensembleSize = ensembleSize;
//        this.maxPredictedLabels = new int[ensembleSize];
//        this.maxPredictedProbabilities = new double[ensembleSize];
    }

    /**
     *
     * @param correctLabels correctLabels[testSize]
     * @param predictedLabels predictedLabels[ensembleSize][testSize]
     * @param rand
     * @return
     */
    public int[] majorityVotedClass(int[] correctLabels, int[][] predictedLabels, Random rand){
        int votedAdded;

        return null;
    }

    public int majorityVotedClassForOneInstance(int correctLabel, int[] predictedLabels, Random rand){
        int votedAdded;

        return -1;
    }

    /**
     *
     * @param correctLabels correctLabels[testSize]
     * @param predictedLabels predictedLabels[ensembleSize][testSize]
     * @param modelWeights modelWeights[ensembleSize] if null assume weight = 1
     * @param rand
     * @return probability distribution of predicted class
     */
    public double[] weightedVoteUsingLabels(int[] correctLabels, int[][] predictedLabels, int[] modelWeights, Random rand){
        return null;
    }

    /**
     *
     * @param correctLabels correctLabels[testSize] probability
     * @param predictedLabels predictedLabels[ensembleSize][testSize] probability
     * @param modelWeights modelWeights[ensembleSize] if null assume weight = 1
     * @param rand
     * @return probability distribution of predicted class
     */
    public double[] weightedVoteUsingProbabilities(double[] correctLabels, double[][] predictedLabels, int[] modelWeights, Random rand){
        return null;
    }

}
