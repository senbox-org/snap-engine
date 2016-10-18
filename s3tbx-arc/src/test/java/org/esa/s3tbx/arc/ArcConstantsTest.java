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

import org.junit.Test;

import static org.junit.Assert.*;

public class ArcConstantsTest {

    @Test
    public void testBandNameConstants() {
        assertEquals("btemp_nadir_0370", ArcConstants.SOURCE_RASTER_NAMES_AATSR[0]);
        assertEquals("btemp_nadir_1100", ArcConstants.SOURCE_RASTER_NAMES_AATSR[1]);
        assertEquals("btemp_nadir_1200", ArcConstants.SOURCE_RASTER_NAMES_AATSR[2]);
        assertEquals("btemp_fward_0370", ArcConstants.SOURCE_RASTER_NAMES_AATSR[3]);
        assertEquals("btemp_fward_1100", ArcConstants.SOURCE_RASTER_NAMES_AATSR[4]);
        assertEquals("btemp_fward_1200", ArcConstants.SOURCE_RASTER_NAMES_AATSR[5]);
    }
}
