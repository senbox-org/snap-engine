package org.esa.snap.jp2.reader.internal;

import org.esa.snap.jp2.reader.JP2ImageFile;

import java.nio.file.Path;

public interface JP2BandData {

    public int getDataBufferType();

    public JP2ImageFile getJp2ImageFile();

    public Path getLocalCacheFolder();

    public int getBandCount();
}
