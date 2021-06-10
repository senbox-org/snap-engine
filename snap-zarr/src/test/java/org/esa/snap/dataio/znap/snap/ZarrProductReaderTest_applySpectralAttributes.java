/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.znap.snap;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


public class ZarrProductReaderTest_applySpectralAttributes {

    private Band sourceBand;
    private Map<String, Object> attributes;
    private Band targetBand;
    private ZarrProductReader reader;

    @Before
    public void setUp() throws Exception {
        sourceBand = new Band("source", ProductData.TYPE_FLOAT32, 10, 10);
        targetBand = new Band("target", ProductData.TYPE_UINT8, 20, 20);
        attributes = new HashMap<>();
        reader = new ZarrProductReader(new ZarrProductReaderPlugIn());
    }

    @Test
    public void applySpectralBandwidth() {
        //preparation
        sourceBand.setSpectralBandwidth(123.4f);
        ZarrProductWriter.collectBandAttributes(sourceBand, attributes);
        assertThat(attributes.size(), is(2));
        //execution
        reader.applyBandAttributes(attributes, targetBand);
        //verification
        assertThat(targetBand.getSpectralBandwidth(), is(123.4f));
    }

    @Test
    public void applySpectralWavelength() {
        //preparation
        sourceBand.setSpectralWavelength(234.5f);
        ZarrProductWriter.collectBandAttributes(sourceBand, attributes);
        assertThat(attributes.size(), is(2));
        //execution
        reader.applyBandAttributes(attributes, targetBand);
        //verification
        assertThat(targetBand.getSpectralWavelength(), is(234.5f));
    }

    @Test
    public void applySolarFlux() {
        //preparation
        sourceBand.setSolarFlux(24.3f);
        ZarrProductWriter.collectBandAttributes(sourceBand, attributes);
        assertThat(attributes.size(), is(1));
        //execution
        reader.applyBandAttributes(attributes, targetBand);
        //verification
        assertThat(targetBand.getSolarFlux(), is(24.3f));
    }

    @Test
    public void applySpectralBandIndex() {
        //preparation
        sourceBand.setSpectralBandIndex(24);
        ZarrProductWriter.collectBandAttributes(sourceBand, attributes);
        assertThat(attributes.size(), is(1));
        //execution
        reader.applyBandAttributes(attributes, targetBand);
        //verification
        assertThat(targetBand.getSpectralBandIndex(), is(24));
    }
}