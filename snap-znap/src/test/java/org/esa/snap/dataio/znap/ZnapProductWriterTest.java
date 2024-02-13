package org.esa.snap.dataio.znap;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.runtime.Config;
import org.junit.Test;

import java.util.prefs.Preferences;

import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ZnapProductWriterTest {

    @Test
    @STTM("SNAP-3636")
    public void  testIsUseZipArchive()  {
        final boolean clearProperty = false;
       testIsUseZip(false, clearProperty);
       testIsUseZip(true, clearProperty);
    }

    @Test
    @STTM("SNAP-3636")
    public void  testIsUseZipArchive_defaultProperty()  {
        final boolean clearProperty = true;
        testIsUseZip(true, clearProperty);
    }

    private static void testIsUseZip(boolean useZip, boolean clearProperty) {
        final Preferences preferences = Config.instance("snap").load().preferences();
        final String oldValue = preferences.get(PROPERTY_NAME_USE_ZIP_ARCHIVE, "true");

        try {
            final ZnapProductWriter writer = new ZnapProductWriter(new ZnapProductWriterPlugIn());
            if (clearProperty) {
                preferences.remove(PROPERTY_NAME_USE_ZIP_ARCHIVE);
            } else {
                preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, Boolean.toString(useZip));
            }

            if (useZip) {
                assertTrue(ZnapProductWriter.isUseZipArchive());
            } else {
                assertFalse(ZnapProductWriter.isUseZipArchive());   ;
            }
        } finally {
            preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, oldValue);
        }
    }
}
