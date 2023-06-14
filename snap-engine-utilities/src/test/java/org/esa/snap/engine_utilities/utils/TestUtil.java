/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.engine_utilities.utils;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtil {

    public static final String PROPERTYNAME_DATA_DIR = "snap.reader.tests.data.dir";

    public static boolean testdataAvailable() {
        String testDataDir = System.getProperty(PROPERTYNAME_DATA_DIR);
        return (testDataDir != null) && !testDataDir.isEmpty() && Files.exists( Paths.get( testDataDir ) );
    }

    public static File getTestFile(String file) {
        final File testTgz = getTestFileOrDirectory(file);
        Assert.assertTrue(String.format("Looking for file: [%s]", testTgz.getAbsolutePath()), testTgz.isFile());
        return testTgz;
    }

    public static File getTestDirectory(String file) {
        final File testTgz = getTestFileOrDirectory(file);
        Assert.assertTrue(String.format("Is directory: [%s]", testTgz.getAbsolutePath()), testTgz.isDirectory());
        return testTgz;
    }

    private static File getTestFileOrDirectory(String file) {
        String partialPath = file;
        if(SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX)
        {
            partialPath = file.replaceAll("\\\\", "/");
        }

        String path = System.getProperty(PROPERTYNAME_DATA_DIR);
        return new File(path, partialPath);
    }
}
