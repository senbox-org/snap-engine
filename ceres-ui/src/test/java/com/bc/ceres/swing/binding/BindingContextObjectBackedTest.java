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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValueSet;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@Ignore("Disabled, propagation of changes often not fast enough.")
public class BindingContextObjectBackedTest {

    private PropertyContainer propertyContainerOB;
    private BindingContext bindingContextOB;
    private TestPojo pojo;


    @Before
    public void setUp() throws Exception {
        Assume.assumeFalse("Fails often on server, therefore disabled in headless mode.", GraphicsEnvironment.isHeadless());

        pojo = new TestPojo();
        propertyContainerOB = PropertyContainer.createObjectBacked(pojo);
        propertyContainerOB.getDescriptor("valueSetBoundIntValue").setValueSet(new ValueSet(TestPojo.intValueSet));
        bindingContextOB = new BindingContext(propertyContainerOB, null);

    }

    @Test
    public void testBindTextField() throws Exception {
        JTextField textField = new JTextField();
        Binding binding = bindingContextOB.bind("stringValue", textField);
        assertNotNull(binding);
        assertSame(textField, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("stringValue", textField.getName());

        textField.setText("Bibo");
        textField.postActionEvent();
        Thread.sleep(150);
        assertEquals("Bibo", propertyContainerOB.getValue("stringValue"));

        propertyContainerOB.setValue("stringValue", "Samson");
        Thread.sleep(150);
        assertEquals("Samson", pojo.stringValue);
        assertEquals("Samson", textField.getText());

        pojo.stringValue = "Oscar";
        assertSame("Oscar", propertyContainerOB.getValue("stringValue"));
        assertNotSame("Oscar", textField.getText()); // value change not detected by binding
    }


    @Test
    public void testAdjustComponents() throws Exception {
        JTextField textField1 = new JTextField();
        JTextField textField2 = new JTextField();
        JCheckBox checkBox = new JCheckBox();

        pojo.booleanValue = true;
        pojo.doubleValue = 3.2;
        pojo.stringValue = "ABC";

        bindingContextOB.bind("booleanValue", checkBox);
        bindingContextOB.bind("doubleValue", textField1);
        bindingContextOB.bind("stringValue", textField2);

        Thread.sleep(150);
        assertTrue(checkBox.isSelected());
        assertEquals("3.2", textField1.getText());
        assertEquals("ABC", textField2.getText());

        pojo.booleanValue = false;
        pojo.doubleValue = 1.5;
        pojo.stringValue = "XYZ";

        assertTrue(checkBox.isSelected());
        assertEquals("3.2", textField1.getText());
        assertEquals("ABC", textField2.getText());

        bindingContextOB.adjustComponents();
        Thread.sleep(150);

        assertFalse(checkBox.isSelected());
        assertEquals("1.5", textField1.getText());
        assertEquals("XYZ", textField2.getText());
    }


    private static JComponent getPrimaryComponent(Binding binding) {
        return binding.getComponents()[0];
    }

    private static class TestPojo {
        boolean booleanValue;
        @SuppressWarnings("UnusedDeclaration")
        int intValue;
        double doubleValue;
        String stringValue;
        @SuppressWarnings("UnusedDeclaration")
        int[] listValue;

        @SuppressWarnings("UnusedDeclaration")
        int valueSetBoundIntValue;
        static Integer[] intValueSet = new Integer[]{101, 102, 103};
    }

}
