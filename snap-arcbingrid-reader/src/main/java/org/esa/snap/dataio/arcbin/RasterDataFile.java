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
package org.esa.snap.dataio.arcbin;

import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.dataio.arcbin.TileIndex.IndexEntry;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;

class RasterDataFile {

    static final String FILE_NAME = "w001001.adf";

    private final ImageInputStream imageInputStream;

    private RasterDataFile(ImageInputStream imageInputStream) {
        this.imageInputStream = imageInputStream;
    }

    void close() throws IOException {
        imageInputStream.close();
    }

    byte[] loadRawTileData(IndexEntry indexEntry) throws IOException {
        byte[] bytes = new byte[indexEntry.size + 2];
        synchronized (imageInputStream) {
            imageInputStream.seek(indexEntry.offset);
            imageInputStream.read(bytes);
        }
        return bytes;
    }

    static RasterDataFile create(Path file) throws IOException {
        ImageInputStream imageInputStream = ImageUtils.getImageInputStream(file);
        imageInputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
        return new RasterDataFile(imageInputStream);
    }
}
