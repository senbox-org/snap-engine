package org.esa.snap.raster.gpf.texture;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.awt.image.Raster;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(LongTestRunner.class)
public class GLCMOp_IntegrationTest {

    private final static String SOURCE_PRODUCT_NAME =
            "subset_0_of_S2B_MSIL1C_20170718T101029_N0205_R022_T34VCL_20170718T101346_idepix_c2rcc_normal.dim";

    @Test
    public void testGLCMOp_Integration() throws IOException, URISyntaxException {
        URL resource = getClass().getResource(SOURCE_PRODUCT_NAME);
        final URI uri = new URI(resource.toString());
        Product sourceProduct = ProductIO.readProduct(uri.getPath());

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("sourceBands", "conc_chl,conc_chl_masked");

        Product glcmProduct = GPF.createProduct("GLCM", parameters, sourceProduct);

        assertNotNull(glcmProduct);
        assertTrue(glcmProduct.containsBand("conc_chl_Contrast"));
        assertTrue(glcmProduct.containsBand("conc_chl_Dissimilarity"));
        assertTrue(glcmProduct.containsBand("conc_chl_Homogeneity"));
        assertTrue(glcmProduct.containsBand("conc_chl_ASM"));
        assertTrue(glcmProduct.containsBand("conc_chl_Energy"));
        assertTrue(glcmProduct.containsBand("conc_chl_MAX"));
        assertTrue(glcmProduct.containsBand("conc_chl_Entropy"));
        assertTrue(glcmProduct.containsBand("conc_chl_GLCMMean"));
        assertTrue(glcmProduct.containsBand("conc_chl_GLCMVariance"));
        assertTrue(glcmProduct.containsBand("conc_chl_GLCMCorrelation"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_Contrast"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_Dissimilarity"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_Homogeneity"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_ASM"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_Energy"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_MAX"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_Entropy"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_GLCMMean"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_GLCMVariance"));
        assertTrue(glcmProduct.containsBand("conc_chl_masked_GLCMCorrelation"));

        Band concChlContrastBand = glcmProduct.getBand("conc_chl_Contrast");
        Raster concChlContrastData = concChlContrastBand.getSourceImage().getData();
        assertEquals(0.0, concChlContrastData.getSampleDouble(5, 5, 0), 1e-8);
        assertEquals(15.342857360839844, concChlContrastData.getSampleDouble(200, 300, 0), 1e-8);
    }
}