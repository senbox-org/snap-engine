/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.OperatorException;
import eu.esa.snap.core.util.ProgressMonitorContext;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationModel;
import org.esa.snap.runtime.Config;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;

/**
 * DEM Handling
 */
public class DEMFactory {

    public static final String LAST_EXTERNAL_DEM_DIR_KEY = "snap.externalDEMDir";
    public static final String AUTODEM = " (Auto Download)";
    public static final String DELAUNAY_INTERPOLATION = "DELAUNAY_INTERPOLATION";
    private static final String ADD_ELEVATION_TILE_SIZE_KEY = "snap.dem.addElevationTileSize";
    private static final String MAX_DEGREES_PER_ELEVATION_TILE_KEY = "snap.dem.maxDegreesPerElevationTile";
    private static final String MAX_DEGREES_PER_ELEVATION_OVERVIEW_TILE_KEY = "snap.dem.maxDegreesPerElevationOverviewTile";
    private static final String ELEVATION_BAND_TILE_CACHE_SIZE_KEY = "snap.dem.elevationBandTileCacheSizeBytes";
    private static final int DEFAULT_ADD_ELEVATION_TILE_SIZE = 128;
    private static final double DEFAULT_MAX_DEGREES_PER_ELEVATION_TILE = 2.0;
    private static final double DEFAULT_MAX_DEGREES_PER_ELEVATION_OVERVIEW_TILE = 24.0;
    private static final long DEFAULT_ELEVATION_BAND_TILE_CACHE_SIZE_BYTES = 268435456L;
    private static final double COPERNICUS_OVERVIEW_OVERSAMPLING = 8.0;

    private static final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
    private static final String[] demNameList;
    private static final String[] demResamplingList = new String[ResamplingFactory.resamplingNames.length + 1];

    static {
        final List<String> sortedDEMNames = new ArrayList<>(descriptors.length);
        for (ElevationModelDescriptor descriptor : descriptors) {
            sortedDEMNames.add(DEMFactory.getDEMDisplayName(descriptor));
        }
        sortedDEMNames.sort((o1, o2) -> o1.compareTo(o2));
        demNameList = sortedDEMNames.toArray(new String[sortedDEMNames.size()]);

        int i = 0;
        for (String resampleName : ResamplingFactory.resamplingNames) {
            demResamplingList[i++] = resampleName;
        }
        demResamplingList[i] = DELAUNAY_INTERPOLATION;
    }

    public static String[] getDEMNameList() {
        return demNameList;
    }

    public static String[] getDEMResamplingMethods() {
        return demResamplingList;
    }

    public static String getProperDEMName(String name) {
        return name.replace(DEMFactory.AUTODEM, "");
    }

    public static ElevationModel createElevationModel(final String demName, String demResamplingMethod) throws IOException {

        final ElevationModelDescriptor demDescriptor = getDemDescriptor(demName);

        Resampling resampleMethod = null;
        if (!demResamplingMethod.equals(DELAUNAY_INTERPOLATION)) {              // resampling not actual used for Delaunay
            resampleMethod = ResamplingFactory.createResampling(demResamplingMethod);
            if(resampleMethod == null) {
                throw new OperatorException("Resampling method "+ demResamplingMethod + " is invalid");
            }
        }

        final ElevationModel dem = demDescriptor.createDem(resampleMethod);
        if (dem == null) {
            throw new IOException("The DEM '" + demName + "' has not been installed.");
        }
        return dem;
    }

    // is of no use
    public static void checkIfDEMInstalled(final String demName) throws IOException {

    }

