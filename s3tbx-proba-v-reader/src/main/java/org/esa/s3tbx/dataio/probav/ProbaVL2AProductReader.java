package org.esa.s3tbx.dataio.probav;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h5.H5Group;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.SystemUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Reader for Proba-V L2A products
 *
 * @author olafd
 */
public class ProbaVL2AProductReader extends AbstractProductReader {

    private int productWidth;
    private int productHeight;

    private File probavFile;
    private int file_id;

    private HashMap<Band, DatasetVar> datasetVars;
    private Product flagProduct;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected ProbaVL2AProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object inputObject = getInput();
        probavFile = ProbaVL2AProductReaderPlugIn.getFileInput(inputObject);
        final String fileName = probavFile.getName();

        Product targetProduct = null;

        if (ProbaVL2AProductReaderPlugIn.isHdf5LibAvailable()) {
            FileFormat h5FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            FileFormat h5File = null;
            try {
                file_id = H5.H5Fopen(probavFile.getAbsolutePath(),  // Name of the file to access.
                                     HDF5Constants.H5F_ACC_RDONLY,  // File access flag
                                     HDF5Constants.H5P_DEFAULT);

                h5File = h5FileFormat.createInstance(probavFile.getAbsolutePath(), FileFormat.READ);
                h5File.open();

                final TreeNode rootNode = h5File.getRootNode();
                targetProduct = createTargetProductFromL2A(probavFile, rootNode);
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

//        attachL2AQualityFlagBand(targetProduct);
        return targetProduct;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band targetBand,
                                          int targetOffsetX,
                                          int targetOffsetY,
                                          int targetWidth,
                                          int targetHeight,
                                          ProductData targetBuffer,
                                          ProgressMonitor pm) throws IOException {

        Assert.state(sourceOffsetX == targetOffsetX, "sourceOffsetX != targetOffsetX");
        Assert.state(sourceOffsetY == targetOffsetY, "sourceOffsetY != targetOffsetY");
        Assert.state(sourceStepX == 1, "sourceStepX != 1");
        Assert.state(sourceStepY == 1, "sourceStepY != 1");
        Assert.state(sourceWidth == targetWidth, "sourceWidth != targetWidth");
        Assert.state(sourceHeight == targetHeight, "sourceHeight != targetHeight");

        final DatasetVar datasetVar = datasetVars.get(targetBand);
        synchronized (datasetVar) {
            if (datasetVar.getName().equals("/LEVEL2A/QUALITY/" + ProbaVConstants.SM_BAND_NAME) &&
                    targetBand.getName().equals(ProbaVConstants.SM_FLAG_BAND_NAME)) {
                ProductData tmpBuffer =
                        ProbaVUtils.getDataBufferForH5Dread(datasetVar.getType(), targetWidth, targetHeight);
                ProbaVUtils.readProbaVData(file_id,
                                           targetWidth, targetHeight,
                                           targetOffsetX, targetOffsetY,
                                           datasetVar.getName(),
                                           datasetVar.getType(),
                                           tmpBuffer);
                setSmFlagBuffer(targetBuffer, tmpBuffer);
            } else {
                ProbaVUtils.readProbaVData(file_id,
                                           targetWidth, targetHeight,
                                           targetOffsetX, targetOffsetY,
                                           datasetVar.getName(),
                                           datasetVar.getType(),
                                           targetBuffer);
            }
        }
    }

    private void setSmFlagBuffer(ProductData targetBuffer, ProductData tmpBuffer) {
        for (int i = 0; i < targetBuffer.getNumElems(); i++) {
            final int qualityValue = tmpBuffer.getElemIntAt(i);
            int smFlagValue = 0;
            final boolean clear = ProbaVBitMaskOp.isClear(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_CLEAR_BIT_INDEX, clear);
            final boolean cloud = ProbaVBitMaskOp.isCloud(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_CLOUD_BIT_INDEX, cloud);
            final boolean undefined = ProbaVBitMaskOp.isUndefined(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_UNDEFINED_BIT_INDEX, undefined);
            final boolean cloudShadow = ProbaVBitMaskOp.isCloudShadow(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_CLOUD_SHADOW_BIT_INDEX, cloudShadow);

            final boolean land = ProbaVBitMaskOp.isLand(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_LAND_BIT_INDEX, land);
            final boolean snowice = ProbaVBitMaskOp.isSnowIce(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_SNOWICE_INDEX, snowice);

            final boolean goodBlue = ProbaVBitMaskOp.isGoodBlue(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_GOOD_BLUE_BIT_INDEX, goodBlue);
            final boolean goodRed = ProbaVBitMaskOp.isGoodRed(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_GOOD_RED_BIT_INDEX, goodRed);
            final boolean goodNir = ProbaVBitMaskOp.isGoodNir(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_GOOD_NIR_BIT_INDEX, goodNir);
            final boolean goodSwir = ProbaVBitMaskOp.isGoodSwir(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_GOOD_SWIR_INDEX, goodSwir);

            final boolean blueCoverage = ProbaVBitMaskOp.isBlueCoverage(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_BLUE_COVERAGE_BIT_INDEX, blueCoverage);
            final boolean redCoverage = ProbaVBitMaskOp.isRedCoverage(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_RED_COVERAGE_BIT_INDEX, redCoverage);
            final boolean nirCoverage = ProbaVBitMaskOp.isNirCoverage(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_NIR_COVERAGE_BIT_INDEX, nirCoverage);
            final boolean swirCoverage = ProbaVBitMaskOp.isSwirCoverage(qualityValue);
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_SWIR_COVERAGE_INDEX, swirCoverage);

            targetBuffer.setElemIntAt(i, smFlagValue);
        }
    }

