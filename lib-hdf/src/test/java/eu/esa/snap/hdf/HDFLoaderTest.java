package eu.esa.snap.hdf;


import com.bc.ceres.annotation.STTM;
import hdf.hdf5lib.H5;
import hdf.hdflib.HDFLibrary;
import org.junit.Test;

import static org.junit.Assert.fail;

public class HDFLoaderTest {

    @Test
    @STTM("SNAP-3553")
    public void testActivate() {
        HDFLoader.ensureHDF5Initialised();

        try {
            H5.H5open();
            H5.H5close();
        } catch (Exception e) {
            fail("HDF5 native lib not initialized");
        }

        try {
            HDFLibrary.HDdont_atexit();
        } catch (Exception e) {
            fail("HDF4 native lib not initialized");
        }
    }
}
