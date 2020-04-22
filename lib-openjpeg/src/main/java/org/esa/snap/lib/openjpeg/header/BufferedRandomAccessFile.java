/*
 * $RCSfile: FileFormatBoxes.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/02/11 05:02:10 $
 * $State: Exp $
 *
 * Class:                   FileFormatMarkers
 *
 * Description:             Contains definitions of boxes used in jp2 files
 *
 *
 *
 * COPYRIGHT:
 *
 * This software module was originally developed by Raphaël Grosbois and
 * Diego Santa Cruz (Swiss Federal Institute of Technology-EPFL); Joel
 * Askelöf (Ericsson Radio Systems AB); and Bertrand Berthelot, David
 * Bouchard, Félix Henry, Gerard Mozelle and Patrice Onno (Canon Research
 * Centre France S.A) in the course of development of the JPEG2000
 * standard as specified by ISO/IEC 15444 (JPEG 2000 Standard). This
 * software module is an implementation of a part of the JPEG 2000
 * Standard. Swiss Federal Institute of Technology-EPFL, Ericsson Radio
 * Systems AB and Canon Research Centre France S.A (collectively JJ2000
 * Partners) agree not to assert against ISO/IEC and users of the JPEG
 * 2000 Standard (Users) any of their rights under the copyright, not
 * including other intellectual property rights, for this software module
 * with respect to the usage by ISO/IEC and Users of this software module
 * or modifications thereof for use in hardware or software products
 * claiming conformance to the JPEG 2000 Standard. Those intending to use
 * this software module in hardware or software products are advised that
 * their use may infringe existing patents. The original developers of
 * this software module, JJ2000 Partners and ISO/IEC assume no liability
 * for use of this software module or modifications thereof. No license
 * or right to this software module is granted for non JPEG 2000 Standard
 * conforming products. JJ2000 Partners have full right to use this
 * software module for his/her own purpose, assign or donate this
 * software module to any third party and to inhibit third parties from
 * using this software module for non JPEG 2000 Standard conforming
 * products. This copyright notice must be included in all copies or
 * derivative works of this software module.
 *
 * Copyright (c) 1999/2000 JJ2000 Partners.
 *
 *
 *
 */
package org.esa.snap.lib.openjpeg.header;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

/**
 * Created by jcoravu on 30/4/2019.
 */
public class BufferedRandomAccessFile implements IRandomAccessFile {

	private final Path file;
	private final boolean canSetFilePosition;
	private final byte[] byteBuffer;

	private long byteBufferStreamOffset;
	private int byteBufferPosition;
	private int byteRead;
	private boolean isEOFInBuffer;

	public BufferedRandomAccessFile(Path file, int bufferSize, boolean canSetFilePosition) throws IOException {
		this.file = file;
		this.canSetFilePosition = canSetFilePosition;
		this.byteBuffer = new byte[bufferSize];

		readBuffer(0);
	}
	
	@Override
	public final short readShort() throws IOException {
		return (short) ((readByte() << 8) | (readByte()));
	}

	@Override
	public final int readUnsignedShort() throws IOException {
		return ((readByte() << 8) | readByte());
	}

	@Override
	public final int readInt() throws IOException {
		return ((readByte() << 24) | (readByte() << 16) | (readByte() << 8) | readByte());
	}

	@Override
	public final long readUnsignedInt() throws IOException {
		return (long) ((readByte() << 24) | (readByte() << 16) | (readByte() << 8) | readByte());
	}

	@Override
	public final long readLong() throws IOException {
		return (((long) readByte() << 56) | ((long) readByte() << 48) | ((long) readByte() << 40) | ((long) readByte() << 32)
				| ((long) readByte() << 24) | ((long) readByte() << 16) | ((long) readByte() << 8) | ((long) readByte()));
	}

	@Override
	public final float readFloat() throws IOException {
		return Float.intBitsToFloat((readByte() << 24) | (readByte() << 16) | (readByte() << 8) | (readByte()));
	}

	@Override
	public final double readDouble() throws IOException {
		return Double.longBitsToDouble(
				((long) readByte() << 56) | ((long) readByte() << 48) | ((long) readByte() << 40) | ((long) readByte() << 32)
						| ((long) readByte() << 24) | ((long) readByte() << 16) | ((long) readByte() << 8) | ((long) readByte()));
	}

	@Override
	public long getPosition() {
		return (byteBufferStreamOffset + byteBufferPosition);
	}

