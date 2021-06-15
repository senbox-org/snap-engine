/*
 *
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
 *
 */

package org.esa.snap.dataio.znap;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.esa.snap.dataio.znap.CFConstantsAndUtils.FLAG_MASKS;
import static org.esa.snap.dataio.znap.CFConstantsAndUtils.FLAG_MEANINGS;
import static org.esa.snap.dataio.znap.CFConstantsAndUtils.FLAG_VALUES;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.FLAG_DESCRIPTIONS;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.NAME_SAMPLE_CODING;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


public class ZarrProductWriterTest_createCfConformSampleCodingAttributes {

    private Map<String, Object> cfConformSampleCodingAttributes;

    @Before
    public void setUp() throws Exception {
        cfConformSampleCodingAttributes = new HashMap<>();
    }

    @Test
    public void noSampleCoding() {
        final Band band = new Band("band", ProductData.TYPE_INT32, 16, 4);

        ZarrProductWriter.collectBandAttributes(band, cfConformSampleCodingAttributes);

        assertThat(cfConformSampleCodingAttributes, is(notNullValue()));
        assertThat(cfConformSampleCodingAttributes.isEmpty(), is(true));
    }

    @Test
    public void indexCoding() {
        final Band band = new Band("band", ProductData.TYPE_INT32, 16, 4);
        final IndexCoding ic = new IndexCoding("IC");
        band.setSampleCoding(ic);
        ic.addIndex("i1", 1, "d1");
        ic.addIndex("i2", 2, "d2   "); // deskription shall be trimmed
        ic.addIndex("i3", 3, "  d3"); // deskription shall be trimmed
        ic.addIndex("i4", 4, "  d4  "); // deskription shall be trimmed
        ic.addIndex("i5", 5, "d5");

        ZarrProductWriter.collectSampleCodingAttributes(band, cfConformSampleCodingAttributes);

        assertThat(cfConformSampleCodingAttributes, is(notNullValue()));
        assertThat(cfConformSampleCodingAttributes.size(), is(4));

        assertThat(cfConformSampleCodingAttributes.containsKey(NAME_SAMPLE_CODING), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MEANINGS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_DESCRIPTIONS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_VALUES), is(true));

