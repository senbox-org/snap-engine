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
import org.esa.snap.core.dataio.dimap.DimapProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.DummyProductBuilder;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.runtime.Config;
import org.junit.Test;

import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

    @Test
    @STTM("SNAP-4218")
    public void testConcurrentImageBandWritingReportsProgressDuringTileWriting() throws IOException {
        final Preferences preferences = Config.instance("snap").load().preferences();
        final String concurrentKey = ProductIO.SYSTEM_PROPERTY_CONCURRENT;
        final String parallelismKey = "snap.parallelism";
        final String oldConcurrent = preferences.get(concurrentKey, null);
        final String oldParallelism = preferences.get(parallelismKey, null);
        final Path tempDirectory = Files.createTempDirectory("test-dir");
        try {
            preferences.putBoolean(concurrentKey, true);
            preferences.putInt(parallelismKey, 2);

            final Product product = new Product("progressProduct", "test", 4, 4);
            addTiledSourceBand(product, "b1", 1.0f);
            addTiledSourceBand(product, "b2", 2.0f);

            final ProductWriter writer = mock(ProductWriter.class);
            when(writer.shouldWrite(any())).thenReturn(true);

            final RecordingProgressMonitor pm = new RecordingProgressMonitor();
            ProductIO.writeProduct(product, tempDirectory.resolve("progress.dim").toFile(), writer, pm);

            assertTrue(pm.getWorkBeforeDone().size() > 1);
        } finally {
            restorePreference(preferences, concurrentKey, oldConcurrent);
            restorePreference(preferences, parallelismKey, oldParallelism);
            FileUtils.deleteTree(tempDirectory.toFile());
        }
    }

    private void verifyHeaderRewrite(Product product, File file, int numberOfInvocations) throws IOException {
        final DimapProductWriter writer = mock(DimapProductWriter.class);
        ProductIO.writeProduct(product, file, writer, ProgressMonitor.NULL);
        verify(writer, times(numberOfInvocations)).writeProductNodes(product, file);
    }

    private static void addTiledSourceBand(Product product, String name, float value) {
        final Band band = product.addBand(name, ProductData.TYPE_FLOAT32);
        band.setSourceImage(createTiledSourceImage(product.getSceneRasterWidth(), product.getSceneRasterHeight(), value));
    }

    private static TiledImage createTiledSourceImage(int width, int height, float value) {
        final SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, 2, 2, 1, 2, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final TiledImage image = new TiledImage(0, 0, width, height, 0, 0, sampleModel, colorModel);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setSample(x, y, 0, value);
            }
        }
        return image;
    }

    private static void restorePreference(Preferences preferences, String key, String oldValue) {
        if (oldValue == null) {
            preferences.remove(key);
        } else {
            preferences.put(key, oldValue);
        }
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

    private static class RecordingProgressMonitor implements ProgressMonitor {

        private final List<Double> workBeforeDone = Collections.synchronizedList(new ArrayList<>());
        private boolean done;

        List<Double> getWorkBeforeDone() {
            return workBeforeDone;
        }

        @Override
        public void beginTask(String taskName, int totalWork) {
            done = false;
        }

        @Override
        public void done() {
            done = true;
        }

        @Override
        public void internalWorked(double work) {
            if (!done && work > 0.0) {
                workBeforeDone.add(work);
            }
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void setCanceled(boolean canceled) {
        }

        @Override
        public void setTaskName(String taskName) {
        }

        @Override
        public void setSubTaskName(String subTaskName) {
        }

        @Override
        public void worked(int work) {
            internalWorked(work);
        }
    }
}
