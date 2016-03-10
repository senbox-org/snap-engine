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
        assertEquals("btemp_nadir_0370", ArcConstants.NADIR_370_BAND);
        assertEquals("btemp_nadir_1100", ArcConstants.NADIR_1100_BAND);
        assertEquals("btemp_nadir_1200", ArcConstants.NADIR_1200_BAND);
        assertEquals("btemp_fward_0370", ArcConstants.FORWARD_370_BAND);
        assertEquals("btemp_fward_1100", ArcConstants.FORWARD_1100_BAND);
        assertEquals("btemp_fward_1200", ArcConstants.FORWARD_1200_BAND);
        assertEquals("sun_elev_nadir", ArcConstants.SUN_ELEV_NADIR);
        assertEquals("sun_elev_fward", ArcConstants.SUN_ELEV_FORWARD);
    }
}
