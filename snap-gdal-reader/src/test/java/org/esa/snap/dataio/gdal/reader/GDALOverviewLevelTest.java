/*
 * Copyright (C) 2025 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.dataio.gdal.reader;

import com.bc.ceres.multilevel.MultiLevelImage;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.gdal.reader.plugins.GTiffDriverProductReaderPlugIn;
import org.junit.Before;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * A GeoTIFF's overviews do not have to step by a factor of two.
 * <p>
 * {@code gdaladdo 4 8 16} is a common way to build a Cloud-Optimized GeoTIFF,
 * and the ICEYE Open Data Initiative ships its products that way: a
 * 20000 x 20000 GRD carries overviews of 5000, 2500 and 1250, with no 2x level
 * at all.
 * <p>
 * SNAP builds one pyramid level per overview ({@code levelCount =
 * getOverviewCount() + 1}) and sizes each level as {@code size >> level}, so it
 * assumes overview <i>n</i> decimates by 2^(n+1). When the file decimates by 4
 * instead, every level above 0 gets a canvas twice as wide and twice as tall as
 * the overview that fills it, and the image is left as no-data outside the
 * quarter that was written. On screen the scene collapses into one corner as
 * soon as the view zooms out far enough to leave level 0.
 * <p>
 * The fixture reproduces exactly that shape at 256 x 256 with overviews of 64,
 * 32 and 16. Every sample in every IFD is non-zero, so any zero read back from a
 * level is canvas the reader failed to fill.
 *
 * @author Luis Veci
 */
public class GDALOverviewLevelTest {

    private static final String FIXTURE = "overviews_4_8_16.tif";
    private static final int BASE_SIZE = 256;

    private File fixture;

    @Before
    public void setUp() throws Exception {
        final Path dir = Files.createTempDirectory("gdal-overviews");
        dir.toFile().deleteOnExit();
        fixture = dir.resolve(FIXTURE).toFile();
        fixture.deleteOnExit();
        try (InputStream in = GDALOverviewLevelTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull("missing fixture " + FIXTURE, in);
            Files.copy(in, fixture.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // The GDAL plug-ins report UNABLE when the native library is not present,
        // so this doubles as the availability check.
        assumeTrue("GDAL is not available",
                new GTiffDriverProductReaderPlugIn().getDecodeQualification(fixture)
                        != DecodeQualification.UNABLE);
    }

    private static Product read(final File file) throws Exception {
        final Product product = new GTiffDriverProductReaderPlugIn()
                .createReaderInstance().readProductNodes(file, null);
        assertNotNull(product);
        return product;
    }

    /** Sanity: the fixture really does decimate by 4, not by 2. */
    @Test
    public void testFixtureHasNonPowerOfTwoOverviews() throws Exception {
        final Product product = read(fixture);
        try {
            assertEquals(BASE_SIZE, product.getSceneRasterWidth());
            assertEquals(BASE_SIZE, product.getSceneRasterHeight());
            // 3 overviews -> 4 pyramid levels
            assertEquals(4, product.getBandAt(0).getSourceImage().getModel().getLevelCount());
        } finally {
            product.dispose();
        }
    }

    /**
     * Every pyramid level must be filled edge to edge. Before the fix, levels 1
     * to 3 are filled only in their top-left quarter.
     */
    @Test
    public void testEveryPyramidLevelIsFullyPopulated() throws Exception {
        final Product product = read(fixture);
        try {
            final MultiLevelImage image = product.getBandAt(0).getSourceImage();
            final int levelCount = image.getModel().getLevelCount();

            for (int level = 0; level < levelCount; ++level) {
                final RenderedImage levelImage = image.getImage(level);
                final int w = levelImage.getWidth();
                final int h = levelImage.getHeight();
                final Raster raster = levelImage.getData(new Rectangle(0, 0, w, h));

                int zeros = 0;
                for (int y = 0; y < h; ++y) {
                    for (int x = 0; x < w; ++x) {
                        if (raster.getSampleDouble(x, y, 0) == 0) {
                            ++zeros;
                        }
                    }
                }
                assertEquals("level " + level + " (" + w + "x" + h + ") has "
                        + zeros + " unfilled samples of " + (w * h)
                        + "; the overview that filled it is smaller than the level canvas",
                        0, zeros);
            }
        } finally {
            product.dispose();
        }
    }

    /**
     * The bottom-right corner of each level is the first thing to disappear when
     * a level is filled from a too-small overview, and it is what makes the scene
     * appear stuck in a corner on screen.
     */
    @Test
    public void testFarCornerOfEachLevelHasData() throws Exception {
        final Product product = read(fixture);
        try {
            final MultiLevelImage image = product.getBandAt(0).getSourceImage();
            for (int level = 0; level < image.getModel().getLevelCount(); ++level) {
                final RenderedImage levelImage = image.getImage(level);
                final int x = levelImage.getWidth() - 1;
                final int y = levelImage.getHeight() - 1;
                final double value = levelImage.getData(new Rectangle(x, y, 1, 1))
                        .getSampleDouble(x, y, 0);
                assertTrue("level " + level + " is empty at its far corner (" + x + "," + y + ")",
                        value != 0);
            }
        } finally {
            product.dispose();
        }
    }
}
