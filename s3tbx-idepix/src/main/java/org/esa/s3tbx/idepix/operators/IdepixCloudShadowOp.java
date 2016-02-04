package org.esa.s3tbx.idepix.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.RectangleExtender;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "idepix.operators.cloudshadow",
        version = "2.2",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2008 by Brockmann Consult",
        description = "This operator provides a sensor-dependent cloud shadow algorithm.")
public class IdepixCloudShadowOp extends Operator {

    private static final int MEAN_EARTH_RADIUS = 6372000;

    private static final int MAX_ITER = 5;

    private static final double DIST_THRESHOLD = 1 / 740.0;

    private RectangleExtender rectCalculator;
    private GeoCoding geoCoding;
    private RasterDataNode altitudeRDN;

    private Band cloudFlagBand;

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "cloud")
    private Product cloudProduct;
    @SourceProduct(alias = "ctp", optional = true)
    private Product ctpProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter
    private int shadowWidth;
    @Parameter(description = "CTP constant value", defaultValue = "500.0")
    private float ctpConstantValue;

    @Parameter(description = " CTP value to use in MERIS cloud shadow algorithm",
            defaultValue = "Derive from Neural Net")
    private String ctpMode;

    @Parameter(description = "If 'true', cloud shadow is computed for cloud buffer as well",
            defaultValue = "false")
    private boolean shadowForCloudBuffer;

    private int sourceProductTypeId;


    @Override
    public void initialize() throws OperatorException {
        setSourceProductTypeId();
        createTargetProduct();

        if (l1bProduct.getProductType().equals(
                EnvisatConstants.MERIS_FSG_L1B_PRODUCT_TYPE_NAME)) {
            if (shadowWidth == 0) {
                shadowWidth = 16;
            }
            altitudeRDN = l1bProduct.getBand("altitude");
        } else {
            if (shadowWidth == 0) {
                shadowWidth = 64;
            }
            altitudeRDN = l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME);
        }
        final int shadowHeight = shadowWidth;
        rectCalculator = new RectangleExtender(new Rectangle(cloudProduct.getSceneRasterWidth(),
                cloudProduct.getSceneRasterHeight()),
                shadowWidth, shadowHeight);
        geoCoding = l1bProduct.getSceneGeoCoding();
    }

    private void createTargetProduct() throws OperatorException {
        int sceneWidth = cloudProduct.getSceneRasterWidth();
        int sceneHeight = cloudProduct.getSceneRasterHeight();

        targetProduct = new Product(cloudProduct.getName(), cloudProduct.getProductType(), sceneWidth, sceneHeight);

        cloudFlagBand = targetProduct.addBand(IdepixUtils.IDEPIX_CLOUD_FLAGS, ProductData.TYPE_INT32);
        FlagCoding flagCoding = IdepixUtils.createIdepixFlagCoding(IdepixUtils.IDEPIX_CLOUD_FLAGS);
        cloudFlagBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);

        ProductUtils.copyTiePointGrids(cloudProduct, targetProduct);

        ProductUtils.copyGeoCoding(l1bProduct, targetProduct);
        targetProduct.setStartTime(cloudProduct.getStartTime());
        targetProduct.setEndTime(cloudProduct.getEndTime());
        ProductUtils.copyMetadata(cloudProduct, targetProduct);

        // set bit masks:
        IdepixUtils.setupIdepixCloudscreeningBitmasks(targetProduct);

        switch (sourceProductTypeId) {
            case IdepixConstants.PRODUCT_TYPE_MERIS:
                for (Band b : cloudProduct.getBands()) {
                    if (!b.isFlagBand() && !targetProduct.containsBand(b.getName())) {
                        ProductUtils.copyBand(b.getName(), cloudProduct, targetProduct, true);
                    }
                }
                break;
            // todo: define and implement algorithms for AATSR, VGT
            case IdepixConstants.PRODUCT_TYPE_AATSR:
                throw new OperatorException("No cloud shadow algorithm available for AATSR");
            case IdepixConstants.PRODUCT_TYPE_VGT:
                throw new OperatorException("No cloud shadow algorithm available for VGT");
        }
        ProductUtils.copyFlagBands(l1bProduct, targetProduct, true);
    }

    private void setSourceProductTypeId() {
        if (l1bProduct.getProductType().startsWith("MER")) {
            sourceProductTypeId = IdepixConstants.PRODUCT_TYPE_MERIS;
        } else if (l1bProduct.getProductType().startsWith("ATS")) {
            sourceProductTypeId = IdepixConstants.PRODUCT_TYPE_AATSR;
        } else if (l1bProduct.getProductType().startsWith("VGT")) {
            sourceProductTypeId = IdepixConstants.PRODUCT_TYPE_VGT;
        } else {
            sourceProductTypeId = IdepixConstants.PRODUCT_TYPE_INVALID;
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        if (band == cloudFlagBand) {

            // copy flags from input cloud band...
            Rectangle targetRectangle = targetTile.getRectangle();
            Rectangle sourceRectangle = rectCalculator.extend(targetRectangle);

            Tile inputCloudTile = getSourceTile(cloudProduct.getBand(IdepixUtils.IDEPIX_CLOUD_FLAGS),
                    sourceRectangle);
            copyInputCloudFlags(targetTile, targetRectangle, inputCloudTile);

            // compute cloud shadow and add to cloud classification band...
            Tile szaTile = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME),
                    sourceRectangle);
            Tile saaTile = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME),
                    sourceRectangle);
            Tile vzaTile = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME),
                    sourceRectangle);
            Tile vaaTile = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME),
                    sourceRectangle);

            Tile altTile = getSourceTile(altitudeRDN, sourceRectangle);
            Tile ctpTile = null;
            float ctp = 500.0f;
            if (ctpProduct != null && ctpMode.equals(IdepixConstants.CTP_MODE_DEFAULT)) {
                ctpTile = getSourceTile(ctpProduct.getBand("cloud_top_press"), sourceRectangle);
            } else if (!ctpMode.equals(IdepixConstants.CTP_MODE_DEFAULT)) {
                ctp = Integer.parseInt(ctpMode.substring(0, 3));
            }

            for (int y = sourceRectangle.y; y < sourceRectangle.y + sourceRectangle.height; y++) {
                for (int x = sourceRectangle.x; x < sourceRectangle.x + sourceRectangle.width; x++) {
                    int cloudFlag = inputCloudTile.getSampleInt(x, y);
                    if (BitSetter.isFlagSet(cloudFlag, IdepixConstants.F_CLOUD) ||
                            (shadowForCloudBuffer && BitSetter.isFlagSet(cloudFlag, IdepixConstants.F_CLOUD_BUFFER))) {
                        final float sza = szaTile.getSampleFloat(x, y) * MathUtils.DTOR_F;
                        final float saa = saaTile.getSampleFloat(x, y) * MathUtils.DTOR_F;
                        final float vza = vzaTile.getSampleFloat(x, y) * MathUtils.DTOR_F;
                        final float vaa = vaaTile.getSampleFloat(x, y) * MathUtils.DTOR_F;

                        PixelPos pixelPos = new PixelPos(x, y);
                        final GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
                        // use ctp as taken from user option (value set)
                        if (ctpTile != null) {
                            ctp = ctpTile.getSampleFloat(x, y);
                        }
                        if (ctp > 0) {
                            float cloudAlt = computeHeightFromPressure(ctp);
                            GeoPos shadowPos = getCloudShadow(altTile, geoCoding, sza, saa, vza, vaa, cloudAlt, geoPos);
                            if (shadowPos != null) {
                                pixelPos = geoCoding.getPixelPos(shadowPos, pixelPos);

                                if (targetRectangle.contains(pixelPos)) {
                                    final int pixelX = MathUtils.floorInt(pixelPos.x);
                                    final int pixelY = MathUtils.floorInt(pixelPos.y);
                                    targetTile.setSample(pixelX, pixelY, IdepixConstants.F_CLOUD_SHADOW, true);
                                }
                            }
                        }
                    }
                }
            }

            // keep cloud shadow only for non-cloudy pixels
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    int cloudFlag = inputCloudTile.getSampleInt(x, y);
                    if (BitSetter.isFlagSet(cloudFlag, IdepixConstants.F_CLOUD)) {
                        targetTile.setSample(x, y, IdepixConstants.F_CLOUD_SHADOW, false);
                    }
                }
            }

        }
    }

    private void copyInputCloudFlags(Tile targetTile, Rectangle targetRectangle, Tile inputCloudTile) {
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                targetTile.setSample(x, y, inputCloudTile.getSampleInt(x, y));
            }
        }
    }

    private float computeHeightFromPressure(float pressure) {
        return (float) (-8000 * Math.log(pressure / 1013.0f));
    }

    public static GeoPos getCloudShadow(Tile altTile, GeoCoding geoCoding, float sza, float saa, float vza,
                                        float vaa, float cloudAlt, GeoPos appCloud) {

        // NOTE:
        // this is the same MERIS cloud shadow algorithm as implemented SDR module...

        double surfaceAlt = getAltitude(altTile, geoCoding, appCloud);

        // deltaX and deltaY are the corrections to apply to get the
        // real cloud position from the apparent one
        // deltaX/deltaY are in meters
        final double deltaX = -(cloudAlt - surfaceAlt) * Math.tan(vza) * Math.sin(vaa);
        final double deltaY = -(cloudAlt - surfaceAlt) * Math.tan(vza) * Math.cos(vaa);

        // distLat and distLon are in degrees
        double distLat = -(deltaY / MEAN_EARTH_RADIUS) * MathUtils.RTOD;
        double distLon = -(deltaX / (MEAN_EARTH_RADIUS *
                Math.cos(appCloud.getLat() * MathUtils.DTOR))) * MathUtils.RTOD;

        double latCloud = appCloud.getLat() + distLat;
        double lonCloud = appCloud.getLon() + distLon;

        // once the cloud position is know, we iterate to get the shadow
        // position
        int iter = 0;
        double dist = 2 * DIST_THRESHOLD;
        surfaceAlt = 0;
        double lat = latCloud;
        double lon = lonCloud;
        GeoPos pos = new GeoPos();

        while ((iter < MAX_ITER) && (dist > DIST_THRESHOLD)
                && (surfaceAlt < cloudAlt)) {
            double lat0 = lat;
            double lon0 = lon;
            pos.setLocation((float) lat, (float) lon);
            PixelPos pixelPos = geoCoding.getPixelPos(pos, null);
            if (!(pixelPos.isValid() && altTile.getRectangle().contains(pixelPos))) {
                return null;
            }
            surfaceAlt = getAltitude(altTile, geoCoding, pos);

            double deltaProjX = (cloudAlt - surfaceAlt) * Math.tan(sza) * Math.sin(saa);
            double deltaProjY = (cloudAlt - surfaceAlt) * Math.tan(sza) * Math.cos(saa);

            // distLat and distLon are in degrees
            distLat = -(deltaProjY / MEAN_EARTH_RADIUS) * MathUtils.RTOD;
            lat = latCloud + distLat;
            distLon = -(deltaProjX / (MEAN_EARTH_RADIUS * Math.cos(lat * MathUtils.DTOR))) * MathUtils.RTOD;
            lon = lonCloud + distLon;

            dist = Math.max(Math.abs(lat - lat0), Math.abs(lon - lon0));
            iter++;
        }

        if (surfaceAlt < cloudAlt && iter < MAX_ITER && dist < DIST_THRESHOLD) {
            return new GeoPos((float) lat, (float) lon);
        }
        return null;
    }

    private static float getAltitude(Tile altTile, GeoCoding geoCoding, GeoPos geoPos) {
        final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
        Rectangle rectangle = altTile.getRectangle();
        final int x = (int) MathUtils.roundAndCrop(pixelPos.x, rectangle.x, rectangle.x + rectangle.width - 1);
        final int y = (int) MathUtils.roundAndCrop(pixelPos.y, rectangle.y, rectangle.y + rectangle.height - 1);
        return altTile.getSampleFloat(x, y);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(IdepixCloudShadowOp.class);
        }
    }
}