	@Override
	public long getLength() throws IOException {
		return Files.size(this.file);
	}

	@Override
	public void seek(long offset) throws IOException {
		if ((offset >= byteBufferStreamOffset) && (offset < (byteBufferStreamOffset + byteBuffer.length))) {
			if (isEOFInBuffer && offset > byteBufferStreamOffset + byteRead) {
				// We are seeking beyond EOF in read-only mode!
				throw new EOFException();
			}
			long result = offset - this.byteBufferStreamOffset;
			if (result > Integer.MAX_VALUE) {
				throw new IllegalStateException("The byte buffer position " + result + " is greater than the maximmum integer number " + Integer.MAX_VALUE + ".");
			}
			this.byteBufferPosition = (int)result;
		} else {
			readBuffer(offset);
		}
	}

	@Override
	public final int readByte() throws IOException {
		if (byteBufferPosition < byteRead) { // The byte can be read from the buffer
			// In Java, the bytes are always signed.
			return (byteBuffer[byteBufferPosition++] & 0xFF);
		} else if (isEOFInBuffer) { // EOF is reached
			byteBufferPosition = byteRead + 1; // Set position to EOF
			throw new EOFException();
		} else { // End of the buffer is reached
			readBuffer(byteBufferStreamOffset + byteBufferPosition);
			return readByte();
		}
	}

	@Override
	public final void read(byte buffer[], int offset, int lenght) throws IOException {
		int currentLenght; // current length to read
		while (lenght > 0) {
			// There still is some data to read
			if (byteBufferPosition < byteRead) { // We can read some data from buffer
				currentLenght = byteRead - byteBufferPosition;
				if (currentLenght > lenght) {
					currentLenght = lenght;
				}
				System.arraycopy(byteBuffer, byteBufferPosition, buffer, offset, currentLenght);
				byteBufferPosition += currentLenght;
				offset += currentLenght;
				lenght -= currentLenght;
			} else if (isEOFInBuffer) {
				byteBufferPosition = byteRead + 1; // Set position to EOF
				throw new EOFException();
			} else { // Buffer empty => get more data
				readBuffer(byteBufferStreamOffset + byteBufferPosition);
			}
		}
	}

	private void readBuffer(long offset) throws IOException {
		// don't allow to seek beyond end of file if reading only
		if (offset >= getLength()) {
			throw new EOFException();
		}
		// set new offset
		this.byteBufferStreamOffset = offset;

		Set<? extends OpenOption> options = Collections.emptySet();
		long bufferSize = Math.min(10 * 1024, this.byteBufferStreamOffset);
		if (bufferSize > Integer.MAX_VALUE) {
			throw new IllegalStateException("The buffer size " + bufferSize + " is greater than the maximmum integer number " + Integer.MAX_VALUE + ".");
		}
		int capacity = (int)bufferSize;
		ByteBuffer byteBufferObject = ByteBuffer.allocate(capacity);

		if (this.canSetFilePosition) {
			try (FileChannel fileChannel = FileChannel.open(this.file, StandardOpenOption.READ);
				 InputStream inputStream = Channels.newInputStream(fileChannel.position(this.byteBufferStreamOffset))) {

				readFromInputStream(inputStream);
			}
		} else {
			FileSystemProvider fileSystemProvider = this.file.getFileSystem().provider();
			try (SeekableByteChannel seekableByteChannel = fileSystemProvider.newByteChannel(this.file, options)) {
				while (seekableByteChannel.position() < this.byteBufferStreamOffset) {
					seekableByteChannel.read(byteBufferObject);
					byteBufferObject.clear();
					int difference = (int)(this.byteBufferStreamOffset - seekableByteChannel.position());
					if (difference < byteBufferObject.capacity()) {
						byteBufferObject.limit(difference);
					}
				}
				try (InputStream inputStream = Channels.newInputStream(seekableByteChannel)) {
					readFromInputStream(inputStream);
				}
			}
		}

		byteBufferPosition = 0;
		if (byteRead < byteBuffer.length) { // Not enough data in input file.
			isEOFInBuffer = true;
			if (byteRead == -1) {
				byteRead++;
			}
		} else {
			isEOFInBuffer = false;
		}
	}

	private void readFromInputStream(InputStream inputStream) throws IOException {
		this.byteRead = inputStream.read(this.byteBuffer, 0, this.byteBuffer.length);
	}
}
