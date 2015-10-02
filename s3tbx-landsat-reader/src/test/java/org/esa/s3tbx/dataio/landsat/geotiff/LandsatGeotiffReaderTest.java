package org.esa.s3tbx.dataio.landsat.geotiff;

import java.awt.geom.Point2D;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
public class LandsatGeotiffReaderTest {

    private void assertPointEquals(Point2D expected, Point2D actual) {
        assertEquals(expected.getX(), actual.getX(), 1.0e-6);
        assertEquals(expected.getY(), actual.getY(), 1.0e-6);
    }
}