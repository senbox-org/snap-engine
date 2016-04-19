package org.esa.s3tbx.dataio.avhrr.metop;

import org.esa.s3tbx.dataio.avhrr.metop.MetopRad2ReflFactorCalibrator;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/**
 * @author Marco Peters
 */
public class MetopRad2ReflFactorCalibratorTest {

    @Test
    public void testCalibration() {
        MetopRad2ReflFactorCalibrator calibrator = new MetopRad2ReflFactorCalibrator(139.8732, 1);
        assertEquals(16.418472, calibrator.calibrate((float) (7.31)), 1.0e-6f);
    }
}