package org.esa.lib.gdal.activator;

import org.esa.s2tbx.dataio.gdal.drivers.GDAL;

/**
 * GDAL Driver Info class containing information about a GDAL driver.
 *
 * @author Jean Coravu
 */
public class GDALDriverInfo {
    private final String extensionName;
    private final String driverName;
    private final String driverDisplayName;
    private final String creationDataTypes;

    /**
     * Creates new instance for this class.
     *
     * @param extensionName     The driver extension name
     * @param driverName        The driver name
     * @param driverDisplayName The driver display name
     * @param creationDataTypes The data types used to create a band (ex: Byte Int16 UInt16 Int32 UInt32 Float32 Float64)
     */
    public GDALDriverInfo(String extensionName, String driverName, String driverDisplayName, String creationDataTypes) {
        this.extensionName = extensionName;
        this.driverName = driverName;
        this.driverDisplayName = driverDisplayName;
        this.creationDataTypes = creationDataTypes;
    }

    /**
     * Gets the driver display name
     *
     * @return the driver display name
     */
    public String getDriverDisplayName() {
        return this.driverDisplayName;
    }

    /**
     * Gets the driver extension name
     *
     * @return the driver extension name
     */
    public String getExtensionName() {
        return this.extensionName;
    }

    /**
     * Gets the driver name
     *
     * @return the driver name
     */
    public String getDriverName() {
        return this.driverName;
    }

    /**
     * Gets the data types used to create a band
     *
     * @return the data types used to create a band
     */
    public String getCreationDataTypes() {
        return this.creationDataTypes;
    }

    /**
     * Checks whether the available creation data types of the driver contains the GDAL data type.
     *
     * @param gdalDataType the GDAl data type to check
     * @return {@code true} if the driver can export the product containing the specified data type; {@code false} otherwise
     */
    public boolean canExportProduct(int gdalDataType) {
        boolean allowedDataType = true;
        String gdalDataTypeName = GDAL.getDataTypeName(gdalDataType);
        if (this.creationDataTypes != null) {
            allowedDataType = this.creationDataTypes.contains(gdalDataTypeName);
        }
        return allowedDataType;
    }

    /**
     * Gets the writer plugin format name
     *
     * @return the writer plugin format name
     */
    public final String getWriterPluginFormatName() {
        return "GDAL-" + this.driverName + "-WRITER";
    }
}
