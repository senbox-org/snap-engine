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

import static org.esa.snap.core.datamodel.Mask.RangeType.PROPERTY_NAME_MAXIMUM;
import static org.esa.snap.core.datamodel.Mask.RangeType.PROPERTY_NAME_MINIMUM;
import static org.esa.snap.core.datamodel.Mask.RangeType.PROPERTY_NAME_RASTER;

/**
 * @author Marco Peters
 * @author Sabine Embacher
 * @since SNAP 9.0
 */
public class RangeTypeMaskPersistenceConverter extends AbstractMaskPersistenceConverter  {

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 will be used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "RT_MASK:1";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    protected Mask.ImageType createImageType() {
        return Mask.RangeType.INSTANCE;
    }

    @Override
    protected void configureMask(Mask mask, Container root, Product product) {
        final double minimum = root.getProperty(PROPERTY_NAME_MINIMUM).getValueDouble();
        final double maximum = root.getProperty(PROPERTY_NAME_MAXIMUM).getValueDouble();
        final String raster =  root.getProperty(PROPERTY_NAME_RASTER).getValueString();

        final PropertyContainer imageConfig = mask.getImageConfig();
        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, minimum);
        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, maximum);
        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, raster);
    }

    @Override
    protected void configureContainer(Container root, Mask mask) {
        final PropertyContainer config = mask.getImageConfig();
        Object minValue = config.getValue(PROPERTY_NAME_MINIMUM);
        Object maxValue = config.getValue(PROPERTY_NAME_MAXIMUM);
        Object rasterValue = config.getValue(PROPERTY_NAME_RASTER);

        root.add(new Property<>(PROPERTY_NAME_MINIMUM, minValue));
        root.add(new Property<>(PROPERTY_NAME_MAXIMUM, maxValue));
        root.add(new Property<>(PROPERTY_NAME_RASTER, rasterValue));
    }
}
