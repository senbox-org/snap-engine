package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Olaf Danne
 */
public class CfGeocodingPartTest {

    @Test
    public void testIsGlobalShifted180() {
        Array longitudeData = Array.makeArray(DataType.DOUBLE, 480, 0.0, 0.75);
        assertTrue(CfGeocodingPart.isGlobalShifted180(longitudeData));

        longitudeData = Array.makeArray(DataType.DOUBLE, 480, 0.75, 0.75);
        assertTrue(CfGeocodingPart.isGlobalShifted180(longitudeData));

        longitudeData = Array.makeArray(DataType.DOUBLE, 480, 0.375, 0.75);
        assertTrue(CfGeocodingPart.isGlobalShifted180(longitudeData));

        longitudeData = Array.makeArray(DataType.DOUBLE, 480, 1.0, 0.75);
        assertFalse(CfGeocodingPart.isGlobalShifted180(longitudeData));

        longitudeData = Array.makeArray(DataType.DOUBLE, 480, -0.375, 0.75);
        assertFalse(CfGeocodingPart.isGlobalShifted180(longitudeData));
    }
}
