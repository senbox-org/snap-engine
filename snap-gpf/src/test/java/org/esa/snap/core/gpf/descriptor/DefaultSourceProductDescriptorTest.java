package org.esa.snap.core.gpf.descriptor;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultSourceProductDescriptorTest {

    @Test
    public void testSetIsOptional()  {
        final DefaultSourceProductDescriptor sourceProductDescriptor = new DefaultSourceProductDescriptor();

        assertFalse(sourceProductDescriptor.isOptional());

        sourceProductDescriptor.optional = false;
        assertFalse(sourceProductDescriptor.isOptional());

        sourceProductDescriptor.optional = true;
        assertTrue(sourceProductDescriptor.isOptional());
    }
}
