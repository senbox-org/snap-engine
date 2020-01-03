package org.esa.snap.core.image;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.*;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.ArrayList;

/**
 * Created by jcoravu on 11/12/2019.
 */
public abstract class AbstractMosaicSubsetMultiLevelSource<TileDataType> extends AbstractMultiLevelSource {

    protected final Rectangle imageReadBounds;
    protected final Dimension tileSize;
    protected final TileImageDisposer tileImageDisposer;

    protected AbstractMosaicSubsetMultiLevelSource(Rectangle imageReadBounds, Dimension tileSize, GeoCoding geoCoding) {
        this(DefaultMultiLevelModel.getLevelCount(imageReadBounds.width, imageReadBounds.height), imageReadBounds, tileSize, geoCoding);
    }

    protected AbstractMosaicSubsetMultiLevelSource(int levelCount, Rectangle imageReadBounds, Dimension tileSize, GeoCoding geoCoding) {
        super(new DefaultMultiLevelModel(levelCount, Product.findImageToModelTransform(geoCoding), imageReadBounds.width, imageReadBounds.height));

        this.imageReadBounds = imageReadBounds;
        this.tileSize = tileSize;

        this.tileImageDisposer = new TileImageDisposer();
    }

    protected abstract SourcelessOpImage buildTileOpImage(Rectangle imageCellReadBounds, int level, Point tileOffsetFromCellReadBounds, Dimension tileSize, TileDataType tileData);

    @Override
    public synchronized void reset() {
        super.reset();

        this.tileImageDisposer.disposeAll();
        System.gc();
    }

    protected final java.util.List<RenderedImage> buildTileImages(int level, Rectangle imageCellReadBounds, float translateLevelOffsetX, float translateLevelOffsetY, TileDataType tileData) {
        int columnTileCount = ImageUtils.computeTileCount(imageCellReadBounds.width, this.tileSize.width);
        float levelImageWidth = computeLevelImageSize(imageCellReadBounds.width, columnTileCount, level);

        int rowTileCount = ImageUtils.computeTileCount(imageCellReadBounds.height, this.tileSize.height);
        float levelImageHeight = computeLevelImageSize(imageCellReadBounds.height, rowTileCount, level);

        java.util.List<RenderedImage> tileImages = new ArrayList<>(columnTileCount * rowTileCount);
        float levelTranslateWidth = (float) ImageUtils.computeLevelSizeAsDouble(this.tileSize.width, level);
        float levelTranslateHeight = (float) ImageUtils.computeLevelSizeAsDouble(this.tileSize.height, level);
        float levelTotalTranslateWidth = 0.0f;
        float levelTotalTranslateHeight = 0.0f;
        for (int tileRowIndex = 0; tileRowIndex < rowTileCount; tileRowIndex++) {
            int tileOffsetY = tileRowIndex * this.tileSize.height;
            boolean isLastRow = (tileRowIndex == rowTileCount - 1);
            int tileHeight = isLastRow ? (imageCellReadBounds.height - tileOffsetY) : this.tileSize.height;
            int levelImageTileHeight = ImageUtils.computeLevelSize(tileHeight, level);
            for (int tileColumnIndex = 0; tileColumnIndex < columnTileCount; tileColumnIndex++) {
                int tileOffsetX = tileColumnIndex * this.tileSize.width;
                boolean isLastColumn = (tileColumnIndex == columnTileCount - 1);
                int tileWidth = isLastColumn ? (imageCellReadBounds.width - tileOffsetX) : this.tileSize.width;
                int levelImageTileWidth = ImageUtils.computeLevelSize(tileWidth, level);

                Dimension currentTileSize = new Dimension(tileWidth, tileHeight);
                Point tileOffsetFromCellReadBounds = new Point(tileOffsetX, tileOffsetY);

                SourcelessOpImage tileOpImage = buildTileOpImage(imageCellReadBounds, level, tileOffsetFromCellReadBounds, currentTileSize, tileData);
                validateTileImageSize(tileOpImage, levelImageTileWidth, levelImageTileHeight);
                this.tileImageDisposer.registerForDisposal(tileOpImage);

                float levelTranslateX = computeTranslateOffset(tileColumnIndex, columnTileCount, levelTranslateWidth, tileOpImage.getWidth(), levelImageWidth);
                float levelTranslateY = computeTranslateOffset(tileRowIndex, rowTileCount, levelTranslateHeight, tileOpImage.getHeight(), levelImageHeight);

                RenderedOp opImage = TranslateDescriptor.create(tileOpImage, translateLevelOffsetX + levelTranslateX, translateLevelOffsetY + levelTranslateY, Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
                tileImages.add(opImage);

                if (isLastRow && isLastColumn) {
                    levelTotalTranslateWidth = levelTranslateX + tileOpImage.getWidth();
                    levelTotalTranslateHeight = levelTranslateY + tileOpImage.getHeight();
                }
            }
        }
        if (levelImageWidth != levelTotalTranslateWidth) {
            throw new IllegalStateException("Invalid width: imageLevelWidth="+levelImageWidth+", totalTranslateWidth="+levelTotalTranslateWidth);
        }
        if (levelImageHeight != levelTotalTranslateHeight) {
            throw new IllegalStateException("Invalid height: imageLevelHeight="+levelImageHeight+", totalTranslateHeight="+levelTotalTranslateHeight);
        }

        return tileImages;
    }

    protected final RenderedOp buildMosaicOp(int level, java.util.List<RenderedImage> tileImages) {
        return buildMosaicOp(level, tileImages, false);
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
        RenderedOp mosaicOp = MosaicDescriptor.create(sources, MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, sourceRois, null, null, hints);

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

    protected static void validateTileImageSize(SourcelessOpImage tileOpImage, int levelImageTileWidth, int levelImageTileHeight) {
        if (tileOpImage.getWidth() != levelImageTileWidth) {
            throw new IllegalStateException("The image tile width " + tileOpImage.getWidth() + " is different than the level tile width " + levelImageTileWidth + ".");
        }
        if (tileOpImage.getHeight() != levelImageTileHeight) {
            throw new IllegalStateException("The image tile height " + tileOpImage.getHeight() + " is different than the level tile height " + levelImageTileHeight + ".");
        }
    }

    private static float computeTranslateOffset(int tileIndex, int tileCount, float levelTranslateTileSize, int levelImageTileSize, float levelImageTotalSize) {
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

    protected static float computeLevelImageSize(int imageSize, int tileCount, int level) {
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
}
