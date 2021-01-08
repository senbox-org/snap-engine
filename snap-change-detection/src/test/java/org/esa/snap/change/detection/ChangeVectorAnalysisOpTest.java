package org.esa.snap.change.detection;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import com.bc.ceres.core.ProgressMonitor;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public class ChangeVectorAnalysisOpTest {
    @Test
    public void testChangeVectorAnalysisOp() throws Exception {
        Float[] band1_t1_pixels = { Float.NaN, 10.0f, 0.0f, 0.0f, 0.0f };
        Float[] band2_t1_pixels = { 3.0f, 15.0f, 0.0f, 0.0f, 1.0f };
        final Product produit_t1 = createTestProduct("produit_t1", band1_t1_pixels, band2_t1_pixels);
        Float[] band1_t2_pixels = { 5.0f, 10.0f, 0.1f, 0.0f, 0.0f };
        Float[] band2_t2_pixels = { 3.0f, 15.0f, 0.1f, 1.0f, 0.0f };
        final Product produit_t2 = createTestProduct("produit_t2", band1_t2_pixels, band2_t2_pixels);
        ChangeVectorAnalysisOp op = new ChangeVectorAnalysisOp();
        op.setParameterDefaultValues();
        op.setParameter("magnitudeThreshold", "0.2");
        op.setSourceProduct("sourceProduct1", produit_t1);
        op.setSourceProduct("sourceProduct2", produit_t2);
        op.setParameter("sourceBand1", "band1");
        op.setParameter("sourceBand2", "band2");
        op.doExecute(ProgressMonitor.NULL);
        Product targetProduct = op.getTargetProduct();
        //test magnitude result
        Band magnitude_result = targetProduct.getBand("magnitude");
        float[] magnitude_output = new float[5];
        magnitude_result.readPixels(0, 0, 5, 1, magnitude_output, ProgressMonitor.NULL);
        
        

        //test direction result
        Band direction_result = targetProduct.getBand("direction");
        float[] direction_output = new float[5];
        direction_result.readPixels(0, 0, 5, 1, direction_output, ProgressMonitor.NULL);
        assertArrayEquals("Magnitude output test",new float[]{Float.NaN,0.0f,0.0f,1.0f,1.0f}, magnitude_output, 1e-4f);
        assertArrayEquals("Direction output test",new float[]{Float.NaN,0.0f,0.0f,90.0f,270.0f}, direction_output, 1e-4f);

    }

    private static Product createTestProduct(String name, Float[] band1_pixels, Float[] band2_pixels) {
        final Product product = new Product(name, "test", 5, 1);
        Band band1 = new Band("band1", ProductData.TYPE_FLOAT32, 5, 1);
        band1.ensureRasterData();
        band1.setSpectralWavelength(400.0f);
        band1.setSpectralBandIndex(0);
        for (int k=0;k<band1_pixels.length;k++)
            band1.setPixelFloat(k, 0, band1_pixels[k]);
        
        product.addBand(band1);

        Band band2 = new Band("band2", ProductData.TYPE_FLOAT32, 5, 1);
        band2.ensureRasterData();
        for (int k=0;k<band2_pixels.length;k++)
            band2.setPixelFloat(k, 0, band2_pixels[k]);
        band2.setSpectralWavelength(600.0f);
        band2.setSpectralBandIndex(1);
        product.addBand(band2);

        return product;
    }

}