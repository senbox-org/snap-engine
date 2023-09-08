/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataop.resamp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BiSincInterpolationResamplingTest {

    final Resampling resampling = Resampling.BISINC_5_POINT_INTERPOLATION;
    final TestRaster raster = new TestRaster();

    @Test
    public void testCreateIndex() {
        final Resampling.Index index = resampling.createIndex();
        assertNotNull(index);
        assertNotNull(index.i);
        assertNotNull(index);
        assertNotNull(index.i);
        assertNotNull(index.j);
        assertNotNull(index.ki);
        assertNotNull(index.kj);
        assertEquals(5, index.i.length);
        assertEquals(5, index.j.length);
        assertEquals(1, index.ki.length);
        assertEquals(1, index.kj.length);
    }

    @Test
    public void testIndexAndSample() throws Exception {
        final Resampling.Index index = resampling.createIndex();

        testIndexAndSample(
                index,
                2.2f, 2.3f,
                0.0, 0.0, 1.0, 2.0, 3.0,
                0.0, 0.0, 1.0, 2.0, 3.0,
                0.7f,
                0.8f,
                24.65136f);
    }

    private void testIndexAndSample(
            final Resampling.Index index,
            float x, float y,
            double i1Exp, double i2Exp, double i3Exp, double i4Exp, double i5Exp,
            double j1Exp, double j2Exp, double j3Exp, double j4Exp, double j5Exp,
            float ki1Exp,
            float kj1Exp,
            float sampleExp) throws Exception {

        resampling.computeIndex(x, y, raster.getWidth(), raster.getHeight(), index);

        assertEquals(i1Exp, index.i[0], 1e-8);
        assertEquals(i2Exp, index.i[1], 1e-8);
        assertEquals(i3Exp, index.i[2], 1e-8);
        assertEquals(i4Exp, index.i[3], 1e-8);
        assertEquals(i5Exp, index.i[4], 1e-8);

        assertEquals(j1Exp, index.j[0], 1e-8);
        assertEquals(j2Exp, index.j[1], 1e-8);
        assertEquals(j3Exp, index.j[2], 1e-8);
        assertEquals(j4Exp, index.j[3], 1e-8);
        assertEquals(j5Exp, index.j[4], 1e-8);

        assertEquals(ki1Exp, index.ki[0], 1e-5f);
        assertEquals(kj1Exp, index.kj[0], 1e-5f);

        double sample = resampling.resample(raster, index);
        assertEquals(sampleExp, sample, 1e-5f);
    }

    @Test
    public void testCornerBasedIndex() throws Exception {
        testCornerIndex(2.2f, 2.3f);
    }

    private void testCornerIndex(final float x, final float y) throws Exception {
        final Resampling.Index index = resampling.createIndex();
        resampling.computeCornerBasedIndex(x, y, raster.getWidth(), raster.getHeight(), index);

        final Resampling.Index indexExp = resampling.createIndex();
        computeExpectedIndex(x, y, raster.getWidth(), raster.getHeight(), indexExp);

        assertEquals(indexExp.i[0], index.i[0], 1e-8);
        assertEquals(indexExp.i[1], index.i[1], 1e-8);
        assertEquals(indexExp.i[2], index.i[2], 1e-8);
        assertEquals(indexExp.i[3], index.i[3], 1e-8);
        assertEquals(indexExp.i[4], index.i[4], 1e-8);
        assertEquals(indexExp.j[0], index.j[0], 1e-8);
        assertEquals(indexExp.j[1], index.j[1], 1e-8);
        assertEquals(indexExp.j[2], index.j[2], 1e-8);
        assertEquals(indexExp.j[3], index.j[3], 1e-8);
        assertEquals(indexExp.j[4], index.j[4], 1e-8);
        assertEquals(indexExp.ki[0], index.ki[0], 1e-8);
        assertEquals(indexExp.kj[0], index.kj[0], 1e-8);
    }

    private void computeExpectedIndex(final double x, final double y, final int width, final int height, final Resampling.Index index) {
        index.x = x;
        index.y = y;
        index.width = width;
        index.height = height;


        final int i0 = (int) Math.floor(x);
        final int j0 = (int) Math.floor(y);

        final int iMax = width - 1;
        final int jMax = height - 1;

        index.i0 = i0;
        index.j0 = j0;

        for (int i = 0; i < 5; i++) {
            index.i[i] = Math.min(Math.max(i0 - 2 + i, 0), iMax);
        }
        index.ki[0] = x - i0;
        for (int j = 0; j < 5; j++) {
            index.j[j] = Math.min(Math.max(j0 - 2 + j, 0), jMax);
        }
        index.kj[0] = y - j0;
    }
}
