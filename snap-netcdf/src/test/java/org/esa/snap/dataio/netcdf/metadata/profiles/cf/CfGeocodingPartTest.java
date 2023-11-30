package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

//import com.bc.ceres.annotation.STTM;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    @Test
    //@STTM("SNAP-3584")
    public void testReadVarAsDoubleArray() throws IOException {
        final Variable variable = mock(Variable.class);
        final Array array = mock(Array.class);
        final double[] data = {1.0, 2.0, 3.0};

        when(variable.read()).thenReturn(array);
        when(array.get1DJavaArray(DataType.DOUBLE)).thenReturn(data);

        final double[] dataRead = CfGeocodingPart.readVarAsDoubleArray(variable);
        assertArrayEquals(data, dataRead, 1e-8);

        verify(variable, times(1)).read();
        verify(array, times(1)).get1DJavaArray(DataType.DOUBLE);
        verifyNoMoreInteractions(variable, array);
    }
}
