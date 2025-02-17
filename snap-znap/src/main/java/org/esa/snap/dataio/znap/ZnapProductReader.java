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
import com.bc.zarr.DataType;
import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;
import com.bc.zarr.storage.FileSystemStore;
import com.bc.zarr.storage.Store;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.geometry.VectorDataNodeIO;
import org.esa.snap.core.dataio.geometry.VectorDataNodeReader;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.JsonLanguageSupport;
import org.esa.snap.core.dataio.persistence.Persistence;
import org.esa.snap.core.dataio.persistence.PersistenceDecoder;
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.DataNode;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeometryDescriptor;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptorRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.StxFactory;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.ISO8601Converter;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.InvalidRangeException;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.esa.snap.core.datamodel.ConvolutionFilterBandPersistenceConverter.PROP_NAME_BAND_NAME;
import static org.esa.snap.core.datamodel.ConvolutionFilterBandPersistenceConverter.ROOT_NAME_SPECTRAL_BAND_INFO;
import static org.esa.snap.core.util.SystemUtils.LOG;
import static org.esa.snap.dataio.znap.CFConstantsAndUtils.FLAG_MASKS;
import static org.esa.snap.dataio.znap.CFConstantsAndUtils.FLAG_MEANINGS;
import static org.esa.snap.dataio.znap.CFConstantsAndUtils.FLAG_VALUES;
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
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_PALETTE_AUTO_DISTRIBUTE;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_PALETTE_DISCRETE;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_PALETTE_NUM_COLORS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_PALETTE_POINTS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.COLOR_RGBA;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.DATASET_AUTO_GROUPING;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.DISCONTINUITY;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.FLAG_DESCRIPTIONS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.HISTOGRAM_MATCHING;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.IMAGE_INFO;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.KEY_SNAP_PRODUCT_METADATA_JSON;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.KEY_SNAP_VECTOR_DATA_SUBGROUP;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.LABEL;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.LOG_10_SCALED;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NAME_FILTER_BANDS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NAME_MASKS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NAME_SAMPLE_CODING;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NO_DATA_COLOR_RGBA;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NO_DATA_VALUE_USED;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.QUICKLOOK_BAND_NAME;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.SAMPLE;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.SOLAR_FLUX;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.SPECTRAL_BAND_INDEX;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.STATISTICS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.UNCERTAINTY_BAND_NAME;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.UNCERTAINTY_VISUALISATION_MODE;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.VALID_PIXEL_EXPRESSION;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.VIRTUAL_BAND_EXPRESSION;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.WAVELENGTH;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.cast;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.convertToPath;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.getSnapDataType;
import static ucar.nc2.constants.ACDD.TIME_END;
import static ucar.nc2.constants.ACDD.TIME_START;
import static ucar.nc2.constants.CDM.FILL_VALUE;
import static ucar.nc2.constants.CF.ADD_OFFSET;
import static ucar.nc2.constants.CF.LONG_NAME;
import static ucar.nc2.constants.CF.MISSING_VALUE;
import static ucar.nc2.constants.CF.SCALE_FACTOR;
import static ucar.nc2.constants.CF.UNITS;

public class ZnapProductReader extends AbstractProductReader {

    private ProductReaderPlugIn binaryReaderPlugIn;
    private String binaryFileExtension;
    private HashMap<DataNode, Product> binaryProducts;
    private final Map<ProductNode, Map<String, Object>> nodeAttributes = new HashMap<>();
    private final Map<Map<String, Object>, GeoCoding> sharedGeoCodings = new HashMap<>();

    private final Persistence persistence = new Persistence();
    private final JsonLanguageSupport languageSupport = new JsonLanguageSupport();
    private Store store;
    private Product product;
    private ZarrGroup rootGroup;
    private Path rootPath;

    protected ZnapProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    public void readBandRasterData(Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) {
        throw new IllegalStateException("Data is provided by images");
    }

