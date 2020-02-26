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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jcoravu on 30/4/2019.
 */
public class ContiguousCodestreamBox implements IMarkers {

	private SIZMarkerSegment siz;
	private CODMarkerSegment cod;
	private QCDMarkerSegment qcd;
	private RGNMarkerSegment rgn;

	public ContiguousCodestreamBox(IRandomAccessFile jp2FileStream) throws IOException {
		if (jp2FileStream.readShort() == IMarkers.SOC) {
			short marker = jp2FileStream.readShort();
			if (marker == SIZ) {
				Map<Short, DataInputStream> result = new HashMap<Short, DataInputStream>();

				DataInputStream sizInputStream = readBytes(marker, jp2FileStream);
				result.put(SIZ, sizInputStream);

				while ((marker = jp2FileStream.readShort()) != SOT) {
					boolean existingMarker = result.containsKey(marker);
					validateMarkerSegment(marker, existingMarker);
					DataInputStream inputStream = readBytes(marker, jp2FileStream);
					result.put(marker, inputStream);
				}

				DataInputStream dataInputStream = result.get(SIZ);
				if (dataInputStream != null) {
					readSIZ(dataInputStream);
				}

				dataInputStream = result.get(COD);
				if (dataInputStream != null) {
					readCOD(dataInputStream);
				}

				dataInputStream = result.get(QCD);
				if (dataInputStream != null) {
					readQCD(dataInputStream);
				}

				dataInputStream = result.get(RGN);
				if (dataInputStream != null) {
					readRGN(dataInputStream);
				}
			} else {
				throw new InvalidContiguousCodestreamException("First marker after " + "SOC " + "must be SIZ " + Integer.toHexString(marker));
			}
		} else {
			throw new InvalidContiguousCodestreamException("SOC marker segment not " + " found at the " + "beginning of the " + "codestream.");
		}
	}

	public String toStringMainHeader() {
		return siz + " " + cod + " " + qcd;
	}

	public final int getNumComps() {
		return siz.getNumComps();
	}

	public SIZMarkerSegment getSiz() {
		return siz;
	}

	public CODMarkerSegment getCod() {
		return cod;
	}

	public QCDMarkerSegment getQcd() {
		return qcd;
	}

	public RGNMarkerSegment getRgn() {
		return rgn;
	}

	private void readSIZ(DataInputStream jp2FileStream) throws IOException {
		this.siz = new SIZMarkerSegment();
		this.siz.readData(jp2FileStream);
	}

	private void readQCD(DataInputStream jp2FileStream) throws IOException {
		this.qcd = new QCDMarkerSegment();
		this.qcd.readData(jp2FileStream, this.cod.getNumberOfLevels());
	}

	private void readCOD(DataInputStream jp2FileStream) throws IOException {
		this.cod = new CODMarkerSegment();
		this.cod.readData(jp2FileStream);
	}

	private void readRGN(DataInputStream jp2FileStream) throws IOException {
		this.rgn = new RGNMarkerSegment();
		this.rgn.readData(jp2FileStream, this.siz.getNumComps());
	}

	private static void validateMarkerSegment(short marker, boolean existingMarker) throws IOException {
		if (marker == SIZ) {
			throw new InvalidContiguousCodestreamException("More than one SIZ marker segment found in main header");
		} else if (marker == SOD) {
			throw new InvalidContiguousCodestreamException("SOD found in main header");
		} else if (marker == EOC) {
			throw new InvalidContiguousCodestreamException("EOC found in main header");
		} else if (marker == COD) {
			if (existingMarker) {
				throw new InvalidContiguousCodestreamException("More than one COD marker found in main header");
			}
		} else if (marker == QCD) {
			if (existingMarker) {
				throw new InvalidContiguousCodestreamException("More than one QCD marker found in main header");
			}
		} else if (marker == CRG) {
			if (existingMarker) {
				throw new InvalidContiguousCodestreamException("More than one CRG " + "marker " + "found in main header");
			}
		} else if (marker == TLM) {
			if (existingMarker) {
				throw new InvalidContiguousCodestreamException("More than one TLM " + "marker " + "found in main header");
			}
		} else if (marker == PLM) {
			if (existingMarker) {
				throw new InvalidContiguousCodestreamException("More than one PLM " + "marker " + "found in main header");
			}
		} else if (marker == POC) {
			if (existingMarker) {
				throw new InvalidContiguousCodestreamException("More than one POC " + "marker segment found " + "in main header");
			}
		} else if (marker == PLT) {
			throw new InvalidContiguousCodestreamException("PLT found in main header");
		} else if (marker == PPT) {
			throw new InvalidContiguousCodestreamException("PPT found in main header");
		}
	}

	private static DataInputStream readBytes(short marker, IRandomAccessFile jp2FileStream) throws IOException {
		if (marker < 0xffffff30 || marker > 0xffffff3f) {
			// Read marker segment length and create corresponding byte buffer
			int markerSegmentLenght = jp2FileStream.readUnsignedShort();
			byte[] buffer = new byte[markerSegmentLenght];

			// Copy data (after re-insertion of the marker segment length);
			buffer[0] = (byte) ((markerSegmentLenght >> 8) & 0xFF);
			buffer[1] = (byte) (markerSegmentLenght & 0xFF);
			jp2FileStream.read(buffer, 2, markerSegmentLenght - 2);

			ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
			return new DataInputStream(bais);
		}
		return null;
	}
}
