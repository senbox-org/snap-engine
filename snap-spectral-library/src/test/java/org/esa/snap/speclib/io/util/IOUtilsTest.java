package org.esa.snap.speclib.io.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.io.csv.util.CsvUtils;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;


public class IOUtilsTest {


    @Test
    @STTM("SNAP-4177")
    public void test_hasExtension() {
        assertTrue(IOUtils.hasExtension(Paths.get("library.csv"), "csv"));
        assertTrue(IOUtils.hasExtension(Paths.get("library.CSV"), "csv"));
        assertFalse(IOUtils.hasExtension(Paths.get("library.geojson"), "csv"));
        assertFalse(IOUtils.hasExtension(null, "csv"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_stripExtension() {
        assertEquals("library", IOUtils.stripExtension("library.csv"));
        assertEquals("library.abc", IOUtils.stripExtension("library.abc.csv"));
        assertEquals("library", IOUtils.stripExtension("library"));
        assertEquals(".hidden", IOUtils.stripExtension(".hidden"));
    }
}