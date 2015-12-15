/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
import org.esa.s3tbx.meris.brr.HelperFunctions;
import org.esa.s3tbx.meris.brr.RayleighCorrection;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataProvider;
import org.esa.s3tbx.util.math.FractIndex;
import org.esa.s3tbx.util.math.Interp;
import org.esa.s3tbx.util.math.LUT;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.awt.image.Raster;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * Operator for computing aerosol apparent pressure (LISE algorithm).
 * todo: cleanup!!! --> e.g  move I/O methods to an auxdata class
 *
 * @author Olaf Danne
 * @version $Revision: 6824 $ $Date: 2009-11-03 16:02:02 +0100 (Di, 03 Nov 2009) $
 */
@OperatorMetadata(alias = "idepix.operators.LisePressure",
                  version = "2.2",
                  internal = true,
                  authors = "Olaf Danne",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "This operator computes aerosol apparent pressure with LISE algorithm.")
public class LisePressureOp extends BasisOp {

    @SourceProduct(alias = "l1b", description = "The source product.")
    Product sourceProduct;
    @SourceProduct(alias = "rhotoa")
    private Product rhoToaProduct;
    @TargetProduct(description = "The target product.")
    Product targetProduct;

    @Parameter(description = "If 'true' the algorithm will apply straylight correction.", defaultValue = "false")
    public boolean straylightCorr = false;
    @Parameter(description = "If 'true' the algorithm will compute LISE P1.", defaultValue = "true")
    public boolean outputP1 = true;
    @Parameter(description = "If 'true' the algorithm will compute LISE surface pressure.", defaultValue = "true")
    public boolean outputPressureSurface = true;
    @Parameter(description = "If 'true' the algorithm will compute LISE P2.", defaultValue = "true")
    public boolean outputP2 = true;
    @Parameter(description = "If 'true' the algorithm will compute LISE PScatt.", defaultValue = "true")
    public boolean outputPScatt = true;

    private static final String P_1_LISE = "p1_lise";
    public static final String PRESSURE_LISE_P1 = P_1_LISE;
    private static final String SURFACE_PRESS_LISE = "surface_press_lise";
    private static final String PSCATT_LISE = "pscatt_lise";
    public static final String PRESSURE_LISE_PSCATT = PSCATT_LISE;
    public static final String PRESSURE_LISE_PSURF = SURFACE_PRESS_LISE;
    private static final String P_2_LISE = "p2_lise";
    public static final String PRESSURE_LISE_P2 = P_2_LISE;

    private static final String INVALID_EXPRESSION = "l1_flags.INVALID";
    private static final String INVALID_EXPRESSION_LAND = "l1_flags.INVALID or not l1_flags.LAND_OCEAN";

    private static final String O2_RAYLEIGH_TRANSMITTANCES_FILE_NAME = "transmittances_O2_Ray_OCEAN_21f.d";
    private static final String O2_ATM_TRANSMITTANCES_FILE_NAME = "transmittances_O2_RSf_OCEAN_21f.d";
    private static final String O2_FRESNEL_TRANSMITTANCES_FILE_NAME = "transmittances_O2_fresnel_OCEAN_21f.d";
    private static final String O2_ATM_AEROSOL_TRANSMITTANCES_FILE_NAME = "transmittances_O2_atm_aer_OCEAN_21f.d";
    private static final String SPECTRAL_COEFFICIENTS_FILE_NAME = "meris_band_o2.d";
    private static final String FRESNEL_COEFFICIENTS_FILE_NAME = "fresnel_coeff.d";
    private static final String C_COEFFICIENTS_FILE_NAME = "c_coeff_lise.d";
    private static final String AIRMASSES_LISE_FILE_NAME = "airmasses_lise.d";
    private static final String RHO_TOA_LISE_FILE_NAME = "rho_toa_lise.d";
    private static final String APF_JUNGE_FILE_NAME = "apf_junge_10.d";
    private static final String STRAYLIGHT_COEFF_FILE_NAME = "stray_ratio.d";
    private static final String STRAYLIGHT_CORR_WAVELENGTH_FILE_NAME = "lambda.d";

    private static final int BB760 = 10;
    private static final int NFILTER = 21;
    private static final int NLAYER = 21;

    private static final int C_NUM_M = 6;
    private static final int C_NUM_RHO = 6;

    private static final int NPIXEL = 4625;
    private double[] spectralCoefficients = new double[NPIXEL];

    private static final int NFRESNEL = 91;
    private double[] fresnelCoefficients = new double[NFRESNEL];

    private static final int NJUNGE = 181;
    private double[] apfJunge = new double[NJUNGE];

    private static final int DETECTOR_LENGTH_RR = 925;
    private double[] straylightCoefficients = new double[DETECTOR_LENGTH_RR]; // reduced resolution only!
    private double[] straylightCorrWavelengths = new double[DETECTOR_LENGTH_RR];

    /**
     * Rayleigh Scattering Coeff series, number of coefficient Order
     */
    private static final int RAYSCATT_NUM_ORD = 4;
    /**
     * Rayleigh Scattering Coeff series, number of Series
     */
    private static final int RAYSCATT_NUM_SER = 3;

    private static final double standardSeaSurfacePressure = 1013.25;

    // central wavelength for each detector at 761 nm
    static final double[] o2FilterWavelengths = {
            760.7d, 760.8d, 760.9d, 761.0d, 761.1d, 761.2d, 761.3d,
            761.4d, 761.5d, 761.6d, 761.7d, 761.8d, 761.9d, 762.0d,
            762.1d, 762.2d, 762.3d, 762.4d, 762.5d, 762.6d, 762.7d
    };

