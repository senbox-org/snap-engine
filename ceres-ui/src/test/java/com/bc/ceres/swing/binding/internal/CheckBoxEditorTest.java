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
package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.BindingContext;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;

public class CheckBoxEditorTest {

    @Test
    public void testIsApplicable() {
        CheckBoxEditor checkBoxEditor = new CheckBoxEditor();

        PropertyDescriptor booleanDescriptor = new PropertyDescriptor("test", Boolean.TYPE);
        assertTrue(checkBoxEditor.isValidFor(booleanDescriptor));

        PropertyDescriptor doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        assertFalse(checkBoxEditor.isValidFor(doubleDescriptor));
    }

    @Test
    public void testCreateEditorComponent() {
        CheckBoxEditor checkBoxEditor = new CheckBoxEditor();

        PropertyContainer propertyContainer = PropertyContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(propertyContainer);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("b");
        assertSame(Boolean.TYPE, propertyDescriptor.getType());

        assertTrue(checkBoxEditor.isValidFor(propertyDescriptor));
        JComponent editorComponent = checkBoxEditor.createEditorComponent(propertyDescriptor, bindingContext);
        assertNotNull(editorComponent);
        assertSame(JCheckBox.class, editorComponent.getClass());
        JComponent[] components = bindingContext.getBinding("b").getComponents();
        assertEquals(1, components.length);
        assertSame(JCheckBox.class, components[0].getClass());
    }

    private static class V {
        boolean b;
    }
}
