package org.esa.snap.dataio.netcdf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.DummyProductBuilder;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.SystemUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.Assert.*;

public class NetCdfActivatorTest {

    String arch = System.getProperty("os.arch");
    String osName = System.getProperty("os.name");

    @Before
    public void setUp() {
        NetCdfActivator.activated.set(false);
    }

    @AfterClass
    public static void afterClass() {
        // This ensures that the classloader is garbage collected and thus the
        // netcdf native libs are unloaded again tb 2024-10-28
        System.gc();
    }

    @Test
    @STTM("SNAP-3729")
    public void testActivateNativeLibraries() {

        String nativeLibrary;
        String lowercaseOSName = this.osName.toLowerCase();

        if (lowercaseOSName.contains("windows")) {
            nativeLibrary = "netcdf.dll";
        } else if (lowercaseOSName.contains("linux")) {
            nativeLibrary = "libnetcdf.so";
        } else if (lowercaseOSName.contains("mac")) {
            nativeLibrary = "libnetcdf.dylib";
        } else {
            fail("OS is not supported!");
            return;
        }

        try {
            // check if files are copied to install directory
            Path expectedLib = getExectedLibraryPath(this.arch, nativeLibrary);
            NetCdfActivator.activate();
            assertTrue(Files.exists(expectedLib));
        } catch (Exception e) {
            fail("Native libraries could not be copied to auxdata directory: " + e.getMessage());
        }

        try {
            // check native functionality
            checkNativeFunctionality();
        } catch (IOException e) {
            fail("Native Library " + nativeLibrary + " could not be loaded: " + e.getMessage());
        }
    }


    public Path getExectedLibraryPath(String arch, String nativeLibrary) {

        final ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(NetCdfActivator.class);
        assert moduleMetadata != null;
        String version = moduleMetadata.getVersion();

        final Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("netcdf_natives").resolve(version);
        final Path jna_path = auxdataDirectory.toAbsolutePath().resolve(arch);

        return Path.of(jna_path.toString()).resolve(nativeLibrary);
    }

    public void checkNativeFunctionality() throws IOException {
        DummyProductBuilder pb = new DummyProductBuilder();
        pb.size(DummyProductBuilder.Size.SMALL);
        pb.gc(DummyProductBuilder.GC.PER_PIXEL);
        pb.gcOcc(DummyProductBuilder.GCOcc.UNIQUE);
        pb.sizeOcc(DummyProductBuilder.SizeOcc.SINGLE);
        Product product = pb.create();
        product.getBand("latitude").setName("lat");
        product.getBand("longitude").setName("lon");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("crs", "EPSG:4326");
        Product reprojectProduct = GPF.createProduct("Reproject", parameters, product);

        File nc4testFile = File.createTempFile("nc4test", ".nc");
        ProductIO.writeProduct(reprojectProduct, nc4testFile.getAbsolutePath(), "NetCDF4-CF");
        reprojectProduct.dispose();
        product.dispose();
    }

}