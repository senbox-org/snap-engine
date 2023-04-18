package org.esa.snap.gpf;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.GPF;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class AddElevationOpTest {

    private static final String PRODUCT_NAME =
            "subset_0_of_MER_FR__1PNEPA20060730_093924_000000982049_00480_23079_0746.dim";
    private static final String DEM_NAME = "45N015E_5M.ACE2";

    @Test
    public void testAddingExternalDEM() throws IOException, URISyntaxException {
        final Map<String, Object> parametersMap = new HashMap<>();
        final File externalDEMFile = new File(getResourcePath(DEM_NAME));
        parametersMap.put("demName", "External DEM");
        parametersMap.put("externalDEMFile", externalDEMFile);
        parametersMap.put("externalDEMNoDataValue", -500.0);
        parametersMap.put("demResamplingMethod", ResamplingFactory.BILINEAR_INTERPOLATION_NAME);
        final String pathToProduct = getResourcePath(PRODUCT_NAME);
        final Product sourceProduct = ProductIO.readProduct(pathToProduct);
        final Product elevationProduct = GPF.createProduct("AddElevation", parametersMap, sourceProduct);

        final Band elevationBand = elevationProduct.getBand("elevation");
        assertNotNull(elevationBand);
        assertEquals(-500.0, elevationBand.getNoDataValue(), 1e-8);
        final float sampleFloat = elevationBand.getSampleFloat(37, 29);
        assertEquals(38.6651268, sampleFloat, 1e-8);
    }

    private String getResourcePath(String name) throws URISyntaxException {
        URL url = AddElevationOpTest.class.getResource(name);
        URI uri = new URI(url.toString());
        return uri.getPath();
    }
} 