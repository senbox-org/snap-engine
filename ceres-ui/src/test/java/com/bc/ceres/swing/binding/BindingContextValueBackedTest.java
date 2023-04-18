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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import org.junit.Before;
import org.junit.Test;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BindingContextValueBackedTest {

    BindingContext bindingContextVB;
    PropertyContainer propertyContainerVB;

    private Exception error;
    private JComponent component;

    @Before
    public void setUp() throws Exception {
        propertyContainerVB = PropertyContainer.createValueBacked(TestPojo.class);
        propertyContainerVB.getDescriptor("valueSetBoundIntValue").setValueSet(new ValueSet(TestPojo.intValueSet));
        bindingContextVB = new BindingContext(propertyContainerVB, null);
        bindingContextVB.addProblemListener(new MyBindingProblemListener());

        error = null;
        component = null;
    }

    private void clearError() {
        error = null;
        component = null;
    }

    @Test
    public void testBindSpinner() {
        JSpinner spinner = new JSpinner();
        Binding binding = bindingContextVB.bind("intValue", spinner);
        assertNotNull(binding);
        assertSame(spinner, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("intValue", spinner.getName());

        spinner.setValue(3);
        assertEquals(3, (int)propertyContainerVB.getValue("intValue"));

        propertyContainerVB.setValue("intValue", 76);
        assertEquals(76, spinner.getValue());

    }

    @Test
    public void testBindComboBox() {
        JComboBox<Integer> comboBox = new JComboBox<>(new Integer[]{1, 3, 7});
        comboBox.setEditable(false);

        Binding binding = bindingContextVB.bind("intValue", comboBox);
        assertNotNull(binding);
        assertSame(comboBox, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("intValue", comboBox.getName());

        comboBox.setSelectedItem(3);
        assertEquals(3, comboBox.getSelectedItem());
        assertEquals(3, binding.getPropertyValue());

        propertyContainerVB.setValue("intValue", 1);
        assertEquals(1, comboBox.getSelectedItem());
        assertEquals(1, binding.getPropertyValue());

        // "4" is not allowed, because it is not in the comboBox's model
        comboBox.setSelectedItem(4);
        assertEquals(1, comboBox.getSelectedItem());
        assertEquals(1, binding.getPropertyValue());

        comboBox.setEditable(true);

        // Now "4" is allowed, because the comboBox is editable
        comboBox.setSelectedItem(4);
        assertEquals(4, comboBox.getSelectedItem());
        assertEquals(4, binding.getPropertyValue());

        propertyContainerVB.setValue("intValue", 5);
        assertEquals(5, comboBox.getSelectedItem());
        assertEquals(5, binding.getPropertyValue());

        comboBox.setEditable(false);

        final ValueSet valueSet = new ValueSet(new Object[]{10, 20});
        final PropertyDescriptor descriptor = binding.getContext().getPropertySet().getDescriptor("intValue");
        descriptor.setValueSet(valueSet);

        assertEquals(2, comboBox.getModel().getSize());
        assertEquals(10, comboBox.getModel().getElementAt(0).intValue());
        assertEquals(20, comboBox.getModel().getElementAt(1).intValue());

        assertEquals(10, comboBox.getSelectedItem());
        assertEquals(10, binding.getPropertyValue());
        assertNull(binding.getProblem());
    }

    @Test
    public void testBindFormattedTextFieldToString() {
        JFormattedTextField textField = new JFormattedTextField();
        Binding binding = bindingContextVB.bind("stringValue", textField);
        assertNotNull(binding);
        assertSame(textField, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("stringValue", textField.getName());

        textField.setValue("Bibo");
        assertEquals("Bibo", propertyContainerVB.getValue("stringValue"));

        propertyContainerVB.setValue("stringValue", "Samson");
        assertEquals("Samson", textField.getValue());
    }

    @Test
    public void testBindFormattedTextFieldToDouble() {
        JFormattedTextField textField = new JFormattedTextField();
        Binding binding = bindingContextVB.bind("doubleValue", textField);
        assertNotNull(binding);
        assertSame(textField, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("doubleValue", textField.getName());

        textField.setValue(3.14);
        assertEquals(3.14, propertyContainerVB.getValue("doubleValue"), 1.0e-6);

        propertyContainerVB.setValue("doubleValue", 2.71);
        assertEquals(2.71, textField.getValue());
    }

    @Test
    public void testBindTextArea() throws Exception {
        JTextArea textArea = new JTextArea();
        TextComponentAdapter textComponentAdapter = new TextComponentAdapter(textArea);
        Binding binding = bindingContextVB.bind("stringValue", textComponentAdapter);
        assertNotNull(binding);
        assertSame(textArea, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("stringValue", textArea.getName());

        textArea.setText("Bibo");
        textComponentAdapter.actionPerformed(null);
        Thread.sleep(150);
        assertEquals("Bibo", propertyContainerVB.getValue("stringValue"));

        propertyContainerVB.setValue("stringValue", "Samson");
        Thread.sleep(150);
        assertEquals("Samson", textArea.getText());
    }

    @Test
    public void testBindCheckBox() {
        JCheckBox checkBox = new JCheckBox();
        Binding binding = bindingContextVB.bind("booleanValue", checkBox);
        assertNotNull(binding);
        assertSame(checkBox, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("booleanValue", checkBox.getName());

        checkBox.doClick();
        assertEquals(true, propertyContainerVB.getValue("booleanValue"));

        propertyContainerVB.setValue("booleanValue", false);
        assertFalse(checkBox.isSelected());
    }

    @Test
    public void testBindRadioButton() {
        JRadioButton radioButton = new JRadioButton();
        Binding binding = bindingContextVB.bind("booleanValue", radioButton);
        assertNotNull(binding);
        assertSame(radioButton, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("booleanValue", radioButton.getName());

        radioButton.doClick();
        assertEquals(true, propertyContainerVB.getValue("booleanValue"));

        propertyContainerVB.setValue("booleanValue", false);
        assertFalse(radioButton.isSelected());
    }

    @Test
    public void testBindButtonGroup() throws ValidationException {
        JRadioButton radioButton1 = new JRadioButton();
        JRadioButton radioButton2 = new JRadioButton();
        JRadioButton radioButton3 = new JRadioButton();

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(radioButton1);
        buttonGroup.add(radioButton2);
        buttonGroup.add(radioButton3);

        Property m = propertyContainerVB.getProperty("valueSetBoundIntValue");

        m.setValue(TestPojo.intValueSet[0]);

        Binding binding = bindingContextVB.bind("valueSetBoundIntValue", buttonGroup);
        assertNotNull(binding);
        assertSame(radioButton1, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(3, binding.getComponents().length);
        assertSame(radioButton1, binding.getComponents()[0]);
        assertSame(radioButton2, binding.getComponents()[1]);
        assertSame(radioButton3, binding.getComponents()[2]);

        assertTrue(radioButton1.isSelected());
        assertFalse(radioButton2.isSelected());
        assertFalse(radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[0], m.getValue());

        radioButton3.doClick();
        assertFalse(radioButton1.isSelected());
        assertFalse(radioButton2.isSelected());
        assertTrue(radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[2], m.getValue());

        radioButton2.doClick();
        assertFalse(radioButton1.isSelected());
        assertTrue(radioButton2.isSelected());
        assertFalse(radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[1], m.getValue());

        m.setValue(TestPojo.intValueSet[0]);
        assertTrue(radioButton1.isSelected());
        assertFalse(radioButton2.isSelected());
        assertFalse(radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[0], m.getValue());

        m.setValue(TestPojo.intValueSet[2]);
        assertFalse(radioButton1.isSelected());
        assertFalse(radioButton2.isSelected());
        assertTrue(radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[2], m.getValue());

        m.setValue(TestPojo.intValueSet[1]);
        assertFalse(radioButton1.isSelected());
        assertTrue(radioButton2.isSelected());
        assertFalse(radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[1], m.getValue());
    }

    @Test
    public void testBindListSelection() {
        JList<Integer> list = new JList<>(new Integer[]{3, 4, 5, 6, 7});
        Binding binding = bindingContextVB.bind("listValue", list, true);
        assertNotNull(binding);
        assertSame(list, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("listValue", list.getName());

        list.setSelectedIndex(2);
        assertArrayEquals(new int[]{5}, propertyContainerVB.getValue("listValue"));

        propertyContainerVB.setValue("listValue", new int[]{6});
        assertEquals(6, list.getSelectedValue().intValue());
    }

    @Test
    public void testSecondaryComponent() {
        JTextField textField = new JTextField();
        Binding binding = bindingContextVB.bind("stringValue", textField);
        JLabel label = new JLabel("myLabel");
        binding.addComponent(label);

        JComponent[] components = binding.getComponents();
        assertNotNull(components);
        assertEquals(2, components.length);
        assertSame(getPrimaryComponent(binding), components[0]);
        assertSame(label, components[1]);
    }

    @Test
    public void testProblemManagement() {
        JTextField intTextField = new JTextField();
        JTextField stringTextField = new JTextField();
        final MyChangeListener listener = new MyChangeListener();

        clearError();
        bindingContextVB.addProblemListener(listener);
        bindingContextVB.addPropertyChangeListener(listener);
        bindingContextVB.bind("intValue", intTextField);
        bindingContextVB.bind("stringValue", stringTextField);

        assertEquals("", listener.trace);
        assertFalse(bindingContextVB.hasProblems());
        assertNotNull(bindingContextVB.getProblems());
        assertEquals(0, bindingContextVB.getProblems().length);
        assertNull(error);
        assertNull(component);

        clearError();
        bindingContextVB.getBinding("intValue").setPropertyValue("a");

        assertEquals("P;", listener.trace);
        assertTrue(bindingContextVB.hasProblems());
        assertNotNull(bindingContextVB.getProblems());
        assertEquals(1, bindingContextVB.getProblems().length);
        assertNotNull(error);
        assertSame(intTextField, component);

        clearError();
        bindingContextVB.getBinding("stringValue").setPropertyValue(5);

        assertEquals("P;P;", listener.trace);
        assertTrue(bindingContextVB.hasProblems());
        assertNotNull(bindingContextVB.getProblems());
        assertEquals(2, bindingContextVB.getProblems().length);
        assertNotNull(error);
        assertSame(stringTextField, component);

        clearError();
        bindingContextVB.getBinding("intValue").setPropertyValue(5);

        assertEquals("P;P;V;C;", listener.trace);
        assertTrue(bindingContextVB.hasProblems());
        assertNotNull(bindingContextVB.getProblems());
        assertEquals(1, bindingContextVB.getProblems().length);
        assertNull(error);
        assertNull(component);

        clearError();
        bindingContextVB.getBinding("stringValue").setPropertyValue("a");

        assertEquals("P;P;V;C;V;C;", listener.trace);
        assertFalse(bindingContextVB.hasProblems());
        assertNotNull(bindingContextVB.getProblems());
        assertEquals(0, bindingContextVB.getProblems().length);
        assertNull(error);
        assertNull(component);
    }


    static JComponent getPrimaryComponent(Binding binding) {
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

    private static class MyChangeListener implements BindingProblemListener, PropertyChangeListener {
        String trace = "";

        @Override
        public void problemReported(BindingProblem newProblem, BindingProblem oldProblem) {
            trace += "P;";
        }

        @Override
        public void problemCleared(BindingProblem oldProblem) {
            trace += "C;";
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            trace += "V;";
        }
    }

    private class MyBindingProblemListener implements BindingProblemListener {
        @Override
            public void problemReported(BindingProblem newProblem, BindingProblem oldProblem) {
            error = newProblem.getCause();
            component = newProblem.getBinding().getComponents()[0];
        }

        @Override
            public void problemCleared(BindingProblem oldProblem) {
            error = null;
            component = null;
        }
    }
}
