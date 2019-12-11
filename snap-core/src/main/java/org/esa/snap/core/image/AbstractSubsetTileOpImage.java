package org.esa.snap.core.image;

import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.snap.core.util.ImageUtils;

import java.awt.*;

/**
 * Created by jcoravu on 11/12/2019.
 */
public abstract class AbstractSubsetTileOpImage extends SingleBandedOpImage {

    protected final LevelImageSupport levelImageSupport;
    protected final Point imageOffset;
    protected final Point levelTileOffset;
    protected final int bandIndex;

    protected AbstractSubsetTileOpImage(MultiLevelModel imageMultiLevelModel, int dataBufferType, int bandIndex, Rectangle imageBounds, Dimension tileSize, Point tileOffset, int level) {
        this(dataBufferType, bandIndex, imageBounds, tileSize, ImageUtils.computeTileDimensionAtResolutionLevel(tileSize, level),
                tileOffset, ResolutionLevel.create(imageMultiLevelModel, level));
    }

    private AbstractSubsetTileOpImage(int dataBufferType, int bandIndex, Rectangle imageBounds, Dimension tileSize, Dimension subTileSize, Point tileOffset, ResolutionLevel resolutionLevel) {
        super(dataBufferType, null, tileSize.width, tileSize.height, subTileSize, null, resolutionLevel);

        this.bandIndex = bandIndex;
        this.levelTileOffset = new Point(ImageUtils.computeLevelSize(tileOffset.x, resolutionLevel.getIndex()), ImageUtils.computeLevelSize(tileOffset.y, resolutionLevel.getIndex()));
        this.imageOffset = new Point(imageBounds.x, imageBounds.y);
        this.levelImageSupport = new LevelImageSupport(imageBounds.width, imageBounds.height, resolutionLevel);
    }

    protected final Rectangle computeIntersection(Rectangle destinationRectangle) {
        int sourceImageWidth = getImageWidth();
        int sourceImageHeight = getImageHeight();
        Rectangle sourceImageBounds = new Rectangle(this.imageOffset.x, this.imageOffset.y, sourceImageWidth, sourceImageHeight);

        int destinationSourceWidth = this.levelImageSupport.getSourceWidth(destinationRectangle.width);
        int destinationSourceHeight = this.levelImageSupport.getSourceHeight(destinationRectangle.height);
        int destinationSourceX = this.levelImageSupport.getSourceX(this.levelTileOffset.x + destinationRectangle.x);
        int destinationSourceY = this.levelImageSupport.getSourceY(this.levelTileOffset.y + destinationRectangle.y);
        if (destinationSourceX + destinationSourceWidth > sourceImageWidth) {
            destinationSourceWidth = sourceImageWidth - destinationSourceX;
        }
        if (destinationSourceY + destinationSourceHeight > sourceImageHeight) {
            destinationSourceHeight = sourceImageHeight - destinationSourceY;
        }
        destinationSourceX += this.imageOffset.x;
        destinationSourceY += this.imageOffset.y;

        Rectangle tileBoundsInSourceImage = new Rectangle(destinationSourceX, destinationSourceY, destinationSourceWidth, destinationSourceHeight);
        return sourceImageBounds.intersection(tileBoundsInSourceImage);
    }

    private int getImageWidth() {
        return this.levelImageSupport.getSourceWidth();
    }

    private int getImageHeight() {
        return this.levelImageSupport.getSourceHeight();
    }
}
