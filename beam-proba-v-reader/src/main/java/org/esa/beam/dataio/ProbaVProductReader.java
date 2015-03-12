package org.esa.beam.dataio;

import com.bc.ceres.core.ProgressMonitor;
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

    static final String PROBAV_L1C_FILENAME_REGEXP =
            "PROBAV_L1C_[0-9]{8}_[0-9]{6}_[0-2]{1}_V003.(?i)(hdf5)";
    static final String PROBAV_S1_TOA_FILENAME_REGEXP =
            "PROBAV_S1_TOA_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_V[0-9]{3}.(?i)(hdf5)";
    static final String PROBAV_S1_TOC_FILENAME_REGEXP =
            "PROBAV_S1_TOC_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_V[0-9]{3}.(?i)(hdf5)";
    static final String PROBAV_S10_TOC_FILENAME_REGEXP =
            "PROBAV_S10_TOC_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_V[0-9]{3}.(?i)(hdf5)";
    static final String PROBAV_S10_TOC_NDVI_FILENAME_REGEXP =
            "PROBAV_S1_TOA_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_NDVI_V[0-9]{3}.(?i)(hdf5)";   // todo: shall this be supported?

    private static boolean hdf5LibAvailable = false;

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

        // check of which of the supported product types the input is:
        if (ProbaVProductReaderPlugIn.isProbaL1CProduct(fileName)) {
            return createTargetProductFromL1C(inputFile);
        } else if (ProbaVProductReaderPlugIn.isProbaS1ToaProduct(fileName)) {
            return createTargetProductFromS1Toa(inputFile);
        } else if (ProbaVProductReaderPlugIn.isProbaS1TocProduct(fileName)) {
            return createTargetProductFromS1Toc(inputFile);
        } else if (ProbaVProductReaderPlugIn.isProbaS10TocProduct(fileName)) {
            return createTargetProductFromS10Toc(inputFile);
        } else if (ProbaVProductReaderPlugIn.isProbaS10TocNdviProduct(fileName)) {
            return createTargetProductFromS10TocNdvi(inputFile);
        }

        return null;
    }

    private Product createTargetProductFromS10TocNdvi(File inputFile) {
        // todo: do we need this?
        return null;
    }

    private Product createTargetProductFromS10Toc(File inputFile) {
        // todo
        return null;
    }

    private Product createTargetProductFromS1Toc(File inputFile) {
        // todo
        return null;
    }

    private Product createTargetProductFromS1Toa(File inputFile) {
        // todo
        return null;
    }

    private Product createTargetProductFromL1C(File inputFile) {
        Product product = null;
        if (ProbaVProductReaderPlugIn.isHdf5LibAvailable()) {
            FileFormat h5FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            FileFormat h5File = null;
            try {
                h5File = h5FileFormat.createInstance(inputFile.getAbsolutePath(), FileFormat.READ);
                final int h5FileId = h5File.open();
                System.out.println("h5FileId = " + h5FileId);

                final TreeNode rootNode = h5File.getRootNode();
                if (rootNode != null) {
                    final TreeNode level1cChildNode = rootNode.getChildAt(2);
                    for (int i = 0; i < level1cChildNode.getChildCount(); i++) {
                        final TreeNode level1cBandsChild = level1cChildNode.getChildAt(i);
                        final String reflectanceBandName = level1cBandsChild.toString();
                        if (reflectanceBandName.startsWith("SWIR")) {
                            // todo: in L1C product we have two different sizes:
                            // blue, nir, red: 18984 x 5200
                            // swir 1-3:   9492*1024
                        } else {
                            //  blue, nir, red:
                            final TreeNode l1cQualityChild = level1cBandsChild.getChildAt(0);   // todo
                            final TreeNode l1cToaChild = level1cBandsChild.getChildAt(1);
                            System.out.println("Child: " + l1cToaChild.toString());
                            final H5ScalarDS scalarDS = (H5ScalarDS) ((DefaultMutableTreeNode) l1cToaChild).getUserObject();
                            scalarDS.open();
                            scalarDS.read();
                            final int yDim = (int) scalarDS.getDims()[0];
                            final int xDim = (int) scalarDS.getDims()[1];
                            final short[] data = (short[]) scalarDS.getData();
                            final List<Attribute> metadata = scalarDS.getMetadata();
                            final float scaleFactor = ProbaVUtils.getScaleFactor(metadata);
                            if (product == null) {
                                product = new Product(inputFile.getName(), "PROBA-V", xDim, yDim);
                            }
                            final Band toaBand = product.addBand(reflectanceBandName + "_TOA", ProductData.TYPE_FLOAT32);
                            final ProbaVToaImage image = new ProbaVToaImage(toaBand, data, scaleFactor);
                            toaBand.setSourceImage(image);
                        }
                    }
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
        } else {
            // todo
        }
        return product;

    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

    }

}
