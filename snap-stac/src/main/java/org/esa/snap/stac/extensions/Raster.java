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
package org.esa.snap.stac.extensions;

public class Raster implements StacExtension {

    private Raster() {
    }

    public final static String raster = "raster";
    public final static String schema = "https://stac-extensions.github.io/raster/v1.1.0/schema.json";

    public final static String bands = "raster:bands";

    public final static String name = "name";

    //number|string	Pixel values used to identify pixels that are nodata in the band either by the pixel value as a number or nan, inf or -inf (all strings).
    public final static String nodata = "nodata";

    //string	One of area or point. Indicates whether a pixel value should be assumed to represent a sampling over the region of the pixel or a point sample at the center of the pixel.
    public final static String sampling = "sampling";

    //string	The data type of the pixels in the band. One of the data types as described above.
    public final static String data_type = "data_type";

    //number	The actual number of bits used for this band. Normally only present when the number of bits is non-standard for the datatype, such as when a 1 bit TIFF is represented as byte
    public final static String bits_per_sample = "bits_per_sample";

    //number	Average spatial resolution (in meters) of the pixels in the band.
    public final static String spatial_resolution = "spatial_resolution";

    //Statistics Object	Statistics of all the pixels in the band
    public final static String statistics = "statistics";

    //string	unit denomination of the pixel value
    public final static String unit = "unit";

    //number	multiplicator factor of the pixel value to transform into the value (i.e. translate digital number to reflectance).
    public final static String scale = "scale";

    //number	number to be added to the pixel value (after scaling) to transform into the value (i.e. translate digital number to reflectance).
    public final static String offset = "offset";

    //Histogram Object	Histogram distribution information of the pixels values in the band
    public final static String histogram = "histogram";


    // Statistics

    //number	mean value of all the pixels in the band
    public final static String mean = "mean";

    //number	minimum value of the pixels in the band
    public final static String minimum = "minimum";

    //number	maximum value of the pixels in the band
    public final static String maximum = "maximum";

    //number	standard deviation value of the pixels in the band
    public final static String stdev = "stdev";

    //number	percentage of valid (not nodata) pixel
    public final static String valid_percent = "valid_percent";

    // data types

    public final static String int8 = "int8";           // 8-bit integer
    public final static String int16 = "int16";         // 16-bit integer
    public final static String int32 = "int32";         // 32-bit integer
    public final static String int64 = "int64";         // 64-bit integer
    public final static String uint8 = "uint8";         // unsigned 8-bit integer (common for 8-bit RGB PNG's)
    public final static String uint16 = "uint16";       // unsigned 16-bit integer
    public final static String uint32 = "uint32";       // unsigned 32-bit integer
    public final static String uint64 = "uint64";       // unsigned 64-bit integer
    public final static String float16 = "float16";     // 16-bit float
    public final static String float32 = "float32";     // 32-bit float
    public final static String float64 = "float64";     // 64-big float
    public final static String cint16 = "cint16";       // 16-bit complex integer
    public final static String cint32 = "cint32";       // 32-bit complex integer
    public final static String cfloat32 = "cfloat32";   // 32-bit complex float
    public final static String cfloat64 = "cfloat64";   // 64-bit complex float

    //Histogram

    //number	number of buckets of the distribution.
    public final static String count = "count";

    //number	minimum value of the distribution. Also the mean value of the first bucket.
    public final static String min = "min";

    //number	minimum value of the distribution. Also the mean value of the last bucket.
    public final static String max = "max";

    //[number]	Array of integer indicating the number of pixels included in the bucket.
    public final static String buckets = "buckets";
}
