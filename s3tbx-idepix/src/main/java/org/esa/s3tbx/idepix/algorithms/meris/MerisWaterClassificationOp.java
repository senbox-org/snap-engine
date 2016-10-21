package org.esa.s3tbx.idepix.algorithms.meris;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.seaice.SeaIceClassification;
import org.esa.s3tbx.idepix.core.seaice.SeaIceClassifier;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.s3tbx.idepix.core.util.SchillerNeuralNetWrapper;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataException;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataProvider;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.s3tbx.util.math.FractIndex;
import org.esa.s3tbx.util.math.Interp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.RectangleExtender;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * MERIS pixel classification operator.
 * Only water pixels are classified from NN approach (following BEAM Cawa and OC-CCI algorithm).
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Meris.Water",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Idepix water pixel classification operator for OLCI.")
public class MerisWaterClassificationOp extends Operator {

    private Band cloudFlagBand;
    private SeaIceClassifier seaIceClassifier;
    private Band landWaterBand;
    private Band nnOutputBand;

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "rhotoa")
    private Product rhoToaProduct;
    @SourceProduct(alias = "waterMask")
    private Product waterMaskProduct;

    @SuppressWarnings({"FieldCanBeLocal"})
    @TargetProduct
    private Product targetProduct;

    @Parameter(label = " Sea Ice Climatology Value", defaultValue = "false")
    private boolean ccOutputSeaIceClimatologyValue;

    @Parameter(defaultValue = "false",
            description = "Check for sea/lake ice also outside Sea Ice Climatology area.",
            label = "Check for sea/lake ice also outside Sea Ice Climatology area"
    )
    private boolean ignoreSeaIceClimatology;

    @Parameter(label = "Cloud screening 'ambiguous' threshold", defaultValue = "1.4")
    private double cloudScreeningAmbiguous;     // Schiller, used in previous approach only

    @Parameter(label = "Cloud screening 'sure' threshold", defaultValue = "1.8")
    private double cloudScreeningSure;         // Schiller, used in previous approach only

    @Parameter(defaultValue = "false",
            label = " Write NN value to the target product.",
            description = " If applied, write NN value to the target product ")
    private boolean outputSchillerNNValue;

    @Parameter(defaultValue = "2.0",
            label = " NN cloud ambiguous lower boundary ",
            description = " NN cloud ambiguous lower boundary ")
    double schillerNNCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "3.7",
            label = " NN cloud ambiguous/sure separation value ",
            description = " NN cloud ambiguous cloud ambiguous/sure separation value ")
    double schillerNNCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.05",
            label = " NN cloud sure/snow separation value ",
            description = " NN cloud ambiguous cloud sure/snow separation value ")
    double schillerNNCloudSureSnowSeparationValue;

    public static final String MERIS_WATER_NET_NAME = "11x8x5x3_876.8_water.net";
    public static final String MERIS_ALL_NET_NAME = "11x8x5x3_1409.7_all.net";

    ThreadLocal<SchillerNeuralNetWrapper> merisWaterNeuralNet;
    ThreadLocal<SchillerNeuralNetWrapper> merisAllNeuralNet;

    private L2AuxData auxData;

    private static final double SEA_ICE_CLIM_THRESHOLD = 10.0;

    private RectangleExtender rectExtender;

    @Override
    public void initialize() throws OperatorException {
        try {
            auxData = L2AuxDataProvider.getInstance().getAuxdata(l1bProduct);
        } catch (L2AuxDataException e) {
            throw new OperatorException("Could not load L2Auxdata", e);
        }

        readSchillerNets();
        createTargetProduct();

        initSeaIceClassifier();

        landWaterBand = waterMaskProduct.getBand("land_water_fraction");

        rectExtender = new RectangleExtender(new Rectangle(l1bProduct.getSceneRasterWidth(),
                                                           l1bProduct.getSceneRasterHeight()), 1, 1);
    }

    private void readSchillerNets() {
        try (InputStream isWater = getClass().getResourceAsStream(MERIS_WATER_NET_NAME);
             InputStream isAll = getClass().getResourceAsStream(MERIS_ALL_NET_NAME)) {
            merisWaterNeuralNet = SchillerNeuralNetWrapper.create(isWater);
            merisAllNeuralNet = SchillerNeuralNetWrapper.create(isAll);
        } catch (IOException e) {
            throw new OperatorException("Cannot read Neural Nets: " + e.getMessage());
        }
    }

    private void initSeaIceClassifier() {
        final ProductData.UTC startTime = getSourceProduct().getStartTime();
        final int monthIndex = startTime.getAsCalendar().get(Calendar.MONTH);
        try {
            seaIceClassifier = new SeaIceClassifier(monthIndex + 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createTargetProduct() {
        targetProduct = IdepixIO.createCompatibleTargetProduct(l1bProduct, "MER", "MER_L2", true);

        cloudFlagBand = targetProduct.addBand(IdepixIO.IDEPIX_CLASSIF_FLAGS, ProductData.TYPE_INT16);
        FlagCoding flagCoding = MerisUtils.createMerisFlagCoding(IdepixIO.IDEPIX_CLASSIF_FLAGS);
        cloudFlagBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);

        if (outputSchillerNNValue) {
            nnOutputBand = targetProduct.addBand(IdepixConstants.NN_OUTPUT_BAND_NAME,
                                                 ProductData.TYPE_FLOAT32);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        try {
            final Rectangle sourceRectangle = rectExtender.extend(targetRectangle);

            Tile[] rhoToaTiles = new Tile[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
            for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
                final int suffixStart = Rad2ReflConstants.MERIS_REFL_BAND_NAMES[i].indexOf("_");
                final String reflBandname = Rad2ReflConstants.MERIS_REFL_BAND_NAMES[i].substring(0, suffixStart);
                final Band rhoToaBand = rhoToaProduct.getBand(reflBandname + "_" + (i + 1));
                rhoToaTiles[i] = getSourceTile(rhoToaBand, sourceRectangle);
            }

            Tile l1FlagsTile = getSourceTile(l1bProduct.getBand(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME),
                                             sourceRectangle);
            Tile waterFractionTile = getSourceTile(landWaterBand, sourceRectangle);

            Tile szaTile = null;
            Tile vzaTile = null;
            Tile saaTile = null;
            Tile vaaTile = null;
            Tile windUTile = null;
            Tile windVTile = null;
            if (band == cloudFlagBand) {
                szaTile = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME),
                                        sourceRectangle);
                vzaTile = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME),
                                        sourceRectangle);
                saaTile = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME),
                                        sourceRectangle);
                vaaTile = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME),
                                        sourceRectangle);
                windUTile = getSourceTile(l1bProduct.getTiePointGrid("zonal_wind"), sourceRectangle);
                windVTile = getSourceTile(l1bProduct.getTiePointGrid("merid_wind"), sourceRectangle);
            }

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    if (!l1FlagsTile.getSampleBit(x, y, MerisConstants.L1_F_INVALID)) {
                        final int waterFraction = waterFractionTile.getSampleInt(x, y);

                        if (isLandPixel(x, y, l1FlagsTile, waterFraction)) {
                            if (band == cloudFlagBand) {
                                targetTile.setSample(x, y, MerisConstants.L1_F_LAND, true);
                            } else {
                                targetTile.setSample(x, y, Float.NaN);
                            }
                        } else {
                            if (band == cloudFlagBand) {
                                classifyCloud(x, y, rhoToaTiles, windUTile, windVTile, szaTile, vzaTile, saaTile, vaaTile,
                                              targetTile, waterFraction);
                            }
                            if (outputSchillerNNValue && band == nnOutputBand) {
                                final double[] nnOutput = getMerisNNOutput(x, y, rhoToaTiles);
                                targetTile.setSample(x, y, nnOutput[0]);
                            }
                        }
                    } else {
                        targetTile.setSample(x, y, IdepixConstants.IDEPIX_INVALID, true);
                    }
                }
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private boolean isLandPixel(int x, int y, Tile l1FlagsTile, int waterFraction) {
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            // values bigger than 100 indicate no data
            if (waterFraction <= 100) {
                // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
                // is always 0 or 100!! (TS, OD, 20140502)
                return waterFraction == 0;
            } else {
                return l1FlagsTile.getSampleBit(x, y, MerisConstants.L1_F_LAND);
            }
        } else {
            return l1FlagsTile.getSampleBit(x, y, MerisConstants.L1_F_LAND);
        }
    }

    private boolean isCoastlinePixel(int x, int y, int waterFraction) {
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        // values bigger than 100 indicate no data
        // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
        // is always 0 or 100!! (TS, OD, 20140502)
        return getGeoPos(x, y).lat > -58f && waterFraction <= 100 && waterFraction < 100 && waterFraction > 0;
    }

    private void classifyCloud(int x, int y, Tile[] rhoToaTiles, Tile winduTile, Tile windvTile,
                               Tile szaTile, Tile vzaTile, Tile saaTile, Tile vaaTile, Tile targetTile, 
                               int waterFraction) {

        final boolean isCoastline = isCoastlinePixel(x, y, waterFraction);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_COASTLINE, isCoastline);

        boolean is_snow_ice;
        boolean is_glint_risk = !isCoastline && 
                isGlintRisk(x, y, rhoToaTiles, winduTile, windvTile, szaTile, vzaTile, saaTile, vaaTile);
        boolean checkForSeaIce = false;
        if (!isCoastline) {
            // over water
            final GeoPos geoPos = getGeoPos(x, y);
            checkForSeaIce = ignoreSeaIceClimatology || isPixelClassifiedAsSeaice(geoPos);
            // glint makes sense only if we have no sea ice
            is_glint_risk = is_glint_risk && !isPixelClassifiedAsSeaice(geoPos);

        }

        boolean isCloudSure = false;
        boolean isCloudAmbiguous;

        double[] nnOutput = getMerisNNOutput(x, y, rhoToaTiles);
        if (!targetTile.getSampleBit(x, y, IdepixConstants.IDEPIX_INVALID)) {
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, false);
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, false);
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, false);
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, false);
            isCloudAmbiguous = nnOutput[0] > schillerNNCloudAmbiguousLowerBoundaryValue &&
                    nnOutput[0] <= schillerNNCloudAmbiguousSureSeparationValue;
            if (isCloudAmbiguous) {
                // this would be as 'CLOUD_AMBIGUOUS'...
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, true);
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, true);
            }
            // check for snow_ice separation below if needed, first set all to cloud
            isCloudSure = nnOutput[0] > schillerNNCloudAmbiguousSureSeparationValue;
            if (isCloudSure) {
                // this would be as 'CLOUD_SURE'...
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, true);
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, true);
            }

            is_snow_ice = false;
            if (checkForSeaIce) {
                is_snow_ice = nnOutput[0] > schillerNNCloudSureSnowSeparationValue;
            }
            if (is_snow_ice) {
                // this would be as 'SNOW/ICE'...
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, true);
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, false);
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, false);
            }
        }
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_GLINT_RISK, is_glint_risk && !isCloudSure);
    }

    private double[] getMerisNNOutput(int x, int y, Tile[] rhoToaTiles) {
            return getMerisNNOutputImpl(x, y, rhoToaTiles, merisAllNeuralNet.get());
    }

    private double[] getMerisNNOutputImpl(int x, int y, Tile[] rhoToaTiles, SchillerNeuralNetWrapper nnWrapper) {
        double[] nnInput = nnWrapper.getInputVector();
        for (int i = 0; i < nnInput.length; i++) {
            nnInput[i] = Math.sqrt(rhoToaTiles[i].getSampleFloat(x, y));
        }
        return nnWrapper.getNeuralNet().calc(nnInput);
    }

    private boolean isGlintRisk(int x, int y, Tile[] rhoToaTiles, Tile winduTile, Tile windvTile,
                                Tile szaTile, Tile vzaTile, Tile saaTile, Tile vaaTile) {
        final float rhoGlint = (float) computeRhoGlint(x, y, winduTile, windvTile, szaTile, vzaTile, saaTile, vaaTile);
        return (rhoGlint >= 0.2 * rhoToaTiles[12].getSampleFloat(x, y));
    }

    private double computeChiW(int x, int y, Tile winduTile, Tile windvTile, Tile saaTile) {
        final double phiw = azimuth(winduTile.getSampleFloat(x, y), windvTile.getSampleFloat(x, y));
        /* and "scattering" angle */
        final double arg = MathUtils.DTOR * (saaTile.getSampleFloat(x, y) - phiw);
        return MathUtils.RTOD * (Math.acos(Math.cos(arg)));
    }

    private double computeRhoGlint(int x, int y, Tile winduTile, Tile windvTile,
                                   Tile szaTile, Tile vzaTile, Tile saaTile, Tile vaaTile) {
        final double chiw = computeChiW(x, y, winduTile, windvTile, saaTile);
        final float vaa = vaaTile.getSampleFloat(x, y);
        final float saa = saaTile.getSampleFloat(x, y);
        final double deltaAzimuth = (float) IdepixUtils.computeAzimuthDifference(vaa, saa);
        final float windU = winduTile.getSampleFloat(x, y);
        final float windV = windvTile.getSampleFloat(x, y);
        final double windm = Math.sqrt(windU * windU + windV * windV);
            /* allows to retrieve Glint reflectance for current geometry and wind */
        return glintRef(szaTile.getSampleFloat(x, y), vzaTile.getSampleFloat(x, y), deltaAzimuth, windm, chiw);
    }

    private double glintRef(double thetas, double thetav, double delta, double windm, double chiw) {
        FractIndex[] rogIndex = FractIndex.createArray(5);

        Interp.interpCoord(chiw, auxData.rog.getTab(0), rogIndex[0]);
        Interp.interpCoord(thetav, auxData.rog.getTab(1), rogIndex[1]);
        Interp.interpCoord(delta, auxData.rog.getTab(2), rogIndex[2]);
        Interp.interpCoord(windm, auxData.rog.getTab(3), rogIndex[3]);
        Interp.interpCoord(thetas, auxData.rog.getTab(4), rogIndex[4]);
        return Interp.interpolate(auxData.rog.getJavaArray(), rogIndex);
    }

    private boolean isPixelClassifiedAsSeaice(GeoPos geoPos) {
        // check given pixel, but also neighbour cell from 1x1 deg sea ice climatology...
        final double maxLon = 360.0;
        final double minLon = 0.0;
        final double maxLat = 180.0;
        final double minLat = 0.0;

        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                // for sea ice climatology indices, we need to shift lat/lon onto [0,180]/[0,360]...
                double lon = geoPos.lon + 180.0 + x * 1.0;
                double lat = 90.0 - geoPos.lat + y * 1.0;
                lon = Math.max(lon, minLon);
                lon = Math.min(lon, maxLon);
                lat = Math.max(lat, minLat);
                lat = Math.min(lat, maxLat);
                final SeaIceClassification classification = seaIceClassifier.getClassification(lat, lon);
                if (classification.max >= SEA_ICE_CLIM_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    private GeoPos getGeoPos(int x, int y) {
        final GeoPos geoPos = new GeoPos();
        final GeoCoding geoCoding = getSourceProduct().getSceneGeoCoding();
        final PixelPos pixelPos = new PixelPos(x, y);
        geoCoding.getGeoPos(pixelPos, geoPos);
        return geoPos;
    }

    private double azimuth(double x, double y) {
        if (y > 0.0) {
            // DPM #2.6.5.1.1-1
            return (MathUtils.RTOD * Math.atan(x / y));
        } else if (y < 0.0) {
            // DPM #2.6.5.1.1-5
            return (180.0 + MathUtils.RTOD * Math.atan(x / y));
        } else {
            // DPM #2.6.5.1.1-6
            return (x >= 0.0 ? 90.0 : 270.0);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MerisWaterClassificationOp.class);
        }
    }

}
