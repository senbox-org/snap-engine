package eu.esa.snap.hdf;

import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.lib.NativeLibraryTools;
import hdf.hdf5lib.H5;
import hdf.hdflib.HDFLibrary;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class HDFLoader implements Activator {

    public static AtomicBoolean loaded = new AtomicBoolean(false);

    private static Boolean hdf5LibAvailable = null;
    private static final Logger logger = Logger.getLogger(HDFLoader.class.getName());


    /**
     * Ensures that the HDF5 native library is loaded and initialised
     */
    public static void ensureHDF5Initialised() {
        if (hdf5LibAvailable == null) {
            synchronized (logger) {
                hdf5LibAvailable = checkAndInitHdf5Lib();
            }
        }
        if (!hdf5LibAvailable) {
            throw new IllegalStateException("HDF5 NOT initialised! Check log for details.");
        }
    }

    /**
     * Checks whether the HDF5 native library is loaded in SNAP and initialise it
     *
     * @return {@code true} when the HDF5 native library is loaded in SNAP
     */
    private static boolean checkAndInitHdf5Lib() {
        setupHDFNativeLibrary();
        return loadHDFNativeLibrary();
    }

    private static boolean loadHDFNativeLibrary(){
        try {
            HDFLibrary.loadH4Lib();
            H5.loadH5Lib();
            return true;
        }catch (Throwable error) {
            logger.warning(MessageFormat.format("{0}: HDF-5 library not available: {1}: {2}", HDFLoader.class, error.getClass(), error.getMessage()));
            logger.warning("HDF/HDF5 dependent readers disabled.");
            return false;
        }
    }

    private static void setupHDFNativeLibrary() {
        if (!loaded.getAndSet(true)) {
            final Path auxdataDirectory = installHDFNativeLibrary();
            if (auxdataDirectory == null){
                SystemUtils.LOG.severe("HDF configuration error: failed to retrieve auxdata path");
                return;
            }

            final String arch = System.getProperty("os.arch").toLowerCase();
            final String sysName = System.getProperty("os.name").toLowerCase();
            Path jna_path = auxdataDirectory.toAbsolutePath().resolve(arch);

            String hdf4Library = null;
            String hdf5Library = null;

            try {
                String nativeLibraryRoot = NativeLibraryTools.HDF_NATIVE_LIBRARIES_ROOT;
                NativeLibraryTools.copyLoaderLibrary(nativeLibraryRoot);

                if (arch.equals("amd64")) {
                    if (sysName.contains("windows")) {
                        System.load(jna_path.resolve("hdf.dll").toString());
                        System.load(jna_path.resolve("mfhdf.dll").toString());
                        System.load(jna_path.resolve("hdf5.dll").toString());
                        hdf4Library = "hdf_java.dll";
                        hdf5Library = "hdf5_java.dll";
                    } else {
                        // linux
                        hdf4Library = "libhdf_java.so";
                        hdf5Library = "libhdf5_java.so";
                    }
                } else if (sysName.contains("mac")) {
                    // mac intel and arm
                    jna_path = auxdataDirectory.toAbsolutePath().resolve("macosx");
                    hdf4Library = "libhdf_java.dylib";
                    hdf5Library = "libhdf5_java.dylib";
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

    private static Path installHDFNativeLibrary() {
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(HDFLoader.class).resolve("lib");
        final ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(HDFLoader.class);

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
        setupHDFNativeLibrary();
    }

    @Override
    public void stop() {
        // nothing to do here 2023-09-22 TB
    }
}
