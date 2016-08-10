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

import java.awt.*;

/**
 * Operator used to consolidate Idepix classification flag for VGT, currently:
 * - basic flag consolidation following GK
 * - cloud shadow
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

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "vgtCloud")
    private Product vgtCloudProduct;

    private Band origCloudFlagBand;
    private Band origSmFlagBand;

    @Override
    public void initialize() throws OperatorException {

        Product finalVgtCloudProduct = vgtCloudProduct;

        Product postProcessedCloudProduct = createTargetProduct(vgtCloudProduct,
                                                                "postProcessedCloud", "postProcessedCloud");

        origCloudFlagBand = finalVgtCloudProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        origSmFlagBand = l1bProduct.getBand("SM");

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

    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();

        final Tile cloudFlagTile = getSourceTile(origCloudFlagBand, targetRectangle);
        final Tile smFlagTile = getSourceTile(origSmFlagBand, targetRectangle);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {

                if (targetRectangle.contains(x, y)) {
                    boolean isInvalid = targetTile.getSampleBit(x, y, IdepixConstants.F_INVALID);
                    if (!isInvalid) {
                        combineFlags(x, y, cloudFlagTile, targetTile);
                        consolidateFlagging(x, y, smFlagTile, targetTile);

                        setCloudShadow(x, y, smFlagTile, targetTile);
                    }
                }
            }
        }
    }

    private void setCloudShadow(int x, int y, Tile smFlagTile, Tile targetTile) {
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

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(VgtPostProcessOp.class);
        }
    }
}
