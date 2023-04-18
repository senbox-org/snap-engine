package org.esa.snap.core.image;

public class DecompressedImageSupport {

    private final int level;
    private final int decompressedTileWidth;
    private final int decompressedTileHeight;

    public DecompressedImageSupport(int level, int decompressedTileWidth, int decompressedTileHeight) {
        this.level = level;
        this.decompressedTileWidth = decompressedTileWidth;
        this.decompressedTileHeight = decompressedTileHeight;
    }

    public int getLevel() {
        return level;
    }

    public int getDecompressedTileWidth() {
        return decompressedTileWidth;
    }

    public int getDecompressedTileHeight() {
        return decompressedTileHeight;
    }
}
