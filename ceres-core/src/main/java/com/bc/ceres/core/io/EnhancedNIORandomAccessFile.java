/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
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
package com.bc.ceres.core.io;

import com.bc.ceres.util.CleanUpState;
import com.bc.ceres.util.CleanerRegistry;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * @author Simone Giannecchini, GeoSolutions.
 * <p/>
 * -- NOTES from a class we derived this class from --
 * <p/>
 * <p/>
 * RandomAccessFile.java. By Russ Rew, based on BufferedRandomAccessFile by Alex
 * McManus, based on Sun's source code for java.io.RandomAccessFile. For Alex
 * McManus version from which this derives, see his <a
 * href="http://www.aber.ac.uk/~agm/Java.html"> Freeware Java Classes</a>.
 * <p/>
 * A buffered drop-in replacement for java.io.RandomAccessFile. Instances of
 * this class realise substantial speed increases over java.io.RandomAccessFile
 * through the use of buffering. This is a subclass of Object, as it was not
 * possible to subclass java.io.RandomAccessFile because many of the methods are
 * final. However, if it is necessary to use RandomAccessFile and
 * java.io.RandomAccessFile interchangeably, both classes implement the
 * DataInput and DataOutput interfaces.
 * @author Alex McManus
 * @author Russ Rew
 * @author john caron
 * @version $Id: EnhancedNIORandomAccessFile.java,v 1.2 2011-08-17 19:18:57 lveci Exp $
 * @see DataInput
 * @see DataOutput
 * @see java.io.RandomAccessFile
 *
 * <p>All operations exposed by this class are serialized on the instance.
 * Use {@link #withChannel(ChannelOperation)} or
 * {@link #withChannelVoid(ChannelConsumer)} for coordinated channel access.
 * The channel passed to a callback must not be retained or used after the
 * callback returns. Callbacks execute while this instance is locked and must
 * not block waiting for another operation on this instance.</p>
 */
public final class EnhancedNIORandomAccessFile implements DataInput, DataOutput, Closeable {

    /**
     * _more_
     */
    static public final int BIG_ENDIAN = 0;

    /**
     * _more_
     */
    static public final int LITTLE_ENDIAN = 1;

    // debug leaks - keep track of open files

    /**
     * The default buffer size, in bytes.
     */
    public static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

    private static final int W_ATTEMPTS_LIMIT = 10;

    /**
     * _more_
     */
    private final Path file;


    /**
     * The offset in bytes from the eraf start, of the next read or write
     * operation.
     */
    private long filePosition;


    /**
     * The offset in bytes of the end of the data in the buffer, from the start
     * of the eraf. This can be calculated from
     * <code>bufferStart + dataSize</code>, but it is cached to speed up the
     * read( ) method.
     */
    private long dataEnd;

    /**
     * True if we are at the end of the eraf.
     */
    private boolean endOfFile;


    /**
     * The current endian (big or little) mode of the eraf.
     */
    private boolean bigEndian = true;


    private final EnhancedRandomAccessFileState state;

    /**
     * Constructor, default buffer size.
     *
     * @param file  file of the eraf
     * @param modes same as for java.nio.channels.FileChannel; at least one
     *              explicit mode, such as {@link StandardOpenOption#READ}
     *              or {@link StandardOpenOption#WRITE}, is required
     * @throws IOException if an I/O error occurs.
     */
    public EnhancedNIORandomAccessFile(Path file, OpenOption... modes)
            throws IOException {
        this(file, DEFAULT_BUFFER_SIZE, modes);
        setCached(false);
    }

    /**
     * Constructor.
     *
     * @param file       file of the eraf
     * @param modes      same as for java.nio.channels.FileChannel; at least one
     *                   explicit mode, such as {@link StandardOpenOption#READ}
     *                   or {@link StandardOpenOption#WRITE}, is required
     * @param bufferSize size of buffer to use.
     * @throws IOException if an I/O error occurs.
     */
    public EnhancedNIORandomAccessFile(Path file, int bufferSize, OpenOption... modes)
            throws IOException {
        this(file, new byte[Math.max(bufferSize, 0)], modes);
        setCached(true);
    }

    /**
     * Constructor.
     *
     * @param file   file of the eraf
     * @param modes  same as for java.nio.channels.FileChannel; at least one
     *               explicit mode, such as {@link StandardOpenOption#READ}
     *               or {@link StandardOpenOption#WRITE}, is required
     * @param buffer the buffer to use.
     * @throws IOException if an I/O error occurs.
     */
    public EnhancedNIORandomAccessFile(Path file, byte[] buffer, OpenOption... modes)
            throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (buffer == null) {
            throw new IllegalArgumentException("buffer cannot be null");
        }
        if (modes == null) {
            throw new IllegalArgumentException("modes cannot be null");
        }
        if (modes.length == 0) {
            throw new IllegalArgumentException("modes must not be empty");
        }
        for (OpenOption mode : modes) {
            if (mode == null) {
                throw new IllegalArgumentException("modes cannot contain null");
            }
        }
        if (buffer.length == 0) {
            throw new IllegalArgumentException("bufferSize must be positive");
        }
        if (Arrays.asList(modes).contains(StandardOpenOption.APPEND)) {
            throw new IllegalArgumentException("APPEND is not supported due to contradiction with random access semantics.");
        }
        this.file = file;
        this.dataEnd = 0;
        this.filePosition = 0;
        this.endOfFile = false;

        final FileChannel fileChannel = FileChannel.open(file, modes);
        this.state = new EnhancedRandomAccessFileState(
                fileChannel,
                buffer,
                0,
                0,
                Stream.of(modes).noneMatch(mode -> mode.equals(StandardOpenOption.WRITE) || mode.equals(StandardOpenOption.APPEND)),
                Stream.of(modes).noneMatch(mode -> mode.equals(StandardOpenOption.READ)),
                false,
                -1);
        CleanerRegistry.getInstance().register(this, state);
    }

    /**
     * Close the eraf, and release any associated system resources.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void close() throws IOException {
        if (state.isFullyClosed()) {
            return;
        }
        IOException failure = null;
        try {
            if (!state.isClosed() || state.cacheChannel != null) {
                flush();
                state.closeState();
            }
        } catch (IOException e) {
            failure = e;
        } finally {
            try {
                closeCache();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            } finally {
                CleanerRegistry.getInstance().cleanup(this);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void checkClosed() throws IOException {
        if (state.isClosed()) {
            throw new IOException("closed");
        }
    }

    private static void checkArrayNotNull(Object array, String name) {
        if (array == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    /**
     * Return true if eraf pointer is at end of eraf.
     *
     * @return true if eraf pointer is at end of eraf
     * @throws IOException if an I/O error occurs, eg. this {@code EnhancedNIORandomAccessFile} is closed
     */
    public synchronized boolean isAtEndOfFile() throws IOException {
        checkClosed();
        return endOfFile;
    }

    /**
     * Executes an operation on the underlying channel while keeping this
     * instance's logical position and buffer state consistent.
     * <p>
     * Pending buffered writes are flushed before the operation.  After the
     * operation, the channel position becomes the ERAF position and the
     * buffer is discarded so that subsequent reads cannot use stale data.
     * If the operation fails after modifying the channel, the buffer is still
     * invalidated.
     *
     * @param operation the operation to execute on the underlying channel
     * @param <T>       the operation result type
     * @return the operation result
     * @throws IOException if this ERAF is closed or the operation fails
     * @implNote The callback must not retain or close the channel, use it after
     * the callback returns, or block waiting for another operation on this
     * instance.
     */
    public synchronized <T> T withChannel(ChannelOperation<T> operation) throws IOException {
        checkClosed();
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        flush();
        state.fileChannel.position(filePosition);
        Throwable operationFailure = null;
        try {
            return operation.execute(state.fileChannel);
        } catch (IOException | RuntimeException | Error exception) {
            operationFailure = exception;
            throw exception;
        } finally {
            try {
                synchronizeFromChannel();
            } catch (IOException | RuntimeException synchronizationFailure) {
                if (operationFailure != null) {
                    operationFailure.addSuppressed(synchronizationFailure);
                } else {
                    throw synchronizationFailure;
                }
            }
        }
    }

    /**
     * Executes a void operation on the underlying channel while keeping this
     * instance's logical position and buffer state consistent.
     *
     * @param operation the operation to execute on the underlying channel
     * @throws IOException if this ERAF is closed or the operation fails
     * @implNote The callback must not retain or close the channel, use it after
     * the callback returns, or block waiting for another operation on this
     * instance.
     */
    public synchronized void withChannelVoid(ChannelConsumer operation) throws IOException {
        checkClosed();
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        withChannel(channel -> {
            operation.accept(channel);
            return null;
        });
    }

    /**
     * Synchronizes this ERAF after a scoped channel callback.
     * <p>
     * Any pending ERAF writes are flushed first.  The channel's current
     * position becomes the ERAF position, and all cached bytes are discarded.
     * This uses a last-writer-wins policy for external writes; buffered bytes
     * are not merged with changes made through the channel.
     *
     * @throws IOException if this ERAF or its underlying channel is closed
     */
    private synchronized void synchronizeFromChannel() throws IOException {
        checkClosed();
        flush();
        filePosition = state.fileChannel.position();
        invalidateBuffer();
        state.cachedRanges.clear();
    }

    private void invalidateBuffer() {
        state.bufferStart = filePosition;
        state.dataSize = 0;
        state.bufferModified = false;
        dataEnd = filePosition;
        endOfFile = false;
    }

    @FunctionalInterface
    public interface ChannelOperation<T> {
        /**
         * Executes while the owning ERAF instance is locked.
         *
         * @param channel the underlying channel, available only during the
         *                callback; it must not be retained or closed
         * @return the operation result
         * @throws IOException if channel access fails
         */
        T execute(FileChannel channel) throws IOException;
    }

    @FunctionalInterface
    public interface ChannelConsumer {
        /**
         * Executes while the owning ERAF instance is locked.
         *
         * @param channel the underlying channel, available only during the
         *                callback; it must not be retained or closed
         * @throws IOException if channel access fails
         */
        void accept(FileChannel channel) throws IOException;
    }

    /**
     * Set the position in the eraf for the next read or write.
     *
     * @param pos the offset (in bytes) from the start of the eraf.
     * @throws IOException if an I/O error occurrs.
     */
    public synchronized void seek(long pos) throws IOException {
        checkClosed();
        if (pos < 0) {
            throw new IndexOutOfBoundsException("Negative positioning not allowed. Positioning pointer 'pos' must be positive.");
        }
        if (pos < getFlushedPosition()) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        // If the seek is into the buffer, just update the eraf pointer.
        if ((pos >= state.bufferStart) && (pos < dataEnd)) {
            filePosition = pos;
            endOfFile = false;
            return;
        }

        // If the current buffer is modified, write it to disk.
        if (!state.readonly && state.bufferModified && cached) {
            flush();
        }

        // need new buffer
        state.bufferStart = pos;
        filePosition = pos;

        state.fileChannel.position(pos);
        int wAttempts = 0;
        while (true) {
            if (state.readonly) {
                state.dataSize = read_(pos, state.buffer, 0, state.buffer.length);
                if (state.dataSize == 0) {
                    wAttempts = wAttempts + 1;
                    if (wAttempts >= W_ATTEMPTS_LIMIT) {
                        throw new IOException("fail to read after " + (pos) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
                    }
                } else {
                    break;
                }
            } else if (!state.writeOnly) {
                state.dataSize = read_(pos, state.buffer, 0, 1);
                break;
            } else {
                state.dataSize = 0;
                break;
            }
        }

        if (state.dataSize < 0) {
            state.dataSize = 0;
            endOfFile = true;
        } else {
            endOfFile = false;
        }

        // Cache the position of the buffer end.
        dataEnd = state.bufferStart + state.dataSize;
    }

    /**
     * Returns the current position in the eraf, where the next read or write
     * will occur.
     *
     * @return the offset from the start of the eraf in bytes.
     * @throws IOException if an I/O error occurs, eg. this {@code EnhancedNIORandomAccessFile} is closed
     */
    public synchronized long getFilePointer() throws IOException {
        checkClosed();
        return filePosition;
    }

    /**
     * Get the eraf file, or name.
     *
     * @return _more_
     */
    public synchronized Path getFile() {
        return file;
    }

    /**
     * Get the length of the eraf. The data in the buffer (which may not have
     * been written the disk yet) is taken into account.
     *
     * @return the length of the eraf in bytes.
     * @throws IOException if an I/O error occurrs.
     */
    public synchronized long length() throws IOException {
        checkClosed();
        long fileLength = state.fileChannel.size();
        return state.bufferModified ? Math.max(fileLength, dataEnd) : fileLength;
    }

    /**
     * Change the current endian mode. Subsequent reads of short, int, float, double, long, char will use this.
     *
     * @param bo the byteOrder
     * @throws IOException if an I/O error occurs, eg. this {@code EnhancedNIORandomAccessFile} is closed
     */
    public synchronized void setByteOrder(final ByteOrder bo) throws IOException {
        checkClosed();
        if (bo == null) {
            throw new IllegalArgumentException("ByteOrder can't be null");
        }
        this.bigEndian = (bo == ByteOrder.BIG_ENDIAN);
    }

    /**
     * Returns the current endian mode.
     *
     * @return the current endian mode.
     * @throws IOException if an I/O error occurs, eg. this {@code EnhancedNIORandomAccessFile} is closed
     */
    public synchronized ByteOrder getByteOrder() throws IOException {
        checkClosed();
        return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    /**
     * Writes pending buffered data to the file.
     * Unlike {@link #flushBefore(long)}, this method does not advance the
     * flushed position and does not restrict seeking to earlier positions.
     *
     * @throws IOException if an I/O error occurrs.
     */
    public synchronized void flush() throws IOException {
        checkClosed();
        if (state.bufferModified && cached) {
            final int wLen = state.write_(state.bufferStart, state.buffer, 0, state.dataSize);
            updateCache(state.bufferStart, state.buffer, 0, wLen);
            state.dataSize -= wLen;
            state.bufferStart += wLen;
            dataEnd = state.bufferStart + state.dataSize;
            state.bufferModified = state.dataSize != 0;
            endOfFile = wLen <= 0;
        }
    }

    /**
     * Discards the initial portion of the stream prior to the
     * indicated position.  Attempting to seek to an offset within the
     * flushed portion of the stream will result in an
     * {@code IndexOutOfBoundsException}.
     *
     * @param pos a {@code long} containing the length of the
     *            stream prefix that may be flushed.
     * @throws IndexOutOfBoundsException if {@code pos} lies
     *                                   in the flushed portion of the stream or past the current stream
     *                                   position.
     * @throws IOException               if an I/O error occurrs.
     */
    public synchronized void flushBefore(long pos) throws IOException {
        checkClosed();
        if (pos < getFlushedPosition()) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        if (pos > getFilePointer()) {
            throw new IndexOutOfBoundsException("pos > filePosition!");
        }
        final long len = pos - getFlushedPosition();
        if (state.bufferModified && cached) {
            final int wLen = state.write_(state.bufferStart, state.buffer, 0, (int) Math.min(len, state.dataSize));
            updateCache(state.bufferStart, state.buffer, 0, wLen);
            if (wLen < state.dataSize) {
                //shift the remaining buffered bytes to index 0
                System.arraycopy(state.buffer, wLen, state.buffer, 0, state.dataSize - wLen);
            }
            state.dataSize -= wLen;
            state.flushedPos = pos;
            state.bufferStart += wLen;
            dataEnd = state.bufferStart + state.dataSize;
            state.bufferModified = state.dataSize != 0;
            endOfFile = wLen <= 0;
        } else {
            state.flushedPos += len;
        }
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
    public synchronized long getFlushedPosition() {
        return state.flushedPos;
    }

    /**
     * Make sure eraf is at least this long when its closed. needed when not
     * using fill mode, and not all data is written.
     *
     * @param minLength the minimum size of the physical destination file of the eraf
     * @throws IOException if an I/O error occurs, eg. this {@code EnhancedNIORandomAccessFile} is closed
     */
    public synchronized void setMinLength(long minLength) throws IOException {
        checkClosed();
        if (minLength < 0) {
            throw new IndexOutOfBoundsException("Negative length not allowed. The limit pointer 'minLength' must be positive.");
        }
        //If Read-only, reject setting the minLength by throwing NonWritableChannelException
        if (state.readonly) {
            throw new NonWritableChannelException();
        }
        state.minLength = minLength;
        state.cachedRanges.clear();
    }

    //
    // Read primitives.
    //

    /**
     * Read a byte of data from the eraf, blocking until data is available.
     *
     * @return the next byte of data, or -1 if the end of the eraf is reached.
     * @throws IOException if an I/O error occurrs.
     */
    public synchronized int read() throws IOException {
        checkClosed();
        // Check for read permission of eraf.
        if (state.writeOnly) {
            throw new NonReadableChannelException();
        }
        // Check for end of eraf.
        if (endOfFile) {
            return -1;
        }
        // If not cached, no buffer is used, then, read directly
        if (!cached) {
            int wAttempts = 0;
            while (true) {
                final byte[] b = new byte[1];
                final int len = read_(filePosition, b, 0, 1);
                if (len == 0) {
                    wAttempts = wAttempts + 1;
                    if (wAttempts >= W_ATTEMPTS_LIMIT) {
                        throw new IOException("fail to read after " + (filePosition) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
                    }
                    continue;
                }
                endOfFile = (len == -1);
                filePosition += endOfFile ? 0 : len;
                return endOfFile ? -1 : (b[0] & 0xFF);
            }
        }

        // If the eraf position is within the data, return the byte...
        if (filePosition < dataEnd && filePosition >= state.bufferStart) {
            final int pos = (int) (filePosition - state.bufferStart);
            filePosition++;
            return (state.buffer[pos] & 0xff);

            // ...or seek to fill the buffer, and try again.
        } else {
            seek(filePosition);
            return read();
        }
    }

    /**
     * Read up to <code>len</code> bytes into an array, at a specified offset.
     * This will block until at least one byte has been read.
     *
     * @param b   the byte array to receive the bytes.
     * @param off the offset in the array where copying will start.
     * @param len the number of bytes to copy.
     * @return the actual number of bytes read, or -1 if there is not more data
     * due to the end of the eraf being reached.
     * @throws IOException if an I/O error occurrs.
     */
    public synchronized int readBytes(byte[] b, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(b, "byte buffer");
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > b.length - len!");
        }

        if (len == 0) {
            return 0;
        }
        // Check for read permission of eraf.
        if (state.writeOnly) {
            throw new NonReadableChannelException();
        }
        // Check for end of eraf.
        if (endOfFile) {
            return -1;
        }

        // If not cached, no buffer is used, then, read directly
        if (!cached) {
            int wAttempts = 0;
            while (true) {
                final int rLen = read_(filePosition, b, off, len);
                if (rLen == 0) {
                    wAttempts = wAttempts + 1;
                    if (wAttempts >= W_ATTEMPTS_LIMIT) {
                        throw new IOException("fail to read after " + (filePosition) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
                    }
                    continue;
                }
                endOfFile = (rLen == -1);
                filePosition += endOfFile ? 0 : rLen;
                return rLen;
            }
        }
        int rLen = 0;
        while (!endOfFile && len > 0) {
            // See how many bytes are available in the buffer - if none,
            // seek to the eraf position to update the buffer and try again.
            final int bytesAvailable = (int) (dataEnd - filePosition);
            if (bytesAvailable < 1 || filePosition < state.bufferStart) {
                seek(filePosition);
                continue;
            }

            // Copy as much as we can.
            final int copyLength = Math.min(bytesAvailable, len);
            System.arraycopy(state.buffer, (int) (filePosition - state.bufferStart), b, off, copyLength);
            filePosition += copyLength;
            rLen += copyLength;
            off += copyLength;
            len -= copyLength;
        }
        return rLen;
    }

    /**
     * read directly, without going through the buffer
     *
     * @param pos    the read start position
     * @param b      the destination buffer
     * @param offset the buffer offset
     * @param len    the requested read length
     * @return the actually read length
     * @throws IOException if an I/O error occurrs.
     */
    private int read_(long pos, byte[] b, int offset, int len)
            throws IOException {
        if (state.writeOnly) {
            throw new NonReadableChannelException();
        }
        if (state.cacheChannel != null) {
            return readFromCache(pos, b, offset, len);
        }
        return readFromSource(pos, b, offset, len);
    }

    private int readFromSource(long pos, byte[] b, int offset, int len) throws IOException {
        state.fileChannel.position(pos);
        final ByteBuffer buffer = ByteBuffer.wrap(b, offset, len);
        return Math.toIntExact(state.fileChannel.read(buffer));
    }

    private int readFromCache(long pos, byte[] b, int offset, int len) throws IOException {
        int total = 0;
        while (total < len) {
            final long current = checkedEnd(pos, total);
            final Map.Entry<Long, Long> cached = state.cachedRanges.floorEntry(current);
            final long cachedEnd = cached != null ? cached.getValue() : -1L;
            if (cachedEnd > current) {
                final int count = (int) Math.min(len - total, cachedEnd - current);
                state.cacheChannel.position(current);
                final int read = readFullyOrEof(state.cacheChannel, b, offset + total, count);
                if (read <= 0) {
                    invalidateCachedRange(current, count);
                    continue;
                }
                total += read;
            } else {
                final Map.Entry<Long, Long> next = state.cachedRanges.higherEntry(current);
                final long nextStart = next != null ? next.getKey() : Long.MAX_VALUE;
                final int count = (int) Math.clamp(nextStart - current, 1L, len - total);
                final int read = readFromSource(current, b, offset + total, count);
                if (read <= 0) {
                    return total == 0 ? read : total;
                }
                state.cacheChannel.position(current);
                writeFully(state.cacheChannel, b, offset + total, read);
                addCachedRange(current, checkedEnd(current, read));
                total += read;
                if (read < count) {
                    return total;
                }
            }
        }
        return total;
    }

    private static int readFullyOrEof(FileChannel channel, byte[] b, int offset, int len) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(b, offset, len);
        int total = 0;
        while (buffer.hasRemaining()) {
            final int count = channel.read(buffer);
            if (count < 0) {
                return total == 0 ? -1 : total;
            }
            if (count == 0) {
                break;
            }
            total += count;
        }
        return total;
    }

    private static void writeFully(FileChannel channel, byte[] b, int offset, int len) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(b, offset, len);
        while (buffer.hasRemaining()) {
            if (channel.write(buffer) == 0) {
                throw new IOException("Unable to write cache file");
            }
        }
    }

    /**
     * Read up to <code>len</code> bytes into an array, at a specified offset.
     * This will block until at least one byte has been read.
     *
     * @param b   the byte array to receive the bytes.
     * @param off the offset in the array where copying will start.
     * @param len the number of bytes to copy.
     * @return the actual number of bytes read, or -1 if there is not more data
     * due to the end of the eraf being reached.
     * @throws IOException if an I/O error occurrs.
     */
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        return readBytes(b, off, len);
    }

    /**
     * Read up to <code>b.length( )</code> bytes into an array. This will
     * block until at least one byte has been read.
     *
     * @param b the byte array to receive the bytes.
     * @return the actual number of bytes read, or -1 if there is not more data
     * due to the end of the eraf being reached.
     * @throws IOException if an I/O error occurrs.
     */
    public synchronized int read(byte[] b) throws IOException {
        checkClosed();
        checkArrayNotNull(b, "byte buffer");
        return readBytes(b, 0, b.length);
    }

    /**
     * Reads <code>b.length</code> bytes from this eraf into the byte array.
     * This method reads repeatedly from the eraf until all the bytes are read.
     * This method blocks until all the bytes are read, the end of the stream is
     * detected, or an exception is thrown.
     *
     * @param b the buffer into which the data is read.
     * @throws EOFException if this eraf reaches the end before reading all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(byte [] b) throws IOException {
        checkClosed();
        checkArrayNotNull(b, "byte buffer");
        readFully(b, 0, b.length);
    }

    /**
     * Reads exactly <code>len</code> bytes from this eraf into the byte
     * array. This method reads repeatedly from the eraf until all the bytes are
     * read. This method blocks until all the bytes are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws EOFException if this eraf reaches the end before reading all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized void readFully(byte [] b, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(b, "byte buffer");
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > b.length - len!");
        }
        int n = 0;
        int wAttempts = 0;
        while (n < len) {
            final int count = this.readBytes(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            wAttempts = count < 1 ? wAttempts + 1 : 0;
            if (wAttempts >= W_ATTEMPTS_LIMIT) {
                throw new IOException("fail to read after " + (off + n) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
            }
            n += count;
        }
    }

    /**
     * Reads exactly <code>len</code> shorts from this eraf into the short
     * array. This method reads repeatedly from the eraf until all the shorts are
     * read. This method blocks until all the shorts are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param s   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws EOFException if this eraf reaches the end before reading all the shorts.
     * @throws IOException  if an I/O error occurs.
     */
    public synchronized void readFully(short[] s, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(s, "short buffer");
        if (off < 0 || len < 0 || off > s.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > s.length - len!");
        }
        int n = 0;
        int wAttempts = 0;
        while (n < len) {
            final int count = this.readShorts(s, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            wAttempts = count < 1 ? wAttempts + 1 : 0;
            if (wAttempts >= W_ATTEMPTS_LIMIT) {
                throw new IOException("fail to read after " + (off + n) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
            }
            n += count;
        }
    }

    /**
     * Reads exactly <code>len</code> chars from this eraf into the char
     * array. This method reads repeatedly from the eraf until all the chars are
     * read. This method blocks until all the chars are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param c   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws EOFException if this eraf reaches the end before reading all the chars.
     * @throws IOException  if an I/O error occurs.
     */
    public synchronized void readFully(char[] c, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(c, "char buffer");
        if (off < 0 || len < 0 || off > c.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > c.length - len!");
        }
        int n = 0;
        int wAttempts = 0;
        while (n < len) {
            final int count = this.readChars(c, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            wAttempts = count < 1 ? wAttempts + 1 : 0;
            if (wAttempts >= W_ATTEMPTS_LIMIT) {
                throw new IOException("fail to read after " + (off + n) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
            }
            n += count;
        }
    }

    /**
     * Reads exactly <code>len</code> ints from this eraf into the int
     * array. This method reads repeatedly from the eraf until all the ints are
     * read. This method blocks until all the ints are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param i   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws EOFException if this eraf reaches the end before reading all the ints.
     * @throws IOException  if an I/O error occurs.
     */
    public synchronized void readFully(int[] i, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(i, "int buffer");
        if (off < 0 || len < 0 || off > i.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > i.length - len!");
        }
        int n = 0;
        int wAttempts = 0;
        while (n < len) {
            final int count = this.readInts(i, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            wAttempts = count < 1 ? wAttempts + 1 : 0;
            if (wAttempts >= W_ATTEMPTS_LIMIT) {
                throw new IOException("fail to read after " + (off + n) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
            }
            n += count;
        }
    }

    /**
     * Reads exactly <code>len</code> longs from this eraf into the long
     * array. This method reads repeatedly from the eraf until all the longs are
     * read. This method blocks until all the longs are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param l   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws EOFException if this eraf reaches the end before reading all the longs.
     * @throws IOException  if an I/O error occurs.
     */
    public synchronized void readFully(long[] l, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(l, "long buffer");
        if (off < 0 || len < 0 || off > l.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > l.length - len!");
        }
        int n = 0;
        int wAttempts = 0;
        while (n < len) {
            final int count = this.readLongs(l, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            wAttempts = count < 1 ? wAttempts + 1 : 0;
            if (wAttempts >= W_ATTEMPTS_LIMIT) {
                throw new IOException("fail to read after " + (off + n) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
            }
            n += count;
        }
    }

    /**
     * Reads exactly <code>len</code> floats from this eraf into the float
     * array. This method reads repeatedly from the eraf until all the floats are
     * read. This method blocks until all the floats are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param f   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws EOFException if this eraf reaches the end before reading all the floats.
     * @throws IOException  if an I/O error occurs.
     */
    public synchronized void readFully(float[] f, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(f, "float buffer");
        if (off < 0 || len < 0 || off > f.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > f.length - len!");
        }
        int n = 0;
        int wAttempts = 0;
        while (n < len) {
            final int count = this.readFloats(f, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            wAttempts = count < 1 ? wAttempts + 1 : 0;
            if (wAttempts >= W_ATTEMPTS_LIMIT) {
                throw new IOException("fail to read after " + (off + n) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
            }
            n += count;
        }
    }

    /**
     * Reads exactly <code>len</code> doubles from this eraf into the double
     * array. This method reads repeatedly from the eraf until all the doubles are
     * read. This method blocks until all the doubles are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param d   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws EOFException if this eraf reaches the end before reading all the doubles.
     * @throws IOException  if an I/O error occurs.
     */
    public synchronized void readFully(double[] d, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(d, "double buffer");
        if (off < 0 || len < 0 || off > d.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > d.length - len!");
        }
        int n = 0;
        int wAttempts = 0;
        while (n < len) {
            final int count = this.readDoubles(d, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            wAttempts = count < 1 ? wAttempts + 1 : 0;
            if (wAttempts >= W_ATTEMPTS_LIMIT) {
                throw new IOException("fail to read after " + (off + n) + " position. " + W_ATTEMPTS_LIMIT + " read attempts.");
            }
            n += count;
        }
    }

    /**
     * Moves the file pointer relative to its current position by up to
     * {@code n} bytes. Positive values move forward and negative values
     * move backward. The movement is clamped at the beginning and end of
     * the file, so the returned value may be smaller than the requested
     * distance and has the same sign as the requested distance.
     * This method never throws an {@code EOFException}.
     * The actual
     * number of bytes skipped is returned.
     *
     * @param n the number of bytes to be skipped.
     * @return the number of bytes actually skipped.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized int skipBytes(int n) throws IOException {
        return Math.toIntExact(skipBytes((long) n));
    }

    /**
     * Makes an attempt to skip over
     * {@code n} bytes
     * of data from the input
     * stream, discarding the skipped bytes. However,
     * it may skip
     * over some smaller number of
     * bytes, possibly zero. This may result from
     * any of a
     * number of conditions; reaching
     * end of file before {@code n} bytes
     * have been skipped is
     * only one possibility.
     * This method never throws an {@code EOFException}.
     * The actual
     * number of bytes skipped is returned.
     *
     * @param n the number of bytes to be skipped.
     * @return the number of bytes actually skipped.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized long skipBytes(long n) throws IOException {
        checkClosed();
        final long current = getFilePointer();
        if (n < 0) {
            final long target = n < -current ? 0 : current + n;
            final long skipped = target - current;
            seek(target);
            return skipped;
        }
        final long size = length();
        if (current >= size) {
            return 0;
        }
        final long skipped = Math.min(n, size - current);
        seek(current + skipped);
        return skipped;
    }

    /**
     * Unread the last byte read.
     *
     * @throws IOException if an I/O error occurs, eg. this {@code EnhancedNIORandomAccessFile} is closed
     */
    public synchronized void unread() throws IOException {
        checkClosed();
        if (getFilePointer() > 0 && getFilePointer() > getFlushedPosition()) {
            filePosition--;
            endOfFile = false;
        }
    }

    //
    // DataInput methods.
    //

    /**
     * Reads a <code>boolean</code> from this eraf. This method reads a single
     * byte from the eraf. A value of <code>0</code> represents
     * <code>false</code>. Any other value represents <code>true</code>.
     * This method blocks until the byte is read, the end of the stream is
     * detected, or an exception is thrown.
     *
     * @return the <code>boolean</code> value read.
     * @throws EOFException if this eraf has reached the end.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized boolean readBoolean() throws IOException {
        final int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (ch != 0);
    }

    /**
     * Reads a signed 8-bit value from this eraf. This method reads a byte from
     * the eraf. If the byte read is <code>b</code>, where
     * <code>0&nbsp;&lt;=&nbsp;b&nbsp;&lt;=&nbsp;255</code>, then the result
     * is:
     * <ul>
     * <code>
     * (byte)(b)
     * </code>
     * </ul>
     * <p/>
     * This method blocks until the byte is read, the end of the stream is
     * detected, or an exception is thrown.
     *
     * @return the next byte of this eraf as a signed 8-bit <code>byte</code>.
     * @throws EOFException if this eraf has reached the end.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized byte readByte() throws IOException {
        final int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) (ch);
    }

    /**
     * Reads an unsigned 8-bit number from this eraf. This method reads a byte
     * from this eraf and returns that byte.
     * <p/>
     * This method blocks until the byte is read, the end of the stream is
     * detected, or an exception is thrown.
     *
     * @return the next byte of this eraf, interpreted as an unsigned 8-bit
     * number.
     * @throws EOFException if this eraf has reached the end.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized int readUnsignedByte() throws IOException {
        final int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    /**
     * Reads a signed 16-bit number from this eraf. The method reads 2 bytes
     * from this eraf. If the two bytes read, in order, are <code>b1</code>
     * and <code>b2</code>, where each of the two values is between
     * <code>0</code> and <code>255</code>, inclusive, then the result is
     * equal to:
     * <ul>
     * <code>
     * (short)((b1 &lt;&lt; 8) | b2)
     * </code>
     * </ul>
     * <p/>
     * This method blocks until the two bytes are read, the end of the stream is
     * detected, or an exception is thrown.
     *
     * @return the next two bytes of this eraf, interpreted as a signed 16-bit
     * number.
     * @throws EOFException if this eraf reaches the end before reading two bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized short readShort() throws IOException {
        final byte[] b = new byte[Short.BYTES];
        readFully(b);
        return toShort(b[0], b[1]);
    }

    /**
     * Reads up to <code>len</code> shorts from this eraf into the short
     * array.
     * This method blocks until all the shorts are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param s   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized int readShorts(short[] s, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(s, "short buffer");
        if (off < 0 || len < 0 || off > s.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > s.length - len!");
        }
        int count = 0;
        if ((long) len * Short.BYTES > Integer.MAX_VALUE) {
            final int chunkSize = len / Short.BYTES;
            for (int chunk = 0; chunk < Short.BYTES; chunk++) {
                final int rLen = readShorts(s, off + (chunkSize * chunk), chunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            final int lastChunkSize = len % Short.BYTES;
            if (lastChunkSize > 0) {
                final int rLen = readShorts(s, off + (chunkSize * Short.BYTES), lastChunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            return count;
        }
        final byte[] byteBuf = new byte[len * Short.BYTES];
        int rLen = readBytes(byteBuf, 0, len * Short.BYTES);
        if (rLen < 0) {
            return rLen;
        }
        final int remaining = rLen % Short.BYTES;
        if (remaining != 0) {
            filePosition -= remaining;
            return -1;
        }
        rLen /= Short.BYTES;
        toShorts(byteBuf, s, off, rLen);
        count += rLen;

        return count;
    }

    private void toShorts(byte[] b, short[] s, int off, int len) {
        int boff = 0;
        for (int j = 0; j < len; j++) {
            s[off + j] = toShort(b[boff], b[boff + 1]);
            boff += Short.BYTES;
        }
    }

    private short toShort(byte b0, byte b1) {
        if (bigEndian) {
            return (short) (((b0 & 0xFF) << 8) + (b1 & 0xFF));
        } else {
            return (short) (((b1 & 0xFF) << 8) + (b0 & 0xFF));
        }
    }

    /**
     * Reads an unsigned 16-bit number from this eraf. This method reads two
     * bytes from the eraf. If the bytes read, in order, are <code>b1</code>
     * and <code>b2</code>, where
     * <code>0&nbsp;&lt;=&nbsp;b1, b2&nbsp;&lt;=&nbsp;255</code>, then the
     * result is equal to:
     * <ul>
     * <code>
     * (b1 &lt;&lt; 8) | b2
     * </code>
     * </ul>
     * <p/>
     * This method blocks until the two bytes are read, the end of the stream is
     * detected, or an exception is thrown.
     *
     * @return the next two bytes of this eraf, interpreted as an unsigned
     * 16-bit integer.
     * @throws EOFException if this eraf reaches the end before reading two bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized int readUnsignedShort() throws IOException {
        final byte[] b = new byte[2];
        if (read(b, 0, 2) < 2) {
            throw new EOFException();
        }
        if (bigEndian) {
            return ((b[0] & 0xFF) << 8) + (b[1] & 0xFF) & 0xFFFF;
        } else {
            return ((b[1] & 0xFF) << 8) + (b[0] & 0xFF) & 0xFFFF;
        }
    }

    /**
     * Reads a Unicode character from this eraf. This method reads two bytes
     * from the eraf. If the bytes read, in order, are <code>b1</code> and
     * <code>b2</code>, where
     * <code>0&nbsp;&lt;=&nbsp;b1,&nbsp;b2&nbsp;&lt;=&nbsp;255</code>, then
     * the result is equal to:
     * <ul>
     * <code>
     * (char)((b1 &lt;&lt; 8) | b2)
     * </code>
     * </ul>
     * <p/>
     * This method blocks until the two bytes are read, the end of the stream is
     * detected, or an exception is thrown.
     *
     * @return the next two bytes of this eraf as a Unicode character.
     * @throws EOFException if this eraf reaches the end before reading two bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized char readChar() throws IOException {
        final byte[] b = new byte[Character.BYTES];
        readFully(b);
        return toChar(b[0], b[1]);
    }

    /**
     * Reads up to <code>len</code> chars from this eraf into the char
     * array.
     * This method blocks until all the chars are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param c   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of chars to read.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized int readChars(char[] c, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(c, "char buffer");
        if (off < 0 || len < 0 || off > c.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > c.length - len!");
        }
        int count = 0;
        if ((long) len * Character.BYTES > Integer.MAX_VALUE) {
            final int chunkSize = len / Character.BYTES;
            for (int chunk = 0; chunk < Character.BYTES; chunk++) {
                final int rLen = readChars(c, off + (chunkSize * chunk), chunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            final int lastChunkSize = len % Character.BYTES;
            if (lastChunkSize > 0) {
                final int rLen = readChars(c, off + (chunkSize * Character.BYTES), lastChunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            return count;
        }
        final byte[] byteBuf = new byte[len * Character.BYTES];
        int rLen = readBytes(byteBuf, 0, len * Character.BYTES);
        if (rLen < 0) {
            return rLen;
        }
        final int remaining = rLen % Character.BYTES;
        if (remaining != 0) {
            filePosition -= remaining;
            return -1;
        }
        rLen /= Character.BYTES;
        toChars(byteBuf, c, off, rLen);
        count += rLen;
        return count;
    }

    private void toChars(byte[] b, char[] c, int off, int len) {
        int boff = 0;
        for (int j = 0; j < len; j++) {
            c[off + j] = toChar(b[boff], b[boff + 1]);
            boff += Character.BYTES;
        }
    }

    private char toChar(byte b0, byte b1) {
        if (bigEndian) {
            return (char) (((b0 & 0xFF) << 8) + (b1 & 0xFF));
        } else {
            return (char) (((b1 & 0xFF) << 8) + (b0 & 0xFF));
        }
    }

    /**
     * Reads a signed 32-bit integer from this eraf. This method reads 4 bytes
     * from the eraf. If the bytes read, in order, are <code>b1</code>,
     * <code>b2</code>, <code>b3</code>, and <code>b4</code>, where
     * <code>0&nbsp;&lt;=&nbsp;b1, b2, b3, b4&nbsp;&lt;=&nbsp;255</code>,
     * then the result is equal to:
     * <ul>
     * <code>
     * (b1 &lt;&lt; 24) | (b2 &lt;&lt; 16) + (b3 &lt;&lt; 8) + b4
     * </code>
     * </ul>
     * <p/>
     * This method blocks until the four bytes are read, the end of the stream
     * is detected, or an exception is thrown.
     *
     * @return the next four bytes of this eraf, interpreted as an
     * <code>int</code>.
     * @throws EOFException if this eraf reaches the end before reading four bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized int readInt() throws IOException {
        final byte[] b = new byte[Integer.BYTES];
        readFully(b);
        return toInt(b[0], b[1], b[2], b[3]);
    }

    /**
     * Reads up to <code>len</code> ints from this eraf into the int
     * array.
     * This method blocks until all the ints are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param i   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of ints to read.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized int readInts(int[] i, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(i, "int buffer");
        if (off < 0 || len < 0 || off > i.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > i.length - len!");
        }
        int count = 0;
        if ((long) len * Integer.BYTES > Integer.MAX_VALUE) {
            final int chunkSize = len / Integer.BYTES;
            for (int chunk = 0; chunk < Integer.BYTES; chunk++) {
                final int rLen = readInts(i, off + (chunkSize * chunk), chunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            final int lastChunkSize = len % Integer.BYTES;
            if (lastChunkSize > 0) {
                final int rLen = readInts(i, off + (chunkSize * Integer.BYTES), lastChunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            return count;
        }
        final byte[] byteBuf = new byte[len * Integer.BYTES];
        int rLen = readBytes(byteBuf, 0, len * Integer.BYTES);
        if (rLen < 0) {
            return rLen;
        }
        final int remaining = rLen % Integer.BYTES;
        if (remaining != 0) {
            filePosition -= remaining;
            return -1;
        }
        rLen /= Integer.BYTES;
        toInts(byteBuf, i, off, rLen);
        count += rLen;
        return count;
    }

    private void toInts(byte[] b, int[] i, int off, int len) {
        int boff = 0;
        for (int j = 0; j < len; j++) {
            i[off + j] = toInt(b[boff], b[boff + 1], b[boff + 2], b[boff + 3]);
            boff += Integer.BYTES;
        }
    }

    private int toInt(byte b0, byte b1, byte b2, byte b3) {
        if (bigEndian) {
            return (((b0 & 0xFF) << 24) + ((b1 & 0xFF) << 16))
                    + (((b2 & 0xFF) << 8) + (b3 & 0xFF));
        } else {
            return (((b3 & 0xFF) << 24) + ((b2 & 0xFF) << 16))
                    + (((b1 & 0xFF) << 8) + (b0 & 0xFF));
        }
    }

    /**
     * Reads an unsigned 32-bit integer from the stream using the current byte
     * order.
     *
     * @return the unsigned 32-bit value as a {@code long}, in the range
     *         {@code 0} to {@code 0xFFFFFFFFL}
     * @throws EOFException if the stream reaches the end before four bytes can
     *                      be read
     * @throws IOException if an I/O error occurs
     */
    public synchronized long readUnsignedInt() throws IOException {
        // retaining only the first 4 bytes, ignoring sign when extending
        return ((long) readInt()) & 0xFFFFFFFFL;
    }

    /**
     * Read an integer at the given position, bypassing all buffering.
     *
     * @param pos read a byte at this position
     * @return The int that was read
     * @throws IOException if an I/O error occurs.
     */
    public synchronized int readIntUnbuffered(long pos) throws IOException {
        checkClosed();
        if (pos < 0) {
            throw new IndexOutOfBoundsException("Negative positioning not allowed. Positioning pointer 'pos' must be positive.");
        }
        if (pos < getFlushedPosition()) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        // Direct reads must observe dirty cached data before bypassing the buffer.
        flush();
        final byte[] bb = new byte[Integer.BYTES];
        if (read_(pos, bb, 0, Integer.BYTES) < Integer.BYTES) {
            throw new EOFException();
        }
        final int ch1 = bb[0] & 0xFF;
        final int ch2 = bb[1] & 0xFF;
        final int ch3 = bb[2] & 0xFF;
        final int ch4 = bb[3] & 0xFF;

        if (bigEndian) {
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
        } else {
            return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
        }
    }

    /**
     * Reads a signed 64-bit integer from this eraf. This method reads eight
     * bytes from the eraf. If the bytes read, in order, are <code>b1</code>,
     * <code>b2</code>, <code>b3</code>, <code>b4</code>,
     * <code>b5</code>, <code>b6</code>, <code>b7</code>, and
     * <code>b8,</code> where:
     * <ul>
     * <code>
     * 0 &lt;= b1, b2, b3, b4, b5, b6, b7, b8 &lt;=255,
     * </code>
     * </ul>
     * <p/>
     * then the result is equal to:
     * <p/>
     * <blockquote>
     * <p/>
     * <pre>
     * ((long) b1 &lt;&lt; 56) + ((long) b2 &lt;&lt; 48) + ((long) b3 &lt;&lt; 40) + ((long) b4 &lt;&lt; 32)
     * 		+ ((long) b5 &lt;&lt; 24) + ((long) b6 &lt;&lt; 16) + ((long) b7 &lt;&lt; 8) + b8
     * </pre>
     * <p/>
     * </blockquote>
     * <p/>
     * This method blocks until the eight bytes are read, the end of the stream
     * is detected, or an exception is thrown.
     *
     * @return the next eight bytes of this eraf, interpreted as a
     * <code>long</code>.
     * @throws EOFException if this eraf reaches the end before reading eight bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public synchronized long readLong() throws IOException {
        final byte[] b = new byte[Long.BYTES];
        readFully(b);
        return toLong(b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7]);
    }

    /**
     * Reads up to <code>len</code> longs from this eraf into the long
     * array.
     * This method blocks until all the longs are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param l   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of longs to read.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized int readLongs(long[] l, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(l, "long buffer");
        if (off < 0 || len < 0 || off > l.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > l.length - len!");
        }
        int count = 0;
        if ((long) len * Long.BYTES > Integer.MAX_VALUE) {
            final int chunkSize = len / Long.BYTES;
            for (int chunk = 0; chunk < Long.BYTES; chunk++) {
                final int rLen = readLongs(l, off + (chunkSize * chunk), chunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            final int lastChunkSize = len % Long.BYTES;
            if (lastChunkSize > 0) {
                final int rLen = readLongs(l, off + (chunkSize * Long.BYTES), lastChunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            return count;
        }
        final byte[] byteBuf = new byte[len * Long.BYTES];
        int rLen = readBytes(byteBuf, 0, len * Long.BYTES);
        if (rLen < 0) {
            return rLen;
        }
        final int remaining = rLen % Long.BYTES;
        if (remaining != 0) {
            filePosition -= remaining;
            return -1;
        }
        rLen /= Long.BYTES;
        toLongs(byteBuf, l, off, rLen);
        count += rLen;
        return count;
    }

    private void toLongs(byte[] b, long[] l, int off, int len) {
        int boff = 0;
        for (int j = 0; j < len; j++) {
            l[off + j] = toLong(b[boff], b[boff + 1], b[boff + 2], b[boff + 3], b[boff + 4], b[boff + 5], b[boff + 6], b[boff + 7]);
            boff += Long.BYTES;
        }
    }

    private long toLong(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
        if (bigEndian) {
            return (((b0 & 0xFFL) << 56) + ((b1 & 0xFFL) << 48)
                    + ((b2 & 0xFFL) << 40) + ((b3 & 0xFFL) << 32)
                    + ((b4 & 0xFFL) << 24) + ((b5 & 0xFFL) << 16)
                    + ((b6 & 0xFFL) << 8) + ((b7 & 0xFFL)));
        } else {
            return (((b7 & 0xFFL) << 56) + ((b6 & 0xFFL) << 48)
                    + ((b5 & 0xFFL) << 40) + ((b4 & 0xffL) << 32)
                    + ((b3 & 0xFFL) << 24) + ((b2 & 0xFFL) << 16)
                    + ((b1 & 0xFFL) << 8) + ((b0 & 0xFFL)));
        }
    }

    /**
     * Reads a <code>float</code> from this eraf. This method reads an
     * <code>int</code> value as if by the <code>readInt</code> method and
     * then converts that <code>int</code> to a <code>float</code> using the
     * <code>intBitsToFloat</code> method in class <code>Float</code>.
     * <p/>
     * This method blocks until the four bytes are read, the end of the stream
     * is detected, or an exception is thrown.
     *
     * @return the next four bytes of this eraf, interpreted as a
     * <code>float</code>.
     * @throws EOFException if this eraf reaches the end before reading four bytes.
     * @throws IOException  if an I/O error occurs.
     * @see java.io.RandomAccessFile#readInt()
     * @see java.lang.Float#intBitsToFloat(int)
     */
    @Override
    public synchronized float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads up to <code>len</code> floats from this eraf into the float
     * array.
     * This method blocks until all the floats are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param f   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of floats to read.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized int readFloats(float[] f, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(f, "float buffer");
        if (off < 0 || len < 0 || off > f.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > f.length - len!");
        }
        int count = 0;
        if ((long) len * Float.BYTES > Integer.MAX_VALUE) {
            final int chunkSize = len / Float.BYTES;
            for (int chunk = 0; chunk < Float.BYTES; chunk++) {
                final int rLen = readFloats(f, off + (chunkSize * chunk), chunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            final int lastChunkSize = len % Float.BYTES;
            if (lastChunkSize > 0) {
                final int rLen = readFloats(f, off + (chunkSize * Float.BYTES), lastChunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            return count;
        }
        final byte[] byteBuf = new byte[len * Float.BYTES];
        int rLen = readBytes(byteBuf, 0, len * Float.BYTES);
        if (rLen < 0) {
            return rLen;
        }
        final int remaining = rLen % Float.BYTES;
        if (remaining != 0) {
            filePosition -= remaining;
            return -1;
        }
        rLen /= Float.BYTES;
        toFloats(byteBuf, f, off, rLen);
        count += rLen;
        return count;
    }

    private void toFloats(byte[] b, float[] f, int off, int len) {
        int boff = 0;
        for (int j = 0; j < len; j++) {
            f[off + j] = Float.intBitsToFloat(toInt(b[boff], b[boff + 1], b[boff + 2], b[boff + 3]));
            boff += Float.BYTES;
        }
    }

    /**
     * Reads a <code>double</code> from this eraf. This method reads a
     * <code>long</code> value as if by the <code>readLong</code> method and
     * then converts that <code>long</code> to a <code>double</code> using
     * the <code>longBitsToDouble</code> method in class <code>Double</code>.
     * <p/>
     * This method blocks until the eight bytes are read, the end of the stream
     * is detected, or an exception is thrown.
     *
     * @return the next eight bytes of this eraf, interpreted as a
     * <code>double</code>.
     * @throws EOFException if this eraf reaches the end before reading eight bytes.
     * @throws IOException  if an I/O error occurs.
     * @see java.io.RandomAccessFile#readLong()
     * @see java.lang.Double#longBitsToDouble(long)
     */
    @Override
    public synchronized double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads up to <code>len</code> doubles from this eraf into the double
     * array.
     * This method blocks until all the doubles are read, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @param d   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of doubles to read.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized int readDoubles(double[] d, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(d, "double buffer");
        if (off < 0 || len < 0 || off > d.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > d.length - len!");
        }
        int count = 0;
        if ((long) len * Double.BYTES > Integer.MAX_VALUE) {
            final int chunkSize = len / Double.BYTES;
            for (int chunk = 0; chunk < Double.BYTES; chunk++) {
                final int rLen = readDoubles(d, off + (chunkSize * chunk), chunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            final int lastChunkSize = len % Double.BYTES;
            if (lastChunkSize > 0) {
                final int rLen = readDoubles(d, off + (chunkSize * Double.BYTES), lastChunkSize);
                if (rLen < 0) {
                    return count > 0 ? count : rLen;
                }
                count += rLen;
            }
            return count;
        }
        final byte[] byteBuf = new byte[len * Double.BYTES];
        int rLen = readBytes(byteBuf, 0, len * Double.BYTES);
        if (rLen < 0) {
            return rLen;
        }
        final int remaining = rLen % Double.BYTES;
        if (remaining != 0) {
            filePosition -= remaining;
            return -1;
        }
        rLen /= Double.BYTES;
        toDoubles(byteBuf, d, off, rLen);
        count += rLen;
        return count;
    }

    private void toDoubles(byte[] b, double[] d, int off, int len) {
        int boff = 0;
        for (int j = 0; j < len; j++) {
            d[off + j] = Double.longBitsToDouble(toLong(b[boff], b[boff + 1], b[boff + 2], b[boff + 3], b[boff + 4], b[boff + 5], b[boff + 6], b[boff + 7]));
            boff += Double.BYTES;
        }
    }

    /**
     * Reads the next line of text from this eraf. This method successively
     * reads bytes from the eraf until it reaches the end of a line of text.
     * <p/>
     * <p/>
     * A line of text is terminated by a carriage-return character (<code>'&#92;r'</code>),
     * a newline character (<code>'&#92;n'</code>), a carriage-return
     * character immediately followed by a newline character, or the end of the
     * input stream. The line-terminating character(s), if any, are not included as
     * part of the string returned.
     * <p/>
     * <p/>
     * This method blocks until a newline character is read, a carriage return
     * and the byte following it are read (to see if it is a newline), the end
     * of the stream is detected, or an exception is thrown.
     *
     * @return the next line of text from this eraf.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized String readLine() throws IOException {
        final ByteArrayOutputStream line = new ByteArrayOutputStream(0);
        int c;
        boolean loneCR = false;
        loop:
        while (true) {
            c = read();
            switch (c) {
                case -1:
                case '\n':
                    break loop;
                case '\r':
                    c = read();
                    if (c != -1 && c != '\n') {
                        unread();
                    } else {
                        loneCR = line.size() == 0;
                    }
                    break loop;
                default:
                    line.write(c);
                    break;
            }
        }
        if ((c == -1) && (line.size() == 0)) {
            return loneCR ? "" : null;
        }
        return line.toString(StandardCharsets.ISO_8859_1);
    }

    /**
     * Reads in a string from this eraf. The string has been encoded using a
     * modified UTF-8 format.
     * <p/>
     * The first two bytes are read as if by <code>readUnsignedShort</code>.
     * This value gives the number of following bytes that are in the encoded
     * string, not the length of the resulting string. The following bytes are
     * then interpreted as bytes encoding characters in the UTF-8 format and are
     * converted into characters.
     * <p/>
     * This method blocks until all the bytes are read, the end of the stream is
     * detected, or an exception is thrown.
     *
     * @return a Unicode string.
     * @throws EOFException           if this eraf reaches the end before reading all the bytes.
     * @throws IOException            if an I/O error occurs.
     * @throws UTFDataFormatException if the bytes do not represent valid UTF-8 encoding of a
     *                                Unicode string.
     * @see java.io.RandomAccessFile#readUnsignedShort()
     */
    @Override
    public synchronized String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    /**
     * Read a String of known length.
     *
     * @param nbytes number of bytes to read
     * @return String wrapping the bytes.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized String readString(int nbytes) throws IOException {
        return readString(nbytes, StandardCharsets.ISO_8859_1);
    }

    /**
     * Read a String of known length and character encoding used for decoding.
     *
     * @param nbytes  number of bytes to read
     * @param charset the character encoding used for decoding
     * @return String wrapping the bytes.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized String readString(int nbytes, Charset charset) throws IOException {
        checkClosed();
        if (nbytes < 0) {
            throw new IndexOutOfBoundsException("Negative size of String not allowed. Bytes size count 'nbytes' must be positive.");
        }
        if (charset == null) {
            throw new IllegalArgumentException("NULL charset is not allowed. The charset must be provided.");
        }
        final byte[] data = new byte[nbytes];
        readFully(data);
        return new String(data, charset);
    }

    //
    // DataOutput methods.
    //

    /**
     * Writes a <code>boolean</code> to the eraf as a 1-byte value. The value
     * <code>true</code> is written out as the value <code>(byte)1</code>;
     * the value <code>false</code> is written out as the value
     * <code>(byte)0</code>.
     *
     * @param v a <code>boolean</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    /**
     * Writes a <code>byte</code> to the eraf as a 1-byte value.
     *
     * @param v a <code>byte</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeByte(int v) throws IOException {
        write(v);
    }

    /**
     * Writes a <code>short</code> to the eraf as two bytes, high byte first.
     *
     * @param v a <code>short</code> to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeShort(int v) throws IOException {
        if (bigEndian) {
            write((v >>> 8) & 0xFF);
            write((v) & 0xFF);
        } else {
            write((v) & 0xFF);
            write((v >>> 8) & 0xFF);
        }
    }

    /**
     * Writes a sequence of shorts to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The short {@code s[off]} is written first, then the short
     * {@code s[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * @param s   an array of {@code short}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code short}s to write.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off}
     *                                   is greater than {@code s.length - len}.
     * @throws NullPointerException      if {@code s} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    public synchronized void writeShorts(short[] s, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(s, "short buffer");
        if (off < 0 || len < 0 || off > s.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > s.length - len!");
        }
        for (int i = off, end = off + len; i < end; i++) {
            writeShort(s[i]);
        }
    }

    /**
     * Writes a <code>char</code> to the eraf as a 2-byte value, high byte
     * first.
     *
     * @param v a <code>char</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeChar(int v) throws IOException {
        if (bigEndian) {
            write((v >>> 8) & 0xFF);
            write((v) & 0xFF);
        } else {
            write((v) & 0xFF);
            write((v >>> 8) & 0xFF);
        }
    }

    /**
     * Writes a sequence of chars to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The char {@code c[off]} is written first, then the char
     * {@code c[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * @param c   an array of {@code char}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code char}s to write.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off}
     *                                   is greater than {@code c.length - len}.
     * @throws NullPointerException      if {@code c} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    public synchronized void writeChars(char[] c, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(c, "char buffer");
        if (off < 0 || len < 0 || off > c.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > c.length - len!");
        }
        for (int i = off, end = off + len; i < end; i++) {
            writeChar(c[i]);
        }
    }

    /**
     * Writes an <code>int</code> to the eraf as four bytes, high byte first.
     *
     * @param v an <code>int</code> to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeInt(int v) throws IOException {
        if (bigEndian) {
            write((v >>> 24) & 0xFF);
            write((v >>> 16) & 0xFF);
            write((v >>> 8) & 0xFF);
            write((v) & 0xFF);
        } else {
            write((v) & 0xFF);
            write((v >>> 8) & 0xFF);
            write((v >>> 16) & 0xFF);
            write((v >>> 24) & 0xFF);
        }
    }

    /**
     * Writes a sequence of ints to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The int {@code i[off]} is written first, then the int
     * {@code i[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * @param i   an array of {@code int}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code int}s to write.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off}
     *                                   is greater than {@code i.length - len}.
     * @throws NullPointerException      if {@code i} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    public synchronized void writeInts(int[] i, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(i, "int buffer");
        if (off < 0 || len < 0 || off > i.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > i.length - len!");
        }
        for (int j = off, end = off + len; j < end; j++) {
            writeInt(i[j]);
        }
    }

    /**
     * Writes a <code>long</code> to the eraf as eight bytes, high byte first.
     *
     * @param v a <code>long</code> to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeLong(long v) throws IOException {

        if (bigEndian) {
            write((int) (v >>> 56) & 0xFF);
            write((int) (v >>> 48) & 0xFF);
            write((int) (v >>> 40) & 0xFF);
            write((int) (v >>> 32) & 0xFF);
            write((int) (v >>> 24) & 0xFF);
            write((int) (v >>> 16) & 0xFF);
            write((int) (v >>> 8) & 0xFF);
            write((int) (v) & 0xFF);
        } else {
            write((int) (v) & 0xFF);
            write((int) (v >>> 8) & 0xFF);
            write((int) (v >>> 16) & 0xFF);
            write((int) (v >>> 24) & 0xFF);
            write((int) (v >>> 32) & 0xFF);
            write((int) (v >>> 40) & 0xFF);
            write((int) (v >>> 48) & 0xFF);
            write((int) (v >>> 56) & 0xFF);

        }
    }

    /**
     * Writes a sequence of longs to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The long {@code l[off]} is written first, then the long
     * {@code l[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * @param l   an array of {@code long}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code long}s to write.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off}
     *                                   is greater than {@code l.length - len}.
     * @throws NullPointerException      if {@code l} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    public synchronized void writeLongs(long[] l, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(l, "long buffer");
        if (off < 0 || len < 0 || off > l.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > l.length - len!");
        }
        for (int i = off, end = off + len; i < end; i++) {
            writeLong(l[i]);
        }
    }

    /**
     * Converts the float argument to an <code>int</code> using the
     * <code>floatToIntBits</code> method in class <code>Float</code>, and
     * then writes that <code>int</code> value to the eraf as a 4-byte
     * quantity, high byte first.
     *
     * @param v a <code>float</code> value to be written.
     * @throws IOException if an I/O error occurs.
     * @see java.lang.Float#floatToIntBits(float)
     */
    @Override
    public synchronized void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    /**
     * Writes a sequence of floats to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The float {@code f[off]} is written first, then the float
     * {@code f[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * @param f   an array of {@code float}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code float}s to write.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off}
     *                                   is greater than {@code f.length - len}.
     * @throws NullPointerException      if {@code f} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    public synchronized void writeFloats(float[] f, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(f, "float buffer");
        if (off < 0 || len < 0 || off > f.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > f.length - len!");
        }
        for (int i = off, end = off + len; i < end; i++) {
            writeFloat(f[i]);
        }
    }

    /**
     * Converts the double argument to a <code>long</code> using the
     * <code>doubleToLongBits</code> method in class <code>Double</code>,
     * and then writes that <code>long</code> value to the eraf as an 8-byte
     * quantity, high byte first.
     *
     * @param v a <code>double</code> value to be written.
     * @throws IOException if an I/O error occurs.
     * @see java.lang.Double#doubleToLongBits(double)
     */
    @Override
    public synchronized void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    /**
     * Writes a sequence of doubles to the stream at the current
     * position.  If {@code len} is 0, nothing is written.
     * The double {@code d[off]} is written first, then the double
     * {@code d[off + 1]}, and so on.  The byte order of the
     * stream is used to determine the order in which the individual
     * bytes are written.
     *
     * @param d   an array of {@code doubles}s to be written.
     * @param off the start offset in the data.
     * @param len the number of {@code double}s to write.
     * @throws IndexOutOfBoundsException if {@code off} is
     *                                   negative, {@code len} is negative, or {@code off}
     *                                   is greater than {@code d.length - len}.
     * @throws NullPointerException      if {@code d} is
     *                                   {@code null}.
     * @throws IOException               if an I/O error occurs.
     */
    public synchronized void writeDoubles(double[] d, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(d, "double buffer");
        if (off < 0 || len < 0 || off > d.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > d.length - len!");
        }
        for (int i = off, end = off + len; i < end; i++) {
            writeDouble(d[i]);
        }
    }

    /**
     * Writes the string to the eraf as a sequence of bytes. Each character in
     * the string is written out, in sequence, by discarding its high eight
     * bits.
     *
     * @param s a string of bytes to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeBytes(String s) throws IOException {
        checkClosed();
        final int len = s.length();
        for (int i = 0; i < len; i++) {
            write((byte) s.charAt(i));
        }
    }

    /**
     * Writes the character array to the eraf as a sequence of bytes. Each
     * character in the string is written out, in sequence, by discarding its
     * high eight bits.
     *
     * @param b   a character array of bytes to be written.
     * @param off the index of the first character to write.
     * @param len the number of characters to write.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized void writeBytes(char[] b, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(b, "byte buffer");
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > b.length - len!");
        }
        for (int i = off, end = off + len; i < end; i++) {
            write((byte) b[i]);
        }
    }

    /**
     * Writes a string to the eraf as a sequence of characters. Each character
     * is written to the data output stream as if by the <code>writeChar</code>
     * method.
     *
     * @param s a <code>String</code> value to be written.
     * @throws IOException if an I/O error occurs.
     * @see java.io.RandomAccessFile#writeChar(int)
     */
    @Override
    public synchronized void writeChars(String s) throws IOException {
        checkClosed();
        final int len = s.length();
        for (int i = 0; i < len; i++) {
            final int v = s.charAt(i);
            writeChar(v);
        }
    }

    /**
     * Writes a string to the eraf using UTF-8 encoding in a machine-independent
     * manner.
     * <p/>
     * First, two bytes are written to the eraf as if by the
     * <code>writeShort</code> method giving the number of bytes to follow.
     * This value is the number of bytes actually written out, not the length of
     * the string. Following the length, each character of the string is output,
     * in sequence, using the UTF-8 encoding for each character.
     *
     * @param str a string to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void writeUTF(String str) throws IOException {
        checkClosed();
        final int strlen = str.length();
        int utflen = 0;

        for (int i = 0; i < strlen; i++) {
            final int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }
        if (utflen > 65535) {
            throw new UTFDataFormatException();
        }

        write((utflen >>> 8) & 0xFF);
        write((utflen) & 0xFF);
        for (int i = 0; i < strlen; i++) {
            final int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                write(c);
            } else if (c > 0x07FF) {
                write(0xE0 | ((c >> 12) & 0x0F));
                write(0x80 | ((c >> 6) & 0x3F));
                write(0x80 | ((c) & 0x3F));
            } else {
                write(0xC0 | ((c >> 6) & 0x1F));
                write(0x80 | ((c) & 0x3F));
            }
        }
    }

    //
    // Write primitives.
    //

    /**
     * Writes to the eraf the eight low-order bits of the argument {@code b}.
     * The 24 high-order bits of {@code b} are ignored.
     *
     * @param b the byte to be written.
     * @throws IOException if an I/O error occurrs.
     */
    @Override
    public synchronized void write(int b) throws IOException {
        checkClosed();
        //If Read-only, reject write by throwing NonWritableChannelException
        if (state.readonly) {
            throw new NonWritableChannelException();
        }

        // If not cached, no buffer is used, then, write directly
        if (!cached) {
            final byte[] bytes = new byte[]{(byte) b};
            final int wLen = state.write_(filePosition, bytes, 0, 1);
            updateCache(filePosition, bytes, 0, wLen);
            filePosition += wLen;
            endOfFile = wLen <= 0;
            return;
        }

        // If the eraf position is within the block of data...
        if (filePosition < dataEnd) {
            state.buffer[(int) (filePosition++ - state.bufferStart)] = (byte) b;
            state.bufferModified = true;
            endOfFile = false;

            // ...or (assuming that seek will not allow the eraf pointer
            // to move beyond the end of the eraf) get the correct block of
            // data...
        } else {

            // If there is room in the buffer, expand it...
            if (state.dataSize != state.buffer.length) {
                state.buffer[(int) (filePosition++ - state.bufferStart)] = (byte) b;
                state.bufferModified = true;
                state.dataSize++;
                dataEnd++;
                endOfFile = false;

                // ...or do another seek to get a new buffer, and start again...
            } else {
                seek(filePosition);
                write(b);
            }
        }
    }

    /**
     * Write <code>len</code> bytes from an array to the eraf.
     *
     * @param b   the array containing the data.
     * @param off the offset in the array to the data.
     * @param len the length of the data.
     * @throws IOException if an I/O error occurrs.
     */
    public synchronized void writeBytes(byte[] b, int off, int len) throws IOException {
        checkClosed();
        checkArrayNotNull(b, "byte buffer");
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException
                    ("off < 0 || len < 0 || off > b.length - len!");
        }
        //If Read-only, reject write by throwing NonWritableChannelException
        if (state.readonly) {
            throw new NonWritableChannelException();
        }

        // If not cached, no buffer is used, then, write directly
        if (!cached) {
            final int wLen = state.write_(filePosition, b, off, len);
            updateCache(filePosition, b, off, wLen);
            filePosition += wLen;
            endOfFile = wLen <= 0;
            return;
        }

        // If the amount of data is small (less than a full buffer)...
        if (len < state.buffer.length) {

            // If any of the data fits within the buffer...
            int spaceInBuffer = 0;
            int copyLength = 0;
            if (filePosition >= state.bufferStart) {
                spaceInBuffer = (int) ((state.bufferStart + state.buffer.length) - filePosition);
            }
            if (spaceInBuffer > 0) {

                // Copy as much as possible to the buffer.
                copyLength = Math.min(spaceInBuffer, len);
                System.arraycopy(b, off, state.buffer,
                        (int) (filePosition - state.bufferStart), copyLength);
                state.bufferModified = true;
                final long myDataEnd = filePosition + copyLength;
                dataEnd = Math.max(myDataEnd, dataEnd);
                state.dataSize = (int) (dataEnd - state.bufferStart);
                filePosition += copyLength;
                endOfFile = false;
                // /System.out.println("--copy to buffer "+copyLength+" "+len);
            }

            // If there is any data remaining, move to the new position and copy
            // to
            // the new buffer.
            if (copyLength < len) {
                // System.out.println("--need more "+copyLength+" "+len+" space=
                // "+spaceInBuffer);
                seek(filePosition); // triggers a flush
                System.arraycopy(b, off + copyLength, state.buffer,
                        (int) (filePosition - state.bufferStart), len - copyLength);
                state.bufferModified = true;
                final long myDataEnd = filePosition + (len - copyLength);
                dataEnd = Math.max(myDataEnd, dataEnd);
                state.dataSize = (int) (dataEnd - state.bufferStart);
                filePosition += (len - copyLength);
                endOfFile = false;
            }

            // ...or write a lot of data...
        } else {

            // Flush the current buffer, and write this data to the eraf.
            if (state.bufferModified) {
                flush();
                state.bufferStart = dataEnd = state.dataSize = 0;
            }
            // System.out.println("--write at "+filePosition+" "+len);
            final int wLen = state.write_(filePosition, b, off, len);
            updateCache(filePosition, b, off, wLen);
            filePosition += wLen;
            state.dataSize = 0;
            dataEnd = 0;
            state.bufferModified = false;
            endOfFile = wLen <= 0;
        }
    }

    /**
     * Writes <code>b.length</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this eraf.
     *
     * @param b the data.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void write(byte [] b) throws IOException {
        checkClosed();
        checkArrayNotNull(b, "byte buffer");
        writeBytes(b, 0, b.length);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array starting at
     * offset <code>off</code> to this eraf.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void write(byte [] b, int off, int len) throws IOException {
        writeBytes(b, off, len);
    }

    /**
     * Create a string representation of this object.
     *
     * @return a string representation of the state of the object.
     */
    @Override
    public synchronized String toString() {
        return "fp=" + filePosition + ", bs=" + state.bufferStart + ", de=" + dataEnd
                + ", ds=" + state.dataSize + ", bl=" + state.buffer.length + ", readonly="
                + state.readonly + ", bm=" + state.bufferModified;
    }

    /**
     * Support for FileCache.
     */
    private boolean cached;

    private String location;

    /**
     * Controls the caching of this {@code EnhancedNIORandomAccessFile} in order to allow seeking backwards.
     *
     * @param cached the caching switch {@code true} if this {@code EnhancedNIORandomAccessFile}
     *               caches data itself in order to allow seeking backwards
     * @throws IOException if an I/O error occurs, eg. this {@code EnhancedNIORandomAccessFile} is closed
     */
    public synchronized void setCached(boolean cached) throws IOException {
        flush();
        state.bufferModified = false;
        state.bufferStart = filePosition;
        state.dataSize = 0;
        dataEnd = filePosition;
        endOfFile = false;
        this.cached = cached;
    }

    /**
     * Activates a temporary disk cache in the specified directory, or
     * deactivates the disk cache when {@code location} is {@code null}.
     * Existing cached ranges are discarded when the location changes.
     *
     * @param location the cache directory, or {@code null} to disable the
     *                 disk cache
     * @throws IOException if the cache file cannot be created, closed, or
     *                     deleted
     * @throws IllegalArgumentException if {@code location} is not a directory
     */
    public synchronized void setLocation(String location) throws IOException {
        checkClosed();
        flush();
        if (location == null) {
            closeCache();
            this.location = null;
            return;
        }

        final Path directory = Path.of(location);
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Cache location is not a directory: " + location);
        }

        final Path newCacheFile = Files.createTempFile(directory, "eraf-", ".cache");
        final FileChannel newCacheChannel;
        try {
            newCacheChannel = FileChannel.open(newCacheFile,
                    StandardOpenOption.READ, StandardOpenOption.WRITE);
        } catch (IOException e) {
            Files.deleteIfExists(newCacheFile);
            throw e;
        }

        try {
            closeCache();
        } catch (IOException e) {
            try {
                newCacheChannel.close();
            } finally {
                Files.deleteIfExists(newCacheFile);
            }
            throw e;
        }
        state.cacheFile = newCacheFile;
        state.cacheChannel = newCacheChannel;
        state.cachedRanges.clear();
        this.location = location;
    }

    /**
     * Returns {@code true} if this {@code EnhancedNIORandomAccessFile}
     * caches data itself in order to allow seeking backwards.
     * Applications may consult this in order to decide how frequently,
     * or whether, to flush in order to conserve cache resources.
     *
     * @return {@code true} if this {@code EnhancedNIORandomAccessFile}
     * caches data.
     * @see #isCachedMemory
     * @see #isCachedFile
     */
    public synchronized boolean isCached() {
        return cached || state.cacheChannel != null;
    }

    /**
     * Returns {@code true} if this {@code EnhancedNIORandomAccessFile}
     * caches data itself in order to allow seeking backwards, and
     * the cache is kept in main memory.  Applications may consult
     * this in order to decide how frequently, or whether, to flush
     * in order to conserve cache resources.
     *
     * @return {@code true} if this {@code EnhancedNIORandomAccessFile}
     * caches data in main memory.
     * @see #isCached
     * @see #isCachedFile
     */
    public synchronized boolean isCachedMemory() {
        return cached && state.cacheChannel == null;
    }

    /**
     * Returns {@code true} if this {@code EnhancedNIORandomAccessFile}
     * caches data itself in order to allow seeking backwards, and
     * the cache is kept in a temporary file.  Applications may consult
     * this in order to decide how frequently, or whether, to flush
     * in order to conserve cache resources.
     *
     * @return {@code true} if this {@code EnhancedNIORandomAccessFile}
     * caches data in a temporary file.
     * @see #isCached
     * @see #isCachedMemory
     */
    public synchronized boolean isCachedFile() {
        return state.cacheChannel != null;
    }

    /**
     * Flushes buffered data and requests that the underlying file channel
     * force file and metadata updates.
     *
     * @throws IOException if an I/O error occurs while flushing or forcing the
     *                     channel
     */
    public synchronized void synch() throws IOException {
        flush();
        state.fileChannel.force(true);
        if (state.cacheChannel != null) {
            state.cacheChannel.force(true);
        }
    }

    /**
     * Returns the cache directory configured for this instance.
     *
     * @return the cache directory, or {@code null} if the disk cache is
     *         disabled
     */
    public synchronized String getLocation() {
        return location;
    }

    private void closeCache() throws IOException {
        state.closeCache();
    }

    private void addCachedRange(long start, long end) {
        if (start >= end) {
            return;
        }
        final Map.Entry<Long, Long> previous = state.cachedRanges.floorEntry(start);
        if (previous != null && previous.getValue() >= start) {
            start = previous.getKey();
            end = Math.max(end, previous.getValue());
            state.cachedRanges.remove(previous.getKey());
        }
        Map.Entry<Long, Long> next = state.cachedRanges.ceilingEntry(start);
        while (next != null && next.getKey() <= end) {
            end = Math.max(end, next.getValue());
            state.cachedRanges.remove(next.getKey());
            next = state.cachedRanges.ceilingEntry(start);
        }
        state.cachedRanges.put(start, end);
    }

    private void invalidateCachedRange(long start, long length) throws IOException {
        if (length <= 0) {
            return;
        }
        final long end = checkedEnd(start, length);
        final Map.Entry<Long, Long> containing = state.cachedRanges.floorEntry(start);
        if (containing != null && containing.getValue() > start) {
            final long oldEnd = containing.getValue();
            state.cachedRanges.remove(containing.getKey());
            if (containing.getKey() < start) {
                state.cachedRanges.put(containing.getKey(), start);
            }
            if (oldEnd > end) {
                state.cachedRanges.put(end, oldEnd);
            }
        }
        Map.Entry<Long, Long> next = state.cachedRanges.ceilingEntry(start);
        while (next != null && next.getKey() < end) {
            final long oldEnd = next.getValue();
            state.cachedRanges.remove(next.getKey());
            if (oldEnd > end) {
                state.cachedRanges.put(end, oldEnd);
                break;
            }
            next = state.cachedRanges.ceilingEntry(start);
        }
    }

    private void updateCache(long position, byte[] bytes, int offset, int length) throws IOException {
        if (state.cacheChannel == null || length <= 0) {
            return;
        }
        state.cacheChannel.position(position);
        writeFully(state.cacheChannel, bytes, offset, length);
        addCachedRange(position, checkedEnd(position, length));
    }

    private static long checkedEnd(long position, long length) throws IOException {
        try {
            return Math.addExact(position, length);
        } catch (ArithmeticException e) {
            throw new IOException("File position exceeds the supported range", e);
        }
    }


    private static final class EnhancedRandomAccessFileState implements CleanUpState {
        private volatile FileChannel fileChannel;
        private final byte[] buffer;
        private long bufferStart;
        private long flushedPos = 0;
        private int dataSize;
        private final boolean readonly;
        private final boolean writeOnly;
        private boolean bufferModified;
        private long minLength;
        private FileChannel cacheChannel;
        private Path cacheFile;
        private final NavigableMap<Long, Long> cachedRanges = new TreeMap<>();

        private EnhancedRandomAccessFileState(FileChannel fileChannel,
                                              byte[] buffer,
                                              long bufferStart,
                                              int dataSize,
                                              boolean readonly,
                                              boolean writeOnly,
                                              boolean bufferModified,
                                              long minLength) {
            this.fileChannel = fileChannel;
            this.buffer = buffer;
            this.bufferStart = bufferStart;
            this.dataSize = dataSize;
            this.readonly = readonly;
            this.writeOnly = writeOnly;
            this.bufferModified = bufferModified;
            this.minLength = minLength;
        }

        public synchronized boolean isClosed() {
            return fileChannel == null || !fileChannel.isOpen();
        }

        private synchronized boolean isFullyClosed() {
            return isClosed() && cacheChannel == null && cacheFile == null;
        }

        @Override
        public synchronized void run() {
            try {
                closeState();
            } catch (IOException e) {
                Logger.getLogger(EnhancedNIORandomAccessFile.EnhancedRandomAccessFileState.class.getName()).log(Level.SEVERE, e.getMessage(), e);
            }
        }

        private synchronized void closeState() throws IOException {
            IOException failure = null;
            try {
                if (fileChannel != null) {
                    try {
                        if (!readonly && bufferModified) {
                            write_(bufferStart, buffer, 0, dataSize);
                        }

                        if (!readonly && minLength >= 0 && minLength != fileChannel.size()) {
                            if (minLength <= fileChannel.size()) {
                                fileChannel.truncate(minLength);
                            } else {
                                write_(minLength - 1, new byte[1], 0, 1);
                            }
                        }
                    } catch (IOException e) {
                        failure = e;
                    } finally {
                        try {
                            fileChannel.close();
                        } catch (IOException e) {
                            if (failure == null) {
                                failure = e;
                            } else {
                                failure.addSuppressed(e);
                            }
                        } finally {
                            fileChannel = null;
                        }
                    }
                }
            } finally {
                bufferModified = false;
                dataSize = 0;
                minLength = -1;
                try {
                    closeCache();
                } catch (IOException e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        }

        private synchronized void closeCache() throws IOException {
            IOException failure = null;
            if (cacheChannel != null) {
                try {
                    cacheChannel.close();
                    cacheChannel = null;
                } catch (IOException e) {
                    failure = e;
                }
            }
            if (cacheFile != null && cacheChannel == null) {
                try {
                    Files.deleteIfExists(cacheFile);
                    cacheFile = null;
                    cachedRanges.clear();
                } catch (IOException e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        }

        /**
         * write directly, without going through the buffer
         *
         * @param pos    the write start position
         * @param b      the buffer source
         * @param offset the source buffer offset
         * @param len    the requested write length
         * @return the actually write length
         * @throws IOException if an I/O error occurrs.
         */
        private int write_(long pos, byte[] b, int offset, int len) throws IOException {
            //If Read-only, reject write by throwing NonWritableChannelException
            if (readonly) {
                throw new NonWritableChannelException();
            }
            fileChannel.position(pos);
            final ByteBuffer buffer = ByteBuffer.wrap(b, offset, len);
            int wLen = 0;
            int wCount;
            int wAttempts = 0;
            while (buffer.hasRemaining()) {
                wCount = fileChannel.write(buffer);
                wLen += wCount;
                wAttempts = wCount < 1 ? wAttempts + 1 : 0;
                if (wAttempts >= W_ATTEMPTS_LIMIT) {
                    throw new IOException("fail to write after " + wLen + " position. " + W_ATTEMPTS_LIMIT + " write attempts.");
                }
            }
            return wLen;
        }

    }
}
