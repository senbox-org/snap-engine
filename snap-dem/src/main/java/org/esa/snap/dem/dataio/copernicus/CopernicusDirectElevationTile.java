package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.runtime.Config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;


/**
 * Holds one Copernicus DEM tile, read straight from its GeoTIFF.
 * <p>
 * The tile is filled one row block at a time on first touch and never evicted, so no row is ever
 * decoded twice for the life of the tile. Block height follows the source's own internal tiling
 * (1024 rows for the 30 m DEM, the whole image for the 90 m DEM): a partial read of a tiled
 * GeoTIFF decodes every internal tile it overlaps, so smaller blocks would repeat that work,
 * while larger ones would decode rows nobody asked for.
 * <p>
 * Reads of an already loaded block take no lock. Only the first touch of a block synchronizes,
 * which also serialises access to the single-threaded GeoTIFF reader underneath.
 */
public final class CopernicusDirectElevationTile implements ElevationTile {

    /**
     * Floor for the derived block height, so a strip-organised source that reports a handful of
     * rows per strip does not degenerate into thousands of tiny reads.
     */
    private static final int MIN_DERIVED_BLOCK_ROWS = 256;

    /** Retired in favour of per-block filling; warned about once if a user still has it set. */
    private static final String RETIRED_CACHE_BYTES_KEY = "snap.dem.copernicus.fullTileCacheMaxBytes";
    private static final String BLOCK_ROWS_KEY = "snap.dem.copernicus.blockRows";

    private static final AtomicBoolean RETIRED_KEY_WARNED = new AtomicBoolean();
    private static final AtomicBoolean OVERRIDE_ADJUSTED_WARNED = new AtomicBoolean();

    private final ElevationModel demModel;
    private final CopernicusTileSource source;
    private final int sourceWidth;
    private final int sourceHeight;
    private final int targetWidth;
    private final int targetHeight;
    private final int tileXIndex;
    private final int tileYIndex;
    private final int blockRows;
    private final boolean useDEMGravitationalModel;

    /** One entry per row block, published by {@link AtomicReferenceArray} once fully built. */
    private final AtomicReferenceArray<float[]> blocks;

    private volatile boolean disposed;


    public CopernicusDirectElevationTile(final ElevationModel demModel, final CopernicusTileSource source,
                                         final int targetWidth, final int targetHeight,
                                         final int tileXIndex, final int tileYIndex) {
        this(demModel, source, targetWidth, targetHeight, tileXIndex, tileYIndex,
             readBlockRowsPreference(),
             Config.instance().preferences().getBoolean("snap.useDEMGravitationalModel", true));
    }

    private static int readBlockRowsPreference() {
        if (Config.instance().preferences().get(RETIRED_CACHE_BYTES_KEY, null) != null
                && RETIRED_KEY_WARNED.compareAndSet(false, true)) {
            SystemUtils.LOG.warning(RETIRED_CACHE_BYTES_KEY + " is no longer used and is being ignored."
                    + " Copernicus DEM tiles are now filled one aligned row block at a time and kept for"
                    + " the life of the tile. Bound total DEM memory with"
                    + " snap.dem.copernicus.maxCachedTiles instead.");
        }
        return Config.instance().preferences().getInt(BLOCK_ROWS_KEY, 0);
    }

    /**
     * @param blockRowsOverride rows to read per block, or 0 to derive it from the source's
     *                          internal tiling (the default, and the right choice for a COG).
     *                          A non-zero value is rounded up to a whole multiple of that
     *                          internal tile height - see {@link #alignUp}.
     */
    CopernicusDirectElevationTile(final ElevationModel demModel, final CopernicusTileSource source,
                                  final int targetWidth, final int targetHeight,
                                  final int tileXIndex, final int tileYIndex,
                                  final int blockRowsOverride, final boolean useDEMGravitationalModel) {
        this.demModel = demModel;
        this.source = source;
        sourceWidth = source.getWidth();
        sourceHeight = source.getHeight();
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.tileXIndex = tileXIndex;
        this.tileYIndex = tileYIndex;
        this.useDEMGravitationalModel = useDEMGravitationalModel;
        this.blockRows = computeBlockRows(source, targetHeight, blockRowsOverride);
        blocks = new AtomicReferenceArray<>((targetHeight + blockRows - 1) / blockRows);
        warnIfOverrideAdjusted(source, targetHeight, blockRowsOverride, blockRows);
    }

