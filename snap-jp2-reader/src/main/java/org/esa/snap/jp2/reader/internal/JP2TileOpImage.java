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

package org.esa.snap.jp2.reader.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelModel;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.snap.jp2.reader.JP2ImageFile;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.SingleBandedOpImage;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.lib.openjpeg.dataio.OpenJP2Decoder;
import org.esa.snap.lib.openjpeg.dataio.Utils;
import org.esa.snap.lib.openjpeg.jp2.TileLayout;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;
import org.esa.snap.runtime.Config;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A JAI operator for handling JP2 tiles.
 *
 * @author Cosmin Cara
 * modified 20191108 to read a specific area from the input product Denisa Stefanescu
 */
public class JP2TileOpImage extends SingleBandedOpImage {
    // We need this sort of cache to hold the tile rectangles because
    // it seems that the TIFFImageReader.getImageWidth() goes into the stream
    // each time and, therefore, takes unnecessary time
    private static final Map<Path, Rectangle> tileDims = new HashMap<>();

    private static final int[] bands = new int[] { 0 };

    private Boolean useOpenJp2Jna;

    private final TileLayout tileLayout;

    private final JP2ImageFile jp2ImageFile;
    private final Path cacheDir;
    private final int tileIndex;
    private final int bandIndex;
    private final int dataType;
    private final Logger logger;
    private final Point tileOffset;

    private JP2TileOpImage(JP2ImageFile jp2ImageFile, int bandIdx, Path cacheDir, int row, int col,
                           TileLayout tileLayout, MultiLevelModel imageModel, int dataType, int level, Point tileOffset)
                           throws IOException {

        super(dataType, null, tileLayout.tileWidth, tileLayout.tileHeight,
                getTileDimAtResolutionLevel(tileLayout.tileWidth, tileLayout.tileHeight, level),
                null, ResolutionLevel.create(imageModel, level));

        Assert.notNull(jp2ImageFile, "jp2ImageFile");
        Assert.notNull(cacheDir, "cacheDir");
        Assert.notNull(tileLayout, "tileLayout");
        Assert.notNull(imageModel, "imageModel");

        this.logger = SystemUtils.LOG;
        this.jp2ImageFile = jp2ImageFile;
        this.cacheDir = cacheDir;
        this.tileLayout = tileLayout;
        this.tileIndex = col + row * tileLayout.numXTiles;
        this.bandIndex = bandIdx;
        this.dataType = dataType;
        this.tileOffset = tileOffset;

        //if (useOpenJp2Jna == null) {
            /* Uncomment to use the direct openJp2 decompression */
            String openJp2 = OpenJpegExecRetriever.getOpenJp2();
            useOpenJp2Jna = Boolean.parseBoolean(Config.instance("s2tbx").preferences().get("use.openjp2.jna", "false")) &&
                    openJp2 != null && tileLayout.numBands == 1;
            /*useOpenJp2Jna = false;*/
        //}
    }

    private JP2TileOpImage(JP2ImageFile jp2ImageFile, int bandIdx, Path cacheDir, int row, int col,
                           TileLayout tileLayout, MultiLevelModel imageModel, int dataType, int level)
            throws IOException {

        this(jp2ImageFile, bandIdx, cacheDir, row, col, tileLayout, imageModel,dataType, level, new Point(0,0));
    }