    @Override
    public void close() throws IOException {
        if (binaryProducts != null) {
            for (Product product : binaryProducts.values()) {
                product.dispose();
            }
            binaryProducts.clear();
            binaryProducts = null;
        }
        super.close();
        if (store != null) {
            store.close();
        }
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Path inputPath = convertToPath(getInput());
        final String lowerName = inputPath.getFileName().toString().toLowerCase();
        if (lowerName.endsWith(".znap.zip") || lowerName.endsWith(".znap")) {
            rootPath = inputPath;
        } else {
            rootPath = inputPath.getParent();
        }
        assert rootPath != null;
        if (Files.isRegularFile(rootPath)) {
            store = new ZnapZipStore(rootPath);
        } else {
            store = new FileSystemStore(rootPath);
        }
        rootGroup = ZarrGroup.open(store);
        final Map<String, Object> productAttributes = rootGroup.getAttributes();

        final String productName = cast(productAttributes.get(ATT_NAME_PRODUCT_NAME));
        final String productType = cast(productAttributes.get(ATT_NAME_PRODUCT_TYPE));
        final String productDesc = cast(productAttributes.get(ATT_NAME_PRODUCT_DESC));
        final ProductData.UTC sensingStart = getTime(productAttributes, TIME_START, rootPath); // "time_coverage_start"
        final ProductData.UTC sensingStop = getTime(productAttributes, TIME_END, rootPath); // "time_coverage_end"
        final int sceneRasterWidth = ((Number) productAttributes.get(ATT_NAME_PRODUCT_SCENE_WIDTH)).intValue();
        final int sceneRasterHeight = ((Number) productAttributes.get(ATT_NAME_PRODUCT_SCENE_HEIGHT)).intValue();
        product = new Product(productName, productType, sceneRasterWidth, sceneRasterHeight, this);
        product.setDescription(productDesc);
        product.setStartTime(sensingStart);
        product.setEndTime(sensingStop);
        if (productAttributes.get(DATASET_AUTO_GROUPING) != null) {
            product.setAutoGrouping((String) productAttributes.get(DATASET_AUTO_GROUPING));
        }
        if (productAttributes.get(QUICKLOOK_BAND_NAME) != null) {
            product.setQuicklookBandName(((String) productAttributes.get(QUICKLOOK_BAND_NAME)).trim());
        }

        final InputStream metadataIS = store.getInputStream(KEY_SNAP_PRODUCT_METADATA_JSON);
        if (metadataIS != null) {
            try (InputStreamReader in = new InputStreamReader(metadataIS)) {
                final MetadataElement[] metadataElements = ZnapConstantsAndUtils.readProductMetadata(in);
                for (MetadataElement metadataElement : metadataElements) {
                    product.getMetadataRoot().addElement(metadataElement);
                }
            }
        }

        initTiepointGridsAndBands(productAttributes);
        initMasks(productAttributes);
        initFilterBands(productAttributes);
        product.setFileLocation(rootPath.toFile());
        product.setProductReader(this);
        product.setModified(false);
        addGeocodings(productAttributes);
        readVectorData();
        return product;
    }

