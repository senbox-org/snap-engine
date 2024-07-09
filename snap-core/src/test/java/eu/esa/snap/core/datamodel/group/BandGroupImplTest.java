package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

public class BandGroupImplTest {

    @Test
    @STTM("SNAP-3702")
    public void testIndexConstruction() {
        final BandGroupingPath bandGroupingPath = new BandGroupingPath(new String[]{"whatever"});
        final int idx = 27;

        BandGroupImpl.Index index = new BandGroupImpl.Index(bandGroupingPath, idx);
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

        final BandGroupImpl bandGrouping = new BandGroupImpl(paths);
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

        final BandGroupImpl theOne = new BandGroupImpl(paths);
        final BandGroupImpl theOther = new BandGroupImpl(paths);
        final BandGroupImpl theDifferent = new BandGroupImpl(empty);

        assertTrue(theOne.equals(theOther));
        assertTrue(theOther.equals(theOne));
        assertTrue(theOne.equals(theOne));

        assertFalse(theOne.equals(theDifferent));
        assertFalse(theDifferent.equals(theOther));

        assertFalse(theDifferent.equals(23.778));
    }

    @Test
    @STTM("SNAP-3702")
    public void testFormat() {
        final String[][] paths = {
                new String[]{"L_1", "L_2"},
                new String[]{"L_1_err"}
        };

        BandGroupImpl bandGrouping = new BandGroupImpl(paths);
        assertEquals("L_1/L_2:L_1_err", bandGrouping.format());

        final String[][] empty = new String[0][];
        bandGrouping = new BandGroupImpl(empty);
        assertEquals("", bandGrouping.format());
    }

    @Test
    @STTM("SNAP-3702")
    public void testSize() {
        final String[][] paths = {
                new String[]{"AMP", "BIMP"},
                new String[]{"dark", "bright"}
        };

        BandGroupImpl bandGrouping = new BandGroupImpl(paths);
        assertEquals(2, bandGrouping.size());
    }

    @Test
    @STTM("SNAP-3702")
    public void testGet() {
        final String[][] paths = {
                new String[]{"AMP", "BIMP"},
                new String[]{"dark", "bright"}
        };

        final BandGroupImpl bandGrouping = new BandGroupImpl(paths);
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
        final BandGroupImpl bandGrouping = new BandGroupImpl(paths);
        assertEquals(-1, bandGrouping.indexOf("dark"));
    }

    @Test
    @STTM("SNAP-3702")
    public void testParse() {
        BandGroup bandGroup = BandGroupImpl.parse("L_1:L_1/err:L_2:L_2/err:L_10:L_10/err:L_11:L_11/err:L_21:L_21/err");
        assertEquals(10, bandGroup.size());

        assertNull(BandGroupImpl.parse(""));
    }

    @Test
    @STTM("SNAP-3702")
    public void testSetGetName() {
        final BandGroupImpl bandGrouping = new BandGroupImpl(new String[0][]);

        assertEquals("", bandGrouping.getName());

        bandGrouping.setName("heffalump");
        assertEquals("heffalump", bandGrouping.getName());
    }

    @Test
    @STTM("SNAP-3702")
    public void testGetMatchingBandnames_emptyGrouping() {
        final BandGroupImpl bandGrouping = new BandGroupImpl(new String[0][]);

        final Product product = new Product("testy", "test_type");
        product.addBand(new Band("short", ProductData.TYPE_INT16, 5, 6));
        product.addBand(new Band("shorter", ProductData.TYPE_INT16, 5, 6));

        String[] names = bandGrouping.getMatchingBandNames(product);
        assertEquals(0, names.length);
    }

    @Test
    @STTM("SNAP-3702")
    public void testGetMatchingBandnames_oneGrouping() {
        final BandGroupImpl bandGrouping = (BandGroupImpl) BandGroupImpl.parse("L_1:L_1_err");

        final Product product = new Product("testy", "test_type");
        product.addBand(new Band("L_1_CAM1", ProductData.TYPE_INT16, 5, 6));
        product.addBand(new Band("shorter", ProductData.TYPE_INT16, 5, 6));

        String[] names = bandGrouping.getMatchingBandNames(product);
        assertEquals(1, names.length);
        assertEquals("L_1_CAM1", names[0]);
    }

    @Test
    @STTM("SNAP-3702")
    public void testGetMatchingBandnames_twoGroupings() {
        final BandGroupImpl bandGrouping = (BandGroupImpl) BandGroupImpl.parse("M*_rho_toa/lambda0:FWHM");

        final Product product = new Product("testy", "test_type");
        product.addBand(new Band("M08_rho_toa", ProductData.TYPE_INT16, 5, 6));
        product.addBand(new Band("lambda0_c02", ProductData.TYPE_INT16, 5, 6));
        product.addBand(new Band("unmatched", ProductData.TYPE_INT16, 5, 6));

        String[] names = bandGrouping.getMatchingBandNames(product);
        assertEquals(2, names.length);
        assertEquals("M08_rho_toa", names[0]);
        assertEquals("lambda0_c02", names[1]);
    }

    @Test
    @STTM("SNAP-3702")
    public void testCreateWithBandNames() {
        final String[] bandNames = {"radiance_1", "radiance_2", "radiance_5"};

        final BandGroupImpl bandGroup = new BandGroupImpl("my_radiances", bandNames);
        assertEquals("my_radiances", bandGroup.getName());

        final String[] returnedNames = bandGroup.get(0);
        assertArrayEquals(bandNames, returnedNames);
    }

    @Test
    @STTM("SNAP-3702")
    public void testCreateWithBandNames_getMatchingBandNames() {
        final String[] bandNames = {"reflec_03", "reflec_04", "reflec_08"};

        final BandGroupImpl bandGroup = new BandGroupImpl("important_reflec", bandNames);

        final Product product = new Product("testy", "test_type");
        for (int i = 1; i < 10; i++){
            product.addBand(new Band("reflec_0" + i, ProductData.TYPE_INT16, 5, 6));
        }

        final String[] names = bandGroup.getMatchingBandNames(product);
        assertArrayEquals(bandNames, names);
    }
}
