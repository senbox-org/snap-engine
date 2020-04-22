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

import java.io.IOException;

/**
 * Created by jcoravu on 30/4/2019.
 */
public interface IRandomAccessFile {

    public short readShort() throws IOException;

    public int readUnsignedShort() throws IOException;

    public int readInt() throws IOException;

    public long readUnsignedInt() throws IOException;

    public long readLong() throws IOException;

    public float readFloat() throws IOException;

    public double readDouble() throws IOException;

    public long getPosition() throws IOException;

    public long getLength() throws IOException;

    public void seek(long off) throws IOException;

    public int readByte() throws IOException;

    public void read(byte buffer[], int offset, int lenght) throws IOException;
}
