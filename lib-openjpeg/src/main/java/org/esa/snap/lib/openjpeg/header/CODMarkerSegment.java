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

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by jcoravu on 30/4/2019.
 */
public class CODMarkerSegment extends AbstractMarkerSegment {

	private int lcod;
	private int scod; // Coding style
	private int sgcod_po; // Progression order
	private int sgcod_nl; // Number of layers
	private int sgcod_mct; // Multiple component transformation
	private int spcod_ndl; // Number of decomposition levels
	private int spcod_cw; // Code-blocks width
	private int spcod_ch; // Code-blocks height
	private int spcod_cs; // Code-blocks style
	private int waveletTransformId;
	private int[] spcod_ps; // Precinct size
	/** Is the precinct partition used */
	private boolean precinctPartitionIsUsed;

	public CODMarkerSegment() {
	}

	@Override
	public void readData(DataInputStream jp2FileStream) throws IOException {
		// Lcod (marker length)
		this.lcod = jp2FileStream.readUnsignedShort();

		// Scod (block style)
		// We only support wavelet transformed data
		this.scod = jp2FileStream.readUnsignedByte();

		int cstyle = this.scod;
		if ((cstyle & SCOX_PRECINCT_PARTITION) != 0) {
			precinctPartitionIsUsed = true;
			// Remove flag
			cstyle &= ~(SCOX_PRECINCT_PARTITION);
		} else {
			precinctPartitionIsUsed = false;
		}

		// SOP markers

		if ((cstyle & SCOX_USE_SOP) != 0) {
			// SOP markers are used
			// Remove flag
			cstyle &= ~(SCOX_USE_SOP);
		} else {
			// SOP markers are not used
		}

		// EPH markers
		if ((cstyle & SCOX_USE_EPH) != 0) {
			// EPH markers are used
			// Remove flag
			cstyle &= ~(SCOX_USE_EPH);
		} else {
			// EPH markers are not used
		}

		// SGcod
		// Read the progressive order
		this.sgcod_po = jp2FileStream.readUnsignedByte();

		// Read the number of layers
		this.sgcod_nl = jp2FileStream.readUnsignedShort();
		if (this.sgcod_nl <= 0 || this.sgcod_nl > 65535) {
			throw new InvalidContiguousCodestreamException("Number of layers out of " + "range: 1--65535");
		}

		// Multiple component transform
		this.sgcod_mct = jp2FileStream.readUnsignedByte();

		// SPcod
		// decomposition levels
		this.spcod_ndl = jp2FileStream.readUnsignedByte();
		if (this.spcod_ndl > 32) {
			throw new InvalidContiguousCodestreamException("Number of decomposition " + "levels out of range: " + "0--32");
		}

		// Read the code-blocks dimensions
		this.spcod_cw = jp2FileStream.readUnsignedByte();
		this.spcod_ch = jp2FileStream.readUnsignedByte();
		
		// Style of the code-block coding passes
		this.spcod_cs = jp2FileStream.readUnsignedByte();

		// read the wavelet transform id
		this.waveletTransformId = jp2FileStream.readUnsignedByte();
		if (this.waveletTransformId >= (1 << 7)) {
			throw new InvalidContiguousCodestreamException("Custom filters not supported.");
		}
		switch (this.waveletTransformId) {
			case FilterTypes.W9X7_IRREVERSIBLE:
				break;
			case FilterTypes.W5X3_REVERSIBLE:
				break;
			default:
				throw new InvalidContiguousCodestreamException("Specified wavelet filter is not JPEG 2000 part I compliant.");
		}

		this.spcod_ps = new int[this.spcod_ndl + 1];
		if (this.precinctPartitionIsUsed) {
			for (int rl = this.spcod_ndl; rl >= 0; rl--) {
				this.spcod_ps[this.spcod_ndl - rl] = jp2FileStream.readUnsignedByte();
			}
		} else {
			for (int rl = this.spcod_ndl; rl >= 0; rl--) {
				this.spcod_ps[this.spcod_ndl - rl] = IMarkers.PRECINCT_PARTITION_DEF_SIZE;
			}
		}

		this.precinctPartitionIsUsed = true;
	}