    /**
     * Factory method for creating a TileOpImage instance.
     *
     * @param jp2ImageFile     The JP2 file
     * @param cacheDir      The directory where decompressed tiles will be extracted
     * @param bandIdx       The index of the band for which the operator is created
     * @param row           The row of the tile in the scene layout
     * @param col           The column of the tile in the scene layout
     * @param tileLayout    The scene layout
     * @param imageModel    The multi-level image model
     * @param dataType      The data type of the tile raster
     * @param level         The resolution at which the tile is created
     */
    public static PlanarImage create(JP2ImageFile jp2ImageFile, Path cacheDir, int bandIdx, int row, int col, TileLayout tileLayout,
                                     MultiLevelModel imageModel, int dataType, int level, Point tileOffset)
                                     throws IOException {

        Assert.notNull(cacheDir, "cacheDir");
        Assert.notNull(tileLayout, "imageLayout");
        Assert.notNull(imageModel, "imageModel");
        Point offset = new Point(scaleValue(tileOffset.x,level),scaleValue(tileOffset.y,level));
        if (jp2ImageFile == null) {
            ImageLayout imageLayout = buildImageLayout(tileLayout.tileWidth, tileLayout.tileHeight, dataType, level);
            return ConstantDescriptor.create((float) imageLayout.getWidth(null), (float) imageLayout.getHeight(null), new Short[]{0}, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));
        } else {
            return new JP2TileOpImage(jp2ImageFile, bandIdx, cacheDir, row, col, tileLayout, imageModel, dataType, level , offset);
        }
    }

    public static PlanarImage create(JP2ImageFile jp2ImageFile, Path cacheDir, int bandIdx, int row, int col, TileLayout tileLayout,
                                     MultiLevelModel imageModel, int dataType, int level)
                                     throws IOException {

        Assert.notNull(cacheDir, "cacheDir");
        Assert.notNull(tileLayout, "imageLayout");
        Assert.notNull(imageModel, "imageModel");
        if (jp2ImageFile == null) {
            ImageLayout imageLayout = buildImageLayout(tileLayout.tileWidth, tileLayout.tileHeight, dataType, level);
            return ConstantDescriptor.create((float) imageLayout.getWidth(null), (float) imageLayout.getHeight(null), new Short[]{0}, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));
        } else {
            return new JP2TileOpImage(jp2ImageFile, bandIdx, cacheDir, row, col, tileLayout, imageModel, dataType, level);
        }
    }

    public static ImageLayout buildImageLayout(int tileWidth, int tileHeight, int dataType, int level) {
        int targetWidth = tileWidth;
        int targetHeight = tileHeight;
        Dimension targetTileDim = getTileDimAtResolutionLevel(tileWidth, tileHeight, level);
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataType, targetWidth, targetHeight);

        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        if (colorModel == null) {
            ColorSpace cs = ColorSpace.getInstance(1003);
            int[] nBits = new int[]{DataBuffer.getDataTypeSize(dataType)};
            colorModel = new ComponentColorModel(cs, nBits, false, true, 1, dataType);
        }
        return new ImageLayout(0, 0, targetWidth, targetHeight, 0, 0, targetTileDim.width, targetTileDim.height, sampleModel, colorModel);
    }

    @Override
    protected synchronized void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        if (useOpenJp2Jna) {
            computeRectDirect(dest, destRect);
        } else {
            computeRectIndirect(dest, destRect);
        }
    }

    private void computeRectIndirect(WritableRaster dest, Rectangle destRect) {
        try {
            Path tile = decompressTile(tileIndex, getLevel());
            RenderedImage readTileImage = null;
            if (tile != null) {
                try (ImageReader imageReader = new ImageReader(tile)) {
                    final DataBuffer dataBuffer = dest.getDataBuffer();
                    int tileWidth = this.getTileWidth();
                    int tileHeight = this.getTileHeight();
                    final int fileTileX = destRect.x / tileLayout.tileWidth;
                    final int fileTileY = destRect.y / tileLayout.tileHeight;
                    int fileTileOriginX = destRect.x - fileTileX * tileLayout.tileWidth + this.tileOffset.x;
                    int fileTileOriginY = destRect.y - fileTileY * tileLayout.tileHeight + this.tileOffset.y;
                    Rectangle fileTileRect = tileDims.get(tile);
                    if (fileTileRect == null) {
                        fileTileRect = new Rectangle(0, 0, imageReader.getImageWidth(), imageReader.getImageHeight());
                        tileDims.put(tile, fileTileRect);
                    }
                    if (fileTileOriginX == 0 && tileLayout.tileWidth == tileWidth
                            && fileTileOriginY == 0 && tileLayout.tileHeight == tileHeight
                            && tileWidth * tileHeight == dataBuffer.getSize()
                            && imageReader.getImageWidth() == tileLayout.width
                            && imageReader.getImageHeight() == tileLayout.height) {
                        readTileImage = imageReader.read();
                    } else {
                        //check is needed because a pixel is lose when the scaleValue is approx. upper bound
                        if (fileTileOriginX + tileWidth > tileWidth) {
                            fileTileOriginX--;
                        }
                        if (fileTileOriginY + tileHeight > tileHeight) {
                            fileTileOriginY--;
                        }
                        final Rectangle tileRect = new Rectangle(fileTileOriginX, fileTileOriginY, tileWidth, tileHeight);
                        final Rectangle intersection = fileTileRect.intersection(tileRect);
                        if (!intersection.isEmpty()) {
                            readTileImage = imageReader.read(intersection);
                        }
                    }
                    if (readTileImage != null) {
                        Raster readBandRaster = readTileImage.getData().createChild(0, 0, readTileImage.getWidth(),readTileImage.getHeight(), 0, 0, new int[] { bandIndex });
                        dest.setDataElements(dest.getMinX(),dest.getMinY(),readBandRaster);
                    }
                } catch (IOException e) {
                    logger.severe(e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    private Path getLocalImageFile() throws IOException {
        synchronized (this.jp2ImageFile) {
            return this.jp2ImageFile.getLocalFile();
        }
    }

    private void computeRectDirect(WritableRaster dest, Rectangle destRect) {
        try (OpenJP2Decoder decoder = new OpenJP2Decoder(this.cacheDir, getLocalImageFile(), this.bandIndex, this.dataType, getLevel(), 20, tileIndex)) {
            Raster readTileImage = null;
            final DataBuffer dataBuffer = dest.getDataBuffer();
            int tileWidth = this.getTileWidth();
            int tileHeight = this.getTileHeight();
            final int fileTileX = destRect.x / tileLayout.tileWidth;
            final int fileTileY = destRect.y / tileLayout.tileHeight;
            int fileTileOriginX = destRect.x - fileTileX * tileLayout.tileWidth + this.tileOffset.x;
            int fileTileOriginY = destRect.y - fileTileY * tileLayout.tileHeight + this.tileOffset.y;
            Dimension dimensions = decoder.getImageDimensions();
            Rectangle fileTileRect = new Rectangle(0, 0, dimensions.width, dimensions.height);

            if (fileTileOriginX == 0 && tileLayout.tileWidth == tileWidth
                    && fileTileOriginY == 0 && tileLayout.tileHeight == tileHeight
                    && tileWidth * tileHeight == dataBuffer.getSize()
                    && dimensions.width == tileLayout.width
                    && dimensions.height == tileLayout.height) {
                readTileImage = decoder.read(null);
            } else {
                //check is needed because a pixel is lose when the scaleValue is approx. upper bound
                if (fileTileOriginX + tileWidth > tileWidth) {
                    fileTileOriginX--;
                }
                if (fileTileOriginY + tileHeight > tileHeight) {
                    fileTileOriginY--;
                }
                final Rectangle tileRect = new Rectangle(fileTileOriginX, fileTileOriginY, tileWidth, tileHeight);
                final Rectangle intersection = fileTileRect.intersection(tileRect);
                if (!intersection.isEmpty()) {
                    readTileImage = decoder.read(intersection);
                }
            }
            if (readTileImage != null) {
                Raster readBandRaster = readTileImage.createChild(0, 0, readTileImage.getWidth(), readTileImage.getHeight(), 0, 0, bands);
                dest.setDataElements(dest.getMinX(), dest.getMinY(), readBandRaster);
            }

        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    public static int scaleValue(int source, int level) {
        int size = source >> level;
        int sizeTest = size << level;
        if (sizeTest < source) {
            size++;
        }
        return size;
    }

    private static Dimension getTileDimAtResolutionLevel(int fullTileWidth, int fullTileHeight, int level) {
        int width = scaleValue(fullTileWidth, level);
        int height = scaleValue(fullTileHeight, level);
        return getTileDim(width, height);
    }

    private static Dimension getTileDim(int width, int height) {
        return new Dimension(width < JAI.getDefaultTileSize().width ? width : JAI.getDefaultTileSize().width,
                height < JAI.getDefaultTileSize().height ? height : JAI.getDefaultTileSize().height);
    }

    private static Path getPath(Path basePath, String...relatives) {
        if (relatives == null || relatives.length == 0) {
            return basePath;
        }
        return Paths.get(basePath.toAbsolutePath().toString(), relatives);
    }

    private static String getFileNameWithoutExtension(Path path) {
        if (path == null || Files.isDirectory(path)) {
            return null;
        }
        String fileName = path.getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    private Path decompressTile(int tileIndex, int level) throws IOException {
        Path imageFile = getLocalImageFile();
        Path tileFile = getPath(cacheDir, getFileNameWithoutExtension(imageFile).toLowerCase() + "_tile_" + String.valueOf(tileIndex) + "_" + String.valueOf(level) + ".tif");
        if ((!Files.exists(tileFile)) || (Utils.diffLastModifiedTimes(tileFile.toFile(), imageFile.toFile()) < 0L)) {
            final OpjExecutor decompress = new OpjExecutor(OpenJpegExecRetriever.getOpjDecompress());
            final Map<String, String> params = new HashMap<String, String>() {{
                put("-i", Utils.GetIterativeShortPathNameW(imageFile.toString()));
                put("-r", String.valueOf(level));
                put("-l", "20");
            }};
            String tileFileName;
            if (org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS && (tileFile.getParent() != null)) {
                tileFileName = Utils.GetIterativeShortPathNameW(tileFile.getParent().toString()) + File.separator + tileFile.getName(tileFile.getNameCount()-1);
            }
            else {
                tileFileName = tileFile.toString();
            }

            params.put("-o", tileFileName);
            params.put("-t", String.valueOf(tileIndex));
            params.put("-p", String.valueOf(DataBuffer.getDataTypeSize(this.getSampleModel().getDataType())));
            params.put("-threads", "ALL_CPUS");

            if (decompress.execute(params) != 0) {
                logger.severe(decompress.getLastError());
                tileFile = null;
            } else {
                logger.fine("Decompressed tile #" + String.valueOf(tileIndex) + " @ resolution " + String.valueOf(level));
            }
        }
        return tileFile;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    private static class ImageReader implements AutoCloseable {
        private TIFFImageReader imageReader;
        ImageInputStream inputStream;

        ImageReader(Path input) throws IOException {
            Iterator<javax.imageio.ImageReader> imageReaders = ImageIO.getImageReadersByFormatName("tiff");
            while (imageReaders.hasNext()) {
                final javax.imageio.ImageReader reader = imageReaders.next();
                if (reader instanceof TIFFImageReader) {
                    imageReader = (TIFFImageReader) reader;
                    break;
                }
            }
            if (imageReader == null) {
                throw new IOException("Tiff imageReader not found");
            }
            inputStream = ImageIO.createImageInputStream(input.toFile());
            imageReader.setInput(inputStream);
        }

        int getImageWidth() throws IOException {
            return inputStream != null ? imageReader.getWidth(0) : 0;
        }

        int getImageHeight() throws IOException {
            return inputStream != null ? imageReader.getHeight(0) : 0;
        }

        public RenderedImage read() throws IOException {
            if (inputStream == null) {
                throw new IOException("No input stream");
            }
            return imageReader.readAsRenderedImage(0, null);
        }

        public RenderedImage read(Rectangle rectangle) throws IOException {
            if (inputStream == null) {
                throw new IOException("No input stream");
            }
            ImageReadParam params = imageReader.getDefaultReadParam();
            params.setSourceRegion(rectangle);
            return imageReader.read(0, params);
        }

        public void close() {
            try {
                if (inputStream != null)
                    inputStream.close();
                //imageReader.dispose();
            } catch (IOException ignored) {
            }
        }
    }

}
