package org.esa.beam.dataio;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.util.BitSetter;

/**
 * This Operator extracts and interprets the relevant bit information stored in SM mask.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Probav.Synthesis.Bitmask",
        description = "extracts and interprets the relevant bit information stored in SM mask",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2015 by Brockmann Consult",
        internal = true)
public class ProbaVSynthesisBitMaskOp extends PixelOperator {

    private static final int SRC_FLAG = 0;
    private static final int TRG_FLAG = 0;

    @SourceProduct
    private Product sourceProduct;

    private static final String TARGET_FLAG_BAND_NAME = ProbaVConstants.SM_FLAG_BAND_NAME;


    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        targetProduct.addBand(TARGET_FLAG_BAND_NAME, ProductData.TYPE_INT16);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(SRC_FLAG, ProbaVConstants.SM_BAND_NAME);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(TRG_FLAG, TARGET_FLAG_BAND_NAME);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int srcFlagValue = sourceSamples[SRC_FLAG].getInt();
        computeSynthesisSmMask(srcFlagValue, targetSamples);
    }

    private void computeSynthesisSmMask(int srcValue, WritableSample[] targetSamples) {
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_CLEAR_BIT_INDEX, isClear(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_UNDEFINED_BIT_INDEX, isUndefined(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_CLOUD_BIT_INDEX, isCloud(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_SNOWICE_INDEX, isSnowIce(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_CLOUD_SHADOW_BIT_INDEX, isCloudShadow(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_LAND_BIT_INDEX, isLand(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_GOOD_SWIR_INDEX, isGoodSwir(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_GOOD_NIR_BIT_INDEX, isGoodNir(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_GOOD_RED_BIT_INDEX, isGoodRed(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_GOOD_BLUE_BIT_INDEX, isGoodBlue(srcValue));

    }

    private boolean isClear(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isUndefined(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isCloud(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isSnowIce(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isCloudShadow(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isLand(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 3);
    }

    private boolean isGoodSwir(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 4);
    }

    private boolean isGoodNir(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 5);
    }

    private boolean isGoodRed(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 6);
    }

    private boolean isGoodBlue(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 7);
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ProbaVSynthesisBitMaskOp.class);
        }
    }
}
