package org.esa.beam.dataio;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h5.H5Group;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.ImageUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * Reader for Proba-V L1c products
 * todo: to be completed when requirements and specifications are available
 *
 * @author olafd
 */
public class ProbaVL1cProductReader extends AbstractProductReader {

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
    protected ProbaVL1cProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object inputObject = getInput();
        probavFile = ProbaVL1cProductReaderPlugIn.getFileInput(inputObject);
        final String fileName = probavFile.getName();

        Product targetProduct = null;

        if (ProbaVL1cProductReaderPlugIn.isHdf5LibAvailable()) {
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
                if (ProbaVL1cProductReaderPlugIn.isProbaVL1cProduct(fileName)) {
                    targetProduct = createTargetProductFromL1C(probavFile, rootNode);
                }
            } catch (Exception e) {
                throw new IOException("Failed to open file " + probavFile.getPath());
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

    private void addStartStopTimes(Product product, DefaultMutableTreeNode level3ChildNode) throws HDF5Exception, ParseException {
        final H5Group timeGroup = (H5Group) level3ChildNode.getUserObject();
        final List timeMetadata = timeGroup.getMetadata();
        product.setStartTime(ProductData.UTC.parse(ProbaVUtils.getStartEndTimeFromAttributes(timeMetadata)[0],
                ProbaVConstants.PROBAV_DATE_FORMAT_PATTERN));
        product.setEndTime(ProductData.UTC.parse(ProbaVUtils.getStartEndTimeFromAttributes(timeMetadata)[1],
                ProbaVConstants.PROBAV_DATE_FORMAT_PATTERN));
    }

    private void addQualityMetadata(Product product, DefaultMutableTreeNode level3ChildNode) throws HDF5Exception {
        final H5Group qualityGroup = (H5Group) level3ChildNode.getUserObject();
        final List qualityMetadata = qualityGroup.getMetadata();
        addSynthesisMetadataElement(qualityMetadata, product, ProbaVConstants.QUALITY_NAME);
    }

    private void setNdviBand(Product product, TreeNode level3ChildNode) throws Exception {
//        final H5ScalarDS ndviDS = getH5ScalarDS(level3ChildNode.getChildAt(0));
//        final Band ndviBand = createTargetBand(product, ndviDS, "NDVI", ProductData.TYPE_FLOAT32);
//        ndviBand.setDescription("Normalized Difference Vegetation Index");
//        ndviBand.setUnit("dl");
//        ndviBand.setNoDataValue(ProbaVConstants.NDVI_NO_DATA_VALUE);
//        ndviBand.setNoDataValueUsed(true);
//        final byte[] ndviData = (byte[]) ndviDS.getData();
//        // the scaling does not work properly with the original data, convert to scaled floats manually
//        final float[] ndviFloatData = ProbaVUtils.getNdviAsFloat(ndviBand, ndviData);
//        final ProbaVRasterImage ndviImage = new ProbaVRasterImage(ndviBand, ndviFloatData);
//        ndviBand.setSourceImage(ndviImage);

        final H5ScalarDS ndviDS = (H5ScalarDS) ((DefaultMutableTreeNode) level3ChildNode.getChildAt(0)).getUserObject();
        final Band ndviBand = createTargetBand(product, ndviDS, "NDVI", ProductData.TYPE_UINT8);

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

    private void setBandProperties(H5ScalarDS ds, Band band) throws HDF5Exception {
        band.setDescription(ProbaVUtils.getDescriptionFromAttributes(ds.getMetadata()));
        band.setUnit(ProbaVUtils.getUnitFromAttributes(ds.getMetadata()));
        band.setNoDataValue(ProbaVUtils.getNoDataValueFromAttributes(ds.getMetadata()));
        band.setNoDataValueUsed(true);
        setSpectralProperties(band);
    }

    private void setSpectralProperties(Band band) {
        if (band.getName().endsWith("REFL_BLUE")) {
            band.setSpectralBandIndex(0);
            band.setSpectralWavelength(462.0f);
            band.setSpectralBandwidth(48.0f);
        } else if (band.getName().endsWith("REFL_RED")) {
            band.setSpectralBandIndex(1);
            band.setSpectralWavelength(655.5f);
            band.setSpectralBandwidth(81.0f);
        } else if (band.getName().endsWith("REFL_NIR")) {
            band.setSpectralBandIndex(2);
            band.setSpectralWavelength(843.0f);
            band.setSpectralBandwidth(142.0f);
        } else if (band.getName().endsWith("REFL_SWIR")) {
            band.setSpectralBandIndex(3);
            band.setSpectralWavelength(1599.0f);
            band.setSpectralBandwidth(70.0f);
        }
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

    private static void attachL1cQualityFlagBand(Product probavProduct, Product flagProduct, String sourceQualityBandName) {
        final String targetQualityFlagBandName = sourceQualityBandName + "_FLAGS";
        FlagCoding probavL1cQualityFlagCoding = new FlagCoding(targetQualityFlagBandName);
        ProbaVUtils.addL1cQualityFlags(probavL1cQualityFlagCoding, sourceQualityBandName);
        ProbaVUtils.addL1cQualityMasks(probavProduct, sourceQualityBandName, targetQualityFlagBandName);
        probavProduct.getFlagCodingGroup().add(probavL1cQualityFlagCoding);

        final Band l1cQualityFlagBand = probavProduct.addBand(targetQualityFlagBandName, ProductData.TYPE_UINT8);
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

    private Band createTargetBand(Product product, H5ScalarDS scalarDS, String bandName, int dataType) throws Exception {
        final List<Attribute> metadata = scalarDS.getMetadata();
        final float scaleFactor = ProbaVUtils.getScaleFactorFromAttributes(metadata);
        final float scaleOffset = ProbaVUtils.getScaleOffsetFromAttributes(metadata);
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

    private void addSynthesisMetadataElement(List<Attribute> rootMetadata,
                                             final Product product,
                                             String metadataElementName) {
        final MetadataElement metadataElement = new MetadataElement(metadataElementName);

        for (Attribute attribute : rootMetadata) {
            metadataElement.addAttribute(new MetadataAttribute(attribute.getName(),
                    ProductData.createInstance(ProbaVUtils.getAttributeValue(attribute)), true));
        }
        product.getMetadataRoot().addElement(metadataElement);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException(String.format("No source to read for band '%s'.", destBand.getName()));
    }

}
