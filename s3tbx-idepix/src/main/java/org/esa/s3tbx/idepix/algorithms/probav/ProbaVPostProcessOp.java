package org.esa.s3tbx.idepix.algorithms.probav;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.collocation.CollocateOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.RectangleExtender;

import java.awt.*;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 17.08.2016
 * Time: 18:19
 *
 * @author olafd
 */
public class ProbaVPostProcessOp extends Operator {
    //    @Parameter(defaultValue = "true",
//            label = " Compute cloud shadow",
//            description = " Compute cloud shadow with latest 'fronts' algorithm")
    private boolean computeCloudShadow = true;   // always done currently

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "probavCloud")
    private Product probavCloudProduct;

    private Band origCloudFlagBand;
    private Band origSmFlagBand;

    private RectangleExtender rectCalculator;

    @Override
    public void initialize() throws OperatorException {

        Product postProcessedCloudProduct = createTargetProduct(probavCloudProduct,
                                                                "postProcessedCloud", "postProcessedCloud");

        origCloudFlagBand = probavCloudProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        origSmFlagBand = l1bProduct.getBand("SM_FLAGS");

        int extendedWidth = 64;
        int extendedHeight = 64; // todo: what do we need?

        rectCalculator = new RectangleExtender(new Rectangle(l1bProduct.getSceneRasterWidth(),
                                                             l1bProduct.getSceneRasterHeight()),
                                               extendedWidth, extendedHeight
        );

        ProductUtils.copyBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS, probavCloudProduct, postProcessedCloudProduct, false);
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
        final Rectangle srcRectangle = rectCalculator.extend(targetRectangle);

        final Tile cloudFlagTile = getSourceTile(origCloudFlagBand, srcRectangle);
        final Tile smFlagTile = getSourceTile(origSmFlagBand, srcRectangle);

        for (int y = srcRectangle.y; y < srcRectangle.y + srcRectangle.height; y++) {
            checkForCancellation();
            for (int x = srcRectangle.x; x < srcRectangle.x + srcRectangle.width; x++) {

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
        // as requested by JM, 20160302:
        final boolean smCloudShadow = smFlagTile.getSampleBit(x, y, ProbaVClassificationOp.SM_F_CLOUDSHADOW);
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
        final boolean smClear = smFlagTile.getSampleBit(x, y, ProbaVClassificationOp.SM_F_CLEAR);
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
        final boolean smCloud = smFlagTile.getSampleBit(x, y, ProbaVClassificationOp.SM_F_CLOUD);
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
            super(ProbaVPostProcessOp.class);
        }
    }
}
