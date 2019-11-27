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
import org.esa.snap.core.dataio.MetadataInspector;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
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
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.geotiff.EPSGCodes;
import org.esa.snap.core.util.geotiff.GeoTIFFCodes;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.dataio.FileImageInputStreamSpi;
import org.esa.snap.dataio.geotiff.internal.GeoKeyEntry;
import org.esa.snap.engine_utilities.util.FileSystemUtils;
import org.esa.snap.engine_utilities.util.ZipFileSystemBuilder;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffConstants;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffMetadata2CRSAdapter;
import org.geotools.coverage.grid.io.imageio.geotiff.PixelScale;
import org.geotools.coverage.grid.io.imageio.geotiff.TiePoint;
import org.geotools.factory.Hints;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.matrix.GeneralMatrix;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.xml.sax.SAXException;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeoTiffProductReader extends AbstractProductReader {

    private static final Logger logger = Logger.getLogger(GeoTiffProductReader.class.getName());

    private static final int BUFFER_SIZE = 1024 * 1024;

    // non standard ASCII tag (code 42112) to extract GDAL metadata,
    // see https://gdal.org/drivers/raster/gtiff.html#metadata:
    private static final int TIFFTAG_GDAL_METADATA = 42112;

    private ImageInputStreamSpi imageInputStreamSpi;
    private GeoTiffImageReader geoTiffImageReader;
    private boolean isGlobalShifted180;

    public GeoTiffProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);

        this.imageInputStreamSpi = registerImageInputStreamSpi();
    }

    @Override
    public MetadataInspector getMetadataInspector() {
        return new GeoTiffMetadataInspector();
    }

    @Override
    protected synchronized Product readProductNodesImpl() throws IOException {
        boolean success = false;
        try {
            Object productInputFile = super.getInput();

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Read the GeoTiff product from input '" + productInputFile + "'.");
            }

            Path productPath = null;
            if (productInputFile instanceof String) {
                productPath = new File((String)productInputFile).toPath();
                this.geoTiffImageReader = buildGeoTiffImageReader(productPath);
            } else if (productInputFile instanceof File) {
                productPath = ((File) productInputFile).toPath();
                this.geoTiffImageReader = buildGeoTiffImageReader(productPath);
            } else if (productInputFile instanceof Path) {
                productPath = (Path) productInputFile;
                this.geoTiffImageReader = buildGeoTiffImageReader(productPath);
            } else if (productInputFile instanceof InputStream) {
                this.geoTiffImageReader = new GeoTiffImageReader((InputStream)productInputFile, null);
            } else {
                throw new IllegalArgumentException("Unknown input '"+productInputFile+"'.");
            }

            Product product = readProduct(this.geoTiffImageReader, productPath);
            success = true;
            return product;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            if (!success && this.geoTiffImageReader != null) {
                this.geoTiffImageReader.close();
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();

        try {
            if (this.geoTiffImageReader != null) {
                this.geoTiffImageReader.close();
                this.geoTiffImageReader = null;
            }
        } finally {
            if (this.imageInputStreamSpi != null) {
                IIORegistry.getDefaultInstance().deregisterServiceProvider(this.imageInputStreamSpi);
                this.imageInputStreamSpi = null;
            }
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidthOLD, int sourceHeightOLD, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm)
                                          throws IOException {

        throw new UnsupportedOperationException("Method not implemented");
    }

    Product readProduct(GeoTiffImageReader geoTiffImageReader, Path productPath) throws Exception {
        Rectangle imageBounds = computeImageBounds(geoTiffImageReader.getImageWidth(), geoTiffImageReader.getImageHeight());
        Dimension productSize = new Dimension(imageBounds.width, imageBounds.height);

        TIFFImageMetadata imageMetadata = geoTiffImageReader.getImageMetadata();
        TiffFileInfo tiffInfo = new TiffFileInfo(imageMetadata.getRootIFD());
        TIFFField tagNumberField = tiffInfo.getField(Utils.PRIVATE_BEAM_TIFF_TAG_NUMBER);
        Product product = null;
        if (tagNumberField != null && tagNumberField.getType() == TIFFTag.TIFF_ASCII) {
            String tagNumberText = tagNumberField.getAsString(0).trim();
            if (tagNumberText.contains("<Dimap_Document")) { // with DIMAP header
                product = buildProductFromDimapHeader(tagNumberText, productSize);
            }
        }
        if (product == null) {            // without DIMAP header
            TIFFRenderedImage baseImage = geoTiffImageReader.getBaseImage();
            String productType = getReaderPlugIn().getFormatNames()[0];
            product = buildProductWithoutDimapHeader(productPath, productType, tiffInfo, baseImage, productSize);
            boolean createIndexedImageInfo = (tiffInfo.containsField(BaselineTIFFTagSet.TAG_COLOR_MAP) && baseImage.getColorModel() instanceof IndexColorModel);
            if (createIndexedImageInfo) {
                for (int i = 0; i < product.getNumBands(); i++) {
                    Band band = product.getBandAt(i);
                    band.setImageInfo(createIndexedImageInfo(product, baseImage, band));
                }
            }
        }

        if (tiffInfo.isGeotiff()) {
            applyGeoCoding(tiffInfo, imageMetadata, product);
        }

        Dimension preferredTileSize = computePreferredTiling(this.isGlobalShifted180, geoTiffImageReader, product.getSceneRasterSize());
        product.setPreferredTileSize(preferredTileSize);
        if (productPath != null) {
            product.setFileLocation(productPath.toFile());
        }
        product.setProductReader(this);

        GeoCoding bandGeoCoding = product.getSceneGeoCoding();
        int bandIndex = 0;
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            int dataType = ImageManager.getDataBufferType(band.getDataType());
            GeoTiffMultiLevelSource multiLevelSource = new GeoTiffMultiLevelSource(geoTiffImageReader, dataType, imageBounds, preferredTileSize, bandIndex, bandGeoCoding, this.isGlobalShifted180);
            band.setSourceImage(new DefaultMultiLevelImage(multiLevelSource));
            if (!(band instanceof VirtualBand || band instanceof FilterBand)) {
                bandIndex++;
            }
        }
        TiffTagToMetadataConverter.addTiffTagsToMetadata(imageMetadata, tiffInfo, product.getMetadataRoot());

        return product;
    }

    private Rectangle computeImageBounds(int imageWidth, int imageHeight) {
        Rectangle imageBounds = null;
        if (getSubsetDef() != null) {
            imageBounds = getSubsetDef().getRegion();
            if (imageBounds != null) {
                if (imageBounds.width > imageWidth) {
                    throw new IllegalArgumentException("The visible region width " + imageBounds.width + " cannot be greater than the image width " + imageWidth + ".");
                }
                if (imageBounds.height > imageHeight) {
                    throw new IllegalArgumentException("The visible region height " + imageBounds.height + " cannot be greater than the image height " + imageHeight + ".");
                }
            }
        }
        if (imageBounds == null) {
            imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
        }
        return imageBounds;
    }

    public static Product buildProductWithoutDimapHeader(Path productPath, String productType, TiffFileInfo tiffInfo, TIFFRenderedImage baseImage, Dimension productSize)
                                                         throws Exception {

        String productName = null;
        if (tiffInfo.containsField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION)) {
            TIFFField tagImageDescriptionField = tiffInfo.getField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION);
            productName = tagImageDescriptionField.getAsString(0).trim();
        }
        if (StringUtils.isBlank(productName) && productPath != null) {
            productName = FileUtils.getFilenameWithoutExtension(productPath.getFileName().toString());
        } else {
            productName = "geotiff";
        }

        Product product = new Product(productName, productType, productSize.width, productSize.height);

        Band[] bands = buildBands(tiffInfo, baseImage, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        for (int i = 0; i < bands.length; i++) {
            product.addBand(bands[i]);
        }

        return product;
    }

    private static void removeGeoCodingAndTiePointGrids(Product product) {
        product.setSceneGeoCoding(null);
        final TiePointGrid[] pointGrids = product.getTiePointGrids();
        for (TiePointGrid pointGrid : pointGrids) {
            product.removeTiePointGrid(pointGrid);
        }
    }

    private static Band[] buildBands(TiffFileInfo tiffInfo, TIFFRenderedImage baseImage, int productWidth, int productHeight) throws Exception {
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

    public static Product buildProductFromDimapHeader(String tagNumberText, Dimension productSize) throws IOException {
        Product product = null;
        InputStream inputStream = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            inputStream = new ByteArrayInputStream(tagNumberText.getBytes());
            Document document = new DOMBuilder().build(builder.parse(inputStream));
            product = DimapProductHelpers.createProduct(document, productSize);
            removeGeoCodingAndTiePointGrids(product);
        } catch (ParserConfigurationException | SAXException ignore) {
            // ignore if it can not be read
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return product;
    }

    private static Dimension computePreferredTiling(boolean isGlobalShifted180, GeoTiffImageReader geoTiffImageReader, Dimension productSize) throws IOException {
        Dimension dimension;
        if (isGlobalShifted180) {
            dimension = new Dimension(productSize.width, productSize.height);
        } else {
            int imageWidth = geoTiffImageReader.getImageWidth();
            int imageHeight = geoTiffImageReader.getImageHeight();
            int tileWidth = geoTiffImageReader.getTileWidth();
            int tileHeight = geoTiffImageReader.getTileHeight();
            boolean isBadTiling = (tileWidth <= 1 || tileHeight <= 1 || imageWidth == tileWidth || imageHeight == tileHeight);
            if (isBadTiling) {
                dimension = JAIUtils.computePreferredTileSize(productSize.width, productSize.height, 1);
            } else {
                if (tileWidth > productSize.width) {
                    tileWidth = productSize.width;
                }
                if (tileHeight > productSize.height) {
                    tileHeight = productSize.height;
                }
                dimension = new Dimension(tileWidth, tileHeight);
            }
        }
        return dimension;
    }

    private static ImageInfo createIndexedImageInfo(Product product, TIFFRenderedImage baseImage, Band band) {
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

    private void applyGeoCoding(TiffFileInfo info, TIFFImageMetadata metadata, Product product) {
        if (info.containsField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT)) {

            double[] tiePoints = info.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT).getAsDoubles();

            boolean isGlobal = isGlobal(product.getSceneRasterWidth(), info);

            // check if we have a global geographic lat/lon with lon from 0..360 instead of -180..180
            final double deltaX = Math.ceil(360.0d / product.getSceneRasterWidth());
            if (isGlobal && tiePoints.length == 6 && Math.abs(tiePoints[3]) < deltaX) {
                // e.g. tiePoints[3] = -0.5, productWidth=722 --> we have a lon range of 361 which should start
                // at or near -180 but not at zero
                this.isGlobalShifted180 = true;
                // subtract 180 from the longitudes
                tiePoints[3] -= 180.0;
            }

            if (canCreateTiePointGeoCoding(tiePoints)) {
                applyTiePointGeoCoding(info, tiePoints, product);
            } else if (canCreateGcpGeoCoding(tiePoints)) {
                applyGcpGeoCoding(info, tiePoints, product);
            }
        }

        if (product.getSceneGeoCoding() == null) {
            try {
                product.setSceneGeoCoding(buildGeoCoding(metadata, product.getSceneRasterSize()));
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private static boolean isGlobal(int productWidth, TiffFileInfo info) {
        boolean isGlobal = false;
        final TIFFField pixelScaleField = info.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
        if (pixelScaleField != null) {
            double[] pixelScales = pixelScaleField.getAsDoubles();

            if (isPixelScaleValid(pixelScales)) {
                final double widthInDegree = pixelScales[0] * productWidth;
                isGlobal = Math.ceil(widthInDegree) >= 360;
            }
        }

        return isGlobal;
    }

    private static boolean isPixelScaleValid(double[] pixelScales) {
        return pixelScales != null &&
                !Double.isNaN(pixelScales[0]) && !Double.isInfinite(pixelScales[0]) &&
                !Double.isNaN(pixelScales[1]) && !Double.isInfinite(pixelScales[1]);
    }

    public static GeoCoding buildGeoCoding(TIFFImageMetadata metadata, Dimension productSize) throws Exception {
        final GeoTiffIIOMetadataDecoder metadataDecoder = new GeoTiffIIOMetadataDecoder(metadata);
        Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true);
        final GeoTiffMetadata2CRSAdapter geoTiff2CRSAdapter = new GeoTiffMetadata2CRSAdapter(hints);
        // todo reactivate the following line if geotools has fixed the problem. (see BEAM-1510)
        // final MathTransform toModel = GeoTiffMetadata2CRSAdapter.getRasterToModel(metadataDecoder, false);
        final MathTransform toModel = getRasterToModel(metadataDecoder);
        CoordinateReferenceSystem crs;
        try {
            crs = geoTiff2CRSAdapter.createCoordinateSystem(metadataDecoder);
        } catch (UnsupportedOperationException e) {
            if (toModel == null) {
                throw e;
            } else {
                // ENVI falls back to WGS84, if no CRS is given in the GeoTIFF.
                crs = DefaultGeographicCRS.WGS84;
            }
        }
        Rectangle imageBounds = new Rectangle(productSize.width, productSize.height);
        return new CrsGeoCoding(crs, imageBounds, (AffineTransform) toModel);
    }

    /*
     * Copied from GeoTools GeoTiffMetadata2CRSAdapter because the given tie-point offset is
     * not correctly interpreted in GeoTools. The tie-point should be placed at the pixel center
     * if RasterPixelIsPoint is set as value for GTRasterTypeGeoKey.
     * See links:
     * http://www.remotesensing.org/geotiff/faq.html#PixelIsPoint
     * http://lists.osgeo.org/pipermail/gdal-dev/2007-November/015040.html
     * http://trac.osgeo.org/gdal/wiki/rfc33_gtiff_pixelispoint
     */
    private static MathTransform getRasterToModel(final GeoTiffIIOMetadataDecoder metadata) throws GeoTiffException {
        //
        // Load initials
        //
        final boolean hasTiePoints = metadata.hasTiePoints();
        final boolean hasPixelScales = metadata.hasPixelScales();
        final boolean hasModelTransformation = metadata.hasModelTrasformation();
        int rasterType = getGeoKeyAsInt(GeoTiffConstants.GTRasterTypeGeoKey, metadata);
        // geotiff spec says that PixelIsArea is the default
        if (rasterType == GeoTiffConstants.UNDEFINED) {
            rasterType = GeoTiffConstants.RasterPixelIsArea;
        }
        MathTransform xform;
        if (hasTiePoints && hasPixelScales) {

            //
            // we use tie points and pixel scales to build the grid to world
            //
            // model space
            final TiePoint[] tiePoints = metadata.getModelTiePoints();
            final PixelScale pixScales = metadata.getModelPixelScales();


            // here is the matrix we need to build
            final GeneralMatrix gm = new GeneralMatrix(3);
            final double scaleRaster2ModelLongitude = pixScales.getScaleX();
            final double scaleRaster2ModelLatitude = -pixScales.getScaleY();
            // "raster" space
            final double tiePointColumn = tiePoints[0].getValueAt(0) + (rasterType == GeoTiffConstants.RasterPixelIsPoint ? 0.5 : 0);
            final double tiePointRow = tiePoints[0].getValueAt(1) + (rasterType == GeoTiffConstants.RasterPixelIsPoint ? 0.5 : 0);

            // compute an "offset and scale" matrix
            gm.setElement(0, 0, scaleRaster2ModelLongitude);
            gm.setElement(1, 1, scaleRaster2ModelLatitude);
            gm.setElement(0, 1, 0);
            gm.setElement(1, 0, 0);

            gm.setElement(0, 2, tiePoints[0].getValueAt(3) - (scaleRaster2ModelLongitude * tiePointColumn));
            gm.setElement(1, 2, tiePoints[0].getValueAt(4) - (scaleRaster2ModelLatitude * tiePointRow));

            // make it a LinearTransform
            xform = ProjectiveTransform.create(gm);

        } else if (hasModelTransformation) {
            if (rasterType == GeoTiffConstants.RasterPixelIsPoint) {
                final AffineTransform tempTransform = new AffineTransform(metadata.getModelTransformation());
                tempTransform.concatenate(AffineTransform.getTranslateInstance(0.5, 0.5));
                xform = ProjectiveTransform.create(tempTransform);
            } else {
                assert rasterType == GeoTiffConstants.RasterPixelIsArea;
                xform = ProjectiveTransform.create(metadata.getModelTransformation());
            }
        } else {
            throw new GeoTiffException(metadata, "Unknown Raster to Model configuration.", null);
        }

        return xform;
    }

    private static int getGeoKeyAsInt(final int key, final GeoTiffIIOMetadataDecoder metadata) {
        try {
            return Integer.parseInt(metadata.getGeoKey(key));
        } catch (NumberFormatException ne) {
            SystemUtils.LOG.log(Level.FINE, ne.getMessage(), ne);
            return GeoTiffConstants.UNDEFINED;
        }
    }

    private static void applyTiePointGeoCoding(TiffFileInfo info, double[] tiePoints, Product product) {
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

        String[] names = Utils.findSuitableLatLonNames(product);
        final TiePointGrid latGrid = new TiePointGrid(
                names[0], width, height, xMin, yMin, xDiff, yDiff, lats);
        final TiePointGrid lonGrid = new TiePointGrid(
                names[1], width, height, xMin, yMin, xDiff, yDiff, lons);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, datum));
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

        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

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

            final Placemark gcp = Placemark.createPointPlacemark(gcpDescriptor, "gcp_" + i, "GCP_" + i, "",
                    pixelPos, geoPos, product.getSceneGeoCoding());
            gcpGroup.add(gcp);
        }

        final Placemark[] gcps = gcpGroup.toArray(new Placemark[gcpGroup.getNodeCount()]);
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        product.setSceneGeoCoding(new GcpGeoCoding(method, gcps, width, height, datum));
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

    public static GeoTiffImageReader buildGeoTiffImageReader(Path productPath) throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        if (productPath.getFileName().toString().toLowerCase().endsWith(GeoTiffProductReaderPlugIn.ZIP_FILE_EXTENSION)) {
            boolean success = false;
            FileSystem fileSystem = null;
            try {
                fileSystem = ZipFileSystemBuilder.newZipFileSystem(productPath);
                TreeSet<String> filePaths = FileSystemUtils.listAllFilePaths(fileSystem);
                Iterator<String> itFileNames = filePaths.iterator();
                while (itFileNames.hasNext() && !success) {
                    String filePath = itFileNames.next();
                    boolean extensionMatches = Arrays.stream(GeoTiffProductReaderPlugIn.TIFF_FILE_EXTENSION).anyMatch(filePath.toLowerCase()::endsWith);
                    if (extensionMatches) {
                        int startIndex = 0;
                        if (filePath.startsWith(fileSystem.getSeparator())) {
                            startIndex = fileSystem.getSeparator().length(); // the file path starts with '/' (the root folder in the zip archive)
                        }
                        if (filePath.indexOf(fileSystem.getSeparator(), startIndex) < 0) {
                            Path tiffImagePath = fileSystem.getPath(filePath);
                            InputStream inputStream = Files.newInputStream(tiffImagePath);
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
                            GeoTiffImageReader geoTiffImageReader = new GeoTiffImageReader(bufferedInputStream, fileSystem);
                            success = true;
                            return geoTiffImageReader;
                        }
                    }
                }
                throw new IllegalArgumentException("The zip archive '" + productPath.toString()+"' does not contain an image.");
            } finally {
                if (fileSystem != null && !success) {
                    fileSystem.close();
                }
            }
        } else {
            return new GeoTiffImageReader(productPath.toFile());
        }
    }

    private static FileImageInputStreamSpi registerImageInputStreamSpi() {
        FileImageInputStreamSpi imageInputStreamSpi = null;
        IIORegistry defaultInstance = IIORegistry.getDefaultInstance();
        if (defaultInstance.getServiceProviderByClass(FileImageInputStreamSpi.class) == null) {
            // register only if not already registered
            ImageInputStreamSpi toUnorder = null;
            Iterator<ImageInputStreamSpi> serviceProviders = defaultInstance.getServiceProviders(ImageInputStreamSpi.class, true);
            while (serviceProviders.hasNext()) {
                ImageInputStreamSpi current = serviceProviders.next();
                if (current.getInputClass() == File.class) {
                    toUnorder = current;
                    break;
                }
            }
            imageInputStreamSpi = new FileImageInputStreamSpi();
            defaultInstance.registerServiceProvider(imageInputStreamSpi);
            if (toUnorder != null) {
                // Make the custom Spi to be the first one to be used.
                defaultInstance.setOrdering(ImageInputStreamSpi.class, imageInputStreamSpi, toUnorder);
            }
        }
        return imageInputStreamSpi;
    }
}
