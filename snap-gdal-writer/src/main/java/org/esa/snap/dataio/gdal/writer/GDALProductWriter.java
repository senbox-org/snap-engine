/*
 *
 *  * Copyright (C) 2017 CS ROMANIA
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
package org.esa.snap.dataio.gdal.writer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.lib.gdal.activator.GDALDriverInfo;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.dataio.gdal.GDALLoader;
import org.esa.snap.dataio.gdal.drivers.*;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.JAI;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic writer for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GDALProductWriter extends AbstractProductWriter {

    private static final Logger logger = Logger.getLogger(GDALProductWriter.class.getName());
    private static final Map<Integer, Integer> gdalTypeMap;
    private static final Set<Integer> gdalUIntTypes;
    private static final Set<Integer> gdalIntTypes;
    private static final Set<Integer> gdalFloatTypes;
    private final GDALDriverInfo writerDriver;
    private Dataset gdalDataset;
    private String[] writeOptions;
    private int gdalDataType;

    private Driver gdalDriver;

    static {
        gdalTypeMap = new HashMap<>();
        gdalTypeMap.put(ProductData.TYPE_INT8, GDALConstConstants.gdtByte());
        gdalTypeMap.put(ProductData.TYPE_UINT8, GDALConstConstants.gdtByte());
        gdalTypeMap.put(ProductData.TYPE_INT16, GDALConstConstants.gdtInt16());
        gdalTypeMap.put(ProductData.TYPE_UINT16, GDALConstConstants.gdtUint16());
        gdalTypeMap.put(ProductData.TYPE_INT32, GDALConstConstants.gdtInt32());
        gdalTypeMap.put(ProductData.TYPE_UINT32, GDALConstConstants.gdtUint32());
        gdalTypeMap.put(ProductData.TYPE_FLOAT32, GDALConstConstants.gdtFloat32());
        gdalTypeMap.put(ProductData.TYPE_FLOAT64, GDALConstConstants.gdtFloat64());
        gdalUIntTypes = new HashSet<>() {{
           add(GDALConstConstants.gdtByte()); add(GDALConstConstants.gdtUint16()); add(GDALConstConstants.gdtUint32());
        }};
        gdalIntTypes = new HashSet<>() {{
           add(GDALConstConstants.gdtInt16()); add(GDALConstConstants.gdtInt32());
        }};
        gdalFloatTypes = new HashSet<>() {{
           add(GDALConstConstants.gdtFloat32()); add(GDALConstConstants.gdtFloat64());
        }};
    }

    public GDALProductWriter(ProductWriterPlugIn writerPlugIn, GDALDriverInfo writerDriver) {
        super(writerPlugIn);
        this.writerDriver = writerDriver;
    }

    private static Path getFileInput(Object input) {
        if (input instanceof String) {
            return Paths.get((String) input);
        } else if (input instanceof File) {
            return ((File) input).toPath();
        } else if (input instanceof Path) {
            return (Path) input;
        }
        return null;
    }

    private static void checkBufferSize(int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        int expectedBufferSize = sourceWidth * sourceHeight;
        int actualBufferSize = sourceBuffer.getNumElems();
        Guardian.assertEquals("sourceWidth * sourceHeight", actualBufferSize, expectedBufferSize);  /*I18N*/
    }

    private static void checkSourceRegionInsideBandRegion(int sourceWidth, long sourceBandWidth, int sourceHeight,
                                                          long sourceBandHeight, int sourceOffsetX, int sourceOffsetY) {

        Guardian.assertWithinRange("sourceWidth", sourceWidth, 1, sourceBandWidth);
        Guardian.assertWithinRange("sourceHeight", sourceHeight, 1, sourceBandHeight);
        Guardian.assertWithinRange("sourceOffsetX", sourceOffsetX, 0, sourceBandWidth - sourceWidth);
        Guardian.assertWithinRange("sourceOffsetY", sourceOffsetY, 0, sourceBandHeight - sourceHeight);
    }

    @Override
    protected void writeProductNodesImpl() {
        Object output = getOutput();

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Saving the product into the file '" + output.toString() + "' using the GDAL plugin writer '" + getWriterPlugIn().getClass().getName() + "'.");
        }

        Path outputFile = getFileInput(output);
        if (outputFile == null) {
            throw new IllegalArgumentException("The file '" + output.toString() + "' to save the product is invalid.");
        }

        Product sourceProduct = getSourceProduct();

        int imageWidth = sourceProduct.getSceneRasterWidth();
        int imageHeight = sourceProduct.getSceneRasterHeight();
        int bandCount = sourceProduct.getNumBands();

        Band sourceBand = sourceProduct.getBandAt(0);
        gdalDataType = GDALLoader.getInstance().getGDALDataType(sourceBand.getDataType());
        for (int i = 1; i < bandCount; i++) {
            sourceBand = sourceProduct.getBandAt(i);
            if (gdalDataType != GDALLoader.getInstance().getGDALDataType(sourceBand.getDataType())) {
                // If source bands have different data types, get the most permissive type
                gdalDataType = getMostPermissiveDataType(gdalDataType,
                                                         GDALLoader.getInstance().getGDALDataType(sourceBand.getDataType()));
                //break;
                //throw new IllegalArgumentException("GDAL Geotiff writer cannot write a product containing bands with different data types (the data type of band index " + i + " is " + sourceBand.getDataType() + ", different from band index 0).");
            }
        }

        String fileName = outputFile.toFile().getName();
        if (!StringUtils.endsWithIgnoreCase(fileName, this.writerDriver.getExtensionName())) {
            throw new IllegalArgumentException("The extension of the file name '" + fileName + "' is unknown.");
        }
        if (!this.writerDriver.canExportProduct(gdalDataType)) {
            String gdalDataTypeName = GDAL.getDataTypeName(gdalDataType);
            String message = MessageFormat.format("The GDAL driver ''{0}'' does not support the data type ''{1}'' to create a new product." +
                            " The available types are ''{2}''.",
                    this.writerDriver.getDriverDisplayName(), gdalDataTypeName, this.writerDriver.getCreationDataTypes());
            throw new IllegalArgumentException(message);
        }

        final boolean isCOG = this.writerDriver.getDriverName().contentEquals("COG");
        if (isCOG) {//when the writer attempts to write COG
            this.gdalDriver = GDAL.getDriverByName("MEM");//use 'GTiff' driver to write the temporary outputFile because the COG driver not allows creating datasets using 'driver.create()' as 'https://gdal.org/drivers/raster/cog.html' says
        } else {
            this.gdalDriver = GDAL.getDriverByName(this.writerDriver.getDriverName());
        }
        if (this.gdalDriver == null) {
            throw new NullPointerException("The GDAL driver '" + this.writerDriver.getDriverDisplayName() + "' (" + this.writerDriver.getDriverName() + ") used to write the product does not exist.");
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Using the GDAL driver '" + this.gdalDriver.getLongName() + "' (" + this.gdalDriver.getShortName() + ") to save the product.");
        }

        // COG driver doesn't support TILED
        String gdalWriteOptions = isCOG ? "" : Config.instance().preferences().get("snap.dataio.gdal.creationoptions", "TILED=YES");
        if (!gdalWriteOptions.contains("BLOCK")) {
            Dimension size = sourceProduct.getPreferredTileSize();
            if (size == null) {
                size = JAI.getDefaultTileSize() != null ? JAI.getDefaultTileSize() : new Dimension(1024, 1024);
            }
            gdalWriteOptions += isCOG
                    ? ";BLOCKSIZE=" + (size.width - size.width % 16)
                    : ";BLOCKXSIZE=" + (size.width - size.width % 16) + ";BLOCKYSIZE=" + (size.height - size.height % 16);
            if (!isCOG) {
                // COG driver doesn't support INTERLEAVE and PROFILE
                gdalWriteOptions += ";INTERLEAVE=BAND;PROFILE=GeoTIFF";
            }
        }
        if (gdalWriteOptions.contains("COMPRESS")) {
            if (!gdalWriteOptions.contains("PREDICTOR")) {
                if (gdalFloatTypes.contains(gdalDataType)) {
                    gdalWriteOptions += ";PREDICTOR=3";
                } else {
                    gdalWriteOptions += ";PREDICTOR=2";
                }
            }
        }
        this.writeOptions = StringUtils.stringToArray(gdalWriteOptions, ";");
        // By default, GDAL will use 5% of the installed memory for cache.
        // Writing will happen when flushing from memory, hence a long delay at the end of writing.
        final String strCacheMax = Config.instance().preferences().get("snap.dataio.gdal.cachemax", "268435456");
        final long cacheMax = Long.parseLong(strCacheMax);
        if (cacheMax < 0 || cacheMax > Integer.MAX_VALUE) {
            GDAL.setCacheMax(256 * 1024 * 1024);
        } else {
            GDAL.setCacheMax((int) cacheMax);
        }
        this.gdalDataset = this.gdalDriver.create(outputFile.toString(), imageWidth, imageHeight, bandCount, gdalDataType, writeOptions);
        if (this.gdalDataset == null) {
            throw new NullPointerException("Failed creating the file to export the product for driver '" + this.gdalDriver.getLongName() + "'.");
        }

        GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
        if (geoCoding == null) {
            this.gdalDataset.setProjection("");
        } else if (geoCoding.getImageToMapTransform() instanceof AffineTransform2D) {
            this.gdalDataset.setProjection(geoCoding.getMapCRS().toWKT());
            AffineTransform2D transform = (AffineTransform2D) geoCoding.getImageToMapTransform();
            double[] gdalGeoTransform = new double[6];
            gdalGeoTransform[0] = transform.getTranslateX();
            gdalGeoTransform[1] = transform.getScaleX();
            gdalGeoTransform[2] = transform.getShearX();
            gdalGeoTransform[3] = transform.getTranslateY();
            gdalGeoTransform[4] = transform.getShearY();
            gdalGeoTransform[5] = transform.getScaleY();

            this.gdalDataset.setGeoTransform(gdalGeoTransform);
        } else if (geoCoding instanceof TiePointGeoCoding) {
            writeGCPGeoCodingFromTiePointGridNodes(gdalDataset, geoCoding, sourceProduct);
        }
    }

    /**
     * SIITBX-435: GDAL doesn't support concurrent writing to the same file (or Dataset), so this must be synchronized.
     * Make this method synchronized for fixing artifacts noticed on products created with GDAL Export (GeoTiff) from SNAP menu, due to fact that GDAL Dataset does not allow concurrent write.
     */
    @Override
    public synchronized void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) {
        Guardian.assertNotNull("sourceBand", sourceBand);
        Guardian.assertNotNull("sourceBuffer", sourceBuffer);
        checkBufferSize(sourceWidth, sourceHeight, sourceBuffer);

        long sourceBandWidth = sourceBand.getRasterWidth();
        long sourceBandHeight = sourceBand.getRasterHeight();
        checkSourceRegionInsideBandRegion(sourceWidth, sourceBandWidth, sourceHeight, sourceBandHeight, sourceOffsetX, sourceOffsetY);

        Product sourceProduct = getSourceProduct();
        int bandIndex = sourceProduct.getBandIndex(sourceBand.getName());
        try (org.esa.snap.dataio.gdal.drivers.Band gdalBand = this.gdalDataset.getRasterBand(bandIndex + 1)) {
            if (gdalBand == null) {
                throw new NullPointerException("Failed creating the band with index " + bandIndex + " to export the product for driver '" + this.gdalDriver.getLongName() + "'.");
            }
            int result;
            pm.beginTask("Writing band '" + sourceBand.getName() + "'...", sourceHeight);
            gdalBand.setScale(Double.compare(sourceBand.getScalingFactor(), 0.0) == 0 ? 1.0 : sourceBand.getScalingFactor());
            gdalBand.setOffset(sourceBand.getScalingOffset());
            if (sourceBand.isNoDataValueSet()) {
            	gdalBand.setNoDataValue(sourceBand.getNoDataValue());
            } else {
            	gdalBand.setNoDataValue(Float.NaN);
            }
            gdalBand.setUnitType(sourceBand.getUnit());
            gdalBand.setDescription(sourceBand.getName());
            final int dataType = sourceBand.getDataType();
            final int gdalDataType = gdalTypeMap.get(dataType);
            final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(sourceWidth * sourceHeight * (GDAL.getDataTypeSize(gdalDataType) >> 3));
            byteBuffer.order(ByteOrder.nativeOrder());
            try {
                switch (dataType) {
                    case ProductData.TYPE_UINT8:
                    case ProductData.TYPE_INT8:
                        byteBuffer.put((byte[]) sourceBuffer.getElems());
                        break;
                    case ProductData.TYPE_INT16:
                    case ProductData.TYPE_UINT16:
                        byteBuffer.asShortBuffer().put((short[]) sourceBuffer.getElems());
                        break;
                    case ProductData.TYPE_INT32:
                    case ProductData.TYPE_UINT32:
                        byteBuffer.asIntBuffer().put((int[]) sourceBuffer.getElems());
                        break;
                    case ProductData.TYPE_FLOAT32:
                        byteBuffer.asFloatBuffer().put((float[]) sourceBuffer.getElems());
                        break;
                    case ProductData.TYPE_FLOAT64:
                        byteBuffer.asDoubleBuffer().put((double[]) sourceBuffer.getElems());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown GDAL data type " + dataType + ".");
                }
                result = gdalBand.writeRasterDirect(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, gdalDataType, byteBuffer);
                byteBuffer.clear();
                if (result != GDALConst.ceNone()) {
                    throw new IllegalArgumentException("Failed to write the data for band name '" + sourceBand.getName() + "' and driver '" + this.gdalDriver.getLongName() + "'.");
                }
                pm.worked(1);
            } finally {
                pm.done();
            }
        } catch (Throwable e) {
            throw new NullPointerException("Failed creating the band with index " + bandIndex + " to export the product for driver '" + this.gdalDriver.getLongName() + "'. Reason: " + e.getMessage());
        }
    }

    @Override
    public void flush() {
        //nothing to do
    }

    @Override
    public void close() {
        RuntimeException failure = null;

        try {
            if (this.gdalDataset != null) {
                if (this.writerDriver.getDriverName().contains("COG")) {
                    try (Driver cogDriver = GDAL.getDriverByName(this.writerDriver.getDriverName())) {//use the COG driver
                        String outputFile = getFileInput(getOutput()).toString();//use the real output file name
                        Dataset tempDataset = this.gdalDataset;
                        //create the final output dataset file (the COG product) from temporary dataset file without options for write
                        this.gdalDataset = cogDriver.createCopy(outputFile, this.gdalDataset, Objects.requireNonNullElseGet(writeOptions, () -> new String[0]));//create the final output dataset file (the COG product) from temporary dataset file with options for write
                        tempDataset.delete();//close the temporary dataset file
                    } catch (IOException e) {
                        failure = new RuntimeException(e);
                    }
                }
            }
        } finally {
            if (this.gdalDataset != null) {
                try {
                    this.gdalDataset.delete();
                }catch (Exception e) {
                    if (failure == null) {
                        failure = new RuntimeException(e);
                    }else {
                        failure.addSuppressed(e);
                    }
                } finally {
                    this.gdalDataset = null;
                }
            }
            if (gdalDriver!= null){
                try {
                    gdalDriver.close();
                } catch (IOException e) {
                    if (failure == null) {
                        failure = new RuntimeException(e);
                    }else {
                        failure.addSuppressed(e);
                    }
                } finally {
                    this.gdalDriver = null;
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public void deleteOutput() {
        //nothing to do
    }

    private int getMostPermissiveDataType(int referenceType, int currentType) {
        if (gdalFloatTypes.contains(currentType)) {
            if (gdalFloatTypes.contains(referenceType)) {
                return Math.max(referenceType, currentType);
            } else {
                return currentType;
            }
        } else {
            if (gdalFloatTypes.contains(referenceType)) {
                return referenceType;
            } else {
                int masked = currentType & referenceType;
                return masked <= 4 ? masked << 1 : masked;
            }
        }
    }

    /**
     *  Writes GDAL GCP-based geocoding derived directly from the product's {@link TiePointGeoCoding} support grids.
     *
     * @param dataset the GDAL dataset being written
     * @param geoCoding the source product geocoding (instance of {@link TiePointGeoCoding})
     * @param product  the source product containing tie-point grids
     */
    private static void writeGCPGeoCodingFromTiePointGridNodes(Dataset dataset,
                                     GeoCoding geoCoding, Product product) {

        int sceneW = product.getSceneRasterWidth();
        int sceneH = product.getSceneRasterHeight();

        final int nbGCPsX = 10;
        final int nbGCPsY = 10;

        int stepX =  (int)Math.ceil((double)sceneW/nbGCPsX);  // grid across X
        int stepY = (int)Math.ceil((double)sceneH/nbGCPsY);  // grid across Y

        final java.util.LinkedHashMap<Long, GCP> uniqGCPs = new java.util.LinkedHashMap<>();

        // ---- scene corners + edges ----
        //corners
        addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, 0, 0);
        addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, sceneW - 1, 0);
        addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, 0, sceneH - 1);
        addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, sceneW - 1, sceneH - 1);

        // Sample full border
        for (int x = 0; x < sceneW; x += stepX) {
            addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, x, 0);
            addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, x, sceneH - 1);
        }
        for (int y = 0; y < sceneH; y += stepY) {
            addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, 0, y);
            addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, sceneW - 1, y);
        }

        //Sample grid center
        int centerY = Math.round((sceneH-1)/2);
        int centerX = Math.round((sceneW-1)/2);
        addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, centerX, centerY);
        addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, centerX, sceneH - centerY);
        addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, sceneW - centerX, centerY);
        addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, sceneW - centerX, sceneH - centerY);

        // Sample interior grid
        for (int y = 0; y < sceneH; y += stepY) {
        for (int x = 0; x < sceneW; x += stepX) {
                addScenePixelGcpFromGeoCoding(uniqGCPs, geoCoding, x, y);
            }
        }

        // If too few valid points, don't write broken geocoding
        final ArrayList<GCP> gcpList = new ArrayList<>(uniqGCPs.values());
        try {
            if (gcpList.size() < 4) {
                dataset.setProjection("");
                return;
            }
            String wkt = extractGeoCodingWkt(geoCoding);
            dataset.setGCPs(gcpList, wkt);
            dataset.setProjection(wkt);
        }finally {
            closeGcps(gcpList);
        }
    }

    /**
     * Adds a GDAL {@link GCP} corresponding to a scene pixel position using the provided {@link GeoCoding}.
     *
     * @param gcpMap      map of unique GCPs keyed by quantized pixel position
     * @param geoCoding the geocoding used to compute geographic coordinates
     * @param x         pixel column index
     * @param y         pixel row index
     */
    private static void addScenePixelGcpFromGeoCoding(java.util.Map<Long, GCP> gcpMap, GeoCoding geoCoding, int x, int y) {
        final double px = x + 0.5;
        final double py = y + 0.5;

        GeoPos gp = geoCoding.getGeoPos(new PixelPos((float) px, (float) py), null);

        if (gp == null || !gp.isValid()) return;

        final double lat = gp.getLat();
        final double lon = gp.getLon();

        if (Double.isNaN(lat) || Double.isNaN(lon)) return;

        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return;

        final long key = createQuantizedPixelKey(px, py);

        if (!gcpMap.containsKey(key)) {
            gcpMap.put(key, GCP.create(px, py, lon, lat, 0.0));
        }
    }

    /**
     * Closes the provided GDAL GCP wrappers.
     *
     * @param gcps the GCP wrappers to close; {@code null} entries are ignored
     * @throws RuntimeException if closing any GCP fails
     */
    private static void closeGcps(Collection<GCP> gcps) {
        RuntimeException failure = null;

        for (GCP gcp : gcps) {
            if (gcp != null) {
                try {
                    gcp.close();
                } catch (IOException e) {
                    if (failure == null) {
                        failure = new RuntimeException("Failed to close GDAL GCP.", e);
                    }
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Creates a quantized 64-bit key for a pixel position.
     *
     * <p>Coordinates are quantized to 1/1000 pixel precision to suppress minor floating-point
     * differences, then packed (X in upper 32 bits, Y in lower 32 bits). Used for unique pixel
     * identification when generating GCP samples.</p>
     *
     * @param px pixel X coordinate
     * @param py pixel Y coordinate
     * @return packed key representing the quantized pixel position
     */
    private static long createQuantizedPixelKey(double px, double py) {
        long x = Math.round(px * 1000.0);
        long y = Math.round(py * 1000.0);

        return (x << 32) | (y & 0xffffffffL);
    }

    /**
     * Extract WKT from GeoCoding
     * @param geoCoding
     * @return
     */
    private static String extractGeoCodingWkt(GeoCoding geoCoding){
        String wkt = null;
        try {
            final CoordinateReferenceSystem crs = geoCoding.getMapCRS();
            if (crs != null) wkt = crs.toWKT();
        } catch (Exception ignore) {}

        if (wkt == null) {
            try {
                wkt = CRS.decode("EPSG:4326", true).toWKT();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create WKT for EPSG:4326", e);
            }
        }
        return wkt;
    }
}
