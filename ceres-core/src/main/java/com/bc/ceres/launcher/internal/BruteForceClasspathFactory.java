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

import com.bc.ceres.core.runtime.RuntimeConfig;

import java.nio.file.Path;
import java.util.ArrayList;


public class BruteForceClasspathFactory extends AbstractClasspathFactory {

    private ArrayList<Path> classpath;

    public BruteForceClasspathFactory(RuntimeConfig config) {
        super(config);
        classpath = new ArrayList<>(32);
    }

    @Override
    public void processClasspathFile(Path file, LibType libType, int level) {
        if (level > 2) {
            return;
        }
        final String name = file.getFileName().toString().toLowerCase();
        if (!name.startsWith(CERES_LAUNCHER_PREFIX)) {
            if (LibType.LIBRARY.equals(libType)) {
                classpath.add(file);
            } else if (LibType.MODULE.equals(libType)) {
                if (level == 0) {
                    classpath.add(file);
                } else if (level == 2 && "lib".equalsIgnoreCase(file.getParent().getFileName().toString())) {
                    classpath.add(file);
                }
            }
        }
    }

    @Override
    protected Path[] getClasspathFiles() {
        return classpath.toArray(new Path[classpath.size()]);
    }
}
