package org.esa.beam.dataio;

import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.util.List;
import java.util.logging.Level;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.03.2015
 * Time: 09:53
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
                    result = result.concat(Integer.toString(ival) + "  ");
                }
                break;
            case Datatype.CLASS_FLOAT:
                float[] fvals = (float[]) attribute.getValue();
                for (float fval : fvals) {
                    result = result.concat(Float.toString(fval) + "  ");
                }
                break;
            case Datatype.CLASS_STRING:
                String[] svals = (String[]) attribute.getValue();
                for (String sval : svals) {
                    result = result.concat(sval + "  ");
                }
                break;
            default:
                break;
        }

        return result;
    }

    public static short[][] convert1Dto2DShort(final short[] array1D, final int rows, final int cols) {
        if (array1D.length != (rows * cols))
            throw new IllegalArgumentException("Invalid array1D length");

        short[][] array2D = new short[rows][cols];
        for (int i = 0; i < rows; i++)
            System.arraycopy(array1D, (i * cols), array2D[i], 0, cols);

        return array2D;
    }

    public static float getScaleFactor(List<Attribute> metadata) {
        float scaleFactor = 1.0f;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("SCALE")) {
                try {
                    scaleFactor = Float.parseFloat(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse scale factor: " + e.getMessage());
                }
            }
        }
        return 1.0f / scaleFactor;
    }

    public static float getScaleOffset(List<Attribute> metadata) {
        float scaleOffset = 0.0f;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("OFFSET")) {
                try {
                    scaleOffset = Float.parseFloat(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse scale offset: " + e.getMessage());
                }
            }
        }
        return scaleOffset;
    }

    public static double getGeometryCoordinateValue(List<Attribute> metadata, String coordinateName) {
        double coordValue = Double.NaN;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals(coordinateName)) {
                try {
                    coordValue = Float.parseFloat(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse geometry coordinate: " +
                            e.getMessage());
                }
            }
        }
        return coordValue;
    }

    public static String getGeometryCrsString(List<Attribute> metadata) {
        String crsString = null;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("MAP_PROJECTION_WKT")) {
                try {
                    crsString = getAttributeValue(attribute);
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse CRS WKT string: " +
                            e.getMessage());
                }
            }
        }
        return crsString;
    }

    public static int getSynthesisProductRasterDimension(String productName) {
        // we have the products:
        // PROBAV_S1_TOA_X07Y04_20131025_1KM_V003
        // PROBAV_S1_TOA_X00Y01_20131025_333M_V003
        // PROBAV_S1_TOC_X07Y04_20131025_1KM_V003
        // PROBAV_S1_TOC_X00Y01_20131025_333M_V003
        // PROBAV_S10_TOC_X07Y04_20131025_1KM_V003
        // PROBAV_S10_TOC_X00Y01_20131025_333M_V003
        //
        return (isSynthesis1kmProduct(productName) ? ProbaVConstants.SYNTHESIS_PRODUCT_DIMENSION_1km :
                ProbaVConstants.SYNTHESIS_PRODUCT_DIMENSION_333m);
    }

    public static boolean isSynthesis1kmProduct(String productName) {
        return productName.toUpperCase().contains("_1KM_");
    }

    public static boolean isSynthesis333mProduct(String productName) {
        return productName.toUpperCase().contains("_333M_");
    }

    public static boolean isSynthesis1dayProduct(String productName) {
        return productName.toUpperCase().startsWith("PROBAV_S1_");
    }

    public static boolean isSynthesis10dayProduct(String productName) {
        return productName.toUpperCase().startsWith("PROBAV_S10_");
    }

    public static void addSmMasks(Product probavProduct) {
        ProductNodeGroup<Mask> maskGroup = probavProduct.getMaskGroup();
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.CLEAR_FLAG_NAME,
                ProbaVConstants.CLEAR_FLAG_DESCR, ProbaVConstants.SM_COLORS[0], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.UNDEFINED_FLAG_NAME,
                ProbaVConstants.UNDEFINED_FLAG_DESCR, ProbaVConstants.SM_COLORS[1], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.CLOUD_FLAG_NAME,
                ProbaVConstants.CLOUD_FLAG_DESCR, ProbaVConstants.SM_COLORS[2], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SNOWICE_FLAG_NAME,
                ProbaVConstants.SNOWICE_FLAG_DESCR, ProbaVConstants.SM_COLORS[3], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.CLOUD_SHADOW_FLAG_NAME,
                ProbaVConstants.CLOUD_SHADOW_FLAG_DESCR, ProbaVConstants.SM_COLORS[4], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.LAND_FLAG_NAME,
                ProbaVConstants.LAND_FLAG_DESCR, ProbaVConstants.SM_COLORS[5], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.GOOD_SWIR_FLAG_NAME,
                ProbaVConstants.GOOD_SWIR_FLAG_DESCR, ProbaVConstants.SM_COLORS[6], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.GOOD_NIR_FLAG_NAME,
                ProbaVConstants.GOOD_NIR_FLAG_DESCR, ProbaVConstants.SM_COLORS[7], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.GOOD_RED_FLAG_NAME,
                ProbaVConstants.GOOD_RED_FLAG_DESCR, ProbaVConstants.SM_COLORS[8], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.GOOD_BLUE_FLAG_NAME,
                ProbaVConstants.GOOD_BLUE_FLAG_DESCR, ProbaVConstants.SM_COLORS[9], 0.5f);
    }

    public static void addSmFlags(FlagCoding probavSmFlagCoding) {
        probavSmFlagCoding.addFlag(ProbaVConstants.CLEAR_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.CLEAR_BIT_INDEX),
                ProbaVConstants.CLEAR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.UNDEFINED_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.UNDEFINED_BIT_INDEX),
                ProbaVConstants.UNDEFINED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.CLOUD_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.CLOUD_BIT_INDEX),
                ProbaVConstants.CLOUD_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SNOWICE_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.SNOWICE_INDEX),
                ProbaVConstants.SNOWICE_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.CLOUD_SHADOW_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.CLOUD_SHADOW_BIT_INDEX),
                ProbaVConstants.CLOUD_SHADOW_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.LAND_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.LAND_BIT_INDEX),
                ProbaVConstants.LAND_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.GOOD_SWIR_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.GOOD_SWIR_INDEX),
                ProbaVConstants.GOOD_SWIR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.GOOD_NIR_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.GOOD_NIR_BIT_INDEX),
                ProbaVConstants.GOOD_NIR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.GOOD_RED_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.GOOD_RED_BIT_INDEX),
                ProbaVConstants.GOOD_RED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.GOOD_BLUE_FLAG_NAME,
                BitSetter.setFlag(0, ProbaVConstants.GOOD_BLUE_BIT_INDEX),
                ProbaVConstants.GOOD_BLUE_FLAG_DESCR);
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
