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

package com.bc.ceres.swing.selection;

import com.bc.ceres.swing.selection.support.DefaultSelection;
import org.junit.Test;

import static org.junit.Assert.*;

public class SelectionTest {

    @Test
    public void testEmpty() {
        assertNotNull(Selection.EMPTY);
        assertNull(Selection.EMPTY.getSelectedValue());
        assertNotNull(Selection.EMPTY.getSelectedValues());
        assertEquals(0, Selection.EMPTY.getSelectedValues().length);
        assertTrue(Selection.EMPTY.isEmpty());

        assertEquals("", Selection.EMPTY.getPresentationName());
        assertEquals("Selection.EMPTY", Selection.EMPTY.toString());

        assertNull(Selection.EMPTY.createTransferable(false));
        assertNull(Selection.EMPTY.createTransferable(true));

        assertNotEquals(null, Selection.EMPTY);
        assertNotEquals("A", Selection.EMPTY);
        assertNotEquals(Selection.EMPTY, new DefaultSelection("A"));
        assertEquals(Selection.EMPTY, Selection.EMPTY);
    }
}
