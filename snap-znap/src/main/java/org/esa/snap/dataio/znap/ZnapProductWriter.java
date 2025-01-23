/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.dataio.znap;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.multilevel.MultiLevelImage;
import com.bc.zarr.ArrayParams;
import com.bc.zarr.Compressor;
import com.bc.zarr.CompressorFactory;
import com.bc.zarr.DataType;
import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;
import com.bc.zarr.storage.FileSystemStore;
import com.bc.zarr.storage.Store;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geometry.VectorDataNodeIO;
import org.esa.snap.core.dataio.geometry.WriterBasedVectorDataNodeWriter;
import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.JsonLanguageSupport;
import org.esa.snap.core.dataio.persistence.Persistence;
import org.esa.snap.core.dataio.persistence.PersistenceEncoder;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.ISO8601Converter;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import ucar.ma2.InvalidRangeException;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import static org.esa.snap.core.util.StringUtils.isNotNullAndNotEmpty;
import static org.esa.snap.core.util.SystemUtils.LOG;
import static org.esa.snap.dataio.znap.CFConstantsAndUtils.FLAG_MASKS;
import static org.esa.snap.dataio.znap.CFConstantsAndUtils.FLAG_MEANINGS;
import static org.esa.snap.dataio.znap.CFConstantsAndUtils.FLAG_VALUES;
import static org.esa.snap.dataio.znap.CFConstantsAndUtils.tryFindUnitString;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_BINARY_FORMAT;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_GEOCODING;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_OFFSET_X;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_OFFSET_Y;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_ORIGINAL_RASTER_DATA_NODE_ORDER;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_PRODUCT_DESC;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_PRODUCT_NAME;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_PRODUCT_SCENE_HEIGHT;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_PRODUCT_SCENE_WIDTH;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_PRODUCT_TYPE;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_SUBSAMPLING_X;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ATT_NAME_SUBSAMPLING_Y;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.BANDWIDTH;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.BANDWIDTH_UNIT;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_PALETTE_AUTO_DISTRIBUTE;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_PALETTE_DISCRETE;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_PALETTE_NUM_COLORS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_PALETTE_POINTS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_RGBA;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.DATASET_AUTO_GROUPING;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.DISCONTINUITY;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.FLAG_DESCRIPTIONS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.HISTOGRAM_MATCHING;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.IDX_HEIGHT;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.IDX_WIDTH;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.IDX_X;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.IDX_Y;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.IMAGE_INFO;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.KEY_SNAP_PRODUCT_METADATA_JSON;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.KEY_SNAP_VECTOR_DATA_SUBGROUP;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.LABEL;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.LOG_10_SCALED;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NAME_FILTER_BANDS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NAME_MASKS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NAME_SNAP_SUBGROUP;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NAME_VECTOR_DATA_SUBGROUP;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NO_DATA_COLOR_RGBA;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NO_DATA_VALUE_USED;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.QUICKLOOK_BAND_NAME;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.SAMPLE;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.SOLAR_FLUX;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.SPECTRAL_BAND_INDEX;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.STATISTICS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.UNCERTAINTY_BAND_NAME;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.UNCERTAINTY_VISUALISATION_MODE;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.UNIT_EXTENSION;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.VALID_PIXEL_EXPRESSION;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.VIRTUAL_BAND_EXPRESSION;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.WAVELENGTH;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.WAVELENGTH_UNIT;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ZNAP_CONTAINER_EXTENSION;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.ZNAP_ZIP_CONTAINER_EXTENSION;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.convertToPath;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.getSnapDataType;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.writeProductMetadata;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.DEFAULT_COMPRESSION_LEVEL;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.DEFAULT_COMPRESSOR_ID;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.DEFAULT_USE_ZIP_ARCHIVE;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_BINARY_FORMAT;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_COMPRESSION_LEVEL;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_COMPRESSOR_ID;
import static ucar.nc2.constants.ACDD.TIME_END;
import static ucar.nc2.constants.ACDD.TIME_START;
import static ucar.nc2.constants.CDM.FILL_VALUE;
import static ucar.nc2.constants.CDM.UNSIGNED;
import static ucar.nc2.constants.CF.ADD_OFFSET;
import static ucar.nc2.constants.CF.COORDINATES;
import static ucar.nc2.constants.CF.LONG_NAME;
import static ucar.nc2.constants.CF.SCALE_FACTOR;
import static ucar.nc2.constants.CF.UNITS;

public class ZnapProductWriter extends AbstractProductWriter {

    private final HashMap<Band, BinaryWriter> zarrWriters = new HashMap<>();
    private final Compressor compressor;
    private final ProductWriterPlugIn binaryWriterPlugIn;

    private final Persistence persistence = new Persistence();
    private final JsonLanguageSupport languageSupport = new JsonLanguageSupport();

    private ZarrGroup zarrGroup;
    private Path outputRoot;
    private Store zarrStore;
    private DimensionNameGenerator dimensionNameGenerator;
    private Properties testProperties;

    public ZnapProductWriter(final ZnapProductWriterPlugIn productWriterPlugIn) {
        super(productWriterPlugIn);
        compressor = CompressorFactory.create(getCompressorId(), "level", getCompressionLevel());
        binaryWriterPlugIn = getBinaryWriterPlugin(productWriterPlugIn);
    }

