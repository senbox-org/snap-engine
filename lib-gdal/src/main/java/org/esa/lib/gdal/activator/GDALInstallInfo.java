package org.esa.lib.gdal.activator;

import org.esa.snap.runtime.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * GDAL Install Info class which stores the location of installed GDAL distribution.
 *
 * @author Cosmin Cara
 */
public class GDALInstallInfo {
    public static final GDALInstallInfo INSTANCE = new GDALInstallInfo();

    private Path binLocation;
    private GDALWriterPlugInListener listener;

    private GDALInstallInfo(){
        //nothing to init
    }

    /**
     * Sets the location which contains GDAL binaries.
     *
     * @param binLocation the location which contains GDAL binaries
     */
    public synchronized void setLocations(Path binLocation) {
        this.binLocation = binLocation;
        try {
            Config config = Config.instance("s2tbx");
            config.load();
            Preferences preferences = config.preferences();
            preferences.put("gdal.apps.path", this.binLocation.toString());
            preferences.flush();
        } catch (BackingStoreException exception) {
            // ignore exception
        }
        fireListener();
    }

    /**
     * Sets the GDAL writer plugin listener
     *
     * @param listener the GDAL writer plugin listener
     */
    public synchronized void setListener(GDALWriterPlugInListener listener) {
        this.listener = listener;
        fireListener();
    }

    /**
     * Fires the stored GDAL writer plugin listener
     */
    private void fireListener() {
        if (this.listener != null && isPresent()) {
            this.listener.writeDriversSuccessfullyInstalled();
            this.listener = null;
        }
    }

    /**
     * Checks whether the location which contains GDAL binaries is stored and exists
     *
     * @return {@code true} if the location which contains GDAL binaries is stored and exists
     */
    public boolean isPresent() {
        return this.binLocation != null && Files.exists(this.binLocation);
    }
}


