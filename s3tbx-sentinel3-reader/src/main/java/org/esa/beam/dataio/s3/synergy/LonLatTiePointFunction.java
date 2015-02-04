package org.esa.beam.dataio.s3.synergy;
/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.dataio.s3.LonLatFunction;
import org.esa.beam.util.math.DistanceMeasure;

import java.util.Arrays;
import java.util.Comparator;

final class LonLatTiePointFunction implements LonLatFunction {

    private final double[][] data;
    private final Comparator<double[]> comparator = new Comparator<double[]>() {
        @Override
        public int compare(double[] o1, double[] o2) {
            return Double.compare(o1[1], o2[1]);
        }
    };
    private final int pixelCount;

    LonLatTiePointFunction(double[] lonData, double[] latData, double[] functionData, int pixelCount) {
        this.pixelCount = pixelCount;
        data = new double[lonData.length][3];
        for (int i = 0; i < data.length; i++) {
            data[i][0] = lonData[i];
            data[i][1] = latData[i];
            data[i][2] = functionData[i];
        }
        Arrays.sort(data, comparator);
    }

    @Override
    public double getValue(double lon, double lat) {
        // TODO - this algorithm does not work in general

        final int index = Math.abs(Arrays.binarySearch(data, new double[]{0.0, lat, 0.0}, comparator));
        final DistanceMeasure distanceCalculator = new DC(lon, lat);
        final int minIndex = Math.max(0, index - pixelCount - 2);
        final int maxIndex = Math.min(data.length, index + pixelCount + 2);

        double value = Double.NaN;
        double minDistance = Double.MAX_VALUE;

        for (int k = minIndex; k < maxIndex; k++) {
            final double[] point = data[k];
            final double distance = distanceCalculator.distance(point[0], point[1]);

            if (distance < minDistance) {
                minDistance = distance;
                value = point[2];
            }
        }

        return value;
    }

    private static final class DC implements DistanceMeasure {

        private final double lon;
        private final double si;
        private final double co;

        private DC(double lon, double lat) {
            this.lon = lon;
            this.si = Math.sin(Math.toRadians(lat));
            this.co = Math.cos(Math.toRadians(lat));
        }

        @Override
        public double distance(double lon, double lat) {
            final double phi = Math.toRadians(lat);
            return -(si * Math.sin(phi) + co * Math.cos(phi) * Math.cos(Math.toRadians(lon - this.lon)));
        }
    }
}