    /**
     * This constructor exists for test purposes only.
     * By specifying the testProperties, the various snap.preferences used in the writer can be simulated.
     * @param productWriterPlugIn
     */
    ZnapProductWriter(final ZnapProductWriterPlugIn productWriterPlugIn, Properties testProperties) {
        super(productWriterPlugIn);
        this.testProperties = testProperties;
        compressor = CompressorFactory.create(getCompressorId(), "level", getCompressionLevel());
        binaryWriterPlugIn = getBinaryWriterPlugin(productWriterPlugIn);
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        final BinaryWriter binaryWriter = zarrWriters.get(sourceBand);
        final int[] to = {sourceOffsetY, sourceOffsetX}; // common data model manner { y, x }
        final int[] shape = {sourceHeight, sourceWidth};  // common data model manner { y, x }
        try {
            binaryWriter.write(ensureNotLogarithmicData(sourceBand, sourceBuffer), shape, to);
        } catch (InvalidRangeException e) {
            throw new IOException("Invalid range while writing raster '" + sourceBand.getName() + "'", e);
        }
        pm.done();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        for (BinaryWriter value : zarrWriters.values()) {
            value.dispose();
        }
        zarrWriters.clear();
        if (zarrStore != null) {
            try {
                zarrStore.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Unable to close the zarr product writer store.", e);
            }
        }
    }

    @Override
    public void deleteOutput() {
        // todo implement the deletion of the output
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        dimensionNameGenerator = new DimensionNameGenerator();
        outputRoot = convertToPath(getOutput());
        final Path parentDir = Objects.requireNonNull(outputRoot).getParent();
        Files.createDirectories(parentDir);
        final Product product = getSourceProduct();
        dimensionNameGenerator.getDimensionNameFor("x", product.getSceneRasterWidth());
        dimensionNameGenerator.getDimensionNameFor("y", product.getSceneRasterHeight());
        final boolean useZipArchive = isUseZipArchive();
        final String stripedFilename = getFilenameWithoutExtension();
        if (useZipArchive) {
            outputRoot = parentDir.resolve(stripedFilename + ZNAP_ZIP_CONTAINER_EXTENSION);
            zarrStore = new ZnapZipStore(outputRoot);
//            zarrStore = new ZipStore(outputRoot);
        } else {
            outputRoot = parentDir.resolve(stripedFilename + ZNAP_CONTAINER_EXTENSION);
            zarrStore = new FileSystemStore(outputRoot);
        }
        zarrGroup = ZarrGroup.create(zarrStore);
        final ZarrGroup snapSubGroup = zarrGroup.createSubGroup(NAME_SNAP_SUBGROUP);

        try (
                OutputStream productMetadataStream = zarrStore.getOutputStream(KEY_SNAP_PRODUCT_METADATA_JSON);
                OutputStreamWriter out = new OutputStreamWriter(productMetadataStream)
        ) {
            writeProductMetadata(out, product.getMetadataRoot().getElements());
        }

        snapSubGroup.createSubGroup(NAME_VECTOR_DATA_SUBGROUP);
        writeVectorData();
        for (TiePointGrid tiePointGrid : product.getTiePointGrids()) {
            writeTiePointGrid(tiePointGrid);
        }
        for (Band band : product.getBands()) {
            if (band instanceof FilterBand) {
                continue;
            }
            initializeZarrArrayForBand(band);
            if (shouldWrite(band)) {
                initializeZarrBandWriter(band);
            }
        }
        zarrGroup.writeAttributes(collectProductAttributes());
    }

    private String getFilenameWithoutExtension() {
        String fileName = outputRoot.getFileName().toString();
        fileName = fileName.toLowerCase().endsWith(".zip") ? fileName.substring(0, fileName.length() - 4) : fileName;
        fileName = fileName.toLowerCase().endsWith(".znap") ? fileName.substring(0, fileName.length() - 5) : fileName;
        return fileName;
    }

    private void writeVectorData() throws IOException {
        Product product = getSourceProduct();
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
        zarrStore.delete(KEY_SNAP_VECTOR_DATA_SUBGROUP);

        if (vectorDataGroup.getNodeCount() > 0) {
            for (int i = 0; i < vectorDataGroup.getNodeCount(); i++) {
                VectorDataNode vectorDataNode = vectorDataGroup.get(i);
                writeVectorData(vectorDataNode);
            }
        }
    }

    void writeVectorData(VectorDataNode vectorDataNode) {
        try {
            WriterBasedVectorDataNodeWriter vectorDataNodeWriter = new WriterBasedVectorDataNodeWriter();
            final Path filePath = Paths.get(KEY_SNAP_VECTOR_DATA_SUBGROUP).resolve(vectorDataNode.getName() + VectorDataNodeIO.FILENAME_EXTENSION);
            try (
                    final OutputStream os = zarrStore.getOutputStream(filePath.toString());
                    final Writer writer = new OutputStreamWriter(os)
            ) {
                vectorDataNodeWriter.write(vectorDataNode, writer);
            }
        } catch (IOException e) {
            SystemUtils.LOG.throwing("ZarrProductWriter", "writeVectorData", e);
        }
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        if (node instanceof VirtualBand
            || node instanceof FilterBand) {
            return false;
        }
        return super.shouldWrite(node);
    }

