package org.esa.snap.dataio.netcdf.util;

import org.esa.snap.dataio.netcdf.NetCdfActivator;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NetcdfFileOpenerNativeNc4Test {

    @Test
    public void testOpenNativeNc4ReadsNetcdf4VariableAndAttributes() throws Exception {
        NetCdfActivator.activate();
        final File file = createNetcdf4File();

        try (NetcdfFile netcdfFile = NetcdfFileOpener.openNativeNc4(file)) {
            assertNotNull(netcdfFile);
            assertEquals("native-nc4-test", netcdfFile.findGlobalAttribute("title").getStringValue());

            final Variable variable = netcdfFile.findVariable("data");
            assertNotNull(variable);
            final Attribute units = variable.findAttribute("units");
            assertNotNull(units);
            assertEquals("count", units.getStringValue());

            final Array array = variable.read(new Section(new int[]{1, 0}, new int[]{1, 3}));
            assertArrayEquals(new int[]{10, 11, 12}, (int[]) array.get1DJavaArray(DataType.INT));
        } finally {
            file.delete();
        }
    }

    @Test(expected = IOException.class)
    public void testOpenNativeNc4WithMissingFileThrowsIOException() throws IOException {
        NetcdfFileOpener.openNativeNc4(new File("missing-native-nc4-file.nc"));
    }

    private static File createNetcdf4File() throws IOException, InvalidRangeException {
        final File file = File.createTempFile("NetcdfFileOpenerNativeNc4Test", ".nc");
        final NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, file.getAbsolutePath());

        writer.addDimension("y", 2);
        writer.addDimension("x", 3);
        final Variable variable = writer.addVariable("data", DataType.INT, "y x");
        writer.addGlobalAttribute("title", "native-nc4-test");
        variable.addAttribute(new Attribute("units", "count"));
        writer.create();

        try {
            writer.write(variable, Array.factory(DataType.INT, new int[]{2, 3}, new int[]{0, 1, 2, 10, 11, 12}));
        } finally {
            writer.close();
        }
        return file;
    }
}
