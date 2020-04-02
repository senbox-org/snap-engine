package org.esa.snap.core.image;

import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ImageUtils;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Created by jcoravu on 11/12/2019.
 */
public abstract class AbstractSubsetTileOpImage extends SingleBandedOpImage {

    protected final LevelImageSupport levelImageSupport;
    protected final Point imageReadOffset;
    protected final Point levelTileOffsetFromReadBounds;

    protected AbstractSubsetTileOpImage(MultiLevelModel imageMultiLevelModel, int dataBufferType, Rectangle imageReadBounds,
                                        Dimension tileSize, Point tileOffsetFromReadBounds, int level) {

        this(dataBufferType, imageReadBounds, tileSize, ImageUtils.computeLevelTileDimension(tileSize, level),
                tileOffsetFromReadBounds, ResolutionLevel.create(imageMultiLevelModel, level));
    }

    protected AbstractSubsetTileOpImage(int dataBufferType, Rectangle imageReadBounds, Dimension tileSize, Dimension subTileSize,
                                      Point tileOffsetFromReadBounds, ResolutionLevel resolutionLevel) {

        super(dataBufferType, null, tileSize.width, tileSize.height, subTileSize, null, resolutionLevel);

        this.levelTileOffsetFromReadBounds = ImageUtils.computeLevelOffset(tileOffsetFromReadBounds, resolutionLevel.getIndex());
        this.imageReadOffset = new Point(imageReadBounds.x, imageReadBounds.y);
        this.levelImageSupport = new LevelImageSupport(imageReadBounds.width, imageReadBounds.height, resolutionLevel);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        dispose();
    }

    protected final Rectangle computeIntersectionOnNormalBounds(Rectangle levelDestinationRectangle) {
        int sourceImageWidth = getImageWidth();
        int sourceImageHeight = getImageHeight();

        int destinationSourceWidth = this.levelImageSupport.getSourceWidth(levelDestinationRectangle.width);
        int destinationSourceHeight = this.levelImageSupport.getSourceHeight(levelDestinationRectangle.height);
        int destinationSourceX = this.levelImageSupport.getSourceX(this.levelTileOffsetFromReadBounds.x + levelDestinationRectangle.x);
        int destinationSourceY = this.levelImageSupport.getSourceY(this.levelTileOffsetFromReadBounds.y + levelDestinationRectangle.y);
        if (destinationSourceX + destinationSourceWidth > sourceImageWidth) {
            destinationSourceWidth = sourceImageWidth - destinationSourceX;
        }
        if (destinationSourceY + destinationSourceHeight > sourceImageHeight) {
            destinationSourceHeight = sourceImageHeight - destinationSourceY;
        }
        destinationSourceX += this.imageReadOffset.x;
        destinationSourceY += this.imageReadOffset.y;

        Rectangle normalTileBoundsInSourceImage = new Rectangle(destinationSourceX, destinationSourceY, destinationSourceWidth, destinationSourceHeight);
        Rectangle normalSourceImageBounds = new Rectangle(this.imageReadOffset.x, this.imageReadOffset.y, sourceImageWidth, sourceImageHeight);
        return normalSourceImageBounds.intersection(normalTileBoundsInSourceImage);
    }

    protected final void writeDataOnLevelRaster(Raster normalRasterData, Rectangle normalBoundsIntersection, WritableRaster levelDestinationRaster,
                                                Rectangle levelDestinationRectangle, int bandIndex) {

        int offsetY = this.levelTileOffsetFromReadBounds.y + levelDestinationRectangle.y;
        int offsetX = this.levelTileOffsetFromReadBounds.x + levelDestinationRectangle.x;
        for (int y = 0; y < levelDestinationRectangle.height; y++) {
            int currentSrcYOffset = this.imageReadOffset.y + computeSourceY(offsetY + y);
            validateCoordinate(currentSrcYOffset, normalBoundsIntersection.y, normalBoundsIntersection.height);
            for (int x = 0; x < levelDestinationRectangle.width; x++) {
                int currentSrcXOffset = this.imageReadOffset.x + computeSourceX(offsetX + x);
                validateCoordinate(currentSrcXOffset, normalBoundsIntersection.x, normalBoundsIntersection.width);
                double value = normalRasterData.getSampleDouble(currentSrcXOffset, currentSrcYOffset, bandIndex);
                levelDestinationRaster.setSample(levelDestinationRectangle.x + x, levelDestinationRectangle.y + y, bandIndex, value);
            }
        }
    }

    protected final int getProductDataType() {
        int dataBufferType = getSampleModel().getDataType();
        return ImageManager.getProductDataType(dataBufferType);
    }

    protected final void writeDataOnLevelRaster(Rectangle normalTileBoundsIntersection, ProductData normalTileData, WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) {
        ProductData destData;
        boolean directMode = (levelDestinationRaster.getDataBuffer().getSize() == levelDestinationRectangle.width * levelDestinationRectangle.height);
        if (directMode) {
            destData = ProductData.createInstance(normalTileData.getType(), ImageUtils.getPrimitiveArray(levelDestinationRaster.getDataBuffer()));
        } else {
            destData = ProductData.createInstance(normalTileData.getType(), levelDestinationRectangle.width * levelDestinationRectangle.height);
        }
        for (int y = 0; y < levelDestinationRectangle.height; y++) {
            int currentSrcYOffset = this.imageReadOffset.y + this.levelImageSupport.getSourceY(this.levelTileOffsetFromReadBounds.y + levelDestinationRectangle.y + y);
            int currentDestYOffset = y * levelDestinationRectangle.width;
            for (int x = 0; x < levelDestinationRectangle.width; x++) {
                int currentSrcXOffset = this.imageReadOffset.x + this.levelImageSupport.getSourceX(this.levelTileOffsetFromReadBounds.x + levelDestinationRectangle.x + x);
                double value = getSourceValue(normalTileBoundsIntersection, normalTileData, currentSrcXOffset, currentSrcYOffset);
                destData.setElemDoubleAt(currentDestYOffset + x, value);
            }
        }
        if (!directMode) {
            levelDestinationRaster.setDataElements(levelDestinationRectangle.x, levelDestinationRectangle.y, levelDestinationRectangle.width, levelDestinationRectangle.height, destData.getElems());
        }
    }

    private int computeSourceX(double x) {
        return this.levelImageSupport.getSourceCoord(x, 0, this.levelImageSupport.getSourceWidth()-1);
    }

    private int computeSourceY(double y) {
        return this.levelImageSupport.getSourceCoord(y, 0, this.levelImageSupport.getSourceHeight()-1);
    }

    private int getImageWidth() {
        return this.levelImageSupport.getSourceWidth();
    }

    private int getImageHeight() {
        return this.levelImageSupport.getSourceHeight();
    }

    private static void validateCoordinate(int coordinateToCheck, int minimumCoordinate, int size) {
        if ((coordinateToCheck < minimumCoordinate) || (coordinateToCheck > (minimumCoordinate + size))) {
            throw new IllegalStateException("The coordinate " + coordinateToCheck + " is out of bounds. The minimum coordinate is " + minimumCoordinate+ " and the size is " + size + ".");
        }
    }

    private static double getSourceValue(Rectangle tileRect, ProductData tileData, int sourceX, int sourceY) {
        int currentX = sourceX - tileRect.x;
        int currentY = sourceY - tileRect.y;
        return tileData.getElemDoubleAt(currentY * tileRect.width + currentX);
    }
}
