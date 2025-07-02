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

package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.nc.NWritableFactory;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class CfFlagCodingPartTest {

    @Test
    public void testReplaceNonWordCharacters() {
        assertEquals("a_b", CfFlagCodingPart.replaceNonWordCharacters("a/b"));
        assertEquals("a_b", CfFlagCodingPart.replaceNonWordCharacters("a / b"));
        assertEquals("a_b", CfFlagCodingPart.replaceNonWordCharacters("a.b"));
    }

    @Test
    public void testWriteFlagCoding() throws Exception {
        Band flagBand = new Band("flag_band", ProductData.TYPE_UINT8, 10, 10);
        FlagCoding flagCoding = new FlagCoding("some_flags");
        flagBand.setSampleCoding(flagCoding);
        flagCoding.setDescription("A Flag Coding");
        for (int i = 0; i < 8; i++) {
            addFlag(flagCoding, i);
        }
        NFileWriteable n3writable = NWritableFactory.create("not stored", "netcdf3");
        n3writable.addDimension("y", flagBand.getRasterHeight());
        n3writable.addDimension("x", flagBand.getRasterWidth());
        final DataType ncDataType = DataTypeUtils.getNetcdfDataType(flagBand.getDataType());
        NVariable variable = n3writable.addVariable(flagBand.getName(), ncDataType, null, "y x");
        CfBandPart.writeCfBandAttributes(flagBand, variable);
        CfFlagCodingPart.writeFlagCoding(flagBand, n3writable);

        NVariable someFlagsVariable = n3writable.findVariable("flag_band");
        assertNotNull(someFlagsVariable);
        Attribute flagMasksAttrib = someFlagsVariable.findAttribute("flag_masks");
        assertNotNull(flagMasksAttrib);
        if (someFlagsVariable.findAttribute("_Unsigned").getStringValue().equals("true")) {
            someFlagsVariable.setDataType(someFlagsVariable.getDataType().withSignedness(DataType.Signedness.UNSIGNED));
        }

        assertEquals(someFlagsVariable.getDataType(), flagMasksAttrib.getDataType());
        assertEquals(8, flagMasksAttrib.getLength());
        assertTrue(flagMasksAttrib.getDataType().isUnsigned());
        for (int i = 0; i < 8; i++) {
            assertEquals(1 << i, flagMasksAttrib.getValues().getInt(i));
        }

        Attribute descriptionAttrib = someFlagsVariable.findAttribute("long_name");
        assertNotNull(flagCoding.getDescription(), descriptionAttrib.getStringValue());

    }

    private void addFlag(FlagCoding flagCoding, int index) {
        MetadataAttribute attribute;

        attribute = new MetadataAttribute(String.format("%d_FLAG", index + 1), ProductData.TYPE_UINT8);
        final int maskValue = 1 << index;
        attribute.getData().setElemInt(maskValue);
        flagCoding.addAttribute(attribute);
    }

    @Test
    public void testEnforceUsingedDataType_byte() {
        byte[] bytes = {1, 2, 127, -128};
        Array factory = Array.factory(DataType.BYTE, new int[]{bytes.length}, bytes);
        Array unsingedBytes = CfFlagCodingPart.enforceUnsignedDataType(factory);
        assertEquals(1, unsingedBytes.getInt(0));
        assertEquals(2, unsingedBytes.getInt(1));
        assertEquals(127, unsingedBytes.getInt(2));
        assertEquals(128, unsingedBytes.getInt(3));
    }

    @Test
    public void testEnforceUsingedDataType_short() {
        short[] shorts = {1, 2, 32767, -32768};
        Array factory = Array.factory(DataType.SHORT, new int[]{shorts.length}, shorts);
        Array unsingedShorts = CfFlagCodingPart.enforceUnsignedDataType(factory);
        assertEquals(1, unsingedShorts.getInt(0));
        assertEquals(2, unsingedShorts.getInt(1));
        assertEquals(32767, unsingedShorts.getInt(2));
        assertEquals(32768, unsingedShorts.getInt(3));
    }

    @Test
    public void testEnforceUsingedDataType_int() {
        int[] ints = {1, 2, 2147483647, -2147483648};
        Array factory = Array.factory(DataType.INT, new int[]{ints.length}, ints);
        Array unsingedInt = CfFlagCodingPart.enforceUnsignedDataType(factory);
        assertEquals(1, unsingedInt.getInt(0));
        assertEquals(2, unsingedInt.getInt(1));
        assertEquals(2147483647, unsingedInt.getInt(2));
        // Long data type is not supported, so this values needs to stay negative.
        assertEquals(-2147483648, unsingedInt.getInt(3));
    }

    @Test
    @STTM("SNAP-3641")
    public void testWriteFlagCodingWithOptionalValues() throws Exception {
        Band flagBand = new Band("flag_band", ProductData.TYPE_UINT8, 10, 10);
        FlagCoding flagCoding = new FlagCoding("some_flags");
        flagCoding.setDescription("Flags with and without values");
        flagBand.setSampleCoding(flagCoding);

        final int mask = 0b0001;
        final int maskAndValValue = 42;
        flagCoding.addFlag("maskOnly", mask, "nur mask");
        flagCoding.addFlag("maskWithValue", mask, maskAndValValue, "mit value");

        NFileWriteable n3w = NWritableFactory.create("unused", "netcdf3");
        n3w.addDimension("y", flagBand.getRasterHeight());
        n3w.addDimension("x", flagBand.getRasterWidth());
        DataType ncType = DataTypeUtils.getNetcdfDataType(flagBand.getDataType());
        NVariable var = n3w.addVariable(flagBand.getName(), ncType, null, "y x");

        CfBandPart.writeCfBandAttributes(flagBand, var);
        CfFlagCodingPart.writeFlagCoding(flagBand, n3w);

        Attribute meaningsAttr = var.findAttribute("flag_meanings");
        assertEquals("maskOnly maskWithValue", meaningsAttr.getStringValue());

        // assertions for flag_masks
        Attribute masksAttr = var.findAttribute("flag_masks");
        assertNotNull(masksAttr);
        assertEquals(2, masksAttr.getLength());
        Array masksArr = masksAttr.getValues();
        assertEquals(mask, masksArr.getInt(0));
        assertEquals(mask, masksArr.getInt(1));
        assertTrue(masksAttr.getDataType().isUnsigned());

        // assertions for flag_values
        Attribute valuesAttr = var.findAttribute("flag_values");
        assertNotNull(valuesAttr);
        assertEquals(2, valuesAttr.getLength());
        Array valuesArr = valuesAttr.getValues();
        assertEquals(mask, valuesArr.getInt(0));
        assertEquals(maskAndValValue, valuesArr.getInt(1));
        assertTrue( valuesAttr.getDataType().isUnsigned());

        Attribute descAttr = var.findAttribute("long_name");
        assertEquals("Flags with and without values", descAttr.getStringValue());
    }

    @Test
    @STTM("SNAP-3641")
    public void testWriteFlagCodingWithoutValues() throws Exception {
        Band flagBand = new Band("flag_band", ProductData.TYPE_UINT8, 10, 10);
        FlagCoding flagCoding = new FlagCoding("some_flags");
        flagCoding.setDescription("Flags without values");
        flagBand.setSampleCoding(flagCoding);

        final int mask1 = 0b0001;
        final int mask2 = 0b0010;
        flagCoding.addFlag("mask1", mask1,"mask1");
        flagCoding.addFlag("mask2", mask2, "mask2");

        NFileWriteable n3w = NWritableFactory.create("unused", "netcdf3");
        n3w.addDimension("y", flagBand.getRasterHeight());
        n3w.addDimension("x", flagBand.getRasterWidth());
        DataType ncType = DataTypeUtils.getNetcdfDataType(flagBand.getDataType());
        NVariable var = n3w.addVariable(flagBand.getName(), ncType, null, "y x");

        CfBandPart.writeCfBandAttributes(flagBand, var);
        CfFlagCodingPart.writeFlagCoding(flagBand, n3w);

        // assertions for flag_masks
        Attribute masksAttr = var.findAttribute("flag_masks");
        assertNotNull(masksAttr);
        assertEquals(2, masksAttr.getLength());
        Array masksArr = masksAttr.getValues();
        assertEquals(mask1, masksArr.getInt(0));
        assertEquals(mask2, masksArr.getInt(1));
        assertTrue(masksAttr.getDataType().isUnsigned());

        // assertions for flag_values
        Attribute valuesAttr = var.findAttribute("flag_values");
        assertNull(valuesAttr);

        Attribute descAttr = var.findAttribute("long_name");
        assertEquals("Flags without values", descAttr.getStringValue());
    }


    @Test
    @STTM("SNAP-3641")
    public void testToStorageArray_byte() {
        // sigend byte
        int[] in_signed = new int[] { -128, 127 };
        byte[] expected_signed = new byte[] {-128, 127 };
        DataType dType_signed = DataType.BYTE;

        Object storageArray = CfFlagCodingPart.toStorageArray(dType_signed, in_signed);

        assertTrue(storageArray instanceof byte[]);
        byte[] actual = (byte[]) storageArray;
        assertArrayEquals(expected_signed, actual);

        // unsigend byte
        int[] in_unsigned = new int[] { 0, 255 };
        byte[] expected_unsigned = new byte[] {0, (byte)255 };
        DataType dType_unsigned = DataType.UBYTE;

        Object storageArray_unsigned = CfFlagCodingPart.toStorageArray(dType_unsigned, in_unsigned);

        assertTrue(storageArray_unsigned instanceof byte[]);
        byte[] actual_unsigned = (byte[]) storageArray_unsigned;
        assertArrayEquals(expected_unsigned, actual_unsigned);
    }

    @Test
    @STTM("SNAP-3641")
    public void testToStorageArray_short() {
        // signed short
        int[] in_signed = new int[] { Short.MIN_VALUE, Short.MAX_VALUE };
        short[] expected_signed = new short[] { Short.MIN_VALUE, Short.MAX_VALUE };
        DataType dType_signed = DataType.SHORT;

        Object storage_signed = CfFlagCodingPart.toStorageArray(dType_signed, in_signed);
        assertTrue(storage_signed instanceof short[]);
        short[] actual_signed = (short[]) storage_signed;
        assertArrayEquals(expected_signed, actual_signed);

        // unsigned short
        int[] in_unsigned = new int[] { 0, 0xFFFF };
        short[] expected_unsigned = new short[] { (short) 0, (short) 0xFFFF };
        DataType dType_unsigned = DataType.USHORT;

        Object storage_unsigned = CfFlagCodingPart.toStorageArray(dType_unsigned, in_unsigned);
        assertTrue(storage_unsigned instanceof short[]);
        short[] actual_unsigned = (short[]) storage_unsigned;
        assertArrayEquals(expected_unsigned, actual_unsigned);
        assertEquals(0,      Short.toUnsignedInt(actual_unsigned[0]));
        assertEquals(0xFFFF, Short.toUnsignedInt(actual_unsigned[1]));
    }

    @Test
    @STTM("SNAP-3641")
    public void testToStorageArray_int() {
        // signed int
        int[] inSigned = new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
        int[] expectedSigned = new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
        DataType dTypeSigned = DataType.INT;

        Object storageSigned = CfFlagCodingPart.toStorageArray(dTypeSigned, inSigned);
        assertTrue(storageSigned instanceof int[]);
        int[] actualSigned = (int[]) storageSigned;
        assertArrayEquals(expectedSigned, actualSigned);

        // unsigned int
        int[] inUnsigned = new int[] { 0, 0xFFFFFFFF };
        int[] expectedUnsigned = new int[] { 0, (int) 0xFFFFFFFF };
        DataType dTypeUnsigned = DataType.UINT;

        Object storageUnsigned = CfFlagCodingPart.toStorageArray(dTypeUnsigned, inUnsigned);
        assertTrue(storageUnsigned instanceof int[]);
        int[] actualUnsigned = (int[]) storageUnsigned;
        assertArrayEquals(expectedUnsigned, actualUnsigned);
        assertEquals(0L, Integer.toUnsignedLong(actualUnsigned[0]));
        assertEquals(0xFFFFFFFFL, Integer.toUnsignedLong(actualUnsigned[1]));
    }

    @Test
    @STTM("SNAP-3641")
    public void testToStorageArray_notSupportedDataType() {
        int[] in = new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
        DataType dType = DataType.LONG;

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CfFlagCodingPart.toStorageArray(dType, in)
        );
        assertEquals("Unsupported DataType: " + dType, ex.getMessage());
    }
}
