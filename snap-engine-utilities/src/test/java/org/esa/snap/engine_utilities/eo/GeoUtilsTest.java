package org.esa.snap.engine_utilities.eo;

import org.esa.snap.core.datamodel.GeoPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeoUtilsTest {

    @Test
    public void testGeo2xyz() {
        final double[] xyz = new double[3];
        final GeoPos geoPos = new GeoPos(22.8, -122.67);

        GeoUtils.geo2xyz(geoPos, xyz);

        assertEquals(-3175493.9396992284, xyz[0], 1e-8);
        assertEquals(-4952041.666332033, xyz[1], 1e-8);
        assertEquals(2456316.444437012, xyz[2], 1e-8);
    }

    @Test
    public void testGeo2xyz_parametrized() {
        final double[] xyz = new double[3];

        GeoUtils.geo2xyz(22.8, -122.67, 0, xyz, GeoUtils.EarthModel.WGS84);

        assertEquals(-3175493.9396992284, xyz[0], 1e-8);
        assertEquals(-4952041.666332033, xyz[1], 1e-8);
        assertEquals(2456316.444437012, xyz[2], 1e-8);

        GeoUtils.geo2xyz(22.8, -122.67, 1200, xyz, GeoUtils.EarthModel.WGS84);

        assertEquals(-3176091.0853676796, xyz[0], 1e-8);
        assertEquals(-4952972.888462258, xyz[1], 1e-8);
        assertEquals(2456781.4631407545, xyz[2], 1e-8);

        GeoUtils.geo2xyz(22.8, -122.67, 0, xyz, GeoUtils.EarthModel.GRS80);

        assertEquals(-3175493.9397070734, xyz[0], 1e-8);
        assertEquals(-4952041.666344267, xyz[1], 1e-8);
        assertEquals(2456316.4443617957, xyz[2], 1e-8);
    }

    @Test
    public void testXyz2Geo() {
        final double[] xyz = new double[] {-3175493, -4952041, 2456316};
        final GeoPos geoPos = new GeoPos();

        GeoUtils.xyz2geo(xyz, geoPos);
        assertEquals(-122.66999435424805, geoPos.lon, 1e-8);
        assertEquals(22.799999237060547, geoPos.lat, 1e-8);
    }

    @Test
    public void testXyz2Geo_parametrized() {
        final double[] xyz = new double[] {-3175493, -4952041, 2456316};
        final GeoPos geoPos = new GeoPos();

        GeoUtils.xyz2geo(xyz, geoPos, GeoUtils.EarthModel.WGS84);
        assertEquals(-122.66999435424805, geoPos.lon, 1e-8);
        assertEquals(22.799999237060547, geoPos.lat, 1e-8);

        GeoUtils.xyz2geo(xyz, geoPos, GeoUtils.EarthModel.GRS80);
        assertEquals(-122.66999435424805, geoPos.lon, 1e-8);
        assertEquals(22.799999237060547, geoPos.lat, 1e-8);
    }
}
