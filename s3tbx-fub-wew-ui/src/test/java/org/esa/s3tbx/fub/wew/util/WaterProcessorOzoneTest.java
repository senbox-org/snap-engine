package org.esa.s3tbx.fub.wew.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class WaterProcessorOzoneTest {

    @Test
    public void testO3tauAndO3excoeff() throws Exception {
        assertEquals(Double.NaN, WaterProcessorOzone.O3excoeff(2.0), 1e-8);
        assertEquals(9.5, WaterProcessorOzone.O3excoeff(2.050000e+02), 1e-8);
        assertEquals(+1.000000e-06, WaterProcessorOzone.O3excoeff(1.800000e+03), 1e-8);
        assertEquals(17.1, WaterProcessorOzone.O3tau(2.050000e+02, 1.800000e+03), 1e-8);
    }

}