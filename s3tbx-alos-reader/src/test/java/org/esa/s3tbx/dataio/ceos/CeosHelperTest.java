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

package org.esa.s3tbx.dataio.ceos;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
public class CeosHelperTest {

    @Test
    public void testGetFileFromInput() {
        String inputString = "testFile";

        File result = CeosHelper.getFileFromInput(inputString);
        assertEquals(inputString, result.getName());

        File inputFile = new File("anotherTest");
        result = CeosHelper.getFileFromInput(inputFile);
        assertEquals(inputFile.getName(), result.getName());

        result = CeosHelper.getFileFromInput(new Double(9));
        assertNull(result);
    }

}
