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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.esa.snap.lib.openjpeg.jp2.boxes.BitsPerComponentBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.ColorSpecificationBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.ContiguousCodestreamBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.FileTypeBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.IgnoredBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.ImageHeaderBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.Jp2HeaderBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.Jpeg2000SignatureBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.UuidBox;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
public enum BoxType {
    ____("    ", IgnoredBox.class),
    jP__("jP  ", Jpeg2000SignatureBox.class),
    ftyp("ftyp", FileTypeBox.class),
    jp2h("jp2h", Jp2HeaderBox.class),
    ihdr("ihdr", ImageHeaderBox.class),
    bpcc("bpcc", BitsPerComponentBox.class),
    colr("colr", ColorSpecificationBox.class),
    jp2c("jp2c", ContiguousCodestreamBox.class),
    uuid("uuid", UuidBox.class),;


    private final static Map<Integer, BoxType> codeMap;
    private final String symbol;
    private final int code;
    private final Class<? extends Box> type;

    static {
        final BoxType[] values = BoxType.values();
        codeMap = new HashMap<Integer, BoxType>();
        for (BoxType value : values) {
            codeMap.put(value.getCode(), value);
        }
    }

    private BoxType(String symbol, Class<? extends Box> type) {
        this.symbol = symbol;
        this.code = decode4b(symbol);
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getCode() {
        return code;
    }

    public Class<? extends Box> getType() {
        return type;
    }

    public Box createBox(long offset, long length, int dataOffset) {
        try {
            Constructor<? extends Box> boxConstructor = type.getConstructor(BoxType.class, Long.TYPE, Long.TYPE, Integer.TYPE);
            return boxConstructor.newInstance(this, offset, length, dataOffset);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static BoxType get(Integer code) {
        return codeMap.get(code);
    }

    public static int decode4b(String v) {
        if (v.length() != 4)
            throw new IllegalArgumentException();
        return v.charAt(0) << 24
                | v.charAt(1) << 16
                | v.charAt(2) << 8
                | v.charAt(3);
    }

    public static String encode4b(int v) {
        return new String(new byte[]{
                (byte) ((v & 0xff000000) >> 24),
                (byte) ((v & 0x00ff0000) >> 16),
                (byte) ((v & 0x0000ff00) >> 8),
                (byte) ((v & 0x000000ff))});
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
