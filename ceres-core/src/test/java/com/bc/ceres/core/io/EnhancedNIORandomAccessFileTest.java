package com.bc.ceres.core.io;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.junit.After;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class EnhancedNIORandomAccessFileTest {

    private final Path tempFolder = IOHelper.createATempDir();

    @After
    public void deleteTemporaryFiles() {
        if (Files.notExists(tempFolder)) {
            return;
        }
        assertTrue("Failed to delete temporary test directory: " + tempFolder,
                IOHelper.delete(tempFolder));
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeFlushesBufferedWrites() throws Exception {
        final Path file = tempFolder.resolve("buffered.bin");
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 16, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            eraf.write("hello".getBytes(StandardCharsets.UTF_8));
        }
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeIsIdempotent() throws Exception {
        final Path file = tempFolder.resolve("idempotent.bin");
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 16, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            eraf.writeByte(7);

            eraf.close();
            eraf.close();
            assertEquals(1, Files.size(file));
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeRetriesCacheDeletionAfterFailure() throws Exception {
        final Path file = tempFolder.resolve("cache-cleanup-retry.bin");
        final Path blockingDirectory = tempFolder.resolve("cache-cleanup-retry-directory");
        final Path blockingFile = blockingDirectory.resolve("child");
        Files.createDirectory(blockingDirectory);
        Files.write(blockingFile, new byte[]{1});

        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 16,
                StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            final Field stateField = EnhancedNIORandomAccessFile.class.getDeclaredField("state");
            stateField.setAccessible(true);
            final Object state = stateField.get(eraf);
            final Field cacheFileField = state.getClass().getDeclaredField("cacheFile");
            cacheFileField.setAccessible(true);
            cacheFileField.set(state, blockingDirectory);

            try {
                eraf.close();
                fail("closing must report a cache deletion failure");
            } catch (IOException expected) {
                // The non-empty directory is retained as the pending cache resource.
            }

            assertTrue(Files.exists(blockingDirectory));
            Files.delete(blockingFile);
            eraf.close();
            assertFalse(Files.exists(blockingDirectory));
        } finally {
            Files.deleteIfExists(blockingFile);
            Files.deleteIfExists(blockingDirectory);
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeTruncatesToMinLength() throws Exception {
        final Path file = tempFolder.resolve("truncate.bin");
        Files.writeString(file, "0123456789");
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 16, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            eraf.setMinLength(4);
        }
        assertEquals(4L, Files.size(file));
        assertArrayEquals("0123".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeExtendsToMinLength() throws Exception {
        final Path file = tempFolder.resolve("extend.bin");
        Files.write(file, new byte[]{1, 2, 3});
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 16, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            eraf.setMinLength(8);
        }
        assertEquals(8L, Files.size(file));
        final byte[] content = Files.readAllBytes(file);
        assertEquals(1, content[0]);
        assertEquals(2, content[1]);
        assertEquals(3, content[2]);
        assertArrayEquals(new byte[]{1, 2, 3, 0, 0, 0, 0, 0}, content);
    }

    @Test
    @STTM("SNAP-4105")
    public void test_closeOnReadonlyFile() throws Exception {
        final Path file = tempFolder.resolve("readonly.bin");
        Files.writeString(file, "abc");
        new EnhancedNIORandomAccessFile(file, 16, StandardOpenOption.READ).close();
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));
    }

    @Test
    public void testByteReadWriteAndPositioningForCachedAndUncachedFiles() throws Exception {
        for (boolean cached : new boolean[]{false, true}) {
            final Path file = tempFolder.resolve("bytes-" + cached + ".bin");
            try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, cached, 3)) {
                eraf.write(new byte[]{0, 1, 2, 3, 4});
                assertEquals(5L, eraf.getFilePointer());
                assertEquals(5L, eraf.length());

                eraf.seek(1);
                assertEquals(1, eraf.readUnsignedByte());
                eraf.unread();
                assertEquals(1, eraf.readUnsignedByte());

                eraf.seek(0);
                final byte[] actual = new byte[5];
                assertEquals(5, eraf.read(actual));
                assertArrayEquals(new byte[]{0, 1, 2, 3, 4}, actual);
                assertEquals(-1, eraf.read());
                assertTrue(eraf.isAtEndOfFile());
            }
        }
    }

    @Test
    public void testSkipBytesAndArgumentValidation() throws Exception {
        final Path file = tempFolder.resolve("skip.bin");
        Files.write(file, new byte[]{10, 11, 12, 13});
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 2, StandardOpenOption.READ)) {
            assertEquals(2L, eraf.skipBytes(2L));
            assertEquals(12, eraf.readUnsignedByte());
            assertEquals(1L, eraf.skipBytes(99L));
            assertEquals(0L, eraf.skipBytes(1L));
            assertEquals(-2L, eraf.skipBytes(-2L));
            assertEquals(12, eraf.readUnsignedByte());
            assertEquals(-3L, eraf.skipBytes(-99L));
            assertEquals(0L, eraf.getFilePointer());
            assertEquals(0L, eraf.skipBytes(-1L));
            try {
                eraf.seek(-1);
                fail("negative seek must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }
        }
    }

    @Test
    public void testPrimitiveRoundTripInBothByteOrders() throws Exception {
        for (ByteOrder order : new ByteOrder[]{ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN}) {
            final Path file = tempFolder.resolve("primitives-" + order + ".bin");
            try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 5)) {
                eraf.setByteOrder(order);
                eraf.writeBoolean(true);
                eraf.writeByte(-2);
                eraf.writeShort(0x8123);
                eraf.writeChar(0x4567);
                eraf.writeInt(0x81234567);
                eraf.writeLong(0x8123456789ABCDEFL);
                eraf.writeFloat(12.5f);
                eraf.writeDouble(-98.25);

                eraf.seek(0);
                assertTrue(eraf.readBoolean());
                assertEquals((byte) -2, eraf.readByte());
                assertEquals((short) 0x8123, eraf.readShort());
                assertEquals((char) 0x4567, eraf.readChar());
                assertEquals(0x81234567, eraf.readInt());
                assertEquals(0x8123456789ABCDEFL, eraf.readLong());
                assertEquals(12.5f, eraf.readFloat(), 0.0f);
                assertEquals(-98.25, eraf.readDouble(), 0.0);
            }
        }
    }

    @Test
    public void testBulkPrimitiveRoundTripWithOffsets() throws Exception {
        final Path file = tempFolder.resolve("bulk-primitives.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 4)) {
            final short[] shorts = {1, -2, 300};
            final char[] chars = {'a', '\u0000', '€'};
            final int[] ints = {1, -2, 0x12345678};
            final long[] longs = {1L, -2L, 0x123456789ABCDEFL};
            final float[] floats = {1.5f, -2.5f, 3.25f};
            final double[] doubles = {1.5, -2.5, 3.25};

            eraf.writeShorts(shorts, 0, shorts.length);
            eraf.writeChars(chars, 0, chars.length);
            eraf.writeInts(ints, 0, ints.length);
            eraf.writeLongs(longs, 0, longs.length);
            eraf.writeFloats(floats, 0, floats.length);
            eraf.writeDoubles(doubles, 0, doubles.length);

            eraf.seek(0);
            final short[] shortResult = new short[5];
            final char[] charResult = new char[5];
            final int[] intResult = new int[5];
            final long[] longResult = new long[5];
            final float[] floatResult = new float[5];
            final double[] doubleResult = new double[5];
            assertEquals(3, eraf.readShorts(shortResult, 1, 3));
            assertEquals(3, eraf.readChars(charResult, 1, 3));
            assertEquals(3, eraf.readInts(intResult, 1, 3));
            assertEquals(3, eraf.readLongs(longResult, 1, 3));
            assertEquals(3, eraf.readFloats(floatResult, 1, 3));
            assertEquals(3, eraf.readDoubles(doubleResult, 1, 3));
            assertArrayEquals(shorts, new short[]{shortResult[1], shortResult[2], shortResult[3]});
            assertArrayEquals(chars, new char[]{charResult[1], charResult[2], charResult[3]});
            assertArrayEquals(ints, new int[]{intResult[1], intResult[2], intResult[3]});
            assertArrayEquals(longs, new long[]{longResult[1], longResult[2], longResult[3]});
            assertArrayEquals(floats, new float[]{floatResult[1], floatResult[2], floatResult[3]}, 0.0f);
            assertArrayEquals(doubles, new double[]{doubleResult[1], doubleResult[2], doubleResult[3]}, 0.0);
        }
    }

    @Test
    public void testReadFullyAndTruncatedPrimitiveInput() throws Exception {
        final Path file = tempFolder.resolve("truncated.bin");
        Files.write(file, new byte[]{0x01});
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 2, StandardOpenOption.READ)) {
            try {
                eraf.readShort();
                fail("truncated short must fail");
            } catch (EOFException expected) {
                // expected
            }
            eraf.seek(0);
            try {
                eraf.readFully(new int[1], 0, 1);
                fail("truncated int must fail");
            } catch (EOFException expected) {
                // expected
            }
        }
    }

    @Test
    public void testReadLineAndStringEncodings() throws Exception {
        final Path file = tempFolder.resolve("lines.bin");
        Files.write(file, new byte[]{'a', '\r', 'b', '\n', '\r', '\n', '\r'});
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 2, StandardOpenOption.READ)) {
            assertEquals("a", eraf.readLine());
            assertEquals("b", eraf.readLine());
            assertEquals("", eraf.readLine());
            assertEquals("", eraf.readLine());
            assertNull(eraf.readLine());
        }

        final Path stringFile = tempFolder.resolve("string.bin");
        Files.writeString(stringFile, "hé");
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(stringFile, StandardOpenOption.READ)) {
            assertEquals("hé", eraf.readString(3, StandardCharsets.UTF_8));
            eraf.seek(0);
            assertEquals("hÃ©", eraf.readString(3));
            try {
                eraf.readString(-1);
                fail("negative string size must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }
            try {
                eraf.readString(0, null);
                fail("null charset must fail");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    @Test
    public void testReadWriteAccessModesAndWriteOnlySeekLength() throws Exception {
        final Path file = tempFolder.resolve("modes.bin");
        Files.createFile(file);
        try (EnhancedNIORandomAccessFile readOnly = new EnhancedNIORandomAccessFile(file, 4, StandardOpenOption.READ, StandardOpenOption.CREATE)) {
            try {
                readOnly.writeByte(1);
                fail("read-only write must fail");
            } catch (NonWritableChannelException expected) {
                // expected
            }
        }

        try (EnhancedNIORandomAccessFile writeOnly = new EnhancedNIORandomAccessFile(file, 4, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            writeOnly.seek(10);
            assertEquals(0L, writeOnly.length());
            try {
                writeOnly.read();
                fail("write-only read must fail");
            } catch (NonReadableChannelException expected) {
                // expected
            }
            writeOnly.writeByte(7);
            assertEquals(11L, writeOnly.length());
        }
        assertEquals(11L, Files.size(file));
    }

    @Test
    public void testChannelFlushAndFlushBefore() throws Exception {
        final Path file = tempFolder.resolve("channel.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 4)) {
            eraf.write(new byte[]{1, 2, 3, 4, 5});
            assertEquals(0L, eraf.getFlushedPosition());
            eraf.flushBefore(3);
            assertEquals(3L, eraf.getFlushedPosition());
            try {
                eraf.seek(2);
                fail("seeking before flushed position must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }
            final long chPos = eraf.withChannel(FileChannel::position);
            assertEquals(eraf.getFilePointer(), chPos);
            eraf.synch();
        }
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, Files.readAllBytes(file));
    }

    @Test
    public void testSynchronizeFromChannelInvalidatesCachedData() throws Exception {
        final Path file = tempFolder.resolve("channel-sync.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.write(new byte[]{'a', 'b', 'c'});
            eraf.seek(0);
            assertEquals('a', eraf.readUnsignedByte());

            eraf.withChannel(channel -> channel.position(0));
            final int wLen = eraf.withChannel(channel -> channel.write(ByteBuffer.wrap(new byte[]{'X'})));
            assertEquals(1, wLen);

            eraf.seek(0);
            assertEquals('X', eraf.readUnsignedByte());
            assertEquals('b', eraf.readUnsignedByte());
            assertEquals('c', eraf.readUnsignedByte());
        }
    }

    @Test
    public void testSynchronizeFromChannelAdoptsDirectChannelPosition() throws Exception {
        final Path file = tempFolder.resolve("channel-position-sync.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.write(new byte[]{1, 2, 3, 4});
            eraf.withChannel(channel -> channel.position(2));

            assertEquals(2L, eraf.getFilePointer());
        }
    }

    @Test
    public void testWithChannelFlushesAndResynchronizes() throws Exception {
        final Path file = tempFolder.resolve("channel-operation.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.write(new byte[]{'a', 'b', 'c'});

            final long channelPosition = eraf.withChannel(channel -> {
                channel.position(1);
                final int wLen = channel.write(ByteBuffer.wrap(new byte[]{'Y'}));
                assertEquals(1, wLen);
                return channel.position();
            });

            assertEquals(2L, channelPosition);
            assertEquals(2L, eraf.getFilePointer());
            eraf.seek(0);
            assertEquals('a', eraf.readUnsignedByte());
            assertEquals('Y', eraf.readUnsignedByte());
            assertEquals('c', eraf.readUnsignedByte());
        }
    }

    @Test
    public void testWithChannelVoidFlushesAndResynchronizes() throws Exception {
        final Path file = tempFolder.resolve("channel-void-operation.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.write(new byte[]{'a', 'b', 'c'});

            eraf.withChannelVoid(channel -> {
                channel.position(1);
                final int wLen = channel.write(ByteBuffer.wrap(new byte[]{'Y'}));
                assertEquals(1, wLen);
                channel.force(true);
            });

            assertEquals(2L, eraf.getFilePointer());
            eraf.seek(0);
            assertEquals('a', eraf.readUnsignedByte());
            assertEquals('Y', eraf.readUnsignedByte());
            assertEquals('c', eraf.readUnsignedByte());
        }
    }

    @Test
    public void testWithChannelVoidRejectsNullOperation() throws Exception {
        final Path file = tempFolder.resolve("channel-void-null-operation.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            try {
                eraf.withChannelVoid(null);
                fail("null channel operation must fail");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    @Test
    public void testWithChannelVoidPreservesOperationFailureWhenSynchronizationFails() throws Exception {
        final Path file = tempFolder.resolve("channel-void-operation-failure.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            try {
                eraf.withChannelVoid(channel -> {
                    channel.close();
                    throw new IOException("void operation failure");
                });
                fail("withChannelVoid must propagate the operation failure");
            } catch (IOException expected) {
                assertEquals("void operation failure", expected.getMessage());
                assertEquals(1, expected.getSuppressed().length);
                assertTrue(expected.getSuppressed()[0] instanceof IOException);
            }
        }
    }

    @Test
    public void testWithChannelVoidChecksClosedStateBeforeNullOperation() throws Exception {
        final Path file = tempFolder.resolve("channel-void-closed-null-operation.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.close();

            try {
                eraf.withChannelVoid(null);
                fail("closed ERAF must reject the operation before validating null");
            } catch (IOException expected) {
                assertEquals("closed", expected.getMessage());
            }
        }
    }

    @Test
    public void testWithChannelPreservesOperationFailureWhenSynchronizationFails() throws Exception {
        final Path file = tempFolder.resolve("channel-operation-failure.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            try {
                eraf.withChannel(channel -> {
                    channel.close();
                    throw new IOException("operation failure");
                });
                fail("withChannel must propagate the operation failure");
            } catch (IOException expected) {
                assertEquals("operation failure", expected.getMessage());
                assertEquals(1, expected.getSuppressed().length);
                assertTrue(expected.getSuppressed()[0] instanceof IOException);
            }
        }
    }

    @Test
    public void testWithChannelRejectsNullOperation() throws Exception {
        final Path file = tempFolder.resolve("channel-null-operation.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            try {
                eraf.withChannel(null);
                fail("null channel operation must fail");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    @Test
    public void testConcurrentWritesAreSerialized() throws Exception {
        final Path file = tempFolder.resolve("concurrent-writes.bin");
        try (ExecutorService executor = Executors.newFixedThreadPool(4);
             EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 32)) {
            final List<Future<?>> futures = new ArrayList<>();
            for (int task = 0; task < 4; task++) {
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < 100; i++) {
                        eraf.writeByte(i);
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertEachByteWasWrittenFourTimes(file);
    }

    @Test
    public void testConcurrentErafAndChannelOperationsAreSerialized() throws Exception {
        final Path file = tempFolder.resolve("concurrent-eraf-channel.bin");
        try (ExecutorService executor = Executors.newFixedThreadPool(4);
             EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 32)) {
            final List<Future<?>> futures = new ArrayList<>();
            for (int task = 0; task < 2; task++) {
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < 100; i++) {
                        eraf.writeByte(i);
                    }
                    return null;
                }));
            }
            for (int task = 0; task < 2; task++) {
                futures.add(executor.submit(() -> {
                    final AtomicInteger wLen = new AtomicInteger();
                    for (int i = 0; i < 100; i++) {
                        final int value = i;
                        eraf.withChannel(channel -> {
                            wLen.addAndGet(channel.write(ByteBuffer.wrap(new byte[]{(byte) value})));
                            return null;
                        });
                    }
                    assertEquals(100, wLen.get());
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertEachByteWasWrittenFourTimes(file);
    }

    @Test
    public void testFlushPersistsDataWithoutAdvancingFlushedPosition() throws Exception {
        final Path file = tempFolder.resolve("flush.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.write(new byte[]{1, 2, 3, 4});
            assertEquals(0L, eraf.getFlushedPosition());

            eraf.flush();

            assertEquals(0L, eraf.getFlushedPosition());
            assertArrayEquals(new byte[]{1, 2, 3, 4}, Files.readAllBytes(file));
            eraf.seek(0);
            assertEquals(1, eraf.readUnsignedByte());
        }
    }

    @Test
    public void testFlushBeforeRejectsInvalidPositions() throws Exception {
        final Path file = tempFolder.resolve("flush-before-ranges.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.write(new byte[]{1, 2, 3, 4});

            try {
                eraf.flushBefore(5);
                fail("flushBefore past the current position must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }

            eraf.flushBefore(2);
            try {
                eraf.flushBefore(1);
                fail("flushBefore before the flushed position must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }
        }
    }

    @Test
    public void testInvalidArrayRangesAndZeroLengthOperations() throws Exception {
        final Path file = tempFolder.resolve("ranges.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 4)) {
            assertEquals(0, eraf.readBytes(new byte[2], 0, 0));
            eraf.writeBytes(new byte[2], 0, 0);
            assertEquals(0, eraf.readShorts(new short[2], 0, 0));
            eraf.writeShorts(new short[2], 0, 0);
            try {
                eraf.readBytes(new byte[2], 2, 1);
                fail("invalid byte range must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }
            try {
                eraf.writeInts(new int[2], -1, 1);
                fail("invalid primitive range must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }
        }
    }

    @Test
    public void testReadIntUnbufferedSeesBufferedWritesAndPreservesPointer() throws Exception {
        final Path file = tempFolder.resolve("unbuffered-int.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.writeInt(0x01020304);
            assertEquals(4L, eraf.getFilePointer());

            assertEquals(0x01020304, eraf.readIntUnbuffered(0));
            assertEquals(4L, eraf.getFilePointer());

            try {
                eraf.readIntUnbuffered(-1);
                fail("negative unbuffered position must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }
        }
    }

    @Test
    public void testUnsignedReadsAndUncachedPrimitiveRoundTrip() throws Exception {
        final Path file = tempFolder.resolve("unsigned-uncached.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, false, 4)) {
            eraf.writeInt(-1);
            eraf.writeShort(0xFFFF);
            eraf.writeLong(0x0123456789ABCDEFL);
            eraf.writeFloat(3.5f);
            eraf.writeDouble(-7.25);

            eraf.seek(0);
            assertEquals(0xFFFFFFFFL, eraf.readUnsignedInt());
            assertEquals(0xFFFF, eraf.readUnsignedShort());
            assertEquals(0x0123456789ABCDEFL, eraf.readLong());
            assertEquals(3.5f, eraf.readFloat(), 0.0f);
            assertEquals(-7.25, eraf.readDouble(), 0.0);
        }
    }

    @Test
    public void testReadFloatsValidatesRangeAndReadsValues() throws Exception {
        final Path file = tempFolder.resolve("floats.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.writeFloats(new float[]{1.25f, -2.5f}, 0, 2);
            eraf.seek(0);

            final float[] values = new float[4];
            eraf.readFloats(values, 1, 2);
            assertArrayEquals(new float[]{1.25f, -2.5f},
                    new float[]{values[1], values[2]}, 0.0f);

            try {
                eraf.readFloats(values, -1, 1);
                fail("negative start must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }
            try {
                eraf.readFloats(values, 3, 2);
                fail("range past the array must fail");
            } catch (IndexOutOfBoundsException expected) {
                // expected
            }
        }
    }

    @Test
    public void testTextWritersRoundTrip() throws Exception {
        final Path file = tempFolder.resolve("text-writers.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            eraf.writeBytes("ABC");
            eraf.writeChars("xy");
            eraf.writeUTF("hello");

            eraf.seek(0);
            assertEquals("ABC", eraf.readString(3));
            assertEquals('x', eraf.readChar());
            assertEquals('y', eraf.readChar());
            assertEquals("hello", eraf.readUTF());
        }
    }

    @Test
    public void testEmptyStringWritesAfterCloseFail() throws Exception {
        final Path file = tempFolder.resolve("empty-string-after-close.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 4)) {
            eraf.close();

            try {
                eraf.writeBytes("");
                fail("empty writeBytes after close must fail");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.writeChars("");
                fail("empty writeChars after close must fail");
            } catch (IOException expected) {
                // expected
            }
        }
    }

    @Test
    public void testPrimitiveReadFullyOverloads() throws Exception {
        final Path file = tempFolder.resolve("read-fully-primitives.bin");
        try (EnhancedNIORandomAccessFile writer = openReadWrite(file, true, 4)) {
            writer.writeShort(0x1234);
            writer.writeChar('x');
            writer.writeInt(0x12345678);
            writer.writeLong(0x0123456789ABCDEFL);
            writer.writeFloat(1.5f);
            writer.writeDouble(-2.5);
        }

        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 4, StandardOpenOption.READ)) {
            final short[] shorts = new short[1];
            final char[] chars = new char[1];
            final int[] ints = new int[1];
            final long[] longs = new long[1];
            final float[] floats = new float[1];
            final double[] doubles = new double[1];
            eraf.readFully(shorts, 0, 1);
            eraf.readFully(chars, 0, 1);
            eraf.readFully(ints, 0, 1);
            eraf.readFully(longs, 0, 1);
            eraf.readFully(floats, 0, 1);
            eraf.readFully(doubles, 0, 1);

            assertEquals((short) 0x1234, shorts[0]);
            assertEquals('x', chars[0]);
            assertEquals(0x12345678, ints[0]);
            assertEquals(0x0123456789ABCDEFL, longs[0]);
            assertEquals(1.5f, floats[0], 0.0f);
            assertEquals(-2.5, doubles[0], 0.0);
        }
    }

    @Test
    public void testReadBytesPartialReadAndEof() throws Exception {
        final Path file = tempFolder.resolve("partial-read.bin");
        Files.write(file, new byte[]{1, 2, 3});
        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file, 2, StandardOpenOption.READ)) {
            final byte[] values = new byte[5];
            assertEquals(3, eraf.readBytes(values, 0, values.length));
            assertArrayEquals(new byte[]{1, 2, 3}, new byte[]{values[0], values[1], values[2]});
            assertEquals(-1, eraf.readBytes(values, 0, values.length));
            assertEquals(0, eraf.readBytes(values, 0, 0));
        }
    }

    @Test
    public void testCacheStateAndLocation() throws Exception {
        final Path file = tempFolder.resolve("cache-state.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 4)) {
            assertTrue(eraf.isCached());
            assertTrue(eraf.isCachedMemory());
            assertFalse(eraf.isCachedFile());

            eraf.setLocation(tempFolder.toString());
            assertEquals(tempFolder.toString(), eraf.getLocation());
            assertTrue(eraf.isCachedFile());
            assertFalse(eraf.isCachedMemory());

            eraf.setLocation(null);
            assertNull(eraf.getLocation());
            assertFalse(eraf.isCachedFile());
            assertTrue(eraf.isCachedMemory());

            eraf.setCached(false);
            assertFalse(eraf.isCached());
            eraf.setCached(true);
            assertTrue(eraf.isCached());
        }
    }

    @Test
    public void testFileCacheSupportsRandomReadsAndIsRemovedOnDeactivation() throws Exception {
        final Path file = tempFolder.resolve("disk-cache.bin");
        Files.write(file, new byte[]{0, 1, 2, 3, 4, 5, 6, 7});
        final long cacheCountBefore = countCacheFiles();

        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file,
                StandardOpenOption.READ)) {
            eraf.setCached(false);
            eraf.setLocation(tempFolder.toString());
            assertEquals(cacheCountBefore + 1, countCacheFiles());

            eraf.seek(4);
            assertEquals(4, eraf.readUnsignedByte());
            eraf.seek(0);
            assertEquals(0, eraf.readUnsignedByte());

            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
                channel.position(4);
                final int wLen = channel.write(ByteBuffer.wrap(new byte[]{40}));
                assertEquals(1, wLen);
            }
            eraf.seek(4);
            assertEquals(4, eraf.readUnsignedByte());

            eraf.setLocation(null);
            assertEquals(cacheCountBefore, countCacheFiles());
            eraf.seek(4);
            assertEquals(40, eraf.readUnsignedByte());
        }
        assertEquals(cacheCountBefore, countCacheFiles());
    }

    @Test
    public void testFileCacheWritesThroughForUncachedWrites() throws Exception {
        final Path file = tempFolder.resolve("disk-cache-write.bin");
        Files.write(file, new byte[]{0, 0, 0, 0});
        final long cacheCountBefore = countCacheFiles();

        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file,
                StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            eraf.setLocation(tempFolder.toString());
            assertEquals(cacheCountBefore + 1, countCacheFiles());
            eraf.setCached(false);
            eraf.seek(2);
            eraf.writeByte(22);
            eraf.seek(2);
            assertEquals(22, eraf.readUnsignedByte());
        }

        assertArrayEquals(new byte[]{0, 0, 22, 0}, Files.readAllBytes(file));
        assertEquals(cacheCountBefore, countCacheFiles());
    }

    @Test
    public void testInvalidCacheLocationPreservesActiveCache() throws Exception {
        final Path file = tempFolder.resolve("invalid-cache-location.bin");
        Files.write(file, new byte[]{1, 2, 3});
        final long cacheCountBefore = countCacheFiles();

        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file,
                StandardOpenOption.READ)) {
            eraf.setLocation(tempFolder.toString());
            assertEquals(cacheCountBefore + 1, countCacheFiles());

            try {
                eraf.setLocation(file.toString());
                fail("a regular file must not be accepted as cache directory");
            } catch (IllegalArgumentException expected) {
                // expected
            }

            assertEquals(tempFolder.toString(), eraf.getLocation());
            assertTrue(eraf.isCachedFile());
            assertEquals(cacheCountBefore + 1, countCacheFiles());
        }
        assertEquals(cacheCountBefore, countCacheFiles());
    }

    @Test
    public void testCloseCleansCacheAfterSourceChannelWasClosedExternally() throws Exception {
        final Path file = tempFolder.resolve("externally-closed-cache.bin");
        Files.write(file, new byte[]{1, 2, 3});
        final long cacheCountBefore = countCacheFiles();

        try (EnhancedNIORandomAccessFile eraf = new EnhancedNIORandomAccessFile(file,
                StandardOpenOption.READ)) {
            eraf.setLocation(tempFolder.toString());
            assertEquals(cacheCountBefore + 1, countCacheFiles());

            try {
                eraf.withChannel(channel -> {
                    channel.close();
                    return null;
                });
                fail("channel closure must be reported");
            } catch (IOException expected) {
                // expected
            }

            try {
                eraf.close();
                fail("closing an ERAF with an externally closed source must report the failure");
            } catch (IOException expected) {
                // expected
            }
            assertEquals(cacheCountBefore, countCacheFiles());
        }
    }

    @Test
    public void testCacheToggleAtNonZeroPositionPreservesWrites() throws Exception {
        final Path file = tempFolder.resolve("cache-toggle-non-zero.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 4)) {
            eraf.seek(5);
            eraf.writeByte(0x11);

            eraf.setCached(false);
            eraf.setCached(true);
            eraf.write(new byte[]{0x22, 0x33});
        }

        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0x11, 0x22, 0x33}, Files.readAllBytes(file));
    }

    @Test
    public void testMetadataAndByteOrderAccessors() throws Exception {
        final Path file = tempFolder.resolve("metadata.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 4)) {
            assertEquals(file, eraf.getFile());
            assertEquals(ByteOrder.BIG_ENDIAN, eraf.getByteOrder());
            eraf.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            assertEquals(ByteOrder.LITTLE_ENDIAN, eraf.getByteOrder());
            assertTrue(eraf.toString().contains("fp="));

            try {
                eraf.setByteOrder(null);
                fail("null byte order must fail");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    @Test
    public void testOperationsAfterCloseThrowIOException() throws Exception {
        final Path file = tempFolder.resolve("after-close.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 4)) {
            eraf.close();

            try {
                eraf.read();
                fail("read after close must fail");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.writeByte(1);
                fail("write after close must fail");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.seek(0);
                fail("seek after close must fail");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.length();
                fail("length after close must fail");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.setCached(false);
                fail("setCached after close must fail");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.synch();
                fail("synch after close must fail");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.read((byte[]) null);
                fail("read after close must fail before null validation");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.readFully((byte[]) null);
                fail("readFully after close must fail before null validation");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.readFully((byte[]) null, 0, 0);
                fail("readFully range overload after close must fail before validation");
            } catch (IOException expected) {
                // expected
            }
            try {
                eraf.write((byte[]) null);
                fail("write after close must fail before null validation");
            } catch (IOException expected) {
                // expected
            }
        }
    }

    @Test
    public void testInvalidBufferSizesAreRejected() throws Exception {
        final Path file = tempFolder.resolve("invalid-buffer.bin");
        for (int bufferSize : new int[]{0, -1}) {
            try {
                new EnhancedNIORandomAccessFile(file, bufferSize,
                        StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE).close();
                fail("non-positive buffer size must fail");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
        try {
            new EnhancedNIORandomAccessFile(file, new byte[0],
                    StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE).close();
            fail("empty buffer must fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void testNullConstructorArgumentsAreRejected() throws Exception {
        final Path file = tempFolder.resolve("null-constructor-arguments.bin");

        try {
            new EnhancedNIORandomAccessFile(null, new byte[4], StandardOpenOption.READ).close();
            fail("null file must fail");
        } catch (IllegalArgumentException expected) {
            assertEquals("file cannot be null", expected.getMessage());
        }

        try {
            new EnhancedNIORandomAccessFile(file, (byte[]) null, StandardOpenOption.READ).close();
            fail("null buffer must fail");
        } catch (IllegalArgumentException expected) {
            assertEquals("buffer cannot be null", expected.getMessage());
        }

        try {
            new EnhancedNIORandomAccessFile(file, new byte[4], (StandardOpenOption[]) null).close();
            fail("null modes must fail");
        } catch (IllegalArgumentException expected) {
            assertEquals("modes cannot be null", expected.getMessage());
        }

        try {
            new EnhancedNIORandomAccessFile(file, new byte[4], StandardOpenOption.READ, null).close();
            fail("null mode entry must fail");
        } catch (IllegalArgumentException expected) {
            assertEquals("modes cannot contain null", expected.getMessage());
        }
    }

    @Test
    public void testNullArrayArgumentsAreRejected() throws Exception {
        final Path file = tempFolder.resolve("null-array-arguments.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            try {
                eraf.read((byte[]) null);
                fail("null byte read buffer must fail");
            } catch (IllegalArgumentException expected) {
                assertEquals("byte buffer cannot be null", expected.getMessage());
            }

            try {
                eraf.write((byte[]) null);
                fail("null byte write buffer must fail");
            } catch (IllegalArgumentException expected) {
                assertEquals("byte buffer cannot be null", expected.getMessage());
            }

            try {
                eraf.readShorts(null, 0, 1);
                fail("null short read buffer must fail");
            } catch (IllegalArgumentException expected) {
                assertEquals("short buffer cannot be null", expected.getMessage());
            }

            try {
                eraf.writeInts(null, 0, 1);
                fail("null int write buffer must fail");
            } catch (IllegalArgumentException expected) {
                assertEquals("int buffer cannot be null", expected.getMessage());
            }
        }
    }

    @Test
    public void testReadFullyRejectsInvalidByteRanges() throws Exception {
        final Path file = tempFolder.resolve("invalid-read-fully-range.bin");
        try (EnhancedNIORandomAccessFile eraf = openReadWrite(file, true, 8)) {
            final byte[] buffer = new byte[4];
            for (int[] range : new int[][]{{-1, 1}, {0, -1}, {5, 0}, {3, 2}}) {
                try {
                    eraf.readFully(buffer, range[0], range[1]);
                    fail("invalid byte range must fail");
                } catch (IndexOutOfBoundsException expected) {
                    // expected
                }
            }

            eraf.readFully(buffer, buffer.length, 0);
        }
    }

    @Test
    public void testEmptyOpenOptionsAreRejected() throws Exception {
        final Path file = tempFolder.resolve("empty-open-options.bin");
        try {
            new EnhancedNIORandomAccessFile(file, new byte[4]).close();
            fail("empty open options must fail");
        } catch (IllegalArgumentException expected) {
            assertEquals("modes must not be empty", expected.getMessage());
        }
    }

    private EnhancedNIORandomAccessFile openReadWrite(Path file, boolean cached, int bufferSize) throws IOException {
        if (cached) {
            return new EnhancedNIORandomAccessFile(file, bufferSize,
                    StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        }
        return new EnhancedNIORandomAccessFile(file,
                StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    }

    private long countCacheFiles() throws IOException {
        final Collection<Path> cacheFiles = IOHelper.listFiles(tempFolder, path -> path.getFileName().toString().startsWith("eraf-")
                && path.getFileName().toString().endsWith(".cache"));
        return cacheFiles == null ? 0 : cacheFiles.size();
    }

    private void assertEachByteWasWrittenFourTimes(Path file) throws IOException {
        final byte[] bytes = Files.readAllBytes(file);
        assertEquals(400, bytes.length);
        final int[] counts = new int[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        for (byte value : bytes) {
            counts[value - Byte.MIN_VALUE]++;
        }
        for (int value = 0; value < 100; value++) {
            assertEquals(4, counts[value - Byte.MIN_VALUE]);
        }
    }

}
