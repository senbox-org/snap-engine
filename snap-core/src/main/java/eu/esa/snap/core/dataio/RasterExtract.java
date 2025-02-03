package eu.esa.snap.core.dataio;

public class RasterExtract {

    private int xOffset;
    private int yOffset;
    private int width;
    private int height;
    private int stepX;
    private int stepY;

    public RasterExtract(int xOffset, int yOffset, int width, int height, int stepX, int stepY) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;
        this.stepX = stepX;
        this.stepY = stepY;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getStepX() {
        return stepX;
    }

    public int getStepY() {
        return stepY;
    }
}
