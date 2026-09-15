package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

/**
 * Covers the one production line the whole block design depends on:
 * {@link GeoTiffCopernicusTileSource#getPreferredBlockHeight()}, which reports the GeoTIFF's
 * internal tile height. Reading in blocks that are not a whole multiple of it makes the reader
 * decode the same internal tiles repeatedly - the SNAP 14 defect this class exists to avoid.
 * <p>
 * The fixtures are real, minimal TIFFs written here rather than committed binaries.
 */
public class GeoTiffCopernicusTileSourceTest {

    private static final int W = 32;
    private static final int H = 32;

    private static final String EGM_KEY = "snap.useDEMGravitationalModel";
    private static final String BLOCK_ROWS_KEY = "snap.dem.copernicus.blockRows";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private String savedEgm;
    private String savedBlockRows;

    @Before
    public void setUp() {
        savedEgm = Config.instance().preferences().get(EGM_KEY, null);
        savedBlockRows = Config.instance().preferences().get(BLOCK_ROWS_KEY, null);
        // the integration test compares raw encoded pixels, and asserts the DERIVED block height
        Config.instance().preferences().putBoolean(EGM_KEY, false);
        Config.instance().preferences().remove(BLOCK_ROWS_KEY);
    }

    @After
    public void tearDown() {
        restore(EGM_KEY, savedEgm);
        restore(BLOCK_ROWS_KEY, savedBlockRows);
    }

    private static void restore(final String key, final String saved) {
        if (saved == null) {
            Config.instance().preferences().remove(key);
        } else {
            Config.instance().preferences().put(key, saved);
        }
    }

    @Test
    public void reportsTheInternalTileHeightOfATiledTiff() throws Exception {
        withSource(writeTiff(tempFolder.newFile("tiled.tif"), 16), source -> {
            assertEquals(W, source.getWidth());
            assertEquals(H, source.getHeight());
            assertEquals("a 16-row internal tiling must be reported as such",
                         16, source.getPreferredBlockHeight());
        });
    }

    /**
     * Internal tiles taller than the image - the real Copernicus 90 m case (1200 x 1200 image,
     * 2048 x 2048 tiling), which must collapse to a single block.
     */
    @Test
    public void neverReportsMoreRowsThanTheImageHas() throws Exception {
        withSource(writeTiff(tempFolder.newFile("bigtile.tif"), 64), source ->
                assertEquals("must clamp to the image height", H, source.getPreferredBlockHeight()));
    }

    /**
     * A strip-per-row file makes the reader report 1. Passing that on would be worse than saying
     * nothing: every block height is a whole multiple of 1, so alignment becomes a no-op and an
     * unaligned override sails through. The contract says report the full image height instead.
     */
    @Test
    public void aStripedTiffReportsTheImageHeightNotOne() throws Exception {
        withSource(writeTiff(tempFolder.newFile("striped.tif"), 0), source ->
                assertEquals("a source that cannot tell must report the full image height",
                             H, source.getPreferredBlockHeight()));
    }

    /** readRows must return exactly the pixels of the requested band, in row-major order. */
    @Test
    public void readRowsReturnsTheRequestedBandOfPixels() throws Exception {
        withSource(writeTiff(tempFolder.newFile("values.tif"), 16), source -> {
            assertBand(source, 0, 16);
            assertBand(source, 16, 16);   // a band ending exactly at the last row
        });
    }

    /** A band that straddles the internal tile boundary must still come back correct. */
    @Test
    public void readRowsAcrossAnInternalTileBoundary() throws Exception {
        withSource(writeTiff(tempFolder.newFile("straddle.tif"), 16), source ->
                assertBand(source, 8, 16));   // rows 8..23, crossing the y=16 tile edge
    }

    /**
     * The seam both shipped SNAP 14 defects crossed: a REAL GeoTIFF source driving a REAL tile.
     * Everything else in the suite feeds the tile a hand-written stub, so nothing else checks that
     * the reader's reported tiling is what the tile actually blocks on.
     */
    @Test
    public void aRealGeoTiffDrivesTheTileBlockGeometryAndValues() throws Exception {
        final int width = 256;
        final int height = 1024;
        final int internalTile = 256;               // 4 block rows over the image
        final File file = writeTiff(tempFolder.newFile("integration.tif"), internalTile, width, height);

        final CountingSource source = new CountingSource(new GeoTiffCopernicusTileSource(file));
        try {
            // the PUBLIC constructor, i.e. exactly what Copernicus30mFile.createDirectTile builds
            final CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(
                    new StubElevationModel(), source, width, height, 111, 40);

            assertEquals("block height must come from the file's own internal tiling",
                         internalTile, tile.getBlockRows());

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final float got = tile.getSample(x, y);      // no eager message: 262k samples
                    if (got != pixel(x, y)) {
                        fail("(" + x + ',' + y + ") expected " + pixel(x, y) + " but was " + got);
                    }
                }
            }

