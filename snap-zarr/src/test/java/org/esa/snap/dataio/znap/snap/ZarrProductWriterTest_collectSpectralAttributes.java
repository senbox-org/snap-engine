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

import static org.esa.snap.dataio.znap.snap.ZnapConstantsAndUtils.BANDWIDTH;
import static org.esa.snap.dataio.znap.snap.ZnapConstantsAndUtils.SOLAR_FLUX;
import static org.esa.snap.dataio.znap.snap.ZnapConstantsAndUtils.SPECTRAL_BAND_INDEX;
import static org.esa.snap.dataio.znap.snap.ZnapConstantsAndUtils.UNIT_EXTENSION;
import static org.esa.snap.dataio.znap.snap.ZnapConstantsAndUtils.WAVELENGTH;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ZarrProductWriterTest_collectSpectralAttributes {

    private Band band;

    @Before
    public void setUp() throws Exception {
        band = new Band("band", ProductData.TYPE_FLOAT32, 10, 10);
    }

    @Test
    public void collectBandwidth() {
        final HashMap<String, Object> attributes = new HashMap<>();

        band.setSpectralBandwidth(234.5f);
        ZarrProductWriter.collectBandAttributes(band, attributes);

        assertThat(attributes.size(), is(2));
        assertThat(attributes.containsKey(BANDWIDTH), is(true));
        assertThat(attributes.containsKey(BANDWIDTH+UNIT_EXTENSION), is(true));
        assertThat(attributes.get(BANDWIDTH), is(234.5f));
        assertThat(attributes.get(BANDWIDTH+UNIT_EXTENSION), is("nm"));
    }

    @Test
    public void collectWavelength() {
        final HashMap<String, Object> attributes = new HashMap<>();

        band.setSpectralWavelength(123.4f);
        ZarrProductWriter.collectBandAttributes(band, attributes);

        assertThat(attributes.size(), is(2));
        assertThat(attributes.containsKey(WAVELENGTH), is(true));
        assertThat(attributes.containsKey(WAVELENGTH + UNIT_EXTENSION), is(true));
        assertThat(attributes.get(WAVELENGTH), is(123.4f));
        assertThat(attributes.get(WAVELENGTH+UNIT_EXTENSION), is("nm"));
    }

    @Test
    public void collectSolarFlux() {
        final HashMap<String, Object> attributes = new HashMap<>();

        band.setSolarFlux(23.4f);
        ZarrProductWriter.collectBandAttributes(band, attributes);

        assertThat(attributes.size(), is(1));
        assertThat(attributes.containsKey(SOLAR_FLUX), is(true));
        assertThat(attributes.get(SOLAR_FLUX), is(23.4f));
    }

    @Test
    public void collectSpectralBandIndex() {
        final HashMap<String, Object> attributes = new HashMap<>();

        band.setSpectralBandIndex(23);
        ZarrProductWriter.collectBandAttributes(band, attributes);

        assertThat(attributes.size(), is(1));
        assertThat(attributes.containsKey(SPECTRAL_BAND_INDEX), is(true));
        assertThat(attributes.get(SPECTRAL_BAND_INDEX), is(23));
    }


}