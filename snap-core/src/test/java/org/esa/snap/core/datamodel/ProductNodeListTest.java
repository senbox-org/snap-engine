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

import static org.junit.Assert.*;

public class ProductNodeListTest {

    private ProductNodeList<MetadataAttribute> _nodeList;
    private MetadataAttribute _attribute1;
    private MetadataAttribute _attribute2;
    private MetadataAttribute _attribute3;
    private MetadataAttribute _attribute4;


    @Before
    public void setUp() {
        _nodeList = new ProductNodeList<MetadataAttribute>();
        _attribute1 = new MetadataAttribute("attribute1", ProductData.createInstance(ProductData.TYPE_INT32), true);
        _attribute2 = new MetadataAttribute("attribute2", ProductData.createInstance(ProductData.TYPE_INT32), true);
        _attribute3 = new MetadataAttribute("attribute3", ProductData.createInstance(ProductData.TYPE_INT32), true);
        _attribute4 = new MetadataAttribute("attribute4", ProductData.createInstance(ProductData.TYPE_INT32), true);
    }

    @Test
    public void testGetAt() {
        addAllNodes();

        assertEquals(_nodeList.getAt(0), _attribute1);
        assertEquals(_nodeList.getAt(1), _attribute2);
        assertEquals(_nodeList.getAt(2), _attribute3);
        assertEquals(_nodeList.getAt(3), _attribute4);

        try {
            _nodeList.getAt(-1);
            fail("IndexOutOfBoundsException expected");
        } catch (java.lang.IndexOutOfBoundsException e) {
        }

        try {
            _nodeList.getAt(4);
            fail("IndexOutOfBoundsException expected");
        } catch (java.lang.IndexOutOfBoundsException e) {
        }

        removeAllNodes();
    }

    @Test
    public void testGet() {
        addAllNodes();
        assertEquals(_nodeList.get("attribute1"), _attribute1);
        assertEquals(_nodeList.get("attribute2"), _attribute2);
        assertEquals(_nodeList.get("attribute3"), _attribute3);
        assertEquals(_nodeList.get("attribute4"), _attribute4);
        assertEquals(_nodeList.get("ATTRIBUTE1"), _attribute1);
        assertEquals(_nodeList.get("ATTRIBUTE2"), _attribute2);
        assertEquals(_nodeList.get("ATTRIBUTE3"), _attribute3);
        assertEquals(_nodeList.get("ATTRIBUTE4"), _attribute4);
        assertNull(_nodeList.get("ATTRIBUTEX"));
        removeAllNodes();
    }

    @Test
    public void testContains() {
        addAllNodes();
        assertTrue(_nodeList.contains("attribute1"));
        assertTrue(_nodeList.contains("attribute2"));
        assertTrue(_nodeList.contains("attribute3"));
        assertTrue(_nodeList.contains("attribute4"));
        assertTrue(_nodeList.contains("ATTRIBUTE1"));
        assertTrue(_nodeList.contains("ATTRIBUTE2"));
        assertTrue(_nodeList.contains("ATTRIBUTE3"));
        assertTrue(_nodeList.contains("ATTRIBUTE4"));
        assertFalse(_nodeList.contains("ATTRIBUTEX"));
        removeAllNodes();
    }

    @Test
    public void testGetNames() {
        addAllNodes();
        String[] names = _nodeList.getNames();
        assertEquals(names[0], "attribute1");
        assertEquals(names[1], "attribute2");
        assertEquals(names[2], "attribute3");
        assertEquals(names[3], "attribute4");
        removeAllNodes();
    }

    @Test
    public void testToArray() {
        addAllNodes();
        ProductNode[] nodes = _nodeList.toArray();
        assertEquals(nodes[0], _attribute1);
        assertEquals(nodes[1], _attribute2);
        assertEquals(nodes[2], _attribute3);
        assertEquals(nodes[3], _attribute4);
        removeAllNodes();
    }

    @Test
    public void testIndexOf() {
        addAllNodes();
        assertEquals(_nodeList.indexOf("attribute1"), 0);
        assertEquals(_nodeList.indexOf("attribute2"), 1);
        assertEquals(_nodeList.indexOf("attribute3"), 2);
        assertEquals(_nodeList.indexOf("attribute4"), 3);
        assertEquals(_nodeList.indexOf("ATTRIBUTE1"), 0);
        assertEquals(_nodeList.indexOf("ATTRIBUTE2"), 1);
        assertEquals(_nodeList.indexOf("ATTRIBUTE3"), 2);
        assertEquals(_nodeList.indexOf("ATTRIBUTE4"), 3);
        assertEquals(_nodeList.indexOf("ATTRIBUTEX"), -1);
        removeAllNodes();
    }

    @Test
    public void testAddAndRemoveAndSize() {
        assertEquals(_nodeList.size(), 0);
        _nodeList.add(_attribute1);
        assertEquals(_nodeList.size(), 1);
        _nodeList.add(_attribute2);
        assertEquals(_nodeList.size(), 2);
        _nodeList.add(_attribute3);
        assertEquals(_nodeList.size(), 3);
        _nodeList.add(_attribute4);
        assertEquals(_nodeList.size(), 4);
        _nodeList.remove(_attribute1);
        assertEquals(_nodeList.size(), 3);
        _nodeList.removeAll();
        assertEquals(_nodeList.size(), 0);
    }

    private void addAllNodes() {
        _nodeList.add(_attribute1);
        _nodeList.add(_attribute2);
        _nodeList.add(_attribute3);
        _nodeList.add(_attribute4);
    }

    private void removeAllNodes() {
        _nodeList.removeAll();
    }
}
