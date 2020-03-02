/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2013-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.lib.openjpeg.dataio.s2;

import org.apache.commons.lang.SystemUtils;
import org.esa.snap.lib.openjpeg.dataio.Utils;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author opicas-p
*/
public class ShortenTest {

    @Before
    public void beforeMethod() {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
    }

    /**
     * Test we can shorten a long directory name. Can't test with a very long directory name since
     * it is not valid on Windows.
     *
     * @throws Exception
     */
    @Test
    public void testLongDirectoryName() throws Exception {
        String mediumPath = "C:\\5A3AA7c3-475c-42a5-9a25-94d6a93c67b7\\S2A_OPER_PRD_MSIL1C_PDMC_20130621T120000_R065_V20091211T165928_20091211T170025";
        File directory = new File(mediumPath);

        if (directory.mkdirs()) {
            String shortPath = Utils.GetShortPathNameW(mediumPath);
            Assert.assertEquals("C:\\5A3AA7~1\\S2A_OP~1", shortPath);
            FileUtils.deleteTree(directory.getParentFile());
        }
    }

    /**
     * Test we can shorten a long file name. Can't test with a very long file name since
     * it is not valid on Windows.
     *
     * @throws Exception
     */
    @Test
    public void testLongFileName() throws Exception {
        String mediumPath = "C:\\5A3AA7c3-475c-42a5-9a25-94d6a93c67b7\\S2A_OPER_PRD_MSIL1C_PDMC_20130621T120000_R065_V20091211T165928_20091211T170025.JP2";
        File mediumFile = new File(mediumPath);
        if (mediumFile.getParentFile().mkdir() && mediumFile.createNewFile()) {
            String shortPath = Utils.GetShortPathNameW(mediumPath);
            Assert.assertEquals("C:\\5A3AA7~1\\S2A_OP~1.JP2", shortPath);
            FileUtils.deleteTree(mediumFile.getParentFile());
        }
    }

    /**
     * Test GetIterativeShortPathNameW returns "" when the path does not exist.
     * It also tests we cleaned well the files created
     *
     * @throws Exception
     */
    @Test
    public void testVeryLongFileName2() throws Exception {
        String nonExistingPath = "C:\\5a3aa7c3-475c-42a5-9a25-94d6a93c67b7";
        String shortPath = Utils.GetIterativeShortPathNameW(nonExistingPath);
        Assert.assertEquals("", shortPath);
    }
}
