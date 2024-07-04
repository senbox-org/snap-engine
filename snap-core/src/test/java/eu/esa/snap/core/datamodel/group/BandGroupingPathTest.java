package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.*;

public class BandGroupingPathTest {

    @Test
    @STTM("SNAP-3702")
    public void testConstruction_empty() {
        final String[] strings = new String[0];
        final BandGroupingPath bandGroupingPath = new BandGroupingPath(strings);
        final String[] inputPath = bandGroupingPath.getInputPath();

        assertSame(strings, inputPath);
    }

    @Test
    @STTM("SNAP-3702")
    public void testConstruction_standard() {
        final String[] strings = {"band/path/test"};
        final BandGroupingPath bandGroupingPath = new BandGroupingPath(strings);
        final String[] inputPath = bandGroupingPath.getInputPath();

        assertSame(strings, inputPath);
    }

    @Test
    @STTM("SNAP-3702")
    public void testContains() {
        final String[] strings = {"test"};
        final BandGroupingPath bandGroupingPath = new BandGroupingPath(strings);
        assertTrue(bandGroupingPath.contains("test_refl_03"));
        assertFalse(bandGroupingPath.contains("longitude"));
    }

    @Test
    @STTM("SNAP-3702")
    public void testContains_wildcard() {
        String[] strings = {"foo/*/test.txt"};

        BandGroupingPath bandGroupingPath = new BandGroupingPath(strings);

        assertTrue(bandGroupingPath.contains("foo/bar/test.txt"));

        strings = new String[]{"foo/**"};
        bandGroupingPath = new BandGroupingPath(strings);
        assertTrue(bandGroupingPath.contains("foo/bar/doz/test.txt"));
    }
}
