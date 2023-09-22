package eu.esa.snap.hdf;


import com.bc.ceres.annotation.STTM;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdflib.HDFLibrary;
import org.junit.Test;

import static org.junit.Assert.fail;

public class HdfActivatorTest {

    @Test
    @STTM("SNAP-3546")
    public void testActivate() {
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
