package org.esa.snap.core.image;

/**
 * Created by jcoravu on 12/12/2019.
 */
public class MosaicMatrix {

    private final int rowCount;
    private final int columnCount;
    private final MatrixCell[][] internal;

    public MosaicMatrix(int rowCount, int columnCount) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.internal = new MatrixCell[rowCount][columnCount];
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void addCell(MatrixCell matrixCell) {
        if (matrixCell == null) {
            throw new NullPointerException("The matrix cell is null.");
        }
        int rowIndex = -1;
        int columnIndex = -1;
        boolean canContinue = true;
        for (int row=0; row < this.rowCount && canContinue; row++) {
            for (int column=0; column < this.columnCount && canContinue; column++) {
                if (this.internal[row][column] == null) {
                    rowIndex = row;
                    columnIndex = column;
                    canContinue = false;
                }
            }
        }
        if (rowIndex == -1 || columnIndex == -1) {
            throw new IllegalArgumentException("Cannot add cell past to the matrix size.");
        }
        setCellAt(rowIndex, columnIndex, matrixCell, false, false);
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
        for (int rowIndex = 0; rowIndex < this.rowCount; rowIndex++) {
            int rowTotalWidth = 0;
            for (int columnIndex = 0; columnIndex < this.columnCount; columnIndex++) {
                rowTotalWidth += this.internal[rowIndex][columnIndex].getCellWidth();
            }
            if (totalWidth < rowTotalWidth) {
                totalWidth = rowTotalWidth;
            }
        }
        return totalWidth;
    }

    public int computeTotalHeight() {
        if (!isConsistent()) {
            throw new UnsupportedOperationException("Current matrix has unassigned cells!");
        }
        int totalHeight = 0;
        for (int columnIndex = 0; columnIndex < this.columnCount; columnIndex++) {
            int columnTotalHeight = 0;
            for (int rowIndex = 0; rowIndex < this.rowCount; rowIndex++) {
                columnTotalHeight += this.internal[rowIndex][columnIndex].getCellHeight();
            }
            if (totalHeight < columnTotalHeight) {
                totalHeight = columnTotalHeight;
            }
        }
        return totalHeight;
    }

    public boolean isConsistent() {
        for (int rowIndex = 0; rowIndex < this.rowCount; rowIndex++) {
            for (int columnIndex = 0; columnIndex < this.columnCount; columnIndex++) {
                if (this.internal[rowIndex][columnIndex] == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setCellAt(int rowIndex, int columnIndex, MatrixCell matrixCell, boolean doNotValidateLastColumnWidth, boolean doNotValidateLastRowHeight) {
        if (rowIndex < 0 || rowIndex > this.rowCount - 1) {
            throw new IllegalArgumentException("Invalid row index " + rowIndex + ". The row count is " + this.rowCount + ".");
        }
        if (columnIndex < 0 || columnIndex > this.columnCount - 1) {
            throw new IllegalArgumentException("Invalid column index " + columnIndex + ". The column count is " + this.columnCount + ".");
        }

        // validate cell width
        if (rowIndex > 0) {
            MatrixCell previousTopRowCell = this.internal[rowIndex - 1][columnIndex];
            if (matrixCell.getCellWidth() != previousTopRowCell.getCellWidth()) {
                // different cell width values
                boolean canThrowException = true;
                if (columnIndex  == this.columnCount - 1) {
                    // the last column
                    if (doNotValidateLastColumnWidth) {
                        canThrowException = false;
                    }
                }
                if (canThrowException) {
                    throw new IllegalArgumentException("Cell width " + matrixCell.getCellWidth() + " is different from that of previously top added cell width " + previousTopRowCell.getCellWidth() + ".");
                }
            }
        }
        // validate cell height
        if (columnIndex > 0) {
            MatrixCell previousLeftColumnCell = this.internal[rowIndex][columnIndex - 1];
            if (matrixCell.getCellHeight() != previousLeftColumnCell.getCellHeight()) {
                // different cell height values
                boolean canThrowException = true;
                if (rowIndex  == this.rowCount - 1) {
                    // the last row
                    if (doNotValidateLastRowHeight) {
                        canThrowException = false;
                    }
                }
                if (canThrowException) {
                    throw new IllegalArgumentException("Cell height " + matrixCell.getCellHeight() + " is different from that of previously left added cell height " + previousLeftColumnCell.getCellHeight() + ".");
                }
            }
        }

        MatrixCell cell = this.internal[rowIndex][columnIndex];
        if (cell == null) {
            this.internal[rowIndex][columnIndex] = matrixCell;
        } else {
            throw new IllegalArgumentException("The cell from row index " + rowIndex + " and column index " + columnIndex + " has already a value.");
        }
    }

    public static interface MatrixCell {

        public int getCellWidth();

        public int getCellHeight();
    }
}
