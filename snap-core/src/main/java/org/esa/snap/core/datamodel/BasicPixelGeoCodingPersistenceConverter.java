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

public class BasicPixelGeoCodingPersistenceConverter implements PersistenceConverter<BasicPixelGeoCoding> {

    private static final String NAME_PIXEL_GEO_CODING = "PixelGeoCoding";
    private static final String NAME_PIXEL_GEO_CODING_2 = "PixelGeoCoding2";
    private static final String NAME_LATITUDE_BAND = "LATITUDE_BAND";
    private static final String NAME_LONGITUDE_BAND = "LONGITUDE_BAND";
    private static final String NAME_VALID_MASK_EXPRESSION = "VALID_MASK_EXPRESSION";
    private static final String NAME_SEARCH_RADIUS = "SEARCH_RADIUS";
    private static final String NAME_PIXEL_POSITION_ESTIMATOR = "Pixel_Position_Estimator";

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 is used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "BPGC:1";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    public Item encode(BasicPixelGeoCoding pixelGeoCoding) {
        final String latBandName = pixelGeoCoding.getLatBand().getName();
        final String lonBandName = pixelGeoCoding.getLonBand().getName();
        final String validMask = pixelGeoCoding.getValidMask();
        final int searchRadius = pixelGeoCoding.getSearchRadius();
        final GeoCoding posEstimator = pixelGeoCoding.getPixelPosEstimator();

        final Container rootContainer;
        if (pixelGeoCoding instanceof PixelGeoCoding) {
            rootContainer = createRootContainer(NAME_PIXEL_GEO_CODING);
        } else {
            rootContainer = createRootContainer(NAME_PIXEL_GEO_CODING_2);
        }
        rootContainer.add(new Property<>(NAME_LATITUDE_BAND, latBandName));
        rootContainer.add(new Property<>(NAME_LONGITUDE_BAND, lonBandName));
        if (validMask != null && !validMask.trim().isEmpty()) {
            rootContainer.add(new Property<>(NAME_VALID_MASK_EXPRESSION, validMask));
        }
        rootContainer.add(new Property<>(NAME_SEARCH_RADIUS, searchRadius));
        if (posEstimator != null) {
            final PersistenceEncoder<Object> encoder = new Persistence().getEncoder(posEstimator);
            final Item encodedEstimator = encoder.encode(posEstimator);
            final Container estimatorContainer = new Container(NAME_PIXEL_POSITION_ESTIMATOR);
            rootContainer.add(estimatorContainer);
            if (encodedEstimator.isContainer()) {
                estimatorContainer.add((Container) encodedEstimator);
            } else {
                estimatorContainer.add((Property<?>) encodedEstimator);
            }
        }
        return rootContainer;
    }

    @Override
    public BasicPixelGeoCoding decode(Item item, Product product) {
        final Container rootContainer = item.asContainer();
        final String latBandName = rootContainer.getProperty(NAME_LATITUDE_BAND).getValueString();
        final String lonBandName = rootContainer.getProperty(NAME_LONGITUDE_BAND).getValueString();
        final Band latBand = product.getBand(latBandName);
        final Band lonBand = product.getBand(lonBandName);
        int searchRadius = rootContainer.getProperty(NAME_SEARCH_RADIUS).getValueInt();
        if (searchRadius == 0) {
            searchRadius = 6;
        }
        String validMask = null;
        final Property<?> validMaskProp = rootContainer.getProperty(NAME_VALID_MASK_EXPRESSION);
        if (validMaskProp != null) {
            final String valueString = validMaskProp.getValueString();
            if (valueString != null && valueString.trim().length() > 0) {
                validMask = valueString.trim();
            }
        }
        final Container posEstimatorContainer = rootContainer.getContainer(NAME_PIXEL_POSITION_ESTIMATOR);
        GeoCoding estimator = null;
        if (posEstimatorContainer != null) {
            final Container estimGC = posEstimatorContainer.getContainers()[0];
            final PersistenceDecoder<GeoCoding> decoder = new Persistence().getDecoder(estimGC);
            estimator = decoder.decode(estimGC, product);
//            if (estimator != null) {
//                product.setSceneGeoCoding(estimator);
//            }
        }

        final String codingName = rootContainer.getName();
        if (NAME_PIXEL_GEO_CODING.equals(codingName)) {
            return new PixelGeoCoding(latBand, lonBand, validMask, searchRadius, estimator);
        }
        if (NAME_PIXEL_GEO_CODING_2.equals(codingName)) {
            return new PixelGeoCoding2(latBand, lonBand, validMask, searchRadius, estimator);
        }
        return null;
    }

    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {

        // A DimapHistoricalDecoder is not needed because this persistence converter
        // will never be used in DIMAP context.
        return new HistoricalDecoder[0];
    }
}