    Map<String, Object> collectProductAttributes() throws ProductIOException {
        final Map<String, Object> attributes = new LinkedHashMap<>();
        collectVersion(attributes);
        collectGeneralProductAttrs(attributes);
        collectProductGeoCodingAttrs(attributes);
        // flag attributes are collected per Band (Flag or Index Band). see collectSampleCodingAttributes()
        collectMaskAttrs(attributes);
        collectFilterBandAttrs(attributes);
        collectOriginalRasterDataNodeOrder(attributes);
        return attributes;
    }

    private void collectVersion(Map<String, Object> attributes) {
        attributes.put("znap_format", "0.1");
    }

    private void collectMaskAttrs(Map<String, Object> attributes) {
        final ProductNodeGroup<Mask> maskGroup = getSourceProduct().getMaskGroup();
        final Mask[] masks = maskGroup.toArray(new Mask[0]);
        final ArrayList<Map<String, Object>> encodedMasks = new ArrayList<>();
        for (Mask mask : masks) {
            final PersistenceEncoder<Mask> encoder = persistence.getEncoder(mask);
            if (encoder == null) {
                continue;
            }
            final Item encoded = encoder.encode(mask);
            final Map<String, Object> maskEncoded = languageSupport.translateToLanguageObject(encoded);
            encodedMasks.add(maskEncoded);
        }
        if (encodedMasks.size() > 0) {
            attributes.put(NAME_MASKS, encodedMasks);
        }
    }

    private void collectOriginalRasterDataNodeOrder(Map<String, Object> attributes) {
        final List<RasterDataNode> rasterDataNodes = getSourceProduct().getRasterDataNodes();
        final List<String> names = new ArrayList<>();
        for (RasterDataNode rasterDataNode : rasterDataNodes) {
            names.add(rasterDataNode.getName());
        }
        attributes.put(ATT_NAME_ORIGINAL_RASTER_DATA_NODE_ORDER, names);
    }

    private void collectFilterBandAttrs(Map<String, Object> attributes) {
        final Band[] bands = getSourceProduct().getBands();
        final List<Map<String, Object>> filterBands = new ArrayList<>();
        for (Band band : bands) {
            if (!(band instanceof FilterBand)) {
                continue;
            }
            final PersistenceEncoder<Object> encoder = persistence.getEncoder(band);
            if (encoder == null) {
                LOG.warning("Unable to find a PersistenceEncoder for FilterBand '" + band.getName() + "'.");
                continue;
            }
            final Item item = encoder.encode(band);
            if (item != null) {
                final Map<String, Object> languageObject = languageSupport.translateToLanguageObject(item);
                filterBands.add(languageObject);
            }
        }
        if (filterBands.size() > 0) {
            attributes.put(NAME_FILTER_BANDS, filterBands);
        }
    }

    private void collectProductGeoCodingAttrs(Map<String, Object> attrs) throws ProductIOException {
        final ArrayList<String> coordNames = new ArrayList<>();
        final GeoCoding gc = getSourceProduct().getSceneGeoCoding();
        //noinspection deprecation
        if (gc instanceof BasicPixelGeoCoding) {
            @SuppressWarnings("deprecation")
            final BasicPixelGeoCoding pgc = (BasicPixelGeoCoding) gc;
            coordNames.add(pgc.getLatBand().getName());
            coordNames.add(pgc.getLonBand().getName());
        }
        if (gc instanceof ComponentGeoCoding) {
            final ComponentGeoCoding pgc = (ComponentGeoCoding) gc;
            final GeoRaster geoRaster = pgc.getGeoRaster();
            if (geoRaster.getSubsamplingX() == 1.0 && geoRaster.getSubsamplingY() == 1.0) {
                coordNames.add(geoRaster.getLatVariableName());
                coordNames.add(geoRaster.getLonVariableName());
            }
        }
        if (coordNames.size() > 0) {
            final String[] names = coordNames.toArray(new String[0]);
            final String allNames = StringUtils.arrayToString(names, " ");
            attrs.put(COORDINATES, allNames);
        }
        if (gc != null) {
            attrs.put(ATT_NAME_GEOCODING, getGeoCodingAttributes(gc));
        }
    }

    private void collectGeneralProductAttrs(Map<String, Object> attrs) {
        final Product product = getSourceProduct();
        attrs.put(ATT_NAME_PRODUCT_NAME, product.getName());
        attrs.put(ATT_NAME_PRODUCT_TYPE, product.getProductType());
        attrs.put(ATT_NAME_PRODUCT_DESC, product.getDescription());
        attrs.put(ATT_NAME_PRODUCT_SCENE_WIDTH, product.getSceneRasterWidth());
        attrs.put(ATT_NAME_PRODUCT_SCENE_HEIGHT, product.getSceneRasterHeight());
        addTimeAttribute(attrs, TIME_START, product.getStartTime()); // "time_coverage_start"
        addTimeAttribute(attrs, TIME_END, product.getEndTime()); // "time_coverage_end"

        if (product.getAutoGrouping() != null) {
            attrs.put(DATASET_AUTO_GROUPING, product.getAutoGrouping().toString());
        }
        if (isNotNullAndNotEmpty(product.getQuicklookBandName())) {
            attrs.put(QUICKLOOK_BAND_NAME, product.getQuicklookBandName());
        }
    }

