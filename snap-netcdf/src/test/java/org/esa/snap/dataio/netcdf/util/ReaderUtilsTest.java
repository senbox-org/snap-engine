package org.esa.snap.dataio.netcdf.util;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReaderUtilsTest {

    @Test
    @STTM("SNAP-3825")
    public void testGetScalingFactor_scale_factor() {
        final Attribute scaleAttribute = mock(Attribute.class);
        when(scaleAttribute.isString()).thenReturn(false);
        when(scaleAttribute.getNumericValue()).thenReturn(26.98867);

        final Variable variable = mock(Variable.class);
        when(variable.findAttribute("scale_factor")).thenReturn(scaleAttribute);

        double scalingFactor = ReaderUtils.getScalingFactor(variable);
        assertEquals(26.98867, scalingFactor, 1e-8);
    }

    @Test
    @STTM("SNAP-3825")
    public void testGetScalingFactor_slope() {
        final Attribute scaleAttribute = mock(Attribute.class);
        when(scaleAttribute.isString()).thenReturn(false);
        when(scaleAttribute.getNumericValue()).thenReturn(27.335);

        final Variable variable = mock(Variable.class);
        when(variable.findAttribute("slope")).thenReturn(scaleAttribute);

        double scalingFactor = ReaderUtils.getScalingFactor(variable);
        assertEquals(27.335, scalingFactor, 1e-8);
    }

    @Test
    @STTM("SNAP-3825")
    public void testGetScalingFactor_scaling_factor() {
        final Attribute scaleAttribute = mock(Attribute.class);
        when(scaleAttribute.isString()).thenReturn(false);
        when(scaleAttribute.getNumericValue()).thenReturn(28.435);

        final Variable variable = mock(Variable.class);
        when(variable.findAttribute("scaling_factor")).thenReturn(scaleAttribute);

        double scalingFactor = ReaderUtils.getScalingFactor(variable);
        assertEquals(28.435, scalingFactor, 1e-8);
    }

    @Test
    @STTM("SNAP-3825")
    public void testGetScalingFactor_nonePresent() {
        final Variable variable = mock(Variable.class);

        double scalingFactor = ReaderUtils.getScalingFactor(variable);
        assertEquals(1.0, scalingFactor, 1e-8);
    }

    @Test
    @STTM("SNAP-3825")
    public void testGetAddOffset_scale_factor() {
        final Attribute offsetAttribute = mock(Attribute.class);
        when(offsetAttribute.isString()).thenReturn(false);
        when(offsetAttribute.getNumericValue()).thenReturn(-11.6);

        final Variable variable = mock(Variable.class);
        when(variable.findAttribute("add_offset")).thenReturn(offsetAttribute);

        final double offset = ReaderUtils.getAddOffset(variable);
        assertEquals(-11.6, offset, 1e-8);
    }

    @Test
    @STTM("SNAP-3825")
    public void testGetAddOffset_intercept() {
        final Attribute offsetAttribute = mock(Attribute.class);
        when(offsetAttribute.isString()).thenReturn(false);
        when(offsetAttribute.getNumericValue()).thenReturn(-12.7);

        final Variable variable = mock(Variable.class);
        when(variable.findAttribute("intercept")).thenReturn(offsetAttribute);

        final double offset = ReaderUtils.getAddOffset(variable);
        assertEquals(-12.7, offset, 1e-8);
    }

    @Test
    @STTM("SNAP-3825")
    public void testGetAddOffset_nonePresent() {
        final Variable variable = mock(Variable.class);

        final double offset = ReaderUtils.getAddOffset(variable);
        assertEquals(0.0, offset, 1e-8);
    }

    @Test
    @STTM("SNAP-3825")
    public void testMustScale() {
        assertTrue(ReaderUtils.mustScale(0.8, 0.0));
        assertTrue(ReaderUtils.mustScale(0.0, -273.0));
        assertTrue(ReaderUtils.mustScale(100.0, 206.0));

        assertFalse(ReaderUtils.mustScale(1.0, 0.0));
    }
}
