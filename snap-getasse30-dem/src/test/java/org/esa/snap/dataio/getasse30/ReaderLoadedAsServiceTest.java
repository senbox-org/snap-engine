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
package org.esa.snap.dataio.getasse30;

import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class ReaderLoadedAsServiceTest {

    @Test
    public void testReaderIsLoaded() {
        int readerCount = 0;

        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns("GETASSE30");

        while (readerPlugIns.hasNext()) {
            readerCount++;
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
        }

        assertEquals(1, readerCount);
    }
}
