package org.esa.s3tbx.olci.radiometry.operator;


import org.esa.s3tbx.olci.radiometry.smilecorr.GaseousAbsorptionAlgorithm;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionAlgorithmTest {

    @Test
    public void testCalExponential() {
        GaseousAbsorptionAlgorithm gaseousAbsorptionAlgorithm = new GaseousAbsorptionAlgorithm();
        assertEquals(3.3546262790251185E-4, gaseousAbsorptionAlgorithm.calExponential(2, 2, 2), 1e-8);
    }
}