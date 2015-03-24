package org.esa.beam.dataio;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h5.H5Group;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 11.03.2015
 * Time: 16:25
 *
 * @author olafd
 */
public class ProbaVProductReader extends AbstractProductReader {

    private int productWidth;
    private int productHeight;

    private File probavFile;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected ProbaVProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object inputObject = getInput();
        probavFile = ProbaVProductReaderPlugIn.getFileInput(inputObject);
        final String fileName = probavFile.getName();

        Product targetProduct = null;

        if (ProbaVProductReaderPlugIn.isHdf5LibAvailable()) {
            FileFormat h5FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            FileFormat h5File = null;
            try {
                h5File = h5FileFormat.createInstance(probavFile.getAbsolutePath(), FileFormat.READ);
                final int h5FileId = h5File.open();
                System.out.println("h5FileId = " + h5FileId);

                final TreeNode rootNode = h5File.getRootNode();

                // check of which of the supported product types the input is:
                if (ProbaVProductReaderPlugIn.isProbaL1CProduct(fileName)) {
                    targetProduct = createTargetProductFromL1C(probavFile, rootNode);
                } else if (ProbaVProductReaderPlugIn.isProbaSynthesisProduct(fileName)) {
                    targetProduct = createTargetProductFromSynthesis(probavFile, rootNode);
                } else if (ProbaVProductReaderPlugIn.isProbaS10TocNdviProduct(fileName)) {
                    targetProduct = createTargetProductFromS10TocNdvi(probavFile, rootNode);
                }
            } catch (Exception e) {
                e.printStackTrace();      // todo
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

    private Product createTargetProductFromS10TocNdvi(File inputFile, TreeNode rootNode) {
        // todo: do we need this?
        return null;
    }

    private Product createTargetProductFromSynthesis(File inputFile, TreeNode inputFileRootNode) throws Exception {
        Product product = null;

        if (inputFileRootNode != null) {

            // todo (see e.g. Chris-Proba reader):
            // - product metadata
            // - band properties and metadata
            // - exception handling
            // - closing of files/products
            // - extract constants
            // - no data values


            final TreeNode level3Node = inputFileRootNode.getChildAt(0);        // 'LEVEL3'
            productWidth = (int) getH5ScalarDS(level3Node.getChildAt(0).getChildAt(0)).getDims()[0];
            productHeight = (int) getH5ScalarDS(level3Node.getChildAt(0).getChildAt(0)).getDims()[1];
            product = new Product(inputFile.getName(), "PROBA-V SYNTHESIS", productWidth, productHeight);
            product.setAutoGrouping("TOA_REFL:TOC_REFL:VAA:VZA");

            final H5Group rootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
            final List rootMetadata = rootGroup.getMetadata();
            product.setDescription(ProbaVUtils.getProductDescription(rootMetadata));
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
                            System.out.println("level3GeometryChildNode = " + level3GeometryChildNodeName);
                            if (isSynthesisSunAngleDataNode(level3GeometryChildNodeName)) {
                                final H5ScalarDS sunAngleDS = getH5ScalarDS(level3GeometryChildNode);
                                final Band sunAngleBand = createTargetBand(product, sunAngleDS, level3GeometryChildNodeName,
                                                                           ProductData.TYPE_UINT8);
                                final byte[] sunAngleData = (byte[]) sunAngleDS.getData();
                                final ProbaVRasterImage ndviImage = new ProbaVRasterImage(sunAngleBand, sunAngleData);
                                sunAngleBand.setSourceImage(ndviImage);
                            } else if (isSynthesisViewAngleGroupNode(level3GeometryChildNodeName)) {
                                for (int k = 0; k < level3GeometryChildNode.getChildCount(); k++) {
                                    final TreeNode level3GeometryViewAngleChildNode = level3GeometryChildNode.getChildAt(k);
                                    final H5ScalarDS geometryViewAngleDS = getH5ScalarDS(level3GeometryViewAngleChildNode);
                                    final String level3GeometryViewAngleChildNodeName =
                                            level3GeometryViewAngleChildNode.toString();
                                    final Band geometryViewAngleBand = createTargetBand(product,
                                                                                        geometryViewAngleDS,
                                                                                        level3GeometryViewAngleChildNodeName + "_" + level3GeometryChildNodeName,
                                                                                        ProductData.TYPE_UINT8);
                                    final byte[] geometryViewAngleData = (byte[]) geometryViewAngleDS.getData();
                                    final ProbaVRasterImage geometryViewAngleImage =
                                            new ProbaVRasterImage(geometryViewAngleBand, geometryViewAngleData);
                                    geometryViewAngleBand.setSourceImage(geometryViewAngleImage);
                                }
                            }
                        }
                        break;
                    case "NDVI":
                        // 8-bit unsigned character
                        final H5ScalarDS ndviDS = getH5ScalarDS(level3ChildNode.getChildAt(0));
//                        final Band ndviBand = createTargetBand(product, ndviDS, "NDVI", ProductData.TYPE_UINT8);
                        final Band ndviBand = createTargetBand(product, ndviDS, "NDVI", ProductData.TYPE_FLOAT32);
                        final byte[] ndviData = (byte[]) ndviDS.getData();
                        // todo: the scaling with the original data looks weird, check why!
                        // for the moment use workaround: convert to scaled floats manually
                        final float[] ndviFloatData = ProbaVUtils.getNdviAsFloat(ndviBand, ndviData);
//                        final ProbaVRasterImage ndviImage = new ProbaVRasterImage(ndviBand, ndviData);
                        final ProbaVRasterImage ndviImage = new ProbaVRasterImage(ndviBand, ndviFloatData);
                        ndviBand.setSourceImage(ndviImage);
                        break;
                    case "QUALITY":
                        // 8-bit unsigned character
                        final H5ScalarDS qualityDS = getH5ScalarDS(level3ChildNode.getChildAt(0));
                        Product flagProduct = new Product("QUALITY", "flags", productWidth, productHeight);
                        final Band smBand =
                                createTargetBand(flagProduct, qualityDS, ProbaVConstants.SM_BAND_NAME, ProductData.TYPE_UINT8);
                        final byte[] qualityData = (byte[]) qualityDS.getData();
                        final ProbaVRasterImage image = new ProbaVRasterImage(smBand, qualityData);
                        smBand.setSourceImage(image);
                        attachSynthesisQualityFlagBand(product, flagProduct);
                        break;
                    case "RADIOMETRY":
                        // 16-bit integer
                        //  blue, nir, red, swir:
                        for (int j = 0; j < level3ChildNode.getChildCount(); j++) {
                            final TreeNode level3RadiometryChildNode = level3ChildNode.getChildAt(j);
                            final H5ScalarDS radiometryDS = getH5ScalarDS(level3RadiometryChildNode.getChildAt(0));
                            final String level3RadiometryChildNodeName = level3RadiometryChildNode.toString();
                            final String radiometryBandPrefix =
                                    (ProbaVProductReaderPlugIn.isProbaS1ToaProduct(inputFile.getName())) ?
                                            "TOA_REFL_" : "TOC_REFL_";
                            final Band radiometryBand = createTargetBand(product,
                                                                         radiometryDS,
                                                                         radiometryBandPrefix + level3RadiometryChildNodeName,
                                                                         ProductData.TYPE_INT16);
                            final short[] radiometryData = (short[]) radiometryDS.getData();
                            final ProbaVRasterImage radiometryImage = new ProbaVRasterImage(radiometryBand, radiometryData);
                            radiometryBand.setSourceImage(radiometryImage);
                        }
                        break;
                    case "TIME":
                        // 16-bit unsigned integer
                        final H5ScalarDS timeDS = getH5ScalarDS(level3ChildNode.getChildAt(0));
                        final Band timeBand = createTargetBand(product, timeDS, "TIME", ProductData.TYPE_UINT16);
                        final short[] timeData = (short[]) timeDS.getData();
                        final ProbaVRasterImage timeImage = new ProbaVRasterImage(timeBand, timeData);
                        timeBand.setSourceImage(timeImage);

                        // add start/end time to product:
                        final H5Group timeGroup = (H5Group) ((DefaultMutableTreeNode) level3ChildNode).getUserObject();
                        final List timeMetadata = timeGroup.getMetadata();
                        product.setStartTime(ProductData.UTC.parse(ProbaVUtils.getStartEndTime(timeMetadata)[0],
                                                                   ProbaVConstants.PROBAV_DATE_FORMAT_PATTERN));
                        product.setEndTime(ProductData.UTC.parse(ProbaVUtils.getStartEndTime(timeMetadata)[1],
                                                                 ProbaVConstants.PROBAV_DATE_FORMAT_PATTERN));
                        break;
                    default:
                        break;
                }
            }
        }

