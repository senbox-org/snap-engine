/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.jp2.reader;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.metadata.XmlMetadataParser;
import org.esa.snap.core.metadata.XmlMetadataParserFactory;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.dataio.ImageRegistryUtils;
import org.esa.snap.jp2.reader.internal.JP2MultiLevelSource;
import org.esa.snap.jp2.reader.internal.JP2ProductReaderConstants;
import org.esa.snap.jp2.reader.metadata.CodeStreamInfo;
import org.esa.snap.jp2.reader.metadata.ImageInfo;
import org.esa.snap.jp2.reader.metadata.Jp2XmlMetadata;
import org.esa.snap.jp2.reader.metadata.OpjDumpFile;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;
import org.esa.snap.lib.openjpeg.utils.OpenJpegUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.esa.snap.lib.openjpeg.utils.OpenJpegUtils.getTileLayoutWithInputStream;
import static org.esa.snap.lib.openjpeg.utils.OpenJpegUtils.validateOpenJpegExecutables;

/**
 * Generic reader for JP2 files.
 *
 * @author Cosmin Cara
 * modified 20191108 to support parameters for reader by Denisa Stefanescu
 */
public class JP2ProductReader extends AbstractProductReader {

    private static final Logger logger = Logger.getLogger(JP2ProductReader.class.getName());

    static {
        XmlMetadataParserFactory.registerParser(Jp2XmlMetadata.class, new XmlMetadataParser<>(Jp2XmlMetadata.class));
    }

    private VirtualJP2File virtualJp2File;

    public JP2ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    public void close() throws IOException {
        super.close();

        closeResources();
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        if (this.virtualJp2File != null) {
            throw new IllegalStateException("There is already a file.");
        }

        boolean success = false;
        try {
            Object productInput = super.getInput(); // invoke the 'getInput' method from the parent class
            ProductSubsetDef subsetDef = super.getSubsetDef(); // invoke the 'getSubsetDef' method from the parent class

            Path jp2File = AbstractProductReader.convertInputToPath(productInput);

            if (getReaderPlugIn().getDecodeQualification(productInput) == DecodeQualification.UNABLE) {
                throw new IOException("The selected product cannot be read with the current reader.");
            }

            this.virtualJp2File = new VirtualJP2File(jp2File, getClass());

            OpjDumpFile opjDumpFile = readJP2FileHeader(jp2File, this.virtualJp2File);

            ImageInfo imageInfo = opjDumpFile.getImageInfo();
            CodeStreamInfo csInfo = opjDumpFile.getCodeStreamInfo();
            Jp2XmlMetadata metadata = opjDumpFile.getMetadata();

            int defaultImageWidth = imageInfo.getWidth();
            int defaultImageHeight = imageInfo.getHeight();

            Rectangle productBounds;
            if (subsetDef == null || subsetDef.getSubsetRegion() == null) {
                productBounds = new Rectangle(0, 0, defaultImageWidth, defaultImageHeight);
            } else {
                GeoCoding productDefaultGeoCoding = null;
                if (metadata != null) {
                    Point2D origin = metadata.getOrigin();
                    productDefaultGeoCoding = computeCrsGeoCoding(origin, metadata, defaultImageWidth, defaultImageHeight, null);
                    if (productDefaultGeoCoding == null) {
                        Map<String, TiePointGrid> tiePointGrids = computeTiePointGrids(origin, defaultImageWidth, defaultImageHeight, metadata);
                        if (!tiePointGrids.isEmpty()) {
                            TiePointGrid lonGrid = tiePointGrids.get("lon");
                            TiePointGrid latGrid = tiePointGrids.get("lat");
                            productDefaultGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);
                        }
                    }
                }
                productBounds = subsetDef.getSubsetRegion().computeProductPixelRegion(productDefaultGeoCoding, defaultImageWidth, defaultImageHeight, false);
            }
            Product product = new Product(this.virtualJp2File.getFileName(), JP2ProductReaderConstants.TYPE, productBounds.width, productBounds.height);
            product.setFileLocation(jp2File.toFile());
            product.setProductReader(this);

            Dimension defaultJAIReadTileSize = JAI.getDefaultTileSize();
            product.setPreferredTileSize(defaultJAIReadTileSize);

            MetadataElement metadataRoot = product.getMetadataRoot();
            if (subsetDef == null || !subsetDef.isIgnoreMetadata()) {
                metadataRoot.addElement(imageInfo.toMetadataElement());
                metadataRoot.addElement(csInfo.toMetadataElement());
            }
            if (metadata != null) {
                metadata.setFileName(jp2File.toString());
                if (subsetDef == null || !subsetDef.isIgnoreMetadata()) {
                    metadataRoot.addElement(metadata.getRootElement());
                }
                addGeoCoding(product, metadata, subsetDef, defaultImageWidth, defaultImageHeight);
            }

            addBands(product, imageInfo, csInfo, productBounds, subsetDef, defaultJAIReadTileSize);

            success = true;

            return product;
        } catch (RuntimeException | IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException(exception);
        } finally {
            if (!success) {
                closeResources();
            }
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm)
                                          throws IOException {
        // do nothing
    }