    // gaussian angle grid point for the Rayleigh TO2
    static final double[] gaussianAngles = {
            2.84d, 6.52d, 10.22d, 13.93d, 17.64d, 21.35d, 25.06d, 28.77d,
            32.48d, 36.19d, 39.90d, 43.61d, 47.32d, 51.03d, 54.74d, 58.46d,
            62.17d, 65.88d, 69.59d, 73.30d, 77.01d, 80.72d, 84.43d, 88.14d
    };

    private double[][][] to2Ray = new double[NFILTER][24][24];           // Rayleigh TO2
    private double[][][][] to2Atm = new double[NFILTER][NLAYER][24][24]; // Atmospheric TO2 at 21 pressure levels
    private double[][][] to2Fresnel = new double[NFILTER][24][24];       // Fresnel TO2
    private double[][][] to2AtmAerosol = new double[NFILTER][24][24];    // Aerosol atmospheric TO2
    private double[] pressureLevels = new double[NLAYER];                // pressure standard levels
    private double[] cCoeff = new double[NFILTER * 6 * 6];                      // C coefficients (LISE)
    private double[] airMassesLise = new double[6];                      // air masses (LISE)
    private double[] rhoToaLise = new double[6];                         // rhoToa (LISE)

//    protected RayleighCorrection rayleighCorrection;

    // index 0: W0 wavelength for band 11
    // indices 1,2:  bracketted wavemengths for W0
//    private double[] band11CentralWavelengths = new double[3];


    private L2AuxData auxData;
    private VirtualBandOpImage invalidImage;
    private VirtualBandOpImage invalidLandImage;
    private LUT coeffLUT;
    private Band psurfLiseBand;
    private Band p1LiseBand;
    private Band p2LiseBand;
    private Band pscattLiseBand;


    @Override
    public void initialize() throws OperatorException {
        if (sourceProduct != null) {
            createTargetProduct();
        }

        try {
            initL2AuxData();
            readLiseAuxData();
            if (straylightCorr) {
                readStraylightCoeff();
                readStraylightCorrWavelengths();
            }
        } catch (Exception e) {
            throw new OperatorException("Failed to load aux data:\n" + e.getMessage());
        }
    }

