/*
 * $Id: L2AuxData.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.util.math.FractIndex;
import org.esa.s3tbx.util.math.Interp;
import org.esa.s3tbx.util.math.LUT;
import org.esa.s3tbx.util.math.MDArray;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.io.IOException;

/**
 * The <code>L2AuxData</code> class is a collection of all relevant MERIS Level 2 DPM parameters.
 * <p/>
 * For convinience reasons, this class models a C-language-like structure which has only public fields and no methods.
 */
@SuppressWarnings("JavaDoc")
public final class L2AuxData implements Constants {
    /**
     * Square of the sun-earth distance.
     */
    public double sun_earth_distance_square;
    /**
     * Factor used for reflectance conversion: Ratio actual sun-earth distance to mean sun-earth distance.
     */
    public double seasonal_factor;
    /**
     * Number of detectors in current L1b data product.
     */
    public int detector_count;
    /**
     * Maximum Pressure.
     */
    public double maxPress;
    /**
     * Pressure scale height to account for altitude.
     */
    public double press_scale_height;
    /**
     * Width of pressure confidence interval
     */
    public double press_confidence;
    /**
     * Cloud classification decision table, size=256
     */
    public boolean[] land_decision_table;
    /**
     * Cloud classification decision table, size=256
     */
    public boolean[] water_decision_table;
    /**
     * Numerator band index for slope test #1
     */
    public int band_slope_n_1;
    /**
     * Denominator band index for slope test #1
     */
    public int band_slope_d_1;
    /**
     * Numerator band index for slope test #2
     */
    public int band_slope_n_2;
    /**
     * Denominator band index for slope test #2
     */
    public int band_slope_d_2;
    /**
     * Band index for reflectance test
     */
    public int band_bright_n;
    /**
     * low threshold for slope test 1
     */
    public double slope_1_low_thr;
    /**
     * high threshold for slope test 1
     */
    public double slope_1_high_thr;
    /**
     * low threshold for slope test 2
     */
    public double slope_2_low_thr;
    /**
     * high threshold for slope test 2
     */
    public double slope_2_high_thr;
    /**
     * Detector solar irradiance, dimension sizes are <code>[{@link Constants#L1_BAND_NUM}][{@link
     * Constants#RR_DETECTOR_COUNT}]</code> for RR products and <code>[{@link Constants#L1_BAND_NUM}][{@link
     * Constants#FR_DETECTOR_COUNT}]</code> for FR products
     */
    public double[][] detector_solar_irradiance;
    /**
     * Detector central wavelengths, dimension sizes are <code>[{@link Constants#L1_BAND_NUM}][{@link
     * Constants#RR_DETECTOR_COUNT}]</code> for RR products and <code>[{@link Constants#L1_BAND_NUM}][{@link
     * Constants#FR_DETECTOR_COUNT}]</code> for FR products
     */
    public double[][] central_wavelength;
    /**
     * Detector theoretical wavelengths, sizes is <code>[{@link Constants#L1_BAND_NUM}]
     */
    public double[] theoretical_wavelength;
    /**
     * Default radiance for saturated pixels.
     */
    public double[] Saturation_L;
    /**
     * Parameters for smile correction over land.
     */
    public L2AuxData.SmileParams land_smile_params;
    /**
     * Parameters for smile correction over water.
     */
    public L2AuxData.SmileParams water_smile_params;
    /**
     * polynomial coeff for H2O correction [PPOL_NUM_SHIFT]
     */
    public double[] spectral_shift_H2Owavelength;
    /**
     * polynomial coeff for H2O correction [L1_BAND_NUM][H2OT_POLY_K]
     */
    public double[][] H2Ocoef;
    /**
     * polynomial coeff for H2O correction  [PPOL_NUM_SHIFT][H2OT_POLY_K]
     */
    public double[][] H2OcoefSpecShift;

