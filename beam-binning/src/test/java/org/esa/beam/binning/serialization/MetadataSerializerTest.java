package org.esa.beam.binning.serialization;


import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MetadataSerializerTest {

    private MetadataSerializer serializer;

    @Before
    public void setUp() {
        serializer = new MetadataSerializer();
    }

    @Test
    public void testToXml_Null() {
        final String xml = serializer.toXml(null);
        assertEquals("", xml);
    }

    @Test
    public void testFromXml_Null() {
        final MetadataElement metadataElement = serializer.fromXml(null);
        assertNull(metadataElement);
    }

    @Test
    public void testFromXml_EmptyString() {
        final MetadataElement metadataElement = serializer.fromXml("");
        assertNull(metadataElement);
    }

    @Test
    public void testSerializeSimpleElement() {
        final MetadataElement element = new MetadataElement("just_one");

        final String xml = serializer.toXml(element);
        final MetadataElement serialized = serializer.fromXml(xml);
        assertNotNull(serialized);

        assertEquals(element.getName(), serialized.getName());
    }

    @Test
    public void testSerializeSimpleElement_withAttribute() {
        final MetadataElement element = new MetadataElement("with_attribute");
        element.addAttribute(new MetadataAttribute("attribute", ProductData.createInstance(new double[]{9.887}), true));


        final String xml = serializer.toXml(element);
        final MetadataElement serialized = serializer.fromXml(xml);
        assertNotNull(serialized);

        assertEquals(element.getName(), serialized.getName());
        assertEquals(1, serialized.getNumAttributes());

        final MetadataAttribute attribute = serialized.getAttribute("attribute");
        assertNotNull(attribute);
        assertEquals(9.887, attribute.getData().getElemDouble(), 1e-8);
    }

    @Test
    public void testSerializeNestedElements() {
        final MetadataElement root = new MetadataElement("root");
        root.addElement(new MetadataElement("nested_1"));
        final MetadataElement nested_2 = new MetadataElement("nested_2");
        nested_2.addElement(new MetadataElement("nested_nested"));
        root.addElement(nested_2);

        final String xml = serializer.toXml(root);
        final MetadataElement serialized = serializer.fromXml(xml);
        assertNotNull(serialized);

        assertEquals(root.getName(), serialized.getName());
        assertEquals(2, serialized.getNumElements());

        assertNotNull(serialized.getElement("nested_1"));

        final MetadataElement serializedNested = serialized.getElement("nested_2");
        assertNotNull(serializedNested);
        assertNotNull(serializedNested.getElement("nested_nested"));
    }

    @Test
    public void testSerializeNestedElements_withAttributes() {
        final MetadataElement root = new MetadataElement("root");
        root.addAttribute(new MetadataAttribute("root_attrib", ProductData.createInstance(new int[]{14}), true));

        final MetadataElement nested_1 = new MetadataElement("nested_1");
        nested_1.addAttribute(new MetadataAttribute("nested_1_attrib", ProductData.createInstance("A_string_value"), false));
        root.addElement(nested_1);

        final MetadataElement nested_2 = new MetadataElement("nested_2");
        nested_2.addAttribute(new MetadataAttribute("nested_2_attrib", ProductData.createInstance(new float[]{1.866f}), false));
        final MetadataElement nested_nested = new MetadataElement("nested_nested");
        nested_nested.addAttribute(new MetadataAttribute("nested_nested_attrib", ProductData.UTC.create(new Date(6625152000l), 0), true));
        nested_2.addElement(nested_nested);
        root.addElement(nested_2);

        final String xml = serializer.toXml(root);
        final MetadataElement serialized = serializer.fromXml(xml);
        assertNotNull(serialized);

        assertEquals(root.getName(), serialized.getName());
        assertEquals(14, serialized.getAttributeInt("root_attrib"));
        assertEquals(2, serialized.getNumElements());

        final MetadataElement serialized_n1 = serialized.getElement("nested_1");
        assertNotNull(serialized_n1);
        assertEquals("A_string_value", serialized_n1.getAttributeString("nested_1_attrib"));

        final MetadataElement serialized_n2 = serialized.getElement("nested_2");
        assertNotNull(serialized_n2);
        assertEquals(1.866f, serialized_n2.getAttributeDouble("nested_2_attrib"), 1e-8);

        final MetadataElement serialized_nn = serialized_n2.getElement("nested_nested");
        assertNotNull(serialized_nn);
        final MetadataAttribute nested_nested_attrib = serialized_nn.getAttribute("nested_nested_attrib");
        final ProductData.UTC data = (ProductData.UTC) nested_nested_attrib.getData();
        assertEquals(6625152000L, data.getAsDate().getTime());
    }
}
