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

package org.esa.snap.dataio.znap;

import org.esa.snap.core.datamodel.ProductData;

public enum SnapDataType {
    TYPE_FLOAT64(ProductData.TYPE_FLOAT64),
    TYPE_FLOAT32(ProductData.TYPE_FLOAT32),
    TYPE_INT8(ProductData.TYPE_INT8),
    TYPE_UINT8(ProductData.TYPE_UINT8),
    TYPE_INT16(ProductData.TYPE_INT16),
    TYPE_UINT16(ProductData.TYPE_UINT16),
    TYPE_INT32(ProductData.TYPE_INT32),
    TYPE_UINT32(ProductData.TYPE_UINT32);

    private final int value;

    SnapDataType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
