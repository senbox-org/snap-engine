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

package org.esa.snap.lib.openjpeg.dataio;

import org.esa.snap.lib.openjpeg.utils.CommandOutput;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;
import org.esa.snap.lib.openjpeg.utils.OpenJpegUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpjDumpTest {

    Path opjDumpPath;

    @Before
    public void retreiveOpjDump() {
        opjDumpPath = Paths.get(OpenJpegExecRetriever.getOpjDump());
        Assume.assumeTrue(Files.exists(opjDumpPath));
    }

    @Test
    public void testGetTileLayoutFromOpjDump() throws URISyntaxException, IOException {
        String jp2Path = "/org/esa/snap/lib/openjpeg/dataio/s2/l2a/S2A_USER_MSI_L2A_TL_MPS__20150210T180608_A000069_T14RMQ_B03_20m.jp2";

        final Path pathToJP2File = Paths.get(OpjDumpTest.class.getResource(jp2Path).toURI());
        try {
            OpenJpegUtils.getTileLayoutWithOpenJPEG(opjDumpPath.toAbsolutePath().toString(), pathToJP2File);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testLaunchOpjDump() throws URISyntaxException, IOException {
        String jp2Path = "/org/esa/snap/lib/openjpeg/dataio/s2/l2a/S2A_USER_MSI_L2A_TL_MPS__20150210T180608_A000069_T14RMQ_B03_20m.jp2";

        String pathToJp2File = OpjDumpTest.class.getResource(jp2Path).getPath();

        if(IS_OS_WINDOWS) {
            pathToJp2File = pathToJp2File.substring(1);
        }

        ProcessBuilder builder = new ProcessBuilder(opjDumpPath.toAbsolutePath().toString(), "-i", pathToJp2File);

        try {

            CommandOutput cout = OpenJpegUtils.runProcess(builder);
            assertNotNull(cout);
            assertTrue("Wrong output: " + cout.getTextOutput(), cout.getTextOutput().contains("correctly decoded"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