    Map<String, Object> getGeoCodingAttributes(GeoCoding gc) throws ProductIOException {
        final PersistenceEncoder<Object> encoder = persistence.getEncoder(gc);
        if (encoder != null) {
            final Item item = encoder.encode(gc);
            return languageSupport.translateToLanguageObject(item);
        }
        if (gc instanceof CrsGeoCoding) {
            final HashMap<String, Object> map = new HashMap<>();
            final CrsGeoCoding crsGeoCoding = (CrsGeoCoding) gc;
            final String wkt = crsGeoCoding.getMapCRS().toString().replaceAll("\n *", "").replaceAll("\r *", "");
            map.put(DimapProductConstants.TAG_WKT, wkt);
            final double[] matrix = new double[6];
            ((AffineTransform) crsGeoCoding.getImageToMapTransform()).getMatrix(matrix);
            map.put(DimapProductConstants.TAG_IMAGE_TO_MODEL_TRANSFORM, matrix);
            return map;
        }
        throw new ProductIOException("Unknown GeoCoding: " + gc.getClass().getName());
    }

    void collectRasterAttributes(RasterDataNode rdNode, Map<String, Object> attributes) {
        if (rdNode.getDescription() != null) {
            attributes.put(LONG_NAME, rdNode.getDescription());
        }
        if (rdNode.getUnit() != null) {
            attributes.put(UNITS, tryFindUnitString(rdNode.getUnit()));
        }
        final int nodeDataType = rdNode.getDataType();
        // TODO: 22.07.2019 SE -- shall log10 scaled really be prohibited
        final Number noDataValue;
        if (!rdNode.isLog10Scaled()) {

            if (ProductData.isUIntType(nodeDataType)) {
                attributes.put(UNSIGNED, String.valueOf(true));
            }
            if (rdNode.getScalingFactor() != 1.0) {
                attributes.put(SCALE_FACTOR, rdNode.getScalingFactor());
            }
            if (rdNode.getScalingOffset() != 0.0) {
                attributes.put(ADD_OFFSET, rdNode.getScalingOffset());
            }
            noDataValue = rdNode.getNoDataValue();
        } else {
            // scaling information is not written anymore for log10 scaled bands
            // instead we always write geophysical values
            // we do this because log scaling is not supported by NetCDF-CF conventions
            noDataValue = rdNode.getGeophysicalNoDataValue();
        }
        if (noDataValue.doubleValue() != 0.0) {
            if (ProductData.isIntType(nodeDataType)) {
                final long longValue = noDataValue.longValue();
                if (ProductData.isUIntType(nodeDataType)) {
                    attributes.put(FILL_VALUE, longValue & 0xffffffffL);
                } else {
                    attributes.put(FILL_VALUE, longValue);
                }
            } else if (ProductData.TYPE_FLOAT64 == nodeDataType) {
                attributes.put(FILL_VALUE, noDataValue.doubleValue());
            } else {
                attributes.put(FILL_VALUE, noDataValue.floatValue());
            }
        }
        if (rdNode.isNoDataValueUsed()) {
            attributes.put(NO_DATA_VALUE_USED, rdNode.isNoDataValueUsed());
        }

        if (isNotNullAndNotEmpty(rdNode.getValidPixelExpression())) {
            attributes.put(VALID_PIXEL_EXPRESSION, rdNode.getValidPixelExpression());
        }

        collectAncillaryElements(rdNode, attributes);

        AffineTransform imageToModelTransform = rdNode.getImageToModelTransform();
        if (!imageToModelTransform.isIdentity()) {
            final double[] matrix = new double[6];
            imageToModelTransform.getMatrix(matrix);
            attributes.put(DimapProductConstants.TAG_IMAGE_TO_MODEL_TRANSFORM, matrix);

        }
        if (rdNode.isStxSet()) {
            final LinkedHashMap<String, Object> stxM = new LinkedHashMap<>();
            attributes.put(STATISTICS, stxM);
            final Stx stx = rdNode.getStx();
            stxM.put(DimapProductConstants.TAG_STX_MIN, stx.getMinimum());
            stxM.put(DimapProductConstants.TAG_STX_MAX, stx.getMaximum());
            stxM.put(DimapProductConstants.TAG_STX_MEAN, stx.getMean());
            stxM.put(DimapProductConstants.TAG_STX_STDDEV, stx.getStandardDeviation());
            stxM.put(DimapProductConstants.TAG_STX_LEVEL, stx.getResolutionLevel());
            final int[] bins = stx.getHistogramBins();
            if (bins != null && bins.length > 0) {
                stxM.put(DimapProductConstants.TAG_HISTOGRAM, bins);
            }
        }

        final ImageInfo imageInfo = rdNode.getImageInfo();
        if (imageInfo != null) {
            final LinkedHashMap<String, Object> infoM = new LinkedHashMap<>();

            final ColorPaletteDef paletteDef = imageInfo.getColorPaletteDef();
            final ArrayList<Map<String, Object>> pointsL = new ArrayList<>();
            for (ColorPaletteDef.Point point : paletteDef.getPoints()) {
                final LinkedHashMap<String, Object> pointM = new LinkedHashMap<>();
                if (StringUtils.isNotNullAndNotEmpty(point.getLabel())) {
                    pointM.put(LABEL, point.getLabel());
                }
                pointM.put(SAMPLE, point.getSample());
                pointM.put(COLOR_RGBA, createColorObject(point.getColor()));
                pointsL.add(pointM);
            }
            infoM.put(COLOR_PALETTE_POINTS, pointsL);
            infoM.put(COLOR_PALETTE_NUM_COLORS, paletteDef.getNumColors());
            infoM.put(COLOR_PALETTE_AUTO_DISTRIBUTE, paletteDef.isAutoDistribute());
            infoM.put(COLOR_PALETTE_DISCRETE, paletteDef.isDiscrete());
            final Color noDataColor = imageInfo.getNoDataColor();
            if (noDataColor != null) {
                infoM.put(NO_DATA_COLOR_RGBA, createColorObject(noDataColor));
            }
            infoM.put(HISTOGRAM_MATCHING, imageInfo.getHistogramMatching().toString());
            infoM.put(LOG_10_SCALED, imageInfo.isLogScaled());
            if (StringUtils.isNotNullAndNotEmpty(imageInfo.getUncertaintyBandName())) {
                infoM.put(UNCERTAINTY_BAND_NAME, imageInfo.getUncertaintyBandName());
            }
            if (imageInfo.getUncertaintyVisualisationMode() != null) {
                final ImageInfo.UncertaintyVisualisationMode mode = imageInfo.getUncertaintyVisualisationMode();
                infoM.put(UNCERTAINTY_VISUALISATION_MODE, mode.toString());
            }
            attributes.put(IMAGE_INFO, infoM);
        }
    }

