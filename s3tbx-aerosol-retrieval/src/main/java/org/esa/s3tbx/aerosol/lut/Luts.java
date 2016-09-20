/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.aerosol.lut;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteOrder;

/**
 * Access to the LookUpTables
 */
public class Luts {

    private static final String aotLutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_MOMO_ContinentalI_80_SDR_noG_v2.bin";
    private static final String aotKxLutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_MOMO_ContinentalI_80_SDR_noG_Kx-AOD_v2.bin";
    private static final String cwvLutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_6S_Tg_CWV_OZO.bin";
    private static final String cwvKxLutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_6S_Kx-CWV_OZO.bin";
    private static final String nskyLutDwPattern = "%INSTRUMENT%/%INSTRUMENT%_ContinentalI_80_Nsky_dw.bin";
    private static final String nskyLutDUpPattern = "%INSTRUMENT%/%INSTRUMENT%_ContinentalI_80_Nsky_up.bin";
    private static final String coeffPattern = "%INSTRUMENT%/N2B_coefs_%INSTRUMENT%_rmse_v2.txt";
    private static final String coeffDPattern = "%INSTRUMENT%/N2B_coefs_%INSTRUMENT%_Ddw_Dup.txt";


    public static ImageInputStream getAotLutData(String instrument) {
        return openStream(aotLutPattern.replace("%INSTRUMENT%", instrument));
    }

    public static ImageInputStream getAotKxLutData(String instrument) {
        return openStream(aotKxLutPattern.replace("%INSTRUMENT%", instrument));
    }

    public static ImageInputStream getCwvLutData(String instrument) {
        return openStream(cwvLutPattern.replace("%INSTRUMENT%", instrument));
    }

    public static ImageInputStream getCwvKxLutData(String instrument) {
        return openStream(cwvKxLutPattern.replace("%INSTRUMENT%", instrument));
    }

    public static ImageInputStream getNskyDwLutData(String instrument) {
        return openStream(nskyLutDwPattern.replace("%INSTRUMENT%", instrument));
    }

    public static ImageInputStream getNskyUpLutData(String instrument) {
        return openStream(nskyLutDUpPattern.replace("%INSTRUMENT%", instrument));
    }

    public static BufferedReader getN2BCoeffReader(String instrument) {
        return openReader(coeffPattern.replace("%INSTRUMENT%", instrument));
    }

    public static BufferedReader getN2BCoeffDReader(String instrument) {
        return openReader(coeffDPattern.replace("%INSTRUMENT%", instrument));
    }

    private static ImageInputStream openStream(String path) {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(openResource(path));
        ImageInputStream imageInputStream = new MemoryCacheImageInputStream(bufferedInputStream);
        imageInputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        return imageInputStream;
    }

    private static BufferedReader openReader(String path) {
        return new BufferedReader(new InputStreamReader(openResource(path)));
    }

    private static InputStream openResource(String path) {
        InputStream inputStream = Luts.class.getResourceAsStream(path);
        if (inputStream == null) {
            throw new IllegalArgumentException("Could not find resource: " + path);
        }
        return inputStream;
    }

    public static float[] readDimension(ImageInputStream iis) throws IOException {
        return readDimension(iis, iis.readInt());
    }

    public static float[] readDimension(ImageInputStream iis, int len) throws IOException {
        float[] dim = new float[len];
        iis.readFully(dim, 0, len);
        return dim;
    }
}