        assertThat(cfConformSampleCodingAttributes.get(NAME_SAMPLE_CODING), is("IC"));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MEANINGS), is(new String[]{
                "i1", "i2", "i3", "i4", "i5"}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_DESCRIPTIONS), is(new String[]{
                "d1", "d2", "d3", "d4", "d5"}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_VALUES), is(new int[]{
                1, 2, 3, 4, 5}));
    }

    @Test
    public void indexCoding_noOrEmptyDescription() {
        final Band band = new Band("band", ProductData.TYPE_INT32, 16, 4);
        final IndexCoding ic = new IndexCoding("IC");
        band.setSampleCoding(ic);
        ic.addIndex("i1", 1, null);
        ic.addIndex("i2", 2, "");
        ic.addIndex("i3", 3, " ");
        ic.addIndex("i4", 4, "  ");
        ic.addIndex("i5", 5, "   ");

        ZarrProductWriter.collectSampleCodingAttributes(band, this.cfConformSampleCodingAttributes);

        assertThat(cfConformSampleCodingAttributes, is(notNullValue()));
        assertThat(cfConformSampleCodingAttributes.size(), is(3));

        assertThat(cfConformSampleCodingAttributes.containsKey(NAME_SAMPLE_CODING), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MEANINGS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_VALUES), is(true));

        assertThat(cfConformSampleCodingAttributes.get(NAME_SAMPLE_CODING), is("IC"));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MEANINGS), is(new String[]{
                "i1", "i2", "i3", "i4", "i5"}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_VALUES), is(new int[]{
                1, 2, 3, 4, 5}));
    }

    @Test
    public void singleBitFlagCoding() {
        final Band band = new Band("band", ProductData.TYPE_INT32, 16, 4);
        final FlagCoding fc = new FlagCoding("FC");
        band.setSampleCoding(fc);
        fc.addFlag("f1", 0b00000001, "d1");
        fc.addFlag("f2", 0b00000010, "d2");
        fc.addFlag("f3", 0b00000100, "d3");
        fc.addFlag("f4", 0b00001000, "d4");
        fc.addFlag("f5", 0b00010000, "d5");

        ZarrProductWriter.collectSampleCodingAttributes(band, this.cfConformSampleCodingAttributes);

        assertThat(cfConformSampleCodingAttributes, is(notNullValue()));
        assertThat(cfConformSampleCodingAttributes.size(), is(4));

        assertThat(cfConformSampleCodingAttributes.containsKey(NAME_SAMPLE_CODING), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MEANINGS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_DESCRIPTIONS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MASKS), is(true));

        assertThat(cfConformSampleCodingAttributes.get(NAME_SAMPLE_CODING), is("FC"));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MEANINGS), is(new String[]{
                "f1", "f2", "f3", "f4", "f5"}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_DESCRIPTIONS), is(new String[]{
                "d1", "d2", "d3", "d4", "d5"}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MASKS), is(new int[]{
                1, 2, 4, 8, 16}));

    }

    @Test
    public void singleBitFlagCoding_noOrEmptyDescription() {
        final Band band = new Band("band", ProductData.TYPE_INT32, 16, 4);
        final FlagCoding fc = new FlagCoding("FC");
        band.setSampleCoding(fc);
        fc.addFlag("f1", 0b00000001, null);
        fc.addFlag("f2", 0b00000010, "");
        fc.addFlag("f3", 0b00000100, " ");
        fc.addFlag("f4", 0b00001000, "  ");
        fc.addFlag("f5", 0b00010000, null);

        ZarrProductWriter.collectSampleCodingAttributes(band, this.cfConformSampleCodingAttributes);

        assertThat(cfConformSampleCodingAttributes, is(notNullValue()));
        assertThat(cfConformSampleCodingAttributes.size(), is(3));

        assertThat(cfConformSampleCodingAttributes.containsKey(NAME_SAMPLE_CODING), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MEANINGS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MASKS), is(true));

        assertThat(cfConformSampleCodingAttributes.get(NAME_SAMPLE_CODING), is("FC"));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MEANINGS), is(new String[]{
                "f1", "f2", "f3", "f4", "f5"}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MASKS), is(new int[]{
                1, 2, 4, 8, 16}));

    }

    @Test
    public void mixedSingleBitAndBitFieldFlagCoding() {
        final Band band = new Band("band", ProductData.TYPE_INT32, 16, 4);
        final FlagCoding fc = new FlagCoding("FC");
        band.setSampleCoding(fc);
        fc.addFlag("f1", 0b00000001, "d1");
        fc.addFlag("f2", 0b00000010, "d2");
        fc.addFlag("fa", 0b00001100, 0b0000, "da  ");
        fc.addFlag("fb", 0b00001100, 0b0100, "  db");
        fc.addFlag("fc", 0b00001100, 0b1000, " dc ");
        fc.addFlag("fd", 0b00001100, 0b1100, "dd");
        fc.addFlag("f5", 0b00010000, "d5");

        ZarrProductWriter.collectSampleCodingAttributes(band, this.cfConformSampleCodingAttributes);

        assertThat(cfConformSampleCodingAttributes, is(notNullValue()));
        assertThat(cfConformSampleCodingAttributes.size(), is(5));

        assertThat(cfConformSampleCodingAttributes.containsKey(NAME_SAMPLE_CODING), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MEANINGS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_DESCRIPTIONS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MASKS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_VALUES), is(true));

        assertThat(cfConformSampleCodingAttributes.get(NAME_SAMPLE_CODING), is("FC"));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MEANINGS), is(new String[]{
                "f1", "f2", "fa", "fb", "fc", "fd", "f5"}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_DESCRIPTIONS), is(new String[]{
                "d1", "d2", "da", "db", "dc", "dd", "d5"}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MASKS), is(new int[]{
                1, 2, 12, 12, 12, 12, 16}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_VALUES), is(new int[]{
                1, 2, 0, 4, 8, 12, 16}));
    }

    @Test
    public void mixedSingleBitAndBitFieldFlagCoding_noOrEmptyDescription() {
        final Band band = new Band("band", ProductData.TYPE_INT32, 16, 4);
        final FlagCoding fc = new FlagCoding("FC");
        band.setSampleCoding(fc);
        fc.addFlag("f1", 0b00000001, "");
        fc.addFlag("f2", 0b00000010, null);
        fc.addFlag("fa", 0b00001100, 0b0000, null);
        fc.addFlag("fb", 0b00001100, 0b0100, "");
        fc.addFlag("fc", 0b00001100, 0b1000, " ");
        fc.addFlag("fd", 0b00001100, 0b1100, "  ");
        fc.addFlag("f5", 0b00010000, "");

        ZarrProductWriter.collectSampleCodingAttributes(band, this.cfConformSampleCodingAttributes);

        assertThat(cfConformSampleCodingAttributes, is(notNullValue()));
        assertThat(cfConformSampleCodingAttributes.size(), is(4));

        assertThat(cfConformSampleCodingAttributes.containsKey(NAME_SAMPLE_CODING), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MEANINGS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_MASKS), is(true));
        assertThat(cfConformSampleCodingAttributes.containsKey(FLAG_VALUES), is(true));

        assertThat(cfConformSampleCodingAttributes.get(NAME_SAMPLE_CODING), is("FC"));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MEANINGS), is(new String[]{
                "f1", "f2", "fa", "fb", "fc", "fd", "f5"}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_MASKS), is(new int[]{
                1, 2, 12, 12, 12, 12, 16}));
        assertThat(cfConformSampleCodingAttributes.get(FLAG_VALUES), is(new int[]{
                1, 2, 0, 4, 8, 12, 16}));
    }
}