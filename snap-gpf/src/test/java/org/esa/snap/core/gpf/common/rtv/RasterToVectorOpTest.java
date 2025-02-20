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
package org.esa.snap.core.gpf.common.rtv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GcpGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.gpf.GPF;
import org.junit.Test;

public class RasterToVectorOpTest {

	@Test
	public void testOperatorExecutionOnePolygon() throws Exception {
        final Product sp = createTestProduct(100, 100);
        
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("bandName", "raster_1");

        Product tp = GPF.createProduct("Raster-To-Vector", parameters, sp);

        Band band = tp.getBandAt(0);
        band.readRasterDataFully();
        
        ProductNodeGroup<VectorDataNode> vectorNode = tp.getVectorDataGroup();
        
        assertTrue(vectorNode.contains("shapes"));
        
        VectorDataNode vdn = vectorNode.get("shapes");
        assertEquals(1, vdn.getFeatureCollection().size());
	}
	
	@Test
	public void testOperatorExecutionTwoPolygons() throws Exception {
        final Product sp = createTestProduct(100, 100);

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("bandName", "raster_2");

        Product tp = GPF.createProduct("Raster-To-Vector", parameters, sp);
		
        Band band = tp.getBandAt(0);
        band.readRasterDataFully();
        
        ProductNodeGroup<VectorDataNode> vectorNode = tp.getVectorDataGroup();
        
        assertTrue(vectorNode.contains("shapes"));
        
        VectorDataNode vdn = vectorNode.get("shapes");
        assertEquals(2, vdn.getFeatureCollection().size());
	}

	@Test
	public void testOperatorExecutionFourPolygons() throws Exception {
        final Product sp = createTestProduct(100, 100);

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("bandName", "raster_3");

        Product tp = GPF.createProduct("Raster-To-Vector", parameters, sp);
       
        Band band = tp.getBandAt(0);
        band.readRasterDataFully();

        ProductNodeGroup<VectorDataNode> vectorNode = tp.getVectorDataGroup();
        
        assertTrue(vectorNode.contains("shapes"));
        
        VectorDataNode vdn = vectorNode.get("shapes");
        assertEquals(4, vdn.getFeatureCollection().size());
	}
	

	@Test
	public void testOperatorExecutionThreePolygonsDifferentValue() throws Exception {
        final Product sp = createTestProduct(100, 100);

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("bandName", "raster_4");

        Product tp = GPF.createProduct("Raster-To-Vector", parameters, sp);
       
        Band band = tp.getBandAt(0);
        band.readRasterDataFully();

        ProductNodeGroup<VectorDataNode> vectorNode = tp.getVectorDataGroup();
        
        assertTrue(vectorNode.contains("shapes"));
        
        VectorDataNode vdn = vectorNode.get("shapes");
        assertEquals(3, vdn.getFeatureCollection().size());
	}

    private Product createTestProduct(int w, int h) {
        Product product = new Product("p", "t", w, h);

        Placemark[] gcps = {
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p1", "p1", "", new PixelPos(0.5f, 0.5f), new GeoPos(10, -10),
                                               null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p2", "p2", "", new PixelPos(w - 0.5f, 0.5f), new GeoPos(10, 10),
                                               null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p3", "p3", "", new PixelPos(w - 0.5f, h - 0.5f), new GeoPos(-10, 10),
                                               null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p4", "p4", "", new PixelPos(0.5f, h - 0.5f), new GeoPos(-10, -10),
                                               null),
        };
        product.setSceneGeoCoding(new GcpGeoCoding(GcpGeoCoding.Method.POLYNOMIAL1, gcps, w, h, Datum.WGS_84));

        product.setPreferredTileSize(w / 10, h / 10);
        
        // the first band contains only the value 1
        Band band1 = product.addBand("raster_1", ProductData.TYPE_INT32);
        int[] band1Values = new int[w * h];
        Arrays.fill(band1Values, 1);
        band1.setData(ProductData.createInstance(band1Values));

        // the second band contains two areas with value 1 separated by a line with value 0
        Band band2 = product.addBand("raster_2", ProductData.TYPE_INT32);
        int[] band2Values = new int[w * h];
        Arrays.fill(band2Values, 1);
        Arrays.fill(band2Values, w * h / 2, w * h / 2 + w, 0);
        band2.setData(ProductData.createInstance(band2Values));

        // the third band contains 4 areas with value 1 separated by lines with value 0
        Band band3 = product.addBand("raster_3", ProductData.TYPE_INT32);
        int[] band3Values = new int[w * h];
        Arrays.fill(band3Values, 1);
        Arrays.fill(band3Values, w * h / 2, w * h / 2 + w, 0);
        for (int i = 0; i < h; i++) {
        	band3Values[w * i + w / 2] = 0;
        }
        band3.setData(ProductData.createInstance(band3Values));

        // the fourth band contains 3 areas with values 1, 2 and 3
        Band band4 = product.addBand("raster_4", ProductData.TYPE_INT32);
        int[] band4Values = new int[w * h];
        Arrays.fill(band4Values, 0, w * h / 3, 1);
        Arrays.fill(band4Values, w * h / 3 + 1, w * 2 * h / 3, 2);
        Arrays.fill(band4Values, w * 2 * h / 3 + 1, w * h, 3);
        band4.setData(ProductData.createInstance(band4Values));
        
        return product;
    }
}
