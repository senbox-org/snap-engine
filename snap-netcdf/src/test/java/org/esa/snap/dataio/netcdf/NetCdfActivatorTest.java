package org.esa.snap.dataio.netcdf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class NetCdfActivatorTest {

    String originalArch = System.getProperty("os.arch");
    String originalOsName = System.getProperty("os.name");

    @Before
    public void setUp() {
        NetCdfActivator.activated.set(false);
    }

    @After
    public void tearDown() {
        System.setProperty("os.arch", originalArch);
        System.setProperty("os.name", originalOsName);
    }

    @Test
    @STTM("SNAP-3729")
    public void testActivateForAmd64Windows() {

        if (!originalOsName.toLowerCase().contains("windows")) return;

        String nativeLibrary = "netcdf.dll";
        String arch = "amd64";
        System.setProperty("os.arch", arch);
        System.setProperty("os.name", "Windows");

        try {
            // check if files are copied to install directory
            Path expectedLib = getExectedLibraryPath(arch, nativeLibrary);
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

        if (!originalOsName.toLowerCase().contains("linux")) return;

        String nativeLibrary = "libcurl.so.4";
        String arch = "amd64";
        System.setProperty("os.arch", arch);
        System.setProperty("os.name", "Linux");

        try {
            // check if files are copied to install directory
            Path expectedLib = getExectedLibraryPath(arch, nativeLibrary);
            assertTrue(Files.exists(expectedLib));

            // TODO belu: test if systen.load was successful

        } catch (Exception e) {
            fail("Activation failed for AMD64 on Linux: " + e.getMessage());
        }
    }

    @Test
    @STTM("SNAP-3729")
    public void testActivateForAarch64MacOs() {

        if (!originalOsName.toLowerCase().contains("mac") && !originalArch.contains("aarch64")) return;

        String nativeLibrary = "libzstd.1.dylib";
        String arch = "aarch64";
        System.setProperty("os.arch", arch);

        try {
            // check if files are copied to install directory
            Path expectedLib = getExectedLibraryPath(arch, nativeLibrary);
            assertTrue(Files.exists(expectedLib));

        // TODO belu: test if systen.load was successful

        } catch (Exception e) {
            fail("Activation failed for Aarch64 on MacOs: " + e.getMessage());
        }
    }

    @Test
    @STTM("SNAP-3729")
    public void testActivateForX86_64MacOs() {

        if (!originalOsName.toLowerCase().contains("mac") && !originalArch.contains("x86_64")) return;

        String nativeLibrary = "libzstd.1.dylib";
        String arch = "aarch64";
        System.setProperty("os.arch", arch);

        try {
            // check if files are copied to install directory
            Path expectedLib = getExectedLibraryPath(arch, nativeLibrary);
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