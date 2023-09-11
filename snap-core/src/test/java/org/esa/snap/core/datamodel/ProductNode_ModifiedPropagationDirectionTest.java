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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProductNode_ModifiedPropagationDirectionTest {

    @Test
    public void testFireNodeModified_SequenceDirection() {
        final Product product = new Product("n", "t", 5, 5);
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement elem1 = new MetadataElement("elem1");
        final MetadataElement elem2 = new MetadataElement("elem2");
        final MetadataElement elem3 = new MetadataElement("elem3");
        final MetadataAttribute attrib = new MetadataAttribute("attrib",
                ProductData.createInstance(new int[]{2, 3}),
                false);

        assertFalse(product.isModified());
        assertFalse(root.isModified());
        assertFalse(elem1.isModified());
        assertFalse(elem2.isModified());
        assertFalse(elem3.isModified());
        assertFalse(attrib.isModified());

        elem3.addAttribute(attrib);
        elem2.addElement(elem3);
        elem1.addElement(elem2);
        root.addElement(elem1);

        assertTrue(product.isModified());
        assertTrue(root.isModified());
        assertTrue(elem1.isModified());
        assertTrue(elem2.isModified());
        assertTrue(elem3.isModified());
        assertFalse(attrib.isModified());

        attrib.setModified(true);

        assertTrue(product.isModified());
        assertTrue(root.isModified());
        assertTrue(elem1.isModified());
        assertTrue(elem2.isModified());
        assertTrue(elem3.isModified());
        assertTrue(attrib.isModified());

        product.setModified(false);

        assertFalse(product.isModified());
        assertFalse(root.isModified());
        assertFalse(elem1.isModified());
        assertFalse(elem2.isModified());
        assertFalse(elem3.isModified());
        assertFalse(attrib.isModified());

        elem2.setModified(true);

        assertTrue(product.isModified());
        assertTrue(root.isModified());
        assertTrue(elem1.isModified());
        assertTrue(elem2.isModified());
        assertFalse(elem3.isModified());
        assertFalse(attrib.isModified());

        root.setModified(false);

        assertTrue(product.isModified());
        assertFalse(root.isModified());
        assertFalse(elem1.isModified());
        assertFalse(elem2.isModified());
        assertFalse(elem3.isModified());
        assertFalse(attrib.isModified());
    }
}
