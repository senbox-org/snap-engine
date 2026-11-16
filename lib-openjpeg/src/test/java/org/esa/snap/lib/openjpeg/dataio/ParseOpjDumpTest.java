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

import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.esa.snap.lib.openjpeg.jp2.TileLayout;
import org.esa.snap.lib.openjpeg.utils.OpenJpegUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParseOpjDumpTest {

    @Test
    public void testRun4() throws IOException
    {
        String jp2Path = "/org/esa/snap/lib/openjpeg/dataio/s2/l2a/out.txt";
        final URL fileURL = ParseOpjDumpTest.class.getResource(jp2Path);
        assertNotNull(fileURL);
        final Path file = UrlHelper.urlToFile(fileURL);
        assertNotNull(file);

        List<String> content = Files.readAllLines(file);

        TileLayout result = OpenJpegUtils.parseOpjDump(content);
        
        assertEquals(5490, result.width);
        assertEquals(5490, result.height);
        assertEquals(5490, result.tileWidth);
        assertEquals(5490, result.tileHeight);
        assertEquals(1, result.numXTiles);
        assertEquals(1, result.numYTiles);
        assertEquals(6, result.numResolutions);
    }

    @Test
    public void testRun5() throws IOException
    {
        String jp2Path = "/org/esa/snap/lib/openjpeg/dataio/s2/l1c/out.txt";
        final URL fileURL = ParseOpjDumpTest.class.getResource(jp2Path);
        assertNotNull(fileURL);
        final Path file = UrlHelper.urlToFile(fileURL);
        assertNotNull(file);

        List<String> content = Files.readAllLines(file);

        TileLayout result = OpenJpegUtils.parseOpjDump(content);

        assertEquals(10980, result.width);
        assertEquals(10980, result.height);
        assertEquals(2048, result.tileWidth);
        assertEquals(2048, result.tileHeight);
        assertEquals(6, result.numXTiles);
        assertEquals(6, result.numYTiles);
        assertEquals(6, result.numResolutions);
    }

}