    /**
     * polynomial coeff for O2 correction [PPOL_NUM_SHIFT][O2T_POLY_K]
     */
    public double[][] O2coef;
    /**
     * Spectral shift tabulated values, size is {@link Constants#PPOL_NUM_SHIFT}
     */
    public double[] spectral_shift_wavelength;
    /**
     * Ozone Optical Thickness for 1 cm.atm
     */
    public double[] tauO3_norm;
    /**
     * Maximum allowed value for sun zenith angle
     */
    public double TETAS_LIM;
    /**
     * [12][12][19] reflectance threshold for 665 nm band
     */
    public LUT r7thresh;
    /**
     * [12][12][19] reflectance threshold for 865 nm band
     */
    public LUT r13thresh;
    /**
     * Threshold for inland water discrimination
     */
    public double[] alpha_thresh;
    /**
     * Bands to be used in inland water processing
     */
    public int[] lap_b_thresh;
    /**
     * Factor for 865 nm band used for inland water screening.
     */
    public double lap_beta_l;
    /**
     * Factor for 865 nm band used for island screening.
     */
    public double lap_beta_w;
    /**
     * [7][19][25][5][27] Glint reflectances
     */
    public LUT rog;
    /**
     * Standard pressure.
     */
    public double Pstd;
    /**
     * Rayleigh optical thickness.
     */
    public double[] tau_R;
    /**
     * Constants A, B used for Rayleigh phase function.
     */
    public double[] AB;
    /**
     * Rayleigh transmittance coefficients TR
     */
    public double[] Raytrans;
    /**
     * Threshold on Rayleigh corrected reflectance land pixels
     */
    public LUT Rhorc_442_land_LUT;
    /**
     * Thresh. on Rayleigh corrected reflect. ocean pixels
     */
    public LUT Rhorc_442_ocean_LUT;
    /**
     * Rayl Scattering Coeff serie 1 [4][3][12][12].
     */
    public LUT Rayscatt_coeff_s;
    /**
     * Polynomial coefficients for pressure retrieval.
     */
    public LUT polcoeff;
    /**
     * Correction factors for surface pressure retrieval.
     */
    public LUT C;
    /**
     * Pressure difference threshold values for land pixels.
     */
    public LUT DPthresh_land;
    /**
     * Pressure difference threshold values for ocean pixels.
     */
    public LUT DPthresh_ocean;
    /**
     * Rayleigh spherical albedo LUT
     */
    public LUT Rayalb;

    /**
     * Holds smile correction information for all bands.
     */
    public static class SmileParams {
        /**
         * Is the smile correction enabled for a given band?
         */
        public final boolean[] enabled = new boolean[L1_BAND_NUM];
        /**
         * Contains the indexes of the two bands used for interpolation.
         */
        public final int[][] derivative_band_id = new int[L1_BAND_NUM][2];
    }

    private Product sourceProduct;

    /**
     * @param config
     * @param product Source product, must have start/stop time set and have a tie-point grid "sun_zenith"
     * @throws L2AuxDataException
     * @throws IOException
     */
    public L2AuxData(DpmConfig config, Product product) throws L2AuxDataException, IOException {
        sourceProduct = product;
        loadAuxData(config);
    }

    private void loadAuxData(DpmConfig config) throws L2AuxDataException, IOException {
        final AuxFile auxFileO = AuxFile.open('O', config.getAuxDatabaseFile("lv2conf", null));
        final AuxFile auxFileP = AuxFile.open('P', config.getAuxDatabaseFile("atmosphere", null));
        final AuxFile auxFileS = AuxFile.open('S', config.getAuxDatabaseFile("landaero", null));
        final AuxFile auxFileT = AuxFile.open('T', config.getAuxDatabaseFile("case1", null));
        try {
            loadConfigurationAuxData(auxFileO);
            loadAtmosphereAuxData(auxFileP);
            loadLandAerosolAuxData(auxFileS, auxFileT);
            loadRayscattCoeffAuxData(auxFileP, auxFileS);
        } finally {
            auxFileO.close();
            auxFileP.close();
            auxFileS.close();
            auxFileT.close();
        }
    }

