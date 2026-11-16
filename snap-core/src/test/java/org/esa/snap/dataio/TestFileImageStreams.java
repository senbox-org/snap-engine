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

import com.bc.ceres.core.io.EnhancedNIORandomAccessFile;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.FileNotFoundException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestFileImageStreams {

    public Path tempFolder = IOHelper.createATempDir();

    // === NIOFileImageInputStream ===

    @Test
    public void testInputStreamCreateAndRead() throws Exception {
        final Path file = tempFolder.resolve("input.bin");
        Files.writeString(file, "Hello World");

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            assertNotNull(stream);
            assertEquals(file, stream.getFile());
            assertTrue(stream.length() > 0);
        }
    }

    @Test
    public void testInputStreamReadByte() throws Exception {
        final Path file = tempFolder.resolve("byte.bin");
        Files.write(file, new byte[]{0x41, 0x42, 0x43}); // A, B, C

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            assertEquals(0x41, stream.readByte());
        }
    }

    @Test
    public void testInputStreamSeek() throws Exception {
        final Path file = tempFolder.resolve("seek.bin");
        Files.writeString(file, "ABCDEFGHIJ");

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            stream.seek(5);
            assertEquals('F', stream.readByte());
        }
    }

    @Test
    public void testInputStreamNegativeSkip() throws Exception {
        final Path file = tempFolder.resolve("negative-skip.bin");
        Files.writeString(file, "ABCDEFGHIJ");

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            stream.seek(5);
            assertEquals(-2L, stream.skipBytes(-2L));
            assertEquals('D', stream.readByte());
            assertEquals(-4L, stream.skipBytes(-99L));
            assertEquals(0L, stream.getStreamPosition());
        }
    }

    @Test
    public void testInputStreamSetByteOrder() throws Exception {
        final Path file = tempFolder.resolve("order.bin");
        Files.write(file, new byte[]{1, 2});

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            // Default should be BIG_ENDIAN
            assertEquals(ByteOrder.BIG_ENDIAN, stream.getByteOrder());

            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            assertEquals(ByteOrder.LITTLE_ENDIAN, stream.getByteOrder());
        }
    }

    @Test
    public void testInputStreamToString() throws Exception {
        final Path file = tempFolder.resolve("tostring.bin");
        Files.write(file, new byte[]{1});

        String desc;
        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            desc = stream.toString();
        }
        assertTrue(desc.contains("NIOFileImageInputStream"));
        assertTrue(desc.contains(file.toString()));
    }

    @Test
    public void testInputStreamCloseIsIdempotent() throws Exception {
        final Path file = tempFolder.resolve("close.bin");
        Files.write(file, new byte[]{1});

        NIOFileImageInputStream stream = new NIOFileImageInputStream(file);
        stream.close();
        stream.close(); // should not throw
    }

    @Test
    public void testInputStreamReadFully() throws Exception {
        final Path file = tempFolder.resolve("full.bin");
        byte[] data = {1, 2, 3, 4, 5};
        Files.write(file, data);

        byte[] buf;
        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            buf = new byte[5];
            stream.readFully(buf);
        }
        assertArrayEquals(data, buf);
    }

    @Test
    public void testInputStreamNestedMarkResetRestoresPositionAndBitOffset() throws Exception {
        final Path file = tempFolder.resolve("nested-mark-reset.bin");
        Files.write(file, new byte[]{(byte) 0xB5, (byte) 0x3C, (byte) 0x7A});

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            assertEquals(0b101, stream.readBits(3));
            stream.mark();
            assertEquals(0L, stream.getStreamPosition());
            assertEquals(3, stream.getBitOffset());

            assertEquals(0b10101, stream.readBits(5));
            stream.mark();
            assertEquals(1L, stream.getStreamPosition());
            assertEquals(0, stream.getBitOffset());

            assertEquals(0b0011, stream.readBits(4));
            stream.reset();
            assertEquals(1L, stream.getStreamPosition());
            assertEquals(0, stream.getBitOffset());

            stream.reset();
            assertEquals(0L, stream.getStreamPosition());
            assertEquals(3, stream.getBitOffset());
        }
    }

    @Test
    public void testInputStreamMarkResetAtFlushedPositionRemainsValid() throws Exception {
        final Path file = tempFolder.resolve("mark-reset-flush.bin");
        Files.write(file, new byte[]{1, 2, 3, 4});

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            stream.seek(2);
            stream.mark();
            stream.flushBefore(2);
            stream.readByte();
            stream.reset();

            assertEquals(2L, stream.getStreamPosition());
        }
    }

    @Test
    public void testInputStreamConcurrentReadsAreSerialized() throws Exception {
        final Path file = tempFolder.resolve("concurrent-read.bin");
        final byte[] data = new byte[4096];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        Files.write(file, data);

        final int threadCount = 8;
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicIntegerArray counts = new AtomicIntegerArray(256);
        final List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
                for (int i = 0; i < threadCount; i++) {
                    futures.add(executor.submit(() -> {
                        start.await();
                        int value;
                        while ((value = stream.read()) >= 0) {
                            counts.incrementAndGet(value);
                        }
                        return null;
                    }));
                }
                start.countDown();
                for (Future<?> future : futures) {
                    future.get();
                }
            } finally {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            }
        }

        for (int value = 0; value < counts.length(); value++) {
            assertEquals(data.length / 256, counts.get(value));
        }
    }

    @Test
    public void testInputStreamConcurrentCloseIsIdempotent() throws Exception {
        final Path file = tempFolder.resolve("concurrent-close.bin");
        Files.write(file, new byte[]{1});

        final int threadCount = 8;
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
                for (int i = 0; i < threadCount; i++) {
                    futures.add(executor.submit(() -> {
                        start.await();
                        stream.close();
                        return null;
                    }));
                }
                start.countDown();
                for (Future<?> future : futures) {
                    future.get();
                }
            } finally {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void testInputStreamNullFile() throws Exception {
        try (NIOFileImageInputStream fileImageInputStreamExt = new NIOFileImageInputStream(null)) {
            fileImageInputStreamExt.dispose();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testInputStreamNullFileWithBufferSize() throws Exception {
        new NIOFileImageInputStream(null, 1024).close();
    }

    @Test(expected = NullPointerException.class)
    public void testInputStreamRejectsNullEnhancedRandomAccessFile() throws Exception {
        new NIOFileImageInputStream(tempFolder.resolve("null-eraf.bin"), null).close();
    }

    @Test
    public void testInputStreamClosesEnhancedRandomAccessFileWhenConstructionFails() throws Exception {
        final Path file = tempFolder.resolve("construction-failure.bin");
        Files.write(file, new byte[]{1});
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ)) {

            try {
                new NIOFileImageInputStream(null, eraf);
                throw new AssertionError("Expected NullPointerException");
            } catch (NullPointerException expected) {
                // The supplied resource must be closed when construction fails.
            }

            try {
                eraf.getFilePointer();
                throw new AssertionError("Expected the supplied ERAF to be closed");
            } catch (java.io.IOException expected) {
                // Expected: the constructor closed eraf.
            }
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void testInputStreamNonExistentFile() throws Exception {
        try (NIOFileImageInputStream fileImageInputStreamExt = new NIOFileImageInputStream(tempFolder.resolve("nonexistent.bin"))) {
            fileImageInputStreamExt.dispose();
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void testInputStreamDirectory() throws Exception {
        final Path directory = Files.createDirectory(tempFolder.resolve("input-directory"));
        new NIOFileImageInputStream(directory).close();
    }

    @Test
    public void testInputStreamCreateStaticMethod() throws Exception {
        final Path file = tempFolder.resolve("static.bin");
        Files.write(file, new byte[]{1, 2, 3});

        try (ImageInputStream stream = NIOFileImageInputStream.createInputStream(file)) {
            assertNotNull(stream);
            assertTrue(stream instanceof NIOFileImageInputStream);
        }
    }

    @Test
    public void testInputStreamWithBufferSize() throws Exception {
        final Path file = tempFolder.resolve("buffered.bin");
        Files.writeString(file, "buffered content");

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file, 1024)) {
            assertNotNull(stream);
            assertTrue(stream.length() > 0);
        }
    }

    @Test
    public void testInputStreamReadsRepresentativeBitLengthsAtEveryOffset() throws Exception {
        final int[] bitLengths = {1, 7, 8, 9, 15, 16, 31, 32, 63, 64};
        final long value = 0x0123456789ABCDEFL;
        final int prefixValue = 0xA5;

        for (int offset = 0; offset <= 7; offset++) {
            for (int bitLength : bitLengths) {
                final Path file = tempFolder.resolve("input-bits-" + offset + "-" + bitLength + ".bin");
                Files.write(file, expectedBits(prefixValue, offset, value, bitLength));

                try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
                    assertEquals(prefixValue & bitMask(offset), stream.readBits(offset));
                    assertEquals(value & bitMask(bitLength), stream.readBits(bitLength));
                    assertEquals((offset + bitLength) / 8, stream.getStreamPosition());
                    assertEquals((offset + bitLength) % 8, stream.getBitOffset());
                }
            }
        }
    }

    @Test
    public void testInputStreamReads64BitsWithHighBitAtEveryNonzeroOffset() throws Exception {
        final long value = 0xFEDCBA9876543210L;
        final int prefixValue = 0x7F;

        for (int offset = 1; offset <= 7; offset++) {
            final Path file = tempFolder.resolve("input-high-bit-" + offset + ".bin");
            Files.write(file, expectedBits(prefixValue, offset, value, 64));

            try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
                assertEquals(prefixValue & bitMask(offset), stream.readBits(offset));
                assertEquals(value, stream.readBits(64));
                assertEquals((offset + 64) / 8, stream.getStreamPosition());
                assertEquals((offset + 64) % 8, stream.getBitOffset());
            }
        }
    }

    @Test
    public void testInputStreamReadBitMatchesIndividualBits() throws Exception {
        final Path file = tempFolder.resolve("input-single-bits.bin");
        final byte[] data = {(byte) 0xA5, (byte) 0x3C};
        Files.write(file, data);

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            for (int bitIndex = 0; bitIndex < data.length * 8; bitIndex++) {
                final int expected = (data[bitIndex / 8] >>> (7 - bitIndex % 8)) & 1;
                assertEquals(expected, stream.readBit());
            }
            assertEquals(2, stream.getStreamPosition());
            assertEquals(0, stream.getBitOffset());
        }
    }

    @Test
    public void testInputStreamReadBitsValidatesRangeAndHandlesZeroBits() throws Exception {
        final Path file = tempFolder.resolve("input-bit-range.bin");
        Files.write(file, new byte[]{0x01});

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            assertEquals(0L, stream.readBits(0));
            assertEquals(0, stream.getStreamPosition());
            assertEquals(0, stream.getBitOffset());
            assertThrows(IllegalArgumentException.class, () -> stream.readBits(-1));
            assertThrows(IllegalArgumentException.class, () -> stream.readBits(65));
        }
    }

    @Test
    public void testInputStreamReadBitsThrowsAtEndOfFile() throws Exception {
        final Path file = tempFolder.resolve("input-bit-eof.bin");
        Files.write(file, new byte[]{(byte) 0xA5});

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            assertThrows(java.io.EOFException.class, () -> stream.readBits(9));
        }
    }

    // === NIOFileImageOutputStream ===

    @Test
    public void testOutputStreamCreateAndWrite() throws Exception {
        final Path file = tempFolder.resolve("output.bin");

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            assertNotNull(stream);
            assertEquals(file, stream.getFile());

            stream.write(0x41);
        }

        final byte[] content = Files.readAllBytes(file);
        assertEquals(1, content.length);
        assertEquals(0x41, content[0]);
    }

    @Test
    public void testOutputStreamWriteBytes() throws Exception {
        final Path file = tempFolder.resolve("bytes.bin");

        final byte[] data;
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            data = new byte[]{1, 2, 3, 4, 5};
            stream.write(data, 0, data.length);
        }

        assertArrayEquals(data, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamFlushesPendingBits() throws Exception {
        final Path file = tempFolder.resolve("bits.bin");

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xABC, 12);
            stream.flush();
        }

        try (NIOFileImageInputStream stream = new NIOFileImageInputStream(file)) {
            assertEquals(0xAB, stream.readUnsignedByte());
            assertEquals(0xC0, stream.readUnsignedByte());
        }
    }

    @Test
    public void testOutputStreamWrites64Bits() throws Exception {
        final Path file = tempFolder.resolve("64-bits.bin");

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0x0123456789ABCDEFL, 64);
        }

        assertArrayEquals(new byte[]{0x01, 0x23, 0x45, 0x67, (byte) 0x89,
                        (byte) 0xAB, (byte) 0xCD, (byte) 0xEF},
                Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamWritesBitsAfterPartialByte() throws Exception {
        final Path file = tempFolder.resolve("mixed-bits.bin");

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            stream.writeBits(0xABCD, 16);
        }

        assertArrayEquals(new byte[]{(byte) 0xAA, (byte) 0xBC, (byte) 0xD0},
                Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamWrites64BitsAfterPartialByte() throws Exception {
        final Path file = tempFolder.resolve("mixed-64-bits.bin");

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            stream.writeBits(0x0123456789ABCDEFL, 64);
        }

        assertArrayEquals(new byte[]{(byte) 0xA0, 0x12, 0x34, 0x56, 0x78,
                        (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0},
                Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamWritesRepresentativeBitLengthsAtEveryOffset() throws Exception {
        final int[] bitLengths = {1, 7, 8, 9, 15, 16, 31, 32, 63, 64};
        final long value = 0x0123456789ABCDEFL;
        final int prefixValue = 0xA5;

        for (int offset = 0; offset <= 7; offset++) {
            for (int bitLength : bitLengths) {
                final Path file = tempFolder.resolve("bits-" + offset + "-" + bitLength + ".bin");
                try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
                    stream.writeBits(prefixValue, offset);
                    stream.writeBits(value, bitLength);
                }

                assertArrayEquals(expectedBits(prefixValue, offset, value, bitLength),
                        Files.readAllBytes(file));
            }
        }
    }

    @Test
    public void testOutputStreamInvalidPrimitiveRangesPreservePendingBits() throws Exception {
        final Path file = tempFolder.resolve("invalid-primitive-ranges.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);

            assertThrows(IndexOutOfBoundsException.class, () -> stream.writeShorts(new short[1], 1, 1));
            assertEquals(4, stream.getBitOffset());
            assertThrows(IndexOutOfBoundsException.class, () -> stream.writeChars(new char[1], 1, 1));
            assertEquals(4, stream.getBitOffset());
            assertThrows(IndexOutOfBoundsException.class, () -> stream.writeInts(new int[1], 1, 1));
            assertEquals(4, stream.getBitOffset());
            assertThrows(IndexOutOfBoundsException.class, () -> stream.writeLongs(new long[1], 1, 1));
            assertEquals(4, stream.getBitOffset());
            assertThrows(IndexOutOfBoundsException.class, () -> stream.writeFloats(new float[1], 1, 1));
            assertEquals(4, stream.getBitOffset());
            assertThrows(IndexOutOfBoundsException.class, () -> stream.writeDoubles(new double[1], 1, 1));
            assertEquals(4, stream.getBitOffset());
        }

        assertArrayEquals(new byte[]{(byte) 0xA0}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamFlushBeforeFlushesPendingBitsAtCurrentPosition() throws Exception {
        final Path file = tempFolder.resolve("flush-before-pending-bits.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            stream.flushBefore(0);
            assertEquals(0, stream.getBitOffset());
            assertEquals(1, stream.getStreamPosition());
        }

        assertArrayEquals(new byte[]{(byte) 0xA0}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamFlushBeforeBeyondPositionPreservesPendingBits() throws Exception {
        final Path file = tempFolder.resolve("flush-before-invalid-pending-bits.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            assertThrows(IndexOutOfBoundsException.class, () -> stream.flushBefore(1));
            assertEquals(4, stream.getBitOffset());
        }

        assertArrayEquals(new byte[]{(byte) 0xA0}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamSeekResetsPendingBitOffsetBeforeWriting() throws Exception {
        final Path file = tempFolder.resolve("seek-after-pending-bits.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            stream.seek(0);
            assertEquals(0, stream.getBitOffset());
            stream.write(0x05);
        }

        assertArrayEquals(new byte[]{0x05}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamClosedOperationsFailWithIOException() throws Exception {
        final Path file = tempFolder.resolve("closed-output-operations.bin");
        final NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file);
        stream.close();

        assertThrows(java.io.IOException.class, () -> stream.write(1));
        assertThrows(java.io.IOException.class, () -> stream.writeBits(1, 1));
        assertThrows(java.io.IOException.class, () -> stream.flushBefore(0));
        assertThrows(java.io.IOException.class, () -> stream.seek(0));
    }

    @Test
    public void testOutputStreamReadResetsPendingBitOffset() throws Exception {
        final Path file = tempFolder.resolve("read-after-pending-bits.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            assertEquals(0xA0, stream.read());
            assertEquals(0, stream.getBitOffset());
            assertEquals(1, stream.getStreamPosition());
        }

        assertArrayEquals(new byte[]{(byte) 0xA0}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamInheritedBitAndCursorOperationsPreserveState() throws Exception {
        final Path file = tempFolder.resolve("inherited-output-state.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            stream.setBitOffset(0);
            assertEquals(0, stream.getBitOffset());

            stream.seek(0);
            stream.writeBits(0xAB, 8);
            stream.mark();
            stream.writeBits(0xCD, 8);
            stream.reset();
            assertEquals(1, stream.getStreamPosition());
            assertEquals(0, stream.getBitOffset());

            stream.skipBytes(1);
            assertEquals(2, stream.getStreamPosition());
            stream.flush();
            stream.flush();
        }

        assertArrayEquals(new byte[]{(byte) 0xAB, (byte) 0xCD}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamNegativeSkip() throws Exception {
        final Path file = tempFolder.resolve("negative-output-skip.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.write(new byte[]{'A', 'B', 'C'});
            assertEquals(-2L, stream.skipBytes(-2L));
            assertEquals(1L, stream.getStreamPosition());
            stream.writeByte('X');
        }

        assertArrayEquals(new byte[]{'A', 'X', 'C'}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamDisposeFlushesPendingBitsAndIsIdempotent() throws Exception {
        final Path file = tempFolder.resolve("dispose-pending-bits.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            stream.dispose();
            stream.dispose();
        }

        assertArrayEquals(new byte[]{(byte) 0xA0}, Files.readAllBytes(file));
    }

    private static byte[] expectedBits(int prefixValue, int prefixBits, long value, int valueBits) {
        final int totalBits = prefixBits + valueBits;
        final byte[] expected = new byte[(totalBits + 7) / 8];
        int bitIndex = 0;
        for (int index = prefixBits - 1; index >= 0; index--) {
            bitIndex = setExpectedBit(expected, bitIndex, (prefixValue >> index) & 1);
        }
        for (int index = valueBits - 1; index >= 0; index--) {
            bitIndex = setExpectedBit(expected, bitIndex, (int) ((value >> index) & 1L));
        }
        return expected;
    }

    private static long bitMask(int bitCount) {
        return bitCount == 64 ? -1L : (1L << bitCount) - 1;
    }

    private static int setExpectedBit(byte[] bytes, int bitIndex, int bit) {
        if (bit != 0) {
            bytes[bitIndex / 8] |= (byte) (1 << (7 - bitIndex % 8));
        }
        return bitIndex + 1;
    }

    @Test
    public void testOutputStreamRejectsOverflowingArrayRange() throws Exception {
        final Path file = tempFolder.resolve("bounds.bin");

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeInts(new int[1], Integer.MAX_VALUE, 1);
        } catch (IndexOutOfBoundsException expected) {
            return;
        }
        throw new AssertionError("Expected IndexOutOfBoundsException");
    }

    @Test
    public void testOutputStreamInvalidByteRangeDoesNotFlushPendingBits() throws Exception {
        final Path file = tempFolder.resolve("invalid-range.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            stream.write(new byte[1], 2, 1);
            throw new AssertionError("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // The pending bits must remain buffered until a valid operation or close.
        }

        assertArrayEquals(new byte[]{(byte) 0xA0}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamInvalidPrimitiveRangeDoesNotFlushPendingBits() throws Exception {
        final Path file = tempFolder.resolve("invalid-primitive-range.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            try {
                stream.writeInts(new int[1], 1, 1);
                throw new AssertionError("Expected IndexOutOfBoundsException");
            } catch (IndexOutOfBoundsException expected) {
                // The pending bits must remain buffered until a valid operation or close.
            }
        }

        assertArrayEquals(new byte[]{(byte) 0xA0}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamRejectsNullByteArray() throws Exception {
        final Path file = tempFolder.resolve("null-array.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.write((byte[]) null);
            throw new AssertionError("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    @Test
    public void testOutputStreamRejectsNullPrimitiveArrays() throws Exception {
        final Path file = tempFolder.resolve("null-primitive-arrays.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            assertThrows(NullPointerException.class, () -> stream.writeShorts(null, 0, 0));
            assertThrows(NullPointerException.class, () -> stream.writeChars(null, 0, 0));
            assertThrows(NullPointerException.class, () -> stream.writeInts(null, 0, 0));
            assertThrows(NullPointerException.class, () -> stream.writeLongs(null, 0, 0));
            assertThrows(NullPointerException.class, () -> stream.writeFloats(null, 0, 0));
            assertThrows(NullPointerException.class, () -> stream.writeDoubles(null, 0, 0));
        }
    }

    @Test
    public void testOutputStreamNullByteArrayDoesNotFlushPendingBits() throws Exception {
        final Path file = tempFolder.resolve("null-array-pending-bits.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.writeBits(0xA, 4);
            try {
                stream.write((byte[]) null);
                throw new AssertionError("Expected NullPointerException");
            } catch (NullPointerException expected) {
                // The pending bits must remain buffered until close.
            }
        }

        assertArrayEquals(new byte[]{(byte) 0xA0}, Files.readAllBytes(file));
    }

    @Test
    public void testBufferedOutputStreamLittleEndianPrimitiveWrite() throws Exception {
        final Path file = tempFolder.resolve("little-endian-output.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file, 2048)) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            stream.writeShort(0x1234);
            stream.writeInt(0x78563412);
        }

        assertArrayEquals(new byte[]{0x34, 0x12, 0x12, 0x34, 0x56, 0x78},
                Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamSeekAndWrite() throws Exception {
        final Path file = tempFolder.resolve("seekwrite.bin");

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.write(new byte[]{0, 0, 0, 0, 0}, 0, 5);
            stream.seek(2);
            stream.write(0xFF);
        }

        final byte[] content = Files.readAllBytes(file);
        assertEquals(5, content.length);
        assertEquals((byte) 0xFF, content[2]);
    }

    @Test
    public void testOutputStreamLength() throws Exception {
        final Path file = tempFolder.resolve("length.bin");

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.write(new byte[]{1, 2, 3}, 0, 3);
            assertEquals(3, stream.length());
        }
    }

    @Test
    public void testOutputStreamToString() throws Exception {
        final Path file = tempFolder.resolve("outstr.bin");

        final String desc;
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            desc = stream.toString();
        }
        assertTrue(desc.contains("NIOFileImageOutputStream"));
        assertTrue(desc.contains(file.toString()));
    }

    @Test
    public void testOutputStreamCloseIsIdempotent() throws Exception {
        final Path file = tempFolder.resolve("outclose.bin");

        NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file);
        stream.write(0x01);
        stream.close();
        stream.close(); // should not throw
    }

    @Test
    public void testOutputStreamCreateStaticMethod() throws Exception {
        final Path file = tempFolder.resolve("outstatic.bin");

        try (ImageOutputStream stream = NIOFileImageOutputStream.createOutputStream(file)) {
            assertNotNull(stream);
            assertTrue(stream instanceof NIOFileImageOutputStream);
        }
    }

    @Test
    public void testOutputStreamCanEditWithoutTruncatingExistingFile() throws Exception {
        final Path file = tempFolder.resolve("preserve-existing.bin");
        Files.write(file, new byte[]{1, 2, 3, 4, 5});

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file, false)) {
            stream.seek(2);
            stream.write(9);
        }

        assertArrayEquals(new byte[]{1, 2, 9, 4, 5}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamCanExtendExistingFileWithoutTruncating() throws Exception {
        final Path file = tempFolder.resolve("extend-existing.bin");
        Files.write(file, new byte[]{1, 2, 3});

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file, 2048, false)) {
            stream.seek(5);
            stream.write(9);
        }

        assertArrayEquals(new byte[]{1, 2, 3, 0, 0, 9}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamNonTruncatingModeCreatesMissingFile() throws Exception {
        final Path file = tempFolder.resolve("preserve-missing.bin");

        try (ImageOutputStream stream = NIOFileImageOutputStream.createOutputStream(file, 2048, false)) {
            stream.write(7);
        }

        assertArrayEquals(new byte[]{7}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamDefaultModePreservesExistingFile() throws Exception {
        final Path file = tempFolder.resolve("preserve-default.bin");
        Files.write(file, new byte[]{1, 2, 3});

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.seek(1);
            stream.write(9);
        }

        assertArrayEquals(new byte[]{1, 9, 3}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamExplicitTruncatingModeStillTruncatesExistingFile() throws Exception {
        final Path file = tempFolder.resolve("truncate-explicit.bin");
        Files.write(file, new byte[]{1, 2, 3});

        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file, true)) {
            stream.write(9);
        }

        assertArrayEquals(new byte[]{9}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamWithBufferSize() throws Exception {
        final Path file = tempFolder.resolve("outbuf.bin");
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file, 2048)) {
            stream.write(new byte[]{10, 20, 30}, 0, 3);
        }

        assertArrayEquals(new byte[]{10, 20, 30}, Files.readAllBytes(file));
    }

    @Test
    public void testOutputStreamFlushMakesBytesVisibleForBothBufferConfigurations() throws Exception {
        final byte[] expected = {10, 20, 30};
        for (int bufferSize : new int[]{-1, 2048}) {
            final Path file = tempFolder.resolve("flush-visible-" + bufferSize + ".bin");
            try (NIOFileImageOutputStream stream = bufferSize < 0
                    ? new NIOFileImageOutputStream(file)
                    : new NIOFileImageOutputStream(file, bufferSize)) {
                stream.write(expected, 0, expected.length);
                stream.flush();
                assertArrayEquals(expected, Files.readAllBytes(file));
            }
        }
    }

    @Test
    public void testOutputStreamFlushCompletesPendingBitsForBothBufferConfigurations() throws Exception {
        final byte[] expected = {(byte) 0xA0};
        for (int bufferSize : new int[]{-1, 2048}) {
            final Path file = tempFolder.resolve("flush-pending-bits-" + bufferSize + ".bin");
            try (NIOFileImageOutputStream stream = bufferSize < 0
                    ? new NIOFileImageOutputStream(file)
                    : new NIOFileImageOutputStream(file, bufferSize)) {
                stream.writeBits(0xA, 4);
                stream.flush();
                assertEquals(0, stream.getBitOffset());
                assertEquals(1, stream.getStreamPosition());
                assertArrayEquals(expected, Files.readAllBytes(file));

                stream.flush();
                assertEquals(0, stream.getBitOffset());
                assertEquals(1, stream.getStreamPosition());
                assertArrayEquals(expected, Files.readAllBytes(file));
            }
        }
    }

    @Test
    public void testOutputStreamReadBack() throws Exception {
        final Path file = tempFolder.resolve("readback.bin");

        final int val;
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file)) {
            stream.write(new byte[]{65, 66, 67}, 0, 3);
            stream.seek(0);
            val = stream.read();
        }
        assertEquals(65, val);
    }

    @Test
    public void testOutputStreamConcurrentByteWritesAreSerialized() throws Exception {
        final Path file = tempFolder.resolve("concurrent-bytes.bin");
        final int threadCount = 8;
        final int writesPerThread = 512;
        final CountDownLatch start = new CountDownLatch(1);
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file);
             ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            final List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < threadCount; thread++) {
                final int value = thread;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < writesPerThread; i++) {
                        stream.write(value);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        }

        final byte[] content = Files.readAllBytes(file);
        assertEquals(threadCount * writesPerThread, content.length);
        final int[] counts = new int[threadCount];
        for (byte value : content) {
            assertTrue((value & 0xff) < threadCount);
            counts[value & 0xff]++;
        }
        for (int count : counts) {
            assertEquals(writesPerThread, count);
        }
    }

    @Test
    public void testOutputStreamConcurrentBitWritesAreSerialized() throws Exception {
        final Path file = tempFolder.resolve("concurrent-bits.bin");
        final int threadCount = 8;
        final int writesPerThread = 100;
        final CountDownLatch start = new CountDownLatch(1);
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file);
             ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            final List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < threadCount; thread++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < writesPerThread; i++) {
                        stream.writeBits(0xA, 4);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        }

        final byte[] content = Files.readAllBytes(file);
        assertEquals(threadCount * writesPerThread / 2, content.length);
        for (byte value : content) {
            assertEquals((byte) 0xAA, value);
        }
    }

    @Test
    public void testOutputStreamConcurrentCloseIsSafe() throws Exception {
        final Path file = tempFolder.resolve("concurrent-close.bin");
        final int threadCount = 8;
        final CountDownLatch start = new CountDownLatch(1);
        try (NIOFileImageOutputStream stream = new NIOFileImageOutputStream(file);
             ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            stream.writeBits(0xA, 4);
            final List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < threadCount; thread++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    stream.close();
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        }

        assertArrayEquals(new byte[]{(byte) 0xA0}, Files.readAllBytes(file));
    }
}
