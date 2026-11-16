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


import com.bc.ceres.core.io.EnhancedNIORandomAccessFile;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.assertEquals;

/**
 * Tests runtime behaviour and performance of {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}.
 * May be used to store intermediate spatial bins.
 *
 * @author Norman Fomferra
 */
public class MappedByteBufferTest {

    private static final int MiB = 1024 * 1024;

    private Path file;

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

        try (final EnhancedNIORandomAccessFile raf1 = new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            try {
                final MappedByteBuffer buffer1 = raf1.withChannel(gc1 -> gc1.map(FileChannel.MapMode.READ_WRITE, 0, chunkSize));
                buffer1.putDouble(0, 0.111);
                buffer1.putDouble(chunkSize - 8, 1.222);
            } finally {
                assertEquals(chunkSize, Files.size(file));
            }
        }

        try (final EnhancedNIORandomAccessFile raf1 = new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            try {
                final MappedByteBuffer buffer2 = raf1.withChannel(fc2 -> fc2.map(FileChannel.MapMode.READ_WRITE, 0, 2 * chunkSize));
                assertEquals(0.111, buffer2.getDouble(0), 1e-10);
                assertEquals(1.222, buffer2.getDouble(chunkSize - 8), 1e-10);
                buffer2.putDouble(2 * chunkSize - 8, 2.333);
            } finally {
                assertEquals(2 * chunkSize, Files.size(file));
            }
        }

        try (final EnhancedNIORandomAccessFile raf3 = new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            try {
                final MappedByteBuffer buffer3 = raf3.withChannel(fc3 -> fc3.map(FileChannel.MapMode.READ_WRITE, 0, 3 * chunkSize));
                assertEquals(0.111, buffer3.getDouble(0), 1e-10);
                assertEquals(1.222, buffer3.getDouble(chunkSize - 8), 1e-10);
                assertEquals(2.333, buffer3.getDouble(2 * chunkSize - 8), 1e-10);
                buffer3.putDouble(3 * chunkSize - 8, 3.444);
            } finally {
                assertEquals(3 * chunkSize, Files.size(file));
            }
        }

        try (final EnhancedNIORandomAccessFile raf4 = new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            try {
                final MappedByteBuffer buffer4 = raf4.withChannel(fc4 -> fc4.map(FileChannel.MapMode.READ_WRITE, 0, 3 * chunkSize));
                assertEquals(0.111, buffer4.getDouble(0), 1e-10);
                assertEquals(1.222, buffer4.getDouble(chunkSize - 8), 1e-10);
                assertEquals(2.333, buffer4.getDouble(2 * chunkSize - 8), 1e-10);
                assertEquals(3.444, buffer4.getDouble(3 * chunkSize - 8), 1e-10);
            } finally {
                assertEquals(3 * chunkSize, Files.size(file));
            }
        }
    }


    private static long getFreeMiB() {
        return Runtime.getRuntime().freeMemory() / MiB;
    }

    static Path genTestFile() {
        return IOHelper.createTempFile(MappedByteBufferTest.class.getSimpleName() + "-", ".dat");
    }

    static void deleteFile(Path file) {
        if (Files.exists(file)) {
            if (!IOHelper.delete(file)) {
                IOHelper.deleteOnExit(file);
            }
        }
    }
}
