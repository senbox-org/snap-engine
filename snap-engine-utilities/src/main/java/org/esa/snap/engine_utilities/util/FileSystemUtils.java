package org.esa.snap.engine_utilities.util;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Created by jcoravu on 26/11/2019.
 */
public class FileSystemUtils {

    private FileSystemUtils() {
    }

    public static TreeSet<String> listAllFilePaths(FileSystem fileSystem) throws IOException {
        AllFilesVisitor filesVisitor = new AllFilesVisitor();
        Iterator<Path> itRootDirectories = fileSystem.getRootDirectories().iterator();
        while (itRootDirectories.hasNext()) {
            Path zipArchiveRoot = itRootDirectories.next();
            Files.walkFileTree(zipArchiveRoot, filesVisitor);
        }
        return filesVisitor.getFilePaths();
    }
}
