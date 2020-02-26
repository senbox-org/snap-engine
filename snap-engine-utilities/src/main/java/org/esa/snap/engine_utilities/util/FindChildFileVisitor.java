package org.esa.snap.engine_utilities.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by jcoravu on 28/11/2019.
 */
public class FindChildFileVisitor extends SimpleFileVisitor<Path> {

    private final Path childFileToFind;

    private Path existingChildFile;

    public FindChildFileVisitor(Path childFileToFind) {
        this.childFileToFind = childFileToFind;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toString().equalsIgnoreCase(this.childFileToFind.toString())) {
            this.existingChildFile = file;
            return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
    }

    public Path getExistingChildFile() {
        return this.existingChildFile;
    }
}
