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
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.Range;

import java.awt.*;
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

    private TiePointGrid _latGrid;
    private TiePointGrid _lonGrid;
    int _scanlineHeight;
    int _scanlineOffset;

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
        if (latGrid.getRasterWidth() != lonGrid.getRasterWidth() ||
                latGrid.getRasterHeight() != lonGrid.getRasterHeight() ||
                latGrid.getOffsetX() != lonGrid.getOffsetX() ||
                latGrid.getOffsetY() != lonGrid.getOffsetY() ||
                latGrid.getSubSamplingX() != lonGrid.getSubSamplingX() ||
                latGrid.getSubSamplingY() != lonGrid.getSubSamplingY()) {
            throw new IllegalArgumentException("latGrid is not compatible with lonGrid");
        }
        _latGrid = latGrid;
        _lonGrid = lonGrid;
        setGridOwner(_lonGrid.getOwner());
        _scanlineHeight = scanlineHeight;
        _scanlineOffset = 0;
        try {
            init();
        } catch (IOException e) {
            throw new IllegalArgumentException("can not init geocode");
        }
    }

    /**
     * get the number of line in the whole scene
     * @return lines in the scene
     */
    public int getSceneHeight() {
        return _lonGrid.getSceneRasterHeight();
    }

    /**
     * get the number of lines (num detectors) in a scan
     * @return number of lines in a scan
     */
    public int getScanlineHeight() {
        return _scanlineHeight;
    }

    /**
     * get the number of lines between the start of a scan and the first line of data
     * @return scan line offset
     */
    public int getScanlineOffset() {
        return _scanlineOffset;
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
        if (_latGrid == null || that._latGrid == null) {
            return false;
        }
        if (!_latGrid.equals(that._latGrid)) {
            return false;
        }
        if (_lonGrid == null || that._lonGrid == null) {
            return false;
        }
        if (!_lonGrid.equals(that._lonGrid)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = _latGrid != null ? _latGrid.hashCode() : 0;
        result = 31 * result + (_lonGrid != null ? _lonGrid.hashCode() : 0);
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
        _latGrid = null;
        _lonGrid = null;
    }

    /**
     * walk through the latitude and find the edge of the scan
     * where the lat overlaps the previous lat.  set _scanlineOffset
     */
    private void calculateScanlineOffset() {
        int start = -1;
        final float[] latPoints = _latGrid.getTiePoints();
        int latWidth = _latGrid.getRasterWidth();

        // look at first pixel in each line
        for(int i=1; i<_latGrid.getRasterHeight(); i++) {
            if(latPoints[(i-1)*latWidth] < latPoints[i*latWidth]) {
                start = (int)(i*_latGrid.getSubSamplingY());
                break;
            }
        }
        // if not found try end of line
        if(start == -1) {
            for(int i=1; i<_latGrid.getRasterHeight(); i++) {
                if(latPoints[i*latWidth-1] < latPoints[(i+1)*latWidth-1]) {
                    start = (int)(i*_latGrid.getSubSamplingY());
                    break;
                }
            }
        }

        if(start == -1) {       // did not find an overlap
            _scanlineOffset = 0;
        } else {
            _scanlineOffset = (_scanlineHeight - start) % _scanlineHeight;
        }

    }

    private void init() throws IOException {
        _gcList = new ArrayList<GeoCoding>();
        _centerLineList = new ArrayList<PolyLine>();
        final float osX = _lonGrid.getOffsetX();
        final float osY = _lonGrid.getOffsetY();
        final float ssX = _lonGrid.getSubSamplingX();
        final float ssY = _lonGrid.getSubSamplingY();

        final float[] latFloats = (float[]) _latGrid.getDataElems();
        final float[] lonFloats = (float[]) _lonGrid.getDataElems();

        calculateScanlineOffset();

        final int gridW = _lonGrid.getRasterWidth();
        final int gridH = _lonGrid.getRasterHeight();

        final int gridScanlineHeight = (int)(_scanlineHeight / ssY);
        final int gridScanlineOffset = (int)(_scanlineOffset / ssY);
        final int gcRawWidth = gridW * gridScanlineHeight;

        int firstY = 0;

        // create first partial stripe if needed
        if (gridScanlineOffset != 0) {
            firstY = gridScanlineHeight - gridScanlineOffset;
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, 0, lons, gridScanlineOffset * gridW, firstY * gridW);
            System.arraycopy(latFloats, 0, lats, gridScanlineOffset * gridW, firstY * gridW);
            for (int x = 0; x < gridW; x++) {
                float deltaLat;
                float deltaLon;
                float refLat = latFloats[x];
                float refLon = lonFloats[x];
                if(firstY > 1) {
                    deltaLat = latFloats[gridW + x] - latFloats[x];
                    deltaLon = lonFloats[gridW + x] - lonFloats[x];
                } else {
                    deltaLat = latFloats[(firstY+1)*gridW + x] - latFloats[firstY*gridW + x];
                    deltaLon = lonFloats[(firstY+1)*gridW + x] - lonFloats[firstY*gridW + x];
                }
                for (int y = 0; y < gridScanlineOffset; y++) {
                    lons[y * gridW + x] = refLon - (deltaLon * (gridScanlineOffset - y));
                    lats[y * gridW + x] = refLat - (deltaLat * (gridScanlineOffset - y));
                }
            }
            addStripeGeocode(lats, lons, 0-gridScanlineOffset, gridW, gridScanlineHeight, osX, osY, ssX, ssY);
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
                                  float offsetX, float offsetY, float subSamplingX, float subSamplingY) throws IOException {
        GeoCoding gc = createStripeGeocode(lats, lons, y, stripeW, stripeH, offsetX, offsetY, subSamplingX, subSamplingY);
        if (gc != null) {
            _gcList.add(gc);
            _centerLineList.add(createCenterPolyLine(gc, _latGrid.getSceneRasterWidth(), _scanlineHeight));
        } else {
            _gcList.add(null);
            _centerLineList.add(null);
        }
    }

    private GeoCoding createStripeGeocode(float[] lats, float[] lons, int y, int stripeW, int stripeH,
                                          float offsetX, float offsetY, float subSamplingX, float subSamplingY) throws IOException {
        final Range range = Range.computeRangeFloat(lats, IndexValidator.TRUE, null, ProgressMonitor.NULL);
        if (range.getMin() < -90) {
            return null;
        } else {
            final ModisTiePointGrid latGrid = new ModisTiePointGrid("lat" + y, stripeW, stripeH, offsetX, offsetY, subSamplingX, subSamplingY, lats);
            final ModisTiePointGrid lonGrid = new ModisTiePointGrid("lon" + y, stripeW, stripeH, offsetX, offsetY, subSamplingX, subSamplingY, lons, TiePointGrid.DISCONT_AT_180);
            final TiePointGeoCoding geoCoding = new TiePointGeoCoding(latGrid, lonGrid, getDatum());
            _cross180 = _cross180 || geoCoding.isCrossingMeridianAt180();
            return geoCoding;
        }
    }

    /**
     * Transfers the geo-coding of the {@link org.esa.beam.framework.datamodel.Scene srcScene} to the {@link org.esa.beam.framework.datamodel.Scene destScene} with respect to the given
     * {@link org.esa.beam.framework.dataio.ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        final String latGridName = _latGrid.getName();
        final String lonGridName = _lonGrid.getName();
        final Product destProduct = destScene.getProduct();

        removeTiePointGrid(destScene, latGridName);
        removeTiePointGrid(destScene, lonGridName);

        if (subsetDef == null) {
            destProduct.addTiePointGrid(_latGrid);
            destProduct.addTiePointGrid(_lonGrid);
            BowtieTiePointGeoCoding destGeo = new BowtieTiePointGeoCoding(_latGrid, _lonGrid, _scanlineHeight);
            destScene.setGeoCoding(destGeo);
            return true;
        }

        if (subsetDef.getSubSamplingX() != 1 || subsetDef.getSubSamplingY() != 1) {
            TiePointGrid latGrid = destProduct.getTiePointGrid(latGridName);
            if (latGrid == null) {
                latGrid = TiePointGrid.createSubset(_latGrid, subsetDef);
                destProduct.addTiePointGrid(latGrid);
            }
            TiePointGrid lonGrid = destProduct.getTiePointGrid(lonGridName);
            if (lonGrid == null) {
                lonGrid = TiePointGrid.createSubset(_lonGrid, subsetDef);
                destProduct.addTiePointGrid(lonGrid);
            }

            if (latGrid != null && lonGrid != null) {
                destScene.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, getDatum()));
                return true;
            }
            return false;
        }

        Rectangle region = subsetDef.getRegion();
        if(region == null) {
            destProduct.addTiePointGrid(_latGrid);
            destProduct.addTiePointGrid(_lonGrid);
            BowtieTiePointGeoCoding destGeo = new BowtieTiePointGeoCoding(_latGrid, _lonGrid, _scanlineHeight);
            destScene.setGeoCoding(destGeo);
            return true;
        }

        // make sub grids
        float[] newLatFloats = new float[region.width * region.height];
        float[] newLonFloats = new float[region.width * region.height];
        float[] tmpStripeFloats = new float[region.width * _scanlineHeight];
        int firstY = region.y;
        int newScanlineOffset = (_scanlineOffset + region.y) % _scanlineHeight;
        int gcIndex = (_scanlineOffset + region.y) / _scanlineHeight;

        // copy first partial stripe
        if(newScanlineOffset != 0) {
            int copyH = _scanlineHeight-newScanlineOffset;
            if (copyH > region.height) {
                copyH = region.height;
            }
            TiePointGeoCoding stripeGc = (TiePointGeoCoding) _gcList.get(gcIndex);
            tmpStripeFloats = stripeGc.getLatGrid().getPixels(region.x, newScanlineOffset, region.width, copyH, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLatFloats, 0, copyH*region.width);
            tmpStripeFloats = stripeGc.getLonGrid().getPixels(region.x, newScanlineOffset, region.width, copyH, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLonFloats, 0, copyH*region.width);

            firstY += _scanlineHeight-newScanlineOffset;
            gcIndex++;
        }

        // copy all the middle stripes
        while((firstY+_scanlineHeight) <= (region.y+region.height)) {
            TiePointGeoCoding stripeGc = (TiePointGeoCoding) _gcList.get(gcIndex);
            tmpStripeFloats = stripeGc.getLatGrid().getPixels(region.x, 0, region.width, _scanlineHeight, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLatFloats, (firstY-region.y)*region.width, _scanlineHeight*region.width);
            tmpStripeFloats = stripeGc.getLonGrid().getPixels(region.x, 0, region.width, _scanlineHeight, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLonFloats, (firstY-region.y)*region.width, _scanlineHeight*region.width);

            firstY += _scanlineHeight;
            gcIndex++;
        }

        // copy last partial stripe
        int lastH = (region.y+region.height) - firstY;
        if(lastH > 0) {
            TiePointGeoCoding stripeGc = (TiePointGeoCoding) _gcList.get(gcIndex);
            tmpStripeFloats = stripeGc.getLatGrid().getPixels(region.x, 0, region.width, lastH, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLatFloats, (firstY-region.y)*region.width, lastH*region.width);
            tmpStripeFloats = stripeGc.getLonGrid().getPixels(region.x, 0, region.width, lastH, tmpStripeFloats);
            System.arraycopy(tmpStripeFloats, 0, newLonFloats, (firstY-region.y)*region.width, lastH*region.width);
        }

        TiePointGrid latGrid = new TiePointGrid(latGridName, region.width, region.height, 0.5f, 0.5f, 1, 1, newLatFloats);
        TiePointGrid lonGrid = new TiePointGrid(lonGridName, region.width, region.height, 0.5f, 0.5f, 1, 1, newLonFloats);

        destProduct.addTiePointGrid(latGrid);
        destProduct.addTiePointGrid(lonGrid);

        BowtieTiePointGeoCoding destGeo = new BowtieTiePointGeoCoding(latGrid, lonGrid, getScanlineHeight());
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
