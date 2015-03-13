package org.esa.beam.dataio;

import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import org.esa.beam.util.logging.BeamLogManager;

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
        return 1.0f/scaleFactor;
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

}
