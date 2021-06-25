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
import org.esa.snap.core.dataio.persistence.Persistence;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.PersistenceDecoder;
import org.esa.snap.core.dataio.persistence.PersistenceEncoder;
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.SystemUtils;

import java.util.logging.Level;

import static org.esa.snap.core.datamodel.GeoCodingPersistenceHelper.createDatumContainer;

public class GcpGeoCodingPersistenceConverter extends PersistenceConverter<GcpGeoCoding> {

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 is used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "GcpGC:1";

    private static final String NAME_GCP_GEO_CODING = "GcpGeoCoding";
    private static final String NAME_GEOPOSITION = "Geoposition";
    private static final String NAME_GEOPOSITION_POINTS = "Geoposition_Points";
    private static final String NAME_INTERPOLATION_METHOD = "INTERPOLATION_METHOD";
    private static final String NAME_ORIGINAL_GEOCODING = "Original_Geocoding";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    public Item encode(GcpGeoCoding geoCoding) {
        final Container root = createRootContainer(NAME_GCP_GEO_CODING);
        root.add(createDatumContainer(geoCoding));

        final Container geoPosition = new Container(NAME_GEOPOSITION);
        root.add(geoPosition);

        final Container geoPositionPoints = new Container(NAME_GEOPOSITION_POINTS);
        geoPosition.add(geoPositionPoints);

        geoPositionPoints.add(new Property<>(NAME_INTERPOLATION_METHOD, geoCoding.getMethod().name()));
        final GeoCoding originalGC = geoCoding.getOriginalGeoCoding();
        if (!(originalGC == null || originalGC instanceof GcpGeoCoding)) {
            final Persistence persistence = new Persistence();
            final PersistenceEncoder<Object> encoder = persistence.getEncoder(originalGC);
            if (encoder != null) {
                final Container originalGeocoding = new Container(NAME_ORIGINAL_GEOCODING);
                geoPositionPoints.add(originalGeocoding);
                final Item originalEncoded = encoder.encode(originalGC);
                originalGeocoding.add(originalEncoded.asContainer());
            }
        }
        return root;
    }

    @Override
    public GcpGeoCoding decodeImpl(Item item, Product product) {
        final Container root = item.asContainer();
        final Container CRS = root.getContainer(GeoCodingPersistenceHelper.NAME_COORDINATE_REF_SYS);
        final Datum datum = GeoCodingPersistenceHelper.createDatum(CRS);

        final Container geoPosition = root.getContainer(NAME_GEOPOSITION);
        final Container geoPositionPoints = geoPosition.getContainer(NAME_GEOPOSITION_POINTS);

        GeoCoding originalGC = null;
        final Container originalGCCont = geoPositionPoints.getContainer(NAME_ORIGINAL_GEOCODING);
        if (originalGCCont != null) {
            final Container[] containers = originalGCCont.getContainers();
            for (Container container : containers) {
                if (container == null) {
                    continue;
                }
                final PersistenceDecoder<Object> decoder = new Persistence().getDecoder(container);
                originalGC = (GeoCoding) decoder.decode(container, product);
                break;
            }
        }

        final String interpName = geoPositionPoints.getProperty(NAME_INTERPOLATION_METHOD).getValueString();
        final GcpGeoCoding.Method interpolationMethod = GcpGeoCoding.Method.valueOf(interpName);
        final PlacemarkGroup gcpGroup = product.getGcpGroup();
        final Placemark[] placemarks = gcpGroup.toArray(new Placemark[gcpGroup.getNodeCount()]);
        GcpGeoCoding gcpGeoCoding = null;
        try {
            gcpGeoCoding = new GcpGeoCoding(interpolationMethod, placemarks,
                                            product.getSceneRasterWidth(),
                                            product.getSceneRasterHeight(),
                                            datum);
            if (originalGC != null) {
                gcpGeoCoding.setOriginalGeoCoding(originalGC);
            }
        } catch (Exception e) {
            SystemUtils.LOG.log(Level.WARNING, "Unable to create GcpGeoCoding.", e);
        }
        return gcpGeoCoding;
    }

    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {

        // A DimapHistoricalDecoder is not needed because this persistence converter
        // will never be used in DIMAP context.
        return new HistoricalDecoder[0];
    }
}
