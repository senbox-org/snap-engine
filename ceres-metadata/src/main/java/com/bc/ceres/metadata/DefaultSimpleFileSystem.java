/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.metadata;

import com.bc.ceres.core.runtime.internal.IOHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An implementation of the {@link SimpleFileSystem} interface relying on {@link Path}.
 *
 * @author MarcoZ
 * @author Bettina
 * @since Ceres 0.13.2
 */
public class DefaultSimpleFileSystem implements SimpleFileSystem {

    @Override
    public Reader createReader(String path) throws IOException {
        return Files.newBufferedReader(Path.of(path));
    }

    @Override
    public Writer createWriter(String path) throws IOException {
        return Files.newBufferedWriter(Path.of(path));
    }

    @Override
    public String[] list(String path) throws IOException {
        Path directory = Paths.get(path);
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            return IOHelper.list(directory).toArray(new String[0]);
        } else {
            return null;
        }
    }

    @Override
    public boolean isFile(String path) {
        return Files.isRegularFile(Path.of(path));
    }
}
