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
public class RGNMarkerSegment extends AbstractMarkerSegment {

    private int lrgn;
    private int crgn;
    private int srgn;
    private int sprgn;

    public RGNMarkerSegment() {
    }

    @Override
    public void readData(DataInputStream jp2FileStream) throws IOException {
    }

    public void readData(DataInputStream jp2FileStream, int numComps) throws IOException {
        // Lrgn (marker length)
        this.lrgn = jp2FileStream.readUnsignedShort();

        // Read component
        this.crgn = (numComps < 257) ? jp2FileStream.readUnsignedByte() : jp2FileStream.readUnsignedShort();
        if (this.crgn >= numComps) {
            throw new InvalidContiguousCodestreamException("Invalid component " + "index in RGN marker" + this.crgn);
        }

        // Read type of RGN.(Srgn)
        this.srgn = jp2FileStream.readUnsignedByte();

        // Check that we can handle it.
        if (this.srgn != SRGN_IMPLICIT) {
            throw new InvalidContiguousCodestreamException("Unknown or unsupported " + "Srgn parameter in ROI " + "marker");
        }

        // SPrgn
        this.sprgn = jp2FileStream.readUnsignedByte();
    }

    @Override
    public String toString() {
        String str = "\n --- RGN (" + this.lrgn + " bytes) ---\n";
        str += " Component : " + this.crgn + "\n";
        if (this.srgn == 0) {
            str += " ROI style : Implicit\n";
        } else {
            str += " ROI style : Unsupported\n";
        }
        str += " ROI shift : " + this.sprgn + "\n";
        str += "\n";
        return str;
    }

    public int getROIShift() {
        return this.sprgn; // the region of interest
    }
}
