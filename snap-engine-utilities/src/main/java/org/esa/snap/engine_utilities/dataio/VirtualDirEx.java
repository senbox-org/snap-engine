/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.engine_utilities.dataio;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.commons.*;
import org.esa.snap.engine_utilities.util.FileSystemUtils;
import org.esa.snap.engine_utilities.util.ZipFileSystemBuilder;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class extends or alters the features of com.bc.ceres.core.VirtualDir class with a Tar/Tgz implementation
 * and proper methods of retrieving the contents of the virtual directory.
 *
 * @author Cosmin Cara
 */
public abstract class VirtualDirEx extends VirtualDir implements Closeable {

    private static final Logger logger = Logger.getLogger(VirtualDirEx.class.getName());

    private final static HashSet<String> COMPRESSED_EXTENSIONS = new HashSet<String>() {{
        add(".zip");
        add(".tgz");
        add(".gz");
        add(".z");
        add(".tar");
        add(".bz");
        add(".lzh");
        add(".tbz");
    }};

    private int depth;

    protected VirtualDirEx() {

        depth = 1;
    }

    public static VirtualDirEx build(Path path) throws IOException {
        return build(path, false, true);
    }

    public static VirtualDirEx build(Path path, boolean copyFilesFromDirectoryOnLocalDisk, boolean copyFilesFromArchiveOnLocalDisk) throws IOException {
        AbstractVirtualPath virtualDir;
        if (Files.isRegularFile(path)) {
            // the path represents a file
            if (isPackedFile(path)) {
                // the path represents an archive
                String fileName = path.getFileName().toString();
                if (VirtualDirTgz.isTgz(fileName) || VirtualDirEx.isTar(fileName)) {
                    return new VirtualDirTgz(path);
                } else {
                    // check if the file represents a zip archive
                    boolean zipFile;
                    try {
                        zipFile = FileSystemUtils.isZipFile(path);
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                        throw new IllegalStateException(e);
                    }
                    if (zipFile) {
                        virtualDir = new VirtualZipPath(path, copyFilesFromArchiveOnLocalDisk);
                    } else {
                        throw new IllegalArgumentException("The path '" + path + "' does not represent a zip archive. " + getPathClassNameExceptionMessage(path));
                    }
                }
            } else {
                Path parentPath = path.getParent();
                if (parentPath == null) {
                    throw new IllegalArgumentException("Unable to retrieve the parent of the file '" + path + "'. " + getPathClassNameExceptionMessage(path));
                } else if (Files.isDirectory(parentPath)) {
                    virtualDir = new VirtualDirPath(parentPath, copyFilesFromDirectoryOnLocalDisk);
                } else {
                    throw new IllegalArgumentException("Unable to check if the parent of the file '" + path + "' represents a directory. " + getPathClassNameExceptionMessage(path));
                }
            }
        } else if (Files.isDirectory(path)) {
            // the path represents a directory
            virtualDir = new VirtualDirPath(path, copyFilesFromDirectoryOnLocalDisk);
        } else {
            throw new IllegalArgumentException("Unable to check if the path '" + path + "' represents a file or a directory. " + getPathClassNameExceptionMessage(path));
        }
        return new VirtualDirWrapper(virtualDir);
    }

    private static String getPathClassNameExceptionMessage(Path path) {
        return "The path type is '" + path.getClass().getName() + "'.";
    }

    /**
     * Helper method to check if a file is either packed (i.e. tar file) or compressed.
     * The test is performed agains a set of pre-defined file extensions.
     *
     * @param filePath The file to be tested
     * @return <code>true</code> if the file is packed or compressed, {@code false} otherwise
     */
    public static boolean isPackedFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int pointIndex = fileName.lastIndexOf('.');
        if (pointIndex <= 0) {
            return false;
        }
        String extension = fileName.substring(pointIndex);
        return !StringUtils.isNullOrEmpty(extension) && COMPRESSED_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * Checks if the file name belongs to a tar file.
     *
     * @param filename The name of the file to be tested.
     * @return {@code true} if the file is a tar file, <code>false</code> otherwise
     */
    public static boolean isTar(String filename) {
        final String lcName = filename.toLowerCase();
        return lcName.endsWith(".tar");
    }

    public abstract Path buildPath(String first, String... more);

    public abstract String getFileSystemSeparator();

    public abstract FilePathInputStream getInputStream(String path) throws IOException;

    public abstract FilePath getFilePath(String childRelativePath) throws IOException;

    public abstract Path makeLocalTempFolder() throws IOException;

    public void setFolderDepth(int value) {
        depth = value;
    }

