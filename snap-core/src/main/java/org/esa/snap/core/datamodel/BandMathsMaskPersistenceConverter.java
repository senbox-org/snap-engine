/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.core.datamodel;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.Property;

import static org.esa.snap.core.datamodel.Mask.BandMathsType.PROPERTY_NAME_EXPRESSION;

/**
 * @author Marco Peters
 * @author Sabine Embacher
 * @since SNAP 9.0
 */
class BandMathsMaskPersistenceConverter extends AbstractMaskPersistenceConverter {

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 will be used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "BM_MASK:1";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    protected Mask.ImageType createImageType() {
        return Mask.BandMathsType.INSTANCE;
    }

    @Override
    protected void configureMask(Mask mask, Container root) {
        final String expression = root.getProperty(PROPERTY_NAME_EXPRESSION).getValueString();

        final PropertyContainer imageConfig = mask.getImageConfig();
        imageConfig.setValue(PROPERTY_NAME_EXPRESSION, expression);
    }

    @Override
    protected void configureContainer(Container root, Mask mask) {
        final PropertyContainer config = mask.getImageConfig();
        final String expression = config.getValue(PROPERTY_NAME_EXPRESSION).toString();

        root.add(new Property<>(PROPERTY_NAME_EXPRESSION, expression));
    }
}
