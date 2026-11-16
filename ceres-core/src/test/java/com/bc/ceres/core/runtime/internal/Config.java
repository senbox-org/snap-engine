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
import java.net.URL;


public class Config {

    private static final Path TEST_DIR;
    private static final String TEST_DIRNAME = "testdirs";

    static {
        Path testDir = null;
        URL resource = Config.class.getResource("/" + TEST_DIRNAME);
        if (resource != null) {
            testDir = UrlHelper.urlToFile(resource);
        }
        if (testDir == null) {
            testDir = Path.of("target/test-classes/" + TEST_DIRNAME).toAbsolutePath();
        }
        TEST_DIR = testDir;
    }

    public static Path getDirForAppA() {
        return TEST_DIR.resolve("app-a");
    }

    public static Path getDirForAppB() {
        return TEST_DIR.resolve("app-b");
    }

    public static Path getDirForAppC() {
        return TEST_DIR.resolve("app-c");
    }

    public static Path getRepositoryDir() {
        return TEST_DIR.resolve("repository");
    }
}
