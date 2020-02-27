package org.esa.snap.jp2.reader.internal;

import com.bc.ceres.glevel.MultiLevelModel;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.snap.jp2.reader.JP2ImageFile;
import org.esa.snap.engine_utilities.util.PathUtils;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.SingleBandedOpImage;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.lib.openjpeg.dataio.OpenJP2Decoder;
import org.esa.snap.lib.openjpeg.dataio.Utils;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;
import org.esa.snap.runtime.Config;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
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
public class JP2TileOpImage extends SingleBandedOpImage {

    private static final Logger logger = Logger.getLogger(JP2TileOpImage.class.getName());

    private static final byte LAYER = 20;

    private final boolean useOpenJp2Jna;
    private final JP2ImageFile jp2ImageFile;
    private final Path cacheDir;
    private final int decompressTileIndex;
    private final int bandIndex;
    private final Point tileOffsetFromDecompressedImage;
    private final Dimension decompresedTileSize;
    private final Point tileOffsetFromImage;

    public JP2TileOpImage(JP2ImageFile jp2ImageFile, Path cacheDir, MultiLevelModel imageModel, Dimension decompresedTileSize, int bandCount, int bandIndex, int dataBufferType,
                          Dimension tileSize, Point tileOffsetFromDecompressedImage, Point tileOffsetFromImage, int decompressTileIndex, int level) {

        super(dataBufferType, null, tileSize.width, tileSize.height, ImageUtils.computeLevelTileDimension(tileSize, level),
                null, ResolutionLevel.create(imageModel, level));

        this.jp2ImageFile = jp2ImageFile;
        this.bandIndex = bandIndex;
        this.cacheDir = cacheDir;
        this.decompresedTileSize = decompresedTileSize;
        this.decompressTileIndex = decompressTileIndex;
        this.tileOffsetFromImage = tileOffsetFromImage;
        this.tileOffsetFromDecompressedImage = tileOffsetFromDecompressedImage;

        String openJp2 = OpenJpegExecRetriever.getOpenJp2();
        this.useOpenJp2Jna = openJp2 != null && bandCount == 1 && Boolean.parseBoolean(Config.instance("s2tbx").preferences().get("use.openjp2.jna", "false"));
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        dispose();
    }

    @Override
    protected synchronized void computeRect(PlanarImage[] sources, WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) {
        try {
            if (this.useOpenJp2Jna) {
                computeRectDirect(levelDestinationRaster, levelDestinationRectangle);
            } else {
                computeRectIndirect(levelDestinationRaster, levelDestinationRectangle);
            }
        } catch (InterruptedException | IOException ex) {
            throw new IllegalStateException("Failed to read the data for level " + getLevel() + " and rectangle " + levelDestinationRectangle + ".", ex);
        }
    }

    private void computeRectDirect(WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) throws IOException {
        if (this.bandIndex != 0) {
            throw new IllegalStateException("The OpenJP2Decoder API can be used to the band count is 1 and band index is 0.");
        }
        int level = getLevel();
        try (OpenJP2Decoder decoder = new OpenJP2Decoder(this.cacheDir, getLocalImageFile(), this.bandIndex, getSampleModel().getDataType(), level, LAYER, this.decompressTileIndex)) {
            Dimension levelDecompressedImageSize = decoder.getImageDimensions(); // the whole image size from the specified level
            Rectangle intersection = computeLevelDirectIntersection(level, levelDecompressedImageSize.width, levelDecompressedImageSize.height, levelDestinationRectangle);
            if (!intersection.isEmpty()) {
                Raster readTileImage = decoder.read(intersection);
                writeDataOnLevelRaster(levelDestinationRaster, readTileImage);
            }
        }
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
        Raster readBandRaster = readTileImage.createChild(0, 0, readTileImage.getWidth(), readTileImage.getHeight(), 0, 0, new int[]{this.bandIndex});
        levelDestinationRaster.setDataElements(levelDestinationRaster.getMinX(), levelDestinationRaster.getMinY(), readBandRaster);
    }

