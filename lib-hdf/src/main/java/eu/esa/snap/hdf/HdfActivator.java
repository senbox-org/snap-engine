package eu.esa.snap.hdf;

import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.lib.NativeLibraryTools;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.snap.core.util.ModuleMetadata;
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
            final Path auxdataDirectory = installResourceFiles();
            if (auxdataDirectory == null){
                SystemUtils.LOG.severe("HDF configuration error: failed to retrieve auxdata path");
                return;
            }

            final String arch = System.getProperty("os.arch").toLowerCase();
            final Path jna_path = auxdataDirectory.toAbsolutePath().resolve(arch);

            String hdf4Library = null;
            String hdf5Library = null;

            try {
                String nativeLibraryRoot = NativeLibraryTools.HDF_NATIVE_LIBRARIES_ROOT;
                NativeLibraryTools.copyLoaderLibrary(nativeLibraryRoot);

                if (arch.equals("amd64")) {
                    String sysName = System.getProperty("os.name").toLowerCase();

                    if (sysName.contains("windows")) {
                        hdf4Library = "jhdf.dll";
                        hdf5Library = "jhdf5.dll";
                    } else {
                        // linux
                        hdf4Library = "libjhdf.so";
                        hdf5Library = "libjhdf5.so";
                    }
                } else if (arch.equals("x86_64")) {
                    // mac intel
                    hdf4Library = "libjhdf.jnilib";
                    hdf5Library = "libjhdf5.jnilib";
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (hdf4Library != null || hdf5Library != null) {
                SystemUtils.LOG.fine("****hdf_jna_path = " + jna_path);
                System.setProperty(HDFLibrary.HDFPATH_PROPERTY_KEY, jna_path.resolve(hdf4Library).toString());
                System.setProperty(H5.H5PATH_PROPERTY_KEY, jna_path.resolve(hdf5Library).toString());
            }
        }
    }

    private static Path installResourceFiles() {
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(HdfActivator.class).resolve("lib");
        final ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(HdfActivator.class);

        String version = "unknown";
        if (moduleMetadata != null) {
            version = moduleMetadata.getVersion();
        }

        final Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("hdf_natives").resolve(version);
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);

        try {
            SystemUtils.LOG.fine("installing HDF resources from " + sourceDirPath + " into " + auxdataDirectory);
            resourceInstaller.install(".*", ProgressMonitor.NULL);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Native libraries for HDF could not be extracted to " + auxdataDirectory);
            return null;
        }
        return auxdataDirectory;
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
