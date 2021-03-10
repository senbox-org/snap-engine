package org.esa.snap.dem.dataio.copernicus;

import org.junit.Test;
import org.locationtech.jts.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDownload {

    @Test
    public void testStorage() throws Exception {
        final File downloadDir = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();
        final int dataSize = 3000;
        CopernicusDownloader d = new CopernicusDownloader(downloadDir) {
            @Override
            InputStream createInputStream(String download_path) {
                return new ByteArrayInputStream(new byte[dataSize]);
            }
        };

        d.downloadTiles(-80, 43, 30);
        final String fileName30m = CopernicusDownloader.createTileFilename(-80, 43, "30") + ".tif";
        final File file30m = new File(downloadDir, fileName30m);
        assertTrue(file30m.exists());
        assertEquals(dataSize, file30m.length());

        d.downloadTiles(-80, 43, 90);
        final String fileName90m = CopernicusDownloader.createTileFilename(-80, 43, "10") + ".tif";
        final File file90m = new File(downloadDir, fileName90m);
        assertTrue(file90m.exists());
        assertEquals(dataSize, file90m.length());
    }

    @Test
    public void testGetName() {
        Assert.equals(CopernicusDownloader.createTileFilename(3, -80, "30"), "Copernicus_DSM_COG_30_N03_00_W080_00_DEM");
        Assert.equals(CopernicusDownloader.createTileFilename(-3, -80, "30"), "Copernicus_DSM_COG_30_S03_00_W080_00_DEM");
        Assert.equals(CopernicusDownloader.createTileFilename(3, 80, "30"), "Copernicus_DSM_COG_30_N03_00_E080_00_DEM");
        Assert.equals(CopernicusDownloader.createTileFilename(-3, 80, "30"), "Copernicus_DSM_COG_30_S03_00_E080_00_DEM");


        Assert.equals(CopernicusDownloader.createTileFilename(3, -80, "10"), "Copernicus_DSM_COG_10_N03_00_W080_00_DEM");
        Assert.equals(CopernicusDownloader.createTileFilename(-3, -80, "10"), "Copernicus_DSM_COG_10_S03_00_W080_00_DEM");
        Assert.equals(CopernicusDownloader.createTileFilename(3, 80, "10"), "Copernicus_DSM_COG_10_N03_00_E080_00_DEM");
        Assert.equals(CopernicusDownloader.createTileFilename(-3, 80, "10"), "Copernicus_DSM_COG_10_S03_00_E080_00_DEM");
    }


}
