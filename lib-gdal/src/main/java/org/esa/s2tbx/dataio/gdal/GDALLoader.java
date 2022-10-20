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
package org.esa.s2tbx.dataio.gdal;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.s2tbx.dataio.gdal.drivers.GDAL;
import org.esa.s2tbx.dataio.gdal.drivers.GDALConstConstants;
import org.esa.snap.core.datamodel.ProductData;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final Logger logger = Logger.getLogger(GDALLoader.class.getName());

    private boolean gdalIsInitialized = false;
    private boolean gdalInitialisationExecuted = false;
    private GDALVersion gdalVersion;
    private URLClassLoader gdalVersionLoader;

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
     * Initializes GDAL native libraries to be used by SNAP.
     */
    public void initGDAL() {
        if (!this.gdalInitialisationExecuted) {
            try {
                this.gdalVersion = GDALVersion.getGDALVersion();
                GDALDistributionInstaller.setupDistribution(this.gdalVersion);
                this.gdalVersionLoader = new URLClassLoader(new URL[]{this.gdalVersion.getJNILibraryFilePath().toUri().toURL()}, GDALLoader.class.getClassLoader());
                this.gdalIsInitialized = true;
                initDrivers();
                postGDALInit();
                Path gdalDistributionBinFolderPath = Paths.get(this.gdalVersion.getLocation());
                GDALInstallInfo.INSTANCE.setLocations(gdalDistributionBinFolderPath);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Failed to initialize GDAL native drivers. GDAL readers and writers were disabled." + t.getMessage());
                this.gdalIsInitialized = false;
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
        if (!this.gdalIsInitialized) {
            throw new IllegalStateException("GDAL Loader not initialized.");
        }
        return this.gdalVersionLoader;
    }

    /**
     * Gets the GDAL data type corresponding to the data type of a band.
     *
     * @param bandDataType the data type of the band to convert to the GDAL data type
     * @return the GDAL data type
     */
    public int getGDALDataType(int bandDataType) {
        if (!this.gdalIsInitialized) {
            throw new IllegalStateException("GDAL library not initialized");
        }
        Integer gdalResult = this.bandToGDALDataTypes.get(bandDataType);
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
        if (!this.gdalIsInitialized) {
            throw new IllegalStateException("GDAL library not initialized");
        }
        for (Map.Entry<Integer, Integer> entry : this.bandToGDALDataTypes.entrySet()) {
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
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Init the GDAL drivers on " + this.gdalVersion.getOsCategory().getOperatingSystemName() + ".");
        }
        GDAL.allRegister();// GDAL init drivers
    }
}
