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

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.nc.NWritableFactory;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;

/**
 * @author Marco Peters
 */
public class CfFlagCodingPartTest extends TestCase {

    public void testReplaceNonWordCharacters() {
       assertEquals("a_b", CfFlagCodingPart.replaceNonWordCharacters("a/b"));
       assertEquals("a_b", CfFlagCodingPart.replaceNonWordCharacters("a / b"));
       assertEquals("a_b", CfFlagCodingPart.replaceNonWordCharacters("a.b"));
    }

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
        NVariable variable = n3writable.addVariable(flagBand.getName(), ncDataType, null,"y x");
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

    public void testEnforceUsingedDataType_byte() {
        byte[] bytes = {1, 2, 127, -128};
        Array factory = Array.factory(DataType.BYTE, new int[]{bytes.length}, bytes);
        Array unsingedBytes = CfFlagCodingPart.enforceUnsignedDataType(factory);
        assertEquals(1, unsingedBytes.getInt(0));
        assertEquals(2, unsingedBytes.getInt(1));
        assertEquals(127, unsingedBytes.getInt(2));
        assertEquals(128, unsingedBytes.getInt(3));
    }

    public void testEnforceUsingedDataType_short() {
        short[] shorts = {1, 2, 32767, -32768};
        Array factory = Array.factory(DataType.SHORT, new int[]{shorts.length}, shorts);
        Array unsingedShorts = CfFlagCodingPart.enforceUnsignedDataType(factory);
        assertEquals(1, unsingedShorts.getInt(0));
        assertEquals(2, unsingedShorts.getInt(1));
        assertEquals(32767, unsingedShorts.getInt(2));
        assertEquals(32768, unsingedShorts.getInt(3));
    }

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
}
