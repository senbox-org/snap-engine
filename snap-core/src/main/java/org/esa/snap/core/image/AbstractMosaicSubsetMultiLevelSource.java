package org.esa.snap.core.image;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.*;
import javax.media.jai.operator.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by jcoravu on 11/12/2019.
 */
public abstract class AbstractMosaicSubsetMultiLevelSource extends AbstractMultiLevelSource {

    protected final Rectangle imageReadBounds;
    protected final Dimension tileSize;

    private final TileImageDisposer tileImageDisposer;

    protected AbstractMosaicSubsetMultiLevelSource(Rectangle imageReadBounds, Dimension tileSize, GeoCoding geoCoding) {
        this(DefaultMultiLevelModel.getLevelCount(imageReadBounds.width, imageReadBounds.height), imageReadBounds, tileSize, geoCoding);
    }

    protected AbstractMosaicSubsetMultiLevelSource(Rectangle imageReadBounds, Dimension tileSize, AffineTransform imageToModelTransform) {
        this(DefaultMultiLevelModel.getLevelCount(imageReadBounds.width, imageReadBounds.height), imageReadBounds, tileSize, imageToModelTransform);
    }

    protected AbstractMosaicSubsetMultiLevelSource(int levelCount, Rectangle imageReadBounds, Dimension tileSize, GeoCoding geoCoding) {
        this(levelCount, imageReadBounds, tileSize, Product.findImageToModelTransform(geoCoding));
    }

    protected AbstractMosaicSubsetMultiLevelSource(int levelCount, Rectangle imageReadBounds, Dimension tileSize, AffineTransform imageToModelTransform) {
        super(new DefaultMultiLevelModel(levelCount, imageToModelTransform, imageReadBounds.width, imageReadBounds.height));

        this.imageReadBounds = imageReadBounds;
        this.tileSize = tileSize;

        this.tileImageDisposer = new TileImageDisposer();
    }

    @Override
    public synchronized void reset() {
        super.reset();

        this.tileImageDisposer.disposeAll();
        System.gc();
    }

