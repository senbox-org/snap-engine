package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SRTMGeoTiffTest {

    @Test
    public void testReadProduct() throws Exception {
        URL resource = getClass().getResource("srtm_05_09.zip");
        assertNotNull(resource);

        File file = new File(resource.toURI());
        GeoTiffProductReader productReader = buildProductReader();

        Product product = productReader.readProductNodes(file, null);
        assertNotNull(product);

        Band band = product.getBandAt(0);
        assertNotNull(band);

        int x = 5000;
        int y = 100;
        int w = 5;
        int h = 5;
        final int[] pixels = new int[w*h];
        band.readPixels(x,y, w, h, pixels);

        for(int i=0; i < pixels.length; ++i) {
            System.out.print(pixels[i] + " ");
        }

        assertEquals(145, pixels[0]);
        assertEquals(149, pixels[1]);
    }

    private static GeoTiffProductReader buildProductReader() {
        GeoTiffProductReaderPlugIn readerPlugIn = new GeoTiffProductReaderPlugIn();
        return  new GeoTiffProductReader(readerPlugIn);
    }
}
