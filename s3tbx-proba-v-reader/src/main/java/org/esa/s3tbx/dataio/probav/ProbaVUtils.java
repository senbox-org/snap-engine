package org.esa.s3tbx.dataio.probav;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.h5.H5Datatype;
import ncsa.hdf.object.h5.H5Group;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.SystemUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;

/**
 * Proba-V utility methods
 *
 * @author olafd
 */
public class ProbaVUtils {

    /**
     * Returns the value of a given HDF attribute
     *
     * @param attribute - input attribute
     * @return the value as string
     */
    public static String getAttributeValue(Attribute attribute) {
        String result = "";
        switch (attribute.getType().getDatatypeClass()) {
            case Datatype.CLASS_INTEGER:
                int[] ivals = (int[]) attribute.getValue();
                for (int ival : ivals) {
                    result = result.concat(Integer.toString(ival) + " ");
                }
                break;
            case Datatype.CLASS_FLOAT:
                float[] fvals = (float[]) attribute.getValue();
                for (float fval : fvals) {
                    result = result.concat(Float.toString(fval) + " ");
                }
                break;
            case Datatype.CLASS_STRING:
                String[] svals = (String[]) attribute.getValue();
                for (String sval : svals) {
                    result = result.concat(sval + " ");
                }
                break;
            default:
                break;
        }

        return result.trim();
    }

