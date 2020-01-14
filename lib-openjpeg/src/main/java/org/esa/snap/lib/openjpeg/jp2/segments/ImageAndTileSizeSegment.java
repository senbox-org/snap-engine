/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2013-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.lib.openjpeg.jp2.segments;

import org.esa.snap.lib.openjpeg.jp2.MarkerSegment;
import org.esa.snap.lib.openjpeg.jp2.MarkerType;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public class ImageAndTileSizeSegment extends MarkerSegment {
    /**
     * Length of marker segment in bytes (not including the marker). The value of this parameter is determined by the following equation: Lsiz = 38 + 3 * Csiz
     */
    int lsiz;
    /**
     * Denotes capabilities that a decoder needs to properly decode the codestream.
     */
    int rsiz;
    /*
     * Width of the reference grid.
     */
    long xsiz;
    /*
     * Height of the reference grid.
     */
    long ysiz;
    /*
     *   Horizontal offset from the origin of the reference grid to the left side of the image area.
     */
    long xosiz;
    /*
     * Vertical offset from the origin of the reference grid to the top side of the image area.
     */
    long yosiz;
    /*
    *  Width of one reference tile with respect to the reference grid.
    */
    long xtsiz;
    /*
     * Height of one reference tile with respect to the reference grid.
     */
    long ytsiz;

    /*
     * Horizontal offset from the origin of the reference grid to the left side of the first tile.
     */
    long xtosiz;
    /*
     * Vertical offset from the origin of the reference grid to the top side of the first tile.
     */
    long ytosiz;
    /*
     * Number of components in the image.
     */
    int csiz;
    /*
     *  Precision (depth) in bits and sign of the ith component samples.
      *  The precision is the precision of the component samples before DC level shifting is performed
       *  (i.e., the precision of the original component samples before any processing is performed).
       *   There is one occurrence of this parameter for each component.
       *   The order corresponds to the component's index, starting with zero.
     */
    int[] ssiz;
    /*
     *  Horizontal separation of a sample of ith component with respect to the reference grid.
      *  There is one occurrence of this parameter for each component.
     */
    int[] xrsiz;
    /*
     * Vertical separation of a sample of ith component with respect to the reference grid.
     * There is one occurrence of this parameter for each component.
     */
    int[] yrsiz;

    public ImageAndTileSizeSegment(MarkerType markerType) {
        super(markerType);
    }

    public int getLsiz() {
        return lsiz;
    }

    public int getRsiz() {
        return rsiz;
    }

    public long getXsiz() {
        return xsiz;
    }

    public long getYsiz() {
        return ysiz;
    }

    public long getXosiz() {
        return xosiz;
    }

    public long getYosiz() {
        return yosiz;
    }

    public long getXtsiz() {
        return xtsiz;
    }

    public long getYtsiz() {
        return ytsiz;
    }

    public long getXtosiz() {
        return xtosiz;
    }

    public long getYtosiz() {
        return ytosiz;
    }

    public int getCsiz() {
        return csiz;
    }

    public int[] getSsiz() {
        return ssiz;
    }

    public int[] getXRsiz() {
        return xrsiz;
    }

    public int[] getYRsiz() {
        return yrsiz;
    }

    @Override
    public void readFrom(ImageInputStream stream) throws IOException {
        lsiz = stream.readShort() & 0x0000fffff;
        rsiz = stream.readShort() & 0x0000fffff;
        xsiz = stream.readInt() & 0x00000000ffffffffffL;
        ysiz = stream.readInt() & 0x00000000ffffffffffL;
        xosiz = stream.readInt() & 0x00000000ffffffffffL;
        yosiz = stream.readInt() & 0x00000000ffffffffffL;
        xtsiz = stream.readInt() & 0x00000000ffffffffffL;
        ytsiz = stream.readInt() & 0x00000000ffffffffffL;
        xtosiz = stream.readInt() & 0x00000000ffffffffffL;
        ytosiz = stream.readInt() & 0x00000000ffffffffffL;
        csiz = stream.readShort() & 0x0000fffff;
        if (csiz < 1 || csiz > 16384) {
            throw new IOException("Invalid SIZ parameter: Csiz = " + csiz);
        }
        ssiz = new int[csiz];
        xrsiz = new int[csiz];
        yrsiz = new int[csiz];
        for (int i = 0; i < csiz; i++) {
            ssiz[i] = stream.readByte() & 0xff;
            xrsiz[i] = stream.readByte() & 0xff;
            yrsiz[i] = stream.readByte() & 0xff;
        }
    }
}
