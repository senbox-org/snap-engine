package org.esa.beam.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h5.H5Group;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.logging.BeamLogManager;
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
 * Reader for Proba-V Synthesis products
 *
 * @author olafd
 */
public class ProbaVNewSynthesisProductReader extends AbstractProductReader {

    private int productWidth;
    private int productHeight;

    private File probavFile;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected ProbaVNewSynthesisProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object inputObject = getInput();
        probavFile = ProbaVNewSynthesisProductReaderPlugIn.getFileInput(inputObject);
        final String fileName = probavFile.getName();

        Product targetProduct = null;

        if (ProbaVNewSynthesisProductReaderPlugIn.isHdf5LibAvailable()) {
            FileFormat h5FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            FileFormat h5File = null;
            try {
                final int file_id = H5.H5Fopen(probavFile.getAbsolutePath(),  // Name of the file to access.
                                               HDF5Constants.H5F_ACC_RDONLY,  // File access flag
                                               HDF5Constants.H5P_DEFAULT);    // access_id, se H5P_DEFAULT for default access properties.

                h5File = h5FileFormat.createInstance(probavFile.getAbsolutePath(), FileFormat.READ);
                h5File.open();

                final TreeNode rootNode = h5File.getRootNode();

                // check of which of the supported product types the input is:
                if (ProbaVNewSynthesisProductReaderPlugIn.isProbaSynthesisToaProduct(fileName) ||
                        ProbaVNewSynthesisProductReaderPlugIn.isProbaSynthesisTocProduct(fileName)) {
//                    targetProduct = createTargetProductFromSynthesis(probavFile, rootNode);
                } else if (ProbaVNewSynthesisProductReaderPlugIn.isProbaSynthesisNdviProduct(fileName)) {
                    targetProduct = createTargetProductFromSynthesisNdvi(probavFile, rootNode, file_id);
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
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException(String.format("No source to read for band '%s'.", destBand.getName()));
    }

    //////////// private methods //////////////////

    private Product createTargetProductFromSynthesisNdvi(File inputFile, TreeNode inputFileRootNode, int file_id) throws Exception {

        Product product = null;
        if (inputFileRootNode != null) {
            final TreeNode level3Node = inputFileRootNode.getChildAt(0);        // 'LEVEL3'
            final H5ScalarDS ds =
                    (H5ScalarDS) ((DefaultMutableTreeNode) level3Node.getChildAt(1).getChildAt(0)).getUserObject();
            ds.open();
            ds.read();
            productWidth = (int) ds.getDims()[0];
            productHeight = (int) ds.getDims()[1];
            product = new Product(inputFile.getName(), "PROBA-V SYNTHESIS NDVI", productWidth, productHeight);
            final int tileSize = productWidth / 10;    // todo discuss
            product.setPreferredTileSize(tileSize, tileSize);
            product.setNumResolutionsMax(6);  // todo discuss

            final H5Group rootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
            final List rootMetadata = rootGroup.getMetadata();
            addSynthesisMetadataElement(rootMetadata, product, ProbaVConstants.MPH_NAME);
            product.setDescription(ProbaVUtils.getDescriptionFromAttributes(rootMetadata));
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
                        setNdviBand(product, level3ChildNode, file_id);
                        break;

                    case "QUALITY":
                        addQualityMetadata(product, (DefaultMutableTreeNode) level3ChildNode);
                        break;

                    case "TIME":
                        // add start/end time to product:
                        addStartStopTimes(product, (DefaultMutableTreeNode) level3ChildNode);
                        break;

                    default:
                        break;
                }
            }
        }
        return product;
    }

