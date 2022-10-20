package org.esa.lib.gdal.activator;

import org.esa.s2tbx.dataio.gdal.GDALLoader;
import org.esa.snap.runtime.Activator;

/**
 * GDAL Plugin Activator class to install the GDAL library and add the GDAL writer plugin.
 *
 * @author Jean Coravu
 */
public class GDALPlugInActivator implements Activator {

    public GDALPlugInActivator() {
        //nothing to init
    }

    /**
     * Starts the plugin activator
     */
    @Override
    public void start() {
        GDALLoader.getInstance().initGDAL();
    }

    /**
     * Stops the plugin activator
     */
    @Override
    public void stop() {
        //nothing to do
    }
}
