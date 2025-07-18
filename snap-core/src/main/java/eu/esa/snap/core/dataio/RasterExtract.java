package eu.esa.snap.core.dataio;

public class RasterExtract {

    private final int xOffset;
    private final int yOffset;
    private final int width;
    private final int height;
    private final int stepX;
    private final int stepY;
    private int layerIdx;

    public RasterExtract(int xOffset, int yOffset, int width, int height, int stepX, int stepY) {
        this(xOffset, yOffset, width, height, stepX, stepY, -1);
    }

    public RasterExtract(int xOffset, int yOffset, int width, int height, int stepX, int stepY, int layerIdx) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;
        this.stepX = stepX;
        this.stepY = stepY;
        this.layerIdx = layerIdx;
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

    public int getLayerIdx() {
        return layerIdx;
    }

    public void setLayerIdx(int layerIdx) {
        this.layerIdx = layerIdx;
    }
}
