/*
 *
 *  * Copyright (C) 2019 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.dataio.gdal;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.gdal.drivers.GDAL;
import org.esa.snap.dataio.gdal.drivers.GDALConstConstants;
import org.esa.snap.engine_utilities.file.FileHelper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GDAL Loader class for loading GDAL native libraries from distribution.
 *
 * @author Jean Coravu
 * @author Adrian Drăghici
 */
public final class GDALLoader {

    private static final GDALLoader INSTANCE = new GDALLoader();

    private static final String GDAL_NATIVE_LIBRARY_LOADER_CLASS_NAME = "org.esa.snap.NativeLibraryLoader";

    private static final Logger logger = Logger.getLogger(GDALLoader.class.getName());

    private boolean gdalInitialisationExecuted = false;
    private GDALVersion gdalVersion;
    private GDALLoaderClassLoader gdalVersionLoader;

    private Map<Integer, Integer> bandToGDALDataTypes;

    private GDALLoader() {

    }

    /**
     * Returns instance of this class.
     *
     * @return the instance of this class.
     */
    public static GDALLoader getInstance() {
        return INSTANCE;
    }

    /**
     * Ensures GDAL library was initialised
     */
    public static void ensureGDALInitialised(){
        if (!GDALInstallInfo.INSTANCE.isPresent()) {
            getInstance().initGDAL();
            if (!GDALInstallInfo.INSTANCE.isPresent()) {
                throw new IllegalStateException("GDAL NOT initialised! Check log for details.");
            }
        }
    }

    /**
     * Initializes GDAL native libraries to be used by SNAP.
     */
    private void initGDAL() {
        if (!this.gdalInitialisationExecuted) {
            try {
                this.gdalVersion = GDALVersion.getGDALVersion();
                GDALDistributionInstaller.setupDistribution(this.gdalVersion);
                this.gdalVersionLoader = new GDALLoaderClassLoader(new URL[]{this.gdalVersion.getJNILibraryFilePath().toUri().toURL(), GDALVersion.getLoaderLibraryFilePath().toUri().toURL()}, this.gdalVersion.getGDALNativeLibraryFilesPath());
                loadGDALNativeLibrary();
                GDALInstallInfo.INSTANCE.setLocations(this.gdalVersion.getLocationPath());
                initDrivers();
                GDALDistributionInstaller.setupProj(gdalVersion);
                postGDALInit();
                logger.log(Level.FINE, () -> "GDAL initialised SUCCESSFULLY!");
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Failed to initialize GDAL native drivers. GDAL readers and writers were disabled." + t.getMessage());
            }
            this.gdalInitialisationExecuted = true;
        }
    }

    /**
     * Saves internal mappings between GDAL data types and bands data types.
     */
    private void postGDALInit() {
        this.bandToGDALDataTypes = new HashMap<>();
        this.bandToGDALDataTypes.put(ProductData.TYPE_INT8, GDALConstConstants.gdtByte());//add missing data type int8 (SIITBX-435)
        this.bandToGDALDataTypes.put(ProductData.TYPE_UINT8, GDALConstConstants.gdtByte());
        this.bandToGDALDataTypes.put(ProductData.TYPE_INT16, GDALConstConstants.gdtInt16());//add missing data type int16 (SIITBX-435)
        this.bandToGDALDataTypes.put(ProductData.TYPE_UINT16, GDALConstConstants.gdtUint16());//correct data type for uint16
        this.bandToGDALDataTypes.put(ProductData.TYPE_INT32, GDALConstConstants.gdtInt32());
        this.bandToGDALDataTypes.put(ProductData.TYPE_UINT32, GDALConstConstants.gdtUint32());
        this.bandToGDALDataTypes.put(ProductData.TYPE_FLOAT32, GDALConstConstants.gdtFloat32());
        this.bandToGDALDataTypes.put(ProductData.TYPE_FLOAT64, GDALConstConstants.gdtFloat64());
    }

    /**
     * Gets the GDAL JNI URL class loader for loading JNI drivers of current version native libraries.
     *
     * @return the GDAL JNI URL class loader for loading JNI drivers of current version native libraries
     */
    public URLClassLoader getGDALVersionLoader() {
        ensureGDALInitialised();
        return this.gdalVersionLoader;
    }

    /**
     * Gets the GDAL data type corresponding to the data type of a band.
     *
     * @param bandDataType the data type of the band to convert to the GDAL data type
     * @return the GDAL data type
     */
    public int getGDALDataType(int bandDataType) {
        ensureGDALInitialised();
        final Integer gdalResult = this.bandToGDALDataTypes.get(bandDataType);
        if (gdalResult != null) {
            return gdalResult;
        }
        throw new IllegalArgumentException("Unknown band data type " + bandDataType + ".");
    }

    /**
     * Get the data type of the band corresponding to the GDAL data type.
     *
     * @param gdalDataType the GDAL data type to convert to the data type of the band
     * @return the data type of the band
     */
    public int getBandDataType(int gdalDataType) {
        ensureGDALInitialised();
        for (final Map.Entry<Integer, Integer> entry : this.bandToGDALDataTypes.entrySet()) {
            if (entry.getValue() == gdalDataType) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unknown band data type " + gdalDataType + ".");
    }

    /**
     * Gets the GDAL version of loaded driver.
     *
     * @return the GDAL version of loaded driver
     */
    public GDALVersion getGdalVersion() {
        return gdalVersion;
    }

    /**
     * Init the drivers if the GDAL library is installed.
     */
    private void initDrivers() {
        logger.log(Level.FINE, "Init the GDAL drivers on " + OSCategory.getOSCategory().getOperatingSystemName() + ".");
        GDAL.allRegister();// GDAL init drivers
    }

    /**
     * Loads the GDAL native library used for access GDAL native methods.
     */
    private void loadGDALNativeLibrary() {
        try {
            copyLoaderLibrary();
            final Method loaderMethod = this.gdalVersionLoader.loadClass(GDAL_NATIVE_LIBRARY_LOADER_CLASS_NAME).getMethod("loadNativeLibrary", Path.class);
            for (Path nativeLibraryFilePath : this.gdalVersion.getGDALNativeLibraryFilesPath()) {
                loaderMethod.invoke(null, nativeLibraryFilePath);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Load the GDAL native library '" + nativeLibraryFilePath.getFileName() + "'.");
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Copies the loader library used for load GDAL native library.
     *
     * @throws IOException When IO error occurs
     */
    private static void copyLoaderLibrary() throws IOException {
        final Path loaderFilePath = GDALVersion.getLoaderLibraryFilePath();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Copy the loader library file.");
        }

        final URL libraryFileURLFromSources = GDALVersion.getLoaderFilePathFromSources();
        if (libraryFileURLFromSources != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("The loader library file path on the local disk is '" + loaderFilePath + "' and the library file name from sources is '" + libraryFileURLFromSources + "'.");
            }

            FileHelper.copyFile(libraryFileURLFromSources, loaderFilePath);
        } else {
            throw new IllegalStateException("Unable to get loader libraryFileURLFromSources");
        }
    }

}
