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

public class StacIndexer implements Indexer {

    private final StacDatabase stacDatabase;

    public StacIndexer() throws Exception {
        stacDatabase = new StacDatabase();
        stacDatabase.initialize();
    }

    public void index(final StacItem stacItem) throws Exception {
        stacDatabase.saveItem(stacItem);
    }
}
