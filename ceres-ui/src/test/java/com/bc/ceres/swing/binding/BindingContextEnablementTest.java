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
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BindingContextEnablementTest {

    @Test
    public void testEnablement() {
        final BindingContext bindingContext = new BindingContext(PropertyContainer.createObjectBacked(new X()));
        final JTextField aField = new JTextField();
        final JTextField bField = new JTextField();
        bindingContext.bind("a", aField);
        bindingContext.bind("b", bField);

        assertTrue(aField.isEnabled());
        assertTrue(bField.isEnabled());

        bindingContext.bindEnabledState("b", true, "a", "Hanni");
        bindingContext.adjustComponents();

        assertTrue(aField.isEnabled());
        assertFalse(bField.isEnabled());

        bindingContext.getPropertySet().setValue("a", "Nanni");

        assertTrue(aField.isEnabled());
        assertFalse(bField.isEnabled());

        bindingContext.getPropertySet().setValue("a", "Hanni");

        assertTrue(aField.isEnabled());
        assertTrue(bField.isEnabled());

        bindingContext.unbind(bindingContext.getBinding("b"));

        bindingContext.getPropertySet().setValue("a", "Pfanni");

        assertTrue(aField.isEnabled());
        assertTrue(bField.isEnabled());
    }

    public static class X {
        String a;
        String b;
    }
}
