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
     * @param scanlineHeight the number of detectors in a scan
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
            scanlineOffset = (scanlineHeight - start) % scanlineHeight;
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

        final int gridScanlineHeight = (int)(scanlineHeight / ssY);
        final int gridScanlineOffset = (int)(scanlineOffset / ssY);
        final int gcRawWidth = gridW * scanlineHeight;

        int firstY = 0;

        // create first partial stripe if needed
        if (gridScanlineOffset != 0) {
            firstY = gridScanlineHeight - gridScanlineOffset;
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, 0, lons, gridScanlineOffset * gridW, (gridScanlineHeight - gridScanlineOffset) * gridW);
            System.arraycopy(latFloats, 0, lats, gridScanlineOffset * gridW, (gridScanlineHeight - gridScanlineOffset) * gridW);
            for (int x = 0; x < gridW; x++) {
                float deltaLat;
                float deltaLon;
                float refLat = latFloats[x];
                float refLon = lonFloats[x];
                if ((gridScanlineHeight - gridScanlineOffset) > 1) {
                    deltaLat = latFloats[gridW + x] - latFloats[x];
                    deltaLon = lonFloats[gridW + x] - lonFloats[x];
                } else {
                    deltaLat = latFloats[(firstY + 1) * gridW + x] - latFloats[firstY * gridW + x];
                    deltaLon = lonFloats[(firstY + 1) * gridW + x] - lonFloats[firstY * gridW + x];
                }
                for (int y = 0; y < gridScanlineOffset; y++) {
                    lons[y * gridW + x] = refLon - (deltaLon * (gridScanlineOffset - y));
                    lats[y * gridW + x] = refLat - (deltaLat * (gridScanlineOffset - y));
                }
            }
            addStripeGeocode(lats, lons, 0 - gridScanlineOffset, gridW, gridScanlineHeight, osX, osY, ssX, ssY);
        }

        // add all of the normal scans
        for (; firstY + gridScanlineHeight <= gridH; firstY += gridScanlineHeight) {
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, firstY * gridW, lons, 0, gcRawWidth);
            System.arraycopy(latFloats, firstY * gridW, lats, 0, gcRawWidth);
            addStripeGeocode(lats, lons, firstY, gridW, gridScanlineHeight, osX, osY, ssX, ssY);
        }

        // create last partial stripe if needed
        if(firstY < gridH) {
            int lastStripeH = gridH - firstY;
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(latFloats, firstY * gridW, lats, 0, lastStripeH * gridW);
            System.arraycopy(lonFloats, firstY * gridW, lons, 0, lastStripeH * gridW);
            for (int x = 0; x < gridW; x++) {
                float deltaLat;
                float deltaLon;
                float refLat = latFloats[(gridH-1) * gridW + x];
                float refLon = lonFloats[(gridH-1) * gridW + x];
                if(lastStripeH > 1) {
                    deltaLat = refLat - latFloats[(gridH-2) * gridW + x];
                    deltaLon = refLon - lonFloats[(gridH-2) * gridW + x];
                } else {
                    deltaLat = latFloats[(firstY-1) * gridW + x] - latFloats[(firstY-2) * gridW + x];
                    deltaLon = lonFloats[(firstY-1) * gridW + x] - lonFloats[(firstY-2) * gridW + x];
                }
                for (int y = 0; y < gridScanlineHeight-lastStripeH; y++) {
                    lats[(y+lastStripeH) * gridW + x] = refLat + (deltaLat * y);
                    lons[(y+lastStripeH) * gridW + x] = refLon + (deltaLon * y);
                }
            }
            addStripeGeocode(lats, lons, firstY, gridW, gridScanlineHeight, osX, osY, ssX, ssY);
        }

        initSmallestAndLargestValidGeocodingIndices();
    }

    private void addStripeGeocode(float[] lats, float[] lons, int y, int stripeW, int stripeH,
                                  double offsetX, double offsetY, double subSamplingX, double subSamplingY) throws IOException {
        GeoCoding gc = createStripeGeocode(lats, lons, y, stripeW, stripeH, offsetX, offsetY, subSamplingX, subSamplingY);
        if (gc != null) {
            gcList.add(gc);
            centerLineList.add(createCenterPolyLine(gc, latGrid.getRasterWidth(), scanlineHeight));
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
        final Product destProduct = destScene.getProduct();

        removeTiePointGrid(destScene, latGridName);
        removeTiePointGrid(destScene, lonGridName);

        if (subsetDef == null) {
            destProduct.addTiePointGrid(latGrid);
            destProduct.addTiePointGrid(lonGrid);
            BowtieTiePointGeoCoding destGeo = new BowtieTiePointGeoCoding(latGrid, lonGrid, scanlineHeight);
            destScene.setGeoCoding(destGeo);
            return true;
        }

        if (subsetDef.getSubSamplingX() != 1 || subsetDef.getSubSamplingY() != 1) {
            TiePointGrid destLatGrid = destProduct.getTiePointGrid(latGridName);
            if (destLatGrid == null) {
                destLatGrid = TiePointGrid.createSubset(latGrid, subsetDef);
                destProduct.addTiePointGrid(destLatGrid);
            }
            TiePointGrid destLonGrid = destProduct.getTiePointGrid(lonGridName);
            if (destLonGrid == null) {
                destLonGrid = TiePointGrid.createSubset(lonGrid, subsetDef);
                destProduct.addTiePointGrid(destLonGrid);
            }

            if (destLatGrid != null && destLonGrid != null) {
                destScene.setGeoCoding(new TiePointGeoCoding(destLatGrid, destLonGrid, getDatum()));
                return true;
            }
            return false;
        }

        Rectangle region = subsetDef.getRegion();
        if(region == null) {
            destProduct.addTiePointGrid(latGrid);
            destProduct.addTiePointGrid(lonGrid);
            BowtieTiePointGeoCoding destGeo = new BowtieTiePointGeoCoding(latGrid, lonGrid, scanlineHeight);
            destScene.setGeoCoding(destGeo);
            return true;
        }

        // make sub grids
        float[] newLatFloats = new float[region.width * region.height];
        float[] newLonFloats = new float[region.width * region.height];
        float[] tmpStripeFloats = new float[region.width * scanlineHeight];
        int firstY = region.y;
        int newScanlineOffset = (scanlineOffset + region.y) % scanlineHeight;
        int gcIndex = (scanlineOffset + region.y) / scanlineHeight;

        // copy first partial stripe
        if(newScanlineOffset != 0) {
            int copyH = scanlineHeight-newScanlineOffset;
            TiePointGeoCoding stripeGc = (TiePointGeoCoding) gcList.get(gcIndex);
            tmpStripeFloats = stripeGc.getLatGrid().getPixels(region.x, newScanlineOffset, region.width, copyH, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLatFloats, 0, copyH*region.width);
            tmpStripeFloats = stripeGc.getLonGrid().getPixels(region.x, newScanlineOffset, region.width, copyH, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLonFloats, 0, copyH*region.width);

            firstY += copyH;
            gcIndex++;
        }

        // copy all the middle stripes
        while((firstY+scanlineHeight) <= (region.y+region.height)) {
            TiePointGeoCoding stripeGc = (TiePointGeoCoding) gcList.get(gcIndex);
            tmpStripeFloats = stripeGc.getLatGrid().getPixels(region.x, 0, region.width, scanlineHeight, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLatFloats, (firstY-region.y)*region.width, scanlineHeight*region.width);
            tmpStripeFloats = stripeGc.getLonGrid().getPixels(region.x, 0, region.width, scanlineHeight, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLonFloats, (firstY-region.y)*region.width, scanlineHeight*region.width);

            firstY += scanlineHeight;
            gcIndex++;
        }

        // copy last partial stripe
        int lastH = (region.y+region.height) - firstY;
        if(lastH > 0) {
            TiePointGeoCoding stripeGc = (TiePointGeoCoding) gcList.get(gcIndex);
            tmpStripeFloats = stripeGc.getLatGrid().getPixels(region.x, 0, region.width, lastH, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLatFloats, (firstY-region.y)*region.width, lastH*region.width);
            tmpStripeFloats = stripeGc.getLonGrid().getPixels(region.x, 0, region.width, lastH, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLonFloats, (firstY-region.y)*region.width, lastH*region.width);
        }

        TiePointGrid subLatGrid = new TiePointGrid(latGridName, region.width, region.height, 0.5f, 0.5f, 1, 1, newLatFloats);
        TiePointGrid subLonGrid = new TiePointGrid(lonGridName, region.width, region.height, 0.5f, 0.5f, 1, 1, newLonFloats);

        destProduct.addTiePointGrid(subLatGrid);
        destProduct.addTiePointGrid(subLonGrid);

        BowtieTiePointGeoCoding destGeo = new BowtieTiePointGeoCoding(subLatGrid, subLonGrid, scanlineHeight);
        destScene.setGeoCoding(destGeo);

        return true;
    }

    private void removeTiePointGrid(Scene destScene, String gridName) {
        final TiePointGrid tiePointGrid = destScene.getProduct().getTiePointGrid(gridName);
        if (tiePointGrid != null) {
            destScene.getProduct().removeTiePointGrid(tiePointGrid);
        }
    }

}
