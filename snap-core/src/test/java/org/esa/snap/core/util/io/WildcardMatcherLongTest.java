/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.util.io;

import com.bc.ceres.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(LongTestRunner.class)
public class WildcardMatcherLongTest {

    @Test
    public void testGlobInCwd() throws Exception {

        File cwd = new File(".").getCanonicalFile();
        File[] alreadyExistingTxtFiles = cwd.listFiles((dir, name) -> name.endsWith(".txt"));
        final File[] testFiles = {
                /*0*/new File(cwd, "WildcardMatcherTest-1.txt"),
                /*1*/new File(cwd, "WildcardMatcherTest-2.txt"),
                /*2*/new File(cwd, "WildcardMatcherTest-3.dat"),
                /*3*/new File(cwd, "WildcardMatcherTest-4.txt"),
                /*4*/new File(cwd, "WildcardMatcherTest-5.dat"),
        };
        int expectedTxtFileCount = alreadyExistingTxtFiles.length + 3;

        try {
            for (File file : testFiles) {
                if (!file.createNewFile()) {
                    System.out.println("Warning: test file could not be created: " + file);
                    System.out.println("Warning: testGlobInCwd() not performed");
                    return;
                }
            }

            File[] files = WildcardMatcher.glob("*.txt");
            assertNotNull(files);
            assertEquals(expectedTxtFileCount, files.length);
            assertTrue(containsFile(files, testFiles[0]));
            assertTrue(containsFile(files, testFiles[1]));
            assertTrue(containsFile(files, testFiles[3]));
            assertTrue(getIndexOf(testFiles[0], files) < getIndexOf(testFiles[1], files));
            assertTrue(getIndexOf(testFiles[1], files) < getIndexOf(testFiles[3], files));

            files = WildcardMatcher.glob("./*.txt");
            assertNotNull(files);
            assertEquals(expectedTxtFileCount, files.length);
            assertTrue(containsFile(files, testFiles[0]));
            assertTrue(containsFile(files, testFiles[1]));
            assertTrue(containsFile(files, testFiles[3]));
            assertTrue(getIndexOf(testFiles[0], files) < getIndexOf(testFiles[1], files));
            assertTrue(getIndexOf(testFiles[1], files) < getIndexOf(testFiles[3], files));

        } finally {
            for (File file : testFiles) {
                if (file.exists() && !file.delete()) {
                    System.out.println("Warning: test file could not be deleted: " + file);
                }
            }
        }
    }

    private boolean containsFile(File[] searchIn, File toFind) {
        return getIndexOf(toFind, searchIn) >= 0;
    }

    private int getIndexOf(File item, File[] searchIn) {
        return Arrays.binarySearch(searchIn, item);
    }

}
