package org.esa.s3tbx.fu;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class FuAlgoFactoryTest {


    @Test
    public void testCreationMeris() throws Exception {
        final double[] spectrum = new double[]{0.222, 0.219, 0.277, 0.219, 0.235, 0.260, 0.281, 0.287, 0.273};
        final FuAlgoFactory algoFactory = new FuAlgoFactory(Instrument.MERIS);
        final FuAlgo algo = algoFactory.create();
        FuResult result = algo.compute(spectrum);
        assertEquals(20, result.getFuValue());
        assertEquals(25.92721922205948, result.getHueAngle(), 1e-8);
    }

    @Test
    public void testCreationModis() throws Exception {
        final double[] spectrum = new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0};
        final FuAlgoFactory algoFactory = new FuAlgoFactory(Instrument.MODIS);
        final FuAlgo algo = algoFactory.create();
        FuResult result = algo.compute(spectrum);

        assertEquals(18, result.getFuValue());
        assertEquals(34.596036004742274, result.getHueAngle(), 1e-8);
    }


    @Test
    public void testCreationSeaWifs() throws Exception {
        final double[] spectrum = new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        final FuAlgoFactory algoFactory = new FuAlgoFactory(Instrument.SEAWIFS);
        final FuAlgo algo = algoFactory.create();
        FuResult result = algo.compute(spectrum);

        assertEquals(17, result.getFuValue());
        assertEquals(34.97265830605461, result.getHueAngle(), 1e-8);
    }


    @Test
    public void testCreationOLCI() throws Exception {
        final double[] spectrum = new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0};
        final FuAlgoFactory algoFactory = new FuAlgoFactory(Instrument.OLCI);
        final FuAlgo algo = algoFactory.create();
        FuResult result = algo.compute(spectrum);

        assertEquals(17, (int) result.getFuValue());
        assertEquals(39.07673192537948, result.getHueAngle(), 1e-8);
    }


}
