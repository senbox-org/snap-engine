package org.esa.s3tbx.dataio.modis.attribute;

import org.esa.s3tbx.dataio.modis.ModisGlobalAttributes;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ImappAttributesTest {

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInheritance() {
        final ImappAttributes imappAttributes = new ImappAttributes(new File("."), null, null);

        assertTrue(imappAttributes instanceof ModisGlobalAttributes);
    }

    @Test
    public void testGetProductType() {
        ImappAttributes imappAttributes = new ImappAttributes(new File("MOD021KM.A2006038.0722.hdf"), null, null);
        assertEquals("MOD021KM", imappAttributes.getProductType());

        imappAttributes = new ImappAttributes(new File("ATS_NR__2PNMAP20060918_095303_000000532051_00193_23794_0001.N1"), null, null);
        assertEquals("unknown", imappAttributes.getProductType());
    }

    @Test
    public void testGetProductName() {
        ImappAttributes imappAttributes = new ImappAttributes(new File("MOD021KM.A2006038.0722.hdf"), null, null);
        assertEquals("MOD021KM.A2006038.0722", imappAttributes.getProductName());

        imappAttributes = new ImappAttributes(new File("MOD021KM.A20050930110428.20050930111128_v1.5.hdf"), null, null);
        assertEquals("MOD021KM.A20050930110428.20050930111128_v1.5", imappAttributes.getProductName());
    }

    @Test
    public void testIsImappFormat() {
        final ImappAttributes imappAttributes = new ImappAttributes(new File("."), null, null);
        assertTrue(imappAttributes.isImappFormat());
    }

    @Test
    public void testGetEosType() {
        final ImappAttributes imappAttributes = new ImappAttributes(new File("."), null, null);
        assertNull(imappAttributes.getEosType());
    }

    @Test
    public void testCreateGeoCoding() {
        final ImappAttributes imappAttributes = new ImappAttributes(new File("."), null, null);
        assertNull(imappAttributes.createGeocoding());
    }
}