    private void loadConfigurationAuxData(final AuxFile auxFileO) throws IOException, L2AuxDataException {

        // Default radiance for saturated pixels.
        Saturation_L = auxFileO.readDoubleArray("O202", L1_BAND_NUM);

        // Square of the sun-earth distance.
        sun_earth_distance_square = auxFileO.readDouble("O300");

        // todo move to another place
        // Ratio actual sun-earth distance to mean sun-earth distance.
        seasonal_factor = 1.0;
        final ProductData.UTC startTime = sourceProduct.getStartTime();
        final ProductData.UTC stopTime = sourceProduct.getEndTime();
        if (startTime != null && stopTime != null) {
            // DPM 2.1.4-3
            final double daysSince2000 = 0.5 * (startTime.getMJD() + stopTime.getMJD());
            seasonal_factor = Utils.computeSeasonalFactor(daysSince2000,
                                                          sun_earth_distance_square);
        }

        final byte[] sw_land_smile = auxFileO.readRecord("O301", ProductData.TYPE_ASCII).getElemString().getBytes();
        final byte[] sw_water_smile = auxFileO.readRecord("O302", ProductData.TYPE_ASCII).getElemString().getBytes();
        final byte[] derivative_land_smile = auxFileO.readRecord("O303", ProductData.TYPE_ASCII).getElemString().getBytes();
        final byte[] derivative_water_smile = auxFileO.readRecord("O304", ProductData.TYPE_ASCII).getElemString().getBytes();

        theoretical_wavelength = auxFileO.readDoubleArray("O305", L1_BAND_NUM);
        land_smile_params = new SmileParams();
        water_smile_params = new SmileParams();
        for (int i = 0; i < L1_BAND_NUM; i++) {
            land_smile_params.enabled[i] = sw_land_smile[i] != 0;
            land_smile_params.derivative_band_id[i][0] = derivative_land_smile[2 * i + 0] - 1;
            land_smile_params.derivative_band_id[i][1] = derivative_land_smile[2 * i + 1] - 1;
            water_smile_params.enabled[i] = sw_water_smile[i] != 0;
            water_smile_params.derivative_band_id[i][0] = derivative_water_smile[2 * i + 0] - 1;
            water_smile_params.derivative_band_id[i][1] = derivative_water_smile[2 * i + 1] - 1;
        }

        final String central_wavelength_key;
        final String detector_solar_irradiance_key;
        if (Utils.isProductRR(sourceProduct)) {
            detector_count = RR_DETECTOR_COUNT;
            central_wavelength_key = "O307";
            detector_solar_irradiance_key = "O308";
        } else if (Utils.isProductFR(sourceProduct)) {
            detector_count = FR_DETECTOR_COUNT;
            central_wavelength_key = "O309";
            detector_solar_irradiance_key = "O30A";
        } else {
            throw new L2AuxDataException("Input product is neither MERIS RR nor FR");
        }
        int num_elems = L1_BAND_NUM * detector_count;

        // Detector central wavelengths
        final float[] central_wavelength_data = auxFileO.readFloatArray(central_wavelength_key, num_elems);
        central_wavelength = new double[L1_BAND_NUM][detector_count];
        MDArray.copyFlatIntoDeep(central_wavelength_data, central_wavelength);

        // Detector solar irradiance
        final float[] detector_solar_irradiance_data = auxFileO.readFloatArray(detector_solar_irradiance_key, num_elems);
        detector_solar_irradiance = new double[L1_BAND_NUM][detector_count];
        MDArray.copyFlatIntoDeep(detector_solar_irradiance_data, detector_solar_irradiance);

        // Maximum allowed value for sun zenith angle
        TETAS_LIM = auxFileO.readFloat("O40G");

        // Confidence interval between pressure estimates
        press_confidence = auxFileO.readFloat("O500");

        /* v1.1: readRecord GADS classification */
        band_slope_n_1 = auxFileO.readInt("O501");
        band_slope_d_1 = auxFileO.readInt("O502");
        slope_1_low_thr = auxFileO.readFloat("O503");
        slope_1_high_thr = auxFileO.readFloat("O504");
        band_slope_n_2 = auxFileO.readInt("O505");
        band_slope_d_2 = auxFileO.readInt("O506");
        slope_2_low_thr = auxFileO.readFloat("O507");
        slope_2_high_thr = auxFileO.readFloat("O508");
        int[] ibuf = auxFileO.readUIntArray("O509", 256);
        water_decision_table = new boolean[256];
        for (int i = 0; i < ibuf.length; i++) {
            water_decision_table[i] = ibuf[i] != 0;
        }
        ibuf = auxFileO.readUIntArray("O50A", 256);
        land_decision_table = new boolean[256];
        for (int i = 0; i < ibuf.length; i++) {
            land_decision_table[i] = ibuf[i] != 0;
        }
        /* v1.2 */
        band_bright_n = auxFileO.readInt("O50B");

        /* band no. in file = 1 + band no in code */
        band_slope_n_1--;
        band_slope_d_1--;
        band_slope_n_2--;
        band_slope_d_2--;
        band_bright_n--;

        // q tabulated values for GADS reflectance threshold
        final double[] q_tabulated_values = auxFileO.readDoubleArray("O50C", DDV_NUM_SZA);
        // Df tabulated values for GADS reflectance threshold
        final double[] Df_tabulated_values = auxFileO.readDoubleArray("O50E", DDV_NUM_ADA);

        ProductData pdata = null;
        assert DDV_NUM_SZA == DDV_NUM_VZA;

        // element count of the symetric matrix
        final int elementCount = ((DDV_NUM_SZA * DDV_NUM_SZA + DDV_NUM_SZA) / 2) * DDV_NUM_ADA;

        float[] fBuf;
        int z;
        pdata = auxFileO.readRecord("O600", 0, elementCount, ProductData.TYPE_FLOAT32, null);
        fBuf = (float[]) pdata.getElems();
        z = 0;
        float[][][] Rhorc_442_land_LUT_data = new float[DDV_NUM_SZA][DDV_NUM_VZA][DDV_NUM_ADA];
        for (int its = 0; its < DDV_NUM_SZA; its++) {
            for (int itv = its; itv < DDV_NUM_VZA; itv++) {
                for (int idf = 0; idf < DDV_NUM_ADA; idf++) {
                    Rhorc_442_land_LUT_data[its][itv][idf] = fBuf[z];
                    if (its != itv) {
                        Rhorc_442_land_LUT_data[itv][its][idf] = fBuf[z];
                    }
                    z++;
                }
            }
        }
        assert z == elementCount;

        Rhorc_442_land_LUT = new LUT(Rhorc_442_land_LUT_data);
        Rhorc_442_land_LUT.setTab(0, q_tabulated_values);
        Rhorc_442_land_LUT.setTab(1, q_tabulated_values);
        Rhorc_442_land_LUT.setTab(2, Df_tabulated_values);

        pdata = auxFileO.readRecord("O600", 1, elementCount, ProductData.TYPE_FLOAT32, pdata);
        fBuf = (float[]) pdata.getElems();
        z = 0;
        float[][][] Rhorc_442_ocean_LUT_data = new float[DDV_NUM_SZA][DDV_NUM_VZA][DDV_NUM_ADA];
        for (int its = 0; its < DDV_NUM_SZA; its++) {
            for (int itv = its; itv < DDV_NUM_VZA; itv++) {
                for (int idf = 0; idf < DDV_NUM_ADA; idf++) {
                    Rhorc_442_ocean_LUT_data[its][itv][idf] = fBuf[z];
                    if (its != itv) {
                        Rhorc_442_ocean_LUT_data[itv][its][idf] = fBuf[z];
                    }
                    z++;
                }
            }
        }
        assert z == elementCount;

        Rhorc_442_ocean_LUT = new LUT(Rhorc_442_ocean_LUT_data);
        Rhorc_442_ocean_LUT.setTab(0, q_tabulated_values);
        Rhorc_442_ocean_LUT.setTab(1, q_tabulated_values);
        Rhorc_442_ocean_LUT.setTab(2, Df_tabulated_values);
    }

