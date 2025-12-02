package eu.esa.snap.core.dataio.cache;

class RowCol {

    private int cacheRow;
    private int cacheCol;

    public RowCol(int row, int col) {
        cacheRow = row;
        cacheCol = col;
    }

    public int getCacheRow() {
        return cacheRow;
    }

    public void setCacheRow(int cacheRow) {
        this.cacheRow = cacheRow;
    }

    public int getCacheCol() {
        return cacheCol;
    }

    public void setCacheCol(int cacheCol) {
        this.cacheCol = cacheCol;
    }
}
