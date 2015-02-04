package org.esa.beam.dataio.s3.synergy;/*
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
import org.junit.Ignore;
import org.junit.Test;
import ucar.nc2.Variable;

import static org.junit.Assert.assertEquals;

public class LonLatTiePointFunctionTest {

    @Test
    //takes a lot of time
    @Ignore
    public void testApproximation() throws Exception {

        NcFile ncFile1 = null;
        NcFile ncFile2 = null;
        try {
            ncFile1 = NcFile.openResource("tiepoints_olci.nc");

            final double[] lonData = ncFile1.read("OLC_TP_lon");
            final double[] latData = ncFile1.read("OLC_TP_lat");
            for (final Variable variable : ncFile1.getVariables(".*")) {
                final double[] variableData = ncFile1.read(variable.getName());

                testApproximationForVariable(lonData, latData, variableData);
            }

            ncFile2 = NcFile.openResource("tiepoints_meteo.nc");
            for (final Variable variable : ncFile2.getVariables(".*")) {
                final double[] variableData = ncFile2.read(variable.getName());

                testApproximationForVariable(lonData, latData, variableData);
            }
        } finally {
            if (ncFile1 != null) {
                ncFile1.close();
            }
            if (ncFile2 != null) {
                ncFile2.close();
            }
        }
    }

    private void testApproximationForVariable(double[] lonData, double[] latData, double[] variableData) {
        final LonLatFunction function = new LonLatTiePointFunction(lonData,
                                                                   latData,
                                                                   variableData, lonData.length);

        for (int i = 0; i < variableData.length; i++) {
            final double lon = lonData[i];
            final double lat = latData[i];
            final double var = variableData[i];
            final double actual = function.getValue(lon, lat);

            assertEquals(var, actual, 0.0);
        }
    }

}
