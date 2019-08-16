package org.esa.snap.core.util.grid.isin;

import static org.esa.snap.core.util.grid.isin.Const.*;

class IsinInverse {

    private static final long NZONE_MAX = 360 * 3600;  // Maximum number of longitudinal zones

    double false_east;
    double false_north;
    double sphere;
    double sphere_inv;
    double ang_size_inv;
    long nrow;
    long nrow_half;
    double lon_cen_mer;
    double ref_lon;
    double col_dist;
    double col_dist_inv;
    int ijustify;
    IsinRow[] row;

    void init(double radius, double centerLon, double falseEasting, double falseNorth, double dzone, double djustify) {
        if (radius <= EPS_SPHERE) {
            throw new IllegalArgumentException("bad parameter; sphere radius invalid");
        }

        if (centerLon < -Const.TWOPI || centerLon > Const.TWOPI) {
            throw new IllegalArgumentException("bad parameter; longitude of central meridian invalid");
        }

        if (dzone < (2.0 - EPS_CNVT) ||
                dzone > ((double) NZONE_MAX + EPS_CNVT)) {
            throw new IllegalArgumentException("bad parameter; nzone out of range");
        }

        final long nzone = (long) (dzone + EPS_CNVT);
        if (Math.abs(dzone - nzone) > EPS_CNVT) {
            throw new IllegalArgumentException("bad parameter; nzone not near an integer value");
        }

        if ((nzone % 2) != 0) {
            throw new IllegalArgumentException("bad parameter; nzone not multiple of two");
        }

        if (djustify < -EPS_CNVT ||
                djustify > (2.0 + EPS_CNVT)) {
            throw new IllegalArgumentException("bad parameter; djustify out of range");
        }

        final int ijustify = (int) (djustify + EPS_CNVT);
        if (Math.abs(djustify - ijustify) > EPS_CNVT) {
            throw new IllegalArgumentException("bad parameter; ijustify not near an integer value");
        }

        double lon_cen_mer = centerLon;
        if (lon_cen_mer < -Math.PI) {
            lon_cen_mer += TWOPI;
        } else if (lon_cen_mer > Math.PI) {
            lon_cen_mer -= TWOPI;
        }

        this.false_east = falseEasting;
        this.false_north = falseNorth;
        this.sphere = radius;
        this.sphere_inv = 1.0 / radius;
        this.ang_size_inv = nzone / Math.PI;
        this.nrow = nzone;
        this.nrow_half = nzone / 2;
        this.lon_cen_mer = lon_cen_mer;
        this.ref_lon = lon_cen_mer - Math.PI;
        if (this.ref_lon < -Math.PI) {
            this.ref_lon += TWOPI;
        }
        this.ijustify = ijustify;

        this.row = new IsinRow[(int) nrow_half];
        for (int irow = 0; irow < nrow_half; irow++) {
            final IsinRow currentRow = new IsinRow();

            // Calculate latitude at center of row
            final double clat = HALFPI * (1.0 - ((double) irow + 0.5) / nrow_half);

            // Calculate number of columns per row
            if (ijustify < 2)
                currentRow.ncol = (long) ((2.0 * Math.cos(clat) * nrow) + 0.5);
            else { /* make the number of columns even */
                currentRow.ncol = (long) ((Math.cos(clat) * nrow) + 0.5);
                currentRow.ncol *= 2;
            }

            // Must have at least one column
            if (currentRow.ncol < 1) {
                currentRow.ncol = 1;
            }

            // Save the inverse of the number of columns
            currentRow.ncol_inv = 1.0 / currentRow.ncol;

            // Calculate the column number of the column whose left edge touches the
            // central meridian
            if (ijustify == 1) {
                currentRow.icol_cen = (currentRow.ncol + 1) / 2;
            } else {
                currentRow.icol_cen = currentRow.ncol / 2;
            }

            row[irow] = currentRow;
        }

        // Get the number of columns at the equator
        final long ncol_cen = row[(int) (nrow_half - 1)].ncol;

        // Calculate the distance at the equator between
        // the centers of two columns (and the inverse)

        col_dist = (TWOPI * sphere) / ncol_cen;
        col_dist_inv = ncol_cen / (TWOPI * sphere);
    }

    IsinPoint transform(IsinPoint point) {
        final double x = point.getX();
        final double y = point.getY();

        // Latitude
        double lat = (y - false_north) * sphere_inv;
        if (lat < -HALFPI || lat > HALFPI) {
            lat = Double.NaN;
        }

        // Integer row number
        long irow = (int) ((HALFPI - lat) * ang_size_inv);
        if (irow >= nrow_half) {
            irow = (this.nrow - 1) - irow;
        }
        if (irow < 0) {
            irow = 0;
        }

        // Column number (relative to center)
        double col = (x - false_east) * col_dist_inv;

        // Fractional longitude (between 0 and 1)
        final int nrow = (int) irow;
        double flon = (col + row[nrow].icol_cen) * row[nrow].ncol_inv;
        if (flon < 0.0 || flon > 1.0) {
            return new IsinPoint(Double.NaN, Double.NaN);
        }

        // Actual longitude
        double lon = ref_lon + (flon * TWOPI);
        if (lon >= Math.PI) {
            lon -= TWOPI;
        }
        if (lon < -Math.PI) {
            lon += TWOPI;
        }

        return new IsinPoint(lon, lat);
    }
}
