package org.esa.snap.engine_utilities.dataio;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;

/**
 * Created by jcoravu on 3/6/2019.
 */
public abstract class ListFilesAndFolderVisitor implements FileVisitor<Path> {

    public ListFilesAndFolderVisitor() {
    }

    @Override
    public final FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public final FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}
