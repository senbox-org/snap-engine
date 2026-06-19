package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mFile;
import org.esa.snap.dem.dataio.copernicus.copernicus90m.Copernicus90mFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;


public class CopernicusElevationFileTest {


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Test
    @STTM("SNAP-4213")
    public void test30mMissingRemoteTileIsCachedAsNoDataTile() throws Exception {
        File localFile = new File(temporaryFolder.getRoot(), "Copernicus_DSM_COG_10_N03_00_W080_00_DEM.tif");
        CountingCopernicus30mFile file = new CountingCopernicus30mFile(localFile);

        ElevationTile firstTile = file.getTile(ProgressMonitor.NULL);
        ElevationTile secondTile = file.getTile(ProgressMonitor.NULL);

        assertNotNull(firstTile);
        assertSame(firstTile, secondTile);
        assertEquals(0.0f, firstTile.getSample(0, 0), 0.0f);
        assertEquals(1, file.downloadCount);
    }

    @Test
    @STTM("SNAP-4213")
    public void test90mKnownMissingRemoteTileIsCachedAsNoDataTileWithoutDownload() throws Exception {
        File localFile = new File(temporaryFolder.getRoot(), "Copernicus_DSM_COG_30_N03_00_W080_00_DEM.tif");
        CountingCopernicus90mFile file = new CountingCopernicus90mFile(localFile);

        ElevationTile firstTile = file.getTile(ProgressMonitor.NULL);
        ElevationTile secondTile = file.getTile(ProgressMonitor.NULL);

        assertNotNull(firstTile);
        assertSame(firstTile, secondTile);
        assertEquals(0.0f, firstTile.getSample(0, 0), 0.0f);
        assertEquals(0, file.downloadCount);
    }

    @Test
    @STTM("SNAP-4213")
    public void test90mKnownMissingTileSkipsRemoteLookup() throws Exception {
        File localFile = new File(temporaryFolder.getRoot(), "Copernicus_DSM_COG_30_N00_00_W160_00_DEM.tif");
        CountingCopernicus90mFile file = new CountingCopernicus90mFile(localFile);

        ElevationTile tile = file.getTile(ProgressMonitor.NULL);

        assertNotNull(tile);
        assertEquals(0.0f, tile.getSample(0, 0), 0.0f);
        assertEquals(0, file.downloadCount);
    }

    private static class CountingCopernicus30mFile extends Copernicus30mFile {

        private int downloadCount;

        CountingCopernicus30mFile(File localFile) {
            super(null, localFile, null);
        }

        @Override
        protected boolean downloadTiles(int lat, int lon, int resolution, ProgressMonitor progressMonitor) {
            downloadCount++;
            return false;
        }

        @Override
        protected ElevationTile createNoDataTile() {
            return new CopernicusNoDataElevationTile(0.0f);
        }
    }

    private static class CountingCopernicus90mFile extends Copernicus90mFile {

        private int downloadCount;

        CountingCopernicus90mFile(File localFile) {
            super(null, localFile, null);
        }

        @Override
        protected boolean downloadTiles(int lat, int lon, int resolution, ProgressMonitor progressMonitor) {
            downloadCount++;
            return false;
        }

        @Override
        protected ElevationTile createNoDataTile() {
            return new CopernicusNoDataElevationTile(0.0f);
        }
    }
}
