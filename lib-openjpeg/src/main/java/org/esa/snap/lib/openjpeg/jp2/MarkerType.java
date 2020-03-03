/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2013-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.lib.openjpeg.jp2;

import org.esa.snap.lib.openjpeg.jp2.segments.CodingStyleDefaultSegment;
import org.esa.snap.lib.openjpeg.jp2.segments.IgnoredSegment;
import org.esa.snap.lib.openjpeg.jp2.segments.ImageAndTileSizeSegment;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
public enum MarkerType {
    ___(0x0000, IgnoredSegment.class),

    // Delimiting markers and marker segments

    // Start of codestream
    SOC(0xFF4F, Marker.class),
    // Start of tile-part
    SOT(0xFF90, Marker.class),
    // Start of data
    SOD(0xFF93, Marker.class),
    // End of codestream
    EOC(0xFFD9, Marker.class),

    // Fixed information marker segments

    // Image and tile size
    SIZ(0xFF51, ImageAndTileSizeSegment.class),
    // Functional marker segments
    // Coding style default
    COD(0xFF52, CodingStyleDefaultSegment.class),
    // Coding style component
    COC(0xFF53, Marker.class),
    // Region-of-interest
    RGN(0xFF5E, Marker.class),
    // Quantization default
    QCD(0xFF5C, Marker.class),
    // Quantization component
    QCC(0xFF5D, Marker.class),
    // Progression order change
    POC(0xFF5F, Marker.class),
    // Pointer marker segments
    TLM(0xFF55, Marker.class),
    // Tile-part lengths
    PLM(0xFF57, Marker.class),
    // Packet length, main header
    PLT(0xFF58, Marker.class),
    // Packet length, tile-part header
    PPM(0xFF60, Marker.class),
    // Packed packet headers, main header
    PPT(0xFF61, Marker.class),
    // Packed packet headers, tile-part header
    //TLM(0xFF55),

    // In-bit-stream markers and marker segments

    // Start of packet
    SOP(0xFF91, Marker.class),
    // End of packet header
    EPH(0xFF92, Marker.class),

    // Informational marker segments

    // Component registration
    CRG(0xFF63, Marker.class),
    // Comment
    COM(0xFF64, Marker.class);

    private final static Map<Integer, MarkerType> codeMap;
    private final int code;
    private final Class<? extends MarkerSegment> type;

    private MarkerType(int code, Class<? extends MarkerSegment> type) {
        this.code = code;
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public String toHexString() {
        return "0x" + Integer.toHexString(code).toUpperCase();
    }

    public MarkerSegment createSegment() {
        try {
            Constructor<? extends MarkerSegment> boxConstructor = type.getConstructor(MarkerType.class);
            return boxConstructor.newInstance(this);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    static {
        final MarkerType[] values = MarkerType.values();
        codeMap = new HashMap<Integer, MarkerType>();
        for (MarkerType value : values) {
            codeMap.put(value.getCode(), value);
        }
    }

    public static MarkerType get(Integer code) {
        return codeMap.get(code);
    }
}