    private void closeResources() {
        if (this.virtualJp2File != null) {
            try {
                this.virtualJp2File.deleteLocalFilesOnExit();
            } catch (IOException e) {
                // ignore
            }
            this.virtualJp2File = null;
        }
        System.gc();
    }

    private static void addGeoCoding(Product product, Jp2XmlMetadata metadata, ProductSubsetDef subsetDef, int defaultProductWidth, int defaultProductHeight) {
        int imageWidth = product.getSceneRasterWidth();
        int imageHeight = product.getSceneRasterHeight();
        Rectangle subsetRegion = null;
        if (subsetDef != null) {
            subsetRegion = subsetDef.getRegion();
        }
        Point2D origin = metadata.getOrigin();
        GeoCoding geoCoding = computeCrsGeoCoding(origin, metadata, defaultProductWidth, defaultProductHeight, subsetRegion);
        if (geoCoding == null) {
            Map<String, TiePointGrid> tiePointGrids = computeTiePointGrids(origin, imageWidth, imageHeight, metadata);
            if (!tiePointGrids.isEmpty()) {
                TiePointGrid lonGrid = tiePointGrids.get("lon");
                TiePointGrid latGrid = tiePointGrids.get("lat");
                if (subsetDef != null) {
                    lonGrid = TiePointGrid.createSubset(lonGrid, subsetDef);
                    latGrid = TiePointGrid.createSubset(latGrid, subsetDef);
                }
                geoCoding = new TiePointGeoCoding(latGrid, lonGrid);
                product.addTiePointGrid(latGrid);
                product.addTiePointGrid(lonGrid);
            }
        }
        if (geoCoding != null) {
            product.setSceneGeoCoding(geoCoding);
        }
    }

    public static Map<String, TiePointGrid> computeTiePointGrids(Point2D origin, int imageWidth, int imageHeight, Jp2XmlMetadata metadata) {
        Map<String, TiePointGrid> tiePointGrids = new HashMap<>();
        float[] latPoints = null;
        float[] lonPoints = null;
        if (origin != null) {
            float oX = (float) origin.getX();
            float oY = (float) origin.getY();
            float h = (float) imageHeight * (float) metadata.getStepY();
            float w = (float) imageWidth * (float) metadata.getStepX();
            latPoints = new float[]{oY + h, oY + h, oY, oY};
            lonPoints = new float[]{oX, oX + w, oX, oX + w};
        } else {
            List<Point2D> polygonPositions = metadata.getPolygonPositions();
            if (polygonPositions != null && polygonPositions.size() > 0) {
                latPoints = new float[]{(float) polygonPositions.get(0).getX(),
                        (float) polygonPositions.get(1).getX(),
                        (float) polygonPositions.get(3).getX(),
                        (float) polygonPositions.get(2).getX()};
                lonPoints = new float[]{(float) polygonPositions.get(0).getY(),
                        (float) polygonPositions.get(1).getY(),
                        (float) polygonPositions.get(3).getY(),
                        (float) polygonPositions.get(2).getY()};
            }
        }
        if (latPoints != null) {
            TiePointGrid latGrid = buildTiePointGrid("latitude", 2, 2, 0, 0, imageWidth, imageHeight, latPoints);
            TiePointGrid lonGrid = buildTiePointGrid("longitude", 2, 2, 0, 0, imageWidth, imageHeight, lonPoints);
            tiePointGrids.put("lat", latGrid);
            tiePointGrids.put("lon", lonGrid);
        }
        return tiePointGrids;
    }

