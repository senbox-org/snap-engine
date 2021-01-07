package org.esa.snap.change.detection;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import com.bc.ceres.core.ProgressMonitor;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Vector;

public class ChangeVectorAnalysisOpTest {
    @Test
    public void testChangeVectorAnalysisOp() {
        Float[] band1_t1_pixels = { Float.NaN, 10.0f, 0.0f, 0.0f, 1.0f };
        Float[] band2_t1_pixels = { 3.0f, 15.0f, 0.0f, 0.0f, 2.0f };
        final Product produit_t1 = createTestProduct("produit_t1", band1_t1_pixels, band2_t1_pixels);
        Float[] band1_t2_pixels = { 5.0f, 10.0f, 0.2f, 0.0f, 2.0f };
        Float[] band2_t2_pixels = { 3.0f, 15.0f, 0.1f, 0.0f, 1.0f };
        final Product produit_t2 = createTestProduct("produit_t2", band1_t2_pixels, band2_t2_pixels);
        ChangeVectorAnalysisOp op = new ChangeVectorAnalysisOp();
        op.setParameterDefaultValues();
        op.setSourceProducts(produit_t1, produit_t2);
        op.setParameter("sourceBand1", "band1");
        op.setParameter("sourceBand2", "band2");
        op.doExecute(ProgressMonitor.NULL);
        Product targetProduct = op.getTargetProduct();
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