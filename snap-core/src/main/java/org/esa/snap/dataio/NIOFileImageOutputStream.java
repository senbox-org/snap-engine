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

import javax.imageio.stream.ImageOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * An implementation of {@link ImageOutputStream} that take its output on a
 * {@link Path}.
 *
 * <p>Public stream operations are synchronized. Concurrent callers share the
 * same stream position and pending bit buffer; callers must still externally
 * synchronize a sequence of multiple operations that must be atomic. This
 * synchronization also applies to inherited cursor, bit-offset, flush, and
 * close operations, which share the same stream state.</p>
 *
 * <p>The default constructors and factory method preserve an existing target
 * file. Use the overload with {@code truncateExisting == true} to truncate an
 * existing file when opening it.</p>
 *
 * <p>{@link #flush()} makes buffered bytes available through the underlying
 * file, but does not guarantee that the bytes are durable on the storage
 * device. Durability requires a separately managed channel or another
 * application-specific force operation.</p>
 *
 * <p>The target file must not be modified through another channel or process
 * while this stream is open. Such modifications can invalidate the cached
 * stream contents and position.</p>
 *
 * @author Daniele Romagnoli, GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public final class NIOFileImageOutputStream extends NIOFileImageInputStream
        implements ImageOutputStream {

    /**
     * Creates a new instance of {@link NIOFileImageOutputStream}
     *
     * @param file the input file {@link Path}
     * @return the {@link NIOFileImageOutputStream} new instance
     * @throws NullPointerException  if <code>file</code> is <code>null</code>.
     * @throws SecurityException     if a security manager exists and does not allow writing
     *                               access to the eraf.
     * @throws FileNotFoundException if <code>file</code> is a directory or cannot be opened
     *                               for writing for any other reason.
     * @throws IOException           if an I/O error occurs.
     */
    public static ImageOutputStream createOutputStream(final Path file) throws IOException {
        return new NIOFileImageOutputStream(file);
    }

    /**
     * Creates an output stream for a {@link Path}.
     *
     * @param file             the output file
     * @param truncateExisting whether an existing file shall be truncated when opened
     * @return the new output stream
     * @throws IOException if an I/O error occurs
     */
    public static ImageOutputStream createOutputStream(final Path file,
                                                        final boolean truncateExisting) throws IOException {
        return new NIOFileImageOutputStream(file, truncateExisting);
    }

    /**
     * Creates an output stream for a {@link Path} using the specified buffer size.
     *
     * @param file             the output file
     * @param bufSize          size of the underlying buffer
     * @param truncateExisting whether an existing file shall be truncated when opened
     * @return the new output stream
     * @throws IOException if an I/O error occurs
     */
    public static ImageOutputStream createOutputStream(final Path file, final int bufSize,
                                                        final boolean truncateExisting) throws IOException {
        return new NIOFileImageOutputStream(file, bufSize, truncateExisting);
    }

    /**
     * Constructs a {@link NIOFileImageOutputStream} that will write to a
     * given {@link Path}.
     *
     * @param file a {@link Path} to write to.
     * @throws NullPointerException  if <code>file</code> is <code>null</code>.
     * @throws SecurityException     if a security manager exists and does not allow writing
     *                               access to the eraf.
     * @throws FileNotFoundException if <code>file</code> is a directory or cannot be opened
     *                               for writing for any other reason.
     * @throws IOException           if an I/O error occurs.
     */
    public NIOFileImageOutputStream(Path file)
            throws IOException {
        this(file, false);
    }

    /**
     * Constructs an output stream for a given {@link Path}.
     *
     * <p>If {@code truncateExisting} is {@code false}, existing contents are
     * preserved and writes modify the file in place.</p>
     *
     * @param file             a {@link Path} to write to
     * @param truncateExisting whether an existing file shall be truncated when opened
     * @throws IOException if an I/O error occurs
     */
    public NIOFileImageOutputStream(Path file, boolean truncateExisting)
            throws IOException {
        this(file, createRandomAccessFile(file, truncateExisting));
    }

    /**
     * Constructs a {@link NIOFileImageOutputStream} that will write to a
     * given {@link Path}.
     *
     * @param file    a {@link Path} to write to.
     * @param bufSize size of the underlying buffer.
     * @throws NullPointerException  if <code>file</code> is <code>null</code>.
     * @throws SecurityException     if a security manager exists and does not allow writing
     *                               access to the eraf.
     * @throws FileNotFoundException if <code>file</code> is a directory or cannot be opened
     *                               for writing for any other reason.
     * @throws IOException           if an I/O error occurs.
     */
    public NIOFileImageOutputStream(Path file, int bufSize)
            throws IOException {
        this(file, bufSize, false);
    }

    /**
     * Constructs an output stream for a given {@link Path} using the specified
     * underlying buffer size.
     *
     * <p>If {@code truncateExisting} is {@code false}, existing contents are
     * preserved and writes modify the file in place.</p>
     *
     * @param file             a {@link Path} to write to
     * @param bufSize          size of the underlying buffer
     * @param truncateExisting whether an existing file shall be truncated when opened
     * @throws IOException if an I/O error occurs
     */
    public NIOFileImageOutputStream(Path file, int bufSize, boolean truncateExisting)
            throws IOException {
        this(file, createRandomAccessFile(file, bufSize, truncateExisting));
    }

    private static EnhancedNIORandomAccessFile createRandomAccessFile(Path file, boolean truncateExisting)
            throws IOException {
        if (truncateExisting) {
            return new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ, StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        return new EnhancedNIORandomAccessFile(file, StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE);
    }

    private static EnhancedNIORandomAccessFile createRandomAccessFile(Path file, int bufSize,
                                                                       boolean truncateExisting)
            throws IOException {
        if (truncateExisting) {
            return new EnhancedNIORandomAccessFile(file, bufSize, StandardOpenOption.READ, StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        return new EnhancedNIORandomAccessFile(file, bufSize, StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE);
    }

    /**
     * Constructs a {@link NIOFileImageOutputStream} that will write to a
     * given {@link Path}.
     *
     * @param file a {@link Path} to write to.
     * @param eraf the {@link EnhancedNIORandomAccessFile} instance
     * @throws NullPointerException  if <code>file</code> is <code>null</code>.
     * @throws SecurityException     if a security manager exists and does not allow writing
     *                               access to the eraf.
     * @throws FileNotFoundException if <code>file</code> is a directory or cannot be opened
     *                               for writing for any other reason.
     * @throws IOException           if an I/O error occurs.
     */
    private NIOFileImageOutputStream(Path file, EnhancedNIORandomAccessFile eraf) throws IOException {
        super(file, eraf);
        // NOTE: this must be done accordingly to what ImageInputStreamImpl
        // does, otherwise some ImageReader subclasses might not work.
        this.eraf.setByteOrder(getByteOrder());
    }

    /**
     * Writes a single byte to the stream at the current position.
     * The 24 high-order bits of {@code b} are ignored.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param b an {@code int} whose lower 8 bits are to be
     *          written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void write(int b) throws IOException {
        flushBits();
        writeByteDirect(b);
    }

    /**
     * Writes a sequence of bytes to the stream at the current
     * position.  If {@code b.length} is 0, nothing is written.
     * The byte {@code b[0]} is written first, then the byte
     * {@code b[1]}, and so on.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param b an array of {@code byte}s to be written.
     * @throws NullPointerException if {@code b} is
     *                              {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    @Override
    public synchronized void write(byte[] b) throws IOException {
        checkClosed();
        Objects.requireNonNull(b, "b");
        flushBits();
        eraf.write(b);
    }

    /**
     * Writes a sequence of bytes to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The byte {@code b[off]} is written first, then the byte
     * {@code b[off + 1]}, and so on.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param b   an array of {@code byte}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code byte}s to write.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative,
     *                                   or the requested range exceeds {@code b.length}.
     * @throws NullPointerException      if {@code b} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        checkClosed();
        Objects.requireNonNull(b, "b");
        checkArrayRange(b.length, off, len);
        flushBits();
        eraf.writeBytes(b, off, len);
    }

    /**
     * Writes a {@code boolean} value to the stream.  If
     * {@code v} is true, the value {@code (byte)1} is
     * written; if {@code v} is false, the value
     * {@code (byte)0} is written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v the {@code boolean} to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeBoolean(boolean v) throws IOException {
        flushBits();
        eraf.writeBoolean(v);
    }

    /**
     * Writes the 8 low-order bits of {@code v} to the
     * stream. The 24 high-order bits of {@code v} are ignored.
     * (This means that {@code writeByte} does exactly the same
     * thing as {@code write} for an integer argument.)
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v an {@code int} containing the byte value to be
     *          written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeByte(int v) throws IOException {
        flushBits();
        eraf.writeByte(v);
    }

    /**
     * Writes the 16 low-order bits of {@code v} to the
     * stream. The 16 high-order bits of {@code v} are ignored.
     * If the stream uses network byte order, the bytes written, in
     * order, will be:
     *
     * <pre>
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)(v &amp; 0xff)
     * </pre>
     * <p>
     * Otherwise, the bytes written will be:
     *
     * <pre>
     * (byte)(v &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * </pre>
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v an {@code int} containing the short value to be
     *          written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeShort(int v) throws IOException {
        flushBits();
        eraf.writeShort(v);
    }

    /**
     * This method is a synonym for {@link #writeShort writeShort}.
     *
     * @param v an {@code int} containing the char (unsigned
     *          short) value to be written.
     * @throws IOException if an I/O error occurs.
     * @see #writeShort(int)
     */
    @Override
    public synchronized void writeChar(int v) throws IOException {
        flushBits();
        eraf.writeChar(v);
    }

    /**
     * Writes the 32 bits of {@code v} to the stream.  If the
     * stream uses network byte order, the bytes written, in order,
     * will be:
     *
     * <pre>
     * (byte)((v &gt;&gt; 24) &amp; 0xff)
     * (byte)((v &gt;&gt; 16) &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)(v &amp; 0xff)
     * </pre>
     * <p>
     * Otherwise, the bytes written will be:
     *
     * <pre>
     * (byte)(v &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)((v &gt;&gt; 16) &amp; 0xff)
     * (byte)((v &gt;&gt; 24) &amp; 0xff)
     * </pre>
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v an {@code int} containing the value to be
     *          written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeInt(int v) throws IOException {
        flushBits();
        eraf.writeInt(v);
    }

    /**
     * Writes the 64 bits of {@code v} to the stream.  If the
     * stream uses network byte order, the bytes written, in order,
     * will be:
     *
     * <pre>
     * (byte)((v &gt;&gt; 56) &amp; 0xff)
     * (byte)((v &gt;&gt; 48) &amp; 0xff)
     * (byte)((v &gt;&gt; 40) &amp; 0xff)
     * (byte)((v &gt;&gt; 32) &amp; 0xff)
     * (byte)((v &gt;&gt; 24) &amp; 0xff)
     * (byte)((v &gt;&gt; 16) &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)(v &amp; 0xff)
     * </pre>
     * <p>
     * Otherwise, the bytes written will be:
     *
     * <pre>
     * (byte)(v &amp; 0xff)
     * (byte)((v &gt;&gt; 8) &amp; 0xff)
     * (byte)((v &gt;&gt; 16) &amp; 0xff)
     * (byte)((v &gt;&gt; 24) &amp; 0xff)
     * (byte)((v &gt;&gt; 32) &amp; 0xff)
     * (byte)((v &gt;&gt; 40) &amp; 0xff)
     * (byte)((v &gt;&gt; 48) &amp; 0xff)
     * (byte)((v &gt;&gt; 56) &amp; 0xff)
     * </pre>
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v a {@code long} containing the value to be
     *          written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeLong(long v) throws IOException {
        flushBits();
        eraf.writeLong(v);
    }

    /**
     * Writes a {@code float} value, which is comprised of four
     * bytes, to the output stream. It does this as if it first
     * converts this {@code float} value to an {@code int}
     * in exactly the manner of the {@code Float.floatToIntBits}
     * method and then writes the int value in exactly the manner of
     * the {@code writeInt} method.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v a {@code float} containing the value to be
     *          written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeFloat(float v) throws IOException {
        flushBits();
        eraf.writeFloat(v);
    }

    /**
     * Writes a {@code double} value, which is comprised of eight
     * bytes, to the output stream. It does this as if it first
     * converts this {@code double} value to a {@code long}
     * in exactly the manner of the
     * {@code Double.doubleToLongBits} method and then writes the
     * long value in exactly the manner of the {@code writeLong}
     * method.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param v a {@code double} containing the value to be
     *          written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeDouble(double v) throws IOException {
        flushBits();
        eraf.writeDouble(v);
    }

    /**
     * Writes a string to the output stream. For every character in
     * the string {@code s}, taken in order, one byte is written
     * to the output stream. If {@code s} is {@code null}, a
     * {@code NullPointerException} is thrown.
     *
     * <p> If {@code s.length} is zero, then no bytes are
     * written. Otherwise, the character {@code s[0]} is written
     * first, then {@code s[1]}, and so on; the last character
     * written is {@code s[s.length-1]}. For each character, one
     * byte is written, the low-order byte, in exactly the manner of
     * the {@code writeByte} method. The high-order eight bits of
     * each character in the string are ignored.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param s a {@code String} containing the value to be
     *          written.
     * @throws NullPointerException if {@code s} is
     *                              {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    @Override
    public synchronized void writeBytes(String s) throws IOException {
        flushBits();
        eraf.writeBytes(s);
    }

    /**
     * Writes a string to the output stream. For every character in
     * the string {@code s}, taken in order, two bytes are
     * written to the output stream, ordered according to the current
     * byte order setting.  If network byte order is being used, the
     * high-order byte is written first; the order is reversed
     * otherwise.  If {@code s} is {@code null}, a
     * {@code NullPointerException} is thrown.
     *
     * <p> If {@code s.length} is zero, then no bytes are
     * written. Otherwise, the character {@code s[0]} is written
     * first, then {@code s[1]}, and so on; the last character
     * written is {@code s[s.length-1]}.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param s a {@code String} containing the value to be
     *          written.
     * @throws NullPointerException if {@code s} is
     *                              {@code null}.
     * @throws IOException          if an I/O error occurs.
     */
    @Override
    public synchronized void writeChars(String s) throws IOException {
        flushBits();
        eraf.writeChars(s);
    }

    /**
     * Writes two bytes of length information to the output stream in
     * network byte order, followed by the
     * <a href="../../../../java.base/java/io/DataInput.html#modified-utf-8">
     * modified UTF-8</a>
     * representation of every character in the string {@code s}.
     * If {@code s} is {@code null}, a
     * {@code NullPointerException} is thrown.  Each character in
     * the string {@code s} is converted to a group of one, two,
     * or three bytes, depending on the value of the character.
     *
     * <p> If a character {@code c} is in the range
     * <code>&#92;u0001</code> through <code>&#92;u007f</code>, it is
     * represented by one byte:
     *
     * <pre>
     * (byte)c
     * </pre>
     *
     * <p> If a character {@code c} is <code>&#92;u0000</code> or
     * is in the range <code>&#92;u0080</code> through
     * <code>&#92;u07ff</code>, then it is represented by two bytes,
     * to be written in the order shown:
     *
     * <pre><code>
     * (byte)(0xc0 | (0x1f &amp; (c &gt;&gt; 6)))
     * (byte)(0x80 | (0x3f &amp; c))
     * </code></pre>
     *
     * <p> If a character {@code c} is in the range
     * <code>&#92;u0800</code> through {@code uffff}, then it is
     * represented by three bytes, to be written in the order shown:
     *
     * <pre><code>
     * (byte)(0xe0 | (0x0f &amp; (c &gt;&gt; 12)))
     * (byte)(0x80 | (0x3f &amp; (c &gt;&gt; 6)))
     * (byte)(0x80 | (0x3f &amp; c))
     * </code></pre>
     *
     * <p> First, the total number of bytes needed to represent all
     * the characters of {@code s} is calculated. If this number
     * is larger than {@code 65535}, then a
     * {@code UTFDataFormatException} is thrown. Otherwise, this
     * length is written to the output stream in exactly the manner of
     * the {@code writeShort} method; after this, the one-, two-,
     * or three-byte representation of each character in the string
     * {@code s} is written.
     *
     * <p> The current byte order setting is ignored.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * <p><strong>Note:</strong> This method should not be used in
     * the  implementation of image formats that use standard UTF-8,
     * because  the modified UTF-8 used here is incompatible with
     * standard UTF-8.
     *
     * @param s a {@code String} containing the value to be
     *          written.
     * @throws NullPointerException           if {@code s} is
     *                                        {@code null}.
     * @throws java.io.UTFDataFormatException if the modified UTF-8
     *                                        representation of {@code s} requires more than 65536 bytes.
     * @throws IOException                    if an I/O error occurs.
     */
    @Override
    public synchronized void writeUTF(String s) throws IOException {
        flushBits();
        eraf.writeUTF(s);
    }

    /**
     * Writes a sequence of shorts to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The short {@code s[off]} is written first, then the short
     * {@code s[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param s   an array of {@code short}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code short}s to write.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative,
     *                                   or the requested range exceeds {@code s.length}.
     * @throws NullPointerException      if {@code s} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void writeShorts(short[] s, int off, int len) throws IOException {
        checkClosed();
        Objects.requireNonNull(s, "s");
        checkArrayRange(s.length, off, len);
        flushBits();
        eraf.writeShorts(s, off, len);
    }

    /**
     * Writes a sequence of chars to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The char {@code c[off]} is written first, then the char
     * {@code c[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param c   an array of {@code char}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code char}s to write.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative,
     *                                   or the requested range exceeds {@code c.length}.
     * @throws NullPointerException      if {@code c} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void writeChars(char[] c, int off, int len) throws IOException {
        checkClosed();
        Objects.requireNonNull(c, "c");
        checkArrayRange(c.length, off, len);
        flushBits();
        eraf.writeChars(c, off, len);
    }

    /**
     * Writes a sequence of ints to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The int {@code i[off]} is written first, then the int
     * {@code i[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param i   an array of {@code int}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code int}s to write.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative,
     *                                   or the requested range exceeds {@code i.length}.
     * @throws NullPointerException      if {@code i} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void writeInts(int[] i, int off, int len) throws IOException {
        checkClosed();
        Objects.requireNonNull(i, "i");
        checkArrayRange(i.length, off, len);
        flushBits();
        eraf.writeInts(i, off, len);
    }

    /**
     * Writes a sequence of longs to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The long {@code l[off]} is written first, then the long
     * {@code l[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param l   an array of {@code long}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code long}s to write.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative,
     *                                   or the requested range exceeds {@code l.length}.
     * @throws NullPointerException      if {@code l} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void writeLongs(long[] l, int off, int len) throws IOException {
        checkClosed();
        Objects.requireNonNull(l, "l");
        checkArrayRange(l.length, off, len);
        flushBits();
        eraf.writeLongs(l, off, len);
    }

    /**
     * Writes a sequence of floats to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The float {@code f[off]} is written first, then the float
     * {@code f[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param f   an array of {@code float}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code float}s to write.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative,
     *                                   or the requested range exceeds {@code f.length}.
     * @throws NullPointerException      if {@code f} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void writeFloats(float[] f, int off, int len) throws IOException {
        checkClosed();
        Objects.requireNonNull(f, "f");
        checkArrayRange(f.length, off, len);
        flushBits();
        eraf.writeFloats(f, off, len);
    }

    /**
     * Writes a sequence of doubles to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The double {@code d[off]} is written first, then the double
     * {@code d[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * <p> If the bit offset within the stream is non-zero, the
     * remainder of the current byte is padded with 0s
     * and written out first.  The bit offset will be 0 after the
     * write.
     *
     * @param d   an array of {@code doubles}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code double}s to write.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative,
     *                                   or the requested range exceeds {@code d.length}.
     * @throws NullPointerException      if {@code d} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized void writeDoubles(double[] d, int off, int len) throws IOException {
        checkClosed();
        Objects.requireNonNull(d, "d");
        checkArrayRange(d.length, off, len);
        flushBits();
        eraf.writeDoubles(d, off, len);
    }

    /**
     * Writes a single bit, given by the least significant bit of the
     * argument, to the stream at the current bit offset within the
     * current byte position.  The upper 31 bits of the argument are
     * ignored.  The given bit replaces the previous bit at that
     * position.  The bit offset is advanced by one and reduced modulo
     * 8.
     *
     * <p> If any bits of a particular byte have never been set
     * at the time the byte is flushed to the destination, those
     * bits will be set to 0 automatically.
     *
     * @param bit an {@code int} whose least significant bit
     *            is to be written to the stream.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeBit(int bit) throws IOException {
        checkClosed();
        writeBits((1L & bit), 1);
    }

    /**
     * Writes a sequence of bits, given by the {@code numBits}
     * least significant bits of the {@code bits} argument in
     * left-to-right order, to the stream at the current bit offset
     * within the current byte position.  The upper {@code 64 - numBits}
     * bits of the argument are ignored.  The bit
     * offset is advanced by {@code numBits} and reduced modulo
     * 8.  Note that a bit offset of 0 always indicates the
     * most-significant bit of the byte, and bytes of bits are written
     * out in sequence as they are encountered.  Thus bit writes are
     * always effectively in network byte order.  The actual stream
     * byte order setting is ignored.
     *
     * <p> Bit data may be accumulated in memory indefinitely, until
     * {@code flushBefore} is called.  At that time, all bit data
     * prior to the flushed position will be written.
     *
     * <p> If any bits of a particular byte have never been set
     * at the time the byte is flushed to the destination, those
     * bits will be set to 0 automatically.
     *
     * @param bits    a {@code long} containing the bits to be
     *                written, starting with the bit in position {@code numBits - 1}
     *                down to the least significant bit.
     * @param numBits an {@code int} between 0 and 64, inclusive.
     * @throws IllegalArgumentException if {@code numBits} is
     *                                  not between 0 and 64, inclusive.
     * @throws IOException              if an I/O error occurs.
     */
    @Override
    public synchronized void writeBits(long bits, int numBits) throws IOException {
        checkClosed();
        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException("Bad value for numBits!");
        }
        if (numBits == 0) {
            return;
        }

        // Prologue: deal with pre-existing bits

        // Bug 4499158, 4507868 - if we're at the beginning of the stream
        // and the bit offset is 0, there can't be any pre-existing bits
        if ((getStreamPosition() > 0) || (bitOffset > 0)) {
            int offset = bitOffset;  // read() will reset bitOffset
            int partialByte = read();
            if (partialByte != -1) {
                seek(getStreamPosition() - 1);
            } else {
                partialByte = 0;
            }

            if (numBits + offset < 8) {
                // Notch out the partial byte and drop in the new bits
                int shift = 8 - (offset + numBits);
                int mask = -1 >>> (32 - numBits);
                partialByte &= ~(mask << shift);  // Clear out old bits
                partialByte = (int) (partialByte | ((bits & mask) << shift)); // Or in new ones
                writeByteDirect(partialByte);
                seek(getStreamPosition() - 1);
                bitOffset = offset + numBits;
                numBits = 0;  // Signal that we are done
            } else {
                // Fill out the partial byte and reduce numBits
                int num = 8 - offset;
                int mask = -1 >>> (32 - num);
                partialByte &= ~mask;  // Clear out bits
                partialByte = (int) (partialByte | ((bits >> (numBits - num)) & mask));
                // Note that bitOffset is already 0, so there is no risk
                // of this advancing to the next byte
                writeByteDirect(partialByte);
                numBits -= num;
            }
        }

        // Now write any whole bytes
        if (numBits > 7) {
            // numBits is the number of input bits still to consume, while
            // bits retains the original value. The extra low-order bits are
            // therefore part of the shift used to select each whole byte.
            int extra = numBits % 8;
            for (int numBytes = numBits / 8; numBytes > 0; numBytes--) {
                int shift = (numBytes - 1) * 8 + extra;
                int value = (int) ((shift == 0)
                        ? bits & 0xFF
                        : (bits >> shift) & 0xFF);
                writeByteDirect(value);
            }
            numBits = extra;
        }

        // Epilogue: write out remaining partial byte, if any
        // Note that we may be at EOF, in which case we pad with 0,
        // or not, in which case we must preserve the existing bits
        if (numBits != 0) {
            // If we are not at the end of the file, read the current byte
            // If we are at the end of the file, initialize our byte to 0.
            int partialByte;
            partialByte = read();
            if (partialByte != -1) {
                seek(getStreamPosition() - 1);
            }
            // Fix 4494976: writeBit(int) does not pad the remainder
            // of the current byte with 0s
            else { // EOF
                partialByte = 0;
            }

            int shift = 8 - numBits;
            int mask = -1 >>> (32 - numBits);
            partialByte &= ~(mask << shift);
            partialByte = (int) (partialByte | (bits & mask) << shift);
            // bitOffset is always already 0 when we get here.
            writeByteDirect(partialByte);
            seek(getStreamPosition() - 1);
            bitOffset = numBits;
        }
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
        if (pos < getFlushedPosition()) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        if (pos > getStreamPosition()) {
            throw new IndexOutOfBoundsException("pos > streamPosition!");
        }
        flushBits();
        this.eraf.flushBefore(pos);
    }

    /**
     * Flushes pending bit data and all buffered bytes to the underlying file.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void flush() throws IOException {
        checkClosed();
        flushBits();
        this.eraf.flush();
    }

    /**
     * Closes the underlying {@link EnhancedNIORandomAccessFile}.
     *
     * @throws IOException in case something bad happens.
     */
    @Override
    public synchronized void close() throws IOException {
        try {
            if (!isClosed()) {
                flushBits();
            }
        } catch (IOException | RuntimeException | Error exception) {
            try {
                super.close();
            } catch (Throwable closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }

        super.close();
    }

    /**
     * If the bit offset is non-zero, forces the remaining bits
     * in the current byte to 0 and advances the stream position
     * by one.
     *
     * @throws IOException if an I/O error occurs.
     */
    private void flushBits() throws IOException {
        checkClosed();
        if (bitOffset != 0) {
            int offset = bitOffset;
            int partialByte = read(); // Sets bitOffset to 0
            if (partialByte < 0) {
                // Fix 4465683: When bitOffset is set
                // to something non-zero beyond EOF,
                // we should set that whole byte to
                // zero and write it to stream.
                partialByte = 0;
                bitOffset = 0;
            } else {
                seek(getStreamPosition() - 1);
                partialByte &= -1 << (8 - offset);
            }
            writeByteDirect(partialByte);
        }
    }

    /**
     * Writes one byte without changing bit state or acquiring the stream
     * monitor. The caller must hold this stream's monitor and must have
     * handled any pending bits before calling this method.
     */
    private void writeByteDirect(int b) throws IOException {
        eraf.write(b);
    }

    private static void checkArrayRange(int arrayLength, int off, int len) {
        Objects.checkFromIndexSize(off, len, arrayLength);
    }

    /**
     * Provides a simple description for this {@link ImageOutputStream}.
     *
     * @return a simple description for this {@link ImageOutputStream}.
     */
    @Override
    public synchronized String toString() {
        return "NIOFileImageOutputStream which points to " + this.file.toString();
    }
}
