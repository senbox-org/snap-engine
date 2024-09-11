package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.gdal.GDALLoader;
import org.esa.snap.dataio.gdal.GDALLoaderClassLoader;
import org.esa.snap.dataio.gdal.GDALVersion;
import org.esa.snap.runtime.Activator;
import ucar.nc2.jni.netcdf.Nc4Iosp;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.esa.snap.dataio.gdal.GDALLoader.GDAL_NATIVE_LIBRARY_LOADER_CLASS_NAME;

public class NetCdfActivator implements Activator {

    public static AtomicBoolean activated = new AtomicBoolean(false);

    public static void activate() {
        if (!activated.getAndSet(true)) {
            final Path auxdataDirectory = installResourceFiles();
            if (auxdataDirectory == null){
                // @todo 1 tb throw? Log! 2024-09-10
                return;
            }

            final String arch = System.getProperty("os.arch").toLowerCase();
            final Path jna_path = auxdataDirectory.toAbsolutePath().resolve(arch);

            // ----- GDAL stuff -----
            // ----------------------
            try {
                GDALLoader.copyLoaderLibrary();

                final GDALVersion gdalVersion = GDALVersion.getGDALVersion();
                final URL jniLibraryFileUrl = gdalVersion.getJNILibraryFilePath().toUri().toURL();
                final URL loaderLibraryUrl = GDALVersion.getLoaderLibraryFilePath().toUri().toURL();

                final GDALLoaderClassLoader gdalLoaderClassLoader = new GDALLoaderClassLoader(new URL[]{jniLibraryFileUrl, loaderLibraryUrl}, new Path[]{jna_path});
                final Method loaderMethod = gdalLoaderClassLoader.loadClass(GDAL_NATIVE_LIBRARY_LOADER_CLASS_NAME).getMethod("loadNativeLibrary", Path.class);
                // the order of loading is important! tb 2024-09-10
                loaderMethod.invoke(null, jna_path.resolve("libcurl.dll"));
                loaderMethod.invoke(null, jna_path.resolve("zlib1.dll"));
                loaderMethod.invoke(null, jna_path.resolve("hdf5.dll"));
                loaderMethod.invoke(null, jna_path.resolve("hdf5_hl.dll"));
                loaderMethod.invoke(null, jna_path.resolve("netcdf.dll"));
            } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            // ----------------------
            // ----- GDAL stuff -----

            SystemUtils.LOG.fine("****netcdf_jna_path = " + jna_path);
            Nc4Iosp.setLibraryAndPath(jna_path.toString(), null);
        }
    }

    private static Path installResourceFiles() {
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(NetCdfActivator.class).resolve("lib");
        final ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(NetCdfActivator.class);

        String version = "unknown";
        if (moduleMetadata != null) {
            version = moduleMetadata.getVersion();
        }

        final Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("netcdf_natives").resolve(version);
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);

        try {
            SystemUtils.LOG.fine("installing NetCDF resources from " + sourceDirPath + " into " + auxdataDirectory);
            resourceInstaller.install(".*", ProgressMonitor.NULL);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Native libraries for NetCDF could not be extracted to " + auxdataDirectory);
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
        // empty
    }

}