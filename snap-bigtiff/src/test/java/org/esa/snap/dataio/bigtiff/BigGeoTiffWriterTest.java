/**
 * 
 */
package org.esa.snap.dataio.bigtiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the BigGeoTiff writter
 */
public class BigGeoTiffWriterTest {

    private final static File TEST_DIR = new File("test_data_biggeotiff_writer");

    private Product outProduct;

    @Before
    public void setup() {
        if (!TEST_DIR.mkdirs()) {
            fail("unable to create test directory");
        }
        final int width = 14;
        final int height = 14;
        outProduct = new Product("P", "T", width, height);
        final Band bandInt16 = outProduct.addBand("B1", ProductData.TYPE_INT16);
        bandInt16.setDataElems(createShortData(getProductSize(), 23));
        ImageManager.getInstance().getSourceImage(bandInt16, 0);
    }

    @After
    public void tearDown() {
        if (!FileUtils.deleteTree(TEST_DIR)) {
            fail("unable to delete test directory");
        }
    }

    
    /**
     * Verify SNAP-3631
     */
    @Test
    public void testWriteProductIssue3631() {
    	// verify the name of the product
    	assertEquals("P", outProduct.getName());
    	
        // save the product with the name of a band
    	final File location = new File(TEST_DIR, "B1.tif");
        final String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;
        try {
			ProductIO.writeProduct(outProduct, location.getAbsolutePath(), bigGeoTiffFormatName);
		} catch (IOException e) {
			fail("Error writing product: " + e.getMessage());
		}

        try(final Product readProd = ProductIO.readProduct(location, bigGeoTiffFormatName)) {
			
			// check that the name was updated
			assertEquals("B1", readProd.getName());
			
		} catch (IOException e) {
			fail("Error reading product: " + e.getMessage());
		}
    }
    

    private int getProductSize() {
        final int w = outProduct.getSceneRasterWidth();
        final int h = outProduct.getSceneRasterHeight();
        return w * h;
    }
    
    private static short[] createShortData(final int size, final int offset) {
        final short[] shorts = new short[size];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) (i + offset);
        }
        return shorts;
    }
}
