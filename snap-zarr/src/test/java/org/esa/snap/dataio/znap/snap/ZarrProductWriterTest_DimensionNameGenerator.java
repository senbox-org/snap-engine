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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ZarrProductWriterTest_DimensionNameGenerator {

    private ZarrProductWriter.DimensionNameGenerator generator;

    @Before
    public void setUp() throws Exception {
        generator = new ZarrProductWriter.DimensionNameGenerator();
    }

    @Test
    public void testThatReturnValueIsTheSameIfConditionsAreTheSame() {
        final String firstTime_22 = generator.getDimensionNameFor("x", 22);
        final String secondTime_22 = generator.getDimensionNameFor("x", 22);
        assertThat(firstTime_22).isEqualTo("x");
        assertThat(secondTime_22).isSameAs(firstTime_22);

        final String firstTime_44 = generator.getDimensionNameFor("x", 44);
        final String secondTime_44 = generator.getDimensionNameFor("x", 44);
        assertThat(firstTime_44).isEqualTo("x_1");
        assertThat(secondTime_44).isSameAs(firstTime_44);
    }

    @Test
    public void testThatEachDimensionStartsWithoutNumberExtension() {
        assertThat(generator.getDimensionNameFor("a", 22)).isEqualTo("a");
        assertThat(generator.getDimensionNameFor("b", 22)).isEqualTo("b");
        assertThat(generator.getDimensionNameFor("c", 22)).isEqualTo("c");
        assertThat(generator.getDimensionNameFor("d", 22)).isEqualTo("d");
    }

    @Test
    public void testThatEachDimensionWithTheSameNameButDifferentSizeBecomesANumberExtendedName() {
        generator.getDimensionNameFor("a", 22);
        generator.getDimensionNameFor("b", 22);
        generator.getDimensionNameFor("c", 22);
        generator.getDimensionNameFor("d", 22);

        assertThat(generator.getDimensionNameFor("a", 44)).isEqualTo("a_1");
        assertThat(generator.getDimensionNameFor("b", 44)).isEqualTo("b_1");
        assertThat(generator.getDimensionNameFor("c", 44)).isEqualTo("c_1");
        assertThat(generator.getDimensionNameFor("d", 44)).isEqualTo("d_1");
    }
}