    private void collectAncillaryElements(RasterDataNode rdNode, Map<String, Object> attributes) {
        final Container root = new Container("root");
        RasterDataNodePersistenceHelper.addAncillaryElements(root, rdNode);
        final Map<String, Map<String, Object>> rootMap = languageSupport.translateToLanguageObject(root);
        final Map<String, Object> ancillaryElements = rootMap.get("root");
        for (Map.Entry<String, Object> entry : ancillaryElements.entrySet()) {
            attributes.put(entry.getKey(), entry.getValue());
        }
    }

    private static Object createColorObject(Color c) {
        return new int[]{c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()};
    }

    static void collectBandAttributes(Band band, Map<String, Object> attributes) {
        if (band.getSpectralBandwidth() > 0) {
            attributes.put(BANDWIDTH, band.getSpectralBandwidth());
            attributes.put(BANDWIDTH + UNIT_EXTENSION, BANDWIDTH_UNIT);
        }
        if (band.getSpectralWavelength() > 0) {
            attributes.put(WAVELENGTH, band.getSpectralWavelength());
            attributes.put(WAVELENGTH + UNIT_EXTENSION, WAVELENGTH_UNIT);
        }
        if (band.getSolarFlux() > 0) {
            // TODO: 15.11.2019 SE -- unit for solarFlux
            attributes.put(SOLAR_FLUX, band.getSolarFlux());
        }
        if ((float) band.getSpectralBandIndex() >= 0) {
            attributes.put(SPECTRAL_BAND_INDEX, band.getSpectralBandIndex());
        }
    }

    private void addTimeAttribute(Map<String, Object> productAttributes, String attName, ProductData.UTC utc) {
        if (utc != null) {
            productAttributes.put(attName, ISO8601Converter.format(utc));
        }
    }

    private ProductData ensureNotLogarithmicData(Band sourceBand, ProductData data) {
        final boolean log10Scaled = sourceBand.isLog10Scaled();
        if (!log10Scaled) {
            return data;
        }
        final ProductData scaledData = ProductData.createInstance(ProductData.TYPE_FLOAT32, data.getNumElems());
        for (int i = 0; i < data.getNumElems(); i++) {
            // TODO: 09.06.2021 SE -- really scale each value step by step ?
            scaledData.setElemDoubleAt(i, sourceBand.scale(data.getElemDoubleAt(i)));
        }
        return scaledData;
    }

    static void collectSampleCodingAttributes(Band band, Map<String, Object> attributes) {
        final SampleCoding sampleCoding = band.getSampleCoding();
        if (sampleCoding == null) {
            return;
        }

        attributes.put(ZnapConstantsAndUtils.NAME_SAMPLE_CODING, sampleCoding.getName());

        final boolean indexBand = band.isIndexBand();
        final boolean flagBand = band.isFlagBand();
        if (!(indexBand || flagBand)) {
            LOG.warning("Band references a SampleCoding but this is neither an IndexCoding nor a FlagCoding.");
            return;
        }

        final int numCodings = sampleCoding.getNumAttributes();
        final String[] names = new String[numCodings];
        final String[] descriptions = new String[numCodings];
        final int[] masks = new int[numCodings];
        final int[] values = new int[numCodings];
        final MetadataAttribute[] codingAtts = sampleCoding.getAttributes();
        boolean alsoFlagValues = false;
        for (int i = 0; i < codingAtts.length; i++) {
            MetadataAttribute attribute = codingAtts[i];
            names[i] = attribute.getName();
            descriptions[i] = attribute.getDescription();
            final ProductData data = attribute.getData();
            if (indexBand) {
                values[i] = data.getElemInt();
            } else {
                masks[i] = data.getElemInt();
                final boolean twoElements = data.getNumElems() == 2;
                values[i] = twoElements ? data.getElemIntAt(1) : data.getElemInt();
                alsoFlagValues = alsoFlagValues || twoElements;
            }
        }
        attributes.put(FLAG_MEANINGS, names);
        if (indexBand) {
            attributes.put(FLAG_VALUES, values);
        } else {
            attributes.put(FLAG_MASKS, masks);
            if (alsoFlagValues) {
                attributes.put(FLAG_VALUES, values);
            }
        }
        if (containsNotEmptyStrings(descriptions)) {
            attributes.put(FLAG_DESCRIPTIONS, descriptions);
        }
    }