    /**
     * This method computes the different Lise pressures for a given pixel
     *
     * @param rayleighCorrection
     * @param auxData
     * @param pressureIndex:     - 0: press_toa
     *                           - 1: press_surface
     *                           - 2: press_bottom_rayleigh
     *                           - 3: press_bottom_fresnel
     * @param szaDeg             - sun zenith angle (degrees)
     * @param vzaDeg             - view zenith angle (degrees)
     * @param csza               - cosine of sza
     * @param cvza               - cosine of vza
     * @param ssza               - sine of sza
     * @param svza               - sine of vza
     * @param azimDiff           - difference sun-view azimuth
     * @param rhoToa10           - reflectance band 10
     * @param rhoToa11           - reflectance band 11
     * @param rhoToa12           - reflectance band 12
     * @param w0
     * @param airMass
     *
     * @return
     */
    public double computeLisePressures(RayleighCorrection rayleighCorrection,
                                       L2AuxData auxData, final int pressureIndex,
                                       final double szaDeg, final double vzaDeg, final double csza, final double cvza,
                                       final double ssza, final double svza, final double azimDiff,
                                       final double rhoToa10, final double rhoToa11, final double rhoToa12,
                                       final double w0,
                                       final double airMass) {

        // Determine nearest filter
        int filterIndex = getNearestFilterIndex(w0);

        // Compute the geometric conditions
        final double cosphi = Math.cos(azimDiff);

        // scattering angle
        final double theta = MathUtils.RTOD * Math.acos(-csza * cvza - ssza * svza * cosphi);
        // scattering angle for the coupling scattering-Fresnel reflection
        final double xsi = MathUtils.RTOD * Math.acos(csza * cvza - ssza * svza * cosphi);

        final int indsza = (int) Math.round(szaDeg);
        final int indvza = (int) Math.round(vzaDeg);
        final int indtheta = (int) Math.round(theta);
        final int indxsi = (int) Math.round(xsi);

        // Determine nearest angle for SZA & VZA
        final int gaussIndexS = getNearestGaussIndex(szaDeg);
        final int gaussIndexV = getNearestGaussIndex(vzaDeg);

        // Compute the reference RO_TOA at 761
        final double rhoRef = linearInterpol(761.0d, 753.0d, 778.0d, rhoToa10, rhoToa12);

        // Ratio of the two bands
        double to2Ratio = rhoToa11 / rhoRef;

        // Computation of the apparent pressure P1
        // (an intermediate result - not needed to proceed)
        if (pressureIndex == 0) {
            return getPressure(o2FilterWavelengths[filterIndex],
                               o2FilterWavelengths[filterIndex + 1],
                               w0, szaDeg, vzaDeg,
                               filterIndex, gaussIndexS, gaussIndexV, to2Ratio);
        }

        // get pSurf using 21x6x6 C coefficients:
        if (pressureIndex == 1) {
            final FractIndex[] cIndex = FractIndex.createArray(2);
            Interp.interpCoord(airMass, coeffLUT.getTab(2), cIndex[1]);
            Interp.interpCoord(rhoToa10, coeffLUT.getTab(1), cIndex[0]);
            double[][][] cLut = (double[][][]) coeffLUT.getJavaArray();
            double cCoeffResult = Interp.interpolate(cLut[filterIndex], cIndex);
            double eta = rhoToa11 / rhoToa10;
            eta *= cCoeffResult;
            // Computation of the surface pressure pSurf
            return getPressure(o2FilterWavelengths[filterIndex],
                               o2FilterWavelengths[filterIndex + 1],
                               w0, szaDeg, vzaDeg,
                               filterIndex, gaussIndexS, gaussIndexV, eta);
        }

        // Compute Rayleigh reflectance at 761
        double ray761;
        if (rayleighCorrection != null && auxData != null) {
            ray761 = computeRayleighReflectanceCh11(rayleighCorrection, auxData, szaDeg, vzaDeg,
                                                    ssza, svza, csza, cvza, azimDiff);
        } else {
            ray761 = computeRayleighReflectance(szaDeg, vzaDeg, theta, standardSeaSurfacePressure);
        }

        // Compute the Rayleigh O2 transmittance
        final double trO2 = computeO2Transmittance(o2FilterWavelengths[filterIndex],
                                                   o2FilterWavelengths[filterIndex + 1], to2Ray,
                                                   w0, szaDeg, vzaDeg,
                                                   filterIndex, gaussIndexS, gaussIndexV);

        // // Rayleigh correction on the O2 transmittance
        final double to2RCorrected = (rhoToa11 - ray761 * trO2) / (rhoRef - ray761);

        // Determination of the aerosol apparent pressure
        // after Rayleigh correction
        // (an intermediate result - not needed to proceed)
        if (pressureIndex == 2) {
            return getPressure(o2FilterWavelengths[filterIndex],
                               o2FilterWavelengths[filterIndex + 1],
                               w0, szaDeg, vzaDeg,
                               filterIndex, gaussIndexS, gaussIndexV,
                               to2RCorrected);
        }

        // Determination of the aerosol pressure after surface
        // correction:

        // Compute the aerosol O2 transmittance
        final double trAerosol = computeO2Transmittance(o2FilterWavelengths[filterIndex],
                                                        o2FilterWavelengths[filterIndex + 1],
                                                        to2AtmAerosol, w0,
                                                        szaDeg, vzaDeg, filterIndex, gaussIndexS,
                                                        gaussIndexV);

        // Compute the aerosol fresnel O2 transmittance for direct to diffuse
        final double trFresnel1 = computeO2Transmittance(o2FilterWavelengths[filterIndex],
                                                         o2FilterWavelengths[filterIndex + 1],
                                                         to2Fresnel, w0,
                                                         szaDeg, vzaDeg, filterIndex, gaussIndexS,
                                                         gaussIndexV);

        // Compute the aerosol fresnel O2 transmittance for diffuse to direct
        final double trFresnel2 = computeO2Transmittance(o2FilterWavelengths[filterIndex],
                                                         o2FilterWavelengths[filterIndex + 1],
                                                         to2Fresnel, w0,
//				vzaDeg, szaDeg, filterIndex, gaussIndexS,     // LB, 02.10.09
                                                         szaDeg, vzaDeg, filterIndex, gaussIndexS,
                                                         gaussIndexV);

        // Compute the APF ratio between forward and backward scattering
        final double pfb = apfJunge[indxsi] / apfJunge[indtheta];

        // Compute the contribution of the aerosol-Fresnel
        // This contribution is an output for further flag
        final double caf = 1.0 + pfb * (fresnelCoefficients[indsza] + fresnelCoefficients[indvza]);

        // Correction of the O2 transmittance by the coupling
        //   aerosol-Fresnel
        final double xx = (trAerosol + pfb *
                                       (trFresnel2 * fresnelCoefficients[indvza] + trFresnel1 * fresnelCoefficients[indsza])) / caf;

        final double to2Rf = to2RCorrected * trAerosol / xx;

        return getPressure(o2FilterWavelengths[filterIndex],
                           o2FilterWavelengths[filterIndex + 1],
                           w0, szaDeg, vzaDeg,
                           filterIndex, gaussIndexS, gaussIndexV,
                           to2Rf);
    }

    /*
     * This method reads additional auxdata provided by LISE.
     */
    void readLiseAuxData() throws IOException {
        readSpectralCoefficients();
        readFresnelCoefficients();
        readCCoefficients();
        readApfJunge();
        readO2RayleighTransmittances();
        readO2AtmTransmittances();
        readO2FresnelTransmittances();
        readO2AtmAerosolTransmittances();
    }

    /*
    * This method initialises the L2 auxdata.
    */
    private void initL2AuxData() throws OperatorException {
        try {
            L2AuxDataProvider auxdataProvider = L2AuxDataProvider.getInstance();
            auxData = auxdataProvider.getAuxdata(sourceProduct);
//            rayleighCorrection = new RayleighCorrection(auxData);
        } catch (Exception e) {
            throw new OperatorException("Failed to load L2AuxData:\n" + e.getMessage(), e);
        }

    }

    /*
    * This method reads the spectral characterization coefficients
    */
    private void readSpectralCoefficients() throws IOException {
        final InputStream inputStream = LisePressureOp.class.getResourceAsStream(SPECTRAL_COEFFICIENTS_FILE_NAME);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            for (int i = 0; i < spectralCoefficients.length / 4; i++) {
                double coeffAverage = 0.0;
                for (int j = 0; j < 4; j++) {
                    String line = bufferedReader.readLine();
                    line = line.trim();
                    coeffAverage += Float.parseFloat(line);
                }
                spectralCoefficients[i] = coeffAverage / 4.0;
            }
        } catch (IOException e) {
            throw new OperatorException("Failed to load Spectral Coefficients:\n" + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new OperatorException("Failed to load Spectral Coefficients:\n" + e.getMessage(), e);
        } finally {
            bufferedReader.close();
        }
    }