    //////////// private methods //////////////////

    private Product createTargetProductFromL2A(File inputFile, TreeNode inputFileRootNode) throws Exception {
        Product product = null;

        if (inputFileRootNode != null) {

            final TreeNode level2ANode = inputFileRootNode.getChildAt(0);        // 'LEVEL2A'
            productWidth = (int) ProbaVUtils.getH5ScalarDS(level2ANode.getChildAt(0).getChildAt(1)).getDims()[1];   // take from SAA
            productHeight = (int) ProbaVUtils.getH5ScalarDS(level2ANode.getChildAt(0).getChildAt(1)).getDims()[0];
            product = new Product(inputFile.getName(), "PROBA-V L2A", productWidth, productHeight);
            product.setPreferredTileSize(productWidth, 16);
            product.setAutoGrouping("TOA_REFL:VAA:VZA");

            datasetVars = new HashMap<Band, DatasetVar>(32);

            final H5Group rootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
            final List rootMetadata = rootGroup.getMetadata();
            ProbaVUtils.addProbaVMetadataElement(rootMetadata, product, ProbaVConstants.MPH_NAME);
            product.setDescription(ProbaVUtils.getStringAttributeValue(rootMetadata, "DESCRIPTION"));
            product.setFileLocation(inputFile);

            for (int i = 0; i < level2ANode.getChildCount(); i++) {
                // we have: 'GEOMETRY', 'QUALITY', 'RADIOMETRY'
                final TreeNode level2AChildNode = level2ANode.getChildAt(i);
                final String level2AChildNodeName = level2AChildNode.toString();

                switch (level2AChildNodeName) {
                    case "GEOMETRY":
                        setL2AGeoCoding(product, inputFileRootNode, level2AChildNode);   // todo
                        // 8-bit unsigned character
                        for (int j = 1; j < level2AChildNode.getChildCount(); j++) {     // skip 'CONTOUR'
                            final TreeNode level2AGeometryChildNode = level2AChildNode.getChildAt(j);
                            final String level2AGeometryChildNodeName = level2AGeometryChildNode.toString();
                            if (ProbaVUtils.isProbaVSunAngleDataNode(level2AGeometryChildNodeName)) {
                                final H5ScalarDS sunAngleDS = ProbaVUtils.getH5ScalarDS(level2AGeometryChildNode);
                                final Band sunAngleBand = ProbaVUtils.createTargetBand(product,
                                                                                       sunAngleDS.getMetadata(),
                                                                                       level2AGeometryChildNodeName,
                                                                                       ProductData.TYPE_UINT8);
                                ProbaVUtils.setBandUnitAndDescription(sunAngleDS.getMetadata(), sunAngleBand);
                                sunAngleBand.setNoDataValue(ProbaVConstants.GEOMETRY_NO_DATA_VALUE);
                                sunAngleBand.setNoDataValueUsed(true);

                                final String sunAngleDatasetName = "/LEVEL2A/GEOMETRY/" + level2AGeometryChildNodeName;
                                final int sunAngleDatatypeClass = sunAngleDS.getDatatype().getDatatypeClass();   // 0
                                datasetVars.put(sunAngleBand,
                                                new ProbaVL2AProductReader.DatasetVar(sunAngleDatasetName,
                                                                                      sunAngleDatatypeClass));
                            } else if (ProbaVUtils.isProbaVViewAngleGroupNode(level2AGeometryChildNodeName)) {
                                for (int k = 0; k < level2AGeometryChildNode.getChildCount(); k++) {
                                    final TreeNode level2AGeometryViewAngleChildNode = level2AGeometryChildNode.getChildAt(k);
                                    final H5ScalarDS viewAngleDS = ProbaVUtils.getH5ScalarDS(level2AGeometryViewAngleChildNode);
                                    final String level2AGeometryViewAngleChildNodeName =
                                            level2AGeometryViewAngleChildNode.toString();
                                    final String viewAnglebandName = level2AGeometryViewAngleChildNodeName + "_" +
                                            level2AGeometryChildNodeName;
                                    final Band viewAngleBand = ProbaVUtils.createTargetBand(product,
                                                                                            viewAngleDS.getMetadata(),
                                                                                            viewAnglebandName,
                                                                                            ProductData.TYPE_UINT8);
                                    ProbaVUtils.setBandUnitAndDescription(viewAngleDS.getMetadata(), viewAngleBand);
                                    viewAngleBand.setNoDataValue(ProbaVConstants.GEOMETRY_NO_DATA_VALUE);
                                    viewAngleBand.setNoDataValueUsed(true);

                                    final String viewAngleDatasetName = "/LEVEL2A/GEOMETRY/" +
                                            level2AGeometryChildNodeName + "/" + level2AGeometryViewAngleChildNodeName;
                                    final int viewAngleDatatypeClass = viewAngleDS.getDatatype().getDatatypeClass();   // 0
                                    datasetVars.put(viewAngleBand,
                                                    new ProbaVL2AProductReader.DatasetVar(viewAngleDatasetName,
                                                                                          viewAngleDatatypeClass));
                                }
                            }
                        }
                        break;

                    case "QUALITY":
                        // 8-bit unsigned character
                        final H5ScalarDS qualityDS = ProbaVUtils.getH5ScalarDS(level2AChildNode.getChildAt(0));
                        flagProduct = new Product("QUALITY", "flags", productWidth, productHeight);

                        FlagCoding probavSmFlagCoding = new FlagCoding(ProbaVConstants.SM_FLAG_BAND_NAME);
                        ProbaVUtils.addL2AQualityFlags(probavSmFlagCoding);
                        ProbaVUtils.addL2AQualityMasks(product);
                        product.getFlagCodingGroup().add(probavSmFlagCoding);
                        final Band smFlagBand =
                                product.addBand(ProbaVConstants.SM_FLAG_BAND_NAME, ProductData.TYPE_INT16);
                        smFlagBand.setDescription("PROBA-V L2A SM Flags");
                        smFlagBand.setSampleCoding(probavSmFlagCoding);


//                        final Band smBand =
//                                ProbaVUtils.createTargetBand(flagProduct,
//                                                             qualityDS.getMetadata(),
//                                                             ProbaVConstants.SM_BAND_NAME,
//                                                             ProductData.TYPE_UINT16);  // different to Synthesis product!
//                        ProbaVUtils.setBandUnitAndDescription(qualityDS.getMetadata(), smBand);

                        final String qualityDatasetName = "/LEVEL2A/QUALITY/" + ProbaVConstants.SM_BAND_NAME;
                        final int qualityDatatypeClass = qualityDS.getDatatype().getDatatypeClass();
                        datasetVars.put(smFlagBand,
                                        new ProbaVL2AProductReader.DatasetVar(qualityDatasetName,
                                                                              qualityDatatypeClass));

//                        final ProductData qualityRasterData = ProbaVUtils.getProbaVRasterData(file_id, productWidth, productHeight,
//                                                                                              qualityDatasetName,
//                                                                                              qualityDatatypeClass);
//                        final RenderedImage qualityImage = ImageUtils.createRenderedImage(productWidth,
//                                                                                          productHeight,
//                                                                                          qualityRasterData);
//                        smBand.setSourceImage(qualityImage);
//
//                        // attach flag band:
//                        attachL2AQualityFlagBand(product);

                        // metadata:
                        ProbaVUtils.addQualityMetadata(product, (DefaultMutableTreeNode) level2AChildNode);

                        break;

                    case "RADIOMETRY":
                        // 16-bit integer
                        //  blue, nir, red, swir:
                        for (int j = 0; j < level2AChildNode.getChildCount(); j++) {
                            // we want the sequence BLUE, RED, NIR, SWIR, rather than original BLUE, NIR, RED, SWIR...
                            final int k = ProbaVConstants.RADIOMETRY_CHILD_INDEX[j];
                            final TreeNode level2ARadiometryChildNode = level2AChildNode.getChildAt(k);
                            final H5ScalarDS radiometryDS = ProbaVUtils.getH5ScalarDS(level2ARadiometryChildNode.getChildAt(0));
                            final String level2ARadiometryChildNodeName = level2ARadiometryChildNode.toString();
                            final String radiometryBandPrePrefix = "TOA";
                            final String radiometryBandPrefix = radiometryBandPrePrefix + "_REFL_";
                            final Band radiometryBand = ProbaVUtils.createTargetBand(product,
                                                                                     radiometryDS.getMetadata(),
                                                                                     radiometryBandPrefix + level2ARadiometryChildNodeName,
                                                                                     ProductData.TYPE_INT16);
                            ProbaVUtils.setBandUnitAndDescription(radiometryDS.getMetadata(), radiometryBand);
                            ProbaVUtils.setSpectralBandProperties(radiometryBand);
                            radiometryBand.setNoDataValue(ProbaVConstants.RADIOMETRY_NO_DATA_VALUE);
                            radiometryBand.setNoDataValueUsed(true);

                            final String radiometryDatasetName = "/LEVEL2A/RADIOMETRY/" +
                                    level2ARadiometryChildNodeName + "/" + radiometryBandPrePrefix;
                            final int radiometryDatatypeClass = radiometryDS.getDatatype().getDatatypeClass();
                            datasetVars.put(radiometryBand,
                                            new ProbaVL2AProductReader.DatasetVar(radiometryDatasetName,
                                                                                  radiometryDatatypeClass));

//                            final ProductData radiometryRasterData = ProbaVUtils.getProbaVRasterData(file_id, productWidth, productHeight,
//                                                                                                     radiometryDatasetName,
//                                                                                                     radiometryDatatypeClass);
//                            final RenderedImage radiometryImage = ImageUtils.createRenderedImage(productWidth,
//                                                                                                 productHeight,
//                                                                                                 radiometryRasterData);
//
//                            radiometryBand.setSourceImage(radiometryImage);
                        }
                        break;

                    default:
                        break;
                }
            }
        }

        return product;
    }

