package eu.esa.snap.hdf;


import com.bc.ceres.annotation.STTM;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdflib.HDFLibrary;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.fail;

public class HdfActivatorTest {

    @Test
    @STTM("SNAP-3553")
    @Ignore("Related to NativeLibraryUtils")
    public void testActivate() {
        // skip this test on ARM CPUs, we're not supporting these for now tb 2023-10-12
        final String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.equals("aarch64")) {
            System.out.println("HdfActivatorTest: skipping on ARM CPU");
            return;
        }

        HdfActivator.activate();

        try {
            H5.H5open();
            H5.H5close();
        } catch (Exception e) {
            fail("HDF5 native lib not initialized");
        }

        try {
            HDFLibrary.loadH4Lib();
        } catch (Exception e) {
            fail("HDF4 native lib not initialized");
        }
    }
}
