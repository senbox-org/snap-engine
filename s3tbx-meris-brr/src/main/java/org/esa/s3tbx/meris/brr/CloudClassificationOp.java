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
package org.esa.s3tbx.meris.brr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.s3tbx.meris.dpm.PixelId;
import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataException;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataProvider;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
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

//import PixelId;
//import Constants;
//import L2AuxData;
//import L2AuxDataException;
//import L2AuxDataProvider;


@OperatorMetadata(alias = "Meris.CloudClassification",
                  version = "2.3.4",
                  internal = true,
                  authors = "Marco Zühlke",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "MERIS L2 cloud classification.")
public class CloudClassificationOp extends MerisBasisOp implements Constants {

    public static final String CLOUD_FLAGS = "cloud_classif_flags";
    public static final String PRESSURE_CTP = "ctp_ipf";
    public static final String PRESSURE_SURFACE = "surface_press_ipf";

    private static final int BAND_BRIGHT_N = 0;
    private static final int BAND_SLOPE_N_1 = 1;
    private static final int BAND_SLOPE_N_2 = 2;

    public static final int F_CLOUD = 0;
    public static final int F_BRIGHT = 1;
    public static final int F_LOW_NN_P = 2;
    public static final int F_PCD_NN_P = 3;
    public static final int F_LOW_POL_P = 4;
    public static final int F_PCD_POL_P = 5;
    public static final int F_CONFIDENCE_P = 6;
    public static final int F_SLOPE_1 = 7;
    public static final int F_SLOPE_2 = 8;

    private L2AuxData auxData;

