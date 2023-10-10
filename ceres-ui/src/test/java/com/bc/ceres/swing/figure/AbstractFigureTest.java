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

package com.bc.ceres.swing.figure;

import org.junit.Test;

import java.awt.geom.AffineTransform;

import static org.junit.Assert.*;

public class AbstractFigureTest {

    @Test
    public void testDefaultProperties() {
        Figure f = new AbstractFigureImpl();
        assertNotNull(f.getChangeListeners());
        assertFalse(f.isSelectable());
        assertEquals(0, f.getChangeListeners().length);
        assertNull(f.getFigure(null, new AffineTransform()));
        assertEquals(0, f.getFigureCount());
        assertEquals(0, f.getMaxSelectionStage());
        assertNull(f.getFigure(null, new AffineTransform()));
        assertNotNull(f.getFigures(null));
        assertEquals(0, f.getFigures(null).length);
        assertNotNull(f.getFigures());
        assertEquals(0, f.getFigures().length);
        assertEquals(0, f.getMaxSelectionStage());
        assertNotNull(f.createHandles(1));
        assertEquals(0, f.createHandles(1).length);
    }

    @Test
    public void testThatCloneDoesNotCopyListeners() {
        Figure f = new AbstractFigureImpl();
        f.addChangeListener(event -> {
        });
        AbstractFigure cf = (AbstractFigure) f.clone();
        assertNotNull(cf.getChangeListeners());
        assertEquals(0, cf.getChangeListeners().length);
    }

    @Test
    public void testListeners() {
        AbstractFigureImpl f = new AbstractFigureImpl();
        final Figure[] figureBuf = new Figure[1];
        f.addChangeListener(event -> figureBuf[0] = event.getSourceFigure());
        assertNull(figureBuf[0]);
        f.postChangeEvent();
        assertEquals(f, figureBuf[0]);
    }

    @Test
    public void testDisposeRemovesListeners() {
        Figure f = new AbstractFigureImpl();
        f.addChangeListener(event -> {
        });
        FigureChangeListener[] listeners = f.getChangeListeners();
        assertNotNull(listeners);
        assertTrue(listeners.length >= 1);
        f.dispose();
        listeners = f.getChangeListeners();
        assertNotNull(listeners);
        assertEquals(0, listeners.length);
    }

    @Test
    public void testGeometricOperationsAreNotSupported() {
        Figure f = new AbstractFigureImpl();

        try {
            f.move(0, 0);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.rotate(null, 0);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.scale(null, 0, 0);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }
    }

    @Test
    public void testThatChildrenAreNotSupported() {
        Figure f = new AbstractFigureImpl();

        try {
            f.addFigure(new TestFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.addFigure(0, new TestFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.addFigures(new TestFigure(), new TestFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.getFigure(0);
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            f.removeFigure(new TestFigure());
            fail("IllegalStateException expected!");
        } catch (IllegalStateException e) {
            // ok
        }
    }
}