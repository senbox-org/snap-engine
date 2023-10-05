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
package com.bc.ceres.swing.binding;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.internal.CheckBoxEditor;
import com.bc.ceres.swing.binding.internal.NumericEditor;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class ValueEditorRegistryTest {

    private PropertyEditorRegistry editorRegistry;

    @Before
    public void setUp() throws Exception {
        editorRegistry = PropertyEditorRegistry.getInstance();
    }

    @Test
    public void testGetValueEditor_notExistent() {
        PropertyEditor propertyEditor = editorRegistry.getPropertyEditor("foo");
        assertNull(propertyEditor);
    }

    @Test
    public void testGetValueEditor_TextField() throws Exception {
        PropertyEditor propertyEditor = editorRegistry.getPropertyEditor(TextFieldEditor.class.getName());
        assertNotNull(propertyEditor);
        assertSame(TextFieldEditor.class, propertyEditor.getClass());
    }

    @Test
    public void testFindValueEditor_Null() {
        try {
            editorRegistry.findPropertyEditor(null);
            fail();
        } catch (RuntimeException ignored) {
        }
    }

    @Test
    public void testFindValueEditor_UnknownEditor() {
        PropertyDescriptor descriptor = new PropertyDescriptor("test", TestCase.class);
        PropertyEditor propertyEditor = editorRegistry.findPropertyEditor(descriptor);
        assertNotNull(propertyEditor);
        assertSame(TextFieldEditor.class, propertyEditor.getClass());
    }

    @Test
    public void testFindValueEditor_SpecifiedEditor() {
        PropertyDescriptor descriptor = new PropertyDescriptor("test", Double.class);
        CheckBoxEditor checkBoxEditor = new CheckBoxEditor();
        descriptor.setAttribute("propertyEditor", checkBoxEditor);
        PropertyEditor propertyEditor = editorRegistry.findPropertyEditor(descriptor);
        assertNotNull(propertyEditor);
        assertSame(checkBoxEditor, propertyEditor);
    }

    @Test
    public void testFindValueEditor_MatchingEditor() {
        PropertyDescriptor descriptor = new PropertyDescriptor("test", Double.class);
        PropertyEditor propertyEditor = editorRegistry.findPropertyEditor(descriptor);
        assertNotNull(propertyEditor);
        assertSame(NumericEditor.class, propertyEditor.getClass());
    }
}