    private void loadAtmosphereAuxData(final AuxFile auxFileP) throws IOException {
        // Rayleigh transmittance polynomial coeff
        Raytrans = auxFileP.readDoubleArray("P200", 3);
        // Rayleigh optical thickness tabulated values
        final double[] rayalbTab = auxFileP.readDoubleArray("P201", RAYALB_NUM_TAU);
        // zenith angle scale for pressure variance threshold tables
        final double[] theta_scale = auxFileP.readDoubleArray("P206", PVART_NUM_SZA);
        // Rayleigh phase function A, B coeff
        AB = auxFileP.readDoubleArray("P208", 2);
        // air mass tabulated values, M=(cos qs)-1+(cos qv)-1
        final double[] air_mass_tab = auxFileP.readDoubleArray("P209", C_NUM_M);
        // rho TOA at 753.75 nm tabulated values after gaseous correction
        final double[] rho_TOA_tab = auxFileP.readDoubleArray("P20B", C_NUM_RHO);
        // Standard pressure.
        Pstd = auxFileP.readFloat("P20I");
        // Maximum pressure
        maxPress = auxFileP.readFloat("P20K");
        // Spectral shift tabulated values
        spectral_shift_wavelength = auxFileP.readDoubleArray("P20P", PPOL_NUM_SHIFT);
        // polynomial coeff for H2O correction
        spectral_shift_H2Owavelength = auxFileP.readDoubleArray("P20Q", PPOL_NUM_SHIFT);
        // Pressure scale height to account for altitude
        press_scale_height = auxFileP.readFloat("P20R");
        //  Ozone Optical Thickness for 1 cm.atm
        tauO3_norm = auxFileP.readDoubleArray("P304", L1_BAND_NUM);
        // Rayleigh optical thickness.
        tau_R = auxFileP.readDoubleArray("P303", L1_BAND_NUM);

        final double[] O2coef_buf = auxFileP.readDoubleArray("P400", PPOL_NUM_SHIFT * O2T_POLY_K);
        O2coef = new double[PPOL_NUM_SHIFT][O2T_POLY_K];
        MDArray.copyFlatIntoDeep(O2coef_buf, O2coef);

        final double[] H2OcoefSpecShift_buf = auxFileP.readDoubleArray("P401", PPOL_NUM_SHIFT * H2OT_POLY_K);
        H2OcoefSpecShift = new double[PPOL_NUM_SHIFT][H2OT_POLY_K];
        MDArray.copyFlatIntoDeep(H2OcoefSpecShift_buf, H2OcoefSpecShift);

        final double[] H2Ocoef_buf = auxFileP.readDoubleArray("P402", L1_BAND_NUM * H2OT_POLY_K);
        H2Ocoef = new double[L1_BAND_NUM][H2OT_POLY_K];
        MDArray.copyFlatIntoDeep(H2Ocoef_buf, H2Ocoef);

        // Rayleigh spherical albedo - iodd 6.11.8
        final double[] rayalbLUT = auxFileP.readDoubleArray("P600", RAYALB_NUM_TAU);
        Rayalb = new LUT(rayalbLUT);
        Rayalb.setTab(0, rayalbTab);

        // Pressure difference threshold values for land pixels
        final float[] DPthresh_land_array = auxFileP.readFloatArray("P700", -1);
        int DPthresh_land_array_index = 0;
        float[][] DPthresh_land_data = new float[PVART_NUM_SZA][PVART_NUM_VZA];
        for (int its = 0; its < PVART_NUM_SZA; its++) {
            for (int itv = its; itv < PVART_NUM_VZA; itv++) {
                DPthresh_land_data[its][itv] = DPthresh_land_array[DPthresh_land_array_index];
                if (itv != its) {
                    DPthresh_land_data[itv][its] = DPthresh_land_array[DPthresh_land_array_index];
                }
                DPthresh_land_array_index++;
            }
        }
        DPthresh_land = new LUT(DPthresh_land_data);
        DPthresh_land.setTab(0, theta_scale);
        DPthresh_land.setTab(1, theta_scale);

        // Pressure difference threshold values for water pixels
        final float[] DPthresh_ocean_array = auxFileP.readFloatArray("P701", -1);
        int DPthresh_ocean_array_index = 0;
        float[][] DPthresh_ocean_data = new float[PVART_NUM_SZA][PVART_NUM_VZA];
        for (int its = 0; its < PVART_NUM_SZA; its++) {
            for (int itv = its; itv < PVART_NUM_VZA; itv++) {
                DPthresh_ocean_data[its][itv] = DPthresh_ocean_array[DPthresh_ocean_array_index];
                if (itv != its) {
                    DPthresh_ocean_data[itv][its] = DPthresh_ocean_array[DPthresh_ocean_array_index];
                }
                DPthresh_ocean_array_index++;
            }
        }
        DPthresh_ocean = new LUT(DPthresh_ocean_data);
        DPthresh_ocean.setTab(0, theta_scale);
        DPthresh_ocean.setTab(1, theta_scale);

        // Polynomial coefficients for pressure retrieval
        final int[] polcoeff_sizes = new int[]{PPOL_NUM_SHIFT, PPOL_NUM_ORDER};
        final float[] polcoeff_array = auxFileP.readFloatArray("P800", PPOL_NUM_SHIFT * PPOL_NUM_ORDER);
        final double[] polcoeff_shift_tab = new double[PPOL_NUM_SHIFT];
        for (int i = 0; i < polcoeff_shift_tab.length; i++) {
            polcoeff_shift_tab[i] = i;
        }
        polcoeff = new LUT(polcoeff_sizes, polcoeff_array);
        polcoeff.setTab(0, polcoeff_shift_tab);
        polcoeff.setTab(1, null); // no tabulated values needed for 2nd dimension

        // Correction factors for surface pressure retrieval
        final int[] C_sizes = new int[]{C_NUM_VOLC, C_NUM_M, C_NUM_RHO};
        final float[] C_array = auxFileP.readFloatArray("P802", C_NUM_VOLC * C_NUM_M * C_NUM_RHO);
        C = new LUT(C_sizes, C_array);
        C.setTab(0, null); // no tabulated values needed for 1st dimension
        C.setTab(1, air_mass_tab);
        C.setTab(2, rho_TOA_tab);

    }

