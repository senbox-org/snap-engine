package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Test;

import javax.imageio.stream.FileCacheImageInputStream;
import java.io.File;
import java.net.URI;
import java.net.URL;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class GeoCodingTest {

    @Test
    public void testSmallImageNearGreenwichMeridian() throws Exception {
        final URL resource = getClass().getResource("nearGreenwichMeridian.tif");
        final URI uri = new URI(resource.toString());
        final String filePath = uri.getPath();
        final GeoTiffProductReader reader = new GeoTiffProductReader(new GeoTiffProductReaderPlugIn());
        FileCacheImageInputStream imageInputStream = new FileCacheImageInputStream(resource.openStream(), null);
        GeoTiffImageReader geoTiffImageReader = new GeoTiffImageReader(imageInputStream);
        String defaultProductName = FileUtils.getFilenameWithoutExtension(new File(filePath).getName());
        final Product product = reader.readProduct(geoTiffImageReader, defaultProductName);

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        final GeoPos ul = geoCoding.getGeoPos(new PixelPos(0, 0), null);
        assertEquals(1.92584, ul.lon, 1.0e-5);
        assertEquals(48.28314, ul.lat, 1.0e-5);
        final GeoPos lr = geoCoding.getGeoPos(new PixelPos(49, 49), null);
        assertEquals(2.03596, lr.lon, 1.0e-5);
        assertEquals(48.17303, lr.lat, 1.0e-5);
    }

    @Test
    public void testReadingZip() throws Exception {
        final URL resource = getClass().getResource("nearGreenwichMeridian.zip");
        final URI uri = new URI(resource.toString());
        final String filePath = uri.getPath();
        final GeoTiffProductReader reader = new GeoTiffProductReader(new GeoTiffProductReaderPlugIn());
        final Product product = reader.readProductNodes(filePath, null);

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        final GeoPos ul = geoCoding.getGeoPos(new PixelPos(0, 0), null);
        assertEquals(1.92584, ul.lon, 1.0e-5);
        assertEquals(48.28314, ul.lat, 1.0e-5);
        final GeoPos lr = geoCoding.getGeoPos(new PixelPos(49, 49), null);
        assertEquals(2.03596, lr.lon, 1.0e-5);
        assertEquals(48.17303, lr.lat, 1.0e-5);
    }
}
