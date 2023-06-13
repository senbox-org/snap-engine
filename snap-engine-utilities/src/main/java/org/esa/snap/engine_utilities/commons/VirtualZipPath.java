package org.esa.snap.engine_utilities.commons;

import org.esa.snap.engine_utilities.util.AllFilesVisitor;
import org.esa.snap.engine_utilities.util.ZipFileSystemBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by jcoravu on 3/4/2019.
 */
public class VirtualZipPath extends AbstractVirtualPath {

    private final Path zipPath;

    public VirtualZipPath(Path zipPath, boolean copyFilesOnLocalDisk) {
        super(copyFilesOnLocalDisk);

        this.zipPath = zipPath;
    }

    @Override
    public Path buildPath(String first, String... more) {
        try (FileSystem fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath)) {
            return fileSystem.getPath(first, more);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getFileSystemSeparator() {
        try (FileSystem fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath)) {
            return fileSystem.getSeparator();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getBasePath() {
        return this.zipPath.toString();
    }

    @Override
    public File getBaseFile() {
        return this.zipPath.toFile();
    }

    @Override
    public FilePathInputStream getInputStream(String zipEntryPath) throws IOException {
        boolean success = false;
        FileSystem fileSystem = null;
        try {
            fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath);
            for (Path zipArchiveRoot : fileSystem.getRootDirectories()) {
                Path entryPath = ZipFileSystemBuilder.buildZipEntryPath(zipArchiveRoot, zipEntryPath);
                if (Files.exists(entryPath)) {
                    // the entry exists into the zip archive
                    FilePathInputStream filePathInputStream = getBufferedInputStream(entryPath, fileSystem);
                    success = true;
                    return filePathInputStream;
                }
            }
            throw new FileNotFoundException(getMissingZipEntryExceptionMessage(zipEntryPath));
        } finally {
            if (fileSystem != null && !success) {
                fileSystem.close();
            }
        }
    }

    @Override
    public FilePathInputStream getInputStreamIgnoreCaseIfExists(String zipEntryPath) throws IOException {
        boolean success = true;
        FileSystem fileSystem = null;
        try {
            fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath);
            for (Path zipArchiveRoot : fileSystem.getRootDirectories()) {
                Path entryPathToFind = ZipFileSystemBuilder.buildZipEntryPath(zipArchiveRoot, zipEntryPath);
                FindChildItemVisitor findChildFileVisitor = new FindChildItemVisitor(entryPathToFind);
                Files.walkFileTree(zipArchiveRoot, findChildFileVisitor);
                if (findChildFileVisitor.getExistingChildPath() != null) {
                    // the entry exists into the zip archive
                    return getBufferedInputStream(findChildFileVisitor.getExistingChildPath(), fileSystem);
                }
            }
            success = false;
            return null;
        } finally {
            if (fileSystem != null && !success) {
                fileSystem.close();
            }
        }
    }

    @Override
    public File getFile(String zipEntryPath) throws IOException {
        try (FileSystem fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath)) {
            for (Path zipArchiveRoot : fileSystem.getRootDirectories()) {
                Path entryPath = ZipFileSystemBuilder.buildZipEntryPath(zipArchiveRoot, zipEntryPath);
                if (Files.exists(entryPath)) {
                    // the entry exists into the zip archive
                    Path fileToReturn = copyFileOnLocalDiskIfNeeded(entryPath, zipEntryPath);
                    return fileToReturn.toFile();
                }
            }
            throw new FileNotFoundException(getMissingZipEntryExceptionMessage(zipEntryPath));
        }
    }

    @Override
    public FilePath getFilePath(String zipEntryPath) throws IOException {
        boolean success = false;
        FileSystem fileSystem = null;
        try {
            fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath);
            for (Path zipArchiveRoot : fileSystem.getRootDirectories()) {
                Path entryPath = ZipFileSystemBuilder.buildZipEntryPath(zipArchiveRoot, zipEntryPath);
                if (Files.exists(entryPath)) {
                    // the entry exists into the zip archive
                    success = true;
                    return new FilePath(entryPath, fileSystem);
                }
            }
            throw new FileNotFoundException(getMissingZipEntryExceptionMessage(zipEntryPath));
        } finally {
            if (fileSystem != null && !success) {
                fileSystem.close();
            }
        }
    }

    @Override
    public String[] list(String zipEntryPath) throws IOException {
        try (FileSystem fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath)) {
            for (Path zipArchiveRoot : fileSystem.getRootDirectories()) {
                Path entryPath = zipArchiveRoot;
                if (zipEntryPath != null) {
                    entryPath = ZipFileSystemBuilder.buildZipEntryPath(zipArchiveRoot, zipEntryPath);
                }
                if (Files.exists(entryPath)) {
                    // the zip entry exists
                    if (Files.isDirectory(entryPath)) {
                        List<String> files = new ArrayList<>();
                        String zipFileSystemSeparator = fileSystem.getSeparator();
                        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(entryPath)) {
                            for (Path currentPath : directoryStream) {
                                String fileName = currentPath.getFileName().toString();
                                if (fileName.endsWith(zipFileSystemSeparator)) {
                                    int index = fileName.length() - zipFileSystemSeparator.length();
                                    fileName = fileName.substring(0, index);
                                }
                                files.add(fileName);
                            }
                            return files.toArray(new String[0]);
                        }
                    } else {
                        throw new NotDirectoryException(entryPath.toString());
                    }
                }
            }
            throw new FileNotFoundException(getMissingZipEntryExceptionMessage(zipEntryPath));
        }
    }

    @Override
    public boolean exists(String zipEntryPath) {
        try (FileSystem fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath)) {
            for (Path zipArchiveRoot : fileSystem.getRootDirectories()) {
                Path entryPath = zipArchiveRoot;
                if (zipEntryPath != null) {
                    entryPath = ZipFileSystemBuilder.buildZipEntryPath(zipArchiveRoot, zipEntryPath);
                }
                if (Files.exists(entryPath)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Path getFileIgnoreCaseIfExists(String zipEntryPath) throws IOException {
        try (FileSystem fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath)) {
            for (Path zipArchiveRoot : fileSystem.getRootDirectories()) {
                Path entryPathToFind = ZipFileSystemBuilder.buildZipEntryPath(zipArchiveRoot, zipEntryPath);
                FindChildItemVisitor findChildFileVisitor = new FindChildItemVisitor(entryPathToFind);
                Files.walkFileTree(zipArchiveRoot, findChildFileVisitor);
                if (findChildFileVisitor.getExistingChildPath() != null) {
                    // the entry exists into the zip archive
                    return copyFileOnLocalDiskIfNeeded(findChildFileVisitor.getExistingChildPath(), zipEntryPath);
                }
            }
            return null;
        }
    }

    @Override
    public String[] listAllFiles() throws IOException {
        try (FileSystem fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.zipPath)) {
            AllFilesVisitor filesVisitor = new AllFilesVisitor();
            for (Path zipArchiveRoot : fileSystem.getRootDirectories()) {
                Files.walkFileTree(zipArchiveRoot, filesVisitor);
            }
            TreeSet<String> filePaths = filesVisitor.getFilePaths();
            return filePaths.toArray(new String[0]);
        }
    }

    @Override
    public boolean isCompressed() {
        return true;
    }

    @Override
    public boolean isArchive() {
        return true;
    }

    private String getMissingZipEntryExceptionMessage(String zipEntryPath) {
        return "The zip entry path '" + zipEntryPath + "' does not exist in the zip archive '" + this.zipPath.toString() + "'.";
    }
}