        return product;
    }

    private Product createTargetProductFromL1C(File inputFile, TreeNode inputFileRootNode) throws Exception {
        // todo: we need more detailed specifications how to handle the setup of the different cameras and bands
        // of the L1C products (see Fig. 2 in Product User Manual). Everything below was just experimental so far.
        // Note that the products may also be very large (~60000 x 5200 size of full raster)

        Product product = null;
//        Product[] swirProduct = new Product[3];
//        int swirProductIndex = 0;
//
//        if (inputFileRootNode != null) {
//            final TreeNode level1aNode = inputFileRootNode.getChildAt(0);               // 'LEVEL1A'
//            // todo: do we need any metadata from this group?
//
//            final TreeNode level1bNode = inputFileRootNode.getChildAt(1);               // 'LEVEL1B'
//            // todo: do we need any metadata from this group?
//            // todo: extract lat/lon info from LN1/LT1 and set up tie point geocoding
//            // careful: we have different lat/lon tie point grids again for blue/nir/red vs. SWIR
//            // also, downscaling factor with regard to the L1C raster data is not integer!
//
//            final TreeNode level1cNode = inputFileRootNode.getChildAt(2);               // 'LEVEL1C'
//
//            // when setting up the product, we want to have the SWIR band sizes as reference:
//            int swirHeight = -1;
//            int swirWidth = -1;
//            int highResHeight = -1;
//            int highResWidth = -1;
//            for (int i = 0; i < level1cNode.getChildCount(); i++) {
//                final TreeNode level1cChildNode = level1cNode.getChildAt(i);
//                final String level1cChildNodeName = level1cChildNode.toString();
//                final boolean isSwirBand = level1cChildNodeName.startsWith("SWIR");
//                if (product == null && !isSwirBand) {
//                    final H5ScalarDS ds = getH5ScalarDS(level1cNode.getChildAt(i).getChildAt(0));
//                    highResHeight = (int) ds.getDims()[0];
//                    highResWidth = (int) ds.getDims()[1];
//                    product = new Product(inputFile.getName(), "PROBA-V L1C", highResWidth, highResHeight);
//                    product.setAutoGrouping("BLUE:NIR:RED:SWIR1:SWIR2:SWIR3");
//                    setL1cGeoCoding(product, level1bNode);   // todo (see below)
//                    ds.clear();
//                    ds.close(0);
//                } else if (isSwirBand) {
////                    swirHeight = (int) ds.getDims()[0];
////                    swirWidth = (int) ds.getDims()[1];
//                }
////                ds.clear();
////                ds.close(0);
//            }
//
//            for (int i = 0; i < level1cNode.getChildCount(); i++) {
//                final TreeNode level1cChildNode = level1cNode.getChildAt(i);
//                final String level1cChildNodeName = level1cChildNode.toString();
//                for (int j = 0; j < level1cChildNode.getChildCount(); j++) {
//                    // we have 'Q' and 'TOA'
//                    final TreeNode level1cRadiometryChildNode = level1cChildNode.getChildAt(j);
//                    final String level1cRadiometryChildNodeName = level1cRadiometryChildNode.toString();
//                    System.out.println("level1cRadiometryChildNode = " + level1cRadiometryChildNodeName);
//                    final H5ScalarDS radiometryDS = getH5ScalarDS(level1cRadiometryChildNode);
//                    final String radiometryBandName = level1cChildNodeName + "_" + level1cRadiometryChildNodeName;
//                    if (level1cRadiometryChildNodeName.equals("Q")) {
//                        // 8-bit unsigned character
//                        if (!level1cChildNodeName.startsWith("SWIR")) {
//                            // on the high-res grid, we have: 'BLUE', 'NIR', 'RED'
//                            Product flagProduct = new Product(radiometryBandName, "flags", highResWidth, highResHeight);
//                            final Band qBand =
//                                    createTargetBand(flagProduct, radiometryDS, radiometryBandName, ProductData.TYPE_UINT8);
//                            final byte[] qualityData = (byte[]) radiometryDS.getData();
//                            final ProbaVRasterImage image = new ProbaVRasterImage(qBand, qualityData);
//                            qBand.setSourceImage(image);
//                            attachL1cQualityFlagBand(product, flagProduct, radiometryBandName);
//                        } else {
//                            // todo, see below
//                        }
//                    } else if (level1cRadiometryChildNodeName.equals("TOA")) {
//                        // 16-bit integer
//                        if (!level1cChildNodeName.startsWith("SWIR")) {
//                            // on the high-res grid, we have: 'BLUE', 'NIR', 'RED'
//                            final Band radiometryBand = createTargetBand(product,
//                                                                         radiometryDS,
//                                                                         radiometryBandName + "_REFL",
//                                                                         ProductData.TYPE_INT16);
//                            final short[] radiometryData = (short[]) radiometryDS.getData();
//                            // on the low-res grid, we have: 'SWIR1', 'SWIR2', 'SWIR3'
//                            final ProbaVRasterImage radiometryImage = new ProbaVRasterImage(radiometryBand, radiometryData);
//                            radiometryBand.setSourceImage(radiometryImage);
//                        } else {
//                            // todo: in L1C product we have two different sizes:
//                            // blue, nir, red: e.g. 18984 x 5200 (y=5200 is fix)
//                            // swir 1-3:   9492*1024 (y=1024 is fix)
//                            // --> BEAM: downscale the blue, nir, red bands, as e.g. in Modis35ProductReader (GA)
//                            // --> SNAP: use new functionality for different raster sizes
//
//                            swirProduct[swirProductIndex] =
//                                    new Product(radiometryBandName, "flags", swirWidth, swirHeight);
//                            final Band swirBand = createTargetBand(swirProduct[swirProductIndex],
//                                                                         radiometryDS,
//                                                                         radiometryBandName + "_REFL",
//                                                                         ProductData.TYPE_INT16);
//                            final short[] swirData = (short[]) radiometryDS.getData();
//                            final TiePointGrid latTpg = product.getTiePointGrid(level1cChildNodeName + "_LT1");
//                            final TiePointGrid lonTpg = product.getTiePointGrid(level1cChildNodeName + "_LN1");
//                            swirProduct[swirProductIndex].addTiePointGrid(latTpg);
//                            swirProduct[swirProductIndex].addTiePointGrid(lonTpg);
//                            product.removeTiePointGrid(latTpg);
//                            product.removeTiePointGrid(lonTpg);
//                            swirProduct[swirProductIndex].setGeoCoding(new TiePointGeoCoding(latTpg, lonTpg));
//                            final ProbaVRasterImage radiometryImage = new ProbaVRasterImage(swirBand, swirData);
//                            swirBand.setSourceImage(radiometryImage);
//                            swirProductIndex++;
//                        }
//                    }
//                    radiometryDS.clear();
//                    radiometryDS.close(0);
//                }
//            }
//        }
//
//        // todo: do collocation with SWIR 'single band' temporal products here ?? weird!
////        Product collocateSwirProduct = null;
////        collocateSwirProduct = collocateL1cRadiometryProducts(product, swirProduct[0], "SWIR" + 0);
////        collocateSwirProduct.setAutoGrouping("BLUE:NIR:RED:SWIR1:SWIR2:SWIR3");
////        for (int i=0; i<3; i++) {
////            collocateSwirProduct = collocateL1cRadiometryProducts(product, swirProduct[i], "SWIR" + i);
////        }

//        return collocateSwirProduct;
        return product;
    }

