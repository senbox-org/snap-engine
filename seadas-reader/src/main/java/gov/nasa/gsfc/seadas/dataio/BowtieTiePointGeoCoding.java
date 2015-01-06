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
            start = start % _scanlineHeight;
            if(start == 0) {
                _scanlineOffset = 0;
            } else {
                _scanlineOffset = _scanlineHeight - start;
            }
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

        final int gcRawWidth = gridW * _scanlineHeight;

        int firstY = 0;

        // create first if needed
        // use the delta from the neighboring stripe to extrapolate the data
        if (_scanlineOffset != 0) {
            firstY = _scanlineHeight - _scanlineOffset;
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, 0, lons, _scanlineOffset * gridW, (_scanlineHeight - _scanlineOffset) * gridW);
            System.arraycopy(latFloats, 0, lats, _scanlineOffset * gridW, (_scanlineHeight - _scanlineOffset) * gridW);
            for (int x = 0; x < gridW; x++) {
                int y1 = _scanlineHeight - _scanlineOffset; // coord of first y in next stripe
                int y2 = y1 + _scanlineHeight - 1;         // coord of last y in next stripe
                int index1 = y1 * gridW + x;
                int index2 = y2 * gridW + x;
                float deltaLat = (latFloats[index2] - latFloats[index1]) / (_scanlineHeight - 1);
                float deltaLon = (lonFloats[index2] - lonFloats[index1]) / (_scanlineHeight - 1);
                float refLat = latFloats[x];
                float refLon = lonFloats[x];

                for (int y = 0; y < _scanlineOffset; y++) {
                    lons[y * gridW + x] = refLon - (deltaLon * (_scanlineOffset - y));
                    lats[y * gridW + x] = refLat - (deltaLat * (_scanlineOffset - y));
                }
            }
            addStripeGeocode(lats, lons, 0, gridW, _scanlineHeight, osX, osY, ssX, ssY);
        }

        // add all of the normal scans
        for (int y = firstY; y + _scanlineHeight <= gridH; y += _scanlineHeight) {
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, y * gridW, lons, 0, gcRawWidth);
            System.arraycopy(latFloats, y * gridW, lats, 0, gcRawWidth);
            addStripeGeocode(lats, lons, y, gridW, _scanlineHeight, osX, osY, ssX, ssY);
        }

        // create last stripe
        int lastStripeH = (gridH - _scanlineHeight + _scanlineOffset) % _scanlineHeight;
        if (lastStripeH != 0) {
            int lastStripeY = gridH - lastStripeH - 1; // y coord of first y of last stripe
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, lastStripeY * gridW, lons, 0, lastStripeH * gridW);
            System.arraycopy(latFloats, lastStripeY * gridW, lats, 0, lastStripeH * gridW);
            for (int x = 0; x < gridW; x++) {
                int y1 = lastStripeY - _scanlineHeight; // coord of first y in next stripe
                int y2 = lastStripeY - 1;         // coord of last y in next stripe
                int index1 = y1 * gridW + x;
                int index2 = y2 * gridW + x;
                float deltaLat = (latFloats[index2] - latFloats[index1]) / (_scanlineHeight - 1);
                float deltaLon = (lonFloats[index2] - lonFloats[index1]) / (_scanlineHeight - 1);
                float refLat = latFloats[lastStripeY * gridW + x];
                float refLon = lonFloats[lastStripeY * gridW + x];

                for (int y = lastStripeH; y < _scanlineHeight; y++) {
                    lons[y * gridW + x] = refLon - (deltaLon * (y - lastStripeH + 1));
                    lats[y * gridW + x] = refLat - (deltaLat * (y - lastStripeH + 1));
                }
            }
            addStripeGeocode(lats, lons, lastStripeY, gridW, _scanlineHeight, osX, osY, ssX, ssY);
        }

        initSmallestAndLargestValidGeocodingIndices();
    }

    private void addStripeGeocode(float[] lats, float[] lons, int y, int stripeW, int stripeH,
                                  float offsetX, float offsetY, float subSamplingX, float subSamplingY) throws IOException {
        GeoCoding gc = createStripeGeocode(lats, lons, y, stripeW, stripeH, offsetX, offsetY, subSamplingX, subSamplingY);
        if (gc != null) {
            _gcList.add(gc);
            _centerLineList.add(createCenterPolyLine(gc, stripeW, stripeH));
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

        if (mustRecalculateTiePointGrids(subsetDef)) {
            try {
                recalculateTiePointGrids(srcScene, destScene, subsetDef, latGridName, lonGridName);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return createGeocoding(destScene, ((BowtieTiePointGeoCoding)srcScene.getGeoCoding()).getScanlineHeight());
    }

    private boolean recalculateTiePointGrids(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef, String latGridName, String lonGridName) throws IOException {
        // first step - remove location TP grids that have already been transferred. Their size is
        // calculated wrong in most cases
        final TiePointGrid falseTiePointGrid = destScene.getProduct().getTiePointGrid(latGridName);
        final float rightOffsetX = falseTiePointGrid.getOffsetX();
        final float falseOffsetY = falseTiePointGrid.getOffsetY();
        final float rightSubsamplingX = falseTiePointGrid.getSubSamplingX();
        final float rightSubsamplingY = falseTiePointGrid.getSubSamplingY();

        removeTiePointGrid(destScene, latGridName);
        removeTiePointGrid(destScene, lonGridName);

        final Product srcProduct = srcScene.getProduct();
        final int sceneRasterHeight = srcProduct.getSceneRasterHeight();
        final int tpRasterHeight = srcProduct.getTiePointGrid(lonGridName).getRasterHeight();

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
        final String latGridName = _latGrid.getName();
        final String lonGridName = _lonGrid.getName();
        final TiePointGrid latGrid = destScene.getProduct().getTiePointGrid(latGridName);
        final TiePointGrid lonGrid = destScene.getProduct().getTiePointGrid(lonGridName);
        if (latGrid != null && lonGrid != null) {
            destScene.setGeoCoding(new BowtieTiePointGeoCoding(latGrid, lonGrid, stripeHeight));
            return true;
        }
        return false;
    }

    static boolean mustRecalculateTiePointGrids(ProductSubsetDef subsetDef) {
        if(subsetDef == null) {
            return false;
        }
        return subsetDef.getRegion() != null;
    }

}
