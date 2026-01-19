package org.esa.snap.dataio.gdal.reader;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.multilevel.support.DefaultMultiLevelImage;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.commons.VirtualFile;
import org.esa.snap.dataio.gdal.drivers.Dataset;
import org.esa.snap.dataio.gdal.drivers.Driver;
import org.esa.snap.dataio.gdal.drivers.GCP;
import org.esa.snap.dataio.gdal.drivers.GDAL;
import org.esa.snap.dataio.gdal.drivers.GDALConst;
import org.esa.snap.dataio.gdal.drivers.GDALConstConstants;
import org.esa.snap.engine_utilities.dataio.readers.BaseProductReaderPlugIn;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GcpGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.geotiff.EPSGCodes;
import org.esa.snap.engine_utilities.file.AbstractFile;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.ImageLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Generic reader for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GDALProductReader extends AbstractProductReader {

    private static final Map<Integer, BufferTypeDescriptor> BUFFER_TYPES;

    static {
        BUFFER_TYPES = new HashMap<>();
        BUFFER_TYPES.put(GDALConstConstants.gdtByte(), new BufferTypeDescriptor(8, false, ProductData.TYPE_UINT8, DataBuffer.TYPE_BYTE));
        BUFFER_TYPES.put(GDALConstConstants.gdtInt8(), new BufferTypeDescriptor(8, true, ProductData.TYPE_INT8, DataBuffer.TYPE_BYTE));
        BUFFER_TYPES.put(GDALConstConstants.gdtInt16(), new BufferTypeDescriptor(16, true, ProductData.TYPE_INT16, DataBuffer.TYPE_SHORT));
        BUFFER_TYPES.put(GDALConstConstants.gdtUint16(), new BufferTypeDescriptor(16, false, ProductData.TYPE_UINT16, DataBuffer.TYPE_USHORT));
        BUFFER_TYPES.put(GDALConstConstants.gdtInt32(), new BufferTypeDescriptor(32, true, ProductData.TYPE_INT32, DataBuffer.TYPE_INT));
        BUFFER_TYPES.put(GDALConstConstants.gdtUint32(), new BufferTypeDescriptor(32, false, ProductData.TYPE_UINT32, DataBuffer.TYPE_INT));
        BUFFER_TYPES.put(GDALConstConstants.gdtFloat32(), new BufferTypeDescriptor(32, true, ProductData.TYPE_FLOAT32, DataBuffer.TYPE_FLOAT));
        BUFFER_TYPES.put(GDALConstConstants.gdtFloat64(), new BufferTypeDescriptor(64, true, ProductData.TYPE_FLOAT64, DataBuffer.TYPE_DOUBLE));
        BUFFER_TYPES.put(GDALConstConstants.gdtCInt16(), new BufferTypeDescriptor(16, true, ProductData.TYPE_INT16, DataBuffer.TYPE_SHORT));
        BUFFER_TYPES.put(GDALConstConstants.gdtCInt32(), new BufferTypeDescriptor(32, true, ProductData.TYPE_INT32, DataBuffer.TYPE_INT));
        BUFFER_TYPES.put(GDALConstConstants.gdtCFloat32(), new BufferTypeDescriptor(32, true, ProductData.TYPE_FLOAT32, DataBuffer.TYPE_FLOAT));
        BUFFER_TYPES.put(GDALConstConstants.gdtCFloat64(), new BufferTypeDescriptor(64, true, ProductData.TYPE_FLOAT64, DataBuffer.TYPE_DOUBLE));
    }

    private VirtualFile virtualFile;

    public GDALProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    static Dataset openGDALDataset(Path localProductPath) {
        Dataset gdalDataset = GDAL.open(localProductPath.toString(), GDALConst.gaReadonly());
        if (gdalDataset == null) {
            // unknown file format
            throw new NullPointerException("Failed opening a dataset from the file '" + localProductPath + "' to load the product.");
        }
        return gdalDataset;
    }

    static String computeMaskName(org.esa.snap.dataio.gdal.drivers.Band gdalBand, String bandName) {
        try (org.esa.snap.dataio.gdal.drivers.Band maskBand = gdalBand.getMaskBand()) {
            if (maskBand != null) {
                int maskFlags = gdalBand.getMaskFlags();
                String maskPrefix = null;
                if ((maskFlags & (GDALConstConstants.gmfNodata() | GDALConstConstants.gmfPerDataset())) != 0) {
                    maskPrefix = "nodata_";
                } else if ((maskFlags & (GDALConstConstants.gmfPerDataset() | GDALConstConstants.gmfAlpha())) != 0) {
                    maskPrefix = "alpha_";
                } else if ((maskFlags & (GDALConstConstants.gmfNodata() | GDALConstConstants.gmfPerDataset() | GDALConstConstants.gmfAlpha() | GDALConstConstants.gmfAllValid())) != 0) {
                    maskPrefix = "mask_";
                }
                if (maskPrefix != null) {
                    return maskPrefix + bandName;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open band: "+bandName+". Reason: "+e.getMessage(),e);
        }
        return null;
    }

    private static Dimension computeBandTileSize(org.esa.snap.dataio.gdal.drivers.Band gdalBand, int productWidth, int productHeight) {
        //Dimension tileSize = new Dimension(gdalBand.getXSize(), gdalBand.getYSize());
        Dimension tileSize = new Dimension(gdalBand.getBlockXSize(), gdalBand.getBlockYSize());
        if (tileSize.width <= 1 || tileSize.width > productWidth) {
            tileSize.width = productWidth;
        }
        if (tileSize.height <= 1 || tileSize.height > productHeight) {
            tileSize.height = productHeight;
        }
        return tileSize;
    }

    static String computeBandName(org.esa.snap.dataio.gdal.drivers.Band gdalBand, int bandIndex) {
        String bandName = gdalBand.getDescription();
        if (StringUtils.isNullOrEmpty(bandName)) {
            bandName = String.format("band_%s", bandIndex + 1);
        } else {
            bandName = bandName.replace(' ', '_');
        }
        return bandName;
    }

    static GeoCoding buildGeoCoding(Dataset gdalDataset, Rectangle subsetBounds, Product product) throws FactoryException, TransformException {
        CoordinateReferenceSystem mapCRS;
        try {
            mapCRS = CRS.parseWKT(gdalDataset.getProjectionRef().replaceAll(",?(AXIS\\[\"([A-Za-z]*?)\",[A-Z]*?])", ""));
        } catch (Exception e) {
            mapCRS = null;
        }
        if (mapCRS != null) {
            int imageWidth = gdalDataset.getRasterXSize();
            int imageHeight = gdalDataset.getRasterYSize();
            double[] adfGeoTransform = new double[6];
            gdalDataset.getGeoTransform(adfGeoTransform);
            double originX = adfGeoTransform[0];
            double originY = adfGeoTransform[3];
            double resolutionX = adfGeoTransform[1];
            double resolutionY = (adfGeoTransform[5] > 0) ? adfGeoTransform[5] : -adfGeoTransform[5];
            return ImageUtils.buildCrsGeoCoding(originX, originY, resolutionX, resolutionY, imageWidth, imageHeight, mapCRS, subsetBounds);
        } else {
            String gcpProjection = gdalDataset.getGCPProjection();

            int gcpCount = gdalDataset.getGCPCount();
            final GcpGeoCoding.Method method;
            if (gcpCount >= GcpGeoCoding.Method.POLYNOMIAL3.getTermCountP()) {
                method = GcpGeoCoding.Method.POLYNOMIAL3;
            } else if (gcpCount >= GcpGeoCoding.Method.POLYNOMIAL2.getTermCountP()) {
                method = GcpGeoCoding.Method.POLYNOMIAL2;
            } else if (gcpCount >= GcpGeoCoding.Method.POLYNOMIAL1.getTermCountP()) {
                method = GcpGeoCoding.Method.POLYNOMIAL1;
            } else {
                return null; // not able to apply GCP geocoding; not enough tie points
            }
            int i = 0;
            if (gcpCount > 0) {
                Vector gcps = gdalDataset.getGCPs();
                final GcpDescriptor gcpDescriptor = GcpDescriptor.getInstance();
                final List<Placemark> gcpPlacemarksList=new ArrayList<>();
                final GeoCoding productGeocoding;
                if (product != null) {
                    productGeocoding = product.getSceneGeoCoding();
                } else {
                    productGeocoding = null;
                }
                for (Object gcpJNI : gcps) {
                    GCP gcp = new GCP(gcpJNI);
                    final PixelPos pixelPos = new PixelPos(gcp.getGCPPixel(), gcp.getGCPLine());
                    final GeoPos geoPos = new GeoPos(gcp.getGCPY(), gcp.getGCPX());
                    final Placemark gcpPlacemark = Placemark.createPointPlacemark(gcpDescriptor, "gcp_" + i, "GCP_" + i++, "", pixelPos, geoPos, productGeocoding);
                    gcpPlacemarksList.add(gcpPlacemark);
                }
                final Placemark[] gcpPlacemarks = gcpPlacemarksList.toArray(new Placemark[0]);
                final Datum datum = getDatum(gcpProjection);
                final int productWidth = gdalDataset.getRasterXSize();
                final int productHeight = gdalDataset.getRasterYSize();
                return new GcpGeoCoding(method, gcpPlacemarks, productWidth, productHeight, datum);
            }
        }
        return null;
    }

    private static Datum getDatum(String gcpProjection) {
        String datums = gcpProjection.replaceAll("[\\s\\S]*?AUTHORITY\\[\"EPSG\",\"([\\d]+)\"]?[\\s\\S]*", "$1");
        final Datum datum;
        if (datums.replaceAll("\\d*", "").isEmpty()) {
            final int value = Integer.parseInt(datums);
            if (value == EPSGCodes.GCS_WGS_72) {
                datum = Datum.WGS_72;
            } else if (value == EPSGCodes.GCS_WGS_84) {
                datum = Datum.WGS_84;
            } else {
                datum = Datum.WGS_84;
            }
        } else {
            datum = Datum.WGS_84;
        }
        return datum;
    }

    private static MetadataElement buildMetadataElement(Dataset gdalProduct) {
        Driver hDriver = gdalProduct.getDriver();
        int imageWidth = gdalProduct.getRasterXSize();
        int imageHeight = gdalProduct.getRasterYSize();
        MetadataElement metadataElement = new MetadataElement("Image info");
        metadataElement.setAttributeString("driver", hDriver.getShortName());
        metadataElement.setAttributeInt("width", imageWidth);
        metadataElement.setAttributeInt("height", imageHeight);

        double[] adfGeoTransform = new double[6];
        gdalProduct.getGeoTransform(adfGeoTransform);
        double originX = adfGeoTransform[0];
        double originY = adfGeoTransform[3];
        double pixelSizeX = adfGeoTransform[1];
        double pixelSizeY = (adfGeoTransform[5] > 0) ? adfGeoTransform[5] : -adfGeoTransform[5];

        if (adfGeoTransform[2] == 0.0 && adfGeoTransform[4] == 0.0) {
            metadataElement.setAttributeString("origin", originX + "x" + originY);
            metadataElement.setAttributeString("pixel size", pixelSizeX + "x" + pixelSizeY);
        } else {
            String str1 = adfGeoTransform[0] + "," + adfGeoTransform[1] + "," + adfGeoTransform[3];
            String str2 = adfGeoTransform[3] + "," + adfGeoTransform[4] + "," + adfGeoTransform[5];
            metadataElement.setAttributeString("geo transform", str1 + " " + str2);
        }

        Hashtable<?, ?> dict = gdalProduct.getMetadataDict("");
        Enumeration keys = dict.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = (String) dict.get(key);
            if (!StringUtils.isNullOrEmpty(key) && !StringUtils.isNullOrEmpty(value)) {
                metadataElement.setAttributeString(key, value);
            }
        }
        return metadataElement;
    }

    @Override
    public void close() throws IOException {
        super.close();

        closeResources();
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        boolean success = false;
        try {
            Path productPath = BaseProductReaderPlugIn.convertInputToPath(super.getInput());
            this.virtualFile = new VirtualFile(productPath);
            Product product = readProduct(this.virtualFile.getLocalFile());
            product.setFileLocation(productPath.toFile());

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
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) {
        // do nothing
    }

    private Product readProduct(Path localFile) throws IOException {
        if (localFile == null) {
            throw new NullPointerException("The local file is null.");
        }
        if (!AbstractFile.isLocalPath(localFile)) {
            throw new IllegalArgumentException("The file '" + localFile + "' is not a local file.");
        }
        GDAL.setCacheMax(1024*1024*1024);
        try (Dataset gdalDataset = openGDALDataset(localFile)) {
            int defaultProductWidth = gdalDataset.getRasterXSize();
            int defaultProductHeight = gdalDataset.getRasterYSize();

            ProductSubsetDef subsetDef = getSubsetDef();
            Rectangle productBounds;
            if (subsetDef == null || subsetDef.getSubsetRegion() == null) {
                productBounds = new Rectangle(0, 0, defaultProductWidth, defaultProductHeight);
            } else {
                try {
                    GeoCoding productDefaultGeoCoding = buildGeoCoding(gdalDataset, null, null);
                    productBounds = subsetDef.getSubsetRegion().computeProductPixelRegion(productDefaultGeoCoding, defaultProductWidth, defaultProductHeight, false);
                } catch (FactoryException | TransformException e) {
                    throw new IOException(e);
                }
            }
            if (productBounds.isEmpty()) {
                throw new IllegalStateException("Empty product bounds.");
            }
            if ((productBounds.x + productBounds.width) > defaultProductWidth) {
                throw new IllegalArgumentException("The coordinates are out of bounds: productBounds.x=" + productBounds.x + ", productBounds.width=" + productBounds.width + ", default product width=" + defaultProductWidth);
            }
            if ((productBounds.y + productBounds.height) > defaultProductHeight) {
                throw new IllegalArgumentException("The coordinates are out of bounds: productBounds.y=" + productBounds.y + ", productBounds.height=" + productBounds.height + ", default product height=" + defaultProductHeight);
            }

            Product product = new Product(localFile.getFileName().toString(), this.getReaderPlugIn().getDescription(Locale.getDefault()),
                                          productBounds.width, productBounds.height, this);

            Dimension readTileSize = new Dimension(gdalDataset.getRasterBand(1).getBlockXSize(),
                                                   gdalDataset.getRasterBand(1).getBlockYSize());
            product.setPreferredTileSize(readTileSize);

            MetadataElement metadataElement = null;

            if (subsetDef == null || !subsetDef.isIgnoreMetadata()) {
                metadataElement = buildMetadataElement(gdalDataset);
                product.getMetadataRoot().addElement(metadataElement);
            }

            GeoCoding geoCoding;
            try {
                geoCoding = buildGeoCoding(gdalDataset, productBounds, product);
            } catch (FactoryException | TransformException e) {
                throw new IOException("Not able to create geo-coding information", e);
            }
            if (geoCoding != null) {
                product.setSceneGeoCoding(geoCoding);
            }

            Double[] pass1 = new Double[1];
            int maximumResolutionCount = 1;

            int bandCount = gdalDataset.getRasterCount();
            for (int bandIndex = 0; bandIndex < bandCount; bandIndex++) {
                // bands are not 0-base indexed, so we must add 1
                try (org.esa.snap.dataio.gdal.drivers.Band gdalBand = gdalDataset.getRasterBand(bandIndex + 1)) {
                    String bandName = computeBandName(gdalBand, bandIndex);

                    /* Building overviews when reading the product it's not a good idea,
                       especially when the product resides in a read-only storage.

                        if (gdalBand.getOverviewCount() == 0) {
                        gdalDataset.buildOverviews("NEAREST", new int[]{2, 4, 8, 16, 32, 64, 128});
                    }*/

                    if (subsetDef == null || subsetDef.isNodeAccepted(bandName)) {
                        int gdalDataType = gdalBand.getDataType();
                        BufferTypeDescriptor dataBufferType = BUFFER_TYPES.get(gdalDataType);
                        if (dataBufferType == null) {
                            throw new IllegalArgumentException("Unknown raster data type " + gdalDataType + ".");
                        }

                        Dimension tileSize = computeBandTileSize(gdalBand, productBounds.width, productBounds.height);

                        int levelCount = gdalBand.getOverviewCount() + 1;
                        if (maximumResolutionCount >= levelCount) {
                            maximumResolutionCount = levelCount;
                        }
                        if (levelCount == 1) {
                            levelCount = gdalBand.getOverviewCount() + 1;
                        }

                        String colorInterpretationName = GDAL.getColorInterpretationName(gdalBand.getRasterColorInterpretation());
                        MetadataElement bandMetadataElement = new MetadataElement("Component");
                        bandMetadataElement.setAttributeString("data type", GDAL.getDataTypeName(gdalDataType));
                        bandMetadataElement.setAttributeString("color interpretation", colorInterpretationName);
                        bandMetadataElement.setAttributeString("block size", tileSize.width + "x" + tileSize.height);
                        bandMetadataElement.setAttributeInt("precision", dataBufferType.precision);
                        bandMetadataElement.setAttributeString("signed", Boolean.toString(dataBufferType.signed));
                        if (levelCount > 1) {
                            StringBuilder str = new StringBuilder();
                            for (int iOverview = 0; iOverview < levelCount - 1; iOverview++) {
                                if (iOverview != 0) {
                                    str.append(", ");
                                }
                                try (org.esa.snap.dataio.gdal.drivers.Band hOverview = gdalBand.getOverview(iOverview)) {
                                    str.append(hOverview.getXSize())
                                            .append("x")
                                            .append(hOverview.getYSize());
                                }
                            }
                            bandMetadataElement.setAttributeInt("overview count", levelCount - 1);
                            if (str.length() > 0) {
                                bandMetadataElement.setAttributeString("overviews", str.toString());
                            }
                        }

                        Band productBand = new Band(bandName, dataBufferType.bandDataType, productBounds.width, productBounds.height);
                        productBand.setGeoCoding(geoCoding);

                        gdalBand.getOffset(pass1);
                        if (pass1[0] != null && pass1[0] != 0) {
                            bandMetadataElement.setAttributeDouble("offset", pass1[0]);
                            productBand.setScalingOffset(pass1[0]);
                            pass1[0] = null;
                        }

                        gdalBand.getScale(pass1);
                        if (pass1[0] != null && pass1[0] != 1) {
                            bandMetadataElement.setAttributeDouble("scale", pass1[0]);
                            productBand.setScalingFactor(pass1[0]);
                            pass1[0] = null;
                        } else {
                            productBand.setScalingFactor(1.0);
                        }

                        String unitType = gdalBand.getUnitType();
                        if (unitType != null && !unitType.isEmpty()) {
                            bandMetadataElement.setAttributeString("unit type", unitType);
                            productBand.setUnit(unitType);
                        }

                        Double noDataValue = null;
                        gdalBand.getNoDataValue(pass1);
                        if (pass1[0] != null) {
                            noDataValue = pass1[0];
                            productBand.setNoDataValue(noDataValue);
                            productBand.setNoDataValueUsed(true);
                        } else if (GDALConstConstants.gdtFloat32().equals(gdalDataType)){
                            // If NoData not present in the metadata, for float type assume it's NaN
                            productBand.setNoDataValue(Float.NaN);
                            productBand.setNoDataValueUsed(true);
                        }

                        GDALMultiLevelSource multiLevelSource = new GDALMultiLevelSource(dataBufferType.dataBufferType, productBounds, tileSize, bandIndex,
                                levelCount, geoCoding, noDataValue, readTileSize, localFile);
                        // compute the tile size of the image layout object based on the tile size from the tileOpImage used to read the data
                        ImageLayout imageLayout = multiLevelSource.buildMultiLevelImageLayout();
                        productBand.setSourceImage(new DefaultMultiLevelImage(multiLevelSource, imageLayout));

                        if (metadataElement != null && (subsetDef == null || !subsetDef.isIgnoreMetadata())) {
                            metadataElement.addElement(bandMetadataElement);
                        }

                        product.addBand(productBand);
                        // add the mask
                        String maskName = computeMaskName(gdalBand, bandName);
                        if (maskName != null && (subsetDef == null || subsetDef.isNodeAccepted(maskName))) {
                            String expression = maskName.startsWith("nodata_")
                                                ? String.format("feq('%s.raw',%f)", bandName, product.getBand(bandName).getNoDataValue())
                                                : bandName;
                            Mask mask = Mask.BandMathsType.create(maskName, maskName, productBounds.width, productBounds.height,
                                                                  expression, Color.white, 0.5);
                            ProductUtils.copyGeoCoding(productBand, mask);
                            product.addMask(mask);
                        }
                    }
                }
            }
            product.setNumResolutionsMax(maximumResolutionCount);
            return product;
        }
    }

    private void closeResources() {
        if (this.virtualFile != null) {
            this.virtualFile.close();
            this.virtualFile = null;
        }
    }

    private static class BufferTypeDescriptor {
        final int precision;
        final boolean signed;
        final int bandDataType;
        final int dataBufferType;

        BufferTypeDescriptor(int precision, boolean signed, int bandDataType, int dataBufferType) {
            this.precision = precision;
            this.signed = signed;
            this.bandDataType = bandDataType;
            this.dataBufferType = dataBufferType;
        }
    }
}
