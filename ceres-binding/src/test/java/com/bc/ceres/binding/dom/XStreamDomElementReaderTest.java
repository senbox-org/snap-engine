/*
 * Copyright (C) 2026 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.binding.dom;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class XStreamDomElementReaderTest {

    private DefaultDomElement domElement;
    private XStreamDomElementReader elementReader;

    @Before
    public void setUp() {
        domElement = new DefaultDomElement("who_are_you");
        elementReader = new XStreamDomElementReader(domElement);
    }

    @Test
    public void testGetNodeName() {
        assertEquals("who_are_you", elementReader.getNodeName());
    }

    @Test
    public void testGetValue() {
        domElement.setValue("The King");

        assertEquals("The King", elementReader.getValue());
    }

    @Test
    public void testGetAttribute_byName() {
        domElement.setAttribute("name", "theAttrib");

        assertEquals("theAttrib", elementReader.getAttribute("name"));
        assertNull(domElement.getAttribute("wrong_name"));
    }

    @Test
    public void testGetAttribute_byIndex() {
        domElement.setAttribute("name", "theAttrib");
        domElement.setAttribute("size", "quite_large");

        // sets the attribute name vector tb 2026-10-12
        elementReader.reassignCurrentElement(domElement);

        assertEquals("theAttrib", elementReader.getAttribute(0));
        assertEquals("quite_large", elementReader.getAttribute(1));

        assertNull(elementReader.getAttribute(2));
    }

    @Test
    public void testGetAttributeCount() {
        domElement.setAttribute("name", "theAttrib");
        domElement.setAttribute("size", "quite_large");

        // sets the attribute name vector tb 2026-10-12
        elementReader.reassignCurrentElement(domElement);

        assertEquals(2, elementReader.getAttributeCount());
    }

    @Test
    public void testGetAttributeCount_empty() {
        assertEquals(0, elementReader.getAttributeCount());
    }

    @Test
    public void testGetAttributeName() {
        domElement.setAttribute("name", "theAttrib");
        domElement.setAttribute("size", "quite_large");

        // sets the attribute name vector tb 2026-10-12
        elementReader.reassignCurrentElement(domElement);

        assertEquals("name", elementReader.getAttributeName(0));
        assertEquals("size", elementReader.getAttributeName(1));

        assertNull(elementReader.getAttributeName(2));
    }
}
