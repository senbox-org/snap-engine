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

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Norman Fomferra
 */
public class UuidBox extends Box {

    private UUID uiid;
    private byte[] data;

    public UuidBox(BoxType type, long position, long length, int dataOffset) {
        super(type, position, length, dataOffset);
    }

    public UUID getUiid() {
        return uiid;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void readFrom(BoxReader reader) throws IOException {
        final ImageInputStream stream = reader.getStream();
        final long mostSigBits = stream.readLong();
        final long leastSigBits = stream.readLong();
        uiid = new UUID(mostSigBits, leastSigBits);
        final long dataLength = length - (stream.getStreamPosition() - position);
        data = new byte[(int) dataLength];
        int bytesRead = stream.read(data);
        if (bytesRead != dataLength) {
            throw new IOException(String.format("Unable to read requested %d bytes from UUID Box", dataLength));
        }
    }
}
