package org.esa.s3tbx.idepix.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.util.OperatorUtils;
import org.esa.s3tbx.meris.brr.LandClassificationOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "idepix.operators.MerisBrrCorrection",
                  version = "2.2",
                  internal = true,
                  authors = "Olaf Danne",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "BRR correction over clouds using CTP.")
public class MerisBrrCorrectionOp extends Operator {

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "brr")
    private Product brrProduct;
    @SourceProduct(alias = "refl")
    private Product rad2reflProduct;
    @SourceProduct(alias = "cloud")
    private Product cloudProduct;
    @SourceProduct(alias = "land")
    private Product landProduct;

    @TargetProduct
    private Product targetProduct;

    private Band invalidBand;

    @Override
    public void initialize() throws OperatorException {
        String productType = l1bProduct.getProductType();
        productType = productType.substring(0, productType.indexOf("_1")) + "_1N";

        targetProduct = OperatorUtils.createCompatibleProduct(l1bProduct, "MER", productType);
        for (String bandName : brrProduct.getBandNames()) {
            if (!targetProduct.containsBand(bandName)) {
                if (!brrProduct.getBand(bandName).isFlagBand()) {
                    ProductUtils.copyBand(bandName, brrProduct, targetProduct, true);
                }
            }
        }
        ProductUtils.copyFlagBands(brrProduct, targetProduct, true);

        BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
        bandDescriptor.name = "l1_invalid_mask";
        bandDescriptor.expression = "l1_flags.INVALID";
        bandDescriptor.type = ProductData.TYPESTRING_INT8;
        BandMathsOp bandArithmeticOp = new BandMathsOp();
        bandArithmeticOp.setSourceProduct(l1bProduct);
        bandArithmeticOp.setTargetBandDescriptors(bandDescriptor);
        invalidBand = bandArithmeticOp.getTargetProduct().getBandAt(0);

    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        final int bandNumber = band.getSpectralBandIndex() + 1;

        Tile brrTile = getSourceTile(brrProduct.getBand("brr_" + bandNumber), rectangle);
        Tile rad2reflTile = getSourceTile(rad2reflProduct.getBand("rho_toa_" + bandNumber), rectangle);
        Tile isInvalid = getSourceTile(invalidBand, rectangle);

        Tile surfacePressureTile = getSourceTile(cloudProduct.getBand(MerisClassificationOp.PRESSURE_SURFACE),
                                                 rectangle);
        Tile cloudTopPressureTile = getSourceTile(cloudProduct.getBand(MerisClassificationOp.PRESSURE_CTP),
                                                  rectangle);
        Tile cloudFlagsTile = getSourceTile(cloudProduct.getBand(MerisClassificationOp.CLOUD_FLAGS), rectangle);
        Tile landFlagsTile = getSourceTile(landProduct.getBand(LandClassificationOp.LAND_FLAGS), rectangle);

        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                final float brrCorr;
                if (isInvalid.getSampleBoolean(x, y)) {
                    brrCorr = -1.0f;
                } else {
                    if (cloudFlagsTile.getSampleBit(x, y, MerisClassificationOp.F_CLOUD)) {
                        final float surfacePressure = surfacePressureTile.getSampleFloat(x, y);
                        final float cloudTopPressure = cloudTopPressureTile.getSampleFloat(x, y);
                        final float rad2refl = rad2reflTile.getSampleFloat(x, y);
                        brrCorr = rad2refl * cloudTopPressure / surfacePressure;
                    } else if (landFlagsTile.getSampleBit(x, y, LandClassificationOp.F_ICE)) {
                        brrCorr = rad2reflTile.getSampleFloat(x, y);
                    } else {
                        // leave original value
                        brrCorr = brrTile.getSampleFloat(x, y);
                    }
                }
                targetTile.setSample(x, y, brrCorr);
            }
            checkForCancellation();
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisBrrCorrectionOp.class);
        }
    }
}
