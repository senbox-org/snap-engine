package org.esa.s3tbx.idepix.algorithms.vgt;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.RectangleExtender;

import java.awt.*;

/**
 * Operator used to consolidate Idepix classification flag for VGT:
 * - coastline refinement
 * - cloud shadow (from Fronts)
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Vgt.Postprocess",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Refines the VGT pixel classification over both land and water.")
public class VgtPostProcessOp extends Operator {
    //    @Parameter(defaultValue = "true",
//            label = " Compute cloud shadow",
//            description = " Compute cloud shadow with latest 'fronts' algorithm")
    private boolean computeCloudShadow = true;   // always done currently

    //    @Parameter(defaultValue = "true",
//               label = " Refine pixel classification near coastlines",
//               description = "Refine pixel classification near coastlines. ")
    private boolean refineClassificationNearCoastlines = false; // not yet required

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "vgtCloud")
    private Product vgtCloudProduct;

//    @SourceProduct(alias = "urban", optional = true,
//            label = "VGT urban product (ignored for all other sensors than Proba-V or VGT)",
//            description = "The VGT urban product (ignored for all other sensors than Proba-V or VGT).")
//    private Product vgtUrbanProduct;

    private Band origCloudFlagBand;
    private Band origSmFlagBand;
//    private Band blueBand;
//    private Band redBand;
//    private Band nirBand;
//    private Band swirBand;

//    private Band urbanBand;

    private RectangleExtender rectCalculator;

    @Override
    public void initialize() throws OperatorException {

        Product finalVgtCloudProduct = vgtCloudProduct;
//        if (vgtUrbanProduct != null) {
//            // collocate VGT  with urban (3600x3600)
//            CollocateOp collocateOp = new CollocateOp();
//            collocateOp.setParameterDefaultValues();
//            collocateOp.setMasterProduct(vgtCloudProduct);
//            collocateOp.setSlaveProduct(vgtUrbanProduct);
//            collocateOp.setRenameMasterComponents(false);
//            collocateOp.setRenameSlaveComponents(false);
//            finalVgtCloudProduct = collocateOp.getTargetProduct();
//        }

        Product postProcessedCloudProduct = createTargetProduct(vgtCloudProduct,
                                                                "postProcessedCloud", "postProcessedCloud");

        origCloudFlagBand = finalVgtCloudProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        origSmFlagBand = l1bProduct.getBand("SM");
        //JM&GK 20160212 Todo
//        blueBand = finalVgtCloudProduct.getBand("B0");
//        redBand = finalVgtCloudProduct.getBand("B2");
//        nirBand = finalVgtCloudProduct.getBand("B3");
//        swirBand = finalVgtCloudProduct.getBand("MIR");
//        urbanBand = finalVgtCloudProduct.getBand("band_1");

        int extendedWidth = 64;
        int extendedHeight = 64; // todo: what do we need?

        rectCalculator = new RectangleExtender(new Rectangle(l1bProduct.getSceneRasterWidth(),
                                                             l1bProduct.getSceneRasterHeight()),
                                               extendedWidth, extendedHeight
        );

        ProductUtils.copyBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS, finalVgtCloudProduct, postProcessedCloudProduct, false);
        setTargetProduct(postProcessedCloudProduct);
    }

    private Product createTargetProduct(Product sourceProduct, String name, String type) {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        Product targetProduct = new Product(name, type, sceneWidth, sceneHeight);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        return targetProduct;
    }

    //JM&GK 20160212 Todo
    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        final Rectangle srcRectangle = rectCalculator.extend(targetRectangle);

        final Tile cloudFlagTile = getSourceTile(origCloudFlagBand, srcRectangle);
        final Tile smFlagTile = getSourceTile(origSmFlagBand, srcRectangle);
        //JM&GK 20160212 Todo
//        final Tile blueTile = getSourceTile(blueBand, srcRectangle);
//        final Tile redTile = getSourceTile(redBand, srcRectangle);
//        final Tile nirTile = getSourceTile(nirBand, srcRectangle);
//        final Tile swirTile = getSourceTile(swirBand, srcRectangle);

//        Tile urbanTile = null;
//        if (urbanBand != null) {
//            urbanTile = getSourceTile(urbanBand, srcRectangle);
//        }

        for (int y = srcRectangle.y; y < srcRectangle.y + srcRectangle.height; y++) {
            checkForCancellation();
            for (int x = srcRectangle.x; x < srcRectangle.x + srcRectangle.width; x++) {

                if (targetRectangle.contains(x, y)) {
                    boolean isInvalid = targetTile.getSampleBit(x, y, IdepixConstants.F_INVALID);
                    if (!isInvalid) {
                        combineFlags(x, y, cloudFlagTile, targetTile);
                        consolidateFlagging(x, y, smFlagTile, targetTile);

                        //JM&GK 20160212 Todo
                        // skip haze for the moment
//                        refineHaze(x, y, blueTile, redTile, nirTile, swirTile, urbanTile, targetTile);
                        setCloudShadow(x, y, smFlagTile, targetTile);
                    }
                }
            }
        }
    }

    private void setCloudShadow(int x, int y, Tile smFlagTile, Tile targetTile) {
//        final boolean smCloudShadow =
//                smFlagTile.getSampleBit(x, y, GlobAlbedoVgtClassificationOp.SM_F_CLOUDSHADOW);
        final boolean smCloud1 = smFlagTile.getSampleBit(x, y, VgtClassificationOp.SM_F_CLOUD_1);
        final boolean smCloud2 = smFlagTile.getSampleBit(x, y, VgtClassificationOp.SM_F_CLOUD_2);
        final boolean smCloudShadow = smCloud1 && !smCloud2; // see mask definition in SPOT VGT reader

        final boolean safeCloudFinal = targetTile.getSampleBit(x, y, IdepixConstants.F_CLOUD);
        final boolean isLand = targetTile.getSampleBit(x, y, IdepixConstants.F_LAND);

        final boolean isCloudShadow = smCloudShadow && !safeCloudFinal && isLand;
        targetTile.setSample(x, y, IdepixConstants.F_CLOUD_SHADOW, isCloudShadow);
    }

    private void combineFlags(int x, int y, Tile sourceFlagTile, Tile targetTile) {
        int sourceFlags = sourceFlagTile.getSampleInt(x, y);
        int computedFlags = targetTile.getSampleInt(x, y);
        targetTile.setSample(x, y, sourceFlags | computedFlags);
    }

    private void consolidateFlagging(int x, int y, Tile smFlagTile, Tile targetTile) {
//        final boolean smClear = smFlagTile.getSampleBit(x, y, GlobAlbedoVgtClassificationOp.SM_F_CLEAR);
        final boolean smCloud1 = smFlagTile.getSampleBit(x, y, VgtClassificationOp.SM_F_CLOUD_1);
        final boolean smCloud2 = smFlagTile.getSampleBit(x, y, VgtClassificationOp.SM_F_CLOUD_2);
        final boolean smClear = !smCloud1 && !smCloud2; // see mask definition in SPOT VGT reader

        final boolean idepixLand = targetTile.getSampleBit(x, y, IdepixConstants.F_LAND);
        final boolean idepixClearLand = targetTile.getSampleBit(x, y, IdepixConstants.F_CLEAR_LAND);
        final boolean idepixWater = targetTile.getSampleBit(x, y, IdepixConstants.F_WATER);
        final boolean idepixClearWater = targetTile.getSampleBit(x, y, IdepixConstants.F_CLEAR_WATER);
        final boolean idepixClearSnow = targetTile.getSampleBit(x, y, IdepixConstants.F_CLEAR_SNOW);
        final boolean idepixCloud = targetTile.getSampleBit(x, y, IdepixConstants.F_CLOUD);

        final boolean safeClearLand = smClear && idepixLand && idepixClearLand && !idepixClearSnow;
        final boolean safeClearWater = smClear && idepixWater && idepixClearWater && !idepixClearSnow;
        final boolean potentialCloudSnow = !safeClearLand && idepixLand;
        final boolean safeSnowIce = potentialCloudSnow && idepixClearSnow;
        // GK 20151201;
//        final boolean smCloud = smFlagTile.getSampleBit(x, y, GlobAlbedoVgtClassificationOp.SM_F_CLOUD);
        final boolean smCloud = smCloud1 && smCloud2;
        final boolean safeCloud = idepixCloud || (potentialCloudSnow && (!safeSnowIce && !safeClearWater));
        final boolean safeClearWaterFinal = ((!safeClearLand && !safeSnowIce && !safeCloud && !smCloud) && idepixWater) || safeClearWater;
        final boolean safeClearLandFinal = ((!safeSnowIce && !idepixCloud && !smCloud && !safeClearWaterFinal) && idepixLand) || safeClearLand;
        final boolean safeCloudFinal = safeCloud && (!safeClearLandFinal && !safeClearWaterFinal);

        // GK 20151201;
        targetTile.setSample(x, y, IdepixConstants.F_CLEAR_LAND, safeClearLandFinal);
        targetTile.setSample(x, y, IdepixConstants.F_CLEAR_WATER, safeClearWaterFinal);
        targetTile.setSample(x, y, IdepixConstants.F_CLOUD, safeCloudFinal);
        targetTile.setSample(x, y, IdepixConstants.F_CLEAR_SNOW, safeSnowIce);
    }

//    private void refineHaze(int x, int y,
//                            Tile blueTile, Tile redTile, Tile nirTile, Tile swirTile, Tile urbanTile,
//                            Tile targetTile) {
//
//        final double blue = blueTile.getSampleDouble(x, y);
//        final double red = redTile.getSampleDouble(x, y);
//        final double nir = nirTile.getSampleDouble(x, y);
//        final double swir = swirTile.getSampleDouble(x, y);
//        double[] tcValue = new double[4];
//        double[] tcSlopeValue = new double[2];
//
//        tcValue[0] = 0.332 * blue + 0.603 * red + 0.676 * nir + 0.263 * swir;
//        tcValue[1] = 0.283 * blue + -0.66 * red + 0.577 * nir + 0.388 * swir;
//        tcValue[2] = 0.9 * blue + 0.428 * red + 0.0759 * nir + -0.041 * swir;
//        tcValue[3] = 0.016 * blue + 0.428 * red + -0.452 * nir + 0.882 * swir;
//
//        tcSlopeValue[0] = (tcValue[3] - tcValue[2]);
//        tcSlopeValue[1] = (tcValue[2] - tcValue[1]);
//
//        boolean haze = tcSlopeValue[0] < -0.07 && !(tcSlopeValue[1] < -0.01);
//        boolean urbanFromAuxdata;
//        if (urbanTile != null) {
//            final float urbanfromAuxdataValue = urbanTile.getSampleFloat(x, y);
//            urbanFromAuxdata = (urbanfromAuxdataValue == 1.0f);
//            if (urbanFromAuxdata) {
//                haze = tcSlopeValue[0] < -0.1 && !(tcSlopeValue[1] < -0.01);
//            }
//        }
//
//        final boolean safeCloudFinal = targetTile.getSampleBit(x, y, IdepixConstants.F_CLOUD);
//        final boolean isLand = targetTile.getSampleBit(x, y, IdepixConstants.F_LAND);
//        if (haze && isLand && !safeCloudFinal) {
//            targetTile.setSample(x, y, IdepixConstants.F_HAZE, true);
//        }
//    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(VgtPostProcessOp.class);
        }
    }
}
