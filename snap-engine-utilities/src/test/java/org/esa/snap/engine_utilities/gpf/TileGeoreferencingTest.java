package org.esa.snap.engine_utilities.gpf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;

import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TileGeoreferencingTest {


    // Initialize TileGeoreferencing with valid Product and dimensions
    @Test
    public void initialize_with_valid_product_and_dimensions() {
        Product product = mock(Product.class);
        GeoCoding geoCoding = mock(GeoCoding.class);
        when(product.getSceneGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.isCrossingMeridianAt180()).thenReturn(false);
        TiePointGrid latTPG = mock(TiePointGrid.class);
        TiePointGrid lonTPG = mock(TiePointGrid.class);
        when(OperatorUtils.getLatitude(product)).thenReturn(latTPG);
        when(OperatorUtils.getLongitude(product)).thenReturn(lonTPG);

        TileGeoreferencing tileGeoreferencing = new TileGeoreferencing(product, 0, 0, 10, 10);

        assertNotNull(tileGeoreferencing);
    }

    // Retrieve GeoPos using getGeoPos with valid x, y coordinates
    @Test
    public void get_geopos_with_valid_coordinates() {
        Product product = mock(Product.class);
        GeoCoding geoCoding = mock(GeoCoding.class);
        when(product.getSceneGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.isCrossingMeridianAt180()).thenReturn(false);
        TiePointGrid latTPG = mock(TiePointGrid.class);
        TiePointGrid lonTPG = mock(TiePointGrid.class);
        when(OperatorUtils.getLatitude(product)).thenReturn(latTPG);
        when(OperatorUtils.getLongitude(product)).thenReturn(lonTPG);

        TileGeoreferencing tileGeoreferencing = new TileGeoreferencing(product, 0, 0, 10, 10);
        GeoPos geoPos = new GeoPos();

        tileGeoreferencing.getGeoPos(5, 5, geoPos);

        assertNotNull(geoPos);
    }

    // Retrieve GeoPos using getGeoPos with valid PixelPos
    @Test
    public void get_geopos_with_valid_pixelpos() {
        Product product = mock(Product.class);
        GeoCoding geoCoding = mock(GeoCoding.class);
        when(product.getSceneGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.isCrossingMeridianAt180()).thenReturn(false);
        TiePointGrid latTPG = mock(TiePointGrid.class);
        TiePointGrid lonTPG = mock(TiePointGrid.class);
        when(OperatorUtils.getLatitude(product)).thenReturn(latTPG);
        when(OperatorUtils.getLongitude(product)).thenReturn(lonTPG);

        TileGeoreferencing tileGeoreferencing = new TileGeoreferencing(product, 0, 0, 10, 10);
        GeoPos geoPos = new GeoPos();
        PixelPos pixelPos = new PixelPos(5, 5);

        tileGeoreferencing.getGeoPos(pixelPos, geoPos);

        assertNotNull(geoPos);
    }

    // Retrieve PixelPos using getPixelPos with valid GeoPos
    @Test
    public void get_pixelpos_with_valid_geopos() {
        Product product = mock(Product.class);
        GeoCoding geoCoding = mock(GeoCoding.class);
        when(product.getSceneGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.isCrossingMeridianAt180()).thenReturn(false);

        TileGeoreferencing tileGeoreferencing = new TileGeoreferencing(product, 0, 0, 10, 10);
        GeoPos geoPos = new GeoPos(45.0, 45.0);
        PixelPos pixelPos = new PixelPos();

        tileGeoreferencing.getPixelPos(geoPos, pixelPos);

        assertNotNull(pixelPos);
    }

    // Handle CrsGeoCoding correctly during initialization
    @Test
    public void handle_crsgeocoding_during_initialization() {
        Product product = mock(Product.class);
        CrsGeoCoding crsGeoCoding = mock(CrsGeoCoding.class);
        when(product.getSceneGeoCoding()).thenReturn(crsGeoCoding);
        when(crsGeoCoding.isCrossingMeridianAt180()).thenReturn(false);

        TileGeoreferencing tileGeoreferencing = new TileGeoreferencing(product, 0, 0, 10, 10);

        assertNotNull(tileGeoreferencing);
    }

    // Handle non-CrsGeoCoding correctly during initialization
    @Test
    public void handle_non_crsgeocoding_during_initialization() {
        Product product = mock(Product.class);
        GeoCoding geoCoding = mock(GeoCoding.class);
        when(product.getSceneGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.isCrossingMeridianAt180()).thenReturn(false);

        TileGeoreferencing tileGeoreferencing = new TileGeoreferencing(product, 0, 0, 10, 10);

        assertNotNull(tileGeoreferencing);
    }

    // Retrieve GeoPos with coordinates outside the tile bounds
    @Test
    public void get_geopos_outside_tile_bounds() {
        Product product = mock(Product.class);
        GeoCoding geoCoding = mock(GeoCoding.class);
        when(product.getSceneGeoCoding()).thenReturn(geoCoding);

        TileGeoreferencing tileGeoreferencing = new TileGeoreferencing(product, 0, 0, 10, 10);
        GeoPos geoPos = new GeoPos();

        tileGeoreferencing.getGeoPos(20, 20, geoPos);

        assertNotNull(geoPos);
    }

    // Retrieve PixelPos with GeoPos crossing the 180-degree meridian
    @Test
    public void get_pixelpos_crossing_180_meridian() {
        Product product = mock(Product.class);
        GeoCoding geoCoding = mock(GeoCoding.class);
        when(product.getSceneGeoCoding()).thenReturn(geoCoding);
        when(geoCoding.isCrossingMeridianAt180()).thenReturn(true);

        TileGeoreferencing tileGeoreferencing = new TileGeoreferencing(product, 0, 0, 10, 10);
        GeoPos geoPos = new GeoPos(-45.0, -170.0); // crossing meridian example
        PixelPos pixelPos = new PixelPos();

        tileGeoreferencing.getPixelPos(geoPos, pixelPos);

        assertNotNull(pixelPos);
    }

    @Test
    public void testBandGeoCoding() {
        Product product = TestUtils.createProduct("type", 10, 10);
        Band band = TestUtils.createBand(product, "band1", 10, 10);

        GeoCoding bandGeoCoding = band.getGeoCoding();
        assertNotNull(bandGeoCoding);

        TileGeoreferencing tileGeoreferencing = new TileGeoreferencing(bandGeoCoding, 0, 0, 10, 10);
        GeoPos geoPos = new GeoPos();

        tileGeoreferencing.getGeoPos(5, 5, geoPos);

        assertNotNull(geoPos);
    }
}