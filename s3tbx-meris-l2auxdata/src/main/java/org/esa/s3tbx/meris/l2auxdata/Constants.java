/*
 * $Id: Constants.java,v 1.1 2007/03/27 12:52:22 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

public interface Constants {

    /**
     * Sub-window width for land atmosphere correction.
     */
    int SUBWIN_WIDTH = 4;
    /**
     * Sub-window height for land atmosphere correction.
     */
    int SUBWIN_HEIGHT = 4;

    /* Level 1 Flags Positions */
    int L1_F_COSMETIC = 0;
    int L1_F_DUPLICATED = 1;
    int L1_F_GLINTRISK = 2;
    int L1_F_SUSPECT = 3;
    int L1_F_LAND = 4;
    int L1_F_BRIGHT = 5;
    int L1_F_COAST = 6;
    int L1_F_INVALID = 7;

    /* Level 2 Flags Positions */
    int F_BRIGHT = 0;
    int F_CASE2_S = 1;
    int F_CASE2ANOM = 2;
    int F_CASE2Y = 3;
    int F_CHL1RANGE_IN = 4;
    int F_CHL1RANGE_OUT = 5;
    int F_CIRRUS = 6;
    int F_CLOUD = 7;
    int F_CLOUDPART = 8;
    int F_COASTLINE = 9;
    int F_COSMETIC = 10;
    int F_DDV = 11;
    int F_DUPLICATED = 12;
    int F_HIINLD = 13;
    int F_ICE_HIGHAERO = 14;
    int F_INVALID = 15;
    int F_ISLAND = 16;
    int F_LAND = 17;
    int F_LANDCONS = 18;
    int F_LOINLD = 19;
    int F_MEGLINT = 20;
    int F_ORINP1 = 21;
    int F_ORINP2 = 22;
    int F_ORINPWV = 23;
    int F_OROUT1 = 24;
    int F_OROUT2 = 25;
    int F_OROUTWV = 26;
    int F_SUSPECT = 27;
    int F_UNCGLINT = 28;
    int F_WHITECAPS = 29;
    int F_WVAP = 30;
    int F_ACFAIL = 31;
    int F_CONSOLID = 32;
    int F_ORINP0 = 33;/* vfs 290698 */
    int F_OROUT0 = 34;/* vfs 290698 */
    int F_LOW_NN_P = 35;/* fm  310898 */
    int F_PCD_NN_P = 36;/* fm  310898 */
    int F_LOW_POL_P = 37;/* fm  310898 */
    int F_PCD_POL_P = 38;/* fm  310898 */
    int F_CONFIDENCE_P = 39;/* fm  310898 */
    int F_SLOPE_1 = 40;/* fm  310898 */
    int F_SLOPE_2 = 41;/* fm  310898 */
    int F_UNCERTAIN = 42;/* fm  240899 */
    int F_SUN70 = 43;/* fm  151099 */
    int F_WVHIGLINT = 44;/* MEGS-6-2 */
    int F_TOAVIVEG = 45;/* MEGS-6-2 */
    int F_TOAVIBAD = 46;/* MEGS-6-2 */
    int F_TOAVICSI = 47;/* MEGS-6-2 */
    int F_TOAVIWS = 48;/* MEGS-6-2 */
    int F_TOAVIBRIGHT = 49;/* MEGS-6-2 */
    int F_TOAVIINVALREC = 50; /* MEGS-6-2 */

    /* Bit positions for indexing decision table */
   int CC_BRIGHT = 0;
   int CC_LOW_P_NN = 1;
   int CC_LOW_P_PO = 2;
   int CC_DELTA_P = 3;
   int CC_PCD_NN = 4;
   int CC_PCD_PO = 5;
   int CC_SLOPE_1 = 6;
   int CC_SLOPE_2 = 7;

    /**
     * Convention value for bad algo output.
     */
    final int BAD_VALUE = -1;

/* band definition */
    int bb1 = 0;
    int bb2 = 1;
    int bb3 = 2;
    int bb4 = 3;
    int bb5 = 4;
    int bb6 = 5;
    int bb7 = 6;
    int bb8 = 7;
    int bb9 = 8;
    int bb10 = 9;
    int bb11 = 10;
    int bb12 = 11;
    int bb13 = 12;
    int bb14 = 13;
    int bb15 = 14;

