package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;
import ucar.nc2.jni.netcdf.Nc4Iosp;

import java.io.IOException;
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
        System.out.println("****netcdf_jna_path = " + jna_path);
        Nc4Iosp.setLibraryAndPath(jna_path, null);
    }

    @Override
    public void stop() {
        // empty
    }

}