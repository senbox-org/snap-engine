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
import org.esa.snap.core.dataio.dimap.spi.DimapPersistable;
import org.esa.snap.core.dataio.dimap.spi.DimapPersistence;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.Ellipsoid;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.stac.StacItem;
import org.esa.snap.stac.extensions.DateTime;
import org.esa.snap.stac.extensions.EO;
import org.esa.snap.stac.extensions.Instrument;
import org.esa.snap.stac.extensions.Proj;
import org.esa.snap.stac.extensions.SAR;
import org.esa.snap.stac.extensions.SNAP;
import org.esa.snap.stac.extensions.Sat;
import org.jdom2.Element;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;

@SuppressWarnings("unchecked")
class SnapToStac {

    private final StacItem stacItem;
    private final Product product;
    private final JSONObject propertiesJSON;
    private final JSONObject snapPropertiesJSON;

    SnapToStac(final StacItem stacItem, final Product product) {
        this.stacItem = stacItem;
        this.product = product;
        this.propertiesJSON = stacItem.getProperties();
        snapPropertiesJSON = new JSONObject();
        propertiesJSON.put(SNAP.snap, snapPropertiesJSON);
    }

    void writeProjection() throws IOException {

        final JSONArray shape = new JSONArray();
        shape.add(product.getSceneRasterHeight());
        shape.add(product.getSceneRasterWidth());

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if(geoCoding != null) {
            final JSONObject geoCodingJSON = new JSONObject();
            snapPropertiesJSON.put(SNAP.geocoding, geoCodingJSON);

            geoCodingJSON.put(SNAP.geocoding_class, geoCoding.getClass().getSimpleName());

            if(geoCoding instanceof ComponentGeoCoding) {
                throw new IOException("GeoCoding " + geoCoding + " not supported");
                //writeGeoCoding((ComponentGeoCoding) geoCoding, geoCodingJSON);
            } else if (geoCoding instanceof CrsGeoCoding) {
                writeGeoCoding((CrsGeoCoding) geoCoding);
            } else if (geoCoding instanceof TiePointGeoCoding) {
                writeGeoCoding((TiePointGeoCoding) geoCoding, geoCodingJSON);
            } else if (geoCoding instanceof BasicPixelGeoCoding) {
                throw new IOException("GeoCoding " + geoCoding + " not supported");
                //writeGeoCoding((BasicPixelGeoCoding) geoCoding, geoCodingJSON);
            } else if (geoCoding instanceof FXYGeoCoding) {
                throw new IOException("GeoCoding " + geoCoding + " not supported");
                //writeGeoCoding((FXYGeoCoding) geoCoding, geoCodingJSON);
            } else if (geoCoding instanceof GcpGeoCoding) {
                throw new IOException("GeoCoding " + geoCoding + " not supported");
                //writeGeoCoding((GcpGeoCoding) geoCoding, geoCodingJSON);
            } else {
                final DimapPersistable persistable = DimapPersistence.getPersistable(geoCoding);
                if (persistable != null) {
                    final Element element = persistable.createXmlFromObject(geoCoding);
                    throw new IOException("GeoCoding persistable " + element.getName() + " not supported");
                } else {
                    throw new IOException("GeoCoding " + geoCoding + " not supported");
                }
            }
        }

        propertiesJSON.put(Proj.shape, shape);
    }

    private void writeGeoCoding(final CrsGeoCoding geoCoding) {
        final CoordinateReferenceSystem crs = geoCoding.getMapCRS();
        final double[] matrix = new double[6];
        final MathTransform transform = geoCoding.getImageToMapTransform();
        if (transform instanceof AffineTransform) {
            ((AffineTransform) transform).getMatrix(matrix);
        }

        while(crs.getIdentifiers().iterator().hasNext()) {
            ReferenceIdentifier id = crs.getIdentifiers().iterator().next();
            if (id.getCodeSpace().equals("EPSG")) {
                propertiesJSON.put(Proj.epsg, Integer.parseInt(id.getCode()));
                break;
            }
        }
        propertiesJSON.put(Proj.wkt2, crs.toString());
        propertiesJSON.put(Proj.transform, StringUtils.arrayToCsv(matrix));
    }

//    private void writeGeoCoding(final GcpGeoCoding gcpPointGeoCoding, final JSONObject json) throws IOException {
//
//        final JSONObject coordSystemJSON = new JSONObject();
//        json.put(TAG_COORDINATE_REFERENCE_SYSTEM, coordSystemJSON);
//        writeDatum(gcpPointGeoCoding.getDatum(), coordSystemJSON);
//
//        final JSONObject geoCodingJSON = new JSONObject();
//        json.put(TAG_GEOCODING, geoCodingJSON);
//
//        geoCodingJSON.put(TAG_INTERPOLATION_METHOD, gcpPointGeoCoding.getMethod().name());
//        final GeoCoding originalGeoCoding = gcpPointGeoCoding.getOriginalGeoCoding();
//        if (!(originalGeoCoding == null || originalGeoCoding instanceof GcpGeoCoding)) {
//            final JSONObject origGeoCodingJSON = new JSONObject();
//            geoCodingJSON.put(DimapProductConstants.TAG_ORIGINAL_GEOCODING, origGeoCodingJSON);
//
//            writeGeoCoding(originalGeoCoding, origGeoCodingJSON);
//        }
//    }

