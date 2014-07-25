package org.esa.beam.dataio.s3;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import javax.media.jai.ImageLayout;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PointOpImage;
import javax.media.jai.UnpackedImageData;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

final class LonLatFunctionOpImage extends PointOpImage {

    private LonLatFunction function;

    LonLatFunctionOpImage(RenderedImage lonImage, RenderedImage latImage, ImageLayout imageLayout,
                          LonLatFunction function) {
        super(lonImage, latImage, imageLayout, null, true);
        this.function = function;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle targetRectangle) {
        final int lonDataType = sources[0].getSampleModel().getDataType();
        final int latDataType = sources[1].getSampleModel().getDataType();
        final int targetDataType = target.getSampleModel().getDataType();
        final PixelAccessor lonAcc = new PixelAccessor(getSourceImage(0));
        final PixelAccessor latAcc = new PixelAccessor(getSourceImage(1));
        final PixelAccessor targetAcc = new PixelAccessor(this);
        final UnpackedImageData lonPixels;
        final UnpackedImageData latPixels;
        final UnpackedImageData targetPixels;

        lonPixels = lonAcc.getPixels(sources[0], targetRectangle, lonDataType, false);
        latPixels = latAcc.getPixels(sources[1], targetRectangle, lonDataType, false);
        targetPixels = targetAcc.getPixels(target, targetRectangle, targetDataType, true);

        if (lonDataType == DataBuffer.TYPE_FLOAT && latDataType == DataBuffer.TYPE_FLOAT) {
            if (targetDataType == DataBuffer.TYPE_FLOAT) {
                ff(lonPixels, latPixels, targetPixels, targetRectangle);
            } else if (targetDataType == DataBuffer.TYPE_DOUBLE) {
                fd(lonPixels, latPixels, targetPixels, targetRectangle);
            }
        } else if (lonDataType == DataBuffer.TYPE_DOUBLE && latDataType == DataBuffer.TYPE_DOUBLE) {
            if (targetDataType == DataBuffer.TYPE_FLOAT) {
                df(lonPixels, latPixels, targetPixels, targetRectangle);
            } else if (targetDataType == DataBuffer.TYPE_DOUBLE) {
                dd(lonPixels, latPixels, targetPixels, targetRectangle);
            }
        }

        targetAcc.setPixels(targetPixels);
    }

    private void dd(UnpackedImageData lonPixels, UnpackedImageData latPixels, UnpackedImageData targetPixels,
                    Rectangle targetRectangle) {
        final int lonLineStride = lonPixels.lineStride;
        final int latLineStride = lonPixels.lineStride;
        final int lonPixelStride = lonPixels.pixelStride;
        final int latPixelStride = lonPixels.pixelStride;
        final double[] lonData = lonPixels.getDoubleData(0);
        final double[] latData = latPixels.getDoubleData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        final double[] targetData = targetPixels.getDoubleData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int lonLineOffset = lonPixels.bandOffsets[0];
        int latLineOffset = latPixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        for (int y = 0; y < h; y++) {
            int lonPixelOffset = lonLineOffset;
            int latPixelOffset = latLineOffset;
            lonLineOffset += lonLineStride;
            latLineOffset += latLineStride;

            int targetPixelOffset = targetLineOffset;
            targetLineOffset += targetLineStride;

            for (int x = 0; x < w; x++) {
                final double lon = lonData[lonPixelOffset];
                if (lon >= -180.0 && lon <= 180.0) {
                    final double lat = latData[latPixelOffset];
                    if (lat >= -90.0 && lat <= 90.0) {
                        targetData[targetPixelOffset] = function.getValue(lon, lat);
                    }
                }

                lonPixelOffset += lonPixelStride;
                latPixelOffset += latPixelStride;
                targetPixelOffset += targetPixelStride;
            }
        }
    }

