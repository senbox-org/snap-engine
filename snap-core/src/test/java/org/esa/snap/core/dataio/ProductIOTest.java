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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.dimap.DimapProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.DummyProductBuilder;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ProductIOTest {

    @Test
    public void testThatDefaultReaderAndWriterAreImplemented() {
        assertNotNull(ProductIO.getProductReader("BEAM-DIMAP"));
        assertNotNull(ProductIO.getProductWriter("BEAM-DIMAP"));
    }

    @Test
    public void testReadProductArgsChecking() {
        try {
            ProductIO.readProduct((File) null);
            fail();
        } catch (IOException expected) {
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            ProductIO.readProduct("rallala");
            fail();
        } catch (IOException expected) {
        }
    }

    @Test
    public void testHeaderIsRewrittenIfModified() throws IOException {
        final Product product = new DummyProductBuilder().create();
        final Path tempDirectory = Files.createTempDirectory("test-dir");
        try {
            verifyHeaderRewrite(product, tempDirectory.resolve("test1.dim").toFile(), 1);

            addBandWhichSetsProductToModifed(product);
            verifyHeaderRewrite(product, tempDirectory.resolve("test2.dim").toFile(), 2);
        } finally {
            FileUtils.deleteTree(tempDirectory.toFile());
        }
    }

    private void verifyHeaderRewrite(Product product, File file, int numberOfInvocations) throws IOException {
        final DimapProductWriter writer = mock(DimapProductWriter.class);
        ProductIO.writeProduct(product, file, writer, ProgressMonitor.NULL);
        verify(writer, times(numberOfInvocations)).writeProductNodes(product, file);
    }

    private void addBandWhichSetsProductToModifed(Product product) {
        product.addBand(new Band("modifyingBand", ProductData.TYPE_INT8, product.getSceneRasterWidth(), product.getSceneRasterHeight()) {
            @Override
            public void writeRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData, ProgressMonitor pm) throws IOException {
                super.writeRasterData(offsetX, offsetY, width, height, rasterData, pm);
                getProduct().setModified(true);
            }
        });
    }
}
