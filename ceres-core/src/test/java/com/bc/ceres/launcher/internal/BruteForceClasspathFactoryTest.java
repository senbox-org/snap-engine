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

import com.bc.ceres.core.runtime.internal.DefaultRuntimeConfig;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class BruteForceClasspathFactoryTest {

    @Test
    public void testGetFiles() throws Exception {
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        BruteForceClasspathFactory bfcf = new BruteForceClasspathFactory(config);
        final Path modulesDir = IOHelper.createATempDir().resolve("modules");
        Files.createDirectory(modulesDir);
        final Path beamHelpDir = modulesDir.resolve("beam-help-4.0");
        Files.createDirectory(beamHelpDir);
        try {
            bfcf.processClasspathFile(Path.of("lib/xpp3-1.3.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(Path.of("lib/xstream-1.2.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(Path.of("lib/jdom-1.0.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(Path.of("lib/jhall-2.0.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(Path.of("lib/ceres-launcher.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(Path.of("modules/ceres-core-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
            bfcf.processClasspathFile(Path.of("modules/ceres-ui-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
            bfcf.processClasspathFile(Path.of("modules/beam-core-4.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
            bfcf.processClasspathFile(Path.of("modules/beam-help-4.0"), BruteForceClasspathFactory.LibType.MODULE, 0);
            bfcf.processClasspathFile(Path.of("modules/beam-help-4.0/lib/lib.jar"), BruteForceClasspathFactory.LibType.MODULE, 2);
            bfcf.processClasspathFile(Path.of("modules/beam-help-4.0/lib/tile.zip"), BruteForceClasspathFactory.LibType.MODULE, 2);
            bfcf.processClasspathFile(Path.of("modules/beam-help-4.0/foo/foo.jar"), BruteForceClasspathFactory.LibType.MODULE, 2);
            bfcf.processClasspathFile(Path.of("modules/beam-help-4.0/dgg/tile.zip"), BruteForceClasspathFactory.LibType.MODULE, 2);
            bfcf.processClasspathFile(Path.of("modules/beam-help-4.0/first-level.jar"), BruteForceClasspathFactory.LibType.MODULE, 1);
            Path[] files = bfcf.getClasspathFiles();
            assertEquals(10, files.length);
            assertEquals(Path.of("lib/xpp3-1.3.4.jar"), files[0]);
            assertEquals(Path.of("lib/xstream-1.2.jar"), files[1]);
            assertEquals(Path.of("lib/jdom-1.0.jar"), files[2]);
            assertEquals(Path.of("lib/jhall-2.0.4.jar"), files[3]);
            assertEquals(Path.of("modules/ceres-core-1.0.jar"), files[4]);
            assertEquals(Path.of("modules/ceres-ui-1.0.jar"), files[5]);
            assertEquals(Path.of("modules/beam-core-4.0.jar"), files[6]);
            assertEquals(Path.of("modules/beam-help-4.0"), files[7]);
            assertEquals(Path.of("modules/beam-help-4.0/lib/lib.jar"), files[8]);
            assertEquals(Path.of("modules/beam-help-4.0/lib/tile.zip"), files[9]);
        } finally {
            Files.delete(beamHelpDir);
            Files.delete(modulesDir);
        }
    }
}
