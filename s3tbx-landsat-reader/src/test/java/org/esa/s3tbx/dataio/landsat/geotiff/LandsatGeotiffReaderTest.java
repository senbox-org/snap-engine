package org.esa.s3tbx.dataio.landsat.geotiff;

import org.junit.Test;
import org.opengis.referencing.operation.MathTransform2D;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class LandsatGeotiffReaderTest {

    @Test
    public void testTransform() throws Exception {
        final LandsatGeotiffReader.SceneRasterTransform transform = new LandsatGeotiffReader.SceneRasterTransform(
                new Dimension(7651, 7451), new Dimension(15301, 14901));

        final MathTransform2D forward = transform.getForward();
        assertPointEquals(new Point2D.Double(0, 0), forward.transform(new Point2D.Double(0, 0), null));
        assertPointEquals(new Point2D.Double(15301, 14901), forward.transform(new Point2D.Double(7651, 7451), null));
        assertPointEquals(new Point2D.Double(7650.5, 0), forward.transform(new Point2D.Double(3825.5, 0), null));

        final MathTransform2D inverse = transform.getInverse();
        assertPointEquals(new Point2D.Double(0, 0), inverse.transform(new Point2D.Double(0, 0), null));
        assertPointEquals(new Point2D.Double(7651, 7451), inverse.transform(new Point2D.Double(15301, 14901), null));
        assertPointEquals(new Point2D.Double(3825.5, 0), inverse.transform(new Point2D.Double(7650.5, 0), null));

    }

    private void assertPointEquals(Point2D expected, Point2D actual) {
        assertEquals(expected.getX(), actual.getX(), 1.0e-6);
        assertEquals(expected.getY(), actual.getY(), 1.0e-6);
    }
}