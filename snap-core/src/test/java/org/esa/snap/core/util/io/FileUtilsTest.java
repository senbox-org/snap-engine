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
package org.esa.snap.core.util.io;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

import static org.esa.snap.core.util.io.FileUtils.getFilenameFromPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileUtilsTest {

    @Test
    public void testGetExtension() {
        assertNull(FileUtils.getExtension(Path.of("testfile")));
        assertEquals(".", FileUtils.getExtension(Path.of("testfile.")));
        assertEquals(".ext", FileUtils.getExtension(Path.of("testfile.ext")));
        assertEquals(".EXT", FileUtils.getExtension(Path.of("testfile.EXT")));
        assertEquals(".ext", FileUtils.getExtension(Path.of("directory\\testfile.ext")));
        assertEquals(".ext", FileUtils.getExtension(Path.of("directory/testfile.ext")));
        assertEquals(".ext", FileUtils.getExtension(Path.of("d:testfile.ext")));
    }

    @Test
    public void testGetExtensionDotPos() {
        assertEquals(-1, FileUtils.getExtensionDotPos("testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos(".testfile"));
        assertEquals(8, FileUtils.getExtensionDotPos("testfile."));
        assertEquals(8, FileUtils.getExtensionDotPos("testfile.ext"));
        assertEquals(8, FileUtils.getExtensionDotPos("testfile.EXT"));
        assertEquals(18, FileUtils.getExtensionDotPos("directory\\testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("directory/testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("directory:testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("direc.ext\\testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("direc.ext/testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("direc.ext:testfile.ext"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext\\testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext/testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext:testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext\\.testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext/.testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext:.testfile"));
    }

    @Test
    public void testEnsureExtension_FileParameter() {
        Path dirFile = Path.of("dir.with.dots");

        Path current = FileUtils.ensureExtension(dirFile.resolve("file.smc"), ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.smc.kdt"),
                     current.toString());

        current = FileUtils.ensureExtension(dirFile.resolve("file."), ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());

        current = FileUtils.ensureExtension(dirFile.resolve("file"), ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());
    }

    @Test
    public void testEnsureExtension_StringParameter() {
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.smc.kdt"),
                     FileUtils.ensureExtension(
                             GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.smc"), ".kdt"));
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"),
                     FileUtils.ensureExtension(
                             GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file."), ".kdt"));
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"),
                     FileUtils.ensureExtension(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file"),
                                               ".kdt"));
    }

    @Test
    public void testChangeExtension_FileParameter() {
        Path dirFile = Path.of("dir.with.dots");
        Path fileWithExtension = dirFile.resolve("file.smc");
        Path fileWithDot = dirFile.resolve( "file.");
        Path fileWithoutExtension = dirFile.resolve( "file");

        Path current = FileUtils.exchangeExtension(fileWithExtension, ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());

        current = FileUtils.exchangeExtension(fileWithDot, ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());

        current = FileUtils.exchangeExtension(fileWithoutExtension, ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());
    }

    @Test
    public void testChangeExtension_StringParameter() {
        assertEquals("dir.with.dots/file.kdt", FileUtils.exchangeExtension("dir.with.dots/file.smc", ".kdt"));
        assertEquals("dir.with.dots/file.kdt", FileUtils.exchangeExtension("dir.with.dots/file.", ".kdt"));
        assertEquals("dir.with.dots/file.kdt", FileUtils.exchangeExtension("dir.with.dots/file", ".kdt"));
        assertEquals("dir.with.dots/file", FileUtils.exchangeExtension("dir.with.dots/file.txt", ""));
        assertEquals("dir.with.dots/fileo", FileUtils.exchangeExtension("dir.with.dots/file.txt", "o"));
    }

    @Test
    @STTM("SNAP-3916")
    public void testGetFilenameWithoutAnyExtensions() {
        assertEquals("target", FileUtils.getFilenameWithoutAnyExtensions("target.znap.zip"));
        assertEquals("target", FileUtils.getFilenameWithoutAnyExtensions("target.zip"));
        assertEquals("filename", FileUtils.getFilenameWithoutAnyExtensions("filename"));
        assertEquals("", FileUtils.getFilenameWithoutAnyExtensions(""));
        assertEquals(".bashrc", FileUtils.getFilenameWithoutAnyExtensions(".bashrc"));
    }

    @Test
    public void testExtractFileName() {
        final String expected1 = "tesfile1.dim";
        final String expected2 = "tesfile2.dim";
        final String expected3 = "tesfile3.dim";

        final String separator = IOHelper.SEPARATOR;
        final String path1 = "home" + separator + "tom" + separator + expected1;
        final String path2 = "C:" + separator + "Data" + separator + "TestFiles" + separator + expected2;
        final String path3 = separator + expected3;

        // check that null is not allowed as argument
        try {
            getFilenameFromPath((String) null);
            fail("Exception expected here!");
        } catch (IllegalArgumentException ignored) {
        }

        assertEquals(expected1, getFilenameFromPath(path1));
        assertEquals(expected2, getFilenameFromPath(path2));
        assertEquals(expected3, getFilenameFromPath(path3));
    }

    @Test
    @STTM("SNAP-3627")
    public void testExtractFileName_worksWithSeparatorsFromDifferentOs() {
        final String expected = "file.xml";
        final String pathLinux = "/home/tom/the/" + expected;
        final String pathWindows = "C:\\users\\tom\\" + expected;
        final String pathMixed = "C:\\users/tom\\" + expected;

        assertEquals(expected, getFilenameFromPath(pathLinux));
        assertEquals(expected, getFilenameFromPath(pathWindows));
        assertEquals(expected, getFilenameFromPath(pathMixed));
    }

    @Test
    public void testDeleteFileTree() {
        Path treeRoot = GlobalTestConfig.getSnapTestDataOutputDirectory();
        Path firstDir = treeRoot.resolve( "firstDir");
        Path firstFile = treeRoot.resolve( "firstFile");
        Path secondDir = firstDir.resolve( "secondDir");
        Path secondFile = firstDir.resolve( "secondFile");
        Path thirdFile = secondDir.resolve( "thirdFile");
        Path writeProtectedFile = secondDir.resolve( "protFile");

        try {
            IOHelper.createDirectories(treeRoot);
            assertTrue(Files.exists(treeRoot));

            IOHelper.createDirectories(firstDir);
            assertTrue(Files.exists(firstDir));

            IOHelper.createFile(firstFile);
            assertTrue(Files.exists(firstFile));

            IOHelper.createDirectories(secondDir);
            assertTrue(Files.exists(secondDir));

            IOHelper.createFile(secondFile);
            assertTrue(Files.exists(secondFile));

            IOHelper.createFile(thirdFile);
            assertTrue(Files.exists(thirdFile));

            IOHelper.createFile(writeProtectedFile);
            
            assertTrue(IOHelper.setReadOnly(writeProtectedFile));
            assertTrue(Files.exists(writeProtectedFile));

        } catch (Exception e) {
            fail("unable to create file");
        }

        if (FileUtils.deleteTree(treeRoot)) {
            assertFalse(Files.exists(treeRoot));
            assertFalse(Files.exists(firstDir));
            assertFalse(Files.exists(firstFile));
            assertFalse(Files.exists(secondDir));
            assertFalse(Files.exists(secondFile));
            assertFalse(Files.exists(thirdFile));
            assertFalse(Files.exists(writeProtectedFile));
        } else {
            System.err.println("Error in FileUtilsTest: FileUtils.deleteTree() failed to delete dir " + treeRoot);
        }
    }

    @Test
    public void testDeleteFileTreeExceptions() {
        try {
            FileUtils.deleteTree(null);
            fail("invalid null argument");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testDeleteFileTree_DeleteEmptyDirectories() throws IOException {
        Path treeRoot = GlobalTestConfig.getSnapTestDataOutputDirectory();
        IOHelper.createDirectories(treeRoot);
        assertTrue("treeRoot exists expected", Files.exists(treeRoot));
        Path firstDir = treeRoot.resolve( "firstDir");
        Files.createDirectories(firstDir);
        assertTrue("firstDir exists expected", Files.exists(firstDir));
        Path emptyDir = firstDir.resolve( "emptyDir");
        Files.createDirectories(emptyDir);
        assertTrue("emptyDir exists expected", Files.exists(emptyDir));

        if (FileUtils.deleteTree(treeRoot)) {
            assertFalse("treeRoot not exists expected", Files.exists(treeRoot));
        } else {
            System.err.println("Error in FileUtilsTest: FileUtils.deleteTree() failed to delete dir " + treeRoot);
        }
    }

    @Test
    public void testGetFileAsURL() throws MalformedURLException {
        final Path file = Path.of("/dummy/hugo/daten").toAbsolutePath();

        final URL fileURL = FileUtils.getFileAsUrl(file);

        assertEquals(file.toUri().toURL(), fileURL);
    }

    @Test
    public void testThatGetUrlAsFileIsTheInverseOfGetFileAsUrl() throws MalformedURLException,
            URISyntaxException {
        final Path file1 = Path.of("/dummy/hugo/daten").toAbsolutePath();
        final Path file2 = FileUtils.getUrlAsFile(FileUtils.getFileAsUrl(file1));
        assertEquals(file1, file2);
    }

    @Test
    public void testGetDisplayText() {
        try {
            FileUtils.getDisplayText(null, 50);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
        try {
            FileUtils.getDisplayText(Path.of("alpha/bravo/charly"), 3);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
        //                   "5432109876543210987654321098765432109876543210987654321"
        Path file = Path.of("alpha/bravo/charly/delta/echo/foxtrott/golf/hotel/india");
        char sep = IOHelper.SEPARATOR_CHAR;
        assertEquals("...ndia",
                     FileUtils.getDisplayText(file, 7));
        assertEquals(".../india".replace('/', sep),
                     FileUtils.getDisplayText(file, 12));
        assertEquals(".../golf/hotel/india".replace('/', sep),
                     FileUtils.getDisplayText(file, 20));
        assertEquals("alpha/bravo/charly/delta/echo/foxtrott/golf/hotel/india".replace('/', sep),
                     FileUtils.getDisplayText(file, 80));
    }

    @Test
    public void testResolvingDirectories() throws URISyntaxException, IOException {
        final URL fileURL1 = getClass().getResource("/resource-testdata/auxdata");
        assertNotNull(fileURL1);
        Path folder = UrlHelper.urlToFile(fileURL1);
        assertNotNull(folder);
        final URL fileURL2 = getClass().getResource("/resource-testdata/auxdata/");
        assertNotNull(fileURL2);
        Path folderWithSlash = UrlHelper.urlToFile(fileURL2);
        assertNotNull(folderWithSlash);
        final URL fileURL3 = getClass().getResource("/resource-testdata/auxdata/file-1.txt");
        assertNotNull(fileURL3);
        Path file = UrlHelper.urlToFile(fileURL3);
        assertNotNull(file);
        assertEquals("file-1.txt", folder.relativize(file).toString());
        assertEquals("file-1.txt", folderWithSlash.relativize(file).toString());

        final URI jarURI = FileUtils.ensureJarURI(Objects.requireNonNull(getClass().getResource("/resource-testdata.jar")).toURI());
        try (FileSystem fs = FileSystems.newFileSystem(jarURI, Collections.emptyMap())) {
            Path jarPathFromURI = fs.provider().getPath(jarURI);
            Path folder2 = jarPathFromURI.resolve("auxdata");
            Path folder2WithSlash = jarPathFromURI.resolve("auxdata/");
            String version = System.getProperty("java.version");
            if (version.startsWith("1.8")) {
                // the trailing slash makes a difference when used in path within a jar file in Java 8
                assertFalse(Files.isSameFile(folder2, folder2WithSlash));
                Path file2 = jarPathFromURI.resolve("auxdata").resolve("file-1.txt");
                assertEquals("file-1.txt", folder2.relativize(file2).toString());
                assertEquals("../auxdata/file-1.txt", folder2WithSlash.relativize(file2).toString());
            } else {
                //In versions from 11 onwards the above behavior is fixed.
                assertTrue(Files.isSameFile(folder2, folder2WithSlash));
                Path file2 = jarPathFromURI.resolve("auxdata").resolve("file-1.txt");
                assertEquals("file-1.txt", folder2.relativize(file2).toString());
                assertEquals("file-1.txt", folder2WithSlash.relativize(file2).toString());
            }
        }
    }

    @Test
    public void computeHashForFileTest() throws IOException, URISyntaxException {
        final URL filePathURL = getClass().getResource("/resource-testdata.jar");
        Assert.assertNotNull(filePathURL);
        Assert.assertEquals("15cabe41b0d09a28f93d9a6d81c9b02683bd24ef31c5efb92c534eea1b9e6430", FileUtils.computeHashForFile(UrlHelper.urlToFile(filePathURL)));
    }

    @Test
    public void computeHashForDirectoryTest() throws IOException, URISyntaxException {
        final URL directoryPathURL = getClass().getResource("/resource-testdata/auxdata");
        Assert.assertNotNull(directoryPathURL);
        Assert.assertEquals("77848723f1da8254229c8cdf46b461857645086610f4c2654afe4c15f248a67e", FileUtils.computeHashForDirectory(UrlHelper.urlToFile(directoryPathURL)));
    }
}

