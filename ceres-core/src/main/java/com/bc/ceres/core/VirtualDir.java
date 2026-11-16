/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.core;

import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.util.CleanUpState;
import com.bc.ceres.util.CleanerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// todo - fully support "." and ".." directories.

/**
 * A read-only directory that can either be a directory in the file system or a ZIP-file.
 * Files having '.gz' extensions are automatically decompressed.
 *
 * @author Norman Fomferra
 * @since Ceres 0.11
 */
public abstract class VirtualDir {

    private static final Logger LOG = LoggerFactory.getLogger(VirtualDir.class);

    /**
     * @return The base path name of the directory.
     */
    public abstract String getBasePath();

    /**
     * @return The Path object of the directory.
     */
    public abstract Path getBaseFile();

    /**
     * Opens a reader for the given relative path.
     *
     * @param path The relative file path.
     * @return A reader for the specified relative path.
     * @throws IOException If the file does not exist or if it can't be opened for reading.
     */
    public Reader getReader(String path) throws IOException {
        return new InputStreamReader(getInputStream(path));
    }

    /**
     * Opens an input stream for the given relative file path.
     * Files having '.gz' extensions are automatically decompressed.
     *
     * @param path The relative file path.
     * @return An input stream for the specified relative path.
     * @throws IOException If the file does not exist or if it can't be opened for reading.
     */
    public abstract InputStream getInputStream(String path) throws IOException;

    /**
     * Gets the file for the given relative path.
     *
     * @param path The relative file or directory path.
     * @return Gets the file or directory for the specified file path.
     * @throws IOException If the file or directory does not exist or if it can't be extracted from a ZIP-file.
     */
    public abstract Path getFile(String path) throws IOException;

    /**
     * Returns an array of strings naming the files and directories in the
     * directory denoted by the given relative directory path.
     * <p>
     * There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * @param path The relative directory path.
     * @return An array of strings naming the files and directories in the
     * directory denoted by the given relative directory path.
     * The array will be empty if the directory is empty.
     * @throws IOException If the directory given by the relative path does not exists.
     */
    public abstract String[] list(String path) throws IOException;

    public abstract boolean exists(String path);

    /**
     * Returns an array of strings naming the relative directory path
     * of all file names.
     * <p>
     * There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * @return An array of strings naming the relative directory path
     * and filename to all files.
     * The array will be empty if the directory is empty.
     * @throws IOException If an I/O error is thrown when accessing the virtual dir.
     */
    public abstract String[] listAllFiles() throws IOException;

    /**
     * Closes access to this virtual directory.
     */
    public abstract void close();

    public static VirtualDir create(Path inputPath) {
        if (Files.isDirectory(inputPath)) {
            return new Dir(inputPath);
        }

        try {
            return new Zip(inputPath);
        } catch (IOException ignored) {
            return null;
        }
    }

    public abstract boolean isCompressed();

    public abstract boolean isArchive();

    public Path getTempDir() throws IOException {
        return null;
    }

    private static class Dir extends VirtualDir {

        private final Path dir;

        private Dir(Path file) {
            dir = file;
        }

        @Override
        public String getBasePath() {
            return dir.toString();
        }

        @Override
        public Path getBaseFile() {
            return dir;
        }

        @Override
        public InputStream getInputStream(String path) throws IOException {
            final InputStream inputStream = new BufferedInputStream(Files.newInputStream(getFile(path)));
            if (path.endsWith(".gz")) {
                return new GZIPInputStream(inputStream);
            }
            return inputStream;
        }

        @Override
        public Path getFile(String path) throws IOException {
            Path child = dir.resolve(path);
            if (!Files.exists(child)) {
                throw new FileNotFoundException(child.toString());
            }
            return child;
        }

        @Override
        public String[] list(String path) throws IOException {
            final Path child = getFile(path);
            return IOHelper.list(child).toArray(new String[0]);
        }

