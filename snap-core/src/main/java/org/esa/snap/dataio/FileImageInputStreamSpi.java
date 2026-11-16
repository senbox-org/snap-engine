/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.dataio;

import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * Wrapper over <code>ChannelImageInputStreamSpi</code>.
 * This is used to create a mapped file I/O (using NIO) instead of the "classic" FileImageInputStream.
 *
 * @author Cosmin Cara
 */
public class FileImageInputStreamSpi extends ImageInputStreamSpi {

    public FileImageInputStreamSpi() {
        super();
    }

    @Override
    public String getDescription(Locale locale) {
        return "SNAP NIO File ImageInputStream";
    }

    @Override
    public ImageInputStream createInputStreamInstance(Object input, boolean useCache, java.io.File cacheDir) throws IOException {
        return createInputStreamInstance(input, useCache, cacheDir != null ? cacheDir.toPath() : null);
    }

    @Override
    public ImageInputStream createInputStreamInstance(Object input) throws IOException {
        return createInputStreamInstance(input, false, (Path) null);
    }

    public ImageInputStream createInputStreamInstance(Object input, boolean useCache, Path cacheDir) throws IOException {
        if (!(input instanceof Path inputFile)) {
            throw new IllegalArgumentException("This SPI accepts only java.nio.file.Path");
        }
        // We need to make sure the underlying channel is closed, because it may hold a reference to our file and
        // the respective Java classes do not close it.
        FileChannel channel = FileChannel.open(inputFile, StandardOpenOption.READ);
        if (useCache && cacheDir != null) {
            return new FileCacheImageInputStream(Channels.newInputStream(channel), cacheDir.toFile()) {
                @Override
                public void close() throws IOException {
                    channel.close();
                    super.close();
                }
            };
        }
        channel.close();
        return NIOFileImageInputStream.createInputStream(inputFile);
    }

    @Override
    public Class<?> getInputClass() {
        return Path.class;
    }
}
