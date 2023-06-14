package org.esa.snap.engine_utilities.commons;

import org.esa.snap.engine_utilities.util.AllFilesVisitor;
import org.esa.snap.engine_utilities.util.FileSystemUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by jcoravu on 3/4/2019.
 */
public class VirtualDirPath extends AbstractVirtualPath {

    private final Path dirPath;

    public VirtualDirPath(Path dirPath, boolean copyFilesOnLocalDisk) {
        super(copyFilesOnLocalDisk);

        this.dirPath = dirPath;
    }

    @Override
    public Path buildPath(String first, String... more) {
        return this.dirPath.getFileSystem().getPath(first, more);
    }

    @Override
    public String getFileSystemSeparator() {
        return this.dirPath.getFileSystem().getSeparator();
    }

    @Override
    public String getBasePath() {
        return this.dirPath.toString();
    }

    @Override
    public File getBaseFile() {
        return this.dirPath.toFile();
    }

    @Override
    public FilePathInputStream getInputStream(String childRelativePath) throws IOException {
        Path child = this.dirPath.resolve(childRelativePath);
        if (Files.exists(child)) {
            // the child exists
            return getBufferedInputStream(child, null);
        } else {
            throw new FileNotFoundException(child.toString());
        }
    }

    @Override
    public FilePathInputStream getInputStreamIgnoreCaseIfExists(String childRelativePath) throws IOException {
        Path fileToReturn = findFileIgnoreCase(this.dirPath, childRelativePath);
        if (fileToReturn != null) {
            // the child exists
            return getBufferedInputStream(fileToReturn, null);
        }
        return null;
    }

    @Override
    public File getFile(String childRelativePath) throws IOException {
        Path child = this.dirPath.resolve(childRelativePath);
        if (Files.exists(child)) {
            Path fileToReturn = copyFileOnLocalDiskIfNeeded(child, childRelativePath);
            return fileToReturn.toFile();
        } else {
            throw new FileNotFoundException(child.toString());
        }
    }

    @Override
    public FilePath getFilePath(String childRelativePath) throws IOException {
        Path child = this.dirPath.resolve(childRelativePath);
        if (Files.exists(child)) {
            // the child exists
            return new FilePath(child, null);
        } else {
            throw new FileNotFoundException(child.toString());
        }
    }

    @Override
    public String[] list(String childRelativePath) throws IOException {
        Path child = this.dirPath;
        if (childRelativePath != null) {
            child = this.dirPath.resolve(childRelativePath);
        }
        if (Files.exists(child)) {
            if (Files.isDirectory(child)) {
                List<String> files = new ArrayList<String>();
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(child)) {
                    for (Path currentPath : directoryStream) {
                        files.add(currentPath.getFileName().toString());
                    }
                    return files.toArray(new String[files.size()]);
                }
            } else {
                throw new NotDirectoryException(child.toString());
            }
        } else {
            throw new FileNotFoundException(child.toString());
        }
    }

    @Override
    public boolean exists(String childRelativePath) {
        Path child = this.dirPath;
        if (childRelativePath != null) {
            child = this.dirPath.resolve(childRelativePath);
        }
        return Files.exists(child);
    }

    @Override
    public Path getFileIgnoreCaseIfExists(String childRelativePath) throws IOException {
        Path fileToReturn = findFileIgnoreCase(this.dirPath, childRelativePath);
        if (fileToReturn != null) {
            // the child exists
            return copyFileOnLocalDiskIfNeeded(fileToReturn, childRelativePath);
        }
        return null;
    }

    @Override
    public String[] listAllFiles() throws IOException {
        AllFilesVisitor filesVisitor = new AllFilesVisitor();
        Files.walkFileTree(this.dirPath, filesVisitor);
        TreeSet<String> filePaths = filesVisitor.getFilePaths();
        return filePaths.toArray(new String [filePaths.size()]);
    }

    @Override
    public boolean isCompressed() {
        return false;
    }

    @Override
    public boolean isArchive() {
        return false;
    }

    private static Path buildChildPath(Path parentDirPath, String childRelativePath) {
        String fileSystemSeparator = parentDirPath.getFileSystem().getSeparator();
        String relativePath = FileSystemUtils.replaceFileSeparator(childRelativePath, fileSystemSeparator);
        if (relativePath.startsWith(fileSystemSeparator)) {
            relativePath = relativePath.substring(fileSystemSeparator.length());
        }
        return parentDirPath.resolve(relativePath);
    }

    public static Path findFileIgnoreCase(Path parentDirPath, String childRelativePath) throws IOException {
        Path childPathToFind = buildChildPath(parentDirPath, childRelativePath);
        FindChildItemVisitor findChildFileVisitor = new FindChildItemVisitor(childPathToFind);
        Files.walkFileTree(parentDirPath, findChildFileVisitor);
        if (findChildFileVisitor.getExistingChildPath() != null) {
            return findChildFileVisitor.getExistingChildPath();
        }
        return null;
    }
}
