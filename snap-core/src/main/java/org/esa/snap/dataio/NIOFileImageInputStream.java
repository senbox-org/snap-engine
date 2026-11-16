/*
 *    ImageI/O-Ext - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *    https://imageio-ext.dev.java.net/
 *    (C) 2007 - 2008, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.esa.snap.dataio;

import com.bc.ceres.core.io.EnhancedNIORandomAccessFile;

import javax.imageio.IIOException;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * An implementation of {@link ImageInputStream} that gets its input from a
 * {@link Path}. The eraf contents are assumed to be stable during the lifetime
 * of the object.
 * <p>
 * Public operations are synchronized, so concurrent callers share one
 * consistent stream cursor and bit/mark state. A sequence of multiple calls
 * that must be atomic as a unit still requires external synchronization.
 * Subclasses must also synchronize access to the protected {@code bitOffset}
 * field on this stream instance and preserve synchronization when overriding
 * public operations.
 *
 * @author Simone Giannecchini, GeoSolutions
 */
public class NIOFileImageInputStream
        implements ImageInputStream {

    /**
     * the associated {@link Path}
     */
    protected final Path file;

    protected final EnhancedNIORandomAccessFile eraf;

    private final Deque<Long> markByteStack = new ArrayDeque<>();

    private final Deque<Integer> markBitStack = new ArrayDeque<>();

    private boolean isClosed = false;

    protected int bitOffset;

    /**
     * Creates a new instance of {@link NIOFileImageInputStream}
     *
     * @param file the input file {@link Path}
     * @return the {@link NIOFileImageInputStream} new instance
     * @throws NullPointerException  if <code>file</code> is <code>null</code>.
     * @throws SecurityException     if a security manager exists and does not allow read
     *                               access to the eraf.
     * @throws FileNotFoundException if <code>file</code> is a directory or cannot be opened
     *                               for reading for any other reason.
     * @throws IOException           if an I/O error occurs.
     */
    public static ImageInputStream createInputStream(final Path file) throws IOException {
        return new NIOFileImageInputStream(file);
    }

    /**
     * Constructs a {@link NIOFileImageInputStream} that will read from a
     * given {@link Path}.
     * <p/>
     * The eraf contents must not change between the time this object is
     * constructed and the time of the last call to a read method.
     *
     * @param file a {@link Path} to read from.
     * @throws NullPointerException  if <code>file</code> is <code>null</code>.
     * @throws SecurityException     if a security manager exists and does not allow read
     *                               access to the eraf.
     * @throws FileNotFoundException if <code>file</code> is a directory or cannot be opened
     *                               for reading for any other reason.
     * @throws IOException           if an I/O error occurs.
     */
    public NIOFileImageInputStream(Path file) throws IOException {
        this(file, openReadableFile(file));
    }

    /**
     * Constructs a {@link NIOFileImageInputStream} that will read from a
     * given {@link Path}.
     * <p/>
     * The eraf contents must not change between the time this object is
     * constructed and the time of the last call to a read method.
     *
     * @param file       a {@link Path} to read from.
     * @param bufferSize size of the underlying buffer.
     * @throws NullPointerException  if <code>file</code> is <code>null</code>.
     * @throws SecurityException     if a security manager exists and does not allow read
     *                               access to the eraf.
     * @throws FileNotFoundException if <code>file</code> is a directory or cannot be opened
     *                               for reading for any other reason.
     * @throws IOException           if an I/O error occurs.
     */
    public NIOFileImageInputStream(Path file, int bufferSize) throws IOException {
        this(file, openReadableFile(file, bufferSize));
    }

    /**
     * Constructs a {@link NIOFileImageInputStream} that will read from a
     * given {@link Path}.
     * <p/>
     * The eraf contents must not change between the time this object is
     * constructed and the time of the last call to a read method.
     *
     * @param file a {@link Path} to read from.
     * @param eraf the {@link EnhancedNIORandomAccessFile} instance
     * @throws NullPointerException  if <code>file</code> is <code>null</code>.
     *                               or <code>eraf</code> is <code>null</code>.
     * @throws SecurityException     if a security manager exists and does not allow read
     *                               access to the eraf.
     * @throws FileNotFoundException if <code>file</code> is a directory or cannot be opened
     *                               for reading for any other reason.
     * @throws IOException           if an I/O error occurs.
     */
    protected NIOFileImageInputStream(Path file, EnhancedNIORandomAccessFile eraf) throws IOException {
        Objects.requireNonNull(eraf, "eraf");
        try {
            this.file = validateReadableFile(file);
            this.eraf = eraf;
            // NOTE: this must be done accordingly to what ImageInputStreamImpl
            // does, otherwise some ImageReader subclasses might not work.
            this.eraf.setByteOrder(getByteOrder());
        } catch (IOException | RuntimeException | Error exception) {
            try {
                eraf.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    private static Path validateReadableFile(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        final boolean exists = Files.exists(file);
        final boolean directory = Files.isDirectory(file);
        if (!exists || directory) {
            throw new FileNotFoundException("Invalid input file provided: " + file
                    + " (exists: " + exists + ", isDirectory: " + directory + ")");
        }
        if (!Files.isReadable(file)) {
            throw new IOException("Invalid input file provided: " + file + " (canRead: false)");
        }
        return file;
    }

    private static EnhancedNIORandomAccessFile openReadableFile(Path file)
            throws IOException {
        validateReadableFile(file);
        try {
            return new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ);
        } catch (NoSuchFileException exception) {
            FileNotFoundException fileNotFoundException =
                    new FileNotFoundException("Input file disappeared while opening: " + file);
            fileNotFoundException.initCause(exception);
            throw fileNotFoundException;
        }
    }

    private static EnhancedNIORandomAccessFile openReadableFile(Path file, int bufferSize)
            throws IOException {
        validateReadableFile(file);
        try {
            return new EnhancedNIORandomAccessFile(file, bufferSize, StandardOpenOption.READ);
        } catch (NoSuchFileException exception) {
            FileNotFoundException fileNotFoundException =
                    new FileNotFoundException("Input file disappeared while opening: " + file);
            fileNotFoundException.initCause(exception);
            throw fileNotFoundException;
        }
    }

    /**
     * Reads a byte from the stream and returns it as a
     * {@code byte} value.  Byte values between {@code 0x00}
     * and {@code 0x7f} represent integer values between
     * {@code 0} and {@code 127}.  Values between
     * {@code 0x80} and {@code 0xff} represent negative
     * values from {@code -128} to {@code -1}.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return a signed byte value from the stream.
     * @throws java.io.EOFException if the end of the stream is reached.
     * @throws IOException          if an I/O error occurs.
     */
    @Override
    public synchronized byte readByte() throws IOException {
        setBitOffset(0);
        return eraf.readByte();
    }

    /**
     * Equivalent to {@code readUnsignedShort}, except that the
     * result is returned using the {@code char} datatype.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return an unsigned char value from the stream.
     * @throws java.io.EOFException if the stream reaches the end before
     *                              reading all the bytes.
     * @throws IOException          if an I/O error occurs.
     * @see #readUnsignedShort
     */
    @Override
    public synchronized char readChar() throws IOException {
        setBitOffset(0);
        return eraf.readChar();
    }

    /**
     * Reads 8 bytes from the stream, and (conceptually) concatenates
     * them according to the current byte order and returns the result
     * as a {@code double}.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return a double value from the stream.
     * @throws java.io.EOFException if the stream reaches the end before
     *                              reading all the bytes.
     * @throws IOException          if an I/O error occurs.
     * @see #getByteOrder
     */
    @Override
    public synchronized double readDouble() throws IOException {
        setBitOffset(0);
        return eraf.readDouble();
    }

    /**
     * Reads 4 bytes from the stream, and (conceptually) concatenates
     * them according to the current byte order and returns the result
     * as a {@code float}.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return a float value from the stream.
     * @throws java.io.EOFException if the stream reaches the end before
     *                              reading all the bytes.
     * @throws IOException          if an I/O error occurs.
     * @see #getByteOrder
     */
    @Override
    public synchronized float readFloat() throws IOException {
        setBitOffset(0);
        return eraf.readFloat();
    }

    /**
     * Reads {@code len} bytes from the stream, and stores them
     * into {@code b} starting at index {@code off}.
     * If the end of the stream is reached, a {@code java.io.EOFException}
     * will be thrown.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param b   an array of bytes to be written to.
     * @param off the starting position within {@code b} to write to.
     * @param len the maximum number of {@code byte}s to read.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off + len}
     *                                   is greater than {@code b.length}.
     * @throws NullPointerException      if {@code b} is
     *                                   {@code null}.
     * @throws java.io.EOFException      if the stream reaches the end before
     *                                   reading all the bytes.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(byte[] b, int off, int len) throws IOException {
        setBitOffset(0);
        eraf.readFully(b, off, len);
    }

    /**
     * Reads {@code b.length} bytes from the stream, and stores them
     * into {@code b} starting at index {@code 0}.
     * If the end of the stream is reached, a {@code java.io.EOFException}
     * will be thrown.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param b an array of {@code byte}s.
     * @throws NullPointerException if {@code b} is
     *                              {@code null}.
     * @throws java.io.EOFException if the stream reaches the end before
     *                              reading all the bytes.
     * @throws IOException          if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(byte[] b) throws IOException {
        setBitOffset(0);
        eraf.readFully(b);
    }

    /**
     * Reads {@code len} shorts (signed 16-bit integers) from the
     * stream according to the current byte order, and
     * stores them into {@code s} starting at index
     * {@code off}.  If the end of the stream is reached,
     * a {@code java.io.EOFException} will be thrown.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param s   an array of shorts to be written to.
     * @param off the starting position within {@code s} to write to.
     * @param len the maximum number of {@code short}s to read.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off + len}
     *                                   is greater than {@code s.length}.
     * @throws NullPointerException      if {@code s} is
     *                                   {@code null}.
     * @throws EOFException              if the stream reaches the end before
     *                                   reading all the bytes.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(short[] s, int off, int len) throws IOException {
        setBitOffset(0);
        this.eraf.readFully(s, off, len);
    }

    /**
     * Reads {@code len} chars (unsigned 16-bit integers) from the
     * stream according to the current byte order, and
     * stores them into {@code c} starting at index
     * {@code off}.  If the end of the stream is reached,
     * a {@code java.io.EOFException} will be thrown.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param c   an array of chars to be written to.
     * @param off the starting position within {@code c} to write to.
     * @param len the maximum number of {@code char}s to read.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off + len}
     *                                   is greater than {@code c.length}.
     * @throws NullPointerException      if {@code c} is
     *                                   {@code null}.
     * @throws EOFException              if the stream reaches the end before
     *                                   reading all the bytes.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(char[] c, int off, int len) throws IOException {
        setBitOffset(0);
        this.eraf.readFully(c, off, len);
    }

    /**
     * Reads {@code len} ints (signed 32-bit integers) from the
     * stream according to the current byte order, and
     * stores them into {@code i} starting at index
     * {@code off}.  If the end of the stream is reached,
     * a {@code java.io.EOFException} will be thrown.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param i   an array of ints to be written to.
     * @param off the starting position within {@code i} to write to.
     * @param len the maximum number of {@code int}s to read.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off + len}
     *                                   is greater than {@code i.length}.
     * @throws NullPointerException      if {@code i} is
     *                                   {@code null}.
     * @throws EOFException              if the stream reaches the end before
     *                                   reading all the bytes.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(int[] i, int off, int len) throws IOException {
        setBitOffset(0);
        this.eraf.readFully(i, off, len);
    }

    /**
     * Reads {@code len} longs (signed 64-bit integers) from the
     * stream according to the current byte order, and
     * stores them into {@code l} starting at index
     * {@code off}.  If the end of the stream is reached,
     * a {@code java.io.EOFException} will be thrown.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param l   an array of longs to be written to.
     * @param off the starting position within {@code l} to write to.
     * @param len the maximum number of {@code long}s to read.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off + len}
     *                                   is greater than {@code l.length}.
     * @throws NullPointerException      if {@code l} is
     *                                   {@code null}.
     * @throws EOFException              if the stream reaches the end before
     *                                   reading all the bytes.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(long[] l, int off, int len) throws IOException {
        setBitOffset(0);
        this.eraf.readFully(l, off, len);
    }

    /**
     * Reads {@code len} floats (32-bit IEEE single-precision
     * floats) from the stream according to the current byte order,
     * and stores them into {@code f} starting at
     * index {@code off}.  If the end of the stream is reached,
     * a {@code java.io.EOFException} will be thrown.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param f   an array of floats to be written to.
     * @param off the starting position within {@code f} to write to.
     * @param len the maximum number of {@code float}s to read.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off + len}
     *                                   is greater than {@code f.length}.
     * @throws NullPointerException      if {@code f} is
     *                                   {@code null}.
     * @throws EOFException              if the stream reaches the end before
     *                                   reading all the bytes.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(float[] f, int off, int len) throws IOException {
        setBitOffset(0);
        this.eraf.readFully(f, off, len);
    }

    /**
     * Reads {@code len} doubles (64-bit IEEE double-precision
     * floats) from the stream according to the current byte order,
     * and stores them into {@code d} starting at
     * index {@code off}.  If the end of the stream is reached,
     * a {@code java.io.EOFException} will be thrown.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param d   an array of doubles to be written to.
     * @param off the starting position within {@code d} to write to.
     * @param len the maximum number of {@code double}s to read.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off + len}
     *                                   is greater than {@code d.length}.
     * @throws NullPointerException      if {@code d} is
     *                                   {@code null}.
     * @throws EOFException              if the stream reaches the end before
     *                                   reading all the bytes.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(double[] d, int off, int len) throws IOException {
        setBitOffset(0);
        this.eraf.readFully(d, off, len);
    }

    /**
     * Reads 4 bytes from the stream, and (conceptually) concatenates
     * them according to the current byte order and returns the result
     * as an {@code int}.
     *
     * <p> The bit offset within the stream is ignored and treated as
     * though it were zero.
     *
     * @return a signed int value from the stream.
     * @throws java.io.EOFException if the stream reaches the end before
     *                              reading all the bytes.
     * @throws IOException          if an I/O error occurs.
     * @see #getByteOrder
     */
    @Override
    public synchronized int readInt() throws IOException {
        setBitOffset(0);
        return eraf.readInt();
    }

    /**
     * Reads the next line of text from the input stream.  It reads
     * successive bytes, converting each byte separately into a
     * character, until it encounters a line terminator or end of
     * file; the characters read are then returned as a
     * {@code String}. Note that because this method processes
     * bytes, it does not support input of the full Unicode character
     * set.
     *
     * <p> If end of file is encountered before even one byte can be
     * read, then {@code null} is returned. Otherwise, each byte
     * that is read is converted to type {@code char} by
     * zero-extension. If the character {@code '\n'} is
     * encountered, it is discarded and reading ceases. If the
     * character {@code '\r'} is encountered, it is discarded
     * and, if the following byte converts to the character
     * {@code '\n'}, then that is discarded also; reading then
     * ceases. If end of file is encountered before either of the
     * characters {@code '\n'} and {@code '\r'} is
     * encountered, reading ceases. Once reading has ceased, a
     * {@code String} is returned that contains all the
     * characters read and not discarded, taken in order.  Note that
     * every character in this string will have a value less than
     * <code>&#92;u0100</code>, that is, {@code (char)256}.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return a String containing a line of text from the stream.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized String readLine() throws IOException {
        setBitOffset(0);
        return eraf.readLine();
    }

    /**
     * Returns the byte order with which data values will be read from
     * this stream as an instance of the
     * {@code java.nio.ByteOrder} enumeration.
     *
     * @return one of {@code ByteOrder.BIG_ENDIAN} or
     * {@code ByteOrder.LITTLE_ENDIAN}, indicating which byte
     * order is being used.
     * @see java.nio.ByteOrder
     * @see #setByteOrder
     */
    @Override
    public synchronized ByteOrder getByteOrder() {
        try {
            return eraf.getByteOrder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the current byte position of the stream.  The next read
     * will take place starting at this offset.
     *
     * @return a long containing the position of the stream.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized long getStreamPosition() throws IOException {
        checkClosed();
        return eraf.getFilePointer();
    }

    /**
     * Returns the current bit offset, as an integer between 0 and 7,
     * inclusive.  The bit offset is updated implicitly by calls to
     * the {@code readBits} method.  A value of 0 indicates the
     * most-significant bit, and a value of 7 indicates the least
     * significant bit, of the byte being read.
     *
     * <p> The bit offset is set to 0 when a stream is first
     * opened, and is reset to 0 by calls to {@code seek},
     * {@code skipBytes}, or any {@code read} or
     * {@code readFully} method.
     *
     * @return an {@code int} containing the bit offset between
     * 0 and 7, inclusive.
     * @throws IOException if an I/O error occurs.
     * @see #setBitOffset
     */
    @Override
    public synchronized int getBitOffset() throws IOException {
        checkClosed();
        return bitOffset;
    }

    /**
     * Sets the bit offset to an integer between 0 and 7, inclusive.
     * The byte offset within the stream, as returned by
     * {@code getStreamPosition}, is left unchanged.
     * A value of 0 indicates the
     * most-significant bit, and a value of 7 indicates the least
     * significant bit, of the byte being read.
     *
     * @param bitOffset the desired offset, as an {@code int}
     *                  between 0 and 7, inclusive.
     * @throws IllegalArgumentException if {@code bitOffset}
     *                                  is not between 0 and 7, inclusive.
     * @throws IOException              if an I/O error occurs.
     * @see #getBitOffset
     */
    @Override
    public synchronized void setBitOffset(int bitOffset) throws IOException {
        checkClosed();
        if (bitOffset < 0 || bitOffset > 7) {
            throw new IllegalArgumentException("bitOffset must be between 0 and 7!");
        }
        this.bitOffset = bitOffset;
    }

    /**
     * Reads a single bit from the stream and returns it as an
     * {@code int} with the value {@code 0} or
     * {@code 1}.  The bit offset is advanced by one and reduced
     * modulo 8.
     *
     * @return an {@code int} containing the value {@code 0}
     * or {@code 1}.
     * @throws EOFException if the stream reaches the end before
     *                      reading all the bits.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized int readBit() throws IOException {
        checkClosed();
        // Compute final bit offset before we call read() and seek()
        int newBitOffset = (this.bitOffset + 1) & 0x7;

        int val = read();
        if (val == -1) {
            throw new EOFException();
        }

        if (newBitOffset != 0) {
            // Move byte position back if in the middle of a byte
            seek(getStreamPosition() - 1);
            // Shift the bit to be read to the rightmost position
            val >>= 8 - newBitOffset;
        }
        this.bitOffset = newBitOffset;

        return val & 0x1;
    }

    /**
     * Reads a bitstring from the stream and returns it as a
     * {@code long}, with the first bit read becoming the most
     * significant bit of the output.  The read starts within the byte
     * indicated by {@code getStreamPosition}, at the bit given
     * by {@code getBitOffset}.  The bit offset is advanced by
     * {@code numBits} and reduced modulo 8.
     *
     * <p> The byte order of the stream has no effect on this
     * method.  The return value of this method is constructed as
     * though the bits were read one at a time, and shifted into
     * the right side of the return value, as shown by the following
     * pseudo-code:
     *
     * <pre>{@code
     * long accum = 0L;
     * for (int i = 0; i < numBits; i++) {
     *   accum <<= 1; // Shift left one bit to make room
     *   accum |= readBit();
     * }
     * }</pre>
     * <p>
     * Note that the result of {@code readBits(32)} may thus not
     * be equal to that of {@code readInt()} if a reverse network
     * byte order is being used (i.e., {@code getByteOrder() == ByteOrder.LITTLE_ENDIAN}).
     *
     * <p> If the end of the stream is encountered before all the bits
     * have been read, a {@code java.io.EOFException} is thrown.
     *
     * @param numBits the number of bits to read, as an {@code int}
     *                between 0 and 64, inclusive.
     * @return the bitstring, as a {@code long} with the last bit
     * read stored in the least significant bit.
     * @throws IllegalArgumentException if {@code numBits}
     *                                  is not between 0 and 64, inclusive.
     * @throws EOFException             if the stream reaches the end before
     *                                  reading all the bits.
     * @throws IOException              if an I/O error occurs.
     */
    @Override
    public synchronized long readBits(int numBits) throws IOException {
        checkClosed();
        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException();
        }
        if (numBits == 0) {
            return 0L;
        }

        long accum = 0L;
        int remaining = numBits;
        while (remaining > 0) {
            final int bitsAvailable = 8 - bitOffset;
            final int bitsToConsume = Math.min(remaining, bitsAvailable);
            final int value = eraf.readUnsignedByte();
            final int shift = bitsAvailable - bitsToConsume;
            final long mask = (1L << bitsToConsume) - 1L;
            final long bits = (value >>> shift) & mask;

            accum = (accum << bitsToConsume) | bits;
            remaining -= bitsToConsume;

            final int newBitOffset = (bitOffset + bitsToConsume) & 0x7;
            if (newBitOffset != 0) {
                // Keep the file pointer on the byte containing the pending bits.
                eraf.seek(eraf.getFilePointer() - 1);
            }
            bitOffset = newBitOffset;
        }

        return accum;
    }

    /**
     * Returns {@code true} if this {@code ImageInputStream}
     * caches data itself in order to allow seeking backwards.
     * Applications may consult this in order to decide how frequently,
     * or whether, to flush in order to conserve cache resources.
     *
     * @return {@code true} if this {@code ImageInputStream}
     * caches data.
     * @see #isCachedMemory
     * @see #isCachedFile
     */
    @Override
    public synchronized boolean isCached() {
        return eraf.isCached();
    }

    /**
     * Returns {@code true} if this {@code ImageInputStream}
     * caches data itself in order to allow seeking backwards, and
     * the cache is kept in main memory.  Applications may consult
     * this in order to decide how frequently, or whether, to flush
     * in order to conserve cache resources.
     *
     * @return {@code true} if this {@code ImageInputStream}
     * caches data in main memory.
     * @see #isCached
     * @see #isCachedFile
     */
    @Override
    public synchronized boolean isCachedMemory() {
        return this.eraf.isCachedMemory();
    }

    /**
     * Returns {@code true} if this {@code ImageInputStream}
     * caches data itself in order to allow seeking backwards, and
     * the cache is kept in a temporary file.  Applications may consult
     * this in order to decide how frequently, or whether, to flush
     * in order to conserve cache resources.
     *
     * @return {@code true} if this {@code ImageInputStream}
     * caches data in a temporary file.
     * @see #isCached
     * @see #isCachedMemory
     */
    @Override
    public synchronized boolean isCachedFile() {
        return this.eraf.isCachedFile();
    }

    /**
     * Reads up to {@code b.length} bytes from the stream, and
     * stores them into {@code b} starting at index 0.  The
     * number of bytes read is returned.  If no bytes can be read
     * because the end of the stream has been reached, -1 is returned.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param b an array of bytes to be written to.
     * @return the number of bytes actually read, or {@code -1}
     * to indicate EOF.
     * @throws NullPointerException if {@code b} is
     *                              {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    @Override
    public synchronized int read(byte[] b) throws IOException {
        setBitOffset(0);
        return eraf.read(b);
    }

    /**
     * Moves the stream position forward by a given number of bytes.
     * This method is identical to {@code skipBytes(int)} except
     * that it allows for a larger skip distance. Positive values move
     * forward and negative values move backward; movement is clamped to
     * the stream bounds.
     *
     * @param n a {@code long} containing the number of bytes to
     *          be skipped.
     * @return a {@code long} representing the number of bytes
     * skipped.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized long skipBytes(long n) throws IOException {
        setBitOffset(0);
        return eraf.skipBytes(n);
    }

    /**
     * Reads 8 bytes from the stream, and (conceptually) concatenates
     * them according to the current byte order and returns the result
     * as a {@code long}.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return a signed long value from the stream.
     * @throws java.io.EOFException if the stream reaches the end before
     *                              reading all the bytes.
     * @throws IOException          if an I/O error occurs.
     * @see #getByteOrder
     */
    @Override
    public synchronized long readLong() throws IOException {
        setBitOffset(0);
        return eraf.readLong();
    }

    /**
     * Reads two bytes from the stream, and (conceptually)
     * concatenates them according to the current byte order, and
     * returns the result as a {@code short} value.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return a signed short value from the stream.
     * @throws java.io.EOFException if the stream reaches the end before
     *                              reading all the bytes.
     * @throws IOException          if an I/O error occurs.
     * @see #getByteOrder
     */
    @Override
    public synchronized short readShort() throws IOException {
        setBitOffset(0);
        return eraf.readShort();
    }

    /**
     * Reads a byte from the stream, and (conceptually) converts it to
     * an int, masks it with {@code 0xff} in order to strip off
     * any sign-extension bits, and returns it as a {@code byte}
     * value.
     *
     * <p> Thus, byte values between {@code 0x00} and
     * {@code 0x7f} are simply returned as integer values between
     * {@code 0} and {@code 127}.  Values between
     * {@code 0x80} and {@code 0xff}, which normally
     * represent negative {@code byte} values, will be mapped into
     * positive integers between {@code 128} and
     * {@code 255}.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return an unsigned byte value from the stream.
     * @throws java.io.EOFException if the end of the stream is reached.
     * @throws IOException          if an I/O error occurs.
     */
    @Override
    public synchronized int readUnsignedByte() throws IOException {
        setBitOffset(0);
        return eraf.readUnsignedByte();
    }

    /**
     * Reads 4 bytes from the stream, and (conceptually) concatenates
     * them according to the current byte order, converts the result
     * to a long, masks it with {@code 0xffffffffL} in order to
     * strip off any sign-extension bits, and returns the result as an
     * unsigned {@code long} value.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return an unsigned int value from the stream, as a long.
     * @throws java.io.EOFException if the stream reaches the end before
     *                              reading all the bytes.
     * @throws IOException          if an I/O error occurs.
     * @see #getByteOrder
     */
    @Override
    public synchronized long readUnsignedInt() throws IOException {
        setBitOffset(0);
        return eraf.readUnsignedInt();
    }

    /**
     * Reads two bytes from the stream, and (conceptually)
     * concatenates them according to the current byte order, converts
     * the resulting value to an {@code int}, masks it with
     * {@code 0xffff} in order to strip off any sign-extension
     * bits, and returns the result as an unsigned {@code int}
     * value.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return an unsigned short value from the stream, as an int.
     * @throws java.io.EOFException if the stream reaches the end before
     *                              reading all the bytes.
     * @throws IOException          if an I/O error occurs.
     * @see #getByteOrder
     */
    @Override
    public synchronized int readUnsignedShort() throws IOException {
        setBitOffset(0);
        return eraf.readUnsignedShort();
    }

    /**
     * Reads in a string that has been encoded using a
     * <a href="../../../../java.base/java/io/DataInput.html#modified-utf-8">
     * modified UTF-8</a>
     * format.  The general contract of {@code readUTF} is that
     * it reads a representation of a Unicode character string encoded
     * in modified UTF-8 format; this string of characters is
     * then returned as a {@code String}.
     *
     * <p> First, two bytes are read and used to construct an unsigned
     * 16-bit integer in the manner of the
     * {@code readUnsignedShort} method, using network byte order
     * (regardless of the current byte order setting). This integer
     * value is called the <i>UTF length</i> and specifies the number
     * of additional bytes to be read. These bytes are then converted
     * to characters by considering them in groups. The length of each
     * group is computed from the value of the first byte of the
     * group. The byte following a group, if any, is the first byte of
     * the next group.
     *
     * <p> If the first byte of a group matches the bit pattern
     * {@code 0xxxxxxx} (where {@code x} means "may be
     * {@code 0} or {@code 1}"), then the group consists of
     * just that byte. The byte is zero-extended to form a character.
     *
     * <p> If the first byte of a group matches the bit pattern
     * {@code 110xxxxx}, then the group consists of that byte
     * {@code a} and a second byte {@code b}. If there is no
     * byte {@code b} (because byte {@code a} was the last
     * of the bytes to be read), or if byte {@code b} does not
     * match the bit pattern {@code 10xxxxxx}, then a
     * {@code UTFDataFormatException} is thrown. Otherwise, the
     * group is converted to the character:
     *
     * <pre><code>
     * (char)(((a&amp; 0x1F) &lt;&lt; 6) | (b &amp; 0x3F))
     * </code></pre>
     * <p>
     * If the first byte of a group matches the bit pattern
     * {@code 1110xxxx}, then the group consists of that byte
     * {@code a} and two more bytes {@code b} and
     * {@code c}.  If there is no byte {@code c} (because
     * byte {@code a} was one of the last two of the bytes to be
     * read), or either byte {@code b} or byte {@code c}
     * does not match the bit pattern {@code 10xxxxxx}, then a
     * {@code UTFDataFormatException} is thrown. Otherwise, the
     * group is converted to the character:
     *
     * <pre><code>
     * (char)(((a &amp; 0x0F) &lt;&lt; 12) | ((b &amp; 0x3F) &lt;&lt; 6) | (c &amp; 0x3F))
     * </code></pre>
     * <p>
     * If the first byte of a group matches the pattern
     * {@code 1111xxxx} or the pattern {@code 10xxxxxx},
     * then a {@code UTFDataFormatException} is thrown.
     *
     * <p> If end of file is encountered at any time during this
     * entire process, then a {@code java.io.EOFException} is thrown.
     *
     * <p> After every group has been converted to a character by this
     * process, the characters are gathered, in the same order in
     * which their corresponding groups were read from the input
     * stream, to form a {@code String}, which is returned.
     *
     * <p> The current byte order setting is ignored.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * <p><strong>Note:</strong> This method should not be used in
     * the  implementation of image formats that use standard UTF-8,
     * because  the modified UTF-8 used here is incompatible with
     * standard UTF-8.
     *
     * @return a String read from the stream.
     * @throws java.io.EOFException           if this stream reaches the end
     *                                        before reading all the bytes.
     * @throws java.io.UTFDataFormatException if the bytes do not represent
     *                                        a valid modified UTF-8 encoding of a string.
     * @throws IOException                    if an I/O error occurs.
     */
    @Override
    public synchronized String readUTF() throws IOException {
        setBitOffset(0);
        return eraf.readUTF();
    }

    /**
     * Sets the desired byte order for future reads of data values
     * from this stream.  For example, the sequence of bytes '0x01
     * 0x02 0x03 0x04' if read as a 4-byte integer would have the
     * value '0x01020304' using network byte order and the value
     * '0x04030201' under the reverse byte order.
     *
     * <p> The enumeration class {@code java.nio.ByteOrder} is
     * used to specify the byte order.  A value of
     * {@code ByteOrder.BIG_ENDIAN} specifies so-called
     * big-endian or network byte order, in which the high-order byte
     * comes first.  Motorola and Sparc processors store data in this
     * format, while Intel processors store data in the reverse
     * {@code ByteOrder.LITTLE_ENDIAN} order.
     *
     * <p> The byte order has no effect on the results returned from
     * the {@code readBits} method (or the value written by
     * {@code ImageOutputStream.writeBits}).
     *
     * @param byteOrder one of {@code ByteOrder.BIG_ENDIAN} or
     *                  {@code java.nio.ByteOrder.LITTLE_ENDIAN}, indicating whether
     *                  network byte order or its reverse will be used for future
     *                  reads.
     * @see java.nio.ByteOrder
     * @see #getByteOrder
     * @see #readBits(int)
     */
    @Override
    public synchronized void setByteOrder(ByteOrder byteOrder) {
        try {
            eraf.setByteOrder(byteOrder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Moves the stream position relative to its current position by a
     * given number of bytes. Positive values move forward and negative
     * values move backward. The movement is clamped to the stream bounds,
     * and the actual signed number of bytes skipped is returned. The bit
     * offset is set to zero before moving the position.
     *
     * @param n an {@code int} containing the number of bytes to
     *          be skipped.
     * @return an {@code int} representing the number of bytes skipped.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized int skipBytes(int n) throws IOException {
        setBitOffset(0);
        return eraf.skipBytes(n);
    }

    /**
     * Reads a single byte from the stream and returns it as an
     * integer between 0 and 255.  If the end of the stream is
     * reached, -1 is returned.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return a byte value from the stream, as an int, or -1 to
     * indicate EOF.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized int read() throws IOException {
        setBitOffset(0);
        return eraf.read();
    }

    /**
     * Reads up to {@code len} bytes from the stream, and stores
     * them into {@code b} starting at index {@code off}.
     * The number of bytes read is returned.  If no bytes can be read
     * because the end of the stream has been reached, {@code -1}
     * is returned.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param b   an array of bytes to be written to.
     * @param off the starting position within {@code b} to write to.
     * @param len the maximum number of {@code byte}s to read.
     * @return the number of bytes actually read, or {@code -1}
     * to indicate EOF.
     * @throws NullPointerException      if {@code b} is
     *                                   {@code null}.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off + len}
     *                                   is greater than {@code b.length}.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        if (b == null) {
            throw new NullPointerException("b == null!");
        }
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off+len > b.length || off+len < 0!");
        }
        bitOffset = 0;
        if (len == 0) {
            return 0;
        }
        return eraf.readBytes(b, off, len);
    }

    /**
     * Reads up to {@code len} bytes from the stream, and
     * modifies the supplied {@code IIOByteBuffer} to indicate
     * the byte array, offset, and length where the data may be found.
     * The caller should not attempt to modify the data found in the
     * {@code IIOByteBuffer}.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @param buf an IIOByteBuffer object to be modified.
     * @param len the maximum number of {@code byte}s to read.
     * @throws IndexOutOfBoundsException if {@code len} is
     *                                   negative.
     * @throws NullPointerException      if {@code buf} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void readBytes(IIOByteBuffer buf, int len) throws IOException {
        checkClosed();
        if (len < 0) {
            throw new IndexOutOfBoundsException("len < 0!");
        }
        if (buf == null) {
            throw new NullPointerException("buf == null!");
        }
        bitOffset = 0;
        byte[] data = new byte[len];
        len = read(data, 0, len);
        if (len < 0) {
            len = 0;
        }
        buf.setData(data);
        buf.setOffset(0);
        buf.setLength(len);
    }

    /**
     * Reads a byte from the stream and returns a {@code boolean}
     * value of {@code true} if it is nonzero, {@code false}
     * if it is zero.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return a boolean value from the stream.
     * @throws EOFException if the end of the stream is reached.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized boolean readBoolean() throws IOException {
        setBitOffset(0);
        return this.eraf.readBoolean();
    }

    /**
     * Returns the length of the underlying eraf, or <code>-1</code> if it is
     * unknown.
     *
     * @return the eraf length as a <code>long</code>, or <code>-1</code>.
     */
    @Override
    public final synchronized long length() {
        try {
            checkClosed();
            return eraf.length();
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * Sets the current stream position and resets the bit offset to 0. It is
     * legal to seeking past the end of the eraf; an <code>EOFException</code>
     * will be thrown only if a read is performed. The eraf length will not be
     * increased until a write is performed.
     *
     * @throws IndexOutOfBoundsException if <code>pos</code> is smaller than the flushed
     *                                   position.
     * @throws IOException               if any other I/O error occurs.
     */
    @Override
    public synchronized void seek(long pos) throws IOException {
        checkClosed();
        if (pos < getFlushedPosition()) {
            throw new IndexOutOfBoundsException("IndexOutOfBounds pos < flushedPos! " + pos + " < " + getFlushedPosition());
        }
        bitOffset = 0;
        eraf.seek(pos);
    }

    /**
     * Marks a position in the stream to be returned to by a
     * subsequent call to {@code reset}.  Unlike a standard
     * {@code InputStream}, all {@code ImageInputStream}s
     * support marking.  Additionally, calls to {@code mark} and
     * {@code reset} may be nested arbitrarily.
     *
     * <p> Unlike the {@code mark} methods declared by the
     * {@code Reader} and {@code InputStream} interfaces, no
     * {@code readLimit} parameter is used.  An arbitrary amount
     * of data may be read following the call to {@code mark}.
     *
     * <p> The bit position used by the {@code readBits} method
     * is saved and restored by each pair of calls to
     * {@code mark} and {@code reset}.
     *
     * <p> Note that it is valid for an {@code ImageReader} to call
     * {@code flushBefore} as part of a read operation.
     * Therefore, if an application calls {@code mark} prior to
     * passing that stream to an {@code ImageReader}, the application
     * should not assume that the marked position will remain valid after
     * the read operation has completed.
     */
    @Override
    public synchronized void mark() {
        try {
            markByteStack.push(getStreamPosition());
            markBitStack.push(getBitOffset());
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns the stream pointer to its previous position, including
     * the bit offset, at the time of the most recent unmatched call
     * to {@code mark}.
     *
     * <p> Calls to {@code reset} without a corresponding call
     * to {@code mark} have no effect.
     *
     * <p> An {@code IOException} will be thrown if the previous
     * marked position lies in the discarded portion of the stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void reset() throws IOException {
        checkClosed();
        if (markByteStack.isEmpty() || markBitStack.isEmpty()) {
            return;
        }

        long pos = markByteStack.peek();
        int offset = markBitStack.peek();
        if (pos < this.eraf.getFlushedPosition()) {
            throw new IIOException
                    ("Previous marked position has been discarded!");
        }
        seek(pos);
        markByteStack.pop();
        markBitStack.pop();
        setBitOffset(offset);
    }

    /**
     * Discards the initial portion of the stream prior to the
     * indicated position.  Attempting to seek to an offset within the
     * flushed portion of the stream will result in an
     * {@code IndexOutOfBoundsException}.
     *
     * <p> Calling {@code flushBefore} may allow classes
     * implementing this interface to free up resources such as memory
     * or disk space that are being used to store data from the
     * stream.
     *
     * @param pos a {@code long} containing the length of the
     *            stream prefix that may be flushed.
     * @throws IndexOutOfBoundsException if {@code pos} lies
     *                                   in the flushed portion of the stream or past the current stream
     *                                   position.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void flushBefore(long pos) throws IOException {
        checkClosed();
        this.eraf.flushBefore(pos);
    }

    /**
     * Flushes all buffered bytes to the underlying file.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void flush() throws IOException {
        checkClosed();
        this.eraf.flush();
    }

    /**
     * Returns the earliest position in the stream to which seeking
     * may be performed.  The returned value will be the maximum of
     * all values passed into previous calls to
     * {@code flushBefore}.
     *
     * @return the earliest legal position for seeking, as a
     * {@code long}.
     */
    @Override
    public synchronized long getFlushedPosition() {
        return this.eraf.getFlushedPosition();
    }

    /**
     * Closes the underlying {@link EnhancedNIORandomAccessFile}.
     *
     * @throws IOException in case something bad happens.
     */
    @Override
    public synchronized void close() throws IOException {
        try {
            if (!isClosed) {
                eraf.close();
            }
        } finally {
            isClosed = true;
        }
    }

    protected final synchronized boolean isClosed() {
        return isClosed;
    }

    protected final synchronized void checkClosed() throws IOException {
        if (isClosed) {
            throw new IOException("closed");
        }
    }

    /**
     * Retrieves the {@link Path} we are connected to.
     */
    public final synchronized Path getFile() {
        return file;
    }

    /**
     * Disposes this {@link NIOFileImageInputStream} by closing its
     * underlying {@link EnhancedNIORandomAccessFile}.
     */
    public final synchronized void dispose() {
        try {
            close();
        } catch (IOException e) {
            //
        }
    }

    /**
     * Provides a simple description for this {@link ImageInputStream}.
     *
     * @return a simple description for this {@link ImageInputStream}.
     */
    @Override
    public synchronized String toString() {
        return "NIOFileImageInputStream which points to " + this.file.toString();
    }
}
