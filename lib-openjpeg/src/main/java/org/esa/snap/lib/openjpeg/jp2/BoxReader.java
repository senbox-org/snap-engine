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

import org.esa.snap.lib.openjpeg.jp2.boxes.IgnoredBox;

import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public class BoxReader {
    private final ImageInputStream stream;
    private final long fileLength;
    private final Listener listener;

    public BoxReader(ImageInputStream stream, long fileLength, Listener listener) {
        this.stream = stream;
        this.fileLength = fileLength;

        if (listener == null) {
            this.listener = new AEmptyListener();
        } else {
            this.listener = listener;
        }
    }

    public ImageInputStream getStream() {
        return stream;
    }

    public long getFileLength() {
        return fileLength;
    }

    public Box readBox() throws IOException {

        final long position = stream.getStreamPosition();

        final int nextInt32;
        try {
            nextInt32 = stream.readInt();
        } catch (EOFException e) {
            return null;
        }

        long length = nextInt32 & 0x00000000ffffffffL;
        final int type = stream.readInt();
        int dataOffset = 8;

        if (length == 0L) {
            length = fileLength - position;
        } else if (length == 1L) {
            length = stream.readLong();
            dataOffset += 8;
        }

        final Box box;
        if (length == 0L || length == 1L || length >= 8L) {
            final BoxType boxType = BoxType.get(type);
            if (boxType != null) {
                box = boxType.createBox(position, length, dataOffset);
                box.readFrom(this);
                listener.knownBoxSeen(box);
            } else {
                box = new IgnoredBox(type, position, length, dataOffset);
                stream.seek(position + length);
                listener.unknownBoxSeen(box);
            }
        } else {
            box = new IgnoredBox(type, position, length, dataOffset);
            listener.unknownBoxSeen(box);
        }
        return box;
    }

    public interface Listener {
        void knownBoxSeen(Box box);

        void unknownBoxSeen(Box box);
    }
}

