package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.*;

public class BandGroupPathTest {

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
    @STTM("SNAP-3728")
    public void testConstruction_bandNames() {
        final String[] strings = {"TOA#acdom_443,bbp_443,kd_490,bbp_slope"};
        final BandGroupingPath bandGroupingPath = new BandGroupingPath(strings);

        assertTrue(bandGroupingPath.matchesGrouping("bbp_slope"));
        assertTrue(bandGroupingPath.matchesGrouping("acdom_443"));

        assertFalse(bandGroupingPath.matchesGrouping("Oa10_reflectance"));
        assertFalse(bandGroupingPath.matchesGrouping("latitude"));
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

    @Test
    @STTM("SNAP-3702")
    public void testMatchesGrouping() {
        final String[] strings = {"test"};

        final BandGroupingPath bandGroupingPath = new BandGroupingPath(strings);
        assertTrue(bandGroupingPath.matchesGrouping("test_refl_03"));
        assertFalse(bandGroupingPath.matchesGrouping("longitude"));
    }
}
