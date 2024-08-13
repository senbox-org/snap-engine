package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BandNamesEntryTest {

    @Test
    @STTM("SNAP-3728")
    public void testMatches() {
        final BandNamesEntry iopGrouping = new BandNamesEntry("IOP", "anw_443,acdm_443,aphy_443");

        assertTrue(iopGrouping.matches("acdm_443"));
        assertTrue(iopGrouping.matches("aphy_443"));

        assertFalse(iopGrouping.matches("Oa10_reflectance"));
        assertFalse(iopGrouping.matches(""));
        assertFalse(iopGrouping.matches(null));
    }
}
