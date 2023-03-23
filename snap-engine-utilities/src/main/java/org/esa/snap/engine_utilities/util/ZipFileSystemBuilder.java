package org.esa.snap.engine_utilities.util;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Created by jcoravu on 4/4/2019.
 */
public class ZipFileSystemBuilder {

    private ZipFileSystemBuilder() {
    }

    public static FileSystem newZipFileSystem(Path zipPath) throws IOException {
        return FileSystems.newFileSystem(zipPath, null);
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
