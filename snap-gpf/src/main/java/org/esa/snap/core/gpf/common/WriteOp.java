/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf.common;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.multilevel.MultiLevelImage;
import org.esa.snap.core.dataio.*;
import org.esa.snap.core.dataio.dimap.DimapProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.internal.OperatorExecutor;
import org.esa.snap.core.gpf.internal.OperatorExecutor.ExecutionOrder;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.core.util.math.MathUtils;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * This standard operator is used to store a data product to a specified file location.
 * <p>
 * It is used by the framework, e.g. the {@code gpt} command line tool, to write target products.
 * <p>
 * It may also be used by clients to write out break-point product files. This is done by placing
 * a {@code WriteOp} node after any node in a processing graph:
 * <p>
 * <pre>
 * &lt;node id="anyNodeId"&gt;
 *     &lt;operator&gt;Write&lt;/operator&gt;
 *     &lt;sources&gt;
 *         &lt;source&gt;${anySourceNodeId}&lt;/source&gt;
 *     &lt;/sources&gt;
 *     &lt;parameters&gt;
 *         &lt;file&gt;/home/norman/eo-data/output/test.nc&lt;/file&gt;
 *         &lt;formatName&gt;NetCDF&lt;/formatName&gt;
 *         &lt;deleteOutputOnFailure&gt;true&lt;/deleteOutputOnFailure&gt;
 *         &lt;writeEntireTileRows&gt;true&lt;/writeEntireTileRows&gt;
 *         &lt;clearCacheAfterRowWrite&gt;true&lt;/clearCacheAfterRowWrite&gt;
 *     &lt;/parameters&gt;
 * &lt;/node&gt;
 * </pre>
 * <p>
 * Clients may also use this operator in a programmatic way:
 * <pre>
 *   WriteOp writeOp = new WriteOp(sourceProduct, file, formatName);
 *   writeOp.setDeleteOutputOnFailure(true);
 *   writeOp.setWriteEntireTileRows(true);
 *   writeOp.writeProduct(progressMonitor);
 * </pre>
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "Write",
        category = "Input-Output",
        version = "1.3",
        authors = "Marco Zuehlke, Norman Fomferra",
        copyright = "(c) 2010 by Brockmann Consult",
        description = "Writes a data product to a file.",
        autoWriteDisabled = true)
public class WriteOp extends Operator {

    @TargetProduct
    private Product targetProduct;

    @SourceProduct(alias = "source", description = "The source product to be written.")
    private Product sourceProduct;

