package org.esa.snap.speclib.util.resampling;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.io.csv.util.CsvTable;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

public class SpectralResamplingTest {

    private final double[] enmapInputSpectrum = new double[]{
            0.0581041, 0.05447768, 0.047703203, 0.05075483, 0.05158371, 0.052788906,
            0.05466709, 0.053815167, 0.05235644, 0.05175069, 0.05010918, 0.049060915,
            0.04762088, 0.04554629, 0.04233955, 0.04289402, 0.04224953, 0.040197592,
            0.039995085, 0.04009146, 0.038809046, 0.040494666, 0.04273823, 0.04393642,
            0.045118824, 0.04466195, 0.044460457, 0.04461542, 0.04349551, 0.041548003,
            0.039837692, 0.036526304, 0.034223784, 0.033573437, 0.030791711, 0.029272627,
            0.029295728, 0.028710265, 0.027767867, 0.025852531, 0.02527827, 0.024134388,
            0.023897523, 0.023199439, 0.02109867, 0.019729586, 0.019305987, 0.018913703,
            0.018375132, 0.018099044, 0.017089693, 0.02057491, 0.028567752, 0.038674254,
            0.04727957, 0.048269473, 0.06035516, 0.0826134, 0.10308172, 0.11170257,
            0.101383604, 0.06492057, 0.10706389, 0.116533026, 0.112666, 0.1075456,
            0.106385946, 0.10143183, 0.08000946, 0.08227138, 0.08924345, 0.1006294,
            0.10177352, 0.102422304, 0.10136909, 0.102339715, 0.10220345, 0.09511748,
            0.07523048, 0.0612792, 0.05540112, 0.05481165, 0.042073198, 0.016906073,
            0.018142423, 0.020561308, 0.028864585, 0.044607688, 0.052509803, 0.064795084,
            0.07119629, 0.048625436, 0.043287996, 0.04779212, 0.020160904, 0.013322428,
            0.015310945, 0.025222287, 0.04229353, 0.051927462, 0.06145862, 0.060997136,
            0.06156307, 0.061275892, 0.060655985, 0.05942464, 0.05735479, 0.05495278,
            0.052428994, 0.04587389, 0.032544572, 0.011314435, 0.004139657, 0.007510427,
            0.008983513, 0.02335119, 0.030408397, 0.030919258, 0.032426786, 0.034008343};

    private final double[] enmapInputWvls = new double[]{
            418.416, 424.043, 429.457, 434.686, 439.758, 444.699, 449.539, 454.306, 459.031, 463.73,
            468.411, 473.08, 477.744, 482.411, 487.087, 491.78, 496.497, 501.243, 506.02, 510.829,
            515.672, 520.551, 525.467, 530.424, 535.422, 540.463, 545.551, 550.687, 555.873, 561.112,
            566.405, 571.756, 577.166, 582.636, 588.171, 593.773, 599.446, 605.193, 611.017, 616.923,
            622.921, 628.987, 635.112, 641.294, 647.537, 653.841, 660.207, 666.637, 673.131, 679.691,
            686.319, 693.014, 699.78, 706.617, 713.524, 720.501, 727.545, 734.654, 741.826, 749.06,
            756.353, 763.703, 771.108, 778.567, 786.078, 793.639, 801.249, 808.905, 816.608, 824.355,
            832.145, 839.976, 847.847, 855.757, 863.703, 871.683, 879.693, 887.729, 895.789, 903.87,
            911.968, 920.081, 928.204, 936.335, 944.47, 952.608, 960.748, 968.892, 977.037, 985.186,
            993.338, 1003.88, 1014.72, 1025.66, 1036.7, 1047.84, 1059.07, 1070.39, 1081.78, 1093.26,
            1104.81, 1116.43, 1128.1, 1139.84, 1151.62, 1163.44, 1175.3, 1187.2, 1199.11, 1211.05,
            1223.37, 1235.34, 1247.31, 1259.3, 1271.29, 1283.29, 1295.28, 1307.27, 1319.25, 1331.22};