    private void df(UnpackedImageData lonPixels, UnpackedImageData latPixels, UnpackedImageData targetPixels,
                    Rectangle targetRectangle) {
        final int lonLineStride = lonPixels.lineStride;
        final int latLineStride = lonPixels.lineStride;
        final int lonPixelStride = lonPixels.pixelStride;
        final int latPixelStride = lonPixels.pixelStride;
        final double[] lonData = lonPixels.getDoubleData(0);
        final double[] latData = latPixels.getDoubleData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        final float[] targetData = targetPixels.getFloatData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int lonLineOffset = lonPixels.bandOffsets[0];
        int latLineOffset = latPixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        for (int y = 0; y < h; y++) {
            int lonPixelOffset = lonLineOffset;
            int latPixelOffset = latLineOffset;
            lonLineOffset += lonLineStride;
            latLineOffset += latLineStride;

            int targetPixelOffset = targetLineOffset;
            targetLineOffset += targetLineStride;

            for (int x = 0; x < w; x++) {
                final double lon = lonData[lonPixelOffset];
                if (lon >= -180.0 && lon <= 180.0) {
                    final double lat = latData[latPixelOffset];
                    if (lat >= -90.0 && lat <= 90.0) {
                        targetData[targetPixelOffset] = (float) function.getValue(lon, lat);
                    }
                }

                lonPixelOffset += lonPixelStride;
                latPixelOffset += latPixelStride;
                targetPixelOffset += targetPixelStride;
            }
        }
    }

    private void fd(UnpackedImageData lonPixels, UnpackedImageData latPixels, UnpackedImageData targetPixels,
                    Rectangle targetRectangle) {
        final int lonLineStride = lonPixels.lineStride;
        final int latLineStride = lonPixels.lineStride;
        final int lonPixelStride = lonPixels.pixelStride;
        final int latPixelStride = lonPixels.pixelStride;
        final float[] lonData = lonPixels.getFloatData(0);
        final float[] latData = latPixels.getFloatData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        final double[] targetData = targetPixels.getDoubleData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int lonLineOffset = lonPixels.bandOffsets[0];
        int latLineOffset = latPixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        for (int y = 0; y < h; y++) {
            int lonPixelOffset = lonLineOffset;
            int latPixelOffset = latLineOffset;
            lonLineOffset += lonLineStride;
            latLineOffset += latLineStride;

            int targetPixelOffset = targetLineOffset;
            targetLineOffset += targetLineStride;

            for (int x = 0; x < w; x++) {
                final float lon = lonData[lonPixelOffset];
                if (lon >= -180.0f && lon <= 180.0f) {
                    final float lat = latData[latPixelOffset];
                    if (lat >= -90.0f && lat <= 90.0f) {
                        targetData[targetPixelOffset] = function.getValue(lon, lat);
                    }
                }

                lonPixelOffset += lonPixelStride;
                latPixelOffset += latPixelStride;
                targetPixelOffset += targetPixelStride;
            }
        }
    }

    private void ff(UnpackedImageData lonPixels, UnpackedImageData latPixels, UnpackedImageData targetPixels,
                    Rectangle targetRectangle) {
        final int lonLineStride = lonPixels.lineStride;
        final int latLineStride = lonPixels.lineStride;
        final int lonPixelStride = lonPixels.pixelStride;
        final int latPixelStride = lonPixels.pixelStride;
        final float[] lonData = lonPixels.getFloatData(0);
        final float[] latData = latPixels.getFloatData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        final float[] targetData = targetPixels.getFloatData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int lonLineOffset = lonPixels.bandOffsets[0];
        int latLineOffset = latPixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        for (int y = 0; y < h; y++) {
            int lonPixelOffset = lonLineOffset;
            int latPixelOffset = latLineOffset;
            lonLineOffset += lonLineStride;
            latLineOffset += latLineStride;

            int targetPixelOffset = targetLineOffset;
            targetLineOffset += targetLineStride;

            for (int x = 0; x < w; x++) {
                final float lon = lonData[lonPixelOffset];
                if (lon >= -180.0f && lon <= 180.0f) {
                    final float lat = latData[latPixelOffset];
                    if (lat >= -90.0f && lat <= 90.0f) {
                        targetData[targetPixelOffset] = (float) function.getValue(lon, lat);
                    }
                }

                lonPixelOffset += lonPixelStride;
                latPixelOffset += latPixelStride;
                targetPixelOffset += targetPixelStride;
            }
        }
    }
}
