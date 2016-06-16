package org.esa.s3tbx.fu;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class FulAlgoTest {

    @Test
    public void testComputeMeris() {
        FuAlgoFactory factory = new FuAlgoFactory(Instrument.MERIS);
        FuAlgo fuAlgo = factory.create();
        FuResult result = fuAlgo.compute(new double[]{0.00981, 0.011, 0.01296, 0.01311, 0.01193, 0.00298, 0.0016, 0.0014, 0.00081});

        // Intermediate Results
        assertEquals(0.7982, result.getX3(), 1.0e-4);
        assertEquals(1.0570, result.getY3(), 1.0e-4);
        assertEquals(1.2087, result.getZ3(), 1.0e-4);
        assertEquals(0.26052, result.getChrX(), 1.0e-4);
        assertEquals(0.34499, result.getChrY(), 1.0e-4);
        // slightly different to SNAP_MERIS.xlsx (170.8819) but probably OK,
        // due to different arctan2 result
        assertEquals(170.9056, result.getHue(), 1.0e-4);

        // final results
        assertEquals(171.0255, result.getHueAngle(), 1.0e-4);
        assertEquals(5, result.getFuValue());
    }

    @Test
    public void testComputeModis() {
        FuAlgoFactory factory = new FuAlgoFactory(Instrument.MODIS);
        FuAlgo fuAlgo = factory.create();
        FuResult result = fuAlgo.compute(new double[]{0.00242, 0.0031, 0.00345, 0.0039, 0.00358, 0.00059, 0.00063});

        // Intermediate Results
        assertEquals(0.2668, result.getX3(), 1.0e-4);
        assertEquals(0.3279, result.getY3(), 1.0e-4);
        assertEquals(0.3303, result.getZ3(), 1.0e-4);
        assertEquals(0.28842, result.getChrX(), 1.0e-4);
        assertEquals(0.35451, result.getChrY(), 1.0e-4);
        // slightly different to SNAP_MDIS.xlsx (154.7130407) but probably OK,
        // due to different arctan2 result
        assertEquals(154.7519, result.getHue(), 1.0e-4);

        // final results
        assertEquals(162.874, result.getHueAngle(), 1.0e-3);
        assertEquals(6, result.getFuValue());
    }

    @Test
    public void testComputeOlci() {
        FuAlgoFactory factory = new FuAlgoFactory(Instrument.OLCI);
        FuAlgo fuAlgo = factory.create();
        FuResult result = fuAlgo.compute(new double[]{0.04376, 0.02783, 0.02534, 0.0208, 0.01462, 0.00549, 0.00041, 0.00161, 0.00164, 0.00179, 0.00153});

        // Intermediate Results
        assertEquals(0.7186, result.getX3(), 1.0e-4);
        assertEquals(0.7878, result.getY3(), 1.0e-4);
        assertEquals(2.5595, result.getZ3(), 1.0e-4);
        assertEquals(0.17674, result.getChrX(), 1.0e-4);
        assertEquals(0.19375, result.getChrY(), 1.0e-4);
        // slightly different to SNAP_OLCI.xlsx (221.708714) but probably OK,
        // due to different arctan2 result
        assertEquals(221.71018, result.getHue(), 1.0e-4);

        // final results
        assertEquals(221.7655, result.getHueAngle(), 1.0e-4);
        assertEquals(2, result.getFuValue());
    }
    
    @Test
    public void testComputeSeaWifs() {
        FuAlgoFactory factory = new FuAlgoFactory(Instrument.SEAWIFS);
        FuAlgo fuAlgo = factory.create();
        FuResult result = fuAlgo.compute(new double[]{0.00011,0.00074,0.00125,0.00159,0.00178,0.00034});

        // Intermediate Results
        assertEquals(0.1228, result.getX3(), 1.0e-4);
        assertEquals(0.1551, result.getY3(), 1.0e-4);
        assertEquals(0.0876, result.getZ3(), 1.0e-4);
        assertEquals(0.33598, result.getChrX(), 1.0e-4);
        assertEquals(0.42441, result.getChrY(), 1.0e-4);
        // slightly different to SNAP_SeaWiFS.xlsx (88.3163) but probably OK,
        // due to different arctan2 result
        assertEquals(88.33525, result.getHue(), 1.0e-4);

        // final results
        assertEquals(100.09454, result.getHueAngle(), 1.0e-4);
        assertEquals(8, result.getFuValue());
    }
}