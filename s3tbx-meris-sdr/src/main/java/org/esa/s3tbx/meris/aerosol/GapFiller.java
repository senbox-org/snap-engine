package org.esa.s3tbx.meris.aerosol;

/**
 * Fills gaps in a 2D float array. It uses a quad-tree based interpolation scheme.
 */
public class GapFiller {
    private final int overlap;
    private int width;
    private int height;
    private boolean[][] isGap;

    public GapFiller() {
        this(0);
    }

    public GapFiller(int overlap) {
        if (overlap < 0) {
            throw new IllegalArgumentException("overlap < 0");
        }
        this.overlap = overlap;
    }

    public void setWidthHeight(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void findGaps(float[][] data, float gapValue) {
        isGap = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (data[y][x] == gapValue) {
                    isGap[y][x] = true;
                } else {
                    isGap[y][x] = false;
                }
            }
        }
    }

    public void fillGaps(float[][] data, boolean[][] filled) {
        final int s = (int) getNextBase2Num(Math.max(width, height));
        fillGapsRecursive(data, filled, (width - s) / 2, (height - s) / 2, s, s);
    }

    private long getNextBase2Num(int x) {
        long y = 0L;
        if (x != 0) {
            final long n = Math.abs(x);
            for (y = 1L; y <= n; y *= 2L) {
            }
            y *= (x > 0) ? 1L : -1L;
        }
        return y;
    }

    private void fillGapsRecursive(float[][] data, boolean[][] filled,
                                   int x0, int y0, int sw, int sh) {
        if (sw == 0 || sh == 0) {
            return;
        }
        int x2 = x0 + sw - 1;
        int y2 = y0 + sh - 1;
        if (fillGapsImpl(data, filled, x0 - overlap, y0 - overlap, x2 + overlap, y2 + overlap)) {
            final int swNew = sw / 2;
            final int shNew = sh / 2;
            fillGapsRecursive(data, filled, x0, y0, swNew, shNew);
            fillGapsRecursive(data, filled, x0 + swNew, y0, swNew, shNew);
            fillGapsRecursive(data, filled, x0, y0 + shNew, swNew, shNew);
            fillGapsRecursive(data, filled, x0 + swNew, y0 + shNew, swNew, shNew);
        }
    }

    private boolean fillGapsImpl(float[][] data, boolean[][] filled, int x1, int y1, int x2, int y2) {
        if (x2 < 0 || y2 < 0 || x1 >= width || y1 >= height) {
            return false;
        }
        if (x1 < 0) {
            x1 = 0;
        }
        if (y1 < 0) {
            y1 = 0;
        }
        if (x2 >= width) {
            x2 = width - 1;
        }
        if (y2 >= height) {
            y2 = height - 1;
        }
        if (x1 == x2 && y1 == y2) {
            return false;
        }
        fillGaps(data, filled, x1, y1, x2, y2);
        return true;
    }

    private void fillGaps(float[][] data, boolean[][] filled, int x1, int y1, int x2, int y2) {
        int numGaps = 0;
        int numValues = 0;
        float sumValues = 0;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (isGap[y][x]) {
                    numGaps++;
                } else {
                    sumValues += data[y][x];
                    numValues++;
                }
            }
        }
        if (numGaps > 0 && numValues > 0) {
            float avg = sumValues / numValues;
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    if (isGap[y][x]) {
                        data[y][x] = avg;
                        filled[y][x] = true;
                    }
                }
            }
        }
    }
}