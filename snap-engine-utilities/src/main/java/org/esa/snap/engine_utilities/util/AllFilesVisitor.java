package org.esa.snap.engine_utilities.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.TreeSet;

/**
 * Created by jcoravu on 26/11/2019.
 */
public class AllFilesVisitor extends SimpleFileVisitor<Path> {

    private final TreeSet<String> filePaths;

    public AllFilesVisitor() {
        this.filePaths = new TreeSet<>();
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        this.filePaths.add(file.toString());
        return FileVisitResult.CONTINUE;
    }

    public TreeSet<String> getFilePaths() {
        return this.filePaths;
    }
}
