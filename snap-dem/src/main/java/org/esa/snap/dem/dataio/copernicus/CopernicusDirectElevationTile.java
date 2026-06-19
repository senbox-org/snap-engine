package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.runtime.Config;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


public final class CopernicusDirectElevationTile implements ElevationTile {


    private final ElevationModel demModel;
    private final SourceLevel[] sourceLevels;
    private final int targetWidth;
    private final int targetHeight;
    private final int tileXIndex;
    private final int tileYIndex;
    private final long fullTileCacheMaxBytes;
    private final int blockRows;
    private final boolean useDEMGravitationalModel;
    private final double[][] egmWorkspace;

    private volatile boolean disposed;


    public CopernicusDirectElevationTile(final ElevationModel demModel, final CopernicusTileSource source,
                                         final int targetWidth, final int targetHeight,
                                         final int tileXIndex, final int tileYIndex) {
        this(demModel, new CopernicusTileSource[]{source}, targetWidth, targetHeight, tileXIndex, tileYIndex);
    }

    public CopernicusDirectElevationTile(final ElevationModel demModel, final CopernicusTileSource[] sources,
                                         final int targetWidth, final int targetHeight,
                                         final int tileXIndex, final int tileYIndex) {
        this(demModel, sources, targetWidth, targetHeight, tileXIndex, tileYIndex,
             Config.instance().preferences().getLong("snap.dem.copernicus.fullTileCacheMaxBytes", 16777216L),
             Config.instance().preferences().getInt("snap.dem.copernicus.blockRows", 64),
             Config.instance().preferences().getBoolean("snap.useDEMGravitationalModel", true));
    }

    CopernicusDirectElevationTile(final ElevationModel demModel, final CopernicusTileSource source,
                                  final int targetWidth, final int targetHeight,
                                  final int tileXIndex, final int tileYIndex,
                                  final long fullTileCacheMaxBytes, final int blockRows,
                                  final boolean useDEMGravitationalModel) {
        this(demModel, new CopernicusTileSource[]{source}, targetWidth, targetHeight, tileXIndex, tileYIndex,
             fullTileCacheMaxBytes, blockRows, useDEMGravitationalModel);
    }

    CopernicusDirectElevationTile(final ElevationModel demModel, final CopernicusTileSource[] sources,
                                  final int targetWidth, final int targetHeight,
                                  final int tileXIndex, final int tileYIndex,
                                  final long fullTileCacheMaxBytes, final int blockRows,
                                  final boolean useDEMGravitationalModel) {
        this.demModel = demModel;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.tileXIndex = tileXIndex;
        this.tileYIndex = tileYIndex;
        this.fullTileCacheMaxBytes = Math.max(0, fullTileCacheMaxBytes);
        this.blockRows = Math.max(1, blockRows);
        this.useDEMGravitationalModel = useDEMGravitationalModel;
        this.egmWorkspace = new double[4][4];
        if (sources.length == 0) {
            throw new IllegalArgumentException("At least one Copernicus source level is required");
        }
        sourceLevels = new SourceLevel[sources.length];
        for (int i = 0; i < sources.length; i++) {
            sourceLevels[i] = new SourceLevel(sources[i]);
        }
    }


    @Override
    public synchronized void dispose() {
        clearCache();
        for (SourceLevel sourceLevel : sourceLevels) {
            sourceLevel.close();
        }
        disposed = true;
    }

    @Override
    public synchronized float getSample(final int pixelX, final int pixelY) throws Exception {
        if (disposed) {
            throw new IllegalStateException("Copernicus DEM tile has been disposed");
        }

        final SourceLevel sourceLevel = selectSourceLevel();
        if (sourceLevel.usesTargetResolution()) {
            return getTargetResolutionSample(sourceLevel, pixelX, pixelY);
        }
        return getOverviewSample(sourceLevel, pixelX, pixelY);
    }