/* band definition wrt wavelength*/
    int bb412 = 0;
    int bb442 = 1;
    int bb490 = 2;
    int bb510 = 3;
    int bb560 = 4;
    int bb620 = 5;
    int bb665 = 6;
    int bb681 = 7;
    int bb705 = 8;
    int bb753 = 9;
    int bb760 = 10;
    int bb775 = 11;
    int bb865 = 12;
    int bb890 = 13;
    int bb900 = 14;

    int LATITUDE_TPG_INDEX = 0;
    int LONGITUDE_TPG_INDEX = 1;
    int DEM_ROUGH_TPG_INDEX = 3;
    int DEM_ALT_TPG_INDEX = 2;
    int LAT_CORR_TPG_INDEX = 4;
    int LON_CORR_TPG_INDEX = 5;
    int SUN_ZENITH_TPG_INDEX = 6;
    int SUN_AZIMUTH_TPG_INDEX = 7;
    int VIEW_ZENITH_TPG_INDEX = 8;
    int VIEW_AZIMUTH_TPG_INDEX = 9;
    int ZONAL_WIND_TPG_INDEX = 10;
    int MERID_WIND_TPG_INDEX = 11;
    int ATM_PRESS_TPG_INDEX = 12;
    int OZONE_TPG_INDEX = 13;
    int REL_HUM_TPG_INDEX = 14;

    /* Number of bands in L1b */
    final int L1_BAND_NUM = 15;

    /**
     * Abbreviation for PI / 180.
     */
    double RAD = Math.PI / 180.0;
    /**
     * Abbreviation 180 / PI.
     */
    double DEG = 180.0 / Math.PI;

    /**
     * Number of detectors for MERIS reduced resolution (RR).
     */
    int RR_DETECTOR_COUNT = 925;

    /**
     * Number of detectors for MERIS full resolution (FR).
     */
    int FR_DETECTOR_COUNT = 3700;

    /**
     * for pressure retrieval
     */
    int PPOL_NUM_SHIFT = 21;
    /**
     * for pressure retrieval
     */
    int PPOL_NUM_ORDER = 12;
    /**
     * for pressure retrieval
     */
    int NN_NUM_SHIFT = 21;

    /**
     * for O2 and H2O transmission polynomials
     */
    int O2T_POLY_K = 4;
    /**
     * for O2 and H2O transmission polynomials
     */
    int H2OT_POLY_K = 4;

    /* for  C [19][6][6] Correction factors for surface pressure retrieval - v2.2 */

    /**
     * Correction factor for surface pressure retrieval: Number of volcanic aerosol models.
     */
    int C_NUM_VOLC = 19;
    /**
     * Correction factor for surface pressure retrieval: Number of Air Mass Tabulated Values.
     */
    int C_NUM_M = 6;
    /**
     * Correction factor for surface pressure retrieval: Number of different rho TOA for {@link #bb753}.
     */
    int C_NUM_RHO = 6;

    /**
     * Number of Sun Zenith Angles. For P10varthresh P13varthresh [PVART_NUM_SZA][PVART_NUM_VZA][PVART_NUM_RHO] press
     * var thresh for land/bright pix.
     */
    int PVART_NUM_SZA = 12;
    /**
     * Number Of Viewing Zenith Angles. For P10varthresh P13varthresh [PVART_NUM_SZA][PVART_NUM_VZA][PVART_NUM_RHO]
     * press var thresh for land/bright pix.
     */
    int PVART_NUM_VZA = 12;

    /**
     * Number of Sun Zenith Angles. For DDV_ARVI  [7][DDV_NUM_SZA][DDV_NUM_VZA][DDV_NUM_ADA] ARVI thresholds for DDV
     * screening
     */
    int DDV_NUM_SZA = 12;
    /**
     * Number of Viewing Zenith Angles. For DDV_ARVI  [7][DDV_NUM_SZA][DDV_NUM_VZA][DDV_NUM_ADA] ARVI thresholds for DDV
     * screening
     */
    int DDV_NUM_VZA = 12;
    /**
     * Number of Azimuth Difference Angles. For DDV_ARVI  [7][DDV_NUM_SZA][DDV_NUM_VZA][DDV_NUM_ADA] ARVI thresholds for
     * DDV screening
     */
    int DDV_NUM_ADA = 19;


    /**
     * No volcanic aerosol.
     */
    int VOLC_NONE = 0;

    /////////////////////////////////////////////////////////////////////
    //  LUT Rayscatt_coeff_s [4][3][12][12]
    /////////////////////////////////////////////////////////////////////

    /**
     * Rayleigh Scattering Coeff series, number of coefficient Order
     */
    int RAYSCATT_NUM_ORD = 4;
    /**
     * Rayleigh Scattering Coeff series, number of Series
     */
    int RAYSCATT_NUM_SER = 3;
    /**
     * Rayleigh Scattering Coeff series, number of Sun Zenith Angles
     */
    int RAYSCATT_NUM_SZA = 12;
    /**
     * Rayleigh Scattering Coeff series, number of Viewing Zenith Angles
     */
    int RAYSCATT_NUM_VZA = 12;

    /////////////////////////////////////////////////////////////////////
    // LUT r7thresh  [12][12][19] reflectance threshold for b665
    /////////////////////////////////////////////////////////////////////

    /**
     * Number of Sun Zenith Angles
     */
    int R7T_NUM_SZA = 12;
    /**
     * Number Of Viewing Zenith Angles
     */
    int R7T_NUM_VZA = 12;
    /**
     * Number Of Azimuth difference Angles
     */
    int R7T_NUM_ADA = 19;

    /////////////////////////////////////////////////////////////////////
    // LUT rog  [7][19][25][5][27] Glint reflectances
    /////////////////////////////////////////////////////////////////////

    /**
     * Number of Wind Azimuth Values
     */
    int ROG_NUM_WA = 7;
    /**
     * Number of Viewing Zenith Angles
     */
    int ROG_NUM_VZA = 19;
    /**
     * Number of Azimuth Difference Angles
     */
    int ROG_NUM_ADA = 25;
    /**
     * Number of Wind Modulus Values
     */
    int ROG_NUM_WIND = 5;
    /**
     * CARE set to 9 instead of 27 - Number of Sun Zenith Angles allocated corrected by vfs for full orbit files
     * the clean way would be tp use the same method as for the XC table ... yaka ...
     */
    int ROG_NUM_SZA = 27;
    /**
     * Number of Sun Zenith Angles in file
     */
    int ROG_ALL_SZA = 27;

    /////////////////////////////////////////////////////////////////////////

    /**
     * Bit index of first flag indicating negative reflectance
     */
    int A_RWNEG = 16;

    /**
     * for  Rayalb  [17] Rayleigh spherical albedo wrt tau
     * Number of Rayleigh Optical Thickness
     */
    int RAYALB_NUM_TAU = 17;

}
