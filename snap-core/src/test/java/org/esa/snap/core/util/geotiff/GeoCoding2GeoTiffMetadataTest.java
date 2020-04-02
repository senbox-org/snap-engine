package org.esa.snap.core.util.geotiff;

import org.esa.snap.core.datamodel.*;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.AffineTransform;

import static org.esa.snap.core.util.geotiff.GeoTIFFCodes.*;
import static org.junit.Assert.assertEquals;

public class GeoCoding2GeoTiffMetadataTest {

    @Test
    public void testProjectedGeoCoding() throws Exception {
        final CrsGeoCoding geoCoding = createCrsGeoCoding(new Rectangle(0, 0, 1000, 1500));

        final GeoTIFFMetadata metadata = GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(geoCoding, 1000, 1500);
        assertEquals(ModelTypeGeographic, metadata.getGeoShortParam(GTModelTypeGeoKey));
        assertEquals(4326, metadata.getGeoShortParam(GeographicTypeGeoKey));    // EPSG code tb 2020-03-24
        assertEquals("WGS 84", metadata.getGeoAsciiParam(PCSCitationGeoKey));
    }

    @Test
    public void testFallbackGeoCoding() {
        final PixelGeoCoding pixelGeoCoding = createPixelGeoCoding();

        final GeoTIFFMetadata metadata = GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(pixelGeoCoding, 4, 4);
        assertEquals(ModelTypeGeographic, metadata.getGeoShortParam(GTModelTypeGeoKey));
        assertEquals(RasterPixelIsArea, metadata.getGeoShortParam(GTRasterTypeGeoKey));
        assertEquals(4326, metadata.getGeoShortParam(GeographicTypeGeoKey));    // EPSG code tb 2020-03-24

        final double[] modelPixelScale = metadata.getModelPixelScale();
        assertEquals(3, modelPixelScale.length);
        assertEquals(0.59259033203125, modelPixelScale[0], 1e-8);
        assertEquals(0.555999755859375, modelPixelScale[1], 1e-8);
        assertEquals(0.0, modelPixelScale[2], 1e-8);
    }

    // @todo 2 tb/tb code borrowed from CrsGeoCodingTest - refactor this 2020-03-24
    private CrsGeoCoding createCrsGeoCoding(Rectangle imageBounds) throws Exception {
        AffineTransform i2m = new AffineTransform();
        final int northing = 60;
        final int easting = 5;
        i2m.translate(easting, northing);
        final int scaleX = 1;
        final int scaleY = 1;
        i2m.scale(scaleX, -scaleY);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, i2m);
    }

    private PixelGeoCoding createPixelGeoCoding() {
        final float[] latitudes = {36.4774f, 36.6734f, 36.8468f, 37.002f,
                36.8359f, 37.0334f, 37.2082f, 37.3645f,
                37.1941f, 37.3931f, 37.5692f, 37.7268f,
                37.5519f, 37.7525f, 37.93f, 38.0887f
        };

        final float[] longitudes = {-128.5965f, -127.8378f, -127.1493f, -126.5188f,
                -128.7658f, -128.0039f, -127.3126f, -126.6794f,
                -128.9369f, -128.1719f, -127.4776f, -126.8416f,
                -129.1098f, -128.3416f, -127.6443f, -127.0056f
        };
        final Product product = new Product("fake", "we need", 4, 4);
        final Band longitude = new Band("longitude", ProductData.TYPE_FLOAT32, 4, 4);
        longitude.setRasterData(ProductData.createInstance(longitudes));
        final Band latitude = new Band("latitude", ProductData.TYPE_FLOAT32, 4, 4);
        latitude.setRasterData(ProductData.createInstance(latitudes));
        product.addBand(longitude);
        product.addBand(latitude);

        return new PixelGeoCoding(latitude, longitude, "", 2);
    }
}