    private RayleighCorrection rayleighCorrection;

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "rhotoa")
    private Product rhoToaProduct;
    @SourceProduct(alias = "ctp", optional = true)
    private Product ctpProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter(description = "If 'true' the algorithm will compute L2 Pressures.", defaultValue = "true")
    public boolean l2Pressures = true;
    @Parameter(description = "If 'true' the algorithm will compute L2 Cloud detection flags.", defaultValue = "true")
    public boolean l2CloudDetection = true;
    private PixelId pixelId;


    @Override
    public void initialize() throws OperatorException {
        try {
            auxData = L2AuxDataProvider.getInstance().getAuxdata(l1bProduct);
        } catch (L2AuxDataException e) {
            throw new OperatorException("Could not load L2Auxdata", e);
        }
        pixelId = new PixelId(auxData);
        rayleighCorrection = new RayleighCorrection(auxData);
        createTargetProduct();
    }

    private void createTargetProduct() {
        targetProduct = createCompatibleProduct(l1bProduct, "MER", "MER_L2");

        Band cloudFlagBand = targetProduct.addBand(CLOUD_FLAGS, ProductData.TYPE_INT16);
        FlagCoding flagCoding = createFlagCoding();
        cloudFlagBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);

        if (ctpProduct != null) {
            targetProduct.addBand(PRESSURE_CTP, ProductData.TYPE_FLOAT32);
            targetProduct.addBand(PRESSURE_SURFACE, ProductData.TYPE_FLOAT32);
            //Band pEcmwfBand = targetProduct.addBand(PRESSURE_ECMWF, ProductData.TYPE_FLOAT32);
        }

        if (l1bProduct.getPreferredTileSize() != null) {
            targetProduct.setPreferredTileSize(l1bProduct.getPreferredTileSize());
        }
    }

    public static FlagCoding createFlagCoding() {
        FlagCoding flagCoding = new FlagCoding(CLOUD_FLAGS);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, F_CLOUD), null);
        flagCoding.addFlag("F_BRIGHT", BitSetter.setFlag(0, F_BRIGHT), null);
        flagCoding.addFlag("F_LOW_NN_P", BitSetter.setFlag(0, F_LOW_NN_P), null);
        flagCoding.addFlag("F_PCD_NN_P", BitSetter.setFlag(0, F_PCD_NN_P), null);
        flagCoding.addFlag("F_LOW_POL_P", BitSetter.setFlag(0, F_LOW_POL_P), null);
        flagCoding.addFlag("F_PCD_POL_P", BitSetter.setFlag(0, F_PCD_POL_P), null);
        flagCoding.addFlag("F_CONFIDENCE_P", BitSetter.setFlag(0, F_CONFIDENCE_P), null);
        flagCoding.addFlag("F_SLOPE_1", BitSetter.setFlag(0, F_SLOPE_1), null);
        flagCoding.addFlag("F_SLOPE_2", BitSetter.setFlag(0, F_SLOPE_2), null);
        return flagCoding;
    }

    private SourceData loadSourceTiles(Rectangle rectangle) throws OperatorException {

        SourceData sd = new SourceData();
        sd.rhoToa = new float[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS][0];
        sd.radiance = new Tile[3];

        for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            sd.rhoToa[i] = (float[]) getSourceTile(rhoToaProduct.getBand(Rad2ReflOp.RHO_TOA_BAND_PREFIX + "_" + (i + 1)), rectangle).getRawSamples().getElems();
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
        sd.sza = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME), rectangle).getRawSamples().getElems();
        sd.vza = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME), rectangle).getRawSamples().getElems();
        sd.saa = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME), rectangle).getRawSamples().getElems();
        sd.vaa = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME), rectangle).getRawSamples().getElems();
        sd.altitude = (float[]) getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME), rectangle).getRawSamples().getElems();
        sd.ecmwfPressure = (float[]) getSourceTile(l1bProduct.getTiePointGrid("atm_press"), rectangle).getRawSamples().getElems();
        sd.l1Flags = getSourceTile(l1bProduct.getBand(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME), rectangle);

        return sd;
    }

    // TODO methis is synchronized becasue the JNBN version shipping with BEAM 4.7 is not thread safe
    // TODO remove 'synchnorized' statement if this changes
    @Override
    public synchronized void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        Rectangle rectangle = targetTile.getRectangle();
        pm.beginTask("Processing frame...", rectangle.height + 1);
        try {
            SourceData sd = loadSourceTiles(rectangle);

            Tile ctpTile = null;
            if (ctpProduct != null) {
                ctpTile = getSourceTile(ctpProduct.getBand("cloud_top_press"), rectangle);
            }

            PixelInfo pixelInfo = new PixelInfo();
            int i = 0;
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                pixelInfo.y = y;
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    if (!sd.l1Flags.getSampleBit(x, y, L1_F_INVALID)) {
                        pixelInfo.x = x;
                        pixelInfo.index = i;
                        pixelInfo.airMass = HelperFunctions.calculateAirMass(
                                sd.vza[i], sd.sza[i]);
                        if (sd.l1Flags.getSampleBit(x, y, L1_F_LAND)) {
                            // ECMWF pressure is only corrected for positive
                            // altitudes and only for land pixels
                            pixelInfo.ecmwfPressure = HelperFunctions
                                    .correctEcmwfPressure(sd.ecmwfPressure[i],
                                                          sd.altitude[i],
                                                          auxData.press_scale_height);
                        } else {
                            pixelInfo.ecmwfPressure = sd.ecmwfPressure[i];
                        }
                        if (ctpTile != null) {
                            float ctp = ctpTile.getSampleFloat(x, y);
                            if (band.getName().equals(CLOUD_FLAGS) && l2CloudDetection) {
                                classifyCloud(sd, ctp, pixelInfo, targetTile);
                            }
                            if (band.getName().equals(PRESSURE_SURFACE) && l2Pressures) {
                                setCloudPressureSurface(sd, pixelInfo, targetTile);
                            }
                            if (band.getName().equals(PRESSURE_CTP) && l2Pressures) {
                                setCloudPressureTop(ctp, pixelInfo, targetTile);
                                //if (band.getName().equals(PRESSURE_ECMWF)) {
                                //    setCloudPressureEcmwf(sd, pixelInfo, targetTile);
                                //}
                            }
                        } else {
                            classifyCloud(sd, -1, pixelInfo, targetTile);
                        }
                    }
                    i++;
                }
                pm.worked(1);
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        } finally {
            pm.done();
        }
    }

    public void setCloudPressureSurface(SourceData sd, PixelInfo pixelInfo, Tile targetTile) {
        PixelId.Pressure press = pixelId.computePressure(sd.rhoToa[bb753][pixelInfo.index],
                                                         sd.rhoToa[bb760][pixelInfo.index],
                                                         pixelInfo.airMass,
                                                         sd.detectorIndex[pixelInfo.index]);
        targetTile.setSample(pixelInfo.x, pixelInfo.y, Math.max(0.0, press.value));
    }

    public void setCloudPressureTop(float ctp, PixelInfo pixelInfo, Tile targetTile) {
        targetTile.setSample(pixelInfo.x, pixelInfo.y, ctp);
    }

