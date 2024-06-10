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

//import org.h2gis.utilities.SpatialResultSet;

import java.sql.Timestamp;

public class StacRecord {

    public String stacID, selfHREF;
    public Timestamp time;

//    public StacRecord(final SpatialResultSet resultSet) throws SQLException {
//        int id = resultSet.getInt("id");
//        stacID = resultSet.getString(STAC_ID);
//        selfHREF = resultSet.getString(SELF_HREF);
//
//        time = resultSet.getTimestamp(ACQUISITION_DATE);
//    }
}
