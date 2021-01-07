package org.esa.snap.dem.dataio.copernicus;

import org.junit.Test;
import org.locationtech.jts.util.Assert;

import java.nio.file.Files;


public class TestDownload {


    @Test
    public void testSearch() throws Exception {
        CopernicusDownloader d = new CopernicusDownloader(Files.createTempDirectory(this.getClass().getSimpleName()).toFile());
        d.downloadTiles(-80, 43, 30);
        d.downloadTiles(-80, 43, 90);
        d.downloadTiles(0, 0, 90);

    }
    @Test
    public void testGetName() throws Exception {
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
