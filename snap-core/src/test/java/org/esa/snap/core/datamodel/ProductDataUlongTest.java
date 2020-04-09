package org.esa.snap.core.datamodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProductDataUlongTest {

    @Test
    public void testConstants() {
        assertEquals(23, ProductData.TYPE_UINT64);
        assertEquals("uint64", ProductData.TYPESTRING_UINT64);
    }

    @Test
    public void testCreateInstance() {
        try {
            ProductData.createInstance(ProductData.TYPE_UINT64);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ProductData.createInstance(ProductData.TYPE_UINT64, 17);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetElemSize() {
        assertEquals(8, ProductData.getElemSize(ProductData.TYPE_UINT64));
    }

    @Test
    public void testGetTypeString() {
        assertEquals("uint64", ProductData.getTypeString(ProductData.TYPE_UINT64));
    }

    @Test
    public void testGetType() {
        assertEquals(ProductData.TYPE_UINT64, ProductData.getType("uint64"));
    }

    @Test
    public void testIsIntType() {
        assertTrue(ProductData.isIntType(ProductData.TYPE_UINT64));
    }

    @Test
    public void testIsUIntType() {
        assertTrue(ProductData.isUIntType(ProductData.TYPE_UINT64));
    }

    @Test
    public void testIsFloatingPointType() {
        assertFalse(ProductData.isFloatingPointType(ProductData.TYPE_UINT64));
    }
}
