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

package org.esa.snap.binning.operator;

import org.esa.snap.binning.DataPeriod;
import org.esa.snap.core.datamodel.Product;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Storm
 */
public class SpatialDataDaySourceProductFilterTest {

    private BinningProductFilter parent;
    private DataPeriod dataPeriod;
    private SpatialDataDaySourceProductFilter filter;

    @Before
    public void setUp() throws Exception {
        parent = mock(BinningProductFilter.class);
        when(parent.accept(Mockito.any(Product.class))).thenReturn(true);
        dataPeriod = TestUtils.createSpatialDataPeriod();
        filter = new SpatialDataDaySourceProductFilter(parent, dataPeriod);
    }

    @Test
    public void testAccept() throws Exception {

        assertFalse(filter.accept(createProduct(DataPeriod.Membership.PREVIOUS_PERIODS, DataPeriod.Membership.PREVIOUS_PERIODS)));
        assertTrue(filter.accept(createProduct(DataPeriod.Membership.PREVIOUS_PERIODS, DataPeriod.Membership.CURRENT_PERIOD)));
        assertTrue(filter.accept(createProduct(DataPeriod.Membership.PREVIOUS_PERIODS, DataPeriod.Membership.SUBSEQUENT_PERIODS)));

        assertTrue(filter.accept(createProduct(DataPeriod.Membership.CURRENT_PERIOD, DataPeriod.Membership.CURRENT_PERIOD)));
        assertTrue(filter.accept(createProduct(DataPeriod.Membership.CURRENT_PERIOD, DataPeriod.Membership.SUBSEQUENT_PERIODS)));

        assertFalse(filter.accept(createProduct(DataPeriod.Membership.SUBSEQUENT_PERIODS, DataPeriod.Membership.SUBSEQUENT_PERIODS)));
    }

    @Test
    public void testRejectProduct_IfParentFilterDoNotAcceptTheProduct() {
        //preparation
        when(parent.accept(Mockito.any(Product.class))).thenReturn(false);
        when(parent.getReason()).thenReturn("parent reason");

        //execution
        //verification
        MatcherAssert.assertThat(filter.accept(mock(Product.class)), is(false));
        MatcherAssert.assertThat(filter.getReason(), is("parent reason"));
    }

    private Product createProduct(DataPeriod.Membership firstPeriod, DataPeriod.Membership lastPeriod) {
        return TestUtils.createProduct(dataPeriod, firstPeriod, lastPeriod);
    }

}
