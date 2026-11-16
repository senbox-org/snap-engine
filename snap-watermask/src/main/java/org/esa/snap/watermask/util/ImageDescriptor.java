package org.esa.snap.watermask.util;

import java.nio.file.Path;

/**
 */
public interface ImageDescriptor {

    int getImageWidth();

    int getImageHeight();

    int getTileWidth();

    int getTileHeight();

    Path getAuxdataDir();

    String getZipFileName();
}
