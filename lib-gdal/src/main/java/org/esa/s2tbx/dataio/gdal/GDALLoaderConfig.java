package org.esa.s2tbx.dataio.gdal;

import org.esa.snap.runtime.Config;

import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * GDAL Loader Config class for setting which GDAL version is used by SNAP (internal distribution or installed distribution).
 *
 * @author Adrian Drăghici
 */
public class GDALLoaderConfig {

    private static final String INSTANCE_CONFIG = "s2tbx";
    private static final String PREFERENCE_KEY_USE_INSTALLED_GDAL = "s2tbx.dataio.gdal.installed";
    private static final String PREFERENCE_KEY_SELECTED_INSTALLED_GDAL = "s2tbx.dataio.gdal.installed.version";

    private static final Logger logger = Logger.getLogger(GDALLoaderConfig.class.getName());
    private static final GDALLoaderConfig instance = new GDALLoaderConfig();
    public static final String PREFERENCE_NONE_VALUE_SELECTED_INSTALLED_GDAL = "none";

    private boolean useInstalledGDALLibrary;
    private String selectedInstalledGDALLibrary;

    private GDALLoaderConfig() {
        this.useInstalledGDALLibrary = loadUseInstalledGDALLibrary();
        this.selectedInstalledGDALLibrary = loadSelectedInstalledGDALLibrary();
    }

    /**
     * Returns instance of this class.
     *
     * @return the instance of this class.
     */
    public static GDALLoaderConfig getInstance() {
        return instance;
    }

    /**
     * Fetches the setting for using installed distribution, from SNAP config files.
     *
     * @return the setting for using installed distribution
     */
    private boolean loadUseInstalledGDALLibrary() {
        final Preferences preferences = Config.instance(INSTANCE_CONFIG).load().preferences();
        return preferences.getBoolean(PREFERENCE_KEY_USE_INSTALLED_GDAL, true);
    }

    /**
     * Fetches the setting for selecting installed distribution, from SNAP config files.
     *
     * @return the setting for selecting installed distribution
     */
    private String loadSelectedInstalledGDALLibrary() {
        final Preferences preferences = Config.instance(INSTANCE_CONFIG).load().preferences();
        return preferences.get(PREFERENCE_KEY_SELECTED_INSTALLED_GDAL, PREFERENCE_NONE_VALUE_SELECTED_INSTALLED_GDAL);
    }

    /**
     * Sets the setting for using installed distribution, to SNAP config files.
     *
     * @param useInstalledGDALLibrary the setting for using installed distribution
     */
    public void setUseInstalledGDALLibrary(boolean useInstalledGDALLibrary) {
        this.useInstalledGDALLibrary = useInstalledGDALLibrary;
        final Preferences preferences = Config.instance(INSTANCE_CONFIG).load().preferences();
        preferences.putBoolean(PREFERENCE_KEY_USE_INSTALLED_GDAL, this.useInstalledGDALLibrary);
        try {
            preferences.flush();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    /**
     * Sets the setting for selecting installed distribution, to SNAP config files.
     *
     * @param selectedInstalledGDALLibrary the setting for selecting installed distribution
     */
    public void setSelectedInstalledGDALLibrary(String selectedInstalledGDALLibrary) {
        this.selectedInstalledGDALLibrary = selectedInstalledGDALLibrary;
        final Preferences preferences = Config.instance(INSTANCE_CONFIG).load().preferences();
        preferences.put(PREFERENCE_KEY_SELECTED_INSTALLED_GDAL, this.selectedInstalledGDALLibrary);
        try {
            preferences.flush();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    /**
     * Gets whether installed distribution is set to be used by SNAP.
     *
     * @return {@code true} if installed distribution is set to be used by SNAP
     */
    public boolean useInstalledGDALLibrary() {
        return this.useInstalledGDALLibrary;
    }

    /**
     * Gets the selected installed distribution to be used by SNAP.
     *
     * @return the selected installed distribution to be used by SNAP
     */
    public String getSelectedInstalledGDALLibrary() {
        return selectedInstalledGDALLibrary;
    }
}
