package org.esa.snap.engine_utilities.util;

import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Map;

/**
 * Created by jcoravu on 4/4/2019.
 */
public class ZipFileSystemBuilder {

    private static final ZipFileSystemProvider ZIP_FILE_SYSTEM_PROVIDER = getZipFileSystemProvider();
    private static final Constructor<ZipFileSystem> ZIP_FILE_SYSTEM_CONSTRUCTOR;

    static {
        try {
            Constructor<ZipFileSystem> constructor = ZipFileSystem.class.getDeclaredConstructor(ZipFileSystemProvider.class, Path.class, Map.class);
            constructor.setAccessible(true);
            ZIP_FILE_SYSTEM_CONSTRUCTOR = constructor;
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private ZipFileSystemBuilder() {
    }

    private static ZipFileSystemProvider getZipFileSystemProvider() {
        for (FileSystemProvider fsr : FileSystemProvider.installedProviders()) {
            if (fsr instanceof ZipFileSystemProvider)
                return (ZipFileSystemProvider) fsr;
        }
        throw new FileSystemNotFoundException("The zip file system provider is not installed!");
    }

    public static FileSystem newZipFileSystem(Path zipPath) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (zipPath.getFileSystem() instanceof ZipFileSystem) {
            throw new IllegalArgumentException("Can't create a ZIP file system nested in a ZIP file system. (" + zipPath + " is nested in " + zipPath.getFileSystem() + ")");
        }
        return ZIP_FILE_SYSTEM_CONSTRUCTOR.newInstance(ZIP_FILE_SYSTEM_PROVIDER, zipPath, Collections.emptyMap());
    }

    public static Path buildZipEntryPath(Path zipArchiveRoot, String zipEntryPath) {
        String fileSystemSeparator = zipArchiveRoot.getFileSystem().getSeparator();
        String childRelativePath = FileSystemUtils.replaceFileSeparator(zipEntryPath, fileSystemSeparator);

        String rootAsString = zipArchiveRoot.toString();
        if (childRelativePath.startsWith(rootAsString)) {
            return zipArchiveRoot.getFileSystem().getPath(childRelativePath);
        }
        if (childRelativePath.startsWith(fileSystemSeparator)) {
            childRelativePath = childRelativePath.substring(fileSystemSeparator.length());
        }
        return zipArchiveRoot.resolve(childRelativePath);
    }
}
