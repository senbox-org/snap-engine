package org.esa.snap.jp2.reader.internal;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.snap.core.image.DecompressedImageSupport;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.engine_utilities.util.PathUtils;
import org.esa.snap.lib.openjpeg.dataio.OpenJP2Decoder;
import org.esa.snap.lib.openjpeg.dataio.Utils;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;
import org.esa.snap.runtime.Config;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A JAI operator for handling JP2 tiles.
 *
 * @author Cosmin Cara
 */
public class JP2TileOpImage extends SourcelessOpImage {

    private static final Logger logger = Logger.getLogger(JP2TileOpImage.class.getName());

    private static final byte LAYER = 20;

    private JP2BandSource bandSource;
    private JP2BandData bandData;
    private DecompressedImageSupport decompressedImageSupport;
    private boolean useOpenJp2Jna;
    private boolean isAvailableOpenJp2Jna;
    private int decompressTileIndex;
    private int tileOffsetFromDecompressedImageX;
    private int tileOffsetFromDecompressedImageY;
    private int tileOffsetFromImageX;
    private int tileOffsetFromImageY;
    private String tileFilePrefix;

    public JP2TileOpImage(JP2BandSource bandSource, JP2BandData bandData, DecompressedImageSupport decompressedImageSupport,
                          int tileWidth, int tileHeight, int tileOffsetFromDecompressedImageX, int tileOffsetFromDecompressedImageY,
                          int tileOffsetFromImageX, int tileOffsetFromImageY, int decompressTileIndex, Dimension defaultJAIReadTileSize) {

        this(ImageUtils.buildTileImageLayout(bandData.getDataBufferType(), tileWidth, tileHeight, decompressedImageSupport.getLevel(), defaultJAIReadTileSize));

        this.bandSource = bandSource;
        this.bandData = bandData;
        this.decompressedImageSupport = decompressedImageSupport;
        this.decompressTileIndex = decompressTileIndex;

        this.tileOffsetFromImageX = tileOffsetFromImageX;
        this.tileOffsetFromImageY = tileOffsetFromImageY;

        this.tileOffsetFromDecompressedImageX = tileOffsetFromDecompressedImageX;
        this.tileOffsetFromDecompressedImageY = tileOffsetFromDecompressedImageY;

        String openJp2 = OpenJpegExecRetriever.getOpenJp2();
        this.isAvailableOpenJp2Jna = openJp2 != null;
        this.useOpenJp2Jna = isAvailableOpenJp2Jna && Boolean.parseBoolean(Config.instance("s2tbx").preferences().get("use.openjp2.jna", "false"));
        try {
            this.tileFilePrefix = Utils.getChecksum(PathUtils.getFileNameWithoutExtension(getLocalImageFile()));
        } catch (IOException e) {
            logger.severe(e.getMessage());
            this.tileFilePrefix = Utils.getChecksum(this.bandData.getJp2ImageFile().toString());
        }
    }

