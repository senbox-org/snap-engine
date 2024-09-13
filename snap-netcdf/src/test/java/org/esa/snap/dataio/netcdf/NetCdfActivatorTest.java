package org.esa.snap.dataio.netcdf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.SystemUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class NetCdfActivatorTest {

    String arch = System.getProperty("os.arch");
    String osName = System.getProperty("os.name");

    @Before
    public void setUp() {
        NetCdfActivator.activated.set(false);
    }

    @Test
    @STTM("SNAP-3729")
    public void testActivateForAmd64Windows() {

        if (!this.osName.toLowerCase().contains("windows")) return;

        String nativeLibrary = "netcdf.dll";

        try {
            // check if files are copied to install directory
            Path expectedLib = getExectedLibraryPath(this.arch, nativeLibrary);
            NetCdfActivator.activate();
            assertTrue(Files.exists(expectedLib));

            // TODO belu: test if systen.load was successful

        } catch (Exception e) {
            fail("Activation failed for AMD64 on Windows: " + e.getMessage());
        }
    }

    @Test
    @STTM("SNAP-3729")
    public void testActivateForAmd64Linux() {

        if (!this.osName.toLowerCase().contains("linux")) return;

        String nativeLibrary = "libcurl.so.4";

        try {
            // check if files are copied to install directory
            Path expectedLib = getExectedLibraryPath(this.arch, nativeLibrary);
            assertTrue(Files.exists(expectedLib));

            // TODO belu: test if systen.load was successful

        } catch (Exception e) {
            fail("Activation failed for AMD64 on Linux: " + e.getMessage());
        }
    }

    @Test
    @STTM("SNAP-3729")
    public void testActivateForAarch64MacOs() {

        if (!this.osName.toLowerCase().contains("mac") && !this.arch.contains("aarch64")) return;

        String nativeLibrary = "libzstd.1.dylib";

        try {
            // check if files are copied to install directory
            Path expectedLib = getExectedLibraryPath(this.arch, nativeLibrary);
            assertTrue(Files.exists(expectedLib));

        // TODO belu: test if systen.load was successful

        } catch (Exception e) {
            fail("Activation failed for Aarch64 on MacOs: " + e.getMessage());
        }
    }

    @Test
    @STTM("SNAP-3729")
    public void testActivateForX86_64MacOs() {

        if (!this.osName.toLowerCase().contains("mac") && !this.arch.contains("x86_64")) return;

        String nativeLibrary = "libzstd.1.dylib";

        try {
            // check if files are copied to install directory
            Path expectedLib = getExectedLibraryPath(this.arch, nativeLibrary);
            assertTrue(Files.exists(expectedLib));

            // TODO belu: test if systen.load was successful

        } catch (Exception e) {
            fail("Activation failed for X86_64 on MacOs: " + e.getMessage());
        }
    }



    public Path getExectedLibraryPath(String arch, String nativeLibrary) throws IOException {

        final ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(NetCdfActivator.class);
        assert moduleMetadata != null;
        String version = moduleMetadata.getVersion();

        final Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("netcdf_natives").resolve(version);
        final Path jna_path = auxdataDirectory.toAbsolutePath().resolve(arch);

        return Path.of(jna_path.toString()).resolve(nativeLibrary);
    }

}