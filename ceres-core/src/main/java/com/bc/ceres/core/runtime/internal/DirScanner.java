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

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.Assert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

public class DirScanner {

    private Path baseDir;
    private boolean recursive;
    private boolean filesOnly;

    public DirScanner(Path baseDir) {
        this(baseDir, false);
    }

    public DirScanner(Path baseDir, boolean recursive) {
        this(baseDir, recursive, false);
    }

    public DirScanner(Path baseDir, boolean recursive, boolean filesOnly) {
        Assert.notNull(baseDir, "baseDir");
        this.baseDir = baseDir;
        this.recursive = recursive;
        this.filesOnly = filesOnly;
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public String[] scan() {
        return scan(new NullFilter());
    }

    public String[] scan(Predicate<Path> filter) {
        Assert.notNull(filter, "filter");
        ArrayList<String> strings = new ArrayList<String>(16);
        collectFiles(filter, "", strings);
        return strings.toArray(new String[0]);
    }

    private void collectFiles(Predicate<Path> filter, String relDirPath, ArrayList<String> entries) {
        Path dir = baseDir.resolve(relDirPath);
        Collection<String> fileNames = IOHelper.list(dir);
        if (fileNames != null) {
            for (String fileName : fileNames) {
                String relFilePath = relDirPath + fileName;
                Path file = baseDir.resolve(relFilePath);
                if (Files.isDirectory(file)) {
                    if (!filesOnly && filter.test(file)) {
                        entries.add(relFilePath);
                    }
                    if (recursive) {
                        collectFiles(filter, relFilePath + '/', entries);
                    }
                } else if (filter.test(file)) {
                    entries.add(relFilePath);
                }
            }
        }
    }

    private static class NullFilter implements Predicate<Path> {
        /**
         * @return always returns <code>true</code>
         */
        @Override
        public boolean test(Path path) {
            return true;
        }
    }
}
