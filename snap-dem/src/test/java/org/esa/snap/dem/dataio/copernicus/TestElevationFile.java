package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mElevationModel;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mElevationModelDescriptor;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mFile;
import org.esa.snap.dem.dataio.copernicus.copernicus90m.Copernicus90mElevationModel;
import org.esa.snap.dem.dataio.copernicus.copernicus90m.Copernicus90mFile;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class TestElevationFile {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void test30mFile() throws IOException {
        File testFile = new File (tempFolder.newFolder("dem"), "Copernicus_DSM_COG_10_N55_00_W005_00_DEM.tif");

        Copernicus30mElevationModel model = new Copernicus30mElevationModel(new Copernicus30mElevationModelDescriptor(), Resampling.BICUBIC_INTERPOLATION);
        Copernicus30mFile file = new Copernicus30mFile(model, testFile, (new GeoTiffProductReaderPlugIn()).createReaderInstance());

        Assert.assertNotNull(file);
        Assert.assertNotNull(file.getTile());
    }

    @Test
    public void test90mFile() throws IOException {
        File testFile = new File (tempFolder.newFolder("dem"), "Copernicus_DSM_COG_30_N55_00_W005_00_DEM.tif");

        Copernicus90mElevationModel model = new Copernicus90mElevationModel(new Copernicus30mElevationModelDescriptor(), Resampling.BICUBIC_INTERPOLATION);
        Copernicus90mFile file = new Copernicus90mFile(model, testFile, (new GeoTiffProductReaderPlugIn()).createReaderInstance());
        Assert.assertNotNull(file);
        Assert.assertNotNull(file.getTile());
    }

    @Test
    public void test30mElevationModel() {
        Copernicus30mElevationModel model = new Copernicus30mElevationModel(new Copernicus30mElevationModelDescriptor(), Resampling.BICUBIC_INTERPOLATION);

        Assert.assertEquals("Copernicus_DSM_COG_10_N03_00_W080_00_DEM.tif", model.createTileFilename(3.0, -80));
        Assert.assertEquals("Copernicus_DSM_COG_10_S03_00_E080_00_DEM.tif", model.createTileFilename(-3.0, 80));
        Assert.assertEquals("Copernicus_DSM_COG_10_S03_00_W080_00_DEM.tif", model.createTileFilename(-3.0, -80));
        Assert.assertEquals("Copernicus_DSM_COG_10_N03_00_E080_00_DEM.tif", model.createTileFilename(3.0, 80));
    }

    @Test
    public void test90mElevationModel() {
        Copernicus90mElevationModel model = new Copernicus90mElevationModel(new Copernicus30mElevationModelDescriptor(), Resampling.BICUBIC_INTERPOLATION);

        Assert.assertEquals("Copernicus_DSM_COG_30_N03_00_W080_00_DEM.tif", model.createTileFilename(3.0, -80));
        Assert.assertEquals("Copernicus_DSM_COG_30_S03_00_E080_00_DEM.tif", model.createTileFilename(-3.0, 80));
        Assert.assertEquals("Copernicus_DSM_COG_30_S03_00_W080_00_DEM.tif", model.createTileFilename(-3.0, -80));
        Assert.assertEquals("Copernicus_DSM_COG_30_N03_00_E080_00_DEM.tif", model.createTileFilename(3.0, 80));
    }

    @Test
    @STTM("SNAP-4035")
    public void test30mCreateFile() throws Exception {

        File testFile = new File (tempFolder.newFolder("dem"), "Copernicus_DSM_COG_10_N55_00_W005_00_DEM.tif");

        final float[] expected = new float[] {159.47452f, 150.17595f, 143.38873f};
        Copernicus30mElevationModel model = new Copernicus30mElevationModel(new Copernicus30mElevationModelDescriptor(), Resampling.NEAREST_NEIGHBOUR);
        Copernicus30mFile file = new Copernicus30mFile(model, testFile, (new GeoTiffProductReaderPlugIn()).createReaderInstance());
        final ElevationTile tile = file.getTile();

        final float[] actual = new float[3];
        actual[0] = tile.getSample(0, 0);
        actual[1] = tile.getSample(0, 1);
        actual[2] = tile.getSample(0, 2);
        Assert.assertArrayEquals(expected, actual, 1e-5f);
    }

}
