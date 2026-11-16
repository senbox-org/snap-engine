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

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CanceledException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class IOHelper {

    public static final String PATH_SEPARATOR = System.getProperty("path.separator");
    public static final String SEPARATOR = FileSystems.getDefault().getSeparator();
    public static final char SEPARATOR_CHAR = SEPARATOR.charAt(0);

    public static String getBaseName(File archiveFile) {
        String dirName = archiveFile.getName();
        int pos = dirName.lastIndexOf((int) '.');
        if (pos > 1) {
            dirName = dirName.substring(0, pos);
        }
        return dirName;
    }

    public static String getFileName(URL url) throws UnsupportedEncodingException {
        return new File(URLDecoder.decode(url.getFile(), "UTF-8")).getName();
    }

    public static void copy(File source, File target, ProgressMonitor pm) throws IOException, CanceledException {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(source));
             OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(target))) {
            String taskName = MessageFormat.format("Copying {0}", source.getName());
            int fileSize = (int) Math.max(source.length(), (long) Integer.MAX_VALUE);
            copy(inputStream, outputStream, taskName, fileSize, pm);
        }
    }

    public static void copy(InputStream inputStream, OutputStream outputStream, String taskName, int fileSize,
                            ProgressMonitor pm) throws IOException, CanceledException {
        pm.beginTask(taskName, fileSize);
        try {
            byte[] buffer = new byte[64 * 1024];
            while (true) {
                checkIOTaskCanceled(pm);
                int n = inputStream.read(buffer);
                if (n > 0) {
                    outputStream.write(buffer, 0, n);
                    pm.worked(n);
                } else if (n < buffer.length) {
                    break;
                }
            }
        } finally {
            pm.done();
        }
    }

    private static void checkIOTaskCanceled(ProgressMonitor pm) throws CanceledException {
        if (pm.isCanceled()) {
            throw new CanceledException();
        }
    }

    public static void createDirectory(File dir) throws IOException {
        if (dir.exists()) {
            return;
        }
        if (!dir.mkdir()) {
            throw new IOException(MessageFormat.format("Failed to create directory ''{0}''", dir));
        }
    }

    public static void copy(URLConnection source, File target, ProgressMonitor pm) throws IOException, CanceledException {
        InputStream inputStream = source.getInputStream();
        try {
            OutputStream outputStream = new FileOutputStream(target);
            try {
                String taskName = MessageFormat.format("Downloading {0}", target.getName());
                int contentLength = source.getContentLength();
                copy(inputStream, outputStream, taskName, contentLength, pm);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
    }

    public static void copy(ZipFile zipFile, ZipEntry source, File target, ProgressMonitor pm) throws IOException, CanceledException {
        InputStream inputStream = zipFile.getInputStream(source);
        try {
            OutputStream outputStream = new FileOutputStream(target);
            try {
                String taskName = MessageFormat.format("Unpacking {0}", source.getName());
                int fileSize = (int) source.getSize();
                copy(inputStream, outputStream, taskName, fileSize, pm);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
    }

    public static List<String> unpack(File sourceZipFile, File targetDir, boolean isNative, ProgressMonitor pm) throws IOException, CanceledException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        Logger logger = Logger.getLogger(System.getProperty("ceres.context", "ceres"));
        final Platform platform = isNative ? Platform.getCurrentPlatform() : null;
        if (platform != null) {
            logger.info(MessageFormat.format("Archive [{0}]: Detected platform {1}{2}",
                                             sourceZipFile, platform.getId(), platform.getBitCount()));
        }

        ZipFile zipFile = new ZipFile(sourceZipFile);
        pm.beginTask(MessageFormat.format("Unpacking {0} to {1}", sourceZipFile.getName(), targetDir.getName()),
                     zipFile.size());
        try {
            ArrayList<String> entries = new ArrayList<String>(zipFile.size());
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

            while (enumeration.hasMoreElements()) {
                checkIOTaskCanceled(pm);
                final ZipEntry zipEntry = enumeration.nextElement();
                final String entryName = zipEntry.getName();
                final File targetFile;

                if (platform != null && Platform.isAnyPlatformDir(entryName)) {
                    if (platform.isPlatformDir(entryName)) {
                        targetFile = new File(targetDir, platform.truncatePlatformDir(entryName));
                        logger.info(MessageFormat.format("Archive [{0}]: Unpacking platform dependent entry [{1}]",
                                                         sourceZipFile, entryName));
                    } else {
                        targetFile = null;
                        logger.fine(MessageFormat.format("Archive [{0}]: Ignoring platform dependent entry [{1}]",
                                                         sourceZipFile, entryName));
                    }
                } else {
                    targetFile = new File(targetDir, entryName);
                }

                if (targetFile != null) {
                    pm.setSubTaskName(entryName);
                    File parentDir = targetFile.getParentFile();
                    if (parentDir != null) {
                        parentDir.mkdirs();
                    }
                    if (zipEntry.isDirectory()) {
                        targetFile.mkdir();
                        pm.worked(1);
                    } else {
                        copy(zipFile, zipEntry, targetFile, SubProgressMonitor.create(pm, 1));
                    }
                    entries.add(entryName);
                }
            }
            return entries;
        } finally {
            pm.done();
            zipFile.close();
        }
    }

    public static List<String> pack(File sourceDir, File targetZipFile, ProgressMonitor pm) throws IOException, CanceledException {
        if (!sourceDir.exists()) {
            throw new FileNotFoundException(sourceDir.getPath());
        }

        DirScanner dirScanner = new DirScanner(sourceDir, true, true);
        String[] entryNames = dirScanner.scan();
        ZipOutputStream zipOutputStream = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(targetZipFile)));
        zipOutputStream.setMethod(ZipEntry.DEFLATED);
        pm.beginTask(MessageFormat.format("Packing {0} into {1}", sourceDir.getName(), targetZipFile.getName()),
                     entryNames.length);

        ArrayList<String> entries = new ArrayList<String>(entryNames.length);
        try {
            for (String entryName : entryNames) {
                checkIOTaskCanceled(pm);
                ZipEntry zipEntry = new ZipEntry(entryName.replace('\\', '/'));

                pm.setSubTaskName(entryName);
                File sourceFile = new File(sourceDir, entryName);
                try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile))) {
                    zipOutputStream.putNextEntry(zipEntry);
                    copy(inputStream, zipOutputStream, entryName, (int) sourceFile.length(),
                         SubProgressMonitor.create(pm, 1));
                    zipOutputStream.closeEntry();
                    entries.add(entryName);
                }
            }
            return entries;
        } finally {
            pm.done();
            zipOutputStream.close();
        }
    }

    /**
     * Creates an empty file on the given path
     *
     * @param file the path of the new file
     * @return {@code true} if the file was successfully created
     * @throws IOException if an I/O error occurs.
     */
    public static boolean createFile(Path file) throws IOException {
        if (Files.exists(file)) {
            return false;
        }
        try {
            Files.createFile(file);
            return Files.exists(file);
        } catch (IOException e) {
            throw new IOException(MessageFormat.format("Failed to create file ''{0}''", file));
        }
    }

    /**
     * Creates a temporary file with the given filename.
     * At the end of the JVM execution, it will be deleted.
     *
     * @param prefix the filename prefix of temporary file
     * @param suffix the filename suffix (extension) of temporary file
     * @return the Path of the temporary file
     */
    public static Path createTempFile(String prefix, String suffix) {
        try {
            final Path tempDir = createATempDir();
            return Files.createTempFile(tempDir, prefix, suffix);
        } catch (IOException e) {
            throw new IllegalStateException(MessageFormat.format("Failed to create temporary file ''{0}''", prefix + suffix));
        }
    }

    /**
     * Creates a new temporary directory with a random name on the SNAP temporary directory
     * At the end of the JVM execution, it will be deleted.
     *
     * @return the path of the new temporary directory
     */
    public static Path createATempDir() {
        return createATempDir("");
    }

    /**
     * Creates a new temporary directory with the given name on the SNAP temporary directory
     * At the end of the JVM execution, it will be deleted.
     *
     * @param name the name of new temporary directory
     * @return the path of the new temporary directory
     */
    public static Path createATempDir(String name) {
        try {
            final Path tempDir = VirtualDir.createUniqueTempDir(name);
            deleteOnExit(tempDir);
            return tempDir;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temporary directory", e);
        }
    }

    /**
     * Creates an empty file with predefined size on the given path
     *
     * @param file     the path of the new file
     * @param fileSize the size of the new file
     * @return {@code true} if the file was successfully created
     * @throws IOException if an I/O error occurs.
     */
    public static boolean createFile(Path file, long fileSize) throws IOException {
        try (FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE)) {
            long currentSize = channel.size();
            if (fileSize < currentSize) {
                channel.truncate(fileSize);
            } else if (fileSize > currentSize) {
                channel.position(fileSize - 1);
                return channel.write(ByteBuffer.wrap(new byte[]{0})) == 1;
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(MessageFormat.format("Failed to create file ''{0}'' with the pre-allocated size ''{1}''", file, fileSize));
        }
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     *
     * @param dir the path of the new directory
     * @return {@code true} if the directory was successfully created
     */
    public static boolean createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
            return Files.exists(dir);
        } catch (IOException e) {
            throw new RuntimeException(MessageFormat.format("Failed to create directories for ''{0}''", dir));
        }
    }

    /**
     * Renames a fie or directory
     *
     * @param source the source file or directory
     * @param dest   the target file or directory
     * @return {@code true} if the file or directory was successfully renamed
     */
    public static boolean rename(Path source, Path dest) {
        if (!Files.exists(source) || Files.exists(dest)) {
            return false;
        }
        try {
            Files.move(source, dest);
            return Files.exists(dest);
        } catch (IOException e) {
            throw new RuntimeException(MessageFormat.format("Failed to rename entry from ''{0}'' to ''{1}''", source, dest));
        }
    }

    /**
     * Set ReadOnly attribute to the given file
     *
     * @param file the target file path
     * @return {@code true} if the ReadOnly attribute was successfully applied to the target file
     */
    public static boolean setReadOnly(Path file) {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            if (Files.getFileAttributeView(file, DosFileAttributeView.class) != null) {
                Files.setAttribute(file, "dos:readonly", true);
            } else {
                final Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
                perms.remove(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(file, perms);
            }
            return !Files.isWritable(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set Executable attribute to the given file
     *
     * @param path       the target file path
     * @param executable the Executable flag for the target file
     * @return {@code true} if the Executable attribute was successfully applied to the target file
     * @throws IOException if an I/O error occurs.
     */
    public static boolean setExecutable(Path path, boolean executable) throws IOException {
        // Try POSIX first (Unix systems)
        final PosixFileAttributeView posixView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posixView != null) {
            setPosixExecutable(posixView, executable);
            return true;
        }
        // Fallback to ACL (Windows systems)
        final AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (aclView != null) {
            setAclExecutable(path, aclView, executable);
            return true;
        }
        // Neither supported
        return false;
    }

    private static void setPosixExecutable(PosixFileAttributeView view, boolean executable) throws IOException {
        final Set<PosixFilePermission> perms = view.readAttributes().permissions();
        if (executable) {
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
        } else {
            perms.remove(PosixFilePermission.OWNER_EXECUTE);
            perms.remove(PosixFilePermission.GROUP_EXECUTE);
            perms.remove(PosixFilePermission.OTHERS_EXECUTE);
        }
        view.setPermissions(perms);
    }

    private static void setAclExecutable(Path path, AclFileAttributeView view, boolean executable) throws IOException {
        final List<AclEntry> acl = view.getAcl();
        final UserPrincipal user = path.getFileSystem()
                .getUserPrincipalLookupService()
                .lookupPrincipalByName(System.getProperty("user.name"));
        final Set<AclEntryPermission> execPerms = EnumSet.of(
                AclEntryPermission.EXECUTE,
                AclEntryPermission.READ_DATA
        );
        final List<AclEntry> updatedAcl = new ArrayList<>();
        boolean found = false;
        for (AclEntry entry : acl) {
            if (entry.principal().equals(user) && entry.type() == AclEntryType.ALLOW) {
                final Set<AclEntryPermission> perms = EnumSet.copyOf(entry.permissions());
                if (executable) {
                    perms.addAll(execPerms);
                } else {
                    perms.removeAll(execPerms);
                }
                entry = AclEntry.newBuilder()
                        .setType(entry.type())
                        .setPrincipal(entry.principal())
                        .setPermissions(perms)
                        .setFlags(entry.flags())
                        .build();
                found = true;
            }
            updatedAcl.add(entry);
        }
        // Add entry if none exists
        if (!found && executable) {
            final AclEntry newEntry = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(user)
                    .setPermissions(execPerms)
                    .build();
            updatedAcl.addFirst(newEntry);
        }
        view.setAcl(updatedAcl);
    }

    /**
     * Lists the files and directories from a given directory path
     *
     * @param dir the path of target directory
     * @return the list with paths of files and directories
     */
    public static Collection<Path> listFiles(Path dir) {
        return listFiles(dir, path -> true);
    }

    /**
     * Lists the files and directories from a given directory path using the given filter
     *
     * @param dir    the path of target directory
     * @param filter the filter object
     * @return the list with paths of filtered files and directories
     */
    public static Collection<Path> listFiles(Path dir, Predicate<Path> filter) {
        try (Stream<Path> stream = Files.list(dir)) {
            if (filter != null) {
                return stream.filter(filter).toList();
            }
            return stream.toList();
        } catch (IOException ignored) {
        }
        return null;
    }

    /**
     * Lists the names of files and directories from a given directory path
     *
     * @param dir the path of target directory
     * @return the list with names of files and directories
     */
    public static Collection<String> list(Path dir) {
        return list(dir, path -> true);
    }

    /**
     * Lists the names of files and directories from a given directory path using the given filter
     *
     * @param dir    the path of target directory
     * @param filter the filter object
     * @return the list with names of filtered files and directories
     */
    public static Collection<String> list(Path dir, Predicate<Path> filter) {
        final Collection<Path> files = listFiles(dir, filter);
        if (files != null) {
            final Collection<String> result = new ArrayList<>();
            for (Path file : files) {
                result.add(file.getFileName().toString());
            }
            return result;
        }
        return null;
    }

    /**
     * Returns the size of a file (in bytes).
     *
     * @param file the path of target file
     * @return the size of a file (in bytes).
     */
    public static long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException ignored) {
        }
        return -1;
    }

    /**
     * Tells whether a file is considered hidden.
     *
     * @param file the path of target file
     * @return {@code true} if the file is considered hidden
     */
    public static boolean isHidden(Path file) {
        try {
            return Files.isHidden(file);
        } catch (IOException ignored) {
        }
        return false;
    }

    /**
     * Returns a file's last modified time in milliseconds.
     * @param path the path to the file
     * @return a file's last modified time in milliseconds
     */
    public static long getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
        }
        return System.currentTimeMillis();
    }

    /**
     * Deletes a file or directory at the end of the JVM execution
     *
     * @param path the path of target file or directory
     */
    public static void deleteOnExit(Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> delete(path)));
    }

    /**
     *
     * Deletes the directory <code>treeRoot</code> and all the content recursively.
     *
     * @param treeRoot the path of target directory to be deleted
     * @return {@code true} if the target directory tree is successfully deleted
     */
    public static boolean deleteFileTree(Path treeRoot) {
        Assert.notNull(treeRoot);
        if (Files.notExists(treeRoot)) {
            return false;
        }
        try {
            Files.walkFileTree(treeRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // delete file first
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    // delete directory last
                    Files.deleteIfExists(dir); // directory should now be empty
                    return FileVisitResult.CONTINUE;
                }
            });
            if (!Files.deleteIfExists(treeRoot) && Files.exists(treeRoot)) {
                throw new IOException();
            }
            return true;
        } catch (Exception e) {
            LoggerFactory.getLogger(IOHelper.class).warn("Failed to delete dir: {}", treeRoot);
            return false;
        }
    }

    /**
     * Deletes the file or directory {@code path}.
     *
     * @param path the path of target file or directory to be deleted
     * @return {@code true} if the file or directory is successfully deleted
     */
    public static boolean delete(Path path) {
        if (Files.isDirectory(path)) {
            return deleteFileTree(path);
        } else {
            try {
                Files.delete(path);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

}
