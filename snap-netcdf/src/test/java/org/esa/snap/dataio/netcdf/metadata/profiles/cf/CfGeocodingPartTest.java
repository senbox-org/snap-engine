package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    @STTM("SNAP-3584,SNAP-3825")
    public void testReadVarAsDoubleArray() throws IOException {
        final Variable variable = mock(Variable.class);
        final Array array = mock(Array.class);
        final double[] data = {1.0, 2.0, 3.0};

        when(variable.read()).thenReturn(array);
        when(array.get1DJavaArray(DataType.DOUBLE)).thenReturn(data);

        final double[] dataRead = CfGeocodingPart.readVarAsDoubleArray(variable);
        assertArrayEquals(data, dataRead, 1e-8);

        verify(variable, times(1)).read();
        verify(variable, times(5)).findAttribute(anyString());
        verify(array, times(1)).get1DJavaArray(DataType.DOUBLE);
        verifyNoMoreInteractions(variable, array);
    }

    @Test
    @STTM("SNAP-3640")
    public void testGetGeolocationVariables() {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);

        final Variable anyVar = mock(Variable.class);
        when(anyVar.getShortName()).thenReturn("anyVar");

        final Variable lonVar = mock(Variable.class);
        when(lonVar.getShortName()).thenReturn("longitude");

        final Variable latVar = mock(Variable.class);
        when(latVar.getShortName()).thenReturn("latitude");

        final List<Variable> variableList = new ArrayList<>();
        variableList.add(lonVar);
        variableList.add(anyVar);
        variableList.add(latVar);

        when(netcdfFile.getVariables()).thenReturn(variableList);

        final CfGeocodingPart.GeoVariables geoVariables = CfGeocodingPart.getGeolocationVariables(netcdfFile, "longitude", "latitude");
        assertNotNull(geoVariables);
        assertEquals(lonVar, geoVariables.lonVar);
        assertEquals(latVar, geoVariables.latVar);

        verify(netcdfFile, times(1)).getVariables();
        verifyNoMoreInteractions(netcdfFile);
    }

    @Test
    @STTM("SNAP-3640")
    public void testGetGeolocationVariables_lonMissing() {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);

        final Variable anyVar = mock(Variable.class);
        when(anyVar.getShortName()).thenReturn("anyVar");

        final Variable latVar = mock(Variable.class);
        when(latVar.getShortName()).thenReturn("latitude");

        final List<Variable> variableList = new ArrayList<>();
        variableList.add(anyVar);
        variableList.add(latVar);

        when(netcdfFile.getVariables()).thenReturn(variableList);

        final CfGeocodingPart.GeoVariables geoVariables = CfGeocodingPart.getGeolocationVariables(netcdfFile, "longitude", "latitude");
        assertNotNull(geoVariables);
        assertNull(geoVariables.lonVar);
        assertEquals(latVar, geoVariables.latVar);

        verify(netcdfFile, times(1)).getVariables();
        verifyNoMoreInteractions(netcdfFile);
    }

    @Test
    @STTM("SNAP-3640")
    public void testGetGeolocationVariables_latMissing() {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);

        final Variable anyVar = mock(Variable.class);
        when(anyVar.getShortName()).thenReturn("anyVar");

        final Variable lonVar = mock(Variable.class);
        when(lonVar.getShortName()).thenReturn("longitude");

        final List<Variable> variableList = new ArrayList<>();
        variableList.add(lonVar);
        variableList.add(anyVar);

        when(netcdfFile.getVariables()).thenReturn(variableList);

        final CfGeocodingPart.GeoVariables geoVariables = CfGeocodingPart.getGeolocationVariables(netcdfFile, "longitude", "latitude");
        assertNotNull(geoVariables);
        assertEquals(lonVar, geoVariables.lonVar);
        assertNull(geoVariables.latVar);

        verify(netcdfFile, times(1)).getVariables();
        verifyNoMoreInteractions(netcdfFile);
    }
}
