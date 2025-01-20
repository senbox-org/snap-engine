/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataio;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;

import java.awt.*;
import java.util.HashMap;

import static org.junit.Assert.*;

public class ProductSubsetDefTest {

    private static final float EPS = 1e-5f;
    private ProductSubsetDef _subset;

    @Before
    public void setUp() {
        _subset = new ProductSubsetDef("undefined");
    }

    @Test
    public void testAddBandName() {
        String[] names;

        //at start getNodeNames must return null
        names = _subset.getNodeNames();
        assertNull("names must be null", names);

        //null parameter throws IllegalArgumentException
        //getNodeNames must still return null
        try {
            _subset.addNodeName(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
        names = _subset.getNodeNames();
        assertNull("names must be null", names);

        //enpty String parameter throws IllegalArgumentException
        //getNodeNames must still return null
        try {
            _subset.addNodeName("");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
        names = _subset.getNodeNames();
        assertNull("names must be null", names);

        //adds first band name and getNodeNames must return a filled
        //String[] with one entry
        _subset.addNodeName("band1");
        names = _subset.getNodeNames();
        assertEquals("length must be 1", 1, names.length);
        assertEquals("Index 1 contains", "band1", names[0]);

        //adds second band name and getNodeNames must return a filled
        //String[] with two entrys
        _subset.addNodeName("band2");
        names = _subset.getNodeNames();
        assertEquals("length must be 2", 2, names.length);
        assertEquals("Index 1 contains", "band1", names[0]);
        assertEquals("Index 1 contains", "band2", names[1]);

        //existing band should not be added and getNodeNames must
        //return a filled String[] with two entrys
        _subset.addNodeName("band2");
        names = _subset.getNodeNames();
        assertEquals("length must be 2", 2, names.length);
        assertEquals("Index 1 contains", "band1", names[0]);
        assertEquals("Index 1 contains", "band2", names[1]);
    }

    @Test
    public void testSetBandNames() {
        String[] names;

        try {
            //must NOT throw an IllegalArgumentException
            _subset.setNodeNames(null);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException expected");
        }

        // must add only two entries because band2 are twice String[]
        _subset.setNodeNames(new String[]{"band1", "band2", "band2"});
        names = _subset.getNodeNames();
        assertEquals("length must be two", 2, names.length);
        assertEquals("expected Name", "band1", names[0]);
        assertEquals("expected Name", "band2", names[1]);
    }

    @Test
    public void testRemoveBandName() {
        String[] names;
        _subset.setNodeNames(new String[]{"band1", "band2"});
        names = _subset.getNodeNames();
        assertEquals("length must be two", 2, names.length);
        assertEquals("expected Name", "band1", names[0]);
        assertEquals("expected Name", "band2", names[1]);

        // removeNodeName "band1"
        assertTrue(_subset.removeNodeName("band1"));
        // second remove returns false because band1 already removed
        assertFalse(_subset.removeNodeName("band1"));
        names = _subset.getNodeNames();
        assertEquals("length must be two", 1, names.length);
        assertEquals("expected Name", "band2", names[0]);

        // removeNodeName "band2"
        assertTrue(_subset.removeNodeName("band2"));
        assertNull("subset must be null", _subset.getNodeNames());
    }

    @Test
    public void testGetAndSetRegion() {
        assertNull("initially, getRegion() should return null", _subset.getRegion());

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(20, 30, 25, 35), 0));
        assertNotNull(_subset.getRegion());
        assertEquals(20, _subset.getRegion().x);
        assertEquals(30, _subset.getRegion().y);
        assertEquals(25, _subset.getRegion().width);
        assertEquals(35, _subset.getRegion().height);

        _subset.setSubsetRegion(new PixelSubsetRegion(40, 45, 50, 55, 0));
        assertNotNull(_subset.getRegion());
        assertEquals(40, _subset.getRegion().x);
        assertEquals(45, _subset.getRegion().y);
        assertEquals(50, _subset.getRegion().width);
        assertEquals(55, _subset.getRegion().height);

        // Check that getRegion() returns new rectangle instances each time it is called
        assertNotSame(_subset.getRegion(), _subset.getRegion());

        // reset subset region
        _subset.setSubsetRegion(null);
        assertNull(_subset.getRegion());

        // IllegalArgumentException if x is negative
        try {
            _subset.setSubsetRegion(new PixelSubsetRegion(-1, 2, 3, 4, 0));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if y is negative
        try {
            _subset.setSubsetRegion(new PixelSubsetRegion(1, -1, 3, 4, 0));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if width is zero
        try {
            _subset.setSubsetRegion(new PixelSubsetRegion(1, 2, 0, 4, 0));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if height is zero
        try {
            _subset.setSubsetRegion(new PixelSubsetRegion(1, 2, 3, 0, 0));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if width is negative
        try {
            _subset.setSubsetRegion(new PixelSubsetRegion(1, 2, -1, 4, 0));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if height is negative
        try {
            _subset.setSubsetRegion(new PixelSubsetRegion(1, 2, 3, -1, 0));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if bad values in Rectangle
        try {
            _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(12, 2, 3, -1), 0));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    @STTM("SNAP-369,SNAP-1608")
    public void testGetAndSetPolygon() {
        assertNull("initially, getSubsetPolygon() should return null", _subset.getSubsetPolygon());

        final Coordinate[] productPolygonCoordinates = new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(0, 10),
                new Coordinate(10, 0),
                new Coordinate(0, 0),
        };
        final GeometryFactory geometryFactory = new GeometryFactory();
        final org.locationtech.jts.geom.Polygon subsetPolygon = geometryFactory.createPolygon(geometryFactory.createLinearRing(productPolygonCoordinates), new LinearRing[0]);
        _subset.setSubsetPolygon(subsetPolygon);
        assertNotNull(_subset.getSubsetPolygon());
        assertEquals("POLYGON ((0 0, 0 10, 10 0, 0 0))", _subset.getSubsetPolygon().toText());
    }

    @Test
    public void testGetAndSetSubSampling() {
        assertEquals("initially, getSubSamplingX() should return 1", 1.0f, _subset.getSubSamplingX(), EPS);
        assertEquals("initially, getSubSamplingY() should return 1", 1.0f, _subset.getSubSamplingY(), EPS);

        _subset.setSubSampling(1, 10);
        assertEquals(1.0f, _subset.getSubSamplingX(), EPS);
        assertEquals(10.0f, _subset.getSubSamplingY(), EPS);

        _subset.setSubSampling(10, 1);
        assertEquals(10.0f, _subset.getSubSamplingX(), EPS);
        assertEquals(1.0f, _subset.getSubSamplingY(), EPS);

        // The value to be left unchanged in the following
        _subset.setSubSampling(11, 17);

        // IllegalArgumentException if x is less than 1
        try {
            _subset.setSubSampling(0, 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertEquals(11.0f, _subset.getSubSamplingX(), EPS);
            assertEquals(17.0f, _subset.getSubSamplingY(), EPS);
        }

        // IllegalArgumentException if y is less than 1
        try {
            _subset.setSubSampling(1, 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertEquals(11.0f, _subset.getSubSamplingX(), EPS);
            assertEquals(17.0f, _subset.getSubSamplingY(), EPS);
        }
    }

    @Test
    public void testGetRasterSize() {
        _subset.setSubSampling(1, 1);
        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 1, 1), 0));
        assertEquals(new Dimension(1, 1), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 9, 9), 0));
        assertEquals(new Dimension(9, 9), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 10, 10), 0));
        assertEquals(new Dimension(10, 10), _subset.getSceneRasterSize(100, 100));

        _subset.setSubSampling(2, 2);

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 1, 1), 0));
        assertEquals(new Dimension(1, 1), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 90, 9), 0));
        assertEquals(new Dimension(45, 5), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 100, 10), 0));
        assertEquals(new Dimension(50, 5), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 110, 11), 0));
        assertEquals(new Dimension(50, 6), _subset.getSceneRasterSize(100, 100));

        _subset.setSubSampling(3, 3);

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 10, 1), 0));
        assertEquals(new Dimension(4, 1), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 90, 9), 0));
        assertEquals(new Dimension(30, 3), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 100, 10), 0));
        assertEquals(new Dimension(34, 4), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 110, 11), 0));
        assertEquals(new Dimension(34, 4), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 120, 12), 0));
        assertEquals(new Dimension(34, 4), _subset.getSceneRasterSize(100, 100));

        _subset.setSubsetRegion(new PixelSubsetRegion(new Rectangle(0, 0, 130, 13), 0));
        assertEquals(new Dimension(34, 5), _subset.getSceneRasterSize(100, 100));
    }

    @Test
    public void testMetadataIgnored() {
        ProductSubsetDef subsetInfo = new ProductSubsetDef("undefined");
        //after creation isIgnoreMetadata must be false
        assertFalse(subsetInfo.isIgnoreMetadata());
        subsetInfo.setIgnoreMetadata(true);
        assertTrue(subsetInfo.isIgnoreMetadata());
        subsetInfo.setIgnoreMetadata(false);
        assertFalse(subsetInfo.isIgnoreMetadata());
    }

    @Test
    public void testSetValidSubsetRegionMaps() {
        ProductSubsetDef subsetDef = new ProductSubsetDef();
        HashMap<String, Rectangle> map = new HashMap<>();
        Rectangle validRectangle = new Rectangle(0,0,4,23);
        Rectangle invalidRectangle1 = new Rectangle(0,0,0,0);
        Rectangle invalidRectangle2 = new Rectangle(0,0,4,0);
        Rectangle invalidRectangle3 = new Rectangle(0,0,0,23);

        map.put("valid_rectangle", validRectangle);
        map.put("invalid_rectangle1", invalidRectangle1);
        map.put("invalid_rectangle2", invalidRectangle2);
        map.put("invalid_rectangle3", invalidRectangle3);

        subsetDef.setRegionMap(map);
        subsetDef.setNodeNames(new String[] {"valid_rectangle", "invalid_rectangle1", "invalid_rectangle2", "invalid_rectangle3"});
        HashMap<String, Rectangle> updatedMap = subsetDef.getRegionMap();
        String[] nodeNames = subsetDef.getNodeNames();

        assertEquals(4, updatedMap.size());
        assertEquals(4, nodeNames.length);

        subsetDef.setValidSubsetRegionMaps();

        assertEquals(1, updatedMap.size());
        assertTrue(updatedMap.containsKey("valid_rectangle"));
        assertEquals(validRectangle, updatedMap.get("valid_rectangle"));

        nodeNames = subsetDef.getNodeNames();
        assertEquals(1, nodeNames.length);
        assertEquals("valid_rectangle", nodeNames[0]);
    }
}
