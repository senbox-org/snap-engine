package org.esa.snap.core.datamodel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void testGetLonCornerTextGlyph_noGeoCoding() {
        final Graticule.TextGlyph glyph = Graticule.getLonCornerTextGlyph(null, new PixelPos(1, 2), new PixelPos(3, 4), true, false);
        assertNull(glyph);
    }

    @Test
    public void testGetLonCornerTextGlyph_invalidPxPos() {
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.getGeoPos(any(PixelPos.class), eq(null))).thenReturn(new GeoPos(12.3, 13.4));

        final Graticule.TextGlyph glyph = Graticule.getLonCornerTextGlyph(geoCoding, new PixelPos(Double.NaN, 3), new PixelPos(4, 5), true, true);
        assertNull(glyph);
    }

    @Test
    public void testGetLonCornerTextGlyph() {
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.getGeoPos(any(PixelPos.class), eq(null))).thenReturn(new GeoPos(13.4, 14.5));

        final Graticule.TextGlyph glyph = Graticule.getLonCornerTextGlyph(geoCoding, new PixelPos(3, 4), new PixelPos(5, 6), true, false);
        assertEquals("14°30' E", glyph.getText());
        assertEquals(3.0, glyph.getX(), 1e-8);
        assertEquals(4.0, glyph.getY(), 1e-8);
        assertEquals(0.7853981633974483, glyph.getAngle(), 1e-8);
    }

    @Test
    public void testGetLatCornerTextGlyph_noGeoCoding() {
        final Graticule.TextGlyph glyph = Graticule.getLatCornerTextGlyph(null, new PixelPos(1, 2), new PixelPos(3, 4), true, false);
        assertNull(glyph);
    }

    @Test
    public void testGetLatCornerTextGlyph_invalidPxPos() {
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.getGeoPos(any(PixelPos.class), eq(null))).thenReturn(new GeoPos(13.4, 14.5));

        final Graticule.TextGlyph glyph = Graticule.getLatCornerTextGlyph(geoCoding, new PixelPos(4, Double.NaN), new PixelPos(5, 6), false, false);
        assertNull(glyph);
    }

    @Test
    public void testGetLatCornerTextGlyph() {
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.getGeoPos(any(PixelPos.class), eq(null))).thenReturn(new GeoPos(14.5, 15.6));

        final Graticule.TextGlyph glyph = Graticule.getLatCornerTextGlyph(geoCoding, new PixelPos(4, 5), new PixelPos(6, 7), true, false);
        assertEquals("14°30' N", glyph.getText());
        assertEquals(4.0, glyph.getX(), 1e-8);
        assertEquals(5.0, glyph.getY(), 1e-8);
        assertEquals(0.7853981633974483, glyph.getAngle(), 1e-8);
    }

    @Test
    public void testCreateSouthernLongitudeTextGlyphs() {
        final Graticule.Coord coord_1 = new Graticule.Coord(new GeoPos(-45.6, 22.5), new PixelPos(127, 1456));
        final Graticule.Coord coord_2 = new Graticule.Coord(new GeoPos(-46.7, 23.6), new PixelPos(227, 1556));
        final List<Graticule.Coord> coordList = new ArrayList<>();
        coordList.add(coord_1);
        coordList.add(coord_2);
        final List<List<Graticule.Coord>> listOfLists = new ArrayList<>();
        listOfLists.add(coordList);

        final List<Graticule.TextGlyph> resultList = new ArrayList<>();

        Graticule.createSouthernLongitudeTextGlyphs(listOfLists, resultList, true, true);

        assertEquals(1, resultList.size());
        final Graticule.TextGlyph glyph = resultList.get(0);
        assertEquals("23.6 E", glyph.getText());
        assertEquals(227.0, glyph.getX(), 1e-8);
        assertEquals(1556.0, glyph.getY(), 1e-8);
        assertEquals(-1.5707963267948966, glyph.getAngle(), 1e-8);
    }

    @Test
    public void testCreateSouthernLongitudeTextGlyphs_notEnoughCoords() {
        final Graticule.Coord coord_1 = new Graticule.Coord(new GeoPos(-45.6, 22.5), new PixelPos(127, 1456));
        final List<Graticule.Coord> coordList = new ArrayList<>();
        coordList.add(coord_1);
        final List<List<Graticule.Coord>> listOfLists = new ArrayList<>();
        listOfLists.add(coordList);

        final List<Graticule.TextGlyph> resultList = new ArrayList<>();

        Graticule.createSouthernLongitudeTextGlyphs(listOfLists, resultList, true, true);

        assertEquals(0, resultList.size());
    }

    @Test
    public void testCreateSouthernLongitudeTickPoints() {
        final Graticule.Coord coord_1 = new Graticule.Coord(new GeoPos(-45.6, 22.5), new PixelPos(127, 1456));
        final Graticule.Coord coord_2 = new Graticule.Coord(new GeoPos(-46.7, 23.6), new PixelPos(227, 1556));
        final List<Graticule.Coord> coordList = new ArrayList<>();
        coordList.add(coord_1);
        coordList.add(coord_2);
        final List<List<Graticule.Coord>> listOfLists = new ArrayList<>();
        listOfLists.add(coordList);

        final List<PixelPos> resultList = new ArrayList<>();

        Graticule.createSouthernLongitudeTickPoints(listOfLists, resultList);
        assertEquals(1, resultList.size());
        final PixelPos pixelPos = resultList.get(0);
        assertEquals(227.0, pixelPos.x, 1e-8);
        assertEquals(1556.0, pixelPos.y, 1e-8);
    }
}
