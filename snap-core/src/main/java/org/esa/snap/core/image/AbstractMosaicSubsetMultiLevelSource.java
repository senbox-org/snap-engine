package org.esa.snap.core.image;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.*;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by jcoravu on 11/12/2019.
 */
public abstract class AbstractMosaicSubsetMultiLevelSource extends AbstractMultiLevelSource {

    protected final int dataBufferType;
    protected final Dimension tileSize;
    protected final Rectangle imageBounds;
    protected final int bandIndex;
    protected final TileImageDisposer tileImageDisposer;

    public AbstractMosaicSubsetMultiLevelSource(int dataBufferType, Rectangle imageBounds, Dimension tileSize, int bandIndex, GeoCoding geoCoding) {
        super(new DefaultMultiLevelModel(Product.findImageToModelTransform(geoCoding), imageBounds.width, imageBounds.height));

        if (imageBounds.width < tileSize.width) {
            throw new IllegalArgumentException("The visible region width " + imageBounds.width + " cannot be smaller than the tile width " + tileSize.width + ".");
        }
        if (imageBounds.height < tileSize.height) {
            throw new IllegalArgumentException("The visible region height " + imageBounds.height + " cannot be smaller than the tile height " + tileSize.height + ".");
        }

        this.dataBufferType = dataBufferType;
        this.imageBounds = imageBounds;
        this.tileSize = tileSize;
        this.bandIndex = bandIndex;

        this.tileImageDisposer = new TileImageDisposer();
    }

    protected abstract SourcelessOpImage buildTileOpImage(int level, Dimension currentTileSize, Point tileOffset);

    @Override
    public synchronized void reset() {
        super.reset();

        this.tileImageDisposer.disposeAll();
        System.gc();
    }

    @Override
    protected RenderedImage createImage(int level) {
        int columnTileCount = ImageUtils.computeTileCount(this.imageBounds.width, this.tileSize.width);
        int rowTileCount = ImageUtils.computeTileCount(this.imageBounds.height, this.tileSize.height);

        int imageLevelWidth = ImageUtils.computeLevelSize(this.imageBounds.width, level);
        int imageLevelHeight = ImageUtils.computeLevelSize(this.imageBounds.height, level);

        java.util.List<RenderedImage> tileImages = Collections.synchronizedList(new ArrayList<>(columnTileCount * rowTileCount));
        double factor = 1.0d / Math.pow(2, level);
        float xTranslateWidth = (float) (this.tileSize.width * factor);
        float yTranslateHeight = (float) (this.tileSize.height * factor);
        for (int tileRowIndex = 0; tileRowIndex < rowTileCount; tileRowIndex++) {
            int tileOffsetY = tileRowIndex * this.tileSize.height;
            int tileHeight = this.tileSize.height;
            if (tileRowIndex == rowTileCount - 1) {
                tileHeight = this.imageBounds.height - tileOffsetY; // the last row
            }
            for (int tileColumnIndex = 0; tileColumnIndex < columnTileCount; tileColumnIndex++) {
                int tileOffsetX = tileColumnIndex * this.tileSize.width;
                int tileWidth = this.tileSize.width;
                if (tileColumnIndex == columnTileCount - 1) {
                    tileWidth = this.imageBounds.width - tileOffsetX; // the last column
                }

                Dimension currentTileSize = new Dimension(tileWidth, tileHeight);
                Point tileOffset = new Point(tileOffsetX, tileOffsetY);
                SourcelessOpImage tileOpImage = buildTileOpImage(level, currentTileSize, tileOffset);
                this.tileImageDisposer.registerForDisposal(tileOpImage);

                float translateY = computeTranslateOffset(tileRowIndex, rowTileCount, yTranslateHeight, tileOpImage.getHeight(), imageLevelHeight);
                float translateX = computeTranslateOffset(tileColumnIndex, columnTileCount, xTranslateWidth, tileOpImage.getWidth(), imageLevelWidth);
                RenderedOp opImage = TranslateDescriptor.create(tileOpImage, translateX, translateY, Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
                tileImages.add(opImage);
            }
        }
        if (tileImages.isEmpty()) {
            return null;
        }

        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setMinX(0);
        imageLayout.setMinY(0);
        imageLayout.setTileWidth(this.tileSize.width);
        imageLayout.setTileHeight(this.tileSize.height);
        imageLayout.setTileGridXOffset(0);
        imageLayout.setTileGridYOffset(0);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        RenderedImage[] sources = tileImages.toArray(new RenderedImage[tileImages.size()]);

        RenderedOp mosaicOp = MosaicDescriptor.create(sources, MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null, null, hints);

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

    public int getBandIndex() {
        return bandIndex;
    }

    private static float computeTranslateOffset(int tileIndex, int tileCount, float translateSize, int imageSize, int imageLevelTotalSize) {
        float translateOffset = tileIndex * translateSize;
        if (translateOffset + imageSize > imageLevelTotalSize) {
            if (tileIndex == tileCount - 1) {
                translateOffset = imageLevelTotalSize - imageSize; // the last row
            } else {
                throw new IllegalStateException("Invalid values: translateSize="+translateSize+", translateOffset="+translateOffset+", imageSize="+imageSize+", imageLevelTotalSize="+imageLevelTotalSize);
            }
        }
        return translateOffset;
    }
}