    @Override
    public synchronized void clearCache() {
        for (SourceLevel sourceLevel : sourceLevels) {
            sourceLevel.clearCache();
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    private SourceLevel selectSourceLevel() {
        final int preferredSourceSize = getPreferredSourceSize();
        SourceLevel selectedLevel = sourceLevels[0];
        int selectedSize = selectedLevel.getSourceSize();
        for (SourceLevel sourceLevel : sourceLevels) {
            final int sourceSize = sourceLevel.getSourceSize();
            if (sourceSize >= preferredSourceSize && sourceSize <= selectedSize) {
                selectedLevel = sourceLevel;
                selectedSize = sourceSize;
            }
        }
        return selectedLevel;
    }

    private int getPreferredSourceSize() {
        if (demModel instanceof CopernicusElevationModel) {
            return ((CopernicusElevationModel) demModel).getPreferredSourceSize();
        }
        return Math.max(targetWidth, targetHeight);
    }

    private float getTargetResolutionSample(final SourceLevel sourceLevel, final int pixelX, final int pixelY) throws Exception {
        if (shouldCacheFullTile()) {
            if (sourceLevel.fullTargetTile == null) {
                sourceLevel.fullTargetTile = loadTargetRows(sourceLevel, 0, targetHeight);
            }
            return sourceLevel.fullTargetTile[pixelY * targetWidth + pixelX];
        }

        final int blockStartY = (pixelY / blockRows) * blockRows;
        float[] block = sourceLevel.targetBlockCache.get(blockStartY);
        if (block == null) {
            final int rows = Math.min(blockRows, targetHeight - blockStartY);
            block = loadTargetRows(sourceLevel, blockStartY, rows);
            sourceLevel.targetBlockCache.put(blockStartY, block);
            sourceLevel.targetBlockCacheSizeInBytes += (long) block.length * Float.BYTES;
            evictBlocksIfNeeded(sourceLevel);
        }
        return block[(pixelY - blockStartY) * targetWidth + pixelX];
    }

    private float getOverviewSample(final SourceLevel sourceLevel, final int pixelX, final int pixelY) throws Exception {
        final float[] sourceData = sourceLevel.getSourceData();
        final float sample = interpolate(sourceData, sourceLevel.sourceWidth, sourceLevel.sourceHeight, pixelX, pixelY);
        return addGravitationalModel(pixelX, pixelY, sample);
    }

    private boolean shouldCacheFullTile() {
        return fullTileCacheMaxBytes > 0 && (long) targetWidth * targetHeight * Float.BYTES <= fullTileCacheMaxBytes;
    }

    private float[] loadTargetRows(final SourceLevel sourceLevel, final int targetStartY, final int rows) throws Exception {
        if (sourceLevel.usesTargetResolution()) {
            final float[] targetRows = sourceLevel.source.readRows(targetStartY, rows);
            if (useDEMGravitationalModel) {
                for (int row = 0; row < rows; row++) {
                    addGravitationalModel(targetStartY + row, targetRows, row * targetWidth);
                }
            }
            return targetRows;
        }

        final float[] targetRows = new float[targetWidth * rows];
        final double yScale = (double) targetHeight / (double) sourceLevel.sourceHeight;
        final int sourceStartY = Math.max(0, Math.min((int) Math.floor(targetStartY / yScale), sourceLevel.sourceHeight - 1));
        final int sourceEndY = Math.max(sourceStartY,
                                        Math.min((int) Math.floor((targetStartY + rows - 1) / yScale) + 1,
                                                 sourceLevel.sourceHeight - 1));
        final int sourceRows = sourceEndY - sourceStartY + 1;
        final float[] sourceRowsData = sourceLevel.source.readRows(sourceStartY, sourceRows);

        for (int row = 0; row < rows; row++) {
            final int targetOffset = row * targetWidth;
            final int targetY = targetStartY + row;
            for (int x = 0; x < targetWidth; x++) {
                targetRows[targetOffset + x] = interpolate(sourceRowsData, sourceLevel.sourceWidth, sourceRows,
                                                           x, targetY, sourceStartY);
            }
            if (useDEMGravitationalModel) {
                addGravitationalModel(targetY, targetRows, targetOffset);
            }
        }
        return targetRows;
    }

    private float interpolate(final float[] sourceData, final int sourceWidth, final int sourceHeight,
                              final int targetX, final int targetY) {
        return interpolate(sourceData, sourceWidth, sourceHeight, targetX, targetY, 0);
    }

    private float interpolate(final float[] sourceData, final int sourceWidth, final int sourceHeight,
                              final int targetX, final int targetY, final int sourceY0) {
        final double sourceX = targetX * (double) sourceWidth / (double) targetWidth;
        final double sourceY = targetY * (double) sourceHeight / (double) targetHeight - sourceY0;
        final int sx0 = Math.max(0, Math.min((int) sourceX, sourceWidth - 1));
        final int sy0 = Math.max(0, Math.min((int) sourceY, sourceHeight - 1));
        final int sx1 = Math.min(sx0 + 1, sourceWidth - 1);
        final int sy1 = Math.min(sy0 + 1, sourceHeight - 1);
        final double muX = Math.max(0.0, Math.min(sourceX - sx0, 1.0));
        final double muY = Math.max(0.0, Math.min(sourceY - sy0, 1.0));
        final double s00 = sourceData[sy0 * sourceWidth + sx0];
        final double s10 = sourceData[sy0 * sourceWidth + sx1];
        final double s01 = sourceData[sy1 * sourceWidth + sx0];
        final double s11 = sourceData[sy1 * sourceWidth + sx1];
        final double top = (1.0 - muX) * s00 + muX * s10;
        final double bottom = (1.0 - muX) * s01 + muX * s11;
        return (float) ((1.0 - muY) * top + muY * bottom);
    }

    private float addGravitationalModel(final int pixelX, final int pixelY, final float sample) throws Exception {
        final double noDataValue = demModel.getDescriptor().getNoDataValue();
        if (!useDEMGravitationalModel || sample == noDataValue) {
            return sample;
        }
        final double lat = 90.0 - tileYIndex - (double) pixelY / (double) targetHeight;
        final double lon = tileXIndex - 180.0 + (double) pixelX / (double) targetWidth;
        return (float) (sample + EarthGravitationalModel96.instance().getEGM(lat, lon, egmWorkspace));
    }

    private void addGravitationalModel(final int localY, final float[] targetRows, final int targetOffset) throws Exception {
        final double noDataValue = demModel.getDescriptor().getNoDataValue();
        final double lat = 90.0 - tileYIndex - (double) localY / (double) targetHeight;
        final double lon0 = tileXIndex - 180.0;
        final double lonStep = 1.0 / (double) targetWidth;
        for (int x = 0; x < targetWidth; x++) {
            if (targetRows[targetOffset + x] != noDataValue) {
                targetRows[targetOffset + x] += EarthGravitationalModel96.instance().getEGM(lat, lon0 + x * lonStep, egmWorkspace);
            }
        }
    }

    private void evictBlocksIfNeeded(final SourceLevel sourceLevel) {
        if (fullTileCacheMaxBytes <= 0) {
            return;
        }
        final Iterator<Map.Entry<Integer, float[]>> iterator = sourceLevel.targetBlockCache.entrySet().iterator();
        while (sourceLevel.targetBlockCacheSizeInBytes > fullTileCacheMaxBytes &&
                sourceLevel.targetBlockCache.size() > 1 && iterator.hasNext()) {
            final Map.Entry<Integer, float[]> entry = iterator.next();
            sourceLevel.targetBlockCacheSizeInBytes -= (long) entry.getValue().length * Float.BYTES;
            iterator.remove();
        }
    }

    private final class SourceLevel {
        private final CopernicusTileSource source;
        private final int sourceWidth;
        private final int sourceHeight;
        private final LinkedHashMap<Integer, float[]> targetBlockCache;

        private float[] fullTargetTile;
        private float[] fullSourceTile;
        private long targetBlockCacheSizeInBytes;

        private SourceLevel(final CopernicusTileSource source) {
            this.source = source;
            this.sourceWidth = source.getWidth();
            this.sourceHeight = source.getHeight();
            this.targetBlockCache = new LinkedHashMap<>(16, 0.75f, true);
        }

        private int getSourceSize() {
            return Math.max(sourceWidth, sourceHeight);
        }

        private boolean usesTargetResolution() {
            return sourceWidth == targetWidth && sourceHeight == targetHeight;
        }

        private float[] getSourceData() throws Exception {
            if (fullSourceTile == null) {
                fullSourceTile = source.readRows(0, sourceHeight);
            }
            return fullSourceTile;
        }

        private void clearCache() {
            fullTargetTile = null;
            fullSourceTile = null;
            targetBlockCache.clear();
            targetBlockCacheSizeInBytes = 0;
        }

        private void close() {
            source.close();
        }
    }
}