    @Parameter(description = "The output file to which the data product is written.")
    private File file;

    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME,
            description = "The name of the output file format.")
    private String formatName;

    @Parameter(defaultValue = "true",
            description = "If true, all output files are deleted after a failed write operation.")
    private boolean deleteOutputOnFailure = true;

    @Parameter(defaultValue = "false",
            description = "If true, the write operation waits until an entire tile row is computed.")
    private boolean writeEntireTileRows;

    /**
     * @since BEAM 4.9
     */
    @Parameter(defaultValue = "false",
            description = "If true, the internal tile cache is cleared after a tile row has been written. Ignored if writeEntireTileRows=false.")
    private boolean clearCacheAfterRowWrite;

    private boolean[][][] tilesWritten;
    private final Map<Row, Tile[]> writeCache = new HashMap<>();
    private Dimension[] tileSizes;
    private int[] tileCountsX;

    private ProductWriter productWriter;
    private List<Band> writableBands;

    private boolean outputFileExists = false;
    private boolean incremental = false;

    public WriteOp() {
        setParameterDefaultValues();
        setRequiresAllBands(true);
    }

    public WriteOp(Product sourceProduct, File file, String formatName) {
        this();
        Guardian.assertNotNull("file", file);
        this.sourceProduct = sourceProduct;
        this.file = file;
        this.formatName = formatName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public boolean isDeleteOutputOnFailure() {
        return deleteOutputOnFailure;
    }

    public void setDeleteOutputOnFailure(boolean deleteOutputOnFailure) {
        this.deleteOutputOnFailure = deleteOutputOnFailure;
    }

    public boolean isWriteEntireTileRows() {
        return writeEntireTileRows;
    }

    public void setWriteEntireTileRows(boolean writeEntireTileRows) {
        this.writeEntireTileRows = writeEntireTileRows;
    }

    public boolean isClearCacheAfterRowWrite() {
        return clearCacheAfterRowWrite;
    }

    public void setClearCacheAfterRowWrite(boolean clearCacheAfterRowWrite) {
        this.clearCacheAfterRowWrite = clearCacheAfterRowWrite;
    }

    @Override
    public boolean canComputeTile() {
        return true;
    }

    @Override
    public boolean canComputeTileStack() {
        return true;
    }

    /**
     * Writes the source product.
     *
     * @param pm A progress monitor.
     */
    public void writeProduct(ProgressMonitor pm) {
        long startNanos = System.nanoTime();
        getLogger().info("Start writing product " + getTargetProduct().getName() + " to " + getFile());
        OperatorExecutor operatorExecutor = OperatorExecutor.create(this);
        try {
            if (clearCacheAfterRowWrite && writeEntireTileRows) {
                operatorExecutor.setScheduleRowsSeparate(true);
            }
            operatorExecutor.execute(ExecutionOrder.SCHEDULE_ROW_COLUMN_BAND, "Writing...", pm);

            getLogger().info("End writing product " + getTargetProduct().getName() + " to " + getFile());

            double millis = (System.nanoTime() - startNanos) / 1.0E6;
            double seconds = millis / 1.0E3;
            int w = getTargetProduct().getSceneRasterWidth();
            int h = getTargetProduct().getSceneRasterHeight();

            getLogger().info(String.format("Time: %6.3f s total, %6.3f ms per line, %3.6f ms per pixel",
                    seconds,
                    millis / h,
                    millis / h / w));

            stopTileComputationObservation();
        } catch (OperatorException e) {
            if (deleteOutputOnFailure && !outputFileExists) {
                try {
                    productWriter.deleteOutput();
                } catch (Exception e2) {
                    getLogger().warning("Failed to delete output after failure: " + e2.getMessage());
                }
            }
            throw e;
        } finally {
            dispose();
        }
    }

    @Override
    public void initialize() throws OperatorException {
        targetProduct = sourceProduct;
        outputFileExists = targetProduct.getFileLocation() != null && targetProduct.getFileLocation().exists();
        productWriter = ProductIO.getProductWriter(formatName);
        if (productWriter == null) {
            throw new OperatorException("No data product writer for the '" + formatName + "' format available");
        }
        final EncodeQualification encodeQualification = productWriter.getWriterPlugIn().getEncodeQualification(sourceProduct);
        if (encodeQualification.getPreservation() == EncodeQualification.Preservation.UNABLE) {
            throw new OperatorException("Product writer is unable to write this product as '" + formatName +
                    "': " + encodeQualification.getInfoString());
        }
        productWriter.setIncrementalMode(incremental);
        productWriter.setFormatName(formatName); // only effective for ImageIOWriter
        setTargetProduct(targetProduct);
    }

    private static Product copyProduct(Product source) {
        Product targetProduct = new Product(source.getName(), source.getProductType(),
                source.getSceneRasterWidth(),
                source.getSceneRasterHeight());
        ProductUtils.copyProductNodes(source, targetProduct);
        ProductReader productReader = source.getProductReader();
        if (productReader != null) {
            targetProduct.setProductReader(productReader);
        }
        targetProduct.setFileLocation(source.getFileLocation());
        for (Band band : source.getBands()) {
            if(!targetProduct.containsBand( band.getName())) {
                if (band instanceof VirtualBand) {
                    ProductUtils.copyVirtualBandWithStatistics(targetProduct, (VirtualBand) band, band.getName(), false);
                } else {
                    ProductUtils.copyBand(band.getName(), source, targetProduct, true);
                }
            }
        }
        return targetProduct;
    }

    private Dimension determineTileSize(Band band) {
        Dimension tileSize = null;
        if (band.getRasterWidth() == targetProduct.getSceneRasterWidth() &&
                band.getRasterHeight() == targetProduct.getSceneRasterHeight()) {
            tileSize = targetProduct.getPreferredTileSize();
        }
        if (tileSize == null) {
            tileSize = JAIUtils.computePreferredTileSize(band.getRasterWidth(),
                    band.getRasterHeight(), 1);
        }
        return tileSize;
    }

    @Override
    public void doExecute(ProgressMonitor pm) {
        targetProduct = copyProduct(targetProduct);
        targetProduct.setProductWriter(productWriter);
        setTargetProduct(targetProduct);
        final Band[] bands = targetProduct.getBands();
        writableBands = new ArrayList<>(bands.length);
        for (final Band band : bands) {
            band.getSourceImage(); // trigger source image creation
            if (productWriter.shouldWrite(band)) {
                writableBands.add(band);
            }
        }
        pm.beginTask("Preparing writing", writableBands.size() + 1);
        try {
            tileSizes = new Dimension[writableBands.size()];
            tileCountsX = new int[writableBands.size()];
            tilesWritten = new boolean[writableBands.size()][][];
            for (int i = 0; i < writableBands.size(); i++) {
                Band writableBand = writableBands.get(i);
                Dimension tileSize = determineTileSize(writableBand);

                tileSizes[i] = tileSize;
                int tileCountX = MathUtils.ceilInt(writableBand.getRasterWidth() / (double) tileSize.width);
                tileCountsX[i] = tileCountX;
                int tileCountY = MathUtils.ceilInt(writableBand.getRasterHeight() / (double) tileSize.height);
                tilesWritten[i] = new boolean[tileCountY][tileCountX];

                if (writeEntireTileRows && i > 0 && !tileSize.equals(tileSizes[0])) {
                    writeEntireTileRows = false;        // don't writeEntireTileRows for multisize bands
                }
                pm.worked(1);
            }
            if (!writableBands.isEmpty()) {
                if (writeEntireTileRows) {
                    targetProduct.setPreferredTileSize(tileSizes[0]);
                }
                // Create not existing directories before writing
                if (file != null && file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                productWriter.writeProductNodes(targetProduct, file);
            }
            pm.worked(1);
        } catch (IOException e) {
            throw new OperatorException("Not able to write product file: '" + file.getAbsolutePath() + "'", e);
        } finally {
            pm.done();
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        int bandIndex = writableBands.indexOf(targetBand);
        if (bandIndex == -1) {
            return;
        }
        try {
            final Rectangle rect = targetTile.getRectangle();
            final Dimension tileSize = tileSizes[bandIndex];
            int tileCountX = tileCountsX[bandIndex];
            int tileX = MathUtils.floorInt(targetTile.getMinX() / (double) tileSize.width);
            int tileY = MathUtils.floorInt(targetTile.getMinY() / (double) tileSize.height);
            if (writeEntireTileRows) {
                Row row = new Row(targetBand, tileY);
                Tile[] tileRowToWrite = updateTileRow(row, tileX, targetTile, tileCountX);
                if (tileRowToWrite != null) {
                    writeTileRow(targetBand, tileRowToWrite, clearCacheAfterRowWrite);
                }
                markTileAsHandled(targetBand, tileX, tileY);
                if (clearCacheAfterRowWrite && tileRowToWrite != null && isRowWrittenCompletely(tileY)) {
                    TileCache tileCache = JAI.getDefaultInstance().getTileCache(); // better use targetBand.getSourceImage().getTileCache()
                    if (tileCache != null) {
                        tileCache.memoryControl();
                    }
                }
            } else {
                final ProductData rawSamples = targetTile.getRawSamples();
                synchronized (productWriter) {
                    productWriter.writeBandRasterData(targetBand, rect.x, rect.y,
                            rect.width, rect.height, rawSamples, pm);
                    removeFromCache(targetBand, targetTile);
                }
                markTileAsHandled(targetBand, tileX, tileY);
            }
            if (productWriter instanceof DimapProductWriter && isProductWrittenCompletely()) {
                // If we get here all tiles are written
                // we can update the header only for DIMAP, so rewrite it, to handle intermediate changes
                synchronized (productWriter) {
                    productWriter.writeProductNodes(targetProduct, file);
                }
            }
        } catch (Exception e) {
            if (deleteOutputOnFailure && !outputFileExists) {
                try {
                    productWriter.deleteOutput();
                } catch (IOException ignored) {
                }
            }
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException("Not able to write product file: '" + file.getAbsolutePath() + "'", e);
            }
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        final Set<Map.Entry<Band, Tile>> entrySet = targetTiles.entrySet();
        for (Map.Entry<Band, Tile> tileEntry : entrySet) {
            final Band band = tileEntry.getKey();
            final Tile tile = tileEntry.getValue();

            computeTile(band, tile, pm);
        }
    }

    private Tile[] updateTileRow(Row key, int tileX, Tile currentTile, int tileCountX) {
        synchronized (writeCache) {
            Tile[] tileRow;
            if (writeCache.containsKey(key)) {
                tileRow = writeCache.get(key);
            } else {
                tileRow = new Tile[tileCountX];
                writeCache.put(key, tileRow);
            }
            tileRow[tileX] = currentTile;
            for (Tile tile : tileRow) {
                if (tile == null) {
                    return null;
                }
            }
            writeCache.remove(key);
            return tileRow;
        }
    }

    /**
     * Writes a row of tiles to a specified band. Optionally clears the cache for each tile row after writing.
     *
     * @param band                    The band to which the tile row is written.
     * @param cacheLine               The array of tiles representing a row to be written.
     * @param clearCacheAfterRowWrite If true, clears the cache for each tile after the row is written.
     * @throws IOException If an I/O error occurs during writing.
     */
    private void writeTileRow(Band band, Tile[] cacheLine, boolean clearCacheAfterRowWrite) throws IOException {
        int lineWidth = 0;
        for (Tile tile : cacheLine) {
            lineWidth += tile.getWidth();
        }

        final ProductData productData = ProductData.createInstance(band.getDataType(), lineWidth * cacheLine[0].getHeight());
        final Object writeBuffer = productData.getElems();
        for (Tile tile : cacheLine) {
            final Object tileBuffer = tile.getRawSamples().getElems();
            final int tileWidth = tile.getWidth();
            final int minX = tile.getMinX();
            for (int line = 0; line < tile.getHeight(); line++) {
                int srcPos = line * tileWidth;
                int destPos = minX + line * lineWidth;
                System.arraycopy(tileBuffer, srcPos, writeBuffer, destPos, tileWidth);
            }
            if (clearCacheAfterRowWrite) {
                removeFromCache(band, tile);
            }
        }

        productWriter.writeBandRasterData(band, 0, cacheLine[0].getMinY(), lineWidth, cacheLine[0].getHeight(), productData, ProgressMonitor.NULL);
    }

    private static void removeFromCache(Band band, Tile tile) {
        MultiLevelImage sourceImage = band.getSourceImage();
        Rectangle dataTileRect = tile.getRectangle();
        Point[] tileIndices = sourceImage.getTileIndices(dataTileRect);
        for (Point tileIndex : tileIndices) {
            Rectangle imageTileRect = sourceImage.getTileRect(tileIndex.x, tileIndex.x);
            if (dataTileRect.intersects(imageTileRect)) {
                ImageManager.removeCachedTile(sourceImage, tileIndex);
            }
        }
    }

    private void markTileAsHandled(Band targetBand, int tileX, int tileY) {
        int bandIndex = writableBands.indexOf(targetBand);
        tilesWritten[bandIndex][tileY][tileX] = true;
    }

    private boolean isRowWrittenCompletely(int rowNumber) {
        for (int bandIndex = 0; bandIndex < writableBands.size(); bandIndex++) {
            for (boolean aYTileWritten : tilesWritten[bandIndex][rowNumber]) {
                if (!aYTileWritten) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isProductWrittenCompletely() {
        for (int bandIndex = 0; bandIndex < writableBands.size(); bandIndex++) {
            for (boolean[] aXTileWritten : tilesWritten[bandIndex]) {
                for (boolean aYTileWritten : aXTileWritten) {
                    if (!aYTileWritten) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void dispose() {
        try {
            productWriter.close();
        } catch (IOException ignore) {
        }
        if (writableBands != null) {
            writableBands.clear();
        }
        writeCache.clear();
        super.dispose();
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(WriteOp.class);
        }
    }

    private static class Row {

        private final Band band;
        private final int tileY;

        private Row(Band band, int tileY) {
            this.band = band;
            this.tileY = tileY;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + band.hashCode();
            result = prime * result + tileY;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            Row other = (Row) obj;
            if (!band.equals(other.band)) {
                return false;
            }
            return tileY == other.tileY;
        }
    }
}