    private void initTiepointGridsAndBands(Map<String, Object> productAttributes) throws IOException {
        final HashMap<String, ZarrArray> zarrArrays = new HashMap<>();
        final Set<String> arrayKeys = rootGroup.getArrayKeys();
        for (String arrayKey : arrayKeys) {
            final ZarrArray zarrArray = rootGroup.openArray(arrayKey);
            final String rasterName = getRasterName(arrayKey);
            zarrArrays.put(rasterName, zarrArray);
        }

        final List<String> rasterDataNodeOrder = cast(productAttributes.get(ATT_NAME_ORIGINAL_RASTER_DATA_NODE_ORDER));
        for (String rasterName : rasterDataNodeOrder) {
            if (!zarrArrays.containsKey(rasterName)) {
                continue;
            }
            final ZarrArray zarrArray = zarrArrays.get(rasterName);
            final int[] shape = zarrArray.getShape();
            final int[] chunks = zarrArray.getChunks();
            final DataType zarrDataType = zarrArray.getDataType();

            final SnapDataType snapDataType = getSnapDataType(zarrDataType);
            final int width = shape[1];
            final int height = shape[0];

            final Map<String, Object> attributes = zarrArray.getAttributes();

            final Path rasterDir = rootPath.resolve(rasterName);
            if (attributes.containsKey(ATT_NAME_OFFSET_X)) {
                final double offsetX = cast(attributes.get(ATT_NAME_OFFSET_X));
                final double offsetY = cast(attributes.get(ATT_NAME_OFFSET_Y));
                final double subSamplingX = cast(attributes.get(ATT_NAME_SUBSAMPLING_X));
                final double subSamplingY = cast(attributes.get(ATT_NAME_SUBSAMPLING_Y));
                final float[] dataBuffer = new float[width * height];
                if (attributes.containsKey(ATT_NAME_BINARY_FORMAT)) {
                    initBinaryReaderPlugin(attributes);
                    final Optional<Path> first = Files.walk(rasterDir).filter(path -> path.getFileName().toString().endsWith(binaryFileExtension)).findFirst();
                    if (first.isPresent()) {
                        final Path srcPath = first.get();
                        final ProductReader reader = binaryReaderPlugIn.createReaderInstance();
                        final Product binaryProduct = reader.readProductNodes(srcPath.toFile(), null);
                        binaryProduct.setProductReader(reader);
                        final Band dataBand = binaryProduct.getBand("data");
                        dataBand.readPixels(0, 0, width, height, dataBuffer);
                        binaryProduct.dispose();
                    }
                } else {
                    try {
                        zarrArray.read(dataBuffer, shape, new int[]{0, 0});
                    } catch (InvalidRangeException e) {
                        throw new IOException("InvalidRangeException while reading tie point raster '" + rasterName + "'", e);
                    }
                }
                final TiePointGrid tiePointGrid = new TiePointGrid(rasterName, width, height, offsetX, offsetY, subSamplingX, subSamplingY, dataBuffer);
                if (attributes.containsKey(DISCONTINUITY)) {
                    final Number num = cast(attributes.get(DISCONTINUITY));
                    tiePointGrid.setDiscontinuity(num.intValue());
                }
                product.addTiePointGrid(tiePointGrid);
                applyRasterAttributes(attributes, tiePointGrid);
                nodeAttributes.put(tiePointGrid, attributes);
            } else {
                final Band band;
                final boolean virtualBand = attributes.containsKey(VIRTUAL_BAND_EXPRESSION);
                if (virtualBand) {
                    final String expr = cast(attributes.get(VIRTUAL_BAND_EXPRESSION));
                    band = new VirtualBand(rasterName, snapDataType.getValue(), width, height, expr);
                } else {
                    band = new Band(rasterName, snapDataType.getValue(), width, height);
                }
                product.addBand(band);
                nodeAttributes.put(band, attributes);
                applyBandAttributes(attributes, band);
                if (virtualBand) {
                    continue;
                }
                if (attributes.containsKey(ATT_NAME_BINARY_FORMAT)) {
                    initBinaryReaderPlugin(attributes);
                    final Optional<Path> first = Files.walk(rasterDir).filter(path -> path.getFileName().toString().endsWith(binaryFileExtension)).findFirst();
                    if (first.isPresent()) {
                        final Path srcPath = first.get();
                        final ProductReader reader = binaryReaderPlugIn.createReaderInstance();
                        final Product binaryProduct = reader.readProductNodes(srcPath.toFile(), null);
                        binaryProduct.setProductReader(reader);
                        final Band dataBand = binaryProduct.getBand("data");
                        band.setSourceImage(dataBand.getSourceImage());
                        binaryProducts.put(band, binaryProduct);
                    }
                } else {
                    final ZarrOpImage zarrOpImage = new ZarrOpImage(band, shape, chunks, zarrArray, ResolutionLevel.MAXRES);
                    band.setSourceImage(zarrOpImage);
                }
            }
        }
    }

    private void initMasks(Map<String, Object> productAttributes) {
        final HashMap<String, Map<String, Object>> masksMap = new HashMap<>();
        final List<Map<String, Map<String, Object>>> masksObjectList = cast(productAttributes.get(NAME_MASKS));
        if (masksObjectList != null) {
            for (Map<String, Map<String, Object>> maskObj : masksObjectList) {
                final String name = cast(maskObj.get("Mask").get("NAME"));
                masksMap.put(name, cast(maskObj));
            }
        }

        final List<String> rasterDataNodeOrder = cast(productAttributes.get(ATT_NAME_ORIGINAL_RASTER_DATA_NODE_ORDER));
        for (String rasterName : rasterDataNodeOrder) {
            if (masksMap.containsKey(rasterName)) {
                final Map<String, Object> maskMap = masksMap.get(rasterName);
                final Item item = languageSupport.translateToItem(maskMap);
                final PersistenceDecoder<Mask> decoder = persistence.getDecoder(item);
                if (decoder != null) {
                    final Mask mask = decoder.decode(item, product);
                    if (mask != null) {
                        final String maskName = mask.getName();
                        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
                        if (maskGroup.contains(maskName)) {
                            maskGroup.remove(maskGroup.get(maskName));
                        }
                        maskGroup.add(mask);
                    }
                }
            }
        }
    }

