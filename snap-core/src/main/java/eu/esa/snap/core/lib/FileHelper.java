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

package eu.esa.snap.core.lib;

import com.bc.ceres.core.runtime.internal.IOHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by kraftek on 2/20/2015.
 */
public class FileHelper {

    private FileHelper() {
    }

    public static boolean canCopyOrReplaceFile(Path sourceFile, Path destinationFile) throws IOException {
        boolean copyOrReplaceFile = true;
        if (Files.exists(destinationFile)) {
            // the destination file already exists
            if (Files.isRegularFile(destinationFile)) {
                long sourceFileSizeInBytes = Files.size(sourceFile);
                long destinationFileSizeInBytes = Files.size(destinationFile);
                copyOrReplaceFile = (destinationFileSizeInBytes != sourceFileSizeInBytes);
            } else {
                throw new NotRegularFileException(destinationFile.toString());
            }
        }
        return copyOrReplaceFile;
    }

    public static void copyFileUsingInputStream(Path sourcePath, Path destinationLocalFilePath, int maximumBufferSize) throws IOException {
        try (InputStream inputStream = sourcePath.getFileSystem().provider().newInputStream(sourcePath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, maximumBufferSize);
             ReadableByteChannel readableByteChannel = Channels.newChannel(bufferedInputStream);
             OutputStream fileOutputStream = Files.newOutputStream(destinationLocalFilePath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE)) {

            WritableByteChannel destinationFileChannel = Channels.newChannel(fileOutputStream);
            ByteBuffer buffer = ByteBuffer.allocate(maximumBufferSize);
            while (readableByteChannel.read(buffer) > 0) {
                // prepare the buffer to be drained
                buffer.flip();
                // write to the channel, may block
                destinationFileChannel.write(buffer);
                // if partial transfer, shift remainder down; if buffer is empty, same as doing clear()
                buffer.compact();
            }
            // EOF will leave buffer in fill state
            buffer.flip();
            // make sure the buffer is fully drained.
            while (buffer.hasRemaining()) {
                destinationFileChannel.write(buffer);
            }
        }
    }

    public static void copyFileUsingByteChannel(Path sourcePath, Path destinationPath) throws IOException {
        Set<? extends OpenOption> options = Collections.emptySet();
        FileSystemProvider fileSystemProvider = sourcePath.getFileSystem().provider();
        try (FileChannel sourceFileChannel = fileSystemProvider.newFileChannel(sourcePath, options);
             WritableByteChannel writableByteChannel = Channels.newChannel(Files.newOutputStream(destinationPath))) {

            long sourceFileSize = sourceFileChannel.size();
            long transferredSize = 0;
            long bytesReadNow;
            while ((bytesReadNow = sourceFileChannel.transferTo(transferredSize, sourceFileSize - transferredSize, writableByteChannel)) > 0) {
                transferredSize += bytesReadNow;
            }
        }
    }

    public static void copyFileUsingFileChannel(Path sourcePath, Path destinationLocalFilePath) throws IOException {
        Set<? extends OpenOption> options = Collections.emptySet();
        FileSystemProvider fileSystemProvider = sourcePath.getFileSystem().provider();
        try (FileChannel sourceFileChannel = fileSystemProvider.newFileChannel(sourcePath, options);
             OutputStream fileOutputStream = Files.newOutputStream(destinationLocalFilePath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE)) {

            long sourceFileSize = sourceFileChannel.size();
            WritableByteChannel destinationFileChannel = Channels.newChannel(fileOutputStream);
            long transferredSize = 0;
            long bytesReadNow;
            while ((bytesReadNow = sourceFileChannel.transferTo(transferredSize, sourceFileSize - transferredSize, destinationFileChannel)) > 0) {
                transferredSize += bytesReadNow;
            }
        }
    }

    /**
     * Gets a file (if it exists) or creates a new one.
     * If intermediate directories do not exist, they will be created.
     *
     * @param basePath  The parent folder
     * @param pathFragments Additional subfolders that should end with the file name
     * @return  The Path object
     * @throws IOException
     */
    public static Path getFile(String basePath, String...pathFragments) throws IOException {
        Path file = Path.of(basePath);
        if (!Files.exists(file)) {
            IOHelper.createDirectories(file);
        }
        if (pathFragments != null) {
            for (int i = 0; i < pathFragments.length; i++) {
                file = file.resolve(pathFragments[i]);
                if (i < pathFragments.length - 1) {
                    IOHelper.createDirectories(file);
                } else {
                    if (!Files.exists(file)) {
                        IOHelper.createFile(file);
                    }
                }
            }
        }
        return file;
    }

    public static void copyFile(URL sourceURL, Path destinationFile) throws IOException {
        if (sourceURL == null || destinationFile == null) {
            throw new IllegalArgumentException("One of the arguments is null");
        }
        try (InputStream inputStream = sourceURL.openStream()) {
            IOHelper.createDirectories(destinationFile.getParent());
            try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(destinationFile))) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) > 0) {
                    bos.write(buffer, 0, read);
                }
            }
        }
    }

    private static boolean wasSymlink(ZipFile zipFile, ZipEntry zipEntry) {
        if (!zipEntry.isDirectory() && zipEntry.getSize() < 4096) {
            try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                        String fileContent = bufferedReader.readLine();
                        if(fileContent.replaceAll("[\\d\\w\\\\/.\\-+]*","").isEmpty()) {
                            Path target = Paths.get(fileContent);
                            Path zipDir = Paths.get(zipFile.getName()).getParent();
                            Path zipEntryPath = Paths.get(zipEntry.getName());
                            Path zipEntryDir = zipEntryPath.getParent();
                            Path zipEntryAbsolutePath = zipDir.resolve(zipEntryDir);
                            String targetAbsolutePath = Path.of(zipEntryAbsolutePath.resolve(target).toString()).toRealPath().toString();
                            Path linkPath = zipEntryAbsolutePath.resolve(zipEntryPath.getFileName());
                            // make sure we compare two canonical paths
                            if (targetAbsolutePath.startsWith(Path.of(zipDir.toString()).toRealPath().toString())) {
                                Files.createSymbolicLink(linkPath, target);
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                //ignore
            }
        }
        return false;
    }

    public static void unzip(Path sourceFile, Path destination, boolean keepFolderStructure) throws IOException {
        if (sourceFile == null || destination == null) {
            throw new IllegalArgumentException("One of the arguments is null");
        }
        if (!Files.exists(destination)) {
            Files.createDirectory(destination);
        }
        byte[] buffer;
        try (ZipFile zipFile = new ZipFile(sourceFile.toFile())) {
            ZipEntry entry;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (entry.isDirectory() && !keepFolderStructure)
                    continue;
                Path filePath = destination.resolve(entry.getName());
                Path strippedFilePath = destination.resolve(filePath.getFileName());
                if (!Files.exists(filePath)) {
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else if(!wasSymlink(zipFile, entry)){
                        try (InputStream inputStream = zipFile.getInputStream(entry)) {
                            try (BufferedOutputStream bos = new BufferedOutputStream(
                                    Files.newOutputStream(keepFolderStructure ? filePath : strippedFilePath))) {
                                buffer = new byte[4096];
                                int read;
                                while ((read = inputStream.read(buffer)) > 0) {
                                    bos.write(buffer, 0, read);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
