package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.test.LongTestRunner;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.jts.util.Assert;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

@RunWith(LongTestRunner.class)
public class TestDownload {


    @Test
    @STTM("SNAP-3717")
    public void testSearch() throws Exception {
        File folder = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();
        CopernicusDownloader d = new CopernicusDownloader(folder);
        try{
            URL url = new URL("http://www.google.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            d.downloadTiles(-80, 43, 30);
            d.downloadTiles(-80, 43, 90);
        }catch(Exception e){
            System.out.println("Internet not connected. Download test will be skipped.");
        }

        FileUtils.deleteTree(folder);
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
