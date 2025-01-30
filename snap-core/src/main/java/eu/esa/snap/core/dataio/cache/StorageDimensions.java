package eu.esa.snap.core.dataio.cache;

public class StorageDimensions {

    private int rasterWidth;
    private int rasterHeight;
    private int rasterLayers;

    private int tileWidth;
    private int tileHeight;
    private int tileLayers;

    public StorageDimensions() {
        rasterWidth = -1;
        rasterHeight = -1;
        rasterLayers = -1;
        tileWidth = -1;
        tileHeight = -1;
        tileLayers = -1;
    }

    public int getRasterWidth() {
        return rasterWidth;
    }

    public void setRasterWidth(int rasterWidth) {
        this.rasterWidth = rasterWidth;
    }

    public int getRasterHeight() {
        return rasterHeight;
    }

    public void setRasterHeight(int rasterHeight) {
        this.rasterHeight = rasterHeight;
    }

    public int getRasterLayers() {
        return rasterLayers;
    }

    public void setRasterLayers(int rasterLayers) {
        this.rasterLayers = rasterLayers;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }

    public int getTileLayers() {
        return tileLayers;
    }

    public void setTileLayers(int tileLayers) {
        this.tileLayers = tileLayers;
    }
}
