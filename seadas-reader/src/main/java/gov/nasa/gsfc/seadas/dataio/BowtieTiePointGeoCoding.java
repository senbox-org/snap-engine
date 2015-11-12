/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.math.IndexValidator;
import org.esa.snap.core.util.math.Range;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The <code>BowtieTiePointGeoCoding</code> class is a special geo-coding for
 * MODIS Level-1B and Level-2 swath products.
 * <p/>
 * <p>It enables BEAM to transform the MODIS swaths to uniformly gridded
 * image that is geographically referenced according to user-specified
 * projection and resampling parameters.
 * Correction for oversampling between scans as a function of increasing
 * (off-nadir) scan angle is performed (correction for bow-tie effect).
 */
public class BowtieTiePointGeoCoding extends AbstractBowtieGeoCoding {

    private TiePointGrid latGrid;
    private TiePointGrid lonGrid;
    int scanlineHeight;
    int scanlineOffset;

    /**
     * Constructs geo-coding based on two given tie-point grids.
     *
     * @param latGrid the latitude grid, must not be <code>null</code>
     * @param lonGrid the longitude grid, must not be <code>null</code>
     */
    public BowtieTiePointGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid, int scanlineHeight) {
        super();
        Guardian.assertNotNull("latGrid", latGrid);
        Guardian.assertNotNull("lonGrid", lonGrid);
        if (latGrid.getGridWidth() != lonGrid.getGridWidth() ||
                latGrid.getGridHeight() != lonGrid.getGridHeight() ||
                latGrid.getOffsetX() != lonGrid.getOffsetX() ||
                latGrid.getOffsetY() != lonGrid.getOffsetY() ||
                latGrid.getSubSamplingX() != lonGrid.getSubSamplingX() ||
                latGrid.getSubSamplingY() != lonGrid.getSubSamplingY()) {
            throw new IllegalArgumentException("latGrid is not compatible with lonGrid");
        }
        this.latGrid = latGrid;
        this.lonGrid = lonGrid;
        setGridOwner(this.lonGrid.getOwner());
        this.scanlineHeight = scanlineHeight;
        scanlineOffset = 0;
        try {
            init();
        } catch (IOException e) {
            throw new IllegalArgumentException("can not init geocode");
        }
    }

    /**
     * get the number of line in the whole scene
     *
     * @return lines in the scene
     */
    public int getSceneHeight() {
        return lonGrid.getRasterHeight();
    }

    /**
     * get the number of lines (num detectors) in a scan
     *
     * @return number of lines in a scan
     */
    public int getScanlineHeight() {
        return scanlineHeight;
    }

    /**
     * get the number of lines between the start of a scan and the first line of data
     *
     * @return scan line offset
     */
    public int getScanlineOffset() {
        return scanlineOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BowtieTiePointGeoCoding that = (BowtieTiePointGeoCoding) o;
        if (latGrid == null || that.latGrid == null) {
            return false;
        }
        if (!latGrid.equals(that.latGrid)) {
            return false;
        }
        if (lonGrid == null || that.lonGrid == null) {
            return false;
        }
        if (!lonGrid.equals(that.lonGrid)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = latGrid != null ? latGrid.hashCode() : 0;
        result = 31 * result + (lonGrid != null ? lonGrid.hashCode() : 0);
        return result;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    @Override
    public void dispose() {
        super.dispose();
        latGrid = null;
        lonGrid = null;
    }

    /**
     * walk through the latitude and find the edge of the scan
     * where the lat overlaps the previous lat.  set _scanlineOffset
     */
    private void calculateScanlineOffset() {
        int start = -1;
        final float[] latPoints = latGrid.getTiePoints();
        int latWidth = latGrid.getGridWidth();

        // look at first pixel in each line
        for (int i = 1; i < latGrid.getGridHeight(); i++) {
            if (latPoints[(i - 1) * latWidth] < latPoints[i * latWidth]) {
                start = (int) (i * latGrid.getSubSamplingY());
                break;
            }
        }
        // if not found try end of line
        if (start == -1) {
            for (int i = 1; i < latGrid.getGridHeight(); i++) {
                if (latPoints[i * latWidth - 1] < latPoints[(i + 1) * latWidth - 1]) {
                    start = (int) (i * latGrid.getSubSamplingY());
                    break;
                }
            }
        }

        if (start == -1) {       // did not find an overlap
            scanlineOffset = 0;
        } else {
            start = start % scanlineHeight;
            if (start == 0) {
                scanlineOffset = 0;
            } else {
                scanlineOffset = scanlineHeight - start;
            }
        }
    }


    private void init() throws IOException {
        gcList = new ArrayList<>();
        centerLineList = new ArrayList<>();
        final double osX = lonGrid.getOffsetX();
        final double osY = lonGrid.getOffsetY();
        final double ssX = lonGrid.getSubSamplingX();
        final double ssY = lonGrid.getSubSamplingY();

        final float[] latFloats = (float[]) latGrid.getDataElems();
        final float[] lonFloats = (float[]) lonGrid.getDataElems();

        calculateScanlineOffset();

        final int gridW = lonGrid.getGridWidth();
        final int gridH = lonGrid.getGridHeight();

        final int gcRawWidth = gridW * scanlineHeight;

        int firstY = 0;

        // create first if needed
        // use the delta from the neighboring stripe to extrapolate the data
        if (scanlineOffset != 0) {
            firstY = scanlineHeight - scanlineOffset;
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, 0, lons, scanlineOffset * gridW, (scanlineHeight - scanlineOffset) * gridW);
            System.arraycopy(latFloats, 0, lats, scanlineOffset * gridW, (scanlineHeight - scanlineOffset) * gridW);
            for (int x = 0; x < gridW; x++) {
                int y1 = scanlineHeight - scanlineOffset; // coord of first y in next stripe
                int y2 = y1 + scanlineHeight - 1;         // coord of last y in next stripe
                int index1 = y1 * gridW + x;
                int index2 = y2 * gridW + x;
                float deltaLat = (latFloats[index2] - latFloats[index1]) / (scanlineHeight - 1);
                float deltaLon = (lonFloats[index2] - lonFloats[index1]) / (scanlineHeight - 1);
                float refLat = latFloats[x];
                float refLon = lonFloats[x];

                for (int y = 0; y < scanlineOffset; y++) {
                    lons[y * gridW + x] = refLon - (deltaLon * (scanlineOffset - y));
                    lats[y * gridW + x] = refLat - (deltaLat * (scanlineOffset - y));
                }
            }
            addStripeGeocode(lats, lons, 0, gridW, scanlineHeight, osX, osY, ssX, ssY);
        }

        // add all of the normal scans
        for (int y = firstY; y + scanlineHeight <= gridH; y += scanlineHeight) {
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, y * gridW, lons, 0, gcRawWidth);
            System.arraycopy(latFloats, y * gridW, lats, 0, gcRawWidth);
            addStripeGeocode(lats, lons, y, gridW, scanlineHeight, osX, osY, ssX, ssY);
        }

        // create last stripe
        int lastStripeH = (gridH - scanlineHeight + scanlineOffset) % scanlineHeight;
        if (lastStripeH != 0) {
            int lastStripeY = gridH - lastStripeH - 1; // y coord of first y of last stripe
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, lastStripeY * gridW, lons, 0, lastStripeH * gridW);
            System.arraycopy(latFloats, lastStripeY * gridW, lats, 0, lastStripeH * gridW);
            for (int x = 0; x < gridW; x++) {
                int y1 = lastStripeY - scanlineHeight; // coord of first y in next stripe
                int y2 = lastStripeY - 1;         // coord of last y in next stripe
                int index1 = y1 * gridW + x;
                int index2 = y2 * gridW + x;
                float deltaLat = (latFloats[index2] - latFloats[index1]) / (scanlineHeight - 1);
                float deltaLon = (lonFloats[index2] - lonFloats[index1]) / (scanlineHeight - 1);
                float refLat = latFloats[lastStripeY * gridW + x];
                float refLon = lonFloats[lastStripeY * gridW + x];

                for (int y = lastStripeH; y < scanlineHeight; y++) {
                    lons[y * gridW + x] = refLon - (deltaLon * (y - lastStripeH + 1));
                    lats[y * gridW + x] = refLat - (deltaLat * (y - lastStripeH + 1));
                }
            }
            addStripeGeocode(lats, lons, lastStripeY, gridW, scanlineHeight, osX, osY, ssX, ssY);
        }

        initSmallestAndLargestValidGeocodingIndices();
    }

    private void addStripeGeocode(float[] lats, float[] lons, int y, int stripeW, int stripeH,
                                  double offsetX, double offsetY, double subSamplingX, double subSamplingY) throws IOException {
        GeoCoding gc = createStripeGeocode(lats, lons, y, stripeW, stripeH, offsetX, offsetY, subSamplingX, subSamplingY);
        if (gc != null) {
            gcList.add(gc);
            centerLineList.add(createCenterPolyLine(gc, stripeW, stripeH));
        } else {
            gcList.add(null);
            centerLineList.add(null);
        }
    }

    private GeoCoding createStripeGeocode(float[] lats, float[] lons, int y, int stripeW, int stripeH,
                                          double offsetX, double offsetY, double subSamplingX, double subSamplingY) throws IOException {
        final Range range = Range.computeRangeFloat(lats, IndexValidator.TRUE, null, ProgressMonitor.NULL);
        if (range.getMin() < -90) {
            return null;
        } else {
            final ModisTiePointGrid latGrid = new ModisTiePointGrid("lat" + y, stripeW, stripeH, offsetX, offsetY, subSamplingX, subSamplingY, lats);
            final ModisTiePointGrid lonGrid = new ModisTiePointGrid("lon" + y, stripeW, stripeH, offsetX, offsetY, subSamplingX, subSamplingY, lons, TiePointGrid.DISCONT_AT_180);
            final TiePointGeoCoding geoCoding = new TiePointGeoCoding(latGrid, lonGrid, getDatum());
            cross180 = cross180 || geoCoding.isCrossingMeridianAt180();
            return geoCoding;
        }
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        final String latGridName = latGrid.getName();
        final String lonGridName = lonGrid.getName();

        if (mustRecalculateTiePointGrids(subsetDef)) {
            try {
                recalculateTiePointGrids(srcScene, destScene, subsetDef, latGridName, lonGridName);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return createGeocoding(destScene, ((BowtieTiePointGeoCoding) srcScene.getGeoCoding()).getScanlineHeight());
    }

    private boolean recalculateTiePointGrids(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef, String latGridName, String lonGridName) throws IOException {
        // first step - remove location TP grids that have already been transferred. Their size is
        // calculated wrong in most cases
        final TiePointGrid falseTiePointGrid = destScene.getProduct().getTiePointGrid(latGridName);
        final double rightOffsetX = falseTiePointGrid.getOffsetX();
        final double falseOffsetY = falseTiePointGrid.getOffsetY();
        final double rightSubsamplingX = falseTiePointGrid.getSubSamplingX();
        final double rightSubsamplingY = falseTiePointGrid.getSubSamplingY();

        removeTiePointGrid(destScene, latGridName);
        removeTiePointGrid(destScene, lonGridName);

        final Product srcProduct = srcScene.getProduct();

        final Rectangle region = subsetDef.getRegion();
        final int startY = calculateStartLine(getScanlineHeight(), region);
        final int stopY = calculateStopLine(getScanlineHeight(), region);
        final int extendedHeight = stopY - startY;

        float[] recalculatedLatFloats = new float[region.width * extendedHeight];
        recalculatedLatFloats = srcProduct.getTiePointGrid(latGridName).getPixels(region.x, startY, region.width, extendedHeight, recalculatedLatFloats);

        float[] recalculatedLonFloats = new float[region.width * extendedHeight];
        recalculatedLonFloats = srcProduct.getTiePointGrid(lonGridName).getPixels(region.x, startY, region.width, extendedHeight, recalculatedLonFloats);


        final int yOffsetIncrement = startY - region.y;
        final TiePointGrid correctedLatTiePointGrid = new TiePointGrid(latGridName,
                                                                       region.width,
                                                                       extendedHeight,
                                                                       rightOffsetX,
                                                                       falseOffsetY + yOffsetIncrement,
                                                                       rightSubsamplingX,
                                                                       rightSubsamplingY,
                                                                       recalculatedLatFloats
        );
        final TiePointGrid correctedLonTiePointGrid = new TiePointGrid(lonGridName,
                                                                       region.width,
                                                                       extendedHeight,
                                                                       rightOffsetX,
                                                                       falseOffsetY + yOffsetIncrement,
                                                                       rightSubsamplingX,
                                                                       rightSubsamplingY,
                                                                       recalculatedLonFloats
        );
        destScene.getProduct().addTiePointGrid(correctedLatTiePointGrid);
        destScene.getProduct().addTiePointGrid(correctedLonTiePointGrid);

        return false;
    }

    private void removeTiePointGrid(Scene destScene, String gridName) {
        final TiePointGrid tiePointGrid = destScene.getProduct().getTiePointGrid(gridName);
        if (tiePointGrid != null) {
            destScene.getProduct().removeTiePointGrid(tiePointGrid);
        }
    }

    private boolean createGeocoding(Scene destScene, int stripeHeight) {
        final String latGridName = latGrid.getName();
        final String lonGridName = lonGrid.getName();
        final TiePointGrid latGrid = destScene.getProduct().getTiePointGrid(latGridName);
        final TiePointGrid lonGrid = destScene.getProduct().getTiePointGrid(lonGridName);
        if (latGrid != null && lonGrid != null) {
            destScene.setGeoCoding(new BowtieTiePointGeoCoding(latGrid, lonGrid, stripeHeight));
            return true;
        }
        return false;
    }

    static boolean mustRecalculateTiePointGrids(ProductSubsetDef subsetDef) {
        return subsetDef != null && subsetDef.getRegion() != null;
    }

}
