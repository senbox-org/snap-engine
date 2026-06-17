package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.runtime.Config;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


public final class CopernicusDirectElevationTile implements ElevationTile {


    private final ElevationModel demModel;
    private final CopernicusTileSource source;
    private final int sourceWidth;
    private final int sourceHeight;
    private final int targetWidth;
    private final int targetHeight;
    private final int tileXIndex;
    private final int tileYIndex;
    private final long fullTileCacheMaxBytes;
    private final int blockRows;
    private final boolean useDEMGravitationalModel;
    private final LinkedHashMap<Integer, float[]> blockCache;

    private float[] fullTile;
    private long blockCacheSizeInBytes;
    private volatile boolean disposed;


    public CopernicusDirectElevationTile(final ElevationModel demModel, final CopernicusTileSource source,
                                         final int targetWidth, final int targetHeight,
                                         final int tileXIndex, final int tileYIndex) {
        this(demModel, source, targetWidth, targetHeight, tileXIndex, tileYIndex,
             Config.instance().preferences().getLong("snap.dem.copernicus.fullTileCacheMaxBytes", 16777216L),
             Config.instance().preferences().getInt("snap.dem.copernicus.blockRows", 64),
             Config.instance().preferences().getBoolean("snap.useDEMGravitationalModel", true));
    }

    CopernicusDirectElevationTile(final ElevationModel demModel, final CopernicusTileSource source,
                                  final int targetWidth, final int targetHeight,
                                  final int tileXIndex, final int tileYIndex,
                                  final long fullTileCacheMaxBytes, final int blockRows,
                                  final boolean useDEMGravitationalModel) {
        this.demModel = demModel;
        this.source = source;
        sourceWidth = source.getWidth();
        sourceHeight = source.getHeight();
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.tileXIndex = tileXIndex;
        this.tileYIndex = tileYIndex;
        this.fullTileCacheMaxBytes = Math.max(0, fullTileCacheMaxBytes);
        this.blockRows = Math.max(1, blockRows);
        this.useDEMGravitationalModel = useDEMGravitationalModel;
        blockCache = new LinkedHashMap<>(16, 0.75f, true);
    }


    @Override
    public synchronized void dispose() {
        clearCache();
        source.close();
        disposed = true;
    }

    @Override
    public synchronized float getSample(final int pixelX, final int pixelY) throws Exception {
        if (disposed) {
            throw new IllegalStateException("Copernicus DEM tile has been disposed");
        }
        if (shouldCacheFullTile()) {
            if (fullTile == null) {
                fullTile = loadRows(0, targetHeight);
            }
            return fullTile[pixelY * targetWidth + pixelX];
        }

        final int blockStartY = (pixelY / blockRows) * blockRows;
        float[] block = blockCache.get(blockStartY);
        if (block == null) {
            final int rows = Math.min(blockRows, targetHeight - blockStartY);
            block = loadRows(blockStartY, rows);
            blockCache.put(blockStartY, block);
            blockCacheSizeInBytes += (long) block.length * Float.BYTES;
            evictBlocksIfNeeded();
        }
        return block[(pixelY - blockStartY) * targetWidth + pixelX];
    }

    @Override
    public synchronized void clearCache() {
        fullTile = null;
        blockCache.clear();
        blockCacheSizeInBytes = 0;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    private boolean shouldCacheFullTile() {
        return fullTileCacheMaxBytes > 0 && (long) targetWidth * targetHeight * Float.BYTES <= fullTileCacheMaxBytes;
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
        for (int x = 0; x < targetWidth; x++) {
            if (targetRows[targetOffset + x] != demModel.getDescriptor().getNoDataValue()) {
                final GeoPos geoPos = demModel.getGeoPos(new PixelPos(tileXIndex * targetWidth + x,
                                                                       tileYIndex * targetHeight + localY));
                targetRows[targetOffset + x] += egm.getEGM(geoPos.lat, geoPos.lon, workspace);
            }
        }
    }

    private void evictBlocksIfNeeded() {
        if (fullTileCacheMaxBytes <= 0) {
            return;
        }
        final Iterator<Map.Entry<Integer, float[]>> iterator = blockCache.entrySet().iterator();
        while (blockCacheSizeInBytes > fullTileCacheMaxBytes && blockCache.size() > 1 && iterator.hasNext()) {
            final Map.Entry<Integer, float[]> entry = iterator.next();
            blockCacheSizeInBytes -= (long) entry.getValue().length * Float.BYTES;
            iterator.remove();
        }
    }
}
