package tests.multivariate;

import org.junit.jupiter.api.*;
import distance.multivariate.DTW;
import distance.multivariate.Euclidean;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class EuclideanAndDTWTest {

    public static final double FLOAT_DELTA = 1e-6; //double comparison

    public static int seed = 4611145;
    public static Random rand;
    public static double[][] series1Data = new double[2][];
    public static double[][] series2Data = new double[2][];
    public static int[] allDimensions = IntStream.range(0, series1Data.length).toArray();
    public static int[] subsetOfDimensions = new int[]{0};

    public static boolean useSquaredEuclidean = true;
    public static double lpIndepdenet = 2.0;
    public static double lpDependent = 2.0;
    public static boolean adjustSquredDiff = true;
    public static boolean useSquaredDiff = true;

    public static int dtwWindow = 0;

//    public static final double ANS_DIM_ALL = 3553.0;
//    public static final double ANS_ONE_DIM = 308.0;
    public static final double L1_DIM_ALL = 95;
    public static final double L2_DIM_ALL = 3553.0;
    public static final double L1_ONE_DIM = 0;
    public static final double L2_ONE_DIM = 0;

    @BeforeAll
    static void beforeAll() {
        rand = new Random(seed);

        series1Data[0] = new double[]{2, 35, 14};
        series1Data[1] = new double[]{5, 68, 7};
        series2Data[0] = new double[]{8, 19, 10};
        series2Data[1] = new double[]{15, 12, 4};
    }

    @AfterAll
    static void afterAll() {
        //pass
    }

    @BeforeEach
    public void setUp() {
        //pass
    }

    @AfterEach
    public void tearDown() {
        //pass
    }

    @Test
    public void independentEuclideanTestL1AllDims() {
        Euclidean measure = new Euclidean(false, allDimensions, useSquaredEuclidean);
        measure.setpForIndependentDims(1);
        double distance1 = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L1_DIM_ALL, distance1, FLOAT_DELTA);
        System.out.println("euc_i L1 all dims: " + distance1);
    }

    @Test
    public void independentEuclideanTestL2AllDims() {
        Euclidean measure = new Euclidean(false, allDimensions, useSquaredEuclidean);
        measure.setpForIndependentDims(2);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L2_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("euc_i L2 all dims: " + distance);
    }

    @Test
    public void dependentEuclideanTestL1AllDims() {
        Euclidean eucDep = new Euclidean(true, allDimensions, useSquaredEuclidean);
        eucDep.setpForDependentDims(1);
        double eucDepDistance = eucDep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L1_DIM_ALL, eucDepDistance, FLOAT_DELTA);
        System.out.println("euc_d L1 all dims: " + eucDepDistance);
    }

    @Test
    public void dependentEuclideanTestL2AllDims() {
        Euclidean eucDep = new Euclidean(true, allDimensions, useSquaredEuclidean);
        eucDep.setpForDependentDims(2);
        double eucDepDistance = eucDep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L2_DIM_ALL, eucDepDistance, FLOAT_DELTA);
        System.out.println("euc_d L2 all dims: " + eucDepDistance);
    }


    @Test
    public void independentDTWTestL1AllDims() {
        DTW dtwIndep = new DTW(false, allDimensions, dtwWindow);
        dtwIndep.setpForIndependentDims(1);
        double distance = dtwIndep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L1_DIM_ALL, distance , FLOAT_DELTA);
        System.out.println("dtw_i L1 all dims: " + distance);
    }

    @Test
    public void independentDTWTestL2AllDims() {
        DTW dtwIndep = new DTW(false, allDimensions, dtwWindow);
        dtwIndep.setpForIndependentDims(2);
        double distance = dtwIndep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L2_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("dtw_i L2 all dims: " + distance);
    }

    @Test
    public void dependentDTWTestL1AllDims() {
        DTW dtwDep = new DTW(true, allDimensions, dtwWindow);
        dtwDep.setpForDependentDims(1);
        double distance = dtwDep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L1_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("dtw_d L1 all dims: " + distance);
    }

    @Test
    public void dependentDTWTestL2AllDims() {
        DTW dtwDep = new DTW(true, allDimensions, dtwWindow);
        dtwDep.setpForDependentDims(2);
        double distance = dtwDep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L2_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("dtw_d L2 all dims: " + distance);
    }

    @Test
    public void independentEuclideanTestL1SubsetOfDims() {
        Euclidean measure = new Euclidean(false, subsetOfDimensions, useSquaredEuclidean);
        measure.setpForIndependentDims(1);
        double distance1 = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L1_ONE_DIM, distance1, FLOAT_DELTA);
        System.out.println("euc_i L1 subset dims: " + distance1);
    }

    @Test
    public void independentEuclideanTestL2SubsetOfDims() {
        Euclidean measure = new Euclidean(false, subsetOfDimensions, useSquaredEuclidean);
        measure.setpForIndependentDims(2);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L2_ONE_DIM, distance, FLOAT_DELTA);
        System.out.println("euc_i L2 subset dims: " + distance);
    }

    @Test
    public void dependentEuclideanTestL1SubsetOfDims() {
        Euclidean eucDep = new Euclidean(true, subsetOfDimensions, useSquaredEuclidean);
        eucDep.setpForDependentDims(1);
        double eucDepDistance = eucDep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L1_ONE_DIM, eucDepDistance, FLOAT_DELTA);
        System.out.println("euc_d L1 subset dims: " + eucDepDistance);
    }

    @Test
    public void dependentEuclideanTestL2SubsetOfDims() {
        Euclidean eucDep = new Euclidean(true, subsetOfDimensions, useSquaredEuclidean);
        eucDep.setpForDependentDims(2);
        double eucDepDistance = eucDep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L2_ONE_DIM, eucDepDistance, FLOAT_DELTA);
        System.out.println("euc_d L2 subset dims: " + eucDepDistance);
    }


    @Test
    public void independentDTWTestL1SubsetOfDims() {
        DTW dtwIndep = new DTW(false, subsetOfDimensions, dtwWindow);
        dtwIndep.setpForIndependentDims(1);
        double distance = dtwIndep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L1_ONE_DIM, distance , FLOAT_DELTA);
        System.out.println("dtw_i L1 subset dims: " + distance);
    }

    @Test
    public void independentDTWTestL2SubsetOfDims() {
        DTW dtwIndep = new DTW(false, subsetOfDimensions, dtwWindow);
        dtwIndep.setpForIndependentDims(2);
        double distance = dtwIndep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L2_ONE_DIM, distance, FLOAT_DELTA);
        System.out.println("dtw_i L2 subset dims: " + distance);
    }

    @Test
    public void dependentDTWTestL1SubsetOfDims() {
        DTW dtwDep = new DTW(true, subsetOfDimensions, dtwWindow);
        dtwDep.setpForDependentDims(1);
        double distance = dtwDep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L1_ONE_DIM, distance, FLOAT_DELTA);
        System.out.println("dtw_d L1 subset dims: " + distance);
    }

    @Test
    public void dependentDTWTestL2SubsetOfDims() {
        DTW dtwDep = new DTW(true, subsetOfDimensions, dtwWindow);
        dtwDep.setpForDependentDims(2);
        double distance = dtwDep.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(L2_ONE_DIM, distance, FLOAT_DELTA);
        System.out.println("dtw_d L2 subset dims: " + distance);
    }


}
