package org.esa.snap.core.image;

import java.awt.*;

public class ImageReadBoundsSupport extends LevelImageSupport {

    private final int sourceX;
    private final int sourceY;
    private final Rectangle imageCellReadBounds;

    public ImageReadBoundsSupport(Rectangle imageCellReadBounds, int level, double scale) {
        super(imageCellReadBounds.width, imageCellReadBounds.height, level, scale);

        this.sourceX = imageCellReadBounds.x;
        this.sourceY = imageCellReadBounds.y;
        this.imageCellReadBounds = imageCellReadBounds;
    }

    public int getSourceX() {
        return sourceX;
    }

    public int getSourceY() {
        return sourceY;
    }
}
