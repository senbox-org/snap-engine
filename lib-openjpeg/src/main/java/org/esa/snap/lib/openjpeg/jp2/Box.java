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

import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public abstract class Box {
    protected final BoxType type;
    protected final long position;
    protected final long length;
    protected final int dataOffset;

    protected Box(BoxType type, long position, long length, int dataOffset) {
        this.type = type;
        this.position = position;
        this.length = length;
        this.dataOffset = dataOffset;
    }

    public BoxType getType() {
        return type;
    }

    public String getSymbol() {
        return type.getSymbol();
    }

    public int getCode() {
        return type.getCode();
    }

    public long getPosition() {
        return position;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public long getLength() {
        return length;
    }

    public abstract void readFrom(BoxReader reader) throws IOException;

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
