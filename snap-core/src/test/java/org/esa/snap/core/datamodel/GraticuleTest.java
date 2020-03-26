package org.esa.snap.core.datamodel;

import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.esa.snap.core.jexp.impl.AbstractFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraticuleTest {

    @Test
    public void testGeoPosLonComparator() {
        final Graticule.GeoPosLonComparator comparator = new Graticule.GeoPosLonComparator();

        final GeoPos pos_1 = new GeoPos(0.0, 0.0);
        final GeoPos pos_2 = new GeoPos(0.0, 0.0);

        pos_1.lon = 10;
        pos_2.lon = 20;
        assertEquals(-1, comparator.compare(pos_1, pos_2));

        pos_1.lon = -32;
        pos_2.lon = -32;
        assertEquals(0, comparator.compare(pos_1, pos_2));

        pos_1.lon = 106;
        pos_2.lon = 102;
        assertEquals(1, comparator.compare(pos_1, pos_2));
    }

    @Test
    public void testGeoPosLatComparator() {
        final Graticule.GeoPosLatComparator comparator = new Graticule.GeoPosLatComparator();

        final GeoPos pos_1 = new GeoPos(0.0, 0.0);
        final GeoPos pos_2 = new GeoPos(0.0, 0.0);

        pos_1.lat = -30;
        pos_2.lat = -20;
        assertEquals(-1, comparator.compare(pos_1, pos_2));

        pos_1.lat = -32;
        pos_2.lat = -32;
        assertEquals(0, comparator.compare(pos_1, pos_2));

        pos_1.lat = 51;
        pos_2.lat = 50;
        assertEquals(1, comparator.compare(pos_1, pos_2));
    }

    @Test
    public void testTextGlyph_constructAndGetter() {
        final Graticule.TextGlyph glyph = new Graticule.TextGlyph("heffalump", 1.0, 2.0, 3.0);

        assertEquals("heffalump", glyph.getText());
        assertEquals(1.0, glyph.getX(), 1e-8);
        assertEquals(2.0, glyph.getY(), 1e-8);
        assertEquals(3.0, glyph.getAngle(), 1e-8);
    }

    @Test
    public void testLimitLon() {
        assertEquals(142.0, Graticule.limitLon(-578.0), 1e-8);
        assertEquals(26.0, Graticule.limitLon(-334.0), 1e-8);
        assertEquals(170.2, Graticule.limitLon(-189.8), 1e-8);
        assertEquals(-105.7, Graticule.limitLon(-105.7), 1e-8);
        assertEquals(0.0, Graticule.limitLon(0.0), 1e-8);
        assertEquals(56.7, Graticule.limitLon(56.7), 1e-8);
        assertEquals(-176.12, Graticule.limitLon(183.88), 1e-8);
        assertEquals(-14.0, Graticule.limitLon(346.0), 1e-8);
        assertEquals(117.56, Graticule.limitLon(477.56), 1e-8);
    }

    @Test
    public void testCreateTextGlyph() {
        final Graticule.Coord coord_1 = new Graticule.Coord(null, new PixelPos(10, 12));
        final Graticule.Coord coord_2 = new Graticule.Coord(null, new PixelPos(11, 14));

        final Graticule.TextGlyph glyph = Graticule.createTextGlyph("Wassupman", coord_1, coord_2);
        assertEquals("Wassupman", glyph.getText());
        assertEquals(10.0, glyph.getX(), 1e-8);
        assertEquals(12.0, glyph.getY(), 1e-8);
        assertEquals(1.1071487177940904, glyph.getAngle(), 1e-8);
    }

    @Test
    public void testIsCoordPairValid() {
        final PixelPos pxPos_1 = new PixelPos(10, 12);
        final PixelPos pxPos_2 = new PixelPos(11, 14);
        final Graticule.Coord coord_1 = new Graticule.Coord(null, pxPos_1);
        final Graticule.Coord coord_2 = new Graticule.Coord(null, pxPos_2);

        assertTrue(Graticule.isCoordPairValid(coord_1, coord_2));

        pxPos_1.x = Double.NaN;
        assertFalse(Graticule.isCoordPairValid(coord_1, coord_2));

        pxPos_1.x = 14;
        pxPos_2.x = Double.NaN;
        assertFalse(Graticule.isCoordPairValid(coord_1, coord_2));
    }
}
