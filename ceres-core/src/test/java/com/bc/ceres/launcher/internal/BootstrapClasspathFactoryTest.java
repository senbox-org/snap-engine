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

import com.bc.ceres.core.runtime.RuntimeConfigException;
import com.bc.ceres.core.runtime.internal.DefaultRuntimeConfig;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class BootstrapClasspathFactoryTest {

    @Test
    public void testGetFiles() throws RuntimeConfigException {
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        BootstrapClasspathFactory btcf = new BootstrapClasspathFactory(config);
        btcf.processClasspathFile(Path.of("lib/xpp3-1.3.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/xstream-1.2.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/jdom-1.0.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/jhall-2.0.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/ceres-launcher.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("modules/ceres-core-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(Path.of("modules/ceres-ui-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(Path.of("modules/beam-core-4.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        Path[] files = btcf.getClasspathFiles();
        assertEquals(5, files.length);
        assertEquals(Path.of("modules/ceres-core-1.0.jar"), files[0]);
        assertEquals(Path.of("lib/xpp3-1.3.4.jar"), files[1]);
        assertEquals(Path.of("lib/xstream-1.2.jar"), files[2]);
        assertEquals(Path.of("lib/jdom-1.0.jar"), files[3]);
        assertEquals(Path.of("lib/jhall-2.0.4.jar"), files[4]);
    }

    @Test
    public void testGetFilesWithMultCeresCore_1() throws RuntimeConfigException {
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        BootstrapClasspathFactory btcf = new BootstrapClasspathFactory(config);
        btcf.processClasspathFile(Path.of("lib/xpp3-1.3.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/xstream-1.2.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/jdom-1.0.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/jhall-2.0.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/ceres-launcher.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("modules/ceres-core-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(Path.of("modules/ceres-core-1.0.1.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(Path.of("modules/ceres-ui-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(Path.of("modules/beam-core-4.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        Path[] files = btcf.getClasspathFiles();
        assertEquals(5, files.length);
        assertEquals(Path.of("modules/ceres-core-1.0.1.jar"), files[0]);
        assertEquals(Path.of("lib/xpp3-1.3.4.jar"), files[1]);
        assertEquals(Path.of("lib/xstream-1.2.jar"), files[2]);
        assertEquals(Path.of("lib/jdom-1.0.jar"), files[3]);
        assertEquals(Path.of("lib/jhall-2.0.4.jar"), files[4]);
    }

    @Test
    public void testGetFilesWithMultCeresCore_2() throws RuntimeConfigException {
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        BootstrapClasspathFactory btcf = new BootstrapClasspathFactory(config);
        btcf.processClasspathFile(Path.of("lib/xpp3-1.3.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/xstream-1.2.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/jdom-1.0.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/jhall-2.0.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("lib/ceres-launcher.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
        btcf.processClasspathFile(Path.of("modules/ceres-core-1.0.1.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(Path.of("modules/ceres-core-1.2.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(Path.of("modules/ceres-ui-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        btcf.processClasspathFile(Path.of("modules/beam-core-4.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
        Path[] files = btcf.getClasspathFiles();
        assertEquals(5, files.length);
        assertEquals(Path.of("modules/ceres-core-1.2.jar"), files[0]);
        assertEquals(Path.of("lib/xpp3-1.3.4.jar"), files[1]);
        assertEquals(Path.of("lib/xstream-1.2.jar"), files[2]);
        assertEquals(Path.of("lib/jdom-1.0.jar"), files[3]);
        assertEquals(Path.of("lib/jhall-2.0.4.jar"), files[4]);
    }
}
