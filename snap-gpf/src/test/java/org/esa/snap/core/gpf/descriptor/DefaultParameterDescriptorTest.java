package org.esa.snap.core.gpf.descriptor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultParameterDescriptorTest {

    private DefaultParameterDescriptor defaultParameterDescriptor;

    @Before
    public void setUp() {
        defaultParameterDescriptor = new DefaultParameterDescriptor();
    }

    @Test
    public void testSetIsNotNull() {
        assertFalse(defaultParameterDescriptor.isNotNull());

        defaultParameterDescriptor.setNotNull(false);
        assertFalse(defaultParameterDescriptor.isNotNull());

        defaultParameterDescriptor.setNotNull(true);
        assertTrue(defaultParameterDescriptor.isNotNull());
    }

    @Test
    public void testSetIsNotEmpty() {
        assertFalse(defaultParameterDescriptor.isNotEmpty());

        defaultParameterDescriptor.setNotEmpty(false);
        assertFalse(defaultParameterDescriptor.isNotEmpty());

        defaultParameterDescriptor.setNotEmpty(true);
        assertTrue(defaultParameterDescriptor.isNotEmpty());
    }

    @Test
    public void testSetIsDeprecated() {
        assertFalse(defaultParameterDescriptor.isDeprecated());

        defaultParameterDescriptor.setDeprecated(false);
        assertFalse(defaultParameterDescriptor.isDeprecated());

        defaultParameterDescriptor.setDeprecated(true);
        assertTrue(defaultParameterDescriptor.isDeprecated());
    }
}