    /*
     * This method reads the C coefficients
     */
    private void readCCoefficients() throws IOException {
        readAirMassesLise();
        readRhoToaLise();

        final InputStream inputStream = LisePressureOp.class.getResourceAsStream(C_COEFFICIENTS_FILE_NAME);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            int index = 0;
            for (int i = 0; i < NFILTER; i++) {
                bufferedReader.readLine();
                for (int k = 0; k < 6; k++) {
                    String line = bufferedReader.readLine();
                    line = line.trim();
                    StringTokenizer st = new StringTokenizer(line, " ", false);
                    int mIndex = 0;
                    while (st.hasMoreTokens() && mIndex < 7) {
                        String token = st.nextToken();
                        if (mIndex > 0) {
                            int cCoeffInt = Integer.parseInt(token);
                            if (cCoeffInt == -999) {
                                cCoeffInt = 1000;
                            }
                            cCoeff[index] = 0.001 * cCoeffInt;
                            index++;
                        }
                        mIndex++;
                    }
                }
            }
            final int[] cCoeffSizes = new int[]{NFILTER, C_NUM_M, C_NUM_RHO};
            coeffLUT = new LUT(cCoeffSizes, cCoeff);
            coeffLUT.setTab(0, null); // no tabulated values needed for 1st dimension
            coeffLUT.setTab(2, airMassesLise);
            coeffLUT.setTab(1, rhoToaLise);

        } catch (IOException e) {
            throw new OperatorException("Failed to load C Coefficients:\n" + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new OperatorException("Failed to load C Coefficients:\n" + e.getMessage(), e);
        } finally {
            bufferedReader.close();
        }

    }

    /*
    * This method reads the O2 Rayleigh transmittance
    */
    private void readO2RayleighTransmittances() throws IOException {
        final InputStream inputStream = LisePressureOp.class.getResourceAsStream(O2_RAYLEIGH_TRANSMITTANCES_FILE_NAME);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            for (int i = 0; i < NFILTER; i++) {
                bufferedReader.readLine();
                for (int k = 0; k < 24; k++) {
                    String line = bufferedReader.readLine();
                    line = line.trim();
                    StringTokenizer st = new StringTokenizer(line, " ", false);
                    int mIndex = 0;
                    while (st.hasMoreTokens() && mIndex < 24) {
                        String token = st.nextToken();
                        to2Ray[i][k][mIndex] = Double.parseDouble(token);
                        mIndex++;
                    }
                }
            }
        } catch (IOException e) {
            throw new OperatorException("Failed to load O2 Rayleigh Transmittances:\n" + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new OperatorException("Failed to load O2 Rayleigh Transmittances:\n" + e.getMessage(), e);
        } finally {
            bufferedReader.close();
        }
    }

