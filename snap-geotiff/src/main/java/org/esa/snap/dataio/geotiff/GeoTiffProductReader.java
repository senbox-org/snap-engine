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
package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.GeoTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFRenderedImage;
import org.apache.commons.lang.StringUtils;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.dimap.DimapProductHelpers;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GcpGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.geotiff.EPSGCodes;
import org.esa.snap.core.util.geotiff.GeoTIFFCodes;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.dataio.ImageRegistryUtils;
import org.esa.snap.dataio.geotiff.internal.GeoKeyEntry;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

import javax.imageio.spi.ImageInputStreamSpi;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class GeoTiffProductReader extends AbstractProductReader {

    // non standard ASCII tag (code 42112) to extract GDAL metadata,
    // see https://gdal.org/drivers/raster/gtiff.html#metadata:
    private static final int TIFFTAG_GDAL_METADATA = 42112;

    private ImageInputStreamSpi imageInputStreamSpi;
    private GeoTiffImageReader geoTiffImageReader;

    public GeoTiffProductReader(ProductReaderPlugIn readerPlugIn) {
        this(readerPlugIn, ImageRegistryUtils.registerImageInputStreamSpi());
    }

    public GeoTiffProductReader(ProductReaderPlugIn readerPlugIn, ImageInputStreamSpi imageInputStreamSpi) {
        super(readerPlugIn);

        this.imageInputStreamSpi = imageInputStreamSpi;
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        if (this.geoTiffImageReader != null) {
            throw new IllegalStateException("There is already an image reader.");
        }
        boolean success = false;
        try {
            Object productInput = super.getInput();

            Path productPath = null;
            if (productInput instanceof String) {
                productPath = new File((String) productInput).toPath();
                this.geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(productPath);
            } else if (productInput instanceof File) {
                productPath = ((File) productInput).toPath();
                this.geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(productPath);
            } else if (productInput instanceof Path) {
                productPath = (Path) productInput;
                this.geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(productPath);
            } else if (productInput instanceof InputStream) {
                this.geoTiffImageReader = new GeoTiffImageReader((InputStream) productInput, null);
            } else {
                throw new IllegalArgumentException("Unknown input '" + productInput + "'.");
            }

            String defaultProductName = null;
            if (productPath != null) {
                defaultProductName = FileUtils.getFilenameWithoutExtension(productPath.getFileName().toString());
            }
            Product product = readProduct(this.geoTiffImageReader, defaultProductName);
            if (productPath != null) {
                product.setFileLocation(productPath.toFile());
            }

            success = true;

            return product;
        } catch (RuntimeException | IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException(exception);
        } finally {
            if (!success) {
                closeResources();
            }
        }
    }

    @Override
    public void close() throws IOException {
        super.close();

        closeResources();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm)
                                          throws IOException {

        if (this.geoTiffImageReader == null) {
            throw new NullPointerException("The image reader is null.");
        }

        DefaultMultiLevelImage defaultMultiLevelImage = (DefaultMultiLevelImage)destBand.getSourceImage();
        GeoTiffMultiLevelSource geoTiffMultiLevelSource = (GeoTiffMultiLevelSource)defaultMultiLevelImage.getSource();
        Raster data;
        synchronized (this.geoTiffImageReader) {
            data = this.geoTiffImageReader.readRect(geoTiffMultiLevelSource.isGlobalShifted180(), sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destOffsetX, destOffsetY, destWidth, destHeight);
        }
        DataBuffer dataBuffer = data.getDataBuffer();
        SampleModel sampleModel = data.getSampleModel();
        int dataBufferType = dataBuffer.getDataType();
        int bandIndex = geoTiffMultiLevelSource.getBandIndex();
        boolean isInteger = (dataBufferType == DataBuffer.TYPE_SHORT || dataBufferType == DataBuffer.TYPE_USHORT || dataBufferType == DataBuffer.TYPE_INT);
        if (isInteger && destBuffer.getElems() instanceof int[]) {
            sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), bandIndex, (int[]) destBuffer.getElems(), dataBuffer);
        } else if (dataBufferType == DataBuffer.TYPE_FLOAT && destBuffer.getElems() instanceof float[]) {
            sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), bandIndex, (float[]) destBuffer.getElems(), dataBuffer);
        } else if (dataBufferType == DataBuffer.TYPE_BYTE && destBuffer.getElems() instanceof byte[]) {
            int[] dArray = new int[destWidth * destHeight];
            sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), bandIndex, dArray, dataBuffer);
            for (int i = 0; i < dArray.length; i++) {
                destBuffer.setElemIntAt(i, dArray[i]);
            }
        } else {
            double[] dArray = new double[destWidth * destHeight];
            sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), bandIndex, dArray, dataBuffer);
            if (destBuffer.getElems() instanceof double[]) {
                System.arraycopy(dArray, 0, destBuffer.getElems(), 0, dArray.length);
            } else {
                for (int i = 0; i < dArray.length; i++) {
                    destBuffer.setElemDoubleAt(i, dArray[i]);
                }
            }
        }
    }

    private void closeResources() {
        try {
            if (this.geoTiffImageReader != null) {
                this.geoTiffImageReader.close();
                this.geoTiffImageReader = null;
            }
        } finally {
            if (this.imageInputStreamSpi != null) {
                ImageRegistryUtils.deregisterImageInputStreamSpi(this.imageInputStreamSpi);
                this.imageInputStreamSpi = null;
            }
        }
        System.gc();
    }

    public Product readProduct(GeoTiffImageReader geoTiffImageReader, String productName) throws Exception {
        if (geoTiffImageReader == null) {
            throw new NullPointerException("The image reader is null.");
        }
        final ProductSubsetDef subsetDef = getSubsetDef();
        final int imageWidth = geoTiffImageReader.getImageWidth();
        final int imageHeight = geoTiffImageReader.getImageHeight();

        Rectangle productBounds;
        if (subsetDef == null || subsetDef.getSubsetRegion() == null) {
            productBounds = new Rectangle(0, 0, imageWidth, imageHeight);
        } else {
            final GeoCoding geoCoding = GeoTiffProductReader.readGeoCoding(geoTiffImageReader, null);
            productBounds = subsetDef.getSubsetRegion().computeProductPixelRegion(geoCoding, imageWidth, imageHeight, false);
        }

        return readProduct(geoTiffImageReader, productName, productBounds);
    }

    public Product readProduct(GeoTiffImageReader geoTiffImageReader, String defaultProductName, Rectangle productBounds) throws Exception {
        return readProduct(geoTiffImageReader, defaultProductName, productBounds, null);
    }

    public Product readProduct(GeoTiffImageReader geoTiffImageReader, String defaultProductName, Rectangle productBounds, Double noDataValue) throws Exception {
        if (geoTiffImageReader == null) {
            throw new NullPointerException("The image reader is null.");
        }
        if (productBounds.isEmpty()) {
            throw new IllegalStateException("Empty product bounds.");
        }
        Dimension defaultImageSize = geoTiffImageReader.validateArea(productBounds);

        TIFFImageMetadata imageMetadata = geoTiffImageReader.getImageMetadata();
        TiffFileInfo tiffInfo = new TiffFileInfo(imageMetadata.getRootIFD());
        SampleModel sampleModel;
        Product product = buildProductFromDimapHeader(tiffInfo, productBounds.width, productBounds.height);
        if (product == null) {            // without DIMAP header
            TIFFRenderedImage baseImage = geoTiffImageReader.getBaseImage();
            product = buildProductWithoutDimapHeader(defaultProductName, tiffInfo, baseImage, productBounds.width, productBounds.height);
            sampleModel = baseImage.getSampleModel();
            boolean createIndexedImageInfo = (tiffInfo.containsField(BaselineTIFFTagSet.TAG_COLOR_MAP) && baseImage.getColorModel() instanceof IndexColorModel);
            if (createIndexedImageInfo) {
                for (int i = 0; i < product.getNumBands(); i++) {
                    Band band = product.getBandAt(i);
                    band.setImageInfo(buildIndexedImageInfo(product, baseImage, band));
                }
            }
        } else {
            sampleModel = geoTiffImageReader.getBaseImage().getSampleModel();
        }

        ProductSubsetDef subsetDef = getSubsetDef();
        boolean isGlobalShifted180 = false;
        if (tiffInfo.isGeotiff()) {
            Rectangle subsetRegion = null;
            if (productBounds.x != 0 || productBounds.y != 0 || productBounds.width != defaultImageSize.width || productBounds.height != defaultImageSize.height) {
                subsetRegion = productBounds;
            }
            isGlobalShifted180 = applyGeoCoding(tiffInfo, imageMetadata, defaultImageSize.width, defaultImageSize.height, product, subsetRegion);
        }

        if (subsetDef == null || !subsetDef.isIgnoreMetadata()) {
            // add the metadata to the product
            TiffTagToMetadataConverter.addTiffTagsToMetadata(imageMetadata, tiffInfo, product.getMetadataRoot());
        } else {
            // do not add the metadata to the product and remove the existing metadata
            product.getMetadataRoot().dispose();
            product.getMetadataRoot().setModified(false);
        }

        Dimension preferredMosaicTileSize = computePreferredMosaicTileSize(isGlobalShifted180, product.getSceneRasterSize());
        product.setPreferredTileSize(preferredMosaicTileSize);
        product.setProductReader(this);

        GeoCoding bandGeoCoding = buildBandGeoCoding(product.getSceneGeoCoding(), productBounds.width, productBounds.height);
        AffineTransform2D imageToModelTransform = buildBandImageToModelTransform(productBounds.width, productBounds.height);
        for (int i = 0, bandIndex = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            boolean addBand = (subsetDef == null || subsetDef.isNodeAccepted(band.getName()));
            if (!(band instanceof VirtualBand || band instanceof FilterBand)) {
                // the band is not virtual
                if (addBand) {
                    if (band.getRasterWidth() != productBounds.width) {
                        throw new IllegalStateException("The band width " + band.getRasterWidth() + " is not equal with the product with " + productBounds.width + ".");
                    }
                    if (band.getRasterHeight() != productBounds.height) {
                        throw new IllegalStateException("The band height " + band.getRasterHeight() + " is not equal with the product height " + productBounds.height + ".");
                    }
                    if (bandGeoCoding != null) {
                        band.setGeoCoding(bandGeoCoding);
                    }
                    if (imageToModelTransform != null) {
                        band.setImageToModelTransform(imageToModelTransform);
                    }
                    if (bandIndex >= sampleModel.getNumBands()) {
                        throw new IllegalStateException("The band index " + bandIndex + " must be < " + sampleModel.getNumBands() + ". The band name is '" + band.getName() + "'.");
                    }
                    int dataBufferType = ImageManager.getDataBufferType(band.getDataType()); // sampleModel.getDataType();
                    GeoTiffMultiLevelSource multiLevelSource = new GeoTiffMultiLevelSource(geoTiffImageReader, dataBufferType, productBounds, preferredMosaicTileSize,
                                                                                           bandIndex, band.getGeoCoding(), isGlobalShifted180, noDataValue);
                    band.setSourceImage(new DefaultMultiLevelImage(multiLevelSource));
                }
                bandIndex++; // increment the band index for non virtual bands
            }
            if (!addBand) {
                // the product band is not accepted
                if (product.removeBand(band)) {
                    i--;
                } else {
                    throw new IllegalStateException("Failed to remove the band '" + band.getName()+"' at index " + i + " from the product.");
                }
            }
        }

        return product;
    }

    protected AffineTransform2D buildBandImageToModelTransform(int productWidth, int productHeight) {
        return null;
    }

    protected GeoCoding buildBandGeoCoding(GeoCoding productGeoCoding, int productWidth, int productHeight) throws Exception {
        return productGeoCoding;
    }

    private static Product buildProductFromDimapHeader(TiffFileInfo tiffInfo, int productWidth, int productHeight) throws IOException {
        TIFFField tagNumberField = tiffInfo.getField(Utils.PRIVATE_BEAM_TIFF_TAG_NUMBER);
        Product product = null;
        if (tagNumberField != null && tagNumberField.getType() == TIFFTag.TIFF_ASCII) {
            String tagNumberText = tagNumberField.getAsString(0).trim();
            if (tagNumberText.contains("<Dimap_Document")) { // with DIMAP header
                Dimension productSize = new Dimension(productWidth, productHeight);
                product = buildProductFromDimapHeader(tagNumberText, productSize);
            }
        }
        return product;
    }

    private static Product buildProductWithoutDimapHeader(String defaultProductName, TiffFileInfo tiffInfo, TIFFRenderedImage baseImage, int productWidth, int productHeight)
                                                          throws ParserConfigurationException, SAXException, IOException {

        String productName = null;
        if (tiffInfo.containsField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION)) {
            TIFFField tagImageDescriptionField = tiffInfo.getField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION);
            productName = tagImageDescriptionField.getAsString(0).trim();
        }
        if (StringUtils.isBlank(productName) && defaultProductName != null) {
            productName = defaultProductName;
        } else {
            productName = "geotiff";
        }

        Product product = new Product(productName, GeoTiffProductReaderPlugIn.FORMAT_NAMES[0], productWidth, productHeight);

        Band[] bands = buildBands(tiffInfo, baseImage, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        for (int i = 0; i < bands.length; i++) {
            product.addBand(bands[i]);
        }

        return product;
    }

    private static Band[] buildBands(TiffFileInfo tiffInfo, TIFFRenderedImage baseImage, int productWidth, int productHeight)
                                     throws IOException, ParserConfigurationException, SAXException {

        SampleModel sampleModel = baseImage.getSampleModel();
        int numBands = sampleModel.getNumBands();
        int productDataType = ImageManager.getProductDataType(sampleModel.getDataType());

        // check if GDAL metadata exists. If so, extract all band info and add to bands.
        // todo: so far this has been implemented and tested for PROBA-V S* GeoTiff products only (SIIITBX-85),
        //  for these we get now bands RED, NIR, BLUE, SWIR (or NDVI) instead of band_1,..,band_4.
        //  Explore if this can be generalized for further GeoTiff products.
        TIFFField gdalMetadataTiffField = tiffInfo.getField(TIFFTAG_GDAL_METADATA);
        if (gdalMetadataTiffField != null) {
            String gdalMetadataXmlString = gdalMetadataTiffField.getAsString(0);
            Band[] bandsFromGdalMetadata = Utils.setupBandsFromGdalMetadata(gdalMetadataXmlString, productDataType, productWidth, productHeight);
            if (bandsFromGdalMetadata.length == numBands) {
                return bandsFromGdalMetadata;
            }
        }

        Band[] bands = new Band[numBands];
        for (int i = 0; i < numBands; i++) {
            String bandName = String.format("band_%d", i + 1);
            bands[i] = new Band(bandName, productDataType, productWidth, productHeight);
        }

        return bands;
    }

    private static Product buildProductFromDimapHeader(String tagNumberText, Dimension productSize) throws IOException {
        Product product = null;
        InputStream inputStream = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            inputStream = new ByteArrayInputStream(tagNumberText.getBytes());
            Document document = new DOMBuilder().build(builder.parse(inputStream));
            product = DimapProductHelpers.createProduct(document, GeoTiffProductReaderPlugIn.FORMAT_NAMES[0], productSize);
            // remove the geo coding
            product.setSceneGeoCoding(null);
            TiePointGrid[] pointGrids = product.getTiePointGrids();
            for (TiePointGrid pointGrid : pointGrids) {
                product.removeTiePointGrid(pointGrid);
            }
        } catch (ParserConfigurationException | SAXException ignore) {
            // ignore if it can not be read
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return product;
    }

    private static Dimension computePreferredMosaicTileSize(boolean isGlobalShifted180, Dimension productSize) {
        if (isGlobalShifted180) {
            return new Dimension(productSize.width, productSize.height);
        }
        return ImageUtils.computePreferredMosaicTileSize(productSize.width, productSize.height, 1);
    }

    private static ImageInfo buildIndexedImageInfo(Product product, TIFFRenderedImage baseImage, Band band) {
        final IndexColorModel colorModel = (IndexColorModel) baseImage.getColorModel();
        final IndexCoding indexCoding = new IndexCoding("color_map");
        final int colorCount = colorModel.getMapSize();
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[colorCount];
        for (int j = 0; j < colorCount; j++) {
            final String name = String.format("I%3d", j);
            indexCoding.addIndex(name, j, "");
            points[j] = new ColorPaletteDef.Point(j, new Color(colorModel.getRGB(j)), name);
        }
        product.getIndexCodingGroup().add(indexCoding);
        band.setSampleCoding(indexCoding);

        return new ImageInfo(new ColorPaletteDef(points, points.length));
    }

    private static boolean applyGeoCoding(TiffFileInfo info, TIFFImageMetadata metadata, int defaultImageWidth, int defaultImageHeight, Product product, Rectangle subsetRegion)
                                          throws Exception {

        boolean isGlobalShifted180 = false;
        if (info.containsField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT)) {
            double[] tiePoints = info.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT).getAsDoubles();
            int rasterWidth = defaultImageWidth; //product.getSceneRasterWidth();
            boolean isGlobal = isGlobal(rasterWidth, info);
            // check if we have a global geographic lat/lon with lon from 0..360 instead of -180..180
            double deltaX = Math.ceil(360.0d / rasterWidth);
            if (isGlobal && tiePoints.length == 6 && Math.abs(tiePoints[3]) < deltaX) {
                // e.g. tiePoints[3] = -0.5, productWidth=722 --> we have a lon range of 361 which should start
                // at or near -180 but not at zero
                isGlobalShifted180 = true;
                TiePointGeoCoding tiePointGeoCoding = buildGlobalShiftedTiePointGeoCoding(info, defaultImageWidth, defaultImageHeight, subsetRegion);
                if (tiePointGeoCoding != null) {
                    product.getTiePointGridGroup().add(tiePointGeoCoding.getLatGrid());
                    product.getTiePointGridGroup().add(tiePointGeoCoding.getLonGrid());
                    product.setSceneGeoCoding(tiePointGeoCoding);
                }
            } else if (canCreateTiePointGeoCoding(tiePoints)) {
                String[] names = Utils.findSuitableLatLonNames(product);
                TiePointGeoCoding tiePointGeoCoding = buildTiePointGeoCoding(info, tiePoints, names, subsetRegion);
                product.addTiePointGrid(tiePointGeoCoding.getLatGrid());
                product.addTiePointGrid(tiePointGeoCoding.getLonGrid());
                product.setSceneGeoCoding(tiePointGeoCoding);
            } else if (canCreateGcpGeoCoding(tiePoints)) {
                applyGcpGeoCoding(info, tiePoints, product);
            }
        }
        if (product.getSceneGeoCoding() == null) {
            CrsGeoCoding crsGeoCoding = GeoTiffImageReader.buildGeoCoding(metadata, defaultImageWidth, defaultImageHeight, subsetRegion);
            product.setSceneGeoCoding(crsGeoCoding);
        }
        return isGlobalShifted180;
    }

    private static TiePointGeoCoding buildGlobalShiftedTiePointGeoCoding(TiffFileInfo info, int productWidth, int productHeight, Rectangle subsetRegion) {
        final TIFFField pixelScaleField = info.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
        final TIFFField tiePointField = info.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT);

        if (pixelScaleField != null && tiePointField != null) {
            double[] pixelScales = pixelScaleField.getAsDoubles();
            double[] tiePoints = tiePointField.getAsDoubles();

            if (isPixelScaleValid(pixelScales)) {
                // create new TiePointGeocoding based on given info:
                //int gridWidth = product.getSceneRasterWidth(); // productWidth
                //int gridHeight = product.getSceneRasterHeight(); // productHeight

                float[] latPoints = new float[productWidth * productHeight];
                tiePoints[4] = Math.min(tiePoints[4], 90.0);
                for (int j = 0; j < productHeight; j++) {
                    for (int i = 0; i < productWidth; i++) {
                        latPoints[j * productWidth + i] = (float) (tiePoints[4] - j * pixelScales[1]);
                    }
                }

                float[] lonPoints = new float[productWidth * productHeight];
                for (int j = 0; j < productHeight; j++) {
                    for (int i = 0; i < productWidth; i++) {
                        lonPoints[j * productWidth + i] = (float) (i * pixelScales[0] - 180.0f);
                    }
                }

                TiePointGrid latGrid = new TiePointGrid("latGrid", productWidth, productHeight, 0.0, 0.0, 1.0, 1.0, latPoints);
                TiePointGrid lonGrid = new TiePointGrid("lonGrid", productWidth, productHeight, 0.0, 0.0, 1.0, 1.0, lonPoints);
                if (subsetRegion != null) {
                    ProductSubsetDef productSubsetDef = new ProductSubsetDef();
                    productSubsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
                    productSubsetDef.setSubSampling(1, 1);
                    lonGrid = TiePointGrid.createSubset(lonGrid, productSubsetDef);
                    latGrid = TiePointGrid.createSubset(latGrid, productSubsetDef);
                }

                return new TiePointGeoCoding(latGrid, lonGrid);
            }
        }
        return null;
    }

    private static boolean isGlobal(int productWidth, TiffFileInfo info) {
        TIFFField pixelScaleField = info.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
        if (pixelScaleField != null) {
            double[] pixelScales = pixelScaleField.getAsDoubles();

            if (isPixelScaleValid(pixelScales)) {
                double widthInDegree = pixelScales[0] * productWidth;
                return (Math.ceil(widthInDegree) >= 360);
            }
        }
        return false;
    }

    private static boolean isPixelScaleValid(double[] pixelScales) {
        return pixelScales != null &&
                !Double.isNaN(pixelScales[0]) && !Double.isInfinite(pixelScales[0]) &&
                !Double.isNaN(pixelScales[1]) && !Double.isInfinite(pixelScales[1]);
    }

    public static GeoCoding readGeoCoding(Path productPath, Rectangle subsetRegion) throws Exception {
        try (GeoTiffImageReader geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(productPath)) {
            return readGeoCoding(geoTiffImageReader, subsetRegion);
        }
    }

    public static GeoCoding readGeoCoding(GeoTiffImageReader geoTiffImageReader, Rectangle subsetRegion) throws Exception {
        final TIFFImageMetadata imageMetadata = geoTiffImageReader.getImageMetadata();
        final TiffFileInfo tiffInfo = new TiffFileInfo(imageMetadata.getRootIFD());
        if (tiffInfo.isGeotiff()) {
            final int productWidth = geoTiffImageReader.getImageWidth();
            final int productHeight = geoTiffImageReader.getImageHeight();
            final Product product = new Product("GeoTiff", GeoTiffProductReaderPlugIn.FORMAT_NAMES[0], productWidth, productHeight);
            applyGeoCoding(tiffInfo, imageMetadata, productWidth, productHeight, product, subsetRegion);
            return product.getSceneGeoCoding();
        }
        return null;
    }

    public static Product readMetadataProduct(Path productPath, boolean readGeoCoding) throws Exception {
        try (GeoTiffImageReader geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(productPath)) {
            return GeoTiffProductReader.readMetadataProduct(geoTiffImageReader, readGeoCoding);
        }
    }

    public static Product readMetadataProduct(GeoTiffImageReader geoTiffImageReader, boolean readGeoCoding) throws Exception {
        int productWidth = geoTiffImageReader.getImageWidth();
        int productHeight = geoTiffImageReader.getImageHeight();
        TIFFImageMetadata imageMetadata = geoTiffImageReader.getImageMetadata();
        TiffFileInfo tiffInfo = new TiffFileInfo(imageMetadata.getRootIFD());
        Product product = GeoTiffProductReader.buildProductFromDimapHeader(tiffInfo, productWidth, productHeight);
        if (product == null) {            // without DIMAP header
            product = GeoTiffProductReader.buildProductWithoutDimapHeader(null, tiffInfo, geoTiffImageReader.getBaseImage(), productWidth, productHeight);
        }
        if (readGeoCoding && tiffInfo.isGeotiff()) {
            applyGeoCoding(tiffInfo, imageMetadata, productWidth, productHeight, product, null);
        }
        return product;
    }

    private static TiePointGeoCoding buildTiePointGeoCoding(TiffFileInfo info, double[] tiePoints, String[] names, Rectangle subsetRegion) {
        final SortedSet<Double> xSet = new TreeSet<>();
        final SortedSet<Double> ySet = new TreeSet<>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        final double xMin = xSet.first();
        final double xMax = xSet.last();
        final double xDiff = (xMax - xMin) / (xSet.size() - 1);
        final double yMin = ySet.first();
        final double yMax = ySet.last();
        final double yDiff = (yMax - yMin) / (ySet.size() - 1);

        final int width = xSet.size();
        final int height = ySet.size();

        int idx = 0;
        final Map<Double, Integer> xIdx = new HashMap<>();
        for (Double val : xSet) {
            xIdx.put(val, idx);
            idx++;
        }
        idx = 0;
        final Map<Double, Integer> yIdx = new HashMap<>();
        for (Double val : ySet) {
            yIdx.put(val, idx);
            idx++;
        }

        final float[] lats = new float[width * height];
        final float[] lons = new float[width * height];

        for (int i = 0; i < tiePoints.length; i += 6) {
            final int idxX = xIdx.get(tiePoints[i + 0]);
            final int idxY = yIdx.get(tiePoints[i + 1]);
            final int arrayIdx = idxY * width + idxX;
            lons[arrayIdx] = (float) tiePoints[i + 3];
            lats[arrayIdx] = (float) tiePoints[i + 4];
        }

        SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        Datum datum = getDatum(geoKeyEntries);

        TiePointGrid latGrid = new TiePointGrid(names[0], width, height, xMin, yMin, xDiff, yDiff, lats);
        TiePointGrid lonGrid = new TiePointGrid(names[1], width, height, xMin, yMin, xDiff, yDiff, lons);
        if (subsetRegion != null) {
            ProductSubsetDef productSubsetDef = new ProductSubsetDef();
            productSubsetDef.setSubsetRegion(new PixelSubsetRegion(subsetRegion, 0));
            productSubsetDef.setSubSampling(1, 1);
            lonGrid = TiePointGrid.createSubset(lonGrid, productSubsetDef);
            latGrid = TiePointGrid.createSubset(latGrid, productSubsetDef);
        }

        return new TiePointGeoCoding(latGrid, lonGrid, datum);
    }

    private static boolean canCreateGcpGeoCoding(final double[] tiePoints) {
        int numTiePoints = tiePoints.length / 6;

        // check if positions are valid
        for (int i = 0; i < numTiePoints; i++) {
            final int offset = i * 6;

            final float x = (float) tiePoints[offset + 0];
            final float y = (float) tiePoints[offset + 1];
            final float lon = (float) tiePoints[offset + 3];
            final float lat = (float) tiePoints[offset + 4];

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(lon) || Double.isNaN(lat)) {
                return false;
            }
            final PixelPos pixelPos = new PixelPos(x, y);
            final GeoPos geoPos = new GeoPos(lat, lon);

            if (!pixelPos.isValid() || !geoPos.isValid()) {
                return false;
            }
        }

        if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL3.getTermCountP()) {
            return true;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL2.getTermCountP()) {
            return true;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL1.getTermCountP()) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean canCreateTiePointGeoCoding(final double[] tiePoints) {
        if ((tiePoints.length / 6) <= 1) {
            return false;
        }
        for (double tiePoint : tiePoints) {
            if (Double.isNaN(tiePoint)) {
                return false;
            }
        }
        final SortedSet<Double> xSet = new TreeSet<>();
        final SortedSet<Double> ySet = new TreeSet<>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        return isEquiDistance(xSet) && isEquiDistance(ySet);
    }

    private static boolean isEquiDistance(SortedSet<Double> set) {
        final double min = set.first();
        final double max = set.last();
        final double diff = (max - min) / (set.size() - 1);
        final double diff100000 = diff / 100000;
        final double maxDiff = diff + diff100000;
        final double minDiff = diff - diff100000;

        final Double[] values = set.toArray(new Double[set.size()]);
        for (int i = 1; i < values.length; i++) {
            final double currentDiff = values[i] - values[i - 1];
            if (currentDiff > maxDiff || currentDiff < minDiff) {
                return false;
            }
        }
        return true;
    }

    private static void applyGcpGeoCoding(final TiffFileInfo info, final double[] tiePoints, final Product product) {
        int numTiePoints = tiePoints.length / 6;

        final GcpGeoCoding.Method method;
        if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL3.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL3;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL2.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL2;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL1.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL1;
        } else {
            return; // not able to apply GCP geo coding; not enough tie points
        }

        final GcpDescriptor gcpDescriptor = GcpDescriptor.getInstance();
        final ProductNodeGroup<Placemark> gcpGroup = product.getGcpGroup();
        for (int i = 0; i < numTiePoints; i++) {
            final int offset = i * 6;

            final float x = (float) tiePoints[offset + 0];
            final float y = (float) tiePoints[offset + 1];
            final float lon = (float) tiePoints[offset + 3];
            final float lat = (float) tiePoints[offset + 4];

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(lon) || Double.isNaN(lat)) {
                continue;
            }
            final PixelPos pixelPos = new PixelPos(x, y);
            final GeoPos geoPos = new GeoPos(lat, lon);

            final Placemark gcp = Placemark.createPointPlacemark(gcpDescriptor, "gcp_" + i, "GCP_" + i, "", pixelPos, geoPos, product.getSceneGeoCoding());
            gcpGroup.add(gcp);
        }

        final Placemark[] gcps = gcpGroup.toArray(new Placemark[gcpGroup.getNodeCount()]);
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        final int productWidth = product.getSceneRasterWidth();
        final int productHeight = product.getSceneRasterHeight();
        product.setSceneGeoCoding(new GcpGeoCoding(method, gcps, productWidth, productHeight, datum));
    }

    private static Datum getDatum(Map<Integer, GeoKeyEntry> geoKeyEntries) {
        final Datum datum;
        if (geoKeyEntries.containsKey(GeoTIFFCodes.GeographicTypeGeoKey)) {
            final int value = geoKeyEntries.get(GeoTIFFCodes.GeographicTypeGeoKey).getIntValue();
            if (value == EPSGCodes.GCS_WGS_72) {
                datum = Datum.WGS_72;
            } else if (value == EPSGCodes.GCS_WGS_84) {
                datum = Datum.WGS_84;
            } else {
                //@todo if user defined ... make user defined datum
                datum = Datum.WGS_84;
            }
        } else {
            datum = Datum.WGS_84;
        }
        return datum;
    }
}