    private void loadRayscattCoeffAuxData(final AuxFile auxFileP, final AuxFile auxFileS) throws IOException {
        /* Rayleigh multiple scattering function */
        /* Fourier seriescoefficients - iodd 6.11.7 */
        final float[][][][] Rayscatt_coeff_s_data = new float[RAYSCATT_NUM_ORD][RAYSCATT_NUM_SER][RAYSCATT_NUM_SZA][RAYSCATT_NUM_VZA];
        /* index nesting follows IODD convention...*/
        /* Fourier series order 0 */
        float[] Rayscatt_coeff_s_array = auxFileP.readFloatArray("P500", -1);
        int Rayscatt_coeff_s_array_index = 0;
        for (int its = 0; its < RAYSCATT_NUM_SZA; its++) {
            for (int itv = its; itv < RAYSCATT_NUM_SZA; itv++) {
                for (int k = 0; k < RAYSCATT_NUM_ORD; k++) {
                    Rayscatt_coeff_s_data[k][0][its][itv] = Rayscatt_coeff_s_array[Rayscatt_coeff_s_array_index];
                    if (its != itv) {
                        Rayscatt_coeff_s_data[k][0][itv][its] = Rayscatt_coeff_s_array[Rayscatt_coeff_s_array_index];
                    }
                    Rayscatt_coeff_s_array_index++;
                }
            }
        }
        /* Fourier series order 1 */
        Rayscatt_coeff_s_array = auxFileP.readFloatArray("P501", -1);
        Rayscatt_coeff_s_array_index = 0;
        for (int its = 0; its < RAYSCATT_NUM_SZA; its++) {
            for (int itv = its; itv < RAYSCATT_NUM_SZA; itv++) {
                for (int k = 0; k < RAYSCATT_NUM_ORD; k++) {
                    Rayscatt_coeff_s_data[k][1][its][itv] = Rayscatt_coeff_s_array[Rayscatt_coeff_s_array_index];
                    if (its != itv) {
                        Rayscatt_coeff_s_data[k][1][itv][its] = Rayscatt_coeff_s_array[Rayscatt_coeff_s_array_index];
                    }
                    Rayscatt_coeff_s_array_index++;
                }
            }
        }
        /* Fourier series order 2 */
        Rayscatt_coeff_s_array = auxFileP.readFloatArray("P502", -1);
        Rayscatt_coeff_s_array_index = 0;
        for (int its = 0; its < RAYSCATT_NUM_SZA; its++) {
            for (int itv = its; itv < RAYSCATT_NUM_SZA; itv++) {
                for (int k = 0; k < RAYSCATT_NUM_ORD; k++) {
                    /* Fourier series order 2 */
                    Rayscatt_coeff_s_data[k][2][its][itv] = Rayscatt_coeff_s_array[Rayscatt_coeff_s_array_index];
                    if (its != itv) {
                        Rayscatt_coeff_s_data[k][2][itv][its] = Rayscatt_coeff_s_array[Rayscatt_coeff_s_array_index];
                    }
                    Rayscatt_coeff_s_array_index++;
                }
            }
        }
        /* complete LUT index for selection */
        final double[] Rayscatt_coeff_s_ord_tab = new double[RAYSCATT_NUM_ORD];
        for (int k = 0; k < RAYSCATT_NUM_ORD; k++) {
            Rayscatt_coeff_s_ord_tab[k] = k;
        }
        final double[] Rayscatt_coeff_s_ser_tab = new double[RAYSCATT_NUM_SER];
        for (int s = 0; s < RAYSCATT_NUM_SER; s++) {
            Rayscatt_coeff_s_ser_tab[s] = s;
        }
        Rayscatt_coeff_s = new LUT(Rayscatt_coeff_s_data);
        Rayscatt_coeff_s.setTab(0, Rayscatt_coeff_s_ord_tab);
        Rayscatt_coeff_s.setTab(1, Rayscatt_coeff_s_ser_tab);

        final double[] ang_scale = auxFileS.readDoubleArray("S200", R7T_NUM_SZA);
        Rayscatt_coeff_s.setTab(2, ang_scale);
        Rayscatt_coeff_s.setTab(3, ang_scale);
    }

