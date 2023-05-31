package org.esa.snap.dataio.gdal.gdal;

import org.esa.snap.dataio.gdal.GDALLoader;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GDALLibraryInstaller {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private GDALLibraryInstaller() {
    }

    public static void install() {
        if (!INSTALLED.getAndSet(true)) {
            GDALLoader.ensureGDALInitialised();
        }
    }
}
