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
import java.nio.file.Path;

import static org.junit.Assert.*;

public class PathEditorTest {

  @Test
  public void testIsApplicable() {
    PathEditor pathEditor = new PathEditor();

    PropertyDescriptor fileDescriptor = new PropertyDescriptor("test", Path.class);
    assertTrue(pathEditor.isValidFor(fileDescriptor));

    PropertyDescriptor doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
    assertFalse(pathEditor.isValidFor(doubleDescriptor));
  }

  @Test
  public void testCreateEditorComponent() {
    PathEditor pathEditor = new PathEditor();

    PropertyContainer propertyContainer = PropertyContainer.createValueBacked(V.class);
    BindingContext bindingContext = new BindingContext(propertyContainer);
    PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("filePath");
    assertSame(Path.class, propertyDescriptor.getType());

    assertTrue(pathEditor.isValidFor(propertyDescriptor));
    JComponent editorComponent = pathEditor.createEditorComponent(propertyDescriptor, bindingContext);
    assertNotNull(editorComponent);
    assertSame(JPanel.class, editorComponent.getClass());
    assertEquals(2, editorComponent.getComponentCount());

    JComponent[] components = bindingContext.getBinding("filePath").getComponents();
    assertEquals(1, components.length);
    assertSame(JTextField.class, components[0].getClass());
  }

  private static class V {

    Path filePath;
  }
}
