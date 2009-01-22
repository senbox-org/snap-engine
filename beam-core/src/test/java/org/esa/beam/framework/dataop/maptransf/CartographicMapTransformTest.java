package org.esa.beam.framework.dataop.maptransf;

import junit.framework.TestCase;

import java.awt.geom.Point2D;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.util.UtilConstants;

public class CartographicMapTransformTest extends TestCase {

    public void testCartographicMapTransformShift() {
        CentralMeridianTransform t = new CentralMeridianTransform(0.0);
        // ...


        t = new CentralMeridianTransform(10.0);

    }

    public void testShiftLon() {

        double centralMeridian = 0.0;
        assertEquals(0.0, shiftLon(0.0, centralMeridian));
        assertEquals(+10.0, shiftLon(10.0, centralMeridian));
        assertEquals(-10.0, shiftLon(-10.0, centralMeridian));
        assertEquals(-180.0, shiftLon(-180.0, centralMeridian));
        assertEquals(+180.0, shiftLon(+180.0, centralMeridian));
        assertEquals(+170.0, shiftLon(-190.0, centralMeridian));
        assertEquals(-170.0, shiftLon(+190.0, centralMeridian));

        centralMeridian = -10.0;
        assertEquals(+10.0, shiftLon(0.0, centralMeridian));
        assertEquals(+20.0, shiftLon(10.0, centralMeridian));
        assertEquals(0.0, shiftLon(-10.0, centralMeridian));
        assertEquals(-170.0, shiftLon(-180.0, centralMeridian));
        assertEquals(-170.0, shiftLon(+180.0, centralMeridian));
        assertEquals(-180.0, shiftLon(-190.0, centralMeridian));
        assertEquals(-160.0, shiftLon(+190.0, centralMeridian));

        centralMeridian = 10.0;
        assertEquals(-10.0, shiftLon(0.0, centralMeridian));
        assertEquals(0.0, shiftLon(10.0, centralMeridian));
        assertEquals(-20.0, shiftLon(-10.0, centralMeridian));
        assertEquals(+170.0, shiftLon(-180.0, centralMeridian));
        assertEquals(+170.0, shiftLon(+180.0, centralMeridian));
        assertEquals(+160.0, shiftLon(-190.0, centralMeridian));
        assertEquals(+180.0, shiftLon(+190.0, centralMeridian));

        centralMeridian = -180.0;
        assertEquals(+180.0, shiftLon(0.0, centralMeridian));
        assertEquals(-170.0, shiftLon(10.0, centralMeridian));
        assertEquals(+170.0, shiftLon(-10.0, centralMeridian));
        assertEquals(0.0, shiftLon(-180.0, centralMeridian));
        assertEquals(0.0, shiftLon(+180.0, centralMeridian));
        assertEquals(-10.0, shiftLon(-190.0, centralMeridian));
        assertEquals(+10.0, shiftLon(+190.0, centralMeridian));

        centralMeridian = 180.0;
        assertEquals(-180.0, shiftLon(0.0, centralMeridian));
        assertEquals(-170.0, shiftLon(10.0, centralMeridian));
        assertEquals(+170.0, shiftLon(-10.0, centralMeridian));
        assertEquals(0.0, shiftLon(-180.0, centralMeridian));
        assertEquals(0.0, shiftLon(+180.0, centralMeridian));
        assertEquals(-10.0, shiftLon(-190.0, centralMeridian));
        assertEquals(+10.0, shiftLon(+190.0, centralMeridian));
    }

    private double shiftLon(double lon, double centralMeridian) {
        double lon0 = lon - centralMeridian;
        if (lon0 < -180.0) {
            return 360.0 + lon0;
        } else if (lon0 > 180.0) {
            return lon0 - 360.0;
        } else return lon0;
    }

    private static class CentralMeridianTransform extends CartographicMapTransform {
        public CentralMeridianTransform(double centralMeridian) {

            super(centralMeridian, 500000.0, 0.0, 6378137.0);

        }

        protected Point2D forward_impl(float lat, float lon, Point2D mapPoint) {
            mapPoint.setLocation(lon, lat);
                    mapPoint.setLocation(_a * mapPoint.getX() + _x0,
                _a * mapPoint.getY() + _y0);

            return mapPoint;
        }

        protected GeoPos inverse_impl(float x, float y, GeoPos geoPoint) {
            geoPoint.lon = x;
            geoPoint.lat = y;
            return geoPoint;
        }

        public MapTransformDescriptor getDescriptor() {
            return null;
        }

        public double[] getParameterValues() {
            return new double[0];
        }

        public MapTransform createDeepClone() {
            return new CentralMeridianTransform(getCentralMeridian());
        }
    }
}
