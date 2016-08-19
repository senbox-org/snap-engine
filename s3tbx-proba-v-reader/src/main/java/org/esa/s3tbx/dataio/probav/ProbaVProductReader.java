package org.esa.s3tbx.dataio.probav;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h5.H5Datatype;
import ncsa.hdf.object.h5.H5Group;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Reader for Proba-V L2A products
 *
 * @author olafd
 */
public class ProbaVProductReader extends AbstractProductReader {

    private int productWidth;
    private int productHeight;

    private int file_id;

    private String probavProductType;  // 'LEVEL3' (Synthesis) or 'LEVEL2A'

    private boolean isLevel3TocProduct;
    private boolean isLevel3NdviProduct;

    private HashMap<Band, Hdf5DatasetVar> datasetVars;

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
        File probavFile = ProbaVProductReaderPlugIn.getFileInput(inputObject);

        Product targetProduct = null;

        if (probavFile != null && ProbaVProductReaderPlugIn.isHdf5LibAvailable()) {
            FileFormat h5FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            FileFormat h5File = null;
            try {
                file_id = H5.H5Fopen(probavFile.getAbsolutePath(),  // Name of the file to access.
                                     HDF5Constants.H5F_ACC_RDONLY,  // File access flag
                                     HDF5Constants.H5P_DEFAULT);

                h5File = h5FileFormat.createInstance(probavFile.getAbsolutePath(), FileFormat.READ);
                h5File.open();

                final TreeNode probavTypeNode = h5File.getRootNode().getChildAt(0);
                probavProductType = probavTypeNode.toString();
                isLevel3TocProduct = (probavProductType.equals("LEVEL3")) && ProbaVUtils.isLevel3Toc(probavTypeNode);
                isLevel3NdviProduct = (probavProductType.equals("LEVEL3")) && ProbaVUtils.isLevel3Ndvi(probavTypeNode);

                targetProduct = createTargetProduct(probavFile, h5File.getRootNode());
            } catch (Exception e) {
                throw new IOException("Failed to open file '" + probavFile.getPath() + "': " + e.getMessage(), e);
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

        final Hdf5DatasetVar datasetVar = datasetVars.get(targetBand);
        synchronized (datasetVar) {
            if (datasetVar.getName().equals("/" + probavProductType + "/QUALITY/" + ProbaVConstants.SM_BAND_NAME) &&
                    targetBand.getName().equals(ProbaVConstants.SM_FLAG_BAND_NAME)) {
                ProductData tmpBuffer =
                        ProbaVUtils.getDataBufferForH5Dread(datasetVar.getType(), targetWidth, targetHeight);
                ProbaVUtils.readProbaVData(file_id,
                                           targetWidth, targetHeight,
                                           targetOffsetX, targetOffsetY,
                                           datasetVar.getName(),
                                           datasetVar.getType(),
                                           tmpBuffer);
                ProbaVFlags.setSmFlagBuffer(targetBuffer, tmpBuffer, probavProductType);
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

    //////////// private methods //////////////////

    private Product createTargetProduct(File inputFile, TreeNode inputFileRootNode) throws Exception {
        Product product = null;

        if (inputFileRootNode != null) {
            final TreeNode productTypeNode = inputFileRootNode.getChildAt(0);        // 'LEVEL2A'

            // get dimensions either from GEOMETRY/SAA or for NDVI products from NDVI/NDVI
            final int productTypeNodeStartIndex = isLevel3NdviProduct ? 1 : 0;
            final int rasterNodeStartIndex = probavProductType.equals("LEVEL3") ? 0 : 1;

            productWidth = (int) ProbaVUtils.getH5ScalarDS(productTypeNode.getChildAt(productTypeNodeStartIndex).
                    getChildAt(rasterNodeStartIndex)).getDims()[1];   // take from SAA
            productHeight = (int) ProbaVUtils.getH5ScalarDS(productTypeNode.getChildAt(productTypeNodeStartIndex).
                    getChildAt(rasterNodeStartIndex)).getDims()[0];
            product = new Product(inputFile.getName(), "PROBA-V " + probavProductType, productWidth, productHeight);
            product.setPreferredTileSize(productWidth, 16);
            product.setAutoGrouping("TOA_REFL:TOC_REFL:VAA:VZA");

            datasetVars = new HashMap<>(32);

            final H5Group rootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
            final List rootMetadata = rootGroup.getMetadata();
            ProbaVUtils.addProbaVMetadataElement(rootMetadata, product, ProbaVConstants.MPH_NAME);
            product.setDescription(ProbaVUtils.getStringAttributeValue(rootMetadata, "DESCRIPTION"));
            ProbaVUtils.addStartStopTimes(product, (DefaultMutableTreeNode) inputFileRootNode);
            product.setFileLocation(inputFile);

            for (int i = 0; i < productTypeNode.getChildCount(); i++) {
                // we have: 'GEOMETRY', 'NDVI', 'QUALITY', 'RADIOMETRY', 'TIME'
                final TreeNode productTypeChildNode = productTypeNode.getChildAt(i);
                final String productTypeChildNodeName = productTypeChildNode.toString();


                switch (productTypeChildNodeName) {
                    case "GEOMETRY":
                        createGeometryBand(inputFileRootNode, product, productTypeChildNode);
                        break;

                    case "NDVI":
                        // only present in LEVEL3 Synthesis products
                        if (probavProductType.equals("LEVEL2A")) {
                            break;
                        }
                        createNdviBand(product, productTypeChildNode);
                        break;

                    case "QUALITY":
                        createQualityBand(product, productTypeChildNode);
                        break;

                    case "RADIOMETRY":
                        createRadiometryBand(product, productTypeChildNode);
                        break;

                    case "TIME":
                        // only present in LEVEL3 Synthesis products

                        // add start/end time to product:
                        ProbaVUtils.addStartStopTimes(product, (DefaultMutableTreeNode) productTypeChildNode);
                        if (isLevel3NdviProduct) {
                            // empty in NDVI products
                            break;
                        }

                        createTimeBand(product, productTypeChildNode);
                        break;

                    default:
                        break;
                }
            }
        }

        return product;
    }

    private void createTimeBand(Product product, TreeNode productTypeChildNode) throws Exception {
        final H5ScalarDS timeDS = ProbaVUtils.getH5ScalarDS(productTypeChildNode.getChildAt(0));
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
        datasetVars.put(timeBand, new Hdf5DatasetVar(timeDatasetName,
                                                     timeDatatypeClass));
    }

    private void createQualityBand(Product product, TreeNode productTypeChildNode) throws HDF5Exception {
        if (isLevel3NdviProduct) {
            ProbaVUtils.addQualityMetadata(product, (DefaultMutableTreeNode) productTypeChildNode);
            return;
        }
        final H5ScalarDS qualityDS = ProbaVUtils.getH5ScalarDS(productTypeChildNode.getChildAt(0));

        FlagCoding probavSmFlagCoding = new FlagCoding(ProbaVConstants.SM_FLAG_BAND_NAME);
        ProbaVFlags.addQualityFlags(probavSmFlagCoding, probavProductType);
        ProbaVFlags.addQualityMasks(product, probavProductType);
        product.getFlagCodingGroup().add(probavSmFlagCoding);
        final Band smFlagBand = product.addBand(ProbaVConstants.SM_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        smFlagBand.setDescription("PROBA-V SM Flags");
        smFlagBand.setSampleCoding(probavSmFlagCoding);

        final String qualityDatasetName = "/" + probavProductType + "/QUALITY/" + ProbaVConstants.SM_BAND_NAME;
        final int qualityDatatypeClass = qualityDS.getDatatype().getDatatypeClass();
        datasetVars.put(smFlagBand,
                        new Hdf5DatasetVar(qualityDatasetName,
                                           qualityDatatypeClass));

        // metadata:
        ProbaVUtils.addQualityMetadata(product, (DefaultMutableTreeNode) productTypeChildNode);
    }

    private void createGeometryBand(TreeNode inputFileRootNode, Product product, TreeNode productTypeChildNode) throws Exception {
        ProbaVUtils.setProbaVGeoCoding(product, inputFileRootNode, productTypeChildNode,
                                       productWidth, productHeight);
        if (isLevel3NdviProduct) {
            return;
        }
        // 8-bit unsigned character
        // skip 'CONTOUR' in case of LEVEL2A
        final int childNodeStartIndex = probavProductType.equals("LEVEL3") ? 0 : 1;
        for (int j = childNodeStartIndex; j < productTypeChildNode.getChildCount(); j++) {
            final TreeNode geometryChildNode = productTypeChildNode.getChildAt(j);
            final String geometryChildNodeName = geometryChildNode.toString();
            if (ProbaVUtils.isProbaVSunAngleDataNode(geometryChildNodeName)) {
                final H5ScalarDS sunAngleDS = ProbaVUtils.getH5ScalarDS(geometryChildNode);
                final Band sunAngleBand = ProbaVUtils.createTargetBand(product,
                                                                       sunAngleDS.getMetadata(),
                                                                       geometryChildNodeName,
                                                                       ProductData.TYPE_UINT8);
                ProbaVUtils.setBandUnitAndDescription(sunAngleDS.getMetadata(), sunAngleBand);
                sunAngleBand.setNoDataValue(ProbaVConstants.GEOMETRY_NO_DATA_VALUE);
                sunAngleBand.setNoDataValueUsed(true);

                final String sunAngleDatasetName = "/" + probavProductType + "/GEOMETRY/" + geometryChildNodeName;
                final int sunAngleDatatypeClass = sunAngleDS.getDatatype().getDatatypeClass();   // 0
                datasetVars.put(sunAngleBand, new Hdf5DatasetVar(sunAngleDatasetName,
                                                                 sunAngleDatatypeClass));
            } else if (ProbaVUtils.isProbaVViewAngleGroupNode(geometryChildNodeName)) {
                for (int k = 0; k < geometryChildNode.getChildCount(); k++) {
                    final TreeNode geometryViewAngleChildNode = geometryChildNode.getChildAt(k);
                    final H5ScalarDS viewAngleDS = ProbaVUtils.getH5ScalarDS(geometryViewAngleChildNode);
                    final String geometryViewAngleChildNodeName =
                            geometryViewAngleChildNode.toString();
                    final String viewAnglebandName = geometryViewAngleChildNodeName + "_" +
                            geometryChildNodeName;
                    final Band viewAngleBand = ProbaVUtils.createTargetBand(product,
                                                                            viewAngleDS.getMetadata(),
                                                                            viewAnglebandName,
                                                                            ProductData.TYPE_UINT8);
                    ProbaVUtils.setBandUnitAndDescription(viewAngleDS.getMetadata(), viewAngleBand);
                    viewAngleBand.setNoDataValue(ProbaVConstants.GEOMETRY_NO_DATA_VALUE);
                    viewAngleBand.setNoDataValueUsed(true);

                    final String viewAngleDatasetName = "/" + probavProductType + "/GEOMETRY/" +
                            geometryChildNodeName + "/" + geometryViewAngleChildNodeName;
                    final int viewAngleDatatypeClass = viewAngleDS.getDatatype().getDatatypeClass();   // 0
                    datasetVars.put(viewAngleBand, new Hdf5DatasetVar(viewAngleDatasetName,
                                                                      viewAngleDatatypeClass));
                }
            }
        }
    }

    private void createRadiometryBand(Product product, TreeNode productTypeChildNode) throws Exception {
        // 16-bit integer
        //  blue, nir, red, swir:
        for (int j = 0; j < productTypeChildNode.getChildCount(); j++) {
            // we want the sequence BLUE, RED, NIR, SWIR, rather than original BLUE, NIR, RED, SWIR...
            final int k = ProbaVConstants.RADIOMETRY_CHILD_INDEX[j];
            final TreeNode radiometryChildNode = productTypeChildNode.getChildAt(k);
            final H5ScalarDS radiometryDS = ProbaVUtils.getH5ScalarDS(radiometryChildNode.getChildAt(0));
            final String radiometryChildNodeName = radiometryChildNode.toString();
            final String radiometryBandPrePrefix = isLevel3TocProduct ? "TOC" : "TOA";
            final String radiometryBandPrefix = radiometryBandPrePrefix + "_REFL_";
            final Band radiometryBand = ProbaVUtils.createTargetBand(product,
                                                                     radiometryDS.getMetadata(),
                                                                     radiometryBandPrefix + radiometryChildNodeName,
                                                                     ProductData.TYPE_INT16);
            ProbaVUtils.setBandUnitAndDescription(radiometryDS.getMetadata(), radiometryBand);
            ProbaVUtils.setSpectralBandProperties(radiometryBand);
            radiometryBand.setNoDataValue(ProbaVConstants.RADIOMETRY_NO_DATA_VALUE);
            radiometryBand.setNoDataValueUsed(true);

            final String radiometryDatasetName = "/" + probavProductType + "/RADIOMETRY/" +
                    radiometryChildNodeName + "/" + radiometryBandPrePrefix;
            final int radiometryDatatypeClass = radiometryDS.getDatatype().getDatatypeClass();
            datasetVars.put(radiometryBand,
                            new Hdf5DatasetVar(radiometryDatasetName,
                                               radiometryDatatypeClass));
        }
    }

    private void createNdviBand(Product product, TreeNode level3ChildNode) throws Exception {
        // 8-bit unsigned character
        final H5ScalarDS ndviDS = (H5ScalarDS) ((DefaultMutableTreeNode) level3ChildNode.getChildAt(0)).getUserObject();
        final Band ndviBand = ProbaVUtils.createTargetBand(product, ndviDS.getMetadata(), "NDVI", ProductData.TYPE_UINT8);

        ndviBand.setDescription("Normalized Difference Vegetation Index");
        ndviBand.setUnit("dl");
        ndviBand.setNoDataValue(ProbaVConstants.NDVI_NO_DATA_VALUE);
        ndviBand.setNoDataValueUsed(true);

        final String ndviDatasetName = "/LEVEL3/NDVI/NDVI";
        final int ndviDatatypeClass = ndviDS.getDatatype().getDatatypeClass();

        datasetVars.put(ndviBand, new Hdf5DatasetVar(ndviDatasetName,
                                                     ndviDatatypeClass));
    }

    private static class Hdf5DatasetVar {

        final String name;
        final int type;

        public Hdf5DatasetVar(String name, int type) {
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