    private void loadLandAerosolAuxData(final AuxFile auxFileS, final AuxFile auxFileT) throws IOException, L2AuxDataException {
        assert R7T_NUM_SZA == R7T_NUM_VZA;
        final double[] r7thresh_tab1 = auxFileS.readDoubleArray("S200", R7T_NUM_SZA);
        final double[] r7thresh_tab2 = r7thresh_tab1;
        final double[] r7thresh_tab3 = auxFileS.readDoubleArray("S202", R7T_NUM_ADA);

        alpha_thresh = new double[2];
        alpha_thresh[0] = auxFileS.readDouble("S300");
        alpha_thresh[1] = auxFileS.readDouble("S302");

        /* read rT(665) Thresholds */
        float[] fbuf = auxFileS.readFloatArray("S301", -1);
        float[][][] r7thresh_LUT = new float[R7T_NUM_SZA][R7T_NUM_SZA][R7T_NUM_ADA];
        int z = 0;
        for (int its = 0; its < R7T_NUM_SZA; its++) {
            for (int itv = its; itv < R7T_NUM_SZA; itv++) {
                for (int idf = 0; idf < R7T_NUM_ADA; idf++) {
                    r7thresh_LUT[its][itv][idf] = fbuf[z];
                    if (itv != its) {
                        r7thresh_LUT[itv][its][idf] = fbuf[z];
                    }
                    z++;
                }
            }
        }

        r7thresh = new LUT(r7thresh_LUT);
        r7thresh.setTab(0, r7thresh_tab1);
        r7thresh.setTab(1, r7thresh_tab2);
        r7thresh.setTab(2, r7thresh_tab3);

        /* read rT(865) Thresholds */
        fbuf = auxFileS.readFloatArray("S303", -1);
        float[][][] r13thresh_LUT = new float[R7T_NUM_SZA][R7T_NUM_SZA][R7T_NUM_ADA];
        z = 0;
        for (int its = 0; its < R7T_NUM_SZA; its++) {
            for (int itv = its; itv < R7T_NUM_SZA; itv++) {
                for (int idf = 0; idf < R7T_NUM_ADA; idf++) {
                    r13thresh_LUT[its][itv][idf] = fbuf[z];
                    if (itv != its) {
                        r13thresh_LUT[itv][its][idf] = fbuf[z];
                    }
                    z++;
                }
            }
        }

        r13thresh = new LUT(r13thresh_LUT);
        r13thresh.setTab(0, r7thresh_tab1);
        r13thresh.setTab(1, r7thresh_tab2);
        r13thresh.setTab(2, r7thresh_tab3);

        /* MEGS-6-2 loading of the new lap parameters for inland waters & island screening */
        lap_b_thresh = auxFileS.readIntArray("S204", 2);
        /* convert 1-based index to 0-based */
        lap_b_thresh[0]--;
        lap_b_thresh[1]--;

        lap_beta_l = auxFileS.readDouble("S205");
        lap_beta_w = auxFileS.readDouble("S206");

        TiePointGrid szaGrid = sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME);
        if (szaGrid == null) {
            String msg = String.format("Source product does not contain tie-point grid '%s'",
                                       EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME);
            throw new L2AuxDataException(msg);
        }

