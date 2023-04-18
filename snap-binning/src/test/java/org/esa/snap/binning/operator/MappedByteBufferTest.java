/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static org.junit.Assert.assertEquals;

/**
 * Tests runtime behaviour and performance of {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}.
 * May be used to store intermediate spatial bins.
 *
 * @author Norman Fomferra
 */
public class MappedByteBufferTest {

    private static final int MiB = 1024 * 1024;

    private File file;

    @Before
    public void setUp() throws Exception {
        file = genTestFile();
    }

    @After
    public void tearDown() {
        deleteFile(file);
    }

    @Test
    public void testThatFileMappingsCanGrow() throws Exception {

        final int chunkSize = 100 * MiB;

        final RandomAccessFile raf1 = new RandomAccessFile(file, "rw");
        final FileChannel gc1 = raf1.getChannel();
        try {
            final MappedByteBuffer buffer1 = gc1.map(FileChannel.MapMode.READ_WRITE, 0, chunkSize);
            buffer1.putDouble(0, 0.111);
            buffer1.putDouble(chunkSize - 8, 1.222);
        } finally {
            raf1.close();
            assertEquals(chunkSize, file.length());
        }

        final RandomAccessFile raf2 = new RandomAccessFile(file, "rw");
        final FileChannel fc2 = raf2.getChannel();
        try {
            final MappedByteBuffer buffer2 = fc2.map(FileChannel.MapMode.READ_WRITE, 0, 2 * chunkSize);
            assertEquals(0.111, buffer2.getDouble(0), 1e-10);
            assertEquals(1.222, buffer2.getDouble(chunkSize - 8), 1e-10);
            buffer2.putDouble(2 * chunkSize - 8, 2.333);
        } finally {
            raf2.close();
            assertEquals(2 * chunkSize, file.length());
        }

        final RandomAccessFile raf3 = new RandomAccessFile(file, "rw");
        final FileChannel fc3 = raf3.getChannel();
        try {
            final MappedByteBuffer buffer3 = fc3.map(FileChannel.MapMode.READ_WRITE, 0, 3 * chunkSize);
            assertEquals(0.111, buffer3.getDouble(0), 1e-10);
            assertEquals(1.222, buffer3.getDouble(chunkSize - 8), 1e-10);
            assertEquals(2.333, buffer3.getDouble(2 * chunkSize - 8), 1e-10);
            buffer3.putDouble(3 * chunkSize - 8, 3.444);
        } finally {
            fc3.close();
            raf3.close();
            assertEquals(3 * chunkSize, file.length());
        }

        final RandomAccessFile raf4 = new RandomAccessFile(file, "rw");
        final FileChannel fc4 = raf4.getChannel();
        try {
            final MappedByteBuffer buffer4 = fc4.map(FileChannel.MapMode.READ_WRITE, 0, 3 * chunkSize);
            assertEquals(0.111, buffer4.getDouble(0), 1e-10);
            assertEquals(1.222, buffer4.getDouble(chunkSize - 8), 1e-10);
            assertEquals(2.333, buffer4.getDouble(2 * chunkSize - 8), 1e-10);
            assertEquals(3.444, buffer4.getDouble(3 * chunkSize - 8), 1e-10);
        } finally {
            raf4.close();
            assertEquals(3 * chunkSize, file.length());
        }
    }


    private static long getFreeMiB() {
        return Runtime.getRuntime().freeMemory() / MiB;
    }

    static File genTestFile() throws IOException {
        return File.createTempFile(MappedByteBufferTest.class.getSimpleName() + "-", ".dat");
    }

    static void deleteFile(File file) {
        if (file.exists()) {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }
}
