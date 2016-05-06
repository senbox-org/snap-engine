package org.esa.s3tbx.olci.radiometry.operator;


import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionOpTest {

    @Test
    public void testCalExponential() {
        GaseousAbsorptionOp gaseousAbsorptionOp = new GaseousAbsorptionOp();
        assertEquals(7.38905609893065, gaseousAbsorptionOp.calExponential(2));
        assertEquals(54.598150033144236, gaseousAbsorptionOp.calExponential(2, 2));
        assertEquals(2980.9579870417283, gaseousAbsorptionOp.calExponential(2, 2, 2));
    }
}