/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.LevelImageSupport;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.EngineConfig;

import javax.media.jai.PlanarImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>ProductIO</code> class provides several utility methods concerning data I/O for remote sensing data
 * products.
 * <p> For example, a product can be read in using a single method call:
 * <pre>
 *      Product product =  ProductIO.readProduct("test.prd");
 * </pre>
 * and written out in a similar way:
 * <pre>
 *      ProductIO.writeProduct(product, "HDF5", "test.h5", null);
 * </pre>
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class ProductIO {

    /**
     * The name of the default product format.
     */
    public static final String DEFAULT_FORMAT_NAME = DimapProductConstants.DIMAP_FORMAT_NAME;

    public static final String SYSTEM_PROPERTY_CONCURRENT = "snap.productio.concurrent";
    public static final boolean DEFAULT_WRITE_RASTER_CONCURRENT = true;


    /**
     * Gets a product reader for the given format name.
     *
     * @param formatName the product format name
     *
     * @return a suitable product reader or <code>null</code> if none was found
     */
    public static ProductReader getProductReader(String formatName) {
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        Iterator<ProductReaderPlugIn> it = registry.getReaderPlugIns(formatName);
        if (it.hasNext()) {
            ProductReaderPlugIn plugIn = it.next();
            return plugIn.createReaderInstance();
        }
        return null;
    }

    /**
     * Gets an array of writer product file extensions for the given format name.
     *
     * @param formatName the format name
     *
     * @return an array of extensions or null if the format does not exist
     */
    public static String[] getProductWriterExtensions(String formatName) {
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        Iterator<ProductWriterPlugIn> it = registry.getWriterPlugIns(formatName);
        if (it.hasNext()) {
            ProductWriterPlugIn plugIn = it.next();
            return plugIn.getDefaultFileExtensions();
        }
        return null;
    }

    /**
     * Gets a product writer for the given format name.
     *
     * @param formatName the product format name
     *
     * @return a suitable product writer or <code>null</code> if none was found
     */
    public static ProductWriter getProductWriter(String formatName) {
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        Iterator<ProductWriterPlugIn> it = registry.getWriterPlugIns(formatName);
        if (it.hasNext()) {
            ProductWriterPlugIn plugIn = it.next();
            return plugIn.createWriterInstance();
        }
        return null;
    }

    /**
     * Reads the data product specified by the given file.
     * <p>The returned product will be associated with a reader capable of decoding the file (also
     * see {@link Product#getProductReader() Product.productReader}).
     * If more than one appropriate reader exists in the registry, the returned product will be
     * associated with the reader which is the most preferred according to the product format names
     * supplied as last argument. If no reader capable of decoding the file is capable of handling
     * any of these product formats, the returned product will be associated with the first reader
     * found in the registry which is capable of decoding the file.
     * <p>The method does not automatically load band raster data, so
     * {@link Band#getRasterData() Band.rasterData} will always be null
     * for all bands in the product returned by this method.
     *
     * @param file        the data product file
     * @param formatNames a list of product format names defining the preference, if more than one reader
     *                    found in the registry is capable of decoding the file.
     *
     * @return a data model as an in-memory representation of the given product file or <code>null</code>,
     *         if no appropriate reader was found for the given product file
     *
     * @throws IOException if an I/O error occurs
     * @see #readProduct(String)
     * @see #readProduct(File)
     * @since 4.9
     */
    public static Product readProduct(File file, String... formatNames) throws IOException {
        Guardian.assertNotNull("file", file);

        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getPath());
        }

        final ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();

        for (String formatName : formatNames) {
            ProductReaderPlugIn selectedPlugIn = null;
            if (formatName != null) {
                final Iterator<ProductReaderPlugIn> it = registry.getReaderPlugIns(formatName);

                selectedPlugIn = null;
                while (it.hasNext()) {
                    ProductReaderPlugIn plugIn = it.next();
                    DecodeQualification decodeQualification = plugIn.getDecodeQualification(file);
                    if (decodeQualification == DecodeQualification.INTENDED) {
                        selectedPlugIn = plugIn;
                        break;
                    } else if (decodeQualification == DecodeQualification.SUITABLE) {
                        selectedPlugIn = plugIn;
                    }
                }
            }
            if (selectedPlugIn != null) {
                ProductReader productReader = selectedPlugIn.createReaderInstance();
                if (productReader != null) {
                    return productReader.readProductNodes(file, null);
                }
            }
        }

        return readProductImpl(file, null);
    }

    /**
     * Reads the data product specified by the given file path.
     * <p>The product returned will be associated with the reader appropriate for the given
     * file format (see also {@link Product#getProductReader() Product.productReader}).
     * <p>The method does not automatically read band data, thus
     * {@link Band#getRasterData() Band.rasterData} will always be null
     * for all bands in the product returned by this method.
     *
     * @param filePath the data product file path
     *
     * @return a data model as an in-memory representation of the given product file or <code>null</code> if no
     *         appropriate reader was found for the given product file
     *
     * @throws IOException if an I/O error occurs
     * @see #readProduct(File)
     */
    public static Product readProduct(String filePath) throws IOException {
        return readProductImpl(new File(filePath), null);
    }

    /**
     * Reads the data product specified by the given file.
     * <p>The product returned will be associated with the reader appropriate for the given
     * file format (see also {@link Product#getProductReader() Product.productReader}).
     * <p>The method does not automatically read band data, thus
     * {@link Band#getRasterData() Band.rasterData} will always be null
     * for all bands in the product returned by this method.
     *
     * @param file the data product file
     *
     * @return a data model as an in-memory representation of the given product file or <code>null</code> if no
     *         appropriate reader was found for the given product file
     *
     * @throws IOException if an I/O error occurs
     * @see #readProduct(String)
     */
    public static Product readProduct(File file) throws IOException {
        return readProductImpl(file, null);
    }

    /**
     * Reads the data product specified by the given file.
     * <p>The product returned will be associated with the reader appropriate for the given
     * file format (see also {@link Product#getProductReader() Product.productReader}).
     * <p>The method does not automatically read band data, thus
     * {@link Band#getRasterData() Band.rasterData} will always be null
     * for all bands in the product returned by this method.
     *
     * @param file      the data product file
     * @param subsetDef the subset of a product
     *
     * @return a data model as an in-memory representation of the given product file or <code>null</code> if no
     *         appropriate reader was found for the given product file
     *
     * @throws IOException if an I/O error occurs
     */
    public static Product readProduct(File file, ProductSubsetDef subsetDef) throws IOException {
        return readProductImpl(file, subsetDef);
    }

    private static Product readProductImpl(File file, ProductSubsetDef subsetDef) throws IOException {
        Guardian.assertNotNull("file", file);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getPath());
        }
        final ProductReader productReader = getProductReaderForInput(file);
        if (productReader != null) {
            return productReader.readProductNodes(file, subsetDef);
        }
        return null;
    }

    /**
     * Returns a product reader instance for the given file if any registered product reader can decode the given file.
     *
     * @param file the file to decode.
     *
     * @return a product reader for the given file or <code>null</code> if the file cannot be decoded.
     *
     * @deprecated Since BEAM 4.10. Use {@link #getProductReaderForInput(Object)} instead.
     */
    @Deprecated
    public static ProductReader getProductReaderForFile(File file) {
        return getProductReaderForInput(file);
    }

    /**
     * Tries to find a product reader instance suitable for the given input.
     * The method returns {@code null}, if no
     * registered product reader can handle the given {@code input} value.
     * <p>
     * The {@code input} may be of any type, but most likely it will be a file path given by a {@code String} or
     * {@code File} value. Some readers may also directly support an {@link javax.imageio.stream.ImageInputStream} object.
     *
     * @param input the input object.
     *
     * @return a product reader for the given {@code input} or {@code null} if no registered reader can handle
     *         the it.
     *
     * @see ProductReaderPlugIn#getDecodeQualification(Object)
     * @see ProductReader#readProductNodes(Object, ProductSubsetDef)
     */
    public static ProductReader getProductReaderForInput(Object input) {
        final long startTimeTotal = System.currentTimeMillis();
        Logger logger = EngineConfig.instance().logger();
        logger.fine("Searching reader plugin for '" + input + "'");
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        Iterator<ProductReaderPlugIn> it = registry.getAllReaderPlugIns();
        ProductReaderPlugIn selectedPlugIn = null;
        while (it.hasNext()) {
            ProductReaderPlugIn plugIn = it.next();
            try {

                final long startTime = System.currentTimeMillis();
                DecodeQualification decodeQualification = plugIn.getDecodeQualification(input);
                final long endTime = System.currentTimeMillis();
                logger.fine(String.format("Checking reader plugin %s (took %d ms)", plugIn.getClass().getName(), (endTime - startTime)));
                if (decodeQualification == DecodeQualification.INTENDED) {
                    selectedPlugIn = plugIn;
                    break;
                } else if (decodeQualification == DecodeQualification.SUITABLE) {
                    selectedPlugIn = plugIn;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error attempting to read " + input + " with plugin reader " + plugIn.toString(), e);
            }
        }
        final long endTimeTotal = System.currentTimeMillis();
        logger.fine(String.format("Searching reader plugin took %d ms", (endTimeTotal - startTimeTotal)));
        if (selectedPlugIn != null) {
            logger.fine("Selected " + selectedPlugIn.getClass().getName());
            return selectedPlugIn.createReaderInstance();
        } else {
            logger.fine("No suitable reader plugin found");
            return null;
        }
    }

    /**
     * Writes a product with the specified format to the given file path.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ul>
     * <li>be completely loaded ({@link Band#getRasterData() Band.rasterData} is not <code>null</code>)</li>
     * <li>or the product must be associated with a product reader ({@link Product#getProductReader() Product.productReader} is not <code>null</code>)
     * so that unloaded data can be reloaded.</li>
     * </ul>.
     *
     * @param product    the product, must not be <code>null</code>
     * @param filePath   the file path
     * @param formatName the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                   "BEAM-DIMAP" will be used
     *
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    String filePath,
                                    String formatName) throws IOException {
        writeProduct(product, new File(filePath), formatName, false, ProgressMonitor.NULL);
    }

    /**
     * Writes a product with the specified format to the given file path.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ul>
     * <li>be completely loaded ({@link Band#getRasterData() Band.rasterData} is not <code>null</code>)</li>
     * <li>or the product must be associated with a product reader ({@link Product#getProductReader() Product.productReader} is not <code>null</code>)
     * so that unloaded data can be reloaded.</li>
     * </ul>.
     *
     * @param product    the product, must not be <code>null</code>
     * @param filePath   the file path
     * @param formatName the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                   "BEAM-DIMAP" will be used
     * @param pm         a monitor to inform the user about progress
     *
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    String filePath,
                                    String formatName,
                                    ProgressMonitor pm) throws IOException {
        writeProduct(product, new File(filePath), formatName, false, pm);
    }

    /**
     * Writes a product with the specified format to the given file.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ul>
     * <li>be completely loaded ({@link Band#getRasterData() Band.rasterData} is not <code>null</code>)</li>
     * <li>or the product must be associated with a product reader ({@link Product#getProductReader() Product.productReader} is not <code>null</code>)
     * so that unloaded data can be reloaded.</li>
     * </ul>.
     *
     * @param product     the product, must not be <code>null</code>
     * @param file        the product file , must not be <code>null</code>
     * @param formatName  the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                    "BEAM-DIMAP" will be used
     * @param incremental switch the product writer in incremental mode or not.
     *
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    File file,
                                    String formatName,
                                    boolean incremental) throws IOException {
        writeProduct(product, file, formatName, incremental, ProgressMonitor.NULL);
    }

    /**
     * Writes a product with the specified format to the given file.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ul>
     * <li>be completely loaded ({@link Band#getRasterData() Band.rasterData} is not <code>null</code>)</li>
     * <li>or the product must be associated with a product reader ({@link Product#getProductReader() Product.productReader} is not <code>null</code>)
     * so that unloaded data can be reloaded.</li>
     * </ul>.
     *
     * @param product     the product, must not be <code>null</code>
     * @param file        the product file , must not be <code>null</code>
     * @param formatName  the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                    "BEAM-DIMAP" will be used
     * @param incremental switch the product writer in incremental mode or not.
     * @param pm          a monitor to inform the user about progress
     *
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    File file,
                                    String formatName,
                                    boolean incremental,
                                    ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("file", file);
        if (formatName == null) {
            formatName = DEFAULT_FORMAT_NAME;
        }
        ProductWriter productWriter = getProductWriter(formatName);
        if (productWriter == null) {
            throw new ProductIOException("No product writer for the '" + formatName + "' format available.");
        }
        final EncodeQualification encodeQualification = productWriter.getWriterPlugIn().getEncodeQualification(product);
        if (encodeQualification.getPreservation() == EncodeQualification.Preservation.UNABLE) {
            throw new ProductIOException("Product writer is unable to write product:\n" + encodeQualification.getInfoString());
        }
        productWriter.setIncrementalMode(incremental);

        ProductWriter productWriterOld = product.getProductWriter();
        product.setProductWriter(productWriter);

        IOException ioException = null;
        try {
            long s;
            long e;
            s = System.currentTimeMillis();
            productWriter.writeProductNodes(product, file);
            e = System.currentTimeMillis();
            long t1 = e - s;
            SystemUtils.LOG.fine("write product nodes to " + file.getAbsolutePath() + " took " + StopWatch.getTimeString(t1));

            s = System.currentTimeMillis();
            writeAllBands(product, pm);
            e = System.currentTimeMillis();
            long t2 = e - s;
            SystemUtils.LOG.fine("write all bands of product " + file.getAbsolutePath() + " took " + StopWatch.getTimeString(t2));
            SystemUtils.LOG.fine("Write entire product " + file.getAbsolutePath() + " took " + StopWatch.getTimeString(t1 + t2));
        } catch (IOException e) {
            ioException = e;
        } finally {
            try {
                productWriter.flush();
                productWriter.close();
            } catch (IOException e) {
                if (ioException == null) {
                    ioException = e;
                }
            }
            product.setProductWriter(productWriterOld);
            product.setFileLocation(file);
        }

        if (ioException != null) {
            throw ioException;
        }
    }

    /*
     * This implementation helper methods writes all bands of the given product using the specified product writer. If a
     * band is entirely loaded its data is written out immediately, if not, a band's data raster is written out
     * line-by-line without producing any memory overhead.
     */
    private static void writeAllBands(Product product, ProgressMonitor pm) throws IOException {
        ProductWriter productWriter = product.getProductWriter();
        final boolean concurrent = Config.instance("snap").load().preferences().getBoolean(SYSTEM_PROPERTY_CONCURRENT, DEFAULT_WRITE_RASTER_CONCURRENT);

        // for correct progress indication we need to collect
        // all bands which shall be written to the output
        ArrayList<Band> bandsToWrite = new ArrayList<>();
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            if (productWriter.shouldWrite(band)) {
                bandsToWrite.add(band);
            }
        }

        if (!bandsToWrite.isEmpty()) {
            pm.beginTask("Writing bands of product '" + product.getName() + "'...", bandsToWrite.size());
            try {
                if (concurrent) {
                    writeBandsConcurrent(pm, bandsToWrite);
                } else {
                    writeBandsSequentially(pm, bandsToWrite);
                }
            } finally {
                pm.done();
            }
        }
    }

    private static void writeBandsConcurrent(ProgressMonitor pm, ArrayList<Band> bandsToWrite) throws IOException {
        final int numBands = bandsToWrite.size();
        final int numThreads = Runtime.getRuntime().availableProcessors();
        final int threadsPerBand = numThreads / numBands;
        final int executorSize = threadsPerBand == 0 ? 1 : threadsPerBand;
        Semaphore semaphore = new Semaphore(numThreads);
        List<IOException> ioExceptionCollector = Collections.unmodifiableList(new ArrayList<>());
        for (Band band : bandsToWrite) {
            if (pm.isCanceled()) {
                break;
            }
            ExecutorService executor = null;
            semaphore.acquireUninterruptibly();
            executor = Executors.newFixedThreadPool(executorSize);
            pm.setSubTaskName("Writing band '" + band.getName() + "'");
            ProgressMonitor subPM = SubProgressMonitor.create(pm, 1);
            writeRasterDataFully(subPM, band, executor, semaphore, ioExceptionCollector);
        }
        while (semaphore.availablePermits() < numThreads) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                EngineConfig.instance().logger().log(Level.WARNING,
                                                     "Method ProductIO.writeAllBands(...)' unexpected termination", e);
            }
        }
        for (IOException e : ioExceptionCollector) {
            SystemUtils.LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        if (ioExceptionCollector.size() > 0) {
            IOException ioException = ioExceptionCollector.get(0);
            throw ioException;
        }
    }

    private static void writeBandsSequentially(ProgressMonitor pm, ArrayList<Band> bandsToWrite) throws IOException {
        for (Band band : bandsToWrite) {
            if (pm.isCanceled()) {
                break;
            }
            pm.setSubTaskName("Writing band '" + band.getName() + "'");
            ProgressMonitor subPM = SubProgressMonitor.create(pm, 1);
            writeRasterDataFully(subPM, band, null, null, null);
        }
    }

    /**
     * Constructor. Private, in order to prevent instantiation.
     */
    private ProductIO() {
    }

    private static void writeRasterDataFully(ProgressMonitor pm, Band band, ExecutorService executor, Semaphore semaphore, List<IOException> ioExceptionCollector) throws IOException {
        if (band.hasRasterData()) {
            band.writeRasterData(0, 0, band.getRasterWidth(), band.getRasterHeight(), band.getRasterData(), pm);
            if (semaphore != null) {
                semaphore.release();
            }
        } else {
            final PlanarImage sourceImage = band.getSourceImage();
            final Point[] tileIndices = sourceImage.getTileIndices(
                    new Rectangle(0, 0, sourceImage.getWidth(), sourceImage.getHeight()));
            int numTiles = tileIndices.length;
            pm.beginTask("Writing raster data...", numTiles);
            if (executor != null) {
//                Finisher finisher = new Finisher(band.getName(), pm, semaphore, executor, numTiles);
                Finisher finisher = new Finisher(pm, semaphore, executor, numTiles);
                for (Point tileIndex : tileIndices) {
                    executor.execute(() -> {
                        try {
                            if (pm.isCanceled()) {
                                return;
                            }
                            writeTile(sourceImage, tileIndex, band);
                        } catch (IOException e) {
                            ioExceptionCollector.add(e);
                            pm.setCanceled(true);
                        } finally {
                            finisher.worked();
                        }
                    });
                }
            } else {
                for (final Point tileIndex : tileIndices) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    writeTile(sourceImage, tileIndex, band);
                    pm.worked(1);
                }
            }
        }
    }

    private static void writeTile(PlanarImage sourceImage, Point tileIndex, Band band) throws IOException {
        final Rectangle rect = sourceImage.getTileRect(tileIndex.x, tileIndex.y);
        if (!rect.isEmpty()) {
            final Raster data = sourceImage.getData(rect);
            final ProductData rasterData = band.createCompatibleRasterData(rect.width, rect.height);
            data.getDataElements(rect.x, rect.y, rect.width, rect.height, rasterData.getElems());
            band.writeRasterData(rect.x, rect.y, rect.width, rect.height, rasterData, ProgressMonitor.NULL);
        }
    }

    /**
     * This method is not part of the official API and might change in the future.
     * <p>
     * The method directly delegates to {@link AbstractProductReader#readProductNodesImpl()} which is not
     * publicly available.
     * <p>
     * This overcomes a short coming in the current API. A reader can be used with a SubsetDef but this can not be
     * changed dynamically.
     *
     * @param reader     the reader to read from
     * @param destBand   the band which shall be read
     * @param lvlSupport defines the level (resolution) within the level image pyramid which shall be read
     * @param destRect   the rectangular area which shall be filled with data
     * @param destBuffer the buffer where to put the data
     * @throws IOException in case an error occurs during reading
     */
    // Todo mp 2020-07-03 - https://senbox.atlassian.net/browse/SNAP-1134
    public static void readLevelBandRasterData(AbstractProductReader reader,
                                               Band destBand,
                                               LevelImageSupport lvlSupport,
                                               Rectangle destRect,
                                               ProductData destBuffer) throws IOException {

        Rectangle srcRect = lvlSupport.getSourceRectangle(destRect);
        final int scale = (int) lvlSupport.getScale();

        reader.readBandRasterDataImpl(srcRect.x, srcRect.y, srcRect.width, srcRect.height, scale, scale, destBand,
                                      destRect.x, destRect.y, destRect.width, destRect.height, destBuffer, ProgressMonitor.NULL);
    }

    private static class Finisher {

        private final ProgressMonitor pm;
        private final Semaphore semaphore;
        private final ExecutorService executor;
        private final int work;
        private int counter;

        public Finisher(ProgressMonitor pm, Semaphore semaphore, ExecutorService executor, int counter) {
            this.pm = pm;
            this.semaphore = semaphore;
            this.executor = executor;
            this.work = counter;

        }

        public synchronized void worked() {
            try {
                pm.worked(1);
            } finally {
                counter++;
                if (counter == work) {
                    semaphore.release();
                    executor.shutdown();
                    pm.done();
                }
            }
        }
    }
}
