package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.io.IOUtils;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;
import ucar.nc2.jni.netcdf.Nc4Iosp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class NetCdfActivator implements Activator {

    @Override
    public void start() {
        Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(getClass()).resolve("lib");
        ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(getClass());

        String version = "unknown";
        if (moduleMetadata != null) {
            version = moduleMetadata.getVersion();
        }

        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("netcdf_natives").resolve(version);
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);

        try {
            System.out.println("installing NetCDF resources from " + sourceDirPath + " into " + auxdataDirectory);
            resourceInstaller.install(".*", ProgressMonitor.NULL);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Native libraries for NetCDF could not be extracted to" + auxdataDirectory);
            return;
        }

        String arch = System.getProperty("os.arch").toLowerCase();
        String jna_path = auxdataDirectory.toAbsolutePath().resolve(arch).toString();
        String javaLibPath = System.getProperty("java.library.path");
        if (javaLibPath == null || ! javaLibPath.contains(jna_path)) {
            if (javaLibPath == null) {
                javaLibPath = jna_path;
            } else {
                javaLibPath += ":" + jna_path;
            }
            System.setProperty("java.library.path", javaLibPath);
            // trigger re-initialisation before loading libraries
            try {
                Field field = ClassLoader.class.getDeclaredField("sys_paths");
                field.setAccessible(true);
                field.set(null, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            Process process = new ProcessBuilder("bash", "-c",
                                                 "export LD_LIBRARY_PATH="+auxdataDirectory.toAbsolutePath().resolve(arch).toString()+":$LD_LIBRARY_PATH;" +
                                                         "/usr/bin/ldd "+auxdataDirectory.toAbsolutePath().resolve(arch).toString() + "/libnetcdf.so").start();
            String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            String stderr = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
            System.out.println("ldd libnetcdf.so stderr returns: " + stderr);
            System.out.println("ldd libnetcdf.so stdout returns: " + output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("****netcdf_jna_path = " + jna_path);
        Nc4Iosp.setLibraryAndPath(jna_path, null);
    }

    @Override
    public void stop() {
        // empty
    }

}