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

import com.bc.ceres.binding.PropertyContainer;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.Ellipsoid;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.stac.StacItem;
import org.esa.snap.stac.extensions.Proj;
import org.esa.snap.stac.extensions.SNAP;
import org.geotools.referencing.CRS;
import org.jdom2.Element;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;

class StacToSnap {

    private final StacItem stacItem;
    private final Product product;
    private final JSONObject propertiesJSON;
    private final JSONObject snapPropertiesJSON;

    StacToSnap(final StacItem stacItem, final Product product) {
        this.stacItem = stacItem;
        this.product = product;
        this.propertiesJSON = stacItem.getProperties();
        snapPropertiesJSON = propertiesJSON.containsKey(SNAP.snap) ? (JSONObject)propertiesJSON.get(SNAP.snap) : propertiesJSON;
    }

    void addMasks() throws IOException {
        if (snapPropertiesJSON.containsKey(SNAP.masks)) {
            final JSONArray masksArray = (JSONArray)snapPropertiesJSON.get(SNAP.masks);
            for (final Object o : masksArray) {
                final JSONObject maskJSON = (JSONObject)o;

                final String name = (String)maskJSON.get(SNAP.mask_name);
                final int width;
                final int height;
                if (maskJSON.containsKey(SNAP.mask_raster_width) && maskJSON.containsKey(SNAP.mask_raster_height)) {
                    width = ((Long)maskJSON.get(SNAP.mask_raster_width)).intValue();
                    height = ((Long)maskJSON.get(SNAP.mask_raster_height)).intValue();
                } else {
                    width = product.getSceneRasterWidth();
                    height = product.getSceneRasterHeight();
                }
                final Mask mask = new Mask(name, width, height, createImageType(maskJSON));
                mask.setDescription((String)maskJSON.get(SNAP.mask_description));
                mask.setImageTransparency((Double)maskJSON.get(SNAP.mask_transparency));
                setMaskProperties(mask, maskJSON);
                setImageToModelTransform(maskJSON, mask);

                product.getMaskGroup().add(mask);
            }
        }
    }

    private Mask.ImageType createImageType(final JSONObject maskJSON) throws IOException {
        final String type = (String)maskJSON.get(SNAP.type);
        switch (type) {
            case Mask.BandMathsType.TYPE_NAME:
                return Mask.BandMathsType.INSTANCE;
            case Mask.RangeType.TYPE_NAME:
                return Mask.RangeType.INSTANCE;
            case Mask.VectorDataType.TYPE_NAME:
                return Mask.VectorDataType.INSTANCE;
        }
        throw new IOException("Unsupported mask type " + type);
    }