        Stx stx = szaGrid.getStx(true, ProgressMonitor.NULL);

        /* Read thetas tabulated values for LUTs turbid and Glint */
        fbuf = auxFileT.readFloatArray("T203", -1);

        double[] fbufCopy = new double[27];
        for (int i = 0; i < fbuf.length; i++) {
            fbufCopy[i] = fbuf[i];
        }

        int min;
        int max;
        FractIndex fract = new FractIndex();
        Interp.interpCoord(stx.getMinimum(), fbufCopy, fract);
        min = fract.index;
        Interp.interpCoord(stx.getMaximum(), fbufCopy, fract);
        max = fract.index;
        if (fract.fraction > 0) {
            max++;
        }
        /* table does not accomodate whole thetas range : issue a warning */
        if (max >= min + ROG_NUM_SZA) {
            throw new L2AuxDataException("Wrong thetas(ROG) range: " + min + " to " + max);
        }

        min = Math.min(min, ROG_ALL_SZA - ROG_NUM_SZA);
        final float[] rog_tab5 = new float[ROG_NUM_SZA];
        System.arraycopy(fbuf, min, rog_tab5, 0, ROG_NUM_SZA);

        /* Read thetav tabulated values for LUTs Turbid and Glint */
        final float[] rog_tab2 = auxFileT.readFloatArray("T206", ROG_NUM_VZA);

