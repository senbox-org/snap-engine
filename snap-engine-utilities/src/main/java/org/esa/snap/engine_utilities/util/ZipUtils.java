/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.util;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * For zipping and unzipping compressed files
 */
public class ZipUtils {

    private final static String[] extList = {".zip", ".gz", ".z"};

    public static boolean isZipped(final Path path) {
        if(path.getFileName() == null)
            return false;
        final String name = path.getFileName().toString().toLowerCase();
        for (String ext : extList) {
            if (name.endsWith(ext))
                return true;
        }
        return false;
    }

    public static boolean isZip(final Path inputPath) {
        return inputPath.getFileName() != null && inputPath.getFileName().toString().toLowerCase().endsWith(".zip");
    }

    @Deprecated
    public static boolean findInZip(final Path file, final String prefix, final String suffix) {
        return findInZip(file, prefix, suffix, "") != null;
    }

    public static ZipEntry findInZip(final Path file, final String prefix, final String suffix, final String contains) {
        final Path cachedFile = FileUtils.getCachedFile(file);
        try (final ZipFile productZip = new ZipFile(cachedFile.toFile(), ZipFile.OPEN_READ)){

            final Optional<? extends ZipEntry> result = productZip.stream()
                    .filter(ze -> !ze.isDirectory())
                    .filter(ze -> ze.getName().toLowerCase().endsWith(suffix.toLowerCase()))
                    .filter(ze -> ze.getName().toLowerCase().startsWith(prefix.toLowerCase()))
                    .filter(ze -> ze.getName().toLowerCase().contains(contains.toLowerCase()))
                    .findFirst();
            if(result.isPresent()) {
                return result.get();
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning("unable to read zip file " + file + ": " + e.getMessage());
        }
        return null;
    }

    public static String getRootFolder(final Path file, final String headerFileName) throws IOException {
        try (ZipFile productZip = new ZipFile(file.toFile(), ZipFile.OPEN_READ)) {

            final Optional<? extends ZipEntry> result = productZip.stream()
                    .filter(ze -> !ze.isDirectory())
                    .filter(ze -> ze.getName().toLowerCase().endsWith(headerFileName))
                    .findFirst();
            if (result.isPresent()) {
                ZipEntry ze = result.get();
                String path = ze.toString();
                int sepIndex = path.lastIndexOf('/');
                if (sepIndex > 0) {
                    return path.substring(0, sepIndex) + '/';
                } else {
                    return "";
                }
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning("unable to read zip file " + file + ": " + e.getMessage());
        }
        return "";
    }

    public static boolean isValid(final Path file) {
        try (ZipFile zipfile = new ZipFile(file.toFile())) {
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
                ZipEntry ze = zis.getNextEntry();
                if (ze == null) {
                    return false;
                }
                while (ze != null) {
                    // if it throws an exception fetching any of the following then we know the file is corrupted.
                    try (InputStream in = zipfile.getInputStream(ze)) {
                        ze.getCrc();
                        ze.getCompressedSize();
                        ze = zis.getNextEntry();
                    } catch (IOException e) {
                        return false;
                    }
                }
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
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
                    } else {
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

    public static void zipFile(final Path file, final Path outputZipFile) throws IOException {

        try (ZipOutputStream zipStream = new ZipOutputStream(Files.newOutputStream(outputZipFile))) {
            addToZipFile(file, zipStream);
        }
    }

    private static void addToZipFile(Path file, ZipOutputStream zipStream) {

        try (InputStream inputStream = Files.newInputStream(file)) {

            ZipEntry entry = new ZipEntry(file.getFileName().toString());
            entry.setCreationTime(FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis()));
            zipStream.putNextEntry(entry);

            final byte[] readBuffer = new byte[2048];
            int amountRead;

            while ((amountRead = inputStream.read(readBuffer)) > 0) {
                zipStream.write(readBuffer, 0, amountRead);
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("Unable to zip " + file);
        }
    }
}
