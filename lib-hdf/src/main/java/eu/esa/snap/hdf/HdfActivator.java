package eu.esa.snap.hdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.NativeLibraryUtils;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class HdfActivator implements Activator {

    public static AtomicBoolean activated = new AtomicBoolean(false);

    public static void activate() {
        if (!activated.getAndSet(true)) {
            final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(HdfActivator.class).resolve("lib");
            final ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(HdfActivator.class);

            String version = "unknown";
            if (moduleMetadata != null) {
                version = moduleMetadata.getVersion();
            }

            final Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("hdf_natives").resolve(version);
            System.out.println("[HDF-AT debug]: HdfActivator.activate auxdataDirectory:"+auxdataDirectory);
            final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);

            try {
                SystemUtils.LOG.fine("installing HDF resources from " + sourceDirPath + " into " + auxdataDirectory);
                resourceInstaller.install(".*", ProgressMonitor.NULL);
            } catch (IOException e) {
                System.out.println("[HDF-AT debug]: Native libraries for HDF could not be extracted to:"+auxdataDirectory+" Reason: "+e.getMessage());
                SystemUtils.LOG.severe("Native libraries for HDF could not be extracted to " + auxdataDirectory);
                throw new IllegalStateException(e);
            }

            String arch = System.getProperty("os.arch").toLowerCase();
            String jna_path = auxdataDirectory.toAbsolutePath().resolve(arch).toString();
            String javaLibPath = System.getProperty("java.library.path");
            if (javaLibPath == null || !javaLibPath.contains(jna_path)) {
                NativeLibraryUtils.registerNativePaths(jna_path);
            }
            System.out.println("[HDF-AT debug]: hdf_jna_path:"+jna_path);
            SystemUtils.LOG.fine("****hdf_jna_path = " + jna_path);
        }
    }

    @Override
    public void start() {
        activate();
    }

    @Override
    public void stop() {
        // nothing to do here 2023-09-22 TB
    }
}