    public static void validateDEM(final String demName, final Product srcProduct) throws IOException {
        // check if outside dem area
        if (demName.contains("SRTM")) {
            final GeoCoding geocoding = srcProduct.getSceneGeoCoding();
            final int w = srcProduct.getSceneRasterWidth();
            final int h = srcProduct.getSceneRasterHeight();
            final GeoPos geo1 = geocoding.getGeoPos(new PixelPos(0, 0), null);
            final GeoPos geo2 = geocoding.getGeoPos(new PixelPos(w-1, 0), null);
            final GeoPos geo3 = geocoding.getGeoPos(new PixelPos(w-1, h-1), null);
            final GeoPos geo4 = geocoding.getGeoPos(new PixelPos(0, h-1), null);

            if ((geo1.getLat() > 60 && geo2.getLat() > 60 && geo3.getLat() > 60 && geo4.getLat() > 60) ||
                    (geo1.getLat() < -60 && geo2.getLat() < -60 && geo3.getLat() < -60 && geo4.getLat() < -60)) {
                throw new IOException("Entire image is outside of SRTM valid area.\nPlease use another DEM.");
            }
        }
    }

    public static void fillDEM(final double[][] localDEM, final double alt) {
        for (double[] row : localDEM) {
            Arrays.fill(row, alt);
        }
    }

    public static Dimension getAddElevationTileSize(final Product product) {
        final int configuredSize = Math.max(1, Config.instance().preferences().getInt(ADD_ELEVATION_TILE_SIZE_KEY, DEFAULT_ADD_ELEVATION_TILE_SIZE));
        final double maxDegrees = Config.instance().preferences().getDouble(MAX_DEGREES_PER_ELEVATION_TILE_KEY, DEFAULT_MAX_DEGREES_PER_ELEVATION_TILE);
        return getElevationTileSize(product, configuredSize, maxDegrees);
    }

    public static Dimension getAddElevationOverviewTileSize(final Product product) {
        final int configuredSize = Math.max(1, Config.instance().preferences().getInt(ADD_ELEVATION_TILE_SIZE_KEY, DEFAULT_ADD_ELEVATION_TILE_SIZE));
        final double maxDegrees = Config.instance().preferences().getDouble(MAX_DEGREES_PER_ELEVATION_OVERVIEW_TILE_KEY, DEFAULT_MAX_DEGREES_PER_ELEVATION_OVERVIEW_TILE);
        return getElevationTileSize(product, configuredSize, maxDegrees);
    }

    public static long getElevationBandTileCacheSizeBytes() {
        return Config.instance().preferences().getLong(ELEVATION_BAND_TILE_CACHE_SIZE_KEY, DEFAULT_ELEVATION_BAND_TILE_CACHE_SIZE_BYTES);
    }

    private static Dimension getElevationTileSize(final Product product, final int configuredSize, final double maxDegrees) {
        int tileWidth = Math.min(configuredSize, product.getSceneRasterWidth());
        int tileHeight = Math.min(configuredSize, product.getSceneRasterHeight());

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding != null && maxDegrees > 0.0) {
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            final GeoPos upperLeft = geoCoding.getGeoPos(new PixelPos(0, 0), null);
            final GeoPos upperRight = geoCoding.getGeoPos(new PixelPos(width - 1, 0), null);
            final GeoPos lowerLeft = geoCoding.getGeoPos(new PixelPos(0, height - 1), null);

            final double lonDegreesPerPixel = normalizedLonDistance(upperLeft.lon, upperRight.lon) / Math.max(1, width - 1);
            final double latDegreesPerPixel = Math.abs(upperLeft.lat - lowerLeft.lat) / Math.max(1, height - 1);
            if (lonDegreesPerPixel > 0.0 && !Double.isNaN(lonDegreesPerPixel)) {
                tileWidth = Math.max(1, Math.min(tileWidth, (int) Math.floor(maxDegrees / lonDegreesPerPixel)));
            }
            if (latDegreesPerPixel > 0.0 && !Double.isNaN(latDegreesPerPixel)) {
                tileHeight = Math.max(1, Math.min(tileHeight, (int) Math.floor(maxDegrees / latDegreesPerPixel)));
            }
        }

