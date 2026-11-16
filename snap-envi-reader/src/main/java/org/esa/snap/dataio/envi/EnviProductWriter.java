package org.esa.snap.dataio.envi;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.dataio.cache.VariableCache;
import org.esa.snap.core.dataio.cache.WriteCache;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.dimap.DimapProductReader;
import org.esa.snap.core.dataio.dimap.EnviHeader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.runtime.Config;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * The product writer for ENVI products.
 */
public class EnviProductWriter extends AbstractProductWriter {

    private final static String SYSPROP_USE_CACHE = "snap.dataio.writer.envi.useCache";

    protected Path outputDir;
    protected Path outputFile;
    private Map<Band, ImageOutputStream> bandOutputStreams;
    private boolean incremental = true;

    private final boolean useCache;
    private WriteCache writeCache;

    /**
     * Construct a new instance of a product writer for the given ENVI product writer plug-in.
     *
     * @param writerPlugIn the given ENVI product writer plug-in, must not be <code>null</code>
     */
    public EnviProductWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);

        final Preferences preferences = Config.instance().preferences();
        useCache = preferences.getBoolean(SYSPROP_USE_CACHE, true);
        if (useCache) {
            writeCache = new WriteCache();
        }
    }

    private static void checkSourceRegionInsideBandRegion(int sourceWidth, final int sourceBandWidth, int sourceHeight,
                                                          final int sourceBandHeight, int sourceOffsetX,
                                                          int sourceOffsetY) {
        Guardian.assertWithinRange("sourceWidth", sourceWidth, 1, sourceBandWidth);
        Guardian.assertWithinRange("sourceHeight", sourceHeight, 1, sourceBandHeight);
        Guardian.assertWithinRange("sourceOffsetX", sourceOffsetX, 0, sourceBandWidth - sourceWidth);
        Guardian.assertWithinRange("sourceOffsetY", sourceOffsetY, 0, sourceBandHeight - sourceHeight);
    }

    private static void checkBufferSize(int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        final long expectedBufferSize = (long) sourceWidth * (long) sourceHeight;
        final long actualBufferSize = sourceBuffer.getNumElems();
        Guardian.assertEquals("sourceWidth * sourceHeight", actualBufferSize, expectedBufferSize);  /*I18N*/
    }

    private static void createPhysicalImageFile(Band band, Path file) throws IOException {
        createPhysicalFile(file, getImageFileSize(band));
    }

    private static long getImageFileSize(RasterDataNode band) {
        return (long) ProductData.getElemSize(band.getDataType()) *
                (long) band.getRasterWidth() *
                (long) band.getRasterHeight();
    }

    private static void createPhysicalFile(Path file, long fileSize) throws IOException {
        final Path parentDir = file.getParent();
        if (parentDir != null) {
            IOHelper.createDirectories(parentDir);
        }
        IOHelper.createFile(file, fileSize);
    }

    // @todo 3 tb/tb duplicated code -  tb 2020-07-07
    private static boolean isFullRaster(int sourceWidth, int sourceHeight, int rasterWidth, int rasterHeight) {
        return (sourceWidth == rasterWidth) && (sourceHeight == rasterHeight);
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
        getSourceProduct().setProductWriter(this);
        getSourceProduct().setFileLocation(outputDir);
        deleteRemovedNodes();
    }

    /**
     * Initializes all the internal file and directory elements from the given output file. This method only must be
     * called if the product writer should write the given data to raw data files without calling of writeProductNodes.
     * This may be at the time when a dimap product was opened and the data should be continuously changed in the same
     * product file without an previous call to the saveProductNodes to this product writer.
     *
     * @param outputFile the dimap header file location.
     */
    protected void initDirs(final Path outputFile) {
        final String name = FileUtils.getFilenameWithoutExtension(outputFile);
        outputDir = outputFile.getParent();
        if (outputDir == null) {
            outputDir = Path.of(".");
        }
        outputDir = outputDir.resolve(name);
        IOHelper.createDirectories(outputDir);
        this.outputFile = outputDir.resolve(outputFile.getFileName().toString());
    }

    protected void ensureNamingConvention() {
        if (outputFile != null) {
            getSourceProduct().setName(FileUtils.getFilenameWithoutExtension(outputFile));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeBandRasterData(Band sourceBand,
                                    int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight,
                                    ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
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
            try {
                final VariableCache variableCache = writeCache.get(sourceBand);
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
            long outputPos = (long) sourceOffsetY * (long) sourceBandWidth + sourceOffsetX;
            pm.beginTask("Writing band '" + sourceBand.getName() + "'...", 1);//sourceHeight);
            try {
                synchronized (outputStream) {
                    final long max = sourceHeight * sourceWidth;
                    for (int sourcePos = 0; sourcePos < max; sourcePos += sourceWidth) {
                        sourceBuffer.writeTo(sourcePos, sourceWidth, outputStream, outputPos);
                        outputPos += sourceBandWidth;
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }
    }

    /**
     * Deletes the physically representation of the product from the hard disk.
     */
    public void deleteOutput() throws IOException {
        flush();
        close();
        if (outputFile != null && Files.exists(outputFile) && Files.isRegularFile(outputFile)) {
            Files.delete(outputFile);
        }
    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws java.io.IOException on failure
     */
    public void flush() throws IOException {
        if (bandOutputStreams == null) {
            return;
        }
        if (useCache) {
            writeCache.flush(bandOutputStreams);
        }
        for (Object o : bandOutputStreams.values()) {
            ((ImageOutputStream) o).flush();
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    public void close() throws IOException {
        if (bandOutputStreams == null) {
            return;
        }
        for (Object o : bandOutputStreams.values()) {
            ((ImageOutputStream) o).close();
        }
        bandOutputStreams.clear();
        bandOutputStreams = null;
    }

    /**
     * Returns the data output stream associated with the given <code>Band</code>. If no stream exists, one is created
     * and fed into the hash map
     */
    private ImageOutputStream getOrCreateImageOutputStream(Band band) throws IOException {
        ImageOutputStream outputStream = getImageOutputStream(band);
        if (outputStream == null) {
            outputStream = createImageOutputStream(band);
            if (bandOutputStreams == null) {
                bandOutputStreams = new HashMap<>();
            }
            bandOutputStreams.put(band, outputStream);
        }
        return outputStream;
    }

    private ImageOutputStream getImageOutputStream(Band band) {
        if (bandOutputStreams != null) {
            return bandOutputStreams.get(band);
        }
        return null;
    }

    /**
     * Returns a file associated with the given <code>Band</code>. The method ensures that the file exists and have the
     * right size. Also ensures a recreate if the file not exists or the file have a different file size. A new envi
     * header file was written every call.
     */
    protected Path getValidImageFile(Band band) throws IOException {
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

    protected void writeEnviHeader(Band band) throws IOException {
        EnviHeader.createPhysicalFile(getEnviHeaderFile(band),
                                      band,
                                      band.getRasterWidth(),
                                      band.getRasterHeight());
    }

    protected ImageOutputStream createImageOutputStream(Band band) throws IOException {
        return ImageUtils.getImageOutputStream(getValidImageFile(band));
    }

    protected Path getEnviHeaderFile(Band band) {
        return outputDir.resolve(createEnviHeaderFilename(band));
    }

    protected String createEnviHeaderFilename(Band band) {
        return band.getName() + EnviHeader.FILE_EXTENSION;
    }

    private Path getImageFile(Band band) {
        return outputDir.resolve(createImageFilename(band));
    }

    protected String createImageFilename(Band band) {
        return band.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION;
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
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
        return !Files.exists(imageFile);
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

    /**
     * @throws java.io.IOException if an IOException occurs.
     */
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
            if (outputDir != null && Files.exists(outputDir)) {
                files = IOHelper.listFiles(outputDir, Files::isRegularFile);
            }
            if (files == null) {
                return;
            }
            String name;
            for (Path file : files) {
                name = file.getFileName().toString();
                if (Files.isRegularFile(file) && (name.equals(headerFilename) || name.equals(imageFilename))) {
                    try {
                        Files.delete(file);
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    protected Path getOutputDir() {
        return outputDir;
    }
}