    private Rectangle computeLevelIndirectIntersection(int level, int levelDecompressedTileImageWidth, int levelDecompressedTileImageHeight, Rectangle levelDestinationRectangle) {
        int x = computeIndirectLevelOffsetToRead(level, this.tileOffsetFromDecompressedImage.x, getWidth(), levelDestinationRectangle.x, levelDecompressedTileImageWidth);
        int y = computeIndirectLevelOffsetToRead(level, this.tileOffsetFromDecompressedImage.y, getHeight(), levelDestinationRectangle.y, levelDecompressedTileImageHeight);
        Rectangle levelTileBounds = new Rectangle(x, y, levelDestinationRectangle.width, levelDestinationRectangle.height);
        Rectangle decompressedImageFileTileBounds = new Rectangle(0, 0, levelDecompressedTileImageWidth, levelDecompressedTileImageHeight);
        return decompressedImageFileTileBounds.intersection(levelTileBounds);
    }

    private Rectangle computeLevelDirectIntersection(int level, int levelDecompressedImageWidth, int levelDecompressedImageHeight, Rectangle levelDestinationRectangle) {
        int x = computeDirectLevelOffsetToRead(level, this.tileOffsetFromImage.x, this.decompresedTileSize.width,levelDestinationRectangle.x, levelDestinationRectangle.width);
        int y = computeDirectLevelOffsetToRead(level, this.tileOffsetFromImage.y, this.decompresedTileSize.height, levelDestinationRectangle.y, levelDestinationRectangle.height);

        int decompressedTileLeftX = computeDirectDecompressedTileStartPosition(this.tileOffsetFromImage.x, this.decompresedTileSize.width);
        int levelDecompressedTileLeftX = ImageUtils.computeLevelSize(decompressedTileLeftX, level);

        int decompressedTileTopY = computeDirectDecompressedTileStartPosition(this.tileOffsetFromImage.y, this.decompresedTileSize.height);
        int levelDecompressedTileTopY = ImageUtils.computeLevelSize(decompressedTileTopY, level);

        Rectangle levelTileBounds = new Rectangle(x, y, levelDestinationRectangle.width, levelDestinationRectangle.height);

        Rectangle levelDecompressedImageBounds = new Rectangle(0, 0, levelDecompressedImageWidth, levelDecompressedImageHeight);
        Rectangle intersection = levelDecompressedImageBounds.intersection(levelTileBounds);
        intersection.x -= levelDecompressedTileLeftX;
        intersection.y -= levelDecompressedTileTopY;
        return intersection;
    }

    private Path decompressTile(int level) throws InterruptedException, IOException {
        Path imageFile = getLocalImageFile();
        String imageFileName = PathUtils.getFileNameWithoutExtension(imageFile).toLowerCase() + "_tile_" + String.valueOf(this.decompressTileIndex) + "_" + String.valueOf(level) + ".tif";
        Path tileFile = PathUtils.get(this.cacheDir, imageFileName);
        if ((!Files.exists(tileFile)) || (Utils.diffLastModifiedTimes(tileFile.toFile(), imageFile.toFile()) < 0L)) {
            String tileFileName;
            if (org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS && (tileFile.getParent() != null)) {
                tileFileName = Utils.GetIterativeShortPathNameW(tileFile.getParent().toString()) + File.separator + tileFile.getName(tileFile.getNameCount()-1);
            } else {
                tileFileName = tileFile.toString();
            }

            Map<String, String> params = new HashMap<String, String>();
            params.put("-i", Utils.GetIterativeShortPathNameW(imageFile.toString()));
            params.put("-r", String.valueOf(level));
            params.put("-l", Byte.toString(LAYER));
            params.put("-o", tileFileName);
            params.put("-t", String.valueOf(this.decompressTileIndex));
            params.put("-p", String.valueOf(DataBuffer.getDataTypeSize(getSampleModel().getDataType())));
            params.put("-threads", "ALL_CPUS");

            OpjExecutor decompress = new OpjExecutor(OpenJpegExecRetriever.getOpjDecompress());
            if (decompress.execute(params) != 0) {
                logger.severe(decompress.getLastError());
                tileFile = null;
            } else {
                logger.fine("Decompressed tile #" + String.valueOf(this.decompressTileIndex) + " @ resolution " + String.valueOf(level));
            }
        }
        return tileFile;
    }

    private Path getLocalImageFile() throws IOException {
        synchronized (this.jp2ImageFile) {
            return this.jp2ImageFile.getLocalFile();
        }
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
        private ImageInputStream inputStream;

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
