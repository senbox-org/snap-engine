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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class ProductEventTest {

    /**
     * Tests the functionality for the constructor
     */
    @Test
    public void testRsProductEvent() {
        try {
            new ProductNodeEvent(null, 0);
            fail("ProductNodeEvent construction not allowed with null argument");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the functionality of getNamedNode.
     */
    @Test
    public void testGetNamedNode() {
        ProductNodeEvent event;
        MetadataElement testNode;

        testNode = new MetadataElement("test_me");
        event = new ProductNodeEvent(testNode, 0);
        assertSame(testNode, event.getSourceNode());
    }
}