    private static boolean containsNotEmptyStrings(final String[] strings) {
        if (strings != null && strings.length > 0) {
            for (int i = 0; i < strings.length; i++) {
                final String str = strings[i];
                strings[i] = str != null ? str.trim() : null;
            }
            for (String str : strings) {
                if (str != null && !str.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Number getZarrFillValue(RasterDataNode node) {
        final Double geophysicalNoDataValue = node.getGeophysicalNoDataValue();
        final DataType zarrDataType = getZarrDataType(node);
        if (zarrDataType == DataType.f8) {
            return geophysicalNoDataValue;
        }
        switch (zarrDataType) {
            case f4:
                return geophysicalNoDataValue.floatValue();
            case i1:
            case u1:
            case i2:
            case u2:
            case i4:
            case u4:
                return geophysicalNoDataValue.longValue();
            default:
                throw new IllegalStateException();
        }
    }

    private static DataType getZarrDataType(RasterDataNode node) {
        if (node.isLog10Scaled()) {
            // TODO: 09.06.2021 SE -- really always f4?
            return com.bc.zarr.DataType.f4;
        }
        final int dataType = node.getDataType();
        switch (dataType) {
            case ProductData.TYPE_FLOAT64:
                return DataType.f8;
            case ProductData.TYPE_FLOAT32:
                return DataType.f4;
            case ProductData.TYPE_INT8:
                return DataType.i1;
            case ProductData.TYPE_INT16:
                return DataType.i2;
            case ProductData.TYPE_INT32:
                return DataType.i4;
            case ProductData.TYPE_UINT8:
                return DataType.u1;
            case ProductData.TYPE_UINT16:
                return DataType.u2;
            case ProductData.TYPE_UINT32:
                return DataType.u4;
            default:
                throw new IllegalStateException();
        }
    }

    private String getCompressorId() {
        String compressorID = getPreference(PROPERTY_NAME_COMPRESSOR_ID, DEFAULT_COMPRESSOR_ID);
        compressorID = compressorID != null ? compressorID.trim() : null;
        if (!DEFAULT_COMPRESSOR_ID.equals(compressorID)) {
            LOG.fine("Znap format product writer will use '" + compressorID + "' compression.");
        }
        return compressorID;
    }

    private int getCompressionLevel() {
        final String value = getPreference(PROPERTY_NAME_COMPRESSION_LEVEL, "" + DEFAULT_COMPRESSION_LEVEL);
        final int compressionLevel = Integer.parseInt(value);
        if (compressionLevel != DEFAULT_COMPRESSION_LEVEL) {
            LOG.fine("Znap format product writer will use " + compressionLevel + " compression level.");
        }
        return compressionLevel;
    }

    // package access for testing only tb 2024-02-12
    static boolean isUseZipArchive() {
       final boolean useZipArchive = ZnapProductWriterPlugIn.isUseZipArchive();
        if (useZipArchive != DEFAULT_USE_ZIP_ARCHIVE) {
            final String insert = useZipArchive ? "" : "not ";
            LOG.fine("Znap format product writer will " + insert + "write into an zip archive.");
        }
        return useZipArchive;
    }

    private ProductWriterPlugIn getBinaryWriterPlugin(ZnapProductWriterPlugIn productWriterPlugIn) {
        String defaultBinaryFormatName = productWriterPlugIn.getFormatNames()[0];
        final String binaryFormatName = getPreference(PROPERTY_NAME_BINARY_FORMAT, defaultBinaryFormatName);
        if (defaultBinaryFormatName.equals(binaryFormatName)) {
            return null;
        }
        LOG.fine("Binary data in Znap format should be written as '" + binaryFormatName + "'");
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator<ProductWriterPlugIn> writerPlugIns = plugInManager.getWriterPlugIns(binaryFormatName);
        ProductWriterPlugIn writerPlugIn = writerPlugIns.hasNext() ? writerPlugIns.next() : null;
        if (writerPlugIn == null) {
            throw new IllegalArgumentException("Unable to write binary data as '" + binaryFormatName + "'.");
        }
        return writerPlugIn;
    }

    void setPreferencesForTestPurposesOnly(Properties testPreferences) {
        this.testProperties = testPreferences;
    }

    private String getPreference(String key, String defaultValue) {
        if (testProperties != null) {
            return testProperties.getProperty(key, defaultValue);
        }
        return getPreferences().get(key, defaultValue);
    }

    private Preferences getPreferences() {
        return Config.instance("snap").load().preferences();
    }

    private void writeTiePointGrid(TiePointGrid tiePointGrid) throws IOException {
        final int[] shape = {tiePointGrid.getGridHeight(), tiePointGrid.getGridWidth()}; // common data model manner { y, x }
        final String name = tiePointGrid.getName();
        final Map<String, Object> attributes = collectTiePointGridAttributes(tiePointGrid);
        final ArrayParams arrayParams = new ArrayParams()
                .dataType(getZarrDataType(tiePointGrid))
                .shape(shape)
                .fillValue(getZarrFillValue(tiePointGrid))
                .compressor(compressor);
        final ZarrArray zarrArray = zarrGroup.createArray(name, arrayParams, attributes);

        final ProductData gridData;
        if (tiePointGrid.getGridData() != null) {
            gridData = tiePointGrid.getData();
        } else {
            gridData = readTiePointGridData(tiePointGrid);
        }
        if (binaryWriterPlugIn == null) {
            try {
                zarrArray.write(gridData.getElems(), shape, new int[]{0, 0});
            } catch (InvalidRangeException e) {
                throw new IOException("Invalid range while writing raster '" + name + "'", e);
            }
        } else {
            final ProductWriter writer = createBinaryProductWriter();
            final Product binaryProduct = new Product("_" + name + "_", "binary", shape[IDX_WIDTH], shape[IDX_HEIGHT]);
            final Band binaryBand = binaryProduct.addBand("data", getSnapDataType(zarrArray.getDataType()).getValue());
            final String fileExtension = binaryWriterPlugIn.getDefaultFileExtensions()[0];
            final Path path = outputRoot.resolve(name).resolve(name + fileExtension);
            writer.writeProductNodes(binaryProduct, path.toFile());
            binaryProduct.setProductWriter(writer);
            binaryBand.writeRasterData(0, 0, shape[IDX_WIDTH], shape[IDX_HEIGHT], gridData);
            binaryProduct.dispose();
        }
    }

    private ProductWriter createBinaryProductWriter() {
        final ProductWriter writer = binaryWriterPlugIn.createWriterInstance();
        switchToIntermediateMode(writer);
        return writer;
    }

    private Map<String, Object> collectTiePointGridAttributes(TiePointGrid tiePointGrid) {
        final Map<String, Object> attributes = new HashMap<>();
        collectVersion(attributes);
        final String xName = dimensionNameGenerator.getDimensionNameFor("xt", tiePointGrid.getGridWidth());
        final String yName = dimensionNameGenerator.getDimensionNameFor("yt", tiePointGrid.getGridHeight());
        attributes.put("_ARRAY_DIMENSIONS", new String[]{yName, xName});
        collectRasterAttributes(tiePointGrid, attributes);
        attributes.put(ATT_NAME_OFFSET_X, tiePointGrid.getOffsetX());
        attributes.put(ATT_NAME_OFFSET_Y, tiePointGrid.getOffsetY());
        attributes.put(ATT_NAME_SUBSAMPLING_X, tiePointGrid.getSubSamplingX());
        attributes.put(ATT_NAME_SUBSAMPLING_Y, tiePointGrid.getSubSamplingY());
        if (binaryWriterPlugIn != null) {
            attributes.put(ATT_NAME_BINARY_FORMAT, binaryWriterPlugIn.getFormatNames()[0]);
        }
        final int discontinuity = tiePointGrid.getDiscontinuity();
        if (discontinuity != TiePointGrid.DISCONT_NONE) {
            attributes.put(DISCONTINUITY, discontinuity);
        }
        return attributes;
    }

    private void trimChunks(int[] chunks, int[] shape) {
        for (int i = 0; i < shape.length; i++) {
            if (shape[i] < chunks[i]) {
                chunks[i] = shape[i];
            }
        }
    }

    private void initializeZarrArrayForBand(Band band) throws IOException {
        final int[] shape = getShape(band); // common data model manner { y, x }
        final String name = band.getName();
        final ArrayParams arrayParams = new ArrayParams()
                .dataType(getZarrDataType(band))
                .shape(shape)
                .fillValue(getZarrFillValue(band))
                .compressor(compressor);
        if (binaryWriterPlugIn == null) {
            int[] chunks;
            MultiLevelImage sourceImage = band.getSourceImage();
            chunks = new int[]{sourceImage.getTileHeight(), sourceImage.getTileWidth()}; // common data model manner { y, x }
            trimChunks(chunks, shape);
            arrayParams.chunks(chunks);
        } else {
            arrayParams.chunked(false);
        }
        zarrGroup.createArray(name, arrayParams, collectBandAttributes(band));
    }

    private void initializeZarrBandWriter(Band band) throws IOException {
        int[] shape = getShape(band);
        String name = band.getName();
        ZarrArray zarrArray = zarrGroup.openArray(name);
        final BinaryWriter binaryWriter;
        if (binaryWriterPlugIn == null) {
            binaryWriter = new StandardZarrChunksWriter(zarrArray);
        } else {
            final ProductWriter writer = createBinaryProductWriter();
            final Product binaryProduct = new Product("_" + name + "_", "binary", shape[IDX_WIDTH], shape[IDX_HEIGHT]);
            final GeoCoding geoCoding = band.getGeoCoding();
            if (geoCoding instanceof CrsGeoCoding) {
                binaryProduct.setSceneGeoCoding(geoCoding);
            }
            final Band binaryBand = binaryProduct.addBand("data", getSnapDataType(zarrArray.getDataType()).getValue());
            final String fileExtension = binaryWriterPlugIn.getDefaultFileExtensions()[0];
            final Path path = outputRoot.resolve(name).resolve(name + fileExtension);
            writer.writeProductNodes(binaryProduct, path.toFile());
            binaryProduct.setProductWriter(writer);
            binaryWriter = new UserDefinedBinaryWriter(binaryBand);
        }
        zarrWriters.put(band, binaryWriter);
    }

    private int[] getShape(Band band) {
        return new int[]{band.getRasterHeight(), band.getRasterWidth()};
    }

    private void switchToIntermediateMode(ProductWriter writer) {
        final Class<? extends ProductWriter> writerClass = writer.getClass();
        if (!writerClass.getName().contains("BigGeoTiffProductWriter")) {
            return;
        }
        try {
            final Method setWriteIntermediateProduct = writerClass.getDeclaredMethod("setWriteIntermediateProduct", boolean.class);
            setWriteIntermediateProduct.setAccessible(true);
            setWriteIntermediateProduct.invoke(writer, true);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(
                    "Unable to switch on 'writeIntermediateProduct' on binary product writer '" + binaryWriterPlugIn.getFormatNames()[0] + "'.", e);
        }
    }

    private Map<String, Object> collectBandAttributes(Band band) throws ProductIOException {
        final Map<String, Object> bandAttributes = new LinkedHashMap<>();
        collectVersion(bandAttributes);
        final String xName = dimensionNameGenerator.getDimensionNameFor("x", band.getRasterWidth());
        final String yName = dimensionNameGenerator.getDimensionNameFor("y", band.getRasterHeight());
        bandAttributes.put("_ARRAY_DIMENSIONS", new String[]{yName, xName});
        collectBandAttributes(band, bandAttributes);
        collectSampleCodingAttributes(band, bandAttributes);
        collectVirtualBandAttributes(band, bandAttributes);
        collectRasterAttributes(band, bandAttributes);
        if (binaryWriterPlugIn != null) {
            bandAttributes.put(ATT_NAME_BINARY_FORMAT, binaryWriterPlugIn.getFormatNames()[0]);
        }
        final GeoCoding geoCoding = band.getGeoCoding();
        if (geoCoding != null) {
            bandAttributes.put(ATT_NAME_GEOCODING, getGeoCodingAttributes(geoCoding));
        }
        return bandAttributes;
    }

    static void collectVirtualBandAttributes(Band band, Map<String, Object> bandAttributes) {
        if (band instanceof VirtualBand) {
            final VirtualBand virtualBand = (VirtualBand) band;
            bandAttributes.put(VIRTUAL_BAND_EXPRESSION, virtualBand.getExpression());
        }
    }

    private ProductData readTiePointGridData(TiePointGrid tiePointGrid) throws IOException {
        final int gridWidth = tiePointGrid.getGridWidth();
        final int gridHeight = tiePointGrid.getGridHeight();
        ProductData productData = tiePointGrid.createCompatibleRasterData(gridWidth, gridHeight);
        getSourceProduct().getProductReader().readTiePointGridRasterData(tiePointGrid, 0, 0, gridWidth, gridHeight, productData,
                                                                         ProgressMonitor.NULL);
        return productData;
    }

    private interface BinaryWriter {
        void write(ProductData data, int[] dataShape, int[] offset) throws IOException, InvalidRangeException;

        void dispose();
    }

    private static class StandardZarrChunksWriter implements BinaryWriter {
        private final ZarrArray zarrArray;

        public StandardZarrChunksWriter(ZarrArray zarrArray) {
            this.zarrArray = zarrArray;
        }

        @Override
        public void write(ProductData data, int[] dataShape, int[] offset) throws IOException, InvalidRangeException {
            zarrArray.write(data.getElems(), dataShape, offset);
        }

        @Override
        public void dispose() {
        }
    }

    private static class UserDefinedBinaryWriter implements BinaryWriter {
        private final Band binaryBand;

        public UserDefinedBinaryWriter(Band binaryBand) {
            this.binaryBand = binaryBand;
        }

        @Override
        public void write(ProductData data, int[] dataShape, int[] offset) throws IOException, InvalidRangeException {
            binaryBand.writeRasterData(offset[IDX_X], offset[IDX_Y], dataShape[IDX_WIDTH], dataShape[IDX_HEIGHT], data);
        }

        @Override
        public void dispose() {
            final Product product = binaryBand.getProduct();
            product.dispose();
        }
    }

    static class DimensionNameGenerator {

        private final Map<String, Map<Integer, String>> dimensions = new LinkedHashMap<>();

        public String getDimensionNameFor(String name, int dimSize) {
            final Map<Integer, String> definedDims;
            if (dimensions.containsKey(name)) {
                definedDims = dimensions.get(name);
            } else {
                definedDims = new LinkedHashMap<>();
                dimensions.put(name, definedDims);
            }
            if (definedDims.containsKey(dimSize)) {
                return definedDims.get(dimSize);
            } else {
                if (definedDims.containsValue(name)) {
                    final String newName = name + definedDims.size();
                    definedDims.put(dimSize, newName);
                    return newName;
                } else {
                    definedDims.put(dimSize, name);
                    return name;
                }
            }
        }
    }
}
