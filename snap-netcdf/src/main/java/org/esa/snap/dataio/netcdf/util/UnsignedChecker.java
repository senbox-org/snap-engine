package org.esa.snap.dataio.netcdf.util;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

public class UnsignedChecker {

    public static void setUnsignedType(Variable variable) {
        Attribute isUnsigned = variable.findAttributeIgnoreCase("_unsigned");
        if (isUnsigned != null && isUnsigned.getStringValue().equalsIgnoreCase("true")) {
            variable.setDataType(variable.getDataType().withSignedness(DataType.Signedness.UNSIGNED));
            variable.removeAttribute("_Unsigned");
        }
    }
}
