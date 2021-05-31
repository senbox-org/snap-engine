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

package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.dimap.spi.DimapHistoricalDecoder;
import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.SystemUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.IntStream;

/**
 * @author Sabine Embacher
 */
public class ComponentGeoCodingPersistenceConverter implements PersistenceConverter<ComponentGeoCoding> {

    public static final String NAME_COMPONENT_GEO_CODING = "ComponentGeoCoding";
    public static final String NAME_FORWARD_CODING_KEY = "ForwardCodingKey";
    public static final String NAME_INVERSE_CODING_KEY = "InverseCodingKey";
    public static final String NAME_GEO_CHECKS = "GeoChecks";
    public static final String NAME_GEO_CRS = "GeoCRS";
    public static final String NAME_LON_VARIABLE_NAME = "LonVariableName";
    public static final String NAME_LAT_VARIABLE_NAME = "LatVariableName";
    public static final String NAME_RASTER_RESOLUTION_KM = "RasterResolutionKm";
    public static final String NAME_OFFSET_X = "OffsetX";
    public static final String NAME_OFFSET_Y = "OffsetY";
    public static final String NAME_SUBSAMPLING_X = "SubsamplingX";
    public static final String NAME_SUBSAMPLING_Y = "SubsamplingY";

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 is used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "ComponentGC:1";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    public ComponentGeoCoding decode(Item item, Product product) {
        if (item == null || !item.isContainer() || !NAME_COMPONENT_GEO_CODING.equals(item.getName())) {
            SystemUtils.LOG.warning("For decoding a container with name '" + NAME_COMPONENT_GEO_CODING + "' is expected.");
            return null;
        }
        final Container container = item.asContainer();

        final String forwardKey = container.getProperty(NAME_FORWARD_CODING_KEY).getValueString();
        final String inverseKey = container.getProperty(NAME_INVERSE_CODING_KEY).getValueString();
        final String geoChecksName = container.getProperty(NAME_GEO_CHECKS).getValueString();
        final String geoCrsWKT = container.getProperty(NAME_GEO_CRS).getValueString();
        final String lonVarName = container.getProperty(NAME_LON_VARIABLE_NAME).getValueString();
        final String latVarName = container.getProperty(NAME_LAT_VARIABLE_NAME).getValueString();
        final String resolutionKmStr = container.getProperty(NAME_RASTER_RESOLUTION_KM).getValueString();

        final boolean forwardInvalid = forwardKey == null;
        final boolean inverseInvalid = inverseKey == null;
        final boolean geoChecksInvalid = geoChecksName == null;
        final boolean geoCrsWKTInvalid = geoCrsWKT == null;
        final boolean lonVarNameInvalid = lonVarName == null;
        final boolean latVarNameInvalid = latVarName == null;
        final boolean resolutionKmInvalid = resolutionKmStr == null;

        if (forwardInvalid) {
            SystemUtils.LOG.warning("Property with name '" + NAME_FORWARD_CODING_KEY + "' expected in container item '" + NAME_COMPONENT_GEO_CODING + "'.");
        }
        if (inverseInvalid) {
            SystemUtils.LOG.warning("Property with name '" + NAME_INVERSE_CODING_KEY + "' expected in container item '" + NAME_COMPONENT_GEO_CODING + "'.");
        }
        if (geoChecksInvalid) {
            SystemUtils.LOG.warning("Property with name '" + NAME_GEO_CHECKS + "' expected in container item '" + NAME_COMPONENT_GEO_CODING + "'.");
        }
        if (geoCrsWKTInvalid) {
            SystemUtils.LOG.warning("Property with name '" + NAME_GEO_CRS + "' expected in container item '" + NAME_COMPONENT_GEO_CODING + "'.");
        }
        if (lonVarNameInvalid) {
            SystemUtils.LOG.warning("Property with name '" + NAME_LON_VARIABLE_NAME + "' expected in container item '" + NAME_COMPONENT_GEO_CODING + "'.");
        }
        if (latVarNameInvalid) {
            SystemUtils.LOG.warning("Property with name '" + NAME_LAT_VARIABLE_NAME + "' expected in container item '" + NAME_COMPONENT_GEO_CODING + "'.");
        }
        if (resolutionKmInvalid) {
            SystemUtils.LOG.warning("Property with name '" + NAME_RASTER_RESOLUTION_KM + "' expected in container item '" + NAME_COMPONENT_GEO_CODING + "'.");
        }

        Double resolutionInKm = null;
        try {
            resolutionInKm = Double.parseDouble(resolutionKmStr);
        } catch (NumberFormatException e) {
            SystemUtils.LOG.warning(e.getMessage());
            SystemUtils.LOG.warning("The value '" + resolutionKmStr + "' of property '" + NAME_RASTER_RESOLUTION_KM + "' cannot be parsed to double.");
        }

        boolean invalidValueGeoChecks = false;
        try {
            GeoChecks.valueOf(geoChecksName);
        } catch (IllegalArgumentException e) {
            invalidValueGeoChecks = true;
            SystemUtils.LOG.warning(e.getMessage());
            SystemUtils.LOG.warning("The value '" + geoChecksName + "' of property '" + NAME_GEO_CHECKS + "' is not valid.");
        }

        CoordinateReferenceSystem geoCRS = null;
        try {
            geoCRS = CRS.parseWKT(geoCrsWKT);
        } catch (FactoryException e) {
            SystemUtils.LOG.warning(e.getMessage());
            SystemUtils.LOG.warning("The WKT value '" + geoCrsWKT + "' of property '" + NAME_GEO_CRS + "' is not valid.");
        }

        final String msg_unable_to_create = "Unable to create " + NAME_COMPONENT_GEO_CODING + ".";

        if (forwardInvalid
            || inverseInvalid
            || geoChecksInvalid
            || lonVarNameInvalid
            || latVarNameInvalid
            || resolutionKmInvalid
            || resolutionInKm == null
            || geoChecksName == null
            || invalidValueGeoChecks
            || geoCRS == null) {
            SystemUtils.LOG.warning(msg_unable_to_create);
            return null;
        }

        final RasterDataNode lonRaster = product.getRasterDataNode(lonVarName);
        final RasterDataNode latRaster = product.getRasterDataNode(latVarName);

        if (lonRaster == null || latRaster == null) {
            if (lonRaster == null) {
                SystemUtils.LOG.warning("Unable to find expected longitude raster '" + lonVarName + "' in product.");
            }
            if (latRaster == null) {
                SystemUtils.LOG.warning("Unable to find expected latitude raster '" + latVarName + "' in product.");
            }
            SystemUtils.LOG.warning(msg_unable_to_create);
            return null;
        }

        final GeoRaster geoRaster;
        final HashMap<RasterDataNode, double[]> dataMap = DataHolder.getInstance().getDataMap(product);
        if (lonRaster instanceof TiePointGrid) {
            final int sceneWidth = product.getSceneRasterWidth();
            final int sceneHeight = product.getSceneRasterHeight();
            final TiePointGrid lonTPG = (TiePointGrid) lonRaster;
            final TiePointGrid latTPG = (TiePointGrid) latRaster;

            final int gridWidth = lonTPG.getGridWidth();
            final int gridHeight = lonTPG.getGridHeight();

            final double[] longitudes;
            final double[] latitudes;
            if (dataMap.containsKey(lonRaster)) {
                longitudes = dataMap.get(lonRaster);
                latitudes = dataMap.get(latRaster);
            } else {
                final float[] lons = (float[]) lonTPG.getGridData().getElems();
                longitudes = IntStream.range(0, lons.length).mapToDouble(i -> lons[i]).toArray();
                dataMap.put(lonRaster, longitudes);

                final float[] lats = (float[]) latTPG.getGridData().getElems();
                latitudes = IntStream.range(0, lats.length).mapToDouble(i -> lats[i]).toArray();
                dataMap.put(latRaster, latitudes);
            }

            final double offsetX = lonTPG.getOffsetX();
            final double offsetY = lonTPG.getOffsetY();
            final double subsamplingX = lonTPG.getSubSamplingX();
            final double subsamplingY = lonTPG.getSubSamplingY();

            geoRaster = new GeoRaster(longitudes, latitudes, lonVarName, latVarName, gridWidth, gridHeight,
                                      sceneWidth, sceneHeight, resolutionInKm,
                                      offsetX, offsetY, subsamplingX, subsamplingY);
        } else {
            final int rasterWidth = lonRaster.getRasterWidth();
            final int rasterHeight = lonRaster.getRasterHeight();

            final double[] longitudes;
            final double[] latitudes;
            if (dataMap.containsKey(lonRaster)) {
                longitudes = dataMap.get(lonRaster);
                latitudes = dataMap.get(latRaster);
            } else {
                try {
                    longitudes = RasterUtils.loadGeoData(lonRaster);
                    dataMap.put(lonRaster, longitudes);

                    latitudes = RasterUtils.loadGeoData(latRaster);
                    dataMap.put(latRaster, latitudes);
                } catch (IOException e) {
                    SystemUtils.LOG.warning("error loading geo-data: " + e.getMessage());
                    return null;
                }
            }

            geoRaster = new GeoRaster(longitudes, latitudes, lonVarName, latVarName, rasterWidth, rasterHeight,
                                      resolutionInKm);
        }

        final ForwardCoding forwardCoding = ComponentFactory.getForward(forwardKey);
        final InverseCoding inverseCoding = ComponentFactory.getInverse(inverseKey);
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.valueOf(geoChecksName), geoCRS);
        geoCoding.initialize();
        return geoCoding;
    }

    @Override
    public Item encode(ComponentGeoCoding geoCoding) {
        final String forwardKey = geoCoding.getForwardCoding().getKey();
        final String inverseKey = geoCoding.getInverseCoding().getKey();
        final GeoChecks geoChecks = geoCoding.getGeoChecks();
        final CoordinateReferenceSystem geoCRS = geoCoding.getGeoCRS();
        final GeoRaster geoRaster = geoCoding.getGeoRaster();
        final String lonVarName = geoRaster.getLonVariableName();
        final String latVarName = geoRaster.getLatVariableName();
        final double resolutionKm = geoRaster.getRasterResolutionInKm();
        final double offsetX = geoRaster.getOffsetX();
        final double offsetY = geoRaster.getOffsetY();
        final double subsamplingX = geoRaster.getSubsamplingX();
        final double subsamplingY = geoRaster.getSubsamplingY();

        final Container codingMain = createRootContainer(NAME_COMPONENT_GEO_CODING);
        codingMain.add(new Property<>(NAME_FORWARD_CODING_KEY, forwardKey));
        codingMain.add(new Property<>(NAME_INVERSE_CODING_KEY, inverseKey));
        codingMain.add(new Property<>(NAME_GEO_CHECKS, geoChecks.name()));
        codingMain.add(new Property<>(NAME_GEO_CRS, geoCRS.toWKT()));
        codingMain.add(new Property<>(NAME_LON_VARIABLE_NAME, lonVarName));
        codingMain.add(new Property<>(NAME_LAT_VARIABLE_NAME, latVarName));
        codingMain.add(new Property<>(NAME_RASTER_RESOLUTION_KM, resolutionKm)); // String.valueOf()
        codingMain.add(new Property<>(NAME_OFFSET_X, offsetX)); // String.valueOf()
        codingMain.add(new Property<>(NAME_OFFSET_Y, offsetY)); // String.valueOf()
        codingMain.add(new Property<>(NAME_SUBSAMPLING_X, subsamplingX)); // String.valueOf()
        codingMain.add(new Property<>(NAME_SUBSAMPLING_Y, subsamplingY)); // String.valueOf()

        return codingMain;
    }

    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {
        return new HistoricalDecoder[]{
                new HistoricalDecoder0()
        };
    }

    private static class HistoricalDecoder0 extends DimapHistoricalDecoder {

        @Override
        public boolean canDecode(Item item) {
            return item != null && item.isContainer() && "ComponentGeoCoding".equals(item.getName());
        }

        @Override
        public Item decode(Item item, Product product) {
            final Container container = item.asContainer();
            container.add(new Property<>(KEY_PERSISTENCE_ID, ID_VERSION_1));
            return container;
        }
    }
}
