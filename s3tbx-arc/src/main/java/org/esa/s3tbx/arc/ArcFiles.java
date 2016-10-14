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

public enum ArcFiles {
    ASDI_AATSR("AATSR ASDI coeffs", "ASDI_AATSR.coef"),
    ASDI_ATSR1("ATSR1 ASDI coeffs", "ASDI_ATSR1.coef"),
    ASDI_ATSR2("ATSR2 ASDI coeffs", "ASDI_ATSR2.coef"),
    ARC_N2_SLSTR("SLSTR ARC N2 coeffs","ARC_N2_SLSTR_2011.coef"),
    ARC_N3_SLSTR("SLSTR ARC N3 coeffs","ARC_N3_SLSTR_2011.coef"),
    ARC_D2_SLSTR("SLSTR ARC D2 coeffs","ARC_D2_SLSTR_2011.coef"),
    ARC_D3_SLSTR("SLSTR ARC D3 coeffs","ARC_D3_SLSTR_2011.coef"),
    ARC_N2_AATSR("AATSR ARC N2 coeffs","ARC_N2_AATSR_2007.coef"),
    ARC_N3_AATSR("AATSR ARC N3 coeffs","ARC_N3_AATSR_2007.coef"),
    ARC_D2_AATSR("AATSR ARC D2 coeffs","ARC_D2_AATSR_2007.coef"),
    ARC_D3_AATSR("AATSR ARC D3 coeffs","ARC_D3_AATSR_2007.coef"),
    ARC_N2_ATSR1("ATSR1 ARC N2 coeffs","ARC_N2_ATSR1_1995.coef"),
    ARC_N3_ATSR1("ATSR1 ARC N3 coeffs","ARC_N3_ATSR1_1995.coef"),
    ARC_D2_ATSR1("ATSR1 ARC D2 coeffs","ARC_D2_ATSR1_1995.coef"),
    ARC_D3_ATSR1("ATSR1 ARC D3 coeffs","ARC_D3_ATSR1_1995.coef"),
    ARC_N2_ATSR2("ATSR2 ARC N2 coeffs","ARC_N2_ATSR2_1999.coef"),
    ARC_N3_ATSR2("ATSR2 ARC N3 coeffs","ARC_N3_ATSR2_1999.coef"),
    ARC_D2_ATSR2("ATSR2 ARC D2 coeffs","ARC_D2_ATSR2_1999.coef"),
    ARC_D3_ATSR2("ATSR2 ARC D3 coeffs","ARC_D3_ATSR2_1999.coef");

    private final String label;
    private final String filename;

    ArcFiles(String label, String filename) {
        this.label = label;
        this.filename = filename;
    }

    @Override
    public String toString() {
        return this.label;
    }

    public String getFilename() { return this.filename; }

}
