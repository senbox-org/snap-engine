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

package com.bc.ceres.swing.selection.support;

import org.junit.Test;

import java.awt.datatransfer.DataFlavor;

import static org.junit.Assert.*;

public class DefaultSelectionTest {

    @Test
    public void testEmpty() {
        DefaultSelection selection = new DefaultSelection();

        assertTrue(selection.isEmpty());
        assertNull(selection.getSelectedValue());
        assertNotNull(selection.getSelectedValues());
        assertEquals(0, selection.getSelectedValues().length);
        assertEquals("", selection.getPresentationName());

        assertNull(selection.createTransferable(false));
        assertNull(selection.createTransferable(true));

        assertEquals("DefaultSelection[selectedValues={}]", selection.toString());

        assertNotEquals(null, selection);
        assertEquals(selection, selection);
        assertEquals(selection, new DefaultSelection());
        assertNotEquals(selection, new DefaultSelection("B"));
    }

    @Test
    public void testOneElement() {
        DefaultSelection selection = new DefaultSelection("X");

        assertFalse(selection.isEmpty());
        assertEquals("X", selection.getSelectedValue());
        assertNotNull(selection.getSelectedValues());
        assertEquals(1, selection.getSelectedValues().length);
        assertEquals("X", selection.getSelectedValues()[0]);
        assertEquals("X", selection.getPresentationName());

        assertNotNull(selection.createTransferable(false));
        assertNotNull(selection.createTransferable(true));
        assertTrue(selection.createTransferable(true).isDataFlavorSupported(DataFlavor.stringFlavor));

        assertEquals("DefaultSelection[selectedValues={X}]", selection.toString());

        assertNotEquals(null, selection);
        assertEquals(selection, selection);
        assertEquals(selection, new DefaultSelection("X"));
        assertNotEquals(selection, new DefaultSelection("B"));
    }

    @Test
    public void testMoreElements() {
        DefaultSelection selection = new DefaultSelection("A", "B", "C");

        assertFalse(selection.isEmpty());
        assertEquals("A", selection.getSelectedValue());
        assertNotNull(selection.getSelectedValues());
        assertEquals(3, selection.getSelectedValues().length);
        assertEquals("A", selection.getSelectedValues()[0]);
        assertEquals("B", selection.getSelectedValues()[1]);
        assertEquals("C", selection.getSelectedValues()[2]);
        assertEquals("A", selection.getPresentationName());

        assertNotNull(selection.createTransferable(false));
        assertNotNull(selection.createTransferable(true));
        assertTrue(selection.createTransferable(true).isDataFlavorSupported(DataFlavor.stringFlavor));

        assertEquals("DefaultSelection[selectedValues={A,B,C}]", selection.toString());

        assertNotEquals(null, selection);
        assertEquals(selection, selection);
        assertEquals(selection, new DefaultSelection("A", "B", "C"));
        assertNotEquals(selection, new DefaultSelection("B", "A", "C"));
        assertNotEquals(selection, new DefaultSelection("B"));
    }
}