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

package org.esa.snap.dataio.envisat;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * EnvisatAuxReader Tester.
 *
 * @author lveci
 */
public class EnvisatOrbitReaderTest {

    private final static String doris_por_orbit =
            "org/esa/snap/dataio/testdata/DOR_POR_AXVF-P20080404_014700_20080401_215527_20080403_002327.zip";
    private final static String doris_vor_orbit =
            "org/esa/snap/dataio/testdata/DOR_VOR_AXVF-P20080331_075200_20080301_215527_20080303_002327.zip";

    @Test
    public void testPOROrbitFiles() throws IOException {

        final EnvisatOrbitReader reader = new EnvisatOrbitReader();

        reader.readProduct(doris_por_orbit);

        final EnvisatOrbitReader.OrbitVector orb = getOrbitData(reader);

        ProductData.UTC utc = new ProductData.UTC(orb.utcTime);
        assertEquals("01-APR-2008 21:55:27.000000", utc.format());
        assertEquals(-3300453.451, orb.xPos, 1.0e-8);
        assertEquals(881817.654, orb.yPos, 1.0e-8);
        assertEquals(-6304026.222, orb.zPos, 1.0e-8);
        assertEquals(6673.625193, orb.xVel, 1.0e-8);
        assertEquals(880.089573, orb.yVel, 1.0e-8);
        assertEquals(-3372.728885, orb.zVel, 1.0e-8);
    }

    @Test
    public void testVOROrbitFiles() throws IOException {

        final EnvisatOrbitReader reader = new EnvisatOrbitReader();

        reader.readProduct(doris_vor_orbit);

        final EnvisatOrbitReader.OrbitVector orb = getOrbitData(reader);

        ProductData.UTC utc = new ProductData.UTC(orb.utcTime);
        assertEquals("01-MAR-2008 21:55:27.000000", utc.format());
        assertEquals(6494931.106, orb.xPos, 1.0e-8);
        assertEquals(578715.148, orb.yPos, 1.0e-8);
        assertEquals(-2977719.455, orb.zPos, 1.0e-8);
        assertEquals(3188.730641, orb.xVel, 1.0e-8);
        assertEquals(-1416.295158, orb.yVel, 1.0e-8);
        assertEquals(6692.698996, orb.zVel, 1.0e-8);
    }

    @Test
    public void testInterpolation() throws Exception {

        final EnvisatOrbitReader reader = new EnvisatOrbitReader();

        reader.readProduct(doris_vor_orbit);
        reader.readOrbitData();
        
        final double utc1 = reader.getOrbitVector(1).utcTime;
        final double utc2 = reader.getOrbitVector(2).utcTime;
        final double utc = 0.3*utc1 + 0.7*utc2;
        EnvisatOrbitReader.OrbitVector orb = reader.getOrbitVector(utc);
        /*
        System.out.println("orb.xPos = " + orb.xPos);
        System.out.println("orb.yPos = " + orb.yPos);
        System.out.println("orb.zPos = " + orb.zPos);
        System.out.println("orb.xVel = " + orb.xVel);
        System.out.println("orb.yVel = " + orb.yVel);
        System.out.println("orb.zVel = " + orb.zVel);
        */
        assertEquals(6782111.692571748, orb.xPos, 1.0e-8);
        assertEquals(429053.84925767, orb.yPos, 1.0e-8);
        assertEquals(-2279551.9462241037, orb.zPos, 1.0e-8);
        assertEquals(2436.6730959241404, orb.xVel, 1.0e-8);
        assertEquals(-1513.6396683722035, orb.yVel, 1.0e-8);
        assertEquals(6984.008122409866, orb.zVel, 1.0e-8);
    }

    /**
     * Regression guard for the EnvisatOrbitReader shared-state race (see DorisOrbitFile):
     * the reader holds per-product mutable state (_productFile, dataRecords, recordTimes), so each
     * product must use its own instance. This test loads two different orbit files into two
     * independently-constructed readers and hammers both concurrently, asserting that neither ever
     * returns the other's data. It would fail if the reader were changed to keep that state in
     * shared/static fields again.
     */
    @Test
    @STTM("SNAP-4217")
    public void testConcurrentReadersAreIsolated() throws Exception {

        final EnvisatOrbitReader porReader = new EnvisatOrbitReader();
        porReader.readProduct(doris_por_orbit);
        porReader.readOrbitData();

        final EnvisatOrbitReader vorReader = new EnvisatOrbitReader();
        vorReader.readProduct(doris_vor_orbit);
        vorReader.readOrbitData();

        // first-record X positions captured from testPOROrbitFiles / testVOROrbitFiles
        final double porExpectedXPos = -3300453.451;
        final double vorExpectedXPos = 6494931.106;

        final int iterations = 1000;
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<?> porTask = executor.submit(() -> {
                for (int i = 0; i < iterations; i++) {
                    assertEquals(porExpectedXPos, porReader.getOrbitVector(0).xPos, 1.0e-8);
                }
            });
            final Future<?> vorTask = executor.submit(() -> {
                for (int i = 0; i < iterations; i++) {
                    assertEquals(vorExpectedXPos, vorReader.getOrbitVector(0).xPos, 1.0e-8);
                }
            });
            // surfaces any assertion failure thrown on a worker thread
            porTask.get();
            vorTask.get();
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    static EnvisatOrbitReader.OrbitVector getOrbitData(final EnvisatOrbitReader reader) throws IOException {

        // get the data
        reader.readOrbitData();

        final int numRecords = reader.getNumRecords();

        EnvisatOrbitReader.OrbitVector orb = reader.getOrbitVector(0);
        assertNotNull(orb);

        EnvisatOrbitReader.OrbitVector orb2 = reader.getOrbitVector(numRecords - 1);
        assertNotNull(orb2);

        return orb;
    }

}
