package org.esa.snap.dataio.netcdf.isin;

import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdf4WriterPlugIn;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.junit.Test;

import java.io.IOException;

//@Ignore
public class IsinTileWriteTest {

    @Test
    public void testWrite1KM() throws IOException {
        final CfNetCdf4WriterPlugIn plugIn = new CfNetCdf4WriterPlugIn();

        final NFileWriteable writable = plugIn.createWritable("D:\\Satellite\\DELETE\\test_isin.nc");

    }
}