    private void initFilterBands(Map<String, Object> productAttributes) {
        if (!productAttributes.containsKey(NAME_FILTER_BANDS)) {
            return;
        }
        final List<Map<String, Map<String, Object>>> filterBandAttrs = cast(productAttributes.get(NAME_FILTER_BANDS));
        final HashMap<String, Map<String, Object>> fbAttrMap = new HashMap<>();
        for (Map<String, Map<String, Object>> fbAttr : filterBandAttrs) {
            final String name = cast(fbAttr.get(ROOT_NAME_SPECTRAL_BAND_INFO).get(PROP_NAME_BAND_NAME));
            fbAttrMap.put(name, cast(fbAttr));
        }
        final List<String> rasterDataNodeOrder = cast(productAttributes.get(ATT_NAME_ORIGINAL_RASTER_DATA_NODE_ORDER));
        for (String rasterName : rasterDataNodeOrder) {
            if (!fbAttrMap.containsKey(rasterName)) {
                continue;
            }
            final Map<String, Object> fbAttr = fbAttrMap.get(rasterName);
            final Item item = languageSupport.translateToItem(fbAttr);
            final PersistenceDecoder<FilterBand> decoder = persistence.getDecoder(item);
            if (decoder == null) {
                continue;
            }
            final FilterBand filterBand = decoder.decode(item, product);
            product.addBand(filterBand);
        }
    }

    private void readVectorData() throws IOException {
        final List<String> keys = store.getKeysEndingWith(VectorDataNodeIO.FILENAME_EXTENSION).stream()
                .filter(s -> s.startsWith(KEY_SNAP_VECTOR_DATA_SUBGROUP)).collect(Collectors.toList());
        for (String key : keys) {
            addVectorDataToProduct(key);
        }
    }

