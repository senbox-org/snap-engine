package org.esa.snap.core.image;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.jai.JAIUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

/**
 * Created by jcoravu on 11/12/2019.
 */
public abstract class AbstractSubsetTileOpImage extends SourcelessOpImage {

    protected final ImageReadBoundsSupport imageBoundsSupport;
    protected final int levelTileOffsetFromReadBoundsX;
    protected final int levelTileOffsetFromReadBoundsY;

    protected AbstractSubsetTileOpImage(int dataBufferType, int tileWidth, int tileHeight, int tileOffsetFromReadBoundsX, int tileOffsetFromReadBoundsY,
                                        ImageReadBoundsSupport imageBoundsSupport, Dimension defaultJAIReadTileSize) {

        this(ImageUtils.buildTileImageLayout(dataBufferType, tileWidth, tileHeight, imageBoundsSupport.getLevel(), defaultJAIReadTileSize),
                tileOffsetFromReadBoundsX, tileOffsetFromReadBoundsY, imageBoundsSupport);
    }

    private AbstractSubsetTileOpImage(ImageLayout layout, int tileOffsetFromReadBoundsX, int tileOffsetFromReadBoundsY, ImageReadBoundsSupport imageBoundsSupport) {
        super(layout, null, layout.getSampleModel(null),
                layout.getMinX(null), layout.getMinY(null),
                layout.getWidth(null), layout.getHeight(null));

        this.imageBoundsSupport = imageBoundsSupport;
        this.levelTileOffsetFromReadBoundsX = ImageUtils.computeLevelSize(tileOffsetFromReadBoundsX, imageBoundsSupport.getLevel());
        this.levelTileOffsetFromReadBoundsY = ImageUtils.computeLevelSize(tileOffsetFromReadBoundsY, imageBoundsSupport.getLevel());

        if (getTileCache() == null) {
            setTileCache(JAI.getDefaultInstance().getTileCache());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        dispose();
    }

    protected final int getLevel() {
        return imageBoundsSupport.getLevel();
    }

    protected final Rectangle computeIntersectionOnNormalBounds(Rectangle levelDestinationRectangle) {
        int sourceImageWidth = this.imageBoundsSupport.getSourceWidth();
        int sourceImageHeight = this.imageBoundsSupport.getSourceHeight();

        int destinationSourceWidth = this.imageBoundsSupport.getSourceWidth(levelDestinationRectangle.width);
        int destinationSourceHeight = this.imageBoundsSupport.getSourceHeight(levelDestinationRectangle.height);
        int destinationSourceX = this.imageBoundsSupport.getSourceX(this.levelTileOffsetFromReadBoundsX + levelDestinationRectangle.x);
        int destinationSourceY = this.imageBoundsSupport.getSourceY(this.levelTileOffsetFromReadBoundsY + levelDestinationRectangle.y);
        if (destinationSourceX + destinationSourceWidth > sourceImageWidth) {
            destinationSourceWidth = sourceImageWidth - destinationSourceX;
        }
        if (destinationSourceY + destinationSourceHeight > sourceImageHeight) {
            destinationSourceHeight = sourceImageHeight - destinationSourceY;
        }
        destinationSourceX += this.imageBoundsSupport.getSourceX();
        destinationSourceY += this.imageBoundsSupport.getSourceY();

        return this.imageBoundsSupport.computeIntersection(destinationSourceX, destinationSourceY, destinationSourceWidth, destinationSourceHeight);
    }

    protected final void writeDataOnLevelRaster(Raster normalRasterData, Rectangle normalBoundsIntersection, WritableRaster levelDestinationRaster,
                                                Rectangle levelDestinationRectangle, int bandIndex) {

        int offsetY = this.levelTileOffsetFromReadBoundsY + levelDestinationRectangle.y;
        int offsetX = this.levelTileOffsetFromReadBoundsX + levelDestinationRectangle.x;
        for (int y = 0; y < levelDestinationRectangle.height; y++) {
            int currentSrcYOffset = this.imageBoundsSupport.getSourceY() + computeSourceY(offsetY + y);
            validateCoordinate(currentSrcYOffset, normalBoundsIntersection.y, normalBoundsIntersection.height);
            for (int x = 0; x < levelDestinationRectangle.width; x++) {
                int currentSrcXOffset = this.imageBoundsSupport.getSourceX() + computeSourceX(offsetX + x);
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
            int currentSrcYOffset = this.imageBoundsSupport.getSourceY() + this.imageBoundsSupport.getSourceY(this.levelTileOffsetFromReadBoundsY + levelDestinationRectangle.y + y);
            int currentDestYOffset = y * levelDestinationRectangle.width;
            for (int x = 0; x < levelDestinationRectangle.width; x++) {
                int currentSrcXOffset = this.imageBoundsSupport.getSourceX() + this.imageBoundsSupport.getSourceX(this.levelTileOffsetFromReadBoundsX + levelDestinationRectangle.x + x);
                double value = getSourceValue(normalTileBoundsIntersection, normalTileData, currentSrcXOffset, currentSrcYOffset);
                destData.setElemDoubleAt(currentDestYOffset + x, value);
            }
        }
        if (!directMode) {
            levelDestinationRaster.setDataElements(levelDestinationRectangle.x, levelDestinationRectangle.y, levelDestinationRectangle.width, levelDestinationRectangle.height, destData.getElems());
        }
    }

    protected final int computeSourceX(double x) {
        return this.imageBoundsSupport.getSourceCoord(x, 0, this.imageBoundsSupport.getSourceWidth()-1);
    }

    protected final int computeSourceY(double y) {
        return this.imageBoundsSupport.getSourceCoord(y, 0, this.imageBoundsSupport.getSourceHeight()-1);
    }

    protected final static void validateCoordinate(int coordinateToCheck, int minimumCoordinate, int size) {
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
