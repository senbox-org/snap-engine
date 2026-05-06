package org.esa.snap.dataio.netcdf.util;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static org.junit.Assert.*;


public class ArrayConverterTest {


    @Test
    @STTM("SNAP-4149")
    public void testIdentity() {
        final ArrayConverter converter = ArrayConverter.IDENTITY;

        final Array toConvert = Array.factory(DataType.INT, new int[]{5, 6});
        final Array converted = converter.convert(toConvert);

        assertSame(toConvert, converted);
    }

    @Test
    @STTM("SNAP-4149")
    public void testLsb() {
        final ArrayConverter converter = ArrayConverter.LSB;

        final Array toConvert = Array.factory(DataType.ULONG, new int[]{2, 2}, new long[]{1, 2, 3, 4} );
        final Array converted = converter.convert(toConvert);

        assertEquals(DataType.INT, converted.getDataType());
        assertEquals(4, converted.getSize());
        assertEquals(1, converted.getInt(0));
        assertEquals(4, converted.getInt(3));
    }

    @Test
    @STTM("SNAP-4149")
    public void testMsb() {
        final ArrayConverter converter = ArrayConverter.MSB;

        final Array toConvert = Array.factory(DataType.ULONG, new int[]{2, 2}, new long[]{100000000001L, 200000000002L, 300000000003L, 400000000004L} );
        final Array converted = converter.convert(toConvert);

        assertEquals(DataType.INT, converted.getDataType());
        assertEquals(4, converted.getSize());
        assertEquals(23, converted.getInt(0));
        assertEquals(93, converted.getInt(3));
    }

    @Test
    @STTM("SNAP-4149")
    public void testUint() {
        final ArrayConverter converter = ArrayConverter.UINTCONVERTER;

        final Array toConvert = Array.factory(DataType.UINT, new int[]{2, 2}, new int[]{100000001, 200000002, 300000003, 400000004} );
        final Array converted = converter.convert(toConvert);

        assertEquals(DataType.FLOAT, converted.getDataType());
        assertEquals(4, converted.getSize());
        assertEquals(100000000, converted.getFloat(0), 1e-8);
        assertEquals(400000000, converted.getFloat(3), 1e-8);
    }
}