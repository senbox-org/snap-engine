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
import org.esa.snap.core.dataio.geocoding.forward.TiePointSplineForward;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.DataBuffer;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.esa.snap.csv.dataio.CsvTestUtils.createProductWithPixelGeoCoding;
import static org.esa.snap.csv.dataio.CsvTestUtils.createProductWithoutGeoCoding;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

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

        final Product product = createProductWithoutGeoCoding(0, 2, 3);

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

        final Product product = createProductWithoutGeoCoding(1, 2, 3);

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

        final Product product = createProductWithoutGeoCoding(2, 3, 3);

        writer.writeProductNodes(product, "");
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);

        assertEquals("featureId\tradiance_1:float\tradiance_2:double\tradiance_3:int" + LS +
                "0\t2.0\t12.0\t102" + LS +
                "1\t3.0\t13.0\t103" + LS +
                "2\t4.0\t14.0\t104" + LS +
                "3\t5.0\t15.0\t105" + LS +
                "4\t6.0\t16.0\t106" + LS +
                "5\t7.0\t17.0\t107" + LS +
                "6\t8.0\t18.0\t108" + LS +
                "7\t9.0\t19.0\t109" + LS +
                "8\t10.0\t20.0\t110", stringWriter.toString().trim());
    }

    @Test
    public void testWrite_pixelGeoCoding() throws IOException {
        final ProductWriter writer = createProductWriter(CsvProductWriter.WRITE_FEATURES | CsvProductWriter.WRITE_PROPERTIES);

        final Product product = createProductWithPixelGeoCoding(3, 2, 3);

        writer.writeProductNodes(product, "");
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);

        assertEquals("#sceneRasterWidth=2" + LS +
                "#rasterResolutionInKm=1.3" + LS +
                "featureId\tradiance_1:float\tradiance_2:double\tradiance_3:int\tlongitude:float\tlatitude:float" + LS +
                "0\t3.0\t13.0\t103\t-117.0\t23.0" + LS +
                "1\t4.0\t14.0\t104\t-116.0\t24.0" + LS +
                "2\t5.0\t15.0\t105\t-115.0\t25.0" + LS +
                "3\t6.0\t16.0\t106\t-114.0\t26.0" + LS +
                "4\t7.0\t17.0\t107\t-113.0\t27.0" + LS +
                "5\t8.0\t18.0\t108\t-112.0\t28.0", stringWriter.toString().trim());
    }

    @Test
    public void testWrite_noGeoCoding_withTiePoints() throws IOException {
        final ProductWriter writer = createProductWriter(CsvProductWriter.WRITE_FEATURES | CsvProductWriter.WRITE_PROPERTIES);

        final Product product = createProductWithTiePoints(5, 4, 4);

        writer.writeProductNodes(product, "");
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);

        assertEquals("#sceneRasterWidth=4" + LS +
                        "featureId\tradiance_1:float\tradiance_2:double\tradiance_3:int\ttp_1:float\ttp_2:float" + LS +
                        "0\t5.0\t15.0\t105\t5.0\t6.0" + LS +
                        "1\t6.0\t16.0\t106\t5.5\t6.5" + LS +
                        "2\t7.0\t17.0\t107\t6.0\t7.0" + LS +
                        "3\t8.0\t18.0\t108\t6.5\t7.5" + LS +
                        "4\t9.0\t19.0\t109\t6.0\t7.0" + LS +
                        "5\t10.0\t20.0\t110\t6.5\t7.5" + LS +
                        "6\t11.0\t21.0\t111\t7.0\t8.0" + LS +
                        "7\t12.0\t22.0\t112\t7.5\t8.5" + LS +
                        "8\t13.0\t23.0\t113\t7.0\t8.0" + LS +
                        "9\t14.0\t24.0\t114\t7.5\t8.5" + LS +
                        "10\t15.0\t25.0\t115\t8.0\t9.0" + LS +
                        "11\t16.0\t26.0\t116\t8.5\t9.5" + LS +
                        "12\t17.0\t27.0\t117\t8.0\t9.0" + LS +
                        "13\t18.0\t28.0\t118\t8.5\t9.5" + LS +
                        "14\t19.0\t29.0\t119\t9.0\t10.0" + LS +
                        "15\t20.0\t30.0\t120\t9.5\t10.5",
                stringWriter.toString().trim());
    }

    @Test
    public void testWrite_noGeoCoding_withTiePointsAndGeoCoding() throws IOException {
        final ProductWriter writer = createProductWriter(CsvProductWriter.WRITE_FEATURES | CsvProductWriter.WRITE_PROPERTIES);

        final Product product = createProductWithTiePointsAndGeoCoding(6, 4, 4);

        writer.writeProductNodes(product, "");
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);

        assertEquals("#sceneRasterWidth=4" + LS +
                        "#rasterResolutionInKm=1.3" + LS +
                        "featureId\tradiance_1:float\tradiance_2:double\tradiance_3:int\tlongitude:float\tlatitude:float" + LS +
                        "0\t6.0\t16.0\t106\t6.0\t7.0" + LS +
                        "1\t7.0\t17.0\t107\t6.5\t7.5" + LS +
                        "2\t8.0\t18.0\t108\t7.0\t8.0" + LS +
                        "3\t9.0\t19.0\t109\t7.5\t8.5" + LS +
                        "4\t10.0\t20.0\t110\t7.0\t8.0" + LS +
                        "5\t11.0\t21.0\t111\t7.5\t8.5" + LS +
                        "6\t12.0\t22.0\t112\t8.0\t9.0" + LS +
                        "7\t13.0\t23.0\t113\t8.5\t9.5" + LS +
                        "8\t14.0\t24.0\t114\t8.0\t9.0" + LS +
                        "9\t15.0\t25.0\t115\t8.5\t9.5" + LS +
                        "10\t16.0\t26.0\t116\t9.0\t10.0" + LS +
                        "11\t17.0\t27.0\t117\t9.5\t10.5" + LS +
                        "12\t18.0\t28.0\t118\t9.0\t10.0" + LS +
                        "13\t19.0\t29.0\t119\t9.5\t10.5" + LS +
                        "14\t20.0\t30.0\t120\t10.0\t11.0" + LS +
                        "15\t21.0\t31.0\t121\t10.5\t11.5",
                stringWriter.toString().trim());
    }

    @Test
    public void testGetJavaType() {
        assertEquals("float", CsvProductWriter.getJavaType(DataBuffer.TYPE_FLOAT));
        assertEquals("double", CsvProductWriter.getJavaType(DataBuffer.TYPE_DOUBLE));
        assertEquals("byte", CsvProductWriter.getJavaType(DataBuffer.TYPE_BYTE));
        assertEquals("short", CsvProductWriter.getJavaType(DataBuffer.TYPE_SHORT));
        assertEquals("ushort", CsvProductWriter.getJavaType(DataBuffer.TYPE_USHORT));
        assertEquals("int", CsvProductWriter.getJavaType(DataBuffer.TYPE_INT));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGetJavaType_unsupported() {
        try {
            CsvProductWriter.getJavaType(DataBuffer.TYPE_UNDEFINED);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testFlush() throws IOException {
        final Writer writer = mock(Writer.class);

        final ProductWriter productWriter = new CsvProductWriter(new CsvProductWriterPlugIn()
                , CsvProductWriter.WRITE_FEATURES | CsvProductWriter.WRITE_PROPERTIES, writer);

        productWriter.flush();

        verify(writer, times(1)).flush();
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void testClose() throws IOException {
        final Writer writer = mock(Writer.class);

        final ProductWriter productWriter = new CsvProductWriter(new CsvProductWriterPlugIn(), CsvProductWriter.WRITE_FEATURES | CsvProductWriter.WRITE_PROPERTIES, writer);

        productWriter.close();

        verify(writer, times(1)).close();
        verifyNoMoreInteractions(writer);
    }

    private Product createProductWithTiePoints(int startValue, int width, int height) {
        final Product product = createProductWithoutGeoCoding(startValue, width, height);
        final int tpWidth = width / 2;
        final int tpHeight = height / 2;

        final TiePointGrid tp_1 = new TiePointGrid("tp_1", tpWidth, tpHeight, 0.5, 0.5, 2.0, 2.0, createFloatData(startValue, tpWidth, tpHeight));
        final TiePointGrid tp_2 = new TiePointGrid("tp_2", tpWidth, tpHeight, 0.5, 0.5, 2.0, 2.0, createFloatData(startValue + 1, tpWidth, tpHeight));

        product.addTiePointGrid(tp_1);
        product.addTiePointGrid(tp_2);

        return product;
    }

    private Product createProductWithTiePointsAndGeoCoding(int startValue, int width, int height) {
        final Product product = createProductWithoutGeoCoding(startValue, width, height);
        final int tpWidth = width / 2;
        final int tpHeight = height / 2;

        final TiePointGrid lon = new TiePointGrid("longitude", tpWidth, tpHeight, 0.5, 0.5, 2.0, 2.0, createFloatData(startValue, tpWidth, tpHeight));
        final TiePointGrid lat = new TiePointGrid("latitude", tpWidth, tpHeight, 0.5, 0.5, 2.0, 2.0, createFloatData(startValue + 1, tpWidth, tpHeight));

        product.addTiePointGrid(lon);
        product.addTiePointGrid(lat);

        final GeoRaster geoRaster = new GeoRaster(null, null, "longitude", "latitude",
                tpWidth, tpHeight, width, height, 1.3, 0.5, 0.5, 2.0, 2.0);
        final ForwardCoding forward = ComponentFactory.getForward(TiePointSplineForward.KEY);
        final InverseCoding inverse = ComponentFactory.getInverse(TiePointInverse.KEY);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse);
        product.setSceneGeoCoding(geoCoding);

        return product;
    }

    private float[] createFloatData(int startValue, int width, int height) {
        final float[] floatData = new float[width * height];

        for (int i = 0; i < width * height; i++) {
            floatData[i] = startValue + i;
        }
        return floatData;
    }

    private ProductWriter createProductWriter(int config) {
        return new CsvProductWriter(new CsvProductWriterPlugIn(), config, stringWriter);
    }
}