        return new Dimension(tileWidth, tileHeight);
    }

    private static double normalizedLonDistance(final double lon1, final double lon2) {
        double distance = Math.abs(lon1 - lon2);
        if (distance > 180.0) {
            distance = 360.0 - distance;
        }
        return distance;
    }

    public static String getDEMDisplayName(ElevationModelDescriptor descriptor) {
        if (descriptor.canBeDownloaded())
            return descriptor.getName() + AUTODEM;
        return descriptor.getName();
    }

    public static boolean fillElevationTile(final ElevationModel dem, final double demNoDataValue,
                                            final TileGeoreferencing tileGeoRef, final Tile targetTile,
                                            final boolean nodataValueAtSea,
                                            final ProgressMonitor progressMonitor) throws Exception {
        final ProductData targetData = targetTile.getDataBuffer();
        final TileIndex targetIndex = new TileIndex(targetTile);
        return fillElevation(dem, demNoDataValue, tileGeoRef::getGeoPos, targetTile.getRectangle(), nodataValueAtSea,
                             progressMonitor, new ElevationWriter() {
                    @Override
                    public void beginRow(int y) {
                        targetIndex.calculateStride(y);
                    }

                    @Override
                    public void write(int x, int y, double value) {
                        setElemDouble(targetData, targetIndex.getIndex(x), value);
                    }
                });
    }

    public static boolean fillElevationData(final ElevationModel dem, final double demNoDataValue,
                                            final TileGeoreferencing tileGeoRef, final Rectangle targetRectangle,
                                            final ProductData targetData, final boolean nodataValueAtSea,
                                            final ProgressMonitor progressMonitor) throws Exception {
        return fillElevationData(dem, demNoDataValue, tileGeoRef, targetRectangle, targetRectangle, targetData, nodataValueAtSea, progressMonitor);
    }

    public static boolean fillElevationData(final ElevationModel dem, final double demNoDataValue,
                                            final TileGeoreferencing tileGeoRef, final Rectangle dataRectangle,
                                            final Rectangle computeRectangle,
                                            final ProductData targetData, final boolean nodataValueAtSea,
                                            final ProgressMonitor progressMonitor) throws Exception {
        return fillElevationData(dem, demNoDataValue, tileGeoRef::getGeoPos, dataRectangle, computeRectangle, targetData, nodataValueAtSea, progressMonitor);
    }

    public static boolean fillElevationData(final ElevationModel dem, final double demNoDataValue,
                                            final ElevationGeoPosProvider geoPosProvider,
                                            final Rectangle targetRectangle,
                                            final ProductData targetData, final boolean nodataValueAtSea,
                                            final ProgressMonitor progressMonitor) throws Exception {
        return fillElevationData(dem, demNoDataValue, geoPosProvider, targetRectangle, targetRectangle, targetData, nodataValueAtSea, progressMonitor);
    }

    public static boolean fillElevationData(final ElevationModel dem, final double demNoDataValue,
                                            final ElevationGeoPosProvider geoPosProvider,
                                            final Rectangle dataRectangle,
                                            final Rectangle computeRectangle,
                                            final ProductData targetData, final boolean nodataValueAtSea,
                                            final ProgressMonitor progressMonitor) throws Exception {
        ensureComputeRectangleWithinDataRectangle(dataRectangle, computeRectangle);
        return fillElevation(dem, demNoDataValue, geoPosProvider, computeRectangle, nodataValueAtSea,
                             progressMonitor, new ElevationWriter() {
                    private int rowOffset;

                    @Override
                    public void beginRow(int y) {
                        rowOffset = (y - dataRectangle.y) * dataRectangle.width;
                    }

                    @Override
                    public void write(int x, int y, double value) {
                        setElemDouble(targetData, rowOffset + x - dataRectangle.x, value);
                    }
                });
    }

    @FunctionalInterface
    public interface ElevationGeoPosProvider {
        void getGeoPos(int x, int y, GeoPos geoPos);
    }

    private static void ensureComputeRectangleWithinDataRectangle(final Rectangle dataRectangle,
                                                                  final Rectangle computeRectangle) {
        if (computeRectangle.x < dataRectangle.x ||
                computeRectangle.y < dataRectangle.y ||
                computeRectangle.x + computeRectangle.width > dataRectangle.x + dataRectangle.width ||
                computeRectangle.y + computeRectangle.height > dataRectangle.y + dataRectangle.height) {
            throw new IllegalArgumentException("computeRectangle must be contained in dataRectangle");
        }
    }

    private static boolean fillElevation(final ElevationModel dem, final double demNoDataValue,
                                         final ElevationGeoPosProvider geoPosProvider,
                                         final Rectangle targetRectangle,
                                         final boolean nodataValueAtSea, final ProgressMonitor progressMonitor,
                                         final ElevationWriter writer) throws Exception {
        if (dem instanceof CopernicusElevationModel) {
            final int preferredSourceSize = getPreferredCopernicusSourceSize(geoPosProvider, targetRectangle);
            try (AutoCloseable ignored = ((CopernicusElevationModel) dem).usePreferredSourceSize(preferredSourceSize)) {
                return fillElevationCore(dem, demNoDataValue, geoPosProvider, targetRectangle,
                                         nodataValueAtSea, progressMonitor, writer);
            }
        }
        return fillElevationCore(dem, demNoDataValue, geoPosProvider, targetRectangle,
                                 nodataValueAtSea, progressMonitor, writer);
    }

    private static boolean fillElevationCore(final ElevationModel dem, final double demNoDataValue,
                                             final ElevationGeoPosProvider geoPosProvider,
                                             final Rectangle targetRectangle,
                                             final boolean nodataValueAtSea, final ProgressMonitor progressMonitor,
                                             final ElevationWriter writer) throws Exception {
        final int maxY = targetRectangle.y + targetRectangle.height;
        final int maxX = targetRectangle.x + targetRectangle.width;
        final GeoPos geoPos = new GeoPos();
        final double[][] egmWorkspace = new double[4][4];

        try (ProgressMonitorContext.Scope ignored = ProgressMonitorContext.use(progressMonitor)) {
            boolean valid = false;
            for (int y = targetRectangle.y; y < maxY; y++) {
                ProgressMonitorContext.checkCanceled(progressMonitor);
                writer.beginRow(y);
                for (int x = targetRectangle.x; x < maxX; x++) {
                    if (((x - targetRectangle.x) & 63) == 0) {
                        ProgressMonitorContext.checkCanceled(progressMonitor);
                    }
                    geoPosProvider.getGeoPos(x, y, geoPos);
                    Double alt = dem.getElevation(geoPos);
                    if (alt.equals(demNoDataValue) && !nodataValueAtSea) {
                        alt = (double) EarthGravitationalModel96.instance().getEGM(geoPos.lat, geoPos.lon, egmWorkspace);
                    }
                    if (!valid && !alt.equals(demNoDataValue)) {
                        valid = true;
                    }
                    writer.write(x, y, alt);
                }
            }
            return valid;
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            fillElevationNoData(targetRectangle, writer, demNoDataValue, progressMonitor);
            return false;
        }
    }

    private static int getPreferredCopernicusSourceSize(final ElevationGeoPosProvider geoPosProvider,
                                                        final Rectangle targetRectangle) {
        final double lonPixelsPerDegree = getLonPixelsPerDegree(geoPosProvider, targetRectangle);
        final double latPixelsPerDegree = getLatPixelsPerDegree(geoPosProvider, targetRectangle);
        final double pixelsPerDegree = maxPositive(lonPixelsPerDegree, latPixelsPerDegree);
        if (!isPositiveFinite(pixelsPerDegree)) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.ceil(pixelsPerDegree * COPERNICUS_OVERVIEW_OVERSAMPLING - 1.0e-9));
    }

    private static double getLonPixelsPerDegree(final ElevationGeoPosProvider geoPosProvider,
                                                final Rectangle targetRectangle) {
        if (targetRectangle.width < 2) {
            return Double.NaN;
        }
        final GeoPos left = new GeoPos();
        final GeoPos right = new GeoPos();
        final int y = targetRectangle.y + targetRectangle.height / 2;
        geoPosProvider.getGeoPos(targetRectangle.x, y, left);
        geoPosProvider.getGeoPos(targetRectangle.x + targetRectangle.width - 1, y, right);
        final double lonDistance = normalizedLonDistance(left.lon, right.lon);
        return isPositiveFinite(lonDistance) ? (targetRectangle.width - 1) / lonDistance : Double.NaN;
    }

    private static double getLatPixelsPerDegree(final ElevationGeoPosProvider geoPosProvider,
                                                final Rectangle targetRectangle) {
        if (targetRectangle.height < 2) {
            return Double.NaN;
        }
        final GeoPos upper = new GeoPos();
        final GeoPos lower = new GeoPos();
        final int x = targetRectangle.x + targetRectangle.width / 2;
        geoPosProvider.getGeoPos(x, targetRectangle.y, upper);
        geoPosProvider.getGeoPos(x, targetRectangle.y + targetRectangle.height - 1, lower);
        final double latDistance = Math.abs(upper.lat - lower.lat);
        return isPositiveFinite(latDistance) ? (targetRectangle.height - 1) / latDistance : Double.NaN;
    }

    private static boolean isPositiveFinite(final double value) {
        return value > 0.0 && !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static double maxPositive(final double value1, final double value2) {
        if (!isPositiveFinite(value1)) {
            return value2;
        }
        if (!isPositiveFinite(value2)) {
            return value1;
        }
        return Math.max(value1, value2);
    }

    private static void fillElevationNoData(final Rectangle targetRectangle, final ElevationWriter writer,
                                            final double noDataValue, final ProgressMonitor progressMonitor) {
        final int maxY = targetRectangle.y + targetRectangle.height;
        final int maxX = targetRectangle.x + targetRectangle.width;
        for (int y = targetRectangle.y; y < maxY; y++) {
            ProgressMonitorContext.checkCanceled(progressMonitor);
            writer.beginRow(y);
            for (int x = targetRectangle.x; x < maxX; x++) {
                writer.write(x, y, noDataValue);
            }
        }
    }

    private static void setElemDouble(final ProductData targetData, final int index, final double value) {
        final Object elements = targetData.getElems();
        if (elements instanceof float[]) {
            ((float[]) elements)[index] = (float) value;
        } else if (elements instanceof double[]) {
            ((double[]) elements)[index] = value;
        } else {
            targetData.setElemDoubleAt(index, value);
        }
    }

    private interface ElevationWriter {
        void beginRow(int y);

        void write(int x, int y, double value);
    }

    /**
     * Read DEM for current tile.
     *
     * @param dem            the model
     * @param demNoDataValue the no data value of the dem
     * @param tileGeoRef     the georeferencing of the target product
     * @param x0             The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0             The y coordinate of the pixel at the upper left corner of current tile.
     * @param tileHeight     The tile height.
     * @param tileWidth      The tile width.
     * @param localDEM       The DEM for the tile.
     * @return true if all dem values are valid
     * @throws Exception from DEM
     */
    public static boolean getLocalDEM(final ElevationModel dem, final double demNoDataValue,
                                      final String demResamplingMethod,
                                      final TileGeoreferencing tileGeoRef,
                                      final int x0, final int y0,
                                      final int tileWidth, final int tileHeight,
                                      final Product sourceProduct,
                                      final boolean nodataValueAtSea,
                                      final double[][] localDEM) throws Exception {
        return getLocalDEM(dem, demNoDataValue, demResamplingMethod, tileGeoRef, x0, y0, tileWidth, tileHeight,
                           sourceProduct, nodataValueAtSea, localDEM,
                           ProgressMonitorContext.getCurrentProgressMonitor());
    }

    public static boolean getLocalDEM(final ElevationModel dem, final double demNoDataValue,
                                      final String demResamplingMethod,
                                      final TileGeoreferencing tileGeoRef,
                                      final int x0, final int y0,
                                      final int tileWidth, final int tileHeight,
                                      final Product sourceProduct,
                                      final boolean nodataValueAtSea,
                                      final double[][] localDEM,
                                      ProgressMonitor progressMonitor) throws Exception {

        if (demResamplingMethod != null && demResamplingMethod.equals(DELAUNAY_INTERPOLATION)) {
            //return getLocalDEMUsingDelaunayInterpolation(
            //        dem, demNoDataValue, tileGeoRef, x0, y0, tileWidth, tileHeight, sourceProduct, localDEM);
        }

        // Note: the localDEM covers current tile with 1 extra row above, 1 extra row below, 1 extra column to
        //       the left and 1 extra column to the right of the tile.

        final int maxY = y0 + tileHeight + 1;
        final int maxX = x0 + tileWidth + 1;
        final GeoPos geoPos = new GeoPos();

        try (ProgressMonitorContext.Scope ignored = ProgressMonitorContext.use(progressMonitor)) {
            Double alt;
            boolean valid = false;
            final double[][] v = new double[4][4];
            for (int y = y0 - 1; y < maxY; y++) {
                ProgressMonitorContext.checkCanceled(progressMonitor);
                final int yy = y - y0 + 1;
                for (int x = x0 - 1; x < maxX; x++) {
                    if (((x - x0 + 1) & 63) == 0) {
                        ProgressMonitorContext.checkCanceled(progressMonitor);
                    }
                    tileGeoRef.getGeoPos(x, y, geoPos);
                /*if(!geoPos.isValid()) {
                    localDEM[yy][x - x0 + 1] = demNoDataValue;
                    continue;
                }
                if (geoPos.lon > 180) {
                    geoPos.lon -= 360;
                } else if (geoPos.lon < -180) {
                    geoPos.lon += 360;
                }    */

                    alt = dem.getElevation(geoPos);

                    if (alt.equals(demNoDataValue) && !nodataValueAtSea) {
                        alt = (double)EarthGravitationalModel96.instance().getEGM(geoPos.lat, geoPos.lon, v);
                    }

                    if (!valid && !alt.equals(demNoDataValue)) {
                        valid = true;
                    }

                    localDEM[yy][x - x0 + 1] = alt;
                }
            }
            return valid;
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

  /*  public synchronized static boolean getLocalDEMUsingDelaunayInterpolation(
            final ElevationModel dem, final double demNoDataValue, final TileGeoreferencing tileGeoRef, final int x0,
            final int y0, final int tileWidth, final int tileHeight, final Product sourceProduct,
            final double[][] localDEM) throws Exception {

        // Note: the localDEM covers current tile with 1 extra row above, 1 extra row below, 1 extra column to
        //       the left and 1 extra column to the right of the tile.

        final int maxY = y0 + tileHeight + 1;
        final int maxX = x0 + tileWidth + 1;
        final PixelPos pixelPos = new PixelPos();
        final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(y0 - 1, y0 + tileHeight, x0 - 1, x0 + tileWidth);

        final GeoPos tgtUL = new GeoPos();
        final GeoPos tgtUR = new GeoPos();
        final GeoPos tgtLL = new GeoPos();
        final GeoPos tgtLR = new GeoPos();

        tileGeoRef.getGeoPos(x0 - 1, y0 - 1, tgtUL);
        tileGeoRef.getGeoPos(x0 + tileWidth, y0 - 1, tgtUR);
        tileGeoRef.getGeoPos(x0 - 1, y0 + tileHeight, tgtLL);
        tileGeoRef.getGeoPos(x0 + tileWidth, y0 + tileHeight, tgtLR);

        final double latMin = Math.min(Math.min(Math.min(tgtUL.lat, tgtUR.lat), tgtLL.lat), tgtLR.lat);
        final double latMax = Math.max(Math.max(Math.max(tgtUL.lat, tgtUR.lat), tgtLL.lat), tgtLR.lat);
        final double lonMin = Math.min(Math.min(Math.min(tgtUL.lon, tgtUR.lon), tgtLL.lon), tgtLR.lon);
        final double lonMax = Math.max(Math.max(Math.max(tgtUL.lon, tgtUR.lon), tgtLL.lon), tgtLR.lon);

        final GeoPos upperLeftCorner = new GeoPos(latMax, lonMin);
        final GeoPos lowerRightCorner = new GeoPos(latMin, lonMax);

        GeoPos[] geoCorners = {upperLeftCorner, lowerRightCorner};
        final GeoPos geoExtent = new GeoPos(0.25 * (latMax - latMin), 0.25 * (lonMax - lonMin));

        // inline of extendCorners call: avoiding ambiguous dependencies GeoPos vs GeoPoint //
        // geoCorners = extendCorners(geoExtent, geoCorners);

        geoCorners[0].lat = geoCorners[0].lat + geoExtent.lat;
        geoCorners[0].lon = geoCorners[0].lon - geoExtent.lon;

        geoCorners[1].lat = geoCorners[1].lat - geoExtent.lat;
        geoCorners[1].lon = geoCorners[1].lon + geoExtent.lon;

        if (geoCorners[0].lon > 180) {
            geoCorners[0].lon -= 360;
        }
        if (geoCorners[1].lon > 180) {
            geoCorners[1].lon -= 360;
        }

        boolean crossMeridian = false;
        if (geoCorners[0].lon > 0 && geoCorners[1].lon < 0) {
            crossMeridian = true;
        }

        PixelPos upperLeftCornerPos = dem.getIndex(geoCorners[0]);
        PixelPos lowerRightCornerPos = dem.getIndex(geoCorners[1]);

        upperLeftCornerPos = new PixelPos(Math.floor(upperLeftCornerPos.x), Math.floor(upperLeftCornerPos.y));
        lowerRightCornerPos = new PixelPos(Math.ceil(lowerRightCornerPos.x), Math.ceil(lowerRightCornerPos.y));

        double[][] x_in = null;
        double[][] y_in = null;
        double[][] z_in = null;
        final int nLatPixels = (int) Math.abs(lowerRightCornerPos.y - upperLeftCornerPos.y);
        final PixelPos pos = new PixelPos();
        if (!crossMeridian) {

            final int nLonPixels = (int) Math.abs(lowerRightCornerPos.x - upperLeftCornerPos.x);
            x_in = new double[nLatPixels][nLonPixels];
            y_in = new double[nLatPixels][nLonPixels];
            z_in = new double[nLatPixels][nLonPixels];
            final int startX = (int) upperLeftCornerPos.x;
            final int endX = startX + nLonPixels;
            final int startY = (int) upperLeftCornerPos.y;
            final int endY = startY + nLatPixels;
            for (int y = startY, i = 0; y < endY; y++, i++) {
                for (int x = startX, j = 0; x < endX; x++, j++) {
                    pos.setLocation(x + 0.5f, y + 0.5f);
                    tileGeoRef.getPixelPos(dem.getGeoPos(pos), pixelPos);
                    x_in[i][j] = pixelPos.x; // x coordinate in SAR image tile of given point pos
                    y_in[i][j] = pixelPos.y; // y coordinate in SAR image tile of given point pos
                    try {
                        double elev = dem.getSample(x, y);
                        if (Double.isNaN(elev))
                            elev = demNoDataValue;
                        z_in[i][j] = elev;
                    } catch (Exception e) {
                        z_in[i][j] = demNoDataValue;
                    }
                }
            }

        } else {

            final PixelPos endPixelPos = dem.getIndex(new GeoPos(geoCorners[0].lat, 180));
            final int nLonPixels = (int) (Math.abs(upperLeftCornerPos.x - endPixelPos.x) + lowerRightCornerPos.x);
            x_in = new double[nLatPixels][nLonPixels];
            y_in = new double[nLatPixels][nLonPixels];
            z_in = new double[nLatPixels][nLonPixels];
            final int startX = (int) upperLeftCornerPos.x;
            final int endX = (int) endPixelPos.x;
            final int startY = (int) upperLeftCornerPos.y;
            final int endY = startY + nLatPixels;
            for (int y = startY, i = 0; y < endY; y++, i++) {
                for (int x = startX, j = 0; x < endX; x++, j++) {
                    pos.setLocation(x + 0.5f, y + 0.5f);
                    tileGeoRef.getPixelPos(dem.getGeoPos(pos), pixelPos);
                    x_in[i][j] = pixelPos.x; // x coordinate in SAR image tile of given point pos
                    y_in[i][j] = pixelPos.y; // y coordinate in SAR image tile of given point pos
                    try {
                        double elev = dem.getSample(x, y);
                        if (Double.isNaN(elev))
                            elev = demNoDataValue;
                        z_in[i][j] = elev;
                    } catch (Exception e) {
                        z_in[i][j] = demNoDataValue;
                    }
                }
            }

            for (int y = startY, i = 0; y < endY; y++, i++) {
                for (int x = 0, j = endX - startX; x < (int) lowerRightCornerPos.x; x++, j++) {
                    pos.setLocation(x + 0.5f, y + 0.5f);
                    tileGeoRef.getPixelPos(dem.getGeoPos(pos), pixelPos);
                    x_in[i][j] = pixelPos.x; // x coordinate in SAR image tile of given point pos
                    y_in[i][j] = pixelPos.y; // y coordinate in SAR image tile of given point pos
                    try {
                        double elev = dem.getSample(x, y);
                        if (Double.isNaN(elev))
                            elev = demNoDataValue;
                        z_in[i][j] = elev;
                    } catch (Exception e) {
                        z_in[i][j] = demNoDataValue;
                    }
                }
            }
        }

        // compute range-azimuth spacing ratio
        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        SLCImage meta = new SLCImage(absRoot);
        Orbit orbit = new Orbit(absRoot, 3);
        final long firstLine = tileWindow.linelo + 1;
        final long lastLine = tileWindow.linehi - 1;
        final long firstPixel = tileWindow.pixlo + 1;
        final long lastPixel = tileWindow.pixhi - 1;
        final Point p1 = orbit.lp2xyz(firstLine, firstPixel, meta);
        final Point p2 = orbit.lp2xyz(firstLine, lastPixel, meta);
        final Point p3 = orbit.lp2xyz(lastLine, firstPixel, meta);
        final Point p4 = orbit.lp2xyz(lastLine, lastPixel, meta);
        final double rangeSpacing = ((p1.min(p2)).norm() + (p3.min(p4)).norm()) / 2 / (lastPixel - firstPixel);
        final double aziSpacing = ((p1.min(p3)).norm() + (p2.min(p4)).norm()) / 2 / (lastLine - firstLine);
        final double rngAzRatio = rangeSpacing / aziSpacing;

        // y - lines, x - pixels, z - heights
        final double[][] elevation = org.jlinda.core.utils.TriangleUtils.gridDataLinear(
                y_in, x_in, z_in, tileWindow, rngAzRatio, 1, 1, demNoDataValue, 0);

        double alt;
        boolean valid = false;
        for (int y = y0 - 1; y < maxY; y++) {
            final int yy = y - y0 + 1;
            for (int x = x0 - 1; x < maxX; x++) {
                alt = elevation[yy][x - x0 + 1];
                if (!valid && alt != demNoDataValue)
                    valid = true;
                localDEM[yy][x - x0 + 1] = alt;
            }
        }
        return valid;
    }*/

    private static ElevationModelDescriptor getDemDescriptor(String demName) throws IOException {
        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
        if (demDescriptor == null) {
            throw new IOException("The DEM '" + demName + "' is not supported.");
        }
        return demDescriptor;
    }

    private static GeoPos[] extendCorners(final GeoPos extraGeo, final GeoPos[] inGeo) {

        if (inGeo.length != 2) {
            throw new IllegalArgumentException("Input GeoPos[] array has to have exactly 2 elements");
        }

        GeoPos[] outGeo = new GeoPos[inGeo.length];

        outGeo[0] = new GeoPos();
        outGeo[0].lat = inGeo[0].lat + extraGeo.lat;
        outGeo[0].lon = inGeo[0].lon - extraGeo.lon;

        outGeo[1] = new GeoPos();
        outGeo[1].lat = inGeo[1].lat - extraGeo.lat;
        outGeo[1].lon = inGeo[1].lon + extraGeo.lon;

        return outGeo;
    }

}
