/*
 * Copyright (C) 2026 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueRange;
import org.junit.Test;

import static org.junit.Assert.fail;

public class IntervalValidatorTest {

    @Test
    public void testValidate_success() throws ValidationException {
        final ValueRange valueRange = new ValueRange(2.0, 4.0);
        final IntervalValidator intervalValidator = new IntervalValidator(valueRange);

        intervalValidator.validateValue(null, 2.7);
        intervalValidator.validateValue(null, 3.9);
    }

    @Test
    public void testValidate_fail() {
        final ValueRange valueRange = new ValueRange(2.0, 4.0);
        final IntervalValidator intervalValidator = new IntervalValidator(valueRange);

        final Property property = new Property(new PropertyDescriptor("property", Double.class), null);

        try {
            intervalValidator.validateValue(property, 1.9);
            fail("ValidationException expected");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testValidate_noNumber() throws ValidationException {
        final ValueRange valueRange = new ValueRange(2.0, 4.0);
        final IntervalValidator intervalValidator = new IntervalValidator(valueRange);

        intervalValidator.validateValue(null, "jho, this should pass");
    }
}