//    private Product collocateL1cRadiometryProducts(Product probavProduct, Product swirRadiometryProduct, String radiometryBandName) {
//        CollocateOp collocateOp = new CollocateOp();
//        collocateOp.setParameterDefaultValues();
//        collocateOp.setMasterProduct(probavProduct);
//        collocateOp.setSlaveProduct(swirRadiometryProduct);
//        collocateOp.setRenameMasterComponents(false);
//        collocateOp.setRenameSlaveComponents(false);
//        return collocateOp.getTargetProduct();
//    }

    private void setSynthesisGeoCoding(Product product, TreeNode inputFileRootNode, TreeNode level3ChildNode)
            throws HDF5Exception {

        final H5Group h5GeometryGroup = (H5Group) ((DefaultMutableTreeNode) level3ChildNode).getUserObject();
        final List geometryMetadata = h5GeometryGroup.getMetadata();
        final double easting = ProbaVUtils.getGeometryCoordinateValue(geometryMetadata, "TOP_LEFT_LONGITUDE");
        final double northing = ProbaVUtils.getGeometryCoordinateValue(geometryMetadata, "TOP_LEFT_LATITUDE");
        // pixel size: 10deg/rasterDim, it is also in the 6th and 7th value of MAPPING attribute in the raster nodes
        final double topLeftLon = easting;
        final double topRightLon = ProbaVUtils.getGeometryCoordinateValue(geometryMetadata, "TOP_RIGHT_LONGITUDE");
        final double pixelSizeX = Math.abs(topRightLon - topLeftLon) / productWidth;
        final double topLeftLat = northing;
        final double bottomLeftLat = ProbaVUtils.getGeometryCoordinateValue(geometryMetadata, "BOTTOM_LEFT_LATITUDE");
        final double pixelSizeY = (topLeftLat - bottomLeftLat) / productHeight;

        final H5Group h5RootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
        final List rootMetadata = h5RootGroup.getMetadata();
        final String crsString = ProbaVUtils.getGeometryCrsString(rootMetadata);
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
            final CrsGeoCoding geoCoding = new CrsGeoCoding(crs, productWidth, productHeight, easting, northing, pixelSizeX, pixelSizeY);
            product.setGeoCoding(geoCoding);
        } catch (Exception e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot attach geocoding: " + e.getMessage());
        }
    }

    private void setL1cGeoCoding(Product product, TreeNode level1bNode)
            throws Exception {
        int width = product.getSceneRasterWidth() / ProbaVConstants.L1C_TIEPOINT_SUBS_X + 1;      // the 'plus 1' is important!!!
        int height = product.getSceneRasterHeight() / ProbaVConstants.L1C_TIEPOINT_SUBS_Y + 1;

        for (int i = 0; i < level1bNode.getChildCount(); i++) {
            final TreeNode level1bChildNode = level1bNode.getChildAt(i);
            final String level1bChildNodeName = level1bChildNode.toString();
            System.out.println("level1bChildNodeName = " + level1bChildNodeName);
            if (!level1bChildNodeName.startsWith("CONTOUR")) {
//            if (!level1bChildNodeName.startsWith("SWIR") && !level1bChildNodeName.startsWith("CONTOUR")) {
                    // todo: remove this when all bands are handled properly together
                float[] lonData = new float[width * height];
                float[] latData = new float[width * height];
                for (int j = 0; j < level1bChildNode.getChildCount(); j++) {
                    final TreeNode swirNode = level1bChildNode.getChildAt(j);
                    final String swirChildNodeName = swirNode.toString();
                    System.out.println("swirChildNodeName = " + swirChildNodeName);
                    if (swirChildNodeName.equals("LN1")) {
                        final H5ScalarDS lonDS = getH5ScalarDS(swirNode);
                        for (int k = 0; k < Math.min(lonData.length, ((float[]) lonDS.getData()).length); k++) {
                            lonData[k] = ((float[]) lonDS.getData())[k];
                        }
                    } else if (swirChildNodeName.equals("LT1")) {
                        final H5ScalarDS latDS = getH5ScalarDS(swirNode);
                        for (int k = 0; k < Math.min(latData.length, ((float[]) latDS.getData()).length); k++) {
                            latData[k] = ((float[]) latDS.getData())[k];
                        }
                    }
                }
                final TiePointGrid latGrid = new TiePointGrid(level1bChildNodeName + "_" + "LT1",
                                                              width, height,
                                                              ProbaVConstants.L1C_TIEPOINT_OFFS_X,
                                                              ProbaVConstants.L1C_TIEPOINT_OFFS_Y,
                                                              ProbaVConstants.L1C_TIEPOINT_SUBS_X,
                                                              ProbaVConstants.L1C_TIEPOINT_SUBS_Y,
                                                              latData);
                final TiePointGrid lonGrid = new TiePointGrid(level1bChildNodeName + "_" + "LN1",
                                                              width, height,
                                                              ProbaVConstants.L1C_TIEPOINT_OFFS_X,
                                                              ProbaVConstants.L1C_TIEPOINT_OFFS_Y,
                                                              ProbaVConstants.L1C_TIEPOINT_SUBS_X,
                                                              ProbaVConstants.L1C_TIEPOINT_SUBS_Y,
                                                              lonData);
                product.addTiePointGrid(latGrid);
                product.addTiePointGrid(lonGrid);

                if (level1bChildNodeName.equals("RED")) {
                    // from the camera setup, SWIR2 seems to be the most reasonable geocoding with
                    // regard to the 'center' of all camera swaths
                    // note that the swaths differ quite strongly from each other!
                    // see e.g. PROBAV_L1C_20150128_114238_2_V001.HDF5, and PUM, Fig. 2!

                    // --> TODO: for 'center' product, use the swath of red/blue/nir bands once we have them!! see PUM, Fig. 2!
                    // however, for left/right products, all single-camera swaths contribute to 'full' satellite swath!

                    GeoCoding gc = new TiePointGeoCoding(latGrid, lonGrid);
                    product.setGeoCoding(gc);
                }
            }
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

        ProbaVSynthesisBitMaskOp bitMaskOp = new ProbaVSynthesisBitMaskOp();
        bitMaskOp.setParameterDefaultValues();
        bitMaskOp.setSourceProduct("sourceProduct", flagProduct);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        smFlagBand.setSourceImage(bitMaskProduct.getBand(ProbaVConstants.SM_FLAG_BAND_NAME).getSourceImage());
    }

    private static void attachL1cQualityFlagBand(Product probavProduct, Product flagProduct, String sourceQualityBandName) {
        final String targetQualityFlagBandName = sourceQualityBandName + "_FLAGS";
        FlagCoding probavL1cQualityFlagCoding = new FlagCoding(targetQualityFlagBandName);
        ProbaVUtils.addL1cQualityFlags(probavL1cQualityFlagCoding, sourceQualityBandName);
        ProbaVUtils.addL1cQualityMasks(probavProduct, sourceQualityBandName, targetQualityFlagBandName);
        probavProduct.getFlagCodingGroup().add(probavL1cQualityFlagCoding);

        final Band l1cQualityFlagBand = probavProduct.addBand(targetQualityFlagBandName, ProductData.TYPE_INT8);
        l1cQualityFlagBand.setDescription("PROBA-V L1C Quality Flags");
        l1cQualityFlagBand.setSampleCoding(probavL1cQualityFlagCoding);

        ProbaVL1cBitMaskOp bitMaskOp = new ProbaVL1cBitMaskOp();
        bitMaskOp.setParameterDefaultValues();
        bitMaskOp.setParameter("sourceQualityBandName", sourceQualityBandName);
        bitMaskOp.setParameter("targetQualityFlagBandName", targetQualityFlagBandName);
        bitMaskOp.setSourceProduct("sourceProduct", flagProduct);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        l1cQualityFlagBand.setSourceImage(bitMaskProduct.getBand(targetQualityFlagBandName).getSourceImage());
    }

    private boolean isSynthesisViewAngleGroupNode(String level3GeometryChildNodeName) {
        return level3GeometryChildNodeName.equals("SWIR") || level3GeometryChildNodeName.equals("VNIR");
    }

    private boolean isSynthesisSunAngleDataNode(String level3GeometryChildNodeName) {
        return level3GeometryChildNodeName.equals("SAA") || level3GeometryChildNodeName.equals("SZA");
    }

    private Band createTargetBand(Product product, H5ScalarDS scalarDS, String bandName, int dataType) throws Exception {
        final List<Attribute> metadata = scalarDS.getMetadata();
        final float scaleFactor = ProbaVUtils.getScaleFactor(metadata);
        final float scaleOffset = ProbaVUtils.getScaleOffset(metadata);
        final Band band = product.addBand(bandName, dataType);
        band.setScalingFactor(scaleFactor);
        band.setScalingOffset(scaleOffset);

        return band;
    }

    private H5ScalarDS getH5ScalarDS(TreeNode level3BandsChildNode) throws HDF5Exception {
        H5ScalarDS scalarDS = (H5ScalarDS) ((DefaultMutableTreeNode) level3BandsChildNode).getUserObject();
        scalarDS.open();
        scalarDS.read();
        return scalarDS;
    }

    private void addSynthesisRootMetadataElements(final Product product) {
        final MetadataElement mph = new MetadataElement(ProbaVConstants.MPH_NAME);

        // todo
//        for (final String name : chrisFile.getGlobalAttributeNames()) {
//            if (ProbaVConstants.ATTR_NAME_KEY_TO_MASK.equals(name)) {
//                continue;
//            }
//            final String globalAttribute = chrisFile.getGlobalAttribute(name);
//            mph.addAttribute(new MetadataAttribute(name, ProductData.createInstance(globalAttribute), true));
//
//            if (ProbaVConstants.ATTR_NAME_SOLAR_ZENITH_ANGLE.equals(name)) {
//                addSolarAzimuthAngleIfPossible(product, mph);
//            }
//        }
//
//        mph.addAttribute(new MetadataAttribute(ProbaVConstants.ATTR_NAME_NOISE_REDUCTION,
//                                               ProductData.createInstance("None"), true));

        final MetadataElement bandInfo = createBandInfo();

        product.getMetadataRoot().addElement(mph);
        product.getMetadataRoot().addElement(bandInfo);
    }

    private MetadataElement createBandInfo() {
        // todo
        final MetadataElement bandInfo = new MetadataElement(ProbaVConstants.BAND_INFORMATION_NAME);
//        for (int i = 0; i < chrisFile.getSpectralBandCount(); i++) {
//            final String name = MessageFormat.format("radiance_{0}", i + 1);
//            final MetadataElement element = new MetadataElement(name);
//
//            MetadataAttribute attribute = new MetadataAttribute("Cut-on Wavelength", ProductData.TYPE_FLOAT32);
//            attribute.getData().setElemFloat(chrisFile.getCutOnWavelength(i));
//            attribute.setDescription("Cut-on wavelength");
//            attribute.setUnit("nm");
//            element.addAttribute(attribute);
//
//            attribute = new MetadataAttribute("Cut-off Wavelength", ProductData.TYPE_FLOAT32);
//            attribute.getData().setElemFloat(chrisFile.getCutOffWavelength(i));
//            attribute.setDescription("Cut-off wavelength");
//            attribute.setUnit("nm");
//            element.addAttribute(attribute);
//
//            attribute = new MetadataAttribute("Central Wavelength", ProductData.TYPE_FLOAT32);
//            attribute.getData().setElemFloat(chrisFile.getWavelength(i));
//            attribute.setDescription("Central wavelength");
//            attribute.setUnit("nm");
//            element.addAttribute(attribute);
//
//            attribute = new MetadataAttribute("Bandwidth", ProductData.TYPE_FLOAT32);
//            attribute.getData().setElemFloat(chrisFile.getBandwidth(i));
//            attribute.setDescription("Cut-off minus cut-on wavelength");
//            attribute.setUnit("nm");
//            element.addAttribute(attribute);
//
//            attribute = new MetadataAttribute("Gain Setting", ProductData.TYPE_INT32);
//            attribute.getData().setElemInt(chrisFile.getGainSetting(i));
//            attribute.setDescription("CHRIS analogue electronics gain setting");
//            element.addAttribute(attribute);
//
//            attribute = new MetadataAttribute("Gain Value", ProductData.TYPE_FLOAT32);
//            attribute.getData().setElemFloat(chrisFile.getGainValue(i));
//            attribute.setDescription("Relative analogue gain");
//            element.addAttribute(attribute);
//
//            attribute = new MetadataAttribute("Low Row", ProductData.TYPE_INT32);
//            attribute.getData().setElemInt(chrisFile.getLowRow(i));
//            attribute.setDescription("CCD row number for the cut-on wavelength");
//            element.addAttribute(attribute);
//
//            attribute = new MetadataAttribute("High Row", ProductData.TYPE_INT32);
//            attribute.getData().setElemInt(chrisFile.getHighRow(i));
//            attribute.setDescription("CCD row number for the cut-off wavelength");
//            element.addAttribute(attribute);
//
//            bandInfo.addElement(element);
//        }
        return bandInfo;
    }



    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException(String.format("No source to read for band '%s'.", destBand.getName()));
    }

}
