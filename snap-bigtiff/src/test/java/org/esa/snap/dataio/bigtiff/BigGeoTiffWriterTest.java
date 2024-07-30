/**
 * 
 */
package org.esa.snap.dataio.bigtiff;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

import com.bc.ceres.annotation.STTM;

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
        final Band bandInt16_2 = outProduct.addBand("B2", ProductData.TYPE_INT16);
        bandInt16_2.setDataElems(createShortData(getProductSize(), 23));
        ImageManager.getInstance().getSourceImage(bandInt16_2, 0);
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
    @STTM("SNAP-3631")
    public void testWriteProductIssue3631() {
    	// verify the name of the product
    	assertEquals("P", outProduct.getName());
    	// check that the product has two bands named B1 and B2
    	final String[] bandNames = outProduct.getBandNames();
    	assertArrayEquals(new String[] {"B1", "B2"}, bandNames);

    	// verify that two bands with the same name cannot be defined.
    	final Band b2 = outProduct.getBand("B2");
    	assertNotNull(b2);

    	try {
    		b2.setName("B1");
    		fail("Two bands with the same name were defined!");
    	} catch(Exception ex) {
    		if (!(ex instanceof IllegalArgumentException)) {
        		fail("Invalid exception thrown when setting the same name for a band: " + ex.getClass().getName());
    		}
    	}
    	
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
