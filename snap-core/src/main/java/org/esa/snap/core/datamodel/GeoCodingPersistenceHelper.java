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
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.Ellipsoid;

public class GeoCodingPersistenceHelper {
    public static final String NAME_COORDINATE_REF_SYS = "Coordinate_Reference_System";

    private static final String NAME_ELLIPSOID_MAJ_AXIS = "ELLIPSOID_MAJ_AXIS";
    private static final String NAME_ELLIPSOID_MIN_AXIS = "ELLIPSOID_MIN_AXIS";
    private static final String NAME_HORIZONTAL_CS = "Horizontal_CS";
    private static final String NAME_HORIZONTAL_CS_TYPE = "HORIZONTAL_CS_TYPE";
    private static final String NAME_GEOGRAPHIC_CS = "Geographic_CS";
    private static final String NAME_HORIZONTAL_DATUM = "Horizontal_Datum";
    private static final String NAME_HORIZONTAL_DATUM_NAME = "HORIZONTAL_DATUM_NAME";
    private static final String NAME_ELLIPSOID = "Ellipsoid";
    private static final String NAME_ELLIPSOID_NAME = "ELLIPSOID_NAME";
    private static final String NAME_ELLIPSOID_PARAMETERS = "Ellipsoid_Parameters";

    public static Container createDatumContainer(GeoCoding gc) {
        final Datum datum = gc.getDatum();
        final Ellipsoid ellipsoid = datum.getEllipsoid();

        final Container ellipsoidParamsCont = new Container(NAME_ELLIPSOID_PARAMETERS);
        final Container majAxisCont = new Container(NAME_ELLIPSOID_MAJ_AXIS);
        majAxisCont.add(new Property<>("unit", "M"));
        majAxisCont.add(new Property<>("value", String.valueOf(ellipsoid.getSemiMajor())));
        ellipsoidParamsCont.add(majAxisCont);
        final Container minAxisCont = new Container(NAME_ELLIPSOID_MIN_AXIS);
        minAxisCont.add(new Property<>("unit", "M"));
        minAxisCont.add(new Property<>("value", String.valueOf(ellipsoid.getSemiMinor())));
        ellipsoidParamsCont.add(minAxisCont);
        final Container ellipsoidCont = new Container(NAME_ELLIPSOID);
        ellipsoidCont.add(new Property<>(NAME_ELLIPSOID_NAME, ellipsoid.getName()));
        ellipsoidCont.add(ellipsoidParamsCont);
        final Container horizDatumCont = new Container(NAME_HORIZONTAL_DATUM);
        horizDatumCont.add(new Property<>(NAME_HORIZONTAL_DATUM_NAME, datum.getName()));
        horizDatumCont.add(ellipsoidCont);
        final Container horCsCont = new Container(NAME_HORIZONTAL_CS);
        horCsCont.add(new Property<>(NAME_HORIZONTAL_CS_TYPE, "GEOGRAPHIC"));
        final Container geographicCsCont = new Container(NAME_GEOGRAPHIC_CS);
        horCsCont.add(geographicCsCont);
        geographicCsCont.add(horizDatumCont);
        final Container coordRefSysCont = new Container(NAME_COORDINATE_REF_SYS);
        coordRefSysCont.add(horCsCont);
        return coordRefSysCont;
    }

    public static Datum createDatum(Container crsCont) {
        final Container hcsCont = crsCont.getContainer(NAME_HORIZONTAL_CS);
        if (hcsCont != null) {
            final Container gcsCont = hcsCont.getContainer(NAME_GEOGRAPHIC_CS);
            if (gcsCont != null) {
                final Container horizontalDatumCont = gcsCont.getContainer(NAME_HORIZONTAL_DATUM);
                if (horizontalDatumCont != null) {
                    final String datumName = horizontalDatumCont.getProperty(NAME_HORIZONTAL_DATUM_NAME).getValueString();
                    final Container ellipsoidCont = horizontalDatumCont.getContainer(NAME_ELLIPSOID);
                    if (ellipsoidCont != null) {
                        final String ellipsoidName = ellipsoidCont.getProperty(NAME_ELLIPSOID_NAME).getValueString();
                        final Container ellipsoidParamCont = ellipsoidCont.getContainer(NAME_ELLIPSOID_PARAMETERS);
                        if (ellipsoidParamCont != null) {
                            final Container majorAxisCont = ellipsoidParamCont.getContainer(NAME_ELLIPSOID_MAJ_AXIS);
                            final Container minorAxisCont = ellipsoidParamCont.getContainer(NAME_ELLIPSOID_MIN_AXIS);
                            if (majorAxisCont != null && minorAxisCont != null) {
                                final double majorAxis = majorAxisCont.getProperty("value").getValueDouble();
                                final double minorAxis = minorAxisCont.getProperty("value").getValueDouble();

                                final Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName, minorAxis, majorAxis);
                                return new Datum(datumName, ellipsoid, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        }
        return Datum.WGS_84;
    }
}
