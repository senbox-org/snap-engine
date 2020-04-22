package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.NativeLibraryUtils;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;
import ucar.nc2.jni.netcdf.Nc4Iosp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetCdfActivator implements Activator {

    public static AtomicBoolean activated = new AtomicBoolean(false);

    public static void activate() {
        if (!activated.getAndSet(true)) {
            Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(NetCdfActivator.class).resolve("lib");
            ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(NetCdfActivator.class);

            String version = "unknown";
            if (moduleMetadata != null) {
                version = moduleMetadata.getVersion();
            }

            Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("netcdf_natives").resolve(version);
            final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);

            try {
                SystemUtils.LOG.fine("installing NetCDF resources from " + sourceDirPath + " into " + auxdataDirectory);
                resourceInstaller.install(".*", ProgressMonitor.NULL);
            } catch (IOException e) {
                SystemUtils.LOG.severe("Native libraries for NetCDF could not be extracted to" + auxdataDirectory);
                return;
            }

            String arch = System.getProperty("os.arch").toLowerCase();
            String jna_path = auxdataDirectory.toAbsolutePath().resolve(arch).toString();
            String javaLibPath = System.getProperty("java.library.path");
            if (javaLibPath == null || !javaLibPath.contains(jna_path)) {
                NativeLibraryUtils.registerNativePaths(jna_path);
            }
            SystemUtils.LOG.fine("****netcdf_jna_path = " + jna_path);
            Nc4Iosp.setLibraryAndPath(jna_path, null);
        }
    }

    @Override
    public void start() {
        activate();
    }

    @Override
    public void stop() {
        // empty
    }

}