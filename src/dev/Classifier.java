// Copyright (c) 2016 - Patrick Schäfer (patrick.schaefer@hu-berlin.de)
// Distributed under the GLP 3.0 (See accompanying file LICENSE)
package dev;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.carrotsearch.hppc.FloatContainer;
import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.cursors.FloatCursor;
import com.carrotsearch.hppc.cursors.IntCursor;

import datasets.TimeSeries;

public abstract class Classifier {
  transient ExecutorService exec;

  public static boolean[] NORMALIZATION = new boolean[]{true, false};

  public static boolean DEBUG = false;
  public static boolean ENSEMBLE_WEIGHTS = true;

  public static int threads = 1;

  protected int[][] testIndices;
  protected int[][] trainIndices;
  public static int folds = 10;

  // Blocks for parallel execution
  public final static int BLOCKS = 8;

  static {
    Runtime runtime = Runtime.getRuntime();
    if (runtime.availableProcessors() <= 4) {
      threads = runtime.availableProcessors() - 1;
    } else {
      threads = runtime.availableProcessors();
    }
//    threads = 1; // TODO
  }

  public Classifier() {
    this.exec = Executors.newFixedThreadPool(threads);
  }

  /**
   * Invokes {@code shutdown} when this executor is no longer
   * referenced and it has no threads.
   */
  protected void finalize() {
    if (exec != null) {
      exec.shutdown();
    }
  }

  /**
   * Build a classifier from the a training set with class labels.
   *
   * @param trainSamples The training set
   * @return The accuracy on the train-samples
   */
  public abstract Score fit(final TimeSeries[] trainSamples);

  /**
   * The predicted classes and accuracies of an array of samples.
   *
   * @param testSamples The passed set
   * @return The predictions for each passed sample and the test accuracy.
   */
  public abstract Predictions score(final TimeSeries[] testSamples);


  /**
   * The predicted classes of an array of samples.
   *
   * @param testSamples The passed set
   * @return The predictions for each passed sample.
   */
  public abstract Double[] predict(final TimeSeries[] testSamples);


  /**
   * Performs training and testing on a set of train- and test-samples.
   *
   * @param trainSamples The training set
   * @param testSamples  The training set
   * @return The accuracy on the test- and train-samples
   */
  public abstract Score eval(
          final TimeSeries[] trainSamples, final TimeSeries[] testSamples);


  protected Predictions evalLabels(TimeSeries[] testSamples, Double[] labels) {
    int correct = 0;
    for (int ind = 0; ind < testSamples.length; ind++) {
      correct += compareLabels(labels[ind],(testSamples[ind].getLabel().doubleValue()))? 1 : 0;
    }
    return new Predictions(labels, correct);
  }

  protected Predictions evalLabels(double[] sampleLabels, Double[] labels) {
    int correct = 0;
    for (int ind = 0; ind < sampleLabels.length; ind++) {
      correct += compareLabels(labels[ind],(sampleLabels[ind]))? 1 : 0;
    }
    return new Predictions(labels, correct);
  }

  public static class Words {
    public static int binlog(int bits) {
      int log = 0;
      if ((bits & 0xffff0000) != 0) {
        bits >>>= 16;
        log = 16;
      }
      if (bits >= 256) {
        bits >>>= 8;
        log += 8;
      }
      if (bits >= 16) {
        bits >>>= 4;
        log += 4;
      }
      if (bits >= 4) {
        bits >>>= 2;
        log += 2;
      }
      return log + (bits >>> 1);
    }

    public static long createWord(short[] words, int features, byte usedBits) {
      return fromByteArrayOne(words, features, usedBits);
    }

    /**
     * Returns a long containing the values in bytes.
     *
     * @param bytes
     * @param to
     * @param usedBits
     * @return
     */
    public static long fromByteArrayOne(short[] bytes, int to, byte usedBits) {
      int shortsPerLong = 60 / usedBits;
      to = Math.min(bytes.length, to);

      long bits = 0;
      int start = 0;
      long shiftOffset = 1;
      for (int i = start, end = Math.min(to, shortsPerLong + start); i < end; i++) {
        for (int j = 0, shift = 1; j < usedBits; j++, shift <<= 1) {
          if ((bytes[i] & shift) != 0) {
            bits |= shiftOffset;
          }
          shiftOffset <<= 1;
        }
      }

      return bits;
    }
  }

  public static class Model implements Comparable<Model> {
    public String name;
    public int windowLength;
    public boolean normed;

    public Score score;

    public Model() {
    }

    public Model(
            String name,
            int testing,
            int testSize,
            int training,
            int trainSize,
            boolean normed,
            int windowLength
    ) {
      this(name,
              new Score(name, testing, testSize, training, trainSize, windowLength),
              normed,
              windowLength);
    }

