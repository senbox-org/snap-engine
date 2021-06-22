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
import org.esa.snap.core.util.math.FXYSum;

import java.util.Arrays;

import static org.esa.snap.core.datamodel.GeoCodingPersistenceHelper.createDatumContainer;

public class FXYGeoCodingPersistenceConverter implements PersistenceConverter<FXYGeoCoding> {

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 is used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "FXYGC:1";

    private static final String NAME_FXY_GEO_CODING = "FXYGeoCoding";
    private static final String NAME_GEOPOSITION = "Geoposition";
    private static final String NAME_GEOPOSITION_INSERT = "Geoposition_Insert";
    private static final String NAME_ULX_MAP = "ULXMAP";
    private static final String NAME_ULY_MAP = "ULYMAP";
    private static final String NAME_X_DIM = "XDIM";
    private static final String NAME_Y_DIM = "YDIM";
    private static final String NAME_SIMPLIFIED_LOCATION_MODEL = "Simplified_Location_Model";
    private static final String NAME_DIRECT_LOCATION_MODEL = "Direct_Location_Model";
    private static final String NAME_REVERSE_LOCATION_MODEL = "Reverse_Location_Model";
    private static final String NAME_LC_LIST = "lc_List";
    private static final String NAME_PC_LIST = "pc_List";
    private static final String NAME_IC_LIST = "ic_List";
    private static final String NAME_JC_LIST = "jc_List";

    private static final String ATTRIBUTE_NAME_ORDER = "order";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    public Item encode(FXYGeoCoding geoCoding) {
        final Container root = createRootContainer(NAME_FXY_GEO_CODING);
        root.add(createDatumContainer(geoCoding));

        final Container GeoPosition = new Container(NAME_GEOPOSITION);
        root.add(GeoPosition);

        final Container GeoPositionInsert = new Container(NAME_GEOPOSITION_INSERT);
        GeoPosition.add(GeoPositionInsert);
        GeoPositionInsert.add(new Property<>(NAME_ULX_MAP, geoCoding.getPixelOffsetX()));
        GeoPositionInsert.add(new Property<>(NAME_ULY_MAP, geoCoding.getPixelOffsetY()));
        GeoPositionInsert.add(new Property<>(NAME_X_DIM, geoCoding.getPixelSizeX()));
        GeoPositionInsert.add(new Property<>(NAME_Y_DIM, geoCoding.getPixelSizeY()));

        final Container SimplifiedLocationModel = new Container(NAME_SIMPLIFIED_LOCATION_MODEL);
        GeoPosition.add(SimplifiedLocationModel);

        final Container DirectLocationModel = new Container(NAME_DIRECT_LOCATION_MODEL);
        SimplifiedLocationModel.add(DirectLocationModel);
        DirectLocationModel.set(new Attribute<>(ATTRIBUTE_NAME_ORDER, geoCoding.getLatFunction().getOrder()));
        DirectLocationModel.add(new Property<>(NAME_LC_LIST, geoCoding.getLonFunction().getCoefficients()));
        DirectLocationModel.add(new Property<>(NAME_PC_LIST, geoCoding.getLatFunction().getCoefficients()));

        final int reverseOrder = geoCoding.getPixelXFunction().getOrder();
        final Container ReverseLocationModel = new Container(NAME_REVERSE_LOCATION_MODEL);
        SimplifiedLocationModel.add(ReverseLocationModel);
        ReverseLocationModel.set(new Attribute<>(ATTRIBUTE_NAME_ORDER, String.valueOf(reverseOrder)));
        ReverseLocationModel.add(new Property<>(NAME_IC_LIST, geoCoding.getPixelXFunction().getCoefficients()));
        ReverseLocationModel.add(new Property<>(NAME_JC_LIST, geoCoding.getPixelYFunction().getCoefficients()));

        return root;
    }

    @Override
    public FXYGeoCoding decode(Item item, Product product) {
        final Container root = item.asContainer();
        final Container CRS = root.getContainer(GeoCodingPersistenceHelper.NAME_COORDINATE_REF_SYS);
        final Datum datum = GeoCodingPersistenceHelper.createDatum(CRS);

        final Container geoPosition = root.getContainer(NAME_GEOPOSITION);

        final Container geoPosInsert = geoPosition.getContainer(NAME_GEOPOSITION_INSERT);

        final float ulX = geoPosInsert.getProperty(NAME_ULX_MAP).getValueFloat();
        final float ulY = geoPosInsert.getProperty(NAME_ULY_MAP).getValueFloat();
        final float xDim = geoPosInsert.getProperty(NAME_X_DIM).getValueFloat();
        final float yDim = geoPosInsert.getProperty(NAME_Y_DIM).getValueFloat();

        final Container simplifiedLocationModel = geoPosition.getContainer(NAME_SIMPLIFIED_LOCATION_MODEL);

        final Container directLocationModel = simplifiedLocationModel.getContainer(NAME_DIRECT_LOCATION_MODEL);
        final int dlmOrder = directLocationModel.getAttribute(ATTRIBUTE_NAME_ORDER).getValueInt();
        final Double[] lambdaCoeffs = directLocationModel.getProperty(NAME_LC_LIST).getValueDoubles();
        final Double[] phiCoeffs = directLocationModel.getProperty(NAME_PC_LIST).getValueDoubles();

        final Container reverseLocationModel = simplifiedLocationModel.getContainer(NAME_REVERSE_LOCATION_MODEL);
        final int rlmOrder = reverseLocationModel.getAttribute(ATTRIBUTE_NAME_ORDER).getValueInt();
        final Double[] xCoeffs = reverseLocationModel.getProperty(NAME_IC_LIST).getValueDoubles();
        final Double[] yCoeffs = reverseLocationModel.getProperty(NAME_JC_LIST).getValueDoubles();

        final FXYSum lambdaSum = FXYSum.createFXYSum(dlmOrder, toPrimitive(lambdaCoeffs));
        final FXYSum phiSum = FXYSum.createFXYSum(dlmOrder, toPrimitive(phiCoeffs));
        final FXYSum xSum = FXYSum.createFXYSum(rlmOrder, toPrimitive(xCoeffs));
        final FXYSum ySum = FXYSum.createFXYSum(rlmOrder, toPrimitive(yCoeffs));

        return new FXYGeoCoding(ulX, ulY, xDim, yDim, xSum, ySum, phiSum, lambdaSum, datum);
    }

    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {

        // A DimapHistoricalDecoder is not needed because this persistence converter
        // will never be used in DIMAP context.
        return new HistoricalDecoder[0];
    }

    private double[] toPrimitive(Double[] doubles) {
        return Arrays.stream(doubles).mapToDouble(Double::doubleValue).toArray();
    }
}
