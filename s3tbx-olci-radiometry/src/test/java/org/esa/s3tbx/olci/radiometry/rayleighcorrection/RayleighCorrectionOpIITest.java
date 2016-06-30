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

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrectionOpIITest {
    @Ignore
    @Test
    public void testSearchBandWithWaveLength() throws Exception {
        Product product = new Product("dum", ProductData.TYPESTRING_INT8);
        Band band = new Band("dummy1", 1, 20, 20);
        band.setSpectralWavelength(400);

        product.addBand(band);
//        Product mockSourceProduct = mock(RayleighCorrectionOpII.class, Mockito.CALLS_REAL_METHODS);
    }
}