    /**
     * Finds the first occurrence of the pattern in the list of files of this instance.
     *
     * @param pattern The pattern to be found.
     * @return The first found entry matching the pattern, or {@code null} if none found.
     * @throws IOException in case of an IO error
     */
    public String findFirst(String pattern) throws IOException {
        String found = null;
        String[] entries = list("");
        if (entries != null) {
            for (String entry : entries) {
                if (entry.toLowerCase().contains(pattern)) {
                    found = entry;
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Finds all the files that contain the given string.
     *
     * @param pattern A string to be found in the file name (if any).
     * @return The array of file names that matched the pattern, or {@code NULL} if no file was found.
     */
    public String[] findAll(String pattern) {
        List<String> found = null;
        String[] entries = listAll();
        if (entries != null) {
            found = Arrays.stream(entries).filter(e -> e.toLowerCase().contains(pattern)).collect(Collectors.toList());
        }
        return found != null ? found.toArray(new String[0]) : null;
    }

    public String[] listAllFilesWithPath() {
        return listAll();
    }

    @Override
    public String[] listAllFiles() throws IOException {
        return listAll();
    }

    /**
     * List all the files contained in this virtual directory instance.
     *
     * @return An array of file names
     */
    public String[] listAll(Pattern... patterns) {
        File baseFile = getBaseFile();
        if (VirtualDirEx.isTar(baseFile.getPath()) || VirtualDirTgz.isTgz(baseFile.getPath())) {
            return listAll();
        } else {
            List<String> filesAndFolders;
            try {
                if (isArchive()) {
                    filesAndFolders = listFilesFromZipArchive(baseFile, patterns);
                } else {
                    filesAndFolders = listFilesFromFolder(baseFile, patterns);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e); // cannot open zip file/folder, list will be empty
                filesAndFolders = new ArrayList<>();
            }
            return filesAndFolders.toArray(new String[0]);
        }
    }

    private List<String> listFilesFromZipArchive(File baseFile, Pattern... patterns) throws IOException {
        List<String> filesAndFolders = new ArrayList<>();
        try (FileSystem fileSystem = ZipFileSystemBuilder.newZipFileSystem(baseFile.toPath())) {
            for (Path root : fileSystem.getRootDirectories()) {
                FileVisitor<Path> visitor = new ListFilesAndFolderVisitor() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (root.equals(dir)) {
                            return FileVisitResult.CONTINUE;
                        } else {
                            String zipEntryPath = remoteFirstSeparatorIfExists(root.relativize(dir).toString());
                            if (matchFilters(zipEntryPath, patterns)) {
                                filesAndFolders.add(zipEntryPath);
                                return FileVisitResult.CONTINUE;
                            }
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }

                    private String remoteFirstSeparatorIfExists(String zipEntryPath) {
                        if (zipEntryPath.startsWith("/")) {
                            return zipEntryPath.substring(1);
                        }
                        return zipEntryPath;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String zipEntryPath = remoteFirstSeparatorIfExists(file.toString());
                        if (isTar(zipEntryPath)) {
                            File temporaryFile = getFile(zipEntryPath);
                            try {
                                VirtualDirTgz innerTar = new VirtualDirTgz(temporaryFile.toPath()) {
                                    @Override
                                    public void close() {
                                        // do nothing
                                    }
                                };
                                innerTar.ensureUnpacked(getTempDir());
                                String[] innerFiles = innerTar.listAll();
                                for (String innerFile : innerFiles) {
                                    if (matchFilters(innerFile, patterns)) {
                                        filesAndFolders.add(innerFile);
                                    }
                                }
                            } finally {
                                temporaryFile.delete();
                            }
                        } else {
                            if (matchFilters(zipEntryPath, patterns)) {
                                filesAndFolders.add(zipEntryPath);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                };
                Files.walkFileTree(root, visitor);
            }
        }
        return filesAndFolders;
    }

    private List<String> listFilesFromFolder(File parent, Pattern... filters) throws IOException {
        List<String> filesAndFolders = new ArrayList<>();
        Path parentPath = parent.toPath();
        FileVisitor<Path> visitor = new ListFilesAndFolderVisitor() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (parentPath.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                } else {
                    String relativePath = parentPath.relativize(dir).toString();
                    if (matchFilters(relativePath, filters)) {
                        filesAndFolders.add(relativePath);
                        return FileVisitResult.CONTINUE;
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String relativePath = parentPath.relativize(file).toString();
                if (matchFilters(relativePath, filters)) {
                    filesAndFolders.add(relativePath);
                }
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(parentPath, visitor);
        return filesAndFolders;
    }

    public static boolean matchFilters(String fileNameToCheck, Pattern... filters) {
        return filters.length == 0 || Arrays.stream(filters).anyMatch(p -> p.matcher(fileNameToCheck).matches());
    }
}
