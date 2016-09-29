/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.owt;

import org.junit.Test;

import static org.junit.Assert.*;

public class HyperspectralAuxdataFactoryTest {

    private static final float[] HYPER_WAVELENGTHS = new float[]{
            400, 403, 406, 409, 412, 415, 418, 421, 424, 427, 430, 433, 436, 439, 442,
            445, 448, 451, 454, 457, 460, 463, 466, 469, 472, 475, 478, 481, 484, 487,
            490, 493, 496, 499, 502, 505, 508, 511, 514, 517, 520, 523, 526, 529, 532,
            535, 538, 541, 544, 547, 550, 553, 556, 559, 562, 565, 568, 571, 574, 577,
            580, 583, 586, 589, 592, 595, 598, 601, 604, 607, 610, 613, 616, 619, 622,
            625, 628, 631, 634, 637, 640, 643, 646, 649, 652, 655, 658, 661, 664, 667,
            670, 673, 676, 679, 682, 685, 688, 691, 694, 697, 700, 703, 706, 709, 712,
            715, 718, 721, 724, 727, 730, 733, 736, 739, 742, 745, 748, 751, 754, 757,
            760, 763, 766, 769, 772, 775, 778, 781, 784, 787, 790, 793, 796, 799
    };

    @Test
    public void testReduceSpectralMeans() throws Exception {
        double[][] spectralMeans = new double[][]{
                {0, 0}, {0, 1}, {0, 2}, {0, 3},
                {0, 4}, {0, 5}, {0, 6}, {0, 7},
                {0, 8}, {0, 9}, {0, 10}, {0, 11},
                {0, 12}, {0, 13}, {0, 14}, {0, 15}, {0, 16},
        };
        double[][] actualArray = HyperspectralAuxdataFactory.reduceSpectralMeansToWLs(spectralMeans, new int[]{0, 1, 2, 3, 7, 8, 9, 13, 14, 16});
        double[][] expectedArray = new double[][]{
                {0, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 7},
                {0, 8}, {0, 9}, {0, 13}, {0, 14}, {0, 16},
        };
        assertArrayEquals(expectedArray, actualArray);
    }

    @Test
    public void testReduceCovarianceMatrices() throws Exception {
        double[][] inputData = new double[][]{
                {1, 2, 3, 4, 5, 6, 7, 8},
                {2, 3, 4, 5, 6, 7, 8, 9},
                {3, 4, 5, 6, 7, 8, 9, 10},
                {4, 5, 6, 7, 8, 9, 10, 11},
                {5, 6, 7, 8, 9, 10, 11, 12},
                {6, 7, 8, 9, 10, 11, 12, 13},
                {7, 8, 9, 10, 11, 12, 13, 14},
                {8, 9, 10, 11, 12, 13, 14, 15},
        };
        double[][][] matrices = new double[3][][];
        matrices[0] = inputData;
        matrices[1] = inputData;
        matrices[2] = inputData;
        double[][][] actualArray = HyperspectralAuxdataFactory.reduceCovarianceMatrixToWLs(matrices, new int[]{0, 1, 3, 6});
        double[][] expectedData = new double[][]{
                {1, 2, 4, 7},
                {2, 3, 5, 8},
                {4, 5, 7, 10},
                {7, 8, 10, 13},
        };

        double[][][] expectedArray = new double[3][][];
        expectedArray[0] = expectedData;
        expectedArray[1] = expectedData;
        expectedArray[2] = expectedData;

        assertArrayEquals(expectedArray, actualArray);
    }

    @Test
    public void testFindBestMatchingWavelength_exactValues() {
        int[] actualIndices = HyperspectralAuxdataFactory.findWavelengthIndices(new float[]{400, 427, 490, 556, 667, 799},
                                                                                HYPER_WAVELENGTHS, 1.5f);
        int[] expectedIndices = new int[]{0, 9, 30, 52, 89, 133};

        assertArrayEquals(expectedIndices, actualIndices);
    }

    @Test
    public void testFindBestMatchingWavelength_inbetweenValues() {
        int[] actualIndices = HyperspectralAuxdataFactory.findWavelengthIndices(new float[]{400.3f, 428.1f, 488.6f, 557.49f, 667, 799},
                                                                                HYPER_WAVELENGTHS, 1.5f);
        int[] expectedIndices = new int[]{0, 9, 30, 52, 89, 133};

        assertArrayEquals(expectedIndices, actualIndices);
    }

    @Test(expected = IllegalStateException.class)
    public void testFindBestMatchingWavelength_BelowMinimum() {
        HyperspectralAuxdataFactory.findWavelengthIndices(new float[]{360}, HYPER_WAVELENGTHS, 1.5f);
    }

    @Test(expected = IllegalStateException.class)
    public void testFindBestMatchingWavelength_AboveMaximum() {
        HyperspectralAuxdataFactory.findWavelengthIndices(new float[]{850}, HYPER_WAVELENGTHS, 1.5f);
    }

//    @Test
//    public void testInvCovMatrix_Glass() throws AuxdataFactory.Exception {
//        double[] timGlassValues = {
//                4893410.48415868,
//                -10224103.9474018,
//                6700192.28488435,
//                -1308481.68310133,
//                445410.221755202,
//                -2266801.94484215,
//                1295478.0526651,
//                1695980.80303018,
//                -901530.436612556
//        };
//
//        auxdataFactory = OWT_TYPE.GLASS_5C.getAuxdataFactory();
//        auxdata = auxdataFactory.createAuxdata();
//        double[][][] glass5c = auxdata.getInvertedCovarianceMatrices();
//        assertArrayEquals(timGlassValues, glass5c[0][0], 1.0e-6) ;
//    }

    @Test
    public void testInvCovMatrix_InlandNoBlue() throws AuxdataException {
        double[] timInlandValues = {
                687175.231134958,
                -1335855.38968399,
                713860.185133806,
                317120.902151734,
                -1619201.81830567,
                2415910.20286054,
                -654291.322596574,
                -148146.956167948,
                -114210.464633796
        };
        AuxdataFactory auxdataFactory = OWT_TYPE.INLAND_NO_BLUE_BAND.getAuxdataFactory();
        Auxdata auxdata = auxdataFactory.createAuxdata();
        double[][][] inlandNoBlue = auxdata.getInvertedCovarianceMatrices();
        assertArrayEquals(timInlandValues, inlandNoBlue[0][0], 1.0e-6);

    }
}

