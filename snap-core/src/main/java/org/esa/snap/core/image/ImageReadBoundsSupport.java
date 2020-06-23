package org.esa.snap.core.image;

import java.awt.*;

public class ImageReadBoundsSupport extends LevelImageSupport {

    private final Rectangle imageCellReadBounds;

    public ImageReadBoundsSupport(Rectangle imageCellReadBounds, int level, double scale) {
        super(imageCellReadBounds.width, imageCellReadBounds.height, level, scale);

        this.imageCellReadBounds = imageCellReadBounds;
    }

    public int getSourceX() {
        return this.imageCellReadBounds.x;
    }

    public int getSourceY() {
        return this.imageCellReadBounds.y;
    }

    public Rectangle computeIntersection(int destinationSourceX, int destinationSourceY, int destinationSourceWidth, int destinationSourceHeight) {
        Rectangle normalTileBoundsInSourceImage = new Rectangle(destinationSourceX, destinationSourceY, destinationSourceWidth, destinationSourceHeight);
        return this.imageCellReadBounds.intersection(normalTileBoundsInSourceImage);
    }
}
