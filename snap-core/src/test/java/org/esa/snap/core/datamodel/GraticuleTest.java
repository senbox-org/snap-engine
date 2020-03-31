package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.math.Range;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("ConstantConditions")
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

    @Test
    public void testGetGeoDelta() {
        final RasterDataNode dataNode = mock(RasterDataNode.class);
        when(dataNode.getRasterWidth()).thenReturn(100);
        when(dataNode.getRasterHeight()).thenReturn(200);

        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.getGeoPos(new PixelPos(50.0, 100.0), null)).thenReturn(new GeoPos(10, 20));
        when(geoCoding.getGeoPos(new PixelPos(51.0, 101.0), null)).thenReturn(new GeoPos(11, 20.5));

        final GeoPos delta = Graticule.getGeoDelta(geoCoding, dataNode);
        assertEquals(0.5, delta.getLon(), 1e-8);
        assertEquals(1.0, delta.getLat(), 1e-8);

        verify(dataNode, times(1)).getRasterWidth();
        verify(dataNode, times(1)).getRasterHeight();
        verifyNoMoreInteractions(dataNode);

        verify(geoCoding, times(1)).getGeoPos(new PixelPos(50.0, 100.0), null);
        verify(geoCoding, times(1)).getGeoPos(new PixelPos(51.0, 101.0), null);
        verifyNoMoreInteractions(geoCoding);
    }

    @Test
    public void testGetGeoDelta_antiMeridian() {
        final RasterDataNode dataNode = mock(RasterDataNode.class);
        when(dataNode.getRasterWidth()).thenReturn(100);
        when(dataNode.getRasterHeight()).thenReturn(200);

        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.getGeoPos(new PixelPos(50.0, 100.0), null)).thenReturn(new GeoPos(11.1, 179.5));
        when(geoCoding.getGeoPos(new PixelPos(51.0, 101.0), null)).thenReturn(new GeoPos(11.2, -179.5));

        final GeoPos delta = Graticule.getGeoDelta(geoCoding, dataNode);
        assertEquals(1.0, delta.getLon(), 1e-8);
        assertEquals(0.1, delta.getLat(), 1e-8);

        verify(dataNode, times(1)).getRasterWidth();
        verify(dataNode, times(1)).getRasterHeight();
        verifyNoMoreInteractions(dataNode);

        verify(geoCoding, times(1)).getGeoPos(new PixelPos(50.0, 100.0), null);
        verify(geoCoding, times(1)).getGeoPos(new PixelPos(51.0, 101.0), null);
        verifyNoMoreInteractions(geoCoding);
    }

    @Test
    public void testGetSensibleDegreeIncrement() {
        assertEquals(30.0, Graticule.getSensibleDegreeIncrement(43.0), 1e-8);
        assertEquals(30.0, Graticule.getSensibleDegreeIncrement(30.1), 1e-8);
        assertEquals(30.0, Graticule.getSensibleDegreeIncrement(29.9), 1e-8);
        assertEquals(25.0, Graticule.getSensibleDegreeIncrement(24.9), 1e-8);
        assertEquals(5.0, Graticule.getSensibleDegreeIncrement(7.3), 1e-8);
        assertEquals(5.0, Graticule.getSensibleDegreeIncrement(4.9), 1e-8);
        assertEquals(3.0, Graticule.getSensibleDegreeIncrement(2.7), 1e-8);
        assertEquals(1.0, Graticule.getSensibleDegreeIncrement(1.1), 1e-8);
        assertEquals(0.8333333333333334, Graticule.getSensibleDegreeIncrement(0.9), 1e-8);
        assertEquals(0.16666666666666666, Graticule.getSensibleDegreeIncrement(0.168), 1e-8);
        assertEquals(0.016666666666666666, Graticule.getSensibleDegreeIncrement(0.023), 1e-8);
        assertEquals(0.016666666666666666, Graticule.getSensibleDegreeIncrement(0.0168), 1e-8);
        assertEquals(0.016666666666666666, Graticule.getSensibleDegreeIncrement(0.0004), 1e-8);
    }

    @Test
    public void testGetDesiredMinorSteps() {
        final RasterDataNode dataNode = mock(RasterDataNode.class);
        when(dataNode.getRasterWidth()).thenReturn(100);
        when(dataNode.getRasterHeight()).thenReturn(200);

        assertEquals(25, Graticule.getDesiredMinorSteps(dataNode));

        verify(dataNode, times(1)).getRasterWidth();
        verify(dataNode, times(1)).getRasterHeight();
        verifyNoMoreInteractions(dataNode);
    }

    @Test
    public void testGetDesiredMinorSteps_small_raster() {
        final RasterDataNode dataNode = mock(RasterDataNode.class);
        when(dataNode.getRasterWidth()).thenReturn(10);
        when(dataNode.getRasterHeight()).thenReturn(20);

        assertEquals(3, Graticule.getDesiredMinorSteps(dataNode));

        verify(dataNode, times(1)).getRasterWidth();
        verify(dataNode, times(1)).getRasterHeight();
        verifyNoMoreInteractions(dataNode);
    }

    @Test
    public void testGetGeoBoundaryStep() {
        final RasterDataNode dataNode = mock(RasterDataNode.class);
        when(dataNode.getRasterWidth()).thenReturn(512);
        when(dataNode.getRasterHeight()).thenReturn(2048);

        final GeoCoding geoCoding = mock(GeoCoding.class);

        assertEquals(10, Graticule.getGeoBoundaryStep(geoCoding, dataNode));

        verify(dataNode, times(1)).getRasterWidth();
        verify(dataNode, times(1)).getRasterHeight();
        verifyNoMoreInteractions(dataNode);
    }

    // @todo 3 tb/tb add tests for TiePointGeoCoding 2020-03-30

    @Test
    public void testGetRanges() {
        final GeoPos[] geoPositions = new GeoPos[5];
        geoPositions[0] = new GeoPos(0.0, 10.0);
        geoPositions[1] = new GeoPos(1.0, 11.0);
        geoPositions[2] = new GeoPos(2.0, 14.0);
        geoPositions[3] = new GeoPos(1.5, 16.0);
        geoPositions[4] = new GeoPos(0.5, 12.0);

        final Range[] ranges = Graticule.getRanges(geoPositions);
        assertEquals(2, ranges.length);

        final Range lonRange = ranges[0];
        assertEquals(10.0, lonRange.getMin(), 1e-8);
        assertEquals(16.0, lonRange.getMax(), 1e-8);

        final Range latRange = ranges[1];
        assertEquals(0.0, latRange.getMin(), 1e-8);
        assertEquals(2.0, latRange.getMax(), 1e-8);
    }

    @Test
    public void testComputeMeridianIntersections_fourPoints() {
        final GeoPos[] geoBoundary = new GeoPos[5];
        geoBoundary[0] = new GeoPos(53.338871, -79.703156);
        geoBoundary[1] = new GeoPos(50.416569, -63.416862);
        geoBoundary[2] = new GeoPos(47.879280, -64.973740);
        geoBoundary[3] = new GeoPos(50.695824, -80.429863);
        geoBoundary[4] = new GeoPos(53.338871, -79.703156); // closing polygon tb 2020-03-31

        final List<GeoPos> result = new ArrayList<>();
        Graticule.computeMeridianIntersections(geoBoundary, -72.5,  result);

        assertEquals(2, result.size());
        assertEquals(-72.5, result.get(0).lon, 1e-8);
        assertEquals(52.04638560184177, result.get(0).lat, 1e-8);

        assertEquals(-72.5, result.get(1).lon, 1e-8);
        assertEquals(49.250778042907655, result.get(1).lat, 1e-8);
    }

    @Test
    public void testComputeMeridianIntersections_eightPoints() {
        final GeoPos[] geoBoundary = new GeoPos[9];
        geoBoundary[0] = new GeoPos(53.338871, -79.703156);
        geoBoundary[1] = new GeoPos(52.284065, -71.996193);
        geoBoundary[2] = new GeoPos(50.416569, -63.416862);
        geoBoundary[3] = new GeoPos(49.150463, -64.214897);
        geoBoundary[4] = new GeoPos(47.879280, -64.973740);
        geoBoundary[5] = new GeoPos(49.666119, -73.145988);
        geoBoundary[6] = new GeoPos(50.695824, -80.429863);
        geoBoundary[7] = new GeoPos(52.017761, -80.072128);
        geoBoundary[8] = new GeoPos(53.338871, -79.703156); // closing polygon tb 2020-03-31

        final List<GeoPos> result = new ArrayList<>();
        Graticule.computeMeridianIntersections(geoBoundary, -72.5,  result);
        assertEquals(2, result.size());
        assertEquals(-72.5, result.get(0).lon, 1e-8);
        assertEquals(52.35301805536591, result.get(0).lat, 1e-8);

        assertEquals(-72.5, result.get(1).lon, 1e-8);
        assertEquals(49.52487554386259, result.get(1).lat, 1e-8);

        result.clear();

        Graticule.computeMeridianIntersections(geoBoundary, -80.0,  result);
        assertEquals(2, result.size());
        assertEquals(-80.0, result.get(0).lon, 1e-8);
        assertEquals(50.63505523606391, result.get(0).lat, 1e-8);

        assertEquals(-80.0, result.get(1).lon, 1e-8);
        assertEquals(52.27601642881305, result.get(1).lat, 1e-8);
    }

    @Test
    public void testComputeParallelIntersections_fourPoints() {
        final GeoPos[] geoBoundary = new GeoPos[5];
        geoBoundary[0] = new GeoPos(53.338871, -79.703156);
        geoBoundary[1] = new GeoPos(50.416569, -63.416862);
        geoBoundary[2] = new GeoPos(47.879280, -64.973740);
        geoBoundary[3] = new GeoPos(50.695824, -80.429863);
        geoBoundary[4] = new GeoPos(53.338871, -79.703156); // closing polygon tb 2020-03-31

        final List<GeoPos> result = new ArrayList<>();
        Graticule.computeParallelIntersections(geoBoundary, 50.0,  result);

        assertEquals(2, result.size());
        assertEquals(-63.67246832296203, result.get(0).lon, 1e-8);
        assertEquals(50.0, result.get(0).lat, 1e-8);

        assertEquals(-76.6114453468932, result.get(1).lon, 1e-8);
        assertEquals(50.0, result.get(1).lat, 1e-8);
    }

    @Test
    public void testComputeParallelIntersections_eightPoints() {
        final GeoPos[] geoBoundary = new GeoPos[9];
        geoBoundary[0] = new GeoPos(53.338871, -79.703156);
        geoBoundary[1] = new GeoPos(52.284065, -71.996193);
        geoBoundary[2] = new GeoPos(50.416569, -63.416862);
        geoBoundary[3] = new GeoPos(49.150463, -64.214897);
        geoBoundary[4] = new GeoPos(47.879280, -64.973740);
        geoBoundary[5] = new GeoPos(49.666119, -73.145988);
        geoBoundary[6] = new GeoPos(50.695824, -80.429863);
        geoBoundary[7] = new GeoPos(52.017761, -80.072128);
        geoBoundary[8] = new GeoPos(53.338871, -79.703156); // closing polygon tb 2020-03-31

        final List<GeoPos> result = new ArrayList<>();
        Graticule.computeParallelIntersections(geoBoundary, 51.0,  result);
        assertEquals(2, result.size());
        assertEquals(-66.09716100179759, result.get(0).lon, 1e-8);
        assertEquals(51.0, result.get(0).lat, 1e-8);

        assertEquals(-80.34754863754551, result.get(1).lon, 1e-8);
        assertEquals(51.0, result.get(1).lat, 1e-8);

        result.clear();

        Graticule.computeParallelIntersections(geoBoundary, 52.0,  result);
        assertEquals(2, result.size());
        assertEquals(-70.69119011886131, result.get(0).lon, 1e-8);
        assertEquals(52.0, result.get(0).lat, 1e-8);

        assertEquals(-80.07693437983127, result.get(1).lon, 1e-8);
        assertEquals(52.0, result.get(1).lat, 1e-8);
    }
}
