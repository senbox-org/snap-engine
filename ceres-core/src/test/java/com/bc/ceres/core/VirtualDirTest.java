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

package com.bc.ceres.core;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class VirtualDirTest {

    @Test
    public void testNestedZip() throws Exception {
        Path zipFile = getTestDataDir("VirtualDirTestNested.zip");
        final VirtualDir virtualDir = VirtualDir.create(zipFile);
        assertNotNull(virtualDir);
        try {
            String[] allFileArray = virtualDir.listAllFiles();
            assertNotNull(allFileArray);
            List<String> allFiles = Arrays.asList(allFileArray);

            assertEquals(6, allFiles.size());
            assertTrue(allFiles.contains("dir1/File3"));
            assertTrue(allFiles.contains("dir1/File4"));
            assertTrue(allFiles.contains("dir1/dir3/File6"));
            assertTrue(allFiles.contains("File1"));
            assertTrue(allFiles.contains("File2"));
            assertTrue(allFiles.contains("File5.gz"));

            assertNotNull(virtualDir.getFile("File1"));
            assertNotNull(virtualDir.getFile("File2"));
            assertNotNull(virtualDir.getFile("File5.gz"));
            assertNotNull(virtualDir.getFile("dir1"));
            assertNotNull(virtualDir.getFile("dir1/"));
            assertNotNull(virtualDir.getFile("dir1/dir3"));
            assertNotNull(virtualDir.getFile("dir2"));
            assertNotNull(virtualDir.getFile("dir1/File3"));
            assertNotNull(virtualDir.getFile("dir1/File4"));
            assertNotNull(virtualDir.getFile("dir1/dir3/File6"));
        } finally {
            virtualDir.close();
        }
    }

    @Test
    public void testZip() throws IOException {
        Path zipFile = getTestDataDir("VirtualDirTest.zip");
        final VirtualDir virtualDir = VirtualDir.create(zipFile);
        assertNotNull(virtualDir);
        final Path file1 = virtualDir.getFile("File1");
        assertTrue(Files.exists(file1));
        assertFalse("Path of File1 should not contain the parent path of the zip file.",
                file1.toAbsolutePath().toString().contains(zipFile.getParent().toString()));

        testFileNode(zipFile, virtualDir);

        assertTrue(virtualDir.isCompressed());
        assertTrue(virtualDir.isArchive());
    }

    @Test
    public void testZip_Path() throws IOException {
        Path testDataPath = getTestDataPath("VirtualDirTest.zip");
        final VirtualDir virtualDir = VirtualDir.create(testDataPath);
        assertNotNull(virtualDir);
        final Path file1 = virtualDir.getFile("File1");
        assertTrue(Files.exists(file1));

        assertFalse("Path of File1 should not contain the parent path of the zip file.",
                file1.toAbsolutePath().toString().contains(testDataPath.getParent().toString()));


        testFileNode(testDataPath, virtualDir);

        assertTrue(virtualDir.isCompressed());
        assertTrue(virtualDir.isArchive());
    }

    @Test
    public void testDir() throws IOException {
        final Path file = getTestDataDir("VirtualDirTest.dir");
        // make empty "dir2", because git removes empty dirs
        final Path dir2 = file.resolve("dir2");
        Files.createDirectory(dir2);
        IOHelper.deleteOnExit(dir2);

        final VirtualDir virtualDir = VirtualDir.create(file);
        assertNotNull(virtualDir);
        testFileNode(file, virtualDir);

        assertFalse(virtualDir.isCompressed());
        assertFalse(virtualDir.isArchive());
    }

    @Test
    public void testDir_Path() throws IOException {
        final Path inputPath = getTestDataPath("VirtualDirTest.dir");

        // make empty "dir2", because git removes empty dirs
        final Path dir2 = inputPath.resolve("dir2");
        Files.createDirectories(dir2);
        try {
            final VirtualDir virtualDir = VirtualDir.create(inputPath);
            assertNotNull(virtualDir);
            testFileNode(inputPath, virtualDir);

            assertFalse(virtualDir.isCompressed());
            assertFalse(virtualDir.isArchive());
        } finally {
            deleteTree(dir2);
        }
    }

    private static void deleteTree(Path path) throws IOException {
        try (Stream<Path> filePathStream = Files.walk(path)) {
            filePathStream.forEach(filePath -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    System.out.println(filePath);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    public void testNoZip() throws IOException {
        Path file = getTestDataDir("VirtualDirTest.nozip");
        VirtualDir virtualDir = VirtualDir.create(file);
        assertNull(virtualDir);
    }

    @Test
    public void testNullArg() throws IOException {
        try {
            VirtualDir.create(null);
            fail("NullPointerException?");
        } catch (NullPointerException e) {
            // ok
        }

        try {
            VirtualDir.create((Path) null);
            fail("NullPointerException?");
        } catch (NullPointerException e) {
            // ok
        }
    }

    @Test
    public void testFinalize() throws Throwable {
        Path zipFile = getTestDataDir("VirtualDirTest.zip");
        final VirtualDir virtualDir = VirtualDir.create(zipFile);
        assertNotNull(virtualDir);
        virtualDir.getFile("File1");   // triggers the unzipping

        final Path tempDir = virtualDir.getTempDir();
        assertNotNull(tempDir);

        assertTrue(Files.isDirectory(tempDir));

        virtualDir.close();

        assertFalse(Files.isDirectory(tempDir));
    }

    @Test
    public void testFinalize_withSubdirectories() throws Throwable {
        Path zipFile = getTestDataDir("VirtualDirTest.zip");
        final VirtualDir virtualDir = VirtualDir.create(zipFile);
        assertNotNull(virtualDir);
        virtualDir.getFile("dir1/File3");   // triggers the unzipping

        final Path tempDir = virtualDir.getTempDir();
        assertNotNull(tempDir);

        assertTrue(Files.isDirectory(tempDir));

        virtualDir.close();

        assertFalse(Files.isDirectory(tempDir));
    }

    @Test
    @STTM("SNAP-4105")
    public void testNio_closeCleansTempDir_andRegistrationIsAccepted() throws Exception {
        Path sourceRoot = IOHelper.createATempDir("virtualdir-nio-src");
        Path sourceFile = sourceRoot.resolve("sample.txt");
        Files.writeString(sourceFile, "hello");

        VirtualDir virtualDir = null;
        try {
            Class<?> nioClass = Class.forName("com.bc.ceres.core.VirtualDir$NIO");
            java.lang.reflect.Constructor<?> ctor =
                    nioClass.getDeclaredConstructor(Path.class, String.class, boolean.class, boolean.class, boolean.class);
            ctor.setAccessible(true);

            virtualDir = (VirtualDir) ctor.newInstance(
                    sourceRoot,
                    sourceRoot.toString(),
                    false,
                    false,
                    true
            );

            Path localCopy = virtualDir.getFile("sample.txt");
            assertTrue(Files.exists(localCopy));

            Path tempDir = virtualDir.getTempDir();
            assertNotNull(tempDir);
            assertTrue(Files.isDirectory(tempDir));

            virtualDir.close();

            assertFalse(Files.exists(tempDir));
        } finally {
            if (virtualDir != null) {
                try {
                    virtualDir.close();
                } catch (Exception ignored) {
                }
            }
            Files.deleteIfExists(sourceFile);
            Files.deleteIfExists(sourceRoot);
        }
    }

    private static Path getTestDataDir() {
        Path dir = Path.of("./src/test/data/");
        if (!Files.exists(dir)) {
            dir = Path.of("./ceres-core/src/test/data/");
            if (!Files.exists(dir)) {
                fail("Can't find my test data. Where is '" + dir + "'?");
            }
        }
        return dir;
    }

    private static Path getTestDataDir(String path) {
        return getTestDataDir().resolve(path);
    }

    private static Path getTestDataPath(String path) {
        final Path testDataPath = getTestDataDir();
        return testDataPath.resolve(path);
    }

    private void testFileNode(Path file, VirtualDir virtualDir) throws IOException {
        assertNotNull(virtualDir);
        try {
            assertEquals(file.toString(), virtualDir.getBasePath());
            assertNotNull(virtualDir.getFile("File1"));
            assertNotNull(virtualDir.getFile("File2"));
            assertNotNull(virtualDir.getFile("File5.gz"));
            assertNotNull(virtualDir.getFile("dir1"));
            assertNotNull(virtualDir.getFile("dir1/File3"));
            assertNotNull(virtualDir.getFile("dir1/File4"));
            assertNotNull(virtualDir.getFile("dir2"));

            assertTrue(virtualDir.exists("File2"));
            assertFalse(virtualDir.exists("nonExistentFile"));

            assertEquals(36, Files.size(virtualDir.getFile("File5.gz")));
            try (LineNumberReader reader = new LineNumberReader(virtualDir.getReader("File5.gz"))) {
                assertEquals("Norman was was was here!", reader.readLine());
            }

            List<String> dirNames = Arrays.asList(virtualDir.list("."));
            assertNotNull(dirNames);
            assertEquals(5, dirNames.size());
            assertTrue(dirNames.contains("dir1"));
            assertTrue(dirNames.contains("dir2"));
            assertTrue(dirNames.contains("File1"));
            assertTrue(dirNames.contains("File2"));
            assertTrue(dirNames.contains("File5.gz"));

            dirNames = Arrays.asList(virtualDir.list(""));
            assertNotNull(dirNames);
            assertEquals(5, dirNames.size());
            assertTrue(dirNames.contains("dir1"));
            assertTrue(dirNames.contains("dir2"));
            assertTrue(dirNames.contains("File1"));
            assertTrue(dirNames.contains("File2"));
            assertTrue(dirNames.contains("File5.gz"));

            // todo - test other cases with same result, e.g. "dir1/.."

            List<String> dir1Names = Arrays.asList(virtualDir.list("dir1"));
            assertNotNull(dir1Names);
            assertEquals(2, dir1Names.size());
            assertTrue(dir1Names.contains("File3"));
            assertTrue(dir1Names.contains("File4"));

            // todo - test other cases with same result, e.g. "./dir1"

            String[] dir2Names = virtualDir.list("dir2");
            assertNotNull(dir2Names);
            assertEquals(0, dir2Names.length);

            try {
                virtualDir.list("dir3");
                fail("IOException?");
            } catch (FileNotFoundException ignored) {
                // ok
            }
            try {
                virtualDir.getFile("File3");
                fail("IOException?");
            } catch (FileNotFoundException ignored) {
                // ok
            }
            try {
                virtualDir.getFile("dir1/File1");
                fail("IOException?");
            } catch (FileNotFoundException ignored) {
                // ok
            }

            String[] allFileArray = virtualDir.listAllFiles();
            assertNotNull(allFileArray);
            List<String> allFiles = Arrays.asList(allFileArray);
            assertEquals(5, allFiles.size());
            assertTrue(allFiles.contains("dir1/File3"));
            assertTrue(allFiles.contains("dir1/File4"));
            assertTrue(allFiles.contains("File1"));
            assertTrue(allFiles.contains("File2"));
            assertTrue(allFiles.contains("File5.gz"));
        } finally {
            virtualDir.close();
        }
    }
}