    public Model(
            String name,
            Score score,
            boolean normed,
            int windowLength
    ) {
      this.name = name;
      this.score = score;
      this.normed = normed;
      this.windowLength = windowLength;
    }

    @Override
    public String toString() {
      return score.toString();
    }

    public int compareTo(Model bestScore) {
      return this.score.compareTo(bestScore.score);
    }
  }

  public static class Score implements Comparable<Score> {
    public String name;
    public int training;
    public int trainSize;
    public int testing;
    public int testSize;
    public int windowLength;

    public Double avgOffset; // For earliness

    public Score() {
    }

    public Score(
            String name,
            int testing,
            int testSize,
            int training,
            int trainSize,
            int windowLength
    ) {
      this.name = name;
      this.training = training;
      this.trainSize = trainSize;
      this.testing = testing;
      this.testSize = testSize;
      this.windowLength = windowLength;
    }

    public double getTestingAccuracy() {
      return 1 - formatError(testing, testSize);
    }

    public double getTrainingAccuracy() {
      return 1 - formatError(training, trainSize);
    }

    public Double getTestEarliness() {
      return this.avgOffset;
    }

    public String getEarliness() {
      return String.format(Locale.ENGLISH, "%.2f", this.avgOffset);
    }

    @Override
    public String toString() {
      double test = getTestingAccuracy();
      double train = getTrainingAccuracy();

      if (avgOffset != null) {
          String earliness = getEarliness();
          return this.name+";"+train+";"+test+ ";" + earliness;
      }
      return this.name + ";" + train + ";" + test;
    }


    public int compareTo(Score bestScore) {
      if (this.training > bestScore.training
              || this.training == bestScore.training
              && this.windowLength > bestScore.windowLength // on a tie, prefer the one with the larger window-length
              ) {
        return 1;
      }
      return -1;
    }

    public void clear() {
      this.testing = 0;
      this.training = 0;
    }
  }

  public static class Predictions {
    public Double[] labels;
    public AtomicInteger correct;

    double[][] probabilities;
    int[] realLabels;


    public Predictions(Double[] labels, int bestCorrect) {
      this.labels = labels;
      this.correct = new AtomicInteger(bestCorrect);
    }

    public Predictions(
        Double[] predictions,
        double[][] probabilities,
        int[] realLabels) {
      this.labels = predictions;
      this.probabilities = probabilities;
      this.realLabels = realLabels;
    }
  }

  public static void outputResult(int correct, long time, int testSize) {
    double error = formatError(correct, testSize);
    //String errorStr = MessageFormat.format("{0,number,#.##%}", error);
    String correctStr = MessageFormat.format("{0,number,#.##%}", 1 - error);

    System.out.print("Correct:\t");
    System.out.print("" + correctStr + "");
    System.out.println("\tTime: \t" + (System.currentTimeMillis() - time) / 1000.0 + " s");
  }

  public static double formatError(int correct, int testSize) {
    return Math.round(1000 * (testSize - correct) / (double) (testSize)) / 1000.0;
  }




  private static void swap(int[] array, int idxA, int idxB) {
    int temp = array[idxA];
    array[idxA] = array[idxB];
    array[idxB] = temp;
  }

  // Stratified cross validation


  public static class Pair<E, T> {
    public E key;
    public T value;

    public Pair(E e, T t) {
      this.key = e;
      this.value = t;
    }

    public static <E, T> Pair<E, T> create(E e, T t) {
      return new Pair<>(e, t);
    }

    @Override
    public int hashCode() {
      return this.key.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
      return this.key.equals(((Pair<E, T>) obj).key);
    }
  }


  protected boolean compareLabels(Double label1, Double label2) {
    // compare 1.0000 to 1.0 in String returns false, hence the conversion to double
    return label1 != null && label2 != null && label1.equals(label2);
  }

  protected <E extends Model> Ensemble<E> filterByFactor(
          List<E> results,
          int correctTraining,
          double factor) {

    // sort descending
    Collections.sort(results, Collections.reverseOrder());

    // only keep best scores
    List<E> model = new ArrayList<>();
    for (E score : results) {
      if (score.score.training >= correctTraining * factor) { // all with same score
        model.add(score);
      }
    }

    return new Ensemble<>(model);
  }

