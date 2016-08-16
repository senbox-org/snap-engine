package org.esa.s3tbx.dataio.probav;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h5.H5Datatype;
import ncsa.hdf.object.h5.H5Group;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;

/**
 * Reader for Proba-V products
 *
 * @author olafd
 */
public class ProbaVSynthesisProductReader extends AbstractProductReader {

    private int productWidth;
    private int productHeight;

    private File probavFile;
    private int file_id;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected ProbaVSynthesisProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object inputObject = getInput();
        probavFile = ProbaVSynthesisProductReaderPlugIn.getFileInput(inputObject);
        final String fileName = probavFile.getName();

        Product targetProduct = null;

        if (ProbaVSynthesisProductReaderPlugIn.isHdf5LibAvailable()) {
            FileFormat h5FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            FileFormat h5File = null;
            try {
                file_id = H5.H5Fopen(probavFile.getAbsolutePath(),  // Name of the file to access.
                                     HDF5Constants.H5F_ACC_RDONLY,  // File access flag
                                     HDF5Constants.H5P_DEFAULT);

                h5File = h5FileFormat.createInstance(probavFile.getAbsolutePath(), FileFormat.READ);
                h5File.open();

                final TreeNode rootNode = h5File.getRootNode();

                // check of which of the supported product types the input is:
                if (ProbaVSynthesisProductReaderPlugIn.isProbaSynthesisToaProduct(fileName) ||
                    ProbaVSynthesisProductReaderPlugIn.isProbaSynthesisTocProduct(fileName)) {
                    targetProduct = createTargetProductFromSynthesis(probavFile, rootNode);
                } else if (ProbaVSynthesisProductReaderPlugIn.isProbaSynthesisNdviProduct(fileName)) {
                    targetProduct = createTargetProductFromSynthesisNdvi(probavFile, rootNode);
                }
            } catch (Exception e) {
                throw new IOException("Failed to open file '" + probavFile.getPath() + "': " + e.getMessage());
            } finally {
                if (h5File != null) {
                    try {
                        h5File.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return targetProduct;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        throw new IllegalStateException(String.format("No source to read for band '%s'.", destBand.getName()));
    }

    //////////// private methods //////////////////

    private Product createTargetProductFromSynthesis(File inputFile, TreeNode inputFileRootNode) throws Exception {
        Product product = null;

        if (inputFileRootNode != null) {

            final TreeNode level3Node = inputFileRootNode.getChildAt(0);        // 'LEVEL3'
            productWidth = (int) ProbaVUtils.getH5ScalarDS(level3Node.getChildAt(0).getChildAt(0)).getDims()[0];
            productHeight = (int) ProbaVUtils.getH5ScalarDS(level3Node.getChildAt(0).getChildAt(0)).getDims()[1];
            product = new Product(inputFile.getName(), "PROBA-V SYNTHESIS", productWidth, productHeight);
            product.setPreferredTileSize(productWidth, 16);
            product.setAutoGrouping("TOA_REFL:TOC_REFL:VAA:VZA");

            final H5Group rootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
            final List rootMetadata = rootGroup.getMetadata();
            ProbaVUtils.addProbaVMetadataElement(rootMetadata, product, ProbaVConstants.MPH_NAME);
            product.setDescription(ProbaVUtils.getStringAttributeValue(rootMetadata, "DESCRIPTION"));
            product.setFileLocation(inputFile);

            for (int i = 0; i < level3Node.getChildCount(); i++) {
                // we have: 'GEOMETRY', 'NDVI', 'QUALITY', 'RADIOMETRY', 'TIME'
                final TreeNode level3ChildNode = level3Node.getChildAt(i);
                final String level3ChildNodeName = level3ChildNode.toString();

                switch (level3ChildNodeName) {
                    case "GEOMETRY":
                        setSynthesisGeoCoding(product, inputFileRootNode, level3ChildNode);
                        // 8-bit unsigned character
                        for (int j = 0; j < level3ChildNode.getChildCount(); j++) {
                            final TreeNode level3GeometryChildNode = level3ChildNode.getChildAt(j);
                            final String level3GeometryChildNodeName = level3GeometryChildNode.toString();
                            if (ProbaVUtils.isProbaVSunAngleDataNode(level3GeometryChildNodeName)) {
                                final H5ScalarDS sunAngleDS = ProbaVUtils.getH5ScalarDS(level3GeometryChildNode);
                                final Band sunAngleBand = ProbaVUtils.createTargetBand(product,
                                                                           sunAngleDS.getMetadata(),
                                                                           level3GeometryChildNodeName,
                                                                           ProductData.TYPE_UINT8);
                                ProbaVUtils.setBandUnitAndDescription(sunAngleDS.getMetadata(), sunAngleBand);
                                sunAngleBand.setNoDataValue(ProbaVConstants.GEOMETRY_NO_DATA_VALUE);
                                sunAngleBand.setNoDataValueUsed(true);

                                final String sunAngleDatasetName = "/LEVEL3/GEOMETRY/" + level3GeometryChildNodeName;
                                final int sunAngleDatatypeClass = sunAngleDS.getDatatype().getDatatypeClass();   // 0
                                final ProductData sunAngleRasterData =
                                        ProbaVUtils.getProbaVRasterData(file_id,
                                                                        productWidth, productHeight,
                                                                        sunAngleDatasetName,
                                                                        sunAngleDatatypeClass);
                                final RenderedImage sunAngleImage = ImageUtils.createRenderedImage(productWidth,
                                                                                                   productHeight,
                                                                                                   sunAngleRasterData);
                                sunAngleBand.setSourceImage(sunAngleImage);
                            } else if (ProbaVUtils.isProbaVViewAngleGroupNode(level3GeometryChildNodeName)) {
                                for (int k = 0; k < level3GeometryChildNode.getChildCount(); k++) {
                                    final TreeNode level3GeometryViewAngleChildNode = level3GeometryChildNode.getChildAt(k);
                                    final H5ScalarDS viewAngleDS = ProbaVUtils.getH5ScalarDS(level3GeometryViewAngleChildNode);
                                    final String level3GeometryViewAngleChildNodeName =
                                            level3GeometryViewAngleChildNode.toString();
                                    final String viewAnglebandName = level3GeometryViewAngleChildNodeName + "_" +
                                                                     level3GeometryChildNodeName;
                                    final Band viewAngleBand = ProbaVUtils.createTargetBand(product,
                                                                                viewAngleDS.getMetadata(),
                                                                                viewAnglebandName,
                                                                                ProductData.TYPE_UINT8);
                                    ProbaVUtils.setBandUnitAndDescription(viewAngleDS.getMetadata(), viewAngleBand);
                                    viewAngleBand.setNoDataValue(ProbaVConstants.GEOMETRY_NO_DATA_VALUE);
                                    viewAngleBand.setNoDataValueUsed(true);

                                    final String viewAngleDatasetName = "/LEVEL3/GEOMETRY/" +
                                                                        level3GeometryChildNodeName + "/" + level3GeometryViewAngleChildNodeName;
                                    final int viewAngleDatatypeClass = viewAngleDS.getDatatype().getDatatypeClass();   // 0
                                    final ProductData viewAngleRasterData = ProbaVUtils.getProbaVRasterData(file_id, productWidth, productHeight,
                                                                                                            viewAngleDatasetName,
                                                                                                            viewAngleDatatypeClass);
                                    final RenderedImage viewAngleImage = ImageUtils.createRenderedImage(productWidth,
                                                                                                        productHeight,
                                                                                                        viewAngleRasterData);
                                    viewAngleBand.setSourceImage(viewAngleImage);
                                }
                            }
                        }
                        break;

                    case "NDVI":
                        // 8-bit unsigned character
                        setNdviBand(product, level3ChildNode);
                        break;

                    case "QUALITY":
                        // 8-bit unsigned character
                        final H5ScalarDS qualityDS = ProbaVUtils.getH5ScalarDS(level3ChildNode.getChildAt(0));
                        Product flagProduct = new Product("QUALITY", "flags", productWidth, productHeight);
                        ProductUtils.copyGeoCoding(product, flagProduct);
                        final Band smBand =
                                ProbaVUtils.createTargetBand(flagProduct,
                                                             qualityDS.getMetadata(),
                                                             ProbaVConstants.SM_BAND_NAME,
                                                             ProductData.TYPE_UINT8);
                        ProbaVUtils.setBandUnitAndDescription(qualityDS.getMetadata(), smBand);

                        final String qualityDatasetName = "/LEVEL3/QUALITY/" + ProbaVConstants.SM_BAND_NAME;
                        final int qualityDatatypeClass = qualityDS.getDatatype().getDatatypeClass();
                        final ProductData qualityRasterData = ProbaVUtils.getProbaVRasterData(file_id, productWidth, productHeight,
                                                                                              qualityDatasetName,
                                                                                              qualityDatatypeClass);
                        final RenderedImage qualityImage = ImageUtils.createRenderedImage(productWidth,
                                                                                          productHeight,
                                                                                          qualityRasterData);
                        smBand.setSourceImage(qualityImage);

                        // attach flag band:
                        attachSynthesisQualityFlagBand(product, flagProduct);

                        // metadata:
                        ProbaVUtils.addQualityMetadata(product, (DefaultMutableTreeNode) level3ChildNode);

                        break;

                    case "RADIOMETRY":
                        // 16-bit integer
                        //  blue, nir, red, swir:
                        for (int j = 0; j < level3ChildNode.getChildCount(); j++) {
                            // we want the sequence BLUE, RED, NIR, SWIR, rather than original BLUE, NIR, RED, SWIR...
                            final int k = ProbaVConstants.RADIOMETRY_CHILD_INDEX[j];
                            final TreeNode level3RadiometryChildNode = level3ChildNode.getChildAt(k);
                            final H5ScalarDS radiometryDS = ProbaVUtils.getH5ScalarDS(level3RadiometryChildNode.getChildAt(0));
                            final String level3RadiometryChildNodeName = level3RadiometryChildNode.toString();
                            final String radiometryBandPrePrefix =
                                    (ProbaVSynthesisProductReaderPlugIn.isProbaSynthesisToaProduct(inputFile.getName())) ? "TOA" : "TOC";
                            final String radiometryBandPrefix = radiometryBandPrePrefix + "_REFL_";
                            final Band radiometryBand = ProbaVUtils.createTargetBand(product,
                                                                         radiometryDS.getMetadata(),
                                                                         radiometryBandPrefix + level3RadiometryChildNodeName,
                                                                         ProductData.TYPE_INT16);
                            ProbaVUtils.setBandUnitAndDescription(radiometryDS.getMetadata(), radiometryBand);
                            ProbaVUtils.setSpectralBandProperties(radiometryBand);
                            radiometryBand.setNoDataValue(ProbaVConstants.RADIOMETRY_NO_DATA_VALUE);
                            radiometryBand.setNoDataValueUsed(true);

                            final String radiometryDatasetName = "/LEVEL3/RADIOMETRY/" +
                                                                 level3RadiometryChildNodeName + "/" + radiometryBandPrePrefix;
                            final int radiometryDatatypeClass = radiometryDS.getDatatype().getDatatypeClass();
                            final ProductData radiometryRasterData = ProbaVUtils.getProbaVRasterData(file_id, productWidth, productHeight,
                                                                                                     radiometryDatasetName,
                                                                                                     radiometryDatatypeClass);
                            final RenderedImage radiometryImage = ImageUtils.createRenderedImage(productWidth,
                                                                                                 productHeight,
                                                                                                 radiometryRasterData);

                            radiometryBand.setSourceImage(radiometryImage);
                        }
                        break;

                    case "TIME":
                        final H5ScalarDS timeDS = ProbaVUtils.getH5ScalarDS(level3ChildNode.getChildAt(0));
                        final Band timeBand;
                        // NOTE: it seems that identical product types may have different data types here. E.g.:
                        // PROBAV_S1_TOC_X18Y06_20140316_100M_V001.HDF5 has 8-bit unsigned char (CLASS_CHAR), but
                        // PROBAV_S1_TOA_X18Y02_20140902_100M_V001.HDF5 has 16-bit unsigned integer (CLASS_INTEGER)
                        final int timeDatatypeClass = timeDS.getDatatype().getDatatypeClass();   // 0
                        if (timeDatatypeClass == H5Datatype.CLASS_CHAR) {
                            // 8-bit unsigned character in this case
                            timeBand = ProbaVUtils.createTargetBand(product, timeDS.getMetadata(), "TIME", ProductData.TYPE_UINT8);
                            timeBand.setNoDataValue(ProbaVConstants.TIME_NO_DATA_VALUE_UINT8);
                        } else {
                            // 16-bit unsigned integer
                            timeBand = ProbaVUtils.createTargetBand(product, timeDS.getMetadata(), "TIME", ProductData.TYPE_UINT16);
                            timeBand.setNoDataValue(ProbaVConstants.TIME_NO_DATA_VALUE_UINT16);
                        }
                        ProbaVUtils.setBandUnitAndDescription(timeDS.getMetadata(), timeBand);
                        timeBand.setNoDataValueUsed(true);

                        final String timeDatasetName = "/LEVEL3/TIME/TIME";
                        final ProductData timeRasterData = ProbaVUtils.getProbaVRasterData(file_id, productWidth, productHeight,
                                                                                           timeDatasetName,
                                                                                           timeDatatypeClass);
                        final RenderedImage radiometryImage = ImageUtils.createRenderedImage(productWidth,
                                                                                             productHeight,
                                                                                             timeRasterData);
                        timeBand.setSourceImage(radiometryImage);

                        // add start/end time to product:
                        ProbaVUtils.addStartStopTimes(product, (DefaultMutableTreeNode) level3ChildNode);
                        break;

                    default:
                        break;
                }
            }
        }

        return product;
    }

    private Product createTargetProductFromSynthesisNdvi(File inputFile, TreeNode inputFileRootNode) throws Exception {

        Product product = null;
        if (inputFileRootNode != null) {
            final TreeNode level3Node = inputFileRootNode.getChildAt(0);        // 'LEVEL3'
            productWidth = (int) ProbaVUtils.getH5ScalarDS(level3Node.getChildAt(1).getChildAt(0)).getDims()[0];
            productHeight = (int) ProbaVUtils.getH5ScalarDS(level3Node.getChildAt(1).getChildAt(0)).getDims()[1];
            product = new Product(inputFile.getName(), "PROBA-V SYNTHESIS NDVI", productWidth, productHeight);
            product.setPreferredTileSize(productWidth, 16);

            final H5Group rootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
            final List rootMetadata = rootGroup.getMetadata();
            ProbaVUtils.addProbaVMetadataElement(rootMetadata, product, ProbaVConstants.MPH_NAME);
            product.setDescription(ProbaVUtils.getStringAttributeValue(rootMetadata, "DESCRIPTION"));
            product.setFileLocation(inputFile);

            for (int i = 0; i < level3Node.getChildCount(); i++) {
                // we have: 'GEOMETRY', 'NDVI', 'QUALITY', 'TIME'
                final TreeNode level3ChildNode = level3Node.getChildAt(i);
                final String level3ChildNodeName = level3ChildNode.toString();

                switch (level3ChildNodeName) {
                    case "GEOMETRY":
                        setSynthesisGeoCoding(product, inputFileRootNode, level3ChildNode);
                        break;

                    case "NDVI":
                        // 8-bit unsigned character
                        setNdviBand(product, level3ChildNode);
                        break;

                    case "QUALITY":
                        ProbaVUtils.addQualityMetadata(product, (DefaultMutableTreeNode) level3ChildNode);
                        break;

                    case "TIME":
                        // add start/end time to product:
                        ProbaVUtils.addStartStopTimes(product, (DefaultMutableTreeNode) level3ChildNode);
                        break;

                    default:
                        break;
                }
            }
        }
        return product;
    }

    private void setNdviBand(Product product, TreeNode level3ChildNode) throws Exception {
        final H5ScalarDS ndviDS = (H5ScalarDS) ((DefaultMutableTreeNode) level3ChildNode.getChildAt(0)).getUserObject();
        final Band ndviBand = ProbaVUtils.createTargetBand(product, ndviDS.getMetadata(), "NDVI", ProductData.TYPE_UINT8);

        ndviBand.setDescription("Normalized Difference Vegetation Index");
        ndviBand.setUnit("dl");
        ndviBand.setNoDataValue(ProbaVConstants.NDVI_NO_DATA_VALUE);
        ndviBand.setNoDataValueUsed(true);

        final String ndviDatasetName = "/LEVEL3/NDVI/NDVI";
        final int ndviDatatypeClass = ndviDS.getDatatype().getDatatypeClass();
        final ProductData ndviRasterData = ProbaVUtils.getProbaVRasterData(file_id, productWidth, productHeight,
                                                                           ndviDatasetName, ndviDatatypeClass);

        final RenderedImage ndviImage = ImageUtils.createRenderedImage(productWidth,
                                                                       productHeight,
                                                                       ndviRasterData);
        ndviBand.setSourceImage(ndviImage);
    }

//    private void addStartStopTimes(Product product, DefaultMutableTreeNode level3ChildNode) throws HDF5Exception, ParseException {
//        final H5Group timeGroup = (H5Group) level3ChildNode.getUserObject();
//        final List timeMetadata = timeGroup.getMetadata();
//        product.setStartTime(ProductData.UTC.parse(ProbaVUtils.getStartEndTimeFromAttributes(timeMetadata)[0],
//                                                   ProbaVConstants.PROBAV_DATE_FORMAT_PATTERN));
//        product.setEndTime(ProductData.UTC.parse(ProbaVUtils.getStartEndTimeFromAttributes(timeMetadata)[1],
//                                                 ProbaVConstants.PROBAV_DATE_FORMAT_PATTERN));
//    }
//
//    private void addQualityMetadata(Product product, DefaultMutableTreeNode level3ChildNode) throws HDF5Exception {
//        final H5Group qualityGroup = (H5Group) level3ChildNode.getUserObject();
//        final List qualityMetadata = qualityGroup.getMetadata();
//        addSynthesisMetadataElement(qualityMetadata, product, ProbaVConstants.QUALITY_NAME);
//    }
//
//    private void setBandUnitAndDescription(H5ScalarDS ds, Band band) throws HDF5Exception {
//        band.setDescription(ProbaVUtils.getStringAttributeValue(ds.getMetadata(), "DESCRIPTION"));
//        band.setUnit(ProbaVUtils.getStringAttributeValue(ds.getMetadata(), "UNITS"));
//    }
//
//    private void setSpectralBandProperties(Band band) {
//        if (band.getName().endsWith("REFL_BLUE")) {
//            band.setSpectralBandIndex(0);
//            band.setSpectralWavelength(462.0f);
//            band.setSpectralBandwidth(48.0f);
//        } else if (band.getName().endsWith("REFL_RED")) {
//            band.setSpectralBandIndex(1);
//            band.setSpectralWavelength(655.5f);
//            band.setSpectralBandwidth(81.0f);
//        } else if (band.getName().endsWith("REFL_NIR")) {
//            band.setSpectralBandIndex(2);
//            band.setSpectralWavelength(843.0f);
//            band.setSpectralBandwidth(142.0f);
//        } else if (band.getName().endsWith("REFL_SWIR")) {
//            band.setSpectralBandIndex(3);
//            band.setSpectralWavelength(1599.0f);
//            band.setSpectralBandwidth(70.0f);
//        }
//    }

    private void setSynthesisGeoCoding(Product product, TreeNode inputFileRootNode, TreeNode level3ChildNode)
            throws HDF5Exception {

        final H5Group h5GeometryGroup = (H5Group) ((DefaultMutableTreeNode) level3ChildNode).getUserObject();
        final List geometryMetadata = h5GeometryGroup.getMetadata();
        final double easting = ProbaVUtils.getDoubleAttributeValue(geometryMetadata, "TOP_LEFT_LONGITUDE");
        final double northing = ProbaVUtils.getDoubleAttributeValue(geometryMetadata, "TOP_LEFT_LATITUDE");
        // pixel size: 10deg/rasterDim, it is also in the 6th and 7th value of MAPPING attribute in the raster nodes
        final double topLeftLon = easting;
        final double topRightLon = ProbaVUtils.getDoubleAttributeValue(geometryMetadata, "TOP_RIGHT_LONGITUDE");
        final double pixelSizeX = Math.abs(topRightLon - topLeftLon) / (productWidth - 1);
        final double topLeftLat = northing;
        final double bottomLeftLat = ProbaVUtils.getDoubleAttributeValue(geometryMetadata, "BOTTOM_LEFT_LATITUDE");
        final double pixelSizeY = (topLeftLat - bottomLeftLat) / (productHeight - 1);

        final H5Group h5RootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
        final List rootMetadata = h5RootGroup.getMetadata();
        final String crsString = ProbaVUtils.getStringAttributeValue(rootMetadata, "MAP_PROJECTION_WKT");
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
            final CrsGeoCoding geoCoding = new CrsGeoCoding(crs, productWidth, productHeight, easting, northing, pixelSizeX, pixelSizeY);
            product.setSceneGeoCoding(geoCoding);
        } catch (Exception e) {
            SystemUtils.LOG.log(Level.WARNING, "Cannot attach geocoding: " + e.getMessage());
        }
    }

    private static void attachSynthesisQualityFlagBand(Product probavProduct, Product flagProduct) {
        FlagCoding probavSmFlagCoding = new FlagCoding(ProbaVConstants.SM_FLAG_BAND_NAME);
        ProbaVUtils.addSynthesisQualityFlags(probavSmFlagCoding);
        ProbaVUtils.addSynthesisQualityMasks(probavProduct);
        probavProduct.getFlagCodingGroup().add(probavSmFlagCoding);
        final Band smFlagBand =
                probavProduct.addBand(ProbaVConstants.SM_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        smFlagBand.setDescription("PROBA-V Synthesis SM Flags");
        smFlagBand.setSampleCoding(probavSmFlagCoding);

        ProbaVBitMaskOp bitMaskOp = new ProbaVBitMaskOp();
        bitMaskOp.setParameterDefaultValues();
        bitMaskOp.setSourceProduct("sourceProduct", flagProduct);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        smFlagBand.setSourceImage(bitMaskProduct.getBand(ProbaVConstants.SM_FLAG_BAND_NAME).getSourceImage());
    }

//    private boolean isSynthesisViewAngleGroupNode(String level3GeometryChildNodeName) {
//        return level3GeometryChildNodeName.equals("SWIR") || level3GeometryChildNodeName.equals("VNIR");
//    }
//
//    private boolean isSynthesisSunAngleDataNode(String level3GeometryChildNodeName) {
//        return level3GeometryChildNodeName.equals("SAA") || level3GeometryChildNodeName.equals("SZA");
//    }

//    private Band createTargetBand(Product product, H5ScalarDS scalarDS, String bandName, int dataType) throws Exception {
//        final List<Attribute> metadata = scalarDS.getMetadata();
//        final double scaleFactorAttr = ProbaVUtils.getDoubleAttributeValue(metadata, "SCALE");
//        final double scaleFactor = Double.isNaN(scaleFactorAttr) ? 1.0f : scaleFactorAttr;
//        final double scaleOffsetAttr = ProbaVUtils.getDoubleAttributeValue(metadata, "OFFSET");
//        final double scaleOffset = Double.isNaN(scaleOffsetAttr) ? 0.0f : scaleOffsetAttr;
//        final Band band = product.addBand(bandName, dataType);
//        band.setScalingFactor(1.0 / scaleFactor);
//        band.setScalingOffset(-1.0 * scaleOffset / scaleFactor);
//
//        return band;
//    }
//
//    private H5ScalarDS getH5ScalarDS(TreeNode level3BandsChildNode) throws HDF5Exception {
//        H5ScalarDS scalarDS = (H5ScalarDS) ((DefaultMutableTreeNode) level3BandsChildNode).getUserObject();
//        scalarDS.open();
//        scalarDS.read();
//        // todo: how to optimize the chunk size???
//        // H5.H5Pset_chunk(scalarDS.getFID(), 2, new long[]{1000, 1000});   // something like this does not work, see http://www.slideshare.net/HDFEOS/hdf5-advanced, p.18
//        // H5.H5Pset_cache(0, 0, 0, 0, 0.0); // todo: what about this? Find suitable values.
//        return scalarDS;
//    }
//
//    private void addSynthesisMetadataElement(List<Attribute> rootMetadata,
//                                             final Product product,
//                                             String metadataElementName) {
//        final MetadataElement metadataElement = new MetadataElement(metadataElementName);
//
//        for (Attribute attribute : rootMetadata) {
//            metadataElement.addAttribute(new MetadataAttribute(attribute.getName(),
//                                                               ProductData.createInstance(ProbaVUtils.getAttributeValue(attribute)), true));
//        }
//        product.getMetadataRoot().addElement(metadataElement);
//    }

}