    private void addVectorDataToProduct(String key) {
        final CoordinateReferenceSystem modelCRS;
        if (key.toLowerCase().contains("ground_control_points")) {
            modelCRS = Product.DEFAULT_IMAGE_CRS;
        } else {
            modelCRS = product.getSceneCRS();
        }
        try (Reader reader = new InputStreamReader(store.getInputStream(key))) {
            FeatureUtils.FeatureCrsProvider crsProvider = new FeatureUtils.FeatureCrsProvider() {
                @Override
                public CoordinateReferenceSystem getFeatureCrs(Product product) {
                    return modelCRS;
                }

                @Override
                public boolean clipToProductBounds() {
                    return false;
                }
            };
            OptimalPlacemarkDescriptorProvider descriptorProvider = new OptimalPlacemarkDescriptorProvider();
            final String name = key.substring(key.lastIndexOf("/") + 1);
            VectorDataNode vectorDataNode = VectorDataNodeReader.read(name, reader, product,
                                                                      crsProvider, descriptorProvider, modelCRS,
                                                                      VectorDataNodeIO.DEFAULT_DELIMITER_CHAR,
                                                                      ProgressMonitor.NULL);
            if (vectorDataNode != null) {
                final ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
                final VectorDataNode existing = vectorDataGroup.get(vectorDataNode.getName());
                if (existing != null) {
                    vectorDataGroup.remove(existing);
                }
                vectorDataGroup.add(vectorDataNode);
            }
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.SEVERE, "Error reading '" + key + "'", e);
        }
    }

    private void addGeocodings(Map<String, Object> productAttributes) {
        addGeoCoding(product, productAttributes, product::setSceneGeoCoding);
        final List<RasterDataNode> rasterDataNodes = product.getRasterDataNodes();
        for (RasterDataNode rasterDataNode : rasterDataNodes) {
            final Map<String, Object> attibutes = nodeAttributes.get(rasterDataNode);
            if (attibutes != null) {
                addGeoCoding(rasterDataNode, attibutes, rasterDataNode::setGeoCoding);
            }
        }
    }

    private void addGeoCoding(ProductNode node, Map<String, Object> attributes, GeocodingSetter gcSetter) {
        if (!attributes.containsKey(ATT_NAME_GEOCODING)) {
            return;
        }
        final Map<String, Object> gcAttribs = cast(attributes.get(ATT_NAME_GEOCODING));
        if (gcAttribs == null) {
            return;
        }
        final GeoCoding geoCoding;
        if (sharedGeoCodings.containsKey(gcAttribs)) {
            geoCoding = sharedGeoCodings.get(gcAttribs);
        } else {
            geoCoding = createGeoCoding(node, gcAttribs);
            sharedGeoCodings.put(gcAttribs, geoCoding);
        }
        gcSetter.setGeocoding(geoCoding);
    }

    interface GeocodingSetter {
        void setGeocoding(GeoCoding gc);
    }

    GeoCoding createGeoCoding(ProductNode node, Map<String, Object> gcAttribs) {
        final Product product = node.getProduct();
        if (gcAttribs.containsKey(DimapProductConstants.TAG_WKT)
            && gcAttribs.containsKey(DimapProductConstants.TAG_IMAGE_TO_MODEL_TRANSFORM)) {
            LOG.fine("Try to instantiate " + CrsGeoCoding.class.getSimpleName() + ".");
            final int width;
            final int height;
            if (node instanceof RasterDataNode) {
                final RasterDataNode rasterDataNode = (RasterDataNode) node;
                width = rasterDataNode.getRasterWidth();
                height = rasterDataNode.getRasterHeight();
            } else {
                width = product.getSceneRasterWidth();
                height = product.getSceneRasterHeight();
            }
            LOG.fine("width = " + width);
            LOG.fine("height = " + height);
            try {
                final String wkt = cast(gcAttribs.get(DimapProductConstants.TAG_WKT));
                LOG.fine("wkt = " + wkt);
                final CoordinateReferenceSystem crs = CRS.parseWKT(wkt);

                final List<Double> matrix = cast(gcAttribs.get(DimapProductConstants.TAG_IMAGE_TO_MODEL_TRANSFORM));
                LOG.fine("matrix = " + Arrays.toString(matrix.toArray()));

                final double[] ma = new double[matrix.size()];
                for (int i = 0; i < matrix.size(); i++) {
                    ma[i] = matrix.get(i);
                }
                final AffineTransform i2m = new AffineTransform(ma);
                Rectangle imageBounds = new Rectangle(width, height);
                return new CrsGeoCoding(crs, imageBounds, i2m);
            } catch (FactoryException | TransformException e) {
                LOG.log(Level.WARNING, "Unable to instantiate " + CrsGeoCoding.class.getSimpleName() + ".", e);
            }
        }
//        final String type = (String) gcAttribs.get("type");
        LOG.fine("------------------------------------------------------------------------");
//        if (StringUtils.isNotNullAndNotEmpty(type)) {
//            LOG.info("create " + type + " for " + node.getName());
//        }
        final Item item = languageSupport.translateToItem(gcAttribs);
        final PersistenceDecoder<GeoCoding> decoder = this.persistence.getDecoder(item);
        if (decoder != null) {
            LOG.fine("Try to instantiate geo coding: "
                     + item.getName()
                     + " for " + product.getName());
            GeoCoding gc = decoder.decode(item, product);
            if (gc != null) {
                return gc;
            } else {
                LOG.log(Level.WARNING, "Unable to instantiate geo coding: "
                                       + item.getName());
            }
        }

        return null;
    }

    private void initBinaryReaderPlugin(Map<String, Object> attributes) {
        if (binaryReaderPlugIn == null) {
            final String binaryFormat = cast(attributes.get(ATT_NAME_BINARY_FORMAT));
            binaryReaderPlugIn = ProductIO.getProductReader(binaryFormat).getReaderPlugIn();
            binaryFileExtension = binaryReaderPlugIn.getDefaultFileExtensions()[0];
            binaryProducts = new HashMap<>();
        }
    }

    private String getRasterName(String arrayKey) {
        return arrayKey.contains("/") ? arrayKey.substring(arrayKey.lastIndexOf("/") + 1) : arrayKey;
    }

    @Override
    protected void readBandRasterDataImpl(
            int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
            Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
            ProductData destBuffer, ProgressMonitor pm) {
        throw new IllegalStateException("Data is provided by images");
    }

    void applyBandAttributes(Map<String, Object> attributes, Band band) {
        // TODO: 21.07.2019 SE -- units for bandwidth, wavelength, solarFlux
        if (attributes.get(BANDWIDTH) != null) {
            final Number number = cast(attributes.get(BANDWIDTH));
            band.setSpectralBandwidth(number.floatValue());
        }
        if (attributes.get(WAVELENGTH) != null) {
            final Number number = cast(attributes.get(WAVELENGTH));
            band.setSpectralWavelength(number.floatValue());
        }
        if (attributes.get(SOLAR_FLUX) != null) {
            final Number number = cast(attributes.get(SOLAR_FLUX));
            band.setSolarFlux(number.floatValue());
        }
        if (attributes.get(SPECTRAL_BAND_INDEX) != null) {
            final Number number = cast(attributes.get(SPECTRAL_BAND_INDEX));
            band.setSpectralBandIndex(number.intValue());
        }
        applySampleCodings(attributes, band);
        applyRasterAttributes(attributes, band);
    }

    void applyRasterAttributes(Map<String, Object> attributes, RasterDataNode rasterDataNode) {
        if (attributes.get(LONG_NAME) != null) {
            rasterDataNode.setDescription(cast(attributes.get(LONG_NAME)));
        }
        if (attributes.get(UNITS) != null) {
            rasterDataNode.setUnit(cast(attributes.get(UNITS)));
        }
        if (attributes.get(SCALE_FACTOR) != null) {
            final Number number = cast(attributes.get(SCALE_FACTOR));
            rasterDataNode.setScalingFactor(number.doubleValue());
        }
        if (attributes.get(ADD_OFFSET) != null) {
            final Number number = cast(attributes.get(ADD_OFFSET));
            rasterDataNode.setScalingOffset(number.doubleValue());
        }
        final Number noDataValue = getNoDataValue(attributes);
        if (noDataValue != null) {
            rasterDataNode.setNoDataValue(noDataValue.doubleValue());
        }
        final Object o = attributes.get(NO_DATA_VALUE_USED);
        if (o != null) {
            rasterDataNode.setNoDataValueUsed(cast(o));
        }
        if (attributes.get(VALID_PIXEL_EXPRESSION) != null) {
            rasterDataNode.setValidPixelExpression(cast(attributes.get(VALID_PIXEL_EXPRESSION)));
        }
        applyAncillaryElements(attributes, rasterDataNode);
        if (attributes.get(DimapProductConstants.TAG_IMAGE_TO_MODEL_TRANSFORM) != null) {
            final List<Double> matrix = cast(attributes.get(DimapProductConstants.TAG_IMAGE_TO_MODEL_TRANSFORM));
            LOG.fine("matrix for band '" + rasterDataNode.getName() + "' = " + Arrays.toString(matrix.toArray()));

            final double[] ma = new double[matrix.size()];
            for (int i = 0; i < matrix.size(); i++) {
                ma[i] = matrix.get(i);
            }
            final AffineTransform i2m = new AffineTransform(ma);
            rasterDataNode.setImageToModelTransform(i2m);
        }
        if (attributes.containsKey(STATISTICS)) {
            final Map<String, Object> stxM = cast(attributes.get(STATISTICS));
            final double minSample = getDoubleValue(stxM.get(DimapProductConstants.TAG_STX_MIN));
            final double maxSample = getDoubleValue(stxM.get(DimapProductConstants.TAG_STX_MAX));
            final double meanSample = getDoubleValue(stxM.get(DimapProductConstants.TAG_STX_MEAN));
            final double stdDev = getDoubleValue(stxM.get(DimapProductConstants.TAG_STX_STDDEV));
            final boolean intHistogram = !ProductData.isFloatingPointType(rasterDataNode.getGeophysicalDataType());
            final List<Integer> integers = cast(stxM.get(DimapProductConstants.TAG_HISTOGRAM));
            final int[] bins = integers.stream().mapToInt(Integer::intValue).toArray();
            final int resLevel = ((Number) stxM.get(DimapProductConstants.TAG_STX_LEVEL)).intValue();
            final Stx stx = new StxFactory()
                    .withMinimum(minSample)
                    .withMaximum(maxSample)
                    .withMean(meanSample)
                    .withStandardDeviation(stdDev)
                    .withIntHistogram(intHistogram)
                    .withHistogramBins(bins == null ? new int[0] : bins)
                    .withResolutionLevel(resLevel).create();
            rasterDataNode.setStx(stx);
        }
        if (attributes.containsKey(IMAGE_INFO)) {
            final Map<String, Object> infoM = cast(attributes.get(IMAGE_INFO));
            final List<Map<String, Object>> pointsL = cast(infoM.get(COLOR_PALETTE_POINTS));
            final ArrayList<ColorPaletteDef.Point> points = new ArrayList<>();
            for (Map<String, Object> pointM : pointsL) {
                final ColorPaletteDef.Point point = new ColorPaletteDef.Point();
                if (pointM.containsKey(LABEL)) {
                    String label = cast(pointM.get(LABEL));
                    label = label != null ? label.trim() : label;
                    if (StringUtils.isNotNullAndNotEmpty(label)) {
                        point.setLabel(label);
                    }
                }
                point.setSample(getDoubleValue(pointM.get(SAMPLE)));
                final List<Integer> integers = cast(pointM.get(COLOR_RGBA));
                final int[] rgba = integers.stream().mapToInt(Integer::intValue).toArray();
                point.setColor(createColor(rgba));
                points.add(point);
            }

            final ColorPaletteDef colorPaletteDef = new ColorPaletteDef(points.toArray(new ColorPaletteDef.Point[0]));
            colorPaletteDef.setNumColors((int) infoM.get(COLOR_PALETTE_NUM_COLORS));
            colorPaletteDef.setDiscrete((boolean) infoM.get(COLOR_PALETTE_DISCRETE));
            colorPaletteDef.setAutoDistribute((boolean) infoM.get(COLOR_PALETTE_AUTO_DISTRIBUTE));
            final ImageInfo imageInfo = new ImageInfo(colorPaletteDef);
            if (infoM.containsKey(NO_DATA_COLOR_RGBA)) {
                final List<Integer> integers = cast(infoM.get(NO_DATA_COLOR_RGBA));
                final int[] rgba = integers.stream().mapToInt(Integer::intValue).toArray();
                imageInfo.setNoDataColor(createColor(rgba));
            }
            final String matching = cast(infoM.get(HISTOGRAM_MATCHING));
            final ImageInfo.HistogramMatching histogramMatching = ImageInfo.HistogramMatching.valueOf(matching);
            imageInfo.setHistogramMatching(histogramMatching);
            imageInfo.setLogScaled((boolean) infoM.get(LOG_10_SCALED));
            if (infoM.containsKey(UNCERTAINTY_BAND_NAME)) {
                imageInfo.setUncertaintyBandName(cast(infoM.get(UNCERTAINTY_BAND_NAME)));
            }
            if (infoM.containsKey(UNCERTAINTY_VISUALISATION_MODE)) {
                final String modeName = cast(infoM.get(UNCERTAINTY_VISUALISATION_MODE));
                final ImageInfo.UncertaintyVisualisationMode mode;
                mode = ImageInfo.UncertaintyVisualisationMode.valueOf(modeName);
                imageInfo.setUncertaintyVisualisationMode(mode);
            }
            rasterDataNode.setImageInfo(imageInfo);
        }
    }

    private void applyAncillaryElements(Map<String, Object> attributes, RasterDataNode rasterDataNode) {
        final HashMap<String, Object> root = new HashMap<>();
        root.put("root", attributes);
        final Item item = languageSupport.translateToItem(root);
        RasterDataNodePersistenceHelper.setAncillaryRelations(item.asContainer(), rasterDataNode);
        RasterDataNodePersistenceHelper.setAncillaryVariables(item.asContainer(), rasterDataNode, rasterDataNode.getProduct());
    }

    private static Color createColor(int[] rgba) {
        return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    private static void applySampleCodings(Map<String, Object> attributes, Band band) {
        final String rasterName = band.getName();
        final List<String> flagMeanings = cast(attributes.get(FLAG_MEANINGS));
        if (flagMeanings != null) {

            final List<Number> flagMasks = cast(attributes.get(FLAG_MASKS));
            final List<Number> flagValues = cast(attributes.get(FLAG_VALUES));

            FlagCoding flagCoding = null;
            IndexCoding indexCoding = null;
            final Product product = band.getProduct();
            String sampleCodingName = getSampleCodingName(attributes, rasterName);
            if (flagMasks != null) {
                flagCoding = new FlagCoding(sampleCodingName);
                band.setSampleCoding(flagCoding);
                product.getFlagCodingGroup().add(flagCoding);
            } else if (flagValues != null) {
                indexCoding = new IndexCoding(sampleCodingName);
                band.setSampleCoding(indexCoding);
                product.getIndexCodingGroup().add(indexCoding);
            } else {
                LOG.warning("Raster attributes for '" + rasterName
                            + "' contains the attribute '" + FLAG_MEANINGS
                            + "' but neither an attribute '" + FLAG_MASKS
                            + "' nor an attribute '" + FLAG_VALUES + "'."
                );
                return;
            }
            for (int i = 0; i < flagMeanings.size(); i++) {
                final String meaningName = flagMeanings.get(i);
                final String description = getFlagDescription(attributes, i);
                if (flagMasks != null) {
                    final int flagMask = flagMasks.get(i).intValue();
                    if (flagValues != null) {
                        flagCoding.addFlag(meaningName, flagMask, flagValues.get(i).intValue(), description);
                    } else {
                        flagCoding.addFlag(meaningName, flagMask, description);
                    }
                } else {
                    indexCoding.addIndex(meaningName, flagValues.get(i).intValue(), description);
                }
            }
        }
    }

    private static String getFlagDescription(Map<String, Object> attributes, int pos) {
        if (attributes.containsKey(FLAG_DESCRIPTIONS)) {
            final List<String> list = cast(attributes.get(FLAG_DESCRIPTIONS));
            return list.get(pos);
        }
        return null;
    }

    private static String getSampleCodingName(Map<String, Object> attributes, String rasterName) {
        final String sampleCodingName;
        if (attributes.containsKey(NAME_SAMPLE_CODING)) {
            sampleCodingName = cast(attributes.get(NAME_SAMPLE_CODING));
        } else {
            sampleCodingName = rasterName;
        }
        return sampleCodingName;
    }

    private ProductData.UTC getTime(Map<String, Object> productAttributes, String attributeName, Path rootPath) throws IOException {
        if (!productAttributes.containsKey(attributeName)) {
            return null;
        }
        final String iso8601String = cast(productAttributes.get(attributeName));
        try {
            return ISO8601Converter.parse(iso8601String);
        } catch (ParseException e) {
            throw new IOException("Unparseable " + attributeName + " while reading product '" + rootPath.toString() + "'", e);
        }
    }

    private static Number getNoDataValue(Map<String, Object> attributes) {
        Object attribute = attributes.get(FILL_VALUE);
        if (attribute == null) {
            attribute = attributes.get(MISSING_VALUE);
        }
        if (attribute instanceof String) {
            return Double.valueOf((String) attribute);
        }
        if (attribute != null) {
            return (Number) attribute;
        }
        return null;
    }

    private double getDoubleValue(Object o) {
        return new Property<>("o", o).getValueDouble();
    }

    private static class OptimalPlacemarkDescriptorProvider
            implements VectorDataNodeReader.PlacemarkDescriptorProvider {

        @Override
        public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType) {
            PlacemarkDescriptorRegistry placemarkDescriptorRegistry = PlacemarkDescriptorRegistry.getInstance();
            if (simpleFeatureType.getUserData().containsKey(
                    PlacemarkDescriptorRegistry.PROPERTY_NAME_PLACEMARK_DESCRIPTOR)) {
                String placemarkDescriptorClass = simpleFeatureType.getUserData().get(
                        PlacemarkDescriptorRegistry.PROPERTY_NAME_PLACEMARK_DESCRIPTOR).toString();
                PlacemarkDescriptor placemarkDescriptor = placemarkDescriptorRegistry.getPlacemarkDescriptor(
                        placemarkDescriptorClass);
                if (placemarkDescriptor != null) {
                    return placemarkDescriptor;
                }
            }
            final PlacemarkDescriptor placemarkDescriptor = placemarkDescriptorRegistry.getPlacemarkDescriptor(
                    simpleFeatureType);
            if (placemarkDescriptor != null) {
                return placemarkDescriptor;
            } else {
                return placemarkDescriptorRegistry.getPlacemarkDescriptor(GeometryDescriptor.class);
            }
        }
    }
}