    private void setNdviBand(Product product, TreeNode level3ChildNode, int file_id) throws Exception {
        final H5ScalarDS ndviDS = (H5ScalarDS) ((DefaultMutableTreeNode) level3ChildNode.getChildAt(0)).getUserObject();
        final Band ndviBand = createTargetBand(product, ndviDS, "NDVI", ProductData.TYPE_FLOAT32);

        ndviBand.setDescription("Normalized Difference Vegetation Index");
        ndviBand.setUnit("dl");
        ndviBand.setNoDataValue(ProbaVConstants.NDVI_NO_DATA_VALUE);
        ndviBand.setNoDataValueUsed(true);

        // for all H5.* stuff see https://www.hdfgroup.org/products/java/hdf-java-html/javadocs/ncsa/hdf/hdf5lib/H5.html

//        final long[] dims = {productWidth, productHeight};
//        final int dataspace_id = H5.H5Screate_simple(dims.length, // Number of dimensions of dataspace.
//                                                     dims,        // An array of the size of each dimension.
//                                                     null);       // An array of the maximum size of each dimension. // todo: why null?

//        final int dataset_id = H5.H5Dcreate(file_id,                       // Location identifier
//                                            "NDVI",                        // Dataset name
//                                            HDF5Constants.H5T_STD_I32BE,   // Datatype identifier
//                                            dataspace_id,                  // Dataspace identifier
//                                            HDF5Constants.H5P_DEFAULT,     // Identifier of link creation property list.
//                                            HDF5Constants.H5P_DEFAULT,     // Identifier of dataset creation property list.
//                                            HDF5Constants.H5P_DEFAULT);    // Identifier of dataset access property list

        final int dataset_id = H5.H5Dopen(file_id,                       // Location identifier
                                            "/LEVEL3/NDVI/NDVI",           // Dataset name
                                            HDF5Constants.H5P_DEFAULT);    // Identifier of dataset access property list
        final int dataspace_id = H5.H5Dget_space(dataset_id);

        MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(ndviBand);
        MultiLevelSource ndviMultiLevelImage = new AbstractMultiLevelSource(multiLevelModel) {

            @Override
            protected RenderedImage createImage(int level) {
                return new ProbaVNewRasterImage(ndviBand, ResolutionLevel.create(getModel(), level), dataset_id, dataspace_id);
            }
        };

        ndviBand.setSourceImage(new DefaultMultiLevelImage(ndviMultiLevelImage));
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

    private void setBandUnitAndDescription(H5ScalarDS ds, Band band) throws HDF5Exception {
        band.setDescription(ProbaVUtils.getDescriptionFromAttributes(ds.getMetadata()));
        band.setUnit(ProbaVUtils.getUnitFromAttributes(ds.getMetadata()));
    }

    private void setSpectralBandProperties(Band band) {
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

    private void setSynthesisGeoCoding(Product product, TreeNode inputFileRootNode, TreeNode level3ChildNode)
            throws HDF5Exception {

        final H5Group h5GeometryGroup = (H5Group) ((DefaultMutableTreeNode) level3ChildNode).getUserObject();
        final List geometryMetadata = h5GeometryGroup.getMetadata();
        final double easting = ProbaVUtils.getGeometryCoordinateValueFromAttributes(geometryMetadata, "TOP_LEFT_LONGITUDE");
        final double northing = ProbaVUtils.getGeometryCoordinateValueFromAttributes(geometryMetadata, "TOP_LEFT_LATITUDE");
        // pixel size: 10deg/rasterDim, it is also in the 6th and 7th value of MAPPING attribute in the raster nodes
        final double topLeftLon = easting;
        final double topRightLon = ProbaVUtils.getGeometryCoordinateValueFromAttributes(geometryMetadata, "TOP_RIGHT_LONGITUDE");
        final double pixelSizeX = Math.abs(topRightLon - topLeftLon) / productWidth;
        final double topLeftLat = northing;
        final double bottomLeftLat = ProbaVUtils.getGeometryCoordinateValueFromAttributes(geometryMetadata, "BOTTOM_LEFT_LATITUDE");
        final double pixelSizeY = (topLeftLat - bottomLeftLat) / productHeight;

        final H5Group h5RootGroup = (H5Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
        final List rootMetadata = h5RootGroup.getMetadata();
        final String crsString = ProbaVUtils.getGeometryCrsStringFromAttributes(rootMetadata);
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
            final CrsGeoCoding geoCoding = new CrsGeoCoding(crs, productWidth, productHeight, easting, northing, pixelSizeX, pixelSizeY);
            product.setGeoCoding(geoCoding);
        } catch (Exception e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot attach geocoding: " + e.getMessage());
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

    private boolean isSynthesisViewAngleGroupNode(String level3GeometryChildNodeName) {
        return level3GeometryChildNodeName.equals("SWIR") || level3GeometryChildNodeName.equals("VNIR");
    }

    private boolean isSynthesisSunAngleDataNode(String level3GeometryChildNodeName) {
        return level3GeometryChildNodeName.equals("SAA") || level3GeometryChildNodeName.equals("SZA");
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

    // we don't want scalarDS.read(), as it reads the whole dataset into memory
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

}
