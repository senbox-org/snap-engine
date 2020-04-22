package org.esa.snap.core.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Helper methods for native libraries registration.
 *
 * @author Cosmin Cara
 * @since 8.0.0
 */
public class NativeLibraryUtils {
    private static final String ENV_LIB_PATH = "java.library.path";

    public static void registerNativePaths(Path... paths) {
        if (paths == null || paths.length == 0)
            return;
        registerNativePaths(Arrays.stream(paths).map(Path::toString).collect(Collectors.toList()).toArray(new String[paths.length]));
    }

    public static void registerNativePaths(String... paths) {
        if (paths == null || paths.length == 0)
            return;
        String propertyValue = System.getProperty(ENV_LIB_PATH);
        StringBuilder builder = new StringBuilder();
        for (String path : paths) {
            if (!StringUtils.isNullOrEmpty(propertyValue) &&
                    (!propertyValue.contains(path) ||
                            propertyValue.contains(path + File.separator))) {
                builder.append(path).append(File.pathSeparator);
            }
        }
        if (!StringUtils.isNullOrEmpty(propertyValue)) {
            propertyValue = builder.toString() + propertyValue;
        } else {
            propertyValue = builder.toString();
        }
        System.setProperty(ENV_LIB_PATH, propertyValue);
        try {
            java.lang.reflect.Method initializePathMethod = PrivilegedAccessor.getMethod(ClassLoader.class, "initializePath", new Class[]{String.class});
            initializePathMethod.setAccessible(true);
            String[] updatedUsrPaths = (String[]) initializePathMethod.invoke(null, ENV_LIB_PATH);
            PrivilegedAccessor.setStaticValue(ClassLoader.class, "usr_paths", updatedUsrPaths);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads library either from a JAR archive, or from file system
     * The file from JAR is copied into system temporary directory and then loaded.
     *
     * @param path        The path from which the load is attempted
     * @param libraryName The name of the library to be loaded (without extension)
     * @throws IOException If temporary file creation or read/write operation fails
     */
    public static void loadLibrary(String path, String libraryName) throws IOException {
        path = URLDecoder.decode(path, "UTF-8");
        String mappedLibName = System.mapLibraryName(libraryName);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        path = path.replace('/', File.separatorChar);
        if (path.contains(".jar")) {
            int contentsSeparatorIndex = path.indexOf("!");
            String jarPath = path.substring(0, contentsSeparatorIndex);
            JarFile jarFile = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            JarEntry jarEntry = null;
            while (entries.hasMoreElements()) {
                JarEntry currentEntry = entries.nextElement();
                if (org.apache.commons.lang.StringUtils.containsIgnoreCase(currentEntry.getName(), libraryName)) {
                    jarEntry = currentEntry;
                    break;
                }
            }
            if (jarEntry == null) {
                throw new IOException(String.format("Library %s could not be found in the jar file %s", libraryName, path));
            }
            try (InputStream in = jarFile.getInputStream(jarEntry)) {
                Path tmpPath = Paths.get(System.getProperty("java.io.tmpdir"), "lib", getOSFamily(), mappedLibName);
                try (OutputStream out = FileUtils.openOutputStream(tmpPath.toFile())) {
                    IOUtils.copy(in, out);
                }
                path = tmpPath.toAbsolutePath().toString();
            }
        } else {
            if (!Files.exists(Paths.get(path, mappedLibName))) {
                throw new IOException(String.format("Library %s could not be found in %s", mappedLibName, path));
            }
        }
        registerNativePaths(path);
        System.loadLibrary(libraryName);
    }

    public static String getOSFamily() {
        String ret;
        String sysName = System.getProperty("os.name").toLowerCase();
        String sysArch = System.getProperty("os.arch").toLowerCase();
        if (sysName.contains("windows")) {
            if (sysArch.contains("amd64") || sysArch.contains("x86_x64")) {
                ret = "win64";
            } else {
                ret = "win32";
            }
        } else if (sysName.contains("linux")) {
            ret = "linux";
        } else if (sysName.contains("mac")) {
            ret = "macosx";
        } else {
            throw new NotImplementedException();
        }
        return ret;
    }
}
