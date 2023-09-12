/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.datamodel;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;


public class MetadataElementTest {

    private MetadataElement _testGroup;
    private Object _addedNode;
    private Object _nodeDataChanged;
    private ArrayList _propertyNameList;
    private ArrayList _propertySourceList;

    /**
     * Initialize the tests
     */
    @Before
    public void setUp() {
        _testGroup = new MetadataElement("test");
    }

    /**
     * Tests construction failures
     */
    @Test
    public void testRsAnnotation() {
        try {
            new MetadataElement(null);
            fail("construction with null argument not allowed");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * GuiTest_DialogAndModalDialog the acceptVisitor functionality
     */
    @Test
    public void testAcceptVisitor() {
        LinkedListProductVisitor visitor = new LinkedListProductVisitor();
        List expectedList = new LinkedList();
        assertEquals(expectedList, visitor.getVisitedList());

        _testGroup.acceptVisitor(visitor);
        expectedList.add("test");
        assertEquals(expectedList, visitor.getVisitedList());

        try {
            _testGroup.acceptVisitor(null);
            fail("Null argument for visitor not allowed");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the functionality for addAttribute
     */
    @Test
    public void testAddAttribute() {
        MetadataElement annot = new MetadataElement("test_me");
        MetadataAttribute att;

        // allow null argument
        try {
            annot.addAttribute(null);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException null argument is allowed");
        }

        // add an attribute
        att = new MetadataAttribute("Test1", ProductData.createInstance(ProductData.TYPE_INT32), false);
        annot.addAttribute(att);
        assertEquals(1, annot.getNumAttributes());
        assertEquals(att, annot.getAttributeAt(0));
    }

    /**
     * Tests the functionality for containsAttribute()
     */
    @Test
    public void testContainsAttribute() {
        MetadataElement annot = new MetadataElement("test_me");
        MetadataAttribute att = new MetadataAttribute("Test1", ProductData.createInstance(ProductData.TYPE_INT32),
                false);

        // should not contain anything now
        assertFalse(annot.containsAttribute("Test1"));

        // add attribute an check again
        annot.addAttribute(att);
        assertTrue(annot.containsAttribute("Test1"));

        // tis one should not be there
        assertFalse(annot.containsAttribute("NotMe"));
    }

    /**
     * Tests the functionality for getPropertyValue()
     */
    @Test
    public void testGetAttribute() {
        MetadataElement annot = new MetadataElement("yepp");
        MetadataAttribute att;

        // a new object should not return anything on this request
        try {
            annot.getAttributeAt(0);
            fail("there are no elements in the list");
        } catch (IndexOutOfBoundsException e) {
        }

        att = new MetadataAttribute("GuiTest_DialogAndModalDialog", ProductData.createInstance(ProductData.TYPE_INT32),
                false);
        annot.addAttribute(att);
        assertEquals(att, annot.getAttributeAt(0));
    }

    /**
     * Tests the functionality for getAttributeNames
     */
    @Test
    public void testGetAttributeNames() {
        MetadataElement annot = new MetadataElement("yepp");
        MetadataAttribute att = new MetadataAttribute("GuiTest_DialogAndModalDialog",
                ProductData.createInstance(ProductData.TYPE_INT32), false);

        // initially no strings should be returned
        assertEquals(0, annot.getAttributeNames().length);

        // now add one attribute and check again
        annot.addAttribute(att);
        assertEquals(1, annot.getAttributeNames().length);
        assertEquals("GuiTest_DialogAndModalDialog", annot.getAttributeNames()[0]);
    }

    /**
     * GuiTest_DialogAndModalDialog the functionality for getNumAttributes()
     */
    @Test
    public void testGetNumAttributes() {
        MetadataElement annot = new MetadataElement("yepp");
        MetadataAttribute att = new MetadataAttribute("GuiTest_DialogAndModalDialog",
                ProductData.createInstance(ProductData.TYPE_INT32), false);

        // a new object should not have any attributes
        assertEquals(0, annot.getNumAttributes());

        // add one and test again
        annot.addAttribute(att);
        assertEquals(1, annot.getNumAttributes());
    }

    /**
     * Tests the functionality for removeAttribute()
     */
    @Test
    public void testRemoveAttribute() {
        MetadataElement annot = new MetadataElement("yepp");
        MetadataAttribute att = new MetadataAttribute("GuiTest_DialogAndModalDialog",
                ProductData.createInstance(ProductData.TYPE_INT32), false);
        MetadataAttribute att2 = new MetadataAttribute("GuiTest_DialogAndModalDialog",
                ProductData.createInstance(ProductData.TYPE_INT32), false);

        // add one, check, remove again, check again
        annot.addAttribute(att);
        assertEquals(1, annot.getNumAttributes());
        annot.removeAttribute(att);
        assertEquals(0, annot.getNumAttributes());

        // try to add existent attribute name
        annot.addAttribute(att);
        assertEquals(1, annot.getNumAttributes());
        annot.addAttribute(att2);
        assertEquals(2, annot.getNumAttributes());

        // try to remove non existent attribute
        att2 = new MetadataAttribute("DifferentName", ProductData.createInstance(ProductData.TYPE_INT32), false);
        annot.removeAttribute(att2);
        assertEquals(2, annot.getNumAttributes());
    }

    @Test
    public void testSetAttributeInt() {
        final Product product = new Product("n", "t", 5, 5);
        final MetadataElement elem = new MetadataElement("test");
        product.getMetadataRoot().addElement(elem);
        _propertyNameList = new ArrayList();
        _propertySourceList = new ArrayList();
        product.addProductNodeListener(new ProductNodeListenerAdapter() {
            @Override
            public void nodeAdded(ProductNodeEvent event) {
                _addedNode = event.getSource();
            }

            @Override
            public void nodeDataChanged(ProductNodeEvent event) {
                _nodeDataChanged = event.getSource();
            }

            @Override
            public void nodeChanged(ProductNodeEvent event) {
                _propertyNameList.add(event.getPropertyName());
                _propertySourceList.add(event.getSource());
            }
        });

        product.setModified(false);
        assertEquals(0, elem.getNumAttributes());
        assertNull(_addedNode);
        assertNull(_nodeDataChanged);
        _propertyNameList.clear();
        _propertySourceList.clear();

        elem.setAttributeInt("counter", 3);

        assertEquals(1, elem.getNumAttributes());
        final MetadataAttribute attrib = elem.getAttributeAt(0);
        assertNotNull(attrib);
        assertEquals("counter", attrib.getName());
        assertEquals(3, attrib.getData().getElemInt());

        assertNotNull(_addedNode);
        assertNotNull(_nodeDataChanged);
        assertSame(_addedNode, attrib);
        assertSame(_nodeDataChanged, attrib);
        assertEquals(0, _propertyNameList.size());
        assertEquals(0, _propertySourceList.size());

        product.setModified(false);
        _addedNode = null;
        _nodeDataChanged = null;
        _propertyNameList.clear();
        assertFalse(elem.isModified());

        elem.setAttributeInt("counter", -3);

        assertNull(_addedNode);
        assertNotNull(_nodeDataChanged);
        assertSame(_nodeDataChanged, attrib);
        assertEquals(0, _propertyNameList.size());

        assertEquals(1, elem.getNumAttributes());
        assertSame(attrib, elem.getAttributeAt(0));
        assertEquals("counter", attrib.getName());
        assertEquals(-3, attrib.getData().getElemInt());
    }
}
