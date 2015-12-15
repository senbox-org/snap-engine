/*
 * $Id: CloudClassificationOp.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.idepix.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.s3tbx.meris.brr.HelperFunctions;
import org.esa.s3tbx.meris.brr.Rad2ReflOp;
import org.esa.s3tbx.meris.brr.RayleighCorrection;
import org.esa.s3tbx.meris.dpm.PixelId;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataException;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataProvider;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;

import static org.esa.s3tbx.meris.l2auxdata.Constants.*;


/**
 * This class provides the pixel classification for MERIS (mainly based on IPF approach).
 */
@OperatorMetadata(alias = "idepix.operators.MerisClassification",
                  version = "2.2",
                  internal = true,
                  authors = "Marco Zuehlke, Olaf Danne",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "MERIS L2 cloud classification (version from MEPIX processor).")
public class MerisClassificationOp extends MerisBasisOp {

    public static final String CLOUD_FLAGS = "cloud_classif_flags";
    public static final String PRESSURE_CTP = "cloud_top_press";
    public static final String PRESSURE_SURFACE = "surface_press";
    public static final String SCATT_ANGLE = "scattering_angle";
    public static final String RHO_THRESH_TERM = "rho442_thresh_term";
    public static final String MDSI = "mdsi";
    public static final String CLOUD_PROBABILITY_VALUE = "cloud_probability_value";

    public static final int BAND_BRIGHT_N = 0;
    public static final int BAND_SLOPE_N_1 = 1;
    public static final int BAND_SLOPE_N_2 = 2;

    public static final int BAND_FLH_7 = 3;
    public static final int BAND_FLH_8 = 4;
    public static final int BAND_FLH_9 = 5;

    public static final int F_CLOUD = 0;
    public static final int F_BRIGHT = 1;
    public static final int F_BRIGHT_RC = 2;
    public static final int F_LOW_P_PSCATT = 3;
    public static final int F_LOW_P_P1 = 4;
    public static final int F_SLOPE_1 = 5;
    public static final int F_SLOPE_2 = 6;
    public static final int F_BRIGHT_TOA = 7;
    public static final int F_HIGH_MDSI = 8;
    public static final int F_SNOW_ICE = 9;

    private L2AuxData auxData;

    private RayleighCorrection rayleighCorrection;

