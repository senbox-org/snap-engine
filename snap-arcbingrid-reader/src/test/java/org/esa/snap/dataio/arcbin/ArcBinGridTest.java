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
package org.esa.snap.dataio.arcbin;

import com.bc.ceres.core.runtime.internal.UrlHelper;
import com.bc.ceres.test.LongTestRunner;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static junit.framework.Assert.assertNotNull;

/**
 * Test ArcBinGrid reader
 */
@RunWith(LongTestRunner.class)
public class ArcBinGridTest {


    @Test
    public void testProductIO() throws IOException {
        final URL fileURL = getClass().getResource("elevation/hdr.adf");
        assertNotNull(fileURL);
        final Path file = UrlHelper.urlToFile(fileURL);
        Product product = ProductIO.readProduct(file);
        assertNotNull(product);

        assert(product.getProductReader() instanceof ArcBinGridReader);
    }

    @Test
    public void testReader() throws IOException {
        final URL fileURL = getClass().getResource("elevation/hdr.adf");
        assertNotNull(fileURL);
        final Path file = UrlHelper.urlToFile(fileURL);
        ArcBinGridReaderPlugIn plugin = new ArcBinGridReaderPlugIn();
        assert(plugin.getDecodeQualification(file) == DecodeQualification.INTENDED);

        ProductReader reader = plugin.createReaderInstance();
        assertNotNull(reader);

        reader.readProductNodes(file, null);
    }
}
