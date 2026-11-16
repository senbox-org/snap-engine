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
package org.esa.snap.core.dataio;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.dataio.NIOFileImageInputStream;
import org.esa.snap.dataio.NIOFileImageOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ImageInputAndOutputStreamTest {

    private static final int BYTES_PER_FLOAT = Float.BYTES;
    private static final int LINE_LENGTH = 4;
    private static final int NUM_LINES = 5;
    private static final int BUFFER_OFFSET = 1;
    private static final int OUTPUT_BUFFER_SIZE = 1024;
    private static final long INPUT_RANDOM_SEED = 0L;
    private static final int TOTAL_FLOATS = Math.multiplyExact(NUM_LINES, LINE_LENGTH);
    private static final int INPUT_LINE_BUFFER_LENGTH = Math.addExact(LINE_LENGTH,
            Math.multiplyExact(2, BUFFER_OFFSET));
    private static final long EXPECTED_FILE_SIZE = Math.multiplyExact((long) TOTAL_FLOATS, BYTES_PER_FLOAT);
    private static final long OVERSIZED_OUTPUT_FILE_SIZE = Math.addExact(EXPECTED_FILE_SIZE, BYTES_PER_FLOAT);
    private static final int SENTINEL_BITS = 0x7FC00001;
    private static final int CUSTOM_NAN_BITS = 0x7FC01234;
    private static final byte OUTPUT_FIXTURE_BYTE = (byte) 0x5A;
    private static final float SENTINEL_VALUE = Float.intBitsToFloat(SENTINEL_BITS);

    private Path _testDirectory;
    private Path _testFile;
    private Path _outputFile;
    private float[] _inputFloats;
    private float[] _outputFloats;
    private byte[] _expectedInputBytes;

    @Before
    public void setUp() throws IOException {
        _testDirectory = IOHelper.createATempDir("ImageInputAndOutputStreamTest");

        _testFile = _testDirectory.resolve("testData");
        _outputFile = _testDirectory.resolve("testData-output");

        _inputFloats = new float[TOTAL_FLOATS];
        final Random random = new Random(INPUT_RANDOM_SEED);
        for (int i = 0; i < _inputFloats.length; i++) {
            _inputFloats[i] = random.nextFloat();
        }
        final float[] edgeValues = {
                -0.0f,
                Float.MIN_VALUE,
                -Float.MIN_VALUE,
                Float.MAX_VALUE,
                -Float.MAX_VALUE,
                Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                Float.intBitsToFloat(CUSTOM_NAN_BITS)
        };
        assertTrue("Input fixture is too small for edge-case values: required " + edgeValues.length
                           + ", available " + _inputFloats.length,
                edgeValues.length <= _inputFloats.length);
        System.arraycopy(edgeValues, 0, _inputFloats, 0, edgeValues.length);
        _outputFloats = new float[TOTAL_FLOATS];
        for (int i = 0; i < _outputFloats.length; i++) {
            _outputFloats[i] = i + 12;
        }
        assertTrue("Output fixture is too small for edge-case values: required " + edgeValues.length
                           + ", available " + _outputFloats.length,
                edgeValues.length <= _outputFloats.length);
        System.arraycopy(edgeValues, 0, _outputFloats, 0, edgeValues.length);

        _expectedInputBytes = toBigEndianBytes(_inputFloats);
        Files.write(_testFile, _expectedInputBytes);
        final byte[] oversizedOutput = new byte[Math.toIntExact(OVERSIZED_OUTPUT_FILE_SIZE)];
        Arrays.fill(oversizedOutput, OUTPUT_FIXTURE_BYTE);
        Files.write(_outputFile, oversizedOutput);
        assertEquals("Unexpected input fixture size", EXPECTED_FILE_SIZE, Files.size(_testFile));
        assertEquals("Output truncation fixture is not oversized", OVERSIZED_OUTPUT_FILE_SIZE,
                Files.size(_outputFile));
    }

    @After
    public void tearDown() {
        if (_testDirectory == null || Files.notExists(_testDirectory)) {
            return;
        }
        assertTrue("Failed to delete temporary test directory: " + _testDirectory,
                IOHelper.delete(_testDirectory));
    }

    @Test
    public void testFileImageIOStreamsSupportRandomAccessAndRoundTrip() throws IOException {
        final float[] inputLineBuffer = new float[INPUT_LINE_BUFFER_LENGTH];
        final int byteLineSize = Math.multiplyExact(LINE_LENGTH, BYTES_PER_FLOAT);
        Arrays.fill(inputLineBuffer, SENTINEL_VALUE);
        try (ImageInputStream inputStream = ImageUtils.getImageInputStream(_testFile);
             ImageOutputStream outputStream = ImageUtils.getImageOutputStream(_outputFile)) {
            assertNotNull("Image input stream was not created", inputStream);
            assertNotNull("Image output stream was not created", outputStream);
            assertEquals("Input stream did not start at position zero", 0L, inputStream.getStreamPosition());
            assertEquals("Output stream did not start at position zero", 0L, outputStream.getStreamPosition());
            assertEquals("Output stream did not preserve the existing file", OVERSIZED_OUTPUT_FILE_SIZE,
                    outputStream.length());
            assertTrue("Unexpected image input stream type: " + inputStream.getClass().getName(),
                    inputStream instanceof NIOFileImageInputStream);
            assertTrue("Unexpected image output stream type: " + outputStream.getClass().getName(),
                    outputStream instanceof NIOFileImageOutputStream);
            assertEquals("Unexpected input byte order", ByteOrder.BIG_ENDIAN, inputStream.getByteOrder());
            assertEquals("Unexpected output byte order", ByteOrder.BIG_ENDIAN, outputStream.getByteOrder());
            assertEquals("Unexpected input stream length", EXPECTED_FILE_SIZE, inputStream.length());
            for (int line = NUM_LINES - 1; line >= 0; line--) {
                final int offset = byteOffset(line);
                final int elementOffset = elementOffset(line);
                inputStream.seek(offset);
                inputStream.readFully(inputLineBuffer, BUFFER_OFFSET, LINE_LENGTH);
                assertEquals("Input position after line " + line,
                        Math.addExact(offset, byteLineSize), inputStream.getStreamPosition());
                final String lineContext = "Input line " + line;
                assertRawFloatRangeEquals(_inputFloats, elementOffset, inputLineBuffer, BUFFER_OFFSET,
                        LINE_LENGTH, lineContext);
                assertBufferSentinelsUnchanged(inputLineBuffer, lineContext);
                outputStream.seek(offset);
                outputStream.writeFloats(_outputFloats, elementOffset, LINE_LENGTH);
                assertEquals("Output position after line " + line,
                        Math.addExact(offset, byteLineSize), outputStream.getStreamPosition());
            }
            inputStream.seek(EXPECTED_FILE_SIZE);
            assertEquals("Input stream must be at EOF", -1, inputStream.read());
            assertEquals("Input stream position changed after EOF", EXPECTED_FILE_SIZE,
                    inputStream.getStreamPosition());
            assertEquals("Unexpected output stream length", OVERSIZED_OUTPUT_FILE_SIZE, outputStream.length());
        }
        assertArrayEquals("Input file was modified", _expectedInputBytes, Files.readAllBytes(_testFile));
        assertEquals("Unexpected output file size", OVERSIZED_OUTPUT_FILE_SIZE, Files.size(_outputFile));
        final byte[] outputBytes = Files.readAllBytes(_outputFile);
        for (int i = Math.toIntExact(EXPECTED_FILE_SIZE); i < outputBytes.length; i++) {
            assertEquals("Existing output tail was not preserved at byte " + i,
                    OUTPUT_FIXTURE_BYTE, outputBytes[i]);
        }
        final float[] encodedOutputFloats = new float[_outputFloats.length];
        ByteBuffer.wrap(outputBytes)
                .order(ByteOrder.BIG_ENDIAN)
                .asFloatBuffer()
                .get(encodedOutputFloats);
        assertArrayEquals("Unexpected output float values", _outputFloats, encodedOutputFloats, 0.0f);
        assertArrayEquals("Unexpected output float bits", floatBits(_outputFloats), floatBits(encodedOutputFloats));

        try (ImageInputStream verifyInputStream = ImageUtils.getImageInputStream(_outputFile)) {
            assertEquals("Unexpected verification byte order", ByteOrder.BIG_ENDIAN, verifyInputStream.getByteOrder());
            assertEquals("Unexpected verification stream length", OVERSIZED_OUTPUT_FILE_SIZE,
                    verifyInputStream.length());
            for (int line = NUM_LINES - 1; line >= 0; line--) {
                final int offset = byteOffset(line);
                final int elementOffset = elementOffset(line);
                verifyInputStream.seek(offset);
                verifyInputStream.readFully(inputLineBuffer, BUFFER_OFFSET, LINE_LENGTH);
                assertEquals("Verification position after line " + line,
                        Math.addExact(offset, byteLineSize), verifyInputStream.getStreamPosition());
                final String lineContext = "Verification line " + line;
                assertCanonicalFloatRangeEquals(_outputFloats, elementOffset, inputLineBuffer, BUFFER_OFFSET,
                        LINE_LENGTH, lineContext);
                assertBufferSentinelsUnchanged(inputLineBuffer, lineContext);
            }
            verifyInputStream.seek(OVERSIZED_OUTPUT_FILE_SIZE);
            assertEquals("Verification stream must be at EOF", -1, verifyInputStream.read());
            assertEquals("Verification stream position changed after EOF", OVERSIZED_OUTPUT_FILE_SIZE,
                    verifyInputStream.getStreamPosition());
        }
    }

    @Test
    public void testBufferedOutputStreamPreservesExistingFile() throws IOException {
        try (NIOFileImageOutputStream outputStream =
                     new NIOFileImageOutputStream(_outputFile, OUTPUT_BUFFER_SIZE)) {
            assertEquals("Buffered output stream did not preserve the existing file",
                    OVERSIZED_OUTPUT_FILE_SIZE, outputStream.length());
            assertEquals("Buffered output stream did not start at position zero", 0L, outputStream.getStreamPosition());
            outputStream.write(0x11);
        }
        assertEquals("Buffered output file size changed unexpectedly", OVERSIZED_OUTPUT_FILE_SIZE,
                Files.size(_outputFile));
        assertEquals("Buffered output stream did not edit the existing file", 0x11,
                Files.readAllBytes(_outputFile)[0] & 0xFF);
    }

    private static int byteOffset(int line) {
        return Math.multiplyExact(elementOffset(line), BYTES_PER_FLOAT);
    }

    private static int elementOffset(int line) {
        return Math.multiplyExact(line, LINE_LENGTH);
    }

    private static void assertRawFloatRangeEquals(float[] expected, int expectedOffset,
                                                  float[] actual, int actualOffset, int length, String context) {
        final float[] expectedRange = floatRange(expected, expectedOffset, length);
        final float[] actualRange = floatRange(actual, actualOffset, length);
        assertFloatValuesEqual(expectedRange, actualRange, context + " values");
        assertArrayEquals(context + " raw float bits", rawFloatBits(expectedRange), rawFloatBits(actualRange));
    }

    private static void assertCanonicalFloatRangeEquals(float[] expected, int expectedOffset,
                                                        float[] actual, int actualOffset, int length, String context) {
        final float[] expectedRange = floatRange(expected, expectedOffset, length);
        final float[] actualRange = floatRange(actual, actualOffset, length);
        assertFloatValuesEqual(expectedRange, actualRange, context + " values");
        assertArrayEquals(context + " canonical float bits", floatBits(expectedRange), floatBits(actualRange));
    }

    private static void assertFloatValuesEqual(float[] expected, float[] actual, String context) {
        assertArrayEquals(context, expected, actual, 0.0f);
    }

    private static float[] floatRange(float[] values, int offset, int length) {
        return Arrays.copyOfRange(values, offset, Math.addExact(offset, length));
    }

    private static void assertBufferSentinelsUnchanged(float[] buffer, String context) {
        assertEquals(context + " leading sentinel", SENTINEL_BITS, Float.floatToRawIntBits(buffer[0]));
        assertEquals(context + " trailing sentinel", SENTINEL_BITS,
                Float.floatToRawIntBits(buffer[buffer.length - 1]));
    }

    private static int[] floatBits(float[] values) {
        final int[] bits = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            bits[i] = Float.floatToIntBits(values[i]);
        }
        return bits;
    }

    private static int[] rawFloatBits(float[] values) {
        final int[] bits = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            bits[i] = Float.floatToRawIntBits(values[i]);
        }
        return bits;
    }

    private static byte[] toBigEndianBytes(float[] values) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Math.multiplyExact(values.length, BYTES_PER_FLOAT))
                .order(ByteOrder.BIG_ENDIAN);
        byteBuffer.asFloatBuffer().put(values);
        return byteBuffer.array();
    }
}
