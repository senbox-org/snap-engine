/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 */
package org.esa.snap.dataio.envisat;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.PixelInterpolatingForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.runtime.Config;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.prefs.Preferences;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCoding.SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY;
import static org.esa.snap.core.dataio.geocoding.InverseCoding.KEY_SUFFIX_INTERPOLATING;

/**
 * The <code>EnvisatProductReader</code> class is an implementation of the <code>ProductReader</code> interface
 * exclusively for data products having the standard ESA/ENVISAT raw format.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.EnvisatProductReaderPlugIn
 */
public class EnvisatProductReader extends AbstractProductReader {

    /**
     * @since BEAM 4.9
     */
    private static final String SYSPROP_ENVISAT_USE_PIXEL_GEO_CODING = "snap.envisat.usePixelGeoCoding";
    private static final String SYSPROP_ENVISAT_PIXEL_CODING_INVERSE = "snap.envisat.pixelGeoCoding.inverse";
    private static final String SYSPROP_ENVISAT_TIE_POINT_CODING_FORWARD = "snap.envisat.tiePointGeoCoding.forward";

    /**
     * Represents the product's file.
     */
    private ProductFile productFile;

    /**
     * The width of the raster covering the full scene.
     */
    private int sceneRasterWidth;
    /**
     * The height of the raster covering the full scene.
     */
    private int sceneRasterHeight;

    private Map<Band, BandLineReader> bandlineReaderMap;

