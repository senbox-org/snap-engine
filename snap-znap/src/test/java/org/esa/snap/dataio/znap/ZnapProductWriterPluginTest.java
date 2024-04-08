package org.esa.snap.dataio.znap;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.runtime.Config;
import org.junit.Test;

import java.util.prefs.Preferences;

import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;
import static org.junit.Assert.assertEquals;

public class ZnapProductWriterPluginTest {

    @Test
    @STTM("SNAP-3636")
    public void testGetDefaultFileExtensions_noZip() {
        testDefaultFileExtension("false", ".znap");
    }

    @Test
    @STTM("SNAP-3636")
    public void testGetDefaultFileExtensions_zip() {
        testDefaultFileExtension("true", ".znap.zip");
    }

    private static void testDefaultFileExtension(String useZipArchive, String expectedExtension) {
        final Preferences preferences = Config.instance("snap").load().preferences();
        final String oldValue = preferences.get(PROPERTY_NAME_USE_ZIP_ARCHIVE, "true");

        try {
            preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, useZipArchive);

            final ZnapProductWriterPlugIn plugIn = new ZnapProductWriterPlugIn();
            final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();
            assertEquals(1, defaultFileExtensions.length);
            assertEquals(expectedExtension, defaultFileExtensions[0]);
        } finally {
            preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, oldValue);
        }
    }
}
