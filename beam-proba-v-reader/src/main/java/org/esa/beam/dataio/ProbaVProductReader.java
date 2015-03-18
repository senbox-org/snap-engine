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
        final File inputFile = ProbaVProductReaderPlugIn.getFileInput(inputObject);
        final String fileName = inputFile.getName();

        Product targetProduct = null;

        if (ProbaVProductReaderPlugIn.isHdf5LibAvailable()) {
            FileFormat h5FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            FileFormat h5File = null;
            try {
                h5File = h5FileFormat.createInstance(inputFile.getAbsolutePath(), FileFormat.READ);
                final int h5FileId = h5File.open();
                System.out.println("h5FileId = " + h5FileId);

                final TreeNode rootNode = h5File.getRootNode();

                // check of which of the supported product types the input is:
                if (ProbaVProductReaderPlugIn.isProbaL1CProduct(fileName)) {
                    targetProduct = createTargetProductFromL1C(inputFile, rootNode);
                } else if (ProbaVProductReaderPlugIn.isProbaSynthesisProduct(fileName)) {
                    targetProduct = createTargetProductFromSynthesis(inputFile, rootNode);
                } else if (ProbaVProductReaderPlugIn.isProbaS10TocNdviProduct(fileName)) {
                    targetProduct = createTargetProductFromS10TocNdvi(inputFile, rootNode);
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
            // - start/stop times
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
                        final Band smBand =
                                createTargetBand(product, qualityDS, ProbaVConstants.SM_BAND_NAME, ProductData.TYPE_UINT8);
                        final byte[] qualityData = (byte[]) qualityDS.getData();
                        final ProbaVRasterImage image = new ProbaVRasterImage(smBand, qualityData);
                        smBand.setSourceImage(image);
                        attachSynthesisQualityFlagBand(product);
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
                        break;
                    default:
                        break;
                }
            }
        }

        return product;
    }

    private Product createTargetProductFromL1C(File inputFile, TreeNode inputFileRootNode) throws Exception {
        Product product = null;

        if (inputFileRootNode != null) {
            final TreeNode level1aNode = inputFileRootNode.getChildAt(0);               // 'LEVEL1A'
            // todo: do we need any metadata from this group?

            final TreeNode level1bNode = inputFileRootNode.getChildAt(2);               // 'LEVEL1B'
            // todo: do we need any metadata from this group?
            // todo: extract lat/lon info from LN1/LT1 and set up tie point geocoding
            // careful: we have different lat/lon tie point grids again for blue/nir/red vs. SWIR
            // also, downscaling factor with regard to the L1C raster data is not integer!

            final TreeNode level1cNode = inputFileRootNode.getChildAt(2);               // 'LEVEL1C'

            final int width = (int) getH5ScalarDS(level1cNode.getChildAt(0).getChildAt(0)).getDims()[0];
            final int height = (int) getH5ScalarDS(level1cNode.getChildAt(0).getChildAt(0)).getDims()[1];
            product = new Product(inputFile.getName(), "PROBA-V L1C", width, height);
            product.setAutoGrouping("TOA_REFL:BLUE:NIR:RED:SWIR1:SWIR2:SWIR3");

            for (int i = 0; i < level1cNode.getChildCount(); i++) {
                final TreeNode level1cChildNode = level1cNode.getChildAt(i);
                final String level1cChildNodeName = level1cChildNode.toString();
                if (level1cChildNodeName.startsWith("SWIR")) {
                    // on the smaller grid, we have: 'BLUE', 'NIR', 'RED'
                    for (int j = 0; j < level1cChildNode.getChildCount(); j++) {
                        // we have 'Q' and 'TOA'
                        final TreeNode level1cRadiometryChildNode = level1cChildNode.getChildAt(j);
                        final String level1cRadiometryChildNodeName = level1cRadiometryChildNode.toString();
                        System.out.println("level1cRadiometryChildNode = " + level1cRadiometryChildNodeName);
                        final H5ScalarDS radiometryDS = getH5ScalarDS(level1cRadiometryChildNode);
                        final String radiometryBandName = level1cChildNodeName + "_" + level1cRadiometryChildNodeName;
                        if (level1cRadiometryChildNodeName.equals("Q")) {
                            // 8-bit unsigned character
                            final Band qBand =
                                    createTargetBand(product, radiometryDS, radiometryBandName, ProductData.TYPE_UINT8);
                            final byte[] qualityData = (byte[]) radiometryDS.getData();
                            final ProbaVRasterImage image = new ProbaVRasterImage(qBand, qualityData);
                            qBand.setSourceImage(image);
                            attachL1cQualityFlagBand(product);
                        } else if (level1cRadiometryChildNodeName.equals("TOA")) {
                            // 16-bit integer
                            final Band radiometryBand = createTargetBand(product,
                                                                         radiometryDS,
                                                                         radiometryBandName + "_REFL",
                                                                         ProductData.TYPE_INT16);
                            final short[] radiometryData = (short[]) radiometryDS.getData();
                            final ProbaVRasterImage radiometryImage = new ProbaVRasterImage(radiometryBand, radiometryData);
                            radiometryBand.setSourceImage(radiometryImage);
                        }
                    }
                } else {
                    // todo: in L1C product we have two different sizes:
                    // blue, nir, red: e.g. 18984 x 5200 (y=5200 is fix)
                    // swir 1-3:   9492*1024 (y=1024 is fix)
                    // --> BEAM: downscale the blue, nir, red bands, as e.g. in Modis35ProductReader (GA)
                    // --> SNAP: use new functionality for different raster sizes
                }
            }
        }

        return product;
    }


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

    private void setL1cGeoCoding(Product product, TreeNode inputFileRootNode,
                                 TreeNode level2ChildNode, TreeNode level3ChildNode) throws HDF5Exception {
        // todo
    }

    private static void attachSynthesisQualityFlagBand(Product probavProduct) {
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
        bitMaskOp.setSourceProduct("sourceProduct", probavProduct);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        smFlagBand.setSourceImage(bitMaskProduct.getBand(ProbaVConstants.SM_FLAG_BAND_NAME).getSourceImage());
    }

    private static void attachL1cQualityFlagBand(Product probavProduct) {
        FlagCoding probavL1cQualityFlagCoding = new FlagCoding(ProbaVConstants.Q_FLAG_BAND_NAME);
        ProbaVUtils.addL1cQualityFlags(probavL1cQualityFlagCoding);
        ProbaVUtils.addL1cQualityMasks(probavProduct);
        probavProduct.getFlagCodingGroup().add(probavL1cQualityFlagCoding);
        final Band l1cQualityFlagBand = probavProduct.addBand(ProbaVConstants.Q_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        l1cQualityFlagBand.setDescription("PROBA-V L1C Quality Flags");
        l1cQualityFlagBand.setSampleCoding(probavL1cQualityFlagCoding);

        ProbaVL1cBitMaskOp bitMaskOp = new ProbaVL1cBitMaskOp();
        bitMaskOp.setParameterDefaultValues();
        bitMaskOp.setSourceProduct("sourceProduct", probavProduct);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        l1cQualityFlagBand.setSourceImage(bitMaskProduct.getBand(ProbaVConstants.Q_FLAG_BAND_NAME).getSourceImage());
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

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException(String.format("No source to read for band '%s'.", destBand.getName()));
    }

}
