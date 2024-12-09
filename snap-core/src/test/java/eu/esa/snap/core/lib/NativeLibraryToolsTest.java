package eu.esa.snap.core.lib;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.io.IOException;
import static org.junit.Assert.fail;

public class NativeLibraryToolsTest {

    @Test
    @STTM("SNAP-3729")
    public void testLoad() {
        try {
            NativeLibraryTools.copyLoaderLibrary(NativeLibraryTools.NETCDF_NATIVE_LIBRARIES_ROOT);
        } catch (IOException e) {
            fail("Loader could not be copied to install directory!");
        }
    }
}
