package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;


public class CopernicusDirectElevationTileTest {


    @Test
    @STTM("SNAP-4213")
    public void wholeTileCacheReadsSourceOnlyOnce() throws Exception {
        CountingSource source = new CountingSource(4, 4);
        CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(new FakeElevationModel(),
                source, 4, 4, 0, 0, 1024, 2, false);

        assertEquals(12.0f, tile.getSample(2, 1), 0.0f);
        assertEquals(31.0f, tile.getSample(1, 3), 0.0f);

        assertEquals(1, source.readCount);
    }

    @Test
    @STTM("SNAP-4213")
    public void blockCacheReadsOnlyRequestedBlocks() throws Exception {
        CountingSource source = new CountingSource(4, 8);
        CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(new FakeElevationModel(),
                source, 4, 8, 0, 0, 0, 2, false);

        assertEquals(1.0f, tile.getSample(1, 0), 0.0f);
        assertEquals(11.0f, tile.getSample(1, 1), 0.0f);
        assertEquals(31.0f, tile.getSample(1, 3), 0.0f);

        assertEquals(2, source.readCount);
    }

    @Test
    @STTM("SNAP-4213")
    public void nonSquareSourceIsHorizontallyResampledLazily() throws Exception {
        CountingSource source = new CountingSource(2, 2);
        CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(new FakeElevationModel(),
                source, 4, 2, 0, 0, 0, 1, false);

        assertEquals(0.0f, tile.getSample(0, 0), 0.0f);
        assertEquals(0.5f, tile.getSample(1, 0), 0.0f);
        assertEquals(1.0f, tile.getSample(2, 0), 0.0f);
        assertEquals(1.0f, tile.getSample(3, 0), 0.0f);

        assertEquals(1, source.readCount);
    }

    private static final class CountingSource implements CopernicusTileSource {
        private final int width;
        private final int height;
        private int readCount;

        private CountingSource(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public float[] readRows(int y, int rowCount) {
            readCount++;
            float[] rows = new float[width * rowCount];
            for (int row = 0; row < rowCount; row++) {
                for (int x = 0; x < width; x++) {
                    rows[row * width + x] = (y + row) * 10.0f + x;
                }
            }
            return rows;
        }

        @Override
        public void close() {
        }
    }

    private static final class FakeElevationModel implements ElevationModel {
        private final ElevationModelDescriptor descriptor = new FakeDescriptor();

        @Override
        public ElevationModelDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public double getElevation(GeoPos geoPos) {
            return 0.0;
        }

        @Override
        public PixelPos getIndex(GeoPos geoPos) {
            return new PixelPos();
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos) {
            return new GeoPos(90.0 - pixelPos.y, pixelPos.x - 180.0);
        }

        @Override
        public double getSample(double pixelX, double pixelY) {
            return 0.0;
        }

        @Override
        public boolean getSamples(int[] x, int[] y, double[][] samples) {
            return true;
        }

        @Override
        public Resampling getResampling() {
            return Resampling.NEAREST_NEIGHBOUR;
        }

        @Override
        public void dispose() {
        }
    }

    private static final class FakeDescriptor implements ElevationModelDescriptor {
        @Override
        public String getName() {
            return "Fake";
        }
        @Override
        public float getNoDataValue() {
            return 0.0f;
        }
        @Override
        public int getRasterWidth() {
            return 4;
        }
        @Override
        public int getRasterHeight() {
            return 4;
        }
        @Override
        public int getTileWidthInDegrees() {
            return 1;
        }
        @Override
        public int getTileWidth() {
            return 4;
        }
        @Override
        public int getNumXTiles() {
            return 360;
        }
        @Override
        public int getNumYTiles() {
            return 180;
        }
        @Override
        public ElevationModel createDem(Resampling resampling) {
            return null;
        }
        @Override
        public boolean canBeDownloaded() {
            return false;
        }
        @Override
        public File getDemInstallDir() {
            return new File(".");
        }
    }
}
