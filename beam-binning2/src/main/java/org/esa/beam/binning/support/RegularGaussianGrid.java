package org.esa.beam.binning.support;

import org.esa.beam.binning.PlanetaryGrid;

import java.io.IOException;

/**
 * Implementation of a regular gaussian grid. It is often used in the climate modelling community.
 * <p/>
 * The grid points of a gaussian grid along each latitude are equally spaced. This means that the distance in degree
 * between two adjacent longitudes is the same.
 * Along the longitudes the grid points are not equally spaced. The distance varies along the meridian.
 * There are two types of the gaussian grid. The regular and the reduced grid.
 * While the regular grid has for each grid row the same number of columns, the number of columns varies in the reduced
 * grid type.
 *
 * @author Marco Peters
 * @see ReducedGaussianGrid
 */
public class RegularGaussianGrid implements PlanetaryGrid {

    private final GaussianGridConfig config;
    private final int numRows;

    /**
     * Creates a new regular gaussian grid.
     *
     * @param numRows the number of rows of the grid (from pole to pole)
     */
    public RegularGaussianGrid(int numRows) {
        this.numRows = numRows;
        try {
            config = GaussianGridConfig.load(numRows / 2);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create gaussian grid: " + e.getMessage(), e);
        }
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        int rowIndex = findClosestLat(config.getLatitudePoints(), lat);
        int colIndex = findClosestLon(config.getRegularLongitudePoints(), lon);
        return getFirstBinIndex(rowIndex) + colIndex;
    }

    @Override
    public int getRowIndex(long binIndex) {
        return (int) (binIndex / (config.getRegularColumnCount()));
    }

    @Override
    public long getNumBins() {
        return getNumRows() * config.getRegularColumnCount();
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    @Override
    public int getNumCols(int rowIndex) {
        validateRowIndex(rowIndex);
        return config.getRegularColumnCount();
    }

    @Override
    public long getFirstBinIndex(int rowIndex) {
        validateRowIndex(rowIndex);
        return rowIndex * getNumCols(rowIndex);
    }

    @Override
    public double getCenterLat(int rowIndex) {
        validateRowIndex(rowIndex);
        return config.getLatitude(rowIndex);
    }

    @Override
    public double[] getCenterLatLon(long bin) {
        int row = getRowIndex(bin);
        int col = getColumnIndex(bin);
        double latitude = getCenterLat(row);
        double longitude = config.getRegularLongitudePoints()[col];

        return new double[]{latitude, longitude};
    }

    private void validateRowIndex(int rowIndex) {
        int maxRowIndex = getNumRows() - 1;
        if (rowIndex > maxRowIndex) {
            String msg = String.format("Invalid row index. Maximum allowed is %d, but was %d.", maxRowIndex, rowIndex);
            throw new IllegalArgumentException(msg);
        }
    }

    static int findClosestLat(double[] lats, double lat) {
        int index = lats.length-1 - (int) ((lat + 90) / (180.0 / lats.length));
        index -= 1;
        if (index < 0) {
            index = 0;
        }
        if (index > lats.length - 3) {
            index = lats.length - 3;
        }
        final double dist1 = Math.abs(lats[index + 0] - lat);
        final double dist2 = Math.abs(lats[index + 1] - lat);
        final double dist3 = Math.abs(lats[index + 2] - lat);
        if (dist1 < dist2) {
            return index;
        }
        if (dist2 < dist3) {
            return index + 1;
        }
        return index + 2;
    }

    static int findClosestLon(double[] lons, double lon) {
        int index = (int) ((lon + 180) / (360.0 / lons.length));
        index -= 1;
        if (index < 0) {
            index = 0;
        }
        if (index > lons.length - 3) {
            index = lons.length - 3;
        }
        final double dist1 = Math.abs(lons[index + 0] - lon);
        final double dist2 = Math.abs(lons[index + 1] - lon);
        final double dist3 = Math.abs(lons[index + 2] - lon);
        if (dist1 < dist2) {
            return index;
        }
        if (dist2 < dist3) {
            return index + 1;
        }
        return index + 2;
    }

    private int getColumnIndex(long bin) {
        int rowIndex = getRowIndex(bin);
        long firstBinIndex = getFirstBinIndex(rowIndex);
        return (int) (bin - firstBinIndex);
    }

//    public static class Descriptor implements PlanetaryGridDescriptor {
//
//        @Parameter(label = "Number of Grid Rows", defaultValue = 1024 + "",
//                   description = "Number of rows of the global grid.")
//        private int numRows;
//
//        @Override
//        public String getName() {
//            return "Regular Gaussian Grid";
//        }
//
//        @Override
//        public PropertySet createGridConfig() {
//            return PropertyContainer.createValueBacked(this.getClass(), new ParameterDescriptorFactory());
//        }
//
//        @Override
//        public PlanetaryGrid createGrid(PropertySet config) {
//            return new ReducedGaussianGrid((Integer) config.getProperty("numRows").getValue());
//        }
//    }
}