    private void setMaskProperties(final Mask mask, final JSONObject maskJSON) {
        if(maskJSON.containsKey(SNAP.mask_colour)) {
            final JSONObject colorJSON = (JSONObject) maskJSON.get(SNAP.mask_colour);
            final int r = ((Long) colorJSON.get(SNAP.red)).intValue();
            final int g = ((Long) colorJSON.get(SNAP.green)).intValue();
            final int b = ((Long) colorJSON.get(SNAP.blue)).intValue();
            final int a = ((Long) colorJSON.get(SNAP.alpha)).intValue();
            mask.setImageColor(new Color(r, g, b, a));
        }

        final String type = (String)maskJSON.get(SNAP.type);
        if(type.equals(Mask.BandMathsType.TYPE_NAME)) {
            final PropertyContainer imageConfig = mask.getImageConfig();
            imageConfig.setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, maskJSON.get(SNAP.mask_expression));
        } if(type.equals(Mask.VectorDataType.TYPE_NAME)) {
            try {
                final PropertyContainer imageConfig = mask.getImageConfig();
                imageConfig.setValue(Mask.VectorDataType.PROPERTY_NAME_VECTOR_DATA, maskJSON.get(SNAP.mask_vector_data));
            } catch (Exception e) {
                System.out.println("headerreader: " + e.getMessage());
            }
        } else {
            final PropertyContainer imageConfig = mask.getImageConfig();
            Double min = (Double)maskJSON.get(SNAP.mask_min);
            if(min != null) {
                imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, min);
            }
            Double max = (Double)maskJSON.get(SNAP.mask_max);
            if(max != null) {
                imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, max);
            }
            String raster = (String)maskJSON.get(SNAP.mask_raster);
            if(raster != null) {
                imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, raster);
            }
        }
    }

    private static void setImageToModelTransform(final JSONObject bandJSON, RasterDataNode rasterDataNode) {
        if (!rasterDataNode.isSourceImageSet()) {
            final String transform = (String)bandJSON.get(Proj.transform);
            if (transform != null && transform.length() > 0) {
                double[] matrix = StringUtils.toDoubleArray(transform, null);
                rasterDataNode.setImageToModelTransform(new AffineTransform(matrix));
            }
        } else {
            SystemUtils.LOG.warning(String.format("RasterDataNode '%s': can't set image-to-model transform, " +
                    "source image already set", rasterDataNode.getName()));
        }
    }

    void addFlagsCoding() {
        addSampleCoding(product, snapPropertiesJSON,
                SNAP.flag_coding, SNAP.flag, SNAP.flag_name, SNAP.flag_index, SNAP.flag_description);
    }

    void addIndexCoding() {
        addSampleCoding(product, snapPropertiesJSON,
                SNAP.index_coding, SNAP.index, SNAP.index_name, SNAP.index_value, SNAP.index_description);
    }

    private static void addSampleCoding(final Product product, final JSONObject json,
                                 String tagNameSampleCoding,
                                 String tagNameSampleElements,
                                 String tagNameSampleName,
                                 String tagNameSampleValue,
                                 String tagNameSampleDescription) {
        if(json.containsKey(tagNameSampleCoding)) {
            final JSONArray children = (JSONArray) json.get(tagNameSampleCoding);
            for (Object o : children) {
                final JSONObject flagCodingElem = (JSONObject) o;
                final String codingName = (String)flagCodingElem.get(SNAP.name);
                final SampleCoding sampleCoding;
                if (tagNameSampleElements.equals(SNAP.index)) {
                    final IndexCoding indexCoding = new IndexCoding(codingName);
                    product.getIndexCodingGroup().add(indexCoding);
                    sampleCoding = indexCoding;
                } else {
                    final FlagCoding flagCoding = new FlagCoding(codingName);
                    product.getFlagCodingGroup().add(flagCoding);
                    sampleCoding = flagCoding;
                }
                addSamples(tagNameSampleElements, tagNameSampleName, tagNameSampleValue, tagNameSampleDescription,
                        flagCodingElem, sampleCoding);
            }
        }
    }

    private static void addSamples(String tagNameSampleElements, String tagNameSampleName, String tagNameSampleValue,
                            String tagNameSampleDescription,
                            JSONObject sampleCodingElement, SampleCoding sampleCoding) {
        final JSONArray list = (JSONArray)sampleCodingElement.get(tagNameSampleElements);
        for (Object o : list) {
            final JSONObject element = (JSONObject) o;
            final String name = (String)element.get(tagNameSampleName);
            final int value = ((Long)element.get(tagNameSampleValue)).intValue();
            final String description = (String)element.get(tagNameSampleDescription);
            sampleCoding.addSample(name, value, description);
        }
    }

    void addTiePointGrids() {
        if(!snapPropertiesJSON.containsKey(SNAP.tie_point_grids)) {
            return;
        }

        final JSONArray tpgArray = (JSONArray)snapPropertiesJSON.get(SNAP.tie_point_grids);
        for (Object child : tpgArray) {
            final JSONObject tpgJSON = (JSONObject) child;

            final String name = (String) tpgJSON.get(SNAP.tpg_name);
            if(product.containsTiePointGrid(name)) {
                continue;
            }
            final int width = ((Long)tpgJSON.get(SNAP.tpg_ncols)).intValue();
            final int height = ((Long)tpgJSON.get(SNAP.tpg_nrows)).intValue();
            final double offsX = (Double) tpgJSON.get(SNAP.tpg_offset_x);
            final double offsY = (Double) tpgJSON.get(SNAP.tpg_offset_y);
            final double subsX = (Double) tpgJSON.get(SNAP.tpg_step_x);
            final double subsY = (Double) tpgJSON.get(SNAP.tpg_step_y);
            final int discontinuity = ((Long)tpgJSON.get(SNAP.tpg_discontinuity)).intValue();

            final float[] floats = new float[width * height];
            final JSONArray floatsJSON = (JSONArray)tpgJSON.get(SNAP.tpg_data);
            int i = 0;
            for(Object f : floatsJSON) {
                if(f != null) {
                    floats[i] = ((Double) f).floatValue();
                }
                ++i;
            }

            final TiePointGrid tiePointGrid = new TiePointGrid(name, width, height, offsX, offsY, subsX, subsY, floats,
                    discontinuity);

            tiePointGrid.setDescription((String) tpgJSON.get(SNAP.tpg_description));
            tiePointGrid.setUnit((String) tpgJSON.get(SNAP.tpg_physical_unit));

            setImageToModelTransform(tpgJSON, tiePointGrid);
            product.addTiePointGrid(tiePointGrid);
        }
    }

    void addGeoCoding(final Product product) throws Exception {
        if(product.getSceneGeoCoding() != null) {
            return;
        }

        if(propertiesJSON.containsKey(Proj.wkt2) && propertiesJSON.containsKey(Proj.transform)) {
            final String wkt2 = (String) propertiesJSON.get(Proj.wkt2);
            final String transform = (String) propertiesJSON.get(Proj.transform);

            if (transform != null) {
                final String[] parameters = StringUtils.csvToArray(transform);
                double[] matrix = new double[parameters.length];
                for (int i = 0; i < matrix.length; i++) {
                    matrix[i] = Double.parseDouble(parameters[i]);
                }
                final AffineTransform i2m = new AffineTransform(matrix);
                Rectangle imageBounds = new Rectangle(product.getSceneRasterWidth(),
                        product.getSceneRasterHeight());
                try {
                    final CoordinateReferenceSystem crs = CRS.parseWKT(wkt2);
                    CrsGeoCoding geocoding = new CrsGeoCoding(crs, imageBounds, i2m);

                    product.setSceneGeoCoding(geocoding);
                } catch (TransformException e) {
                    SystemUtils.LOG.severe(e.getMessage());
                }
            }
        } else if(snapPropertiesJSON != null && snapPropertiesJSON.containsKey(SNAP.geocoding)) {
            GeoCoding geoCoding = createGeoCoding(product, snapPropertiesJSON);
            if(geoCoding != null) {
                product.setSceneGeoCoding(geoCoding);
            }
        } else if (stacItem.getBoundingBox() != null) {
            JSONArray bbox = stacItem.getBoundingBox();
            GeoCodingSupport.addGeoCoding(product, bbox);
        }
    }

    public static GeoCoding createGeoCoding(final Product product, final JSONObject json) throws IOException {

        if(json.containsKey(SNAP.geocoding)) {
            final JSONObject geocodingJSON = (JSONObject) json.get(SNAP.geocoding);
            if (geocodingJSON.containsKey(SNAP.geocoding_class)) {
                String geocodingClass = (String) geocodingJSON.get(SNAP.geocoding_class);
                switch (geocodingClass) {
                    case "TiePointGeoCoding":
                        final Datum datum = createDatum(geocodingJSON);
                        GeoCoding geoCoding = createGeoCodingFromGeoPositionPoints(product, datum, geocodingJSON);
                        if(geoCoding == null) {
                            throw new IOException("Unhandled GeoCoding");
                        }
                        return geoCoding;
                }
            }
        }

        return null;

//        final JSONArray geoPosElems = (JSONArray)json.get(TAG_GEOCODINGS);
//        List bandInfoElems = null;
////        final Element imageInterpretationElement = null;//todo json.get(SkyWatchProductConstants.TAG_IMAGE_INTERPRETATION);
////        if(imageInterpretationElement != null) {
////            bandInfoElems = imageInterpretationElement.getChildren(SkyWatchProductConstants.TAG_SPECTRAL_BAND_INFO);
////        }
//        if (geoPosElems.size() > 0) {
//            Map<String, GeoCoding> wktToCrsGeocodingMap = new HashMap<>();
//            final GeoCoding[] geoCodings = new GeoCoding[geoPosElems.size()];
//
//            for (int i = 0; i < geoPosElems.size(); i++) {
//                final JSONObject geoPosElem = (JSONObject) geoPosElems.get(i);
//                final JSONObject geocodingJSON = (JSONObject) geoPosElem.get(TAG_GEOCODING);
//                final JSONObject crsElem = (JSONObject) geoPosElem.get(TAG_COORDINATE_REFERENCE_SYSTEM);
//
//                final int bandIndex;
//                if (geoPosElems.size() > 1) {
//                    bandIndex = 0; //todo Integer.parseInt(geoPosElem.getChildText(SkyWatchProductConstants.TAG_BAND_INDEX));
//                } else {
//                    bandIndex = 0;
//                }
//                //Search corresponding bandInfo
//                Element bandInfoElem = null;
//                if(bandInfoElems != null) {
//                    for(Object object : bandInfoElems) {
//                        Element element = (Element) object;
//                        try {
//                            int index = 0; //todo Integer.parseInt(element.getChildText(SkyWatchProductConstants.TAG_BAND_INDEX));
//                            if (index == bandIndex) {
//                                bandInfoElem = element;
//                                break;
//                            }
//                        } catch (Exception e) {
//                            continue;
//                        }
//                    }
//                }
//                if (geocodingJSON != null && geocodingJSON.containsKey(TAG_FORWARD_CODING_KEY)) {
//                    final GeoCoding crsGeoCoding = createComponentGeoCoding(product, geocodingJSON);
//                    geoCodings[bandIndex] = crsGeoCoding;
//                } else if (crsElem != null && crsElem.containsKey(TAG_WKT)) {
//                    final String wkt = (String)crsElem.get(TAG_WKT);
//                    final String key = wkt + " " + (String)geocodingJSON.get(TAG_IMAGE_TO_MODEL_TRANSFORM);
//                    if (wktToCrsGeocodingMap.containsKey(key)) {
//                        geoCodings[bandIndex] = wktToCrsGeocodingMap.get(key);
//                    } else {
//                        final GeoCoding crsGeoCoding = createCrsGeoCoding(product, geocodingJSON, wkt, bandInfoElem);
//                        geoCodings[bandIndex] = crsGeoCoding;
//                        wktToCrsGeocodingMap.put(key, crsGeoCoding);
//                    }
//                } else if (geocodingJSON.get(TAG_SIMPLIFIED_LOCATION_MODEL) != null &&
//                        geocodingJSON.get(TAG_GEOPOSITION_INSERT) != null) {
//                    geoCodings[bandIndex] = null;//todo createFXYGeoCoding(datum, geocodingJSON);
//                } else if (geocodingJSON.get(TAG_SEARCH_RADIUS) != null &&
//                        geocodingJSON.get(TAG_LATITUDE_BAND) != null) {
//                    final Datum datum = createDatum(json);
//                    geoCodings[bandIndex] = createPixelGeoCoding(product, datum, geocodingJSON);
//                } else {
//                    if (geocodingJSON.containsKey(TAG_GEOPOSITION_POINTS)) {
//                        final Datum datum = createDatum(json);
//                        geoCodings[bandIndex] = createGeoCodingFromGeoPositionPointsElement(product,
//                                datum, (JSONObject)geocodingJSON.get(TAG_GEOPOSITION_POINTS));
//                    } else {
//                        throw new IOException("Unhandled GeoCoding");
////                        final DimapPersistable persistable = DimapPersistence.getPersistable(geoPosElem);
////                        if (persistable != null) {
////                            geoCodings[bandIndex] = (GeoCoding) persistable.createObjectFromXml(geoPosElem, product);
////                        }
//                    }
//                }
//            }
//            return geoCodings;
//        }
//
//        throw new IOException("Unhandled geocoding");
    }

    private static GeoCoding createGeoCodingFromGeoPositionPoints(final Product product, final Datum datum,
                                                                         final JSONObject json) {

        // 1. try creating a tie-point geo-coding
        final String latName = (String)json.get(SNAP.tpg_lat);
        final String lonName = (String)json.get(SNAP.tpg_lon);
        if (latName != null && lonName != null) {
            final TiePointGrid latGrid = product.getTiePointGrid(latName);
            final TiePointGrid lonGrid = product.getTiePointGrid(lonName);
            try {
                if (latGrid != null && lonGrid != null) {
                    return new TiePointGeoCoding(latGrid, lonGrid, datum);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        // 2. try creating a GCP geo-coding
//        final String methodName = (String)json.get(TAG_INTERPOLATION_METHOD);
//        if (methodName != null) {
//            final GcpGeoCoding.Method method = GcpGeoCoding.Method.valueOf(GcpGeoCoding.Method.class, methodName);
//            final PlacemarkGroup gcpGroup = product.getGcpGroup();
//            final Placemark[] placemarks = gcpGroup.toArray(new Placemark[gcpGroup.getNodeCount()]);
//            try {
//                return new GcpGeoCoding(method, placemarks,
//                        product.getSceneRasterWidth(),
//                        product.getSceneRasterHeight(),
//                        datum);
//            } catch (Exception e) {
//                // ignore
//            }
//        }
        return null;
    }

    private static GeoCoding createCrsGeoCoding(Product product, JSONObject geoPositionElem, String wktElem) {
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(wktElem);
            final String i2mElem = (String)geoPositionElem.get(Proj.transform);
            if (i2mElem != null) {
                final String[] parameters = StringUtils.csvToArray(i2mElem);
                double[] matrix = new double[parameters.length];
                for (int i = 0; i < matrix.length; i++) {
                    matrix[i] = Double.valueOf(parameters[i]);
                }
                final AffineTransform i2m = new AffineTransform(matrix);
                Rectangle imageBounds = new Rectangle(product.getSceneRasterWidth(),
                        product.getSceneRasterHeight());
                try {
                    final CrsGeoCoding geoCoding = new CrsGeoCoding(crs, imageBounds, i2m);
                    return geoCoding;
                } catch (TransformException e) {
                    Debug.trace(e);
                }
            }
        } catch (FactoryException e) {
            Debug.trace(e);
        }
        return null;
    }

    private static GeoCoding createCrsGeoCoding(Product product, JSONObject geoPositionElem, String wktElem, Element bandInfoElem) {
        if(bandInfoElem == null) {
            return createCrsGeoCoding(product, geoPositionElem, wktElem);
        }
        //       try {
//            final CoordinateReferenceSystem crs = CRS.parseWKT(wktElem.getTextTrim());
//            final Element i2mElem = geoPositionElem.getChild(TAG_IMAGE_TO_MODEL_TRANSFORM);
//            final Element widthElem = bandInfoElem.getChild(TAG_BAND_RASTER_WIDTH);
//            final Element heightElem = bandInfoElem.getChild(TAG_BAND_RASTER_HEIGHT);
//            int height = product.getSceneRasterHeight();
//            int width = product.getSceneRasterWidth();
//            if(heightElem != null) {
//                try {
//                    height = Integer.valueOf(heightElem.getText());
//                } catch (NumberFormatException e) {
//                    //do nothing, product height will be used
//                }
//            }
//            if(widthElem != null) {
//                try {
//                    width = Integer.valueOf(widthElem.getText());
//                } catch (NumberFormatException e) {
//                    //do nothing, product width will be used
//                }
//            }
//            if (i2mElem != null) {
//                final String[] parameters = StringUtils.csvToArray(i2mElem.getTextTrim());
//                double[] matrix = new double[parameters.length];
//                for (int i = 0; i < matrix.length; i++) {
//                    matrix[i] = Double.valueOf(parameters[i]);
//                }
//                final AffineTransform i2m = new AffineTransform(matrix);
//                Rectangle imageBounds = new Rectangle(width,
//                        height);
//                try {
//                    final CrsGeoCoding geoCoding = new CrsGeoCoding(crs, imageBounds, i2m);
//                    return geoCoding;
//                } catch (TransformException e) {
//                    Debug.trace(e);
//                }
//            }
        //       } catch (FactoryException e) {
        //           Debug.trace(e);
        //       }
        return null;
    }

//    private static GeoCoding createComponentGeoCoding(Product product, JSONObject geoCodingJSON) throws IOException {
//
//        final String forwardKey = (String)geoCodingJSON.get(TAG_FORWARD_CODING_KEY);
//        final String inverseKey = (String)geoCodingJSON.get(TAG_INVERSE_CODING_KEY);
//        final String geoChecksName = (String)geoCodingJSON.get(TAG_GEO_CHECKS);
//        final String geoCrsWKT = (String)geoCodingJSON.get(TAG_GEO_CRS);
//        final String lonVarName = (String)geoCodingJSON.get(TAG_LON_VARIABLE_NAME);
//        final String latVarName = (String)geoCodingJSON.get(TAG_LAT_VARIABLE_NAME);
//        final String resolutionKmStr = (String)geoCodingJSON.get(TAG_RASTER_RESOLUTION_KM);
//
//        Double resolutionInKm = Double.parseDouble(resolutionKmStr);
//
//        GeoChecks.valueOf(geoChecksName);
//        CoordinateReferenceSystem geoCRS;
//        try {
//            geoCRS = CRS.parseWKT(geoCrsWKT);
//        } catch (Exception e) {
//            throw new IOException(e);
//        }
//
//        final RasterDataNode lonRaster = product.getRasterDataNode(lonVarName);
//        final RasterDataNode latRaster = product.getRasterDataNode(latVarName);
//
//        final GeoRaster geoRaster;
//        if (lonRaster instanceof TiePointGrid) {
//            final int sceneWidth = product.getSceneRasterWidth();
//            final int sceneHeight = product.getSceneRasterHeight();
//            final TiePointGrid lonTPG = (TiePointGrid) lonRaster;
//            final TiePointGrid latTPG = (TiePointGrid) latRaster;
//
//            final int gridWidth = lonTPG.getGridWidth();
//            final int gridHeight = lonTPG.getGridHeight();
//
//            final float[] lons = (float[]) lonTPG.getGridData().getElems();
//            final double[] longitudes = IntStream.range(0, lons.length).mapToDouble(i -> lons[i]).toArray();
//
//            final float[] lats = (float[]) latTPG.getGridData().getElems();
//            final double[] latitudes = IntStream.range(0, lats.length).mapToDouble(i -> lats[i]).toArray();
//
//            final double offsetX = lonTPG.getOffsetX();
//            final double offsetY = lonTPG.getOffsetY();
//            final double subsamplingX = lonTPG.getSubSamplingX();
//            final double subsamplingY = lonTPG.getSubSamplingY();
//
//            geoRaster = new GeoRaster(longitudes, latitudes, lonVarName, latVarName, gridWidth, gridHeight,
//                    sceneWidth, sceneHeight, resolutionInKm,
//                    offsetX, offsetY, subsamplingX, subsamplingY);
//        } else {
//            final int rasterWidth = lonRaster.getRasterWidth();
//            final int rasterHeight = lonRaster.getRasterHeight();
//            final int size = rasterWidth * rasterHeight;
//            final double[] longitudes = lonRaster.getSourceImage().getImage(0).getData()
//                    .getPixels(0, 0, rasterWidth, rasterHeight, new double[size]);
//            final double[] latitudes = latRaster.getSourceImage().getImage(0).getData()
//                    .getPixels(0, 0, rasterWidth, rasterHeight, new double[size]);
//            geoRaster = new GeoRaster(longitudes, latitudes, lonVarName, latVarName, rasterWidth, rasterHeight,
//                    resolutionInKm);
//        }
//
//        final ForwardCoding forwardCoding = ComponentFactory.getForward(forwardKey);
//        final InverseCoding inverseCoding = ComponentFactory.getInverse(inverseKey);
//        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.valueOf(geoChecksName), geoCRS);
//        geoCoding.initialize();
//        return geoCoding;
//    }
//
//    static GeoCoding createPixelGeoCoding(Product product, Datum datum, JSONObject geoPosElem) {
//        final Band latBand = product.getBand((String)geoPosElem.get(TAG_LATITUDE_BAND));
//        final Band lonBand = product.getBand((String)geoPosElem.get(TAG_LONGITUDE_BAND));
//        int searchRadius = ((Long)geoPosElem.get(TAG_SEARCH_RADIUS)).intValue();
//        if (searchRadius == 0) {
//            searchRadius = 6;
//        }
//        String validMask = null;
//        if (geoPosElem.containsKey(TAG_VALID_MASK_EXPRESSION)) {
//            validMask = (String)geoPosElem.get(TAG_VALID_MASK_EXPRESSION);
//        }
//        if (geoPosElem.containsKey(TAG_PIXEL_POSITION_ESTIMATOR)) {
//            //   final Content posEstimatorContent = posEstimatorElement.detach();
//            //   final Document dom = new Document();
//            //   dom.addContent(posEstimatorContent);
//            //todo  product.setSceneGeoCoding(createGeoCoding(dom, product)[0]);
//        }
//
//        return GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, validMask, searchRadius);
//    }

//    private static FXYGeoCoding createFXYGeoCoding(Datum datum, Element geoPosElem) {
//        final Element geoPosInsertElem = geoPosElem.getChild(SkyWatchProductConstants.TAG_GEOPOSITION_INSERT);
//
//        final String ulxString = geoPosInsertElem.getChildTextTrim(SkyWatchProductConstants.TAG_ULX_MAP);
//        final float ulX = Float.parseFloat(ulxString);
//        final String ulyString = geoPosInsertElem.getChildTextTrim(SkyWatchProductConstants.TAG_ULY_MAP);
//        final float ulY = Float.parseFloat(ulyString);
//        final String xDimString = geoPosInsertElem.getChildTextTrim(SkyWatchProductConstants.TAG_X_DIM);
//        final float xDim = Float.parseFloat(xDimString);
//        final String yDimString = geoPosInsertElem.getChildTextTrim(SkyWatchProductConstants.TAG_Y_DIM);
//        final float yDim = Float.parseFloat(yDimString);
//
//        final Element simplifiedLMElem = geoPosElem.getChild(
//                SkyWatchProductConstants.TAG_SIMPLIFIED_LOCATION_MODEL);
//        final Element directLMElem = simplifiedLMElem.getChild(SkyWatchProductConstants.TAG_DIRECT_LOCATION_MODEL);
//        final String dlmOrderString = directLMElem.getAttributeValue(SkyWatchProductConstants.ATTRIB_ORDER);
//        final int dlmOrder = Integer.parseInt(dlmOrderString);
//        final Element lcListElem = directLMElem.getChild(SkyWatchProductConstants.TAG_LC_LIST);
//        final List lcElems = lcListElem.getChildren(SkyWatchProductConstants.TAG_LC);
//        final double[] lambdaCoeffs = readCoefficients(lcElems);
//        final Element pcListElem = directLMElem.getChild(SkyWatchProductConstants.TAG_PC_LIST);
//        final List pcElems = pcListElem.getChildren(SkyWatchProductConstants.TAG_PC);
//        final double[] phiCoeffs = readCoefficients(pcElems);
//
//        final Element reverseLMElem = simplifiedLMElem.getChild(
//                SkyWatchProductConstants.TAG_REVERSE_LOCATION_MODEL);
//        final String rlmOrderString = reverseLMElem.getAttributeValue(SkyWatchProductConstants.ATTRIB_ORDER);
//        final int rlmOrder = Integer.parseInt(rlmOrderString);
//        final Element icListElem = reverseLMElem.getChild(SkyWatchProductConstants.TAG_IC_LIST);
//        final List icElems = icListElem.getChildren(SkyWatchProductConstants.TAG_IC);
//        final double[] xCoeffs = readCoefficients(icElems);
//        final Element jcListElem = reverseLMElem.getChild(SkyWatchProductConstants.TAG_JC_LIST);
//        final List jcElems = jcListElem.getChildren(SkyWatchProductConstants.TAG_JC);
//        final double[] yCoeffs = readCoefficients(jcElems);
//
//        final FXYSum lambdaSum = FXYSum.createFXYSum(dlmOrder, lambdaCoeffs);
//        final FXYSum phiSum = FXYSum.createFXYSum(dlmOrder, phiCoeffs);
//        final FXYSum xSum = FXYSum.createFXYSum(rlmOrder, xCoeffs);
//        final FXYSum ySum = FXYSum.createFXYSum(rlmOrder, yCoeffs);
//
//        return new FXYGeoCoding(ulX, ulY, xDim, yDim, xSum, ySum, phiSum, lambdaSum,
//                datum);
//    }
//
//    private static double[] readCoefficients(final List elementList) {
//        final double[] coeffs = new double[elementList.size()];
//        for (Object anElement : elementList) {
//            final Element element = (Element) anElement;
//            final String indexString = element.getAttribute(SkyWatchProductConstants.ATTRIB_INDEX).getValue();
//            final int index = Integer.parseInt(indexString);
//            coeffs[index] = Double.parseDouble(element.getTextTrim());
//        }
//        return coeffs;
//    }

    private static Datum createDatum(final JSONObject json) {

        final JSONObject ellipsoidElem = (JSONObject) json.get(SNAP.datum_ellipsoid);
        if (ellipsoidElem != null) {
            final String ellipsoidName = (String) ellipsoidElem.get(SNAP.datum_ellipsoid_name);
            final double majorAxis = (Double) ellipsoidElem.get(SNAP.datum_ellipsoid_maj_axis);
            final double minorAxis = (Double) ellipsoidElem.get(SNAP.datum_ellipsoid_min_axis);

            final Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName, minorAxis, majorAxis);
            final String datumName = (String) json.get(SNAP.datum_horizontal_datum_name);
            return new Datum(datumName, ellipsoid, 0, 0, 0);
        }
        return Datum.WGS_84;
    }
}
