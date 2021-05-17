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

package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.dataop.maptransf.Datum;

import static org.esa.snap.core.datamodel.GeoCodingPersistenceHelper.createDatum;
import static org.esa.snap.core.datamodel.GeoCodingPersistenceHelper.createDatumContainer;

public class TiePointGeoCodingPersistenceConverter implements PersistenceConverter<TiePointGeoCoding> {

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 is used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "TPGC:1";

    private static final String NAME_TIE_POINT_GEO_CODING = "TiePointGeoCoding";
    private static final String NAME_GEOPOSITION = "Geoposition";
    private static final String NAME_GEOPOSITION_POINTS = "Geoposition_Points";
    private static final String NAME_TIE_POINT_GRID_NAME_LAT = "TIE_POINT_GRID_NAME_LAT";
    private static final String NAME_TIE_POINT_GRID_NAME_LON = "TIE_POINT_GRID_NAME_LON";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    public Item encode(TiePointGeoCoding gc) {
        final Container root = createRootContainer(NAME_TIE_POINT_GEO_CODING);
        root.add(createDatumContainer(gc));

        final Container geoPosPoints = new Container(NAME_GEOPOSITION_POINTS);
        geoPosPoints.add(new Property<>(NAME_TIE_POINT_GRID_NAME_LAT, gc.getLatGrid().getName()));
        geoPosPoints.add(new Property<>(NAME_TIE_POINT_GRID_NAME_LON, gc.getLonGrid().getName()));
        final Container geoPosContainer = new Container(NAME_GEOPOSITION);
        geoPosContainer.add(geoPosPoints);

        root.add(geoPosContainer);

        return root;
    }

    @Override
    public TiePointGeoCoding decode(Item item, Product product) {
        final Container root = item.asContainer();

        final Datum datum = createDatum(root.getContainer(GeoCodingPersistenceHelper.NAME_COORDINATE_REF_SYS));

        final Container geoPosContainer = root.getContainer(NAME_GEOPOSITION);
        final Container geoPointsContainer = geoPosContainer.getContainer(NAME_GEOPOSITION_POINTS);
        final String latName = geoPointsContainer.getProperty(NAME_TIE_POINT_GRID_NAME_LAT).getValueString();
        final String lonName = geoPointsContainer.getProperty(NAME_TIE_POINT_GRID_NAME_LON).getValueString();
        final TiePointGrid latGrid = product.getTiePointGrid(latName);
        final TiePointGrid lonGrid = product.getTiePointGrid(lonName);
        return new TiePointGeoCoding(latGrid, lonGrid, datum);
    }

    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {

        // A DimapHistoricalDecoder is not needed because this persistence converter
        // will never be used in DIMAP context.
        return new HistoricalDecoder[0];
    }
}