    private static int computeBlockRows(final CopernicusTileSource source, final int targetHeight,
                                        final int blockRowsOverride) {
        final int preferred = source.getPreferredBlockHeight();
        if (blockRowsOverride > 0) {
            // A source reporting 0 or 1 gives alignUp nothing to align to (every value is a
            // multiple of 1), so hold the override to the same floor the derived path uses
            // rather than letting a tiny block height through unchallenged.
            final int rows = preferred > 1 ? alignUp(blockRowsOverride, preferred)
                                           : Math.max(blockRowsOverride, MIN_DERIVED_BLOCK_ROWS);
            return Math.min(rows, targetHeight);
        }
        if (preferred <= 0) {
            return targetHeight;
        }
        int rows = preferred;
        if (rows < MIN_DERIVED_BLOCK_ROWS) {
            // whole multiples only, so blocks stay aligned to the source's internal tiling
            rows *= (MIN_DERIVED_BLOCK_ROWS + rows - 1) / rows;
        }
        return Math.min(rows, targetHeight);
    }

    /**
     * Rounds a requested block height up to a whole multiple of the source's internal tile
     * height. Reading fewer rows than that, or across its boundaries, makes the reader decode
     * the same internal tiles again for the next block, so an unaligned request is never
     * cheaper than the aligned one that contains it - it is the defect this class was reworked
     * to remove. The override therefore sets a floor, not an exact size.
     */
    private static int alignUp(final int rows, final int preferred) {
        if (preferred <= 0 || rows % preferred == 0) {
            return rows;
        }
        // in long arithmetic: rounding up a near-Integer.MAX_VALUE request (or aligning to a
        // near-Integer.MAX_VALUE tile height) overflows in int, and a negative block height
        // would size the blocks array to zero
        final long aligned = ((long) rows + preferred - 1) / preferred * preferred;
        return (int) Math.min(aligned, Integer.MAX_VALUE);
    }

    private static void warnIfOverrideAdjusted(final CopernicusTileSource source, final int targetHeight,
                                               final int blockRowsOverride, final int effectiveBlockRows) {
        if (blockRowsOverride <= 0 || effectiveBlockRows == blockRowsOverride
                || !OVERRIDE_ADJUSTED_WARNED.compareAndSet(false, true)) {
            return;
        }
        // Two quite different adjustments end up here; saying "not a whole multiple" for a value
        // that was merely capped at the tile height would be untrue.
        final String reason = effectiveBlockRows == targetHeight && blockRowsOverride > targetHeight
                ? " is larger than the DEM tile (" + targetHeight + " rows)"
                : " is not a whole multiple of the DEM GeoTIFF's internal tile height ("
                  + source.getPreferredBlockHeight() + "), and a smaller read decodes the same"
                  + " internal tiles repeatedly";
        SystemUtils.LOG.warning(BLOCK_ROWS_KEY + '=' + blockRowsOverride + reason
                + "; using " + effectiveBlockRows + " instead.");
    }


    @Override
    public synchronized void dispose() {
        if (disposed) {
            // BaseElevationModel.dispose() disposes every cached tile and then walks the
            // ElevationFiles, which dispose the same tile objects again, so a repeat call is
            // normal. CopernicusTileSource.close() carries no repeat-call contract of its own.
            return;
        }
        clearCache();
        source.close();
        disposed = true;
    }

    @Override
    public float getSample(final int pixelX, final int pixelY) throws Exception {
        final int blockIndex = pixelY / blockRows;
        final float[] block = blocks.get(blockIndex);
        if (block != null) {
            // Loaded blocks are immutable, so this needs no lock. It deliberately does not
            // re-check disposed: a thread that already holds the block reference reads valid
            // data even if the tile is disposed underneath it. That window is one array read
            // wide - dispose() clears the blocks first, so every later read takes the slow path
            // below and throws there, exactly as it did before this class was reworked.
            return block[(pixelY - blockIndex * blockRows) * targetWidth + pixelX];
        }
        return loadBlockAndGetSample(blockIndex, pixelX, pixelY);
    }

