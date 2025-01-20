package org.esa.snap.engine_utilities.commons;

import com.bc.ceres.core.VirtualDir;
import eu.esa.snap.core.lib.FileHelper;
import eu.esa.snap.core.lib.NotRegularFileException;
import org.esa.snap.engine_utilities.file.AbstractFile;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Created by jcoravu on 9/4/2019.
 */
public abstract class AbstractVirtualPath extends VirtualDir {

    private static final Logger logger = Logger.getLogger(AbstractVirtualPath.class.getName());

    private final boolean copyFilesOnLocalDisk;

    private File localTempDir;

    protected AbstractVirtualPath(boolean copyFilesOnLocalDisk) {
        this.copyFilesOnLocalDisk = copyFilesOnLocalDisk;
    }

    public abstract Path buildPath(String first, String... more);

    public abstract String getFileSystemSeparator();

    public abstract Path getFileIgnoreCaseIfExists(String relativePath) throws IOException;

    public abstract FilePathInputStream getInputStreamIgnoreCaseIfExists(String relativePath) throws IOException;

    public abstract FilePathInputStream getInputStream(String path) throws IOException;

    public abstract FilePath getFilePath(String childRelativePath) throws IOException;

    @Override
    public void close() {
        cleanup();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        cleanup();
    }

    @Override
    public File getTempDir() throws IOException {
        return this.localTempDir;
    }

    public Path makeLocalTempFolder() throws IOException {
        if (this.localTempDir == null) {
            this.localTempDir = VirtualDir.createUniqueTempDir();
        }
        return this.localTempDir.toPath();
    }

    protected final Path copyFileOnLocalDiskIfNeeded(Path entryPath, String childRelativePath) throws IOException {
        if (this.copyFilesOnLocalDisk && Files.isRegularFile(entryPath)) {
            // copy the file from the zip archive on the local disk
            if (this.localTempDir == null) {
                this.localTempDir = VirtualDir.createUniqueTempDir();
            }
            Path localFilePath = this.localTempDir.toPath().resolve(childRelativePath);
            if (FileHelper.canCopyOrReplaceFile(entryPath, localFilePath)) {
                Path parentFolder = localFilePath.getParent();

                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Copy file '" + entryPath.toString() + "' to local folder '" + parentFolder.toString() + "'.");
                }

                if (!Files.exists(parentFolder)) {
                    Files.createDirectories(parentFolder);
                }
                FileHelper.copyFileUsingInputStream(entryPath, localFilePath.toString(), AbstractFile.BUFFER_SIZE);
            }
            return localFilePath;
        } else {
            // do not copy the file from the zip archive on the local disk
            return entryPath;
        }
    }

    private void cleanup() {
        if (this.localTempDir != null) {
            deleteFileTree(this.localTempDir);
            this.localTempDir = null;
        }
    }

    protected static FilePathInputStream getBufferedInputStream(Path child, Closeable closeable) throws IOException {
        if (Files.isRegularFile(child)) {
            // the child is a file
            InputStream inputStream = Files.newInputStream(child);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, AbstractFile.BUFFER_SIZE);
            InputStream inputStreamToReturn;
            if (child.toString().endsWith(".gz")) {
                inputStreamToReturn = new GZIPInputStream(bufferedInputStream);
            } else {
                inputStreamToReturn = bufferedInputStream;
            }
            return new FilePathInputStream(child, inputStreamToReturn, closeable);
        } else {
            throw new NotRegularFileException(child.toString());
        }
    }

    protected static class FindChildItemVisitor extends SimpleFileVisitor<Path> {

        private final Path childPathToFind;

        private Path existingChildPath;

        public FindChildItemVisitor(Path childPathToFind) {
            this.childPathToFind = childPathToFind;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return checkItem(dir);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return checkItem(file);
        }

        private FileVisitResult checkItem(Path currentPath) {
            if (currentPath.toString().equalsIgnoreCase(this.childPathToFind.toString())) {
                this.existingChildPath = currentPath;
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }

        Path getExistingChildPath() {
            return this.existingChildPath;
        }
    }
}
