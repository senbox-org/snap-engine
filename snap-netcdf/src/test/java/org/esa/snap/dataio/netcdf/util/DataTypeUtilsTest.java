/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.util;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;
import ucar.ma2.DataType;

import static org.junit.Assert.assertEquals;

public class DataTypeUtilsTest {

    @Test
    public void testConvertToBYTE() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.BYTE);
        assertEquals((byte) 12, convertedNumber);
        assertEquals(DataType.BYTE, DataType.getType(convertedNumber.getClass(),false));
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.BYTE);
        assertEquals((byte) -123, convertedNumber);
        assertEquals(DataType.BYTE, DataType.getType(convertedNumber.getClass(),false));
    }

    @Test
    public void testConvertToSHORT() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.SHORT);
        assertEquals(DataType.SHORT, DataType.getType(convertedNumber.getClass(),false));
        assertEquals((short) 12, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.SHORT);
        assertEquals(DataType.SHORT, DataType.getType(convertedNumber.getClass(),false));
        assertEquals((short) -123, convertedNumber);
    }

    @Test
    public void testConvertToINT() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.INT);
        assertEquals(DataType.INT, DataType.getType(convertedNumber.getClass(),false));
        assertEquals(12, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.INT);
        assertEquals(DataType.INT, DataType.getType(convertedNumber.getClass(),false));
        assertEquals(-123, convertedNumber);
    }

    @Test
    public void testConvertToLONG() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.LONG);
        assertEquals(DataType.LONG, DataType.getType(convertedNumber.getClass(),false));
        assertEquals(12L, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.LONG);
        assertEquals(DataType.LONG, DataType.getType(convertedNumber.getClass(),false));
        assertEquals(-123L, convertedNumber);
    }

    @Test
    public void testConvertToFLOAT() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.FLOAT);
        assertEquals(DataType.FLOAT, DataType.getType(convertedNumber.getClass(),false));
        assertEquals(12.3f, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.FLOAT);
        assertEquals(DataType.FLOAT, DataType.getType(convertedNumber.getClass(),false));
        assertEquals(-123f, convertedNumber);
    }

    @Test
    public void testConvertToDOUBLE() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.DOUBLE);
        assertEquals(DataType.DOUBLE, DataType.getType(convertedNumber.getClass(),false));
        assertEquals(12.3, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.DOUBLE);
        assertEquals(DataType.DOUBLE, DataType.getType(convertedNumber.getClass(),false));
        assertEquals(-123.0, convertedNumber);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertToWithIllegalArgument() {
        DataTypeUtils.convertTo(12.3, DataType.STRING);
    }

    @Test
    public void testGetEquivalentProductDataType() {
        assertEquals(ProductData.TYPE_UINT8, DataTypeUtils.getEquivalentProductDataType(DataType.BYTE, true, false));
        assertEquals(ProductData.TYPE_INT8, DataTypeUtils.getEquivalentProductDataType(DataType.BYTE, false, false));

        assertEquals(ProductData.TYPE_UINT8, DataTypeUtils.getEquivalentProductDataType(DataType.UBYTE, true, false));

        assertEquals(ProductData.TYPE_UINT16, DataTypeUtils.getEquivalentProductDataType(DataType.SHORT, true, false));
        assertEquals(ProductData.TYPE_INT16, DataTypeUtils.getEquivalentProductDataType(DataType.SHORT, false, false));

        assertEquals(ProductData.TYPE_UINT16, DataTypeUtils.getEquivalentProductDataType(DataType.USHORT, true, false));

        assertEquals(ProductData.TYPE_UINT32, DataTypeUtils.getEquivalentProductDataType(DataType.INT, true, false));
        assertEquals(ProductData.TYPE_INT32, DataTypeUtils.getEquivalentProductDataType(DataType.INT, false, false));

        assertEquals(ProductData.TYPE_UINT32, DataTypeUtils.getEquivalentProductDataType(DataType.UINT, true, false));

        assertEquals(ProductData.TYPE_INT64, DataTypeUtils.getEquivalentProductDataType(DataType.LONG, false, false));
        assertEquals(ProductData.TYPE_UINT64, DataTypeUtils.getEquivalentProductDataType(DataType.LONG, true, false));

        assertEquals(ProductData.TYPE_UINT64, DataTypeUtils.getEquivalentProductDataType(DataType.ULONG, false, false));

        assertEquals(ProductData.TYPE_FLOAT32, DataTypeUtils.getEquivalentProductDataType(DataType.FLOAT, false, false));
        assertEquals(ProductData.TYPE_FLOAT64, DataTypeUtils.getEquivalentProductDataType(DataType.DOUBLE, false, false));

        assertEquals(-1, DataTypeUtils.getEquivalentProductDataType(DataType.CHAR, false, true));
        assertEquals(ProductData.TYPE_ASCII, DataTypeUtils.getEquivalentProductDataType(DataType.CHAR, false, false));
        assertEquals(ProductData.TYPE_ASCII, DataTypeUtils.getEquivalentProductDataType(DataType.STRING, false, false));
    }
}
