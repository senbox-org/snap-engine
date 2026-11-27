/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.engine_utilities.gpf;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

/**
 * Computes the pixel-centre latitude/longitude of a tile of a map-projected {@link CrsGeoCoding}
 * without running the (expensive) inverse map projection for every pixel.
 * <p>
 * The tile is partitioned into cells of about {@link #CELL_SIZE} pixels. Within each cell the exact
 * geographic position is computed at 3x3 nodes (corners, edge midpoints, centre) and the remaining
 * pixels are filled by biquadratic (tensor-product Lagrange) interpolation. Every cell is verified
 * against the exact transform at its four quarter-centre points; when any of them deviates by more
 * than {@link #TOLERANCE_DEG} in latitude or longitude (or is NaN) the cell is split into four and the
 * process repeats. Cells that are too small to split, or whose error is too large to be fixed by splitting
 * (predicted from the ~h^3 scaling of the residual), are computed with the exact batch transform of the
 * geocoding. Discontinuities (antimeridian wrap, projection domain edges, poles) therefore end up being
 * computed exactly.
 * <p>
 * The interpolation residual for smooth projections such as Transverse Mercator is dominated by
 * third-order terms; for 32 px cells of 10 m it is of the order of 1e-13 deg, far below the
 * tolerance of 1e-9 deg (about 0.1 mm on the ground, i.e. 1e-5 of a 10 m pixel).
 */
final class CrsTileGeoPosInterpolator {

    /** Maximum accepted deviation from the exact transform at the check points, in degrees. */
    static final double TOLERANCE_DEG = 1.0e-9;
    /** Nominal size (in pixel spans) of the initial interpolation cells. */
    static final int CELL_SIZE = 32;
    /** Cells with a span below this in either direction are not subdivided further but computed exactly. */
    private static final int MIN_SPLIT_SPAN = 8;
    /** Tiles with a span below this in either direction are computed exactly. */
    static final int MIN_TILE_SPAN = 4;
    /** Exact blocks of at least this many pixels use the batch transform of the geocoding. */
    private static final int BATCH_MIN_PIXELS = 16;

    private static final byte NONE = 0;
    private static final byte INTERPOLATED = 1;
    private static final byte EXACT = 2;

    private final CrsGeoCoding geoCoding;
    private final int x1, y1, width;
    private final double[] lat, lon;
    private final byte[] state;
    private final PixelPos pixelPos = new PixelPos();
    private final GeoPos geoPos = new GeoPos();
    private int exactCount;

    private CrsTileGeoPosInterpolator(final CrsGeoCoding geoCoding, final int x1, final int y1,
                                      final int w, final int h, final double[] lat, final double[] lon) {
        this.geoCoding = geoCoding;
        this.x1 = x1;
        this.y1 = y1;
        this.width = w;
        this.lat = lat;
        this.lon = lon;
        this.state = new byte[w * h];
    }

    /**
     * @return true if the tile pixels of this geocoding should be computed by interpolation.
     * Geographic map CRSs are excluded: their image-to-geo transform is affine and already cheap,
     * so they keep the exact path.
     */
    static boolean isApplicable(final CrsGeoCoding geoCoding, final int w, final int h) {
        if (w <= MIN_TILE_SPAN || h <= MIN_TILE_SPAN) {
            return false;
        }
        final CoordinateReferenceSystem mapCRS = geoCoding.getMapCRS();
        return mapCRS != null && !(mapCRS instanceof GeographicCRS);
    }

    /**
     * Fills lat/lon (row-major, w*h) with the pixel-centre geographic positions of the given tile.
     *
     * @return the number of pixels that were evaluated with the exact transform
     */
    static int computePixels(final CrsGeoCoding geoCoding, final int x1, final int y1, final int w, final int h,
                             final double[] lat, final double[] lon) {
        final CrsTileGeoPosInterpolator interpolator = new CrsTileGeoPosInterpolator(geoCoding, x1, y1, w, h, lat, lon);
        interpolator.run(w, h);
        return interpolator.exactCount;
    }

    private void run(final int w, final int h) {
        final int[] xs = partition(w - 1);
        final int[] ys = partition(h - 1);
        for (int j = 0; j + 1 < ys.length; j++) {
            for (int i = 0; i + 1 < xs.length; i++) {
                processCell(xs[i], ys[j], xs[i + 1], ys[j + 1]);
            }
        }
    }

