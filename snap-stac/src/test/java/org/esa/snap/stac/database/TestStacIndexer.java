/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.stac.database;

import org.esa.snap.stac.StacItem;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class TestStacIndexer {

    @Test
    @Ignore
    public void testIndex() throws Exception {
        StacIndexer indexer = new StacIndexer();

        StacItem stacItemMock = mock(StacItem.class);
        indexer.index(stacItemMock);
    }
}
