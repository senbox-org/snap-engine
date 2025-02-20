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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.media.jai.util.Range;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GcpGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.gpf.GPF;
import org.junit.Test;

public class QuantizationOpTest {

	@Test
	public void testOperatorExecutionOneInterval() throws Exception {
        final Product sp = createTestProduct(100, 100);
        
        final Map<String, Object> parameters = new HashMap<>();
        Map<Integer, Range> intervals = new HashMap<>();
        intervals.put(1, new Range(Double.class, -0.1, false, 1.0, true));
        parameters.put("bandName", "raster_1");
        parameters.put("intervalsMap", intervals);

        Product tp = GPF.createProduct("Quantization", parameters, sp);

        Band band = tp.getBandAt(0);
        band.readRasterDataFully();

        for (int i = 0; i < band.getRasterWidth(); i++) {
            for (int j = 0; j < band.getRasterWidth(); j++) {
            	assertEquals(1, band.getSampleInt(i, j));
            }
        }
	}
	
	@Test
	public void testOperatorExecutionTwoIntervals() throws Exception {
        final Product sp = createTestProduct(100, 100);

        final Map<String, Object> parameters = new HashMap<>();
        Map<Integer, Range> intervals = new HashMap<>();
        intervals.put(1, new Range(Double.class, -0.1, false, 1.0, true));
        intervals.put(2, new Range(Double.class, 1.9, false, 3.0, true));
        parameters.put("bandName", "raster_2");
        parameters.put("intervalsMap", intervals);
        
        Product tp = GPF.createProduct("Quantization", parameters, sp);
		
        Band band = tp.getBandAt(0);
        band.readRasterDataFully();

        for (int j = 0; j < band.getRasterWidth(); j++) {
        	for (int i = 0; i < band.getRasterWidth(); i++) {
            	if (j < band.getRasterHeight() / 2) {
                	assertEquals(1, band.getSampleInt(i, j));
            	} else {
            		assertEquals(2, band.getSampleInt(i, j));
            	}
            }
        }
	}

	@Test
	public void testOperatorExecutionFourIntervals() throws Exception {
        final Product sp = createTestProduct(100, 100);

        final Map<String, Object> parameters = new HashMap<>();
        Map<Integer, Range> intervals = new HashMap<>();
        intervals.put(1, new Range(Double.class, -0.1, false, 1.0, true));
        intervals.put(2, new Range(Double.class, 1.9, false, 3.0, true));
        parameters.put("bandName", "raster_3");
        parameters.put("intervalsMap", intervals);

        Product tp = GPF.createProduct("Quantization", parameters, sp);
       
        Band band = tp.getBandAt(0);
        band.readRasterDataFully();

        for (int j = 0; j < band.getRasterWidth(); j++) {
        	for (int i = 0; i < band.getRasterWidth(); i++) {
            	if (j < band.getRasterHeight() / 4) {
                	assertEquals(1, band.getSampleInt(i, j));
            	} else if (j < band.getRasterHeight() / 2) {
            		assertEquals(2, band.getSampleInt(i, j));
            	} else if (j < 3 * band.getRasterHeight() / 4) {
            		assertEquals(1, band.getSampleInt(i, j));
            	} else {
            		assertEquals(0, band.getSampleInt(i, j));
            	}
            }
        }
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
        
        // the first band contains values between 0(exclusive)  and 1 (inclusive)
        Band band1 = product.addBand("raster_1", ProductData.TYPE_FLOAT64);
        Random band1Rand = new Random(System.currentTimeMillis());
        double[] band1Values = band1Rand.doubles(w * h).toArray();
        band1.setData(ProductData.createInstance(band1Values));

        // the second band contains two areas 
        // first has values between 0(exclusive)  and 1 (inclusive)
        // second has values between 2(exclusive)  and 3 (inclusive)
        Band band2 = product.addBand("raster_2", ProductData.TYPE_FLOAT64);
        Random band2Rand = new Random(System.currentTimeMillis());
        double[] band2Values = new double[w * h];
        System.arraycopy(band2Rand.doubles(w * h / 2).toArray(), 0, band2Values, 0, w * h / 2);
        System.arraycopy(band2Rand.doubles(w * h / 2).map(v -> v + 2.0).toArray(), 0, band2Values, w * h / 2, w * h / 2);
        band2.setData(ProductData.createInstance(band2Values));

        // the second band contains four areas 
        // first has values between 0(exclusive)  and 1 (inclusive)
        // second has values between 2(exclusive)  and 3 (inclusive)
        // third has values between 0(exclusive)  and 1 (inclusive)
        // fourth has values between 4(exclusive)  and 5 (inclusive)
        Band band3 = product.addBand("raster_3", ProductData.TYPE_FLOAT64);
        Random band3Rand = new Random(System.currentTimeMillis());
        double[] band3Values = new double[w * h];
        System.arraycopy(band3Rand.doubles(w * h / 4).toArray(), 0, band3Values, 0, w * h / 4);
        System.arraycopy(band3Rand.doubles(w * h / 4).map(v -> v + 2.0).toArray(), 0, band3Values, w * h / 4, w * h / 4);
        System.arraycopy(band3Rand.doubles(w * h / 4).toArray(), 0, band3Values, w * h / 2, w * h / 4);
        System.arraycopy(band3Rand.doubles(w * h / 4).map(v -> v + 4.0).toArray(), 0, band3Values, 3 * w * h / 4, w * h / 4);
        band3.setData(ProductData.createInstance(band3Values));

        return product;
    }
}
