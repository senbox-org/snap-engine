package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.*;

public class BandGroupingImplTest {

    @Test
    @STTM("SNAP-3702")
    public void testIndexConstruction() {
        final BandGroupingPath bandGroupingPath = new BandGroupingPath(new String[]{"whatever"});
        final int idx = 27;

        BandGroupingImpl.Index index = new BandGroupingImpl.Index(bandGroupingPath, idx);
        assertEquals(idx, index.index);
        assertEquals(bandGroupingPath, index.path);
    }

    @Test
    @STTM("SNAP-3702")
    public void testHashCode() {
        final String[][] paths = {
                new String[]{"L_1", "L_2"},
                new String[]{"L_1_err"}
        };

        final BandGroupingImpl bandGrouping = new BandGroupingImpl(paths);
        assertEquals(1501189701, bandGrouping.hashCode());
    }


    @SuppressWarnings({"SimplifiableAssertion", "EqualsWithItself"})
    @Test
    @STTM("SNAP-3702")
    public void testEquals() {
        final String[][] paths = {
                new String[]{"L_1", "L_2"},
                new String[]{"L_1_err"}
        };
        final String[][] empty = new String[0][];

        final BandGroupingImpl theOne = new BandGroupingImpl(paths);
        final BandGroupingImpl theOther = new BandGroupingImpl(paths);
        final BandGroupingImpl theDifferent = new BandGroupingImpl(empty);

        assertTrue(theOne.equals(theOther));
        assertTrue(theOther.equals(theOne));
        assertTrue(theOne.equals(theOne));

        assertFalse(theOne.equals(theDifferent));
        assertFalse(theDifferent.equals(theOther));

        assertFalse(theDifferent.equals(new Double(23.778)));
    }

    @Test
    @STTM("SNAP-3702")
    public void testFormat() {
        final String[][] paths = {
                new String[]{"L_1", "L_2"},
                new String[]{"L_1_err"}
        };

        BandGroupingImpl bandGrouping = new BandGroupingImpl(paths);
        assertEquals("L_1/L_2:L_1_err", bandGrouping.format());

        final String[][] empty = new String[0][];
        bandGrouping = new BandGroupingImpl(empty);
        assertEquals("", bandGrouping.format());
    }

    @Test
    @STTM("SNAP-3702")
    public void testSize() {
        final String[][] paths = {
                new String[]{"AMP", "BIMP"},
                new String[]{"dark", "bright"}
        };

        BandGroupingImpl bandGrouping = new BandGroupingImpl(paths);
        assertEquals(2, bandGrouping.size());
    }

    @Test
    @STTM("SNAP-3702")
    public void testGet() {
        final String[][] paths = {
                new String[]{"AMP", "BIMP"},
                new String[]{"dark", "bright"}
        };

        final BandGroupingImpl bandGrouping = new BandGroupingImpl(paths);
        final String[] resultPaths = bandGrouping.get(1);
        assertEquals(2, resultPaths.length);
        assertEquals("dark", resultPaths[0]);

        // @todo 2 tb/** do we want this behaviour??? 2024-06-11
        try {
            bandGrouping.get(3);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {

        }

        try {
            bandGrouping.get(-1);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {

        }
    }

    @Test
    @STTM("SNAP-3702")
    public void testIndexOf() {
        final String[][] paths = {
                new String[]{"AMP", "BIMP"},
                new String[]{"dark", "bright"}
        };

        // @todo tb/** this is odd behaviour! 2024-06-11
        final BandGroupingImpl bandGrouping = new BandGroupingImpl(paths);
        assertEquals(-1, bandGrouping.indexOf("dark"));
    }

    @Test
    @STTM("SNAP-3702")
    public void testParse() {
        BandGrouping bandGrouping = BandGroupingImpl.parse("L_1:L_1/err:L_2:L_2/err:L_10:L_10/err:L_11:L_11/err:L_21:L_21/err");
        assertEquals(10, bandGrouping.size());

        assertNull(BandGroupingImpl.parse(""));
    }
}