    private void writeGeoCoding(final TiePointGeoCoding tiePointGeoCoding, final JSONObject geoCodingJSON) {

        writeDatum(tiePointGeoCoding.getDatum(), geoCodingJSON);

        final String latGridName = tiePointGeoCoding.getLatGrid().getName();
        final String lonGridName = tiePointGeoCoding.getLonGrid().getName();
        if (latGridName == null || lonGridName == null) {
            return;
        }

        geoCodingJSON.put(SNAP.tpg_lat, latGridName);
        geoCodingJSON.put(SNAP.tpg_lon, lonGridName);
    }

//    private void writeGeoCoding(final ComponentGeoCoding geoCoding, final JSONObject json) {
//
//        final JSONObject geoCodingJSON = new JSONObject();
//        json.put(TAG_GEOCODING, geoCodingJSON);
//
//        final GeoChecks geoChecks = geoCoding.getGeoChecks();
//        final CoordinateReferenceSystem geoCRS = geoCoding.getGeoCRS();
//        final GeoRaster geoRaster = geoCoding.getGeoRaster();
//
//        geoCodingJSON.put(TAG_FORWARD_CODING_KEY, geoCoding.getForwardCoding().getKey());
//        geoCodingJSON.put(TAG_INVERSE_CODING_KEY, geoCoding.getInverseCoding().getKey());
//        geoCodingJSON.put(TAG_GEO_CHECKS, geoChecks.name());
//        geoCodingJSON.put(TAG_GEO_CRS, geoCRS.toWKT());
//        geoCodingJSON.put(TAG_LON_VARIABLE_NAME, geoRaster.getLonVariableName());
//        geoCodingJSON.put(TAG_LAT_VARIABLE_NAME, geoRaster.getLatVariableName());
//        geoCodingJSON.put(TAG_RASTER_RESOLUTION_KM, String.valueOf(geoRaster.getRasterResolutionInKm()));
//        geoCodingJSON.put(TAG_OFFSET_X, String.valueOf(geoRaster.getOffsetX()));
//        geoCodingJSON.put(TAG_OFFSET_Y, String.valueOf(geoRaster.getOffsetY()));
//        geoCodingJSON.put(TAG_SUBSAMPLING_X, String.valueOf(geoRaster.getSubsamplingX()));
//        geoCodingJSON.put(TAG_SUBSAMPLING_Y, String.valueOf(geoRaster.getSubsamplingY()));
//    }

    private static void writeDatum(final Datum datum, final JSONObject json) {

        json.put(SNAP.datum_horizontal_datum_name, datum.getName());

        final JSONObject ellipsoidJSON = new JSONObject();
        json.put(SNAP.datum_ellipsoid, ellipsoidJSON);

        final Ellipsoid ellipsoid = datum.getEllipsoid();
        ellipsoidJSON.put(SNAP.datum_ellipsoid_name, ellipsoid.getName());

        ellipsoidJSON.put(SNAP.datum_ellipsoid_maj_axis, ellipsoid.getSemiMajor());
        ellipsoidJSON.put(SNAP.datum_ellipsoid_min_axis, ellipsoid.getSemiMinor());
    }

