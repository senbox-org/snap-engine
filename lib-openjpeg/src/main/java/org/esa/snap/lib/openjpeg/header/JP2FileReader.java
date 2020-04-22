/*
 * $RCSfile: FileFormatReader.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/04/28 01:25:38 $
 * $State: Exp $
 *
 * Class:                   FileFormatReader
 *
 * Description:             Read J2K file stream
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
 */
package org.esa.snap.lib.openjpeg.header;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 30/4/2019.
 */
public class JP2FileReader implements FileFormatBoxes {

	private static final Logger logger = Logger.getLogger(JP2FileReader.class.getName());

	private static final Set<Integer> BLOCK_TERMINATORS = new HashSet<>();
	static {
		BLOCK_TERMINATORS.add(0);
		BLOCK_TERMINATORS.add(7);
	}

	private ContiguousCodestreamBox contiguousCodestreamBox;
	private List<String> xmlMetadata;

	public JP2FileReader() {
	}

	public ContiguousCodestreamBox getHeaderDecoder() {
		return this.contiguousCodestreamBox;
	}

	public List<String> getXmlMetadata() {
		return this.xmlMetadata;
	}

	public void readFileFormat(Path file, int bufferSize, boolean canSetFilePosition) throws IOException {
		long startTime = System.currentTimeMillis();

		long positionAfterFileTypeBox = readHeader(file, bufferSize, canSetFilePosition);
		if (this.contiguousCodestreamBox == null) {
			// Not a valid JP2 file or codestream
			throw new IOException("Invalid JP2 file: Contiguous codestream box is missing.");
		} else {
			if (this.xmlMetadata == null) {
				readXMLBox(file, bufferSize, positionAfterFileTypeBox);
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			double elapsedTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000.d;
			long sizeInBytes = Files.size(file);
			logger.log(Level.FINE, "Finish reading JP2 file header '"+ file+"', size: "+ sizeInBytes+" bytes, elapsed time: "+ elapsedTimeInSeconds+" seconds.");
		}
	}

	private long readHeader(Path file, int bufferSize, boolean canSetFilePosition) throws IOException {
		BufferedRandomAccessFile jp2FileStream = new BufferedRandomAccessFile(file, bufferSize, canSetFilePosition);

		readJP2SignatureBox(jp2FileStream);

		readFileTypeBox(jp2FileStream);

		// read all remaining boxes
		long fileSizeInBytes = jp2FileStream.getLength();
		long positionAfterFileTypeBox = jp2FileStream.getPosition();
		boolean jp2HeaderBoxFound = false;
		boolean lastBoxFound = false;
		while (!lastBoxFound) {
			long boxPosition = jp2FileStream.getPosition();

			long boxLength = jp2FileStream.readUnsignedInt();
			int boxType = jp2FileStream.readInt();
			long boxExtendedLength = 0;
			if (boxLength == 0) {
				lastBoxFound = true;
			} else if (boxLength == 1) {
				boxExtendedLength = jp2FileStream.readLong();
				boxLength = boxExtendedLength;
			}

			if ((boxPosition + boxLength) == fileSizeInBytes) {
				lastBoxFound = true;
			}
			if (lastBoxFound) {
				boxLength = fileSizeInBytes - jp2FileStream.getPosition();
			}

			if (boxType == JP2_HEADER_BOX) {
				if (jp2HeaderBoxFound) {
					throw new IOException("Invalid JP2 file: Multiple JP2Header boxes found.");
				} else if (this.contiguousCodestreamBox == null) {
					readJP2HeaderBox(boxLength, boxExtendedLength, jp2FileStream);
					jp2HeaderBoxFound = true;
				} else {
					throw new IOException("Invalid JP2 file: The JP2Header box must be before the contiguous code stream.");
				}
			} else if (boxType == CONTIGUOUS_CODESTREAM_BOX) {
				if (jp2HeaderBoxFound) {
					readContiguousCodeStreamBox(boxLength, boxExtendedLength, jp2FileStream);
				} else {
					throw new IOException("Invalid JP2 file: JP2Header box not found before Contiguous codestream box.");
				}
			} else if (boxType == INTELLECTUAL_PROPERTY_BOX) {
				readIntellectualPropertyBox(boxLength);
			} else if (boxType == XML_BOX) {
				readXMLBox(boxLength, jp2FileStream);
			} else if (boxType == UUID_BOX) {
				readUUIDBox(boxLength);
			} else if (boxType == UUID_INFO_BOX) {
				readUUIDInfoBox(boxLength);
			} else if (boxType == ASSOCIATION_BOX) {
				// the association box contains the xml box sometimes
				readAssociationBox(boxLength);
			} else {
				//System.out.println("Unknown box-type: 0x" + Integer.toHexString(boxType));
			}
			if (!lastBoxFound) {
				jp2FileStream.seek(boxPosition + boxLength);
			}
		}
		return positionAfterFileTypeBox;
	}

	private void readXMLBox(Path file, int maximumBufferSize, long positionAfterFileTypeBox) throws IOException {
		InputStream inputStream = Files.newInputStream(file);
		try {
			BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			try {
				int bufferSize = maximumBufferSize;
				if (bufferSize > positionAfterFileTypeBox) {
					bufferSize = (int)positionAfterFileTypeBox;
				}

				byte[] buffer = new byte[bufferSize];
				int bytesToRead = buffer.length;
				int bytesRead;
				long totalTransferredBytes = 0;
				while ((totalTransferredBytes < positionAfterFileTypeBox) && (bytesRead = bufferedInputStream.read(buffer, 0, bytesToRead)) > 0) {
					totalTransferredBytes += bytesRead;
					long remainingBytesToRead = positionAfterFileTypeBox - totalTransferredBytes;
					if (remainingBytesToRead < bytesToRead) {
						bytesToRead = (int)remainingBytesToRead;
					}
				}

				if (totalTransferredBytes == positionAfterFileTypeBox) {
					int firstByte = (FileFormatBoxes.XML_BOX & 0xFF000000) >> 24; // MSB
					int secondByte = (FileFormatBoxes.XML_BOX & 0x00FF0000) >> 16;
					int thirdByte = (FileFormatBoxes.XML_BOX & 0x0000FF00) >> 8;
					int forthByte = (FileFormatBoxes.XML_BOX & 0x000000FF); // LSB
					int[] xmlBoxCodeInReverseOrder = { forthByte, thirdByte, secondByte, firstByte };
					ByteSequenceMatcher xmlTagMatcher = new ByteSequenceMatcher(xmlBoxCodeInReverseOrder);

					long fileSizeInBytes = Files.size(file);
					while (totalTransferredBytes < fileSizeInBytes) {
						int current = bufferedInputStream.read(); // read one byte
						if (current == -1) {
							break; // end of file
						} else {
							totalTransferredBytes++;
							if (xmlTagMatcher.matches(current)) {
								StringBuilder builder = new StringBuilder();
								int currentXMLByte;
								while (totalTransferredBytes < fileSizeInBytes && !BLOCK_TERMINATORS.contains(currentXMLByte = bufferedInputStream.read())) {
									builder.append(Character.toString((char) currentXMLByte));
								}
								if (this.xmlMetadata == null) {
									this.xmlMetadata = new ArrayList<String>();
								}
								this.xmlMetadata.add(builder.toString());
								break; // read only the first xml box
							}
						}
					}
				} else {
					throw new IllegalStateException("The number of transferred bytes "+totalTransferredBytes+" is different than the file position "+ positionAfterFileTypeBox+".");
				}
			} finally {
				bufferedInputStream.close();
			}
		} finally {
			inputStream.close();
		}
	}

	private void readJP2SignatureBox(IRandomAccessFile jp2FileStream) throws IOException {
		if (jp2FileStream.readInt() == 0x0000000C) {
			if (jp2FileStream.readInt() == JP2_SIGNATURE_BOX) {
				if (jp2FileStream.readInt() == JP2_SIGNATURE_BOX_CONTENT) {
					return;
				}
			}
		}
		throw new IOException("nvalid JP2 file: file is neither valid JP2 file nor valid JPEG 2000 codestream");
	}

	private void readFileTypeBox(IRandomAccessFile jp2FileStream) throws IOException {
		// read box length (LBox)
		int length = jp2FileStream.readInt();
		if (length == 0) {
			throw new IOException("Zero-length of Profile Box");
		} else {
			// check that this is a File Type box (TBox)
			if (jp2FileStream.readInt() == FILE_TYPE_BOX) {
				// check for XLBox
				if (length == 1) { // box has 8 byte length
					throw new IOException("File too long.");
				}

				// read Brand field
				jp2FileStream.readInt();

				// read MinV field
				jp2FileStream.readInt();

				// check that there is at least one FT_BR entry in in compatibility list
				boolean foundComp = false;
				int nComp = (length - 16) / 4; // Number of compatibilities.
				for (int i = nComp; i > 0; i--) {
					if (jp2FileStream.readInt() == FT_BR) {
						foundComp = true;
					}
				}
				if (!foundComp) {
					throw new IOException("Invalid JP2 file: missing entry.");
				}
			} else {
				throw new IOException("Invalid JP2 file: File Type box missing");
			}
		}
	}

	/**
	 * Within a JP2 file, there shall be one and only one JP2 Header box. The JP2 Header box may be
	 * located anywhere within the file after the File Type box but before the Contiguous Codestream box.
	 */
	private void readJP2HeaderBox(long boxLength, long longLength, IRandomAccessFile jp2FileStream) throws IOException {
		if (boxLength == 0) { // This can not be last box
			throw new IOException("Zero-length of JP2Header Box");
		}

		// Here the JP2Header data (DBox) would be read if we were to use it
		int firstBlock = jp2FileStream.readInt();
		if (firstBlock == 22) {
		} else {
			throw new IOException("Invalid image header box.");
		}
	}

	private void readContiguousCodeStreamBox(long boxLength, long longLength, IRandomAccessFile jp2FileStream) throws IOException {
		this.contiguousCodestreamBox = new ContiguousCodestreamBox(jp2FileStream);
	}

	private void readIntellectualPropertyBox(long boxLength) {
	}

	/**
	 * An XML box contains vendor specific information (in XML format) other than the information contained
	 * within boxes defined by this Recommendation | International Standard.
	 * There may be multiple XML boxes within the file, and those boxes may be found anywhere in the file except before the File Type box.
	 */
	private void readXMLBox(long boxLength, IRandomAccessFile jp2FileStream) throws IOException {
		StringBuilder builder = new StringBuilder();
		int index = 0;
		int currentByte;
		while (index < boxLength && !BLOCK_TERMINATORS.contains(currentByte = jp2FileStream.readByte())) {
			builder.append(Character.toString((char) currentByte));
			index++;
		}
		if (this.xmlMetadata == null) {
			this.xmlMetadata = new ArrayList<String>();
		}
		this.xmlMetadata.add(builder.toString());
	}

	private void readUUIDBox(long boxLength) {
	}

	private void readUUIDInfoBox(long boxLength) {
	}

	private void readAssociationBox(long boxLength) {
	}

	private class ByteSequenceMatcher {
		private int[] queue;
		private int[] sequence;

		ByteSequenceMatcher(int[] sequenceToMatch) {
			this.sequence = sequenceToMatch;
			this.queue = new int[sequenceToMatch.length];
		}

		public boolean matches(int unsignedByte) {
			insert(unsignedByte);
			return isMatch();
		}

		private void insert(int unsignedByte) {
			System.arraycopy(this.queue, 0, this.queue, 1, this.sequence.length - 1);
			this.queue[0] = unsignedByte;
		}

		private boolean isMatch() {
			boolean result = true;
			for (int i = 0; i < this.sequence.length; i++) {
				result = (this.queue[i] == this.sequence[i]);
				if (!result)
					break;
			}
			return result;
		}
	}

	public static void main(String argv[]) throws IOException {
//		String filePath = "d:\\open-jpeg-files\\1_8bit_component_gamma_1_8_space.jp2";
//		String filePath = "d:\\open-jpeg-files\\sample.jp2";
//		String filePath = "d:\\open-jpeg-files\\IMG_test1.jp2";
//		String filePath = "d:\\open-jpeg-files\\IMG_test2.jp2";
//		String filePath = "d:\\open-jpeg-files\\s2-l1c\\T34HFH_20161206T080312_B02.jp2";
//		String filePath = "C:\\Apache24\\htdocs\\snap\\JP2\\IMG_PHR1A_1,5GB.JP2";
//		String filePath = "C:\\Apache24\\htdocs\\snap\\JP2\\IMG_PHR1A_358MB.JP2";
		String filePath = "D:\\shared\\IMG_PHR1A_PMS_201402040054228_ORT_1336649101-001_R1C1.JP2";

		Path jp2File = Paths.get(filePath);

		System.out.println("Reading file: "+ jp2File.toString()+"\n");

		JP2FileReader fileFormatReader = new JP2FileReader();
		fileFormatReader.readFileFormat(jp2File, 1024 * 1024, true);

		ContiguousCodestreamBox hd = fileFormatReader.getHeaderDecoder();
		SIZMarkerSegment siz = hd.getSiz();

		int nCompCod = hd.getNumComps();
		int nTiles = siz.computeNumTiles();

		// Report information

		String info = nCompCod + " component(s) in codestream, " + nTiles + " tile(s)\n";
		info += "Num tiles: on x " + siz.computeNumTilesX();
		info += " on y: " + siz.computeNumTilesY() + "\n";
		info += "Image dimension: ";
		for (int c = 0; c < nCompCod; c++) {
			info += siz.getCompImgWidth(c) + "x" + siz.getCompImgHeight(c) + " ";
		}

		if (nTiles != 1) {
			info += "\nNom. Tile dim. (in canvas): " + siz.getNominalTileWidth() + "x" + siz.getNominalTileHeight();
		}
		System.out.println(info);
		System.out.println("Main header:\n" + hd.toStringMainHeader());
	}
}