  protected Double[] score(
          final String name,
          final TimeSeries[] samples,
          final List<Pair<Double, Integer>>[] labels,
          final List<Integer> currentWindowLengths) {

    Double[] predictedLabels = new Double[samples.length];
    //long[] maxCounts = new long[samples.length];

    //int correctTesting = 0;
    for (int i = 0; i < labels.length; i++) {
      Map<Double, Long> counts = new HashMap<>();

      for (Pair<Double, Integer> k : labels[i]) {
        if (k != null && k.key != null) {
          Double label = k.key;
          Long count = counts.get(label);
          long increment = ENSEMBLE_WEIGHTS ? k.value : 1;
          count = (count == null) ? increment : count + increment;
          counts.put(label, count);
        }
      }

      long maxCount = -1;
      for (Entry<Double, Long> e : counts.entrySet()) {
        if (predictedLabels[i] == null
                || maxCount < e.getValue()
                || maxCount == e.getValue()  // break ties
                && predictedLabels[i] <= e.getKey()
                ) {
          maxCount = e.getValue();
          // maxCounts[i] = maxCount;
          predictedLabels[i] = e.getKey();
        }
      }
    }

    //System.out.println(Arrays.toString(predictedLabels));
    //System.out.println(Arrays.toString(maxCounts));

    if (DEBUG) {
      System.out.print(name + " Testing with " + currentWindowLengths.size() + " models:\t");
      System.out.println(currentWindowLengths.toString() + "\n");
    }

    return predictedLabels;
  }


  protected Integer[] getWindowsBetween(int minWindowLength, int maxWindowLength) {
    List<Integer> windows = new ArrayList<>();
    for (int windowLength = maxWindowLength; windowLength >= minWindowLength; windowLength--) {
      windows.add(windowLength);
    }
    return windows.toArray(new Integer[]{});
  }

  protected int getMax(TimeSeries[] samples, int maxWindowSize) {
    int max = 0;
    for (TimeSeries ts : samples) {
      max = Math.max(ts.getLength(), max);
    }
    return Math.min(maxWindowSize,max);
  }

  protected static Set<Double> uniqueClassLabels(TimeSeries[] ts) {
    Set<Double> labels = new HashSet<>();
    for (TimeSeries t : ts) {
      labels.add(t.getLabel().doubleValue());
    }
    return labels;
  }

  protected static double magnitude(FloatContainer values) {
    double mag = 0.0D;
    for (FloatCursor value : values) {
      mag = mag + value.value * value.value;
    }
    return Math.sqrt(mag);
  }

  protected static int[] createIndices(int length) {
    int[] indices = new int[length];
    for (int i = 0; i < length; i++) {
      indices[i] = i;
    }
    return indices;
  }

  protected void generateIndices(TimeSeries[] samples) {
    IntArrayList[] sets = getStratifiedTrainTestSplitIndices(samples, folds);
    this.testIndices = new int[folds][];
    this.trainIndices = new int[folds][];
    for (int s = 0; s < folds; s++) {
      this.testIndices[s] = convertToInt(sets[s]);
      this.trainIndices[s] = convertToInt(sets, s);
    }
  }

  protected IntArrayList[] getStratifiedTrainTestSplitIndices(
          TimeSeries[] samples,
          int splits) {

    Map<Double, IntArrayDeque> elements = new HashMap<>();

    for (int i = 0; i < samples.length; i++) {
      Double label = samples[i].getLabel().doubleValue();
      IntArrayDeque sameLabel = elements.get(label);
      if (sameLabel == null) {
        sameLabel = new IntArrayDeque();
        elements.put(label, sameLabel);
      }
      sameLabel.addLast(i);
    }

    // pick samples
    IntArrayList[] sets = new IntArrayList[splits];
    for (int i = 0; i < splits; i++) {
      sets[i] = new IntArrayList();
    }

    // all but one
    for (Entry<Double, IntArrayDeque> data : elements.entrySet()) {
      IntArrayDeque d = data.getValue();
      separate:
      while (true) {
        for (int s = 0; s < splits; s++) {
          if (!d.isEmpty()) {
            int dd = d.removeFirst();
            sets[s].add(dd);
          } else {
            break separate;
          }
        }
      }
    }

    return sets;
  }

  protected static int[] convertToInt(IntArrayList trainSet) {
    int[] train = new int[trainSet.size()];
    int a = 0;
    for (IntCursor i : trainSet) {
      train[a++] = i.value;
    }
    return train;
  }

  protected static int[] convertToInt(IntArrayList[] setToSplit, int exclude) {
    int count = 0;

    for (int i = 0; i < setToSplit.length; i++) {
      if (i != exclude) {
        count += setToSplit[i].size();
      }
    }

    int[] setData = new int[count];
    int a = 0;
    for (int i = 0; i < setToSplit.length; i++) {
      if (i != exclude) {
        for (IntCursor d : setToSplit[i]) {
          setData[a++] = d.value;
        }
      }
    }

    return setData;
  }


}
