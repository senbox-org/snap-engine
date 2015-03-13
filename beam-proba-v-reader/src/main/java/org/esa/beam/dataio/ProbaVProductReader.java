package org.esa.beam.dataio;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 11.03.2015
 * Time: 16:25
 *
 * @author olafd
 */
public class ProbaVProductReader extends AbstractProductReader {

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
            final int rasterDim = ProbaVUtils.getSynthesisProductRasterDimension(inputFile.getName());
            product = new Product(inputFile.getName(), "PROBA-V", rasterDim, rasterDim);

            final TreeNode level3Node = inputFileRootNode.getChildAt(0);        // 'LEVEL3'
            for (int i = 0; i < level3Node.getChildCount(); i++) {
                // we have: 'GEOMETRY', 'NDVI', 'QUALITY', 'RADIOMETRY', 'TIME'
                final TreeNode level3ChildNode = level3Node.getChildAt(i);
                final String level3ChildNodeName = level3ChildNode.toString();

                switch (level3ChildNodeName) {
                    case "GEOMETRY":
                        // 8-bit unsigned character
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
                        // todo: create this as flag band?!
                        final H5ScalarDS qualityDS = getH5ScalarDS(level3ChildNode.getChildAt(0));
                        final Band qualityBand = createTargetBand(product, qualityDS, "SM", ProductData.TYPE_UINT8);
                        final byte[] qualityData = (byte[]) qualityDS.getData();
                        final ProbaVRasterImage image = new ProbaVRasterImage(qualityBand, qualityData);
                        qualityBand.setSourceImage(image);
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
                                                                         level3RadiometryChildNodeName + "_TOA",
                                                                         ProductData.TYPE_INT16);
                            final short[] radiometryData = (short[]) radiometryDS.getData();
                            final ProbaVRasterImage radiometryImage = new ProbaVRasterImage(radiometryBand, radiometryData);
                            radiometryBand.setSourceImage(radiometryImage);
                        }
                        break;
                    case "TIME":
                        // 16-bit unsigned integer
                        break;
                    default:
                        break;
                }
            }
        }

        return product;
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