    private void addBands(Product product, ImageInfo imageInfo, CodeStreamInfo csInfo, Rectangle productBounds, ProductSubsetDef subsetDef, Dimension defaultJAIReadTileSize) {
        List<CodeStreamInfo.TileComponentInfo> componentTilesInfo = csInfo.getComponentTilesInfo();
        int numBands = componentTilesInfo.size();

        Dimension defaultImageSize = new Dimension(imageInfo.getWidth(), imageInfo.getHeight());
        JP2ImageFile jp2ImageFile = new JP2ImageFile(this.virtualJp2File);
        Path localCacheFolder = this.virtualJp2File.getLocalCacheFolder();
        Dimension decompressedTileSize = new Dimension(csInfo.getTileWidth(), csInfo.getTileHeight());

        for (int bandIndex = 0; bandIndex < numBands; bandIndex++) {
            String bandName = "band_" + (bandIndex + 1);
            if (subsetDef == null || subsetDef.isNodeAccepted(bandName)) {
                ImageInfo.ImageInfoComponent bandImageInfo = imageInfo.getComponents().get(bandIndex);
                int snapDataType = getSnapDataTypeFromImageInfo(bandImageInfo);
                int awtDataType = getAwtDataTypeFromImageInfo(bandImageInfo);

                Band band = new Band(bandName, snapDataType, product.getSceneRasterWidth(), product.getSceneRasterHeight());

                JP2MultiLevelSource multiLevelSource = new JP2MultiLevelSource(jp2ImageFile, localCacheFolder, defaultImageSize, productBounds, numBands, bandIndex, decompressedTileSize,
                                                                     csInfo.getNumResolutions(), awtDataType, product.getSceneGeoCoding(), defaultJAIReadTileSize);

                ImageLayout imageLayout = multiLevelSource.buildMultiLevelImageLayout();
                band.setSourceImage(new DefaultMultiLevelImage(multiLevelSource, imageLayout));

                product.addBand(band);
            }
        }
    }

    private static int getSnapDataTypeFromImageInfo(ImageInfo.ImageInfoComponent imageInfo) {
        int precision = imageInfo.getPrecision();
        boolean signed = imageInfo.isSigned();
        if (!signed && precision == 16) {
            return ProductData.TYPE_UINT16;
        }
        return OpenJpegUtils.PRECISION_TYPE_MAP.get(precision);
    }

    private static int getAwtDataTypeFromImageInfo(ImageInfo.ImageInfoComponent imageInfo) {
        int precision = imageInfo.getPrecision();
        boolean signed = imageInfo.isSigned();
        if (!signed && precision == 16) {
            return DataBuffer.TYPE_USHORT;
        }
        return OpenJpegUtils.DATA_TYPE_MAP.get(precision);
    }

    public static CrsGeoCoding computeCrsGeoCoding(Point2D origin, Jp2XmlMetadata metadata, int defaultProductWidth, int defaultProductHeight, Rectangle subsetRegion) {
        String crsGeoCoding = metadata.getCrsGeocoding();
        if (crsGeoCoding != null && origin != null) {
            try {
                CoordinateReferenceSystem mapCRS = CRS.decode(crsGeoCoding.replace("::", ":"));
                return ImageUtils.buildCrsGeoCoding(origin.getX(), origin.getY(), metadata.getStepX(), -metadata.getStepY(),
                                                    defaultProductWidth, defaultProductHeight, mapCRS, subsetRegion);
            } catch (RuntimeException exception) {
                throw  exception;
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to create ge geo coding.", exception);
            }
        }
        return null;
    }

    private static OpjDumpFile readJP2FileHeader(Path jp2File, VirtualJP2File virtualJp2File) throws IOException, InterruptedException {
        OpjDumpFile opjDumpFile = new OpjDumpFile();
        if (OpenJpegUtils.canReadJP2FileHeaderWithOpenJPEG()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Use external application to read the header of the JP2 file '" + jp2File.toString() + "'.");
            }

            if (!validateOpenJpegExecutables(OpenJpegExecRetriever.getOpjDump(), OpenJpegExecRetriever.getOpjDecompress())) {
                throw new IOException("Invalid OpenJpeg executables");
            }
            Path localJp2File = virtualJp2File.getLocalFile();
            opjDumpFile.readHeaderWithOpenJPEG(localJp2File);
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Use input stream to read the header of the JP2 file '" + jp2File.toString() + "'.");
            }

            opjDumpFile.readHeaderWithInputStream(jp2File, 5 * 1024, true);
        }
        return opjDumpFile;
    }
}
