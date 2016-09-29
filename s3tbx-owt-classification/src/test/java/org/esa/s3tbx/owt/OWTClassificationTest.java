/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.owt;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class OWTClassificationTest {

    private Auxdata auxdata;

    @Before
    public void setup() throws Exception {
        auxdata = new CoastalAuxdataFactory("/auxdata/coastal/owt16_meris_stats_101119_5band.hdf").createAuxdata();
    }

    @Test
    public void testFuzzyResults() throws OWTException {
        final double[] reflectances = {0.0307, 0.0414, 0.0500, 0.0507, 0.0454};
        final OWTClassification owtClassification = new OWTClassification(auxdata.getSpectralMeans(),
                                                                          auxdata.getInvertedCovarianceMatrices());
        final double[] classMembershipProbability = owtClassification.computeClassMemberships(reflectances);

        // these values are validated by algorithm provider Timothy Moore
        final double[] expectedValues = new double[]{
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.024374, 0.083183, 0.199592
        };
        assertEquals(expectedValues.length, classMembershipProbability.length);
        final double normalizationFactor = 1.0 / (0.024374 + 0.083183 + 0.19959);
        for (int i = 0; i < classMembershipProbability.length; i++) {
            assertEquals(expectedValues[i], classMembershipProbability[i], 1.0e-5);
            final double normalizedMembership = OWTClassificationOp.normalizeClassMemberships(classMembershipProbability)[i];
            assertEquals(normalizationFactor * expectedValues[i], normalizedMembership, 1.0e-5);
        }

    }
}
