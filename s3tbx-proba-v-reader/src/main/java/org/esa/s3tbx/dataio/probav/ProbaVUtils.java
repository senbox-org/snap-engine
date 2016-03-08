package org.esa.s3tbx.dataio.probav;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.h5.H5Datatype;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.SystemUtils;

import java.awt.Color;
import java.util.List;
import java.util.logging.Level;

/**
 * Proba-V utility methods
 *
 * @author olafd
 */
public class ProbaVUtils {

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

//    public static float getFloatAttributeValue(List<Attribute> metadata, String attributeName) {
//        float floatAttr = Float.NaN;
//        for (Attribute attribute : metadata) {
//            if (attribute.getName().equals(attributeName)) {
//                try {
//                    floatAttr = Float.parseFloat(getAttributeValue(attribute));
//                } catch (NumberFormatException e) {
//                    SystemUtils.LOG.log(Level.WARNING, "Cannot parse float attribute: " + e.getMessage());
//                }
//            }
//        }
//        return floatAttr;
//    }

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

    public static void addSynthesisQualityMasks(Product probavProduct) {
        ProductNodeGroup<Mask> maskGroup = probavProduct.getMaskGroup();
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_CLEAR_FLAG_NAME,
                ProbaVConstants.SM_CLEAR_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[0], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_UNDEFINED_FLAG_NAME,
                ProbaVConstants.SM_UNDEFINED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[1], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_CLOUD_FLAG_NAME,
                ProbaVConstants.SM_CLOUD_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[2], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_SNOWICE_FLAG_NAME,
                ProbaVConstants.SM_SNOWICE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[3], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_CLOUD_SHADOW_FLAG_NAME,
                ProbaVConstants.SM_CLOUD_SHADOW_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[4], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_LAND_FLAG_NAME,
                ProbaVConstants.SM_LAND_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[5], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_SWIR_FLAG_NAME,
                ProbaVConstants.SM_GOOD_SWIR_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[6], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_NIR_FLAG_NAME,
                ProbaVConstants.SM_GOOD_NIR_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[7], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_RED_FLAG_NAME,
                ProbaVConstants.SM_GOOD_RED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[8], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_BLUE_FLAG_NAME,
                ProbaVConstants.SM_GOOD_BLUE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[9], 0.5f);
    }

    public static void addSynthesisQualityFlags(FlagCoding probavSmFlagCoding) {
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_CLEAR_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_CLEAR_BIT_INDEX),
                                   ProbaVConstants.SM_CLEAR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_UNDEFINED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_UNDEFINED_BIT_INDEX),
                                   ProbaVConstants.SM_UNDEFINED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_CLOUD_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_CLOUD_BIT_INDEX),
                                   ProbaVConstants.SM_CLOUD_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_SNOWICE_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_SNOWICE_INDEX),
                                   ProbaVConstants.SM_SNOWICE_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_CLOUD_SHADOW_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_CLOUD_SHADOW_BIT_INDEX),
                                   ProbaVConstants.SM_CLOUD_SHADOW_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_LAND_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_LAND_BIT_INDEX),
                                   ProbaVConstants.SM_LAND_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_SWIR_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_SWIR_INDEX),
                                   ProbaVConstants.SM_GOOD_SWIR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_NIR_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_NIR_BIT_INDEX),
                                   ProbaVConstants.SM_GOOD_NIR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_RED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_RED_BIT_INDEX),
                                   ProbaVConstants.SM_GOOD_RED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_BLUE_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_BLUE_BIT_INDEX),
                                   ProbaVConstants.SM_GOOD_BLUE_FLAG_DESCR);
    }

    public static void addL1cQualityMasks(Product probavProduct, String sourceQualityBandName, String targetQualityFlagBandName) {
        ProductNodeGroup<Mask> maskGroup = probavProduct.getMaskGroup();
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_CORRECT_FLAG_NAME,
                ProbaVConstants.Q_CORRECT_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[0], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_MISSING_FLAG_NAME,
                ProbaVConstants.Q_MISSING_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[1], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_WAS_SATURATED_FLAG_NAME,
                ProbaVConstants.Q_WAS_SATURATED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[2], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_BECAME_SATURATED_FLAG_NAME,
                ProbaVConstants.Q_BECAME_SATURATED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[3], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_BECAME_NEGATIVE_FLAG_NAME,
                ProbaVConstants.Q_BECAME_NEGATIVE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[4], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_INTERPOLATED_FLAG_NAME,
                ProbaVConstants.Q_INTERPOLATED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[5], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_BORDER_COMPRESSED_FLAG_NAME,
                ProbaVConstants.Q_BORDER_COMPRESSED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[6], 0.5f);
    }

    public static void addL1cQualityFlags(FlagCoding probavSmFlagCoding, String sourceQualityBandName) {
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_CORRECT_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_CORRECT_BIT_INDEX),
                                   ProbaVConstants.Q_CORRECT_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_MISSING_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_MISSING_BIT_INDEX),
                                   ProbaVConstants.Q_MISSING_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_WAS_SATURATED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_WAS_SATURATED_BIT_INDEX),
                                   ProbaVConstants.Q_WAS_SATURATED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_BECAME_SATURATED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_BECAME_SATURATED_INDEX),
                                   ProbaVConstants.Q_BECAME_SATURATED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_BECAME_NEGATIVE_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_BECAME_NEGATIVE_BIT_INDEX),
                                   ProbaVConstants.Q_BECAME_NEGATIVE_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_INTERPOLATED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_INTERPOLATED_BIT_INDEX),
                                   ProbaVConstants.Q_INTERPOLATED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_BORDER_COMPRESSED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_BORDER_COMPRESSED_INDEX),
                                   ProbaVConstants.Q_BORDER_COMPRESSED_FLAG_DESCR);
    }


    public static ProductData getProbaVRasterData(int file_id,
                                                  int sourceWidth, int sourceHeight,
                                                  String datasetName, int datatypeClass) {
        try {
            final int dataset_id = H5.H5Dopen(file_id,                       // Location identifier
                                              datasetName, // Dataset name
                                              HDF5Constants.H5P_DEFAULT);    // Identifier of dataset access property list

            final int dataspace_id = H5.H5Dget_space(dataset_id);

            final long[] offset = {0L, 0L};
            final long[] count = {sourceWidth, sourceHeight};

            H5.H5Sselect_hyperslab(dataspace_id,                   // Identifier of dataspace selection to modify
                                   HDF5Constants.H5S_SELECT_SET,   // Operation to perform on current selection.
                                   offset,                         // Offset of start of hyperslab
                                   null,                           // Hyperslab stride.
                                   count,                          // Number of blocks included in hyperslab.
                                   null);                          // Size of block in hyperslab.

            final int memspace_id = H5.H5Screate_simple(count.length, // Number of dimensions of dataspace.
                                                        count,        // An array of the size of each dimension.
                                                        null);       // An array of the maximum size of each dimension.

            final long[] offset_out = {0L, 0L};
            H5.H5Sselect_hyperslab(memspace_id,                   // Identifier of dataspace selection to modify
                                   HDF5Constants.H5S_SELECT_SET,   // Operation to perform on current selection.
                                   offset_out,                         // Offset of start of hyperslab
                                   null,                           // Hyperslab stride.
                                   count,                          // Number of blocks included in hyperslab.
                                   null);                          // Size of block in hyperslab.

            int dataType = ProbaVUtils.getDatatypeForH5Dread(datatypeClass);
            ProductData destBuffer = ProbaVUtils.getDataBufferForH5Dread(datatypeClass, sourceWidth, sourceHeight);

            H5.H5Dread(dataset_id,                    // Identifier of the dataset read from.
                       dataType,                      // Identifier of the memory datatype.
                       memspace_id,                   //  Identifier of the memory dataspace.
                       dataspace_id,                  // Identifier of the dataset's dataspace in the file.
                       HDF5Constants.H5P_DEFAULT,     // Identifier of a transfer property list for this I/O operation.
                       destBuffer.getElems());        // Buffer to store data read from the file.

            H5.H5Dclose(dataset_id);
            H5.H5Sclose(memspace_id);

            return destBuffer;
        } catch (Exception e) {
            SystemUtils.LOG.log(Level.SEVERE, "Cannot read ProbaV raster data '" + datasetName + "': " + e.getMessage());
        }

        return null;
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

    private static ProductData getDataBufferForH5Dread(int datatypeClass, int width, int height) {
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

    private static void addMask(Product mod35Product, ProductNodeGroup<Mask> maskGroup,
                                String bandName, String flagName, String description, Color color, float transparency) {
        int width = mod35Product.getSceneRasterWidth();
        int height = mod35Product.getSceneRasterHeight();
        String maskPrefix = "";
        Mask mask = Mask.BandMathsType.create(maskPrefix + flagName,
                                              description, width, height,
                                              bandName + "." + flagName,
                                              color, transparency);
        maskGroup.add(mask);
    }

}
