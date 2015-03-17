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

    private int rasterDim;

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
                } else if (ProbaVProductReaderPlugIn.isProbaS1ToaProduct(fileName)) {
                    targetProduct = createTargetProductFromS1Toa(inputFile, rootNode);
                } else if (ProbaVProductReaderPlugIn.isProbaS1TocProduct(fileName)) {
                    targetProduct = createTargetProductFromS1Toc(inputFile, rootNode);
                } else if (ProbaVProductReaderPlugIn.isProbaS10TocProduct(fileName)) {
                    targetProduct = createTargetProductFromS10Toc(inputFile, rootNode);
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

    private Product createTargetProductFromS10Toc(File inputFile, TreeNode rootNode) {
        // todo
        return null;
    }

    private Product createTargetProductFromS1Toc(File inputFile, TreeNode rootNode) {
        // todo
        return null;
    }

    private Product createTargetProductFromS1Toa(File inputFile, TreeNode inputFileRootNode) throws Exception {
        Product product = null;

        if (inputFileRootNode != null) {
            rasterDim = ProbaVUtils.getSynthesisProductRasterDimension(inputFile.getName());
            product = new Product(inputFile.getName(), "PROBA-V", rasterDim, rasterDim);

            product.setAutoGrouping("TOA_REFL:VAA:VZA");

            // todo (see e.g. Chris-Proba reader):
            // - SM as flag band  (see description attribute in SM variable, do bit 0-2 as in Mod35BitMaskOp (GA))
            // - start/stop times
            // - product metadata
            // - band properties and metadata
            // - exception handling
            // - closing of files/products
            // - extract constants
            // - no data values

            // - !! TOC product can't be read
            // - !! 1km products can't be read


            final TreeNode level3Node = inputFileRootNode.getChildAt(0);        // 'LEVEL3'
            for (int i = 0; i < level3Node.getChildCount(); i++) {
                // we have: 'GEOMETRY', 'NDVI', 'QUALITY', 'RADIOMETRY', 'TIME'
                final TreeNode level3ChildNode = level3Node.getChildAt(i);
                final String level3ChildNodeName = level3ChildNode.toString();

                switch (level3ChildNodeName) {
                    case "GEOMETRY":
                        setGeoCoding(product, inputFileRootNode, level3ChildNode);
                        // 8-bit unsigned character
                        for (int j = 0; j < level3ChildNode.getChildCount(); j++) {
                            final TreeNode level3GeometryChildNode = level3ChildNode.getChildAt(j);
                            final String level3GeometryChildNodeName = level3GeometryChildNode.toString();
                            System.out.println("level3GeometryChildNode = " + level3GeometryChildNodeName);
                            if (isSunAngleDataNode(level3GeometryChildNodeName)) {
                                final H5ScalarDS sunAngleDS = getH5ScalarDS(level3GeometryChildNode);
                                final Band sunAngleBand = createTargetBand(product, sunAngleDS, level3GeometryChildNodeName,
                                        ProductData.TYPE_UINT8);
                                final byte[] sunAngleData = (byte[]) sunAngleDS.getData();
                                final ProbaVRasterImage ndviImage = new ProbaVRasterImage(sunAngleBand, sunAngleData);
                                sunAngleBand.setSourceImage(ndviImage);
                            } else if (isViewAngleGroupNode(level3GeometryChildNodeName)) {
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
                        final float[] ndviFloatData = getNdviAsFloat(ndviBand, ndviData);
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
                        attachSynthesisSmFlagBand(product);
                        break;
                    case "RADIOMETRY":
                        // 16-bit integer
                        //  blue, nir, red, swir:
                        for (int j = 0; j < level3ChildNode.getChildCount(); j++) {
                            final TreeNode level3RadiometryChildNode = level3ChildNode.getChildAt(j);
                            final H5ScalarDS radiometryDS = getH5ScalarDS(level3RadiometryChildNode.getChildAt(0));
                            final String level3RadiometryChildNodeName = level3RadiometryChildNode.toString();
                            final Band radiometryBand = createTargetBand(product,
                                    radiometryDS,
                                    "TOA_REFL_" + level3RadiometryChildNodeName,
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

    private void setGeoCoding(Product product, TreeNode inputFileRootNode, TreeNode level3ChildNode)
            throws HDF5Exception {

        final H5Group h5GeometryGroup = (H5Group) ((DefaultMutableTreeNode) level3ChildNode).getUserObject();
        final List geometryMetadata = h5GeometryGroup.getMetadata();
        final double easting = ProbaVUtils.getGeometryCoordinateValue(geometryMetadata, "TOP_LEFT_LONGITUDE");
        final double northing = ProbaVUtils.getGeometryCoordinateValue(geometryMetadata, "TOP_LEFT_LATITUDE");
        final int imageWidth = rasterDim;
        final int imageHeight = rasterDim;
        // pixel size: 10deg/rasterDim, it is also in the 6th and 7th value of MAPPING attribute in the raster nodes
        final double topLeftLon = easting;
        final double topRightLon = ProbaVUtils.getGeometryCoordinateValue(geometryMetadata, "TOP_RIGHT_LONGITUDE");
        final double pixelSizeX = Math.abs(topRightLon - topLeftLon) / rasterDim;
        final double topLeftLat = northing;
        final double bottomLeftLat = ProbaVUtils.getGeometryCoordinateValue(geometryMetadata, "BOTTOM_LEFT_LATITUDE");
        final double pixelSizeY = (topLeftLat - bottomLeftLat) / rasterDim;

        final H5Group h5RootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
        final List rootMetadata = h5RootGroup.getMetadata();
        final String crsString = ProbaVUtils.getGeometryCrsString(rootMetadata);
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
            final CrsGeoCoding geoCoding = new CrsGeoCoding(crs, imageWidth, imageHeight, easting, northing, pixelSizeX, pixelSizeY);
            product.setGeoCoding(geoCoding);
        } catch (Exception e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot attach geocoding: " + e.getMessage());
        }
    }

    private static void attachSynthesisSmFlagBand(Product probavProduct) {
        FlagCoding probavSmFlagCoding = new FlagCoding(ProbaVConstants.SM_FLAG_BAND_NAME);
        ProbaVUtils.addSmFlags(probavSmFlagCoding);
        ProbaVUtils.addSmMasks(probavProduct);
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


    private boolean isViewAngleGroupNode(String level3GeometryChildNodeName) {
        return level3GeometryChildNodeName.equals("SWIR") || level3GeometryChildNodeName.equals("VNIR");
    }

    private boolean isSunAngleDataNode(String level3GeometryChildNodeName) {
        return level3GeometryChildNodeName.equals("SAA") || level3GeometryChildNodeName.equals("SZA");
    }

    private float[] getNdviAsFloat(Band ndviBand, byte[] ndviData) {
        float[] ndviFloatData = new float[ndviData.length];

        for (int i = 0; i < ndviFloatData.length; i++) {
            ndviFloatData[i] = (float) ((ndviData[i] - ndviBand.getScalingOffset()) * ndviBand.getScalingFactor());
        }
        ndviBand.setScalingFactor(1.0);
        ndviBand.setScalingOffset(0.0);

        return ndviFloatData;
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


    private Product createTargetProductFromL1C(File inputFile, TreeNode rootNode) throws Exception {
        Product product = null;

        if (rootNode != null) {
            final TreeNode level1cChildNode = rootNode.getChildAt(2);               // 'LEVEL1C'
            for (int i = 0; i < level1cChildNode.getChildCount(); i++) {
                final TreeNode level1cBandsChildNode = level1cChildNode.getChildAt(i);
                final String reflectanceBandName = level1cBandsChildNode.toString();
                if (reflectanceBandName.startsWith("SWIR")) {
                    // todo: in L1C product we have two different sizes:
                    // blue, nir, red: 18984 x 5200
                    // swir 1-3:   9492*1024
                } else {
                    //  blue, nir, red:
                    final TreeNode l1cQualityChild = level1cBandsChildNode.getChildAt(0);   // todo
                    final TreeNode l1cToaChild = level1cBandsChildNode.getChildAt(1);
                    System.out.println("Child: " + l1cToaChild.toString());
                    final H5ScalarDS scalarDS = (H5ScalarDS) ((DefaultMutableTreeNode) l1cToaChild).getUserObject();
                    scalarDS.open();
                    scalarDS.read();
                    final int yDim = (int) scalarDS.getDims()[0];
                    final int xDim = (int) scalarDS.getDims()[1];
                    final short[] data = (short[]) scalarDS.getData();
                    final List<Attribute> metadata = scalarDS.getMetadata();
                    final float scaleFactor = ProbaVUtils.getScaleFactor(metadata);
                    final float scaleOffset = ProbaVUtils.getScaleOffset(metadata);
                    if (product == null) {
                        product = new Product(inputFile.getName(), "PROBA-V", xDim, yDim);
                    }
//                    final Band toaBand = product.addBand(reflectanceBandName + "_TOA", ProductData.TYPE_FLOAT32);
                    final Band toaBand = product.addBand(reflectanceBandName + "_TOA", ProductData.TYPE_INT16);
                    toaBand.setScalingFactor(scaleFactor);
                    toaBand.setScalingOffset(scaleOffset);
                    final ProbaVRasterImage image = new ProbaVRasterImage(toaBand, data);
                    toaBand.setSourceImage(image);
                }
            }
        }

        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

    }

}