    /**
     * Returns the value of an HDF string attribute with given name
     *
     * @param metadata - the metadata containing the attributes
     * @param attributeName - the attribute name
     * @return the value as string
     */
    public static String getStringAttributeValue(List<Attribute> metadata, String attributeName) {
        String stringAttr = null;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals(attributeName)) {
                try {
                    stringAttr = getAttributeValue(attribute);
                } catch (NumberFormatException e) {
                    SystemUtils.LOG.log(Level.WARNING, "Cannot parse string attribute: " +
                            e.getMessage());
                }
            }
        }
        return stringAttr;
    }

    /**
     * Returns the value of an HDF double attribute with given name
     *
     * @param metadata - the metadata containing the attributes
     * @param attributeName - the attribute name
     * @return the value as double
     */
    public static double getDoubleAttributeValue(List<Attribute> metadata, String attributeName) {
        double doubleAttr = Double.NaN;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals(attributeName)) {
                try {
                    doubleAttr = Double.parseDouble(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    SystemUtils.LOG.log(Level.WARNING, "Cannot parse float attribute: " + e.getMessage());
                }
            }
        }
        return doubleAttr;
    }

    /**
     * Returns product start/end times extracted from HDF attribute
     *
     * @param metadata- the metadata containing the attributes
     *
     * @return  start/end times as String[]
     */
    public static String[] getStartEndTimeFromAttributes(List<Attribute> metadata) {
        String[] startStopTimes = new String[2];
        String startDate = "";
        String startTime = "";
        String endDate = "";
        String endTime = "";
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("OBSERVATION_START_DATE")) {
                startDate = getAttributeValue(attribute);
            } else if (attribute.getName().equals("OBSERVATION_START_TIME")) {
                startTime = getAttributeValue(attribute);
            } else if (attribute.getName().equals("OBSERVATION_END_DATE")) {
                endDate = getAttributeValue(attribute);
            } else if (attribute.getName().equals("OBSERVATION_END_TIME")) {
                endTime = getAttributeValue(attribute);
            }
        }

        // format is 'yyyy-mm-dd hh:mm:ss'
        startStopTimes[0] = startDate + " " + startTime;
        startStopTimes[1] = endDate + " " + endTime;
        return startStopTimes;
    }

    /**
     * Reads data from a Proba-V HDF input file into a data buffer
     *
     * @param file_id - HDF file id
     * @param width - buffer width
     * @param height - buffer height
     * @param offsetX - buffer X offset
     * @param offsetY - buffer Y offset
     * @param datasetName - the HDF dataset name
     * @param datatypeClass - the HDF datatype
     * @param destBuffer - the data buffer being filled
     */
    public static void readProbaVData(int file_id,
                                      int width, int height, long offsetX, long offsetY,
                                      String datasetName, int datatypeClass,
                                      ProductData destBuffer) {
        try {
            final int dataset_id = H5.H5Dopen(file_id,                       // Location identifier
                                              datasetName,                   // Dataset name
                                              HDF5Constants.H5P_DEFAULT);    // Identifier of dataset access property list

            final int dataspace_id = H5.H5Dget_space(dataset_id);

            final long[] offset = {offsetY, offsetX};
            final long[] count = {height, width};

            H5.H5Sselect_hyperslab(                                dataspace_id,                   // Identifier of dataspace selection to modify
                                   HDF5Constants.H5S_SELECT_SET,   // Operation to perform on current selection.
                                   offset,                         // Offset of start of hyperslab
                                   null,                           // Hyperslab stride.
                                   count,                          // Number of blocks included in hyperslab.
                                   null);                          // Size of block in hyperslab.

            final int memspace_id = H5.H5Screate_simple(count.length, // Number of dimensions of dataspace.
                                                        count,        // An array of the size of each dimension.
                                                        null);       // An array of the maximum size of each dimension.

            final long[] offset_out = {0L, 0L};
            H5.H5Sselect_hyperslab(                                    memspace_id,                        // Identifier of dataspace selection to modify
                                   HDF5Constants.H5S_SELECT_SET,       // Operation to perform on current selection.
                                   offset_out,                         // Offset of start of hyperslab
                                   null,                               // Hyperslab stride.
                                   count,                              // Number of blocks included in hyperslab.
                                   null);                          // Size of block in hyperslab.

            int dataType = ProbaVUtils.getDatatypeForH5Dread(datatypeClass);

            if (destBuffer != null) {
                H5.H5Dread(                               dataset_id,                    // Identifier of the dataset read from.
                           dataType,                      // Identifier of the memory datatype.
                           memspace_id,                   //  Identifier of the memory dataspace.
                           dataspace_id,                  // Identifier of the dataset's dataspace in the file.
                           HDF5Constants.H5P_DEFAULT,     // Identifier of a transfer property list for this I/O operation.
                           destBuffer.getElems());        // Buffer to store data read from the file.

                H5.H5Dclose(dataset_id);
                H5.H5Sclose(memspace_id);
            }

        } catch (Exception e) {
            SystemUtils.LOG.log(Level.SEVERE, "Cannot read ProbaV raster data '" + datasetName + "': " + e.getMessage());
        }
    }

    /**
     * Checks by data set tree node inspection if a Proba-V product is a Level 3 NDVI product.
     *
     * @param productTypeNode - the data set tree node (starting at LEVEL3)
     * @return boolean
     */
    public static boolean isLevel3Ndvi(TreeNode productTypeNode) {
        boolean hasNdvi = false;
        for (int i = 0; i < productTypeNode.getChildCount(); i++) {
            // we have: 'GEOMETRY', 'NDVI', 'QUALITY', 'RADIOMETRY', 'TIME'
            final TreeNode productTypeChildNode = productTypeNode.getChildAt(i);
            final String productTypeChildNodeName = productTypeChildNode.toString();

            if (productTypeChildNodeName.equals("NDVI")) {
                hasNdvi = true;
                break;
            }
        }

        if (hasNdvi) {
            for (int i = 0; i < productTypeNode.getChildCount(); i++) {
                // check if GEOMETRY, QUALITY, RADIOMETRY, TIME are present but empty
                final TreeNode productTypeChildNode = productTypeNode.getChildAt(i);
                final String productTypeChildNodeName = productTypeChildNode.toString();
                if (!productTypeChildNodeName.equals("NDVI")) {
                    if (productTypeChildNode.getChildCount() > 0) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks by data set tree node inspection if a Proba-V product is a Level 3 TOC product.
     *
     * @param productTypeNode - the data set tree node (starting at LEVEL3)
     * @return boolean
     */
    public static boolean isLevel3Toc(TreeNode productTypeNode) {
        return isReflectanceType(productTypeNode, "TOC");
    }

    /**
     * Checks if tree child note corresponds to viewing angle group
     *
     * @param geometryChildNodeName - the tree child note
     * @return boolean
     */
    public static boolean isProbaVViewAngleGroupNode(String geometryChildNodeName) {
        return geometryChildNodeName.equals("SWIR") || geometryChildNodeName.equals("VNIR");
    }

    /**
     * Checks if tree child note corresponds to sun angle group
     *
     * @param geometryChildNodeName - the tree child note
     * @return boolean
     */
    public static boolean isProbaVSunAngleDataNode(String geometryChildNodeName) {
        return geometryChildNodeName.equals("SAA") || geometryChildNodeName.equals("SZA");
    }

    /**
     * Creates a target band matching given metadata information
     *
     * @param product - the target product
     * @param metadata - the HDF metadata attributes
     * @param bandName - band name
     * @param dataType - data type
     *
     * @return the target band
     * @throws Exception
     */
    public static Band createTargetBand(Product product, List<Attribute> metadata, String bandName, int dataType) throws Exception {
        final double scaleFactorAttr = ProbaVUtils.getDoubleAttributeValue(metadata, "SCALE");
        final double scaleFactor = Double.isNaN(scaleFactorAttr) ? 1.0f : scaleFactorAttr;
        final double scaleOffsetAttr = ProbaVUtils.getDoubleAttributeValue(metadata, "OFFSET");
        final double scaleOffset = Double.isNaN(scaleOffsetAttr) ? 0.0f : scaleOffsetAttr;
        final Band band = product.addBand(bandName, dataType);
        band.setScalingFactor(1.0 / scaleFactor);
        band.setScalingOffset(-1.0 * scaleOffset / scaleFactor);

        return band;
    }

    /**
     * Provides a HDF5 scalar dataset corresponding to given HDF product node
     *
     * @param level3BandsChildNode - the data node
     *
     * @return - the data set (H5ScalarDS)
     * @throws HDF5Exception
     */
    public static H5ScalarDS getH5ScalarDS(TreeNode level3BandsChildNode) throws HDF5Exception {
        H5ScalarDS scalarDS = (H5ScalarDS) ((DefaultMutableTreeNode) level3BandsChildNode).getUserObject();
        scalarDS.open();
        scalarDS.init();
        return scalarDS;
    }

    /**
     * Extracts a HDF metadata element and adds accordingly to given product
     *
     * @param rootMetadata - the HDF metadata
     * @param product - the product
     * @param metadataElementName - the element name
     */
    public static void addProbaVMetadataElement(List<Attribute> rootMetadata,
                                                final Product product,
                                                String metadataElementName) {
        final MetadataElement metadataElement = new MetadataElement(metadataElementName);

        for (Attribute attribute : rootMetadata) {
            metadataElement.addAttribute(new MetadataAttribute(attribute.getName(),
                                                               ProductData.createInstance(ProbaVUtils.getAttributeValue(attribute)), true));
        }
        product.getMetadataRoot().addElement(metadataElement);
    }

    /**
     * Extracs start/stop times from HDF metadata and adds to given product
     *
     * @param product - the product
     * @param level3ChildNode - the HDF node containing the time information
     *
     * @throws HDF5Exception
     * @throws ParseException
     */
    public static void addStartStopTimes(Product product, DefaultMutableTreeNode level3ChildNode) throws HDF5Exception, ParseException {
        final H5Group timeGroup = (H5Group) level3ChildNode.getUserObject();
        final List timeMetadata = timeGroup.getMetadata();
        product.setStartTime(ProductData.UTC.parse(ProbaVUtils.getStartEndTimeFromAttributes(timeMetadata)[0],
                                                   ProbaVConstants.PROBAV_DATE_FORMAT_PATTERN));
        product.setEndTime(ProductData.UTC.parse(ProbaVUtils.getStartEndTimeFromAttributes(timeMetadata)[1],
                                                 ProbaVConstants.PROBAV_DATE_FORMAT_PATTERN));
    }

    /**
     * Extracts quality info from HDF metadata and adds to given product
     *
     * @param product - the product
     * @param level3ChildNode - the HDF node containing the time information
     *
     * @throws HDF5Exception
     */
    public static void addQualityMetadata(Product product, DefaultMutableTreeNode level3ChildNode) throws HDF5Exception {
        final H5Group qualityGroup = (H5Group) level3ChildNode.getUserObject();
        final List qualityMetadata = qualityGroup.getMetadata();
        ProbaVUtils.addProbaVMetadataElement(qualityMetadata, product, ProbaVConstants.QUALITY_NAME);
    }

    /**
     * Extracts unit and description from HDF metadata and adds to given band
     *
     * @param metadata - HDF metadata
     * @param band - the band
     *
     * @throws HDF5Exception
     */
    public static void setBandUnitAndDescription(List<Attribute> metadata, Band band) throws HDF5Exception {
        band.setDescription(ProbaVUtils.getStringAttributeValue(metadata, "DESCRIPTION"));
        band.setUnit(ProbaVUtils.getStringAttributeValue(metadata, "UNITS"));
    }

    /**
     * Sets Proba-V spectral band properties
     *
     * @param band - the spectral band
     */
    public static void setSpectralBandProperties(Band band) {
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

    /**
     * Sets the Proba-V geo coding to a product as extracted from HDF metadata information
     *
     * @param product - the product
     * @param inputFileRootNode - HDF root tree node
     * @param productTypeChildNode - the product type child node (LEVEL2A or LEVEL3)
     * @param productWidth - product width
     * @param productHeight - product height
     *
     * @throws HDF5Exception
     */
    public static void setProbaVGeoCoding(Product product, TreeNode inputFileRootNode, TreeNode productTypeChildNode,
                                          int productWidth, int productHeight) throws HDF5Exception {

        final H5Group h5GeometryGroup = (H5Group) ((DefaultMutableTreeNode) productTypeChildNode).getUserObject();
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

    /**
     * Provides a ProductData instance according to given HDF5 data type
     *
     * @param datatypeClass - the HDF5 data type
     * @param width - buffer width
     * @param height - buffer height
     *
     * @return the data buffer
     */
    public static ProductData getDataBufferForH5Dread(int datatypeClass, int width, int height) {
        switch (datatypeClass) {
            case H5Datatype.CLASS_CHAR:
                return ProductData.createInstance(new byte[width * height]);
            case H5Datatype.CLASS_FLOAT:
                return ProductData.createInstance(new float[width * height]);
            case H5Datatype.CLASS_INTEGER:
                return ProductData.createInstance(new short[width * height]);
            default:
                break;
        }
        return null;
    }

    //// private methods ////

    private static boolean isReflectanceType(TreeNode productTypeNode, String type) {
        for (int i = 0; i < productTypeNode.getChildCount(); i++) {
            // we have: 'GEOMETRY', 'NDVI', 'QUALITY', 'RADIOMETRY', 'TIME'
            final TreeNode productTypeChildNode = productTypeNode.getChildAt(i);
            final String productTypeChildNodeName = productTypeChildNode.toString();

            if (productTypeChildNodeName.equals("RADIOMETRY")) {
                // children are BLUE, RED, NIR, SWIR
                final TreeNode radiometryChildNode = productTypeChildNode.getChildAt(0);
                return radiometryChildNode.getChildAt(0).toString().equals(type);
            }
        }
        return false;
    }

    private static int getDatatypeForH5Dread(int datatypeClass) {
        switch (datatypeClass) {
            case H5Datatype.CLASS_BITFIELD:
                return HDF5Constants.H5T_NATIVE_UINT8;
            case H5Datatype.CLASS_CHAR:
                return HDF5Constants.H5T_NATIVE_UINT8;
            case H5Datatype.CLASS_FLOAT:
                return HDF5Constants.H5T_NATIVE_FLOAT;
            case H5Datatype.CLASS_INTEGER:
                return HDF5Constants.H5T_NATIVE_INT16;
            default:
                break;
        }
        return -1;
    }

}
