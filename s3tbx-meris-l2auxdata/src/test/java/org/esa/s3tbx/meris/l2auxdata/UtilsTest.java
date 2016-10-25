/*
 * $Id: UtilsTest.java,v 1.1 2007/03/27 12:51:42 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

import com.google.common.jimfs.Jimfs;
import junit.framework.TestCase;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

    @Test
    public void testDownloadAndInstall() throws Exception {
        FileSystem fs = Jimfs.newFileSystem();
        Path target = fs.getPath("target");
        Utils.downloadAndInstallAuxdata(target, UtilsTest.class.getResource("zip.zip"));
        Path zipDir = target.resolve("zip");
        assertTrue(Files.isDirectory(zipDir));
        Path imageFile = zipDir.resolve("image.png");
        assertTrue(Files.isRegularFile(imageFile));
        assertEquals(45088, Files.size(imageFile));
        Path textFile = zipDir.resolve("folder").resolve("text.txt");
        assertTrue(Files.isRegularFile(textFile));
        assertEquals(1710, Files.size(textFile));

        fs.close();
    }

    @Test
    public void testSeasonalFactorComputation() {
        double meanEarthSunDist = 149.60e+06 * 1000;
        final double sunEarthDistanceSquare = meanEarthSunDist * meanEarthSunDist;

        final double vStart = Utils.computeSeasonalFactor(0, sunEarthDistanceSquare);
        assertEquals(1, vStart, 0.05);
        assertTrue(vStart < 1.0);

        final double vMid = Utils.computeSeasonalFactor(0.5 * 365, sunEarthDistanceSquare);
        assertEquals(1, vMid, 0.05);
        assertTrue(vMid > 1.0);

        final double vEnd = Utils.computeSeasonalFactor(365, sunEarthDistanceSquare);
        assertEquals(1, vEnd, 0.05);
        assertTrue(vEnd < 1.0);
    }
}

