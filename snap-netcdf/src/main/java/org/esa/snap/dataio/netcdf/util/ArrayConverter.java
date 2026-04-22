package org.esa.snap.dataio.netcdf.util;

import ucar.ma2.Array;
import ucar.ma2.DataType;


public interface ArrayConverter {

    ArrayConverter IDENTITY = array -> array;

    ArrayConverter LSB = array -> {
        final Array convertedArray = Array.factory(DataType.INT, array.getShape());
        for (int i = 0; i < convertedArray.getSize(); i++) {
            convertedArray.setInt(i, (int) (array.getLong(i) & 0x00000000FFFFFFFFL));
        }
        return convertedArray;
    };

    ArrayConverter MSB = array -> {
        final Array convertedArray = Array.factory(DataType.INT, array.getShape());
        for (int i = 0; i < convertedArray.getSize(); i++) {
            convertedArray.setInt(i, (int) (array.getLong(i) >>> 32));
        }
        return convertedArray;
    };

    ArrayConverter UINTCONVERTER = array -> {
        final Array convertedArray = Array.factory(DataType.FLOAT, array.getShape());
        for (int i = 0; i < convertedArray.getSize(); i++) {
            convertedArray.setFloat(i, array.getFloat(i));
        }
        return convertedArray;
    };

    Array convert(Array array);
}
