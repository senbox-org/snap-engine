package com.bc.ceres.core.io;

import com.bc.ceres.annotation.STTM;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;


public class EnhancedRandomAccessFileTest {


    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Test
    @STTM("SNAP-4105")
    public void test_closeFlushesBufferedWrites() throws Exception {
        File file = tempFolder.newFile("buffered.bin");
        EnhancedRandomAccessFile eraf = new EnhancedRandomAccessFile(file, "rw", 16);
        eraf.write("hello".getBytes(StandardCharsets.UTF_8));

        eraf.close();
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file.toPath()));
        assertNull(eraf.getRandomAccessFile());
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeIsIdempotent() throws Exception {
        File file = tempFolder.newFile("idempotent.bin");
        EnhancedRandomAccessFile eraf = new EnhancedRandomAccessFile(file, "rw", 16);
        eraf.writeByte(7);

        eraf.close();
        eraf.close();
        assertNull(eraf.getRandomAccessFile());
        assertEquals(1, Files.size(file.toPath()));
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeTruncatesToMinLength() throws Exception {
        File file = tempFolder.newFile("truncate.bin");
        Files.write(file.toPath(), "0123456789".getBytes(StandardCharsets.UTF_8));
        EnhancedRandomAccessFile eraf = new EnhancedRandomAccessFile(file, "rw", 16);
        eraf.setMinLength(4);

        eraf.close();
        assertEquals(4L, Files.size(file.toPath()));
        assertArrayEquals("0123".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file.toPath()));
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeExtendsToMinLength() throws Exception {
        File file = tempFolder.newFile("extend.bin");
        Files.write(file.toPath(), new byte[] {1, 2, 3});
        EnhancedRandomAccessFile eraf = new EnhancedRandomAccessFile(file, "rw", 16);
        eraf.setMinLength(8);

        eraf.close();
        assertEquals(8L, Files.size(file.toPath()));
        byte[] content = Files.readAllBytes(file.toPath());
        assertEquals(1, content[0]);
        assertEquals(2, content[1]);
        assertEquals(3, content[2]);
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeOnReadonlyFile() throws Exception {
        File file = tempFolder.newFile("readonly.bin");
        Files.write(file.toPath(), "abc".getBytes(StandardCharsets.UTF_8));
        EnhancedRandomAccessFile eraf = new EnhancedRandomAccessFile(file, "r", 16);

        assertNotNull(eraf.getRandomAccessFile());

        eraf.close();
        assertNull(eraf.getRandomAccessFile());
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file.toPath()));
    }
}