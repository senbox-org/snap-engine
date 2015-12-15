/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.idepix.core.seaice;

/**
 * Simple data structure for storing sea ice information.
 *
 * @author Thomas Storm
 */
public class SeaIceClassification {

    /**
     * The mean sea ice value.
     */
    public final double mean;

    /**
     * The minimum sea ice value.
     */
    public final double min;

    /**
     * The maximum sea ice value.
     */
    public final double max;

    /**
     * The standard deviation from the mean value.
     */
    public final double standardDeviation;

    private SeaIceClassification(double mean, double min, double max, double standardDeviation) {
        this.mean = mean;
        this.min = min;
        this.max = max;
        this.standardDeviation = standardDeviation;
    }

    static SeaIceClassification create(double mean, double min, double max, double standardDeviation) {
        return new SeaIceClassification(mean, min, max, standardDeviation);
    }
}