	public int getMultipleComponenTransform() {
		return this.sgcod_mct;
	}

	@Override
	public String toString() {
		String str = "\n --- COD (" + this.lcod + " bytes) ---\n";
		str += " Coding style   : ";
		if (this.scod == 0) {
			str += "Default";
		} else {
			if ((this.scod & SCOX_PRECINCT_PARTITION) != 0) {
				str += "Precints ";
			}
			if ((this.scod & SCOX_USE_SOP) != 0) {
				str += "SOP ";
			}
			if ((this.scod & SCOX_USE_EPH) != 0) {
				str += "EPH ";
			}
			int cb0x = ((this.scod & SCOX_HOR_CB_PART) != 0) ? 1 : 0;
			int cb0y = ((this.scod & SCOX_VER_CB_PART) != 0) ? 1 : 0;
			if (cb0x != 0 || cb0y != 0) {
				str += "Code-blocks offset";
				str += "\n Cblk partition : " + cb0x + "," + cb0y;
			}
		}
		str += "\n";
		str += " Cblk style     : ";
		if (spcod_cs == 0) {
			str += "Default";
		} else {
			if ((spcod_cs & 0x1) != 0)
				str += "Bypass ";
			if ((spcod_cs & 0x2) != 0)
				str += "Reset ";
			if ((spcod_cs & 0x4) != 0)
				str += "Terminate ";
			if ((spcod_cs & 0x8) != 0)
				str += "Vert_causal ";
			if ((spcod_cs & 0x10) != 0)
				str += "Predict ";
			if ((spcod_cs & 0x20) != 0)
				str += "Seg_symb ";
		}
		str += "\n";
		str += " Number of levels : " + getNumberOfLevels() + "\n";
		str += " Progress type : "+ getProgressiveOrder() + "\n";
		str += " Num. of layers : " + getNumberOfLayers() + "\n";
		str += " Cblk dimension : " + getCodeBlockWidth() + "x" + getCodeBlockHeight() + "\n";

		str += " Filter         : ";
		switch (this.waveletTransformId) {
			case FilterTypes.W9X7_IRREVERSIBLE:
				str += " 9-7 irreversible\n";
				break;
			case FilterTypes.W5X3_REVERSIBLE:
				str += " 5-3 reversible\n";
				break;
		}

		str += " Multi comp transform : " + getMultipleComponenTransform() + "\n";
		if (spcod_ps != null) {
			str += " Precincts      : ";
			for (int i = 0; i < getCodeBlockCount(); i++) {
				str += (1 << getCodeBlockWidthExponentOffset(i)) + "x" + (1 << getCodeBlockHeightExponentOffset(i)) + " ";
			}
		}
		str += "\n";
		return str;
	}

	public int getQmfbid() {
		switch (this.waveletTransformId) {
			case FilterTypes.W9X7_IRREVERSIBLE:
				return 0;
			case FilterTypes.W5X3_REVERSIBLE:
				return 1;
			default:
				throw new IllegalArgumentException("Specified wavelet filter is not JPEG 2000 part I compliant.");
		}
	}

	public int getProgressiveOrder() {
		return this.sgcod_po;
	}

	public int getCodeBlockCount() {
		return this.spcod_ps.length;
	}

	public int getCodeBlockWidthExponentOffset(int index) {
		return (spcod_ps[index] & 0x000F);
	}

	public int getCodeBlockHeightExponentOffset(int index) {
		return ((spcod_ps[index] & 0x00F0) >> 4);
	}

	public int getNumberOfLayers() {
		return this.sgcod_nl;
	}

	public int getNumberOfLevels() {
		return this.spcod_ndl;
	}

	public int getCodeBlockWidth() {
		return (1 << (this.spcod_cw + 2));
	}

	public int getCodeBlockHeight() {
		return (1 << (this.spcod_ch + 2));
	}

	public int getCodingStyle() {
		return this.scod;
	}

	public int getCodeBlockStyle() {
		return this.spcod_cs;
	}
}
