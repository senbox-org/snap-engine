package org.esa.beam.dataio;

import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static short[][] convert1Dto2DShort( final short[] array1D, final int rows, final int cols ) {
        if (array1D.length != (rows*cols))
            throw new IllegalArgumentException("Invalid array1D length");

        short[][] array2D = new short[rows][cols];
        for ( int i = 0; i < rows; i++ )
            System.arraycopy(array1D, (i*cols), array2D[i], 0, cols);

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
        return scaleFactor;
    }
}
