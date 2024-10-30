package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WildCardEntryTest {

    @Test
    @STTM("SNAP-3702")
    public void testMatches() {
        WildCardEntry wildCardEntry = new WildCardEntry("/home/is/here/data.nc");
        assertTrue(wildCardEntry.matches("/home/is/here/data.nc"));

        wildCardEntry = new WildCardEntry("*.test");
        assertTrue(wildCardEntry.matches("data.test"));
        assertFalse(wildCardEntry.matches("nasenmann.txt"));
    }
}