    /*
    * This method reads the O2 Atmospheric transmittance
    */
    private void readO2AtmTransmittances() throws IOException {
        final InputStream inputStream = LisePressureOp.class.getResourceAsStream(O2_ATM_TRANSMITTANCES_FILE_NAME);
        final String exMsg = String.format("Failed to load '%s'.", O2_ATM_TRANSMITTANCES_FILE_NAME);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            for (int i = 0; i < NLAYER; i++) {
                String line = bufferedReader.readLine();
                line = line.trim();
                pressureLevels[i] = Double.parseDouble(line);
            }
            bufferedReader.readLine();
            for (int i = 0; i < NLAYER; i++) {
                bufferedReader.readLine();
                for (int j = 0; j < NFILTER; j++) {
                    for (int k = 0; k < 24; k++) {
                        String line = bufferedReader.readLine();
                        line = line.trim();
                        StringTokenizer st = new StringTokenizer(line, " ", false);
                        int mIndex = 0;
                        while (st.hasMoreTokens() && mIndex < 24) {
                            String token = st.nextToken();
                            to2Atm[i][j][k][mIndex] = Double.parseDouble(token);
                            mIndex++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new OperatorException(exMsg, e);
        } catch (NumberFormatException e) {
            throw new OperatorException(exMsg, e);
        } finally {
            bufferedReader.close();
        }
    }

    /*
     * This method reads the O2 Aerosol Fresnel transmittance
     */
    private void readO2FresnelTransmittances() throws IOException {
        final InputStream inputStream = LisePressureOp.class.getResourceAsStream(O2_FRESNEL_TRANSMITTANCES_FILE_NAME);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            for (int i = 0; i < NFILTER; i++) {
                bufferedReader.readLine();
                for (int k = 0; k < 24; k++) {
                    String line = bufferedReader.readLine();
                    line = line.trim();
                    StringTokenizer st = new StringTokenizer(line, " ", false);
                    int mIndex = 0;
                    while (st.hasMoreTokens() && mIndex < 24) {
                        String token = st.nextToken();
                        to2Fresnel[i][k][mIndex] = Double.parseDouble(token);
                        mIndex++;
                    }
                }
            }
        } catch (IOException e) {
            throw new OperatorException("Failed to load O2 Fresnel Transmittances:\n" + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new OperatorException("Failed to load O2 Fresnel Transmittances:\n" + e.getMessage(), e);
        } finally {
            bufferedReader.close();
        }
    }

    /*
     * This method reads the O2 aerosol atmospheric transmittance for Ha=2km
     */
    private void readO2AtmAerosolTransmittances() throws IOException {
        final InputStream inputStream = LisePressureOp.class.getResourceAsStream(
                O2_ATM_AEROSOL_TRANSMITTANCES_FILE_NAME);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            for (int i = 0; i < NFILTER; i++) {
                bufferedReader.readLine();
                for (int k = 0; k < 24; k++) {
                    String line = bufferedReader.readLine();
                    line = line.trim();
                    StringTokenizer st = new StringTokenizer(line, " ", false);
                    int mIndex = 0;
                    while (st.hasMoreTokens() && mIndex < 24) {
                        String token = st.nextToken();
                        to2AtmAerosol[i][k][mIndex] = Double.parseDouble(token);
                        mIndex++;
                    }
                }
            }
        } catch (IOException e) {
            throw new OperatorException("Failed to load O2 Atmospheric Aerosol Transmittances:\n" + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new OperatorException("Failed to load O2 Atmospheric Aerosol Transmittances:\n" + e.getMessage(), e);
        } finally {
            bufferedReader.close();
        }
    }

    /*
    * This method reads the APF of the Junge aerosol model nb 10
    */
    private void readApfJunge() throws IOException {
        readAuxdataArray(APF_JUNGE_FILE_NAME, apfJunge);

    }

    /*
     * This method reads the straylight correction coefficients (RR only!)
     */
    private void readStraylightCoeff() throws IOException {
        readAuxdataArray(STRAYLIGHT_COEFF_FILE_NAME, straylightCoefficients);
    }

    /*
     * This method reads the straylight correction wavelengths (RR only!)
     */
    private void readStraylightCorrWavelengths() throws IOException {
        readAuxdataArray(STRAYLIGHT_CORR_WAVELENGTH_FILE_NAME, straylightCorrWavelengths);
    }

    /*
    * This method reads the LISE rho toa values
    */
    private void readRhoToaLise() throws IOException {
        readAuxdataArray(RHO_TOA_LISE_FILE_NAME, rhoToaLise);
    }

    /*
    * This method reads the LISE air masses
    */
    private void readAirMassesLise() throws IOException {
        readAuxdataArray(AIRMASSES_LISE_FILE_NAME, airMassesLise);
    }

    /*
    * This method reads the Fresnel coefficients
    */
    private void readFresnelCoefficients() throws IOException {
        readAuxdataArray(FRESNEL_COEFFICIENTS_FILE_NAME, fresnelCoefficients);
    }

    private void readAuxdataArray(String fileName, double[] array) throws IOException {
        final InputStream inputStream = LisePressureOp.class.getResourceAsStream(fileName);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            for (int i = 0; i < array.length; i++) {
                String line = bufferedReader.readLine();
                line = line.trim();
                array[i] = Float.parseFloat(line);
            }
        } finally {
            bufferedReader.close();
        }
    }

    /*
    * This method returns the index of the nearest Gauss angle
    */
    int getNearestGaussIndex(double thetaS) {
        int index = -1;

        if (thetaS <= gaussianAngles[0]) {
            index = 0;  // bug fixed: was 1 before (LB, 01.10.09) !!
        } else if (thetaS >= gaussianAngles[22]) {
            index = 22;
        } else {
            double a = 1000.0d;
            for (int i = 0; i < 24; i++) {
                double d = Math.abs(thetaS - gaussianAngles[i]);
                if (d < a) {
                    a = d;
                    index = i;
                }
            }
        }

        return index;
    }

    /*
     * This method returns the index of the nearest O2 filter wavelength
     */
    int getNearestFilterIndex(double w0) {
        int index = -1;

        if (w0 <= o2FilterWavelengths[0]) {
            index = 0;  // bug fixed: was 1 before (LB, 01.10.09) !!
        } else if (w0 >= o2FilterWavelengths[NFILTER - 1]) {
            index = NFILTER - 2;
        } else {
            for (int i = 1; i < NFILTER; i++) { // bug fixed  (LB, 01.10.09)
                if (o2FilterWavelengths[i] >= w0) {
                    index = i - 1;
                    break;
                }
            }
        }

        return index;
    }

    /*
     * This method computes the pressure for:
     * 1) The geometrical conditions (SZA, VZA) by interpolation on the 24 Gaussian angles
     * 2) at the central wavelength of the detector by interpolation on 21 filters
     *
     * @param w0
     * @param thetaS      - sun zenith angle
     * @param thetaV      - view zenith angle
     * @param filterIndex - index of nearest filter
     * @param isza        - index of nearest gaussian angle for thetaS
     * @param ivza        - index of nearest gaussian angle for thetaV
     * @param ratio       - toa11/toa10
     *
     */
    private double getPressure(double centralWvl1, double centralWvl2,
                               double w0, double thetaS, double thetaV, int filterIndex, int isza, int ivza,
                               double ratio) {

        // first filter
        double x0 = thetaV;
        double x1 = gaussianAngles[ivza];
        double x2 = gaussianAngles[ivza + 1];

        double y1 = computePressure(filterIndex, isza, ivza, ratio);
        double y2 = computePressure(filterIndex, isza, ivza + 1, ratio);
        double z1 = linearInterpol(x0, x1, x2, y1, y2);
        y1 = computePressure(filterIndex, isza + 1, ivza, ratio);
        y2 = computePressure(filterIndex, isza + 1, ivza + 1, ratio);
        double z2 = linearInterpol(x0, x1, x2, y1, y2);

        x0 = thetaS;
        x1 = gaussianAngles[isza];
        x2 = gaussianAngles[isza + 1];
        double s1 = linearInterpol(x0, x1, x2, z1, z2);

        // second filter
        x0 = thetaV;
        x1 = gaussianAngles[ivza];
        x2 = gaussianAngles[ivza + 1];

        y1 = computePressure(filterIndex + 1, isza, ivza, ratio);
        y2 = computePressure(filterIndex + 1, isza, ivza + 1, ratio);
        z1 = linearInterpol(x0, x1, x2, y1, y2);
        y1 = computePressure(filterIndex + 1, isza + 1, ivza, ratio);
        y2 = computePressure(filterIndex + 1, isza + 1, ivza + 1, ratio);
        z2 = linearInterpol(x0, x1, x2, y1, y2);

        x0 = thetaS;
        x1 = gaussianAngles[isza];
        x2 = gaussianAngles[isza + 1];
        double s2 = linearInterpol(x0, x1, x2, z1, z2);

        return linearInterpol(w0, centralWvl1, centralWvl2, s1, s2);
    }

    /*
     * This method returns the value of the pressure from the 761/753 ratio
     *
     */
    private double computePressure(int filterIndex, int isza2, int ivza2, double ratio) {

        final double[][][] to2AtmFilterIndex = to2Atm[filterIndex];
        double t1 = Math.log(to2AtmFilterIndex[0][isza2][ivza2]);
        double p1 = pressureLevels[0];
        double t = Math.log(ratio);

        double slope;
        double t2;
        double p2;
        double surfacePressure; // surface pressure to compute

        for (int i = 1; i < NLAYER; i++) {
            p2 = pressureLevels[i];
            t2 = Math.log(to2AtmFilterIndex[i][isza2][ivza2]);
            if (t >= t2) {
                slope = (p2 - p1) / (t2 - t1);
                surfacePressure = p2 + slope * (t - t2);
                return surfacePressure;
            } else {
                t1 = t2;
                p1 = p2;
            }
        }
        p1 = pressureLevels[NLAYER - 2];
        p2 = pressureLevels[NLAYER - 1];
        t1 = Math.log(to2AtmFilterIndex[NLAYER - 2][isza2][ivza2]);
        t2 = Math.log(to2AtmFilterIndex[NLAYER - 1][isza2][ivza2]);

        slope = (p2 - p1) / (t2 - t1);
        surfacePressure = p2 + slope * (t - t2);

        return surfacePressure;
    }

    private double computeRayleighReflectance(double thetas, double thetav, double theta, double pressure) {

        final double conv = Math.acos(-1.0d) / 180.0;
        final double xx = 4.0 * Math.cos(thetas * conv) * Math.cos(thetav * conv);
        final double nPressure = pressure / 1013.25;
        final double l1 = 0.0246 * 0.75 * (1.0 + Math.pow(Math.cos(theta * conv), 2.0));

        return nPressure * l1 / xx;
    }

    private double computeRayleighReflectanceCh11(RayleighCorrection rayleighCorrection, L2AuxData auxData,
                                                  double szaDeg,
                                                  double vzaDeg,
                                                  double sins, double sinv, double mus, double muv,
                                                  double azimDiff) {
        final double sza = szaDeg * MathUtils.DTOR;
        final double vza = vzaDeg * MathUtils.DTOR;
        final double azimDiffDeg = azimDiff * MathUtils.RTOD;

        final double airMass = HelperFunctions.calculateAirMassMusMuv(muv, mus);

        // rayleigh phase function coefficients, PR in DPM
        double[] phaseR = new double[3];
        // rayleigh optical thickness, tauR0 in DPM
        double tauRayleighCh11 = auxData.tau_R[11];

        /* Rayleigh phase function Fourier decomposition */
        rayleighCorrection.phase_rayleigh(mus, muv, sins, sinv, phaseR);

        /* Rayleigh reflectance*/
        return ref_rayleigh_ch11(auxData, azimDiffDeg, sza, vza, mus, muv,
                                 airMass, phaseR, tauRayleighCh11);
    }

    private double ref_rayleigh_ch11(L2AuxData auxData, double delta_azimuth, double sun_zenith,
                                     double view_zenith, double mus, double muv, double airMass,
                                     double[] phaseRayl, double tauRayl) {

        /**
         * Rayleigh reflectance Fourier components.
         */
        final double[] rhoRayl = new double[RAYSCATT_NUM_SER];
        /**
         * Polynomial coeff for computation, a(s) in DPM.
         */
        final double[][] abcd = new double[RAYSCATT_NUM_SER][RAYSCATT_NUM_ORD];
        /**
         * Interp. coordinates into table {@link L2AuxData#Rayscatt_coeff_s}.
         */
        final FractIndex[] ref_rayleigh_i = FractIndex.createArray(2);
        /*
        * interp coordinates for thetas in LUT scale
		*/
        FractIndex tsi = ref_rayleigh_i[0];
        /*
        * interp coordinates for thetav in LUT scale
        */
        FractIndex tvi = ref_rayleigh_i[1];
        //used for all bands, compute once
        double mud = Math.cos(Math.PI / 180.0 * delta_azimuth);
        double mu2d = 2.0 * mud * mud - 1.0;

        /* angle interpolation coordinates */
        Interp.interpCoord(sun_zenith, auxData.Rayscatt_coeff_s.getTab(2), tsi); /*
                                                                                     * fm
																					 * 15/5/97
																					 */
        Interp.interpCoord(view_zenith, auxData.Rayscatt_coeff_s.getTab(3), tvi);

        float[][][][] Rayscatt_coeff_s = (float[][][][]) auxData.Rayscatt_coeff_s.getJavaArray();
        /*
           * pre-computation of multiple scatt coefficients, wavelength
           * independent
           */
        for (int is = 0; is < RAYSCATT_NUM_SER; is++) {
            /* DPM #2.1.17-4 to 2.1.17-7 */
            final double[] abcdAtIS = abcd[is];
            for (int ik = 0; ik < RAYSCATT_NUM_ORD; ik++) {
                abcdAtIS[ik] = Interp.interpolate(Rayscatt_coeff_s[ik][is], ref_rayleigh_i);
            }
        }

        double constTerm = (1.0 - Math.exp(-tauRayl * airMass)) / (4.0 * (mus + muv));
        for (int is = 0; is < RAYSCATT_NUM_SER; is++) {
            /* primary scattering reflectance */
            rhoRayl[is] = phaseRayl[is] * constTerm; /*
														 * DPM #2.1.17-8
														 * CORRECTED
														 */

            /* coefficient for multiple scattering correction */
            double multiScatteringCoeff = 0.0;
            final double[] lhAbcdIS = abcd[is];
            for (int ik = RAYSCATT_NUM_ORD - 1; ik >= 0; ik--) {
                multiScatteringCoeff = 0.0246 * multiScatteringCoeff + lhAbcdIS[ik]; /*
																	 * DPM
																	 * #2.1.17.9
																	 */
            }

            /* Fourier component of Rayleigh reflectance */
            rhoRayl[is] *= multiScatteringCoeff; /* DPM #2.1.17-10 */
        }

        /* Rayleigh reflectance */
        return rhoRayl[0] + 2.0 * mud * rhoRayl[1] + 2.0 * mu2d * rhoRayl[2];   /* DPM #2.1.17-11 */
    }

    /*
     * This method computes the O2 transmittance for:
     * (i) The geometrical conditions (SZA and VZA) by interpolation
     * on the 24 Gaussian angles
     * (ii) at the central wavelength of the detector
     * by interpolation on 21 filters
     * <p/>
     * <p/>
     * This method applies to:
     * (i) the O2 Rayleigh transmittance
     * (ii) the O2 aerosol-reflection transmittance
     * (iii) the O2 aerosol transmittance
     *
     */
    private double computeO2Transmittance(double centralWvl1, double centralWvl2,
                                          double[][][] to2, double w0, double thetaS, double thetaV, int filterIndex,
                                          int isza, int ivza) {

        // first filter

        double x0 = thetaV;
        double x1 = gaussianAngles[ivza];
        double x2 = gaussianAngles[ivza + 1];

        // first SZA
        double y1 = to2[filterIndex][isza][ivza];
        double y2 = to2[filterIndex][isza][ivza + 1];
        double z1 = linearInterpol(x0, x1, x2, y1, y2);
        // second SZA
        y1 = to2[filterIndex][isza + 1][ivza];
        y2 = to2[filterIndex][isza + 1][ivza + 1];
        double z2 = linearInterpol(x0, x1, x2, y1, y2);

        // between the two SZAs
        x0 = thetaS;
        x1 = gaussianAngles[isza];
        x2 = gaussianAngles[isza + 1];
        double s1 = linearInterpol(x0, x1, x2, z1, z2);

        // second filter
        x0 = thetaV;
        x1 = gaussianAngles[ivza];
        x2 = gaussianAngles[ivza + 1];

        // first SZA
        y1 = to2[filterIndex + 1][isza][ivza];
        y2 = to2[filterIndex + 1][isza][ivza + 1];
        z1 = linearInterpol(x0, x1, x2, y1, y2);
        // second SZA
        y1 = to2[filterIndex + 1][isza + 1][ivza];
        y2 = to2[filterIndex + 1][isza + 1][ivza + 1];
        z2 = linearInterpol(x0, x1, x2, y1, y2);

        x0 = thetaS;
        x1 = gaussianAngles[isza];
        x2 = gaussianAngles[isza + 1];
        double s2 = linearInterpol(x0, x1, x2, z1, z2);

        return linearInterpol(w0, centralWvl1, centralWvl2, s1, s2);
    }

    /*
     * This method provides a simple linear interpolation
     */
    private double linearInterpol(double x, double x1, double x2, double y1, double y2) {
        if (x1 == x2) {
            return y1;
        } else {
            final double slope = (y2 - y1) / (x2 - x1);
            return y1 + slope * (x - x1);
        }
    }


    private double getPressureResult(RayleighCorrection rayleighCorrection, int pressureResultIndex, Tile sza,
                                     Tile vza, Tile saa, Tile vaa,
                                     Tile rhoToa10Tile, Tile rhoToa11Tile, Tile rhoToa12Tile,
                                     int y, int x,
                                     final int detectorIndex) {
        final float szaDeg = sza.getSampleFloat(x, y);
        final float vzaDeg = vza.getSampleFloat(x, y);
        final double csza = Math.cos(MathUtils.DTOR * szaDeg);
        final double cvza = Math.cos(MathUtils.DTOR * vzaDeg);
        final double ssza = Math.sin(MathUtils.DTOR * szaDeg);
        final double svza = Math.sin(MathUtils.DTOR * vzaDeg);
        final double azimDiff = MathUtils.DTOR * (vaa.getSampleFloat(x, y) - saa.getSampleFloat(x, y));

        final double rhoToa10 = rhoToa10Tile.getSampleDouble(x, y);
        double rhoToa11 = rhoToa11Tile.getSampleDouble(x, y);
        final double rhoToa12 = rhoToa12Tile.getSampleDouble(x, y);

        final double airMass = HelperFunctions.calculateAirMass(vzaDeg, szaDeg);

        double centralWvl760 = auxData.central_wavelength[BB760][detectorIndex];

        rhoToa11 = applyStraylightCorr(detectorIndex, rhoToa10, rhoToa11);

        return computeLisePressures(rayleighCorrection, auxData, pressureResultIndex,
                                    szaDeg, vzaDeg, csza, cvza, ssza, svza, azimDiff, rhoToa10,
                                    rhoToa11, rhoToa12,
                                    centralWvl760,
                                    airMass);
    }

    private void computePressureResult(RayleighCorrection rayleighCorrection, Tile targetTile,
                                       int pressureResultIndex, Rectangle rectangle, Tile detector, Tile sza,
                                       Tile vza, Tile saa, Tile vaa,
                                       Tile rhoToa10Tile, Tile rhoToa11Tile, Tile rhoToa12Tile,
                                       Raster isInvalid) {
        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                if (isInvalid.getSample(x, y, 0) != 0) {
                    targetTile.setSample(x, y, 0);
                } else {
                    final int detectorIndex = detector.getSampleInt(x, y);
                    final double pressureResult = getPressureResult(rayleighCorrection,
                                                                    pressureResultIndex, sza, vza, saa, vaa,
                                                                    rhoToa10Tile,
                                                                    rhoToa11Tile, rhoToa12Tile, y, x,
                                                                    detectorIndex);

                    targetTile.setSample(x, y, pressureResult);
                }
            }
        }
    }

