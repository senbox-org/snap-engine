package org.esa.snap.dataio.gdal;

import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.esa.snap.dataio.gdal.GDALLoaderConfig.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class GDALLoaderConfigTest {

    private static final boolean USE_INSTALLED_GDAL_LIBRARY = true;
    private static final boolean USE_INTERNAL_GDAL_LIBRARY = false;
    private static final String SELECTED_INSTALLED_GDAL_LIBRARY = "gdal-3-2-1";

    private static final GDALLoaderConfig TEST_GDAL_LOADER_CONFIG = GDALLoaderConfig.getInstance();

    private Boolean currentUseInstalledGDALLibrary;
    private String currentSelectedInstalledGDALLibrary;

    private Preferences fetchPreferences() {
        return Config.instance(CONFIG_NAME).load().preferences();
    }

    @Before
    public void setUp() {
        final Preferences preferences = fetchPreferences();
        currentUseInstalledGDALLibrary = preferences.getBoolean(PREFERENCE_KEY_USE_INSTALLED_GDAL, true);
        currentSelectedInstalledGDALLibrary = preferences.get(PREFERENCE_KEY_SELECTED_INSTALLED_GDAL, PREFERENCE_NONE_VALUE_SELECTED_INSTALLED_GDAL);

    }

    @After
    public void cleanUp() {
        try {
            final Preferences preferences = fetchPreferences();
            final GDALLoaderConfig gdalLoaderConfig = GDALLoaderConfig.getInstance();
            if (currentUseInstalledGDALLibrary != null) {
                preferences.putBoolean(PREFERENCE_KEY_USE_INSTALLED_GDAL, currentUseInstalledGDALLibrary);
                gdalLoaderConfig.setUseInstalledGDALLibrary(currentUseInstalledGDALLibrary);
            }
            if (currentSelectedInstalledGDALLibrary != null) {
                preferences.put(PREFERENCE_KEY_SELECTED_INSTALLED_GDAL, currentSelectedInstalledGDALLibrary);
                gdalLoaderConfig.setSelectedInstalledGDALLibrary(currentSelectedInstalledGDALLibrary);
            }
            preferences.flush();

        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetInstance() {
        assertNotNull(GDALLoaderConfig.getInstance());
    }

    @Test
    public void testSetUseInstalledGDALLibrary() {
        assertNotNull(TEST_GDAL_LOADER_CONFIG);
        TEST_GDAL_LOADER_CONFIG.setUseInstalledGDALLibrary(USE_INSTALLED_GDAL_LIBRARY);
        assertEquals(USE_INSTALLED_GDAL_LIBRARY, fetchPreferences().getBoolean(PREFERENCE_KEY_USE_INSTALLED_GDAL, USE_INTERNAL_GDAL_LIBRARY));
        TEST_GDAL_LOADER_CONFIG.setUseInstalledGDALLibrary(USE_INTERNAL_GDAL_LIBRARY);
        assertEquals(USE_INTERNAL_GDAL_LIBRARY, fetchPreferences().getBoolean(PREFERENCE_KEY_USE_INSTALLED_GDAL, USE_INSTALLED_GDAL_LIBRARY));
    }

    @Test
    public void testSetSelectedInstalledGDALLibrary() {
        assertNotNull(TEST_GDAL_LOADER_CONFIG);
        TEST_GDAL_LOADER_CONFIG.setSelectedInstalledGDALLibrary(SELECTED_INSTALLED_GDAL_LIBRARY);
        assertEquals(SELECTED_INSTALLED_GDAL_LIBRARY, fetchPreferences().get(PREFERENCE_KEY_SELECTED_INSTALLED_GDAL, PREFERENCE_NONE_VALUE_SELECTED_INSTALLED_GDAL));
    }

    @Test
    public void testUseInstalledGDALLibrary() {
        assertNotNull(TEST_GDAL_LOADER_CONFIG);
        assertEquals(fetchPreferences().getBoolean(PREFERENCE_KEY_USE_INSTALLED_GDAL, USE_INSTALLED_GDAL_LIBRARY), TEST_GDAL_LOADER_CONFIG.useInstalledGDALLibrary());
        TEST_GDAL_LOADER_CONFIG.setUseInstalledGDALLibrary(USE_INSTALLED_GDAL_LIBRARY);
        assertEquals(USE_INSTALLED_GDAL_LIBRARY, TEST_GDAL_LOADER_CONFIG.useInstalledGDALLibrary());
    }

    @Test
    public void testGetSelectedInstalledGDALLibrary() {
        assertNotNull(TEST_GDAL_LOADER_CONFIG);
        assertEquals(fetchPreferences().get(PREFERENCE_KEY_SELECTED_INSTALLED_GDAL, PREFERENCE_NONE_VALUE_SELECTED_INSTALLED_GDAL), TEST_GDAL_LOADER_CONFIG.getSelectedInstalledGDALLibrary());
        TEST_GDAL_LOADER_CONFIG.setSelectedInstalledGDALLibrary(SELECTED_INSTALLED_GDAL_LIBRARY);
        assertEquals(SELECTED_INSTALLED_GDAL_LIBRARY, TEST_GDAL_LOADER_CONFIG.getSelectedInstalledGDALLibrary());
    }
}
