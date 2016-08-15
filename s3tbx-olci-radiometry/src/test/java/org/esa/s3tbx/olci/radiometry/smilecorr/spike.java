/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.junit.Test;


import static org.junit.Assert.assertEquals;

/**
 * @author muhammad.bc.
 */

public class spike {
    @Test
    public void testCheck() throws Exception {
        double CO2 = 3.E-4;
        assertEquals(3.0e-4, CO2, 1e-4);
        System.out.println(Math.ceil(708.75));
        assertEquals(709, Math.ceil(708.75), 1e-1);
    }
}
