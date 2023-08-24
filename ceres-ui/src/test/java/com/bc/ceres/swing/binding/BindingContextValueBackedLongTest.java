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

import com.bc.ceres.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.swing.JTextField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(LongTestRunner.class)
public class BindingContextValueBackedLongTest extends BindingContextValueBackedTest{

    @Test
    public void testBindTextField() throws Exception {
        JTextField textField = new JTextField();
        Binding binding = bindingContextVB.bind("stringValue", textField);
        Thread.sleep(1000); // previous value of 100 was not enough for building on my desktop rq-20140426
        assertNotNull(binding);
        assertSame(textField, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("stringValue", textField.getName());

        textField.setText("Bibo");
        textField.postActionEvent();
        assertEquals("Bibo", propertyContainerVB.getValue("stringValue"));

        propertyContainerVB.setValue("stringValue", "Samson");
        Thread.sleep(1000); // previous value of 100 was not enough for building on my desktop rq-20140426
        assertEquals("Samson", textField.getText());
    }


}
