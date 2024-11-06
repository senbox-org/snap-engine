package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.lib.NativeLibraryClassLoader;
import eu.esa.snap.core.lib.NativeLibraryTools;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;
import ucar.nc2.jni.netcdf.Nc4Iosp;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetCdfActivator implements Activator {

    public static AtomicBoolean activated = new AtomicBoolean(false);

    public static void activate() {

        if (!activated.getAndSet(true)) {
            final Path auxdataDirectory = installResourceFiles();
            if (auxdataDirectory == null){
                SystemUtils.LOG.severe("NetCDF configuration error: failed to retrieve auxdata path");
                return;
            }

            final String arch = System.getProperty("os.arch").toLowerCase();
            final Path jna_path = auxdataDirectory.toAbsolutePath().resolve(arch);

            try {
                String nativeLibraryRoot = NativeLibraryTools.NETCDF_NATIVE_LIBRARIES_ROOT;
                NativeLibraryTools.copyLoaderLibrary(nativeLibraryRoot);

                final URL loaderLibraryUrl = NativeLibraryTools.getLoaderLibraryFilePath(nativeLibraryRoot).toUri().toURL();

                try( NativeLibraryClassLoader nativeLibraryClassLoader = new NativeLibraryClassLoader(new URL[]{loaderLibraryUrl}, new Path[]{jna_path})) {
                    final Method loaderMethod = nativeLibraryClassLoader.loadClass(NativeLibraryTools.NATIVE_LOADER_LIBRARY_JAR).getMethod("loadNativeLibrary", Path.class);

                    switch (arch) {
                        case "amd64" -> {
                            String sysName = System.getProperty("os.name").toLowerCase();

                            if (sysName.contains("windows")) {
                                // windows
                                // the order of loading is important! tb 2024-09-10
                                loaderMethod.invoke(null, jna_path.resolve("libcurl.dll"));
                                loaderMethod.invoke(null, jna_path.resolve("zlib1.dll"));
                                loaderMethod.invoke(null, jna_path.resolve("hdf5.dll"));
                                loaderMethod.invoke(null, jna_path.resolve("hdf5_hl.dll"));
                                loaderMethod.invoke(null, jna_path.resolve("netcdf.dll"));

                            } else {
                                // linux
                                // the order of loading is important! tb 2024-09-10
                                loaderMethod.invoke(null, jna_path.resolve("libcurl.so.4"));
                                loaderMethod.invoke(null, jna_path.resolve("libz.so.1"));
                                loaderMethod.invoke(null, jna_path.resolve("libhdf5.so.9.0.0"));
                                loaderMethod.invoke(null, jna_path.resolve("libhdf5_hl.so.9.0.0"));
                                loaderMethod.invoke(null, jna_path.resolve("libnetcdf.so"));
                            }
                        }
                        case "x86_64" -> {
                            // mac intel
                            // the order of loading is important! tb 2024-09-10
                            loaderMethod.invoke(null, jna_path.resolve("libapple_nghttp2.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libcurl.4.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libz.1.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libsz.2.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libhdf5.200.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libhdf5_hl.200.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libnetcdf.dylib"));
                        }
                        case "aarch64" -> {
                            // mac arm
                            // the order of loading is important! tb 2024-09-10
                            loaderMethod.invoke(null, jna_path.resolve("libzstd.1.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libsz.2.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libhdf5.310.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libhdf5_hl.310.dylib"));
                            loaderMethod.invoke(null, jna_path.resolve("libnetcdf.dylib"));
                        }
                        default -> throw new IllegalAccessException("Not known system!!");
                    }
                }
            } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }

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