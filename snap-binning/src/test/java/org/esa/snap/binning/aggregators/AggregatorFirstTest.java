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

package org.esa.snap.binning.aggregators;

import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.MyVariableContext;
import org.esa.snap.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Float.NaN;
import static org.esa.snap.binning.aggregators.AggregatorTestUtils.*;
import static org.junit.Assert.assertEquals;

public class AggregatorFirstTest {

    BinContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
    }

    @Test
    public void testMetadata() {
        AggregatorFirst agg = new AggregatorFirst(new MyVariableContext("a"), "a", "a");

        assertEquals("FIRST", agg.getName());

        assertEquals(1, agg.getSpatialFeatureNames().length);
        assertEquals("a", agg.getSpatialFeatureNames()[0]);

        assertEquals(1, agg.getTemporalFeatureNames().length);
        assertEquals("a", agg.getTemporalFeatureNames()[0]);

        assertEquals(1, agg.getOutputFeatureNames().length);
        assertEquals("a", agg.getOutputFeatureNames()[0]);
    }

    @Test
    public void testAggregatorFirst() {
        AggregatorFirst agg = new AggregatorFirst(new MyVariableContext("a"), "a", "a");

        VectorImpl svec = vec(NaN);
        VectorImpl tvec = vec(NaN);
        VectorImpl out = vec(NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(Float.NaN, svec.get(0), 0.0f);

        agg.aggregateSpatial(ctx, obsNT(7.3f), svec);
        agg.aggregateSpatial(ctx, obsNT(5.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(-0.1f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.0f), svec);
        assertEquals(7.3f, svec.get(0), 1e-5f);

        agg.completeSpatial(ctx, 4, svec);
        assertEquals(7.3f, svec.get(0), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(Float.NaN, tvec.get(0), 0.0f);

        agg.aggregateTemporal(ctx, vec(0.9f), 3, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f), 5, tvec);
        agg.aggregateTemporal(ctx, vec(0.6f), 9, tvec);
        agg.aggregateTemporal(ctx, vec(0.2f), 2, tvec);
        assertEquals(0.9f, tvec.get(0), 1e-5f);

        agg.computeOutput(tvec, out);
        assertEquals(0.9f, tvec.get(0), 1e-5f);
    }

}
