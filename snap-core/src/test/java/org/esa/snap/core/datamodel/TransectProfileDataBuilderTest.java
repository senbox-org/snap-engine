/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import it.geosolutions.jaiext.jts.CoordinateSequence2D;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.geotools.feature.DefaultFeatureCollection;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Storm
 */
public class TransectProfileDataBuilderTest {

    private TransectProfileDataBuilder builder;

    @Before
    public void setUp() throws Exception {
        builder = new TransectProfileDataBuilder();
    }

    @Test
    public void testBuildDefault() throws Exception {
        RasterDataNode dummyBand = createDummyBandWithProduct();
        Shape dummyPath = createDummyPath();
        builder.raster(dummyBand);
        builder.path(dummyPath);

        TransectProfileData data = builder.build();

        assertEquals(1, data.config.boxSize);
        assertSame(dummyBand, data.config.raster);
        assertSame(dummyPath, data.config.path);
        assertTrue(data.config.connectVertices);
        assertNull(data.config.roiMask);
        assertFalse(data.config.useRoiMask);
    }

    @Test
    public void testBuildNonDefaultWithVDN() throws Exception {
        RasterDataNode dummyBand = createDummyBandWithProduct();
        Mask dummyMask = createDummyMask();

        builder.raster(dummyBand);
        builder.pointData(createDummyPoints(new Point2D[]{new Point2D.Double(0, 0), new Point2D.Double(5, 8), new Point2D.Double(19, 4)}));
        builder.boxSize(5);
        builder.roiMask(dummyMask);
        builder.useRoiMask(true);
        builder.connectVertices(false);

        TransectProfileData data = builder.build();

        assertSame(dummyBand, data.config.raster);
        assertNotNull(data.config.path);
        final PathIterator pathIterator = data.config.path.getPathIterator(null);
        final double[] pointData = new double[6];
        pathIterator.currentSegment(pointData);
        assertArrayEquals(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, pointData, 1.0e-6);
        pathIterator.next();
        pathIterator.currentSegment(pointData);
        assertArrayEquals(new double[]{5.0, 8.0, 0.0, 0.0, 0.0, 0.0}, pointData, 1.0e-6);
        pathIterator.next();
        pathIterator.currentSegment(pointData);
        assertArrayEquals(new double[]{19.0, 4.0, 0.0, 0.0, 0.0, 0.0}, pointData, 1.0e-6);
        pathIterator.next();
        assertTrue(pathIterator.isDone());
        assertEquals(5, data.config.boxSize);
        assertSame(dummyMask, data.config.roiMask);
        assertTrue(data.config.useRoiMask);
        assertFalse(data.config.connectVertices);
    }

    @Test
    public void testBuildNonDefaultWithPath() throws Exception {
        RasterDataNode dummyBand = createDummyBandWithProduct();
        Mask dummyMask = createDummyMask();

        builder.raster(dummyBand);
        builder.path(createDummyPath());
        builder.boxSize(13);
        builder.roiMask(dummyMask);
        builder.useRoiMask(true);
        builder.connectVertices(false);

        TransectProfileData data = builder.build();

        assertSame(dummyBand, data.config.raster);
        assertNotNull(data.config.path);
        assertEquals(13, data.config.boxSize);
        assertSame(dummyMask, data.config.roiMask);
        assertTrue(data.config.useRoiMask);
        assertFalse(data.config.connectVertices);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailForMissingRaster() throws Exception {
        builder.path(createDummyPath());
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void testFailForMissingPath() throws Exception {
        builder.raster(createDummyBandWithProduct());
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailForMissingProduct() throws Exception {
        builder.raster(createDummyBandWithoutProduct());
        builder.build();
    }

    private Mask createDummyMask() {
        Mask mask = new Product("dummy", "type", 10, 10).addMask("maskName", Mask.BandMathsType.INSTANCE);
        mask.getImageConfig().setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, "Y <= 1.5");
        return mask;
    }

    private VectorDataNode createDummyPoints(Point2D[] points) {
        final SimpleFeatureType superType = PlainFeatureFactory.createPlainFeatureType("testPoint", Point.class, null);
        final VectorDataNode vdn = new VectorDataNode("dummyPoints", superType);
        final DefaultFeatureCollection featureCollection = vdn.getFeatureCollection();
        final GeometryFactory geomFactory = new GeometryFactory();
        for (int i = 0; i < points.length; i++) {
            Point2D point = points[i];
            final CoordinateSequence2D coordinates = new CoordinateSequence2D(point.getX(), point.getY());
            featureCollection.add(PlainFeatureFactory.createPlainFeature(superType, String.valueOf(i), new Point(coordinates, geomFactory), null));

        }
        return vdn;
    }

    private RasterDataNode createDummyBandWithoutProduct() {
        return new Band("name", ProductData.TYPE_FLOAT32, 10, 10);
    }

    private RasterDataNode createDummyBandWithProduct() {
        final Product product = new Product("pname", "type", 10, 10);
        product.setProductReader(new AbstractProductReader(null) {
            @Override
            protected Product readProductNodesImpl() {
                return product;
            }

            @Override
            protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                                  int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                                  ProductData destBuffer, ProgressMonitor pm) {
            }
        });
        return product.addBand("name", ProductData.TYPE_FLOAT32);
    }

    private Shape createDummyPath() {
        return new Rectangle();
    }
}
