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

    public Point2D getOrigin() {
        Point2D origin = null;
        String coords = getAttributeValue(JP2ProductReaderConstants.TAG_ORIGIN, null);
        if (coords != null) {
            origin = new Point2D.Double(Double.parseDouble(coords.split(" ")[0]), Double.parseDouble(coords.split(" ")[1]));
        }
        return origin;
    }

    public double getStepX() {
        String[] values = getAttributeValues(JP2ProductReaderConstants.TAG_OFFSET_VECTOR);
        if (values != null) {
            return Double.parseDouble(values[0].split(" ")[0]);
        }
        return 0;
    }

    public double getStepY() {
        String[] values = getAttributeValues(JP2ProductReaderConstants.TAG_OFFSET_VECTOR);
        if (values != null) {
            return Double.parseDouble(values[1].split(" ")[1]);
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
}
