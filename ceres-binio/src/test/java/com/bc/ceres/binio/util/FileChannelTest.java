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

package com.bc.ceres.binio.util;

import com.bc.ceres.core.io.EnhancedNIORandomAccessFile;
import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.test.LongTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * See also:
 * <p>
 * java.nio Examples from The Java Developers Almanac 1.4
 * http://exampledepot.com/egs/java.nio/pkg.html
 * ---
 * Class FileChannelImageInputStream
 * http://java.sun.com/products/java-media/jai/forDevelopers/jai-imageio-1_0_01-fcs-docs/com/sun/media/imageio/stream/FileChannelImageInputStream.html
 * ---
 * ByteBuffer : Java Glossary
 * http://mindprod.com/jgloss/bytebuffer.html
 * ---
 * http://chaoticjava.com/posts/nio-efficient-ios-granular-bits/
 * http://chaoticjava.com/posts/nio-data-flow-made-resource-efficient/
 */
@RunWith(LongTestRunner.class)
public class FileChannelTest {
    private Path file;
    private EnhancedNIORandomAccessFile raf;

    /**
     * read some raw bytes from a file
     *
     * @throws IOException if problems with read
     */
    @SuppressWarnings({"UnusedAssignment"})
    private static void readRawBytes() throws IOException {
        final Path testFile = IOHelper.createATempDir().resolve("test.dat");
        final OutputStream fos = Files.newOutputStream(testFile);
        for (int i = 0; i < 1024 * 64; i++) {
            fos.write((byte) i);
        }
        fos.close();

        // allocate a channel to read that file
        FileChannel fc = FileChannel.open(testFile);

        // allocate a buffer, as big a chunk as we are willing to handle at a pop.
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 15);
        showStats("newly allocated read", fc, buffer);

        // read a chunk of raw bytes, up to 15K bytes long
        // -1 means eof.
        int bytesRead = fc.read(buffer);
        showStats("after first read", fc, buffer);

        // flip from filling to emptying
        showStats("before flip", fc, buffer);
        buffer.flip();
        showStats("after flip", fc, buffer);

        byte[] receive = new byte[1024];
        buffer.get(receive);
        showStats("after first get", fc, buffer);

        buffer.get(receive);
        showStats("after second get", fc, buffer);

        // empty buffer to fill with more data.
        buffer.clear();
        showStats("after clear", fc, buffer);

        bytesRead = fc.read(buffer);
        showStats("after second read", fc, buffer);

        // flip from filling to emptying
        showStats("before flip", fc, buffer);
        buffer.flip();
        showStats("after flip", fc, buffer);

        fc.close();
    }

    /**
     * Display state of channel/buffer.
     *
     * @param where description of where we are in the program to label the state snapzhot
     * @param fc    FileChannel reading/writing.
     * @param b     Buffer to display state of:
     * @throws IOException if i/o problems.
     */
    private static void showStats(String where, FileChannel fc, Buffer b) throws IOException {
        System.out
                .println(where +
                        " channelPosition=" +
                        fc.position() +
                        " position=" +
                        b.position() +
                        " limit=" +
                        b.limit() +
                        " remaining=" +
                        b.remaining() +
                        " capacity=" +
                        b.capacity());
    }

    /**
     * test harness
     *
     * @param args not used
     * @throws IOException if problems reading.
     */
    public static void main(String[] args) throws IOException {
        readRawBytes();
    }

    /**
     * Sets up the fixture, for example, open a network connection.
     * This method is called before a test is executed.
     */
    @Before
    public void setUp() throws Exception {
        file = IOHelper.createATempDir().resolve("test.dat");
        IOHelper.delete(file);
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (raf != null) {
                raf.close();
            }
        } finally {
            IOHelper.delete(file);
        }
    }

    @Test
    public void testIt() throws IOException {
        raf = new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        assertTrue(raf.withChannel(AbstractInterruptibleChannel::isOpen));
        byte[] array = new byte[16];
        final ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putLong(123456789);
        buffer.putLong(987654321);
        buffer.rewind();
        int n = raf.withChannel(channel -> channel.write(buffer));
        raf.withChannelVoid(channel -> channel.force(true));
        assertEquals(16, n);
        long size = raf.withChannel(FileChannel::size);
        assertEquals(16, size);
        raf.close();

        raf = new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ);
        size = raf.withChannel(FileChannel::size);
        assertEquals(16, size);
        array = new byte[16];
        final ByteBuffer rBuffer = ByteBuffer.wrap(array);
        raf.withChannel(channel -> channel.read(rBuffer));
        rBuffer.rewind();
        assertEquals(123456789, rBuffer.getLong());
        assertEquals(987654321, rBuffer.getLong());
        raf.close();
    }
}
