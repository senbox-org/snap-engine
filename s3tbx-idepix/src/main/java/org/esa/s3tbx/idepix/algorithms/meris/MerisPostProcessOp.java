package org.esa.s3tbx.idepix.algorithms.meris;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.algorithms.CloudShadowFronts;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.*;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.RectangleExtender;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.util.HashMap;

/**
 * Operator used to consolidate Idepix classification flag for MERIS:
 * - coastline refinement
 * - cloud shadow (from Fronts)
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Meris.Postprocess",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Refines the MERIS pixel classification over both land and water.")
public class MerisPostProcessOp extends Operator {

    @Parameter(defaultValue = "true",
            label = " Compute cloud shadow",
            description = " Compute cloud shadow with latest 'fronts' algorithm")
    private boolean computeCloudShadow;

    @Parameter(defaultValue = "true",
            label = " Refine pixel classification near coastlines",
            description = "Refine pixel classification near coastlines. ")
    private boolean refineClassificationNearCoastlines;

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "merisCloud")
    private Product merisCloudProduct;
    //    @SourceProduct(alias = "ctp", optional = true)
    @SourceProduct(alias = "ctp")
    private Product ctpProduct;
    @SourceProduct(alias = "waterMask")
    private Product waterMaskProduct;

    private Band waterFractionBand;
    private Band origCloudFlagBand;
    private Band ctpBand;
    private TiePointGrid szaTPG;
    private TiePointGrid saaTPG;
    private TiePointGrid altTPG;
    private GeoCoding geoCoding;

    private RectangleExtender rectCalculator;

    @Override
    public void initialize() throws OperatorException {
        Product postProcessedCloudProduct = IdepixUtils.createCompatibleTargetProduct(merisCloudProduct,
                                                                                      "postProcessedCloud",
                                                                                      "postProcessedCloud",
                                                                                      true);

        waterFractionBand = waterMaskProduct.getBand("land_water_fraction");

        geoCoding = l1bProduct.getSceneGeoCoding();

        origCloudFlagBand = merisCloudProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        szaTPG = l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME);
        saaTPG = l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME);
        altTPG = l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME);
        ctpBand = ctpProduct.getBand("cloud_top_press");

        int extendedWidth;
        int extendedHeight;
        if (l1bProduct.getProductType().startsWith("MER_F")) {
            extendedWidth = 64;
            extendedHeight = 64;
        } else {
            extendedWidth = 16;
            extendedHeight = 16;
        }

        rectCalculator = new RectangleExtender(new Rectangle(l1bProduct.getSceneRasterWidth(),
                                                             l1bProduct.getSceneRasterHeight()),
                                               extendedWidth, extendedHeight
        );


        ProductUtils.copyBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS, merisCloudProduct, postProcessedCloudProduct, false);
        setTargetProduct(postProcessedCloudProduct);
    }

    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        final Rectangle srcRectangle = rectCalculator.extend(targetRectangle);

        final Tile sourceFlagTile = getSourceTile(origCloudFlagBand, srcRectangle);
        Tile szaTile = getSourceTile(szaTPG, srcRectangle);
        Tile saaTile = getSourceTile(saaTPG, srcRectangle);
        Tile ctpTile = getSourceTile(ctpBand, srcRectangle);

        Tile altTile = getSourceTile(altTPG, targetRectangle);
        final Tile waterFractionTile = getSourceTile(waterFractionBand, srcRectangle);

        for (int y = srcRectangle.y; y < srcRectangle.y + srcRectangle.height; y++) {
            checkForCancellation();
            for (int x = srcRectangle.x; x < srcRectangle.x + srcRectangle.width; x++) {

                if (targetRectangle.contains(x, y)) {
                    boolean isCloud = sourceFlagTile.getSampleBit(x, y, MerisConstants.F_CLOUD);
                    combineFlags(x, y, sourceFlagTile, targetTile);

                    if (refineClassificationNearCoastlines) {
                        if (isNearCoastline(x, y, waterFractionTile, srcRectangle)) {
                            targetTile.setSample(x, y, MerisConstants.F_COASTLINE, true);
                            refineSnowIceFlaggingForCoastlines(x, y, sourceFlagTile, targetTile);
                            if (isCloud) {
                                refineCloudFlaggingForCoastlines(x, y, sourceFlagTile, waterFractionTile, targetTile, srcRectangle);
                            }
                        }
                    }
                    boolean isCloudAfterRefinement = targetTile.getSampleBit(x, y, MerisConstants.F_CLOUD);
                    if (isCloudAfterRefinement) {
                        targetTile.setSample(x, y, MerisConstants.F_SNOW_ICE, false);
                    }
                }
            }
        }

        if (computeCloudShadow) {
            CloudShadowFronts cloudShadowFronts = new CloudShadowFronts(
                    geoCoding,
                    srcRectangle,
                    targetRectangle,
                    szaTile, saaTile, ctpTile, altTile) {

                @Override
                protected boolean isCloudForShadow(int x, int y) {
                    final boolean is_cloud_current;
                    if (!targetTile.getRectangle().contains(x, y)) {
                        is_cloud_current = sourceFlagTile.getSampleBit(x, y, MerisConstants.F_CLOUD);
                    } else {
                        is_cloud_current = targetTile.getSampleBit(x, y, MerisConstants.F_CLOUD);
                    }
                    if (is_cloud_current) {
                        final boolean isNearCoastline = isNearCoastline(x, y, waterFractionTile, srcRectangle);
                        if (!isNearCoastline) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                protected boolean isCloudFree(int x, int y) {
                    return !sourceFlagTile.getSampleBit(x, y, MerisConstants.F_CLOUD);
                }

                @Override
                protected boolean isSurroundedByCloud(int x, int y) {
                    return isPixelSurrounded(x, y, sourceFlagTile, MerisConstants.F_CLOUD);
                }

                @Override
                protected void setCloudShadow(int x, int y) {
                    targetTile.setSample(x, y, MerisConstants.F_CLOUD_SHADOW, true);
                }
            };
            cloudShadowFronts.computeCloudShadow();
        }
    }

    private void combineFlags(int x, int y, Tile sourceFlagTile, Tile targetTile) {
        int sourceFlags = sourceFlagTile.getSampleInt(x, y);
        int computedFlags = targetTile.getSampleInt(x, y);
        targetTile.setSample(x, y, sourceFlags | computedFlags);
    }

    private boolean isCoastlinePixel(int x, int y, Tile waterFractionTile) {
        boolean isCoastline = false;
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            final int waterFraction = waterFractionTile.getSampleInt(x, y);
            // values bigger than 100 indicate no data
            if (waterFraction <= 100) {
                // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
                // is always 0 or 100!! (TS, OD, 20140502)
                isCoastline = waterFraction < 100 && waterFraction > 0;
            }
        }
        return isCoastline;
    }

    private GeoPos getGeoPos(int x, int y) {
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos(x, y);
        geoCoding.getGeoPos(pixelPos, geoPos);
        return geoPos;
    }

    private boolean isNearCoastline(int x, int y, Tile waterFractionTile, Rectangle rectangle) {
        final int windowWidth = 1;
        final int LEFT_BORDER = Math.max(x - windowWidth, rectangle.x);
        final int RIGHT_BORDER = Math.min(x + windowWidth, rectangle.x + rectangle.width - 1);
        final int TOP_BORDER = Math.max(y - windowWidth, rectangle.y);
        final int BOTTOM_BORDER = Math.min(y + windowWidth, rectangle.y + rectangle.height - 1);
        final int waterFractionCenter = waterFractionTile.getSampleInt(x, y);
        for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
            for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                if (rectangle.contains(i, j)) {
                    if (!(l1bProduct.getSceneGeoCoding() instanceof TiePointGeoCoding) &&
                            !(l1bProduct.getSceneGeoCoding() instanceof CrsGeoCoding)) {
                        if (waterFractionTile.getSampleInt(i, j) != waterFractionCenter) {
                            return true;
                        }
                    } else {
                        if (isCoastlinePixel(i, j, waterFractionTile)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void refineCloudFlaggingForCoastlines(int x, int y, Tile sourceFlagTile, Tile waterFractionTile, Tile targetTile, Rectangle srcRectangle) {
        final int windowWidth = 1;
        final int LEFT_BORDER = Math.max(x - windowWidth, srcRectangle.x);
        final int RIGHT_BORDER = Math.min(x + windowWidth, srcRectangle.x + srcRectangle.width - 1);
        final int TOP_BORDER = Math.max(y - windowWidth, srcRectangle.y);
        final int BOTTOM_BORDER = Math.min(y + windowWidth, srcRectangle.y + srcRectangle.height - 1);
        boolean removeCloudFlag = true;
        if (CloudShadowFronts.isPixelSurrounded(x, y, sourceFlagTile, MerisConstants.F_CLOUD)) {
            removeCloudFlag = false;
        } else {
            Rectangle targetTileRectangle = targetTile.getRectangle();
            for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
                for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                    boolean is_cloud = sourceFlagTile.getSampleBit(i, j, MerisConstants.F_CLOUD);
                    if (is_cloud && targetTileRectangle.contains(i, j) && !isNearCoastline(i, j, waterFractionTile, srcRectangle)) {
                        removeCloudFlag = false;
                        break;
                    }
                }
            }
        }

        if (removeCloudFlag) {
            targetTile.setSample(x, y, MerisConstants.F_CLOUD, false);
            targetTile.setSample(x, y, MerisConstants.F_CLOUD_SURE, false);
            targetTile.setSample(x, y, MerisConstants.F_CLOUD_AMBIGUOUS, false);
        }
    }

    private void refineSnowIceFlaggingForCoastlines(int x, int y, Tile sourceFlagTile, Tile targetTile) {
        final boolean isSnowIce = sourceFlagTile.getSampleBit(x, y, MerisConstants.F_SNOW_ICE);
        if (isSnowIce) {
            targetTile.setSample(x, y, MerisConstants.F_SNOW_ICE, false);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisPostProcessOp.class);
        }
    }
}
