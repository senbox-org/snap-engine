package org.esa.beam.dataio.s3.synergy;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.junit.Test;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CameraImageMosaicTest {

    @Test
    public void testCreateMosaicImage() throws Exception {
        final RenderedImage image1 = createRenderedImage(1);
        final RenderedImage image2 = createRenderedImage(2);
        final RenderedImage image3 = createRenderedImage(3);
        final RenderedImage image4 = createRenderedImage(4);
        final RenderedImage image5 = createRenderedImage(5);

        final RenderedImage mosaicImage = CameraImageMosaic.create(image1, image2, image3, image4, image5);

        assertNotNull(mosaicImage);
        assertEquals(50, mosaicImage.getWidth());
        assertEquals(100, mosaicImage.getHeight());

        final Raster data = mosaicImage.getData();
        for (int x = 0; x < mosaicImage.getWidth(); x++) {
            assertEquals(x / 10 + 1, data.getSample(x, 0, 0));
        }
    }

    private RenderedOp createRenderedImage(int sampleValue) {
        return ConstantDescriptor.create(10.0f, 100.0f, new Integer[]{sampleValue}, null);
    }

    @Test
    public void testCreateMosaicMultiLevelImage() throws Exception {
        final MultiLevelImage image1 = createMultiLevelImage(1, 2);
        final MultiLevelImage image2 = createMultiLevelImage(2, 2);
        final MultiLevelImage image3 = createMultiLevelImage(3, 2);
        final MultiLevelImage image4 = createMultiLevelImage(4, 2);
        final MultiLevelImage image5 = createMultiLevelImage(5, 2);

        final MultiLevelImage mosaicImage = CameraImageMosaic.create(image1, image2, image3, image4, image5);

        assertNotNull(mosaicImage);
        assertEquals(50, mosaicImage.getWidth());
        assertEquals(100, mosaicImage.getHeight());
        assertEquals(2, mosaicImage.getModel().getLevelCount());

        final Raster dataL0 = mosaicImage.getImage(0).getData();
        for (int x = 0; x < mosaicImage.getImage(0).getWidth(); x++) {
            assertEquals(x / 10 + 1, dataL0.getSample(x, 0, 0));
        }
        final Raster dataL1 = mosaicImage.getImage(1).getData();
        for (int x = 0; x < mosaicImage.getImage(1).getWidth(); x++) {
            assertEquals(x / 5 + 1, dataL1.getSample(x, 0, 0));
        }
    }

    private DefaultMultiLevelImage createMultiLevelImage(int sampleValue, int levelCount) {
        return new DefaultMultiLevelImage(new DefaultMultiLevelSource(createRenderedImage(sampleValue), levelCount));
    }
}