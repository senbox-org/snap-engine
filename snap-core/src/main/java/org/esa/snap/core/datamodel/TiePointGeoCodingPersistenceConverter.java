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

import org.esa.snap.core.dataio.persistence.Attribute;
import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.Ellipsoid;

public class TiePointGeoCodingPersistenceConverter implements PersistenceConverter<TiePointGeoCoding> {

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 is used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "TPGC:1";

    private static final String NAME_GEO_CODING = "TiePointGeoCoding";

    private static final String NAME_COORDINATE_REF_SYS = "Coordinate_Reference_System";
    private static final String NAME_HORIZONTAL_CS = "Horizontal_CS";
    private static final String NAME_HORIZONTAL_CS_TYPE = "HORIZONTAL_CS_TYPE";
    private static final String NAME_GEOGRAPHIC_CS = "Geographic_CS";
    private static final String NAME_HORIZONTAL_DATUM = "Horizontal_Datum";
    private static final String NAME_HORIZONTAL_DATUM_NAME = "HORIZONTAL_DATUM_NAME";
    private static final String NAME_ELLIPSOID = "Ellipsoid";
    private static final String NAME_ELLIPSOID_NAME = "ELLIPSOID_NAME";
    private static final String NAME_ELLIPSOID_PARAMETERS = "Ellipsoid_Parameters";
    public static final String NAME_ELLIPSOID_MAJ_AXIS = "ELLIPSOID_MAJ_AXIS";
    public static final String NAME_ELLIPSOID_MIN_AXIS = "ELLIPSOID_MIN_AXIS";

    public static final String NAME_GEOPOSITION = "Geoposition";
    public static final String NAME_GEOPOSITION_POINTS = "Geoposition_Points";
    public static final String NAME_TIE_POINT_GRID_NAME_LAT = "TIE_POINT_GRID_NAME_LAT";
    public static final String NAME_TIE_POINT_GRID_NAME_LON = "TIE_POINT_GRID_NAME_LON";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    public Item encode(TiePointGeoCoding gc) {
        final Container root = createRootContainer(NAME_GEO_CODING);

        final Datum datum = gc.getDatum();
        final Ellipsoid ellipsoid = datum.getEllipsoid();

        final Container ellipsoidParamsContainer = new Container(NAME_ELLIPSOID_PARAMETERS);
        final Property<Object> majAxisProp = new Property<>(NAME_ELLIPSOID_MAJ_AXIS, String.valueOf(ellipsoid.getSemiMajor()));
        final Property<Object> minAxisProp = new Property<>(NAME_ELLIPSOID_MIN_AXIS, String.valueOf(ellipsoid.getSemiMinor()));
        majAxisProp.set(new Attribute<>("unit", "M"));
        minAxisProp.set(new Attribute<>("unit", "M"));
        ellipsoidParamsContainer.add(majAxisProp);
        ellipsoidParamsContainer.add(minAxisProp);
        final Container ellipsoidCont = new Container(NAME_ELLIPSOID);
        ellipsoidCont.add(new Property<>(NAME_ELLIPSOID_NAME, ellipsoid.getName()));
        ellipsoidCont.add(ellipsoidParamsContainer);
        final Container horizDatumCont = new Container(NAME_HORIZONTAL_DATUM);
        horizDatumCont.add(new Property<>(NAME_HORIZONTAL_DATUM_NAME, datum.getName()));
        horizDatumCont.add(ellipsoidCont);
        final Container horCS_Cont = new Container(NAME_HORIZONTAL_CS);
        horCS_Cont.add(new Property<>(NAME_HORIZONTAL_CS_TYPE, "GEOGRAPHIC"));
        final Container geographic_cs = new Container(NAME_GEOGRAPHIC_CS);
        horCS_Cont.add(geographic_cs);
        geographic_cs.add(horizDatumCont);
        final Container coordRefSys = new Container(NAME_COORDINATE_REF_SYS);
        coordRefSys.add(horCS_Cont);

        root.add(coordRefSys);

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

        final Datum datum = createDatum(root.getContainer(NAME_COORDINATE_REF_SYS));

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

    private static Datum createDatum(Container datumRoot) {
        final Container crsElem = datumRoot.getContainer(NAME_COORDINATE_REF_SYS);
        if (crsElem != null) {
            final Container hcsElem = crsElem.getContainer(NAME_HORIZONTAL_CS);
            if (hcsElem != null) {
                final Container gcsElem = hcsElem.getContainer(NAME_GEOGRAPHIC_CS);
                if (gcsElem != null) {
                    final Container horizontalDatumElem = gcsElem.getContainer(NAME_HORIZONTAL_DATUM);
                    if (horizontalDatumElem != null) {
                        final String datumName = horizontalDatumElem.getProperty(NAME_HORIZONTAL_DATUM_NAME).getValueString();
                        final Container ellipsoidElem = horizontalDatumElem.getContainer(NAME_ELLIPSOID);
                        if (ellipsoidElem != null) {
                            final String ellipsoidName = ellipsoidElem.getProperty(NAME_ELLIPSOID_NAME).getValueString();
                            final Container ellipsoidParamElem = ellipsoidElem.getContainer(NAME_ELLIPSOID_PARAMETERS);
                            if (ellipsoidParamElem != null) {
                                final Property<?> majorAxisElem = ellipsoidParamElem.getProperty(NAME_ELLIPSOID_MAJ_AXIS);
                                final Property<?> minorAxisElem = ellipsoidParamElem.getProperty(NAME_ELLIPSOID_MIN_AXIS);
                                if (majorAxisElem != null && minorAxisElem != null) {
                                    final double majorAxis = majorAxisElem.getValueDouble();
                                    final double minorAxis = minorAxisElem.getValueDouble();

                                    final Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName, minorAxis, majorAxis);
                                    return new Datum(datumName, ellipsoid, 0, 0, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        return Datum.WGS_84;
    }

}