            assertEquals("one read per block row, no row decoded twice",
                         height / internalTile, source.reads);
        } finally {
            source.close();
        }
    }

    /** Wraps the real source so the integration test can still count decodes. */
    private static final class CountingSource implements CopernicusTileSource {
        private final CopernicusTileSource inner;
        private int reads;

        private CountingSource(CopernicusTileSource inner) {
            this.inner = inner;
        }

        @Override
        public int getWidth() { return inner.getWidth(); }

        @Override
        public int getHeight() { return inner.getHeight(); }

        @Override
        public int getPreferredBlockHeight() { return inner.getPreferredBlockHeight(); }

        @Override
        public float[] readRows(int y, int rowCount) throws IOException {
            reads++;
            return inner.readRows(y, rowCount);
        }

        @Override
        public void close() { inner.close(); }
    }

    /** Minimal model: with the geoid off the tile never consults it at all. */
    private static final class StubElevationModel implements ElevationModel {
        private final ElevationModelDescriptor descriptor = new StubDescriptor();

        @Override
        public ElevationModelDescriptor getDescriptor() { return descriptor; }
        @Override
        public double getElevation(GeoPos geoPos) { return 0.0; }
        @Override
        public PixelPos getIndex(GeoPos geoPos) { return new PixelPos(); }
        @Override
        public GeoPos getGeoPos(PixelPos pixelPos) { return new GeoPos(); }
        @Override
        public double getSample(double pixelX, double pixelY) { return 0.0; }
        @Override
        public boolean getSamples(int[] x, int[] y, double[][] samples) { return true; }
        @Override
        public Resampling getResampling() { return Resampling.NEAREST_NEIGHBOUR; }
        @Override
        public void dispose() { }
    }

    private static final class StubDescriptor implements ElevationModelDescriptor {
        @Override
        public String getName() { return "Stub"; }
        @Override
        public float getNoDataValue() { return -32768.0f; }
        @Override
        public int getRasterWidth() { return 360 * 3600; }
        @Override
        public int getRasterHeight() { return 180 * 3600; }
        @Override
        public int getTileWidthInDegrees() { return 1; }
        @Override
        public int getTileWidth() { return 3600; }
        @Override
        public int getNumXTiles() { return 360; }
        @Override
        public int getNumYTiles() { return 180; }
        @Override
        public ElevationModel createDem(Resampling resampling) { return null; }
        @Override
        public boolean canBeDownloaded() { return false; }
        @Override
        public File getDemInstallDir() { return new File("."); }
    }

    private static void assertBand(final GeoTiffCopernicusTileSource source, final int y, final int rows)
            throws IOException {
        final float[] band = source.readRows(y, rows);
        assertEquals(W * rows, band.length);
        for (int row = 0; row < rows; row++) {
            for (int x = 0; x < W; x++) {
                assertEquals("(" + x + ',' + (y + row) + ')',
                             pixel(x, y + row), band[row * W + x], 0.0f);
            }
        }
    }

    private interface SourceCheck {
        void run(GeoTiffCopernicusTileSource source) throws Exception;
    }

    private static void withSource(final File file, final SourceCheck check) throws Exception {
        final GeoTiffCopernicusTileSource source = new GeoTiffCopernicusTileSource(file);
        try {
            check.run(source);
        } finally {
            source.close();
        }
    }

    private static float pixel(final int x, final int y) {
        return y * 100.0f + x;
    }

    /**
     * Writes a minimal uncompressed single-band float32 TIFF.
     *
     * @param tileSize internal tile edge in pixels, or 0 to write a strip-organised file
     */
    private static File writeTiff(final File file, final int tileSize) throws IOException {
        return writeTiff(file, tileSize, W, H);
    }

    private static File writeTiff(final File file, final int tileSize, final int W, final int H)
            throws IOException {
        final boolean tiled = tileSize > 0;
        final ByteArrayOutputStream pixels = new ByteArrayOutputStream();
        final int[] offsets;
        final int[] byteCounts;

        if (tiled) {
            final int across = (W + tileSize - 1) / tileSize;
            final int down = (H + tileSize - 1) / tileSize;
            offsets = new int[across * down];
            byteCounts = new int[across * down];
            int index = 0;
            for (int ty = 0; ty < down; ty++) {
                for (int tx = 0; tx < across; tx++) {
                    byteCounts[index] = tileSize * tileSize * 4;
                    offsets[index] = pixels.size();
                    final ByteBuffer buf = ByteBuffer.allocate(byteCounts[index]).order(ByteOrder.LITTLE_ENDIAN);
                    for (int row = 0; row < tileSize; row++) {
                        for (int col = 0; col < tileSize; col++) {
                            final int x = tx * tileSize + col;
                            final int y = ty * tileSize + row;
                            buf.putFloat(x < W && y < H ? pixel(x, y) : 0.0f);   // pad past the edge
                        }
                    }
                    pixels.write(buf.array());
                    index++;
                }
            }
        } else {
            offsets = new int[H];
            byteCounts = new int[H];
            for (int y = 0; y < H; y++) {
                byteCounts[y] = W * 4;
                offsets[y] = pixels.size();
                final ByteBuffer buf = ByteBuffer.allocate(byteCounts[y]).order(ByteOrder.LITTLE_ENDIAN);
                for (int x = 0; x < W; x++) {
                    buf.putFloat(pixel(x, y));
                }
                pixels.write(buf.array());
            }
        }

        // {tag id, type, value}; -1 / -2 mark the offset and byte-count arrays.
        // TIFF requires the IFD entries sorted by tag id.
        final int[][] tags = tiled
                ? new int[][]{
                        {256, 3, W}, {257, 3, H}, {258, 3, 32}, {259, 3, 1}, {262, 3, 1},
                        {277, 3, 1}, {284, 3, 1}, {322, 3, tileSize}, {323, 3, tileSize},
                        {324, 4, -1}, {325, 4, -2}, {339, 3, 3}}
                : new int[][]{
                        {256, 3, W}, {257, 3, H}, {258, 3, 32}, {259, 3, 1}, {262, 3, 1},
                        {273, 4, -1}, {277, 3, 1}, {278, 3, 1}, {279, 4, -2}, {284, 3, 1},
                        {339, 3, 3}};

        final int headerSize = 8;
        final int ifdSize = 2 + tags.length * 12 + 4;
        int cursor = headerSize + ifdSize;              // layout: header | IFD | arrays | pixels
        final int offsetsArrayAt = cursor;
        cursor += offsets.length * 4;
        final int countsArrayAt = cursor;
        cursor += byteCounts.length * 4;
        final int pixelsAt = cursor;

        final ByteBuffer out = ByteBuffer.allocate(pixelsAt + pixels.size()).order(ByteOrder.LITTLE_ENDIAN);
        out.put((byte) 'I').put((byte) 'I').putShort((short) 42).putInt(headerSize);
        out.putShort((short) tags.length);
        for (int[] tag : tags) {
            out.putShort((short) tag[0]);
            out.putShort((short) tag[1]);
            if (tag[2] == -1 || tag[2] == -2) {
                final boolean isOffsets = tag[2] == -1;
                final int count = isOffsets ? offsets.length : byteCounts.length;
                out.putInt(count);
                if (count == 1) {
                    out.putInt(isOffsets ? offsets[0] + pixelsAt : byteCounts[0]);
                } else {
                    out.putInt(isOffsets ? offsetsArrayAt : countsArrayAt);
                }
            } else {
                out.putInt(1);
                if (tag[1] == 3) {
                    out.putShort((short) tag[2]).putShort((short) 0);   // SHORT, left-justified
                } else {
                    out.putInt(tag[2]);
                }
            }
        }
        out.putInt(0);                                  // no next IFD

        for (int offset : offsets) {
            out.putInt(offset + pixelsAt);
        }
        for (int count : byteCounts) {
            out.putInt(count);
        }
        out.put(pixels.toByteArray());

        Files.write(file.toPath(), out.array());
        return file;
    }
}
