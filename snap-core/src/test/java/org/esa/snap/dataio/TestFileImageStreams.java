/*
 * Copyright (C) 2007 - 2008, GeoSolutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.esa.snap.dataio;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class TestFileImageStreams {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // === FileImageInputStreamExtImpl ===

    @Test
    public void testInputStreamCreateAndRead() throws Exception {
        File file = tempFolder.newFile("input.bin");
        Files.write(file.toPath(), "Hello World".getBytes(StandardCharsets.UTF_8));

        FileImageInputStreamExtImpl stream = new FileImageInputStreamExtImpl(file);
        assertNotNull(stream);
        assertEquals(file, stream.getFile());
        assertTrue(stream.length() > 0);
        stream.close();
    }

    @Test
    public void testInputStreamReadByte() throws Exception {
        File file = tempFolder.newFile("byte.bin");
        Files.write(file.toPath(), new byte[]{0x41, 0x42, 0x43}); // A, B, C

        FileImageInputStreamExtImpl stream = new FileImageInputStreamExtImpl(file);
        assertEquals(0x41, stream.readByte());
        stream.close();
    }

    @Test
    public void testInputStreamSeek() throws Exception {
        File file = tempFolder.newFile("seek.bin");
        Files.write(file.toPath(), "ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8));

        FileImageInputStreamExtImpl stream = new FileImageInputStreamExtImpl(file);
        stream.seek(5);
        assertEquals('F', stream.readByte());
        stream.close();
    }

    @Test
    public void testInputStreamSetByteOrder() throws Exception {
        File file = tempFolder.newFile("order.bin");
        Files.write(file.toPath(), new byte[]{1, 2});

        FileImageInputStreamExtImpl stream = new FileImageInputStreamExtImpl(file);
        // Default should be BIG_ENDIAN
        assertEquals(ByteOrder.BIG_ENDIAN, stream.getByteOrder());

        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        assertEquals(ByteOrder.LITTLE_ENDIAN, stream.getByteOrder());
        stream.close();
    }

    @Test
    public void testInputStreamToString() throws Exception {
        File file = tempFolder.newFile("tostring.bin");
        Files.write(file.toPath(), new byte[]{1});

        FileImageInputStreamExtImpl stream = new FileImageInputStreamExtImpl(file);
        String desc = stream.toString();
        assertTrue(desc.contains("FileImageInputStreamExtImpl"));
        assertTrue(desc.contains(file.toString()));
        stream.close();
    }

    @Test
    public void testInputStreamCloseIsIdempotent() throws Exception {
        File file = tempFolder.newFile("close.bin");
        Files.write(file.toPath(), new byte[]{1});

        FileImageInputStreamExtImpl stream = new FileImageInputStreamExtImpl(file);
        stream.close();
        stream.close(); // should not throw
    }

    @Test
    public void testInputStreamReadFully() throws Exception {
        File file = tempFolder.newFile("full.bin");
        byte[] data = {1, 2, 3, 4, 5};
        Files.write(file.toPath(), data);

        FileImageInputStreamExtImpl stream = new FileImageInputStreamExtImpl(file);
        byte[] buf = new byte[5];
        stream.readFully(buf);
        assertArrayEquals(data, buf);
        stream.close();
    }

    @Test(expected = NullPointerException.class)
    public void testInputStreamNullFile() throws Exception {
        new FileImageInputStreamExtImpl(null);
    }

    @Test(expected = FileNotFoundException.class)
    public void testInputStreamNonExistentFile() throws Exception {
        new FileImageInputStreamExtImpl(new File(tempFolder.getRoot(), "nonexistent.bin"));
    }

    @Test
    public void testInputStreamCreateStaticMethod() throws Exception {
        File file = tempFolder.newFile("static.bin");
        Files.write(file.toPath(), new byte[]{1, 2, 3});

        ImageInputStream stream = FileImageInputStreamExtImpl.createInputStream(file);
        assertNotNull(stream);
        assertTrue(stream instanceof FileImageInputStreamExtImpl);
        stream.close();
    }

    @Test
    public void testInputStreamWithBufferSize() throws Exception {
        File file = tempFolder.newFile("buffered.bin");
        Files.write(file.toPath(), "buffered content".getBytes(StandardCharsets.UTF_8));

        FileImageInputStreamExtImpl stream = new FileImageInputStreamExtImpl(file, 1024);
        assertNotNull(stream);
        assertTrue(stream.length() > 0);
        stream.close();
    }

    // === FileImageOutputStreamExtImpl ===

    @Test
    public void testOutputStreamCreateAndWrite() throws Exception {
        File file = tempFolder.newFile("output.bin");

        FileImageOutputStreamExtImpl stream = new FileImageOutputStreamExtImpl(file);
        assertNotNull(stream);
        assertEquals(file, stream.getFile());

        stream.write(0x41);
        stream.close();

        byte[] content = Files.readAllBytes(file.toPath());
        assertEquals(1, content.length);
        assertEquals(0x41, content[0]);
    }

    @Test
    public void testOutputStreamWriteBytes() throws Exception {
        File file = tempFolder.newFile("bytes.bin");

        FileImageOutputStreamExtImpl stream = new FileImageOutputStreamExtImpl(file);
        byte[] data = {1, 2, 3, 4, 5};
        stream.write(data, 0, data.length);
        stream.close();

        assertArrayEquals(data, Files.readAllBytes(file.toPath()));
    }

    @Test
    public void testOutputStreamSeekAndWrite() throws Exception {
        File file = tempFolder.newFile("seekwrite.bin");

        FileImageOutputStreamExtImpl stream = new FileImageOutputStreamExtImpl(file);
        stream.write(new byte[]{0, 0, 0, 0, 0}, 0, 5);
        stream.seek(2);
        stream.write(0xFF);
        stream.close();

        byte[] content = Files.readAllBytes(file.toPath());
        assertEquals(5, content.length);
        assertEquals((byte) 0xFF, content[2]);
    }

    @Test
    public void testOutputStreamLength() throws Exception {
        File file = tempFolder.newFile("length.bin");

        FileImageOutputStreamExtImpl stream = new FileImageOutputStreamExtImpl(file);
        stream.write(new byte[]{1, 2, 3}, 0, 3);
        assertEquals(3, stream.length());
        stream.close();
    }

    @Test
    public void testOutputStreamToString() throws Exception {
        File file = tempFolder.newFile("outstr.bin");

        FileImageOutputStreamExtImpl stream = new FileImageOutputStreamExtImpl(file);
        String desc = stream.toString();
        assertTrue(desc.contains("FileImageOutputStreamExtImpl"));
        assertTrue(desc.contains(file.toString()));
        stream.close();
    }

    @Test
    public void testOutputStreamCloseIsIdempotent() throws Exception {
        File file = tempFolder.newFile("outclose.bin");

        FileImageOutputStreamExtImpl stream = new FileImageOutputStreamExtImpl(file);
        stream.write(0x01);
        stream.close();
        stream.close(); // should not throw
    }

    @Test
    public void testOutputStreamCreateStaticMethod() throws Exception {
        File file = tempFolder.newFile("outstatic.bin");

        ImageOutputStream stream = FileImageOutputStreamExtImpl.createOutputStream(file);
        assertNotNull(stream);
        assertTrue(stream instanceof FileImageOutputStreamExtImpl);
        stream.close();
    }

    @Test
    public void testOutputStreamWithBufferSize() throws Exception {
        File file = tempFolder.newFile("outbuf.bin");

        FileImageOutputStreamExtImpl stream = new FileImageOutputStreamExtImpl(file, 2048);
        stream.write(new byte[]{10, 20, 30}, 0, 3);
        stream.close();

        assertArrayEquals(new byte[]{10, 20, 30}, Files.readAllBytes(file.toPath()));
    }

    @Test
    public void testOutputStreamReadBack() throws Exception {
        File file = tempFolder.newFile("readback.bin");

        FileImageOutputStreamExtImpl stream = new FileImageOutputStreamExtImpl(file);
        stream.write(new byte[]{65, 66, 67}, 0, 3);
        stream.seek(0);
        int val = stream.read();
        assertEquals(65, val);
        stream.close();
    }
}
