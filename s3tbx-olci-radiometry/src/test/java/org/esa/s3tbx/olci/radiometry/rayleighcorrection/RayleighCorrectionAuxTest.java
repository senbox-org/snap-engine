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

import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.net.URL;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

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
    public void testAuxValue() throws Exception {
        Properties properties = RayleighCorrectionAux.loadAuxdata();
        Set<String> strings = properties.stringPropertyNames();

    }
}