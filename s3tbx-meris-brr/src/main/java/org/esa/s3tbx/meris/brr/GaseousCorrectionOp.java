/*
 * $Id: GaseousCorrectionOp.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
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
import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
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
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.util.Map;


@OperatorMetadata(alias = "Meris.GaseousCorrection",
        version = "2.3.4",
        internal = true,
        authors = "Marco Zühlke",
        copyright = "(c) 2007 by Brockmann Consult",
        description = "MERIS L2 gaseous absorbtion correction.")
public class GaseousCorrectionOp extends MerisBasisOp implements Constants {

    public static final String RHO_NG_BAND_PREFIX = "rho_ng";
    public static final String GAS_FLAGS = "gas_flags";
    public static final String TG_BAND_PREFIX = "tg";

    public static final int F_DO_CORRECT = 0;
    public static final int F_SUN70 = 1;
    public static final int F_ORINP0 = 2;
    public static final int F_OROUT0 = 3;

    private L2AuxData auxData;

    private Band flagBand;
    private Band[] rhoNgBands;
    private Band[] tgBands;

    private GaseousAbsorptionCorrection gasCor;

    @SourceProduct(alias="l1b")
    private Product l1bProduct;
    @SourceProduct(alias="rhotoa")
    private Product rhoToaProduct;
    @SourceProduct(alias="cloud")
    private Product cloudProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter
    boolean correctWater = false;
    @Parameter
    boolean exportTg = false;

    private Band[] rhoToaBands;

    @Override
    public void initialize() throws OperatorException {
        try {
            auxData = L2AuxDataProvider.getInstance().getAuxdata(l1bProduct);
            gasCor = new GaseousAbsorptionCorrection(auxData);
        } catch (Exception e) {
            throw new OperatorException("could not load L2Auxdata", e);
        }
        rhoToaBands = new Band[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];

        for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            rhoToaBands[i] = rhoToaProduct.getBand(Rad2ReflOp.RHO_TOA_BAND_PREFIX + "_" + (i + 1));
        }

        createTargetProduct();
    }

    private void createTargetProduct() {
    	targetProduct = createCompatibleProduct(rhoToaProduct, "MER", "MER_L2");
    	
    	rhoNgBands = new Band[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
        for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            rhoNgBands[i] = targetProduct.addBand(RHO_NG_BAND_PREFIX + "_" + (i + 1), ProductData.TYPE_FLOAT32);
            ProductUtils.copySpectralBandProperties(rhoToaProduct.getBandAt(i), rhoNgBands[i]);
            rhoNgBands[i].setNoDataValueUsed(true);
            rhoNgBands[i].setNoDataValue(BAD_VALUE);
        }

        flagBand = targetProduct.addBand(GAS_FLAGS, ProductData.TYPE_INT8);
        FlagCoding flagCoding = createFlagCoding();
        flagBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);
        
        if (exportTg) {
            tgBands = new Band[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
        	for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
                tgBands[i] = targetProduct.addBand(TG_BAND_PREFIX + "_" + (i + 1), ProductData.TYPE_FLOAT32);
                tgBands[i].setNoDataValueUsed(true);
                tgBands[i].setNoDataValue(BAD_VALUE);
            }
        }
        if (l1bProduct.getPreferredTileSize() != null) {
            targetProduct.setPreferredTileSize(l1bProduct.getPreferredTileSize());
        }
    }

    public static FlagCoding createFlagCoding() {
        FlagCoding flagCoding = new FlagCoding(GAS_FLAGS);
        flagCoding.addFlag("F_DO_CORRECT", BitSetter.setFlag(0, F_DO_CORRECT),
                           "Indicates if gaseous correction is applied for given pixel");
        flagCoding.addFlag("F_SUN70", BitSetter.setFlag(0, F_SUN70),
                           "Sun zenith angle is out of range and set to upper limit of 70deg.");
        flagCoding.addFlag("F_ORINP0", BitSetter.setFlag(0, F_ORINP0),
                           "One or more TOA input values out of range (i.e. < 0)");
        flagCoding.addFlag("F_OROUT0", BitSetter.setFlag(0, F_OROUT0),
                           "Output status != 0 (gaseous correction failed)");
        return flagCoding;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        try {
            Tile detectorIndex = getSourceTile(l1bProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME), rectangle);
			Tile sza = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME), rectangle);
			Tile vza = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME), rectangle);
			Tile altitude = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME), rectangle);
			Tile ecmwfOzone = getSourceTile(l1bProduct.getTiePointGrid("ozone"), rectangle);
			Tile l1Flags = getSourceTile(l1bProduct.getBand(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME), rectangle);
			
			Tile[] rhoToa = new Tile[rhoToaBands.length];
			for (int i = 0; i < rhoToa.length; i++) {
                rhoToa[i] = getSourceTile(rhoToaBands[i], rectangle);
			}
			
			Tile cloudFlags = getSourceTile(cloudProduct.getBand(CloudClassificationOp.CLOUD_FLAGS), rectangle);

            Tile gasFlags = targetTiles.get(flagBand);
            Tile[] rhoNg = new Tile[rhoNgBands.length];
            Tile[] tg = null;
            if (exportTg) {
                tg = new Tile[tgBands.length];
            }
            for (int i = 0; i < rhoNgBands.length; i++) {
                rhoNg[i] = targetTiles.get(rhoNgBands[i]);
                if (exportTg) {
                	tg[i] = targetTiles.get(tgBands[i]);
                }
            }

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y += Constants.SUBWIN_HEIGHT) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x += Constants.SUBWIN_WIDTH) {
                    final int xWinEnd = Math.min(rectangle.x + rectangle.width, x + Constants.SUBWIN_WIDTH) - 1;
                    final int yWinEnd = Math.min(rectangle.y + rectangle.height, y + Constants.SUBWIN_HEIGHT) - 1;
                    boolean correctPixel = false;
					boolean correctWaterPixel = false;
					double[] dSumrho = new double[L1_BAND_NUM]; /* accumulator for rho above water */
					
					for (int iy = y; iy <= yWinEnd; iy++) {
					    for (int ix = x; ix <= xWinEnd; ix++) {
					        if (!l1Flags.getSampleBit(ix, iy, L1_F_INVALID) &&
					                !cloudFlags.getSampleBit(ix, iy, CloudClassificationOp.F_CLOUD) &&
					                (correctWater || altitude.getSampleFloat(ix, iy) >= -50.0 || l1Flags.getSampleBit(ix, iy, L1_F_LAND))) {
					
					            correctPixel = true;
					            gasFlags.setSample(ix, iy, F_DO_CORRECT, true);
					
					            /* v4.2: average radiances for water pixels */
					            if (!l1Flags.getSampleBit(ix, iy, L1_F_LAND)) {
					                correctWaterPixel = true;
					                for (int bandId = bb753; bandId <= bb900; bandId++) {
					                    dSumrho[bandId] += rhoToa[bandId].getSampleFloat(ix, iy);
					                }
					            }
					        } else {
					        	writeBadValue(rhoNg, ix, iy);
					        }
					    }
					}
					
					if (correctPixel) {
					    /* v4.2 average TOA radiance */
					    double etaAverageForWater = 0.0;
					    double x2AverageForWater = 0.0;
					    boolean iOrinp0 = false;
					    if (correctWaterPixel) {
					        if ((dSumrho[bb753] > 0.0) && (dSumrho[bb760] > 0.0)) {
					            etaAverageForWater = dSumrho[bb760] / dSumrho[bb753];
					        } else {
					            iOrinp0 = true;
					            etaAverageForWater = 1.0;
					        }
					
					        if ((dSumrho[bb890] > 0.0) && (dSumrho[bb900] > 0.0)) {
					            x2AverageForWater = dSumrho[bb900] / dSumrho[bb890];
					        } else {
					            iOrinp0 = true;
					            x2AverageForWater = 1.0;
					        }
					    }
					
					    /* V.2 APPLY GASEOUS ABSORPTION CORRECTION - DPM Step 2.6.12 */
					
					    /* ozone transmittance on 4x4 window - step 2.6.12.1 */
					    double[] T_o3 = new double[L1_BAND_NUM];   /* ozone transmission */
					    double airMass0 = HelperFunctions.calculateAirMass(vza.getSampleFloat(x, y), sza.getSampleFloat(x, y));
					    trans_o3(airMass0, ecmwfOzone.getSampleFloat(x, y), T_o3);
					
					    /* process each pixel */
					    for (int iy = y; iy <= yWinEnd; iy++) {
					        for (int ix = x; ix <= xWinEnd; ix++) {
					            if (gasFlags.getSampleBit(ix, iy, F_DO_CORRECT)) {
                                    /* band ratios eta, x2 */
					                double eta;
                                    double x2;

                                    /* test SZA - v4.2 */
					                if (sza.getSampleFloat(ix, iy) > auxData.TETAS_LIM) {
					                    gasFlags.setSample(ix, iy, F_SUN70, true);
					                }
					
					                /* gaseous transmittance gasCor : writes rho-ag field - v4.2 */
					                /* do band ratio for land pixels with full exception handling */
					                if (l1Flags.getSampleBit(ix, iy, L1_F_LAND)) {
					                    if ((rhoToa[bb753].getSampleFloat(ix, iy) > 0.0) && (rhoToa[bb760].getSampleFloat(ix, iy) > 0.0)) {
					                        eta = rhoToa[bb760].getSampleFloat(ix, iy) / rhoToa[bb753].getSampleFloat(ix, iy);    //o2
					                    } else {
					                        eta = 1.0;
					                        gasFlags.setSample(ix, iy, F_ORINP0, true);
					                    }
					                    /* DPM #2.6.12.3-1 */
					                    if ((rhoToa[bb890].getSampleFloat(ix, iy) > 0.0) && (rhoToa[bb900].getSampleFloat(ix, iy) > 0.0)) {
					                        x2 = rhoToa[bb900].getSampleFloat(ix, iy) / rhoToa[bb890].getSampleFloat(ix, iy);   //h2o
					                    } else {
					                        x2 = 1.0;
					                        gasFlags.setSample(ix, iy, F_ORINP0, true);
					                    }
					                } else { /* water pixels */
					                    eta = etaAverageForWater;
					                    x2 = x2AverageForWater;
					                    gasFlags.setSample(ix, iy, F_ORINP0, iOrinp0);
					                }
                                    int status = gasCor.gas_correction(ix, iy, T_o3, eta, x2,
                                                                       rhoToa,
                                                                       detectorIndex.getSampleInt(ix, iy),
                                                                       rhoNg,
                                                                       tg,
                                                                       cloudFlags.getSampleBit(ix, iy,
                                                                                               CloudClassificationOp.F_PCD_POL_P));

                                    /* exception handling */
					                gasFlags.setSample(ix, iy, F_OROUT0, status != 0);
					            } else {
					                writeBadValue(rhoNg, ix, iy);
					            }
					        }
					    }
					}
                }
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Computes the ozone transmittance for a given pixel. This routine should be called every 4x4 pixels.
     * <p/>
     * Reference: DPM equation #2.6.12.1-2<br>
     * Uses: <br>
     * {@link L2AuxData#tauO3_norm variables.tauO3_norm} <br>
     *
     * @param airMass air mass
     * @param ozone   total ozone contents
     * @param T_o3    ozone optical thickness in 15 bands
     */
    private void trans_o3(double airMass, double ozone, double[] T_o3) {
        for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
            /* DPM #2.6.12.1-2 */
            T_o3[bandId] = Math.exp(-ozone / 1000.0 * airMass * auxData.tauO3_norm[bandId]);
        }
    }

    private void writeBadValue(Tile[] rhoNg, int x, int y) {
        for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
            rhoNg[bandId].setSample(x, y, BAD_VALUE);
        }
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(GaseousCorrectionOp.class);
        }
    }
}