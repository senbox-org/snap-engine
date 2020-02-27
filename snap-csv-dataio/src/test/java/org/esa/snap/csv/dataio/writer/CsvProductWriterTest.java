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

package org.esa.snap.csv.dataio.writer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelGeoIndexInverse;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductWriterTest {

    private final String LS = "\n";

    private StringWriter stringWriter;

    @Before
    public void setUp() throws Exception {
        stringWriter = new StringWriter();
    }

    @Test
    public void testWrite_noGeoCoding() throws IOException {
        final ProductWriter writer = createProductWriter(CsvProductWriter.WRITE_FEATURES | CsvProductWriter.WRITE_PROPERTIES);

        final Product product = createProductWithoutGeoCoding(0);

        writer.writeProductNodes(product, "");
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);

        assertEquals("#sceneRasterWidth=2" + LS +
                "featureId\tradiance_1:float\tradiance_2:double\tradiance_3:int" + LS +
                "0\t0.0\t10.0\t100" + LS +
                "1\t1.0\t11.0\t101" + LS +
                "2\t2.0\t12.0\t102" + LS +
                "3\t3.0\t13.0\t103" + LS +
                "4\t4.0\t14.0\t104" + LS +
                "5\t5.0\t15.0\t105", stringWriter.toString().trim());
    }

    @Test
    public void testWrite_noGeoCoding_noFeatures() throws IOException {
        // @todo 1 tb/** this is nonsense - either the writer skips the features when the config tells it to do so
        //  or we remove this parameter. It confuses if a parameter does not do anything 2020-02-26
        final ProductWriter writer = createProductWriter(CsvProductWriter.WRITE_PROPERTIES);

        final Product product = createProductWithoutGeoCoding(1);

        writer.writeProductNodes(product, "");
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);

        assertEquals("#sceneRasterWidth=2" + LS +
                "featureId\tradiance_1:float\tradiance_2:double\tradiance_3:int" + LS +
                "0\t1.0\t11.0\t101" + LS +
                "1\t2.0\t12.0\t102" + LS +
                "2\t3.0\t13.0\t103" + LS +
                "3\t4.0\t14.0\t104" + LS +
                "4\t5.0\t15.0\t105" + LS +
                "5\t6.0\t16.0\t106", stringWriter.toString().trim());
    }

    @Test
    public void testWrite_noGeoCoding_noProperties() throws IOException {
        final ProductWriter writer = createProductWriter(CsvProductWriter.WRITE_FEATURES);

        final Product product = createProductWithoutGeoCoding(2);

        writer.writeProductNodes(product, "");
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);

        assertEquals("featureId\tradiance_1:float\tradiance_2:double\tradiance_3:int" + LS +
                "0\t2.0\t12.0\t102" + LS +
                "1\t3.0\t13.0\t103" + LS +
                "2\t4.0\t14.0\t104" + LS +
                "3\t5.0\t15.0\t105" + LS +
                "4\t6.0\t16.0\t106" + LS +
                "5\t7.0\t17.0\t107", stringWriter.toString().trim());
    }

    @Test
    public void testWrite_pixelGeoCoding() throws IOException {
        final ProductWriter writer = createProductWriter(CsvProductWriter.WRITE_FEATURES | CsvProductWriter.WRITE_PROPERTIES);

        final Product product = createProductWithPixelGeoCoding(3);

        writer.writeProductNodes(product, "");
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);

        assertEquals("#sceneRasterWidth=2" + LS +
                "featureId\tradiance_1:float\tradiance_2:double\tradiance_3:int\tlongitude:float\tlatitude:float" + LS +
                "0\t3.0\t13.0\t103\t-117.0\t23.0" + LS +
                "1\t4.0\t14.0\t104\t-116.0\t24.0" + LS +
                "2\t5.0\t15.0\t105\t-115.0\t25.0" + LS +
                "3\t6.0\t16.0\t106\t-114.0\t26.0" + LS +
                "4\t7.0\t17.0\t107\t-113.0\t27.0" + LS +
                "5\t8.0\t18.0\t108\t-112.0\t28.0", stringWriter.toString().trim());
    }

    private void fillBandDataInt(Band band, int startValue) {
        final int rasterWidth = band.getRasterWidth();
        final int rasterHeight = band.getRasterHeight();

        final ProductData data = band.createCompatibleProductData(rasterWidth * rasterHeight);
        int value = startValue;
        int dataIndex = 0;
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                data.setElemIntAt(dataIndex++, value++);
            }
        }
        band.setData(data);
    }

    private void fillBandDataFloat(Band band, int startValue) {
        final int rasterWidth = band.getRasterWidth();
        final int rasterHeight = band.getRasterHeight();

        final ProductData data = band.createCompatibleProductData(rasterWidth * rasterHeight);
        int value = startValue;
        int dataIndex = 0;
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                data.setElemFloatAt(dataIndex++, value++);
            }
        }
        band.setData(data);
    }

    private Product createProductWithoutGeoCoding(int startValue) {
        final Product product = new Product("testProduct", "testType", 2, 3);
        fillBandDataFloat(product.addBand("radiance_1", ProductData.TYPE_FLOAT32), startValue);
        fillBandDataFloat(product.addBand("radiance_2", ProductData.TYPE_FLOAT64), 10 + startValue);
        fillBandDataInt(product.addBand("radiance_3", ProductData.TYPE_INT32), 100 + startValue);
        return product;
    }

    private Product createProductWithPixelGeoCoding(int startValue) {
        final Product product = createProductWithoutGeoCoding(startValue);

        fillBandDataFloat(product.addBand("longitude", ProductData.TYPE_FLOAT32), -120 + startValue);
        fillBandDataFloat(product.addBand("latitude", ProductData.TYPE_FLOAT32), 20 + startValue);

        final GeoRaster geoRaster = new GeoRaster(null, null, "longitude", "latitude",
                2, 3, 1.3);

        final ForwardCoding forward = ComponentFactory.getForward(PixelForward.KEY);
        final InverseCoding inverse = ComponentFactory.getInverse(PixelGeoIndexInverse.KEY_INTERPOLATING);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse);
        product.setSceneGeoCoding(geoCoding);

        return product;
    }

    private ProductWriter createProductWriter(int config) {
        final CsvProductWriterPlugIn plugIn = new CsvProductWriterPlugIn(stringWriter, config);
        return plugIn.createWriterInstance();
    }
}
