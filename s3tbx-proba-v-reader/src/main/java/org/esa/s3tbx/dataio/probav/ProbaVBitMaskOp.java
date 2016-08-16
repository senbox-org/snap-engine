package org.esa.s3tbx.dataio.probav;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.core.util.BitSetter;

/**
 * This Operator extracts and interprets the relevant bit information stored in the Proba-V SM mask.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Probav.Bitmask",
        description = "extracts and interprets the relevant bit information stored in SM mask",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2015 by Brockmann Consult",
        internal = true)
public class ProbaVBitMaskOp extends PixelOperator {

    static final int SRC_FLAG = 0;
    static final int TRG_FLAG = 0;

    @Parameter(defaultValue = "SYNTHESIS", valueSet = {"SYNTHESIS", "L2A"},
            label = " Proba-V product type",
            description = "Proba-V product type (currently SYNTHESIS and L2A products are supported).")
    private String probavProductType;

    @SourceProduct
    Product sourceProduct;

    static final String TARGET_FLAG_BAND_NAME = ProbaVConstants.SM_FLAG_BAND_NAME;


    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        targetProduct.addBand(TARGET_FLAG_BAND_NAME, ProductData.TYPE_INT16);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(SRC_FLAG, ProbaVConstants.SM_BAND_NAME);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(TRG_FLAG, TARGET_FLAG_BAND_NAME);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int srcFlagValue = sourceSamples[SRC_FLAG].getInt();
        if (probavProductType.equals("SYNTHESIS")) {
            computeSynthesisSmMask(srcFlagValue, targetSamples);
        } else {
            computeL2ASmMask(srcFlagValue, targetSamples);
        }
    }

    void computeSynthesisSmMask(int srcValue, WritableSample[] targetSamples) {
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

    void computeL2ASmMask(int srcValue, WritableSample[] targetSamples) {
        computeSynthesisSmMask(srcValue, targetSamples);
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_SWIR_COVERAGE_INDEX, isSwirCoverage(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_NIR_COVERAGE_BIT_INDEX, isNirCoverage(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_RED_COVERAGE_BIT_INDEX, isRedCoverage(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.SM_BLUE_COVERAGE_BIT_INDEX, isBlueCoverage(srcValue));
    }


    static boolean isClear(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    static boolean isUndefined(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    static boolean isCloud(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    static boolean isSnowIce(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                BitSetter.isFlagSet(srcValue, 2));
    }

    static boolean isCloudShadow(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    static boolean isLand(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 3);
    }

    static boolean isGoodSwir(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 4);
    }

    static boolean isGoodNir(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 5);
    }

    static boolean isGoodRed(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 6);
    }

    static boolean isGoodBlue(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 7);
    }

    static boolean isSwirCoverage(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 8);
    }

    static boolean isNirCoverage(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 9);
    }

    static boolean isRedCoverage(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 10);
    }

    static boolean isBlueCoverage(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 11);
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ProbaVBitMaskOp.class);
        }
    }
}
