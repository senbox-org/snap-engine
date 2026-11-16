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

import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;


/**
 * A utility class for scaning directories in the local filesystem.
 *
 * @author Norman Fomferra
 */
public class FileScanner {

    public static void scanDirectory(String dirPath, FileFilter fileFilter, Handler fileHandler) {
        scanDirectory(Path.of(dirPath), fileFilter, fileHandler);
    }

    public static void scanDirectories(String[] dirPaths, FileFilter fileFilter, Handler fileHandler) {
        for (String dirPath : dirPaths) {
            scanDirectory(dirPath, fileFilter, fileHandler);
        }
    }

    public static void scanDirectory(Path dir, FileFilter fileFilter, Handler fileHandler) {
        scanDirectories(new Path[]{dir}, fileFilter, fileHandler);
    }

    public static void scanDirectories(Path[] dirs, FileFilter fileFilter, Handler fileHandler) {
        for (Path dir : dirs) {
            scanDirectoryImpl(dir, fileFilter, fileHandler);
        }
    }

    private static void scanDirectoryImpl(Path dir, FileFilter fileFilter, Handler fileHandler) {
        final Collection<Path> files = IOHelper.listFiles(dir);
        if (files == null) {
            fileHandler.onDirectoryScanRejected(dir);
            return;
        }
        fileHandler.onDirectoryScanStarted(dir);
        for (Path file : files) {
            if (Files.isDirectory(file)) {
                scanDirectoryImpl(file, fileFilter, fileHandler);
            } else if (fileFilter.accept(file.toFile())) {
                fileHandler.onFileAccepted(file);
            }
        }
        fileHandler.onDirectoryScanEnded(dir);
    }

    public interface Handler {
        void onDirectoryScanStarted(Path dir);
        void onDirectoryScanEnded(Path dir);
        void onDirectoryScanRejected(Path dir);
        void onFileAccepted(Path file);
    }

    public static class HandlerAdapter implements Handler {
        public void onDirectoryScanEnded(Path dir) {
        }

        public void onDirectoryScanStarted(Path dir) {
        }

        public void onDirectoryScanRejected(Path dir) {
        }

        public void onFileAccepted(Path file) {
        }
    }
}
