/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s3tbx.processor.flh_mci;

import org.esa.snap.core.gpf.OperatorException;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BaselineAlgorithmTest {

    private BaselineAlgorithm algo;

    @Before
    public void setUp() {
        algo = new BaselineAlgorithm();
    }

    /**
     * Tests the functionality of setWavelengths()
     */
    @org.junit.Test
    public void testSetWavelengths() {
        float correctLow = 650.f;
        float correctSignal = 700.f;
        float correctHigh = 800.f;

        // when correct settings - nothing should happen
        try {
            algo.setWavelengths(correctLow, correctHigh, correctSignal);
        } catch (OperatorException e) {
            fail("no exception expected!");
        }

        // when setting low and signal the same - exception
        try {
            algo.setWavelengths(correctLow, correctHigh, correctLow);
            fail("exception expected!");
        } catch (OperatorException ignored) {
        }

        // when setting high and low the same - exception
        try {
            algo.setWavelengths(correctLow, correctLow, correctSignal);
            fail("exception expected!");
        } catch (OperatorException ignored) {
        }

        // when using negative wavelengths - exception
        try {
            algo.setWavelengths(-correctLow, correctHigh, correctSignal);
            fail("exception expected!");
        } catch (OperatorException ignored) {
        }
        try {
            algo.setWavelengths(correctLow, -correctHigh, correctSignal);
            fail("exception expected!");
        } catch (OperatorException ignored) {
        }
        try {
            algo.setWavelengths(correctLow, correctHigh, -correctSignal);
            fail("exception expected!");
        } catch (OperatorException ignored) {
        }
    }

    /**
     * Tests the cloud correction factor default value constant for correct value
     */
    @org.junit.Test
    public void testDefaultCloudCorrectionFactor() {
        assertEquals(1.005f, BaselineAlgorithm.DEFAULT_CLOUD_CORRECT, 1e-6);
    }

}

