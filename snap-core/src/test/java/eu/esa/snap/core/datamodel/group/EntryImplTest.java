package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntryImplTest {

    @Test
    @STTM("SNAP-3702")
    public void testMatches()  {
        final EntryImpl entry = new EntryImpl("groob");

        assertTrue(entry.matches("testinggroob"));
        assertTrue(entry.matches("groob_bandOne"));

        assertFalse(entry.matches("water_leaving_frog"));
    }
}
