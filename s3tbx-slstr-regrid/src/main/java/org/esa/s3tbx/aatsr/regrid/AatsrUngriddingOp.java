package org.esa.s3tbx.aatsr.regrid;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;

/**
 * @author Alasdhair Beaton (Telespazio VEGA)
 * @author Philip Beavis (Telespazio VEGA)
 */
@OperatorMetadata(description = "Ungrids ATSR L1B products and extracts geolocation and pixel field of view data.",
        alias = "AATSR.Ungrid", authors = "Alasdhair Beaton, Philip Beavis",
        category = "Raster", label = "AATSR Ungridding"
)
public class AatsrUngriddingOp extends PixelOperator {

    @SourceProduct(description = "The source product")
    Product sourceProduct;

    @TargetProduct
    Product targetProduct;

    @Parameter
    boolean trimProductEndWhereNoADS;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        if (!sourceProduct.getProductType().equals("ATS_TOA_1P")) {
            throw new OperatorException("Product does not have correct type");
        }
        //check source product type
        //read and prepare metadata
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        targetSamples[0].set(sourceSamples[0].getDouble() / 2);
        targetSamples[1].set(sourceSamples[1].getDouble() / 2);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineComputedSample(0, sourceProduct.getBandAt(0));
        sampleConfigurer.defineComputedSample(1, sourceProduct.getBandAt(1));
    }

    @Override
    protected Product createTargetProduct() throws OperatorException {
        final Product targetProduct = super.createTargetProduct();
        targetProduct.addBand("nadir view latitude", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("nadir view longitude", ProductData.TYPE_FLOAT32);
        //change target product dimensions if necessary
        return targetProduct;
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyBands();
        productConfigurer.getTargetProduct().setAutoGrouping("nadir:fward");
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, "nadir view latitude");
        sampleConfigurer.defineSample(1, "nadir view longitude");
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AatsrUngriddingOp.class);
        }
    }
}