    /** Splits [0, span] into near-equal intervals of about CELL_SIZE, returning the boundaries. */
    private static int[] partition(final int span) {
        final int n = Math.max(1, span / CELL_SIZE);
        final int[] b = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            b[i] = (int) ((long) span * i / n);
        }
        return b;
    }

    /** Process the cell with inclusive local pixel corners (ax, ay) and (bx, by). */
    private void processCell(final int ax, final int ay, final int bx, final int by) {
        final int sx = bx - ax;
        final int sy = by - ay;
        if (sx < MIN_SPLIT_SPAN / 2 || sy < MIN_SPLIT_SPAN / 2) {
            exactAll(ax, ay, bx, by);
            return;
        }
        final int mx = ax + sx / 2;
        final int my = ay + sy / 2;
        final int[] nx = {ax, mx, bx};
        final int[] ny = {ay, my, by};

        final double[] nLat = new double[9];
        final double[] nLon = new double[9];
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 3; i++) {
                final int idx = exact(nx[i], ny[j]);
                nLat[j * 3 + i] = lat[idx];
                nLon[j * 3 + i] = lon[idx];
            }
        }

        // verify at the quarter-centre points
        final int qx0 = ax + sx / 4, qx1 = ax + (3 * sx) / 4;
        final int qy0 = ay + sy / 4, qy1 = ay + (3 * sy) / 4;
        final double err = Math.max(Math.max(error(qx0, qy0, nx, ny, nLat, nLon), error(qx1, qy0, nx, ny, nLat, nLon)),
                                    Math.max(error(qx0, qy1, nx, ny, nLat, nLon), error(qx1, qy1, nx, ny, nLat, nLon)));

        if (!(err <= TOLERANCE_DEG)) {
            // The residual of a smooth transform shrinks ~8x per halving of the cell. If the cells that would pass
            // are too small to interpolate (or the error is NaN/huge: discontinuity, pole, outside the projection
            // domain) the cell would end up nearly all exact anyway: compute it in one batch.
            if (!canRefine(Math.min(sx, sy), err)) {
                exactAll(ax, ay, bx, by);
            } else {
                processCell(ax, ay, mx, my);
                processCell(mx, ay, bx, my);
                processCell(ax, my, mx, by);
                processCell(mx, my, bx, by);
            }
            return;
        }
        // the cell interior is written unconditionally by fill(); keep the exact values of the check points
        final int[] qIdx = {qy0 * width + qx0, qy0 * width + qx1, qy1 * width + qx0, qy1 * width + qx1};
        final double[] qLat = new double[4], qLon = new double[4];
        for (int k = 0; k < 4; k++) {
            qLat[k] = lat[qIdx[k]];
            qLon[k] = lon[qIdx[k]];
        }
        fill(ax, ay, bx, by, nx, ny, nLat, nLon);
        for (int k = 0; k < 4; k++) {
            lat[qIdx[k]] = qLat[k];
            lon[qIdx[k]] = qLon[k];
        }
    }

    /** @return true if halving cells of the given span until their predicted error passes keeps them interpolable. */
    private static boolean canRefine(final int span, final double err) {
        if (!(err < Double.MAX_VALUE) || span < MIN_SPLIT_SPAN) {
            return false;
        }
        int s = span;
        double e = err;
        while (e > TOLERANCE_DEG) {
            e /= 8;
            s /= 2;
            if (s < MIN_SPLIT_SPAN / 2) {
                return false;
            }
        }
        return true;
    }

    /** @return the larger of the latitude and longitude interpolation errors at (x, y); NaN if undefined. */
    private double error(final int x, final int y, final int[] nx, final int[] ny,
                         final double[] nLat, final double[] nLon) {
        final double[] wx = weights(x, nx);
        final double[] wy = weights(y, ny);
        final double iLat = eval(wx, wy, nLat);
        final double iLon = eval(wx, wy, nLon);
        final int idx = exact(x, y);
        final double eLat = Math.abs(iLat - lat[idx]);
        final double eLon = Math.abs(iLon - lon[idx]);
        return Double.isNaN(eLat) || Double.isNaN(eLon) ? Double.NaN : Math.max(eLat, eLon);
    }

    private void fill(final int ax, final int ay, final int bx, final int by, final int[] nx, final int[] ny,
                      final double[] nLat, final double[] nLon) {
        final int cols = bx - ax + 1;
        final double[] wx0 = new double[cols], wx1 = new double[cols], wx2 = new double[cols];
        for (int x = ax; x <= bx; x++) {
            final double[] w = weights(x, nx);
            wx0[x - ax] = w[0];
            wx1[x - ax] = w[1];
            wx2[x - ax] = w[2];
        }
        for (int y = ay; y <= by; y++) {
            final double[] wy = weights(y, ny);
            // interpolate the three node columns along y
            final double lat0 = wy[0] * nLat[0] + wy[1] * nLat[3] + wy[2] * nLat[6];
            final double lat1 = wy[0] * nLat[1] + wy[1] * nLat[4] + wy[2] * nLat[7];
            final double lat2 = wy[0] * nLat[2] + wy[1] * nLat[5] + wy[2] * nLat[8];
            final double lon0 = wy[0] * nLon[0] + wy[1] * nLon[3] + wy[2] * nLon[6];
            final double lon1 = wy[0] * nLon[1] + wy[1] * nLon[4] + wy[2] * nLon[7];
            final double lon2 = wy[0] * nLon[2] + wy[1] * nLon[5] + wy[2] * nLon[8];
            final int row = y * width + ax;
            if (y == ay || y == by) {
                for (int c = 0; c < cols; c++) {
                    setIfNotExact(row + c, wx0[c], wx1[c], wx2[c], lat0, lat1, lat2, lon0, lon1, lon2);
                }
            } else {
                // Interior pixels belong to this cell only. The only exact values there are the centre node,
                // which Lagrange interpolation reproduces bit-exactly, and the check points, restored by the caller.
                setIfNotExact(row, wx0[0], wx1[0], wx2[0], lat0, lat1, lat2, lon0, lon1, lon2);
                for (int c = 1; c < cols - 1; c++) {
                    lat[row + c] = wx0[c] * lat0 + wx1[c] * lat1 + wx2[c] * lat2;
                    lon[row + c] = wx0[c] * lon0 + wx1[c] * lon1 + wx2[c] * lon2;
                }
                final int c = cols - 1;
                setIfNotExact(row + c, wx0[c], wx1[c], wx2[c], lat0, lat1, lat2, lon0, lon1, lon2);
            }
        }
    }

    private void setIfNotExact(final int idx, final double w0, final double w1, final double w2,
                               final double lat0, final double lat1, final double lat2,
                               final double lon0, final double lon1, final double lon2) {
        if (state[idx] != EXACT) {
            lat[idx] = w0 * lat0 + w1 * lat1 + w2 * lat2;
            lon[idx] = w0 * lon0 + w1 * lon1 + w2 * lon2;
            state[idx] = INTERPOLATED;
        }
    }

    private static double eval(final double[] wx, final double[] wy, final double[] v) {
        double sum = 0;
        for (int j = 0; j < 3; j++) {
            sum += wy[j] * (wx[0] * v[j * 3] + wx[1] * v[j * 3 + 1] + wx[2] * v[j * 3 + 2]);
        }
        return sum;
    }

    /** Quadratic Lagrange weights of position t for nodes p[0] < p[1] < p[2]. */
    private static double[] weights(final int t, final int[] p) {
        final double d0 = t - p[0], d1 = t - p[1], d2 = t - p[2];
        return new double[]{
                d1 * d2 / ((double) (p[0] - p[1]) * (p[0] - p[2])),
                d0 * d2 / ((double) (p[1] - p[0]) * (p[1] - p[2])),
                d0 * d1 / ((double) (p[2] - p[0]) * (p[2] - p[1]))
        };
    }

    private void exactAll(final int ax, final int ay, final int bx, final int by) {
        final int cw = bx - ax + 1, ch = by - ay + 1;
        if (cw * ch >= BATCH_MIN_PIXELS) {
            // same batch transform as the non-interpolated path; cheaper than point by point
            final double[] cLat = new double[cw * ch], cLon = new double[cw * ch];
            geoCoding.getPixels(x1 + ax, y1 + ay, cw, ch, cLat, cLon);
            for (int y = 0; y < ch; y++) {
                int idx = (ay + y) * width + ax;
                for (int x = 0; x < cw; x++, idx++) {
                    if (state[idx] != EXACT) {
                        lat[idx] = cLat[y * cw + x];
                        lon[idx] = cLon[y * cw + x];
                        state[idx] = EXACT;
                        exactCount++;
                    }
                }
            }
            return;
        }
        for (int y = ay; y <= by; y++) {
            for (int x = ax; x <= bx; x++) {
                exact(x, y);
            }
        }
    }

    /** Ensures the exact position at local pixel (x, y) is stored and returns its array index. */
    private int exact(final int x, final int y) {
        final int idx = y * width + x;
        if (state[idx] != EXACT) {
            pixelPos.setLocation(x1 + x + 0.5, y1 + y + 0.5);
            geoCoding.getGeoPos(pixelPos, geoPos);
            lat[idx] = geoPos.lat;
            lon[idx] = geoPos.lon;
            state[idx] = EXACT;
            exactCount++;
        }
        return idx;
    }
}
