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

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrectionAuxTest {
    @Test
    public void testAuxDataExist() throws Exception {
        Path path = RayleighCorrectionAux.installAuxdata();
        Path aux = path.resolve("MER_ATP_AXVACR20091126_115724_20020429_041400_20021224_121445");
        assertNotNull(aux);
    }

    @Test
    public void testAuxData() throws Exception {
        float[][][][] loadAuxdata = RayleighCorrectionAux.loadAuxdata();
        assertEquals(3, loadAuxdata.length);
        assertEquals(12, loadAuxdata[0].length);
        assertEquals(12, loadAuxdata[0][1].length);
        assertEquals(4, loadAuxdata[0][0][0].length);
    }

    @Test
    public void testAuxDataII() throws Exception {
        RayleighCorrectionAux aux = new RayleighCorrectionAux();
        aux.readADF();
    }
}