package org.esa.snap.dataio.gdal.reader;

import org.esa.snap.core.image.ImageReadBoundsSupport;
import org.junit.Test;

import java.awt.*;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class GDALTileOpImageTest {

    @Test
    public void ensureMinimunDimensionTest() {
        GDALTileOpImage tileOpImage = new GDALTileOpImage(new GDALBandSource() {
            @Override
            public Path[] getSourceLocalFiles() {
                return new Path[0];
            }

            @Override
            public int getBandIndex() {
                return 0;
            }
        }, 0,
                1,
                1,
                0,
                0,
                new ImageReadBoundsSupport(new Rectangle(0, 0, 1, 1), 0, 0),
                null);

        assertEquals(1, tileOpImage.ensureMinimunDimension(8, 3));
        assertEquals(1, tileOpImage.ensureMinimunDimension(0, 2));
        assertEquals(4, tileOpImage.ensureMinimunDimension(16, 2));
        assertEquals(32, tileOpImage.ensureMinimunDimension(1024, 5));
        assertEquals(1, tileOpImage.ensureMinimunDimension(1, 30));
        assertEquals(1, tileOpImage.ensureMinimunDimension(-8, 2));
        assertEquals(4, tileOpImage.ensureMinimunDimension(4, 0));
        assertEquals(1, tileOpImage.ensureMinimunDimension(1, 1));
    }
}