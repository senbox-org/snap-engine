/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s3tbx.arc;

import org.esa.snap.dataio.envisat.EnvisatConstants;

/**
 * Provides an interface defining all constants used with the ARC processor.
 */
class ArcConstants {

    public static final String[] SOURCE_RASTER_NAMES_AATSR = new String[]{
            EnvisatConstants.AATSR_L1B_BTEMP_NADIR_0370_BAND_NAME,
            EnvisatConstants.AATSR_L1B_BTEMP_NADIR_1100_BAND_NAME,
            EnvisatConstants.AATSR_L1B_BTEMP_NADIR_1200_BAND_NAME,
            EnvisatConstants.AATSR_L1B_BTEMP_FWARD_0370_BAND_NAME,
            EnvisatConstants.AATSR_L1B_BTEMP_FWARD_1100_BAND_NAME,
            EnvisatConstants.AATSR_L1B_BTEMP_FWARD_1200_BAND_NAME,
            EnvisatConstants.AATSR_VIEW_ELEV_NADIR_DS_NAME,
            EnvisatConstants.AATSR_VIEW_ELEV_FWARD_DS_NAME
    };

    public static final String[] SOURCE_RASTER_NAMES_SLSTR = new String[]{
            "S7_BT_in",
            "S8_BT_in",
            "S9_BT_in",
            "S7_BT_io",
            "S8_BT_io",
            "S9_BT_io",
            "sat_zenith_tn",
            "sat_zenith_to",
            "total_column_water_vapour_tx"
    };

    public static final String LOGGER_NAME = "snap.processor.arc";
    public static final String PROCESS_DUAL_VIEW_SST_LABELTEXT = "Generate dual-view SST";
    public static final String PROCESS_DUAL_VIEW_SST_DESCRIPTION = "Enables/disables generation of the dual-view SST";
    public static final String DEFAULT_DUAL_VIEW_BITMASK = "";
    public static final String PROCESS_NADIR_VIEW_SST_LABELTEXT = "Generate nadir-view SST";
    public static final String PROCESS_NADIR_VIEW_SST_DESCRIPTION = "Enables/disables generation of the nadir-view SST";
    public static final String NADIR_VIEW_COEFF_FILE_DESCRIPTION = "Coefficient file for the nadir-view SST";
    public static final String DUAL_VIEW_COEFF_FILE_DESCRIPTION = "Coefficient file for the dual-view SST";
    public static final String DEFAULT_NADIR_VIEW_BITMASK = "";
    public static final String OUT_BAND_UNIT = "K";
    public static final String OUT_BAND_NADIR_DESCRIPTION = "Nadir-view sea surface temperature";
    public static final String OUT_BAND_DUAL_DESCRIPTION = "Combined view sea surface temperature";
    public static final String PROCESS_ASDI_LABELTEXT = "Generate ASDI";
    public static final String PROCESS_ASDI_DESCRIPTION = "Enables/disables generation of ATSR Saharan Dust Index";
    public static final String DEFAULT_ASDI_BITMASK = "";
    public static final String ASDI_COEFF_FILE_DESCRIPTION = "Coefficient file for ASDI";

}
