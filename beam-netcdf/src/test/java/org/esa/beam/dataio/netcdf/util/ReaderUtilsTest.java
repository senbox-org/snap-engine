package org.esa.beam.dataio.netcdf.util;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import static org.junit.Assert.*;

public class ReaderUtilsTest {

    @Test
    public void testGetVariableName() throws Exception {
        NetcdfFile netcdfFile = null;
        try {
            netcdfFile = NetcdfFileOpener.open(getClass().getResource("test_orig_name.nc").toString());
            Band band = new Band("sun angle", ProductData.TYPE_INT16, 10, 10);
            String variableName = ReaderUtils.getVariableName(netcdfFile, band);
            assertEquals("sun_angle", variableName);
        } finally {
            if (netcdfFile != null) {
                netcdfFile.close();
            }
        }
    }

    @Test
    public void testGetVariableName_2() throws Exception {
        NetcdfFile netcdfFile = null;
        try {
            netcdfFile = NetcdfFileOpener.open(getClass().getResource("test_orig_name.nc").toString());
            Band band = new Band("longitude", ProductData.TYPE_INT16, 10, 10);
            String variableName = ReaderUtils.getVariableName(netcdfFile, band);
            assertEquals("longitude", variableName);
        } finally {
            if (netcdfFile != null) {
                netcdfFile.close();
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetVariableName_exception() throws Exception {
        NetcdfFile netcdfFile = null;
        try {
            netcdfFile = NetcdfFileOpener.open(getClass().getResource("test_orig_name.nc").toString());
            Band band = new Band("kaputtnick", ProductData.TYPE_INT16, 10, 10);
            ReaderUtils.getVariableName(netcdfFile, band);
        } finally {
            if (netcdfFile != null) {
                netcdfFile.close();
            }
        }
    }
}