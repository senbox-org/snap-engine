/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.envisat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnvisatProductReaderTest {

    @Test
    public void testGetResolutionInKilometers() {
        assertEquals(0.3, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.3, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_FRS_L1B_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.3, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_FSG_L1B_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.3, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_FRG_L1B_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.3, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_FR_L2_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.3, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_FRS_L2_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.3, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_FSG_L2_PRODUCT_TYPE_NAME), 1e-8);

        assertEquals(1.2, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(1.2, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_RRG_L1B_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(1.2, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_RR_L2_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(1.2, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_RRC_L2_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(1.2, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.MERIS_RRV_L2_PRODUCT_TYPE_NAME), 1e-8);

        assertEquals(1.0, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(1.0, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.AATSR_L2_NR_PRODUCT_TYPE_NAME), 1e-8);

        assertEquals(0.0125, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_APG_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.03, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_APP_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.225, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_AP_BP_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.15, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_APM_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.012, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_APS_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(1.0, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_GM1_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.225, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_IM_BP_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.0125, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_IMG_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.15, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_IMM_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.03, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_IMP_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.008, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_IMS_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.9, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_WS_BP_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.15, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_WSM_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.008, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_WSS_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.02, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_WVI_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(5.0, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L1B_WVS_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(5.0, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.ASAR_L2_WVW_PRODUCT_TYPE_NAME), 1e-8);

        assertEquals(0.225, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.SAR_IM__BP_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.0125, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.SAR_IMG_1P_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.075, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.SAR_IMM_1P_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.0125, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.SAR_IMP_1P_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(0.0125, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.SAR_IMS_1P_PRODUCT_TYPE_NAME), 1e-8);

        assertEquals(1.0, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.AT1_L1B_TOA_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(1.0, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.AT1_L2_NR_PRODUCT_TYPE_NAME), 1e-8);

        assertEquals(1.0, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.AT2_L1B_TOA_PRODUCT_TYPE_NAME), 1e-8);
        assertEquals(1.0, EnvisatProductReader.getResolutionInKilometers(EnvisatConstants.AT2_L2_NR_PRODUCT_TYPE_NAME), 1e-8);
    }
}
