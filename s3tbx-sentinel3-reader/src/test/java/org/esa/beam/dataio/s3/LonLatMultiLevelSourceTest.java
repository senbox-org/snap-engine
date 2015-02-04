package org.esa.beam.dataio.s3;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.dataio.s3.LonLatFunction;
import org.esa.beam.dataio.s3.LonLatMultiLevelSource;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class LonLatMultiLevelSourceTest {

    @Test
    @Ignore
    public void testCreate_WithNullSources() throws Exception {
        final MultiLevelSource lonSource = new TestMultiLevelSource(10, 10);
        final MultiLevelSource latSource = new TestMultiLevelSource(10, 10);
        final LonLatFunction function = new TestLonLatFunction();
        try {
            LonLatMultiLevelSource.create(null, latSource, function, DataBuffer.TYPE_DOUBLE);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            LonLatMultiLevelSource.create(lonSource, null, function, DataBuffer.TYPE_DOUBLE);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    @Ignore
    public void testCreate_WithNullFunction() throws Exception {
        final MultiLevelSource lonSource = new TestMultiLevelSource(10, 10);
        final MultiLevelSource latSource = new TestMultiLevelSource(10, 10);
        final LonLatFunction function = null;
        try {
            LonLatMultiLevelSource.create(lonSource, latSource, function, DataBuffer.TYPE_DOUBLE);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    @Ignore
    public void testCreate_WithWrongDataType() throws Exception {
        final MultiLevelSource lonSource = new TestMultiLevelSource(10, 10);
        final MultiLevelSource latSource = new TestMultiLevelSource(10, 10);
        final LonLatFunction function = new TestLonLatFunction();
        try {
            LonLatMultiLevelSource.create(lonSource, latSource, function, DataBuffer.TYPE_BYTE);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    @Ignore
    public void testCreate_WithIncompatibleSources() throws Exception {
        final MultiLevelSource lonSource = new TestMultiLevelSource(10, 10);
        final MultiLevelSource latSource = new TestMultiLevelSource(11, 11);
        final LonLatFunction function = new TestLonLatFunction();
        try {
            LonLatMultiLevelSource.create(lonSource, latSource, function, DataBuffer.TYPE_BYTE);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    @Ignore
    public void testCreate() throws Exception {
        final MultiLevelSource lonSource = new TestMultiLevelSource(10, 10);
        final MultiLevelSource latSource = new TestMultiLevelSource(10, 10);
        final LonLatFunction function = new TestLonLatFunction();

        assertNotNull(LonLatMultiLevelSource.create(lonSource, latSource, function, DataBuffer.TYPE_DOUBLE));
        assertNotNull(LonLatMultiLevelSource.create(lonSource, latSource, function, DataBuffer.TYPE_FLOAT));
    }


    private static class TestMultiLevelSource extends AbstractMultiLevelSource {

        private TestMultiLevelSource(int width, int height) {
            super(new DefaultMultiLevelModel(new AffineTransform(), width, height));
        }

        @Override
        protected RenderedImage createImage(int level) {
            return null;
        }
    }

    private static class TestLonLatFunction implements LonLatFunction {

        @Override
        public double getValue(double lon, double lat) {
            return 0.0;
        }
    }
}
