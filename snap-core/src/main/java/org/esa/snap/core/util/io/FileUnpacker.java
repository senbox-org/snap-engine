/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.util.io;

import com.bc.ceres.core.runtime.internal.IOHelper;

import javax.swing.ProgressMonitor;
import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A utility class for unpacking file archives.
 *
 * @author Norman Fomferra
 */
public class FileUnpacker {

    // todo (nf) - dont use progress monitor directly, use an observer instead

    /**
     * Unpacks the given archive file which is in ZIP format to the specified target directory.
     * The method uses a Swing progress monitor to visualize the unpack process.
     *
     * @param archiveFile the ZIP file
     * @param targetDir  the target directory
     * @param parentComponent the parent component to be used by the progress monitor
     * @throws IOException if an I/O error occurs
     */
    public static void unpackZip(final Path archiveFile,
                                 final Path targetDir,
                                 final Component parentComponent) throws IOException {
        final ZipFile zipFile = new ZipFile(archiveFile.toFile());
        final List entryList = extractZipEntries(zipFile);
        final List createdFileList = new ArrayList(entryList.size());
        final List createdDirList = new ArrayList(entryList.size());
        final String message = "Unpacking data from " + archiveFile; /*I18N*/
        final ProgressMonitor progressMonitor = new ProgressMonitor(parentComponent, message, "", 0, entryList.size() - 1);
        try {
            extractZipEntries(zipFile, targetDir, progressMonitor,
                    entryList, createdFileList, createdDirList);
        } catch (IOException e) {
            deleteFilesInList(createdFileList);
            deleteFilesInList(createdDirList);
        } finally {
            progressMonitor.close();
            zipFile.close();
        }
    }

    private static List extractZipEntries(final ZipFile zipFile) {
        final List entryList = new ArrayList(100);
        final Enumeration enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            entryList.add(enumeration.nextElement());
        }
        return entryList;
    }

    private static void extractZipEntries(final ZipFile zipFile,
                                          final Path targetDir,
                                          final ProgressMonitor progressMonitor,
                                          final List entryList,
                                          final List createdFileList,
                                          final List createdDirList) throws IOException {

        progressMonitor.setProgress(0);
        for (int i = 0; i < entryList.size(); i++) {
            final ZipEntry zipEntry = (ZipEntry) entryList.get(i);

            if (progressMonitor.isCanceled()) {
                throw new InterruptedIOException();
            }

            final String zipName = zipEntry.getName().replace('/', IOHelper.SEPARATOR_CHAR);
            final Path outputFile = targetDir.resolve(zipName);
            if (zipEntry.isDirectory()) {
                ensureExistingDir(outputFile, createdDirList);
            } else {
                final String note = "Writing file " + outputFile;   /*I18N*/
                progressMonitor.setNote(note);
                final Path parentDir = outputFile.getParent();
                if (parentDir != null) {
                    ensureExistingDir(parentDir, createdDirList);
                }
                writeOutputFile(zipFile, zipEntry, outputFile, createdFileList);
            }

            progressMonitor.setProgress(i);
        }
    }

    private static void writeOutputFile(final ZipFile zipFile,
                                        final ZipEntry zipEntry,
                                        final Path outputFile,
                                        final List createdFileList) throws IOException {
        final boolean outputFileExists = Files.exists(outputFile);
        final InputStream is = new BufferedInputStream(zipFile.getInputStream(zipEntry));
        final OutputStream os;
        try {
            os = new BufferedOutputStream(Files.newOutputStream(outputFile));
        } catch (IOException e) {
            is.close();
            throw e;
        }
        try {
            IOUtils.copyBytesAndClose(is, os);
        } finally {
            if (!outputFileExists) {
                createdFileList.add(outputFile);
            }
        }
    }

    private static void ensureExistingDir(final Path outputDir,
                                          final List createdDirList) throws IOException {
        final boolean created = IOHelper.createDirectory(outputDir);
        if (created) {
            createdDirList.add(outputDir);
        }
    }

    private static void deleteFilesInList(final List createdFileList) {
        for (int i = createdFileList.size() - 1; i >= 0; i--) {
            Path outputFile = (Path) createdFileList.get(i);
            IOHelper.delete(outputFile);
        }
    }
}
