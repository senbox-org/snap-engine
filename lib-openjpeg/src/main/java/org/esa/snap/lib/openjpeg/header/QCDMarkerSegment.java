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
public class QCDMarkerSegment extends AbstractMarkerSegment {

    private int lqcd;
    private int sqcd;
    private int[][] spqcd;

    public QCDMarkerSegment() {
    }

    public int getQuantizationType() {
        return this.sqcd & ~(SQCX_GB_MASK << SQCX_GB_SHIFT);
    }

    public int getNumGuardBits() {
        return (this.sqcd >> SQCX_GB_SHIFT) & SQCX_GB_MASK;
    }

    public int computeNoQuantizationExponent(int rIndex, int sIndex) {
        return (this.spqcd[rIndex][sIndex] >> SQCX_EXP_SHIFT) & SQCX_EXP_MASK;
    }

    public int computeExponent(int rIndex, int sIndex) {
        return (this.spqcd[rIndex][sIndex] >> 11) & 0x1f;
    }

    private double computeMantissa(int rIndex, int sIndex, int exponent) {
        return (-1f - ((float) computeMantissa(rIndex, sIndex)) / (1 << 11)) / (-1 << exponent);
    }

    public int computeMantissa(int rIndex, int sIndex) {
        return (this.spqcd[rIndex][sIndex] & 0x07ff);
    }

    public int getResolutionLevels() {
        return this.spqcd.length;
    }

    public int getSubbandsAtResolutionLevel(int index) {
        return this.spqcd[index].length;
    }

    @Override
    public String toString() {
        String str = "\n --- QCDMarkerSegment (" + this.lqcd + " bytes) ---\n";
        str += " Quantization type    : ";
        int qt = getQuantizationType();
        if (qt == SQCX_NO_QUANTIZATION) {
            str += "No quantization \n";
        } else if (qt == SQCX_SCALAR_DERIVED) {
            str += "Scalar derived\n";
        } else if (qt == SQCX_SCALAR_EXPOUNDED) {
            str += "Scalar expounded\n";
        }
        str += " Guard bits     : " + getNumGuardBits() + "\n";

        if (qt == SQCX_NO_QUANTIZATION) {
            str += " Exponents   :\n";
            int exp;
            for (int i = 0; i < getResolutionLevels(); i++) {
                for (int j = 0; j < getSubbandsAtResolutionLevel(i); j++) {
                    if (i == 0 && j == 0) {
                        exp = computeNoQuantizationExponent(0, 0);//(spqcd[0][0] >> SQCX_EXP_SHIFT) & SQCX_EXP_MASK;
                        str += "\tr=0 : " + exp + "\n";
                    } else if (i != 0 && j > 0) {
                        exp = computeNoQuantizationExponent(i, j);//(spqcd[i][j] >> SQCX_EXP_SHIFT) & SQCX_EXP_MASK;
                        str += "\tr=" + i + ",s=" + j + " : " + exp + "\n";
                    }
                }
            }
        } else {
            str += " Exp / Mantissa : \n";
            int exp;
            double mantissa;
            for (int i = 0; i < getResolutionLevels(); i++) {
                for (int j = 0; j < getSubbandsAtResolutionLevel(i); j++) {
                    if (i == 0 && j == 0) {
                        exp = computeExponent(0, 0);//(spqcd[0][0] >> 11) & 0x1f;
                        mantissa = computeMantissa(0, 0, exp);//(-1f - ((float) (spqcd[0][0] & 0x07ff)) / (1 << 11)) / (-1 << exp);
                        str += "\tr=0 : " + exp + " / " + mantissa + "\n";
                    } else if (i != 0 && j > 0) {
                        exp = computeExponent(i, j);//(spqcd[i][j] >> 11) & 0x1f;
                        mantissa = computeMantissa(i, j, exp);//(-1f - ((float) (spqcd[i][j] & 0x07ff)) / (1 << 11)) / (-1 << exp);
                        str += "\tr=" + i + ",s=" + j + " : " + exp + " / " + mantissa + "\n";
                    }
                }
            }
        }

        str += "\n";
        return str;
    }