    private final double[] prismaInputSpectrum = new double[]{
            0.06357856, 0.065789476, 0.055098828, 0.04920844, 0.052259535, 0.064120166,
            0.060783815, 0.057733163, 0.057375964, 0.05579568, 0.054379884, 0.04863483,
            0.044245984, 0.040215954, 0.04574708, 0.047016636, 0.046170328, 0.04682072,
            0.04761348, 0.048467204, 0.044543635, 0.042216435, 0.038765814, 0.03696022,
            0.035636663, 0.03462865, 0.03208817, 0.03184494, 0.034984224, 0.029887324,
            0.027721006, 0.026534196, 0.027917983, 0.02601325, 0.027556397, 0.032600872,
            0.042364955, 0.043823827, 0.055858087, 0.07978008, 0.09557285, 0.07613533,
            0.08943734, 0.10366141, 0.097907715, 0.09069953, 0.08221958, 0.06857756,
            0.07653507, 0.0909941, 0.08807424, 0.08986379, 0.089257605, 0.08403985,
            0.056494664, 0.05253663, 0.052663267, 0.02730614, 0.013644617, 0.014668097,
            0.031615384, 0.047347553, 0.056709416, 0.07160681, 0.07103411, 0.07270718,
            0.052946668,  0.07099338,
            0.06982204, 0.069575846, 0.064386286, 0.06150447, 0.061667215, 0.052425023,
            0.03684188, 0.017855598, 0.004193186, 0.00555869, 0.009715090, 0.010664231,
            0.028547615, 0.03456788, 0.03296503, 0.03233909, 0.034206256, 0.040442616,
            0.04215408, 0.044349056, 0.038771678, 0.03247222, 0.036018904, 0.03706376,
            0.030486718, 0.024081193, 0.018062647, 0.00983711, 0.003220507, 3.22338E-4,
            7.584432E-4, -0.00117957, -1.85539E-4, -4.1842E-4, 6.404922E-4, -0.0018924,
            0.001080213, 5.024912E-4, -1.25224E-5, 4.86473E-5, 0.00135011, 0.0020281777};

    private final double[] prismaInputWvls = new double[]{
            405.0, 415.0, 424.0, 432.0, 440.0, 448.0, 455.0, 463.0, 470.0, 477.0,
            485.0, 492.0, 500.0, 507.0, 515.0, 523.0, 530.0, 538.0, 546.0, 554.0,
            562.0, 571.0, 579.0, 588.0, 596.0, 605.0, 614.0, 623.0, 632.0, 641.0,
            650.0, 660.0, 669.0, 679.0, 689.0, 698.0, 708.0, 718.0, 728.0, 739.0,
            749.0, 759.0, 770.0, 780.0, 790.0, 801.0, 812.0, 822.0, 833.0, 844.0,
            854.0, 865.0, 876.0, 887.0, 897.0, 908.0, 919.0, 930.0, 940.0, 951.0,
            962.0, 972.0, 983.0, 993.0, 1004.0, 1015.0, 1021.0, 1031.0, 1041.0, 1051.0,
            1061.0, 1072.0, 1082.0, 1093.0, 1103.0, 1114.0, 1124.0, 1135.0, 1146.0, 1157.0,
            1167.0, 1178.0, 1189.0, 1200.0, 1211.0, 1222.0, 1233.0, 1244.0, 1255.0, 1266.0,
            1277.0, 1288.0, 1299.0, 1310.0, 1321.0, 1332.0, 1343.0, 1354.0, 1365.0, 1376.0,
            1387.0, 1398.0, 1409.0, 1420.0, 1431.0, 1442.0, 1453.0, 1463.0, 1474.0, 1485.0};

    private final double[] olciInputSpectrum = new double[]{
            105.33761, 115.1706, 120.02343, 116.7073, 114.318726, 114.59385, 133.63246, 138.82625,
            138.58409, 138.04797, 132.03772, 131.85347, 37.904022, 65.76642, 114.50595, 122.30608,
            100.14883, 96.83181, 64.75161, 20.552023, 81.00681};

    private final double[] olciInputWvls = new double[]{
            400.1732, 411.75812, 442.95776, 490.5534, 510.52353, 560.5521, 620.395, 665.3918,
            674.1551, 681.66376, 709.24176, 754.2953, 761.8105, 764.92523, 768.0407, 779.3815,
            865.4787, 884.3511, 899.3343, 938.9488, 1015.7598};

    @Test
    @STTM("SNAP-4174")
    public void test_resample_olci_to_enmap() throws Exception {
        final List<SpectralResponseFunction> fullSrfList = getFullSrfList("enmap");

        final double[] resampledSpectrum = SpectralResampling.resample(olciInputSpectrum, olciInputWvls, fullSrfList);

        assertNotNull(resampledSpectrum);
        assertEquals(224, resampledSpectrum.length);
        assertEquals(115.17, resampledSpectrum[0], 1.E-2);
        assertEquals(0.0, resampledSpectrum[1], 1.E-2);
        assertEquals(114.32, resampledSpectrum[19], 1.E-2);
        assertEquals(114.59, resampledSpectrum[28], 1.E-2);
        assertEquals(133.63, resampledSpectrum[41], 1.E-2);
        assertEquals(122.11, resampledSpectrum[63], 1.E-2);
        assertEquals(95.75, resampledSpectrum[77], 1.E-2);
        assertEquals(20.55, resampledSpectrum[89], 1.E-2);
        assertEquals(0.0, resampledSpectrum[100], 1.E-2);
        assertEquals(81.01, resampledSpectrum[103], 1.E-2);
        assertEquals(0.0, resampledSpectrum[223], 1.E-4);
    }

