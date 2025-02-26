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
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.DummyImageInputStream;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.TreeNode;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;

public class AbstractProductReaderTest {

    private AbstractProductReader reader;

    @Test
    public void testParseTileSize() {
        assertNull(AbstractProductReader.parseTileSize(null, 1024));
        assertEquals(Integer.valueOf(1024), AbstractProductReader.parseTileSize("*", 1024));
        assertEquals(Integer.valueOf(256), AbstractProductReader.parseTileSize("256", 1024));
        assertNull(AbstractProductReader.parseTileSize("ten", 1024));
    }

    @Test
    public void testGetConfiguredTileSize_PrefSizeSet() {
        Product product = new Product("a", "b", 1121, 9281);
        product.setPreferredTileSize(256, 512);
        assertNull(AbstractProductReader.getConfiguredTileSize(product, null, null));
        assertEquals(new Dimension(1121, 512), AbstractProductReader.getConfiguredTileSize(product, "*", null));
        assertEquals(new Dimension(32, 512), AbstractProductReader.getConfiguredTileSize(product, "32", null));
        assertEquals(new Dimension(256, 9281), AbstractProductReader.getConfiguredTileSize(product, null, "*"));
        assertEquals(new Dimension(1121, 9281), AbstractProductReader.getConfiguredTileSize(product, "*", "*"));
        assertEquals(new Dimension(32, 9281), AbstractProductReader.getConfiguredTileSize(product, "32", "*"));
        assertEquals(new Dimension(256, 64), AbstractProductReader.getConfiguredTileSize(product, null, "64"));
        assertEquals(new Dimension(1121, 64), AbstractProductReader.getConfiguredTileSize(product, "*", "64"));
        assertEquals(new Dimension(32, 64), AbstractProductReader.getConfiguredTileSize(product, "32", "64"));
    }

    @Test
    public void testGetConfiguredTileSize_PrefSizeNotSet() {
        Product product = new Product("a", "b", 1121, 9281);
        assertNull(AbstractProductReader.getConfiguredTileSize(product, null, null));
        assertEquals(new Dimension(1121, 1121), AbstractProductReader.getConfiguredTileSize(product, "*", null));
        assertEquals(new Dimension(32, 32), AbstractProductReader.getConfiguredTileSize(product, "32", null));
        assertEquals(new Dimension(1121, 9281), AbstractProductReader.getConfiguredTileSize(product, null, "*"));
        assertEquals(new Dimension(1121, 9281), AbstractProductReader.getConfiguredTileSize(product, "*", "*"));
        assertEquals(new Dimension(32, 9281), AbstractProductReader.getConfiguredTileSize(product, "32", "*"));
        assertEquals(new Dimension(64, 64), AbstractProductReader.getConfiguredTileSize(product, null, "64"));
        assertEquals(new Dimension(1121, 64), AbstractProductReader.getConfiguredTileSize(product, "*", "64"));
        assertEquals(new Dimension(32, 64), AbstractProductReader.getConfiguredTileSize(product, "32", "64"));
    }

    @Test
    public void testGetProductComponents_inputFile() throws IOException {
        URL location = AbstractProductReaderTest.class.getProtectionDomain().getCodeSource().getLocation();
        File file = new File(location.getFile());

        reader.readProductNodes(file, null);

        TreeNode<File> productComponents = reader.getProductComponents();
        assertNotNull(productComponents);

        File parent = file.getParentFile();
        assertEquals(parent.getName(), productComponents.getId());
        assertEquals(parent, productComponents.getContent());

        TreeNode<File>[] treeNodes = productComponents.getChildren();
        assertEquals(1, treeNodes.length);
        assertEquals(file.getName(), treeNodes[0].getId());
        assertEquals(file, treeNodes[0].getContent());
    }

    @Test
    public void testGetProductComponents_inputString() throws IOException {
        URL location = AbstractProductReaderTest.class.getProtectionDomain().getCodeSource().getLocation();

        reader.readProductNodes(location.getFile(), null);

        File file = new File(location.getFile());
        TreeNode<File> productComponents = reader.getProductComponents();
        assertNotNull(productComponents);

        File parent = file.getParentFile();
        assertEquals(parent.getName(), productComponents.getId());
        assertEquals(parent, productComponents.getContent());

        TreeNode<File>[] treeNodes = productComponents.getChildren();
        assertEquals(1, treeNodes.length);
        assertEquals(file.getName(), treeNodes[0].getId());
        assertEquals(file, treeNodes[0].getContent());
    }

    @Test
    public void testGetProductComponents_unsupportedInputObject() throws IOException {
        reader.readProductNodes(new DummyImageInputStream(), null);

        TreeNode<File> productComponents = reader.getProductComponents();
        assertNull(productComponents);
    }

    @Test
    @STTM("SNAP-369,SNAP-1608")
    public void testReadProductNodes_subsetByPolygon() throws IOException {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setSubsetRegion(new PixelSubsetRegion(0, 0, 2, 4, 0));
        final Product product = reader.readProductNodes(new Object(), subsetDef);
        assertEquals(AbstractProductReaderTest.TestProductReader.class, product.getProductReader().getClass());
        assertEquals(3,product.getSceneRasterWidth());
        assertEquals(5, product.getSceneRasterHeight());
        final Coordinate[] productPolygonCoordinates = new Coordinate[]{
                new Coordinate(1, 1),
                new Coordinate(1, 3),
                new Coordinate(2, 1),
                new Coordinate(1, 1),
        };
        final GeometryFactory geometryFactory = new GeometryFactory();
        final org.locationtech.jts.geom.Polygon subsetPolygon = geometryFactory.createPolygon(geometryFactory.createLinearRing(productPolygonCoordinates), new LinearRing[0]);
        subsetDef.setSubsetPolygon(subsetPolygon);
        final Product productSub = reader.readProductNodes(new Object(), subsetDef);
        assertEquals(ProductSubsetBuilder.class, productSub.getProductReader().getClass());
        assertEquals(2,productSub.getSceneRasterWidth());
        assertEquals(4, productSub.getSceneRasterHeight());
    }

    @Before
    public void setUp() throws Exception {
        reader = new TestProductReader();
    }

    private class TestProductReader extends AbstractProductReader {

        private TestProductReader() {
            super(null);
        }

        @Override
        protected Product readProductNodesImpl() throws IOException {
            return new Product("test", "what", 3, 5);
        }

        @Override
        protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        }
    }
}
