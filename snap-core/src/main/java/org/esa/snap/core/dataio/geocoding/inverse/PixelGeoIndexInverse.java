/*
 *
 * Copyright (C) 2020 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.util.InterpolationContext;
import org.esa.snap.core.dataio.geocoding.util.InterpolatorFactory;
import org.esa.snap.core.dataio.geocoding.util.XYInterpolator;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.PreferencesPropertyMap;
import org.esa.snap.core.util.math.RsMathUtils;
import org.esa.snap.core.util.math.SphericalDistance;
import org.esa.snap.runtime.Config;

import java.util.Properties;
import java.util.TreeMap;

public class PixelGeoIndexInverse implements InverseCoding {

    public static final String KEY = "INV_PIXEL_GEO_INDEX";
    public static final String KEY_INTERPOLATING = KEY + KEY_SUFFIX_INTERPOLATING;

    private final boolean fractionalAccuracy;
    private final XYInterpolator interpolator;

    private TreeMap<Long, RasterRegion> regionIndex;
    private GeoRaster geoRaster;
    private double multiplicator;
    private double[] longitudes;
    private double[] latitudes;
    private double epsilon;

    PixelGeoIndexInverse() {
        this(false);
    }

    PixelGeoIndexInverse(boolean fractionalAccuracy) {
        this(fractionalAccuracy, new PreferencesPropertyMap(Config.instance("snap").preferences()).getProperties());
    }

    PixelGeoIndexInverse(boolean fractionalAccuracy, Properties properties) {
        this(fractionalAccuracy, InterpolatorFactory.create(properties));
    }

    PixelGeoIndexInverse(boolean fractionalAccuracy, XYInterpolator interpolator) {
        this.fractionalAccuracy = fractionalAccuracy;
        this.interpolator = interpolator;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }

        pixelPos.setInvalid();
        if (!geoPos.isValid()) {
            return pixelPos;
        }

        final long index = toIndex(geoPos.lon, geoPos.lat);
        final RasterRegion rasterRegion = regionIndex.get(index);
        if (rasterRegion == null) {
            return pixelPos;
        }

        if (rasterRegion.isPoint()) {
            getSingleResultPixel(pixelPos, rasterRegion);
        } else {
            getMinimumDistancePixel(pixelPos, geoPos, rasterRegion);
        }

        final SphericalDistance sphericalDistance = new SphericalDistance(geoPos.lon, geoPos.lat);
        final int location = (int) (pixelPos.y) * geoRaster.getSceneWidth() + (int) (pixelPos.x);
        final double distance = sphericalDistance.distance(longitudes[location], latitudes[location]) * RsMathUtils.MEAN_EARTH_RADIUS;
        if (distance < epsilon) {
            if (fractionalAccuracy) {
                final InterpolationContext context = InterpolationContext.extract((int) pixelPos.x, (int) pixelPos.y, longitudes, latitudes, geoRaster.getSceneWidth(), geoRaster.getSceneHeight());
                pixelPos = interpolator.interpolate(geoPos, pixelPos, context);
            }

            pixelPos.x = pixelPos.x + geoRaster.getOffsetX();
            pixelPos.y = pixelPos.y + geoRaster.getOffsetY();
        } else {
            pixelPos.setInvalid();
        }

        return pixelPos;
    }

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        this.geoRaster = geoRaster;
        regionIndex = new TreeMap<>();

        multiplicator = getMultiplicator(geoRaster.getRasterResolutionInKm());

        longitudes = geoRaster.getLongitudes();
        latitudes = geoRaster.getLatitudes();

        final int width = geoRaster.getSceneWidth();
        for (int y = 0; y < geoRaster.getSceneHeight(); y++) {
            final int y_offset = y * width;
            for (int x = 0; x < width; x++) {
                final double lon = longitudes[y_offset + x];
                final double lat = latitudes[y_offset + x];
                if (Double.isNaN(lon) || Double.isNaN(lat)) {
                    continue;
                }

                final long index = toIndex(lon, lat);
                final RasterRegion rasterRegion = regionIndex.get(index);
                if (rasterRegion == null) {
                    regionIndex.put(index, new RasterRegion(x, y));
                } else {
                    rasterRegion.extend(x, y);
                }
            }
        }

        epsilon = geoRaster.getRasterResolutionInKm() * 1000 / Math.sqrt(2.0);
    }

    @Override
    public String getKey() {
        if (fractionalAccuracy) {
            return KEY_INTERPOLATING;
        } else {
            return KEY;
        }
    }

    @Override
    public void dispose() {
        if (regionIndex != null) {
            regionIndex.clear();
            regionIndex = null;
        }
        geoRaster = null;
    }

    @Override
    public InverseCoding clone() {
        final PixelGeoIndexInverse clone = new PixelGeoIndexInverse(fractionalAccuracy, interpolator);

        clone.regionIndex = (TreeMap<Long, RasterRegion>) regionIndex.clone();
        clone.multiplicator = multiplicator;
        clone.geoRaster = geoRaster;
        clone.longitudes = longitudes;
        clone.latitudes = latitudes;
        clone.epsilon = epsilon;

        return clone;
    }

    long toIndex(double lon, double lat) {
        int lon_idx = (int) Math.floor((lon + 180.0) * multiplicator);
        int lat_idx = (int) Math.floor((lat + 90.0) * multiplicator);

        return 100000L * lon_idx + lat_idx;
    }

    static double getMultiplicator(double resolutionInKm) {
        if (resolutionInKm > 33.3) {
            return 1.0;
        } else if (resolutionInKm <= 0.333) {
            return 100.0;
        }

        return 100.0 / (3 * resolutionInKm);
    }

    private void getSingleResultPixel(PixelPos pixelPos, RasterRegion rasterRegion) {
        pixelPos.x = rasterRegion.min_x;
        pixelPos.y = rasterRegion.min_y;
    }

    private void getMinimumDistancePixel(PixelPos pixelPos, GeoPos geoPos, RasterRegion rasterRegion) {
        final Result result = new Result();

        for (int y = rasterRegion.min_y; y <= rasterRegion.max_y; y++) {
            final int offset = y * geoRaster.getSceneWidth();
            for (int x = rasterRegion.min_x; x <= rasterRegion.max_x; x++) {
                final double lon = longitudes[offset + x];
                final double lat = latitudes[offset + x];

                final double dLon = lon - geoPos.lon;
                final double dLat = lat - geoPos.lat;
                final double squareDistance = dLon * dLon + dLat * dLat;

                result.update(x, y, squareDistance);
            }
        }

        pixelPos.x = result.x;
        pixelPos.y = result.y;
    }

    static class RasterRegion {
        int min_x;
        int max_x;
        int min_y;
        int max_y;

        RasterRegion(int x, int y) {
            min_x = x;
            max_x = x;
            min_y = y;
            max_y = y;
        }

        void extend(int x, int y) {
            if (x < min_x) {
                min_x = x;
            }
            if (x > max_x) {
                max_x = x;
            }
            if (y < min_y) {
                min_y = y;
            }
            if (y > max_y) {
                max_y = y;
            }
        }

        boolean isPoint() {
            return min_x == max_x && min_y == max_y;
        }
    }

    public static class Plugin implements InversePlugin {

        private final boolean fractionalAccuracy;

        public Plugin(boolean fractionalAccuracy) {
            this.fractionalAccuracy = fractionalAccuracy;
        }

        @Override
        public InverseCoding create() {
            return new PixelGeoIndexInverse(fractionalAccuracy);
        }
    }
}
