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

import javax.imageio.spi.ImageOutputStreamSpi;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * Wrapper over <code>ChannelImageOutputStreamSpi</code>.
 * This is used to create a mapped file I/O (using NIO) instead of the "classic" FileImageOutputStream.
 *
 * @author Cosmin Cara
 */
public class FileImageOutputStreamSpi extends ImageOutputStreamSpi {

    public FileImageOutputStreamSpi() {
        super();
    }

    @Override
    public ImageOutputStream createOutputStreamInstance(Object output, boolean useCache, java.io.File cacheDir) throws IOException {
        return createOutputStreamInstance(output, useCache, cacheDir != null ? cacheDir.toPath() : null);
    }

    @Override
    public ImageOutputStream createOutputStreamInstance(Object output) throws IOException {
        return createOutputStreamInstance(output, false, (Path) null);
    }

    public ImageOutputStream createOutputStreamInstance(Object output, boolean useCache, Path cacheDir) throws IOException {
        if (!(output instanceof Path outputFile)) {
            throw new IllegalArgumentException("This SPI accepts only java.nio.file.Path");
        }
        // We need to make sure the underlying channel is closed, because it may hold a reference to our file and
        // the respective Java classes do not close it.
        FileChannel channel = FileChannel.open(outputFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        if (useCache && cacheDir != null) {
            return new FileCacheImageOutputStream(Channels.newOutputStream(channel), cacheDir.toFile()) {
                @Override
                public void close() throws IOException {
                    channel.close();
                    super.close();
                }
            };
        }
        channel.close();
        return NIOFileImageOutputStream.createOutputStream(outputFile);
    }

    @Override
    public Class<?> getOutputClass() {
        return Path.class;
    }

    @Override
    public String getDescription(Locale locale) {
        return "SNAP NIO File ImageOutputStream";
    }

}