        @Override
        public boolean exists(String path) {
            try {
                final Path child = getFile(path);
                return Files.exists(child);
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public String[] listAllFiles() throws IOException {
            try (Stream<Path> pathStream = Files.walk(dir)) {
                final Stream<Path> filteredstream = pathStream.filter(Files::isRegularFile);
                final int baseLength = dir.toUri().toString().length();
                final Stream<String> relativeFilesStream = filteredstream.map(path -> path.toUri().toString().substring(baseLength));
                return relativeFilesStream.toArray(String[]::new);
            }
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isCompressed() {
            return false;
        }

        @Override
        public boolean isArchive() {
            return false;
        }
    }

    private static class Zip extends VirtualDir {

        private static final int BUFFER_SIZE = 4 * 1024 * 1024;
        private final ZipResourceState resources;

        private Zip(Path zipFile) throws IOException {
            this.resources = new ZipResourceState(new ZipFile(zipFile.toFile()));
            CleanerRegistry.getInstance().register(this, resources);
        }

        @Override
        public String getBasePath() {
            return requireZipFile().getName();
        }

        @Override
        public Path getBaseFile() {
            return Path.of(requireZipFile().getName());
        }

        @Override
        public InputStream getInputStream(String path) throws IOException {
            return getInputStream(getEntry(path));
        }

        @Override
        public Path getFile(String path) throws IOException {

            ZipEntry zipEntry = getEntry(path);

            if (resources.tempZipFileDir == null) {
                resources.tempZipFileDir = VirtualDir.createUniqueTempDir();
            }

            Path tempFile = resources.tempZipFileDir.resolve(zipEntry.getName());
            if (Files.exists(tempFile)) {
                return tempFile;
            }

            // System.out.println("Extracting ZIP-entry to " + tempFile);
            if (zipEntry.isDirectory()) {
                IOHelper.createDirectories(tempFile);
            } else {
                unzip(zipEntry, tempFile);
            }

            return tempFile;
        }

        @Override
        public String[] list(String path) throws IOException {
            if (".".equals(path) || path.isEmpty()) {
                path = "";
            } else if (!path.endsWith("/")) {
                path += "/";
            }
            boolean dirSeen = false;
            TreeSet<String> nameSet = new TreeSet<>();
            Enumeration<? extends ZipEntry> enumeration = requireZipFile().entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String name = zipEntry.getName();
                if (name.startsWith(path)) {
                    int i1 = path.length();
                    int i2 = name.indexOf('/', i1);
                    String entryName;
                    if (i2 == -1) {
                        entryName = name.substring(i1);
                    } else {
                        entryName = name.substring(i1, i2);
                    }
                    if (!entryName.isEmpty() && !nameSet.contains(entryName)) {
                        nameSet.add(entryName);
                    }
                    dirSeen = true;
                }
            }
            if (!dirSeen) {
                throw new FileNotFoundException(getBasePath() + "!" + path);
            }
            return nameSet.toArray(new String[nameSet.size()]);
        }

        @Override
        public boolean exists(String path) {
            try {
                ZipEntry zipEntry = getEntry(path);
                return zipEntry != null;
            } catch (FileNotFoundException e) {
                return false;
            }
        }

        @Override
        public String[] listAllFiles() {
            List<String> filenameList = new ArrayList<>();
            Enumeration<? extends ZipEntry> enumeration = requireZipFile().entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                if (!zipEntry.isDirectory()) {
                    filenameList.add(zipEntry.getName());
                }
            }
            return filenameList.toArray(new String[0]);
        }

        @Override
        public void close() {
            CleanerRegistry.getInstance().cleanup(this);
        }

        @Override
        public boolean isCompressed() {
            return true;
        }

        @Override
        public boolean isArchive() {
            return true;
        }

        @Override
        public Path getTempDir() throws IOException {
            return resources.tempZipFileDir;
        }


        private InputStream getInputStream(ZipEntry zipEntry) throws IOException {
            InputStream inputStream = requireZipFile().getInputStream(zipEntry);
            if (zipEntry.getName().endsWith(".gz")) {
                return new GZIPInputStream(inputStream);
            }
            return inputStream;
        }

        private ZipEntry getEntry(String path) throws FileNotFoundException {
            ZipEntry zipEntry = requireZipFile().getEntry(path);
            if (zipEntry == null) {
                throw new FileNotFoundException(requireZipFile().getName() + "!" + path);
            }
            return zipEntry;
        }

        private void unzip(ZipEntry zipEntry, Path tempFile) throws IOException {
            try (InputStream is = requireZipFile().getInputStream(zipEntry)) {
                if (is != null) {
                    IOHelper.createDirectories(tempFile.getParent());
                    try (BufferedOutputStream os = new BufferedOutputStream(Files.newOutputStream(tempFile), BUFFER_SIZE)) {
                        byte[] bytes = new byte[1024];
                        int n;
                        while ((n = is.read(bytes)) > 0) {
                            os.write(bytes, 0, n);
                        }
                    } catch (IOException ioe) {
                        throw new IOException("Failed to unzip '" + zipEntry.getName() + "'to '" + tempFile.toAbsolutePath() + "'", ioe);
                    }
                }
            }
        }

        private ZipFile requireZipFile() {
            ZipFile zipFile = resources.zipFile;
            if (zipFile == null) {
                throw new IllegalStateException("ZIP file already closed");
            }
            return zipFile;
        }

        private static final class ZipResourceState implements CleanUpState {
            private ZipFile zipFile;
            private Path tempZipFileDir;

            private ZipResourceState(ZipFile zipFile) {
                this.zipFile = zipFile;
            }

            @Override
            public synchronized void run() {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException ignore) {}
                    zipFile = null;
                }
                if (tempZipFileDir != null) {
                    IOHelper.deleteFileTree(tempZipFileDir);
                    tempZipFileDir = null;
                }
            }
        }
    }

    private static class NIO extends VirtualDir {

        private final Path virtualDirPath;
        private final String basePath;
        private final boolean isCompressed;
        private final boolean isArchive;
        private final boolean localCopyRequired;
        private final NioCleanupState nioResourceState = new NioCleanupState();

        private NIO(Path virtualDirPath, String basePath, boolean isCompressed, boolean isArchive, boolean localCopyRequired) {
            this.virtualDirPath = virtualDirPath;
            this.basePath = basePath;
            this.isCompressed = isCompressed;
            this.isArchive = isArchive;
            this.localCopyRequired = localCopyRequired;

            CleanerRegistry.getInstance().register(this, nioResourceState);
        }

        @Override
        public String getBasePath() {
            return basePath;
        }

        @Override
        public Path getBaseFile() {
            return virtualDirPath;
        }

        @Override
        public InputStream getInputStream(String path) throws IOException {
            Path resolve = virtualDirPath.resolve(path);
            if (Files.exists(resolve)) {
                InputStream inputStream = Files.newInputStream(resolve);
                if (path.endsWith(".gz")) {
                    return new GZIPInputStream(new BufferedInputStream(inputStream));
                }
                return new BufferedInputStream(inputStream);
            } else {
                throw new FileNotFoundException(resolve.toString());
            }
        }

        @Override
        public synchronized Path getFile(String path) throws IOException {
            Path resolve = virtualDirPath.resolve(path);
            if (!Files.exists(resolve)) {
                throw new FileNotFoundException(resolve.toString());
            }

            if (localCopyRequired) {
                if (nioResourceState.tempZipFilePathStore == null) {
                    nioResourceState.tempZipFilePathStore = VirtualDir.createUniqueTempDir();
                }
                Path tempFilePath = nioResourceState.tempZipFilePathStore.resolve(path);
                if (Files.notExists(tempFilePath)) {
                    if (Files.notExists(tempFilePath.getParent())) {
                        Files.createDirectories(tempFilePath.getParent());
                    }
                    Files.copy(resolve, tempFilePath);
                }
                return tempFilePath;
            } else {
                return resolve;
            }
        }


        @Override
        public String[] list(String path) throws IOException {
            Path startingPath = virtualDirPath.resolve(path);
            if (!Files.exists(startingPath)) {
                throw new FileNotFoundException(startingPath.toString());
            }
            List<String> fileNames = new ArrayList<>();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(startingPath)) {
                for (Path child : directoryStream) {
                    String filename = child.getFileName().toString();
                    if (filename.endsWith("/") && filename.length() > 0) {
                        filename = filename.substring(0, filename.length() - 1);
                    }
                    fileNames.add(filename);
                }
            }
            return fileNames.toArray(new String[fileNames.size()]);
        }

        @Override
        public String[] listAllFiles() throws IOException {
            try (Stream<Path> pathStream = Files.walk(virtualDirPath)) {
                Stream<Path> filteredstream = pathStream.filter(new Predicate<Path>() {
                    @Override
                    public boolean test(Path path) {
                        return Files.isRegularFile(path);
                    }
                });
                final int baseLength = virtualDirPath.toUri().toString().length();
                Stream<String> fileStream = filteredstream.map(new Function<Path, String>() {
                    @Override
                    public String apply(Path path) {
                        return path.toUri().toString().substring(baseLength);
                    }
                });
                return fileStream.toArray(new IntFunction<String[]>() {
                    @Override
                    public String[] apply(int value) {
                        return new String[value];
                    }
                });
            }
        }

        @Override
        public boolean exists(String path) {
            return Files.exists(virtualDirPath.resolve(path));
        }

        @Override
        public void close() {
            CleanerRegistry.getInstance().cleanup(this);
        }

        @Override
        public boolean isCompressed() {
            return isCompressed;
        }

        @Override
        public boolean isArchive() {
            return isArchive;
        }

        @Override
        public Path getTempDir() {
            return nioResourceState.tempZipFilePathStore;
        }


        private static final class NioCleanupState implements CleanUpState {
            private Path tempZipFilePathStore;

            @Override
            public synchronized void run() {
                if (tempZipFilePathStore != null) {
                    IOHelper.deleteFileTree(tempZipFilePathStore);
                    tempZipFilePathStore = null;
                }
            }
        }
    }

    private static final int TEMP_DIR_ATTEMPTS = 10000;

    public static Path createUniqueTempDir() throws IOException {
        return createUniqueTempDir("");
    }

    public static Path createUniqueTempDir(String name) throws IOException {
        Path baseDir = getBaseTempDir();
        String baseName = name + System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            Path tempDir = baseDir.resolve(baseName + counter);
            if (IOHelper.createDirectory(tempDir)) {
                return tempDir;
            }
        }
        throw new IllegalStateException("Failed to create directory within "
                + TEMP_DIR_ATTEMPTS + " attempts (tried "
                + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
    }

    public static void clearTempFiles() throws IOException {
        final Path baseDir = getBaseTempDir();
        IOHelper.deleteFileTree(baseDir);
    }

    private static Path getBaseTempDir() throws IOException {
        String contextId = "snap";
        Path tempDir;
        String tempDirName = System.getProperty("java.io.tmpdir");
        if (tempDirName != null) {
            tempDir = Path.of(tempDirName);
            if (Files.exists(tempDir)) {
                String userName = System.getProperty("user.name");
                tempDir = tempDir.resolve(contextId + "-" + userName);
                IOHelper.createDirectory(tempDir);
            }
        } else {
            tempDir = Path.of(System.getProperty("user.home", "."), "." + contextId + "/temp");
            IOHelper.createDirectories(tempDir);
        }
        if (!Files.exists(tempDir)) {
            throw new IOException("Temporary directory not available: " + tempDir);
        }
        return tempDir;
    }

}