    private synchronized float loadBlockAndGetSample(final int blockIndex, final int pixelX, final int pixelY)
            throws Exception {
        if (disposed) {
            throw new IllegalStateException("Copernicus DEM tile has been disposed");
        }
        float[] block = blocks.get(blockIndex);
        if (block == null) {
            final int blockStartY = blockIndex * blockRows;
            block = loadRows(blockStartY, Math.min(blockRows, targetHeight - blockStartY));
            blocks.set(blockIndex, block);
            if (SystemUtils.LOG.isLoggable(Level.FINE)) {
                SystemUtils.LOG.fine("Copernicus DEM tile " + tileXIndex + '/' + tileYIndex
                        + ": loaded rows " + blockStartY + ".." + (blockStartY + block.length / targetWidth - 1)
                        + ", now retaining " + (getRetainedBytes() >> 20) + " MiB of "
                        + ((long) targetWidth * targetHeight * Float.BYTES >> 20) + " MiB");
            }
        }
        return block[(pixelY - blockIndex * blockRows) * targetWidth + pixelX];
    }

    @Override
    public synchronized void clearCache() {
        for (int i = 0; i < blocks.length(); i++) {
            blocks.set(i, null);
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    int getBlockRows() {
        return blockRows;
    }

    /**
     * Bytes currently held by loaded blocks. A tile retains only the blocks it has been asked
     * for, so this is well below the whole-tile figure for a product that overlaps part of a
     * tile. Package-private: the tests pin the trailing block's real height with it, and the
     * FINE log line emitted on each block load reports it.
     */
    long getRetainedBytes() {
        long total = 0;
        for (int i = 0; i < blocks.length(); i++) {
            final float[] block = blocks.get(i);
            if (block != null) {
                total += (long) block.length * Float.BYTES;
            }
        }
        return total;
    }

    private float[] loadRows(final int targetStartY, final int rows) throws Exception {
        final float[] targetRows = new float[targetWidth * rows];
        final int sourceStartY = Math.min(targetStartY, sourceHeight - 1);
        final int sourceRows = Math.max(1, Math.min(rows, sourceHeight - sourceStartY));
        final float[] sourceRowsData = source.readRows(sourceStartY, sourceRows);
        final double upSamplingFactor = (double) targetWidth / (double) sourceWidth;

        for (int row = 0; row < rows; row++) {
            final int sourceRow = Math.min(row, sourceRows - 1);
            final int sourceOffset = sourceRow * sourceWidth;
            final int targetOffset = row * targetWidth;
            for (int x = 0; x < targetWidth; x++) {
                final double sourceX = x / upSamplingFactor;
                final int sx0 = Math.min((int) sourceX, sourceWidth - 1);
                final int sx1 = Math.min(sx0 + 1, sourceWidth - 1);
                final double mu = sourceX - sx0;
                final double sv0 = sourceRowsData[sourceOffset + sx0];
                final double sv1 = sourceRowsData[sourceOffset + sx1];
                targetRows[targetOffset + x] = (float) ((1.0 - mu) * sv0 + mu * sv1);
            }
            if (useDEMGravitationalModel) {
                addGravitationalModel(targetStartY + row, targetRows, targetOffset);
            }
        }
        return targetRows;
    }

    private void addGravitationalModel(final int localY, final float[] targetRows, final int targetOffset) throws Exception {
        final EarthGravitationalModel96 egm = EarthGravitationalModel96.instance();
        final double[][] workspace = new double[4][4];
        final double noDataValue = demModel.getDescriptor().getNoDataValue();
        final double lat = 90.0 - tileYIndex - (double) localY / (double) targetHeight;
        final double lon0 = tileXIndex - 180.0;
        final double lonStep = 1.0 / (double) targetWidth;
        for (int x = 0; x < targetWidth; x++) {
            if (targetRows[targetOffset + x] != noDataValue) {
                targetRows[targetOffset + x] += egm.getEGM(lat, lon0 + x * lonStep, workspace);
            }
        }
    }
}
