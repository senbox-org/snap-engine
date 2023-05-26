package org.esa.lib.gdal.activator;

import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.esa.snap.dataio.gdal.GDALLoaderConfig.CONFIG_NAME;
import static org.junit.Assert.*;

public class GDALInstallInfoTest {

    private static final String INVALID_BIN_LOCATION = "/mock";
    private static final String VALID_BIN_LOCATION = System.getProperty("user.home");
    private static final String PREFERENCES_KEY = "gdal.apps.path";
    private static final byte CONTROL_STATE = 2;

    private static final GDALInstallInfo TEST_INSTANCE = GDALInstallInfo.INSTANCE;

    private String currentValue;

    private Preferences fetchPreferences() {
        final Config config = Config.instance(CONFIG_NAME);
        config.load();
        return config.preferences();
    }

    private String readPreferencesValue() {
        return fetchPreferences().get(PREFERENCES_KEY, null);
    }

    private void writePreferencesValue(String value) {
        try {
            final Preferences preferences = fetchPreferences();
            preferences.put(PREFERENCES_KEY, value);
            preferences.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() {
        currentValue = readPreferencesValue();
    }

    @After
    public void cleanUp() {
        TEST_INSTANCE.setLocations(Paths.get(INVALID_BIN_LOCATION));
        if (currentValue != null) {
            writePreferencesValue(currentValue);
        }
    }

    @Test
    public void testSetLocations() {
        TEST_INSTANCE.setLocations(Paths.get(VALID_BIN_LOCATION));
        assertEquals(VALID_BIN_LOCATION, readPreferencesValue());
    }

    @Test
    public void testSetListener() {
        final MockGDALWriterPlugInListener mockGDALWriterPlugInListener = new MockGDALWriterPlugInListener();
        TEST_INSTANCE.setLocations(Paths.get(VALID_BIN_LOCATION));
        TEST_INSTANCE.setListener(mockGDALWriterPlugInListener);
        assertEquals(CONTROL_STATE, mockGDALWriterPlugInListener.getState());
    }

    @Test
    public void testIsPresent() {
        TEST_INSTANCE.setLocations(Paths.get(INVALID_BIN_LOCATION));
        assertFalse(TEST_INSTANCE.isPresent());
        TEST_INSTANCE.setLocations(Paths.get(VALID_BIN_LOCATION));
        assertTrue(TEST_INSTANCE.isPresent());
    }

    private static class MockGDALWriterPlugInListener implements Runnable, GDALWriterPlugInListener {

        private byte state = 0;

        @Override
        public void run() {
            state = 1;
        }

        @Override
        public void writeDriversSuccessfullyInstalled() {
            state = CONTROL_STATE;
        }

        public byte getState() {
            return state;
        }
    }
}