    @Test
    @STTM("SNAP-4174")
    public void test_resample_enmap_to_olci() throws Exception {
        final List<SpectralResponseFunction> fullSrfList = getFullSrfList("olci");

        final double[] resampledSpectrum = SpectralResampling.resample(enmapInputSpectrum, enmapInputWvls, fullSrfList);
        assertNotNull(resampledSpectrum);
        assertEquals(21, resampledSpectrum.length);

        final double[] expectedSpectrum = new double[]{
                0.0581, 0.0579, 0.0523, 0.0428, 0.0399, 0.042, 0.0256, 0.019, 0.0184, 0.0178, 0.0406,
                0.1042, 0.0649, 0.0649, 0.086, 0.1116, 0.1019, 0.0973, 0.0684, 0.0224, 0.0428
        };
        assertArrayEquals(expectedSpectrum, resampledSpectrum, 1.E-4);
    }

    @Test
    @STTM("SNAP-4174")
    public void test_resample_prisma_to_enmap() throws Exception {
        final List<SpectralResponseFunction> fullSrfList = getFullSrfList("enmap");

        final double[] resampledSpectrum = SpectralResampling.resample(prismaInputSpectrum, prismaInputWvls, fullSrfList);

        assertNotNull(resampledSpectrum);
        assertEquals(224, resampledSpectrum.length);
        assertEquals(0.0635, resampledSpectrum[0], 1.E-4);
        assertEquals(0.0551, resampledSpectrum[1], 1.E-4);
        assertEquals(0.0424, resampledSpectrum[19], 1.E-4);
        assertEquals(0.0482, resampledSpectrum[28], 1.E-4);
        assertEquals(0.0343, resampledSpectrum[41], 1.E-4);
        assertEquals(0.1023, resampledSpectrum[63], 1.E-4);
        assertEquals(0.0821, resampledSpectrum[77], 1.E-4);
        assertEquals(0.0140, resampledSpectrum[89], 1.E-4);
        assertEquals(0.0708, resampledSpectrum[100], 1.E-4);
        assertEquals(0.0624, resampledSpectrum[103], 1.E-4);
        assertEquals(0.0, resampledSpectrum[223], 1.E-4);
    }

    @Test
    @STTM("SNAP-4174")
    public void test_resample_enmap_to_prisma() throws Exception {
        final List<SpectralResponseFunction> fullSrfList = getFullSrfList("prisma");

        final double[] resampledSpectrum = SpectralResampling.resample(enmapInputSpectrum, enmapInputWvls, fullSrfList);

        assertNotNull(resampledSpectrum);
        assertEquals(234, resampledSpectrum.length);
        assertEquals(0.0581, resampledSpectrum[0], 1.E-4);
        assertEquals(0.0579, resampledSpectrum[1], 1.E-4);
        assertEquals(0.0445, resampledSpectrum[19], 1.E-4);
        assertEquals(0.0251, resampledSpectrum[28], 1.E-4);
        assertEquals(0.1062, resampledSpectrum[41], 1.E-4);
        assertEquals(0.0180, resampledSpectrum[63], 1.E-4);
        assertEquals(0.0370, resampledSpectrum[77], 1.E-4);
        assertEquals(0.0477, resampledSpectrum[89], 1.E-4);
        assertEquals(0.0336, resampledSpectrum[100], 1.E-4);
        assertEquals(0.0, resampledSpectrum[103], 1.E-4);
        assertEquals(0.0, resampledSpectrum[233], 1.E-4);
    }

    private List<SpectralResponseFunction> getFullSrfList(String olci) throws URISyntaxException, IOException {
        SpectralResponseFunction srf = new SpectralResponseFunction(olci);

        final URL resource = getClass().getResource("fwhm_" + srf.getID() + ".csv");
        assertNotNull(resource);
        final File csvFile = new File(resource.toURI());
        final CsvTable fwhmTable = SpectralResponseFunction.readFwhmFromCsv(csvFile);

        srf.setFWHMList(fwhmTable);
        final List<SpectralResponseFunction.FWHM> fwhmList = srf.getFwhmList();

        return SpectralResponseFunction.getFullyDefinedSrf(fwhmList);
    }
}
