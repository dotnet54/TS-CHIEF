package tests.multivariate;

import distance.multivariate.*;
import org.junit.jupiter.api.*;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class MultivariateMeasureTest {

    public static final double FLOAT_DELTA = 1e-6; //double comparison

    public static int seed = 4611145;
    public static Random rand;
    public static double[][] series1Data = new double[2][];
    public static double[][] series2Data = new double[2][];
    public static int[] allDimensions = IntStream.range(0, series1Data.length).toArray();
    public static int[] subsetOfDimensions = new int[]{0};

    public static double lpIndepdenet = 2.0;
    public static double lpDependent = 2.0;
    public static boolean adjustSquredDiff = true;
    public static boolean useSquaredDiff = true;

    public static int wDTW = 0;
    public static double gWDTW = 0;
    public static int wLCSS = 0;
    public static double eLCSS = 0.5;
    public static double[] epdLCSS = new double[]{0.5,0.5};
    public static double cMSM = 0.5;
    public static int wERP = 0;
    public static double[] gERP = new double[]{1, 1};;
    public static double nTWE = 0.1;
    public static double lTWE = 0.1;

    public static final double ANS_DIM_ALL = 3553.0;
    public static final double ANS_DIM_ALL_L2 = 3259.5841759341024;
    //    public static final double ANS_DIM_ALL_L2 = 1.0624889E7;
    public static final double ANS_ONE_DIM = 308.0;

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
    public void independentEuclideanTest() {
        Euclidean measure = new Euclidean(false, allDimensions);
        measure.setpForIndependentDims(lpIndepdenet);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL_L2, distance, FLOAT_DELTA);
        System.out.println("euc_i: " + distance);
    }

    @Test
    public void dependentEuclideanTest() {
        Euclidean measure = new Euclidean(true, allDimensions);
        measure.setpForDependentDims(lpDependent);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("euc_d: " + distance);
    }

    @Test
    public void independentDTWTest() {
        DTW measure = new DTW(false, allDimensions, wDTW);
        measure.setpForIndependentDims(lpIndepdenet);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL_L2, distance, FLOAT_DELTA);
        System.out.println("dtw_i: " + distance);
    }

    @Test
    public void dependentDTWTest() {
        DTW measure = new DTW(true, allDimensions, wDTW);
        measure.setpForDependentDims(lpDependent);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("dtw_d: " + distance);
    }

    @Test
    public void independentDDTWTest() {
        DDTW measure = new DDTW(false, allDimensions, wDTW);
        measure.setpForIndependentDims(lpIndepdenet);
        //TODO pass derivative data
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL_L2, distance, FLOAT_DELTA);
        System.out.println("ddtw_i: " + distance);
    }

    @Test
    public void dependentDDTWTest() {
        DDTW measure = new DDTW(true, allDimensions, wDTW);
        measure.setpForDependentDims(lpDependent);
        //TODO pass derivative data
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("ddtw_d: " + distance);
    }

    @Test
    public void independentWDTWTest() {
        WDTW measure = new WDTW(false, allDimensions, gWDTW);
        measure.setpForIndependentDims(lpIndepdenet);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL_L2, distance, FLOAT_DELTA);
        System.out.println("wdtw_i: " + distance);
    }

    @Test
    public void dependentWDTWTest() {
        WDTW measure = new WDTW(true, allDimensions, gWDTW);
        measure.setpForDependentDims(lpDependent);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("wdtw_d: " + distance);
    }

    @Test
    public void independentLCSSTest() {
        LCSS measure = new LCSS(false, allDimensions, wLCSS, eLCSS, epdLCSS);
        measure.setpForIndependentDims(lpIndepdenet);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL_L2, distance, FLOAT_DELTA);
        System.out.println("lcss_i: " + distance);
    }

    @Test
    public void dependentLCSSTest() {
        LCSS measure = new LCSS(true, allDimensions, wLCSS, eLCSS, epdLCSS);
        measure.setpForDependentDims(lpDependent);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("lcss_d: " + distance);
    }

    @Test
    public void independentMSMTest() {
        MSM measure = new MSM(false, allDimensions, cMSM);
        measure.setpForIndependentDims(lpIndepdenet);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL_L2, distance, FLOAT_DELTA);
        System.out.println("msm_i: " + distance);
    }

    @Test
    public void dependentMSMTest() {
        MSM measure = new MSM(true, allDimensions, cMSM);
        measure.setpForDependentDims(lpDependent);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("msm_d: " + distance);
    }

    @Test
    public void independentERPTest() {
        ERP measure = new ERP(false, allDimensions, wERP, gERP);
        measure.setpForIndependentDims(lpIndepdenet);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL_L2, distance, FLOAT_DELTA);
        System.out.println("erp_i: " + distance);
    }

    @Test
    public void dependentERPTest() {
        ERP measure = new ERP(true, allDimensions, wERP, gERP);
        measure.setpForDependentDims(lpDependent);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("erp_d: " + distance);
    }

    @Test
    public void independentTWETest() {
        TWE measure = new TWE(false, allDimensions, nTWE, lTWE);
        measure.setpForIndependentDims(lpIndepdenet);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL_L2, distance, FLOAT_DELTA);
        System.out.println("twe_i: " + distance);
    }

    @Test
    public void dependentTWETest() {
        TWE measure = new TWE(true, allDimensions, nTWE, lTWE);
        measure.setpForDependentDims(lpDependent);
        double distance = measure.distance(series1Data, series2Data, Double.POSITIVE_INFINITY);
        assertEquals(ANS_DIM_ALL, distance, FLOAT_DELTA);
        System.out.println("twe_d: " + distance);
    }

}
