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
package org.esa.snap.stac.internal;

import org.esa.snap.stac.StacCatalog;

public class StacCatalogStats {

    private String name;
    private int collectionCnt = 0;
    private int itemCnt = 0;

    public StacCatalogStats(final StacCatalog catalog) {
        this.name = catalog.getId();
    }

    @Override
    public String toString() {
        return name + ": " + collectionCnt + " collections " + itemCnt + " items";
    }

    public synchronized void incCollection() {
        collectionCnt++;
    }

    public synchronized void incItem() {
        itemCnt++;
    }

    public int getNumCollections() {
        return collectionCnt;
    }

    public int getNumItems() {
        return itemCnt;
    }
}
