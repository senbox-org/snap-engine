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
 * Created by jcoravu on 10/5/2019.
 */
public class SIZMarkerSegment extends AbstractMarkerSegment {

	private int lsiz;
	private int rsiz;
	private int xsiz;
	private int ysiz;
	private int x0siz;
	private int y0siz;
	private int xtsiz;
	private int ytsiz;
	private int xt0siz;
	private int yt0siz;
	private int csiz;
	private int[] ssiz;
	private int[] xrsiz;
	private int[] yrsiz;

	/** Component widths */
	private int[] compWidth;
	/** Component heights */
	private int[] compHeight;
	private boolean[] origSigned;
	private int[] origBitDepth;

	public SIZMarkerSegment() {
	}
	
	@Override
	public void readData(DataInputStream jp2FileStream) throws IOException {
		// Read the length of SIZ marker segment (Lsiz)
		this.lsiz = jp2FileStream.readUnsignedShort();

		// Read the capability of the codestream (Rsiz)
		this.rsiz = jp2FileStream.readUnsignedShort();
		if (this.rsiz > 2) {
			throw new Error("Codestream capabilities not JPEG 2000 - Part I" + " compliant");
		}

		// Read image size
		this.xsiz = jp2FileStream.readInt();
		this.ysiz = jp2FileStream.readInt();
		if (this.xsiz <= 0 || this.ysiz <= 0) {
			throw new IOException("JJ2000 does not support images whose " + "width and/or height not in the " + "range: 1 -- (2^31)-1");
		}

		// Read image offset
		this.x0siz = jp2FileStream.readInt();
		this.y0siz = jp2FileStream.readInt();
		if (this.x0siz < 0 || this.y0siz < 0) {
			throw new IOException("JJ2000 does not support images offset " + "not in the range: 0 -- (2^31)-1");
		}

		// Read size of tile
		this.xtsiz = jp2FileStream.readInt();
		this.ytsiz = jp2FileStream.readInt();
		if (this.xtsiz <= 0 || this.ytsiz <= 0) {
			throw new IOException("JJ2000 does not support tiles whose " + "width and/or height are not in  "
					+ "the range: 1 -- (2^31)-1");
		}

		// Read upper-left tile offset
		this.xt0siz = jp2FileStream.readInt();
		this.yt0siz = jp2FileStream.readInt();
		if (this.xt0siz < 0 || this.yt0siz < 0) {
			throw new IOException("JJ2000 does not support tiles whose " + "offset is not in  " + "the range: 0 -- (2^31)-1");
		}

		// Read number of components and initialize related arrays
		int nComp = this.csiz = jp2FileStream.readUnsignedShort();
		if (nComp < 1 || nComp > 16384) {
			throw new IllegalArgumentException("Number of component out of " + "range 1--16384: " + nComp);
		}

		this.ssiz = new int[nComp];
		this.xrsiz = new int[nComp];
		this.yrsiz = new int[nComp];

		// Read bit-depth and down-sampling factors of each component
		for (int i = 0; i < nComp; i++) {
			this.ssiz[i] = jp2FileStream.readUnsignedByte();
			this.xrsiz[i] = jp2FileStream.readUnsignedByte();
			this.yrsiz[i] = jp2FileStream.readUnsignedByte();
		}
	}
	
	public int getCompImgWidth(int c) {
		if (compWidth == null) {
			compWidth = new int[csiz];
			for (int cc = 0; cc < csiz; cc++) {
				compWidth[cc] = (int) (Math.ceil((xsiz) / (double) xrsiz[cc])
						- Math.ceil(x0siz / (double) xrsiz[cc]));
			}
		}
		return compWidth[c];
	}

	public int getCompImgHeight(int c) {
		if (compHeight == null) {
			compHeight = new int[csiz];
			for (int cc = 0; cc < csiz; cc++) {
				compHeight[cc] = (int) (Math.ceil((ysiz) / (double) yrsiz[cc])
						- Math.ceil(y0siz / (double) yrsiz[cc]));
			}
		}
		return compHeight[c];
	}

	public int computeNumTiles() {
		return computeNumTilesX() * computeNumTilesY();
	}

	public int computeNumTilesY() {
			return ((ysiz - yt0siz + ytsiz - 1) / ytsiz);
	}

	public int computeNumTilesX() {
		return ((xsiz - xt0siz + xtsiz - 1) / xtsiz);
	}

	public boolean isComponentOriginSignedAt(int componentIndex) {
		if (origSigned == null) {
			origSigned = new boolean[csiz];
			for (int cc = 0; cc < csiz; cc++) {
				origSigned[cc] = ((ssiz[cc] >>> SSIZ_DEPTH_BITS) == 1);
			}
		}
		return origSigned[componentIndex];
	}

	public int getComponentOriginBitDepthAt(int componentIndex) {
		if (origBitDepth == null) {
			origBitDepth = new int[csiz];
			for (int cc = 0; cc < csiz; cc++) {
				origBitDepth[cc] = (ssiz[cc] & ((1 << SSIZ_DEPTH_BITS) - 1)) + 1;
			}
		}
		return origBitDepth[componentIndex];
	}

	public final int getTileLeftX() {
		return this.xt0siz;
	}

	public final int getTileTopY() {
		return this.yt0siz;
	}

	public final int getImageLeftX() {
		return this.x0siz;
	}

	public final int getImageTopY() {
		return this.y0siz;
	}

	public final int getImageWidth() {
		return this.xsiz - this.x0siz;
	}

	public final int getImageHeight() {
		return this.ysiz - this.y0siz;
	}

	public final int getNumComps() {
		return this.csiz;
	}

	public final int getNominalTileWidth() {
		return this.xtsiz;
	}

	public final int getNominalTileHeight() {
		return this.ytsiz;
	}

	@Override
	public String toString() {
		String str = "\n --- SIZ (" + lsiz + " bytes) ---\n";
		str += " Capabilities : " + rsiz + "\n";
		str += " Image dim.   : " + getImageWidth() + "x" + getImageHeight() + ", (offset=" + x0siz + "," + y0siz + ")\n";
		str += " Tile dim.    : " + getNominalTileWidth() + "x" + getNominalTileHeight() + ", (offset=" + xt0siz + "," + yt0siz + ")\n";
		str += " Component(s) : " + getNumComps() + "\n";
		str += " Orig. depth  : ";
		for (int i = 0; i < getNumComps(); i++) {
			str += getComponentOriginBitDepthAt(i) + " ";
		}
		str += "\n";
		str += " Orig. signed : ";
		for (int i = 0; i < getNumComps(); i++) {
			str += isComponentOriginSignedAt(i) + " ";
		}
		str += "\n";
		str += " Subs. factor : ";
		for (int i = 0; i < getNumComps(); i++) {
			str += getComponentDxAt(i) + "," + getComponentDyAt(i) + " ";
		}
		str += "\n";
		return str;
	}

	public int getComponentDxAt(int componentIndex) {
		return this.xrsiz[componentIndex];
	}

	public int getComponentDyAt(int componentIndex) {
		return this.yrsiz[componentIndex];
	}
}
