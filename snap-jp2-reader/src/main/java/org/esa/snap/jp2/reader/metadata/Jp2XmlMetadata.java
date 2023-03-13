/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.jp2.reader.metadata;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.jp2.reader.internal.JP2ProductReaderConstants;
import org.esa.snap.core.metadata.XmlMetadata;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * Metadata extracted from JP2 XML blocks.
 *
 * @author Cosmin Cara
 */
public class Jp2XmlMetadata extends XmlMetadata {

    public Jp2XmlMetadata(String name) {
        super(name);
    }

    @Override
    public String getFileName() {
        return "";
    }

    @Override
    public int getNumBands() {
        return 0;
    }

    @Override
    public String getProductName() {
        return null;
    }

    @Override
    public String getFormatName() {
        return null;
    }

    @Override
    public String getMetadataProfile() {
        return null;
    }

    public String[] getBandNames() {
        return getAttributeValues(JP2ProductReaderConstants.TAG_BAND_NAME);
    }

    public double[] getBandScaleFactors() {
        String[] scales = getAttributeValues(JP2ProductReaderConstants.TAG_BAND_SCALE);
        double[] values = null;
        if (scales != null) {
            values = Arrays.stream(scales).flatMapToDouble(s -> DoubleStream.of(Double.parseDouble(s))).toArray();
        }
        return values;
    }

    public double[] getBandOffsets() {
        String[] offsets = getAttributeValues(JP2ProductReaderConstants.TAG_BAND_OFFSET);
        double[] values = null;
        if (offsets != null) {
            values = Arrays.stream(offsets).flatMapToDouble(s -> DoubleStream.of(Double.parseDouble(s))).toArray();
        }
        return values;
    }

    @Override
    public int getRasterWidth() {
        String dims = getAttributeValue(JP2ProductReaderConstants.TAG_RASTER_DIMENSIONS, null);
        if (dims != null) {
            return Integer.parseInt(dims.split(" ")[0]);
        }
        return 0;
    }

    @Override
    public int getRasterHeight() {
        String dims = getAttributeValue(JP2ProductReaderConstants.TAG_RASTER_DIMENSIONS, null);
        if (dims != null) {
            return Integer.parseInt(dims.split(" ")[1]);
        }
        return 0;
    }

    @Override
    public String[] getRasterFileNames() {
        String fileName = getFileName();
        if (fileName != null) {
            return new String[] { fileName };
        } else {
            return new String[0];
        }
    }

    @Override
    public ProductData.UTC getProductStartTime() {
        return null;
    }

    @Override
    public ProductData.UTC getProductEndTime() {
        return null;
    }

    @Override
    public ProductData.UTC getCenterTime() {
        return null;
    }

    @Override
    public String getProductDescription() {
        return null;
    }

    /*
     * SNAP-1400 (https://senbox.atlassian.net/browse/SNAP-1400)
     * SNAP-3459 (https://senbox.atlassian.net/browse/SNAP-3459) - Do not rely on EPSG and check the actual axis name order
     * Keep in mind that there are JP2 files with values like "  42.788298611111109   23.271622685185189", resulting in empty tokens and tokens starting with white space
     * @return
     */
    public Point2D getOrigin() {
        Point2D origin = null;
        String coords = getAttributeValue(JP2ProductReaderConstants.TAG_ORIGIN, null);
        if (coords != null) {
            final String[] result = Arrays.stream(coords.split(" ")).filter(p -> !StringUtils.isEmpty(p)).map(String::trim).toArray(String[]::new);
            final String x = !isReversedAxisOrder() ? result[0] : result[1];
            final String y = !isReversedAxisOrder() ? result[1] : result[0];
            origin = new Point2D.Double(Double.parseDouble(x), Double.parseDouble(y));
        }
        return origin;
    }

    /*
     * SNAP-1400 (https://senbox.atlassian.net/browse/SNAP-1400)
     * SNAP-3459 (https://senbox.atlassian.net/browse/SNAP-3459) - Do not rely on EPSG and check the actual axis name order;
     *                                                             also the values within offsetVector should be considered depending on their logic.
     *  Keep in mind that there are JP2 files with values like ["  -0.000004629629630 0", "0    0.000004629629630"] for offsetVector
     */
    public double getStepX() {
        String[] values = getAttributeValues(JP2ProductReaderConstants.TAG_OFFSET_VECTOR);
        if (values != null) {
            int index = isReversedAxisOrder() ? 1 : 0;
            String[] result = Arrays.stream(values[index].split(" ")).filter(p -> !StringUtils.isEmpty(p)).map(String::trim).toArray(String[]::new);

            return Double.parseDouble(result[1]) != 0 ? Double.parseDouble(result[1]) : Double.parseDouble(result[0]);
        }
        return 0;
    }

    /*
     * SNAP-1400 (https://senbox.atlassian.net/browse/SNAP-1400)
     * SNAP-3459 (https://senbox.atlassian.net/browse/SNAP-3459) - Do not rely on EPSG and check the actual axis name order;
     *                                                             also the values within offsetVector should be considered depending on their logic.
     *  Keep in mind that there are JP2 files with values like ["  -0.000004629629630 0", "0    0.000004629629630"] for offsetVector
     */
    public double getStepY() {
        String[] values = getAttributeValues(JP2ProductReaderConstants.TAG_OFFSET_VECTOR);
        if (values != null) {
            int index = isReversedAxisOrder() ? 0 : 1;
            String[] result = Arrays.stream(values[index].split(" ")).filter(p -> !StringUtils.isEmpty(p)).map(String::trim).toArray(String[]::new);

            return Double.parseDouble(result[0]) != 0 ? Double.parseDouble(result[0]) : Double.parseDouble(result[1]);
        }
        return 0;
    }

    public String getCrsGeocoding() {
        String crs = null;
        String srs = getAttributeValue(JP2ProductReaderConstants.TAG_CRS_NAME, null);
        if (srs == null){
            srs = getAttributeValue(JP2ProductReaderConstants.TAG_CRS_NAME_VARIANT, null);
        }
        if (srs != null && srs.contains("crs")) {
            crs = srs.substring(srs.indexOf("crs:") + 4);
        }
        return crs;
    }

    public List<Point2D> getPolygonPositions() {
        String tiePointGridPointsString = getAttributeValue(JP2ProductReaderConstants.TAG_POLYGON_POSITIONS, null);
        if (!StringUtils.isBlank(tiePointGridPointsString)) {
            List<Point2D> positions = new ArrayList<>();
            String[] values = tiePointGridPointsString.split(" ");
            for (int index = 0; index < values.length; index += 2) {
                double x = Double.parseDouble(values[index]);
                double y = Double.parseDouble(values[index + 1]);
                positions.add(new Point2D.Double(x, y));
            }
            return positions;
        }
        return null;
    }

    /**
     * Check is the axis order is reversed
     *    SNAP-3459 (https://senbox.atlassian.net/browse/SNAP-3459): Do not rely on EPSG and check the actual axis name order
     * @return true is the axis order is reversed (e.g. [y,x] instead of [x,y])
     */
    public boolean isReversedAxisOrder() {
        String[] axisName = getAttributeValues(JP2ProductReaderConstants.TAG_AXIS_NAME);
        return axisName[0].contains("y") || axisName[0].contains("lat");
    }

}