    void writeTiePointGrids() {

        final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
        if (tiePointGrids != null && tiePointGrids.length > 0) {
            final JSONArray tpgArray = new JSONArray();
            snapPropertiesJSON.put(SNAP.tie_point_grids, tpgArray);

            for (TiePointGrid tpg : tiePointGrids) {
                final JSONObject tpgjson = new JSONObject();
                tpgjson.put(SNAP.tpg_name, tpg.getName());
                tpgjson.put(SNAP.tpg_physical_unit, tpg.getUnit());
                tpgjson.put(SNAP.tpg_data_type, ProductData.getTypeString(tpg.getDataType()));
                tpgjson.put(SNAP.tpg_ncols, tpg.getGridWidth());
                tpgjson.put(SNAP.tpg_nrows, tpg.getGridHeight());
                tpgjson.put(SNAP.tpg_step_x, tpg.getSubSamplingX());
                tpgjson.put(SNAP.tpg_step_y, tpg.getSubSamplingY());
                tpgjson.put(SNAP.tpg_offset_x, tpg.getOffsetX());
                tpgjson.put(SNAP.tpg_offset_y, tpg.getOffsetY());
                tpgjson.put(SNAP.tpg_discontinuity, tpg.getDiscontinuity());
                tpgjson.put(SNAP.tpg_description, tpg.getDescription());

                final float[] srcTiePoints = tpg.getTiePoints();
                final JSONArray dataArray = new JSONArray();
                for (float f : srcTiePoints) {
                    dataArray.add(f);
                }
                tpgjson.put(SNAP.tpg_data, dataArray);

                writeImageToModelTransform(tpg, tpgjson);

                tpgArray.add(tpgjson);
            }
        }
    }

    void writeFlagCoding() {

        final SampleCoding[] a = product.getFlagCodingGroup().toArray(new FlagCoding[0]);
        if(a.length > 0) {
            final JSONArray flagCodingArray = new JSONArray();
            snapPropertiesJSON.put(SNAP.flag_coding, flagCodingArray);

            writeSampleCodings(flagCodingArray, a,
                    SNAP.flag, SNAP.flag_name, SNAP.flag_index, SNAP.flag_description);
        }
    }

    void writeIndexCoding() {

        final SampleCoding[] a = product.getIndexCodingGroup().toArray(new IndexCoding[0]);
        if(a.length > 0) {
            final JSONArray indexCodingArray = new JSONArray();
            snapPropertiesJSON.put(SNAP.index_coding, indexCodingArray);

            writeSampleCodings(indexCodingArray, a,
                    SNAP.index, SNAP.index_name, SNAP.index_value, SNAP.index_description);
        }
    }

    private static void writeSampleCodings(final JSONArray jsonArray, SampleCoding[] a, String tagFlag,
                                    String tagName, String tagIndex, String tagDescription) {
        for (SampleCoding sampleCoding : a) {
            final JSONObject codingJSON = new JSONObject();
            jsonArray.add(codingJSON);
            codingJSON.put(SNAP.name, sampleCoding.getName());

            final JSONArray attributesArray = new JSONArray();
            codingJSON.put(tagFlag, attributesArray);

            final String[] names = sampleCoding.getAttributeNames();
            for (String name : names) {
                final MetadataAttribute attribute = sampleCoding.getAttribute(name);
                final JSONObject attrbJSON = new JSONObject();
                attributesArray.add(attrbJSON);

                attrbJSON.put(tagName, attribute.getName());
                attrbJSON.put(tagIndex, attribute.getData().getElemInt());
                attrbJSON.put(tagDescription, attribute.getDescription());
            }
        }
    }

    private static void writeImageToModelTransform(final RasterDataNode rasterDataNode, final JSONObject bandJSON) {
        final AffineTransform imageToModelTransform = rasterDataNode.getImageToModelTransform();
        if (!imageToModelTransform.isIdentity()) {
            final double[] matrix = new double[6];
            imageToModelTransform.getMatrix(matrix);
            bandJSON.put(Proj.transform, StringUtils.arrayToCsv(matrix));
        }
    }

    void writeBoundingBox() {

        final JSONArray bboxArray = new JSONArray();
        stacItem.getJSON().put(StacItem.BBOX, bboxArray);

        final BoundingBox box = GeoCodingSupport.getBoundingBox(product);
        bboxArray.add(box.getMaxX());
        bboxArray.add(box.getMaxY());
        bboxArray.add(box.getMinX());
        bboxArray.add(box.getMinY());
    }

    void writeTimes() {

        final String startTime = DateTime.getFormattedTime(product.getStartTime());
        final String endTime = DateTime.getFormattedTime(product.getEndTime());
        propertiesJSON.put(DateTime.datetime, startTime);
        propertiesJSON.put(DateTime.start_datetime, startTime);
        propertiesJSON.put(DateTime.end_datetime, endTime);
        propertiesJSON.put(DateTime.created, DateTime.getNowUTC());
    }

