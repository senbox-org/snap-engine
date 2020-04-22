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

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ProxyConfig;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for simple ModuleReader.
 */
public class RepositoryScannerTest {

    private Logger NO_LOGGER = Logger.getAnonymousLogger();
    private ProgressMonitor NO_PM = ProgressMonitor.NULL;
    private ProxyConfig NO_PROXY = ProxyConfig.NULL;

    @Ignore("old unused code, no need to test")
    public void testNullArgConvention() throws IOException, CoreException {
        URL NO_URL = new File("").getAbsoluteFile().toURI().toURL();

        try {
            new RepositoryScanner(null, NO_URL, NO_PROXY);
            fail();
        } catch (NullPointerException ignored) {
        }

        try {
            new RepositoryScanner(NO_LOGGER, null, NO_PROXY);
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            new RepositoryScanner(NO_LOGGER, NO_URL, null);
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            new RepositoryScanner(NO_LOGGER, NO_URL, NO_PROXY).scan(null);
            fail();
        } catch (NullPointerException ignored) {
        }
    }

    @Ignore("old unused code, no need to test")
    public void testRepository() throws IOException, CoreException {
        File repositoryDir = Config.getRepositoryDir();

        RepositoryScanner rs = new RepositoryScanner(NO_LOGGER, repositoryDir.toURI().toURL(), NO_PROXY);
        Module[] repositoryModules = rs.scan(NO_PM);
        assertEquals(5, repositoryModules.length);

        Module rm;
        rm = findModule(repositoryModules, "module-a");
        assertNotNull(rm);
        assertTrue(rm.getContentLength() > 0);
        assertTrue(rm.getLastModified() > 0);
        assertNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-b");
        assertNotNull(rm);
        assertTrue(rm.getContentLength() > 0);
        assertTrue(rm.getLastModified() > 0);
        assertNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-c");
        assertNotNull(rm);
        assertTrue(rm.getContentLength() > 0);
        assertTrue(rm.getLastModified() > 0);
        assertNotNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-d");
        assertNotNull(rm);
        assertTrue(rm.getContentLength() > 0);
        assertTrue(rm.getLastModified() > 0);
        assertNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-e");
        assertNotNull(rm);
        assertTrue(rm.getContentLength() > 0);
        assertTrue(rm.getLastModified() > 0);
        assertNotNull(rm.getAboutUrl());

        rm = findModule(repositoryModules, "module-f");
        assertNull(rm);

        rm = findModule(repositoryModules, "module-g");
        assertNull(rm);
    }


    private Module findModule(Module[] modules, String name) {
        for (Module module : modules) {
            if (name.equals(module.getSymbolicName())) {
                return module;
            }
        }
        return null;
    }

}