    @Override
    public void readData(DataInputStream jp2FileStream) throws IOException {
    }

    public void readData(DataInputStream jp2FileStream, int numberOfLevels) throws IOException {
        // Lqcd (length of QCD field)
        this.lqcd = jp2FileStream.readUnsignedShort();

        // Sqcd (quantization style)
        this.sqcd = jp2FileStream.readUnsignedByte();

        int quantizationType = getQuantizationType();

        // If the main header is being read set default value of dequantization spec
        switch (quantizationType) {
            case SQCX_NO_QUANTIZATION:
                break;
            case SQCX_SCALAR_DERIVED:
                break;
            case SQCX_SCALAR_EXPOUNDED:
                break;
            default:
                throw new InvalidContiguousCodestreamException("Unknown or " + "unsupported " + "quantization style " + "in Sqcd field, QCD " + "marker main header");
        }

        if (quantizationType == SQCX_NO_QUANTIZATION) {
            int maxrl = numberOfLevels;//((Integer) decSpec.dls.getDefault()).intValue();
            int minb, maxb, hpd;
            int tmp;

            int[][] exp = new int[maxrl + 1][];
            this.spqcd = new int[maxrl + 1][4];

            for (int rl = 0; rl <= maxrl; rl++) { // Loop on resolution levels
                // Find the number of subbands in the resolution level
                if (rl == 0) { // Only the LL subband
                    minb = 0;
                    maxb = 1;
                } else {
                    // Dyadic decomposition
                    hpd = 1;
                    // Adapt hpd to resolution level
                    if (hpd > maxrl - rl) {
                        hpd -= maxrl - rl;
                    } else {
                        hpd = 1;
                    }
                    // Determine max and min subband index
                    minb = 1 << ((hpd - 1) << 1); // minb = 4^(hpd-1)
                    maxb = 1 << (hpd << 1); // maxb = 4^hpd
                }
                // Allocate array for subbands in resolution level
                exp[rl] = new int[maxb];

                for (int j = minb; j < maxb; j++) {
                    tmp = this.spqcd[rl][j] = jp2FileStream.readUnsignedByte();
                    exp[rl][j] = (tmp >> SQCX_EXP_SHIFT) & SQCX_EXP_MASK;
                }
            } // end for rl
        } else {
            int maxrl = (quantizationType == SQCX_SCALAR_DERIVED) ? 0 : numberOfLevels;
            int minb, maxb, hpd;
            int tmp;

            int[][] exp = new int[maxrl + 1][];
            float[][] nStep = new float[maxrl + 1][];
            this.spqcd = new int[maxrl + 1][4];

            for (int rl = 0; rl <= maxrl; rl++) { // Loop on resolution levels
                // Find the number of subbands in the resolution level
                if (rl == 0) { // Only the LL subband
                    minb = 0;
                    maxb = 1;
                } else {
                    // Dyadic decomposition
                    hpd = 1;

                    // Adapt hpd to resolution level
                    if (hpd > maxrl - rl) {
                        hpd -= maxrl - rl;
                    } else {
                        hpd = 1;
                    }
                    // Determine max and min subband index
                    minb = 1 << ((hpd - 1) << 1); // minb = 4^(hpd-1)
                    maxb = 1 << (hpd << 1); // maxb = 4^hpd
                }
                // Allocate array for subbands in resolution level
                exp[rl] = new int[maxb];
                nStep[rl] = new float[maxb];

                for (int j = minb; j < maxb; j++) {
                    tmp = this.spqcd[rl][j] = jp2FileStream.readUnsignedShort();
                    exp[rl][j] = (tmp >> 11) & 0x1f;
                    // NOTE: the formula below does not support more than 5
                    // bits for the exponent, otherwise (-1<<exp) might
                    // overflow (the - is used to be able to represent 2**31)
                    nStep[rl][j] = (-1f - ((float) (tmp & 0x07ff)) / (1 << 11)) / (-1 << exp[rl][j]);
                }
            } // end for rl
        } // end if (qType != SQCX_NO_QUANTIZATION)
    }
}