    protected final <TileDataType> List<RenderedImage> buildDecompressedTileImages(int level, Rectangle imageCellReadBounds, Dimension decompresedTileSize, int defaultImageWidth,
                                                                                   float translateLevelOffsetX, float translateLevelOffsetY,
                                                                                   DecompressedTileOpImageCallback<TileDataType> tileOpImageCallback, TileDataType tileData) {

        int startTileColumnIndex = imageCellReadBounds.x / decompresedTileSize.width;
        int endTileColumnIndex = computeDecompressedEndTileIndex(startTileColumnIndex, imageCellReadBounds.x, imageCellReadBounds.width, decompresedTileSize.width);

        int startTileRowIndex = imageCellReadBounds.y / decompresedTileSize.height;
        int endTileRowIndex = computeDecompressedEndTileIndex(startTileColumnIndex, imageCellReadBounds.y, imageCellReadBounds.height, decompresedTileSize.height);

        float levelTotalImageWidth = (float) ImageUtils.computeLevelSizeAsDouble(imageCellReadBounds.width, level);
        float levelTotalImageHeight = (float) ImageUtils.computeLevelSizeAsDouble(imageCellReadBounds.height, level);

        int defaultColumnTileCount = ImageUtils.computeTileCount(defaultImageWidth, decompresedTileSize.width);
        java.util.List<RenderedImage> tileImages = new ArrayList<>();

        float levelTranslateY = 0.0f;
        int currentImageTileTopY = imageCellReadBounds.y;
        for (int tileRowIndex = startTileRowIndex; tileRowIndex <= endTileRowIndex; tileRowIndex++) {
            int currentTileHeight = computeDecompressedImageTileSize(startTileRowIndex, endTileRowIndex, tileRowIndex, currentImageTileTopY, imageCellReadBounds.y, imageCellReadBounds.height, decompresedTileSize.height);
            int levelImageTileHeight = ImageUtils.computeLevelSize(currentTileHeight, level);

            int tileOffsetYFromDecompressedImage = currentImageTileTopY - (tileRowIndex * decompresedTileSize.height);
            if (tileOffsetYFromDecompressedImage < 0) {
                throw new IllegalStateException("The tile offset Y from the decompressed image file is negative on level " + level + ".");
            }

            float levelTranslateX = 0.0f;
            int currentImageTileLeftX = imageCellReadBounds.x;
            for (int tileColumnIndex = startTileColumnIndex; tileColumnIndex <= endTileColumnIndex; tileColumnIndex++) {
                int currentTileWidth = computeDecompressedImageTileSize(startTileColumnIndex, endTileColumnIndex, tileColumnIndex, currentImageTileLeftX, imageCellReadBounds.x, imageCellReadBounds.width, decompresedTileSize.width);
                int levelImageTileWidth = ImageUtils.computeLevelSize(currentTileWidth, level);

                int tileOffsetXFromDecompressedImage = currentImageTileLeftX - (tileColumnIndex * decompresedTileSize.width);
                if (tileOffsetXFromDecompressedImage < 0) {
                    throw new IllegalStateException("The tile offset X from the decompressed image file is negative.");
                }

                int decompressTileIndex = tileColumnIndex + (tileRowIndex * defaultColumnTileCount);

                Dimension currentTileSize = new Dimension(currentTileWidth, currentTileHeight);
                Point tileOffsetFromDecompressedImage = new Point(tileOffsetXFromDecompressedImage, tileOffsetYFromDecompressedImage);
                Point tileOffsetFromImage = new Point(currentImageTileLeftX, currentImageTileTopY);

                SourcelessOpImage tileOpImage = tileOpImageCallback.buildTileOpImage(decompresedTileSize, currentTileSize, tileOffsetFromDecompressedImage, tileOffsetFromImage,
                                                                                     decompressTileIndex, level, tileData);
                validateTileImageSize(level, tileOpImage, levelImageTileWidth, levelImageTileHeight);
                this.tileImageDisposer.registerForDisposal(tileOpImage);

                PlanarImage opImage = TranslateDescriptor.create(tileOpImage, translateLevelOffsetX + levelTranslateX, translateLevelOffsetY + levelTranslateY, Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
                tileImages.add(opImage);

                levelTranslateX += (float) ImageUtils.computeLevelSizeAsDouble(currentTileWidth, level);
                currentImageTileLeftX += currentTileWidth;
            }

            if (levelTotalImageWidth != levelTranslateX) {
                throw new IllegalStateException("Invalid translate width: levelTotalImageWidth="+levelTotalImageWidth+", levelTranslateX="+levelTranslateX+", level="+level);
            }

            levelTranslateY += (float) ImageUtils.computeLevelSizeAsDouble(currentTileHeight, level);
            currentImageTileTopY += currentTileHeight;
        }

        if (levelTotalImageHeight != levelTranslateY) {
            throw new IllegalStateException("Invalid translate width: levelTotalImageHeight="+levelTotalImageHeight+", levelTranslateY="+levelTranslateY+", level="+level);
        }

        return tileImages;
    }

    protected final <TileDataType> List<RenderedImage> buildUncompressedTileImages(int level, Rectangle imageCellReadBounds, Dimension uncompressedTileSize,
                                                                                   float translateLevelOffsetX, float translateLevelOffsetY,
                                                                                   UncompressedTileOpImageCallback<TileDataType> tileOpImageCallback, TileDataType tileData) {

        int columnTileCount = ImageUtils.computeTileCount(imageCellReadBounds.width, uncompressedTileSize.width);
        float levelImageWidth = computeLevelImageSize(imageCellReadBounds.width, columnTileCount, level);

        int rowTileCount = ImageUtils.computeTileCount(imageCellReadBounds.height, uncompressedTileSize.height);
        float levelImageHeight = computeLevelImageSize(imageCellReadBounds.height, rowTileCount, level);

        List<RenderedImage> tileImages = new ArrayList<>(columnTileCount * rowTileCount);
        float levelTranslateWidth = (float) ImageUtils.computeLevelSizeAsDouble(uncompressedTileSize.width, level);
        float levelTranslateHeight = (float) ImageUtils.computeLevelSizeAsDouble(uncompressedTileSize.height, level);
        float levelTotalTranslateWidth = 0.0f;
        float levelTotalTranslateHeight = 0.0f;
        for (int tileRowIndex = 0; tileRowIndex < rowTileCount; tileRowIndex++) {
            int tileOffsetY = tileRowIndex * uncompressedTileSize.height;
            boolean isLastRow = (tileRowIndex == rowTileCount - 1);
            int tileHeight = isLastRow ? (imageCellReadBounds.height - tileOffsetY) : uncompressedTileSize.height;
            int levelImageTileHeight = ImageUtils.computeLevelSize(tileHeight, level);
            for (int tileColumnIndex = 0; tileColumnIndex < columnTileCount; tileColumnIndex++) {
                int tileOffsetX = tileColumnIndex * uncompressedTileSize.width;
                boolean isLastColumn = (tileColumnIndex == columnTileCount - 1);
                int tileWidth = isLastColumn ? (imageCellReadBounds.width - tileOffsetX) : uncompressedTileSize.width;
                int levelImageTileWidth = ImageUtils.computeLevelSize(tileWidth, level);

                Dimension currentTileSize = new Dimension(tileWidth, tileHeight);
                Point tileOffsetFromCellReadBounds = new Point(tileOffsetX, tileOffsetY);

                PlanarImage tileOpImage = tileOpImageCallback.buildTileOpImage(imageCellReadBounds, level, tileOffsetFromCellReadBounds, currentTileSize, tileData);
                validateTileImageSize(level, tileOpImage, levelImageTileWidth, levelImageTileHeight);
                this.tileImageDisposer.registerForDisposal(tileOpImage);

                float levelTranslateX = computeUncompressedTileTranslateOffset(tileColumnIndex, columnTileCount, levelTranslateWidth, tileOpImage.getWidth(), levelImageWidth);
                float levelTranslateY = computeUncompressedTileTranslateOffset(tileRowIndex, rowTileCount, levelTranslateHeight, tileOpImage.getHeight(), levelImageHeight);

                RenderedOp opImage = TranslateDescriptor.create(tileOpImage, translateLevelOffsetX + levelTranslateX, translateLevelOffsetY + levelTranslateY, Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
                tileImages.add(opImage);

                if (isLastRow && isLastColumn) {
                    levelTotalTranslateWidth = levelTranslateX + tileOpImage.getWidth();
                    levelTotalTranslateHeight = levelTranslateY + tileOpImage.getHeight();
                }
            }
        }
        if (levelImageWidth != levelTotalTranslateWidth) {
            throw new IllegalStateException("Invalid translate width: imageLevelWidth="+levelImageWidth+", totalTranslateWidth="+levelTotalTranslateWidth+", level="+level);
        }
        if (levelImageHeight != levelTotalTranslateHeight) {
            throw new IllegalStateException("Invalid translate height: imageLevelHeight="+levelImageHeight+", totalTranslateHeight="+levelTotalTranslateHeight+", level="+level);
        }

        return tileImages;
    }

    protected final RenderedOp buildMosaicOp(int level, java.util.List<RenderedImage> tileImages, boolean canCreateSourceROI) {
        if (tileImages.size() == 0) {
            throw new IllegalStateException("No tiles found.");
        }
        int imageLevelWidth = ImageUtils.computeLevelSize(this.imageReadBounds.width, level);
        int imageLevelHeight = ImageUtils.computeLevelSize(this.imageReadBounds.height, level);

        Dimension defaultTileSize = JAI.getDefaultTileSize();
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setMinX(0);
        imageLayout.setMinY(0);
        imageLayout.setTileWidth(defaultTileSize.width); // set the default JAI tile width
        imageLayout.setTileHeight(defaultTileSize.height); // set the default JAI tile height
        imageLayout.setTileGridXOffset(0);
        imageLayout.setTileGridYOffset(0);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        RenderedImage[] sources = tileImages.toArray(new RenderedImage[tileImages.size()]);

        ROI[] sourceRois = null;
        if (canCreateSourceROI) {
            // it must be specified which values shall be mosaicked; the default settings don't work
            // we want all values to be considered
            sourceRois = new ROI[tileImages.size()];
            for (int i = 0; i < sourceRois.length; i++) {
                RenderedImage image = tileImages.get(i);
                ImageLayout roiLayout = new ImageLayout(image);
                ROI roi = new ROI(ConstantDescriptor.create((float) image.getWidth(), (float) image.getHeight(), new Byte[]{Byte.MAX_VALUE}, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, roiLayout)), Byte.MAX_VALUE);
                sourceRois[i] = roi;
            }

        }
        RenderedOp mosaicOp = MosaicDescriptor.create(sources, MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, sourceRois, getMosaicOpSourceThreshold(), getMosaicOpBackgroundValues(), hints);

        if (mosaicOp.getWidth() > imageLevelWidth) {
            throw new IllegalStateException("The mosaic operator width " + mosaicOp.getWidth() + " > than the image width " + imageLevelWidth + ".");
        }
        if (mosaicOp.getHeight() > imageLevelHeight) {
            throw new IllegalStateException("The mosaic operator height " + mosaicOp.getWidth() + " > than the image height " + imageLevelHeight + ".");
        }
        if (mosaicOp.getWidth() < imageLevelWidth || mosaicOp.getHeight() < imageLevelHeight) {
            int rightPad = imageLevelWidth - mosaicOp.getWidth();
            int bottomPad = imageLevelHeight - mosaicOp.getHeight();
            BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
            mosaicOp = BorderDescriptor.create(mosaicOp, 0, rightPad, 0, bottomPad, borderExtender, null);
        }

        return mosaicOp;
    }

    protected double[] getMosaicOpBackgroundValues() {
        return null;
    }

    protected double[][] getMosaicOpSourceThreshold() {
        return null;
    }

    private static void validateTileImageSize(int level, PlanarImage tileOpImage, int levelImageTileWidth, int levelImageTileHeight) {
        if (tileOpImage.getWidth() != levelImageTileWidth) {
            throw new IllegalStateException("The image tile width " + tileOpImage.getWidth() + " is different than the level tile width " + levelImageTileWidth + " on level " + level + ".");
        }
        if (tileOpImage.getHeight() != levelImageTileHeight) {
            throw new IllegalStateException("The image tile height " + tileOpImage.getHeight() + " is different than the level tile height " + levelImageTileHeight + " on level " + level +  ".");
        }
    }

    private static float computeUncompressedTileTranslateOffset(int tileIndex, int tileCount, float levelTranslateTileSize, int levelImageTileSize, float levelImageTotalSize) {
        float translateOffset = tileIndex * levelTranslateTileSize;
        if (translateOffset + levelImageTileSize > levelImageTotalSize) {
            if (tileIndex == tileCount - 1) {
                if (levelImageTotalSize < levelImageTileSize) {
                    throw new IllegalStateException("Invalid values: imageLevelTotalSize="+levelImageTotalSize+", imageSize="+levelImageTileSize);
                }
                translateOffset = levelImageTotalSize - levelImageTileSize; // the last row or column
            } else {
                throw new IllegalStateException("Invalid values: translateSize="+levelTranslateTileSize+", translateOffset="+translateOffset+", imageSize="+levelImageTileSize+", imageLevelTotalSize="+levelImageTotalSize);
            }
        }
        if (translateOffset < 0.0f) {
            throw new IllegalStateException("The translate offset is negative: "+ translateOffset);
        }
        return translateOffset;
    }

    private static float computeLevelImageSize(int imageSize, int tileCount, int level) {
        float levelImageSize;
        if (tileCount > 1) {
            // for more than one tile compute the image size for the specified level as a float number
            levelImageSize = (float) ImageUtils.computeLevelSizeAsDouble(imageSize, level);
        } else if (tileCount == 1) {
            // for only one tile compute the image size for the specified level as an integer number
            levelImageSize = ImageUtils.computeLevelSize(imageSize, level);
        } else {
            throw new IllegalArgumentException("Invalid tile count: " + tileCount);
        }
        return levelImageSize;
    }

    private static int computeDecompressedEndTileIndex(int startTileIndex, int imageReadOffset, int imageReadSize, int tileSize) {
        int endTileIndex = startTileIndex;
        if (imageReadSize > tileSize) {
            int imageReadEndPosition = imageReadOffset + imageReadSize;
            endTileIndex = imageReadEndPosition / tileSize;
            if (imageReadEndPosition % tileSize == 0) {
                endTileIndex--;
            }
        }
        return endTileIndex;
    }

    private static int computeDecompressedImageTileSize(int startTileIndex, int endTileIndex, int currentTileIndex, int currentImageTileOffset, int imageReadOffset, int imageReadSize, int tileSize) {
        int imageReadEndPosition = imageReadOffset + imageReadSize;
        int currentTileHeight = tileSize;
        if (currentTileIndex == startTileIndex) {
            // the first tile
            if (currentTileIndex == endTileIndex) {
                currentTileHeight = imageReadEndPosition - currentImageTileOffset; // only one tile
            } else {
                int tileEndPosition = (currentTileIndex + 1) * tileSize;
                currentTileHeight = tileEndPosition - currentImageTileOffset;
            }
        } else if (currentTileIndex == endTileIndex) {
            currentTileHeight = imageReadEndPosition - currentImageTileOffset; // the last tile
        }
        return currentTileHeight;
    }
}
