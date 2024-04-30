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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.geocoding.GeoCodingFactory;
import org.esa.snap.core.dataio.geometry.VectorDataNodeIO;
import org.esa.snap.core.dataio.geometry.VectorDataNodeReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.input.DOMBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * The <code>DimapProductReader</code> class is an implementation of the {@code ProductReader} interface
 * exclusively for data products having the BEAM-DIMAP product format.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see DimapProductReaderPlugIn
 * @see DimapProductWriterPlugIn
 */
public class DimapProductReader extends AbstractProductReader {

    private Product product;

    private File inputDir;
    private File inputFile;
    private Map<Band, ImageInputStream> bandInputStreams;
    private Map<TiePointGrid, TpgDomInfo> tiePointGridInfoMap;

    private Map<Band, File> bandDataFiles;
    private Set<ReaderExtender> readerExtenders;

    /**
     * Construct a new instance of a product reader for the given BEAM-DIMAP product reader plug-in.
     *
     * @param readerPlugIn the given BEAM-DIMAP product writer plug-in, must not be {@code null}
     */
    public DimapProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public Product getProduct() {
        return product;
    }

    public File getInputDir() {
        return inputDir;
    }

    public File getInputFile() {
        return inputFile;
    }

    /**
     * Provides an implementation of the {@code readProductNodes} interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>This method is called as a last step in the {@code readProductNodes(input, subsetInfo)} method.
     *
     * @throws java.io.IOException        if an I/O error occurs
     * @throws IllegalFileFormatException if the input file in not decodeable
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Product product = processProduct(null);
        if (readerExtenders != null) {
            for (ReaderExtender readerExtender : readerExtenders) {
                readerExtender.completeProductNodesReading(product);
            }
        }
        return product;
    }

    // todo - Put this into interface ReconfigurableProductReader and make DimapProductReader implement it
    public void bindProduct(Object input, Product existingProduct) throws IOException {
        Assert.notNull(input, "input");
        Assert.notNull(existingProduct, "existingProduct");
        setInput(input);
        processProduct(existingProduct);
    }

    protected Product processProduct(Product existingProduct) throws IOException {
        initInput();
        Document dom = readDom();

        product = existingProduct == null ? DimapProductHelpers.createProduct(dom, DimapProductConstants.DIMAP_FORMAT_NAME, null) : existingProduct;
        product.setProductReader(this);

        if (existingProduct == null) {
            tiePointGridInfoMap = readTiePointGrids(dom);
        }

        bindBandsToFiles(dom);
        if (existingProduct == null) {
            readVectorData(Product.DEFAULT_IMAGE_CRS, true);

            // read GCPs and pins from DOM (old-style)
            DimapProductHelpers.addGcps(dom, product);
            DimapProductHelpers.addPins(dom, product);

            initGeoCodings(dom);
            readVectorData(product.getSceneCRS(), false);
            DimapProductHelpers.addMaskUsages(dom, product);
        }
        product.setFileLocation(inputFile);
        product.setModified(false);
        return product;
    }

    private void initGeoCodings(Document dom) throws IOException {
        final GeoCoding[] geoCodings = DimapProductHelpers.createGeoCoding(dom, product);
        if (geoCodings != null) {
            if (geoCodings.length == 1) {
                product.setSceneGeoCoding(geoCodings[0]);
            } else if (geoCodings.length == product.getNumBands()) {
                for (int i = 0; i < geoCodings.length; i++) {
                    final Band band = product.getBandAt(i);
                    if (product.getSceneRasterWidth() == band.getRasterWidth()
                            && product.getSceneRasterHeight() == band.getRasterHeight()) {
                        product.setSceneGeoCoding(geoCodings[i]);
                    }
                    band.setGeoCoding(geoCodings[i]);
                }
            }
        } else {
            final Band lonBand = product.getBand("longitude");
            final Band latBand = product.getBand("latitude");
            if (latBand != null && lonBand != null) {
                final GeoCoding geoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand);
                product.setSceneGeoCoding(geoCoding);
            }
        }
    }

    private void bindBandsToFiles(Document dom) {
        bandDataFiles = DimapProductHelpers.getBandDataFiles(dom, product, inputDir);
        final Band[] bands = product.getBands();
        for (final Band band : bands) {
            if (band instanceof VirtualBand || band instanceof FilterBand) {
                continue;
            }
            final File dataFile = bandDataFiles.get(band);
            if (dataFile == null || !dataFile.canRead()) {
                SystemUtils.LOG.warning(
                        "DimapProductReader: Unable to read file '" + dataFile + "' referenced by '" + band.getName() + "'.");
                SystemUtils.LOG.warning(
                        "DimapProductReader: Removed band '" + band.getName() + "' from product '" + product.getFileLocation() + "'.");
            }
        }
    }

    private static class TpgDomInfo {
        final File inputFile;
        final int dataType;

        private TpgDomInfo(File inputFile, int dataType) {
            this.inputFile = inputFile;
            this.dataType = dataType;
        }
    }

    private Map<TiePointGrid, TpgDomInfo> readTiePointGrids(Document jDomDocument) {
        final String[] tiePointGridNames = product.getTiePointGridNames();
        final HashMap<TiePointGrid, TpgDomInfo> tpgInfoMap = new HashMap<>();
        for (String tiePointGridName : tiePointGridNames) {
            final TiePointGrid tiePointGrid = product.getTiePointGrid(tiePointGridName);
            String dataFile = DimapProductHelpers.getTiePointDataFile(jDomDocument, tiePointGrid.getName());
            final int dataType = DimapProductHelpers.getTiePointDataType(jDomDocument.getRootElement(), tiePointGrid.getName());
            dataFile = FileUtils.exchangeExtension(dataFile, DimapProductConstants.IMAGE_FILE_EXTENSION);
            final File inputFile = new File(inputDir, dataFile);
            tpgInfoMap.put(tiePointGrid, new TpgDomInfo(inputFile, dataType));
        }
        return tpgInfoMap;
    }

    private Document readDom() throws IOException {
        Document dom;
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            Debug.trace("DimapProductReader: about to open file '" + inputFile + "'..."); /*I18N*/
            final InputStream is = new BufferedInputStream(new FileInputStream(inputFile), 256 * 1024);
            dom = new DOMBuilder().build(builder.parse(is));
            is.close();
        } catch (Exception e) {
            throw new IOException("Failed to read DIMAP XML header.", e);
        }
        return dom;
    }

    private void initInput() {
        if (getInput() instanceof String) {
            inputFile = new File((String) getInput());
        } else if (getInput() instanceof File) {
            inputFile = (File) getInput();
        } else {
            throw new IllegalArgumentException("unsupported input source: " + getInput());  /*I18N*/
        }
        Debug.assertNotNull(inputFile); // super.readProductNodes should have checked getInput() != null already
        inputDir = inputFile.getParentFile();
        if (inputDir == null) {
            inputDir = new File(".");
        }
    }

    /**
     * The template method which is called by the  }
     * method after an optional spatial subset has been applied to the input parameters.
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original  call. Since the
     * <code>destOffsetX</code> and {@code destOffsetY} parameters are already taken into acount in the
     * <code>sourceOffsetX</code> and {@code sourceOffsetY} parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param destBand      the destination band which identifies the data source from which to read the sample values
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @param pm            a monitor to inform the user about progress
     * @throws java.io.IOException if  an I/O error occurs
     * @see #getSubsetDef
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        final File dataFile = bandDataFiles.get(destBand);


        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        // For each scan in the data source
        try {
            try (ImageInputStream inputStream = new FileImageInputStream(dataFile)) {
                int destPos = 0;
                final int type = destBuffer.getType();
                final boolean longType = ProductData.TYPE_INT64 == type;
                final ProductData line = ProductData.createInstance(type, sourceWidth);

                for (int sourceY = sourceOffsetY; sourceY < sourceOffsetY + sourceHeight; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final long sourcePosY = (long) sourceY * destBand.getRasterWidth();
                    long inputPos = sourcePosY + sourceOffsetX;
                    destPos = readLineRasterDataImpl(sourceStepX, sourceWidth, destPos, destWidth, destBuffer,
                            inputStream, longType, line, inputPos);
                }
                pm.worked(1);
            } catch (IOException e) {
                throw new IOException("DimapProductReader: Unable to read file '" + dataFile + "' referenced by '"
                        + destBand.getName() + "'.", e);
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void readTiePointGridRasterData(TiePointGrid tpg, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final TpgDomInfo domInfo = tiePointGridInfoMap.get(tpg);
        try (FileImageInputStream inputStream = new FileImageInputStream(domInfo.inputFile)) {
            final int gridWidth = tpg.getGridWidth();
            final float[] fullData = new float[gridWidth * tpg.getGridHeight()];
            inputStream.seek(0);
            if (domInfo.dataType == ProductData.TYPE_FLOAT32) {
                inputStream.readFully(fullData, 0, fullData.length);
            } else {
                final double[] doubles = new double[fullData.length];
                inputStream.readFully(doubles, 0, doubles.length);
                int i = 0;
                for (double d : doubles) {
                    fullData[i] = (float) d;
                    i++;
                }
            }
            final float[] destData = (float[]) destBuffer.getElems();
            if (destData.length == fullData.length) {
                System.arraycopy(fullData, 0, destData, 0, fullData.length);
            } else {
                for (int y1 = 0; y1 < destHeight; y1++) {
                    final int srcPos = gridWidth * (destOffsetY + y1) + destOffsetX;
                    System.arraycopy(fullData, srcPos, destData, y1 * destWidth, destWidth);
                }
            }
            if (tpg.getDiscontinuity() == TiePointGrid.DISCONT_AUTO) {
                tpg.setDiscontinuity(TiePointGrid.getDiscontinuity(destData));
            }
        } catch (Exception e) {
            throw new IOException(
                    MessageFormat.format("I/O error while reading tie-point grid ''{0}''.", tpg.getName()), e);
        }
    }


    private static int readLineRasterDataImpl(int sourceStepX, int sourceWidth, int destPos, int destWidth, ProductData destBuffer,
                                              ImageInputStream inputStream, boolean longType, ProductData line, long inputPos) throws IOException {
        if (sourceStepX == 1) {
            destBuffer.readFrom(destPos, destWidth, inputStream, inputPos);
            destPos += destWidth;
        } else {
            line.readFrom(0, sourceWidth, inputStream, inputPos);
            for (int lineX = 0; lineX < sourceWidth; lineX += sourceStepX) {
                if (longType) {
                    destBuffer.setElemLongAt(destPos, line.getElemLongAt(lineX));
                } else {
                    destBuffer.setElemDoubleAt(destPos, line.getElemDoubleAt(lineX));
                }
                destPos++;
            }
        }
        return destPos;
    }

    @Override
    public boolean isSubsetReadingFullySupported() {
        return true;
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to {@code close()} are undefined.
     * <p>Overrides of this method should always call {@code super.close();} after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (bandInputStreams == null) {
            return;
        }
        for (ImageInputStream imageInputStream : bandInputStreams.values()) {
            (imageInputStream).close();
        }
        bandInputStreams.clear();
        bandInputStreams = null;
        if (readerExtenders != null) {
            readerExtenders.clear();
            readerExtenders = null;
        }
        super.close();
    }

    private void readVectorData(final CoordinateReferenceSystem modelCrs, final boolean onlyGCPs) {
        String dataDirName = FileUtils.getFilenameWithoutExtension(
                inputFile) + DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION;
        File dataDir = new File(inputDir, dataDirName);
        File vectorDataDir = new File(dataDir, "vector_data");
        if (vectorDataDir.exists()) {
            File[] vectorFiles = getVectorDataFiles(vectorDataDir, onlyGCPs);
            for (File vectorFile : vectorFiles) {
                addVectorDataToProduct(vectorFile, modelCrs);
            }
        }
    }

    private void addVectorDataToProduct(File vectorFile, final CoordinateReferenceSystem modelCrs) {
        try (FileReader reader = new FileReader(vectorFile, StandardCharsets.UTF_8)) {
            FeatureUtils.FeatureCrsProvider crsProvider = new FeatureUtils.FeatureCrsProvider() {
                @Override
                public CoordinateReferenceSystem getFeatureCrs(Product product) {
                    return modelCrs;
                }

                @Override
                public boolean clipToProductBounds() {
                    return false;
                }
            };
            OptimalPlacemarkDescriptorProvider descriptorProvider = new OptimalPlacemarkDescriptorProvider();
            VectorDataNode vectorDataNode = VectorDataNodeReader.read(vectorFile.getName(), reader, product,
                    crsProvider, descriptorProvider, modelCrs,
                    VectorDataNodeIO.DEFAULT_DELIMITER_CHAR,
                    ProgressMonitor.NULL);
            if (vectorDataNode != null) {
                final ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
                final VectorDataNode existing = vectorDataGroup.get(vectorDataNode.getName());
                if (existing != null) {
                    vectorDataGroup.remove(existing);
                }
                vectorDataGroup.add(vectorDataNode);
            }
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.SEVERE, "Error reading '" + vectorFile + "'", e);
        }
    }

    private File[] getVectorDataFiles(File vectorDataDir, final boolean onlyGCPs) {
        return vectorDataDir.listFiles((dir, name) -> {
            if (name.endsWith(VectorDataNodeIO.FILENAME_EXTENSION)) {
                if (onlyGCPs) {
                    return "ground_control_points.csv".equals(name);
                } else {
                    return true;
                }
            }
            return false;
        });
    }

    public void addExtender(ReaderExtender extender) {
        if (extender == null) {
            return;
        }
        if (readerExtenders == null) {
            readerExtenders = new HashSet<>();
        }
        readerExtenders.add(extender);
    }

    private static class OptimalPlacemarkDescriptorProvider
            implements VectorDataNodeReader.PlacemarkDescriptorProvider {

        @Override
        public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType) {
            PlacemarkDescriptorRegistry placemarkDescriptorRegistry = PlacemarkDescriptorRegistry.getInstance();
            if (simpleFeatureType.getUserData().containsKey(
                    PlacemarkDescriptorRegistry.PROPERTY_NAME_PLACEMARK_DESCRIPTOR)) {
                String placemarkDescriptorClass = simpleFeatureType.getUserData().get(
                        PlacemarkDescriptorRegistry.PROPERTY_NAME_PLACEMARK_DESCRIPTOR).toString();
                PlacemarkDescriptor placemarkDescriptor = placemarkDescriptorRegistry.getPlacemarkDescriptor(
                        placemarkDescriptorClass);
                if (placemarkDescriptor != null) {
                    return placemarkDescriptor;
                }
            }
            final PlacemarkDescriptor placemarkDescriptor = placemarkDescriptorRegistry.getPlacemarkDescriptor(
                    simpleFeatureType);
            if (placemarkDescriptor != null) {
                return placemarkDescriptor;
            } else {
                return placemarkDescriptorRegistry.getPlacemarkDescriptor(GeometryDescriptor.class);
            }
        }
    }

    public static abstract class ReaderExtender {

        public abstract void completeProductNodesReading(Product product);
    }
}
