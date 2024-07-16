package eu.esa.snap.hdf;


import com.bc.ceres.annotation.STTM;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.snap.core.util.SystemUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

public class HdfActivatorTest {

    @Test
    @STTM("SNAP-3553")
    public void testActivate() {
        System.out.println("[HDF-AT debug]: testActivate start");
        // skip this test on ARM CPUs, we're not supporting these for now tb 2023-10-12
        final String arch = System.getProperty("os.arch").toLowerCase();
        System.out.println("[HDF-AT debug]: testActivate arch:" + arch);
        if (arch.equals("aarch64")) {
            System.out.println("HdfActivatorTest: skipping on ARM CPU");
            return;
        }

        Path p = SystemUtils.getAuxDataPath();
        if (Files.exists(p)) {
            System.out.println("HdfActivatorTest: testActivate Files.exists: " + p);
        } else {
            System.out.println("HdfActivatorTest: testActivate !Files.exists: " + p);
        }
        p = p.resolve("hdf_natives");
        if (Files.exists(p)) {
            System.out.println("HdfActivatorTest: testActivate Files.exists: " + p);
        } else {
            System.out.println("HdfActivatorTest: testActivate !Files.exists: " + p);
        }
        try (Stream<Path> ps = Files.list(p)) {
            boolean empty = true;
            for (Path pi : ps.collect(Collectors.toSet()).toArray(new Path[0])) {
                empty = false;
                System.out.println("HdfActivatorTest: testActivate pi: " + pi);
            }
            if (empty) {
                System.out.println("HdfActivatorTest: testActivate Files.delete: " + p);
                Files.delete(p);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        System.out.println("[HDF-AT debug]: testActivate end");
    }
}
