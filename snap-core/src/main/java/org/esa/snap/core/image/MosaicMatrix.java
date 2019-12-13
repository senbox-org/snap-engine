package org.esa.snap.core.image;

/**
 * Created by jcoravu on 12/12/2019.
 */
public class MosaicMatrix {

    private final int rowCount;
    private final int columnCount;
    private final MatrixCell[][] internal;

    private int currentRowIndex;
    private int currentColumnIndex;

    public MosaicMatrix(int rowCount, int columnCount) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.internal = new MatrixCell[rowCount][columnCount];

        this.currentColumnIndex = 0;
        this.currentRowIndex = 0;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void addCell(MatrixCell matrixCell) {
        if (this.currentRowIndex == this.rowCount && this.currentColumnIndex == this.columnCount) {
            throw new IllegalArgumentException("Cannot add cell past to the matrix size");
        }
        addCellAt(this.currentRowIndex, this.currentColumnIndex, matrixCell);
        if (this.currentColumnIndex == this.columnCount - 1) {
            this.currentColumnIndex = 0;
            this.currentRowIndex++;
        }
        this.currentColumnIndex++;
    }

    public MatrixCell getCellAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex > this.rowCount - 1) {
            throw new IllegalArgumentException("Invalid row index");
        }
        if (columnIndex < 0 || columnIndex > this.columnCount - 1) {
            throw new IllegalArgumentException("Invalid column index");
        }
        return this.internal[rowIndex][columnIndex];
    }

    public int computeTotalWidth() {
        if (!isConsistent()) {
            throw new UnsupportedOperationException("Current matrix has unassigned cells!");
        }
        int totalWidth = 0;
        for (int i = 0; i < this.columnCount; i++) {
            totalWidth += this.internal[0][i].getCellWidth();
        }
        return totalWidth;
    }

    public int computeTotalHeight() {
        if (!isConsistent()) {
            throw new UnsupportedOperationException("Current matrix has unassigned cells!");
        }
        int totalHeight = 0;
        for (int i = 0; i < this.rowCount; i++) {
            totalHeight += this.internal[i][0].getCellHeight();
        }
        return totalHeight;
    }

    public boolean isConsistent() {
        for (int i = 0; i < this.rowCount; i++) {
            for (int j = 0; j < this.columnCount; j++) {
                if (this.internal[i][j] == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public void addCellAt(int rowIndex, int columnIndex, MatrixCell matrixCell) {
        if (rowIndex < 0 || rowIndex > this.rowCount - 1) {
            throw new IllegalArgumentException("Invalid row index");
        }
        if (columnIndex < 0 || columnIndex > this.columnCount - 1) {
            throw new IllegalArgumentException("Invalid column index");
        }
        if (columnIndex > 0) {
            MatrixCell leftCell = this.internal[rowIndex][columnIndex - 1];
            if (matrixCell.getCellHeight() != leftCell.getCellHeight()) {
                throw new IllegalArgumentException("Cell height is different from that of previously added cells.");
            }
        }
        if (rowIndex > 0) {
            MatrixCell upperCell = this.internal[rowIndex - 1][columnIndex];
            if (matrixCell.getCellWidth() != upperCell.getCellWidth()) {
                throw new IllegalArgumentException("Cell width is different from that of previously added cells.");
            }
        }
        MatrixCell cell = this.internal[rowIndex][columnIndex];
        if (cell == null) {
            this.internal[rowIndex][columnIndex] = matrixCell;
        } else {
            throw new IllegalArgumentException("The cell has already a value.");
        }
    }

    public static interface MatrixCell {

        public int getCellWidth();

        public int getCellHeight();
    }

}