    /**
     * Constructs a new ENVISAT product reader.
     *
     * @param readerPlugIn the plug-in which created this reader instance
     */
    public EnvisatProductReader(EnvisatProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public ProductFile getProductFile() {
        return productFile;
    }

    public int getSceneRasterWidth() {
        return sceneRasterWidth;
    }

    public int getSceneRasterHeight() {
        return sceneRasterHeight;
    }


    /**
     * Reads a data product and returns a in-memory representation of it. This method was called by
     * <code>readProductNodes(input, subsetInfo)</code> of the abstract superclass.
     *
     * @throws java.lang.IllegalArgumentException if <code>input</code> type is not one of the supported input sources.
     * @throws java.io.IOException                if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object input = getInput();
        if (input instanceof String || input instanceof File) {
            File file = new File(input.toString());
            try {
                productFile = ProductFile.open(file);
            } catch (IOException e) {
                final InputStream inputStream;
                try {
                    inputStream = EnvisatProductReaderPlugIn.getInflaterInputStream(file);
                } catch (IOException ignored) {
                    throw e;
                }
                productFile = ProductFile.open(file, new FileCacheImageInputStream(inputStream, null));
            }
        } else if (input instanceof ImageInputStream) {
            productFile = ProductFile.open((ImageInputStream) input);
        } else if (input instanceof ProductFile) {
            productFile = (ProductFile) input;
        }

        Debug.assertNotNull(productFile);
        sceneRasterWidth = productFile.getSceneRasterWidth();
        sceneRasterHeight = productFile.getSceneRasterHeight();

        if (getSubsetDef() != null) {
            Dimension s = getSubsetDef().getSceneRasterSize(sceneRasterWidth, sceneRasterHeight);
            sceneRasterWidth = s.width;
            sceneRasterHeight = s.height;
        }

        return createProduct();
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (productFile != null) {
            productFile.close();
            productFile = null;
        }
        super.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final BandLineReader bandLineReader = bandlineReaderMap.get(destBand);
        final int sourceMinX = sourceOffsetX;
        final int sourceMinY = sourceOffsetY;
        final int sourceMaxX = Math.min(destBand.getRasterWidth() - 1, sourceMinX + sourceWidth - 1);
        final int sourceMaxY = sourceMinY + sourceHeight - 1;


        pm.beginTask("Reading band '" + destBand.getName() + "'...", (sourceMaxY - sourceMinY) + 1);
        // For each scan in the data source
        try {

            int destArrayPos = 0;
            for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                bandLineReader.readRasterLine(sourceMinX, sourceMaxX, sourceStepX,
                        sourceY,
                        destBuffer, destArrayPos);

                destArrayPos += destWidth;
                pm.worked(sourceStepY);
            }
            pm.worked(1);
        } finally {
            pm.done();
        }

    }

    private Product createProduct() throws IOException {
        Debug.assertNotNull(getProductFile());

        File file = getProductFile().getFile();
        String productName;
        if (file != null) {
            productName = file.getName();
        } else {
            productName = getProductFile().getProductId();
        }
        productName = FileUtils.createValidFilename(productName);
        productName = FileUtils.getFilenameWithoutExtension(productName); // .zip
        productName = FileUtils.getFilenameWithoutExtension(productName); // .N1

        int sceneRasterHeight = getSceneRasterHeight();
        Product product = new Product(productName,
                getProductFile().getProductType(),
                getSceneRasterWidth(),
                sceneRasterHeight,
                this);

        product.setFileLocation(getProductFile().getFile());
        product.setDescription(getProductFile().getProductDescription());
        final ProductData.UTC startTime = getProductFile().getSceneRasterStartTime();
        final ProductData.UTC endTime = getProductFile().getSceneRasterStopTime();
        product.setStartTime(startTime);
        product.setEndTime(endTime);
        if (startTime != null && endTime != null && sceneRasterHeight > 0) {
            product.setSceneTimeCoding(new LineTimeCoding(sceneRasterHeight, startTime.getMJD(), endTime.getMJD()));
        }
        product.setAutoGrouping(getProductFile().getAutoGroupingPattern());

        addBandsToProduct(product);
        addTiePointGridsToProduct(product);
        addGeoCodingToProduct(product);
        initPointingFactory(product);
        if (!isMetadataIgnored()) {
            addHeaderAnnotationsToProduct(product);
            addDatasetAnnotationsToProduct(product);
        }
        addDefaultMasksToProduct(product);
        addDefaultMasksDefsToBands(product);
        productFile.addCustomMetadata(product);

        return product;
    }

    private void addBandsToProduct(Product product) {
        Debug.assertNotNull(productFile);
        Debug.assertNotNull(product);

        BandLineReader[] bandLineReaders = productFile.getBandLineReaders();
        bandlineReaderMap = new HashMap<>(bandLineReaders.length);
        for (BandLineReader bandLineReader : bandLineReaders) {
            if (bandLineReader.isTiePointBased()) {
                continue;
            }
            if (!(bandLineReader instanceof BandLineReader.Virtual)) {
                if (bandLineReader.getPixelDataReader().getDSD().getDatasetSize() == 0 ||
                        bandLineReader.getPixelDataReader().getDSD().getNumRecords() == 0) {
                    continue;
                }
            }
            String bandName = bandLineReader.getBandName();
            if (isNodeAccepted(bandName)) {
                BandInfo bandInfo = bandLineReader.getBandInfo();
                Band band;

                int width = bandInfo.getWidth();
                int height = bandInfo.getHeight();
                if (getSubsetDef() != null) {
                    Dimension s = getSubsetDef().getSceneRasterSize(width, height);
                    width = s.width;
                    height = s.height;
                }

                if (bandLineReader instanceof BandLineReader.Virtual) {
                    final BandLineReader.Virtual virtual = ((BandLineReader.Virtual) bandLineReader);
                    band = new VirtualBand(bandName, ProductData.TYPE_FLOAT64,//bandInfo.getDataType(),
                            width, height,
                            virtual.getExpression());
                } else {
                    band = new Band(bandName,
                            bandInfo.getDataType() < ProductData.TYPE_FLOAT32 ? bandInfo.getDataType() : bandLineReader.getPixelDataField().getDataType(),
                            width, height);
                }
                band.setScalingOffset(bandInfo.getScalingOffset());

                productFile.setInvalidPixelExpression(band);

                band.setScalingFactor(bandInfo.getScalingFactor());
                band.setLog10Scaled(bandInfo.getScalingMethod() == BandInfo.SCALE_LOG10);
                band.setSpectralBandIndex(bandInfo.getSpectralBandIndex());
                if (bandInfo.getPhysicalUnit() != null) {
                    band.setUnit(bandInfo.getPhysicalUnit());
                }
                if (bandInfo.getDescription() != null) {
                    band.setDescription(bandInfo.getDescription());
                }
                if (bandInfo.getFlagCoding() != null) {
                    product.getFlagCodingGroup().add(bandInfo.getFlagCoding());
                    band.setSampleCoding(bandInfo.getFlagCoding());
                }
                final String expression = bandInfo.getValidExpression();
                if (expression != null && expression.trim().length() > 0) {
                    band.setValidPixelExpression(expression.trim());
                }
                bandlineReaderMap.put(band, bandLineReader);
                product.addBand(band);
            }

        }
        setSpectralBandInfo(product);
    }

    private void addDefaultMasksToProduct(Product product) {
        List<Band> flagDsList = new Vector<>();
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            if (band.getFlagCoding() != null) {
                flagDsList.add(band);
            }
        }
        if (!flagDsList.isEmpty()) {
            for (Band flagDs : flagDsList) {
                String flagDsName = flagDs.getName();
                Mask[] masks = productFile.createDefaultMasks(flagDsName);
                ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
                for (Mask mask : masks) {
                    maskGroup.add(mask);
                }
            }
        }
    }


    private void addDefaultMasksDefsToBands(Product product) {
        for (final Band band : product.getBands()) {
            final String[] maskNames = getDefaultBitmaskNames(band.getName());
            if (maskNames != null) {
                for (final String maskName : maskNames) {
                    final Mask mask = product.getMaskGroup().get(maskName);
                    if (mask != null) {
                        band.getOverlayMaskGroup().add(mask);
                    }
                }
            }
        }
    }

    private String[] getDefaultBitmaskNames(String bandName) {
        return productFile.getDefaultBitmaskNames(bandName);
    }

    private void setSpectralBandInfo(Product product) {
        float[] wavelengths = productFile.getSpectralBandWavelengths();
        float[] bandwidths = productFile.getSpectralBandBandwidths();
        float[] solar_fluxes = productFile.getSpectralBandSolarFluxes();
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            int sbi = band.getSpectralBandIndex();
            if (sbi >= 0) {
                if (wavelengths != null) {
                    band.setSpectralWavelength(wavelengths[sbi % wavelengths.length]);
                }
                if (bandwidths != null) {
                    band.setSpectralBandwidth(bandwidths[sbi % bandwidths.length]);
                }
                if (solar_fluxes != null) {
                    band.setSolarFlux(solar_fluxes[sbi % solar_fluxes.length]);
                }
            }
        }
    }

    private void addTiePointGridsToProduct(Product product) throws IOException {
        BandLineReader[] bandLineReaders = getProductFile().getBandLineReaders();
        for (BandLineReader bandLineReader : bandLineReaders) {
            if (bandLineReader.isTiePointBased()) {
                TiePointGrid tiePointGrid = createTiePointGrid(bandLineReader);
                product.addTiePointGrid(tiePointGrid);
            }
        }
    }

    private void addGeoCodingToProduct(Product product) throws IOException {
        final Preferences preferences = Config.instance("snap").preferences();
        final boolean usePixelGeoCoding = preferences.getBoolean(SYSPROP_ENVISAT_USE_PIXEL_GEO_CODING, false);

        final String productType = productFile.getProductType();

        final ComponentGeoCoding geoCoding;
        if (usePixelGeoCoding &&
                (productType.equalsIgnoreCase(EnvisatConstants.MERIS_FSG_L1B_PRODUCT_TYPE_NAME) ||
                        productType.equalsIgnoreCase(EnvisatConstants.MERIS_FRG_L1B_PRODUCT_TYPE_NAME))) {
            geoCoding = createPixelGeoCoding(product);
        } else {
            geoCoding = createTiePointGeoCoding(product);
        }

        if (geoCoding == null) {
            return;
        }

        // @todo 2 tb/tb maybe not here? 2020-01-15
        geoCoding.initialize();
        product.setSceneGeoCoding(geoCoding);
    }

    private ComponentGeoCoding createPixelGeoCoding(Product product) throws IOException {
        final Band lonBand = product.getBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME);
        final Band latBand = product.getBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME);

        final double[] longitudes = RasterUtils.loadDataScaled(lonBand);
        lonBand.unloadRasterData();
        final double[] latitudes = RasterUtils.loadDataScaled(latBand);
        latBand.unloadRasterData();

        final double resolutionInKilometers = getResolutionInKilometers(productFile.getProductType());

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes,
                EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME, EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME,
                lonBand.getRasterWidth(), lonBand.getRasterHeight(), resolutionInKilometers);
        final String[] codingKeys = getForwardAndInverseKeys_pixelCoding();

        final ForwardCoding forward = ComponentFactory.getForward(codingKeys[0]);
        final InverseCoding inverse = ComponentFactory.getInverse(codingKeys[1]);

        return new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN);
    }

    private String[] getForwardAndInverseKeys_pixelCoding() {
        final String[] codingNames = new String[2];

        final Preferences preferences = Config.instance("snap").preferences();
        final boolean useFractAccuracy = preferences.getBoolean(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY, false);
        codingNames[1] = preferences.get(SYSPROP_ENVISAT_PIXEL_CODING_INVERSE, PixelQuadTreeInverse.KEY);
        if (useFractAccuracy) {
            codingNames[0] = PixelInterpolatingForward.KEY;
            codingNames[1] = codingNames[1].concat(KEY_SUFFIX_INTERPOLATING);
        } else {
            codingNames[0] = PixelForward.KEY;
        }

        return codingNames;
    }

    private String[] getForwardAndInverseKeys_tiePointCoding() {
        final String[] codingNames = new String[2];

        final Preferences preferences = Config.instance("snap").preferences();
        codingNames[0] = preferences.get(SYSPROP_ENVISAT_TIE_POINT_CODING_FORWARD, TiePointBilinearForward.KEY);
        codingNames[1] = TiePointInverse.KEY;

        return codingNames;
    }

    private ComponentGeoCoding createTiePointGeoCoding(Product product) throws IOException {
        final TiePointGrid lonGrid = product.getTiePointGrid(EnvisatConstants.LON_DS_NAME);
        final TiePointGrid latGrid = product.getTiePointGrid(EnvisatConstants.LAT_DS_NAME);
        if (lonGrid == null || latGrid == null) {
            return null;
        }

        final double[] longitudes = RasterUtils.loadData(lonGrid);
        final double[] latitudes = RasterUtils.loadData(latGrid);
        final double resolutionInKilometers = getResolutionInKilometers(productFile.getProductType());

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, EnvisatConstants.LON_DS_NAME, EnvisatConstants.LAT_DS_NAME,
                lonGrid.getGridWidth(), lonGrid.getGridHeight(),
                product.getSceneRasterWidth(), product.getSceneRasterHeight(), resolutionInKilometers,
                lonGrid.getOffsetX(), lonGrid.getOffsetY(),
                lonGrid.getSubSamplingX(), lonGrid.getSubSamplingY());

        final String[] codingKeys = getForwardAndInverseKeys_tiePointCoding();
        final ForwardCoding forward = ComponentFactory.getForward(codingKeys[0]);
        final InverseCoding inverse = ComponentFactory.getInverse(codingKeys[1]);

        return new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN);
    }

    /**
     * Installs an Envisat-specific pointing factory in the given product.
     */
    private static void initPointingFactory(final Product product) {
        PointingFactoryRegistry registry = PointingFactoryRegistry.getInstance();
        PointingFactory pointingFactory = registry.getPointingFactory(product.getProductType());
        product.setPointingFactory(pointingFactory);
    }

    private void addHeaderAnnotationsToProduct(Product product) {
        Debug.assertNotNull(productFile);
        Debug.assertNotNull(product);
        final MetadataElement metaRoot = product.getMetadataRoot();
        metaRoot.addElement(createMetadataGroup("MPH", productFile.getMPH().getParams()));
        metaRoot.addElement(createMetadataGroup("SPH", productFile.getSPH().getParams()));

        final DSD[] dsds = productFile.getDsds();
        final MetadataElement dsdsGroup = new MetadataElement("DSD");
        for (int i = 0; i < dsds.length; i++) {
            final DSD dsd = dsds[i];
            if (dsd != null) {
                final MetadataElement dsdGroup = new MetadataElement("DSD." + (i + 1));
                dsdGroup.addAttribute(
                        new MetadataAttribute("DATASET_NAME",
                                ProductData.createInstance(getNonNullString(dsd.getDatasetName())),
                                true));
                dsdGroup.addAttribute(new MetadataAttribute("DATASET_TYPE",
                        ProductData.createInstance(new String(new char[]{dsd.getDatasetType()})),
                        true));
                dsdGroup.addAttribute(new MetadataAttribute("FILE_NAME",
                        ProductData.createInstance(getNonNullString(dsd.getFileName())),
                        true));
                dsdGroup.addAttribute(new MetadataAttribute("OFFSET", ProductData.createInstance(new long[]{dsd.getDatasetOffset()}), true));
                dsdGroup.addAttribute(new MetadataAttribute("SIZE", ProductData.createInstance(new long[]{dsd.getDatasetSize()}), true));
                dsdGroup.addAttribute(new MetadataAttribute("NUM_RECORDS",
                        ProductData.createInstance(new int[]{dsd.getNumRecords()}),
                        true));
                dsdGroup.addAttribute(new MetadataAttribute("RECORD_SIZE",
                        ProductData.createInstance(new int[]{dsd.getRecordSize()}),
                        true));
                dsdsGroup.addElement(dsdGroup);
            }
        }
        metaRoot.addElement(dsdsGroup);
    }

    private static String getNonNullString(String s) {
        return s != null ? s : "";
    }

    private void addDatasetAnnotationsToProduct(final Product product) throws IOException {
        Debug.assertNotNull(productFile);
        Debug.assertNotNull(product);
        final MetadataElement metaRoot = product.getMetadataRoot();
        final String[] datasetNames = productFile.getValidDatasetNames();
        for (String datasetName : datasetNames) {
            final DSD dsd = productFile.getDSD(datasetName);
            final char dsdType = dsd.getDatasetType();
            if (dsdType == EnvisatConstants.DS_TYPE_ANNOTATION
                    || dsdType == EnvisatConstants.DS_TYPE_GLOBAL_ANNOTATION) {
                final RecordReader recordReader = productFile.getRecordReader(datasetName);
                MetadataElement element = createMetadataElement(datasetName, recordReader);
                metaRoot.addElement(element);
            }
        }
    }

    static MetadataElement createMetadataElement(String datasetName, RecordReader recordReader) throws IOException {
        final int numRecords = recordReader.getNumRecords();
        if (numRecords > 1) {
            return createMetadataTableGroup(datasetName, recordReader);
        } else if (numRecords == 1) {
            return createDatasetTable(datasetName, recordReader);
        }
        return null;
    }

    private TiePointGrid createTiePointGrid(BandLineReader bandLineReader) throws IOException {
        BandInfo bandInfo = bandLineReader.getBandInfo();
        String bandName = bandLineReader.getBandName();
        int gridWidth = bandLineReader.getRasterWidth();
        int gridHeight = bandLineReader.getRasterHeight();
        int pixelDataType = bandLineReader.getPixelDataField().getDataType();
        int tiePointIndex = 0;
        double scalingOffset = bandInfo.getScalingOffset();
        double scalingFactor = bandInfo.getScalingFactor();
        float[] tiePoints = new float[gridWidth * gridHeight];
        for (int y = 0; y < gridHeight; y++) {
            bandLineReader.readLineRecord(y);
            if (pixelDataType == ProductData.TYPE_INT8) {
                byte[] pixelData = (byte[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = (float) (scalingOffset + scalingFactor * pixelData[x]);
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_UINT8) {
                byte[] pixelData = (byte[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = (float) (scalingOffset + scalingFactor * (pixelData[x] & 0xff));
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_INT16) {
                short[] pixelData = (short[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = (float) (scalingOffset + scalingFactor * pixelData[x]);
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_UINT16) {
                short[] pixelData = (short[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = (float) (scalingOffset + scalingFactor * (pixelData[x] & 0xffff));
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_INT32) {
                int[] pixelData = (int[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = (float) (scalingOffset + scalingFactor * pixelData[x]);
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_UINT32) {
                int[] pixelData = (int[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = (float) (scalingOffset + scalingFactor * (pixelData[x] & 0xffffffffL));
                    tiePointIndex++;
                }
            } else if (pixelDataType == ProductData.TYPE_FLOAT32) {
                float[] pixelData = (float[]) bandLineReader.getPixelDataField().getElems();
                if (getProductFile().storesPixelsInChronologicalOrder()) {
                    ArrayUtils.swapArray(pixelData);
                }
                for (int x = 0; x < gridWidth; x++) {
                    tiePoints[tiePointIndex] = (float) (scalingOffset + scalingFactor * pixelData[x]);
                    tiePointIndex++;
                }
            } else {
                throw new IllegalFileFormatException("unhandled tie-point data type"); /*I18N*/
            }
        }
        double offsetX = getProductFile().getTiePointGridOffsetX(gridWidth);
        double offsetY = getProductFile().getTiePointGridOffsetY(gridWidth);
        double subSamplingX = getProductFile().getTiePointSubSamplingX(gridWidth);
        double subSamplingY = getProductFile().getTiePointSubSamplingY(gridWidth);

        final TiePointGrid tiePointGrid = createTiePointGrid(bandName,
                gridWidth,
                gridHeight,
                offsetX,
                offsetY,
                subSamplingX,
                subSamplingY,
                tiePoints);
        if (bandInfo.getPhysicalUnit() != null) {
            tiePointGrid.setUnit(bandInfo.getPhysicalUnit());
        }
        if (bandInfo.getDescription() != null) {
            tiePointGrid.setDescription(bandInfo.getDescription());
        }
        return tiePointGrid;
    }

    static MetadataElement createDatasetTable(String name, RecordReader recordReader) throws IOException {
        Debug.assertTrue(name != null);
        Debug.assertTrue(recordReader != null);

        Record record = recordReader.readRecord();
        return createMetadataGroup(name, record);
    }

    static MetadataElement createMetadataTableGroup(String name, RecordReader recordReader) throws IOException {
        Debug.assertTrue(name != null);
        Debug.assertTrue(recordReader != null);

        MetadataElement metadataTableGroup = new MetadataElement(name);
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < recordReader.getNumRecords(); i++) {
            Record record = recordReader.readRecord(i);
            sb.setLength(0);
            sb.append(name);
            sb.append('.');
            sb.append(i + 1);
            metadataTableGroup.addElement(createMetadataGroup(sb.toString(), record));
        }

        return metadataTableGroup;
    }

    static MetadataElement createMetadataGroup(String name, Record record) {
        Debug.assertNotNullOrEmpty(name);
        Debug.assertNotNull(record);

        MetadataElement metadataGroup = new MetadataElement(name);

        for (int i = 0; i < record.getNumFields(); i++) {
            Field field = record.getFieldAt(i);

            String description = field.getInfo().getDescription();
            if (description != null) {
                if ("Spare".equalsIgnoreCase(description)) {
                    continue;
                }
            }

            MetadataAttribute attribute = new MetadataAttribute(field.getName(), field.getData(), true);
            if (field.getInfo().getPhysicalUnit() != null) {
                attribute.setUnit(field.getInfo().getPhysicalUnit());
            }
            if (description != null) {
                attribute.setDescription(field.getInfo().getDescription());
            }

            metadataGroup.addAttribute(attribute);
        }

        return metadataGroup;
    }

    /**
     * Used by the {@link #createTiePointGrid(String, int, int, double, double, double, double, float[]) createTiePointGrid} method in order to determine
     * the discontinuity mode for angle tie-point grids.
     * <p>The default implementation returns {@link TiePointGrid#DISCONT_AT_180} for
     * the names "lon", "long" or "longitude" ignoring letter case,
     * {@link TiePointGrid#DISCONT_NONE} otherwise.
     *
     * @param name the grid name
     * @return the discontinuity mode, always one of {@link TiePointGrid#DISCONT_NONE}, {@link TiePointGrid#DISCONT_AT_180} and {@link TiePointGrid#DISCONT_AT_360}.
     */
    @Override
    protected int getGridDiscontinutity(String name) {
        if (name.equalsIgnoreCase(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME) ||
                name.equalsIgnoreCase(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME)) {
            return TiePointGrid.DISCONT_AT_360;
        } else if (name.equalsIgnoreCase(EnvisatConstants.AATSR_SUN_AZIMUTH_NADIR_DS_NAME) ||
                name.equalsIgnoreCase(EnvisatConstants.AATSR_VIEW_AZIMUTH_NADIR_DS_NAME) ||
                name.equalsIgnoreCase(EnvisatConstants.AATSR_SUN_AZIMUTH_FWARD_DS_NAME) ||
                name.equalsIgnoreCase(EnvisatConstants.AATSR_VIEW_AZIMUTH_FWARD_DS_NAME)) {
            return TiePointGrid.DISCONT_AT_180;
        } else {
            return TiePointGrid.DISCONT_NONE;
        }
    }

    static double getResolutionInKilometers(String productTypeName) {
        switch (productTypeName) {
            case EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_FRS_L1B_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_FSG_L1B_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_FRG_L1B_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_FR_L2_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_FRS_L2_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_FSG_L2_PRODUCT_TYPE_NAME:
                return EnvisatConstants.MERIS_FR_PX_SIZE_IN_KM;

            case EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_RRG_L1B_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_RR_L2_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_RRC_L2_PRODUCT_TYPE_NAME:
            case EnvisatConstants.MERIS_RRV_L2_PRODUCT_TYPE_NAME:
                return EnvisatConstants.MERIS_RR_PX_SIZE_IN_KM;

            case EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME:
            case EnvisatConstants.AATSR_L2_NR_PRODUCT_TYPE_NAME:
            case EnvisatConstants.AT1_L1B_TOA_PRODUCT_TYPE_NAME:
            case EnvisatConstants.AT1_L2_NR_PRODUCT_TYPE_NAME:
            case EnvisatConstants.AT2_L1B_TOA_PRODUCT_TYPE_NAME:
            case EnvisatConstants.AT2_L2_NR_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ATSR_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_APG_PRODUCT_TYPE_NAME:
            case EnvisatConstants.ASAR_L1B_IMG_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_xxG_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_APP_PRODUCT_TYPE_NAME:
            case EnvisatConstants.ASAR_L1B_IMP_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_xxP_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_AP_BP_PRODUCT_TYPE_NAME:
            case EnvisatConstants.ASAR_L1B_IM_BP_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_BP_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_IMM_PRODUCT_TYPE_NAME:
            case EnvisatConstants.ASAR_L1B_APM_PRODUCT_TYPE_NAME:
            case EnvisatConstants.ASAR_L1B_WSM_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_xxM_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_APS_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_APS_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_IMS_PRODUCT_TYPE_NAME:
            case EnvisatConstants.ASAR_L1B_WSS_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_xxS_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_WS_BP_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_WS_BP_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_WVI_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_WVI_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_WVS_PRODUCT_TYPE_NAME:
            case EnvisatConstants.ASAR_L2_WVW_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_WVS_PX_SIZE_IN_KM;

            case EnvisatConstants.ASAR_L1B_GM1_PRODUCT_TYPE_NAME:
            case EnvisatConstants.ASAR_L1B_GMB_PRODUCT_TYPE_NAME:
                return EnvisatConstants.ASAR_GM1_PX_SIZE_IN_KM;

            case EnvisatConstants.SAR_IM__BP_PRODUCT_TYPE_NAME:
                return EnvisatConstants.SAR_IM_BP_PX_SIZE_IN_KM;

            case EnvisatConstants.SAR_IMG_1P_PRODUCT_TYPE_NAME:
            case EnvisatConstants.SAR_IMP_1P_PRODUCT_TYPE_NAME:
            case EnvisatConstants.SAR_IMS_1P_PRODUCT_TYPE_NAME:
                return EnvisatConstants.SAR_IMx_PX_SIZE_IN_KM;

            case EnvisatConstants.SAR_IMM_1P_PRODUCT_TYPE_NAME:
                return EnvisatConstants.SAR_IMM_PX_SIZE_IN_KM;

            default:
                throw new IllegalStateException("undefined product resolution for type: " + productTypeName);
        }
    }
}
