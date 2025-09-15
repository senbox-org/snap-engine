package eu.esa.snap.core.datamodel;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.maptransf.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PixelInfoStringBuilderTest {

    private StringBuilder stringBuilder;

    @Before
    public void setUp(){
        stringBuilder = new StringBuilder();
    }

    @Test
    @STTM("SNAP-3849")
    public void testAddProductName() {
        PixelInfoStringBuilder.addProductName("havanna_moon", stringBuilder);

        assertEquals("Product:\thavanna_moon\n\n",  stringBuilder.toString());
    }

    @Test
    @STTM("SNAP-3849")
    public void testAddPixelLocation() {
        PixelInfoStringBuilder.addPixelLocation(48, 2065, stringBuilder);

        assertEquals("Image-X:\t48\tpixel\nImage-Y:\t2065\tpixel\n",  stringBuilder.toString());
    }

    @Test
    @STTM("SNAP-3849")
    public void testAddPixelLocation_withRasterName() {
        PixelInfoStringBuilder.addPixelLocation(48, 2065, "rastaman", stringBuilder);

        assertEquals("Image-X.rastaman:\t48\tpixel\nImage-Y.rastaman:\t2065\tpixel\n",  stringBuilder.toString());
    }

    @Test
    @STTM("SNAP-3849")
    public void testAddGeoLocation() {
        final GeoCoding geoCoding = mock(GeoCoding.class);
        when(geoCoding.getGeoPos(any(), any())).thenReturn(new GeoPos(-26.45, 18.109));

        final Product product = new Product("testproduct", "Test_Type");

        final Band band = new Band("test_me", ProductData.TYPE_UINT16, 5, 8);
        band.setGeoCoding(geoCoding);
        product.addBand(band);

        PixelInfoStringBuilder.addGeoPosInformation(new PixelPos(2803.5, 1677.5), band, stringBuilder);
        assertEquals("Longitude:\t18°06'32\" E\tdegree\n" +
                "Latitude:\t26°27' S\tdegree\n", stringBuilder.toString());
    }

    @Test
    @STTM("SNAP-3849")
    public void testAddGeoLocation_withMapGeoCoding() {
        final MapProjection projection = MapProjectionRegistry.getProjection(IdentityTransformDescriptor.NAME);
        final MapGeoCoding mapGeoCoding = new MapGeoCoding(new MapInfo(projection,
                28, 29,
                1000, 100,
                5, 5,
                Datum.WGS_84));
        final Product product = new Product("testproduct", "Test_Type");
        product.setSceneGeoCoding(mapGeoCoding);

        final Band band = new Band("test_me", ProductData.TYPE_UINT16, 5, 8);
        band.setGeoCoding(mapGeoCoding);
        product.addBand(band);

        PixelInfoStringBuilder.addGeoPosInformation(new PixelPos(30.5, 32.5), band, stringBuilder);
        assertEquals("Longitude:\t67°30' W\tdegree\n" +
                "Latitude:\t82°30' N\tdegree\n" +
                "Map-X:\t-67.5\tdegree\n" +
                "Map-Y:\t82.5\tdegree\n", stringBuilder.toString());
    }
}
