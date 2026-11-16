/*
 *
 *  Copyright (c) 2022.
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.engine_utilities.dataio;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class VirtualDirTgzTest {

    @Test
    public void testIsTgz() {
        assertFalse(VirtualDirTgz.isTgz("xxxxx.nc.gz"));
        assertFalse(VirtualDirTgz.isTgz("xxxxx.ppt.gz"));
        assertFalse(VirtualDirTgz.isTgz("xxxxx.gz"));
        assertFalse(VirtualDirTgz.isTgz("xxxxx.tar"));
        assertFalse(VirtualDirTgz.isTgz("xxxxx.geotiff"));

        assertTrue(VirtualDirTgz.isTgz("xxxxx.tgz"));
        assertTrue(VirtualDirTgz.isTgz("xxxxx.tGz"));
        assertTrue(VirtualDirTgz.isTgz("xxxxx.tar.gz"));
        assertTrue(VirtualDirTgz.isTgz("xxxxx.TAR.gz"));
    }

    @Test
    public void testIsTbz() {
        assertFalse(VirtualDirTgz.isTbz("xxxxx.nc.gz"));
        assertFalse(VirtualDirTgz.isTbz("xxxxx.ppt.gz"));
        assertFalse(VirtualDirTgz.isTbz("xxxxx.gz"));
        assertFalse(VirtualDirTgz.isTbz("xxxxx.tar"));
        assertFalse(VirtualDirTgz.isTbz("xxxxx.geotiff"));

        assertTrue(VirtualDirTgz.isTbz("xxxxx.tbz"));
        assertTrue(VirtualDirTgz.isTbz("xxxxx.TBz"));
        assertTrue(VirtualDirTgz.isTbz("xxxxx.tar.bz"));
        assertTrue(VirtualDirTgz.isTbz("xxxxx.TAR.bz2"));
        assertTrue(VirtualDirTgz.isTbz("xxxxx.tar.bz2"));
        assertTrue(VirtualDirTgz.isTbz("xxxxx.tbz2"));
    }

    @Test
    @STTM("SNAP-3627")
    public void testEnsureCorrectPathSegments() {
        final String sep = IOHelper.SEPARATOR;

        final Path extractDir = Paths.get("path");
        final Path correct = Paths.get("path", "of", "segments", "with", "valid", "length");
        String expected = "path" + sep + "of" + sep + "segments" + sep + "with" + sep + "valid" + sep + "length";

        Path file = VirtualDirTgz.ensureCorrectPathSegments(correct, extractDir);
        assertTrue(file.toString().contains(expected));

        final Path invalid = Path.of("path" + sep + "of" + sep + "!se;gme,nts");
        expected = "path" + sep + "of" + sep + "_se_gme_nts";

        file = VirtualDirTgz.ensureCorrectPathSegments(invalid, extractDir);
        assertTrue(file.toString().contains(expected));

        final Path tooLong = Paths.get("path", "dims_op_oc_oc-en_701293165_1", "a_lon_and_borimg_directory_with_the_only_purpose_to_make_the_whole_thing_here_invalid", "ENMAP.HSI.L1C", "ENMAP-HSI-L1CDT0000063762_01-2024-03-03T18_01_31.002_tomblock-cat1distributor_701293163_759889783_2024-03-05T14_06_53.337", "ENMAP01-____L1C-DT0000063762_20240303T180131Z_001_V010401_20240305T120002Z");
        file = VirtualDirTgz.ensureCorrectPathSegments(tooLong, extractDir);
        assertTrue(file.toString().length() < 255);
    }


    @Test
    @STTM("SNAP-4105")
    public void test_close_deletesExtractDir_afterUnpack_pathConstructor() throws Exception {
        Path tarFile = createTarFile("virtual-dir-tgz-path", "folder/test.txt", "hello");

        try (VirtualDirTgz virtualDir = new VirtualDirTgz(tarFile)) {
            final Path extractedFile = virtualDir.getFile("folder/test.txt");

            assertTrue(Files.exists(extractedFile));

            final Path tempDir = virtualDir.getTempDir();
            assertNotNull(tempDir);
            assertTrue(Files.exists(tempDir));
            assertTrue(Files.isDirectory(tempDir));

            virtualDir.close();

            assertFalse(Files.exists(tempDir));
            assertNull(virtualDir.getTempDir());
        } finally {
            Files.deleteIfExists(tarFile);
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_close_isIdempotent_afterUnpack() throws Exception {
        Path tarFile = createTarFile("virtual-dir-tgz-idempotent", "a.txt", "data");

        try (VirtualDirTgz virtualDir = new VirtualDirTgz(tarFile)) {
            final Path extracted = virtualDir.getFile("a.txt");
            assertTrue(Files.exists(extracted));

            final Path tempDir = virtualDir.getTempDir();
            assertNotNull(tempDir);
            assertTrue(Files.exists(tempDir));

            virtualDir.close();
            virtualDir.close();

            assertFalse(Files.exists(tempDir));
            assertNull(virtualDir.getTempDir());
        } finally {
            Files.deleteIfExists(tarFile);
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_close_deletesTempDir_fileConstructor() throws Exception {
        Path tarFile = createTarFile("virtual-dir-tgz-file", "b.txt", "content");

        try (VirtualDirTgz virtualDir = new VirtualDirTgz(tarFile)) {
            final Path extracted = virtualDir.getFile("b.txt");
            assertTrue(Files.exists(extracted));

            final Path tempDir = virtualDir.getTempDir();
            assertNotNull(tempDir);
            assertTrue(Files.exists(tempDir));

            virtualDir.close();

            assertFalse(Files.exists(tempDir));
            assertNull(virtualDir.getTempDir());
        } finally {
            Files.deleteIfExists(tarFile);
        }
    }

    private static Path createTarFile(String prefix, String entryName, String content) throws IOException {
        final Path tarFile = IOHelper.createTempFile(prefix, ".tar");
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        try (OutputStream fileOut = Files.newOutputStream(tarFile);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(fileOut)) {
            TarArchiveEntry entry = new TarArchiveEntry(entryName);
            entry.setSize(bytes.length);
            tarOut.putArchiveEntry(entry);
            tarOut.write(bytes);
            tarOut.closeArchiveEntry();
            tarOut.finish();
        }

        return tarFile;
    }
}
