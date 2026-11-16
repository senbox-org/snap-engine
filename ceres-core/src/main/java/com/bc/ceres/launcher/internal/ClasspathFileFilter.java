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

package com.bc.ceres.launcher.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

public class ClasspathFileFilter implements Predicate<Path> {

    public boolean test(Path file) {
        return isFileAccepted(file);
    }

    public static boolean isFileAccepted(Path file) {
        if (Files.isDirectory(file)) {
            return true;
        }
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".zip");
    }
}
