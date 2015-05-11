package org.esa.beam.dataio;

import junit.framework.TestCase;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.h5.H5Datatype;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author olafd
 */
public class ProbaVUtilsTest extends TestCase {

    public void testGetAttributeValue()  {
        final H5Datatype intDatatype = new H5Datatype(H5Datatype.CLASS_INTEGER, H5Datatype.NATIVE, H5Datatype.NATIVE, -1);
        final Attribute intAttr = new Attribute("intAttr", intDatatype, new long[]{1L});
        intAttr.setValue(new int[]{2});
        final String intAttrValue = ProbaVUtils.getAttributeValue(intAttr);
        assertEquals("2", intAttrValue);

        final H5Datatype floatDatatype = new H5Datatype(H5Datatype.CLASS_FLOAT, H5Datatype.NATIVE, H5Datatype.NATIVE, -1);
        final Attribute floatAttr = new Attribute("floatAttr", floatDatatype, new long[]{1L});
        floatAttr.setValue(new float[]{4.5f, 2.3f});
        final String floatAttrValue = ProbaVUtils.getAttributeValue(floatAttr);
        assertEquals("4.5 2.3", floatAttrValue);

        final H5Datatype stringDatatype = new H5Datatype(H5Datatype.CLASS_STRING, H5Datatype.NATIVE, H5Datatype.NATIVE, -1);
        final Attribute stringAttr = new Attribute("stringAttr", stringDatatype, new long[]{1L});
        stringAttr.setValue(new String[]{"bla", "blubb", "lalala"});
        final String stringAttrValue = ProbaVUtils.getAttributeValue(stringAttr);
        assertEquals("bla blubb lalala", stringAttrValue);
    }

    public void testGetStringAttributes() {
        List<Attribute> attrList = new ArrayList<>();
        final H5Datatype stringDatatype = new H5Datatype(H5Datatype.CLASS_STRING, H5Datatype.NATIVE, H5Datatype.NATIVE, -1);
        final Attribute dummyAttr = new Attribute("dummy", stringDatatype, new long[]{1L});
        attrList.add(dummyAttr);
        assertNull(ProbaVUtils.getStringAttributeValue(attrList, "bla"));

        final Attribute descriptionAttr = new Attribute("DESCRIPTION", stringDatatype, new long[]{1L});
        descriptionAttr.setValue(new String[]{"This is a description"});
        attrList.add(descriptionAttr);
        final String descriptionFromAttributes = ProbaVUtils.getStringAttributeValue(attrList, "DESCRIPTION");
        assertEquals("This is a description", descriptionFromAttributes);

        final Attribute unitsAttr = new Attribute("UNITS", stringDatatype, new long[]{1L});
        unitsAttr.setValue(new String[]{"pounds per square inch"});
        attrList.add(unitsAttr);
        final String unitsFromAttributes = ProbaVUtils.getStringAttributeValue(attrList, "UNITS");
        assertEquals("pounds per square inch", unitsFromAttributes);

    }

    public void testGetFloatAttributes() {
        List<Attribute> attrList = new ArrayList<>();
        final H5Datatype floatDatatype = new H5Datatype(H5Datatype.CLASS_FLOAT, H5Datatype.NATIVE, H5Datatype.NATIVE, -1);
        final Attribute dummyAttr = new Attribute("dummy", floatDatatype, new long[]{1L});
        attrList.add(dummyAttr);
        assertTrue(Float.isNaN(ProbaVUtils.getFloatAttributeValue(attrList, "blubb")));

        final Attribute unitsAttr = new Attribute("NO_DATA", floatDatatype, new long[]{1L});
        unitsAttr.setValue(new float[]{Float.NaN});
        attrList.add(unitsAttr);
        final float noDataFromAttributes = ProbaVUtils.getFloatAttributeValue(attrList, "NO_DATA");
        assertTrue(Float.isNaN(noDataFromAttributes));

        final Attribute scaleAttr = new Attribute("SCALE", floatDatatype, new long[]{1L});
        scaleAttr.setValue(new float[]{250.0f});
        attrList.add(scaleAttr);
        final float scaleFromAttributes = ProbaVUtils.getFloatAttributeValue(attrList, "SCALE");
        assertEquals(250.0f, scaleFromAttributes);

        final Attribute topLeftLatAttr = new Attribute("TOP_LEFT_LONGITUDE", floatDatatype, new long[]{1L});
        topLeftLatAttr.setValue(new float[]{87.3f});
        attrList.add(topLeftLatAttr);
        final float topLeftLatFromAttributes = ProbaVUtils.getFloatAttributeValue(attrList, "TOP_LEFT_LONGITUDE");
        assertEquals(87.3f, topLeftLatFromAttributes);
    }
}
