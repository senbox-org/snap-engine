package org.esa.snap.core.gpf.descriptor;

import org.esa.snap.core.gpf.Operator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class DefaultOperatorDescriptorTest {

    private DefaultOperatorDescriptor operatorDescriptor;

    @Before
    public void setUp() {
        operatorDescriptor = new DefaultOperatorDescriptor();
    }

    @Test
    public void testDefaultProperties() {
        assertNull(operatorDescriptor.getName());
        assertNull(operatorDescriptor.getAlias());
        assertNull(operatorDescriptor.getVersion());
        assertNull(operatorDescriptor.getDescription());
        assertArrayEquals(new ParameterDescriptor[0], operatorDescriptor.getParameterDescriptors());
        assertArrayEquals(new SourceProductDescriptor[0], operatorDescriptor.getSourceProductDescriptors());
        assertArrayEquals(new TargetPropertyDescriptor[0], operatorDescriptor.getTargetPropertyDescriptors());
        assertSame(Operator.class, operatorDescriptor.getOperatorClass());
    }

    @Test
    public void testIsInternal() {
        operatorDescriptor.internal = null;
        assertFalse(operatorDescriptor.isInternal());

        operatorDescriptor.internal = false;
        assertFalse(operatorDescriptor.isInternal());

        operatorDescriptor.internal = true;
        assertTrue(operatorDescriptor.isInternal());
    }

    @Test
    public void testIsAutoWriteDisabled()  {
        operatorDescriptor.autoWriteSuppressed = null;
        assertFalse(operatorDescriptor.isAutoWriteDisabled());

        operatorDescriptor.autoWriteSuppressed = false;
        assertFalse(operatorDescriptor.isAutoWriteDisabled());

        operatorDescriptor.autoWriteSuppressed = true;
        assertTrue(operatorDescriptor.isAutoWriteDisabled());
    }

}