//    public void setCloudPressureEcmwf(SourceData sd, PixelInfo pixelInfo, Tile targetTile) {
//        final ReturnValue press = new ReturnValue();
//
//        Comp_Pressure(sd, pixelInfo, press);
//        targetTile.setSample(pixelInfo.x, pixelInfo.y, Math.max(0.0, pixelInfo.ecmwfPressure));
//    }

    public void classifyCloud(SourceData sd, float ctp, PixelInfo pixelInfo, Tile targetTile) {
        //boolean pcd_poly = Comp_Pressure(pixel) != 0;
        PixelId.Pressure press = pixelId.computePressure(sd.rhoToa[bb753][pixelInfo.index],
                                                         sd.rhoToa[bb760][pixelInfo.index],
                                                         pixelInfo.airMass,
                                                         sd.detectorIndex[pixelInfo.index]);
        boolean pcd_poly = press.error;

        /* apply thresholds on pressure- step 2.1.2 */
        double delta_press_thresh = pixelId.getPressureThreshold(sd.sza[pixelInfo.index], sd.vza[pixelInfo.index],
                                                                 sd.l1Flags.getSampleBit(pixelInfo.x, pixelInfo.y, L1_F_LAND));
        boolean[] pressureThreshFlags = pixelId.getPressureThreshFlags(pixelInfo.ecmwfPressure, press.value, ctp, delta_press_thresh);
        boolean low_P_nn = pressureThreshFlags[0];
        boolean low_P_poly = pressureThreshFlags[1];
        boolean delta_p = pressureThreshFlags[2];

        /* keep for display-debug - added for v2.1 */
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_LOW_NN_P, low_P_nn);
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_PCD_NN_P, true); /* DPM #2.1.5-25 */
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_LOW_POL_P, low_P_poly);
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_PCD_POL_P, pcd_poly); /* DPM #2.1.12-12 */
        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_CONFIDENCE_P, delta_p);

        final boolean[] resultFlags = new boolean[3];

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
        boolean is_cloud = is_cloudy(land_f,
                                     bright_f,
                                     low_P_nn, low_P_poly, delta_p,
                                     slope_1_f, slope_2_f,
                                     true, pcd_poly);

        targetTile.setSample(pixelInfo.x, pixelInfo.y, F_CLOUD, is_cloud);
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

        double sins = Math.sin(dc.sza[pixelInfo.index] * MathUtils.DTOR);
        double sinv = Math.sin(dc.vza[pixelInfo.index] * MathUtils.DTOR);
        double mus = Math.cos(dc.sza[pixelInfo.index] * MathUtils.DTOR);
        double muv = Math.cos(dc.vza[pixelInfo.index] * MathUtils.DTOR);
        final double deltaAzimuth = HelperFunctions.computeAzimuthDifference(dc.vaa[pixelInfo.index], dc.saa[pixelInfo.index]);

        /* Rayleigh phase function Fourier decomposition */
        rayleighCorrection.phase_rayleigh(mus, muv, sins, sinv, phaseR);

        double press = pixelInfo.ecmwfPressure; /* DPM #2.1.7-1 v1.1 */

        /* Rayleigh optical thickness */
        rayleighCorrection.tau_rayleigh(press, tauR); /* DPM #2.1.7-2 */

        /* Rayleigh reflectance - DPM #2.1.7-3 - v1.3 */
        rayleighCorrection.ref_rayleigh(deltaAzimuth, dc.sza[pixelInfo.index], dc.vza[pixelInfo.index],
                                        mus, muv, pixelInfo.airMass, phaseR, tauR, rhoRay);

        /* DPM #2.1.7-4 */
        for (int band = bb412; band <= bb900; band++) {
            rhoAg[band] = dc.rhoToa[band][pixelInfo.index] - rhoRay[band];
        }


        boolean isLand = dc.l1Flags.getSampleBit(pixelInfo.x, pixelInfo.y, L1_F_LAND);
        /* Interpolate threshold on rayleigh corrected reflectance - DPM #2.1.7-9 */
        double rhorc_442_thr = pixelId.getRhoRC442thr(dc.sza[pixelInfo.index], dc.vza[pixelInfo.index], deltaAzimuth, isLand);

        boolean bright_f = pixelId.isBrightFlag(rhoAg, rhorc_442_thr,
                                                dc.radiance[BAND_BRIGHT_N].getSampleFloat(pixelInfo.x, pixelInfo.y));

        /* Spectral slope processor.brr 1 */
        boolean slope1_f = pixelId.isSpectraSlope1Flag(rhoAg, dc.radiance[BAND_SLOPE_N_1].getSampleFloat(pixelInfo.x, pixelInfo.y));
        /* Spectral slope processor.brr 2 */
        boolean slope2_f = pixelId.isSpectraSlope2Flag(rhoAg, dc.radiance[BAND_SLOPE_N_2].getSampleFloat(pixelInfo.x, pixelInfo.y));


        result_flags[0] = bright_f;
        result_flags[1] = slope1_f;
        result_flags[2] = slope2_f;
    }

    /**
     * Table driven cloud classification decision.
     * <p/>
     * <b>DPM Ref.:</b> Level 2, Step 2.1.8 <br> <b>MEGS Ref.:</b> file classcloud.c, function class_cloud  <br>
     *
     * @return <code>true</code> if cloud flag shall be set
     */
    private boolean is_cloudy(boolean land_f, boolean bright_f,
                              boolean low_P_nn, boolean low_P_poly,
                              boolean delta_p, boolean slope_1_f,
                              boolean slope_2_f, boolean pcd_nn,
                              boolean pcd_poly) {
        boolean is_cloud;
        int index = 0;

        /* set bits of index according to inputs */
        index = BitSetter.setFlag(index, CC_BRIGHT, bright_f);
        index = BitSetter.setFlag(index, CC_LOW_P_NN, low_P_nn);
        index = BitSetter.setFlag(index, CC_LOW_P_PO, low_P_poly);
        index = BitSetter.setFlag(index, CC_DELTA_P, delta_p);
        index = BitSetter.setFlag(index, CC_PCD_NN, pcd_nn);
        index = BitSetter.setFlag(index, CC_PCD_PO, pcd_poly);
        index = BitSetter.setFlag(index, CC_SLOPE_1, slope_1_f);
        index = BitSetter.setFlag(index, CC_SLOPE_2, slope_2_f);
        index &= 0xff;

        /* readRecord decision table */
        if (land_f) {
            is_cloud = auxData.land_decision_table[index]; /* DPM #2.1.8-1 */
        } else {
            is_cloud = auxData.water_decision_table[index]; /* DPM #2.1.8-2 */
        }

        return is_cloud;
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
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CloudClassificationOp.class);
        }
    }
}
