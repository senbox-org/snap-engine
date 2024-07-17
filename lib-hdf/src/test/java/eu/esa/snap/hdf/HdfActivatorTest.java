package eu.esa.snap.hdf;


import com.bc.ceres.annotation.STTM;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.snap.core.util.SystemUtils;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

public class HdfActivatorTest {

    private static void execCmd(String... cmd) {
        System.out.println("[HDF-AT debug]: execCmd:" + Arrays.toString(cmd));
        try {
            final ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true);
            final Process p = builder.start();
            final BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (true) {
                final String line = r.readLine();
                if (line == null) {
                    break;
                }
                System.out.println("[HDF-AT debug]: execCmd output:" + line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[HDF-AT debug]: execCmd end");
    }

    private static void delete(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> ps = Files.list(path)) {
                for (Path pi : ps.collect(Collectors.toSet()).toArray(new Path[0])) {
                    delete(pi);
                }
            }
        }
        System.out.println("[HDF-AT debug]: delete:" + path);
        try {
            Files.delete(path);
        } catch (Exception e) {
            System.out.println("[HDF-AT debug]: fail to delete:" + path + " Reason: " + e);
        }
    }

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
        if (arch.equalsIgnoreCase("x86_64")) {//only Mac x86_64
            execCmd("rm", "-rf", "" + p.getParent());
        }
        try {
            delete(p.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