        /* Read deltaphi tabulated values for LUTs Turbid and Glint */
        final float[] rog_tab3 = auxFileT.readFloatArray("T208", ROG_NUM_ADA);

        /* Read Wind speed tabulated values for LUT Glint */
        final float[] rog_tab4 = auxFileT.readFloatArray("T20G", ROG_NUM_WIND);

        /* Read Wind azimuth tabulated values for LUT Glint */
        final float[] rog_tab1 = auxFileT.readFloatArray("T20H", ROG_NUM_WA);

        float[][][][][] rog_LUT = new float[ROG_NUM_WA][ROG_NUM_VZA][ROG_NUM_ADA][ROG_NUM_WIND][ROG_NUM_SZA];
        /* loop on selected Sun zenith angles */
        for (int its = 0; its < ROG_NUM_SZA; its++) {
            /* read five successive tables */
            int ws = 0;
            fbuf = (float[]) auxFileT.readRecord("T700", min + its, -1, ProductData.TYPE_FLOAT32, null).getElems();

            z = 0;
            for (int wa = 0; wa < ROG_NUM_WA; wa++) {
                for (int itv = 0; itv < ROG_NUM_VZA; itv++) {
                    for (int idf = 0; idf < ROG_NUM_ADA; idf++) {
                        rog_LUT[wa][itv][idf][ws][its] = fbuf[z];
                        z++;
                    }
                }
            }
            for (ws = 1; ws < ROG_NUM_WIND; ws++) {
                fbuf = (float[]) auxFileT.readRecord("T70" + ws, min + its, -1, ProductData.TYPE_FLOAT32, null).getElems();
                z = 0;
                for (int wa = 0; wa < ROG_NUM_WA; wa++) {
                    for (int itv = 0; itv < ROG_NUM_VZA; itv++) {
                        for (int idf = 0; idf < ROG_NUM_ADA; idf++) {
                            rog_LUT[wa][itv][idf][ws][its] = fbuf[z];
                            z++;
                        }
                    }
                }
            }
        }
        rog = new LUT(rog_LUT);
        rog.setTab(0, makeDoubleArrayCopy(rog_tab1));
        rog.setTab(1, makeDoubleArrayCopy(rog_tab2));
        rog.setTab(2, makeDoubleArrayCopy(rog_tab3));
        rog.setTab(3, makeDoubleArrayCopy(rog_tab4));
        rog.setTab(4, makeDoubleArrayCopy(rog_tab5));
    }

    static private double[] makeDoubleArrayCopy(float[] floatArray) {
        final double[] doubleArray = new double[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            doubleArray[i] = floatArray[i];
        }
        return doubleArray;
    }
}