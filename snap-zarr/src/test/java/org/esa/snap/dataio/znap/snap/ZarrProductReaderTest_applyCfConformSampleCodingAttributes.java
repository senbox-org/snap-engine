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
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.esa.snap.dataio.znap.snap.ZnapConstantsAndUtils.*;
import static org.esa.snap.dataio.znap.snap.CFConstantsAndUtils.*;

import static org.assertj.core.api.Assertions.*;

public class ZarrProductReaderTest_applyCfConformSampleCodingAttributes {

    private Band band;
    private ZarrProductReader reader;

    @Before
    public void setUp() throws Exception {
        final Product product = new Product("PName", "PType", 1234, 2345);
        band = product.addBand("BName", ProductData.TYPE_INT32);

        reader = new ZarrProductReader(new ZarrProductReaderPlugIn());
    }

    @Test
    public void applySingelBitFlagCoding() {
        // preparation
        final HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(FLAG_MEANINGS, Arrays.asList("m1", "m2", "m3"));
        final double m1 = 0b000010; // should be int but gson library returns parsed doubles
        final double m2 = 0b001000; // should be int but gson library returns parsed doubles
        final double m3 = 0b010000; // should be int but gson library returns parsed doubles
        attributes.put(FLAG_MASKS, Arrays.asList(m1, m2, m3));

        // execution
        reader.applyBandAttributes(attributes, band);

        // verification
        assertThat(band.isFlagBand()).isTrue();
        final FlagCoding flagCoding = band.getFlagCoding();
        assertThat(flagCoding.getName()).isEqualTo("BName");
        assertThat(band.getProduct().getFlagCodingGroup().contains(flagCoding)).isTrue();

        assertThat(flagCoding.getNumAttributes()).isEqualTo(3);
        assertThat(flagCoding.getFlagNames()).isEqualTo(new String[]{"m1", "m2", "m3"});

        assertThat(flagCoding.getFlagMask("m1")).isEqualTo(2);
        assertThat(flagCoding.getFlagMask("m2")).isEqualTo(8);
        assertThat(flagCoding.getFlagMask("m3")).isEqualTo(16);
        assertThat(flagCoding.getFlag("m1").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m2").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m3").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getAttribute("m1").getData().getNumElems()).isEqualTo(1);
        assertThat(flagCoding.getAttribute("m2").getData().getNumElems()).isEqualTo(1);
        assertThat(flagCoding.getAttribute("m3").getData().getNumElems()).isEqualTo(1);
    }

    @Test
    public void applySingelBitFlagCoding_withDescriptions() {
        // preparation
        final HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(FLAG_MEANINGS, Arrays.asList("m1", "m2", "m3"));
        final double m1 = 0b000010; // should be int but gson library returns parsed doubles
        final double m2 = 0b001000; // should be int but gson library returns parsed doubles
        final double m3 = 0b010000; // should be int but gson library returns parsed doubles
        attributes.put(FLAG_MASKS, Arrays.asList(m1, m2, m3));
        attributes.put(FLAG_DESCRIPTIONS, Arrays.asList("d1", "d2", "d3"));

        // execution
        reader.applyBandAttributes(attributes, band);

        // verification
        assertThat(band.isFlagBand()).isTrue();
        final FlagCoding flagCoding = band.getFlagCoding();
        assertThat(flagCoding.getName()).isEqualTo("BName");
        assertThat(band.getProduct().getFlagCodingGroup().contains(flagCoding)).isTrue();

        assertThat(flagCoding.getNumAttributes()).isEqualTo(3);
        assertThat(flagCoding.getFlagNames()).isEqualTo(new String[]{"m1", "m2", "m3"});

        assertThat(flagCoding.getFlagMask("m1")).isEqualTo(2);
        assertThat(flagCoding.getFlagMask("m2")).isEqualTo(8);
        assertThat(flagCoding.getFlagMask("m3")).isEqualTo(16);
        assertThat(flagCoding.getFlag("m1").getDescription()).isEqualTo("d1");
        assertThat(flagCoding.getFlag("m2").getDescription()).isEqualTo("d2");
        assertThat(flagCoding.getFlag("m3").getDescription()).isEqualTo("d3");
        assertThat(flagCoding.getAttribute("m1").getData().getNumElems()).isEqualTo(1);
        assertThat(flagCoding.getAttribute("m2").getData().getNumElems()).isEqualTo(1);
        assertThat(flagCoding.getAttribute("m3").getData().getNumElems()).isEqualTo(1);
    }

    @Test
    public void applySingelBitFlagCoding_withDescriptionsAndSampleCodingName() {
        // preparation
        final HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(FLAG_MEANINGS, Arrays.asList("m1", "m2", "m3"));
        final double m1 = 0b000010; // should be int but gson library returns parsed doubles
        final double m2 = 0b001000; // should be int but gson library returns parsed doubles
        final double m3 = 0b010000; // should be int but gson library returns parsed doubles
        attributes.put(FLAG_MASKS, Arrays.asList(m1, m2, m3));
        attributes.put(FLAG_DESCRIPTIONS, Arrays.asList("d1", "d2", "d3"));
        attributes.put(NAME_SAMPLE_CODING, "SCName");

        // execution
        reader.applyBandAttributes(attributes, band);

        // verification
        assertThat(band.isFlagBand()).isTrue();
        final FlagCoding flagCoding = band.getFlagCoding();
        assertThat(flagCoding.getName()).isEqualTo("SCName");
        assertThat(band.getProduct().getFlagCodingGroup().contains(flagCoding)).isTrue();

        assertThat(flagCoding.getNumAttributes()).isEqualTo(3);
        assertThat(flagCoding.getFlagNames()).isEqualTo(new String[]{"m1", "m2", "m3"});

        assertThat(flagCoding.getFlagMask("m1")).isEqualTo(2);
        assertThat(flagCoding.getFlagMask("m2")).isEqualTo(8);
        assertThat(flagCoding.getFlagMask("m3")).isEqualTo(16);
        assertThat(flagCoding.getFlag("m1").getDescription()).isEqualTo("d1");
        assertThat(flagCoding.getFlag("m2").getDescription()).isEqualTo("d2");
        assertThat(flagCoding.getFlag("m3").getDescription()).isEqualTo("d3");
        assertThat(flagCoding.getAttribute("m1").getData().getNumElems()).isEqualTo(1);
        assertThat(flagCoding.getAttribute("m2").getData().getNumElems()).isEqualTo(1);
        assertThat(flagCoding.getAttribute("m3").getData().getNumElems()).isEqualTo(1);
    }

    @Test
    public void applyBitFieldFlagCoding() {
        // preparation
        final HashMap<String, Object> attributes = new HashMap<>();
        final double m1 = 0b000001; // should be int but gson library returns parsed doubles
        final double m2 = 0b000110; // should be int but gson library returns parsed doubles
        final double m3 = 0b111000; // should be int but gson library returns parsed doubles
        attributes.put(FLAG_MEANINGS, Arrays.asList("m1",
                                                    "m2_1", "m2_2", "m2_3",
                                                    "m3_1", "m3_2", "m3_3", "m3_4", "m3_5", "m3_6", "m3_7"));
        attributes.put(FLAG_MASKS, Arrays.asList(m1,
                                                 m2, m2, m2,
                                                 m3, m3, m3, m3, m3, m3, m3));
        final double v1 = 0b000001; // should be int but gson library returns parsed doubles
        final double v2_1 = 0b000010; // should be int but gson library returns parsed doubles
        final double v2_2 = 0b000100; // should be int but gson library returns parsed doubles
        final double v2_3 = 0b000110; // should be int but gson library returns parsed doubles
        final double v3_1 = 0b001000; // should be int but gson library returns parsed doubles
        final double v3_2 = 0b010000; // should be int but gson library returns parsed doubles
        final double v3_3 = 0b011000; // should be int but gson library returns parsed doubles
        final double v3_4 = 0b100000; // should be int but gson library returns parsed doubles
        final double v3_5 = 0b101000; // should be int but gson library returns parsed doubles
        final double v3_6 = 0b110000; // should be int but gson library returns parsed doubles
        final double v3_7 = 0b111000; // should be int but gson library returns parsed doubles
        attributes.put(FLAG_VALUES, Arrays.asList(v1,
                                                  v2_1, v2_2, v2_3,
                                                  v3_1, v3_2, v3_3, v3_4, v3_5, v3_6, v3_7));

        // execution
        reader.applyBandAttributes(attributes, band);

        // verification
        assertThat(band.isFlagBand()).isTrue();
        final FlagCoding flagCoding = band.getFlagCoding();
        assertThat(flagCoding.getName()).isEqualTo("BName");
        assertThat(band.getProduct().getFlagCodingGroup().contains(flagCoding)).isTrue();

        assertThat(flagCoding.getNumAttributes()).isEqualTo(11);
        assertThat(flagCoding.getFlagNames()).isEqualTo(new String[]{"m1",
                "m2_1", "m2_2", "m2_3",
                "m3_1", "m3_2", "m3_3", "m3_4", "m3_5", "m3_6", "m3_7"});

        assertThat(flagCoding.getFlagMask("m1")).isEqualTo(1);
        assertThat(flagCoding.getFlagMask("m2_1")).isEqualTo(6);
        assertThat(flagCoding.getFlagMask("m2_2")).isEqualTo(6);
        assertThat(flagCoding.getFlagMask("m2_3")).isEqualTo(6);
        assertThat(flagCoding.getFlagMask("m3_1")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_2")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_3")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_4")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_5")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_6")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_7")).isEqualTo(56);
        assertThat(flagCoding.getFlag("m1").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m2_1").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m2_2").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m2_3").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m3_1").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m3_2").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m3_3").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m3_4").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m3_5").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m3_6").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getFlag("m3_7").getDescription()).isNullOrEmpty();
        assertThat(flagCoding.getAttribute("m1").getData().getElems()).isEqualTo(new int[]{1, 1});
        assertThat(flagCoding.getAttribute("m2_1").getData().getElems()).isEqualTo(new int[]{6, 2});
        assertThat(flagCoding.getAttribute("m2_2").getData().getElems()).isEqualTo(new int[]{6, 4});
        assertThat(flagCoding.getAttribute("m2_3").getData().getElems()).isEqualTo(new int[]{6, 6});
        assertThat(flagCoding.getAttribute("m3_1").getData().getElems()).isEqualTo(new int[]{56, 8});
        assertThat(flagCoding.getAttribute("m3_2").getData().getElems()).isEqualTo(new int[]{56, 16});
        assertThat(flagCoding.getAttribute("m3_3").getData().getElems()).isEqualTo(new int[]{56, 24});
        assertThat(flagCoding.getAttribute("m3_4").getData().getElems()).isEqualTo(new int[]{56, 32});
        assertThat(flagCoding.getAttribute("m3_5").getData().getElems()).isEqualTo(new int[]{56, 40});
        assertThat(flagCoding.getAttribute("m3_6").getData().getElems()).isEqualTo(new int[]{56, 48});
        assertThat(flagCoding.getAttribute("m3_7").getData().getElems()).isEqualTo(new int[]{56, 56});
    }

    @Test
    public void applyBitFieldFlagCoding_withDescriptions() {
        // preparation
        final HashMap<String, Object> attributes = new HashMap<>();
        final double m1 = 0b000001; // should be int but gson library returns parsed doubles
        final double m2 = 0b000110; // should be int but gson library returns parsed doubles
        final double m3 = 0b111000; // should be int but gson library returns parsed doubles
        attributes.put(FLAG_MEANINGS, Arrays.asList("m1",
                                                    "m2_1", "m2_2", "m2_3",
                                                    "m3_1", "m3_2", "m3_3", "m3_4", "m3_5", "m3_6", "m3_7"));
        attributes.put(FLAG_MASKS, Arrays.asList(m1,
                                                 m2, m2, m2,
                                                 m3, m3, m3, m3, m3, m3, m3));
        final double v1 = 0b000001; // should be int but gson library returns parsed doubles
        final double v2_1 = 0b000010; // should be int but gson library returns parsed doubles
        final double v2_2 = 0b000100; // should be int but gson library returns parsed doubles
        final double v2_3 = 0b000110; // should be int but gson library returns parsed doubles
        final double v3_1 = 0b001000; // should be int but gson library returns parsed doubles
        final double v3_2 = 0b010000; // should be int but gson library returns parsed doubles
        final double v3_3 = 0b011000; // should be int but gson library returns parsed doubles
        final double v3_4 = 0b100000; // should be int but gson library returns parsed doubles
        final double v3_5 = 0b101000; // should be int but gson library returns parsed doubles
        final double v3_6 = 0b110000; // should be int but gson library returns parsed doubles
        final double v3_7 = 0b111000; // should be int but gson library returns parsed doubles
        attributes.put(FLAG_VALUES, Arrays.asList(v1,
                                                  v2_1, v2_2, v2_3,
                                                  v3_1, v3_2, v3_3, v3_4, v3_5, v3_6, v3_7));
        attributes.put(FLAG_DESCRIPTIONS, Arrays.asList("d1",
                                                        "d2_1", "d2_2", "d2_3",
                                                        "d3_1", "d3_2", "d3_3", "d3_4", "d3_5", "d3_6", "d3_7"));

        // execution
        reader.applyBandAttributes(attributes, band);

        // verification
        assertThat(band.isFlagBand()).isTrue();
        final FlagCoding flagCoding = band.getFlagCoding();
        assertThat(flagCoding.getName()).isEqualTo("BName");
        assertThat(band.getProduct().getFlagCodingGroup().contains(flagCoding)).isTrue();

        assertThat(flagCoding.getNumAttributes()).isEqualTo(11);
        assertThat(flagCoding.getFlagNames()).isEqualTo(new String[]{"m1",
                "m2_1", "m2_2", "m2_3",
                "m3_1", "m3_2", "m3_3", "m3_4", "m3_5", "m3_6", "m3_7"});

        assertThat(flagCoding.getFlagMask("m1")).isEqualTo(1);
        assertThat(flagCoding.getFlagMask("m2_1")).isEqualTo(6);
        assertThat(flagCoding.getFlagMask("m2_2")).isEqualTo(6);
        assertThat(flagCoding.getFlagMask("m2_3")).isEqualTo(6);
        assertThat(flagCoding.getFlagMask("m3_1")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_2")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_3")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_4")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_5")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_6")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_7")).isEqualTo(56);
        assertThat(flagCoding.getFlag("m1").getDescription()).isEqualTo("d1");
        assertThat(flagCoding.getFlag("m2_1").getDescription()).isEqualTo("d2_1");
        assertThat(flagCoding.getFlag("m2_2").getDescription()).isEqualTo("d2_2");
        assertThat(flagCoding.getFlag("m2_3").getDescription()).isEqualTo("d2_3");
        assertThat(flagCoding.getFlag("m3_1").getDescription()).isEqualTo("d3_1");
        assertThat(flagCoding.getFlag("m3_2").getDescription()).isEqualTo("d3_2");
        assertThat(flagCoding.getFlag("m3_3").getDescription()).isEqualTo("d3_3");
        assertThat(flagCoding.getFlag("m3_4").getDescription()).isEqualTo("d3_4");
        assertThat(flagCoding.getFlag("m3_5").getDescription()).isEqualTo("d3_5");
        assertThat(flagCoding.getFlag("m3_6").getDescription()).isEqualTo("d3_6");
        assertThat(flagCoding.getFlag("m3_7").getDescription()).isEqualTo("d3_7");
        assertThat(flagCoding.getAttribute("m1").getData().getElems()).isEqualTo(new int[]{1, 1});
        assertThat(flagCoding.getAttribute("m2_1").getData().getElems()).isEqualTo(new int[]{6, 2});
        assertThat(flagCoding.getAttribute("m2_2").getData().getElems()).isEqualTo(new int[]{6, 4});
        assertThat(flagCoding.getAttribute("m2_3").getData().getElems()).isEqualTo(new int[]{6, 6});
        assertThat(flagCoding.getAttribute("m3_1").getData().getElems()).isEqualTo(new int[]{56, 8});
        assertThat(flagCoding.getAttribute("m3_2").getData().getElems()).isEqualTo(new int[]{56, 16});
        assertThat(flagCoding.getAttribute("m3_3").getData().getElems()).isEqualTo(new int[]{56, 24});
        assertThat(flagCoding.getAttribute("m3_4").getData().getElems()).isEqualTo(new int[]{56, 32});
        assertThat(flagCoding.getAttribute("m3_5").getData().getElems()).isEqualTo(new int[]{56, 40});
        assertThat(flagCoding.getAttribute("m3_6").getData().getElems()).isEqualTo(new int[]{56, 48});
        assertThat(flagCoding.getAttribute("m3_7").getData().getElems()).isEqualTo(new int[]{56, 56});
    }

    @Test
    public void applyBitFieldFlagCoding_withDescriptionsAndSampleCodingName() {
        // preparation
        final HashMap<String, Object> attributes = new HashMap<>();
        final double m1 = 0b000001; // should be int but gson library returns parsed doubles
        final double m2 = 0b000110; // should be int but gson library returns parsed doubles
        final double m3 = 0b111000; // should be int but gson library returns parsed doubles
        attributes.put(FLAG_MEANINGS, Arrays.asList("m1",
                                                    "m2_1", "m2_2", "m2_3",
                                                    "m3_1", "m3_2", "m3_3", "m3_4", "m3_5", "m3_6", "m3_7"));
        attributes.put(FLAG_MASKS, Arrays.asList(m1,
                                                 m2, m2, m2,
                                                 m3, m3, m3, m3, m3, m3, m3));
        final double v1 = 0b000001; // should be int but gson library returns parsed doubles
        final double v2_1 = 0b000010; // should be int but gson library returns parsed doubles
        final double v2_2 = 0b000100; // should be int but gson library returns parsed doubles
        final double v2_3 = 0b000110; // should be int but gson library returns parsed doubles
        final double v3_1 = 0b001000; // should be int but gson library returns parsed doubles
        final double v3_2 = 0b010000; // should be int but gson library returns parsed doubles
        final double v3_3 = 0b011000; // should be int but gson library returns parsed doubles
        final double v3_4 = 0b100000; // should be int but gson library returns parsed doubles
        final double v3_5 = 0b101000; // should be int but gson library returns parsed doubles
        final double v3_6 = 0b110000; // should be int but gson library returns parsed doubles
        final double v3_7 = 0b111000; // should be int but gson library returns parsed doubles
        attributes.put(FLAG_VALUES, Arrays.asList(v1,
                                                  v2_1, v2_2, v2_3,
                                                  v3_1, v3_2, v3_3, v3_4, v3_5, v3_6, v3_7));
        attributes.put(FLAG_DESCRIPTIONS, Arrays.asList("d1",
                                                        "d2_1", "d2_2", "d2_3",
                                                        "d3_1", "d3_2", "d3_3", "d3_4", "d3_5", "d3_6", "d3_7"));
        attributes.put(NAME_SAMPLE_CODING, "SCName");

        // execution
        reader.applyBandAttributes(attributes, band);

        // verification
        assertThat(band.isFlagBand()).isTrue();
        final FlagCoding flagCoding = band.getFlagCoding();
        assertThat(flagCoding.getName()).isEqualTo("SCName");
        assertThat(band.getProduct().getFlagCodingGroup().contains(flagCoding)).isTrue();

        assertThat(flagCoding.getNumAttributes()).isEqualTo(11);
        assertThat(flagCoding.getFlagNames()).isEqualTo(new String[]{"m1",
                "m2_1", "m2_2", "m2_3",
                "m3_1", "m3_2", "m3_3", "m3_4", "m3_5", "m3_6", "m3_7"});

        assertThat(flagCoding.getFlagMask("m1")).isEqualTo(1);
        assertThat(flagCoding.getFlagMask("m2_1")).isEqualTo(6);
        assertThat(flagCoding.getFlagMask("m2_2")).isEqualTo(6);
        assertThat(flagCoding.getFlagMask("m2_3")).isEqualTo(6);
        assertThat(flagCoding.getFlagMask("m3_1")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_2")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_3")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_4")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_5")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_6")).isEqualTo(56);
        assertThat(flagCoding.getFlagMask("m3_7")).isEqualTo(56);
        assertThat(flagCoding.getFlag("m1").getDescription()).isEqualTo("d1");
        assertThat(flagCoding.getFlag("m2_1").getDescription()).isEqualTo("d2_1");
        assertThat(flagCoding.getFlag("m2_2").getDescription()).isEqualTo("d2_2");
        assertThat(flagCoding.getFlag("m2_3").getDescription()).isEqualTo("d2_3");
        assertThat(flagCoding.getFlag("m3_1").getDescription()).isEqualTo("d3_1");
        assertThat(flagCoding.getFlag("m3_2").getDescription()).isEqualTo("d3_2");
        assertThat(flagCoding.getFlag("m3_3").getDescription()).isEqualTo("d3_3");
        assertThat(flagCoding.getFlag("m3_4").getDescription()).isEqualTo("d3_4");
        assertThat(flagCoding.getFlag("m3_5").getDescription()).isEqualTo("d3_5");
        assertThat(flagCoding.getFlag("m3_6").getDescription()).isEqualTo("d3_6");
        assertThat(flagCoding.getFlag("m3_7").getDescription()).isEqualTo("d3_7");
        assertThat(flagCoding.getAttribute("m1").getData().getElems()).isEqualTo(new int[]{1, 1});
        assertThat(flagCoding.getAttribute("m2_1").getData().getElems()).isEqualTo(new int[]{6, 2});
        assertThat(flagCoding.getAttribute("m2_2").getData().getElems()).isEqualTo(new int[]{6, 4});
        assertThat(flagCoding.getAttribute("m2_3").getData().getElems()).isEqualTo(new int[]{6, 6});
        assertThat(flagCoding.getAttribute("m3_1").getData().getElems()).isEqualTo(new int[]{56, 8});
        assertThat(flagCoding.getAttribute("m3_2").getData().getElems()).isEqualTo(new int[]{56, 16});
        assertThat(flagCoding.getAttribute("m3_3").getData().getElems()).isEqualTo(new int[]{56, 24});
        assertThat(flagCoding.getAttribute("m3_4").getData().getElems()).isEqualTo(new int[]{56, 32});
        assertThat(flagCoding.getAttribute("m3_5").getData().getElems()).isEqualTo(new int[]{56, 40});
        assertThat(flagCoding.getAttribute("m3_6").getData().getElems()).isEqualTo(new int[]{56, 48});
        assertThat(flagCoding.getAttribute("m3_7").getData().getElems()).isEqualTo(new int[]{56, 56});
    }
}