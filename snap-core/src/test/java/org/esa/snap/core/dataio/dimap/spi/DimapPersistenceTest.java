/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataio.dimap.spi;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.*;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DimapPersistenceTest {

    @Test
    @STTM("SNAP-3481")
    public void testGetPersistabelByElement() {

        final DimapPersistable gfbPersistable = DimapPersistence.getPersistable(createFilterBandElement(
                "GeneralFilterBand"));
        assertNotNull(gfbPersistable);
        assertTrue(gfbPersistable instanceof GeneralFilterBandPersistable);

        final DimapPersistable cfbPersistable = DimapPersistence.getPersistable(createFilterBandElement(
                "ConvolutionFilterBand"));
        assertNotNull(cfbPersistable);
        assertTrue(cfbPersistable instanceof ConvolutionFilterBandPersistable);

    }

    @Test
    @STTM("SNAP-3481")
    public void testGetPersistabelByObject() {
        final GeneralFilterBand gfb = new GeneralFilterBand("test1", new Band("b", ProductData.TYPE_UINT16, 2, 2), GeneralFilterBand.OpType.MAX, new Kernel(1, 1, new double[1]), 1);
        final DimapPersistable gfbPersistable = DimapPersistence.getPersistable(gfb);
        assertNotNull(gfbPersistable);
        assertTrue(gfbPersistable instanceof GeneralFilterBandPersistable);

        final ConvolutionFilterBand cfb = new ConvolutionFilterBand("test2",
                new Band("b", ProductData.TYPE_INT8, 3, 3),
                new Kernel(2, 2, new double[4]), 1);
        final DimapPersistable cfbPersistable = DimapPersistence.getPersistable(cfb);
        assertNotNull(cfbPersistable);
        assertTrue(cfbPersistable instanceof ConvolutionFilterBandPersistable);
    }

    private static Element createFilterBandElement(String filterType) {
        final Element bandInfo = new Element(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
        final Element filterInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
        final Attribute bandType = new Attribute(DimapProductConstants.ATTRIB_BAND_TYPE, filterType);
        filterInfo.setAttribute(bandType);
        bandInfo.setContent(filterInfo);
        return bandInfo;
    }
}
