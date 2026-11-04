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

package com.bc.ceres.binding.dom;

import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultDomElementTest extends AbstractDomElementTest {

    @Override
    protected DomElement createDomElement(String name) {
        return new DefaultDomElement(name);
    }

    @Override
    protected DomElement createDomElement(String name, String value) {
        return new DefaultDomElement(name, value);
    }

    @Test
    public void testConstructor() {
        try {
            new DefaultDomElement(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new DefaultDomElement(null, "red");
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new DefaultDomElement("color", null);
            // ok
        } catch (NullPointerException e) {
            fail("NPE not expected");
        }
    }

    @Test
    public void testMixChildren() {
        testMixChildren(new DefaultDomElement("a"), new DefaultDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new DefaultDomElement("a"), new DefaultDomElement("b"), new XppDomElement("c"));
        testMixChildren(new DefaultDomElement("a"), new XppDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new DefaultDomElement("a"), new XppDomElement("b"), new XppDomElement("c"));
        testMixChildren(new XppDomElement("a"), new DefaultDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new XppDomElement("a"), new DefaultDomElement("b"), new XppDomElement("c"));
        testMixChildren(new XppDomElement("a"), new XppDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new XppDomElement("a"), new XppDomElement("b"), new XppDomElement("c"));
    }

    @Test
    public void testGetChildren() {
        final DefaultDomElement parent = new DefaultDomElement("parent");
        final DefaultDomElement child1 = new DefaultDomElement("child_1");
        parent.addChild(child1);

        final DefaultDomElement child2 = new DefaultDomElement("child_2");
        parent.addChild(child2);

        final DomElement[] children = parent.getChildren();
        assertEquals(2, children.length);
        assertEquals("child_1", children[0].getName());
    }

    @Test
    public void testGetChildren_empty() {
        final DefaultDomElement parent = new DefaultDomElement("parent");

        final DomElement[] children = parent.getChildren();
        assertEquals(0, children.length);
    }

    @Test
    public void testGetChildren_named() {
        final DefaultDomElement parent = new DefaultDomElement("parent");
        final DefaultDomElement child1 = new DefaultDomElement("child");
        parent.addChild(child1);

        final DefaultDomElement child2 = new DefaultDomElement("child");
        parent.addChild(child2);

        DomElement[] children = parent.getChildren("child");
        assertEquals(2, children.length);
        assertEquals("child", children[1].getName());

        children = parent.getChildren("opa");
        assertEquals(0, children.length);
    }

    @Test
    public void testGetChildren_named_empty() {
        final DefaultDomElement parent = new DefaultDomElement("parent");

        final DomElement[] children = parent.getChildren("child");
        assertEquals(0, children.length);
    }

    @Test
    public void testGetChild_index_empty() {
        final DefaultDomElement parent = new DefaultDomElement("parent");

        assertNull(parent.getChild(8));
    }

    @Test
    public void testGetChild_index_outOfRange() {
        final DefaultDomElement parent = new DefaultDomElement("parent");
        final DefaultDomElement child1 = new DefaultDomElement("child_1");
        parent.addChild(child1);

        assertNull(parent.getChild(2));
    }

    @Test
    public void testGetChild_index() {
        final DefaultDomElement parent = new DefaultDomElement("parent");
        final DefaultDomElement child1 = new DefaultDomElement("child_1");
        parent.addChild(child1);

        final DefaultDomElement child2 = new DefaultDomElement("child_2");
        parent.addChild(child2);

        DomElement child = parent.getChild(0);
        assertEquals("child_1", child.getName());

        child = parent.getChild(1);
        assertEquals("child_2", child.getName());
    }

    private void testMixChildren(DomElement a, DomElement b, DomElement c) {
        b.addChild(c);
        a.addChild(b);

        assertNotNull(a.getChild("b"));
        assertNotNull(a.getChild("b").getChild("c"));

        assertNull(a.getParent());
        assertSame(a, a.getChild("b").getParent());
        assertSame(a.getChild("b"), a.getChild("b").getChild("c").getParent());

        assertEquals("""
                <a>
                <b>
                <c/>
                </b>
                </a>""", a.toXml().replace("  ", ""));
    }
}
