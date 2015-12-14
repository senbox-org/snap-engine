/*
 * $Id: UtilsTest.java,v 1.1 2007/03/27 12:51:42 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {


    public void testSeasonalFactorComputation() {
        double meanEarthSunDist = 149.60e+06 * 1000;
        final double sunEarthDistanceSquare = meanEarthSunDist * meanEarthSunDist;

        final double vStart = Utils.computeSeasonalFactor(0, sunEarthDistanceSquare);
        assertEquals(1, vStart, 0.05);
        assertTrue(vStart < 1.0);

        final double vMid = Utils.computeSeasonalFactor(0.5 * 365, sunEarthDistanceSquare);
        assertEquals(1, vMid, 0.05);
        assertTrue(vMid > 1.0);

        final double vEnd = Utils.computeSeasonalFactor(365, sunEarthDistanceSquare);
        assertEquals(1, vEnd, 0.05);
        assertTrue(vEnd < 1.0);
    }
}

