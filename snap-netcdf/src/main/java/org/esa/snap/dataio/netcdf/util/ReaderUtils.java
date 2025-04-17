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

package org.esa.snap.dataio.netcdf.util;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import ucar.ma2.Array;
import ucar.ma2.MAMath;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.iosp.netcdf3.N3iosp;

import java.util.List;

/**
 * Provides some NetCDF related utility methods.
 */
public class ReaderUtils {

    public static ProductData createProductData(int productDataType, Array values) {
        Object data = values.getStorage();
        if (data instanceof char[]) {
            data = new String((char[]) data);
        }
        return ProductData.createInstance(productDataType, data);
    }

    public static boolean hasValidExtension(String pathname) {
        final String lowerPath = pathname.toLowerCase();
        final String[] validExtensions = Constants.FILE_EXTENSIONS;
        for (String validExtension : validExtensions) {
            validExtension = validExtension.toLowerCase();
            if (lowerPath.endsWith(validExtension)) {
                return true;
            }
        }
        return false;
    }

    public static Variable[] getVariables(List<Variable> variables, String[] names) {
        if (variables == null || names == null) {
            return null;
        }
        if (variables.size() < names.length) {
            return null;
        }
        final Variable[] result = new Variable[names.length];
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            for (Variable variable : variables) {
                if (variable.getFullName().equalsIgnoreCase(name)) {
                    result[i] = variable;
                    break;
                }
            }
            if (result[i] == null) {
                return null;
            }
        }
        return result;
    }

    public static String getRasterName(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.ORIG_NAME_ATT_NAME);
        if (attribute != null) {
            return attribute.getStringValue();
        } else {
            return variable.getFullName();
        }
    }

    public static String getVariableName(RasterDataNode rasterDataNode) {
        String name = N3iosp.makeValidNetcdfObjectName(rasterDataNode.getName());
        name = name.replace('.', '_');
        if (!VariableNameHelper.isVariableNameValid(name)) {
            name = VariableNameHelper.convertToValidName(name);
        }
        return name;
    }

    public static double getScalingFactor(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.SCALE_FACTOR_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.SLOPE_ATT_NAME);
        }
        if (attribute == null) {
            attribute = variable.findAttribute("scaling_factor");
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 1.0;
    }

    public static double getAddOffset(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.ADD_OFFSET_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.INTERCEPT_ATT_NAME);
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 0.0;
    }

    // @todo 3 tb/** this is duplicated in optbx - move to suitable location and merge 2025-04-04
    public static Number getAttributeValue(Attribute attribute) {
        if (attribute.isString()) {
            String stringValue = attribute.getStringValue();
            if (stringValue.endsWith("b")) {
                // Special management for bytes; Can occur in e.g. ASCAT files from EUMETSAT
                return Byte.parseByte(stringValue.substring(0, stringValue.length() - 1));
            } else if (!stringValue.isEmpty()) {
                return Double.parseDouble(stringValue);
            } else {
                return 0;
            }
        } else {
            return attribute.getNumericValue();
        }

    }

    public static boolean mustScale(double scalefactor, double offset) {
        if (scalefactor != 1.0) {
            return true;
        }

        return offset != 0.0;
    }

    public static Array scaleArray(Array rawArray, Variable variable) {
        final double scaleFactor = getScalingFactor(variable);
        final double offset = getAddOffset(variable);

        Array result;
        if (mustScale(scaleFactor, offset)) {
            final MAMath.ScaleOffset scaleOffset = new MAMath.ScaleOffset(scaleFactor, offset);
            result = MAMath.convert2Unpacked(rawArray, scaleOffset);
        } else {
            result = rawArray;
        }

        return result;
    }
}

