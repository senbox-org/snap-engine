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
package org.esa.snap;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.util.SystemUtils;

public class GlobalTestConfig {

    public static final char SEPARATOR_CHAR = FileSystems.getDefault().getSeparator().charAt(0);
    public static final String SNAP_TEST_DATA_INPUT_DIR_PROPERTY_NAME = "org.esa.snap.testdata.in";
    public static final String SNAP_TEST_DATA_OUTPUT_DIR_PROPERTY_NAME = "org.esa.snap.testdata.out";
    public static final String SNAP_TEST_DATA_INPUT_DIR_DEFAULT_PATH = "testdata" + SEPARATOR_CHAR + "in";
    public static final String SNAP_TEST_DATA_OUTPUT_DIR_DEFAULT_PATH = "testdata" + SEPARATOR_CHAR + "out";

    public static Path getSnapTestDataInputDirectory() {
        return getDirectory(SNAP_TEST_DATA_INPUT_DIR_PROPERTY_NAME,
                SNAP_TEST_DATA_INPUT_DIR_DEFAULT_PATH);
    }

    public static Path getSnapTestDataInputFile(String relPath) {
        return getSnapTestDataInputDirectory().resolve(
                        SystemUtils.convertToLocalPath(relPath));
    }

    public static Path getSnapTestDataOutputDirectory() {
        return getDirectory(SNAP_TEST_DATA_OUTPUT_DIR_PROPERTY_NAME,
                SNAP_TEST_DATA_OUTPUT_DIR_DEFAULT_PATH);
    }

    public static Path getSnapTestDataOutputFile(String relPath) {
        return getSnapTestDataOutputDirectory().resolve(SystemUtils.convertToLocalPath(relPath));
    }

    public static Test createTest(final Class testClass, final Path inputFile) {
        if (!Files.exists(inputFile)) {
            return createEmptyTest(testClass, inputFile);
        } else {
            return new TestSuite(testClass);
        }
    }

    private static Test createEmptyTest(final Class testClass, final Path inputFile) {
        return new TestCase(testClass.getName()) {

            @Override
            public void runTest() {
                System.out.println();
                System.out.println(testClass.getName() + ": warning: test will not be performed: testdata not found: ");
                System.out.println(inputFile.toString());
            }
        };
    }

    private static Path getDirectory(String propertyName, String snapRelDefaultPath) {
        String filePath = System.getProperty(propertyName);
        if (filePath != null) {
            return Path.of(filePath);
        }
        return SystemUtils.getApplicationHomeDir().resolve(
                        SystemUtils.convertToLocalPath(snapRelDefaultPath));
    }
}