    void writeBands() {

        final JSONArray bandsArray = new JSONArray();
        propertiesJSON.put(EO.bands, bandsArray);

        for (Band band : product.getBands()) {
            if (!(band instanceof FilterBand)) {
                bandsArray.add(EO.writeBand(band));
            }
        }
    }

    void writeCoordinates() {

        final JSONObject geometry = new JSONObject();
        stacItem.getJSON().put(StacItem.GEOMETRY, geometry);
        geometry.put(StacItem.TYPE, "Polygon");

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding != null) {
            final GeoPos ul = geoCoding.getGeoPos(new PixelPos(0, 0), null);
            final GeoPos ur = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, 0), null);
            final GeoPos lr = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, product.getSceneRasterHeight() - 1), null);
            final GeoPos ll = geoCoding.getGeoPos(new PixelPos(0, product.getSceneRasterHeight() - 1), null);

            final JSONArray cornerArray = new JSONArray();
            cornerArray.add(createCoord(ul.lon, ul.lat));
            cornerArray.add(createCoord(ur.lon, ur.lat));
            cornerArray.add(createCoord(lr.lon, lr.lat));
            cornerArray.add(createCoord(ll.lon, ll.lat));
            cornerArray.add(createCoord(ul.lon, ul.lat));

            final JSONArray polygonArray = new JSONArray();
            polygonArray.add(cornerArray);
            geometry.put(StacItem.COORDINATES, polygonArray);
        }
    }

    private static JSONArray createCoord(final double lon, final double lat) {
        final JSONArray coordArray = new JSONArray();
        coordArray.add(lon);
        coordArray.add(lat);
        return coordArray;
    }

    void writeMasks() {
        final JSONArray masksArray = new JSONArray();

        final Mask[] masks = product.getMaskGroup().toArray(new Mask[product.getMaskGroup().getNodeCount()]);
        for (final Mask mask : masks) {
            final DimapPersistable persistable = DimapPersistence.getPersistable(mask);
            if (persistable != null) {

                final JSONObject maskJSON = new JSONObject();
                masksArray.add(maskJSON);

                maskJSON.put(SNAP.type, mask.getImageType().getName());
                maskJSON.put(SNAP.mask_name, mask.getName());
                maskJSON.put(SNAP.mask_raster_width, mask.getRasterWidth());
                maskJSON.put(SNAP.mask_raster_height, mask.getRasterHeight());
                maskJSON.put(SNAP.mask_description, mask.getDescription());

                addImageConfigElements(maskJSON, mask);
                writeImageToModelTransform(mask, maskJSON);
                configureElement(maskJSON, mask);
            }
        }

        snapPropertiesJSON.put(SNAP.masks, masksArray);
    }

    private static void addImageConfigElements(final JSONObject maskJSON, Mask mask) {
        final JSONObject colorElement = new JSONObject();
        final PropertyContainer config = mask.getImageConfig();
        final Color color = config.getValue(SNAP.mask_colour);
        if(color != null) {
            colorElement.put(SNAP.red, color.getRed());
            colorElement.put(SNAP.green, color.getGreen());
            colorElement.put(SNAP.blue, color.getBlue());
            colorElement.put(SNAP.alpha, color.getAlpha());
            maskJSON.put(SNAP.mask_colour, colorElement);
        }
        final Object transparencyValue = config.getValue(Mask.ImageType.PROPERTY_NAME_TRANSPARENCY);
        maskJSON.put(SNAP.mask_transparency, transparencyValue);
    }

    private static void configureElement(final JSONObject maskJSON, Mask mask) {
        if(mask.getImageType() instanceof Mask.BandMathsType) {
            maskJSON.put(SNAP.mask_expression, mask.getImageConfig().getValue(
                    Mask.BandMathsType.PROPERTY_NAME_EXPRESSION).toString());
        } else if(mask.getImageType() instanceof Mask.VectorDataType) {
            VectorDataNode vectorData = mask.getImageConfig().getValue(
                    Mask.VectorDataType.PROPERTY_NAME_VECTOR_DATA);
            maskJSON.put(SNAP.mask_vector_data, vectorData.getName());
        } else {
            final PropertyContainer config = mask.getImageConfig();
            maskJSON.put(SNAP.mask_min, config.getValue(SNAP.mask_min));
            maskJSON.put(SNAP.mask_max, config.getValue(SNAP.mask_max));
            if(config.getProperty(SNAP.mask_raster) != null) {
                maskJSON.put(SNAP.mask_raster, String.valueOf(config.getValue(SNAP.mask_raster)));
            }
        }
    }

    void writeMetadata() {

        if (product.getQuicklookBandName() != null) {
            propertiesJSON.put(SNAP.quicklook_band_name, product.getQuicklookBandName());
        }

        if(!AbstractMetadata.hasAbstractedMetadata(product)) {
            return;
        }
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if(absRoot != null) {
            if (exist(absRoot, AbstractMetadata.MISSION)) {
                propertiesJSON.put(Instrument.mission, absRoot.getAttributeString(AbstractMetadata.MISSION));
            }
            if (exist(absRoot, AbstractMetadata.ACQUISITION_MODE)) {
                propertiesJSON.put(Instrument.instrument_mode, absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE));
            }
            if (exist(absRoot, "instrument")) {
                JSONArray instruments = new JSONArray();
                instruments.add(absRoot.getAttributeString("instrument"));
                propertiesJSON.put(Instrument.instruments, instruments);
            }
            if (exist(absRoot, AbstractMetadata.REL_ORBIT)) {
                propertiesJSON.put(Sat.relative_orbit, absRoot.getAttributeInt(AbstractMetadata.REL_ORBIT));
            }
            if (exist(absRoot, AbstractMetadata.ABS_ORBIT)) {
                propertiesJSON.put(Sat.absolute_orbit, absRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT));
            }
            if (exist(absRoot, AbstractMetadata.PASS)) {
                propertiesJSON.put(Sat.orbit_state, absRoot.getAttributeString(AbstractMetadata.PASS).toLowerCase());
            }

            final InputProductValidator inputProductValidator = new InputProductValidator(product);
            final boolean isSAR = inputProductValidator.isSARProduct();
            if (isSAR) {
                writeSARProperties(absRoot);
            } else {
                writeOpticalProperties(absRoot);
            }
        }
    }

    private static boolean exist(final MetadataElement elem, final String tag) {
        if (elem.containsAttribute(tag)) {
            final MetadataAttribute attribute = elem.getAttribute(tag);
            final int dataType = attribute.getDataType();
            if (dataType == ProductData.TYPE_ASCII) {
                return !attribute.getData().getElemString().equals(AbstractMetadata.NO_METADATA_STRING);
            } else {
                return attribute.getData().getElemDouble() != AbstractMetadata.NO_METADATA;
            }
        }
        return false;
    }

    private void writeSARProperties(final MetadataElement absRoot) {

        stacItem.addKeywords(SAR.KeyWords.sar);
        stacItem.addExtension(SAR.schema);

        if (exist(absRoot, AbstractMetadata.PRODUCT_TYPE)) {
            propertiesJSON.put(SAR.product_type, absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE));
        }
        if (exist(absRoot, AbstractMetadata.ACQUISITION_MODE)) {
            propertiesJSON.put(SAR.instrument_mode, absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE));
        }
        if (exist(absRoot, AbstractMetadata.radar_frequency)) {
            propertiesJSON.put(SAR.center_frequency, absRoot.getAttributeDouble(AbstractMetadata.radar_frequency));
        }
        if (exist(absRoot, AbstractMetadata.range_spacing)) {
            propertiesJSON.put(SAR.pixel_spacing_range, absRoot.getAttributeDouble(AbstractMetadata.range_spacing));
        }
        if (exist(absRoot, AbstractMetadata.azimuth_spacing)) {
            propertiesJSON.put(SAR.pixel_spacing_azimuth, absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing));
        }
        if (exist(absRoot, AbstractMetadata.range_looks)) {
            propertiesJSON.put(SAR.looks_range, absRoot.getAttributeDouble(AbstractMetadata.range_looks));
        }
        if (exist(absRoot, AbstractMetadata.azimuth_looks)) {
            propertiesJSON.put(SAR.looks_azimuth, absRoot.getAttributeDouble(AbstractMetadata.azimuth_looks));
        }
        if (exist(absRoot, AbstractMetadata.antenna_pointing)) {
            propertiesJSON.put(SAR.observation_direction, absRoot.getAttributeString(AbstractMetadata.antenna_pointing).toLowerCase());
        }
    }

    private void writeOpticalProperties(final MetadataElement absRoot) {
        stacItem.addKeywords(EO.KeyWords.optical);

    }
}
