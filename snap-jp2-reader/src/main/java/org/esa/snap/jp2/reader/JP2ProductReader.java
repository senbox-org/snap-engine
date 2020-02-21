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

import static org.esa.snap.lib.openjpeg.utils.OpenJpegUtils.validateOpenJpegExecutables;

/**
 * Generic reader for JP2 files.
 *
 * @author Cosmin Cara
 * modified 20191108 to support parameters for reader by Denisa Stefanescu
 */
public class JP2ProductReader extends AbstractProductReader {

    private static final Logger logger = Logger.getLogger(JP2ProductReader.class.getName());

    private Product product;
    private VirtualJP2File virtualJp2File;

    public JP2ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);

        registerMetadataParser();
    }

    @Override
    public void close() throws IOException {
        if (this.product != null) {
            for (Band band : this.product.getBands()) {
                MultiLevelImage sourceImage = band.getSourceImage();
                if (sourceImage != null) {
                    sourceImage.reset();
                    sourceImage.dispose();
                }
            }
        }
        this.virtualJp2File.deleteLocalFilesOnExit();

        super.close();
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        Path jp2File = AbstractProductReader.convertInputToPath(super.getInput());

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Reading product from the JP2 file '" + jp2File.toString() + "'.");
        }

        if (getReaderPlugIn().getDecodeQualification(super.getInput()) == DecodeQualification.UNABLE) {
            throw new IOException("The selected product cannot be read with the current reader.");
        }

        this.virtualJp2File = new VirtualJP2File(jp2File, getClass());

        try {
            OpjDumpFile opjDumpFile = new OpjDumpFile();
            if (OpenJpegUtils.canReadJP2FileHeaderWithOpenJPEG()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Use external application to read the header of the JP2 file '" + jp2File.toString() + "'.");
                }

                if (!validateOpenJpegExecutables(OpenJpegExecRetriever.getOpjDump(), OpenJpegExecRetriever.getOpjDecompress())) {
                    throw new IOException("Invalid OpenJpeg executables");
                }
                Path localJp2File = this.virtualJp2File.getLocalFile();
                opjDumpFile.readHeaderWithOpenJPEG(localJp2File);
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Use input stream to read the header of the JP2 file '" + jp2File.toString() + "'.");
                }

                opjDumpFile.readHeaderWithInputStream(jp2File, 5 * 1024, true);
            }

            ImageInfo imageInfo = opjDumpFile.getImageInfo();
            CodeStreamInfo csInfo = opjDumpFile.getCodeStreamInfo();
            Jp2XmlMetadata metadata = opjDumpFile.getMetadata();
            ProductSubsetDef subsetDef = getSubsetDef();
            int imageWidth = imageInfo.getWidth();
            int imageHeight = imageInfo.getHeight();
            Dimension defaultProductSize = new Dimension(imageWidth,imageHeight);
            if (subsetDef != null) {
                GeoCoding productDefaultGeoCoding = null;
                if (metadata != null) {
                    Point2D origin = metadata.getOrigin();
                    productDefaultGeoCoding = computeCrsGeoCoding(origin, metadata, defaultProductSize, null);
                    if (productDefaultGeoCoding == null) {
                        Map<String, TiePointGrid> tiePointGrids = computeTiePointGrids(origin, imageWidth, imageHeight, metadata);
                        if(!tiePointGrids.isEmpty()) {
                            TiePointGrid lonGrid = tiePointGrids.get("lon");
                            TiePointGrid latGrid = tiePointGrids.get("lat");
                            productDefaultGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);
                        }
                    }
                }
                Rectangle subsetRegion;
                if (subsetDef == null || subsetDef.getSubsetRegion() == null) {
                    subsetRegion = new Rectangle(0, 0, imageWidth, imageHeight);
                } else {
                    subsetRegion = subsetDef.getSubsetRegion().computeProductPixelRegion(productDefaultGeoCoding, imageWidth, imageHeight, false);
                }
                imageWidth = subsetRegion.width;
                imageHeight = subsetRegion.height;
            }

            this.product = new Product(this.virtualJp2File.getFileName(), JP2ProductReaderConstants.TYPE, imageWidth, imageHeight);

            MetadataElement metadataRoot = this.product.getMetadataRoot();
            if (getSubsetDef() == null || !getSubsetDef().isIgnoreMetadata()) {
                metadataRoot.addElement(imageInfo.toMetadataElement());
                metadataRoot.addElement(csInfo.toMetadataElement());
            }
            if (metadata != null) {
                metadata.setFileName(jp2File.toString());
                if (getSubsetDef() == null || !getSubsetDef().isIgnoreMetadata()) {
                    metadataRoot.addElement(metadata.getRootElement());
                }
                addGeoCoding(metadata, getSubsetDef(), defaultProductSize);
            }

            double[] bandScales = null;
            double[] bandOffsets = null;
            addBands(imageInfo, csInfo, bandScales, bandOffsets);

            this.product.setPreferredTileSize(JAI.getDefaultTileSize());
            this.product.setFileLocation(jp2File.toFile());
            this.product.setModified(false);

            return this.product;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error while reading file '" + jp2File.toString() + "'.", e);
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm)
                                          throws IOException {
        // do nothing
    }

    private void addGeoCoding(Jp2XmlMetadata metadata, ProductSubsetDef subsetDef, Dimension defaultProductSize) {
        int imageWidth = this.product.getSceneRasterWidth();
        int imageHeight = this.product.getSceneRasterHeight();
        Rectangle subsetRegion = null;
        if(subsetDef != null){
            subsetRegion = subsetDef.getRegion();
        }
        Point2D origin = metadata.getOrigin();
        GeoCoding geoCoding = computeCrsGeoCoding(origin, metadata, defaultProductSize, subsetRegion);
        if (geoCoding == null) {
            Map<String, TiePointGrid> tiePointGrids = computeTiePointGrids(origin, imageWidth, imageHeight, metadata);
            if(!tiePointGrids.isEmpty()) {
                TiePointGrid lonGrid = tiePointGrids.get("lon");
                TiePointGrid latGrid = tiePointGrids.get("lat");
                if (subsetDef != null) {
                    lonGrid = TiePointGrid.createSubset(lonGrid, subsetDef);
                    latGrid = TiePointGrid.createSubset(latGrid, subsetDef);
                }
                geoCoding = new TiePointGeoCoding(latGrid, lonGrid);
                this.product.addTiePointGrid(latGrid);
                this.product.addTiePointGrid(lonGrid);
            }
        }
        if (geoCoding != null) {
            this.product.setSceneGeoCoding(geoCoding);
        }
    }

    public static Map<String, TiePointGrid> computeTiePointGrids(Point2D origin, int imageWidth, int imageHeight, Jp2XmlMetadata metadata) {
        Map<String , TiePointGrid> tiePointGrids = new HashMap<>();
        try {
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
                if (polygonPositions != null) {
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
        } catch (Exception ignored) {
            // ignore
        }
        return tiePointGrids;
    }


    private void addBands(ImageInfo imageInfo, CodeStreamInfo csInfo, double[] bandScales, double[] bandOffsets) {
        List<CodeStreamInfo.TileComponentInfo> componentTilesInfo = csInfo.getComponentTilesInfo();

        Rectangle imageReadBounds = new Rectangle(0, 0, this.product.getSceneRasterWidth(), this.product.getSceneRasterHeight());
        int numBands = componentTilesInfo.size();

        if (getSubsetDef() != null && getSubsetDef().getRegion() != null) {
            imageReadBounds = getSubsetDef().getRegion();
        }
        Dimension defaultImageSize = new Dimension(imageInfo.getWidth(), imageInfo.getHeight());
        JP2ImageFile jp2ImageFile = new JP2ImageFile(this.virtualJp2File);
        Path localCacheFolder = this.virtualJp2File.getLocalCacheFolder();

        for (int bandIndex = 0; bandIndex < numBands; bandIndex++) {
            String bandName = "band_" + (bandIndex + 1);
            if (getSubsetDef() == null || getSubsetDef().isNodeAccepted(bandName)) {
                ImageInfo.ImageInfoComponent bandImageInfo = imageInfo.getComponents().get(bandIndex);
                int snapDataType = getSnapDataTypeFromImageInfo(bandImageInfo);
                int awtDataType = getAwtDataTypeFromImageInfo(bandImageInfo);
                Band virtualBand = new Band("band_" + (bandIndex + 1),
                        snapDataType,
                        this.product.getSceneRasterWidth(),
                        this.product.getSceneRasterHeight());

                Dimension decompresedTileSize = new Dimension(csInfo.getTileWidth(), csInfo.getTileHeight());

                JP2MultiLevelSource source = new JP2MultiLevelSource(jp2ImageFile, localCacheFolder, defaultImageSize, imageReadBounds, numBands, bandIndex,
                                                                                 decompresedTileSize, csInfo.getNumResolutions(), awtDataType, this.product.getSceneGeoCoding());
                virtualBand.setSourceImage(new DefaultMultiLevelImage(source));

                if (bandScales != null && bandOffsets != null) {
                    virtualBand.setScalingFactor(bandScales[bandIndex]);
                    virtualBand.setScalingOffset(bandOffsets[bandIndex]);
                }
                this.product.addBand(virtualBand);
            }
        }
    }

    private void registerMetadataParser() {
        XmlMetadataParserFactory.registerParser(Jp2XmlMetadata.class, new XmlMetadataParser<>(Jp2XmlMetadata.class));
    }

    private int getSnapDataTypeFromImageInfo(ImageInfo.ImageInfoComponent imageInfo) {
        int precision = imageInfo.getPrecision();
        boolean signed = imageInfo.isSigned();
        if(!signed && precision == 16) {
            return ProductData.TYPE_UINT16;
        }
        return OpenJpegUtils.PRECISION_TYPE_MAP.get(precision);
    }

    private int getAwtDataTypeFromImageInfo(ImageInfo.ImageInfoComponent imageInfo) {
        int precision = imageInfo.getPrecision();
        boolean signed = imageInfo.isSigned();
        if(!signed && precision == 16) {
            return DataBuffer.TYPE_USHORT;
        }
        return OpenJpegUtils.DATA_TYPE_MAP.get(precision);
    }

    public static CrsGeoCoding computeCrsGeoCoding(Point2D origin, Jp2XmlMetadata metadata, Dimension defaultProductSize, Rectangle subsetRegion){
        String crsGeoCoding = metadata.getCrsGeocoding();
        if (crsGeoCoding != null && origin != null) {
            try {
                CoordinateReferenceSystem mapCRS = CRS.decode(crsGeoCoding.replace("::", ":"));
                return ImageUtils.buildCrsGeoCoding(origin.getX(), origin.getY(),
                                                         metadata.getStepX(), -metadata.getStepY(),
                                                         defaultProductSize, mapCRS, subsetRegion);
            } catch (Exception gEx) {
                logger.warning(gEx.getMessage());
            }
        }
        return null;
    }
}
