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

import java.nio.file.Path;
import java.util.function.Predicate;

public class JarFilenameFilter implements Predicate<Path> {
    @Override
    public boolean test(Path path) {
        return isJarName(path.getFileName().toString());
    }

    public static boolean isJarName(String name) {
        return name.length() > 4 && (
                isJarExtension(name) ||
                isZipExtension(name));
    }

    public static boolean isJarExtension(String name) {
        return name.endsWith(".jar") || name.endsWith(".JAR");
    }
    public static boolean isZipExtension(String name) {
        return name.endsWith(".zip") || name.endsWith(".ZIP");
    }
}
