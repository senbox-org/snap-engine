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

package org.esa.snap.lib.openjpeg.jp2.boxes;

import org.esa.snap.lib.openjpeg.jp2.Box;
import org.esa.snap.lib.openjpeg.jp2.BoxReader;
import org.esa.snap.lib.openjpeg.jp2.BoxType;

import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public class Jpeg2000SignatureBox extends Box {

    private int signature;

    public Jpeg2000SignatureBox(BoxType type, long position, long length, int dataOffset) {
        super(type, position, length, dataOffset);
    }

    public int getSignature() {
        return signature;
    }

    @Override
    public void readFrom(BoxReader reader) throws IOException {
        signature = reader.getStream().readInt();
    }
}
