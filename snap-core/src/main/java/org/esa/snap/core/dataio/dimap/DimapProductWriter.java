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
package org.esa.snap.core.dataio.dimap;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.dataio.cache.VariableCache;
import org.esa.snap.core.dataio.cache.WriteCache;
import org.esa.snap.core.dataio.geometry.VectorDataNodeIO;
import org.esa.snap.core.dataio.geometry.VectorDataNodeWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.runtime.Config;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * The product writer for the BEAM-DIMAP format.
 * <p>
 * The BEAM-DIMAP version history is provided in the API doc of the {@link DimapProductWriterPlugIn}.
 *
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see DimapProductWriterPlugIn
 * @see DimapProductReaderPlugIn
 */
public class DimapProductWriter extends AbstractProductWriter {

    private final static String SYSPROP_USE_CACHE = "snap.dataio.writer.dimap.useCache";

    private Path outputDir;
    private Path outputFile;
    private Map<Band, ImageOutputStream> bandOutputStreams;
    private Path dataOutputDir = Path.of("");
    private boolean incremental = true;
    private Set<WriterExtender> writerExtenders;

    private boolean useCache;
    private WriteCache writeCache;
    private volatile boolean isCanceled = false;

    /**
     * Construct a new instance of a product writer for the given BEAM-DIMAP product writer plug-in.
     *
     * @param writerPlugIn the given BEAM-DIMAP product writer plug-in, must not be <code>null</code>
     */
    public DimapProductWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
        final Preferences preferences = Config.instance().preferences();
        useCache = preferences.getBoolean(SYSPROP_USE_CACHE, true);
        if (useCache) {
            writeCache = new WriteCache();
        }
    }

    private static void checkSourceRegionInsideBandRegion(int sourceWidth, final long sourceBandWidth, int sourceHeight,
                                                          final long sourceBandHeight, int sourceOffsetX,
                                                          int sourceOffsetY) {
        Guardian.assertWithinRange("sourceWidth", sourceWidth, 1, sourceBandWidth);
        Guardian.assertWithinRange("sourceHeight", sourceHeight, 1, sourceBandHeight);
        Guardian.assertWithinRange("sourceOffsetX", sourceOffsetX, 0, sourceBandWidth - sourceWidth);
        Guardian.assertWithinRange("sourceOffsetY", sourceOffsetY, 0, sourceBandHeight - sourceHeight);
    }

    // package access for testing only tb 2020-07-01
    static boolean isFullRaster(int sourceWidth, int sourceHeight, int rasterWidth, int rasterHeight) {
        return (sourceWidth == rasterWidth) && (sourceHeight == rasterHeight);
    }

    private static void checkBufferSize(int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        final int expectedBufferSize = sourceWidth * sourceHeight;
        final int actualBufferSize = sourceBuffer.getNumElems();
        Guardian.assertEquals("sourceWidth * sourceHeight", actualBufferSize, expectedBufferSize);  /*I18N*/
    }

    private static void createPhysicalImageFile(Band band, Path file) throws IOException {
        createPhysicalFile(file, getImageFileSize(band));
    }

    private static void createPhysicalImageFile(TiePointGrid tiePointGrid, Path file) throws IOException {
        createPhysicalFile(file, getImageFileSize(tiePointGrid));
    }

    private static long getImageFileSize(Band band) {
        return (long) ProductData.getElemSize(band.getDataType()) *
               (long) band.getRasterWidth() *
               (long) band.getRasterHeight();
    }

    private static long getImageFileSize(TiePointGrid tpg) {
        return (long) ProductData.getElemSize(tpg.getDataType()) *
               (long) tpg.getGridWidth() *
               (long) tpg.getGridHeight();
    }

    private static String createEnviHeaderFilename(Band band) {
        return band.getName() + EnviHeader.FILE_EXTENSION;
    }

    private static String createImageFilename(Band band) {
        return band.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION;
    }

    private static void createPhysicalFile(Path file, long fileSize) throws IOException {
        final Path parentDir = file.getParent();
        if (parentDir != null) {
            IOHelper.createDirectories(parentDir);
        }
        IOHelper.createFile(file, fileSize);
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public void setCanceled(boolean canceled) {
        this.isCanceled = canceled;
    }

    /**
     * Returns the output directory of the product beeing written.
     */
    public Path getOutputDir() {
        return outputDir;
    }

    /**
     * Returns all band output streams opened so far.
     */
    public Map<Band, ImageOutputStream> getBandOutputStreams() {
        return bandOutputStreams;
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws java.io.IOException      if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {
        final Object output = getOutput();

        Path outputFile = ProductUtils.getProductPath(output);
        Debug.assertNotNull(outputFile); // super.writeProductNodes should have checked this already
        initDirs(outputFile);

        ensureNamingConvention();
        if (writerExtenders != null) {
            for (WriterExtender dimapWriterExtender : writerExtenders) {
                Path parentFile = outputFile.getParent();
                if (parentFile == null) {
                    throw new IllegalStateException("Could not retrieve the parent directory of '" + outputFile.toAbsolutePath() + "'.");
                }
                dimapWriterExtender.intendToWriteDimapHeaderTo(parentFile, getSourceProduct());
            }
        }
        writeDimapDocument();
        writeVectorData();
        writeTiePointGrids();
        getSourceProduct().setProductWriter(this);
        deleteRemovedNodes();
    }

    /**
     * Initializes all the internal file and directory elements from the given output file. This method only must be
     * called if the product writer should write the given data to raw data files without calling of writeProductNodes.
     * This may be at the time when a dimap product was opened and the data should be continuously changed in the same
     * product file without an previous call to the saveProductNodes to this product writer.
     *
     * @param outputFile the dimap header file location.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public void initDirs(final Path outputFile) throws IOException {
        this.outputFile = FileUtils.ensureExtension(outputFile, DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
        Debug.assertNotNull(this.outputFile); // super.writeProductNodes should have checked this already
        outputDir = this.outputFile.getParent();
        if (outputDir == null) {
            outputDir = Path.of(".");
        }
        dataOutputDir = createDataOutputDir();
        IOHelper.createDirectories(dataOutputDir);

        if (!Files.exists(dataOutputDir)) {
            throw new IOException("failed to create data output directory: " + dataOutputDir.toAbsolutePath()); /*I18N*/
        }
    }

    private Path createDataOutputDir() {
        return outputDir.resolve(FileUtils.getFilenameWithoutExtension(outputFile) + DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION);
    }

    private void ensureNamingConvention() {
        if (outputFile != null) {
            getSourceProduct().setName(FileUtils.getFilenameWithoutExtension(outputFile));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBandRasterData(Band sourceBand,
                                    int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight,
                                    ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        if (isCanceled) {
            return;
        }
        Guardian.assertNotNull("sourceBand", sourceBand);
        Guardian.assertNotNull("sourceBuffer", sourceBuffer);
        checkBufferSize(sourceWidth, sourceHeight, sourceBuffer);
        final int sourceBandWidth = sourceBand.getRasterWidth();
        final int sourceBandHeight = sourceBand.getRasterHeight();
        checkSourceRegionInsideBandRegion(sourceWidth, sourceBandWidth, sourceHeight, sourceBandHeight, sourceOffsetX,
                                          sourceOffsetY);
        final ImageOutputStream outputStream = getOrCreateImageOutputStream(sourceBand);
        final boolean fullRaster = isFullRaster(sourceWidth, sourceHeight, sourceBandWidth, sourceBandHeight);
        if (useCache && (!fullRaster)) {
            final VariableCache variableCache = writeCache.get(sourceBand);
            try {
                pm.beginTask("Writing band '" + sourceBand.getName() + "'...", 1);
                final boolean canWrite = variableCache.update(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceBuffer);
                if (canWrite) {
                    synchronized (outputStream) {
                        variableCache.writeCompletedBlocks(outputStream);
                    }
                }
                pm.worked(1);
            } finally {
                pm.done();
            }
        } else {
            long outputPos = (long) sourceOffsetY * sourceBandWidth + (long) sourceOffsetX;
            pm.beginTask("Writing band '" + sourceBand.getName() + "'...", sourceHeight);
            try {
                synchronized (outputStream) {
                    for (int sourcePos = 0; sourcePos < sourceHeight * sourceWidth; sourcePos += sourceWidth) {
                        sourceBuffer.writeTo(sourcePos, sourceWidth, outputStream, outputPos);
                        outputPos += sourceBandWidth;
                        pm.worked(1);
                        if (pm.isCanceled()) {
                            break;
                        }
                    }
                }
            } finally {
                pm.done();
            }
        }
    }

    /**
     * Deletes the physically representation of the product from the hard disk.
     */
    @Override
    public void deleteOutput() throws IOException {
        flush();
        close();
        if (outputFile != null && Files.exists(outputFile) && Files.isRegularFile(outputFile)) {
            IOHelper.delete(outputFile);
        }

        if (dataOutputDir != null && Files.exists(dataOutputDir) && Files.isDirectory(dataOutputDir)) {
            FileUtils.deleteTree(dataOutputDir);
        }
    }

    /*
     * Returns the data output stream associated with the given <code>Band</code>. If no stream exists, one is created
     * and fed into the hash map
     */

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws java.io.IOException on failure
     */
    @Override
    public synchronized void flush() throws IOException {
        if (bandOutputStreams == null || isCanceled) {
            return;
        }
        if (useCache) {
            writeCache.flush(bandOutputStreams);
        }
        for (ImageOutputStream imageOutputStream : bandOutputStreams.values()) {
            imageOutputStream.flush();
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    @Override
    public synchronized void close() throws IOException {
        flush();
        if (bandOutputStreams == null) {
            return;
        }
        for (ImageOutputStream imageOutputStream : bandOutputStreams.values()) {
            (imageOutputStream).close();
        }
        bandOutputStreams.clear();
        bandOutputStreams = null;
        if (writerExtenders != null) {
            writerExtenders.clear();
            writerExtenders = null;
        }
    }

    /*
     * Returns a file associated with the given <code>Band</code>. The method ensures that the file exists and have the
     * right size. Also ensures a recreate if the file not exists or the file have a different file size. A new envi
     * header file was written every call.
     */

    private void writeDimapDocument() throws IOException {
        final DimapHeaderWriter writer = new DimapHeaderWriter(getSourceProduct(), getOutputFile(),
                                                               dataOutputDir.getFileName().toString());
        writer.writeHeader();
        writer.close();
    }

    private Path getOutputFile() {
        return outputFile;
    }

    private void writeTiePointGrids() throws IOException {
        for (int i = 0; i < getSourceProduct().getNumTiePointGrids(); i++) {
            final TiePointGrid tiePointGrid = getSourceProduct().getTiePointGridAt(i);
            writeTiePointGrid(tiePointGrid);
        }
    }

    private void writeTiePointGrid(TiePointGrid tiePointGrid) throws IOException {
        ensureExistingTiePointGridDir();
        try (final ImageOutputStream outputStream = createImageOutputStream(tiePointGrid)) {
            tiePointGrid.getGridData().writeTo(outputStream);
        }
    }

    private void ensureExistingTiePointGridDir() {
        final Path tiePointGridDir = dataOutputDir.resolve(DimapProductConstants.TIE_POINT_GRID_DIR_NAME);
        IOHelper.createDirectories(tiePointGridDir);
    }

    private synchronized ImageOutputStream getOrCreateImageOutputStream(Band band) throws IOException {
        if (bandOutputStreams == null) {
            bandOutputStreams = new HashMap<>();
        }
        if (!bandOutputStreams.containsKey(band)) {
            ImageOutputStream outputStream = createImageOutputStream(band);
            bandOutputStreams.put(band, outputStream);
        }
        return bandOutputStreams.get(band);
    }

    private Path getValidImageFile(Band band) throws IOException {
        writeEnviHeader(band); // always (re-)write ENVI header
        final Path file = getImageFile(band);
        if (Files.exists(file)) {
            if (Files.size(file) != getImageFileSize(band)) {
                createPhysicalImageFile(band, file);
            }
        } else {
            createPhysicalImageFile(band, file);
        }
        return file;
    }

    private Path getValidImageFile(TiePointGrid tiePointGrid) throws IOException {
        writeEnviHeader(tiePointGrid); // always (re-)write ENVI header
        final Path file = getImageFile(tiePointGrid);
        createPhysicalImageFile(tiePointGrid, file);
        return file;
    }

    private void writeEnviHeader(Band band) throws IOException {
        EnviHeader.createPhysicalFile(getEnviHeaderFile(band),
                                      band,
                                      band.getRasterWidth(),
                                      band.getRasterHeight());
    }

    private void writeEnviHeader(TiePointGrid tiePointGrid) throws IOException {
        EnviHeader.createPhysicalFile(getEnviHeaderFile(tiePointGrid),
                                      tiePointGrid,
                                      tiePointGrid.getGridWidth(),
                                      tiePointGrid.getGridHeight());
    }

    private ImageOutputStream createImageOutputStream(Band band) throws IOException {
        return ImageUtils.getImageOutputStream(getValidImageFile(band));
    }

    private ImageOutputStream createImageOutputStream(TiePointGrid tiePointGrid) throws IOException {
        return ImageUtils.getImageOutputStream(getValidImageFile(tiePointGrid));
    }

    private Path getEnviHeaderFile(Band band) {
        return dataOutputDir.resolve(createEnviHeaderFilename(band));
    }

    private Path getEnviHeaderFile(TiePointGrid tiePointGrid) {
        return dataOutputDir.resolve(DimapProductConstants.TIE_POINT_GRID_DIR_NAME)
                .resolve(tiePointGrid.getName() + EnviHeader.FILE_EXTENSION);
    }

    private Path getImageFile(Band band) {
        return dataOutputDir.resolve(createImageFilename(band));
    }

    private Path getImageFile(TiePointGrid tiePointGrid) {
        return dataOutputDir.resolve(DimapProductConstants.TIE_POINT_GRID_DIR_NAME)
                .resolve(tiePointGrid.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION);
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        if (writerExtenders != null) {
            for (WriterExtender dimapWriterWriterExtender : writerExtenders) {
                final boolean shouldWrite = dimapWriterWriterExtender.vetoableShouldWrite(node);
                if (!shouldWrite) {
                    return false;
                }
            }
        }
        if (node instanceof RasterDataNode && ((RasterDataNode) node).isSynthetic()) {
            return false;
        }
        if (node instanceof VirtualBand) {
            return false;
        }
        if (node instanceof FilterBand) {
            return false;
        }
        if (node.isModified()) {
            return true;
        }
        if (!isIncrementalMode()) {
            return true;
        }
        if (!(node instanceof Band)) {
            return true;
        }
        final Path imageFile = getImageFile((Band) node);
        return !(imageFile != null && Files.exists(imageFile));
    }

    /**
     * Returns whether this product writer writes only modified product nodes.
     *
     * @return <code>true</code> if so
     */
    @Override
    public boolean isIncrementalMode() {
        return incremental;
    }

    /**
     * Enables resp. disables incremental writing of this product writer. By default, a reader should enable progress
     * listening.
     *
     * @param enabled enables or disables progress listening.
     */
    @Override
    public void setIncrementalMode(boolean enabled) {
        incremental = enabled;
    }

    private void deleteRemovedNodes() throws IOException {
        final Product product = getSourceProduct();
        final ProductReader productReader = product.getProductReader();
        if (productReader instanceof DimapProductReader) {
            final ProductNode[] removedNodes = product.getRemovedChildNodes();
            if (removedNodes.length > 0) {
                productReader.close();
                for (ProductNode removedNode : removedNodes) {
                    removedNode.removeFromFile(this);
                }
            }
        }
    }

    @Override
    public void removeBand(Band band) {
        if (band != null) {
            final String headerFilename = createEnviHeaderFilename(band);
            final String imageFilename = createImageFilename(band);
            Collection<Path> files = null;
            if (dataOutputDir != null && Files.exists(dataOutputDir)) {
                files = IOHelper.listFiles(dataOutputDir);
            }
            if (files == null) {
                return;
            }
            for (Path file : files) {
                String name = file.getFileName().toString();
                if (Files.isRegularFile(file) && (name.equals(headerFilename) || name.equals(imageFilename))) {
                    IOHelper.delete(file);
                }
            }
        }
    }

    private void writeVectorData() {
        Product product = getSourceProduct();
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();

        Path vectorDataDir = dataOutputDir.resolve("vector_data");
        if (Files.exists(vectorDataDir)) {
            Collection<Path> files = IOHelper.listFiles(vectorDataDir);
            for (Path file : files) {
                IOHelper.delete(file);
            }
        }

        if (vectorDataGroup.getNodeCount() > 0) {
            IOHelper.createDirectories(vectorDataDir);
            for (int i = 0; i < vectorDataGroup.getNodeCount(); i++) {
                VectorDataNode vectorDataNode = vectorDataGroup.get(i);
                writeVectorData(vectorDataDir, vectorDataNode);
            }
        }
    }

    private void writeVectorData(Path vectorDataDir, VectorDataNode vectorDataNode) {
        try {
            VectorDataNodeWriter vectorDataNodeWriter = new VectorDataNodeWriter();
            vectorDataNodeWriter.write(vectorDataNode, vectorDataDir.resolve(vectorDataNode.getName() + VectorDataNodeIO.FILENAME_EXTENSION));
        } catch (IOException e) {
            SystemUtils.LOG.throwing("DimapProductWriter", "writeVectorData", e);
        }
    }

    public void addExtender(WriterExtender writerExtender) {
        if (writerExtenders == null) {
            writerExtenders = new HashSet<>();
        }
        if (writerExtender != null) {
            writerExtenders.add(writerExtender);
        }
    }

    public static abstract class WriterExtender {

        /**
         * Returns wether the given product node is to be written.
         *
         * @param node the product node
         *
         * @return <code>false</code> if the node should not be written
         */
        public abstract boolean vetoableShouldWrite(ProductNode node);

        /**
         * Notification to do preparations relative to output directory.
         *
         * @param outputDir the directory where the DIMAP header file should be written
         */
        public abstract void intendToWriteDimapHeaderTo(Path outputDir, Product product);
    }
}