    private JP2TileOpImage(ImageLayout layout) {
        super(layout, null, layout.getSampleModel(null),
                layout.getMinX(null), layout.getMinY(null),
                layout.getWidth(null), layout.getHeight(null));

        if (getTileCache() == null) {
            setTileCache(JAI.getDefaultInstance().getTileCache());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        dispose();
    }

    @Override
    protected synchronized void computeRect(PlanarImage[] sources, WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) {
        try {
            boolean alreadyComputed = false;
            if(this.isAvailableOpenJp2Jna){
                //in case og OpenJp2Jna available, 3 possibilities:
                //    1/ the preference is actived, the computation is done
                //    2/ the are more than 4 bands, the computation is enforced
                //    3/ the are less than 5 bands and the preference is not enable, the computation is left to be done
                alreadyComputed = computeRectDirect(levelDestinationRaster, levelDestinationRectangle);
            }
            if(!alreadyComputed)
                computeRectIndirect(levelDestinationRaster, levelDestinationRectangle);
        } catch (InterruptedException | IOException ex) {
            throw new IllegalStateException("Failed to read the data for level " + getLevel() + " and rectangle " + levelDestinationRectangle + ".", ex);
        }
    }

    private int getLevel() {
        return this.decompressedImageSupport.getLevel();
    }

    private boolean computeRectDirect(WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) throws IOException {
        int level = getLevel();
        Path localCacheFolder = this.bandData.getLocalCacheFolder();
        Path localImageFile = getLocalImageFile();
        int dataType = getSampleModel().getDataType();
        try (OpenJP2Decoder decoder = new OpenJP2Decoder(localCacheFolder, localImageFile, this.bandSource.getBandIndex(), dataType, level, LAYER, this.decompressTileIndex)) {
            Dimension levelDecompressedImageSize = decoder.getImageDimensions(); // the whole image size from the specified level
            if(decoder.getBandNumber()>4 || this.useOpenJp2Jna) {
                Rectangle intersection = computeLevelDirectIntersection(level, levelDecompressedImageSize.width, levelDecompressedImageSize.height, levelDestinationRectangle);
                if (!intersection.isEmpty()) {
                    Raster readTileImage = decoder.read(intersection);
                    writeDataOnLevelRaster(levelDestinationRaster, readTileImage);
                }
            }else {
                return false;
            }
        }
        return true;
    }

    private void computeRectIndirect(WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) throws InterruptedException, IOException {
        int level = getLevel();
        Path tileDecompressedFile = decompressTile(level);
        if (tileDecompressedFile != null) {
            try (ImageReader imageReader = new ImageReader(tileDecompressedFile)) {
                Rectangle intersection = computeLevelIndirectIntersection(level, imageReader.getImageWidth(), imageReader.getImageHeight(), levelDestinationRectangle);
                if (!intersection.isEmpty()) {
                    RenderedImage readTileImage = imageReader.read(intersection);
                    writeDataOnLevelRaster(levelDestinationRaster, readTileImage.getData());
                }
            }
        }
    }

    private void writeDataOnLevelRaster(WritableRaster levelDestinationRaster, Raster readTileImage) {
        int index = this.bandSource.getBandIndex();
        Raster readBandRaster = readTileImage.createChild(0, 0, readTileImage.getWidth(), readTileImage.getHeight(), 0, 0, new int[]{index});
        levelDestinationRaster.setDataElements(levelDestinationRaster.getMinX(), levelDestinationRaster.getMinY(), readBandRaster);
    }

    private Rectangle computeLevelIndirectIntersection(int level, int levelDecompressedTileImageWidth, int levelDecompressedTileImageHeight, Rectangle levelDestinationRectangle) {
        int x = computeIndirectLevelOffsetToRead(level, this.tileOffsetFromDecompressedImageX, getWidth(), levelDestinationRectangle.x, levelDecompressedTileImageWidth);
        int y = computeIndirectLevelOffsetToRead(level, this.tileOffsetFromDecompressedImageY, getHeight(), levelDestinationRectangle.y, levelDecompressedTileImageHeight);
        Rectangle levelTileBounds = new Rectangle(x, y, levelDestinationRectangle.width, levelDestinationRectangle.height);
        Rectangle decompressedImageFileTileBounds = new Rectangle(0, 0, levelDecompressedTileImageWidth, levelDecompressedTileImageHeight);
        return decompressedImageFileTileBounds.intersection(levelTileBounds);
    }

    private Rectangle computeLevelDirectIntersection(int level, int levelDecompressedImageWidth, int levelDecompressedImageHeight, Rectangle levelDestinationRectangle) {
        int x = computeDirectLevelOffsetToRead(level, this.tileOffsetFromImageX, this.decompressedImageSupport.getDecompressedTileWidth(), levelDestinationRectangle.x, levelDestinationRectangle.width);
        int y = computeDirectLevelOffsetToRead(level, this.tileOffsetFromImageY, this.decompressedImageSupport.getDecompressedTileHeight(), levelDestinationRectangle.y, levelDestinationRectangle.height);

        int decompressedTileLeftX = computeDirectDecompressedTileStartPosition(this.tileOffsetFromImageX, this.decompressedImageSupport.getDecompressedTileWidth());
        int levelDecompressedTileLeftX = ImageUtils.computeLevelSize(decompressedTileLeftX, level);

        int decompressedTileTopY = computeDirectDecompressedTileStartPosition(this.tileOffsetFromImageY, this.decompressedImageSupport.getDecompressedTileHeight());
        int levelDecompressedTileTopY = ImageUtils.computeLevelSize(decompressedTileTopY, level);

        Rectangle levelTileBounds = new Rectangle(x, y, levelDestinationRectangle.width, levelDestinationRectangle.height);

        Rectangle levelDecompressedImageBounds = new Rectangle(0, 0, levelDecompressedImageWidth, levelDecompressedImageHeight);
        Rectangle intersection = levelDecompressedImageBounds.intersection(levelTileBounds);
        intersection.x -= levelDecompressedTileLeftX;
        intersection.y -= levelDecompressedTileTopY;
        return intersection;
    }

    private Path decompressTile(int level) throws InterruptedException, IOException {
        final Path localImageFile = getLocalImageFile();
        // SNAP-1436: JP2 reader - use shorter file names for decompressed tiles
        //String imageFileName = PathUtils.getFileNameWithoutExtension(localImageFile).toLowerCase() + "_tile_" + String.valueOf(this.decompressTileIndex) + "_" + String.valueOf(level) + ".tif";
        final String imageFileName = this.tileFilePrefix + "_" + this.decompressTileIndex + "_" + level + ".tif";
        Path tileFile = PathUtils.get(this.bandData.getLocalCacheFolder(), imageFileName);
        if ((!Files.exists(tileFile)) || (Utils.diffLastModifiedTimes(tileFile.toFile(), localImageFile.toFile()) < 0L)) {
            String tileFileName;
            if (org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS && (tileFile.getParent() != null)) {
                tileFileName = Utils.GetIterativeShortPathNameW(tileFile.getParent().toString()) + File.separator + tileFile.getName(tileFile.getNameCount()-1);
            } else {
                tileFileName = tileFile.toString();
            }
            final Map<String, String> params = new HashMap<>();
            params.put("-i", Utils.GetIterativeShortPathNameW(localImageFile.toString()));
            params.put("-r", String.valueOf(level));
            params.put("-l", Byte.toString(LAYER));
            params.put("-o", tileFileName);
            params.put("-t", String.valueOf(this.decompressTileIndex));
            params.put("-p", String.valueOf(DataBuffer.getDataTypeSize(getSampleModel().getDataType())));
            params.put("-threads", "ALL_CPUS");

            final OpjExecutor decompress = new OpjExecutor(OpenJpegExecRetriever.getOpjDecompress());
            if (decompress.execute(params) != 0) {
                logger.severe(decompress.getLastError());
                tileFile = null;
            } else {
                logger.fine("Decompressed tile #" + this.decompressTileIndex + " @ resolution " + level);
            }
        }
        return tileFile;
    }

    private Path getLocalImageFile() throws IOException {
        return this.bandData.getJp2ImageFile().getLocalFile();
    }

    private static int computeDirectDecompressedTileEndPosition(int tileOffsetFromImage, int decompresedTileSize) {
        int tileIndex = (tileOffsetFromImage / decompresedTileSize) + 1;
        return (tileIndex * decompresedTileSize);
    }

    private static int computeDirectDecompressedTileStartPosition(int tileOffsetFromImage, int decompresedTileSize) {
        int tileIndex = (tileOffsetFromImage / decompresedTileSize);
        return (tileIndex * decompresedTileSize);
    }

    private static int computeIndirectLevelOffsetToRead(int level, int tileOffsetFromDecompressedImage, int levelTileSize, int levelReadTileOffset, int levelDecompressedImageSize) {
        int levelDecompressedTileOffset = ImageUtils.computeLevelSize(tileOffsetFromDecompressedImage, level);
        int value = levelDecompressedTileOffset + levelReadTileOffset;
        if ((levelDecompressedTileOffset + levelTileSize) > levelDecompressedImageSize) {
            // do not read more data than the decompressed tile size
            int difference = (levelDecompressedTileOffset + levelTileSize) - levelDecompressedImageSize;
            value -= difference;
        }
        if (value < 0) {
            throw new IllegalStateException("The tile offset " + value + " is < 0.");
        }
        return value;
    }

    private static int computeDirectLevelOffsetToRead(int level, int tileOffsetFromImage, int decompresedTileSize, int levelReadTileOffset, int levelReadSize) {
        int levelTileOffsetFromImage = ImageUtils.computeLevelSize(tileOffsetFromImage, level);
        int decompressedTileEndPosition = computeDirectDecompressedTileEndPosition(tileOffsetFromImage, decompresedTileSize);
        int levelDecompressedTileEndPosition = ImageUtils.computeLevelSize(decompressedTileEndPosition, level);

        int value = levelTileOffsetFromImage + levelReadTileOffset;
        if ((value + levelReadSize) > levelDecompressedTileEndPosition) {
            // do not read more data than the decompressed tile size
            int difference = (value + levelReadSize) - levelDecompressedTileEndPosition;
            value -= difference;
        }
        if (value < 0) {
            throw new IllegalStateException("The tile offset " + value + " is < 0.");
        }
        return value;
    }

    private static class ImageReader implements AutoCloseable {

        private TIFFImageReader imageReader;
        private final ImageInputStream inputStream;

        ImageReader(Path input) throws IOException {
            Iterator<javax.imageio.ImageReader> imageReaders = ImageIO.getImageReadersByFormatName("tiff");
            while (imageReaders.hasNext()) {
                javax.imageio.ImageReader reader = imageReaders.next();
                if (reader instanceof TIFFImageReader) {
                    this.imageReader = (TIFFImageReader) reader;
                    break;
                }
            }
            if (this.imageReader == null) {
                throw new NullPointerException("Tiff imageReader not found");
            }
            this.inputStream = ImageIO.createImageInputStream(input.toFile());
            if (this.inputStream == null) {
                throw new NullPointerException("The image stream is null.");
            }
            this.imageReader.setInput(this.inputStream);
        }

        @Override
        public void close() {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ignored) {
            }
        }

        int getImageWidth() throws IOException {
            return this.imageReader.getWidth(0);
        }

        int getImageHeight() throws IOException {
            return this.imageReader.getHeight(0);
        }

        RenderedImage read() throws IOException {
            return this.imageReader.readAsRenderedImage(0, null);
        }

        RenderedImage read(Rectangle rectangle) throws IOException {
            ImageReadParam params = this.imageReader.getDefaultReadParam();
            params.setSourceRegion(rectangle);
            return this.imageReader.read(0, params);
        }
    }
}
