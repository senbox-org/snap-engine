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

package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.esa.s3tbx.olci.radiometry.Sensor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author muhammad.bc.
 */
public class SmileCorrectionUtilsTest {
    @Test
    public void testGetBandIndex() throws Exception {
        assertEquals(8, SmileCorrectionUtils.getSourceBandIndex("band_08"));
        assertEquals(-1, SmileCorrectionUtils.getSourceBandIndex("band"));
        assertEquals(9, SmileCorrectionUtils.getSourceBandIndex("09band"));
        assertEquals(5, SmileCorrectionUtils.getSourceBandIndex("Bla05band"));
        assertEquals(15, SmileCorrectionUtils.getSourceBandIndex("Bla15band"));
    }

    @Test
    public void testSensorTypeOlci() throws Exception {
        Product sourceProduct = new Product("bla", "what", 300, 300);
        sourceProduct.addBand("Oa1_radiance", ProductData.TYPE_UINT8);
        sourceProduct.addBand("Oa2_radiance", ProductData.TYPE_UINT8);
        sourceProduct.addBand("Oa3_radiance", ProductData.TYPE_UINT8);
        Sensor sensorType = SmileCorrectionUtils.getSensorType(sourceProduct);
        assertEquals(Sensor.OLCI, sensorType);
    }

    @Test
    public void testSensorTypeMeris() throws Exception {
        Product sourceProduct = new Product("bla", "what", 300, 300);
        sourceProduct.addBand("radiance_1", ProductData.TYPE_UINT8);
        Sensor sensorType = SmileCorrectionUtils.getSensorType(sourceProduct);
        assertEquals(Sensor.MERIS, sensorType);
    }
}