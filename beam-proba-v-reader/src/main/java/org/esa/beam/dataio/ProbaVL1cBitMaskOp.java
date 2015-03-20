package org.esa.beam.dataio;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;

/**
 * This Operator extracts and interprets the relevant bit information stored in SM mask.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Probav.L1c.Bitmask",
        description = "extracts and interprets the relevant bit information stored in L1c 'Q' mask",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2015 by Brockmann Consult",
        internal = true)
public class ProbaVL1cBitMaskOp extends PixelOperator {

    private static final int SRC_FLAG = 0;
    private static final int TRG_FLAG = 0;

    @SourceProduct
    private Product sourceProduct;

    @Parameter(description = "Source product Quality band name")
    private String sourceQualityBandName;

    @Parameter(description = "Target product Quality flag band name")
    private String targetQualityFlagBandName;


    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        targetProduct.addBand(targetQualityFlagBandName, ProductData.TYPE_INT8);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(SRC_FLAG, sourceQualityBandName);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(TRG_FLAG, targetQualityFlagBandName);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int srcFlagValue = sourceSamples[SRC_FLAG].getInt();
        computeL1cQualityMask(srcFlagValue, targetSamples);
    }

    private void computeL1cQualityMask(int srcValue, WritableSample[] targetSamples) {
        targetSamples[TRG_FLAG].set(ProbaVConstants.Q_CORRECT_BIT_INDEX, isCorrect(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.Q_MISSING_BIT_INDEX, isMissing(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.Q_WAS_SATURATED_BIT_INDEX, wasSaturated(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.Q_BECAME_SATURATED_INDEX, becameSaturated(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.Q_BECAME_NEGATIVE_BIT_INDEX, becameNegative(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.Q_INTERPOLATED_BIT_INDEX, isInterpolated(srcValue));
        targetSamples[TRG_FLAG].set(ProbaVConstants.Q_BORDER_COMPRESSED_INDEX, isBorderCompressed(srcValue));

    }

    private boolean isBorderCompressed(int srcValue) {
        return srcValue == 0;
    }

    private boolean isInterpolated(int srcValue) {
        return srcValue == 1;
    }

    private boolean becameNegative(int srcValue) {
        return srcValue == 2;
    }

    private boolean becameSaturated(int srcValue) {
        return srcValue == 3;
    }

    private boolean wasSaturated(int srcValue) {
        return srcValue == 4;
    }

    private boolean isMissing(int srcValue) {
        return srcValue == 5;
    }

    private boolean isCorrect(int srcValue) {
        return srcValue == 6;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ProbaVL1cBitMaskOp.class);
        }
    }
}