    private Band ctpOutputBand;
    private Band psurfOutputBand;
    private Band scattAngleOutputBand;
    private Band rhoThreshOutputBand;
    private Band mdsiOutputBand;

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "rhotoa")
    private Product rhoToaProduct;
    @SourceProduct(alias = "ctp")
    private Product ctpProduct;
    @SourceProduct(alias = "pressureOutputLise")
    private Product lisePressureProduct;
    @SourceProduct(alias = "pressureBaro")
    private Product pbaroProduct;

    @SuppressWarnings({"FieldCanBeLocal"})
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "If 'true' the algorithm will compute L2 Pressures.", defaultValue = "true")
    private boolean l2Pressures;
    @Parameter(description = "User Defined P1 Pressure Threshold.", defaultValue = "125.0")
    private double userDefinedP1PressureThreshold;
    @Parameter(description = "User Defined PScatt Pressure Threshold.", defaultValue = "700.0")
    private double userDefinedPScattPressureThreshold;
    @Parameter(description = "User Defined RhoTOA442 Threshold.", defaultValue = "0.03")
    private double userDefinedRhoToa442Threshold;
    @Parameter(description = "User Defined Delta RhoTOA442 Threshold.", defaultValue = "0.03")
    private double userDefinedDeltaRhoToa442Threshold;
    @Parameter(description = "User Defined RhoTOA753 Threshold.", defaultValue = "0.1")
    private double userDefinedRhoToa753Threshold;
    @Parameter(description = "User Defined RhoTOA Ratio 753/775 Threshold.", defaultValue = "0.15")
    private double userDefinedRhoToaRatio753775Threshold;
    @Parameter(description = "User Defined MDSI Threshold.", defaultValue = "0.01")
    private double userDefinedMDSIThreshold;
    private Band cloudFlagBand;


    @Override
    public void initialize() throws OperatorException {
        try {
            auxData = L2AuxDataProvider.getInstance().getAuxdata(l1bProduct);
        } catch (L2AuxDataException e) {
            throw new OperatorException("Could not load L2Auxdata", e);
        }
        rayleighCorrection = new RayleighCorrection(auxData);
        createTargetProduct();
    }

    private void createTargetProduct() {
        targetProduct = createCompatibleProduct(l1bProduct, "MER", "MER_L2");

        cloudFlagBand = targetProduct.addBand(CLOUD_FLAGS, ProductData.TYPE_INT16);
        FlagCoding flagCoding = createFlagCoding(CLOUD_FLAGS);
        cloudFlagBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);
        ctpOutputBand = targetProduct.addBand(PRESSURE_CTP, ProductData.TYPE_FLOAT32);
        psurfOutputBand = targetProduct.addBand(PRESSURE_SURFACE, ProductData.TYPE_FLOAT32);
        scattAngleOutputBand = targetProduct.addBand(SCATT_ANGLE, ProductData.TYPE_FLOAT32);
        rhoThreshOutputBand = targetProduct.addBand(RHO_THRESH_TERM, ProductData.TYPE_FLOAT32);
        mdsiOutputBand = targetProduct.addBand(MDSI, ProductData.TYPE_FLOAT32);
    }

    public static FlagCoding createFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, F_CLOUD), null);
        flagCoding.addFlag("F_BRIGHT", BitSetter.setFlag(0, F_BRIGHT), null);
        flagCoding.addFlag("F_BRIGHT_RC", BitSetter.setFlag(0, F_BRIGHT_RC), null);
        flagCoding.addFlag("F_LOW_P_PSCATT", BitSetter.setFlag(0, F_LOW_P_PSCATT), null);
        flagCoding.addFlag("F_LOW_P_P1", BitSetter.setFlag(0, F_LOW_P_P1), null);
        flagCoding.addFlag("F_SLOPE_1", BitSetter.setFlag(0, F_SLOPE_1), null);
        flagCoding.addFlag("F_SLOPE_2", BitSetter.setFlag(0, F_SLOPE_2), null);
        flagCoding.addFlag("F_BRIGHT_TOA", BitSetter.setFlag(0, F_BRIGHT_TOA), null);
        flagCoding.addFlag("F_HIGH_MDSI", BitSetter.setFlag(0, F_HIGH_MDSI), null);
        flagCoding.addFlag("F_SNOW_ICE", BitSetter.setFlag(0, F_SNOW_ICE), null);
        return flagCoding;
    }

    private SourceData loadSourceTiles(Rectangle rectangle) throws OperatorException {

        SourceData sd = new SourceData();
        sd.rhoToa = new float[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS][0];
        sd.radiance = new Tile[3];

        for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            sd.rhoToa[i] = (float[]) getSourceTile(
                    rhoToaProduct.getBand(Rad2ReflOp.RHO_TOA_BAND_PREFIX + "_" + (i + 1)),
                    rectangle).getRawSamples().getElems();
        }
        sd.radiance[BAND_BRIGHT_N] = getSourceTile(
                l1bProduct.getBand(EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES[auxData.band_bright_n]),
                rectangle);
        sd.radiance[BAND_SLOPE_N_1] = getSourceTile(
                l1bProduct.getBand(EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES[auxData.band_slope_n_1]),
                rectangle);
        sd.radiance[BAND_SLOPE_N_2] = getSourceTile(
                l1bProduct.getBand(EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES[auxData.band_slope_n_2]),
                rectangle);
        sd.detectorIndex = (short[]) getSourceTile(
                l1bProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME),
                rectangle).getRawSamples().getElems();
        sd.sza = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME),
                                         rectangle).getRawSamples().getElems();
        sd.vza = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME),
                                         rectangle).getRawSamples().getElems();
        sd.saa = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME),
                                         rectangle).getRawSamples().getElems();
        sd.vaa = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME),
                                         rectangle).getRawSamples().getElems();
        sd.altitude = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME),
                                              rectangle).getRawSamples().getElems();
        sd.ecmwfPressure = (float[]) getSourceTile(l1bProduct.getTiePointGrid("atm_press"),
                                                   rectangle).getRawSamples().getElems();
        sd.l1Flags = getSourceTile(l1bProduct.getBand(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME), rectangle);

        return sd;
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        Rectangle rectangle = targetTile.getRectangle();
        try {
            SourceData sd = loadSourceTiles(rectangle);

            Tile ctpTile = getSourceTile(ctpProduct.getBand("cloud_top_press"), rectangle);
            Tile pbaroTile = getSourceTile(pbaroProduct.getBand(BarometricPressureOp.PRESSURE_BAROMETRIC), rectangle);
            Tile liseP1Tile = getSourceTile(lisePressureProduct.getBand(LisePressureOp.PRESSURE_LISE_P1), rectangle);
            Tile lisePScattTile = getSourceTile(lisePressureProduct.getBand(LisePressureOp.PRESSURE_LISE_PSCATT),
                                                rectangle);

            PixelInfo pixelInfo = new PixelInfo();
            int i = 0;
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                pixelInfo.y = y;
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    if (!sd.l1Flags.getSampleBit(x, y, L1_F_INVALID)) {
                        pixelInfo.x = x;
                        pixelInfo.index = i;
                        pixelInfo.airMass = HelperFunctions.calculateAirMass(sd.vza[i], sd.sza[i]);
                        if (sd.l1Flags.getSampleBit(x, y, L1_F_LAND)) {
                            // ECMWF pressure is only corrected for positive
                            // altitudes and only for land pixels
                            pixelInfo.ecmwfPressure = HelperFunctions.correctEcmwfPressure(sd.ecmwfPressure[i],
                                                                                           sd.altitude[i],
                                                                                           auxData.press_scale_height);
                        } else {
                            pixelInfo.ecmwfPressure = sd.ecmwfPressure[i];
                        }
                        pixelInfo.pbaroPressure = pbaroTile.getSampleFloat(x, y);
                        pixelInfo.p1Pressure = liseP1Tile.getSampleFloat(x, y);
                        pixelInfo.pscattPressure = lisePScattTile.getSampleFloat(x, y);
                        pixelInfo.ctp = ctpTile.getSampleFloat(x, y);
                        pixelInfo.angleInfo = getAngleInfo(sd, pixelInfo);

                        if (band == cloudFlagBand) {
                            classifyCloud(sd, pixelInfo, targetTile);
                        }
                        if (band == psurfOutputBand && l2Pressures) {
                            setCloudPressureSurface(sd, pixelInfo, targetTile);
                        }
                        if (band == ctpOutputBand && l2Pressures) {
                            setCloudTopPressure(pixelInfo, targetTile);
                        }

                        // test, 30.10.09
                        if (band == scattAngleOutputBand) {
                            final double thetaScatt = calcScatteringAngle(pixelInfo);
                            targetTile.setSample(pixelInfo.x, pixelInfo.y, thetaScatt);
                        }
                        if (band == rhoThreshOutputBand) {
                            final double rhoThreshOffsetTerm = calcRhoToa442ThresholdTerm(pixelInfo);
                            targetTile.setSample(pixelInfo.x, pixelInfo.y, rhoThreshOffsetTerm);
                        }
                        // end test

                        if (band == mdsiOutputBand) {
                            setMdsi(sd, pixelInfo, targetTile);
                        }
                    }
                    i++;
                }
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    public void setCloudPressureSurface(SourceData sd, PixelInfo pixelInfo, Tile targetTile) {
        PixelId pixelId = new PixelId(auxData);
        PixelId.Pressure press = pixelId.computePressure(sd.rhoToa[bb753][pixelInfo.index],
                                                         sd.rhoToa[bb760][pixelInfo.index],
                                                         pixelInfo.airMass,
                                                         sd.detectorIndex[pixelInfo.index]);
        targetTile.setSample(pixelInfo.x, pixelInfo.y, Math.max(0.0, press.value));
    }

    public void setCloudTopPressure(PixelInfo pixelInfo, Tile targetTile) {
        targetTile.setSample(pixelInfo.x, pixelInfo.y, pixelInfo.ctp);
    }

    public void classifyCloud(SourceData sd, PixelInfo pixelInfo, Tile targetTile) {
        final boolean[] resultFlags = new boolean[6];

        // Compute slopes- step 2.1.7
        spec_slopes(sd, pixelInfo, resultFlags);
        boolean bright_f = resultFlags[0];
        boolean slope_1_f = resultFlags[1];
        boolean slope_2_f = resultFlags[2];
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_BRIGHT, bright_f);
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_SLOPE_1, slope_1_f);
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_SLOPE_2, slope_2_f);

        // table-driven classification- step 2.1.8
        // DPM #2.1.8-1
        boolean land_f = sd.l1Flags.getSampleBit(pixelInfo.x, pixelInfo.y, L1_F_LAND);
        boolean is_cloud;

        boolean bright_toa_f = resultFlags[3];
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_BRIGHT_TOA, bright_toa_f);
        boolean high_mdsi = resultFlags[4];
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_HIGH_MDSI, high_mdsi);
        boolean bright_rc = resultFlags[5];
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_BRIGHT_RC, bright_rc);

        // new #2.1.8:
        if (!land_f) {
            boolean low_p_p1 = (pixelInfo.p1Pressure < pixelInfo.pbaroPressure - userDefinedP1PressureThreshold) &&
                               (sd.rhoToa[bb753][pixelInfo.index] > userDefinedRhoToa753Threshold);
            is_cloud = (bright_f || low_p_p1) && (!high_mdsi);
            targetTile.setSample(pixelInfo.x, pixelInfo.y, F_LOW_P_P1, low_p_p1);
        } else {
            float rhoToaRatio = sd.rhoToa[bb753][pixelInfo.index] / sd.rhoToa[bb775][pixelInfo.index];
            boolean low_p_pscatt = (pixelInfo.pscattPressure < userDefinedPScattPressureThreshold) &&
                                   (rhoToaRatio > userDefinedRhoToaRatio753775Threshold);
            is_cloud = (bright_f || low_p_pscatt) && (!(high_mdsi && bright_f));
            targetTile.setSample(pixelInfo.x, pixelInfo.y, F_LOW_P_PSCATT, low_p_pscatt);
        }
        boolean snow_ice = (high_mdsi && bright_f);
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_SNOW_ICE, snow_ice);
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_CLOUD, is_cloud);
    }

    private void setMdsi(SourceData sd, PixelInfo pixelInfo, Tile targetTile) {
        final float mdsi = computeMdsi(sd.rhoToa[bb865][pixelInfo.index], sd.rhoToa[bb890][pixelInfo.index]);
        targetTile.setSample(pixelInfo.x, pixelInfo.y, mdsi);
    }

    private AngleInfo getAngleInfo(SourceData dc, PixelInfo pixelInfo) {
        AngleInfo angleInfo = new AngleInfo();

        angleInfo.sins = Math.sin(dc.sza[pixelInfo.index] * MathUtils.DTOR);
        angleInfo.sinv = Math.sin(dc.vza[pixelInfo.index] * MathUtils.DTOR);
        angleInfo.coss = Math.cos(dc.sza[pixelInfo.index] * MathUtils.DTOR);
        angleInfo.cosv = Math.cos(dc.vza[pixelInfo.index] * MathUtils.DTOR);
        // delta azimuth in degree
        angleInfo.deltaAzimuth = HelperFunctions.computeAzimuthDifference(dc.vaa[pixelInfo.index],
                                                                          dc.saa[pixelInfo.index]);

        return angleInfo;
    }

    private double calcScatteringAngle(PixelInfo pixelInfo) {
        final AngleInfo ai = pixelInfo.angleInfo;
        // Compute the geometric conditions
        final double cosphi = Math.cos(ai.deltaAzimuth * MathUtils.DTOR);

        // scattering angle in degree
        return MathUtils.RTOD * Math.acos(-ai.coss * ai.cosv - ai.sins * ai.sinv * cosphi);
    }

    private double calcRhoToa442ThresholdTerm(PixelInfo pixelInfo) {
        final double thetaScatt = calcScatteringAngle(pixelInfo) * MathUtils.DTOR;
        return userDefinedRhoToa442Threshold + userDefinedDeltaRhoToa442Threshold *
                                               Math.cos(thetaScatt) * Math.cos(thetaScatt);
    }

    /**
     * Computes the slope of Rayleigh-corrected reflectance.
     *
     * @param pixelInfo    the pixel structure
     * @param result_flags the return values, <code>resultFlags[0]</code> contains low NN pressure flag (low_P_nn),
     *                     <code>resultFlags[1]</code> contains low polynomial pressure flag (low_P_poly),
     *                     <code>resultFlags[2]</code> contains pressure range flag (delta_p).
     */
    private void spec_slopes(SourceData dc, PixelInfo pixelInfo, boolean[] result_flags) {

        //Rayleigh phase function coefficients, PR in DPM
        final double[] phaseR = new double[RAYSCATT_NUM_SER];
        //Rayleigh optical thickness, tauR0 in DPM
        final double[] tauR = new double[L1_BAND_NUM];
        //Rayleigh corrected reflectance
        final double[] rhoAg = new double[L1_BAND_NUM];
        //Rayleigh correction
        final double[] rhoRay = new double[L1_BAND_NUM];

        final AngleInfo ai = pixelInfo.angleInfo;
        /* Rayleigh phase function Fourier decomposition */
        rayleighCorrection.phase_rayleigh(ai.coss, ai.cosv, ai.sins, ai.sinv, phaseR);

        double press = pixelInfo.ecmwfPressure; /* DPM #2.1.7-1 v1.1 */

        /* Rayleigh optical thickness */
        rayleighCorrection.tau_rayleigh(press, tauR); /* DPM #2.1.7-2 */

        /* Rayleigh reflectance - DPM #2.1.7-3 - v1.3 */
        rayleighCorrection.ref_rayleigh(ai.deltaAzimuth, dc.sza[pixelInfo.index], dc.vza[pixelInfo.index],
                                        ai.coss, ai.cosv, pixelInfo.airMass, phaseR, tauR, rhoRay);

        /* DPM #2.1.7-4 */
        for (int band = bb412; band <= bb900; band++) {
            rhoAg[band] = dc.rhoToa[band][pixelInfo.index] - rhoRay[band];
        }

        PixelId pixelId = new PixelId(auxData);
        boolean isLand = dc.l1Flags.getSampleBit(pixelInfo.x, pixelInfo.y, L1_F_LAND);
        /* Interpolate threshold on rayleigh corrected reflectance - DPM #2.1.7-9 */
        double rhorc_442_thr = pixelId.getRhoRC442thr(dc.sza[pixelInfo.index], dc.vza[pixelInfo.index], ai.deltaAzimuth, isLand);

        /* Derive bright flag by reflectance comparison to threshold - DPM #2.1.7-10 */
        boolean bright_f;

        /* Spectral slope processor.brr 1 */
        boolean slope1_f = pixelId.isSpectraSlope1Flag(rhoAg, dc.radiance[BAND_SLOPE_N_1].getSampleFloat(pixelInfo.x, pixelInfo.y));
        /* Spectral slope processor.brr 2 */
        boolean slope2_f = pixelId.isSpectraSlope2Flag(rhoAg, dc.radiance[BAND_SLOPE_N_2].getSampleFloat(pixelInfo.x, pixelInfo.y));

        boolean bright_toa_f = false;
        boolean bright_rc = (rhoAg[auxData.band_bright_n] > rhorc_442_thr)
                            || isSaturated(dc, pixelInfo.x, pixelInfo.y, BAND_BRIGHT_N, auxData.band_bright_n);
        if (dc.l1Flags.getSampleBit(pixelInfo.x, pixelInfo.y, L1_F_LAND)) {   /* land pixel */
            bright_f = bright_rc && slope1_f && slope2_f;
        } else {

            // test, 30.10.09:
            final double rhoThreshOffsetTerm = calcRhoToa442ThresholdTerm(pixelInfo);
            bright_toa_f = (dc.rhoToa[bb442][pixelInfo.index] > rhoThreshOffsetTerm);
            bright_f = bright_rc || bright_toa_f;
        }

        final float mdsi = computeMdsi(dc.rhoToa[bb865][pixelInfo.index], dc.rhoToa[bb890][pixelInfo.index]);
        boolean high_mdsi = (mdsi > userDefinedMDSIThreshold);

        result_flags[0] = bright_f;
        result_flags[1] = slope1_f;
        result_flags[2] = slope2_f;
        result_flags[3] = bright_toa_f;
        result_flags[4] = high_mdsi;
        result_flags[5] = bright_rc;
    }

    private float computeMdsi(float rhoToa865, float rhoToa885) {
        return (rhoToa865 - rhoToa885) / (rhoToa865 + rhoToa885);
    }

    private boolean isSaturated(SourceData sd, int x, int y, int radianceBandId, int bandId) {
        return sd.radiance[radianceBandId].getSampleFloat(x, y) > auxData.Saturation_L[bandId];
    }
    public static void addBitmasks(Product sourceProduct, Product targetProduct) {
        Mask[] bitmaskDefs = createBitmaskDefs(sourceProduct);

        int index = 0;
        for (Mask bitmaskDef : bitmaskDefs) {
            targetProduct.getMaskGroup().add(index++, bitmaskDef);
        }
    }

    private static Mask[] createBitmaskDefs(Product sourceProduct) {

        Mask[] bitmaskDefs = new Mask[10];

        int w = sourceProduct.getSceneRasterWidth();
        int h = sourceProduct.getSceneRasterHeight();

        bitmaskDefs[0] = Mask.BandMathsType.create("f_cloud", "IDEPIX final cloud flag", w, h, CLOUD_FLAGS + ".F_CLOUD",
                                                   Color.CYAN, 0.5f);
        bitmaskDefs[1] = Mask.BandMathsType.create("f_bright", "IDEPIX combined of old and second bright test", w, h,
                                                   CLOUD_FLAGS + ".F_BRIGHT", new Color(0, 153, 153), 0.5f);
        bitmaskDefs[2] = Mask.BandMathsType.create("f_bright_rc", "IDEPIX old bright test", w, h,
                                                   CLOUD_FLAGS + ".F_BRIGHT_RC", new Color(204, 255, 204), 0.5f);
        bitmaskDefs[3] = Mask.BandMathsType.create("f_low_p_pscatt", "IDEPIX test on apparent scattering (over ocean)",
                                                   w, h, CLOUD_FLAGS + ".F_LOW_P_PSCATT", new Color(153, 153, 0), 0.5f);
        bitmaskDefs[4] = Mask.BandMathsType.create("f_low_p_p1", "IDEPIX test on P1 (over land)", w, h,
                                                   CLOUD_FLAGS + ".F_LOW_P_P1", Color.GRAY, 0.5f);
        bitmaskDefs[5] = Mask.BandMathsType.create("f_slope_1", "IDEPIX old slope 1 test", w, h,
                                                   CLOUD_FLAGS + ".F_SLOPE_1", Color.PINK, 0.5f);
        bitmaskDefs[6] = Mask.BandMathsType.create("f_slope_2", "IDEPIX old slope 2 test", w, h,
                                                   CLOUD_FLAGS + ".F_SLOPE_2", new Color(153, 0, 153), 0.5f);
        bitmaskDefs[7] = Mask.BandMathsType.create("f_bright_toa", "IDEPIX second bright test", w, h,
                                                   CLOUD_FLAGS + ".F_BRIGHT_TOA", Color.LIGHT_GRAY, 0.5f);
        bitmaskDefs[8] = Mask.BandMathsType.create("f_high_mdsi",
                                                   "IDEPIX MDSI above threshold (warning: not sufficient for snow detection)",
                                                   w, h, CLOUD_FLAGS + ".F_HIGH_MDSI", Color.blue, 0.5f);
        bitmaskDefs[9] = Mask.BandMathsType.create("f_snow_ice", "IDEPIX snow/ice flag", w, h,
                                                   CLOUD_FLAGS + ".F_SNOW_ICE", Color.DARK_GRAY, 0.5f);

        return bitmaskDefs;
    }

    private static class SourceData {

        private float[][] rhoToa;
        private Tile[] radiance;
        private short[] detectorIndex;
        private float[] sza;
        private float[] vza;
        private float[] saa;
        private float[] vaa;
        private float[] altitude;
        private float[] ecmwfPressure;
        private Tile l1Flags;
    }

    private static class PixelInfo {

        int index;
        int x;
        int y;
        double airMass;
        float ecmwfPressure;
        float pbaroPressure;
        float p1Pressure;
        float pscattPressure;
        float ctp;
        AngleInfo angleInfo;
    }

    private static class AngleInfo {

        double sins;
        double sinv;
        double coss;
        double cosv;
        double deltaAzimuth;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisClassificationOp.class);
        }
    }
}
