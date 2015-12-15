package org.esa.s3tbx.idepix.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.s3tbx.meris.brr.*;
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
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.util.Map;


@OperatorMetadata(alias = "idepix.operators.IdepixRayleighCorrection",
                  version = "2.2",
                  internal = true,
                  authors = "Marco Zuehlke, Olaf Danne",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "MERIS L2 rayleigh correction.")
public class IdepixRayleighCorrectionOp extends MerisBasisOp implements Constants {

    public static final String BRR_BAND_PREFIX = "brr";
    public static final String RAYLEIGH_REFL_BAND_PREFIX = "rayleigh_refl";
    public static final String RAY_CORR_FLAGS = "ray_corr_flags";

    protected L2AuxData auxData;
    protected RayleighCorrection rayleighCorrection;

    private Band isLandBand;
    private Band[] brrBands;
    private Band[] brrNormalizedBands;
    private Band[] rayleighReflBands;
    private Band flagBand;

    private Band[] transRvBands;
    private Band[] transRsBands;
    private Band[] tauRBands;
    private Band[] sphAlbRBands;

    static final int[] BANDS_TO_CORRECT = new int[]{
            bb1, bb2, bb3, bb4, bb5, bb6, bb7, bb8, bb9, bb10, bb12, bb13, bb14
    };

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "input")
    private Product gascorProduct;
    @SourceProduct(alias = "rhotoa")
    private Product rad2reflProduct;
    @SourceProduct(alias = "land")
    private Product landProduct;
    @SourceProduct(alias = "cloud", optional = true)
    private Product cloudProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter
    boolean correctWater = false;
    @Parameter(defaultValue = LandClassificationOp.LAND_FLAGS + ".F_LANDCONS")
    String landExpression;
    @Parameter
    boolean exportRayCoeffs = false;
    @Parameter
    boolean exportRhoR = false;
    // these bands are needed as input for the spectral unmixing to retrieve mixed pixel flag:
    @Parameter
    boolean exportBrrNormalized = false;


    @Override
    public void initialize() throws OperatorException {
        try {
            auxData = L2AuxDataProvider.getInstance().getAuxdata(l1bProduct);
            rayleighCorrection = new RayleighCorrection(auxData);
        } catch (Exception e) {
            throw new OperatorException("could not load L2Auxdata", e);
        }
        createTargetProduct();
    }

    private void createTargetProduct() throws OperatorException {
        targetProduct = createCompatibleProduct(l1bProduct, "MER", "MER_L2");

        brrBands = addBandGroup(BRR_BAND_PREFIX, "");
        if (exportBrrNormalized) {
            brrNormalizedBands = addBandGroup(BRR_BAND_PREFIX, "_n");
        }
        if (exportRhoR) {
            rayleighReflBands = addBandGroup(RAYLEIGH_REFL_BAND_PREFIX, "");
        }

        flagBand = targetProduct.addBand(RAY_CORR_FLAGS, ProductData.TYPE_INT16);
        FlagCoding flagCoding = createFlagCoding();
        flagBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);

        if (exportRayCoeffs) {
            transRvBands = addBandGroup("transRv", "");
            transRsBands = addBandGroup("transRs", "");
            tauRBands = addBandGroup("tauR", "");
            sphAlbRBands = addBandGroup("sphAlbR", "");
        }

        BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
        bandDescriptor.name = "land_mask";
        bandDescriptor.expression = landExpression;
        bandDescriptor.type = ProductData.TYPESTRING_INT8;
        BandMathsOp bandArithmeticOp = new BandMathsOp();
        bandArithmeticOp.setSourceProduct(landProduct);
        bandArithmeticOp.setTargetBandDescriptors(bandDescriptor);

        isLandBand = bandArithmeticOp.getTargetProduct().getBandAt(0);
        if (l1bProduct.getPreferredTileSize() != null) {
            targetProduct.setPreferredTileSize(l1bProduct.getPreferredTileSize());
        }
    }

    private Band[] addBandGroup(String prefix, String suffix) {
        Band[] bands = new Band[L1_BAND_NUM];
        for (int bandId : BANDS_TO_CORRECT) {
            Band targetBand = targetProduct.addBand(prefix + "_" + (bandId + 1) + suffix, ProductData.TYPE_FLOAT32);
            ProductUtils.copySpectralBandProperties(l1bProduct.getBandAt(bandId), targetBand);
            targetBand.setNoDataValueUsed(true);
            targetBand.setNoDataValue(BAD_VALUE);
            bands[bandId] = targetBand;
        }
        return bands;
    }

    public static FlagCoding createFlagCoding() {
        FlagCoding flagCoding = new FlagCoding(RAY_CORR_FLAGS);
        int bitIndex = 0;
        for (int bandId : BANDS_TO_CORRECT) {
            flagCoding.addFlag("F_NEGATIV_BRR_" + (bandId + 1), BitSetter.setFlag(0, bitIndex), null);
            bitIndex++;
        }
        return flagCoding;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws
                                                                                                       OperatorException {
        try {
            Tile sza = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME), rectangle);
            Tile vza = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME), rectangle);
            Tile saa = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME), rectangle);
            Tile vaa = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME),
                                     rectangle);
            Tile altitude = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME),
                                          rectangle);
            Tile ecmwfPressure = getSourceTile(l1bProduct.getTiePointGrid("atm_press"), rectangle);

            Tile[] rhoNg = new Tile[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
            for (int bandId : BANDS_TO_CORRECT) {
                Band band = gascorProduct.getBand(GaseousCorrectionOp.RHO_NG_BAND_PREFIX + "_" + (bandId + 1));
                rhoNg[bandId] = getSourceTile(band, rectangle);
            }

            Tile[] rad2ReflTile = new Tile[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
            for (int bandId : BANDS_TO_CORRECT) {
                Band band = rad2reflProduct.getBand(Rad2ReflOp.RHO_TOA_BAND_PREFIX + "_" + (bandId + 1));
                rad2ReflTile[bandId] = getSourceTile(band, rectangle);
            }

            Tile isLandCons = getSourceTile(isLandBand, rectangle);

            Tile[] transRvData = null;
            Tile[] transRsData = null;
            Tile[] tauRData = null;
            Tile[] sphAlbRData = null;
            if (exportRayCoeffs) {
                transRvData = getTargetTileGroup(transRvBands, targetTiles);
                transRsData = getTargetTileGroup(transRsBands, targetTiles);
                tauRData = getTargetTileGroup(tauRBands, targetTiles);
                sphAlbRData = getTargetTileGroup(sphAlbRBands, targetTiles);
            }
            Tile[] rayleigh_refl = null;
            if (exportRhoR) {
                rayleigh_refl = getTargetTileGroup(rayleighReflBands, targetTiles);
            }
            Tile[] brr = getTargetTileGroup(brrBands, targetTiles);
            Tile[] brrNormalized = null;
            if (exportBrrNormalized) {
                brrNormalized = getTargetTileGroup(brrNormalizedBands, targetTiles);
            }
            Tile brrFlags = targetTiles.get(flagBand);

            boolean[][] do_corr = new boolean[SUBWIN_HEIGHT][SUBWIN_WIDTH];
            // rayleigh phase function coefficients, PR in DPM
            double[] phaseR = new double[3];
            // rayleigh optical thickness, tauR0 in DPM
            double[] tauR = new double[L1_BAND_NUM];
            // rayleigh reflectance, rhoR4x4 in DPM
            double[] rhoR = new double[L1_BAND_NUM];
            // rayleigh down transmittance, T_R_thetas_4x4
            double[] transRs = new double[L1_BAND_NUM];
            // rayleigh up transmittance, T_R_thetav_4x4
            double[] transRv = new double[L1_BAND_NUM];
            // rayleigh spherical albedo, SR_4x4
            double[] sphAlbR = new double[L1_BAND_NUM];

            Tile surfacePressureTile = null;
            Tile cloudTopPressureTile = null;
            Tile cloudFlagsTile = null;
            if (cloudProduct != null) {
                surfacePressureTile = getSourceTile(cloudProduct.getBand(MerisClassificationOp.PRESSURE_SURFACE),
                                                    rectangle);
                cloudTopPressureTile = getSourceTile(cloudProduct.getBand(MerisClassificationOp.PRESSURE_CTP),
                                                     rectangle);
                cloudFlagsTile = getSourceTile(cloudProduct.getBand(MerisClassificationOp.CLOUD_FLAGS),
                                               rectangle);
            }

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y += Constants.SUBWIN_HEIGHT) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x += Constants.SUBWIN_WIDTH) {
                    final int xWinEnd = Math.min(rectangle.x + rectangle.width, x + Constants.SUBWIN_WIDTH) - 1;
                    final int yWinEnd = Math.min(rectangle.y + rectangle.height, y + Constants.SUBWIN_HEIGHT) - 1;
                    boolean correctPixel = false;

                    for (int iy = y; iy <= yWinEnd; iy++) {
                        for (int ix = x; ix <= xWinEnd; ix++) {
                            if (rhoNg[0].getSampleFloat(ix, iy) != BAD_VALUE &&
                                (correctWater || isLandCons.getSampleBoolean(ix, iy))) {
                                correctPixel = true;
                                do_corr[iy - y][ix - x] = true;
                            } else {
                                do_corr[iy - y][ix - x] = false;
                                for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
                                    if (bandId != bb11 && bandId != bb15) {
                                        brr[bandId].setSample(ix, iy, BAD_VALUE);
                                        if (exportBrrNormalized) {
                                            brrNormalized[bandId].setSample(ix, iy, BAD_VALUE);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (correctPixel) {
                        /* average geometry, ozone for window DPM : just use corner pixel ! */
                        final float szaSampleFloat = sza.getSampleFloat(x, y);
                        final float vzaSampleFloat = vza.getSampleFloat(x, y);

                        final double szaRad = szaSampleFloat * MathUtils.DTOR;
                        final double vzaRad = vzaSampleFloat * MathUtils.DTOR;
                        final double sins = Math.sin(szaRad);
                        final double sinv = Math.sin(vzaRad);
                        final double mus = Math.cos(szaRad);
                        final double muv = Math.cos(vzaRad);
                        final double deltaAzimuth = HelperFunctions.computeAzimuthDifference(vaa.getSampleFloat(x, y),
                                                                                             saa.getSampleFloat(x, y));

                        /*
                              * 2. Rayleigh corrections (DPM section 7.3.3.3.2, step 2.6.15)
                              */
                        double press = HelperFunctions.correctEcmwfPressure(ecmwfPressure.getSampleFloat(x, y),
                                                                            altitude.getSampleFloat(x, y),
                                                                            auxData.press_scale_height); /* DPM #2.6.15.1-3 */
                        final double airMass = HelperFunctions.calculateAirMassMusMuv(muv, mus);

                        /* correct pressure in presence of clouds */
                        if (cloudProduct != null) {
                            final boolean isCloud = cloudFlagsTile.getSampleBit(x, y, CloudClassificationOp.F_CLOUD);
                            if (isCloud) {
                                double ctp = cloudTopPressureTile.getSampleDouble(x, y);
                                double sp = surfacePressureTile.getSampleDouble(x, y);
                                final double pressureCorrectionCloud = ctp / sp;
                                press *= pressureCorrectionCloud;
                            }
                        }

                        /* Rayleigh phase function Fourier decomposition */
                        rayleighCorrection.phase_rayleigh(mus, muv, sins, sinv, phaseR);

                        /* Rayleigh optical thickness */
                        rayleighCorrection.tau_rayleigh(press, tauR);

                        /* Rayleigh reflectance*/
                        rayleighCorrection.ref_rayleigh(deltaAzimuth, szaSampleFloat, vzaSampleFloat, mus, muv,
                                                        airMass, phaseR, tauR, rhoR);

                        /* Rayleigh transmittance */
                        rayleighCorrection.trans_rayleigh(mus, tauR, transRs);
                        rayleighCorrection.trans_rayleigh(muv, tauR, transRv);

                        /* Rayleigh spherical albedo */
                        rayleighCorrection.sphAlb_rayleigh(tauR, sphAlbR);

                        /* process each pixel */
                        for (int iy = y; iy <= yWinEnd; iy++) {
                            for (int ix = x; ix <= xWinEnd; ix++) {
                                if (do_corr[iy - y][ix - x]) {
                                    /* Rayleigh correction for each pixel */
                                    rayleighCorrection.corr_rayleigh(rhoR, sphAlbR, transRs, transRv,
                                                                     rhoNg, brr, ix, iy); /*  (2.6.15.4) */

                                    /* flag negative Rayleigh-corrected reflectance */
                                    for (int bandId : BANDS_TO_CORRECT) {
                                        if (brr[bandId].getSampleFloat(ix, iy) <= 0.0) {
                                            /* set annotation flag for reflectance product - v4.2 */
                                            brrFlags.setSample(ix, iy, (bandId <= bb760 ? bandId : bandId - 1), true);
                                        }
                                    }
                                    if (exportRhoR) {
                                        for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
                                            if (bandId != bb11 && bandId != bb15) {
                                                rayleigh_refl[bandId].setSample(ix, iy, rhoR[bandId]);
                                            }
                                        }
                                    }
                                    if (exportRayCoeffs) {
                                        for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
                                            if (bandId != bb11 && bandId != bb15) {
                                                transRvData[bandId].setSample(ix, iy, transRv[bandId]);
                                                transRsData[bandId].setSample(ix, iy, transRs[bandId]);
                                                tauRData[bandId].setSample(ix, iy, tauR[bandId]);
                                                sphAlbRData[bandId].setSample(ix, iy, sphAlbR[bandId]);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    if (cloudProduct != null) {
                        final boolean isCloud = cloudFlagsTile.getSampleBit(x, y, CloudClassificationOp.F_CLOUD);
                        final boolean isIce = cloudFlagsTile.getSampleBit(x, y, LandClassificationOp.F_ICE);

                        for (int bandId : BANDS_TO_CORRECT) {
                            final float rad2refl = rad2ReflTile[bandId].getSampleFloat(x, y);
                            if (isCloud) {
                                final float surfacePressure = surfacePressureTile.getSampleFloat(x, y);
                                final float cloudTopPressure = cloudTopPressureTile.getSampleFloat(x, y);
                                brr[bandId].setSample(x, y, rad2refl * cloudTopPressure / surfacePressure);
                            } else if (isIce) {
                                brr[bandId].setSample(x, y, rad2refl);
                            }
                        }
                    }
                }
            }

            if (exportBrrNormalized) {
                for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                    for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                        for (int bandId : BANDS_TO_CORRECT) {
                            final float brrValue = brr[bandId].getSampleFloat(x, y);
                            if (brrValue != BAD_VALUE) {
                                final double cosSza = Math.cos(sza.getSampleFloat(x, y) * MathUtils.DTOR);
                                brrNormalized[bandId].setSample(x, y, brrValue / cosSza);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private Tile[] getTargetTileGroup(Band[] bands, Map<Band, Tile> targetTiles) {
        final Tile[] bandRaster = new Tile[L1_BAND_NUM];
        for (int i = 0; i < bands.length; i++) {
            Band band = bands[i];
            if (band != null) {
                bandRaster[i] = targetTiles.get(band);
            }
        }
        return bandRaster;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(IdepixRayleighCorrectionOp.class);
        }
    }
}

