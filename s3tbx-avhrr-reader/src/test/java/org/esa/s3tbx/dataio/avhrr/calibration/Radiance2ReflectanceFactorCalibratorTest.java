package org.esa.s3tbx.dataio.avhrr.calibration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Marco Peters
 */
public class Radiance2ReflectanceFactorCalibratorTest {

    @Test
    public void testCalibration() {
        Radiance2ReflectanceFactorCalibrator calibrator = new Radiance2ReflectanceFactorCalibrator(0.08488, 139.8732, 1);
        assertEquals(16.418472, calibrator.calibrate((float) (86.12158)), 1.0e-6f);
    }
}