    private void createTargetProduct() throws OperatorException {
        targetProduct = createCompatibleProduct(sourceProduct, "MER_CTP", "MER_L2");
        // Surface pressure, land:
        if (outputPressureSurface) {
            psurfLiseBand = targetProduct.addBand(SURFACE_PRESS_LISE, ProductData.TYPE_FLOAT32);
        }
        // TOA pressure, land+ocean:
        if (outputP1) {
            p1LiseBand = targetProduct.addBand(P_1_LISE, ProductData.TYPE_FLOAT32);
        }
        // Bottom pressure, ocean, Rayleigh multiple scattering at 761nm considered:
        if (outputP2) {
            p2LiseBand = targetProduct.addBand(P_2_LISE, ProductData.TYPE_FLOAT32);
        }
        // Bottom pressure, ocean, Rayleigh multiple scattering at 761nm and Fresnel transmittance considered:
        if (outputPScatt) {
            pscattLiseBand = targetProduct.addBand(PSCATT_LISE, ProductData.TYPE_FLOAT32);
        }

        invalidImage = VirtualBandOpImage.builder(INVALID_EXPRESSION, sourceProduct)
                .dataType(ProductData.TYPE_FLOAT32)
                .fillValue(0.0f)
                .tileSize(sourceProduct.getPreferredTileSize())
                .mask(false)
                .level(ResolutionLevel.MAXRES)
                .create();

        invalidLandImage = VirtualBandOpImage.builder(INVALID_EXPRESSION_LAND, sourceProduct)
                .dataType(ProductData.TYPE_FLOAT32)
                .fillValue(0.0f)
                .tileSize(sourceProduct.getPreferredTileSize())
                .mask(false)
                .level(ResolutionLevel.MAXRES)
                .create();
    }