    private void setL2AGeoCoding(Product product, TreeNode inputFileRootNode, TreeNode level2AChildNode)
            throws HDF5Exception {

        final H5Group h5GeometryGroup = (H5Group) ((DefaultMutableTreeNode) level2AChildNode).getUserObject();
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

    //    private static void attachL2AQualityFlagBand(Product probavProduct, Product flagProduct) {
    private void attachL2AQualityFlagBand(Product probavProduct) {
        FlagCoding probavSmFlagCoding = new FlagCoding(ProbaVConstants.SM_FLAG_BAND_NAME);
        ProbaVUtils.addL2AQualityFlags(probavSmFlagCoding);
        ProbaVUtils.addL2AQualityMasks(probavProduct);
        probavProduct.getFlagCodingGroup().add(probavSmFlagCoding);
        final Band smFlagBand =
                probavProduct.addBand(ProbaVConstants.SM_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        smFlagBand.setDescription("PROBA-V L2A SM Flags");
        smFlagBand.setSampleCoding(probavSmFlagCoding);

        ProbaVBitMaskOp bitMaskOp = new ProbaVBitMaskOp();
        bitMaskOp.setParameterDefaultValues();
        bitMaskOp.setParameter("probavProductType", "L2A");
        bitMaskOp.setSourceProduct("sourceProduct", flagProduct);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        smFlagBand.setSourceImage(bitMaskProduct.getBand(ProbaVConstants.SM_FLAG_BAND_NAME).getSourceImage());
    }

    static class DatasetVar {

        final String name;
        final int type;

        public DatasetVar(String name, int type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return type;
        }
    }

}
