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

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;

public class CfFlagCodingPart extends ProfilePartIO {

    private static final String FLAG_MASKS = "flag_masks";
    private static final String FLAG_MEANINGS = "flag_meanings";
    private static final String FLAG_VALUES = "flag_values";

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        // already handled in CfBandPart
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            writeFlagCoding(band, ctx.getNetcdfFileWriteable());
        }
    }

    public static void writeFlagCoding(Band band, NFileWriteable ncFile) throws IOException {
        final FlagCoding fc = band. getFlagCoding();
        if (fc == null) {
            return;
        }

        String[] flagNames = fc.getFlagNames();
        int numFlags = flagNames.length;
        NVariable var = ncFile.findVariable(band.getName());
        DataType dType = ncFile.getNetcdfDataType(band.getDataType());

        StringBuilder meanings = new StringBuilder();
        String desc = fc.getDescription();
        int[] masks = new int[numFlags];
        int[] values = new int[numFlags];
        boolean hasValues = false;


        // construct meanings, masks and (if present) values
        for (int ii = 0; ii < numFlags; ii++) {
            if (ii > 0) {
                meanings.append(" ");
            }
            meanings.append(flagNames[ii]);

            MetadataAttribute attr = fc.getFlag(flagNames[ii]);
            ProductData data = attr.getData();
            masks[ii] = data.getElemIntAt(0);
            values[ii] = masks[ii];

            if (data.getNumElems() > 1) {
                values[ii] = data.getElemIntAt(1);
                hasValues = true;
            }
        }

        // add attributes meanings, description, masks and  (if present) values
        var.addAttribute(FLAG_MEANINGS, meanings.toString());
        if (desc != null && !desc.isEmpty()) {
            var.addAttribute("long_name", desc);
        }
        Object maskStorage = toStorageArray(dType, masks);
        Array maskArray = Array.factory(dType, new int[]{numFlags}, maskStorage);
        Attribute attrMasks = var.addAttribute(FLAG_MASKS, maskArray);
        Attribute attrValues = null;
        if (hasValues) {
            Object valuesStorage = toStorageArray(dType, values);
            Array valueArray = Array.factory(dType, new int[]{numFlags}, valuesStorage);
            attrValues = var.addAttribute(FLAG_VALUES, valueArray);
        }

        try {
            if (dType.isUnsigned()) {
                DataType dtMasks = attrMasks.getDataType().withSignedness(DataType.Signedness.UNSIGNED);
                attrMasks.setDataType(dtMasks);
                if (attrValues != null) {
                    DataType dtValues = attrValues.getDataType().withSignedness(DataType.Signedness.UNSIGNED);
                    attrValues.setDataType(dtValues);
                }
            }
        } catch (NullPointerException ignore) {}
    }

    public static FlagCoding readFlagCoding(ProfileReadContext ctx, String variableName) {
        final Variable variable = ctx.getNetcdfFile().getRootGroup().findVariable(variableName);
        final String codingName = variableName + "_flag_coding";
        if (variable != null) {
            return readFlagCoding(variable, codingName);
        } else {
            return null;
        }
    }

    private static FlagCoding readFlagCoding(Variable variable, String codingName) {
        final Attribute flagMasks = variable.findAttribute(FLAG_MASKS);
        int[] maskValues = null;
        if (flagMasks != null) {
            final Array flagMasksArray = flagMasks.getValues();
            if (flagMasksArray != null) {
                // must enforce unsigned data type
                // even though the unsigned property is set when writing the flag_masks attribute
                Array unsignedMaskData = enforceUnsignedDataType(flagMasksArray);

                // unsignedMaskData.get1DJavaArray(DataType.UINT) is not usable.
                // The resulting data is signed again --> manually copying
                maskValues = new int[flagMasks.getLength()];
                for (int i = 0; i < maskValues.length; i++) {
                    maskValues[i] = unsignedMaskData.getInt(i);
                }
            }
        }

        final Attribute flagMeanings = variable.findAttribute(FLAG_MEANINGS);
        final String[] flagNames;
        if (flagMeanings != null) {
            flagNames = StringUtils.makeStringsUnique(flagMeanings.getStringValue().split(" "));
        } else {
            flagNames = null;
        }
        return createFlagCoding(codingName, maskValues, flagNames);
    }

    static Array enforceUnsignedDataType(Array flagMasksArray) {
        Array unsignedMaskData;
        switch (flagMasksArray.getDataType()) {
            case BYTE:
                unsignedMaskData = Array.factory(DataType.UBYTE, flagMasksArray.getShape(), flagMasksArray.getStorage());
                break;
            case SHORT:
                unsignedMaskData = Array.factory(DataType.USHORT, flagMasksArray.getShape(), flagMasksArray.getStorage());
                break;
            case INT:
                unsignedMaskData = Array.factory(DataType.UINT, flagMasksArray.getShape(), flagMasksArray.getStorage());
                break;
            default:
                unsignedMaskData = flagMasksArray;
                break;
        }
        return unsignedMaskData;
    }

    private static FlagCoding createFlagCoding(String codingName, int[] maskValues, String[] flagNames) {
        if (maskValues != null && flagNames != null && maskValues.length == flagNames.length) {
            final FlagCoding coding = new FlagCoding(codingName);
            for (int i = 0; i < maskValues.length; i++) {
                final String sampleName = replaceNonWordCharacters(flagNames[i]);
                final int sampleValue = maskValues[i];
                coding.addSample(sampleName, sampleValue, "");
            }
            if (coding.getNumAttributes() > 0) {
                return coding;
            }
        }
        return null;
    }

    static String replaceNonWordCharacters(String flagName) {
        return flagName.replaceAll("\\W+", "_");
    }


    private static Object toStorageArray(ucar.ma2.DataType dType, int[] in) {
        switch (dType) {
            case BYTE:
            case UBYTE: {
                byte[] a = new byte[in.length];
                for (int i = 0; i < in.length; i++) {
                    a[i] = (byte) in[i];
                }
                return a;
            }
            case SHORT:
            case USHORT: {
                short[] a = new short[in.length];
                for (int i = 0; i < in.length; i++) {
                    a[i] = (short) in[i];
                }
                return a;
            }
            case INT:
            case UINT:
                return in;
            case LONG:
            case ULONG: {
                long[] a = new long[in.length];
                for (int i = 0; i < in.length; i++) {
                    a[i] = in[i];
                }
                return a;
            }
            default:
                return in;
        }
    }
}