    private double applyStraylightCorr(final int detectorIndex, final double rhoToa10, double rhoToa11) {
        if (straylightCorr) {
            // apply FUB straylight correction...
            rhoToa11 += straylightCoefficients[detectorIndex] * rhoToa10;
        }
        return rhoToa11;
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            Rectangle rectangle = targetTile.getRectangle();
            RayleighCorrection rayleighCorrection = new RayleighCorrection(auxData);

            Tile detector = getSourceTile(sourceProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME),
                                          rectangle);
            Tile sza = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME),
                                     rectangle);
            Tile vza = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME),
                                     rectangle);
            Tile saa = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME),
                                     rectangle);
            Tile vaa = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME),
                                     rectangle);

            Tile rhoToa10 = getSourceTile(rhoToaProduct.getBand("rho_toa_10"), rectangle);
            Tile rhoToa11 = getSourceTile(rhoToaProduct.getBand("rho_toa_11"), rectangle);
            Tile rhoToa12 = getSourceTile(rhoToaProduct.getBand("rho_toa_12"), rectangle);

            Raster isInvalid = null;

            int pressureResultIndex = -1;
            if (band == p1LiseBand && outputP1) {
                pressureResultIndex = 0;
                isInvalid = invalidImage.getData(rectangle);
            }
            if (band == psurfLiseBand && outputPressureSurface) {
                pressureResultIndex = 1;
                isInvalid = invalidLandImage.getData(rectangle);
            }
            if (band == p2LiseBand && outputP2) {
                pressureResultIndex = 2;
                isInvalid = invalidImage.getData(rectangle);
            }
            if (band == pscattLiseBand && outputPScatt) {
                pressureResultIndex = 3;
                // invalidOceanImage.getData(rectangle)
                isInvalid = invalidImage.getData(rectangle);
            }
            if (pressureResultIndex >= 0) {
                computePressureResult(rayleighCorrection, targetTile, pressureResultIndex, rectangle, detector,
                                      sza, vza, saa, vaa,
                                      rhoToa10, rhoToa11, rhoToa12, isInvalid);
            }
        } catch (RuntimeException e) {
            if ((straylightCorr) && (!sourceProduct.getProductType().equals(
                    EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME))) {
                throw new OperatorException
                        ("Straylight correction not possible for full resolution products.");
            } else {
                throw new OperatorException("Failed to process Surface Pressure LISE:\n" + e.getMessage(), e);
            }
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LisePressureOp.class);
        }
    }
}
